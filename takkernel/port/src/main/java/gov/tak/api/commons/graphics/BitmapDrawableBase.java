package gov.tak.api.commons.graphics;

import gov.tak.api.annotation.NonNull;

import java.util.Objects;

abstract class BitmapDrawableBase extends Drawable
{
    final Bitmap _bitmap;

    BitmapDrawableBase(final @NonNull Bitmap bitmap, Object drawableOpaque)
    {
        super(drawableOpaque);
        Objects.requireNonNull(bitmap);
        _bitmap = bitmap;
    }

    @NonNull public final Bitmap getBitmap()
    {
        return _bitmap;
    }
}
