package gov.tak.api.widgets;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.tak.platform.widgets.opengl.GLWidgetsLayer;

public final class WidgetsLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLWidgetsLayer.SPI);
    }

    private final ILayoutWidget root;

    public WidgetsLayer(String name, ILayoutWidget root)
    {
        super(name);

        this.root = root;
    }

    public ILayoutWidget getRoot()
    {
        return this.root;
    }
}

