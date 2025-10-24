package gov.tak.api.commons.graphics;

import java.awt.image.BufferedImage;
import java.nio.Buffer;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public abstract class Drawable extends DrawableBase
{
    static {
        MarshalManager.registerMarshal(new AbstractMarshal(BufferedImage.class, Drawable.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                if (!(in instanceof BufferedImage))
                    return null;

                Bitmap bitmap = MarshalManager.marshal((BufferedImage)in, BufferedImage.class, Bitmap.class);
                return (T) new BitmapDrawable(bitmap);
            }
        }, BufferedImage.class, Drawable.class);
    }

    private Rect _bounds;

    public Drawable()
    {
        _bounds = new Rect();
    }

    Drawable(Object opaqueIgnored)
    {
        this();
    }

    @Override
    @NonNull public Rect getBounds()
    {
        return _bounds;
    }

    @Override
    public void setBounds(@NonNull Rect bounds)
    {
        _bounds.set(bounds);
    }
}
