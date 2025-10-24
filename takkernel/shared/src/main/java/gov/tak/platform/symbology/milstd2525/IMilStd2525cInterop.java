package gov.tak.platform.symbology.milstd2525;

import com.atakmap.math.PointD;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.Status;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface IMilStd2525cInterop<SymbolDef, MilStdSymbol, ShapeInfo>
{
    String getDescription(SymbolDef msInfo);
    String getFullPath(SymbolDef msInfo);
    int getDrawCategory(SymbolDef msInfo);
    String getBasicSymbolId(SymbolDef msInfo);
    String getBasicSymbolId(String code);
    SymbolDef getSymbolDef(String code);
    int getMinPoints(SymbolDef symbolDef);
    int getMaxPoints(SymbolDef symbolDef);
    String getHierarchy(SymbolDef symbolDef);
    String getModifiers(SymbolDef symbolDef);
    Boolean hasModifier(String symbolId, String modifier);
    List<String> getUnitModifierList();
    //int getUnitModifierKey(String modifierLetterCode);
    //String getUnitModifierLetterCode(int modifierKey);
    String getUnitModifierName(String modifierKey);
    List<String> getTGModifierList();
    String getTGModifierName(String modifierKey);
    String renderMultiPointSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize);
    Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String, String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize);
    char getAffiliationLetterCode(String symbolCode);
    String setAffiliation(String symbolCode, String affliation);
    MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize);
    Collection<ShapeInfo> getSymbolShapes(MilStdSymbol symbol);
    float[] getStrokeDashArray(ShapeInfo shapeInfo);
    Status getStatus(String code);
    String setStatus(String code, Status status);
    Amplifier getAmplifier(String symbolCode);
    String setAmplifier(String symbolCode, Amplifier amplifier);
    int getHeadquartersTaskForceDummyMask(String symbolCode);
    String setHeadquartersTaskForceDummyMask(String symbolCode, int mask);
}
