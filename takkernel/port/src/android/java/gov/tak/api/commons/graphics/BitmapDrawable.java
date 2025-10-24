package gov.tak.api.commons.graphics;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.marshal.MarshalManager;

public class BitmapDrawable extends BitmapDrawableBase
{
    public BitmapDrawable(@NonNull Bitmap bitmap)
    {
        super(bitmap,
              new android.graphics.drawable.BitmapDrawable(
                        bitmap.getAndroidBitmapImplementation()));
        _impl.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public BitmapDrawable(@NonNull android.graphics.drawable.BitmapDrawable impl)
    {
        super(MarshalManager.marshal(impl, android.graphics.drawable.Drawable.class, Bitmap.class),
                impl);
    }

    @Override
    public void draw(@NonNull Bitmap bitmap)
    {
        AndroidDrawable.draw(_impl, bitmap);
    }
}
