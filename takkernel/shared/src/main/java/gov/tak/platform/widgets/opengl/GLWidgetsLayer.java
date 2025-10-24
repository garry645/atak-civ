package gov.tak.platform.widgets.opengl;
import android.util.Pair;

import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.RenderContext;
import com.atakmap.map.RenderSurface;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.RenderSurface2;
import gov.tak.api.widgets.WidgetsLayer;
import gov.tak.api.widgets.opengl.IGLWidget;
import gov.tak.platform.marshal.MarshalManager;

public final class GLWidgetsLayer implements GLLayer3
{
    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            // WidgetsLayer : Layer
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer instanceof WidgetsLayer)
                return new GLWidgetsLayer(surface,
                        (WidgetsLayer) layer);
            return null;
        }
    };

    static
    {
        // register all off-the-shelf GL widget SPIs
        GLWidgetFactory.registerSpi(GLRadialButtonWidget.SPI);
        GLWidgetFactory.registerSpi(GLScaleWidget.SPI);
        GLWidgetFactory.registerSpi(GLLinearLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLTextWidget.SPI);
        GLWidgetFactory.registerSpi(GLMarkerIconWidget.SPI);
        GLWidgetFactory.registerSpi(GLCenterBeadWidget.SPI);
        GLWidgetFactory.registerSpi(GLDrawableWidget.SPI);
        GLWidgetFactory.registerSpi(GLRootLayoutWidget.SPI);
    }

    private final RenderContext renderContext;
    private final WidgetsLayer subject;

    private IGLWidget impl;

    /** @deprecated use SPI */
    @Deprecated
    @DeprecatedApi(since = "4.9", forRemoval = true, removeAt = "4.12")
    public GLWidgetsLayer(MapRenderer surface, WidgetsLayer layer) {
        this(LegacyAdapters.getRenderContext(surface), layer);
    }

    public GLWidgetsLayer(RenderContext surface, WidgetsLayer layer) {
        this.renderContext = surface;
        this.subject = layer;

        this.impl = null;
    }

    @Override
    public final void draw(GLMapView view) {
        this.draw(view, GLMapView.RENDER_PASS_UI);
    }

    @Override
    public void release() {
        if (impl != null)
            this.impl.releaseWidget();
        this.impl = null;
    }

    @Override
    public Layer getSubject() {
        return this.subject;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_UI;
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_UI)) {
            if (this.impl == null) {
                gov.tak.api.engine.map.MapRenderer wrapper = LegacyAdapters
                        .adapt(view);
                this.impl = GLWidgetFactory.create(wrapper, this.subject.getRoot());
                this.impl.start();
            }
            GLES20FixedPipeline.glDepthFunc(GLES20FixedPipeline.GL_ALWAYS);
            GLES20FixedPipeline.glPushMatrix();

            RenderSurface surface = renderContext.getRenderSurface();
            float height = (surface instanceof RenderSurface2 ? ((RenderSurface2)surface).getSurfaceHeight() : surface.getHeight()) - 1;

            GLES20FixedPipeline.glTranslatef(0,
                    height,
                    0f);

            GLWidget.DrawState drawState = drawStateFromFixedPipeline(view);
            this.impl.drawWidget(drawState);
            GLES20FixedPipeline.glPopMatrix();
            GLES20FixedPipeline.glDepthFunc(GLES20FixedPipeline.GL_LEQUAL);
        }
    }

    private static gov.tak.platform.widgets.opengl.GLWidget.DrawState drawStateFromFixedPipeline(
            MapRenderer2 mapSceneRenderer2) {
        GLWidget.DrawState drawState = new GLWidget.DrawState(
                MarshalManager.marshal(
                        mapSceneRenderer2.getMapSceneModel(true,
                                mapSceneRenderer2.getDisplayOrigin()),
                        com.atakmap.map.MapSceneModel.class,
                        gov.tak.api.engine.map.MapSceneModel.class));
        drawState.projectionMatrix = new float[16];
        drawState.modelMatrix = new float[16];
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION,
                drawState.projectionMatrix, 0);
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_MODELVIEW,
                drawState.modelMatrix, 0);
        return drawState;
    }
}
