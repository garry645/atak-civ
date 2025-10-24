package gov.tak.platform.symbology.milstd2525;

import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

import ArmyC2.C2SD.Rendering.JavaRenderer;
import ArmyC2.C2SD.Utilities.*;
import JavaTacticalRenderer.TGLight;
import RenderMultipoints.clsRenderer;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.api.symbology.Status;
import gov.tak.platform.marshal.MarshalManager;
import org.json.JSONArray;
import org.json.JSONObject;
import sec.web.renderer.SECRenderer;
import sec.web.renderer.utilities.JavaRendererUtilities;
import sec.web.renderer.utilities.PNGInfo;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

final class MilStd2525cInterop implements IMilStd2525cInterop<SymbolDef, MilStdSymbol, ShapeInfo>
{
    final static String UNIT_MODIFIERS;
    static {
        StringBuilder sb = new StringBuilder();
        final List<String> unitModifiers = ModifiersUnits.GetModifierList();
        for(String modid : unitModifiers) {
            sb.append(modid);
            sb.append('.');
        }
        UNIT_MODIFIERS = sb.toString();

        JavaTacticalRenderer.clsUtility.initializeLinetypes(1);
        ErrorLogger.setLevel(Level.SEVERE);
    }

    final static IGeoPoint[] COORDS_CIRCLE = new IGeoPoint[]
    {
        new GeoPoint(0.3, 0.3),
        new GeoPoint(0.7, 0.3),
        new GeoPoint(0.7, 0.7),
        new GeoPoint(0.3, 0.7),
    };

    final static IGeoPoint[] COORDS_RECT_ELLIPSE = new IGeoPoint[]
    {
        new GeoPoint(0.2, 0.3),
                new GeoPoint(0.8, 0.3),
                new GeoPoint(0.8, 0.7),
                new GeoPoint(0.2, 0.7),
    };

    final static IGeoPoint[] COORDS_POLYGON = new IGeoPoint[]
    {
        new GeoPoint(0.1, 0.1),
        new GeoPoint(0.7, 0.1),
        new GeoPoint(0.9, 0.9),
        new GeoPoint(0.1, 0.9),
        new GeoPoint(0.1, 0.1),
    };

    final static IGeoPoint[] COORDS_LINESTRING_DEFAULT = new IGeoPoint[]
    {
        new GeoPoint(0.1, 0.5),
        new GeoPoint(0.9, 0.5),
    };

    final static IGeoPoint[] COORDS_LINESTRING_TILT_TWO_POINTS = new IGeoPoint[]
    {
        new GeoPoint(0.1, 0.4),
        new GeoPoint(0.9, 0.6),
    };

    final static IGeoPoint[] COORDS_LINESTRING_TILT_THREE_POINTS = new IGeoPoint[]
    {
        new GeoPoint(0.1, 0.49),
        new GeoPoint(0.5, 0.50),
        new GeoPoint(0.9, 0.51),
    };

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
            symbolDef = filter(SymbolDefTable.getInstance().getSymbolDef(code, RendererSettings.Symbology_2525C));
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
    public Boolean hasModifier(String symbolId, String modifier) {
        return SymbolUtilities.hasModifier(symbolId, modifier);
    }

    @Override
    public List<String> getUnitModifierList() {
        return ModifiersUnits.GetModifierList();
    }

    @Override
    public String getUnitModifierName(String modifierKey) {
        return MilStd2525cModifierConstants.getModifierName(modifierKey);
    }

    @Override
    public List<String> getTGModifierList() {
        return ModifiersTG.GetModifierList();
    }

