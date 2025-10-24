package gov.tak.api.commons.graphics;

import android.graphics.drawable.BitmapDrawable;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.MarshalManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Marshals {@link android.graphics.Bitmap} to {@link gov.tak.api.commons.graphics.Bitmap}
 */
final class BitmapMarshal
{
    static void register()
    {
        MarshalManager.registerMarshal(new Portable(), android.graphics.Bitmap.class, gov.tak.api.commons.graphics.Bitmap.class);
        MarshalManager.registerMarshal(new Platform(), gov.tak.api.commons.graphics.Bitmap.class, android.graphics.Bitmap.class);
        MarshalManager.registerMarshal(new Drawable(), android.graphics.drawable.Drawable.class, gov.tak.api.commons.graphics.Bitmap.class);
    }

    /**
     * Marshals from <I>platform</I> {@link android.graphics.Bitmap} to <I>portable</I>
     * {@link gov.tak.api.commons.graphics.Bitmap}
     */
    private final static class Portable implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            android.graphics.Bitmap in = (android.graphics.Bitmap)inOpaque;
            if(in == null)  return null;
            return (T) new gov.tak.api.commons.graphics.Bitmap(in);
        }
    }

    /**
     * Marshals from <I>platform</I> {@link android.graphics.drawable.Drawable} to <I>portable</I>
     * {@link gov.tak.api.commons.graphics.Bitmap}
     */
    private final static class Drawable implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            if(inOpaque == null) return null;

            android.graphics.Bitmap bitmap = null;
            android.graphics.drawable.Drawable drawable = (android.graphics.drawable.Drawable)inOpaque;
            if(drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable)drawable).getBitmap();
            }

            if(bitmap == null) {
                // from https://stackoverflow.com/a/10600736
                if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                    bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
                } else {
                    bitmap = android.graphics.Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                }

                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            return (T) MarshalManager.marshal(
                    bitmap,
                    android.graphics.Bitmap.class,
                    gov.tak.api.commons.graphics.Bitmap.class);
        }
    }

    /**
     * Marshals from <I>portable</I> {@link gov.tak.api.commons.graphics.Bitmap} to <I>platform</I>
     * {@link android.graphics.Bitmap} to
     */
    private final static class Platform implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            gov.tak.api.commons.graphics.Bitmap in = (gov.tak.api.commons.graphics.Bitmap) inOpaque;
            if(in == null)  return null;
            return (T) in.getAndroidBitmapImplementation();
        }
    }
}
