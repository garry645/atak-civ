package gov.tak.platform.widgets;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.platform.marshal.MarshalManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class JoystickWidget extends JoystickWidgetBase {
    public JoystickWidget(int size) {
        super(size);
    }

    @Override
    Drawable createJoystickDrawable(int radius, int inset) {
        BufferedImage bmp = new BufferedImage((radius+inset)*2, (radius+inset)*2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D c = bmp.createGraphics();
        c.setColor(new Color(0x7FFFFFFF, true));
        c.fillOval(inset, inset, radius*2, radius*2);
        c.setColor(new Color(0xFFFFFFFF, true));
        c.setStroke(new BasicStroke(16f));
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.drawOval(inset, inset, radius*2, radius*2);
        return new BitmapDrawable(MarshalManager.marshal(bmp, BufferedImage.class, Bitmap.class));
    }
}
