package com.atakmap.opengl;

import android.graphics.Bitmap;

final class GLUtils
{
    private GLUtils()
    {
    }

    public static void texSubImage2D(int target, int level, int x, int y, Bitmap bitmap)
    {
        android.opengl.GLUtils.texSubImage2D(target, level, x, y, bitmap);
    }

    public static void texImage2D(int target, int level, int format, Bitmap bitmap, int type, int border)
    {
        android.opengl.GLUtils.texImage2D(target, level, format, bitmap, type, border);
    }
}