    @Override
    public String getTGModifierName(String modifierKey) {
        return MilStd2525cModifierConstants.getModifierName(modifierKey);
    }
    @Override
    public String renderMultiPointSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize)
    {
        if(!attributes.containsKey(MilStd2525cAttributes.FillColor)) {
            // XXX - the renderer set a default fill color based on affiliation if no fill color is specified and the
            //       symbol is reported as 3D. if no fill color is specified, explicitly specify as transparent
            attributes = new HashMap<>(attributes);
            attributes.put(MilStd2525cAttributes.FillColor, "00000000");
            attributes.put("fillColor", "00000000");
        }
        if(attributes.containsKey(MilStd2525cAttributes.FillColor)) {
            attributes = new HashMap<>(attributes);
            // XXX - multi-point renderer expects camel-cased attribute
            attributes.put("fillColor", attributes.get(MilStd2525cAttributes.FillColor));
        }
        final String modifiersJson = getModifiersJson(modifiers, attributes);
        synchronized (RenderContextWrapper.class) { 
            RenderContextWrapper.setLabelFont(fontName, fontType, fontSize);
            return SECRenderer.getInstance().RenderMultiPointSymbol(
                // XXX - the renderer will raise a NPE trying to replace the KML folder ID with the specified `id`
                //       string if the symbol is reported as 3D. Pass through the defaulted folder ID when this scenario
                //       is detected upstream
                (id == null && JavaRendererUtilities.is3dSymbol(symbolCode, modifiersJson)) ? "#ID#" : null,
                name,
                description,
                symbolCode,
                controlPoints,
                altitudeMode,
                scale,
                bbox,
                modifiersJson,
                0,
                RendererSettings.Symbology_2525C);
        }
    }

    @Override
    public Bitmap renderSinglePointIcon(String code, Map<String, String> modifiers, Map<String, String> attributes, PointD centerOffset, String fontName, int fontType, int fontSize) {
        final SymbolDef symbolDef = getSymbolDef(code);
        if(symbolDef == null)
            return null;

        // XXX - desktop `mil-sym-renderer` does not implement multi-point icons
        modifiers = new HashMap<>(modifiers);
        modifiers.putAll(attributes);
        PNGInfo icon;
        if(symbolDef.getDrawCategory() == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_POINT) {
            synchronized (RenderContextWrapper.class) {
                RenderContextWrapper.setLabelFont(fontName, fontType, fontSize);
                icon = SECRenderer.getInstance().getMilStdSymbolImage(code, modifiers);
            }
        } else {
            double size = 128d;
            if(attributes.containsKey(MilStd2525cAttributes.PixelSize)) {
                size = Double.parseDouble(attributes.get(MilStd2525cAttributes.PixelSize));
            }

            IGeoPoint[] coords;

            ISymbologyProvider.RendererHints hints = new ISymbologyProvider.RendererHints();
            MilStd2525cSymbologyProvider impl = new MilStd2525cSymbologyProvider();
            hints.shapeType = impl.getDefaultSourceShape(code);
            int pointsLimit = -1;
            switch(hints.shapeType) {
                case Circle:
                    coords = COORDS_CIRCLE;
                    break;
                case Rectangle:
                case Ellipse:
                    coords = COORDS_RECT_ELLIPSE;
                    break;
                case Polygon:
                    coords = COORDS_POLYGON;
                    break;
                case LineString:
                default:
                    TGLight tg = new TGLight();
                    tg.set_SymbolId(code);
                    switch(clsRenderer.getRevDLinetype(tg)) {
                        case 25221000 :
                        case 25222000 :
                        case 25224000 :
                        case 25223000 :
                        case 25225000 :
                            coords = COORDS_LINESTRING_TILT_THREE_POINTS;
                            break;
                        default :
                            switch(symbolDef.getBasicSymbolId()) {
                                case "WO-DHCC----L---" :
                                case "WO-DHHDB---L---" :
                                case "WO-DHDDC---L---" :
                                case "WO-DHDDL---L---" :
                                case "WO-DHPBP---L---" :
                                case "WO-DL-ML---L---" :
                                    coords = COORDS_LINESTRING_TILT_TWO_POINTS;
                                    break;
                                default :
                                    coords = COORDS_LINESTRING_DEFAULT;
                                    break;
                            }
                            break;
                    }
                    break;
            }
            coords = MilStd2525cSymbologyProvider.createControlPoints(
                    symbolDef.getHierarchy(),
                    symbolDef.getDrawCategory(),
                    symbolDef.getMinPoints(),
                    symbolDef.getMaxPoints(),
                    hints,
                    coords,
                    modifiers);


            MilStdSymbol sym = JavaRendererUtilities.createMilstdSymbol(code, null);
            for(Map.Entry<String, String> modifier : modifiers.entrySet()) {
                switch(modifier.getKey()) {
                    case ModifiersTG.AM_DISTANCE:
                    case ModifiersTG.AN_AZIMUTH:
                    case ModifiersTG.X_ALTITUDE_DEPTH:
                        if(modifier.getValue().indexOf(',') > 0) {
                            String[] values = modifier.getValue().split(",");
                            for(int i = 0; i < values.length; i++)
                                sym.setModifier(modifier.getKey(), values[i], i);
                            break;
                        }
                        // fall-through if single value
                    default :
                        sym.setModifier(modifier.getKey(), modifier.getValue());
                        break;
                }
            }
            if(pointsLimit < 0)
                pointsLimit = coords.length;
            ArrayList<Point2D.Double> xy = new ArrayList<>(pointsLimit);
            for(int i = 0; i < pointsLimit; i++)
                xy.add(new Point2D.Double(coords[i].getLatitude(), coords[i].getLongitude()));

            TGLight tg = new TGLight();
            tg.set_SymbolId(code);
            if(clsRenderer.getRevDLinetype(tg) == 23163000 && xy.size() < 4)
                xy.add(xy.get(xy.size()-1));
            sym.setCoordinates(xy);
            icon = getMilStdSymbolImage(JavaRenderer.getInstance(), sym, size, 1d);
        }
        if(icon == null)
            return null;
        if(centerOffset != null) {
            centerOffset.x = icon.getCenterPoint().getX();
            centerOffset.y = icon.getCenterPoint().getY();
        }
        return MarshalManager.marshal(icon.getImage(), BufferedImage.class, Bitmap.class);
    }

    @Override
    public char getAffiliationLetterCode(String symbolCode) {
        final String affiliation = SymbolUtilities.getAffiliation(symbolCode);
        return (affiliation != null && affiliation.length() > 0) ? affiliation.charAt(0) : 'U';
    }

    @Override
    public String setAffiliation(String symbolCode, String affliation) {
        return SymbolUtilities.setAffiliation(symbolCode, affliation);
    }

    @Override
    public MilStdSymbol renderMultiPointSymbolAsMilStdSymbol(String id, String name, String description, String symbolCode, String controlPoints, String altitudeMode, double scale, String bbox, Map<String, String> modifiers, Map<String, String> attributes, String fontName, int fontType, int fontSize) {
        synchronized (RenderContextWrapper.class) { 
            RenderContextWrapper.setLabelFont(fontName, fontType, fontSize);
            return SECRenderer.getInstance().RenderMultiPointAsMilStdSymbol(
                null,
                null,
                null,
                symbolCode,
                controlPoints,
                "clampToGround",
                5869879.2,
                null,
                getModifiersJson(modifiers, attributes),
                RendererSettings.Symbology_2525C);
        }
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

    static String getModifiersJson(Map<String, String> modifiers, Map<String, String> attributes)
    {
        if(modifiers.isEmpty() && attributes.isEmpty())
            return "";
        JSONObject modifiersJson = new JSONObject();
        for(Map.Entry<String, String> entry : modifiers.entrySet()) {
            switch(entry.getKey()) {
                case MilStd2525cModifierConstants.X_ALTITUDE_DEPTH :
                case MilStd2525cModifierConstants.AM_DISTANCE :
                case MilStd2525cModifierConstants.AN_AZIMUTH :
                    // renderer requires value is a JSON array
                    modifiersJson.put(entry.getKey(), toArray(entry.getValue()));
                    break;
                default :
                    modifiersJson.put(entry.getKey(), entry.getValue());
                    break;
            }
        }
        for(Map.Entry<String, String> entry : attributes.entrySet()) {
            modifiersJson.put(entry.getKey(), entry.getValue());
        }
        JSONObject json = new JSONObject();
        json.put("modifiers", modifiersJson);
        return json.toString();
    }

    static JSONArray toArray(String value)
    {
        String[] splits = value.split(",");
        JSONArray array = new JSONArray();
        for(String split : splits)
            array.put(split.trim());
        return array;
    }

    static SymbolDef toSymbolDef(UnitDef unit)
    {
        if(unit == null)
            return null;

        SymbolDef symbol = new SymbolDef();
        symbol.setBasicSymbolId(unit.getBasicSymbolId());
        symbol.setDescription(unit.getDescription());
        symbol.setDrawCategory(SymbolDef.DRAW_CATEGORY_POINT);
        symbol.setHierarchy(unit.getHierarchy());
        symbol.setMinPoints(1);
        symbol.setMaxPoints(1);
        symbol.setModifiers(UNIT_MODIFIERS);
        symbol.setFullPath(unit.getFullPath());
        return filter(symbol);
    }

    static SymbolDef filter(SymbolDef symbol) {
        if(symbol == null)
            return null;

        switch(symbol.getBasicSymbolId()) {
            case "G*M*BCB---****X" :
                symbol.setMinPoints(4);
                symbol.setMaxPoints(4);
                symbol.setDrawCategory(SymbolDef.DRAW_CATEGORY_SUPERAUTOSHAPE);
                break;
            default :
                break;
        }
        return symbol;
    }

    static PNGInfo getMilStdSymbolImage(JavaRenderer jr, MilStdSymbol ms, final double pixelSize, final double geoSize) {
        IPointConversion ipc = new IPointConversion() {
            @Override
            public Point2D PixelsToGeo(Point2D img) {
                return new Point2D.Double(img.getX()/pixelSize*geoSize, img.getY()/pixelSize*geoSize);
            }

            @Override
            public Point2D GeoToPixels(Point2D geo) {
                return new Point2D.Double(geo.getX()/geoSize*pixelSize, geo.getY()/geoSize*pixelSize);
            }
        };
        ImageInfo ii = null;
        PNGInfo pi = null;

        try {
            jr.Render(ms, ipc, (Rectangle2D)null);
            ii = ms.toImageInfo();
        } catch (Exception var6) {
            ErrorLogger.LogException("SECRenderer", "getMilStdSymbolImage(MilStdSymbol)", var6);
        }

        if (ii != null) {
            pi = new PNGInfo(ii);
        }

        return pi;
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

}
