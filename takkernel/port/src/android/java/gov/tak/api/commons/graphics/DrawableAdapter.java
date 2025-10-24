package gov.tak.api.commons.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;

import gov.tak.platform.graphics.Rect;

final class DrawableAdapter extends android.graphics.drawable.Drawable implements DrawableBase.Callback
{
    final Drawable _impl;
    int _alpha;
    int _opacity;
    ColorFilter _filter;

    DrawableAdapter(Drawable impl)
    {
        _impl = impl;
        _alpha = 255;
        _opacity = PixelFormat.TRANSLUCENT;
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = _impl.getBounds();
        Bitmap bitmap = new Bitmap(bounds.width(), bounds.height());
        _impl.draw(bitmap);
        canvas.drawBitmap(bitmap.getAndroidBitmapImplementation(), bounds.left, bounds.top, null);
    }

    @Override
    public void setAlpha(int i)
    {
        _alpha = i;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
        _filter = colorFilter;
    }

    @Override
    public int getOpacity()
    {
        return _opacity;
    }

    @Override
    public void invalidate(Drawable drawable)
    {
        if(drawable != _impl) return;
        final android.graphics.drawable.Drawable.Callback cb = getCallback();
        if(cb != null)
            cb.invalidateDrawable(this);
    }
}
