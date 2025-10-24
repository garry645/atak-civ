package gov.tak.platform.symbology.milstd2525;

import com.atakmap.math.PointD;
import java.util.Map;

import armyc2.c5isr.graphics2d.BasicStroke;
import armyc2.c5isr.graphics2d.Stroke;
import armyc2.c5isr.renderer.MilStdIconRenderer;
import armyc2.c5isr.renderer.utilities.ImageInfo;
import armyc2.c5isr.renderer.utilities.MilStdSymbol;
import armyc2.c5isr.renderer.utilities.ShapeInfo;
import armyc2.c5isr.web.render.WebRenderer;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.platform.marshal.MarshalManager;

public class MilStd2525dInterop implements IMilStd2525dInterop {

    @Override
    public String renderMultiPointSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize) {
        return WebRenderer.RenderSymbol(id, name, description, symbolCode, controlPoints, altitudeMode, scale, bbox, modifiers, attributes, 0);
    }

    @Override
    public Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String, String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize) {
        // XXX - workaround for unchecked `String.charAt` access in library
        while(code.length() < 30)
            code = code + "0";

        ImageInfo image = null;
        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setModifierFont(fontName, fontType, fontSize);
            image = MilStdIconRenderer.getInstance().RenderIcon(code, modifiers, attributes);
        }
        if(image == null)
            return null;

        if(centerOffset != null) {
            centerOffset.x = image.getCenterPoint().x;
            centerOffset.y = image.getCenterPoint().y;
        }

        android.graphics.Bitmap bmp = image.getImage();
        return MarshalManager.marshal(bmp, android.graphics.Bitmap.class, Bitmap.class);
    }

    @Override
    public MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes,
                                                             String fontName, int fontType, int fontSize) {
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
    final static class RenderContextWrapper {

        static String prevFontName;
        static int prevFontSize;
        static int prevFontType;

        public synchronized static void setModifierFont(@NonNull String name, int type, int size) {
            if (!name.equals(prevFontName) || type != prevFontType || size != prevFontSize) {
                armyc2.c5isr.renderer.utilities.RendererSettings.getInstance().setModifierFont(name, type, size);
                prevFontName = name;
                prevFontSize = size;
                prevFontType = type;
            }
        }
    }
}
