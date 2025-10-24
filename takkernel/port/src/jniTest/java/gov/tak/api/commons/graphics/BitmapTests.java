package gov.tak.api.commons.graphics;

import gov.tak.platform.graphics.Color;
import gov.tak.test.KernelTest;

import org.junit.Assert;
import org.junit.Test;

public class BitmapTests extends KernelTest
{
    private static final int TEST_WIDTH = 4;
    private static final int TEST_HEIGHT = 4;

    private static final int BLANK_ARGB = 0;

    private static final int[] BLANK_ARGB_ARRAY = {
            BLANK_ARGB, BLANK_ARGB, BLANK_ARGB, BLANK_ARGB,
            BLANK_ARGB, BLANK_ARGB, BLANK_ARGB, BLANK_ARGB,
            BLANK_ARGB, BLANK_ARGB, BLANK_ARGB, BLANK_ARGB,
            BLANK_ARGB, BLANK_ARGB, BLANK_ARGB, BLANK_ARGB
    };

    private static final int[] WHITE_ARGB_ARRAY = {
            Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE,
            Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE,
            Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE,
            Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE
    };

    private static void assertPixels(Bitmap bitmap, int argb) {
        for (int x = 0; x < bitmap.getWidth(); ++x) {
            for (int y = 0; y < bitmap.getHeight(); ++y) {
                int pixelArgb = bitmap.getPixel(x, y);
                Assert.assertEquals(pixelArgb, argb);
            }
        }
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; ++i) {
            Assert.assertEquals(pixels[i], argb);
        }
    }

    private static void assertPixelArray(Bitmap bitmap, int[] argb) {
        int pi = 0;
        for (int x = 0; x < bitmap.getWidth(); ++x) {
            for (int y = 0; y < bitmap.getHeight(); ++y) {
                int pixelArgb = bitmap.getPixel(x, y);
                Assert.assertEquals(pixelArgb, argb[pi++]);
            }
        }
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; ++i) {
            Assert.assertEquals(pixels[i], argb[i]);
        }
    }

    @Test
    public void testConstructWidthHeight()
    {
        Bitmap apiBitmap = new Bitmap(TEST_WIDTH, TEST_HEIGHT);
        Assert.assertEquals(apiBitmap.getWidth(), TEST_WIDTH);
        Assert.assertEquals(apiBitmap.getHeight(), TEST_HEIGHT);
        assertPixels(apiBitmap, BLANK_ARGB);
        assertPixelArray(apiBitmap, BLANK_ARGB_ARRAY);
    }

    @Test
    public void testConstructWithColors()
    {
        Bitmap apiBitmap = new Bitmap(WHITE_ARGB_ARRAY, 0, TEST_WIDTH, TEST_WIDTH, TEST_HEIGHT);
        Assert.assertEquals(apiBitmap.getWidth(), TEST_WIDTH);
        Assert.assertEquals(apiBitmap.getHeight(), TEST_HEIGHT);
        assertPixels(apiBitmap, Color.WHITE);
        assertPixelArray(apiBitmap, WHITE_ARGB_ARRAY);
    }

    @Test
    public void testConstructSubPixels() {

        int [] sub = {
                Color.WHITE, Color.WHITE,
                Color.WHITE, Color.WHITE
        };

        int[] expect = {
                sub[0], sub[1], 0, 0,
                sub[2], sub[3], 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
        };

        Bitmap apiBitmap = new Bitmap(sub, 0, 2, 0, 0, 2, 2, 4, 4);
        Assert.assertEquals(apiBitmap.getWidth(), 4);
        Assert.assertEquals(apiBitmap.getHeight(), 4);
        assertPixelArray(apiBitmap, expect);
    }
}
