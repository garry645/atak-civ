package gov.tak.platform.symbology.milstd2525;

import android.util.Base64;

import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.lang.Objects;
import com.atakmap.map.AtakMapView;
import com.atakmap.map.Globe;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLRenderGlobals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
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
import gov.tak.platform.marshal.MarshalManager;

abstract class MilStd2525cSymbologyProviderBase<SymbolDef, MilStdSymbol, ShapeInfo> implements ISymbologyProvider2
{
    final static String PURPLE_ICON_FILL = "FFFFA1FF";
    final static AtomicInteger generatedSymId = new AtomicInteger(0);


    final static Map<String, String> ICON_ANNOTATED_SYMBOLS = new HashMap<>();
    static {
        ICON_ANNOTATED_SYMBOLS.put("2.X.3.4.4", "G*M*NZ----****X"); // nuclear
        ICON_ANNOTATED_SYMBOLS.put("2.X.3.4.5", "G*M*NEB---****X"); // bio
        ICON_ANNOTATED_SYMBOLS.put("2.X.3.4.6", "G*M*NEC---****X"); // chemical
    }
    final static Map<String, String> annotationSymbolPlacemarks = new ConcurrentHashMap<>();
    final static String ANNOTATED_SYMBOL_PLACEMARK_TEMPLATE =
        "\t<Style id=\"s_milsym-icon\">\n" +
        "\t\t<IconStyle>\n" +
        "\t\t\t<scale>2.2</scale>\n" +
        "\t\t\t<Icon><href>##ICONURL##</href></Icon>\n" +
        "\t\t</IconStyle>\n" +
        "\t\t<LabelStyle><scale>0</scale></LabelStyle>" +
        "\t</Style>\n" +
        "\t<Placemark>\n" +
        "\t\t<name>##NAME##</name>\n" +
        "\t\t<styleUrl>#s_milsym-icon</styleUrl>\n" +
        "\t\t<Point><coordinates>##CENTROID##,0</coordinates></Point>\n" +
        "\t</Placemark>";

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

    MilStd2525cSymbolTable symbolTable;

    final IMilStd2525cInterop<SymbolDef, MilStdSymbol, ShapeInfo> interop;

    MilStd2525cSymbologyProviderBase(IMilStd2525cInterop<SymbolDef, MilStdSymbol, ShapeInfo> interop)
    {
        this.interop = interop;
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
        return "2525C";
    }

    public @NonNull ShapeType getDefaultSourceShape(String symbolCode)
    {
        ISymbolTable symbols = getSymbolTable();
        ISymbolTable.Symbol milsym = symbols.getSymbol(symbolCode);
        if(milsym == null)
            return ShapeType.LineString;
        final SymbolDef msInfo = interop.getSymbolDef(symbolCode);
        int drawRule = interop.getDrawCategory(msInfo);

        final int minPoints = interop.getMinPoints(msInfo);
        final int maxPoints = interop.getMaxPoints(msInfo);
        final String hierarchy = interop.getHierarchy(msInfo);

        switch(drawRule) {
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_POINT:
                return ShapeType.Point;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_LINE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ARROW:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ROUTE:
                return ShapeType.LineString;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_POLYGON:
                return ShapeType.Polygon;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_SUPERAUTOSHAPE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTARROW:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTLINE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_RECTANGULAR_PARAMETERED_AUTOSHAPE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWO_POINT_RECT_PARAMETERED_AUTOSHAPE :
                return ShapeType.Rectangle;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE:
                // XXX - Minimum Safe Distance Zones 2.X.3.4.1

                // special cases
                if(hierarchy.equals("2.X.3.2.2.6")) // lane
                    return ShapeType.Rectangle;
                else if(hierarchy.equals("2.X.6.4.2")) // bearing line accoustic
                    return ShapeType.Rectangle;
                // general cases based on point count
                if(minPoints == 2 && maxPoints == 2)
                    return ShapeType.Circle;
                else if(minPoints == 3 && maxPoints == 3)
                    return ShapeType.Rectangle;
                else if(minPoints == 3 && maxPoints == 4)
                    return ShapeType.Rectangle;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_CIRCULAR_PARAMETERED_AUTOSHAPE:
                return ShapeType.Circle;
            default :
                break;
        }

        return ShapeType.LineString;
    }

