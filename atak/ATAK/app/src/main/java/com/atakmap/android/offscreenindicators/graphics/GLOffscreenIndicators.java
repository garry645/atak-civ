
package com.atakmap.android.offscreenindicators.graphics;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES30;
import android.os.SystemClock;
import android.util.Pair;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.graphics.AbstractGLMapItem2;
import com.atakmap.android.maps.graphics.GLImageCache;
import com.atakmap.android.offscreenindicators.OffscreenIndicatorController;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;

import com.atakmap.lang.Unsafe;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.layer.opengl.GLAsynchronousLayer2;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.Rectangle;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLRenderBatch2;
import com.atakmap.util.ConfigOptions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.tak.api.engine.map.IMapRendererEnums;

public class GLOffscreenIndicators extends
        GLAsynchronousLayer2<OffscreenIndicatorParams> implements
        OffscreenIndicatorController.OnOffscreenIndicatorsThresholdListener,
        OffscreenIndicatorController.OnItemsChangedListener,
        Marker.OnIconChangedListener, PointMapItem.OnPointChangedListener,
        HitTestControl {

    public final static GLLayerSpi2 SPI2 = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            // OffscreenIndicatorController : Layer
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer instanceof OffscreenIndicatorController)
                return GLLayerFactory.adapt(new GLOffscreenIndicators(surface,
                        (OffscreenIndicatorController) layer));
            return null;
        }
    };

    private static final float LINE_WIDTH = (float) Math.max(
            Math.ceil(1f * GLRenderGlobals.getRelativeScaling()), 1.0f);
    private static final float OUTLINE_WIDTH = LINE_WIDTH + 2;

    private final MapRenderer renderContext;
    private final OffscreenIndicatorController controller;
    private double threshold;
    private double timeout;

    private final static int OFFSCREEN_INDICATOR_DRAWRATE = 1000;
    private long lastdrawn = 0;

    /**
     * Current _set_ of markers eligible for `draw`. Implicitly z-ordered by virtue of copy from
     * pending data payload {@link OffscreenIndicatorParams#markers}.
     */
    private final ArrayList<Marker> observed;
    private final LinkedList<GLMapRenderable2> renderable;

    private boolean debugHitBoxes = false;

    private final AtomicBoolean _invalidatePending = new AtomicBoolean(false);

    public GLOffscreenIndicators(MapRenderer surface,
            OffscreenIndicatorController controller) {
        super(surface, controller);

        this.renderContext = surface;
        this.controller = controller;

        this.threshold = this.controller.getThreshold();
        this.timeout = this.controller.getTimeout();

        this.observed = new ArrayList<>();
        this.renderable = new LinkedList<>();
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_SPRITES;
    }

    @Override
    public void draw(GLMapView view, int pass) {
        GLES20FixedPipeline.glDepthFunc(GLES20FixedPipeline.GL_ALWAYS);
        super.draw(view, pass);
        GLES20FixedPipeline.glDepthFunc(GLES20FixedPipeline.GL_LEQUAL);

        if (debugHitBoxes)
            debugHitBoxes(view);
    }

    private void debugHitBoxes(GLMapView view) {
        Collection<Marker> markers;
        synchronized (this) {
            markers = new ArrayList<>(this.observed);
        }

        view.scratch.geo.set(view.currentPass.drawLat,
                view.currentPass.drawLng);
        com.atakmap.coremap.maps.coords.GeoPointMetaData geo = new com.atakmap.coremap.maps.coords.GeoPointMetaData();
        geo.set(view.scratch.geo);

        for (Marker m : markers) {
            view.currentPass.scene.forward(m.getPoint(), view.scratch.pointF);
            if (!shouldDraw(m, view.scratch.pointF.x, view.scratch.pointF.y,
                    view.currentPass.scene.width, view.currentPass.scene.height,
                    geo.get(),
                    controller.getThreshold(), (long) timeout))
                continue;

            view.scratch.rectF.left = OffscreenIndicatorController.HALO_BORDER_SIZE;
            view.scratch.rectF.top = OffscreenIndicatorController.HALO_BORDER_SIZE;
            view.scratch.rectF.right = view.currentPass.scene.width
                    - OffscreenIndicatorController.HALO_BORDER_SIZE;
            view.scratch.rectF.bottom = view.currentPass.scene.height
                    - OffscreenIndicatorController.HALO_BORDER_SIZE;
            computeHaloPoint(view.scratch.pointF, view.scratch.rectF);

            android.graphics.RectF _hitRect = view.scratch.rectF;
            _hitRect.left = view.scratch.pointF.x
                    - OffscreenIndicatorController.HALO_BORDER_SIZE / 2;
            _hitRect.top = view.scratch.pointF.y
                    + OffscreenIndicatorController.HALO_BORDER_SIZE / 2;
            _hitRect.right = view.scratch.pointF.x
                    + OffscreenIndicatorController.HALO_BORDER_SIZE / 2;
            _hitRect.bottom = view.scratch.pointF.y
                    - OffscreenIndicatorController.HALO_BORDER_SIZE / 2;
            java.nio.FloatBuffer hitrect = Unsafe.allocateDirect(8,
                    java.nio.FloatBuffer.class);
            hitrect.put(0, _hitRect.left);
            hitrect.put(1, _hitRect.bottom);
            hitrect.put(2, _hitRect.left);
            hitrect.put(3, _hitRect.top);
            hitrect.put(4, _hitRect.right);
            hitrect.put(5, _hitRect.top);
            hitrect.put(6, _hitRect.right);
            hitrect.put(7, _hitRect.bottom);
            GLES20FixedPipeline.glColor4f(0.f, 0.f, 1.f, 1.f);
            GLES20FixedPipeline.glLineWidth(2f);
            GLES20FixedPipeline
                    .glEnableClientState(
                            GLES20FixedPipeline.GL_VERTEX_ARRAY);
            GLES20FixedPipeline.glVertexPointer(2,
                    GLES20FixedPipeline.GL_FLOAT, 0, hitrect);
            GLES20FixedPipeline
                    .glDrawArrays(GLES20FixedPipeline.GL_LINE_LOOP, 0, 4);
            GLES20FixedPipeline
                    .glDisableClientState(
                            GLES20FixedPipeline.GL_VERTEX_ARRAY);
            Unsafe.free(hitrect);
        }
    }

    @Override
    protected void initImpl(GLMapView view) {
        super.initImpl(view);

        this.controller.addOnOffscreenIndicatorsThresholdListener(this);
        this.controller.addOnItemsChangedListener(this);
    }

    @Override
    public synchronized void start() {
        super.start();
        this.renderContext.registerControl(
                (OffscreenIndicatorController) this.subject, this);
    }

    @Override
    public synchronized void stop() {
        this.renderContext.unregisterControl(
                (OffscreenIndicatorController) this.subject, this);
        super.stop();
    }

    @Override
    public void release() {
        this.controller.removeOnOffscreenIndicatorsThresholdListener(this);
        this.controller.removeOnItemsChangedListener(this);

        super.release();
    }

    @Override
    protected Collection<GLMapRenderable2> getRenderList() {
        return this.renderable;
    }

    @Override
    protected void resetPendingData(OffscreenIndicatorParams pendingData) {
        //pendingData.markers.clear();
    }

    @Override
    protected void releasePendingData(OffscreenIndicatorParams pendingData) {
        //pendingData.markers.clear();
    }

    @Override
    protected OffscreenIndicatorParams createPendingData() {
        return new OffscreenIndicatorParams();
    }

    @Override
    protected String getBackgroundThreadName() {
        return "Offscreen GL worker@" + Integer.toString(this.hashCode(), 16);
    }

    @Override
    protected boolean updateRenderList(
            ViewState state,
            OffscreenIndicatorParams pendingData) {
        Set<Marker> unobserved = new HashSet<>(this.observed);
        this.observed.clear();

        for (Marker m : pendingData.markers) {
            if (!unobserved.remove(m)) {
                m.addOnPointChangedListener(this);
                m.addOnIconChangedListener(this);
            }

            this.observed.add(m);
        }

        for (Marker m : unobserved) {
            m.removeOnPointChangedListener(this);
            m.removeOnIconChangedListener(this);
        }

        if (!this.observed.isEmpty()) {
            if (this.renderable.isEmpty())
                this.renderable.add(new GLBatchRenderer());
        } else if (!this.renderable.isEmpty()) {
            final Collection<GLMapRenderable2> released = new LinkedList<>(
                    this.renderable);
            this.renderable.clear();

            this.renderContext.queueEvent(new Runnable() {
                @Override
                public void run() {
                    for (GLMapRenderable2 renderable : released)
                        renderable.release();
                    released.clear();
                }
            });
        }

        return true;
    }

    @Override
    protected void query(ViewState state, OffscreenIndicatorParams result) {
        _invalidatePending.set(false);

        // NOTE: setting the value for `debugHitBoxes` here is not strictly thread-safe, but it will
        // approximate per intent and there are no execution risks associated with the concurrency.
        debugHitBoxes = ConfigOptions.getOption("debug-hit-boxes", 0) != 0;

        final long curr = SystemClock.elapsedRealtime();
        if (curr - lastdrawn < OFFSCREEN_INDICATOR_DRAWRATE) {
            // skip drawing 
            return;
        }
        lastdrawn = curr;
        result.markers.clear();

        this.controller.getMarkers(result.markers);

        final GeoPoint viewCenter = new GeoPoint(state.drawLat, state.drawLng);

        Iterator<Marker> iter = result.markers.iterator();
        Marker m;

        if (this.checkQueryThreadAbort())
            return;

        while (iter.hasNext()) {
            if (this.checkQueryThreadAbort())
                break;

            m = iter.next();
            if (!shouldDraw(m, viewCenter, threshold, (long) timeout))
                iter.remove();
        }

        if (this.checkQueryThreadAbort())
            result.markers.clear();
    }

    @Override
    protected void invalidateNoSync() {
        if (_invalidatePending.compareAndSet(false, true))
            super.invalidateNoSync(null);
    }

    @Override
    protected void invalidateNoSync(Envelope region) {
        if (_invalidatePending.compareAndSet(false, true))
            super.invalidateNoSync(null);
    }

    /**************************************************************************/
    // Hit Test Control

    @Override
    public void hitTest(MapRenderer3 renderer, HitTestQueryParameters params,
            Collection<HitTestResult> results) {
        // note that `candidates` will be _render_ z-ordered
        ArrayList<Marker> candidates;
        synchronized (this) {
            candidates = new ArrayList<>(this.observed);
        }

        // NOTE: `HitTestQueryParams` screen-space is LL origin
        final MapSceneModel _mapView = renderer.getMapSceneModel(true,
                IMapRendererEnums.DisplayOrigin.LowerLeft);
        GeoPoint focusGeo = GeoPoint.createMutable();
        _mapView.mapProjection.inverse(_mapView.camera.target, focusGeo);

        final float haloBorderSize = this.controller
                .getHaloBorderSize();
        final float haloIconSize = this.controller
                .getHaloIconSize();
        final float haloBitmapSizeSquared = haloIconSize * haloIconSize;

        // Create a rectangle around the border of the screen that all icons will fall on
        final RectF innerRect = new RectF(haloBorderSize,
                haloBorderSize,
                _mapView.width - haloBorderSize,
                _mapView.height - haloBorderSize);

        final int limit = candidates.size();
        PointF screen = null;
        for (int i = limit - 1; i >= 0; i--) {
            final Marker m = candidates.get(i);
            screen = _mapView.forward(m.getPoint(), screen);
            if (!shouldDraw(m, screen.x, screen.y,
                    _mapView.width, _mapView.height, focusGeo,
                    threshold, (long) timeout))
                continue;

            computeHaloPoint(screen, innerRect);

            final float dx = (screen.x - params.point.x);
            final float dy = (screen.y - params.point.y);

            if ((dx * dx + dy * dy) < haloBitmapSizeSquared) {
                results.add(new HitTestResult(m));
                break;
            }
        }
    }

    /**************************************************************************/
    // On Offscreen Indicators Threshold Listener

    @Override
    public void onOffscreenIndicatorsThresholdChanged(
            OffscreenIndicatorController controller) {
        final double t = controller.getThreshold();
        final double to = controller.getTimeout();

        this.renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                GLOffscreenIndicators.this.threshold = t;
                GLOffscreenIndicators.this.timeout = to;
                GLOffscreenIndicators.this.invalidateNoSync();
            }
        });

    }

    /**************************************************************************/
    // On Point Changed Listener

    @Override
    public void onPointChanged(PointMapItem item) {
        // we could perform our threshold culling here, but in environments
        // where many items' points are changing with high frequency it is
        // better to mark invalid and rebuild the list at the highest frequency
        // that we can manage.
        this.invalidateNoSync();
    }

    /**************************************************************************/
    // On Icon Changed Listener

    @Override
    public void onIconChanged(Marker marker) {
        this.invalidateNoSync();
    }

    /**************************************************************************/
    // On Items Changed Listener

    @Override
    public void onItemsChanged(OffscreenIndicatorController controller) {
        this.invalidateNoSync();
    }

    /**************************************************************************/

    /**
     * Given a "halo" (inset boundary around screen), update the point location such that it is
     * located on the "halo" boundary closest to the original location.
     *
     * @param screenPoint   A point, in screenspace
     * @param haloBorder    The halo boundary, in screenspace. Note that {@code haloRect.top} should
     *                      correspond to the <I>least</I> {@code y}-value for the boundary and
     *                      {@code haloRect.top} should correspond to the <I>greatest</I>
     *                      {@code y}-value for the boundary, regardless of the coordinate system's
     *                      origin.
     */
    public static void computeHaloPoint(PointF screenPoint, RectF haloBorder) {
        screenPoint.x = Math.min(screenPoint.x, haloBorder.right);
        screenPoint.x = Math.max(screenPoint.x, haloBorder.left);
        screenPoint.y = Math.min(screenPoint.y, haloBorder.bottom);
        screenPoint.y = Math.max(screenPoint.y, haloBorder.top);
    }

    public static boolean shouldDraw(Marker m, GeoPoint focusPoint,
            double distanceThreshold, long timeout) {
        return shouldDraw(m, Float.NaN, Float.NaN, 0f, 0f, focusPoint,
                distanceThreshold, timeout);
    }

    public static boolean shouldDraw(Marker m, float x, float y,
            float screenWidth, float screenHeight, GeoPoint focusPoint,
            double distanceThreshold) {
        return shouldDraw(m, x, y, screenWidth, screenHeight, focusPoint,
                distanceThreshold, -1);
    }

    /**
     * Returns {@code true} if an offscreen indicator should be drawn for the specified
     * {@code Marker}, {@code false} otherwise.
     *
     * <P>Note: the <I>timeout</I> related checks inspect metadata on the marker, making this test
     * more expensive. If the test is being performed in a context where the marker previously
     * passed a call to {@code shouldDraw} with a valid {@code timeout} value, the more expensive
     * test may be skipped.
     *
     * @param m                 The marker
     * @param x                 The screenspace x location of the marker, respecting origin
     * @param y                 The screenspace y location of the marker, respecting origin
     * @param screenWidth       The width of the screen
     * @param screenHeight      The height of the screen
     * @param focusPoint        The current focus point of the scene
     * @param distanceThreshold The distance threshold between the {@code m} and {@code focusPoint}
     *                          past which the marker is no longer considered eligible as an
     *                          offscreen indicator
     * @param timeout           The offscreen indicator timeout, in milliseconds. Less than zero to
     *                          skip timeout tests.
     * @return
     */
    public static boolean shouldDraw(Marker m, float x, float y,
            float screenWidth, float screenHeight, GeoPoint focusPoint,
            double distanceThreshold, long timeout) {
        // *** logic refactor from `GLBatchRenderer.draw` ***

        if (m.getMetaBoolean("disable_offscreen_indicator", false)) {
            return false;
        }

        // ignore if marker is within viewport
        if (!Float.isNaN(x) && !Float.isNaN(y)
                && Rectangle.contains(0, 0, screenWidth, screenHeight, x, y))
            return false;

        // *** logic refactor from `query` ***
        if (timeout > 0) {
            if (MapView.getMapView().getSelfMarker() == null)
                return false;

            final String teamColor = MapView.getMapView().getSelfMarker()
                    .getMetaString("team", "Cyan");

            String mTeam = m.getMetaString("team", "");
            long interest = m.getMetaLong("offscreen_interest", -1);
            double intTime = timeout + interest;
            long clock = SystemClock.elapsedRealtime();
            if (!mTeam.equals(teamColor) && (timeout > 0d && intTime < clock))
                return false;
        }

        double distance = MapItem.computeDistance(m, focusPoint);
        if (Double.isNaN(distance) || distance > distanceThreshold)
            return false;

        return true;
    }

    /**************************************************************************/

    private class GLBatchRenderer implements GLMapRenderable2 {

        private GLRenderBatch2 impl;
        private FloatBuffer arcVerts;
        private long arcVertsPointer;
        private final Matrix IDENTITY = Matrix.getIdentity();
        private final Matrix xform = Matrix.getIdentity();

        @Override
        public void draw(GLMapView view, int renderPass) {
            if ((renderPass & GLMapView.RENDER_PASS_SPRITES) == 0)
                return;

            String iconUri = null;
            int iconColor = 0;
            GLImageCache.Entry entry;

            final float haloBorderSize = GLOffscreenIndicators.this.controller
                    .getHaloBorderSize();
            final float haloIconSize = GLOffscreenIndicators.this.controller
                    .getHaloIconSize();

            // Create a rectangle around the border of the screen that all icons will fall on
            final RectF innerRect = new RectF(
                    view.currentPass.left + haloBorderSize,
                    view.currentPass.bottom + haloBorderSize,
                    view.currentPass.right - haloBorderSize,
                    view.currentPass.top - haloBorderSize);

            PointF screenPoint;
            PointF screenCoordsHaloIcon = new PointF();
            PointF markerUL = new PointF();
            PointF markerUR = new PointF();
            PointF markerLR = new PointF();
            PointF markerLL = new PointF();

            final int lineCount = 8;
            final int vertCount = lineCount + 1;

            // instantiate the vertex coords if necessary
            if (this.arcVerts == null) {
                ByteBuffer buf = com.atakmap.lang.Unsafe
                        .allocateDirect(vertCount * 2 * 4);
                buf.order(ByteOrder.nativeOrder());
                this.arcVerts = buf.asFloatBuffer();
                this.arcVertsPointer = Unsafe.getBufferPointer(this.arcVerts);
            }

            if (this.impl == null)
                this.impl = new GLRenderBatch2(1024);
            GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION,
                    view.scratch.matrixF, 0);
            this.impl.setMatrix(GLES20FixedPipeline.GL_PROJECTION,
                    view.scratch.matrixF, 0);
            GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_MODELVIEW,
                    view.scratch.matrixF, 0);
            this.impl.setMatrix(GLES20FixedPipeline.GL_MODELVIEW,
                    view.scratch.matrixF, 0);
            this.impl.begin();

            GeoPoint mgp = GeoPoint.createMutable();
            for (Marker m : GLOffscreenIndicators.this.observed) {

                mgp.set(m.getPoint());
                if (view.continuousScrollEnabled) {
                    // Test to see if point is closer by going over IDL
                    double lng = mgp.getLongitude();
                    double lng2 = lng + (lng < 0 ? 360 : -360);
                    if (Math.abs(view.currentPass.drawLng - lng2) < Math
                            .abs(view.currentPass.drawLng - lng))
                        mgp.set(mgp.getLatitude(), lng2);
                }

                AbstractGLMapItem2.forward(view, mgp, view.scratch.pointD);
                view.scratch.pointF.x = (float) view.scratch.pointD.x;
                view.scratch.pointF.y = (float) view.scratch.pointD.y;
                screenPoint = view.scratch.pointF;

                // check if the marker should be drawn. note that all markers have been screened at
                // this point for timeout via the call to `shouldDraw` in the `query` method, so we
                // will skip that portion of the test on the render thread. any subsequent update to
                // the state (change in markers list, movement of the map, etc.) will force a new
                // query and run the timeout test again
                view.scratch.geo.set(view.currentPass.drawLat,
                        view.currentPass.drawLng);
                if (!shouldDraw(m, screenPoint.x, screenPoint.y,
                        view.currentPass.scene.width,
                        view.currentPass.scene.height, view.scratch.geo,
                        threshold))
                    continue;

                screenCoordsHaloIcon.x = screenPoint.x;
                screenCoordsHaloIcon.y = screenPoint.y;

                computeHaloPoint(screenCoordsHaloIcon, innerRect);

                // add halo
                final float distance = (float) MathUtils.distance(screenPoint.x,
                        screenPoint.y,
                        screenCoordsHaloIcon.x, screenCoordsHaloIcon.y);

                //item.arc.setRadius(distance);
                float radius = distance;

                // Added by Tim: This math here limits the arc to the exact area displayed for
                // objects off any side, but does overdraw some on the corners (but not nearly as
                // much as the full circle used to)
                float arclen;
                float ang = (float) Math.toDegrees(Math.atan2(screenPoint.y
                        - screenCoordsHaloIcon.y,
                        screenCoordsHaloIcon.x - screenPoint.x));
                arclen = (float) Math.toDegrees(Math
                        .acos((distance - haloBorderSize) / distance));

                //item.arc.setOffsetAngle(ang - arclen);
                final double offsetAngle = (ang - arclen);
                //item.arc.setCentralAngle(arclen * 2);
                final double centralAngle = (arclen * 2);
                // End Tim code
                //item.arc.setPoint(screenPoint.x, screenPoint.y);
                //item.arc.setStrokeColor(setArcColor(item));
                final int arcColor = GLOffscreenIndicators.this.controller
                        .getArcColor(m);

                final double angle = Math.toRadians(offsetAngle);
                final double step = Math.toRadians(centralAngle / lineCount);

                for (int i = 0; i < vertCount; i++) {
                    float px = radius * (float) Math.cos(angle + (i * step));
                    float py = radius * (float) Math.sin(angle + (i * step));
                    Unsafe.setFloats(this.arcVertsPointer + (i * 8),
                            screenPoint.x + px,
                            screenPoint.y - py);
                }

                // outline
                this.impl.setLineWidth(OUTLINE_WIDTH);
                this.impl.batch(
                        0,
                        GLES30.GL_LINE_STRIP,
                        2,
                        8, this.arcVerts,
                        0, null,
                        0.0f, 0.0f, 0.0f, 1.0f);

                // arc
                this.impl.setLineWidth(LINE_WIDTH);
                this.impl.batch(
                        0,
                        GLES30.GL_LINE_STRIP,
                        2,
                        8, this.arcVerts,
                        0, null,
                        Color.red(arcColor) / 255f,
                        Color.green(arcColor) / 255f,
                        Color.blue(arcColor) / 255f,
                        Color.alpha(arcColor) / 255f);

                // reset entry
                entry = null;

                // add icon
                Icon icon = m.getIcon();
                if (icon != null) {
                    iconUri = m.getIcon().getImageUri(m.getState());
                    iconColor = m.getIcon().getColor(m.getState());
                }

                // Icon override meta string
                iconUri = m.getMetaString("offscreen_icon_uri", iconUri);
                iconColor = m.getMetaInteger("offscreen_icon_color", iconColor);

                // Load from bitmap
                if (iconUri != null) {
                    GLImageCache imageCache = GLRenderGlobals.get(view)
                            .getImageCache();
                    entry = imageCache.fetchAndRetain(iconUri, true);
                }

                if (entry != null && entry.getTextureId() != 0) {
                    markerUL.x = screenCoordsHaloIcon.x - haloIconSize / 2;
                    markerUL.y = screenCoordsHaloIcon.y + haloIconSize / 2;
                    markerUR.x = screenCoordsHaloIcon.x + haloIconSize / 2;
                    markerUR.y = screenCoordsHaloIcon.y + haloIconSize / 2;
                    markerLR.x = screenCoordsHaloIcon.x + haloIconSize / 2;
                    markerLR.y = screenCoordsHaloIcon.y - haloIconSize / 2;
                    markerLL.x = screenCoordsHaloIcon.x - haloIconSize / 2;
                    markerLL.y = screenCoordsHaloIcon.y - haloIconSize / 2;

                    // rotation
                    if ((m.getStyle()
                            & Marker.STYLE_ROTATE_HEADING_MASK) == Marker.STYLE_ROTATE_HEADING_MASK) {
                        xform.set(IDENTITY);

                        xform.translate(
                                screenCoordsHaloIcon.x, screenCoordsHaloIcon.y,
                                0);
                        xform.rotate(Math
                                .toRadians(view.currentPass.drawRotation
                                        - m.getTrackHeading()));
                        xform.translate(
                                -screenCoordsHaloIcon.x,
                                -screenCoordsHaloIcon.y, 0);

                        view.scratch.pointD.x = markerUL.x;
                        view.scratch.pointD.y = markerUL.y;
                        xform.transform(view.scratch.pointD,
                                view.scratch.pointD);
                        markerUL.x = (float) view.scratch.pointD.x;
                        markerUL.y = (float) view.scratch.pointD.y;

                        view.scratch.pointD.x = markerUR.x;
                        view.scratch.pointD.y = markerUR.y;
                        xform.transform(view.scratch.pointD,
                                view.scratch.pointD);
                        markerUR.x = (float) view.scratch.pointD.x;
                        markerUR.y = (float) view.scratch.pointD.y;

                        view.scratch.pointD.x = markerLR.x;
                        view.scratch.pointD.y = markerLR.y;
                        xform.transform(view.scratch.pointD,
                                view.scratch.pointD);
                        markerLR.x = (float) view.scratch.pointD.x;
                        markerLR.y = (float) view.scratch.pointD.y;

                        view.scratch.pointD.x = markerLL.x;
                        view.scratch.pointD.y = markerLL.y;
                        xform.transform(view.scratch.pointD,
                                view.scratch.pointD);
                        markerLL.x = (float) view.scratch.pointD.x;
                        markerLL.y = (float) view.scratch.pointD.y;
                    }

                    this.impl.batch(
                            entry.getTextureId(),
                            markerUL.x,
                            markerUL.y,
                            markerUR.x,
                            markerUR.y,
                            markerLR.x,
                            markerLR.y,
                            markerLL.x,
                            markerLL.y,
                            (float) entry.getImageTextureX()
                                    / (float) entry.getTextureWidth(),
                            (float) (entry.getImageTextureY() + entry
                                    .getImageTextureHeight())
                                    / (float) entry.getTextureHeight(),
                            (float) (entry.getImageTextureX() + entry
                                    .getImageTextureWidth())
                                    / (float) entry.getTextureWidth(),
                            (float) (entry.getImageTextureY() + entry
                                    .getImageTextureHeight())
                                    / (float) entry.getTextureHeight(),
                            (float) (entry.getImageTextureX() + entry
                                    .getImageTextureWidth())
                                    / (float) entry.getTextureWidth(),
                            (float) entry.getImageTextureY()
                                    / (float) entry.getTextureHeight(),
                            (float) entry.getImageTextureX()
                                    / (float) entry.getTextureWidth(),
                            (float) entry.getImageTextureY()
                                    / (float) entry.getTextureHeight(),
                            Color.red(iconColor) / 255f,
                            Color.green(iconColor) / 255f,
                            Color.blue(iconColor) / 255f,
                            Color.alpha(iconColor) / 255f);
                }
            }
            this.impl.end();

        }

        @Override
        public void release() {
            if (this.impl != null)
                this.impl = null;
            if (this.arcVerts != null) {
                this.arcVerts = null;
                this.arcVertsPointer = 0L;
            }
        }

        @Override
        public int getRenderPass() {
            return GLMapView.RENDER_PASS_SPRITES;
        }
    }
}
