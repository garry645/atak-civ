package gov.tak.platform.symbology.milstd2525;

import static armyc2.c5isr.renderer.utilities.Modifiers.AM_DISTANCE;
import static armyc2.c5isr.renderer.utilities.Modifiers.AN_AZIMUTH;
import static armyc2.c5isr.renderer.utilities.Modifiers.AQ_GUARDED_UNIT;

import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.AtakMapView;
import com.atakmap.map.Globe;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.PointD;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import armyc2.c5isr.renderer.utilities.MSInfo;
import armyc2.c5isr.renderer.utilities.MSLookup;
import armyc2.c5isr.renderer.utilities.MilStdAttributes;
import armyc2.c5isr.renderer.utilities.MilStdSymbol;
import armyc2.c5isr.renderer.utilities.Modifiers;
import armyc2.c5isr.renderer.utilities.SymbolID;
import armyc2.c5isr.renderer.utilities.SymbolUtilities;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.DisplaySettings;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.lang.Parsers;
import gov.tak.platform.system.SystemUtils;


class MilStd2525dSymbologyProviderBase implements ISymbologyProvider2 {
    final static String PURPLE_ICON_FILL = "FFFFA1FF";

    // MIL-STD-2525 A.5.2.3
    final static int CONTEXT_REALITY = 0;
    final static int CONTEXT_EXERCISE = 1;
    final static int CONTEXT_SIMULATION = 2;

    final static int AFFILIATION_PENDING = 0;
    final static int AFFILIATION_UNKNOWN = 1;
    final static int AFFILIATION_ASSUMED_FRIEND = 2;
    final static int AFFILIATION_FRIEND = 3;
    final static int AFFILIATION_NEUTRAL = 4;
    final static int AFFILIATION_SUSPECT_JOKER = 5;
    final static int AFFILIATION_HOSTILE_FAKER = 6;

    final static AtomicInteger generatedSymId = new AtomicInteger(0);


    final static Map<String, Short> milsymStrokeDash = new ConcurrentHashMap<>();
    final static Short SOLID_STROKE_DASH_PATTERN = Short.valueOf((short)0);
    // arbitrary points derived from mil-sym-renderer developer guide
    final static GeoPoint[] STROKE_DASH_QUERY_SHAPE = new GeoPoint[]
    {
            new GeoPoint(8.40185525443334,38.95854638813517),
            new GeoPoint(15.124217101733166,36.694658205882995),
            new GeoPoint(18.49694847529253,40.113591379080155),
            new GeoPoint(8.725267851897936,42.44678226078903),
            new GeoPoint(8.217048055882143,40.76041657400935),
            new GeoPoint(8.40185525443334,38.95854638813517),
    };

    final String DEFAULT_LABEL_FONT_SIZE = "20pt";

    final int version;
    final IMilStd2525dInterop interop;
    MilStd2525dSymbolTable symbolTable;

    MilStd2525dSymbologyProviderBase(int version)
    {
        this.version = version;
        this.interop = new MilStd2525dInterop();
    }

    @Override
    public boolean hasSinglePoint() {
        return true;
    }

    @Override
    public boolean hasMultiPoint() {
        return true;
    }

    @Override
    public String getName() {
        return (version == 13) ? "2525E" : "2525D";
    }

    public @NonNull ShapeType getDefaultSourceShape(String symbolCode)
    {
        ISymbolTable symbols = getSymbolTable();
        ISymbolTable.Symbol milsym = symbols.getSymbol(symbolCode);
        if(milsym == null)
            return ShapeType.LineString;
        if(milsym.getContentMask().contains(ShapeType.Point))
            return ShapeType.Point;
        int drawRule = ((MilStd2525dSymbolTable.SymbolAdapter)milsym).info.getDrawRule();
        // range checks
        if(drawRule >= 800 && drawRule < 900)
            return ShapeType.Rectangle;
        if(drawRule >= 900 && drawRule < 1000)
            return ShapeType.Circle;

        // specific rules
        switch(drawRule) {
            case 701 : // Drawing Ellipse
                return ShapeType.Ellipse;
            case 105 : // Drawing Rectangle
            case 107 :
            case 108 :
            case 117 :
            case 121 :
            case 124 :
            case 212 :
            case 303 :
            case 309 :
            case 315 :
            case 319 :
            case 320 :
            case 322 :
            case 323 :
            case 324 :
            case 326 :
            case 329 :
            case 601 :
                return ShapeType.Rectangle;
            default :
                break;
        }
        return ShapeType.LineString;
    }

