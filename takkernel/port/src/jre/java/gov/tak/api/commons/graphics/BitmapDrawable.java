package gov.tak.api.commons.graphics;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.marshal.MarshalManager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class BitmapDrawable extends BitmapDrawableBase
{
    public BitmapDrawable(@NonNull Bitmap bitmap)
    {
        super(bitmap, null);
    }

    @Override
    public void draw(@NonNull Bitmap bitmap)
    {
        final BufferedImage src = MarshalManager.marshal(_bitmap, Bitmap.class, BufferedImage.class);
        final BufferedImage dst = MarshalManager.marshal(bitmap, Bitmap.class, BufferedImage.class);

        Graphics2D g2d = null;
        try {
            g2d = dst.createGraphics();
            g2d.drawImage(src, 0, 0, null);
        } finally {
            g2d.dispose();
        }
    }
}
