package gov.tak.api.commons.graphics;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.graphics.Rect;

public abstract class DrawableBase
{
    public interface Callback
    {
        void invalidate(Drawable drawable);
    }

    private Callback _callback;
    private ColorFilter _colorFilter;

    /**
     * Draws this {@link Drawable}'s content to the specified {@link Bitmap}.
     *
     * @param bitmap    A {@link Bitmap}
     */
    public abstract void draw(@NonNull Bitmap bitmap);

    /**
     * Sets the color filter to be applied to this {@code Drawable}'s content for subsequent calls
     * to {@link #draw(Bitmap)}. If {@code null} is specified, no filtering will be applied.
     *
     * @param colorFilter   The color filter to be applied or {@code null} if no filter is used.
     */
    public void setColorFilter(ColorFilter colorFilter)
    {
        _colorFilter = colorFilter;
    }

    public ColorFilter getColorFilter()
    {
        return _colorFilter;
    }

    @NonNull public abstract Rect getBounds();

    public abstract void setBounds(@NonNull Rect bounds);

    public void setCallback(Callback callback)
    {
        _callback = callback;
    }

    public Callback getCallback()
    {
        return _callback;
    }

    public void invalidate()
    {
        if(_callback != null)
            _callback.invalidate((Drawable)this);
    }
}