    @Override
    public Collection<Modifier> getModifiers(String symbolCode) {
        final Set<String> modifierIds = new HashSet<>(Modifiers.GetUnitModifierList());
        if(symbolCode != null) {
            MSInfo info = MSLookup.getInstance().getMSLInfo(symbolCode);
            if (info == null)
                return Collections.emptySet();
            modifierIds.retainAll(info.getModifiers());
        }

        Collection<Modifier> modifiers = new ArrayList<>(modifierIds.size());
        for(String mod : modifierIds) {
            String[] name = new String[] {Modifiers.getModifierName(mod)};
            if(name[0] == null)
                continue;
            Class<?> parsedValueType = null;
            switch(mod) {
                case Modifiers.X_ALTITUDE_DEPTH:
                    name = new String[] {"Min Altitude Depth", "Max Altitude Depth"};
                    parsedValueType = Number.class;
                    break;
                case Modifiers.C_QUANTITY:
                case Modifiers.Q_DIRECTION_OF_MOVEMENT:
                case Modifiers.Z_SPEED:
                    parsedValueType = Number.class;
                    break;
                case Modifiers.W_DTG_1:
                case Modifiers.W1_DTG_2:
                    parsedValueType = Date.class;
                    break;
                case Modifiers.D_TASK_FORCE_INDICATOR:
                case Modifiers.S_HQ_STAFF_INDICATOR:
                case Modifiers.AB_FEINT_DUMMY_INDICATOR:
                //case Modifiers.AC_INSTALLATION:
                case Modifiers.AH_AREA_OF_UNCERTAINTY:
                case Modifiers.AI_DEAD_RECKONING_TRAILER:
                case Modifiers.AJ_SPEED_LEADER:
                case Modifiers.AK_PAIRING_LINE:
                case Modifiers.AQ_GUARDED_UNIT:
                    parsedValueType = Boolean.class;
                    break;
                case Modifiers.F_REINFORCED_REDUCED:
                case Modifiers.K_COMBAT_EFFECTIVENESS:
                case Modifiers.R_MOBILITY_INDICATOR:
                case Modifiers.AG_AUX_EQUIP_INDICATOR:
                case Modifiers.AL_OPERATIONAL_CONDITION:
                    parsedValueType = Enum.class;
                    break;
                default:
                    break;

            }
            modifiers.add(new Modifier(Modifiers.getModifierLetterCode(mod), name, parsedValueType));
        }
        return modifiers;
    }
    //If sym is 2525D code that is a rectangular version with an Irregular version,
    //switch to the Irregular version for rendering only, for better modifier rendering.
    //xxxx remove if Milsym Android renderer fixes the issue
    String rectangularToIrregular(String sym) {

        switch (SymbolUtilities.getBasicSymbolID(sym)) {
            case "25241902":
            case "25242305":
            case "25242302":
                return sym.substring(0,15) + "1" + sym.substring(16);

        }
        return sym;
    }
    @Override
    public Collection<Feature> renderMultipointSymbol(String code, IGeoPoint[] points, AttributeSet attrs, RendererHints hints)
    {
        // example code derived from https://github.com/missioncommand/mil-sym-android/wiki#33multipoint-symbology

        MSInfo info = MSLookup.getInstance().getMSLInfo(code);
        if(info == null)
            return null;

        code = rectangularToIrregular(code);

        Map<String, String> modifiers = getModifiers(info, attrs, false, null);
        Map<String, String> attributes = new HashMap<>();
        if(hints != null && hints.strokeColor != 0)
            attributes.put(MilStdAttributes.LineColor, Long.toString(hints.strokeColor&0xFFFFFFFFL, 16));
        if(hints != null)
            attributes.put(MilStdAttributes.LineWidth, Integer.toString((int)Math.ceil(hints.strokeWidth)));
        String fontSizeString = (hints != null && hints.fontSize != 0) ? hints.fontSize + "pt" : DEFAULT_LABEL_FONT_SIZE;

        int fontSize = (hints != null) ? hints.fontSize : 0;
        if(fontSize == 0)
            fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        if(hints == null)
            hints = new RendererHints();
        IGeoPoint[] controlPoints = createControlPoints(info, hints, points, modifiers);

        ArrayList<String> modifierList = Modifiers.GetUnitModifierList();
        for(String mod : info.getModifiers()) {
            if (modifierList.contains(mod))
            {
                String postfix = Modifiers.getModifierLetterCode(mod);
                String key = MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + postfix;
                if(!attrs.containsAttribute(key))
                    continue;
                String meta = attrs.getStringAttribute(key);
                modifiers.put(mod, meta);
            }
        }

        String id = attrs.containsAttribute("id") ? attrs.getStringAttribute("id") : null;
        String name = attrs.containsAttribute("name") ? attrs.getStringAttribute("name") : null;
        String description = attrs.containsAttribute("description") ? attrs.getStringAttribute("description") : null;
        String symbolCode = code;
        // lon,lat<space>
        String altitudeMode = "clampToGround";
        if(hints != null && hints.altitudeMode != Feature.AltitudeMode.ClampToGround) {
            // XXX - stringify user specified altitude mode
        }
        final double resolution = !Double.isNaN(hints.resolution) ?
                hints.resolution : computeDefaultResolution(controlPoints);
        double scale = 1d / Globe.getMapScale(resolution,
                SystemUtils.isOsAndroid() ?
                    2000 :
                    DisplaySettings.getDpi());

        double scaleAdjustDistance = Double.NaN;
        String bbox = null;
        if(hints != null && hints.boundingBox != null) {
            bbox = hints.boundingBox.minX + "," + hints.boundingBox.minY + "," + hints.boundingBox.maxX + ","  + hints.boundingBox.maxY;
            IGeoPoint p1 = new GeoPoint(hints.boundingBox.maxY, hints.boundingBox.maxX);
            IGeoPoint p2 = new GeoPoint(hints.boundingBox.minY, hints.boundingBox.minX);

            scaleAdjustDistance = GeoCalculations.distance(p1, p2);
        }

        /// XX very unexpected behavior in this rendering case, fix with something that looks good.
        if(info.getDrawRule() == 326)
        {
            scale /= 10.0;
            bbox = null;
        }

        ArrayList<ArrayList<IGeoPoint>> clippedPoints = null;

        if (this.getDefaultSourceShape(code) == ShapeType.LineString)
        {
            if (scaleAdjustDistance < 500)
            {
                scale /= 5.0;
                bbox = null;
            }
            else if(scaleAdjustDistance < 750)
            {
                scale /= 3.3;
                bbox = null;
            }
            else if (scaleAdjustDistance < 1000)
            {
                scale /= 1.5;
                bbox = null;
            }
            if (scaleAdjustDistance < 1000 && hints != null && hints.boundingBox != null)
                clippedPoints = Utils.clipPoints(controlPoints, hints.boundingBox);
        }

        String kml = null;
        if (clippedPoints == null)
        {
            String controlPointsStr = controlPointsToString(controlPoints);
            kml = interop.renderMultiPointSymbol(id, name, description, symbolCode, controlPointsStr, altitudeMode, scale, bbox, modifiers, attributes, "serif", 0, fontSize);
        }
        else
        {
            kml = "";
            for(int i=0;i < clippedPoints.size(); ++i) {
                String controlPointsStr = controlPointsToString(clippedPoints.get(i).toArray(new IGeoPoint[0]));
                kml += interop.renderMultiPointSymbol(id, name, description, symbolCode, controlPointsStr, altitudeMode, scale, bbox, modifiers, attributes,"serif", 0, fontSize);
            }
        }

        kml = modifyAltitudeReference(kml, attrs, controlPoints);
        String f = "/vsimem/mil-sym-plugin/" + generatedSymId.incrementAndGet() + ".kml";
        try {
            kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                    "<Document>" + kml + "</Document></kml>";
            org.gdal.gdal.gdal.FileFromMemBuffer(f, kml.getBytes(FileSystemUtils.UTF8_CHARSET));

            Short dashPattern = getStrokeDash(code, hints);

            // parse and inject features
            FeatureDataSource.Content features = FeatureDataSourceContentFactory.parse(new File(f), "ogr");
            try {
                if(features == null)
                    return null;

                Collection<Feature> retval = new LinkedList<>();
                while (features.moveToNext(FeatureDataSource.Content.ContentPointer.FEATURE_SET)) {
                    while (features.moveToNext(FeatureDataSource.Content.ContentPointer.FEATURE)) {
                        FeatureDataSource.FeatureDefinition def = features.get();
                        //SYMBOL(id:"",a:-31.000000,s:0.700000);LABEL(c:#00FF007F,w:100.000000)
                        // XXX - workaround bug where KML labels aren't showing based on the
                        //       placemark definition and resulting OGR style string
                        if (def.styleCoding == FeatureDefinition.STYLE_OGR) {
                            String ogr = (String) def.rawStyle;
                            do {
                                // check for empty icon
                                if (!ogr.startsWith("SYMBOL(id:\"\""))
                                    break;
                                // check for label
                                int labelIdx = ogr.indexOf("LABEL(");
                                if (labelIdx < 0)
                                    break;
                                // check for explicit size
                                if (ogr.indexOf("s:", labelIdx) < 0)
                                    ogr = ogr.substring(0, labelIdx + 6) + "s:" + fontSizeString + "," + ogr.substring(labelIdx + 6);
                                // check for explicit text
                                if (ogr.indexOf("t:", labelIdx) > 0)
                                    break;
                                ogr = ogr.substring(0, labelIdx + 6) +
                                        "t:\"" + def.name + "\"," +
                                        ogr.substring(labelIdx + 6);
                                // strip off SYMBOL
                                final int delimiter = ogr.indexOf(';');
                                if (delimiter < 0)
                                    break;
                                ogr = ogr.substring(delimiter + 1);
                            } while (false);
                            def.rawStyle = ogr;
                        }
                        // apply any dashed stroke pattern
                        if(dashPattern != null && def.rawStyle != null) {
                            if(def.styleCoding == FeatureDefinition.STYLE_OGR) {
                                def.rawStyle = Utils.applyDashPattern(FeatureStyleParser.parse2((String)def.rawStyle), dashPattern);
                                def.styleCoding = FeatureDefinition.STYLE_ATAK_STYLE;
                            } else if(def.styleCoding == FeatureDefinition.STYLE_ATAK_STYLE) {
                                def.rawStyle = Utils.applyDashPattern((Style)def.rawStyle, dashPattern);
                            }
                        }
                        retval.add(def.get());
                    }
                }
                return retval;
            } finally {
                if (features != null)
                    features.close();
            }
        } finally {
            org.gdal.gdal.gdal.Unlink(f);
        }
    }

