package gov.tak.platform.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import gov.tak.platform.marshal.MarshalManager;

public final class JoystickWidget extends JoystickWidgetBase {
    public JoystickWidget(int size) {
        super(size);
    }

    @Override
    gov.tak.api.commons.graphics.Drawable createJoystickDrawable(int radius, int inset) {
        Bitmap bmp = Bitmap.createBitmap((radius+inset)*2, (radius+inset)*2, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint bgPaint = new Paint();
        bgPaint.setColor(0x7FFFFFFF);
        bgPaint.setStyle(Paint.Style.FILL);
        c.drawCircle(radius+inset, radius+inset, radius, bgPaint);
        Paint fgPaint = new Paint();
        fgPaint.setColor(0xFFFFFFFF);
        fgPaint.setStrokeWidth(16);
        fgPaint.setAntiAlias(true);
        fgPaint.setStyle(Paint.Style.STROKE);
        c.drawCircle(radius+inset, radius+inset, radius, fgPaint);
        return MarshalManager.marshal(new BitmapDrawable(bmp), Drawable.class, gov.tak.api.commons.graphics.Drawable.class);
    }
}
