package gov.tak.api.commons.graphics;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.platform.marshal.MarshalManager;

public class BitmapTest
{
    @Test
    public void testMarshalToAndroid()
    {
        Bitmap apiBitmap = new Bitmap(4, 4);
        android.graphics.Bitmap androidBitmap = MarshalManager.service().marshal(apiBitmap, Bitmap.class, android.graphics.Bitmap.class);
        Assert.assertNotNull(androidBitmap);
        for (int x = 0; x < apiBitmap.getWidth(); ++x) {
            for (int y = 0; y < apiBitmap.getHeight(); ++y) {
                Assert.assertEquals(apiBitmap.getPixel(x, y), androidBitmap.getPixel(x, y));
            }
        }
    }

    @Test
    public void testMarshalToApi()
    {
        android.graphics.Bitmap androidBitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888);
        Bitmap apiBitmap = MarshalManager.service().marshal(androidBitmap, android.graphics.Bitmap.class, Bitmap.class);
        Assert.assertNotNull(apiBitmap);
        for (int x = 0; x < apiBitmap.getWidth(); ++x) {
            for (int y = 0; y < apiBitmap.getHeight(); ++y) {
                Assert.assertEquals(apiBitmap.getPixel(x, y), androidBitmap.getPixel(x, y));
            }
        }
    }
}