    @Override
    public Bitmap renderSinglePointIcon(String code, AttributeSet attrs, RendererHints hints) {
        if(code == null)
            return null;
        final MSInfo info = MSLookup.getInstance().getMSLInfo(code);
        if(info == null)
            return null;

        int iconSize = (hints != null) ? hints.iconSize : 0;
        if(iconSize == 0)
            iconSize = (int)Math.ceil(32 * GLRenderGlobals.getRelativeScaling());

        int fontSize = (hints != null) ? hints.fontSize : 0;
        if(fontSize == 0)
            fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        Map<String, String> symRenderAttribs = new HashMap<>();
        symRenderAttribs.put(MilStdAttributes.PixelSize, String.valueOf(iconSize));

        // handle civilian unit types
        switch(getAffiliation(code)) {
            case Hostile:
            case SimulatedHostile:
            case Suspect:
            case SimulatedSuspect:
                // MIL-STD-2525D 5.3.3 - hostile and suspect remain red
                break;
            default :
                final String basicId = SymbolUtilities.getBasicSymbolID(code).substring(0, 4);
                switch(basicId) {
                    case "0112": // air / civilian
                    case "0512": // space / civilian
                    case "1111": // land civilian unit / civilian
                    case "1516": // land equipment / civilian
                    case "3014": // sea surface / civilian
                    case "3512": // sea subsurface / civilian
                        symRenderAttribs.put(MilStdAttributes.FillColor, PURPLE_ICON_FILL);
                        break;
                    default :
                        break;
                }
                break;
        }

        PointD centerOffset = new PointD();
        Bitmap bitmap = interop.renderSinglePointIcon(
                code,
                getModifiers(info, attrs, true, (hints != null) ? hints.controlPoint : null),
                symRenderAttribs,
                centerOffset,
                "serif",
                0,
                fontSize);
        if(bitmap != null && hints != null && hints.iconCenterOffset != null) {
            hints.iconCenterOffset.x = centerOffset.x;
            hints.iconCenterOffset.y = centerOffset.y;
        }
        return bitmap;
    }

    @Override
    public ISymbolTable getSymbolTable() {
        if(symbolTable == null)
            symbolTable = new MilStd2525dSymbolTable(version);
        return symbolTable;
    }

