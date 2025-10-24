package gov.tak.api.commons.graphics;

import java.io.File;
import java.io.InputStream;

public final class BitmapFactory
{
    private BitmapFactory() {}

    /**
     * Decode a file into a bitmap. If the specified file name is null, or cannot be decoded into
     * a bitmap, the function returns null.
     *
     * @param file the file to decode
     *
     * @return Bitmap isntance or null on failure
     */
    public static Bitmap decodeFile(File file)
    {
        if (file == null)
            return null;
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
        try
        {
            return new Bitmap(android.graphics.BitmapFactory.decodeFile(file.getPath(), opts));
        } catch (Exception ex) // for android-port's IOException
        {
            return null;
        }
    }

    /**
     * Decode an input stream into a bitmap. If the input stream is null, or cannot be used to
     * decode a bitmap, the function returns null. The stream's position will be where ever it was
     * after the encoded data was read.
     *
     * @param inputStream the input stream
     *
     * @return Bitmap instance or null on failure
     */
    public static Bitmap decodeStream(InputStream inputStream)
    {
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
        try
        {
            return new Bitmap(android.graphics.BitmapFactory.decodeStream(inputStream, null, opts));
        } catch (Exception ex) // for android-port's IOException
        {
            return null;
        }
    }
}
