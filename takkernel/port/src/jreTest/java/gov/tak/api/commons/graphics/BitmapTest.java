package gov.tak.api.commons.graphics;

import java.awt.image.BufferedImage;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.platform.marshal.MarshalManager;

public class BitmapTest
{
    @Test
    public void testMarshalToAwt()
    {
        Bitmap apiBitmap = new Bitmap(4, 4);
        BufferedImage awtImage = MarshalManager.service().marshal(apiBitmap, Bitmap.class, BufferedImage.class);
        Assert.assertNotNull(awtImage);
        for (int x = 0; x < apiBitmap.getWidth(); ++x) {
            for (int y = 0; y < apiBitmap.getHeight(); ++y) {
                Assert.assertEquals(apiBitmap.getPixel(x, y), awtImage.getRGB(x, y));
            }
        }
    }

    @Test
    public void testMarshalToApi()
    {
        BufferedImage awtImage = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Bitmap apiBitmap = MarshalManager.service().marshal(awtImage, BufferedImage.class, Bitmap.class);
        Assert.assertNotNull(apiBitmap);
        for (int x = 0; x < apiBitmap.getWidth(); ++x) {
            for (int y = 0; y < apiBitmap.getHeight(); ++y) {
                Assert.assertEquals(apiBitmap.getPixel(x, y), awtImage.getRGB(x, y));
            }
        }
    }
}