
package com.atakmap.android.maps.graphics;

import android.util.Pair;

import com.atakmap.android.maps.Arrow;
import com.atakmap.android.maps.Association;
import com.atakmap.android.maps.AxisOfAdvance;
import com.atakmap.android.maps.CircleCrumb;
import com.atakmap.android.maps.CompassRing;
import com.atakmap.android.maps.Doghouse;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaMapPoint;
import com.atakmap.android.maps.MetaShape;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.SensorFOV;
import com.atakmap.android.maps.SimpleRectangle;
import com.atakmap.android.toolbars.RangeAndBearingMapItem;
import com.atakmap.android.track.crumb.Crumb;
import com.atakmap.android.widgets.AutoSizeAngleOverlayShape;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.MapRenderer;

import org.apache.commons.lang.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.Disposable;

public final class GLMapGroup2 implements MapGroup.OnItemListChangedListener,
        MapGroup.OnGroupListChangedListener, MapGroup.OnVisibleChangedListener,
        GLMapItem2.OnVisibleChangedListener, Disposable {

    // private static final int _DEFAULT_SELECTED_COLOR = Color.argb(127, 255, 255, 255);

    public static final String TAG = "GLMapGroup2";

    static {
        GLMapItemFeatureFactory.registerSpi(GLMarkerFeature.SPI, 0);
        GLMapItemFeatureFactory.registerSpi(GLPolylineFeature.SPI, 0);
        GLMapItemFeatureFactory.registerSpi(GLEditablePolylineFeature.SPI, 1);
        GLMapItemFeatureFactory.registerSpi(GLCircleFeature.SPI, 1);
    }

    public GLMapGroup2(MapRenderer surface, GLQuadtreeNode2 renderer,
            MapGroup subject) {
        this(surface, null, renderer, subject);
    }

    GLMapGroup2(MapRenderer surface, GLMapItemFeatures features,
            GLQuadtreeNode2 legacyRenderer, MapGroup subject) {
        renderContext = surface;
        _features = features;
        this.renderer = legacyRenderer;

        // TODO: meta int selected color

        hidden = !subject.getVisible();

        for (MapGroup child : subject.getChildGroups()) {
            onGroupAdded(child, subject);
        }

        for (MapItem item : subject.getItems()) {
            onItemAdded(item, subject);
        }

        _subject = subject;
    }

    public void startObserving(MapGroup subject) {
        subject.addOnGroupListChangedListener(this);
        subject.addOnItemListChangedListener(this);
        subject.addOnVisibleChangedListener(this);
    }

    @Override
    public void onGroupAdded(MapGroup group, MapGroup parent) {
        // if the group is going to use a custom renderer, don't add its items
        // to the quadtree and don't observe it
        if (group.getMetaBoolean("customRenderer", false))
            return;

        GLMapGroup2 observer = new GLMapGroup2(renderContext, _features,
                this.renderer,
                group);
        observer._parent = this;
        observer.startObserving(group);
        synchronized (this) {
            childGroups.put(group.getSerialId(), observer);
        }
    }

    @Override
    public void onGroupRemoved(MapGroup group, MapGroup parent) {
        GLMapGroup2 child;
        synchronized (this) {
            child = childGroups.remove(group.getSerialId());
            if (child == null)
                return;
        }

        child.stopObserving(group);
        child._deepRemoveFromRenderer(true);
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup group) {
        // create the item via the factory. The default spi's default path is to defer to the
        // feature item factory
        final GLMapItem2 factoryDrawable = GLMapItemFactory
                .create3(renderContext, item);
        // if the factory drawable is not null or if deferral is appropriate, accept the factory
        // drawable, else try default creation again without deferring to the feature item factory
        final GLMapItem2 drawable = (factoryDrawable != null
                || _features != null) ? factoryDrawable
                        : createImpl(renderContext, item, false);
        if (drawable != null) {
            drawable.startObserving();
        } else {
            // handled as a feature
            if (_features != null && _features.addItem(item))
                _childFeatures.add(item);
            return;
        }

        // add to the list of observers... if it's an item we don't
        // care about (i.e. MetaMapPoint, etc.) we still leave a null
        // spot to keep our indices aligned
        synchronized (this) {
            childItems.put(item.getSerialId(), drawable);
        }
        drawable.addVisibleListener(this);

        // add it to the renderer
        if (_testVisibility(this, drawable)) {
            this.renderContext.queueEvent(new Runnable() {
                @Override
                public void run() {
                    _addToRenderer(drawable);
                }
            });
        }
    }

    private static Class lookupGLClass(Class<?> itemClass) {
        Class<?> glClass = null;

        try {
            final Package pkg = itemClass.getPackage();
            if (pkg == null)
                return null;

            String packageName = pkg.getName();
            String className = itemClass.getSimpleName();

            // Allow either graphics.GLClassName or just GLClassName? Former is good if you have
            // several, latter seems better if just one
            try {
                String glClassName = packageName + ".graphics.GL" + className;
                glClass = Class.forName(glClassName);
            } catch (ClassNotFoundException e) {
                String glClassName = packageName + ".GL" + className;
                glClass = Class.forName(glClassName);
            }
        } catch (ClassCastException | SecurityException
                | IllegalArgumentException e) {
            // Log.e(TAG, "Unable to instatiate GL class for map item of class " +
            // itemClass.getName());
            // Log.e(TAG, "error: ", e);
        } catch (ClassNotFoundException e) {
            // Log.e(TAG, "Unable to instatiate GL class for map item of class "+
            // itemClass.getName());
            // Log.e(TAG, "error: ", e);
        }

        // Check if it's a GLMapItem
        if (glClass != null && !GLMapItem2.class.isAssignableFrom(glClass)) {
            glClass = null;
            Log.e(TAG,
                    "GL class was not a subclass of GLMapItem2 for item of class "
                            + itemClass.getName());
        }

        return glClass;
    }

    private static boolean _testVisibility(GLMapGroup2 group, GLMapItem2 item) {
        boolean vis = item.isVisible();
        if (vis) {
            while (group != null) {
                if (group.hidden) {
                    vis = false;
                    break;
                }
                group = group._parent;
            }
        }
        return vis;
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup group) {
        if (_childFeatures.remove(item)) {
            _features.removeItem(item);
            return;
        }

        GLMapItem2 drawable;
        synchronized (this) {
            drawable = childItems.remove(item.getSerialId());
            if (drawable == null)
                return;
        }

        drawable.removeVisibleListener(this);
        drawable.stopObserving();

        final GLMapItem2 fDrawable = drawable;
        renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                _removeFromRenderer(fDrawable);
            }
        });
    }

    @Override
    public void onGroupVisibleChanged(final MapGroup group) { // TODO UPDATE THIS

        renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                boolean visible = group.getVisible();
                if (!hidden != visible) {
                    hidden = !visible;
                    if (visible && !deepAreParentsHidden()) {
                        _deepAddToRenderer();
                    } else {
                        _deepRemoveFromRenderer(false);
                    }
                }

            }
        });
    }

    private MapRenderer getRenderContext() {
        return renderContext;
    }

    public void stopObserving(MapGroup group) {
        group.removeOnGroupListChangedListener(this);
        group.removeOnItemListChangedListener(this);
        group.removeOnVisibleChangedListener(this);
    }

    @Override
    public void dispose() {
        _deepRemoveFromRenderer(true);
    }

    public GLMapGroup2 getParentGroup() {
        return _parent;
    }

    private boolean deepAreParentsHidden() {
        if (hidden || _parent == null) {
            return hidden;
        } else {
            return _parent.deepAreParentsHidden();
        }
    }

    protected synchronized void _deepAddToRenderer() {
        if (!hidden) {
            _addAllToRenderer(childItems.values());
            for (MapItem item : _childFeatures) {
                _features.addItem(item);
            }
            Collection<GLMapGroup2> groups = childGroups.values();
            for (GLMapGroup2 g : groups) {
                g._deepAddToRenderer();
            }
        }
    }

    protected synchronized void _deepRemoveFromRenderer(boolean permanent) {
        // Make a copy of this list since it may get cleared later on.
        final List<GLMapItem2> renderItems = new ArrayList<>(
                childItems.values());
        renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                _removeAllFromRenderer(renderItems);
            }
        });

        Collection<GLMapGroup2> groups = childGroups.values();
        for (MapItem item : _childFeatures) {
            _features.removeItem(item);
        }

        if (permanent && _subject != null) {
            stopObserving(_subject);
            _subject = null;
        }

        for (GLMapGroup2 group : groups) {
            group._deepRemoveFromRenderer(permanent);
        }
        if (permanent) {
            childItems.clear();
            childGroups.clear();
            _childFeatures.clear();
        }
    }

    protected void _addToRenderer(GLMapItem2 entry) {
        if (entry.isVisible()) {
            this.addedToRenderer.add(entry);
            this.renderer.insertItem(entry);
        }
    }

    protected void _addAllToRenderer(Collection<GLMapItem2> items) {
        for (GLMapItem2 entry : items)
            _addToRenderer(entry);
    }

    protected void _removeFromRenderer(GLMapItem2 item) {
        if (this.addedToRenderer.remove(item))
            this.renderer.removeItem(item);
    }

    protected void _removeAllFromRenderer(Collection<GLMapItem2> items) {
        for (GLMapItem2 entry : items)
            _removeFromRenderer(entry);
    }

    @Override
    public void onVisibleChanged(final GLMapItem2 item, final boolean visible) {
        renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    _addToRenderer(item);
                } else {
                    _removeFromRenderer(item);
                }
            }
        });
    }

    boolean hidden;
    private MapGroup _subject;
    final HashMap<Long, GLMapItem2> childItems = new HashMap<>();
    final Set<MapItem> _childFeatures = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    final Set<GLMapItem2> addedToRenderer = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    final HashMap<Long, GLMapGroup2> childGroups = new HashMap<>();
    private GLMapGroup2 _parent;
    private final MapRenderer renderContext;
    private final GLQuadtreeNode2 renderer;
    private final GLMapItemFeatures _features;
    static final Map<Class<? extends MapItem>, Class<? extends GLMapItem2>> glClasses = new ConcurrentHashMap<>();
    static final Map<Class<? extends GLMapItem2>, Constructor<?>> glCtors = new ConcurrentHashMap<>();

    static GLMapItem2 createImpl(MapRenderer surface, MapItem item,
            boolean deferToFeatures) {
        // check for Features API handling

        if (item.getClass().equals(Doghouse.class)) {
            // blacklisted forcing over to old renderer
        } else if (deferToFeatures && GLMapItemFeatureFactory.isSupported(item))
            return null;

        // Item is flagged not to render
        // Useful for parent map items that render using children items
        if (item.hasMetaValue("ignoreRender")
                || item instanceof MetaMapPoint
                || item instanceof MetaShape)
            return null;

        // proceed to create default legacy renderer
        if (item instanceof Marker)
            return new GLMarker2(surface, (Marker) item);
        else if (item instanceof RangeAndBearingMapItem)
            return new GLRangeAndBearingMapItem(surface,
                    (RangeAndBearingMapItem) item);
        else if (item instanceof Arrow)
            return new GLArrow2(surface, (Arrow) item);
        else if (item instanceof Association)
            return new GLAssociation2(surface, (Association) item);
        else if (item instanceof CircleCrumb)
            return new GLCircleCrumb(surface, (CircleCrumb) item);
        else if (item instanceof Crumb)
            return new GLCrumb(surface, (Crumb) item);
        else if (item instanceof CompassRing)
            return new GLCompassRing(surface, (CompassRing) item);
        else if (item.getClass().equals(Polyline.class))
            return new GLPolyline(surface, (Polyline) item);
        else if (item instanceof SimpleRectangle)
            return new GLRectangle(surface, (SimpleRectangle) item);
        else if (item instanceof Ellipse)
            return new GLEllipse(surface, (Ellipse) item);
        else if (item instanceof AxisOfAdvance)
            return new GLAxisOfAdvance(surface, (AxisOfAdvance) item);
        else if (item instanceof Doghouse)
            return new GLDogHouse(surface, (Doghouse) item);
        else if (item instanceof AutoSizeAngleOverlayShape)
            return new GLAngleOverlay2(surface,
                    (AutoSizeAngleOverlayShape) item);
        else if (item instanceof SensorFOV)
            return new GLSensorFOV(surface, (SensorFOV) item);

        return reflect(surface, item);
    }

    private static GLMapItem2 reflect(MapRenderer surface,
            MapItem item) {
        // Automatically try to find the GL class if it isn't one of the core map items but
        // something added by another component
        Class<? extends MapItem> itemClass = item.getClass();
        Class<? extends GLMapItem2> glClass = glClasses.get(itemClass);

        Class<?> currentClass = itemClass;
        // If class is cached, use it
        if (glClass == null) {
            // Otherwise, look it up
            while (currentClass != null && glClass == null) {
                glClass = lookupGLClass(currentClass);
                if (glClass == null)
                    currentClass = currentClass.getSuperclass();
            }

            // Cache class if found
            if (glClass != null)
                glClasses.put(itemClass, glClass);
        }

        // direct copy from GLMapGroup
        if (glClass == null) {
            // This is messy.. Should probably just pull in EditablePolyline to MapLibrary and
            // hard code it's GL class above.
            // But for now, it's losely coupled in DrawingTools and this check is needed to pick
            // up any Polyline subclasses
            // that don't have an un-proguardded GL class.
            // (above Polyline check only catches unsubclassed Polylines, since
            // GLEditablePolyline needs to be picked up by the lookup code.)
            if (item instanceof Polyline)
                return new GLPolyline(surface, (Polyline) item);
        }

        // If there is a GL class, instantiate it
        if (glClass != null) {
            Constructor<?> ctor = glCtors.get(glClass);
            if (ctor == null) {
                ctor = ConstructorUtils.getMatchingAccessibleConstructor(
                        glClass,
                        new Class[] {
                                MapRenderer.class, currentClass
                        });
                glCtors.put(glClass, ctor);
            }

            try {
                return (GLMapItem2) ctor.newInstance(surface, item);
            } catch (InstantiationException | InvocationTargetException
                    | IllegalAccessException ignored) {
            }
        }

        return null;
    }

    public final static GLMapItemSpi3 DEFAULT_GLMAPITEM_SPI3 = new GLMapItemSpi3() {

        @Override
        public int getPriority() {
            // this SPI will be the fall-through if all else fails
            return 0;
        }

        @Override
        public GLMapItem2 create(Pair<MapRenderer, MapItem> arg) {
            final MapRenderer surface = arg.first;
            final MapItem item = arg.second;

            return createImpl(surface, item, true);
        }
    };
}
