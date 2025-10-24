package gov.tak.api.commons.graphics;

/**
 * Raster graphic represented by matrix of ARGB pixels.
 */
public final class Bitmap
{
    static
    {
        BitmapMarshal.register();
    }

    static final android.graphics.Bitmap.Config defaultConfig = android.graphics.Bitmap.Config.ARGB_8888;
    private android.graphics.Bitmap impl;

    // package private
    Bitmap(android.graphics.Bitmap androidBitmap)
    {
        impl = androidBitmap;
    }

   android.graphics.Bitmap getAndroidBitmapImplementation()
    {
        return impl;
    }

    /**
     * Create a bitmap with a specified width and height.
     *
     * @param width number of pixels per row
     * @param height number of pixels per column
     *
     * @throws IllegalArgumentException if width or height <= 0
     */
    public Bitmap(int width, int height)
    {
        // matches exception contract
        impl = android.graphics.Bitmap.createBitmap(width, height, defaultConfig);
    }

    /**
     * Create a bitmap with specified ARGB color values.
     *
     * @param argb the ARGB color values to write to the bitmap
     * @param offset the index of the first color to read from argb[]
     * @param stride the number of argb[] elements to skip between rows (positive OR negative)
     * @param x the x coordinate of the first pixel to copy to the origin pixel of the bitmap
     * @param y the y coordinate of the first pixel to copy to the origin pixel of the bitmap
     * @param sourceWidth number of pixels per row to copy from the source argb[]
     * @param sourceHeight number of pixels per column to copy from the source argb[]
     * @param width number of pixels per row of the bitmap
     * @param height number of pixels per column of the bitmap
     *
     * @throws IllegalArgumentException if width or height <= 0
     * @throws ArrayIndexOutOfBoundsException – if the pixels array is too small to index according
     * to offset, stride, x, y, width and height parameters
     */
    public Bitmap(int[] argb, int offset, int stride, int x, int y, int sourceWidth, int sourceHeight, int width, int height)
    {
        // matches exception contract
        this(width, height);
        impl.setPixels(argb, offset, stride, x, y, sourceWidth, sourceHeight);
    }

    /**
     * Create a bitmap with specified ARGB color values.
     *
     * @param argb the ARGB color values to write to the bitmap
     * @param offset the index of the first color to read from argb[]
     * @param stride the number of argb[] elements to skip between rows (positive OR negative)
     * @param width number of pixels per row of the bitmap
     * @param height number of pixels per column of the bitmap
     *
     * @throws IllegalArgumentException if width or height <= 0
     * @throws ArrayIndexOutOfBoundsException – if the pixels array is too small to index according
     * to offset, stride, x, y, width and height parameters
     */
    public Bitmap(int[] argb, int offset, int stride, int width, int height)
    {
        this(argb, offset, stride, 0, 0, width, height, width, height);
    }

    /**
     * Get the number of pixels per row
     *
     * @return positive integer indicating number of pixels per row
     */
    public int getWidth()
    {
        return impl.getWidth();
    }

    /**
     * Get the number of pixels per column
     *
     * @return positive integer indicating number of pixels per column
     */
    public int getHeight()
    {
        return impl.getHeight();
    }

    /**
     * Get the ARGB color value of a pixel at a given x, y coordinate
     *
     * @param x the x coordinate of the pixel
     * @param y the y coordinate of the pixel
     *
     * @return the ARGB color value
     *
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    public int getPixel(int x, int y)
    {
        // matches exception contract
        return impl.getPixel(x, y);
    }

    /**
     * Returns in argb[] a copy of the data in the bitmap.
     *
     * @param argb the array to receive the bitmap's colors
     * @param offset the first index to write into argb[]
     * @param stride the number of entries in pixels[] to skip between rows (must be >= bitmap's width). Can be negative.
     * @param x the x coordinate of the first pixel to read from the bitmap
     * @param y the y coordinate of the first pixel to read from the bitmap
     * @param width the number of pixels to read from each row
     * @param height the number of rows to read
     *
     * @throws IllegalArgumentException if x, y, width, height exceed the bounds of the bitmap,
     * or if abs(stride) < width.
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small to receive the
     * specified number of pixels.
     */
    public void getPixels(int[] argb, int offset, int stride, int x, int y, int width, int height)
    {
        // matches exception contract
        impl.getPixels(argb, offset, stride, x, y, width, height);
    }

    /**
     * Write the specified ARGB color value into the bitmap at the x,y coordinate.
     *
     * @param x the x coordinate of the pixel to replace (0...width-1)
     * @param y the y coordinate of the pixel to replace (0...height-1)
     * @param argb the ARGB color value to write into the bitmap
     *
     * @throws IllegalArgumentException if x, y are outside of the bitmap's bounds.
     */
    public void setPixel(int x, int y, int argb)
    {
        // matches exception contract
        impl.setPixel(x, y, argb);
    }

    /**
     * Replace pixels in the bitmap with the colors in the array.

     * @param argb the ARGB color values to write to the bitmap
     * @param offset the index of the first color to read from argb[]
     * @param stride the number of colors in pixels[] to skip between rows.
     * @param x the x coordinate of the first pixel to write to in the bitmap.
     * @param y the y coordinate of the first pixel to write to in the bitmap.
     * @param width the number of colors to copy from argb[] per row
     * @param height the number of rows to write to the bitmap
     *
     * @throws IllegalArgumentException if x, y, width, height are outside of the bitmap's bounds.
     * @throws ArrayIndexOutOfBoundsException if the pixels array is too small to receive the specified number of pixels.
     */
    public void setPixels(int[] argb, int offset, int stride, int x, int y, int width, int height)
    {
        // matches exception contract
        impl.setPixels(argb, offset, stride, x, y, width, height);
    }
}