    @Override
    public Affiliation getAffiliation(String symbolCode) {
        final int affiliation = SymbolID.getAffiliation(symbolCode);
        final int context = SymbolID.getContext(symbolCode);
        switch(affiliation) {
            case AFFILIATION_PENDING:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.ExercisePending;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedPending;
                    default :
                        return Affiliation.Pending;
                }
            case AFFILIATION_UNKNOWN:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.ExerciseUnknown;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedUnknown;
                    default :
                        return Affiliation.Unknown;
                }
            case AFFILIATION_ASSUMED_FRIEND:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.ExerciseAssumedFriend;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedAssumedFriend;
                    default :
                        return Affiliation.AssumedFriend;
                }
            case AFFILIATION_FRIEND:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.ExerciseFriend;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedFriend;
                    default :
                        return Affiliation.Friend;
                }
            case AFFILIATION_NEUTRAL:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.ExerciseNeutral;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedNeutral;
                    default :
                        return Affiliation.Neutral;
                }
            case AFFILIATION_SUSPECT_JOKER:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.Joker;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedSuspect;
                    default :
                        return Affiliation.Suspect;
                }
            case AFFILIATION_HOSTILE_FAKER:
                switch(context) {
                    case CONTEXT_EXERCISE:
                        return Affiliation.Faker;
                    case CONTEXT_SIMULATION:
                        return Affiliation.SimulatedHostile;
                    default :
                        return Affiliation.Hostile;
                }
        }

        return Affiliation.Unknown;
    }

    @Override
    public String setAffiliation(String symbolCode, Affiliation affiliation) {
        switch(affiliation) {
            case ExercisePending:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_PENDING);
                break;
            case Faker:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_HOSTILE_FAKER);
                break;
            case Joker:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_SUSPECT_JOKER);
                break;
            case Friend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_FRIEND);
                break;
            case Hostile:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_HOSTILE_FAKER);
                break;
            case Neutral:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_NEUTRAL);
                break;
            case Pending:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_PENDING);
                break;
            case Suspect:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_SUSPECT_JOKER);
                break;
            case Unknown:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_UNKNOWN);
                break;
            case AssumedFriend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_REALITY*10) + AFFILIATION_ASSUMED_FRIEND);
                break;
            case ExerciseAssumedFriend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_ASSUMED_FRIEND);
                break;
            case ExerciseFriend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_FRIEND);
                break;
            case ExerciseNeutral:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_NEUTRAL);
                break;
            case ExerciseUnknown:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_EXERCISE*10) + AFFILIATION_UNKNOWN);
                break;
            case SimulatedFriend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_FRIEND);
                break;
            case SimulatedHostile:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_HOSTILE_FAKER);
                break;
            case SimulatedNeutral:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_NEUTRAL);
                break;
            case SimulatedPending:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_PENDING);
                break;
            case SimulatedSuspect:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_SUSPECT_JOKER);
                break;
            case SimulatedUnknown:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_UNKNOWN);
                break;
            case SimulatedAssumedFriend:
                symbolCode = SymbolID.setStandardIdentity(symbolCode, (CONTEXT_SIMULATION*10) + AFFILIATION_ASSUMED_FRIEND);
                break;
            default :
                break;
        }
        return symbolCode;
    }

    /**
     *
     * @param milsym    The symbol code
     * @return  The stroke dashing or {@code null} if the symbol's stroke is not dashed. When
     *          non-{@code null}, the first index represents a lengthwise _on_ segment (in pixels),
     *          the second index a lengthwise _off_ segment with similar alternating on and off
     *          segments for the remainder of the array elements.
     */
    Short getStrokeDash(String milsym, RendererHints hints)
    {
        // NOTE: the caching mechanism n
        final Short cached = milsymStrokeDash.get(milsym);
        if(cached != null)
            return (cached != 0) ? cached : null;


        int fontSize = (hints != null) ? hints.fontSize : 0;
        if(fontSize == 0)
            fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        try {
            Map<String, String> modifiers = new HashMap<>();
            IGeoPoint[] controlPoints = createControlPoints(
                    MSLookup.getInstance().getMSLInfo(milsym),
                    new RendererHints(),
                    STROKE_DASH_QUERY_SHAPE,
                    modifiers);

            Map<String, String> attributes = new HashMap<>();
            attributes.put(MilStdAttributes.LineColor, Long.toString(0xFFFFFFFFL, 16));
            attributes.put(MilStdAttributes.LineWidth, "1");
            // arbitrary values derived from mil-sym-renderer developer guide; should produce a
            // well-formed symbology
            MilStdSymbol mSymbol = interop.renderMultiPointSymbolAsMilStdSymbol(
                    null,
                    null,
                    null,
                    milsym,
                    controlPointsToString(controlPoints != null ? controlPoints : STROKE_DASH_QUERY_SHAPE),
                    "absolute",
                    5869879.2,
                    null,
                    modifiers,
                    attributes, "serif", 0, fontSize);
            Short dashPattern = SOLID_STROKE_DASH_PATTERN;
            final float[] dashArray = this.interop.getStrokeDashArray(mSymbol);
            if(dashArray != null) {
                // check for baked known patterns
                Short known = MilSymRendererDashPatterns.get(dashArray);
                if (known != null) {
                    dashPattern = known;
                } else {
                    // synthesize the pattern mask

                    // determine total length of pattern
                    float totalPatternLength = 0f;
                    for (int i = 0; i < dashArray.length; i++)
                        totalPatternLength += Math.ceil(dashArray[i]);
                    // derive a masked, scaled to most closely represent the pattern, given the
                    // available bits
                    int mask = 0xFFFF;
                    final int maskWidth = (int) Math.max(1, 16 / totalPatternLength);
                    mask >>>= 16 - maskWidth;
                    for (int i = 0; i < dashArray.length; i++) {
                        for (int j = 0; j < (int) Math.ceil(dashArray[i] / maskWidth); j++) {
                            if (i % 2 == 0) {
                                dashPattern = (short) ((dashPattern << maskWidth) | mask);
                            } else {
                                dashPattern = (short) (dashPattern << maskWidth);
                            }
                        }
                    }
                }
            }
            milsymStrokeDash.put(milsym, dashPattern);
        } catch(Throwable ignored) {}
        return null;
    }

    IGeoPoint[] createControlPoints(MSInfo msInfo, RendererHints hints, IGeoPoint[] originalPoints, Map<String, String> modifiers)
    {
        final int drawRule = msInfo.getDrawRule();
        IGeoPoint[] controlPoints = originalPoints;
        IGeoPoint controlPoint = null;
        // first-point-as-last for closed shapes
        //rect with 1 AM distance
        if(drawRule == 801)
        {
            if(originalPoints.length < 4)
                return null;

            controlPoints = new IGeoPoint[2];
            controlPoints[0] = new GeoPoint((originalPoints[0].getLatitude() + originalPoints[1].getLatitude())/2.0,
                    (originalPoints[0].getLongitude() + originalPoints[1].getLongitude())/2.0);
            controlPoints[1] = new GeoPoint((originalPoints[2].getLatitude() + originalPoints[3].getLatitude())/2.0,
                    (originalPoints[2].getLongitude() + originalPoints[3].getLongitude())/2.0);

            GeoPoint p1 = new GeoPoint((originalPoints[1].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[1].getLongitude() + originalPoints[2].getLongitude())/2.0);
            GeoPoint p2 = new GeoPoint((originalPoints[3].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[0].getLongitude())/2.0);

            double distance = GeoCalculations.distance(p1, p2);
            modifiers.put(AM_DISTANCE, Double.toString(distance));
        }
        //xx "Area Targets, Rectangular Target"
        //requires 2 distance one azimuth, may need some continued iteration
        else if(drawRule == 802)
        {
            if(originalPoints.length < 3)
                return null;
            GeoPoint p1 = new GeoPoint(originalPoints[0].getLatitude(),
                    originalPoints[0].getLongitude());
            GeoPoint p2 = new GeoPoint(originalPoints[1].getLatitude(),
                    originalPoints[1].getLongitude());

            GeoPoint p3 = new GeoPoint(originalPoints[2].getLatitude(),
                    originalPoints[2].getLongitude());

            double d1 = GeoCalculations.distance(p1, p2);
            double d2 = GeoCalculations.distance(p1, p3);


            double lat1 = controlPoints[0].getLatitude() - controlPoints[1].getLatitude();
            double lon1 = controlPoints[0].getLongitude() - controlPoints[1].getLongitude();

            double lat2 = controlPoints[0].getLatitude() - controlPoints[2].getLatitude();
            double lon2 = controlPoints[0].getLongitude() - controlPoints[2].getLongitude();

            double deg1 = Math.atan2(lon1, lat1) / Math.PI * 180.0 - 180.0;
            double deg2 = Math.atan2(lon2, lat2) / Math.PI * 180.0 - 180.0;

            modifiers.put(AM_DISTANCE, d1 + "," + d2);
            modifiers.put(AN_AZIMUTH,  "" + deg1);

            controlPoints = new IGeoPoint[1];
            controlPoints[0] = originalPoints[0];
        }
        //rect with 1 AM distance
        else if(drawRule == 803)
        {
            if(originalPoints.length < 4)
                return null;

            GeoPoint p1 = new GeoPoint((originalPoints[1].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[1].getLongitude() + originalPoints[2].getLongitude())/2.0);
            GeoPoint p2 = new GeoPoint((originalPoints[3].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[0].getLongitude())/2.0);

            double distance = GeoCalculations.distance(p1, p2);
            modifiers.put(AM_DISTANCE, Double.toString(distance));

        }
        else if(drawRule == 322 || drawRule == 323 || drawRule == 212 || drawRule == 124 || drawRule == 117 || drawRule == 319)
        {
            if(originalPoints.length < 4)
                return null;

            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

            controlPoints[0] = originalPoints[0];
            controlPoints[2] = p0;
            controlPoints[1] = originalPoints[1];
        }
        else if(drawRule == 329)
        {
            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[2].getLatitude() + originalPoints[3].getLatitude())/2.0,
                    (originalPoints[2].getLongitude() + originalPoints[3].getLongitude())/2.0);

            controlPoints[0] = p0;
            controlPoints[1] = originalPoints[1];
            controlPoints[2] = originalPoints[0];
        }
        else if(drawRule == 303)
        {
            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

            controlPoints[0] = p0;
            controlPoints[1] = originalPoints[0];
            controlPoints[2] = originalPoints[1];
        }
        else if(drawRule == 105)
        {
            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

            controlPoints[2] = p0;
            controlPoints[0] = originalPoints[0];
            controlPoints[1] = originalPoints[1];
        }
        else if(drawRule == 309 || drawRule == 320)
        {
            controlPoints = new IGeoPoint[2];
            GeoPoint p1 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);
            GeoPoint p2 = new GeoPoint(
                    (originalPoints[1].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[1].getLongitude() + originalPoints[0].getLongitude())/2.0);

            controlPoints[0] = p1;
            controlPoints[1] = p2;
        }
        else if(drawRule == 601)
        {
            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[1].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[1].getLongitude() + originalPoints[0].getLongitude())/2.0);
            GeoPoint p1 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);
            GeoPoint p2 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[0].getLongitude())/2.0);

            controlPoints[0] = p0;
            controlPoints[1] = p1;
            controlPoints[2] = p2;
        }
        else if (drawRule == 326 || drawRule == 121 || drawRule == 107)
        {
            if(originalPoints.length < 4)
                return null;

            GeoPoint p0 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

            controlPoints = new IGeoPoint[3];
            controlPoints[0] = p0;
            controlPoints[1] = originalPoints[0];
            controlPoints[2] = originalPoints[1];

            if(drawRule == 326)
                modifiers.put(AM_DISTANCE, Double.toString(100000.0));

        }
        else if (drawRule == 108)
        {
            controlPoints = new IGeoPoint[4];
            controlPoints[0] = originalPoints[2];
            controlPoints[1] = originalPoints[3];
            controlPoints[2] = originalPoints[1];
            controlPoints[3] = originalPoints[0];
        }
        //circular with AM distance
        else if(drawRule == 901 || drawRule == 902)
        {
            if(hints.shapeType == ShapeType.Circle)
            {
                IGeoPoint[] centerAndRadius = getCircleCenterAndRadius(originalPoints);
                if(centerAndRadius == null)
                    return null;
                controlPoints = new IGeoPoint[] { centerAndRadius[0] };
                modifiers.put(AM_DISTANCE, Double.toString(GeoCalculations.distance(centerAndRadius[0], centerAndRadius[1])));
            }
            else
            {
                if(originalPoints.length < 2)
                    return null;

                IGeoPoint p1 = new gov.tak.api.engine.map.coords.GeoPoint(originalPoints[0].getLatitude(),
                        originalPoints[0].getLongitude());
                IGeoPoint p2 = new gov.tak.api.engine.map.coords.GeoPoint(originalPoints[1].getLatitude(),
                        originalPoints[1].getLongitude());

                double distance = GeoCalculations.distance(p1, p2);
                modifiers.put(AM_DISTANCE, Double.toString(distance));
            }
        }
        //range fan with 2 AM distance / 2 AN azimuth
        else if(drawRule == 1001)
        {
            if(originalPoints.length < 3)
                return null;

            GeoPoint p1 = new GeoPoint(originalPoints[0].getLatitude(),
                    originalPoints[0].getLongitude());
            GeoPoint p2 = new GeoPoint(originalPoints[1].getLatitude(),
                    originalPoints[1].getLongitude());

            GeoPoint p3 = new GeoPoint(originalPoints[2].getLatitude(),
                    originalPoints[2].getLongitude());

            double d1 = GeoCalculations.distance(p1, p2);
            double d2 = GeoCalculations.distance(p1, p3);


            double lat1 = controlPoints[0].getLatitude() - controlPoints[1].getLatitude();
            double lon1 = controlPoints[0].getLongitude() - controlPoints[1].getLongitude();

            double lat2 = controlPoints[0].getLatitude() - controlPoints[2].getLatitude();
            double lon2 = controlPoints[0].getLongitude() - controlPoints[2].getLongitude();

            double deg1 = Math.atan2(lon1, lat1) / Math.PI * 180.0 - 180.0;
            double deg2 = Math.atan2(lon2, lat2) / Math.PI * 180.0 - 180.0;

            modifiers.put(AM_DISTANCE, d1 + "," + d2);
            modifiers.put(AN_AZIMUTH,  deg1 + "," + deg2);

            controlPoints = new IGeoPoint[1];
            controlPoints[0] = originalPoints[0];
        }
        //ellipse
