package gov.tak.platform.widgets.opengl;

import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.RenderSurface;
import gov.tak.api.engine.map.RenderSurface2;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.IRootLayoutWidget;
import gov.tak.api.widgets.opengl.IGLWidgetSpi;

/**
 * Specialization of {@link GLLayoutWidget} that automatically resizes a {@link IRootLayoutWidget}
 * to match the render surface when it is the root (does not have a parent).
 */
public final class GLRootLayoutWidget extends GLLayoutWidget implements RenderSurface.OnSizeChangedListener
{
    public final static IGLWidgetSpi SPI = new IGLWidgetSpi()
    {
        @Override
        public int getPriority()
        {
            // always before base
            return GLLayoutWidget.SPI.getPriority() + 1;
        }

        @Override
        public GLWidget create(MapRenderer renderContext, IMapWidget subject)
        {
            if(!(subject instanceof IRootLayoutWidget))
                return null;
            IRootLayoutWidget rootLayout = (IRootLayoutWidget) subject;
            if(rootLayout.getParent()  != null)
                return null;
            return new GLRootLayoutWidget(rootLayout, renderContext);
        }
    };

    public GLRootLayoutWidget(IRootLayoutWidget subject, MapRenderer renderer) {
        super(subject, renderer);
    }

    @Override
    public void start() {
        super.start();

        final RenderSurface surface = _renderContext.getRenderSurface();
        surface.addOnSizeChangedListener(this);
        int surfaceWidth = surface instanceof RenderSurface2 ? ((RenderSurface2)surface).getSurfaceWidth() : surface.getWidth();
        int surfaceHeight = surface instanceof RenderSurface2 ? ((RenderSurface2)surface).getSurfaceHeight() : surface.getHeight();
        onSizeChanged(surface, surfaceWidth, surfaceHeight);
    }

    @Override
    public void stop() {
        _renderContext.getRenderSurface().removeOnSizeChangedListener(this);
        super.stop();
    }

    @Override
    public void onSizeChanged(RenderSurface surface, int width, int height) {
        _subject.setSize(width, height);
    }
}
