
package com.atakmap.android.drawing.milsym;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.importexport.handlers.ParentMapItem;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.MultiPolyline;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.util.EditAction;
import com.atakmap.android.util.Undoable;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.Vector2D;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.MathUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.marshal.IMarshal;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;

final class TacticalGraphic implements
        Shape.OnPointsChangedListener,
        Shape.OnStrokeColorChangedListener,
        Shape.OnStrokeWeightChangedListener,
        DrawingCircle.OnRadiusChangedListener,
        EditablePolyline.OnEditableChangedListener,
        MapItem.OnMetadataChangedListener,
        MapItem.OnVisibleChangedListener,
        MapEventDispatcher.MapEventDispatchListener {

    static {
        MarshalManager.registerMarshal(new IMarshal() {
            @Override
            public <T, V> T marshal(V v) {
                GeoPoint[] src = (GeoPoint[]) v;
                IGeoPoint[] dst = new IGeoPoint[src.length];
                for (int i = 0; i < src.length; i++)
                    dst[i] = MarshalManager.marshal(src[i], GeoPoint.class,
                            IGeoPoint.class);
                return (T) dst;
            }
        }, GeoPoint[].class, IGeoPoint[].class);
    }

    private final Shape subject;

    private Marker drawingMarker = null;

    private final Set<Long> fids;
    private String milsym;

    private final FeatureDataStore2 datastore;

    private GeoPoint[] originalPoints;
    private int prevFill;

    private GeoPoint dragStart;


    private final static DefaultMapGroup junkMapGroup = new DefaultMapGroup("__TG");


    private final PropertyChangeListener milSymChangedListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            AttributeSet attrs = (AttributeSet) propertyChangeEvent.getSource();
            String uid = attrs.getStringAttribute("uid", null);
            if (uid != null) {
                MapItem mapItem = MapView.getMapView().getMapItem(uid);
                onMilSymChanged(mapItem);
            }
        }
    };

    private final PropertyChangeListener modifierChangedListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            refresh();
        }
    };

    private static final String TAG = "TacticalGraphic";

    UnitPreferences _prefs = new UnitPreferences(MapView.getMapView().getContext());

    TacticalGraphic(FeatureDataStore2 datastore,
                    Shape subject) {
        this.datastore = datastore;
        this.subject = subject;
        this.milsym = subject.getMetaString("milsym", milsym);
        this.fids = new HashSet<>();

        originalPoints = subject.getPoints();

        this.subject.addOnMetadataChangedListener("editing", this);

        // Detail handler updates the AttributeSet; change notification won't propagate through onMetaDataChanged
        AttributeSet subjectAttrs = MarshalManager.marshal(subject,
                MetaDataHolder2.class, AttributeSet.class);
        subjectAttrs.addPropertyChangeListener("milsym", milSymChangedListener);

        final ISymbologyProvider symbology = SymbologyProvider.getProviderFromSymbol(milsym);
        final Collection<Modifier> modifiers = symbology != null ?
                symbology.getModifiers(milsym) : null;
        if(modifiers != null) {
            for (Modifier mod : modifiers) {
                subjectAttrs.addPropertyChangeListener(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                + mod.getId(),
                        modifierChangedListener);
            }
        }

        onMilSymChanged(this.subject);
    }

    void recomputeControlPoint() {
        GeoPoint[] newPoints = subject.getPoints();

        if (!MilSymUtils.isEditing(subject) || newPoints.length == 0
                || originalPoints.length == 0)
            return;

        if (SymbologyProvider.getDefaultSourceShape(milsym) != ShapeType.LineString)
            return;

        GeoPoint nP0 = newPoints[0];
        GeoPoint oP0 = originalPoints[0];

        if (nP0.equals(oP0))
            return;

        if (originalPoints.length < 2 || newPoints.length < 2)
            return;

        GeoPoint p0 = originalPoints[0];
        GeoPoint p1 = originalPoints[1];

        GeoPoint np0 = newPoints[0];
        GeoPoint np1 = newPoints[1];

        if (drawingMarker == null) {
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT, p0.getLatitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON,
                    p0.getLongitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT, p0.getAltitude());
            return;
        }

        Vector2D vecCP = new Vector2D(
                drawingMarker.getPoint().getLatitude() - p0.getLatitude(),
                drawingMarker.getPoint().getLongitude() - p0.getLongitude());
        Vector2D vec = new Vector2D(p1.getLatitude() - p0.getLatitude(),
                p1.getLongitude() - p0.getLongitude());
        Vector2D nvec = new Vector2D(np1.getLatitude() - np0.getLatitude(),
                np1.getLongitude() - np0.getLongitude());

        double distance = vecCP.mag();

        vec = vec.normalize();
        vecCP = vecCP.normalize();
        nvec = nvec.normalize();

        double angle = -Math.acos(vec.dot(vecCP));

        Vector2D dir = new Vector2D(
                Math.cos(angle) * nvec.x - Math.sin(angle) * nvec.y,
                Math.sin(angle) * nvec.x + Math.cos(angle) * nvec.y);

        dir = dir.scale(distance);

        GeoPoint point = new GeoPoint(nP0.getLatitude() + dir.x,
                nP0.getLongitude() + dir.y);

        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT, point.getLatitude());
        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON, point.getLongitude());
        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT, point.getAltitude());

        drawingMarker.setPoint(point);
    }

    @Override
    public void onPointsChanged(Shape shape) {
        recomputeControlPoint();
        refresh();
        originalPoints = subject.getPoints();

    }

    @Override
    public void onRadiusChanged(DrawingCircle var1, double var2) {
        refresh();
    }

    @Override
    public void onStrokeColorChanged(Shape shape) {
        refresh();
    }

    @Override
    public void onStrokeWeightChanged(Shape shape) {
        refresh();
    }

    @Override
    public void onEditableChanged(EditablePolyline editablePolyline) {
        refresh();
    }

    @Override
    public void onVisibleChanged(MapItem mapItem) {
        try {
            datastore.setFeatureSetVisible(subject.getSerialId(),
                    mapItem.getVisible());
        } catch (DataStoreException ignored) {
        }
    }

    void deleteDrawingMarker() {
        if (drawingMarker == null)
            return;

        drawingMarker.removeFromGroup();
        drawingMarker.dispose();
        drawingMarker = null;
    }

    void removeControlPoint() {
        deleteDrawingMarker();
        subject.removeMetaData(MilSymStandardAttributes.MILSYM_CP_LON);
        subject.removeMetaData(MilSymStandardAttributes.MILSYM_CP_LAT);
        subject.removeMetaData(MilSymStandardAttributes.MILSYM_CP_ALT);
    }

    GeoPoint getControlPoint() {
        if (drawingMarker != null)
            return drawingMarker.getPoint();
        else if (subject.hasMetaValue(MilSymStandardAttributes.MILSYM_CP_LAT)
                && subject.hasMetaValue(MilSymStandardAttributes.MILSYM_CP_LON))
            return new GeoPoint(
                    subject.getMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT,
                            Double.NaN),
                    subject.getMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON,
                            Double.NaN),
                    subject.getMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT,
                            Double.NaN));
        else
            return null;
    }

    void ensureDrawingMarker(GeoPoint point) {
        if (point == null)
            return;
        if (drawingMarker == null) {
            final String uid = UUID.randomUUID().toString();
            this.drawingMarker = generateDrawingMarker(point, uid);
        } else {
            drawingMarker.setPoint(point);
        }

        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT, point.getLatitude());
        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON, point.getLongitude());
        subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT, point.getAltitude());
    }

    @NonNull
    private static Marker generateDrawingMarker(GeoPoint point, String uid) {
        Marker m = new Marker(point, uid);
        m.setTitle("");
        m.setType("side_" + DrawingEllipse.COT_TYPE);
        m.setMetaBoolean("drag", true);
        m.setMetaBoolean("editable", true);
        m.setMovable(true);

        // always hide these types of waypoints on the objects list
        m.setMetaBoolean("addToObjList", false);
        m.setMetaBoolean("ignoreMenu", true);
        m.setMetaString("how", "h-g-i-g-o"); // don't autostale it
        m.setVisible(true);
        return m;
    }

    void endEditing() {
        subject.setMetaBoolean("ignoreRender", true);
        final MapGroup parent = subject.getGroup();

        if (drawingMarker != null) {
            drawingMarker.removeFromGroup();

            final MapView mapView = MapView.getMapView();
            if (mapView == null)
                return;
            final MapEventDispatcher dispatcher = mapView
                    .getMapEventDispatcher();

            dispatcher.removeMapEventListener(MapEvent.ITEM_DRAG_STARTED, this);
            dispatcher.removeMapEventListener(MapEvent.ITEM_DRAG_CONTINUED,
                    this);
            dispatcher.removeMapEventListener(MapEvent.ITEM_DRAG_DROPPED, this);
        }

        if (subject instanceof EditablePolyline
                && !(subject instanceof MultiPolyline)) {
            // cycle parent to rebuild renderer
            if (parent != null) {
                junkMapGroup.addItem(subject);
                parent.addItem(subject);
            }

        }
        //adding / removing Rectangle from parent exposes a host of problems
        //perform a different rendering rebuild path for Rectangle
        if (subject instanceof ParentMapItem) {
            ParentMapItem parentMapItem = (ParentMapItem) subject;
            if (subject.getGroup() != null) {
                for (MapItem childItem : parentMapItem.getChildMapGroup()
                        .getItems()) {
                    childItem.setMetaBoolean("ignoreRender", true);
                }
                MapGroup childGroup = parentMapItem.getChildMapGroup();


                junkMapGroup.addGroup(childGroup);
                if (subject.getGroup() != null) {

                    subject.getGroup().addGroup(childGroup);
                }
            } else {
                // delete features because the subject has been removed
                try {
                    datastore.acquireModifyLock(true);
                    try {
                        for (long fid : fids)
                            datastore.deleteFeature(fid);
                        fids.clear();
                    } finally {
                        datastore.releaseModifyLock();
                    }
                } catch (DataStoreException | InterruptedException ignored) {
                }
            }

        }
    }

    void beginEditing() {
        showDrawingShape();
        ensureDrawingMarker(getControlPoint());

        if (drawingMarker != null) {
            ((ParentMapItem) subject).getChildMapGroup().addItem(drawingMarker);

            final MapView mapView = MapView.getMapView();
            if (mapView == null)
                return;
            final MapEventDispatcher dispatcher = mapView
                    .getMapEventDispatcher();

            dispatcher.addMapEventListener(MapEvent.ITEM_DRAG_STARTED, this);
            dispatcher.addMapEventListener(MapEvent.ITEM_DRAG_CONTINUED, this);
            dispatcher.addMapEventListener(MapEvent.ITEM_DRAG_DROPPED, this);
        }
    }

    void showDrawingShape() {
        final MapGroup parent = subject.getGroup();

        if (subject instanceof EditablePolyline) {
            subject.removeMetaData("ignoreRender");

            // cycle parent to rebuild renderer
            if (parent != null) {
                junkMapGroup.addItem(subject);
                parent.addItem(subject);
            }

        }
        if (subject instanceof ParentMapItem) {
            MapGroup childMapGroup = ((ParentMapItem) subject)
                    .getChildMapGroup();
            for (MapItem child : childMapGroup.getItems()) {
                child.removeMetaData("ignoreRender");
            }
            subject.removeMetaData("ignoreRender");

            if (parent != null) {
                junkMapGroup.addGroup(childMapGroup);
                subject.getGroup().addGroup(childMapGroup);
            }

        }
    }

    public boolean isHidingDrawingShape(MapItem item) {
        //DrawingEllipse has meta value "ignoreRender" by default
        if (item instanceof DrawingEllipse)
            item = ((DrawingEllipse) subject).getOutermostEllipse();
        if (item instanceof MultiPolyline) {
            List<DrawingShape> list = ((MultiPolyline) subject).getLines();
            if (!list.isEmpty())
                item = list.get(0);
        }
        return item.hasMetaValue("ignoreRender");
    }

    boolean shouldEndEditing(boolean isEditing) {
        boolean hasIgnoreRender = isHidingDrawingShape(subject);
        if (!hasIgnoreRender && !isEditing)
            return true;
        else if (isEditing)
            return false;

        if (MilSymUtils.isDrawingShape(subject)) {
            MapGroup childMapGroup = ((ParentMapItem) subject)
                    .getChildMapGroup();
            for (MapItem child : childMapGroup.getItems()) {
                if (!child.hasMetaValue("ignoreRender"))
                    return true;
            }
        }
        return false;
    }

    public boolean isInBounds(GeoBounds bounds) {
        if (bounds == null)
            return false;
        GeoBounds subjectBounds = getBounds();
        if (subjectBounds == null)
            return false;

        return bounds.contains(subjectBounds) || subjectBounds.contains(bounds)
                || bounds.intersects(subjectBounds);
    }

    public GeoBounds getBounds() {
        GeoBounds.Builder builder = new GeoBounds.Builder();
        for (GeoPoint point : subject.getPoints()) {
            builder.add(point.getLatitude(), point.getLongitude());
        }
        return builder.build();
    }

    void refresh() {
        if (milsym == null)
            return;
        final boolean isEditing = MilSymUtils.isEditing(subject);
        final int strokeWidth = isEditing ? 1
                : (int) Math.max(subject.getStrokeWeight(), 1d);
        final int strokeColor = getColor(subject.getStrokeColor(),
                isEditing ? 0.5f : 1f);

        final ShapeType shapeType;
        Shape pointsSource = subject;
        boolean close = true;
        int pointsLimit = Integer.MAX_VALUE;
        if (subject instanceof DrawingCircle) {
            shapeType = ShapeType.Circle;
            pointsSource = ((DrawingCircle) subject).getOutermostRing();
        } else if (subject instanceof DrawingEllipse) {
            shapeType = ShapeType.Ellipse;
            pointsSource = ((DrawingEllipse) subject).getOutermostEllipse();
        } else if (subject instanceof DrawingRectangle) {
            shapeType = ShapeType.Rectangle;
            pointsLimit = 4;
        } else if (MathUtils.hasBits(subject.getStyle(),
                Polyline.STYLE_CLOSED_MASK)) {
            shapeType = ShapeType.Polygon;
        } else {
            shapeType = ShapeType.LineString;
            close = false;
        }

        GeoPoint[] sourcePoints = pointsSource.getPoints();

        // if the source shape is exclusively a rectangle, ensure 4 points
        do {
            final ISymbolTable symbols = SymbologyProvider.getSymbolTable(milsym);
            if(symbols == null)
                break;
            final ISymbolTable.Symbol symbol = symbols.getSymbol(milsym);
            if (symbol == null)
                break;
            final EnumSet<ShapeType> sourceShapes = symbol.getContentMask();
            if (sourceShapes.size() == 1
                    && sourceShapes.contains(ShapeType.Rectangle)
                    && sourcePoints.length < 4)
                return;
        } while(false);

        if (sourcePoints.length == 0)
            return;

        if (pointsLimit < sourcePoints.length) {
            GeoPoint[] tmp = new GeoPoint[pointsLimit];
            System.arraycopy(sourcePoints, 0, tmp, 0, pointsLimit);
            sourcePoints = tmp;
        }
        if (close) {
            GeoPoint[] tmp = new GeoPoint[sourcePoints.length + 1];
            System.arraycopy(sourcePoints, 0, tmp, 0, sourcePoints.length);
            tmp[sourcePoints.length] = sourcePoints[0];
            sourcePoints = tmp;
        }
        AttributeSet attrs = new AttributeSet();
        if (subject.getTitle() != null)
            attrs.setAttribute("name", subject.getTitle());

        final Collection<Modifier> mods = SymbologyProvider.getModifiers(milsym);
        if(mods != null) {
            for (Modifier mod : mods) {
                String postfix = mod.getId();
                String meta = subject.getMetaString(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + postfix, null);
                if (meta != null) {
                    attrs.setAttribute(
                            MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + postfix, meta);
                }
            }
        }
        final GeoBounds screenBounds = MapView.getMapView().getBounds();

        final ISymbologyProvider.RendererHints hints = new ISymbologyProvider.RendererHints();
        hints.strokeColor = strokeColor;
        hints.strokeWidth = strokeWidth;
        hints.shapeType = shapeType;
        hints.resolution = MapView.getMapView().getMapResolution();
        hints.boundingBox = new Envelope(screenBounds.getWest(),
                screenBounds.getSouth(), 0d, screenBounds.getEast(),
                screenBounds.getNorth(), 0d);
        hints.controlPoint = MarshalManager.marshal(
                getControlPoint(),
                GeoPoint.class,
                IGeoPoint.class);

        boolean agl =  _prefs.get("alt_display_agl", false);
        attrs.setAttribute("milsym.altitudeReference", agl ? "AGL" : _prefs.getAltitudeReference());
        attrs.setAttribute("milsym.altitudeUnits", _prefs.getAltitudeUnits().getPlural());

        Collection<Feature> features;
        try {
            features = SymbologyProvider.renderMultipointSymbol(
                    milsym,
                    MarshalManager.marshal(sourcePoints, GeoPoint[].class,
                            IGeoPoint[].class),
                    attrs,
                    hints);
        } catch (Exception e) {
            Log.e(TAG, "error occurred rendering " + milsym, e);
            return;
        }

        if (hints.controlPoint != null) {
            ensureDrawingMarker(MarshalManager.marshal(hints.controlPoint,
                    IGeoPoint.class, GeoPoint.class));
        } else {
            deleteDrawingMarker();
        }
        if (features == null)
            return;

        // parse and inject features
        try {
            datastore.acquireModifyLock(true);
            try {
                // delete old features
                synchronized (this) {
                    for (long fid : fids)
                        datastore.deleteFeature(fid);
                    fids.clear();

                    if (Utils.getFeatureSet(datastore,
                            subject.getSerialId()) == null)
                        datastore.insertFeatureSet(new FeatureSet(
                                subject.getSerialId(), "milsym", "milsym",
                                subject.getUID(), Double.MAX_VALUE, 0d, 1L));

                    // add new features
                    for (Feature f : features) {
                        FeatureDataSource.FeatureDefinition def = new FeatureDataSource.FeatureDefinition();
                        def.name = f.getName();
                        def.rawGeom = f.getGeometry();
                        def.geomCoding = FeatureDefinition.GEOM_ATAK_GEOMETRY;
                        def.rawStyle = f.getStyle();
                        def.styleCoding = FeatureDefinition.STYLE_ATAK_STYLE;
                        def.attributes = f.getAttributes();
                        def.extrude = f.getExtrude();
                        def.altitudeMode = f.getAltitudeMode();
                        final long fid = datastore.insertFeature(
                                subject.getSerialId(),
                                FeatureDataStore2.FEATURE_ID_NONE, def,
                                FeatureDataStore2.FEATURE_VERSION_NONE);
                        fids.add(fid);
                    }
                }
            } finally {
                datastore.releaseModifyLock();
            }
        } catch (DataStoreException | InterruptedException ignored) {
        }

        if (shouldEndEditing(isEditing)) {
            endEditing();
        } else if (isHidingDrawingShape(subject) && isEditing) {
            beginEditing();
        }
    }

    @Override
    public void onMetadataChanged(MapItem mapItem, String s) {
        if (subject.getEditing()) {
            prevFill = subject.getFillColor();
            subject.setFillColor(0x00FFFFFF & prevFill);
        } else {
            subject.setFillColor(prevFill);
        }
        refresh();
    }
    private void onMilSymChanged(MapItem mapItem) {
        onMilSymChanged(mapItem, false);
    }
    private void onMilSymChanged(MapItem mapItem, boolean forceRefresh) {
        if (!(mapItem instanceof Polyline)
                && !MilSymUtils.isDrawingShape(mapItem))
            return;
        final String milsym = this.subject.getMetaString("milsym", null);
        final String prevMilsym = this.milsym;
        if (Objects.equals(this.milsym, milsym) && !forceRefresh)
            return;

        if (this.milsym != null)
            removeControlPoint();

        if (this.milsym == null && milsym != null) {
            this.milsym = milsym;
            if (this.subject instanceof DrawingCircle)
                ((DrawingCircle) this.subject)
                        .addOnRadiusChangedListener(this);
            this.subject.addOnPointsChangedListener(this);
            this.subject.addOnStrokeColorChangedListener(this);
            this.subject.addOnStrokeWeightChangedListener(this);
            this.subject.addOnVisibleChangedListener(this);
            this.onPointsChanged(this.subject);
        } else if (this.milsym != null && milsym == null) {
            this.milsym = null;
            if (this.subject instanceof DrawingCircle)
                ((DrawingCircle) this.subject)
                        .removeOnRadiusChangedListener(this);

            this.subject.removeOnPointsChangedListener(this);
            this.subject.removeOnStrokeColorChangedListener(this);
            this.subject.removeOnStrokeWeightChangedListener(this);
            this.subject.removeOnVisibleChangedListener(this);
            AttributeSet subjectAttrs = MarshalManager.marshal(subject,
                    MetaDataHolder2.class, AttributeSet.class);
            subjectAttrs.removePropertyChangeListener("milsym",
                    milSymChangedListener);
            this.subject.removeOnMetadataChangedListener("editing", this);
            for (Modifier mod : SymbologyProvider.getNullModifiersForCode(prevMilsym)) {
                subjectAttrs.removePropertyChangeListener(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + mod.getId(),
                        modifierChangedListener);
            }

            // delete features
            try {
                datastore.acquireModifyLock(true);
                try {
                    for (long fid : fids)
                        datastore.deleteFeature(fid);
                    fids.clear();
                } finally {
                    datastore.releaseModifyLock();
                }
            } catch (DataStoreException | InterruptedException ignored) {
            }

            // restore
            if (isHidingDrawingShape(subject)) {
                showDrawingShape();

            }
        } else { // !this.milsym.equals(milsym)
            this.milsym = milsym;
            this.onPointsChanged(this.subject);
        }
    }

    static int getColor(int color, float alphamod) {
        return ((int) Math.max(Color.alpha(color) * alphamod, 16f) << 24)
                | (color & 0xFFFFFF);
    }

    static void clearMilsymMetadata(MapItem item) {
        item.removeMetaData("milsym");
        item.removeMetaData(MilSymStandardAttributes.MILSYM_CP_LAT);
        item.removeMetaData(MilSymStandardAttributes.MILSYM_CP_LON);
        item.removeMetaData(MilSymStandardAttributes.MILSYM_CP_ALT);

        for (String key : item.getAttributeNames()) {
            if (key.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
                item.removeMetaData(key);
        }
    }

    Shape getSubject() {
        return subject;
    }

    class EditControlPointAction extends EditAction {
        GeoPoint oldCP, newCP;

        public EditControlPointAction(GeoPoint newCP, GeoPoint oldCP) {
            this.oldCP = oldCP;
            this.newCP = newCP;
        }

        @Override
        public boolean run() {
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT,
                    newCP.getLatitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON,
                    newCP.getLongitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT,
                    newCP.getAltitude());

            drawingMarker.setPoint(newCP);
            refresh();
            return true;
        }

        @Override
        public void undo() {
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LAT,
                    oldCP.getLatitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_LON,
                    oldCP.getLongitude());
            subject.setMetaDouble(MilSymStandardAttributes.MILSYM_CP_ALT,
                    oldCP.getAltitude());

            drawingMarker.setPoint(oldCP);
            refresh();
        }

        @Override
        public String getDescription() {
            return null;
        }
    }

    @Override
    public void onMapEvent(MapEvent event) {
        if (drawingMarker == null || event.getItem() != drawingMarker)
            return;

        final String type = event.getType();

        if (!(type.equals(MapEvent.ITEM_DRAG_CONTINUED)
                || type.equals(MapEvent.ITEM_DRAG_STARTED) ||
                type.equals(MapEvent.ITEM_DRAG_DROPPED)))
            return;

        GeoPointMetaData gp = MapView.getMapView()
                .inverseWithElevation(
                        event.getPointF().x,
                        event.getPointF().y);

        if (!gp.get().isValid())
            return;

        EditControlPointAction action = new EditControlPointAction(gp.get(),
                dragStart);

        switch (type) {
            case MapEvent.ITEM_DRAG_STARTED:
                dragStart = drawingMarker.getPoint();
                action.run();
                break;
            case MapEvent.ITEM_DRAG_CONTINUED:
                action.run();
                break;
            case MapEvent.ITEM_DRAG_DROPPED:
                Tool tool = ToolManagerBroadcastReceiver.getInstance()
                        .getActiveTool();

                if (tool instanceof Undoable) {
                    ((Undoable) tool).run(action);
                } else
                    action.run();

                break;
        }
    }

}
