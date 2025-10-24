
package com.atakmap.android.maps.graphics;

import android.util.Pair;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.track.crumb.Crumb;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.MathUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GLCrumb2 extends GLPointMapItem2 implements
        Crumb.OnCrumbColorChangedListener,
        Crumb.OnCrumbDirectionChangedListener, Crumb.OnCrumbSizeChangedListener,
        Crumb.OnCrumbDrawLineToSurfaceChangedListener {

    public final static GLMapItemSpi3 SPI = new GLMapItemSpi3() {
        WeakReference<GLCrumbs> crumbsRendererRef = new WeakReference<>(null);

        @Override
        public int getPriority() {
            return 1;
        }

        @Override
        public GLMapItem2 create(Pair<MapRenderer, MapItem> object) {
            final MapItem item = object.second;
            if (!(item instanceof Crumb))
                return null;
            GLCrumbs crumbsRenderer;
            synchronized (this) {
                crumbsRenderer = crumbsRendererRef.get();
                if (crumbsRenderer == null) {
                    crumbsRenderer = new GLCrumbs();
                    crumbsRendererRef = new WeakReference<>(crumbsRenderer);
                }
            }
            return new GLCrumb2(object.first, (Crumb) item, crumbsRenderer);
        }
    };

    final GLCrumbs crumbsRenderer;
    boolean drawLineToSurface;
    int color;
    float radius;
    double rot;
    final Crumb sub;
    int crumbid = -1;
    AtomicBoolean dirty;

    public GLCrumb2(MapRenderer surface, Crumb subject,
            GLCrumbs crumbsRenderer) {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
        this.crumbsRenderer = crumbsRenderer;
        sub = subject;

        this.color = subject.getColor();
        this.rot = subject.getDirection();
        radius = subject.getSize() * GLRenderGlobals.getRelativeScaling();
        this.drawLineToSurface = subject.getDrawLineToSurface();
        this.dirty = new AtomicBoolean(false);
    }

    @Override
    public void onCrumbColorChanged(Crumb crumb) {
        if (this.color != crumb.getColor()) {
            this.color = crumb.getColor();
            dirty.set(true);
        }
    }

    @Override
    public void onCrumbDrawLineToSurfaceChanged(Crumb crumb) {
        if (this.drawLineToSurface != crumb.getDrawLineToSurface()) {
            this.drawLineToSurface = crumb.getDrawLineToSurface();
            dirty.set(true);
        }
    }

    @Override
    public void onCrumbDirectionChanged(Crumb crumb) {
        if (this.rot != crumb.getDirection()) {
            this.rot = crumb.getDirection();
            dirty.set(true);
        }
    }

    @Override
    public void onCrumbSizeChanged(Crumb crumb) {
        if (this.radius != (crumb.getSize()
                * GLRenderGlobals.getRelativeScaling())) {
            this.radius = crumb.getSize()
                    * GLRenderGlobals.getRelativeScaling();
            dirty.set(true);
        }
    }

    @Override
    public void draw(GLMapView ortho, int renderPass) {
        if (!MathUtils.hasBits(renderPass, this.renderPass))
            return;
        validateCrumb();

        // invoke draw on crumbs renderer
        this.crumbsRenderer.setLollipopsVisible(getLollipopsVisible());
        this.crumbsRenderer.setClampToGroundAtNadir(getClampToGroundAtNadir());
        this.crumbsRenderer.draw(ortho, renderPass);
    }

    @Override
    public void release() {
        super.release();

        // remove from crumb renderer
        if (crumbid != -1) {
            crumbsRenderer.removeCrumb(crumbid);
            crumbid = -1;
            dirty.set(true);
        }
    }

    // XXX - why isn't this using the super implementation?

    @Override
    public void onPointChanged(PointMapItem item) {
        final GeoPoint p = item.getPoint();
        this.context.queueEvent(new Runnable() {
            @Override
            public void run() {
                point = p;

                final double N = point.getLatitude() + .0001; // about 10m
                final double S = point.getLatitude() - .0001;
                final double E = point.getLongitude() + .0001;
                final double W = point.getLongitude() - .0001;

                bounds.set(N, W, S, E);
                // lollipop bottom
                bounds.setMinAltitude(DEFAULT_MIN_ALT);
                // assume if point is above 9000m it is above terrain
                bounds.setMaxAltitude(Math.max(
                        Double.isNaN(point.getAltitude()) ? point.getAltitude()
                                : 0d,
                        DEFAULT_MAX_ALT));

                // IF YOU NEED TO, remove it and re-add it
                // if outside bounds of node
                dispatchOnBoundsChanged();
            }
        });
        if (!p.equals(point)) {
            dirty.set(true);
        }
    }

    @Override
    public void startObserving() {
        Crumb crumb = (Crumb) this.subject;
        super.startObserving();
        crumb.addCrumbColorListener(this);
        crumb.addCrumbDirectionListener(this);
        crumb.addCrumbSizeListener(this);
        crumb.addCrumbDrawLineToSurfaceListener(this);

        dirty.set(true);
    }

    @Override
    public void stopObserving() {
        Crumb crumb = (Crumb) this.subject;
        super.stopObserving();
        crumb.removeCrumbColorListener(this);
        crumb.removeCrumbDirectionListener(this);
        crumb.removeCrumbSizeListener(this);
        crumb.removeCrumbDrawLineToSurfaceListener(this);
    }

    private void validateCrumb() {
        if (dirty.compareAndSet(true, false) || crumbid == -1) {
            this.color = sub.getColor();
            this.rot = sub.getDirection();
            radius = sub.getSize() * GLRenderGlobals.getRelativeScaling();
            this.drawLineToSurface = sub.getDrawLineToSurface();

            crumbid = crumbsRenderer.updateCrumb(crumbid, point, rot, radius,
                    color,
                    drawLineToSurface,
                    altMode == Feature.AltitudeMode.ClampToGround);
        }
    }
}