    @Override
    public Collection<Modifier> getModifiers(String symbolCode) {
        final Set<String> modifierIds = new HashSet<>(interop.getUnitModifierList());
        modifierIds.addAll(interop.getTGModifierList());
        // remove the following modifiers as they are computed and added
        // behind the scenes during control point creation and are therefore
        // not user modifiable
        modifierIds.removeAll(Arrays.asList("AM", "AN"));
        if(symbolCode != null) {
            SymbolDef info = interop.getSymbolDef(symbolCode);
            if (info == null)
                return Collections.emptySet();
            //T.W.W1.
            final String s = interop.getModifiers(info);
            if(s == null || s.equals(""))
                return Collections.emptySet();

            Set<String> infoMods = new HashSet<>();
            String[] mods = s.split("\\.");
            for(int i = 0; i < mods.length; i++) {
                // SymbolDef.getModifiers() is valid for Tactical Graphics (symbol codes starting with 'G')
                // and METOCs (symbol codes starting with 'W') for UnitDef (single point symbols) the
                // SymbolUtilities.hasModifier() function can be used to further refine what modifiers
                // are actually valid for a particular symbol
                if (symbolCode.startsWith("G") || symbolCode.startsWith("W") ||
                    interop.hasModifier(symbolCode, mods[i])) {
                    infoMods.add(mods[i]);
                }
            }
            modifierIds.retainAll(infoMods);
        }

        Collection<Modifier> modifiers = new ArrayList<>(modifierIds.size());
        for(String mod : modifierIds) {
            String[] name = new String[] {interop.getUnitModifierName(mod)};
            if(name[0] == null) {
                // if it's not a Unit Modifier try a TG Modifier
                name = new String[] {interop.getTGModifierName(mod)};
                if (name[0] == null)
                    continue;
            }
            Class<?> parsedValueType = null;
            if(mod.equals(MilStd2525cModifierConstants.X_ALTITUDE_DEPTH)) {
                name = new String[]{"Min Altitude Depth", "Max Altitude Depth"};
                parsedValueType = Number.class;
            } else if(mod.equals(MilStd2525cModifierConstants.Q_DIRECTION_OF_MOVEMENT) || mod.equals(MilStd2525cModifierConstants.Z_SPEED)) {
                parsedValueType = Number.class;
            } else if(mod.equals(MilStd2525cModifierConstants.W_DTG_1) || mod.equals(MilStd2525cModifierConstants.W1_DTG_2)) {
                parsedValueType = Date.class;
            } else if(mod.equals(MilStd2525cModifierConstants.D_TASK_FORCE_INDICATOR) ||
                      mod.equals(MilStd2525cModifierConstants.S_HQ_STAFF_OR_OFFSET_INDICATOR) ||
                      mod.equals(MilStd2525cModifierConstants.AB_FEINT_DUMMY_INDICATOR) ||
                      mod.equals(MilStd2525cModifierConstants.AC_INSTALLATION) ||
                      mod.equals(MilStd2525cModifierConstants.AH_AREA_OF_UNCERTAINTY) ||
                      mod.equals(MilStd2525cModifierConstants.AI_DEAD_RECKONING_TRAILER) ||
                      mod.equals(MilStd2525cModifierConstants.AJ_SPEED_LEADER) ||
                      //mod.equals(MilStd2525cModifierConstants.AQ_GUARDED_UNIT) ||
                      mod.equals(MilStd2525cModifierConstants.AK_PAIRING_LINE)) {
                parsedValueType = Boolean.class;
            } else if(mod.equals(MilStd2525cModifierConstants.F_REINFORCED_REDUCED) ||
                    mod.equals(MilStd2525cModifierConstants.R_MOBILITY_INDICATOR) ||
                    mod.equals(MilStd2525cModifierConstants.K_COMBAT_EFFECTIVENESS) ||
                    mod.equals(MilStd2525cModifierConstants.AG_AUX_EQUIP_INDICATOR) ||
                    mod.equals(MilStd2525cModifierConstants.AL_OPERATIONAL_CONDITION)) {
                parsedValueType = Enum.class;
            }

            modifiers.add(new Modifier(mod, name, parsedValueType));
        }
        return modifiers;
    }

