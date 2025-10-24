package com.atakmap.opengl;

import android.graphics.Bitmap;
import com.atakmap.coremap.log.Log;
import com.atakmap.lang.Unsafe;
import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.marshal.MarshalManager;

import java.nio.ByteBuffer;

final class GLUtils
{
    private GLUtils()
    {
    }

    public static void texSubImage2D(int target, int level, int x, int y, Bitmap bitmap)
    {
        int format;
        int type;
        ByteBuffer data = null;
        try {
            switch (bitmap.getConfig()) {
                case RGB_565:
                    format = GLES30.GL_RGB;
                    type = GLES30.GL_UNSIGNED_SHORT_5_6_5;
                    data = Unsafe.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 2, ByteBuffer.class);
                    bitmap.copyPixelsToBuffer(data);
                    break;
                case ARGB_8888:
                    format = GLES30.GL_RGBA;
                    type = GLES30.GL_UNSIGNED_BYTE;
                    data = Unsafe.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4, ByteBuffer.class);
                    bitmap.copyPixelsToBuffer(data);
                    break;
                default:
                    Log.w("GLUtils", "texSubImage2D Bitmap.Config " + bitmap.getConfig() + " not supported.");
                    return;
            }
            GLES30.glTexSubImage2D(target, level, x, y, bitmap.getWidth(), bitmap.getHeight(), format, type, data);
        } finally {
            if(data != null)
                Unsafe.free(data);
        }
    }

    public static void texImage2D(int target, int level, int format, Bitmap bitmap, int type, int border)
    {
        ByteBuffer data = null;
        try {
            switch (bitmap.getConfig()) {
                case RGB_565:
                    data = Unsafe.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 2, ByteBuffer.class);
                    bitmap.copyPixelsToBuffer(data);
                    break;
                case ARGB_8888:
                    data = Unsafe.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4, ByteBuffer.class);
                    bitmap.copyPixelsToBuffer(data);
                    break;
                default:
                    Log.w("GLUtil", "texSubImage2D Bitmap.Config " + bitmap.getConfig() + " not supported.");
                    return;
            }
            GLES30.glTexImage2D(target, level, format, bitmap.getWidth(), bitmap.getHeight(), border, format, type, data);
        } finally {
            if(data != null)
                Unsafe.free(data);
        }
    }
}
