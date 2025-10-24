package gov.tak.api.commons.graphics;

import android.graphics.PorterDuff;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.marshal.MarshalManager;

public abstract class Drawable extends DrawableBase
{
    static
    {
        MarshalManager.registerMarshal(new IMarshal() {
            @Override
            public <T, V> T marshal(V in) {
                if(in == null)  return null;
                else if(in instanceof android.graphics.drawable.BitmapDrawable) return (T) new BitmapDrawable((android.graphics.drawable.BitmapDrawable)in);
                else return (T) new AndroidDrawable((android.graphics.drawable.Drawable)in);
            }
        }, android.graphics.drawable.Drawable.class, Drawable.class);
        MarshalManager.registerMarshal(new IMarshal() {
            @Override
            public <T, V> T marshal(V in) {
                if(in == null)  return null;
                final Drawable drawable = (Drawable) in;
                return (T)drawable._impl;
            }
        }, Drawable.class, android.graphics.drawable.Drawable.class);
    }

    android.graphics.drawable.Drawable _impl;

    public Drawable()
    {
        this(null);
    }

    Drawable(Object opaque)
    {
        android.graphics.drawable.Drawable impl = (android.graphics.drawable.Drawable) opaque;
        if(impl == null) impl = new DrawableAdapter(this);
        _impl = impl;
    }

    @Override
    @NonNull public final Rect getBounds()
    {
        android.graphics.Rect bounds = _impl.getBounds();
        return new Rect(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    @Override
    public void setBounds(@NonNull Rect bounds)
    {
        _impl.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        if(colorFilter != null) {
            _impl.setColorFilter(
                    colorFilter.getColor(),
                    MarshalManager.marshal(
                            colorFilter.getBlendMode(),
                            ColorBlendMode.class,
                            PorterDuff.Mode.class));
        } else {
            _impl.clearColorFilter();
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        final android.graphics.drawable.Drawable.Callback cb = _impl.getCallback();
        if(cb != null)
            cb.invalidateDrawable(_impl);
    }
}