    //If sym is 2525C code that is a rectangular version with an Irregular version,
    //switch to the Irregular version for rendering only, for better modifier rendering.
    //xxxx remove if Milsym Android renderer fixes the issue
    String rectangularToIrregular(String sym) {

        if(sym == null || sym.length() <8 ) {
            return sym;
        }
        sym = sym.toUpperCase();
        if(sym.charAt(7) != 'R')
            return sym;

        String sub = sym.substring(4,8);

        switch (sub) {
            case "AZXR":
            case "ACFR":
            case "ACAR":
            case "ACSR":
            case "ACNR":
            case "ACDR":
            case "AKBR":
            case "ACRR":
            case "ACZR":
            case "AZCR":
            case "ACBR":
            case "AZFR":
            case "AKPR":
            case "ACER":
            case "ACVR":
            case "AZIR":
                StringBuilder builder = new StringBuilder(sym);
                builder.setCharAt(7, 'I');
                return builder.toString();
        }

        return sym;
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

    @Override public @Nullable Collection<Feature> renderMultipointSymbol(@NonNull String code, @NonNull IGeoPoint[] points, @Nullable AttributeSet attrs, @Nullable RendererHints hints)
    {
        if(points.length == 0)
            return Collections.emptySet();

        code = rectangularToIrregular(code);

        // example code derived from https://github.com/missioncommand/mil-sym-android/wiki#33multipoint-symbology
        SymbolDef info = interop.getSymbolDef(code);
        if(info == null)
            return null;

        Map<String, String> modifiers = getModifiers(info, attrs, false, null);
        Map<String, String> attributes = new HashMap<>();
        if(hints != null && hints.strokeColor != 0)
            attributes.put(MilStd2525cAttributes.LineColor, Long.toString(hints.strokeColor&0xFFFFFFFFL, 16));
        if(hints != null)
            attributes.put(MilStd2525cAttributes.LineWidth, Integer.toString((int)Math.ceil(hints.strokeWidth)));
        String fontSizeString = (hints != null && hints.fontSize != 0) ? hints.fontSize + "pt" : DEFAULT_LABEL_FONT_SIZE;

        int fontSize = (hints != null) ? hints.fontSize : 0;
        if(fontSize == 0)
            fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        if(hints == null)
            hints = new RendererHints();
        IGeoPoint[] controlPoints = createControlPoints(info, hints, points, modifiers);

        String id = attrs != null && attrs.containsAttribute("id") ? attrs.getStringAttribute("id") : null;
        String name = attrs != null && attrs.containsAttribute("name") ? attrs.getStringAttribute("name") : null;
        String description = attrs != null && attrs.containsAttribute("description") ? attrs.getStringAttribute("description") : null;
        String symbolCode = code;
        // lon,lat<space>
        String altitudeMode = "clampToGround";
        if(hints.altitudeMode != Feature.AltitudeMode.ClampToGround) {
            // XXX - stringify user specified altitude mode
        }
        final double resolution = !Double.isNaN(hints.resolution) ?
                hints.resolution : computeDefaultResolution(controlPoints);
        double scale = 1d / Globe.getMapScale(resolution, DisplaySettings.getDpi());

        double scaleAdjustDistance = Double.NaN;
        String bbox = null;
        if(hints.boundingBox != null) {
            bbox = hints.boundingBox.minX + "," + hints.boundingBox.minY + "," + hints.boundingBox.maxX + ","  + hints.boundingBox.maxY;
            IGeoPoint p1 = new GeoPoint(hints.boundingBox.maxY, hints.boundingBox.maxX);
            IGeoPoint p2 = new GeoPoint(hints.boundingBox.minY, hints.boundingBox.minX);

            scaleAdjustDistance = GeoCalculations.distance(p1, p2);
        }

        /// XX very unexpected behavior in this rendering case, fix with something that looks good.
        if(interop.getDrawCategory(info) == 326)
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
            if (scaleAdjustDistance < 1000 && hints.boundingBox != null)
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
                kml += interop.renderMultiPointSymbol(id, name, description, symbolCode, controlPointsStr, altitudeMode, scale, bbox, modifiers, attributes, "serif", 0, fontSize);
            }
        }

        kml = modifyAltitudeReference(kml, attrs, controlPoints);

