package gov.tak.api.commons.graphics;

import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.MarshalManager;

import java.awt.image.BufferedImage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Marshals {@link java.awt.image.BufferedImage} to {@link gov.tak.api.commons.graphics.Bitmap}
 */
final class BitmapMarshal
{
    static void register()
    {
        MarshalManager.registerMarshal(new Portable(), java.awt.image.BufferedImage.class, gov.tak.api.commons.graphics.Bitmap.class);
        MarshalManager.registerMarshal(new Platform(), gov.tak.api.commons.graphics.Bitmap.class, java.awt.image.BufferedImage.class);
        MarshalManager.registerMarshal(new AndroidPlatform(), gov.tak.api.commons.graphics.Bitmap.class, android.graphics.Bitmap.class);
    }

    /**
     * Marshals from <I>platform</I> {@link java.awt.image.BufferedImage} to <I>portable</I>
     * {@link gov.tak.api.commons.graphics.Bitmap}
     */
    private final static class Portable implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            BufferedImage in = (BufferedImage)inOpaque;
            if(in == null)  return null;
            try
            {
                Constructor<android.graphics.Bitmap> androidPortCtor = android.graphics.Bitmap.class.getDeclaredConstructor(BufferedImage.class);
                androidPortCtor.setAccessible(true);
                android.graphics.Bitmap androidBitmap = (android.graphics.Bitmap) androidPortCtor.newInstance(in);
                return (T) new gov.tak.api.commons.graphics.Bitmap(androidBitmap);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e)
            {
                return null;
            }
        }
    }

    /**
     * Marshals from <I>portable</I> {@link gov.tak.api.commons.graphics.Bitmap} to <I>platform</I>
     * {@link java.awt.image.BufferedImage}
     */
    private final static class Platform implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            gov.tak.api.commons.graphics.Bitmap in = (gov.tak.api.commons.graphics.Bitmap) inOpaque;
            if(in == null)  return null;

            // double unwrap
            android.graphics.Bitmap impl = (android.graphics.Bitmap)in.getAndroidBitmapImplementation();
            try
            {
                Method getImplMethod = android.graphics.Bitmap.class.getDeclaredMethod(
                        "getAwtBufferedImageImplementation");
                getImplMethod.setAccessible(true);
                return (T) getImplMethod.invoke(impl);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
            {
                return null;
            }
        }
    }

    /**
     * Marshals from <I>portable</I> {@link gov.tak.api.commons.graphics.Bitmap} to <I>platform</I>
     * {@link android.graphics.Bitmap}
     */
    private final static class AndroidPlatform implements IMarshal
    {
        @Override public <T, V> T marshal(V inOpaque)
        {
            gov.tak.api.commons.graphics.Bitmap in = (gov.tak.api.commons.graphics.Bitmap) inOpaque;
            if(in == null)  return null;
            return (T) in.getAndroidBitmapImplementation();
        }
    }
}
