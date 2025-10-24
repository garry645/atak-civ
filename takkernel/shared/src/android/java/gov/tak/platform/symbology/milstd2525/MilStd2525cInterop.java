package gov.tak.platform.symbology.milstd2525;

import android.util.SparseArray;

import com.atakmap.math.PointD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import armyc2.c2sd.graphics2d.BasicStroke;
import armyc2.c2sd.graphics2d.Stroke;
import armyc2.c2sd.renderer.MilStdIconRenderer;
import armyc2.c2sd.renderer.utilities.ImageInfo;
import armyc2.c2sd.renderer.utilities.MilStdAttributes;
import armyc2.c2sd.renderer.utilities.MilStdSymbol;
import armyc2.c2sd.renderer.utilities.ModifiersTG;
import armyc2.c2sd.renderer.utilities.ModifiersUnits;
import armyc2.c2sd.renderer.utilities.RendererSettings;
import armyc2.c2sd.renderer.utilities.ShapeInfo;
import armyc2.c2sd.renderer.utilities.SymbolDef;
import armyc2.c2sd.renderer.utilities.SymbolDefTable;
import armyc2.c2sd.renderer.utilities.SymbolUtilities;
import armyc2.c2sd.renderer.utilities.UnitDef;
import armyc2.c2sd.renderer.utilities.UnitDefTable;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.platform.marshal.MarshalManager;
import sec.web.render.SECWebRenderer;
import gov.tak.api.symbology.Status;

final class MilStd2525cInterop implements IMilStd2525cInterop<SymbolDef, MilStdSymbol, ShapeInfo>
{
    final static String UNIT_MODIFIERS;
    static {
        StringBuilder sb = new StringBuilder();
        final List<Integer> unitModifiers = ModifiersUnits.GetModifierList();
        for(Integer modid : unitModifiers) {
            sb.append(ModifiersUnits.getModifierLetterCode(modid));
            sb.append('.');
            ModifiersUnits.getModifierName(0);
        }
        UNIT_MODIFIERS = sb.toString();
    }

    final Map<String, Integer> unitModifierIds = new HashMap<>();

    final Map<String, Integer> tgModifierIds = new HashMap<>();

    MilStd2525cInterop()
    {
        List<Integer> userMods = ModifiersUnits.GetModifierList();
        for(Integer modid : userMods)
            unitModifierIds.put(ModifiersUnits.getModifierLetterCode(modid), modid);

        List<Integer> tgMods = ModifiersTG.GetModifierList();
        for(Integer modid : tgMods)
            tgModifierIds.put(ModifiersTG.getModifierLetterCode(modid), modid);
    }

    @Override
    public String getFullPath(SymbolDef msInfo) {
        return msInfo.getFullPath();
    }

    @Override
    public String getBasicSymbolId(SymbolDef msInfo) {
        return msInfo.getBasicSymbolId();
    }

    @Override
    public String getBasicSymbolId(String code) {
        return SymbolUtilities.getBasicSymbolID(code);
    }

    @Override
    public SymbolDef getSymbolDef(String code) {
        code = getBasicSymbolId(code);
        SymbolDef symbolDef;
        do {
            symbolDef = SymbolDefTable.getInstance().getSymbolDef(code, RendererSettings.Symbology_2525C);
            if(symbolDef != null)
                break;
            symbolDef = toSymbolDef(UnitDefTable.getInstance().getUnitDef(code, RendererSettings.Symbology_2525C));
            if(symbolDef != null)
                break;

            // done
        } while(false);
        return symbolDef;
    }

    @Override
    public int getDrawCategory(SymbolDef symbolDef) {
        return symbolDef.getDrawCategory();
    }

    @Override
    public int getMinPoints(SymbolDef symbolDef) {
        return symbolDef.getMinPoints();
    }

    @Override
    public int getMaxPoints(SymbolDef symbolDef) {
        return symbolDef.getMaxPoints();
    }