        // XXX - handle several symbols that have a centroid icon that is not returned as part of
        //       the `mil-sym-renderer` output
        final String iconCode = ICON_ANNOTATED_SYMBOLS.get(interop.getHierarchy(info));
        if(iconCode != null) {
            Envelope.Builder mbbb = new Envelope.Builder();
            for(int i = 0; i < points.length; i++)
                mbbb.add(points[i].getLongitude(), points[i].getLatitude());
            final Envelope mbb = mbbb.build();

            do {
                // NOTE: not strictly thread-safe, but good enough
                String annotationSymbolPlacemark = annotationSymbolPlacemarks.get(iconCode);
                if(annotationSymbolPlacemark == null) {
                    final Bitmap icon = renderSinglePointIcon(iconCode, null, null);
                    if (icon == null)
                        break;

                    android.graphics.Bitmap bmp = MarshalManager.marshal(icon, Bitmap.class, android.graphics.Bitmap.class);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(bmp.getWidth() * bmp.getHeight() * 2);
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bytes);

                    final String iconUri = "base64://" + Base64.encodeToString(bytes.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP);
                    annotationSymbolPlacemark = ANNOTATED_SYMBOL_PLACEMARK_TEMPLATE
                            .replace("##ICONURL##", iconUri);
                    annotationSymbolPlacemarks.put(symbolCode, annotationSymbolPlacemark);
                }

                kml += annotationSymbolPlacemark
                        //.replace("##NAME##", name)
                        .replace("##CENTROID##", String.valueOf((mbb.minX + mbb.maxX) / 2d) + "," + String.valueOf((mbb.minY + mbb.maxY) / 2d));
            } while(false);
        }

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

        int iconSize = (hints != null) ? hints.iconSize : 0;
        if(iconSize == 0)
            iconSize = (int)Math.ceil(32 * GLRenderGlobals.getRelativeScaling());

        int fontSize = (hints != null) ? hints.fontSize : 0;
        if(fontSize == 0)
            fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        Map<String, String> symRenderAttribs = new HashMap<>();
        symRenderAttribs.put(MilStd2525cAttributes.PixelSize, String.valueOf(iconSize));
        symRenderAttribs.put(MilStd2525cAttributes.KeepUnitRatio, "false");
        symRenderAttribs.put(MilStd2525cAttributes.DrawAsIcon, "false");


        final SymbolDef sym = interop.getSymbolDef(code);

        // handle civilian unit types
        if(sym != null) {
            switch (getAffiliation(code)) {
                case Hostile:
                case SimulatedHostile:
                case Suspect:
                case SimulatedSuspect:
                    // MIL-STD-2525C 5.3.2 - hostile and suspect remain red
                    break;
                default:
                    final String basicId = interop.getBasicSymbolId(code);
                    if (basicId.startsWith("S*A*C") ||
                        basicId.startsWith("S*S*X") ||
                        basicId.startsWith("S*G*UULC") ||
                        basicId.startsWith("S*G*EVC")) {

                        symRenderAttribs.put(MilStd2525cAttributes.FillColor, PURPLE_ICON_FILL);
                    }
                    break;
            }
        }
        return interop.renderSinglePointIcon(
                code,
                getModifiers(sym, attrs, true, (hints != null) ? hints.controlPoint : null),
                symRenderAttribs,
                (hints != null) ? hints.iconCenterOffset : null, "serif", 0, fontSize);
    }

    @Override
    public ISymbolTable getSymbolTable() {
        if(symbolTable == null)
            symbolTable = new MilStd2525cSymbolTable();
        return symbolTable;
    }

    @Override
    public Affiliation getAffiliation(String symbolCode) {

        //final char affiliation = SymbolUtilities.getAffiliation(symbolCode);
        final char affiliation = interop.getAffiliationLetterCode(symbolCode);
        switch(affiliation) {
            case 'P':
                return Affiliation.Pending;
            case 'U':
                return Affiliation.Unknown;
            case 'A':
                return Affiliation.AssumedFriend;
            case 'F':
                return Affiliation.Friend;
            case 'N':
                return Affiliation.Neutral;
            case 'S':
                return Affiliation.Suspect;
            case 'H':
                return Affiliation.Hostile;
            case 'G':
                return Affiliation.ExercisePending;
            case 'W':
                return Affiliation.ExerciseUnknown;
            case 'M':
                return Affiliation.ExerciseAssumedFriend;
            case 'D':
                return Affiliation.ExerciseFriend;
            case 'L':
                return Affiliation.ExerciseNeutral;
            case 'J':
                return Affiliation.Joker;
            case 'K':
                return Affiliation.Faker;
            default :
                return Affiliation.Unknown;
        }
    }

    @Override
    public String setAffiliation(String symbolCode, Affiliation affiliation) {
        switch(affiliation) {
            case ExercisePending:
                symbolCode = interop.setAffiliation(symbolCode, "G");
                break;
            case Faker:
                symbolCode = interop.setAffiliation(symbolCode, "K");
                break;
            case Joker:
                symbolCode = interop.setAffiliation(symbolCode, "J");
                break;
            case Friend:
                symbolCode = interop.setAffiliation(symbolCode, "F");
                break;
            case Hostile:
                symbolCode = interop.setAffiliation(symbolCode, "H");
                break;
            case Neutral:
                symbolCode = interop.setAffiliation(symbolCode, "N");
                break;
            case Pending:
                symbolCode = interop.setAffiliation(symbolCode, "P");
                break;
            case Suspect:
                symbolCode = interop.setAffiliation(symbolCode, "S");
                break;
            case Unknown:
                symbolCode = interop.setAffiliation(symbolCode, "U");
                break;
            case AssumedFriend:
                symbolCode = interop.setAffiliation(symbolCode, "A");
                break;
            case ExerciseAssumedFriend:
                symbolCode = interop.setAffiliation(symbolCode, "M");
                break;
            case ExerciseFriend:
                symbolCode = interop.setAffiliation(symbolCode, "D");
                break;
            case ExerciseNeutral:
                symbolCode = interop.setAffiliation(symbolCode, "L");
                break;
            case ExerciseUnknown:
                symbolCode = interop.setAffiliation(symbolCode, "W");
                break;
            case SimulatedFriend:
                symbolCode = interop.setAffiliation(symbolCode, "F");
                break;
            case SimulatedHostile:
                symbolCode = interop.setAffiliation(symbolCode, "H");
                break;
            case SimulatedNeutral:
                symbolCode = interop.setAffiliation(symbolCode, "N");
                break;
            case SimulatedPending:
                symbolCode = interop.setAffiliation(symbolCode, "P");
                break;
            case SimulatedSuspect:
                symbolCode = interop.setAffiliation(symbolCode, "S");
                break;
            case SimulatedUnknown:
                symbolCode = interop.setAffiliation(symbolCode, "U");
                break;
            case SimulatedAssumedFriend:
                symbolCode = interop.setAffiliation(symbolCode, "A");
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

        try {
            Map<String, String> modifiers = new HashMap<>();
            modifiers.put(MilStd2525cModifierConstants.AM_DISTANCE, "1000");
            modifiers.put(MilStd2525cModifierConstants.WIDTH, "1000");
            modifiers.put(MilStd2525cModifierConstants.LENGTH, "1000");
            modifiers.put(MilStd2525cModifierConstants.RADIUS, "1000");

            int fontSize = (hints != null) ? hints.fontSize : 0;
            if(fontSize == 0)
                fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

            IGeoPoint[] controlPoints = createControlPoints(
                    interop.getSymbolDef(milsym),
                    new RendererHints(),
                    STROKE_DASH_QUERY_SHAPE,
                    modifiers);

            Map<String, String> attributes = new HashMap<>();
            attributes.put(MilStd2525cAttributes.LineColor, Long.toString(0xFFFFFFFFL, 16));
            attributes.put(MilStd2525cAttributes.LineWidth, "1");
            // arbitrary values derived from mil-sym-renderer developer guide; should produce a
            // well-formed symbology
            MilStdSymbol mSymbol = interop.renderMultiPointSymbolAsMilStdSymbol(
                    null,
                    null,
                    null,
                    milsym,
                    controlPointsToString(controlPoints != null ? controlPoints : STROKE_DASH_QUERY_SHAPE),
                    "clampToGround",
                    5869879.2,
                    null,
                    modifiers,
                    attributes, "serif", 0, fontSize);
            Short dashPattern = SOLID_STROKE_DASH_PATTERN;
            // inspect the shapes to derive the pattern
            for (ShapeInfo shape : interop.getSymbolShapes(mSymbol)) {
                final float[] dashArray = interop.getStrokeDashArray(shape);
                if(dashArray == null)
                    continue;
                // check for baked known patterns
                Short known = MilSymRendererDashPatterns.get(dashArray);
                if(known != null) {
                    dashPattern = known;
                    break;
                }

                // synthesize the pattern mask

                // determine total length of pattern
                float totalPatternLength = 0f;
                for(int i = 0; i < dashArray.length; i++)
                    totalPatternLength += Math.ceil(dashArray[i]);
                // derive a masked, scaled to most closely represent the pattern, given the
                // available bits
                int mask = 0xFFFF;
                final int maskWidth = (int)Math.max(1, 16/totalPatternLength);
                mask >>>= 16-maskWidth;
                for (int i = 0; i < dashArray.length; i++) {
                    for (int j = 0; j < (int) Math.ceil(dashArray[i]/maskWidth); j++) {
                        if (i % 2 == 0) {
                            dashPattern = (short) ((dashPattern << maskWidth) | mask);
                        } else {
                            dashPattern = (short) (dashPattern << maskWidth);
                        }
                    }
                }

                break;
            }
            milsymStrokeDash.put(milsym, dashPattern);
        } catch(Throwable ignored) {}
        return null;
    }

    IGeoPoint[] createControlPoints(SymbolDef msInfo, RendererHints hints, IGeoPoint[] originalPoints, Map<String, String> modifiers)
    {
        final String hierarchy = interop.getHierarchy(msInfo);
        final int drawRule = interop.getDrawCategory(msInfo);
        final int minPoints = interop.getMinPoints(msInfo);
        final int maxPoints = interop.getMaxPoints(msInfo);

        return createControlPoints(hierarchy, drawRule, minPoints, maxPoints, hints, originalPoints, modifiers);
    }

    static IGeoPoint[] createControlPoints(String hierarchy, int drawRule, int minPoints, int maxPoints, RendererHints hints, IGeoPoint[] originalPoints, Map<String, String> modifiers)
    {
        IGeoPoint[] controlPoints = originalPoints;
        IGeoPoint controlPoint = null;

        //xx "Area Targets, Rectangular Target"
        //requires 2 distance one azimuth, may need some continued iteration
        if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_RECTANGULAR_PARAMETERED_AUTOSHAPE)
        {
            if(!isRectangle(originalPoints))
                return null;
            GeoPoint p1 = new GeoPoint(originalPoints[0].getLatitude(),
                    originalPoints[0].getLongitude());
            GeoPoint p2 = new GeoPoint(originalPoints[1].getLatitude(),
                    originalPoints[1].getLongitude());

            GeoPoint p3 = new GeoPoint(originalPoints[2].getLatitude(),
                    originalPoints[2].getLongitude());

            double d1 = GeoCalculations.distance(p2, p3);
            double d2 = GeoCalculations.distance(p1, p2);

            double deg1 = GeoCalculations.bearing(p1, p2);

            modifiers.put(MilStd2525cModifierConstants.AM_DISTANCE, d1 + "," + d2);
            modifiers.put(MilStd2525cModifierConstants.AN_AZIMUTH,  "" + deg1);

            controlPoints = new IGeoPoint[1];
            controlPoints[0] = GeoCalculations.midpoint(p1, p3);
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWO_POINT_RECT_PARAMETERED_AUTOSHAPE)
        {
            if(!isRectangle(originalPoints))
                return null;

            controlPoints = new IGeoPoint[2];
            GeoPoint p1 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);
            GeoPoint p2 = new GeoPoint(
                    (originalPoints[1].getLatitude() + originalPoints[0].getLatitude())/2.0,
                    (originalPoints[1].getLongitude() + originalPoints[0].getLongitude())/2.0);

            double d1 = GeoCalculations.distance(originalPoints[0], originalPoints[1]);

            controlPoints[0] = p1;
            controlPoints[1] = p2;
            modifiers.put(MilStd2525cModifierConstants.AM_DISTANCE, String.valueOf(d1));
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE &&
                minPoints == 3 &&
                (maxPoints == 3 || maxPoints == 4) &&
                isRectangle(originalPoints))
        {
            // assuming rectangle
            controlPoints = new IGeoPoint[3];
            GeoPoint p0 = new GeoPoint(
                    (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                    (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

            controlPoints[0] = p0;
            controlPoints[1] = originalPoints[0];
            controlPoints[2] = originalPoints[1];
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_SUPERAUTOSHAPE &&
                isRectangle(originalPoints))
        {
            // rectangle handling
            switch(hierarchy)
            {
                case "2.X.1.10":
                {
                    controlPoints = new IGeoPoint[3];

                    controlPoints[2] = originalPoints[2];
                    controlPoints[0] = originalPoints[0];
                    controlPoints[1] = originalPoints[1];
                    break;
                }
                case "2.X.3.1.7.4" :
                {
                    controlPoints = new IGeoPoint[3];

                    controlPoints[2] = originalPoints[3];
                    controlPoints[0] = originalPoints[0];
                    controlPoints[1] = originalPoints[1];
                    break;
                }
                case "2.X.2.3.1" :
                case "2.X.2.4.2.2" :
                case "2.X.2.5.3.3" :
                case "2.X.2.6.1.1" :
                {
                    controlPoints = new IGeoPoint[3];
                    GeoPoint p0 = new GeoPoint(
                            (originalPoints[0].getLatitude() + originalPoints[1].getLatitude())/2.0,
                            (originalPoints[0].getLongitude() + originalPoints[1].getLongitude())/2.0);

                    controlPoints[0] = p0;
                    controlPoints[1] = originalPoints[2];
                    controlPoints[2] = originalPoints[3];
                    break;
                }
                case "2.X.2.5.3.4" :
                case "2.X.3.1.6.3" :
                case "2.X.3.2.2.2" :
                case "6.X.4.12.3" :
                {
                    controlPoints = new IGeoPoint[4];
                    controlPoints[0] = originalPoints[2];
                    controlPoints[1] = originalPoints[3];
                    controlPoints[2] = originalPoints[1];
                    controlPoints[3] = originalPoints[0];
                    break;
                }
                case "2.X.2.5.2.4" :
                case "2.X.3.1.9.1" :
                case "2.X.3.1.9.2" :
                case "2.X.3.1.9.3" :
                case "2.X.3.1.9.4" :
                {
                    controlPoints = new IGeoPoint[3];
                    GeoPoint width = new GeoPoint(
                            (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                            (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

                    GeoPoint mid0 = new GeoPoint(
                            (originalPoints[3].getLatitude() + originalPoints[0].getLatitude())/2.0,
                            (originalPoints[3].getLongitude() + originalPoints[0].getLongitude())/2.0);
                    GeoPoint mid1 = new GeoPoint(
                            (originalPoints[2].getLatitude() + originalPoints[1].getLatitude())/2.0,
                            (originalPoints[2].getLongitude() + originalPoints[1].getLongitude())/2.0);


                    controlPoints[0] = mid0;
                    controlPoints[1] = mid1;
                    controlPoints[2] = width;
                    break;
                }
                case "2.X.1.18" :
                case "2.X.3.2.2" :
                {
                    // 4 control points, unmodified
                    break;
                }
                default:
                {
                    controlPoints = new IGeoPoint[3];
                    GeoPoint p0 = new GeoPoint(
                            (originalPoints[3].getLatitude() + originalPoints[2].getLatitude())/2.0,
                            (originalPoints[3].getLongitude() + originalPoints[2].getLongitude())/2.0);

                    controlPoints[2] = p0;
                    controlPoints[0] = originalPoints[0];
                    controlPoints[1] = originalPoints[1];
                    break;
                }
            }
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTARROW ||
                drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTLINE ||
                (drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE &&
                        (hierarchy.equals("2.X.3.2.2.6") ||
                         hierarchy.equals("2.X.6.4.2"))))
        {
            if(isRectangle(originalPoints))
            {
                controlPoints = new IGeoPoint[2];
                GeoPoint p1 = new GeoPoint(
                        (originalPoints[3].getLatitude() + originalPoints[2].getLatitude()) / 2.0,
                        (originalPoints[3].getLongitude() + originalPoints[2].getLongitude()) / 2.0);
                GeoPoint p2 = new GeoPoint(
                        (originalPoints[1].getLatitude() + originalPoints[0].getLatitude()) / 2.0,
                        (originalPoints[1].getLongitude() + originalPoints[0].getLongitude()) / 2.0);

                controlPoints[0] = p1;
                controlPoints[1] = p2;
            }
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ROUTE)
        {
            controlPoint = (hints.controlPoint == null) ?
                    computeControlPoint(hierarchy, drawRule, maxPoints, originalPoints) : hints.controlPoint;
            if(controlPoint != null)
            {
                IGeoPoint[] temp = new IGeoPoint[controlPoints.length+1];
                System.arraycopy(controlPoints, 0, temp, 0, controlPoints.length);
                temp[temp.length-1] = controlPoint;
                controlPoints = temp;
            }
        }
        else if(hints.shapeType == ShapeType.Circle && (drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE && maxPoints == 2))
        {
            IGeoPoint[] centerAndRadius = getCircleCenterAndRadius(originalPoints);
            if(centerAndRadius == null)
                return null;

            controlPoint = (hints.controlPoint == null) ?
                    computeControlPoint(hierarchy, drawRule, maxPoints, originalPoints) : hints.controlPoint;
            if(controlPoint != null) {
                double lat = centerAndRadius[0].getLatitude() - controlPoint.getLatitude();
                double lon = centerAndRadius[0].getLongitude() - controlPoint.getLongitude();

                double deg = Math.atan2(lon, lat) / Math.PI * 180.0 - 180.0;

                centerAndRadius[1] = GeoCalculations.pointAtDistance(
                        centerAndRadius[0],
                        deg,
                        GeoCalculations.distance(centerAndRadius[0], centerAndRadius[1]));
            }
            controlPoints = centerAndRadius;
        }
        else if(drawRule == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_CIRCULAR_PARAMETERED_AUTOSHAPE)
        {
            IGeoPoint[] centerAndRadius = getCircleCenterAndRadius(originalPoints);
            if(centerAndRadius == null)
                return null;

            controlPoints = new IGeoPoint[] { centerAndRadius[0] };
            modifiers.put(MilStd2525cModifierConstants.AM_DISTANCE, String.valueOf(GeoCalculations.distance(centerAndRadius[0], centerAndRadius[1])));
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

    static boolean isRectangle(IGeoPoint[] originalPoints)
    {
        return originalPoints.length == 5 && originalPoints[0].equals(originalPoints[4]);
    }

    static IGeoPoint computeControlPoint(String hierarchy, int drawCategory, int maxPoints, IGeoPoint[] points)
    {
        if(drawCategory == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ROUTE)
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
        else if(drawCategory == MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE &&
                maxPoints == 2 &&
                !hierarchy.equals("2.X.3.2.2.6") &&
                !hierarchy.equals("2.X.6.4.2"))
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

    Map<String, String> getModifiers(SymbolDef info, AttributeSet attrs, boolean singlePoint, IGeoPoint referencePoint)
    {
        Map<String, String> modifiers = new HashMap<>();
        if(attrs != null)
        {
            Set<String> modifierList = new HashSet<>(interop.getUnitModifierList());
            //T.W.W1.
            final String s = interop.getModifiers(info);
            if (s != null)
            {
                String[] mods = s.split("\\.");
                for (int i = 0; i < mods.length; i++)
                {
                    final String mod = mods[i];
                    if (modifierList.contains(mod))
                    {
                        if (!attrs.containsAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + mod))
                            continue;
                        String meta = attrs.getStringAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + mod);

                        // Enforce max lengths for modifier values
                        if (MilStd2525cModifierConstants.MaxLength.containsKey(mod)) {
                            int maxLength = MilStd2525cModifierConstants.MaxLength.get(mod);
                            if (meta.length() > maxLength) {
                                meta = meta.substring(0, maxLength);
                            }
                        }

                        // single point modifier rendering is GIGO; perform reference+unit
                        // conversion
                        if(singlePoint && mod.equals(MilStd2525cModifierConstants.X_ALTITUDE_DEPTH) && !meta.isEmpty()) {
                            final String[] altitudes = formatAltitude(
                                    attrs,
                                    referencePoint != null ?
                                            new IGeoPoint[] {referencePoint} : new IGeoPoint[0]);
                            if(altitudes == null) {
                                meta = null;
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

                        if(meta != null)
                            modifiers.put(mod, meta);
                    }
                }
            }
        }
        return modifiers;
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
    @Override
    public Status getStatus(String code) {
        return interop.getStatus(code);
    }

    @Override
    public String setStatus(String code, Status status) {
        return interop.setStatus(code, status);
    }

    @Override
    public Amplifier getAmplifier(String symbolCode) {
        return interop.getAmplifier(symbolCode);
    }

    @Override
    public String setAmplifier(String symbolCode, Amplifier amplifier) {
        return interop.setAmplifier(symbolCode, amplifier);
    }

    @Override
    public int getHeadquartersTaskForceDummyMask(String symbolCode) {
        return interop.getHeadquartersTaskForceDummyMask(symbolCode);
    }

    @Override
    public String setHeadquartersTaskForceDummyMask(String symbolCode, int mask) {
        return interop.setHeadquartersTaskForceDummyMask(symbolCode, mask);
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
}
