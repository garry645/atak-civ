package gov.tak.platform.symbology.milstd2525;

import armyc2.c5isr.renderer.MilStdIconRenderer;
import armyc2.c5isr.renderer.utilities.ImageInfo;
import armyc2.c5isr.renderer.utilities.MilStdSymbol;
import armyc2.c5isr.renderer.utilities.ShapeInfo;
import armyc2.c5isr.web.render.WebRenderer;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.platform.marshal.MarshalManager;
import com.atakmap.math.PointD;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Map;

class MilStd2525dInterop implements IMilStd2525dInterop {

    @Override
    public String renderMultiPointSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize) {
        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setLabelFontD(fontName, fontType, fontSize);

            return WebRenderer.RenderSymbol(id, name, description, symbolCode, controlPoints, altitudeMode, scale, bbox, modifiers, attributes, 0);
        }
    }

    @Override
    public Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String, String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize) {
        // XXX - workaround for unchecked `String.charAt` access in library
        while(code.length() < 30)
            code = code + "0";
        ImageInfo info = null;

        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setLabelFontD(fontName, fontType, fontSize);
            info = MilStdIconRenderer.getInstance().RenderIcon(code, modifiers, attributes);
        }

        if(info == null)
            return null;

        if(centerOffset != null) {
            centerOffset.x = info.getSymbolCenterPoint().x;
            centerOffset.y = info.getSymbolCenterPoint().y;
        }

        BufferedImage bmp = info.getSquareImageInfo().getImage();
        return MarshalManager.marshal(bmp, BufferedImage.class, Bitmap.class);
    }

    @Override
    public MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize) {

        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setLabelFontD(fontName, fontType, fontSize);

            return WebRenderer.RenderMultiPointAsMilStdSymbol(
                    id,
                    name,
                    description,
                    symbolCode,
                    controlPoints,
                    altitudeMode,
                    scale,
                    bbox,
                    modifiers,
                    attributes);
        }
    }

    @Override
    public float[] getStrokeDashArray(MilStdSymbol mSymbol) {
        // inspect the shapes to derive the pattern
        for (ShapeInfo shape : mSymbol.getSymbolShapes()) {
            final Stroke s = shape.getStroke();
            if (!(s instanceof BasicStroke))
                continue;
            BasicStroke bs = (BasicStroke) s;
            final float[] dashArray = bs.getDashArray();
            if(dashArray == null)
                continue;
            return dashArray;
        }
        return null;
    }
}
