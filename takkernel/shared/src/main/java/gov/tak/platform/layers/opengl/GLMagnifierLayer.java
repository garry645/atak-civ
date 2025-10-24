
package gov.tak.platform.layers.opengl;

import android.util.Pair;

import com.atakmap.android.maps.graphics.GLTriangle;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.Globe;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceBuilder;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.control.ElevationSourceControl;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapRenderable;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.util.Visitor;

import java.util.Collection;
import java.util.LinkedList;

import gov.tak.api.engine.map.RenderSurface;
import gov.tak.api.engine.map.RenderSurface2;
import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.layers.MagnifierLayer;

public final class GLMagnifierLayer implements
        MagnifierLayer.OnLocationChangedListener,
        MagnifierLayer.OnScaleChangedListener,
        GLLayer3,
        MapControl {
    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            // MapTargetBubble : Layer
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer instanceof MagnifierLayer)
                return new GLMagnifierLayer(surface, (MagnifierLayer) layer);
            return null;
        }
    };

    /**
     * also refered to as the reticle tool
     */

    public static final String TAG = "GLMapTargetBubble";

    private static final int _STENCIL_CIRCLE_POINT_COUNT = 48;

    private final double circleRadius;

    private final MagnifierLayer subject;

    private boolean initialized;

    private final MapRenderer renderCtx;

    private Rect viewport;
    private GLTriangle.Fan _circle;

    // Renders the map imagery within the target
    private GLMapView renderer;

    // Main GL map view instance
    private final GLMapView parent;

    private float subjectLeft;
    private float subjectTop;
    private float subjectRight;
    private float subjectBottom;
    private float initialFocusX;
    private float initialFocusY;

    public GLMagnifierLayer(final MapRenderer surface,
                            MagnifierLayer subject) {

        this.renderCtx = surface;
        this.parent = (GLMapView) this.renderCtx;
        this.subject = subject;

        final RenderSurface renderSurface = LegacyAdapters.getRenderContext(surface).getRenderSurface();

        this.viewport = subject.getViewport();
        int surfaceWidth = renderSurface instanceof RenderSurface2 ? ((RenderSurface2)renderSurface).getSurfaceWidth() : renderSurface.getWidth();
        int surfaceHeight = renderSurface instanceof RenderSurface2 ? ((RenderSurface2)renderSurface).getSurfaceHeight() : renderSurface.getHeight();
        if(this.viewport != null)
        {
            this.subjectLeft = viewport.left;
            this.subjectTop = surfaceHeight - viewport.top;
            this.subjectRight = viewport.right;
            this.subjectBottom = surfaceHeight - viewport.bottom;
        } else {
            this.subjectLeft = 0;
            this.subjectRight = surfaceWidth-1;
            this.subjectBottom = 0;
            this.subjectTop = surfaceHeight-1;
        }
        circleRadius = (subjectRight-subjectLeft) / 2d;
        this.initialized = false;
    }

    public MapRenderer2 getRenderer() {
        return renderer;
    }

    // GLMapView interface, for backwards API compatibility
    public final <T extends MapControl> boolean visitControl(Layer2 layer,
            Visitor<T> visitor, Class<T> ctrlClazz) {
        final GLMapView r = this.renderer;
        if (r == null)
            return false;
        return r.visitControl(layer, visitor, ctrlClazz);
    }

    protected final void queueEvent(Runnable r) {
        this.renderCtx.queueEvent(r);
    }

    @Override
    public void onMagnifierLocationChanged(final MagnifierLayer bubble, double latitude, double longitude) {
        refreshLookAt();
    }

    @Override
    public void onMagnifierScaleChanged(MagnifierLayer bubble, double scale, boolean scaleIsRelative) {
        refreshLookAt();
    }
    @Override
    public void draw(GLMapView view) {
        this.draw(view, -1);
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if (!MathUtils.hasBits(renderPass, getRenderPass()))
            return;

        if (!this.initialized) {
            this.renderer = new GLTargetBubbleView(parent);
            this.renderer.start();
            refreshLookAtGL();
            if (this.viewport != null) {
                /* create the stencil buffer circle */
                _circle = new GLTriangle.Fan(2, _STENCIL_CIRCLE_POINT_COUNT);
                double angleStep = 2 * Math.PI / _STENCIL_CIRCLE_POINT_COUNT;
                for (int i = 0; i < _STENCIL_CIRCLE_POINT_COUNT; ++i) {
                    double angle = i * angleStep;
                    double cx = circleRadius * Math.cos(angle);
                    double cy = circleRadius * Math.sin(angle);
                    _circle.setX(i, (float) cx);
                    _circle.setY(i, (float) cy);
                }
            }

            this.initialFocusX = view.currentPass.focusx;
            this.initialFocusY = view.currentPass.focusy;
            this.initialized = true;
        }

        view.scratch.depth.save();
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (_circle != null) {
            GLES20FixedPipeline
                    .glClear(GLES20FixedPipeline.GL_STENCIL_BUFFER_BIT);
            GLES20FixedPipeline.glStencilMask(0xFFFFFFFF);
            GLES20FixedPipeline.glStencilFunc(GLES20FixedPipeline.GL_ALWAYS,
                    0x1,
                    0x1);
            GLES20FixedPipeline.glStencilOp(GLES20FixedPipeline.GL_KEEP,
                    GLES20FixedPipeline.GL_KEEP,
                    GLES20FixedPipeline.GL_INCR);
            GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_STENCIL_TEST);
            GLES20FixedPipeline.glColorMask(false, false, false, false);

            final float right = subjectRight;
            final float left = subjectLeft;
            final float bottom = subjectBottom;
            final float top = subjectTop;

            final float width = right - left;
            final float height = top - bottom;

            final float focusOffsetX = view.currentPass.focusx - initialFocusX;
            final float focusOffsetY = view.currentPass.focusy - initialFocusY;

            GLES20FixedPipeline.glPushMatrix();
            GLES20FixedPipeline.glTranslatef(focusOffsetX + left + width / 2,
                    focusOffsetY + bottom + height / 2,
                    0);
            GLES20FixedPipeline.glColor4f(1f, 0f, 0f, 1f);
            _circle.draw();
            GLES20FixedPipeline.glPopMatrix();

            GLES20FixedPipeline.glStencilMask(0xFFFFFFFF);
            GLES20FixedPipeline.glStencilFunc(GLES20FixedPipeline.GL_EQUAL,
                    0x1,
                    0x1);
            GLES20FixedPipeline.glStencilOp(GLES20FixedPipeline.GL_KEEP,
                    GLES20FixedPipeline.GL_KEEP,
                    GLES20FixedPipeline.GL_KEEP);
            GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_STENCIL_TEST);
            GLES20FixedPipeline.glColorMask(true, true, true, true);
        }
        this.renderer.render();
        if (_circle != null) {
            GLES20FixedPipeline.glDisable(GLES20FixedPipeline.GL_STENCIL_TEST);
        }

        view.scratch.depth.restore();
        GLES30.glClear(GLES30.GL_STENCIL_BUFFER_BIT);
    }

    @Override
    public void release() {
        if (this.renderer != null) {
            this.renderer.stop();
            Collection<GLMapRenderable> renderables = new LinkedList<>();
            this.renderer.getMapRenderables(renderables);
            for (GLMapRenderable r : renderables) {
                if (r instanceof GLLayer3)
                    ((GLLayer3) r).stop();
                r.release();
            }
            this.renderer.release();
            this.renderer.dispose();
            this.renderer = null;
        }

        _circle = null;
        this.initialized = false;
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_UI;
    }

    /**************************************************************************/
    // GL Layer

    @Override
    public Layer getSubject() {
        return this.subject;
    }

    @Override
    public void start() {
        this.subject.addOnLocationChangedListener(this);
        this.subject.addOnScaleChangedListener(this);
        this.renderCtx.registerControl(this.subject, this);
    }

    @Override
    public void stop() {
        this.renderCtx.unregisterControl(this.subject, this);
        this.subject.removeOnLocationChangedListener(this);
        this.subject.removeOnScaleChangedListener(this);
    }

    private void refreshLookAt() {
        renderCtx.queueEvent(new Runnable() {
            @Override
            public void run() {
                refreshLookAtGL();
            }
        });
    }

    private void refreshLookAtGL() {
        if (this.renderer != null) {
            double resolution =
                subject.isMagnifierScaleRelative() ?
                    renderer.currentScene.drawMapResolution / subject.getMagnifierScale() :
                    Globe.getMapResolution(
                        renderer.getRenderSurface().getDpi(),
                        subject.getMagnifierScale());
            renderer.lookAt(new GeoPoint(subject.getLatitude(),
                    subject.getLongitude()),
                    resolution, 0d, 0d, false);
        }
    }

    /**************************************************************************/

    private final class GLTargetBubbleView extends GLMapView {

        GLMapView parent;

        GLTargetBubbleView(GLMapView parent) {
            super(LegacyAdapters.getRenderContext(renderCtx),
                    subject.getGlobe(),
                    parent.currentPass.left,
                    parent.currentPass.bottom,
                    parent.currentPass.right,
                    parent.currentPass.top,
                    true);
            this.parent = parent;
            setFocusPointOffset(parent.getFocusPointOffsetX(),
                    parent.getFocusPointOffsetY());

            // disable terrain for the magnifier layer with ortho camera
            final ElevationSourceControl ctrl = this.getControl(ElevationSourceControl.class);
            if(ctrl != null)
                ctrl.setElevationSource(ElevationSourceBuilder.multiplex("empty", new ElevationSource[0]));
        }
    }
}