//        else if(drawRule == 701 && shapeType == ShapeType.Ellipse)
//        {
//            Ellipse ellipse = ((DrawingEllipse) subject).getOutermostEllipse();
//            controlPoints = new IGeoPoint[1];
//            controlPoints[0] = ellipse.getCenter().get();
//            modifiers.append(AM_DISTANCE, ellipse.getWidth() / 2.0 + "," + ellipse.getLength() / 2.0);
//            modifiers.append(AN_AZIMUTH,  "" + -ellipse.getAngle());
//        }
        else if(drawRule == 501 || drawRule == 502)
        {
            controlPoint = (hints.controlPoint == null) ?
                    computeControlPoint(msInfo, originalPoints) : hints.controlPoint;
            if(controlPoint != null)
            {
                IGeoPoint[] temp = new IGeoPoint[controlPoints.length+1];
                System.arraycopy(controlPoints, 0, temp, 0, controlPoints.length);
                temp[temp.length-1] = controlPoint;
                controlPoints = temp;
            }
        }
        else if(hints.shapeType == ShapeType.Circle && (drawRule == 115 || drawRule == 116))
        {
            IGeoPoint[] centerAndRadius = getCircleCenterAndRadius(originalPoints);
            if(centerAndRadius == null)
                return null;

            controlPoint = (hints.controlPoint == null) ?
                    computeControlPoint(msInfo, originalPoints) : hints.controlPoint;
            if(controlPoint != null) {
                double lat = centerAndRadius[0].getLatitude() - controlPoint.getLatitude();
                double lon = centerAndRadius[0].getLongitude() - controlPoint.getLongitude();

                double deg = Math.atan2(lon, lat) / Math.PI * 180.0 - 180.0;

                centerAndRadius[1] = GeoCalculations.pointAtDistance(
                        centerAndRadius[0],
                        deg,
                        GeoCalculations.distance(centerAndRadius[0], centerAndRadius[1]));
            }
            controlPoints =  centerAndRadius;
        }
        hints.controlPoint = controlPoint;

        return controlPoints;
    }

    static IGeoPoint[] getCircleCenterAndRadius(IGeoPoint[] originalPoints)
    {
        if(originalPoints.length < 2)
            return null;
        IGeoPoint ca = originalPoints[0];
        IGeoPoint cb = originalPoints[1];
        double diameter = GeoCalculations.distance(ca, cb);
        for(int i = 2; i < originalPoints.length; i++) {
            double d = GeoCalculations.distance(ca, originalPoints[i]);
            if(d > diameter) {
                cb = originalPoints[i];
                diameter = d;
            }
        }
        return new IGeoPoint[] { GeoCalculations.midpoint(ca, cb), cb };
    }

    static IGeoPoint computeControlPoint(MSInfo info, IGeoPoint[] points)
    {
        if(info.getDrawRule() == 501 || info.getDrawRule() == 502)
        {
            if(points.length < 2)
                return null;

            GeoPoint p0 = new GeoPoint
                    (points[0].getLatitude(), points[0].getLongitude() );
            GeoPoint p1 = new GeoPoint
                    (points[1].getLatitude(), points[1].getLongitude() );

            double distance = GeoCalculations.distance(p0, p1);
            double az = GeoCalculations.bearing(p0, p1);

            IGeoPoint atd = GeoCalculations.pointAtDistance(p0, p1, 1d / 3d);
            return GeoCalculations.pointAtDistance(atd, az+90d, distance/3d);
        }
        else if(info.getDrawRule() == 115 || info.getDrawRule() == 116)
        {
            IGeoPoint[] centerAndRadius = getCircleCenterAndRadius(points);
            if(centerAndRadius == null)
                return null;
            return GeoCalculations.pointAtDistance(
                    centerAndRadius[0],
                    0.0,
                    GeoCalculations.distance(centerAndRadius[0], centerAndRadius[1])*1.1);
        }
        return null;
    }

    static String controlPointsToString(IGeoPoint[] controlPoints)
    {
        StringBuilder controlPointsStr = new StringBuilder();
        if(controlPoints.length > 0) {
            controlPointsStr.append(controlPoints[0].getLongitude());
            controlPointsStr.append(',');
            controlPointsStr.append(controlPoints[0].getLatitude());
            for (int i = 1; i < controlPoints.length; i++) {
                controlPointsStr.append(' ');
                controlPointsStr.append(controlPoints[i].getLongitude());
                controlPointsStr.append(',');
                controlPointsStr.append(controlPoints[i].getLatitude());
            }
        }
        return controlPointsStr.toString();
    }

    Map<String, String> getModifiers(MSInfo info, AttributeSet attrs, boolean singlePoint, IGeoPoint referencePoint)
    {
        Map<String, String> modifiers = new HashMap<>();
        if(attrs != null)
        {
            Set<String> modifierList = new HashSet<>(Modifiers.GetUnitModifierList());
            final ArrayList<String> mods = info.getModifiers();
            for (String mod : mods)
            {
                if (modifierList.contains(mod))
                {
                    final String modid = Modifiers.getModifierLetterCode(mod);
                    final String key = MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + modid;
                    if (!attrs.containsAttribute(key))
                        continue;
                    String meta = attrs.getStringAttribute(key);
                    // Table E-2
                    if(mod.equals(AQ_GUARDED_UNIT)) {
                        switch(meta) {
                            case "true" :
                            case "BG" :
                                meta = "BG";
                                break;
                            default :
                                continue;
                        }
                    }
                    // single point modifier rendering is GIGO; perform reference+unit
                    // conversion
                    if(singlePoint && modid.equals(MilStd2525cModifierConstants.X_ALTITUDE_DEPTH) && !meta.isEmpty()) {
                        final String[] altitudes = formatAltitude(
                                attrs,
                                referencePoint != null ?
                                        new IGeoPoint[] {referencePoint} : new IGeoPoint[0]);
                        if(altitudes == null) {
                            continue;
                        } else {
                            StringBuilder x = new StringBuilder();
                            for(String alt : altitudes) {
                                if(alt == null)
                                    continue;
                                if(x.length() > 0)
                                    x.append(',');
                                x.append(alt);
                            }
                            meta = (x.length() > 0) ? x.toString() : null;
                        }
                    }

                    modifiers.put(mod, meta);
                }
            }
        }

        return modifiers;
    }

    @Override
    public Status getStatus(String code) {
        switch(SymbolID.getStatus(code)) {
            case 1:
                return Status.PlannedAnticipatedSuspect;
            case 2:
                return Status.PresentFullyCapable;
            case 3:
                return Status.PresentDamaged;
            case 4:
                return Status.PresentDestroyed;
            case 5:
                return Status.PresentFullToCapacity;
            default:
                return Status.Present;
        }
    }

    @Override
    public String setStatus(String code, Status status) {
        int stat = 0;
        switch(status) {
            case PlannedAnticipatedSuspect:
                stat = 1;
                break;
            case PresentFullyCapable:
                stat = 2;
                break;
            case PresentDamaged:
                stat = 3;
                break;
            case PresentDestroyed:
                stat = 4;
                break;
            case PresentFullToCapacity:
                stat = 5;
                break;
        }

        return SymbolID.setStatus(code, stat);

    }

    @Override
    public Amplifier getAmplifier(String symbolCode) {
        final int amplifier =SymbolID.getAmplifierDescriptor(symbolCode);
        switch(amplifier) {
            case 0:
                return null;
            case 11:
                return Amplifier.Team_Crew;
            case 12 :
                return Amplifier.Squad;
            case 13:
                return Amplifier.Section;
            case 14 :
                return Amplifier.Platoon_Detachment;
            case 15 :
                return Amplifier.Company_Battery_Troop;
            case 16 :
                return Amplifier.Battalion_Squadron;
            case 17 :
                return Amplifier.Regiment_Group;
            case 18 :
                return Amplifier.Brigade;
                //Echelon at division and above 2
            case 21 :
                return Amplifier.Division;
            case 22 :
                return Amplifier.Corps_MEF;
            case 23 :
                return Amplifier.Army;
            case 24 :
                return Amplifier.ArmyGroup_Front;
            case 25 :
                return Amplifier.Region_Theater;
            case 26 :
                return Amplifier.Command;
            case 31:
                return Amplifier.WheeledLimitedCrossCountry;
            case 32 :
                return Amplifier.WheeledCrossCountry;
            case 33 :
                return Amplifier.Tracked;
            case 34 :
                return Amplifier.WheeledAndTrackedCombination;
            case 35 :
                return Amplifier.Towed;
            case 36 :
                return Amplifier.Rail;
            case 37 :
                return Amplifier.PackAnimals;
            case 41 :
                return Amplifier.OverSnow;
            case 42 :
                return Amplifier.Sled;
            case 51 :
                return Amplifier.Barge;
            case 52 :
                return Amplifier.Amphibious;
            case 61 :
                return Amplifier.ShortTowedArray;
            case 62 :
                return Amplifier.LongTowedArray;
            default :
                return null;
        }
    }

    @Override
    public String setAmplifier(String symbolCode, Amplifier amplifier) {
        if(amplifier == null)
            return SymbolID.setAmplifierDescriptor(symbolCode, 0);
        switch(amplifier) {
            case Team_Crew :
                return SymbolID.setAmplifierDescriptor(symbolCode, 11);
            case Squad :
                return SymbolID.setAmplifierDescriptor(symbolCode, 12);
            case Section:
                return SymbolID.setAmplifierDescriptor(symbolCode, 13);
            case Platoon_Detachment :
                return SymbolID.setAmplifierDescriptor(symbolCode, 14);
            case Company_Battery_Troop :
                return SymbolID.setAmplifierDescriptor(symbolCode, 15);
            case Battalion_Squadron :
                return SymbolID.setAmplifierDescriptor(symbolCode, 16);
            case Regiment_Group :
                return SymbolID.setAmplifierDescriptor(symbolCode, 17);
            case Brigade :
                return SymbolID.setAmplifierDescriptor(symbolCode, 18);
            case Division:
                return SymbolID.setAmplifierDescriptor(symbolCode, 21);
            case Corps_MEF:
                return SymbolID.setAmplifierDescriptor(symbolCode, 22);
            case Army:
                return SymbolID.setAmplifierDescriptor(symbolCode, 23);
            case ArmyGroup_Front:
                return SymbolID.setAmplifierDescriptor(symbolCode, 24);
            case Region_Theater:
                return SymbolID.setAmplifierDescriptor(symbolCode, 25);
            case Command:
                return SymbolID.setAmplifierDescriptor(symbolCode, 26);
            case WheeledLimitedCrossCountry:
                return SymbolID.setAmplifierDescriptor(symbolCode, 31);
            case WheeledCrossCountry:
                return SymbolID.setAmplifierDescriptor(symbolCode, 32);
            case Tracked:
                return SymbolID.setAmplifierDescriptor(symbolCode, 33);
            case WheeledAndTrackedCombination:
                return SymbolID.setAmplifierDescriptor(symbolCode, 34);
            case Towed:
                return SymbolID.setAmplifierDescriptor(symbolCode, 35);
            case Rail:
                return SymbolID.setAmplifierDescriptor(symbolCode, 36);
            case PackAnimals:
                return SymbolID.setAmplifierDescriptor(symbolCode, 37);
            case OverSnow:
                return SymbolID.setAmplifierDescriptor(symbolCode, 41);
            case Sled:
                return SymbolID.setAmplifierDescriptor(symbolCode, 42);
            case Barge:
                return SymbolID.setAmplifierDescriptor(symbolCode, 51);
            case Amphibious:
                return SymbolID.setAmplifierDescriptor(symbolCode, 52);
            case ShortTowedArray:
                return SymbolID.setAmplifierDescriptor(symbolCode, 61);
            case LongTowedArray:
                return SymbolID.setAmplifierDescriptor(symbolCode, 62);
            default :
                return null;
        }
    }

    @Override
    public int getHeadquartersTaskForceDummyMask(String symbolCode) {
        switch(SymbolID.getHQTFD(symbolCode)) {
            case 0 :
                return 0;
            case 1 :
                return MASK_DUMMY_FEINT;
            case 2 :
                return MASK_HEADQUARTERS;
            case 3 :
                return MASK_DUMMY_FEINT|MASK_HEADQUARTERS;
            case 4 :
                return MASK_TASKFORCE;
            case 5 :
                return MASK_DUMMY_FEINT|MASK_TASKFORCE;
            case 6 :
                return MASK_HEADQUARTERS|MASK_TASKFORCE;
            case 7 :
                return MASK_DUMMY_FEINT|MASK_HEADQUARTERS|MASK_TASKFORCE;
            default :
                return 0;
        }
    }

    @Override
    public String setHeadquartersTaskForceDummyMask(String symbolCode, int mask) {
        mask &= MASK_DUMMY_FEINT|MASK_TASKFORCE|MASK_HEADQUARTERS;
        switch(mask) {
            case 0 :
                return SymbolID.setHQTFD(symbolCode, 0);
            case MASK_DUMMY_FEINT :
                return SymbolID.setHQTFD(symbolCode, 1);
            case MASK_HEADQUARTERS :
                return SymbolID.setHQTFD(symbolCode, 2);
            case MASK_DUMMY_FEINT|MASK_HEADQUARTERS :
                return SymbolID.setHQTFD(symbolCode, 3);
            case MASK_TASKFORCE:
                return SymbolID.setHQTFD(symbolCode, 4);
            case MASK_DUMMY_FEINT|MASK_TASKFORCE :
                return SymbolID.setHQTFD(symbolCode, 5);
            case MASK_HEADQUARTERS|MASK_TASKFORCE :
                return SymbolID.setHQTFD(symbolCode, 6);
            case MASK_DUMMY_FEINT|MASK_HEADQUARTERS|MASK_TASKFORCE :
                return SymbolID.setHQTFD(symbolCode, 7);
            default :
                return SymbolID.setHQTFD(symbolCode, 0);
        }
    }
    static String[] formatAltitude(AttributeSet attrs, IGeoPoint[]controlPoints) {
        String X = attrs != null ? attrs.getStringAttribute("milsym.modifier.X", null) : null;
        if(X == null)
            return null;

        String []split = X.split(",");
        if(split.length < 1)
            return null;
        final String units = formatAltitudeUnits(attrs, controlPoints);
        final String[] altitudes = new String[split.length];
        for(int i = 0; i < split.length; i++) {
            if(FileSystemUtils.isEmpty(split[i]))
                continue;
            double altitude = Parsers.parseDouble(split[i], Double.NaN);
            if(Double.isNaN(altitude))
                continue;
            altitude = displayAltitudeFromMSLMeters(altitude, attrs, controlPoints);
            altitudes[i] = String.format("%.2f", altitude) + " " + units;
        }
        return altitudes;
    }

    //Modify MIN / MAX ALT in multipoint shapes.  Set the correct units (M / ft) and
    // the correct altitude reference (MSL / AGL / HAE)
    static String modifyAltitudeReference(String kml, AttributeSet attrs, IGeoPoint[]controlPoints) {
        final String[] minMax = formatAltitude(attrs, controlPoints);
        if(minMax == null)
            return kml;

        boolean hasMin = minMax.length > 0 && !FileSystemUtils.isEmpty(minMax[0]);
        boolean hasMax = minMax.length > 1 && !FileSystemUtils.isEmpty(minMax[1]);

        if(hasMin)
            kml = kml.replaceAll("\\[MIN ALT: (.*?)\\]",
                    "[MIN ALT: " + minMax[0]  + "]");
        if(hasMax)
            kml = kml.replaceAll("\\[MAX ALT: (.*?)\\]",
                    "[MAX ALT: "  + minMax[1]  + "]");

        return kml;
    }
    private static IGeoPoint getControlPoint(IGeoPoint[]controlPoints) {
        if(controlPoints == null || controlPoints.length < 1)
            return null;

        if(controlPoints.length == 1)
            return controlPoints[0];

        int pointCount = controlPoints.length;
        if(controlPoints[0].getLatitude() == controlPoints[pointCount-1].getLatitude()
                && controlPoints[0].getLongitude() == controlPoints[pointCount-1].getLongitude()) {

            pointCount--;
        }

        return GeoCalculations.computeAverage(controlPoints, 0, pointCount, true);
    }

    private static double displayAltitudeFromMSLMeters(double mslMeters, AttributeSet attrs, IGeoPoint[]controlPoints) {
        String altReference = attrs != null && attrs.containsAttribute("milsym.altitudeReference") ? attrs.getStringAttribute("milsym.altitudeReference") : "MSL";
        String altitudeUnits = attrs != null && attrs.containsAttribute("milsym.altitudeUnits") ? attrs.getStringAttribute("milsym.altitudeUnits") : "feet";

        // if there is no reference, it won't be possible to convert MSL=>[HAE,AGL]
        if(controlPoints == null || controlPoints.length == 0)
            altReference = "MSL";

        double displayAlt = mslMeters;
        if (!altReference.equals("MSL")) {
            IGeoPoint referencePoint = getControlPoint(controlPoints);
            if (referencePoint == null) {
                return Double.NaN;
            }

            final double lat = referencePoint.getLatitude();
            final double lng = referencePoint.getLongitude();

            if (altReference.equals("HAE")) {
                displayAlt = GeoCalculations.mslToHae(lat, lng, displayAlt);
            } else if (altReference.equals("AGL")) {
                // get local elevation (as MSL)
                double elevation = GeoCalculations.haeToMsl(lat, lng, ElevationManager.getElevation(lat, lng, null));
                displayAlt -= elevation;
            }
        }
        if(!altitudeUnits.equals("meters")) {
            return displayAlt * ConversionFactors.METERS_TO_FEET;
        }

        return displayAlt;
    }

    private static String formatAltitudeUnits(AttributeSet attrs, IGeoPoint[] controlPoints) {
        String altitudeUnits = attrs != null && attrs.containsAttribute("milsym.altitudeUnits") ? attrs.getStringAttribute("milsym.altitudeUnits") : "feet";
        String altReference = attrs != null && attrs.containsAttribute("milsym.altitudeReference") ? attrs.getStringAttribute("milsym.altitudeReference") : "MSL";
        // if there is no reference, it won't be possible to convert MSL=>[HAE,AGL]
        if(controlPoints == null || controlPoints.length == 0)
            altReference = "MSL";

        String unit = altitudeUnits.equals("meters") ? "m" : "ft";

        return unit + " " + altReference;
    }

    /** computes a default resolution for the renderer given the bounding box of the control points */
    static double computeDefaultResolution(IGeoPoint[] points)
    {
        final double zoom0 = OSMUtils.mapnikTileResolution(0);
        if (points.length < 2) return zoom0;
        double minX = points[0].getLongitude();
        double minY = points[0].getLatitude();
        double maxX = minX;
        double maxY = minY;
        for (int i = 1; i < points.length; i++)
        {
            final double x = points[i].getLongitude();
            final double y = points[i].getLatitude();
            if (x < minX)
                minX = x;
            else if (x > maxX) maxX = x;
            if (y < minY)
                minY = y;
            else if (y > maxY) maxY = y;
        }
        double dx = maxX - minX;
        if (dx > 180d) dx -= 180d;
        final double dy = maxY - minY;
        final double gsdy = (dy != 0) ? OSMUtils.mapnikTileResolution((int) Math.ceil(Math.log(180.0 / dy) / Math.log(2.0))) : zoom0;
        final double gsdx = (dx != 0) ? OSMUtils.mapnikTileResolution((int) Math.ceil(Math.log(180.0 / dx) / Math.log(2.0))) : zoom0;
        final double fudge = 2.0d;
        return Math.min(gsdx, gsdy) / fudge;
    }
}
