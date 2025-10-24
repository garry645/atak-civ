package gov.tak.platform.symbology.milstd2525;

import com.atakmap.math.PointD;

import java.util.Map;

import armyc2.c5isr.renderer.utilities.MilStdSymbol;
import gov.tak.api.commons.graphics.Bitmap;

interface IMilStd2525dInterop
{
    String renderMultiPointSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize);
    Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String, String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize);
    MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize);
    float[] getStrokeDashArray(MilStdSymbol shapeInfo);
}