    @Override
    public String getHierarchy(SymbolDef symbolDef) {
        return symbolDef.getHierarchy();
    }

    @Override
    public String getModifiers(SymbolDef symbolDef) {
        return symbolDef.getModifiers();
    }

    @Override
    public Boolean hasModifier(String symbolId, String modifier) { return SymbolUtilities.hasModifier(symbolId, unitModifierIds.get(modifier)); }

    @Override
    public List<String> getUnitModifierList() {
        return new ArrayList<>(unitModifierIds.keySet());
    }

    @Override
    public String getUnitModifierName(String modifierKey) {
        Integer modid = unitModifierIds.get(modifierKey);
        if(modid == null)
            return null;
        return ModifiersUnits.getModifierName(modid);
    }

    @Override
    public List<String> getTGModifierList() {
        return new ArrayList<>(tgModifierIds.keySet());
    }

    @Override
    public String getTGModifierName(String modifierKey) {
        Integer modid = tgModifierIds.get(modifierKey);
        if (modid == null)
            return null;
        return ModifiersTG.getModifierName(modid);
    }

    @Override
    public String renderMultiPointSymbol(String id, String name, String description,
                                         String symbolCode, String controlPoints,
                                         String altitudeMode, double scale, String bbox,
                                         Map<String, String> modifiers, Map<String, String> attributes,
                                         String fontName, int fontType, int fontSize)
    {

        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setModifierFont(fontName, fontType, fontSize);
            return SECWebRenderer.RenderSymbol(
                    id,
                    name,
                    description,
                    symbolCode,
                    controlPoints,
                    altitudeMode,
                    scale,
                    bbox,
                    marshalModifiers(modifiers, true, symbolCode),
                    marshalAttributes(attributes),
                    0,
                    RendererSettings.Symbology_2525C);
        }
    }

    @Override
    public Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String,
            String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize) {

        ImageInfo icon = null;
        synchronized (RenderContextWrapper.class) {
            RenderContextWrapper.setModifierFont(fontName, fontType, fontSize);
            icon = MilStdIconRenderer.getInstance().RenderIcon(
                    code,
                    marshalModifiers(modifiers, false, code),
                    marshalAttributes(attributes));
        }

        if(icon == null)
            return null;
        if(centerOffset != null) {
            centerOffset.x = icon.getCenterPoint().x;
            centerOffset.y = icon.getCenterPoint().y;
        }
        return MarshalManager.marshal(icon.getImage(), android.graphics.Bitmap.class, Bitmap.class);
    }

    @Override
    public char getAffiliationLetterCode(String symbolCode) {
        return SymbolUtilities.getAffiliation(symbolCode);
    }

    @Override
    public String setAffiliation(String symbolCode, String affliation) {
        return SymbolUtilities.setAffiliation(symbolCode, affliation);
    }

    @Override
    public MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name,
                                                             String description, String symbolCode,
                                                             String controlPoints, String altitudeMode,
                                                             double scale, String bbox, Map<String, String> modifiers,
                                                             Map<String, String> attributes,
                                                             String fontName, int fontType, int fontSize) {
        return SECWebRenderer.RenderMultiPointAsMilStdSymbol(
                null,
                null,
                null,
                symbolCode,
                controlPoints,
                "clampToGround",
                5869879.2,
                null,
                marshalModifiers(modifiers, true, symbolCode),
                marshalAttributes(attributes),
                RendererSettings.Symbology_2525C);
    }

    @Override
    public Collection<ShapeInfo> getSymbolShapes(MilStdSymbol milStdSymbol) {
        return milStdSymbol.getSymbolShapes();
    }

    @Override
    public float[] getStrokeDashArray(ShapeInfo shape)
    {
        final Stroke s = shape.getStroke();
        if (!(s instanceof BasicStroke))
            return null;
        BasicStroke bs = (BasicStroke) s;
        return bs.getDashArray();
    }

    @Override
    public String getDescription(SymbolDef msInfo) {
        return msInfo.getDescription();
    }

    static SparseArray<String> marshalModifiers(Map<String, String> modifiers, boolean multiPoint, String symbol)
    {
        SparseArray<String> array = new SparseArray<>();
        for(Map.Entry<String, String> entry : modifiers.entrySet()) {
            int modid;
            if (multiPoint || SymbolUtilities.isTacticalGraphic(symbol)) {
                modid = ModifiersTG.getModifierKey(entry.getKey());
            } else {
                modid = ModifiersUnits.getModifierKey(entry.getKey());
            }
            if(modid == -1)
                continue;
            array.append(modid, entry.getValue());
        }
        return array;
    }

    static SparseArray<String> marshalAttributes(Map<String, String> attributes)
    {
        SparseArray<String> array = new SparseArray<>();
        for(Map.Entry<String, String> entry : attributes.entrySet()) {
            switch(entry.getKey()) {
                case MilStd2525cAttributes.DrawAsIcon:
                    array.append(MilStdAttributes.DrawAsIcon, entry.getValue());
                    break;
                case MilStd2525cAttributes.LineColor:
                    array.append(MilStdAttributes.LineColor, entry.getValue());
                    break;
                case MilStd2525cAttributes.LineWidth:
                    array.append(MilStdAttributes.LineWidth, entry.getValue());
                    break;
                case MilStd2525cAttributes.PixelSize:
                    array.append(MilStdAttributes.PixelSize, entry.getValue());
                    break;
                case MilStd2525cAttributes.FillColor:
                    array.append(MilStdAttributes.FillColor, entry.getValue());
                    break;
                case MilStd2525cAttributes.KeepUnitRatio:
                    array.append(MilStdAttributes.KeepUnitRatio, entry.getValue());
                    break;
                case MilStd2525cAttributes.TextColor:
                    array.append(MilStdAttributes.TextColor, entry.getValue());
                    break;
                default :
                    break;
            }
        }
        return array;
    }

    static SymbolDef toSymbolDef(UnitDef unit)
    {
        return (unit != null) ?
                    new SymbolDef(unit.getBasicSymbolId(),
                                  unit.getDescription(),
                                  SymbolDef.DRAW_CATEGORY_POINT,
                                  unit.getHierarchy(),
                            1,
                            1,
                                    UNIT_MODIFIERS,
                                    unit.getFullPath()) :
                    null;
    }
    @Override
    public Status getStatus(String code) {
        String stat = SymbolUtilities.getStatus(code);

        switch (stat) {
            case "A":
                return Status.PlannedAnticipatedSuspect;
            case "C":
                return Status.PresentFullyCapable;
            case "D":
                return Status.PresentDamaged;
            case "X":
                return Status.PresentDestroyed;
            case "F":
                return Status.PresentFullToCapacity;
            default:
                return Status.Present;
        }
    }

    @Override
    public String setStatus(String code, Status status) {
        String stat = "P";
        switch(status) {
            case PlannedAnticipatedSuspect:
                stat = "A";
                break;
            case PresentFullyCapable:
                stat = "C";
                break;
            case PresentDamaged:
                stat = "D";
                break;
            case PresentDestroyed:
                stat = "X";
                break;
            case PresentFullToCapacity:
                stat = "F";
                break;
        }

        if ((code.length() == 15)
                && (!SymbolUtilities.isWeather(code)) && (!SymbolUtilities.isBasicShape(code)))
        {
            return code.substring(0, 3) + stat + code.substring(4, 15);
        }

        return code;
    }

    @Override
    public Amplifier getAmplifier(String symbolCode) {
        if(SymbolUtilities.isInstallation(symbolCode)) {
            // XXX -
            return null;
        } else if(SymbolUtilities.isMobility(symbolCode)) {
            final String mobilitySymbolModifier = symbolCode.substring(10, 12);
            switch (mobilitySymbolModifier) {
                case "MO":
                    return Amplifier.WheeledLimitedCrossCountry;
                case "MP":
                    return Amplifier.WheeledCrossCountry;
                case "MQ":
                    return Amplifier.Tracked;
                case "MR":
                    return Amplifier.WheeledAndTrackedCombination;
                case "MS":
                    return Amplifier.Towed;
                case "MT":
                    return Amplifier.Rail;
                case "MW":
                    return Amplifier.PackAnimals;
                case "MU":
                    return Amplifier.OverSnow;
                case "MV":
                    return Amplifier.Sled;
                case "MX":
                    return Amplifier.Barge;
                case "MY":
                    return Amplifier.Amphibious;
                case "NS":
                    return Amplifier.ShortTowedArray;
                case "NL":
                    return Amplifier.LongTowedArray;
                default:
                    return null;
            }
        } else { // echelon
            final String echelon = SymbolUtilities.getEchelon(symbolCode);
            if(echelon.equals("-") || echelon.equals("*"))
                return null;
            switch(echelon) {
                case "A":
                    return Amplifier.Team_Crew;
                case "B" :
                    return Amplifier.Squad;
                case "C" :
                    return Amplifier.Section;
                case "D" :
                    return Amplifier.Platoon_Detachment;
                case "E" :
                    return Amplifier.Company_Battery_Troop;
                case "F" :
                    return Amplifier.Battalion_Squadron;
                case "G" :
                    return Amplifier.Regiment_Group;
                case "H" :
                    return Amplifier.Brigade;
                case "I" :
                    return Amplifier.Division;
                case "J" :
                    return Amplifier.Corps_MEF;
                case "K" :
                    return Amplifier.Army;
                case "L" :
                    return Amplifier.ArmyGroup_Front;
                case "M" :
                    return Amplifier.Region_Theater;
                case "N" :
                    return Amplifier.Command;
                default :
                    return null;
            }
        }
    }

    @Override
    public String setAmplifier(String symbolCode, Amplifier amplifier) {
        // if installation or mobility amplifier, blow away HQTFD
        if(SymbolUtilities.isInstallation(symbolCode) || SymbolUtilities.isMobility(symbolCode))
            setHeadquartersTaskForceDummyMask(symbolCode, 0);
        if(amplifier == null)
            return SymbolUtilities.setEchelon(symbolCode, "-");
        switch(amplifier) {
            case Team_Crew:
                return SymbolUtilities.setEchelon(symbolCode, "A");
            case Squad:
                return SymbolUtilities.setEchelon(symbolCode, "B");
            case Section:
                return SymbolUtilities.setEchelon(symbolCode, "C");
            case Platoon_Detachment:
                return SymbolUtilities.setEchelon(symbolCode, "D");
            case Company_Battery_Troop:
                return SymbolUtilities.setEchelon(symbolCode, "E");
            case Battalion_Squadron:
                return SymbolUtilities.setEchelon(symbolCode, "F");
            case Regiment_Group:
                return SymbolUtilities.setEchelon(symbolCode, "G");
            case Brigade:
                return SymbolUtilities.setEchelon(symbolCode, "H");
            case Division:
                return SymbolUtilities.setEchelon(symbolCode, "I");
            case Corps_MEF:
                return SymbolUtilities.setEchelon(symbolCode, "J");
            case Army:
                return SymbolUtilities.setEchelon(symbolCode, "K");
            case ArmyGroup_Front:
                return SymbolUtilities.setEchelon(symbolCode, "L");
            case Region_Theater:
                return SymbolUtilities.setEchelon(symbolCode, "M");
            case Command:
                return SymbolUtilities.setEchelon(symbolCode, "N");
            case WheeledLimitedCrossCountry:
                return symbolCode.substring(0, 10) + "MO" + symbolCode.substring(12, 15);
            case WheeledCrossCountry:
                return symbolCode.substring(0, 10) + "MP" + symbolCode.substring(12, 15);
            case Tracked:
                return symbolCode.substring(0, 10) + "MQ" + symbolCode.substring(12, 15);
            case WheeledAndTrackedCombination:
                return symbolCode.substring(0, 10) + "MR" + symbolCode.substring(12, 15);
            case Towed:
                return symbolCode.substring(0, 10) + "MS" + symbolCode.substring(12, 15);
            case Rail:
                return symbolCode.substring(0, 10) + "MT" + symbolCode.substring(12, 15);
            case PackAnimals:
                return symbolCode.substring(0, 10) + "MW" + symbolCode.substring(12, 15);
            case OverSnow :
                return symbolCode.substring(0, 10) + "MU" + symbolCode.substring(12, 15);
            case Sled:
                return symbolCode.substring(0, 10) + "MV" + symbolCode.substring(12, 15);
            case Barge:
                return symbolCode.substring(0, 10) + "MX" + symbolCode.substring(12, 15);
            case Amphibious:
                return symbolCode.substring(0, 10) + "MY" + symbolCode.substring(12, 15);
            case ShortTowedArray:
                return symbolCode.substring(0, 10) + "NS" + symbolCode.substring(12, 15);
            case LongTowedArray:
                return symbolCode.substring(0, 10) + "NL" + symbolCode.substring(12, 15);
            default :
                return SymbolUtilities.setEchelon(symbolCode, "-");
        }
    }

    @Override
    public int getHeadquartersTaskForceDummyMask(String symbolCode) {
        int mask = 0;
        if(SymbolUtilities.isHQ(symbolCode))
            mask |= ISymbologyProvider2.MASK_HEADQUARTERS;
        if(SymbolUtilities.isTaskForce(symbolCode))
            mask |= ISymbologyProvider2.MASK_TASKFORCE;
        if(SymbolUtilities.isFeintDummy(symbolCode))
            mask |= ISymbologyProvider2.MASK_DUMMY_FEINT;
        return mask;
    }

    @Override
    public String setHeadquartersTaskForceDummyMask(String strSymbolID, int mask) {
        // XXX - not seeing library impl
        if(strSymbolID.length() == 15) {
            mask &= ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_TASKFORCE|ISymbologyProvider2.MASK_HEADQUARTERS;
            char hqtfd = '-';
            switch(mask) {
                case 0 :
                    hqtfd = '-';
                    break;
                case ISymbologyProvider2.MASK_DUMMY_FEINT :
                    hqtfd = 'F';
                    break;
                case ISymbologyProvider2.MASK_HEADQUARTERS :
                    hqtfd = 'A';
                    break;
                case ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_HEADQUARTERS :
                    hqtfd = 'C';
                    break;
                case ISymbologyProvider2.MASK_TASKFORCE:
                    hqtfd = 'E';
                    break;
                case ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_TASKFORCE :
                    hqtfd = 'G';
                    break;
                case ISymbologyProvider2.MASK_HEADQUARTERS|ISymbologyProvider2.MASK_TASKFORCE :
                    hqtfd = 'B';
                    break;
                case ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_HEADQUARTERS|ISymbologyProvider2.MASK_TASKFORCE :
                    hqtfd = 'D';
                    break;
                default :
                    hqtfd = '-';
                    break;
            }
            if(strSymbolID.charAt(10) != hqtfd) {
                char[] code = strSymbolID.toCharArray();
                code[10] = hqtfd;
                if(code[11] != '-' && code[11] >= 'O' && code[11] <= 'Z') {
                    code[11] = '-';
                }
                strSymbolID = new String(code);
            }
        }
        return strSymbolID;
    }

    final static class RenderContextWrapper {

        static String prevFontName;
        static int prevFontSize;
        static int prevFontType;

        public synchronized static void setModifierFont(@NonNull String name, int type, int size) {
            if (!name.equals(prevFontName) || type != prevFontType || size != prevFontSize) {
                RendererSettings.getInstance().setModifierFont(name, type, size);
                prevFontName = name;
                prevFontSize = size;
                prevFontType = type;
            }
        }
    }

}
