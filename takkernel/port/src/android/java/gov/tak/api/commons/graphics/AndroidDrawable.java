package gov.tak.api.commons.graphics;

import android.graphics.Canvas;

final class AndroidDrawable extends Drawable
{
    AndroidDrawable(android.graphics.drawable.Drawable impl)
    {
        super(impl);
    }

    @Override
    public void draw(Bitmap bitmap) {
        draw(_impl, bitmap);
    }

    static void draw(android.graphics.drawable.Drawable drawable, Bitmap bitmap)
    {
        android.graphics.Canvas c = new Canvas(bitmap.getAndroidBitmapImplementation());
        drawable.draw(c);
    }
}
