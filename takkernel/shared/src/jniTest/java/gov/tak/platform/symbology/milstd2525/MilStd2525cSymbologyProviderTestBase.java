package gov.tak.platform.symbology.milstd2525;

import com.atakmap.map.EngineLibrary;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.ogr.OgrFeatureDataSource;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.util.AttributeSet;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

abstract class MilStd2525cSymbologyProviderTestBase extends MilStd2525SymbologyProviderTestBase
{
    final static String forwardLineOfTroopsId = "G*GPGLF---****X";
    final static String airCivilianFixedWingId = "S*A*C-----*****";
    final static String noFireAreaCircularId = "G*F*ACNC--****X";

    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525cSymbologyProvider();
    }

    @Override
    boolean expectedHasSinglePoint() {
        return true;
    }

    @Override
    boolean expectedHasMultiPoint() {
        return true;
    }

    @Override
    String expectedProviderName() {
        return "2525C";
    }

    @Override
    EnumSet<Affiliation> supportedAffiliations() {
        EnumSet<Affiliation> affiliations = EnumSet.allOf(Affiliation.class);
        // 2525C does not have support for simulated
        affiliations.remove(Affiliation.SimulatedAssumedFriend);
        affiliations.remove(Affiliation.SimulatedFriend);
        affiliations.remove(Affiliation.SimulatedHostile);
        affiliations.remove(Affiliation.SimulatedSuspect);
        affiliations.remove(Affiliation.SimulatedNeutral);
        affiliations.remove(Affiliation.SimulatedPending);
        affiliations.remove(Affiliation.SimulatedUnknown);
        return affiliations;
    }

    @Test
    public void getDefaultSourceShape_unit() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        final ShapeType shape = milsym.getDefaultSourceShape(airCivilianFixedWingId);
        Assert.assertNotNull(shape);
        Assert.assertEquals(ShapeType.Point, shape);
    }
    @Test
    public void getDefaultSourceShape_multipoint() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        final ShapeType shape = milsym.getDefaultSourceShape(forwardLineOfTroopsId);
        Assert.assertNotNull(shape);
        Assert.assertEquals(ShapeType.LineString, shape);
    }

    //    Collection<Modifier> getModifiers(String symbolCode);
    //    Collection<Feature> renderMultipointSymbol(@NonNull String code, @NonNull  IGeoPoint[] points, @Nullable AttributeSet attrs, @Nullable  RendererHints hints);
    //    Bitmap renderSinglePointIcon(@NonNull  String code, @Nullable AttributeSet attrs, @Nullable RendererHints hints);

    @Test
    public void getSymbolTable() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        final ISymbolTable table = milsym.getSymbolTable();
        Assert.assertNotNull(table);
    }

    @Test
    public void affiliationRoundtrip() {
        affiliationRoundtrip(airCivilianFixedWingId);
    }

    @Test
    public void amplifierRoundtrip() {
        amplifierRoundtrip(airCivilianFixedWingId);
    }

    @Test
    public void hqTfDummyRoundtrip() {
        hqTfDummyMaskRoundtrip(airCivilianFixedWingId);
    }

    @Test
    public void circularParameterizedDrawRule() {
        final String symbolcode = "G*F*ACNC--****X";
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        final ShapeType shape = milsym.getDefaultSourceShape(symbolcode);

        // verify circle
        Assert.assertNotNull(shape);
        Assert.assertEquals(ShapeType.Circle, shape);

        IGeoPoint[] points = new IGeoPoint[4];
        points[0] = new GeoPoint(39.0001, -78.0000);
        points[1] = new GeoPoint(39.0000, -77.9999);
        points[2] = new GeoPoint(38.9999, -78.0000);
        points[3] = new GeoPoint(39.0000, -78.0001);
        points[0] = new GeoPoint(39.0001, -78.0000);

        Map<String, String> modifiers = new HashMap<>();
        IGeoPoint[] controlPoints = milsym.createControlPoints(milsym.interop.getSymbolDef(symbolcode), new ISymbologyProvider.RendererHints(), points, modifiers);

        Assert.assertNotNull(controlPoints);
        Assert.assertTrue(modifiers.containsKey(MilStd2525cModifierConstants.AM_DISTANCE));
    }

    @Test
    public void getModifiers() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        Collection<Modifier> symbolModifiers = milsym.getModifiers(noFireAreaCircularId);
        List<String> modifierList = new ArrayList<>();
        for (Modifier modifier : symbolModifiers) {
            modifierList.add(modifier.getId());
        }
        Collections.sort(modifierList);
        String modifierString = String.join(",", modifierList);
        Assert.assertEquals(modifierString, "T,W,W1");
    }

    @Test
    public void multipointSymbologyDetectedAs3D() {
        GdalLibrary.init();
        FeatureDataSourceContentFactory.register(new OgrFeatureDataSource());

        final String code = "GUG-AAM---****X";
        final IGeoPoint[] points = new IGeoPoint[7];
        points[0] = new GeoPoint(37.550962414643045,-77.44981589421361);
        points[1] = new GeoPoint(37.55358795317539,-77.426568999278);
        points[2] = new GeoPoint(37.53711339150623,-77.41528436260225);
        points[3] = new GeoPoint(37.524691626392965,-77.43573622005918);
        points[4] = new GeoPoint(37.531984551961244,-77.4504983135398);
        points[5] = new GeoPoint(37.5442144743874,-77.45440669017106);
        points[6] = new GeoPoint(37.550962414643045,-77.44981589421361);
        AttributeSet attrs = new AttributeSet();
        attrs.setAttribute("milsym.modifier.W1", "");
        attrs.setAttribute("milsym.modifier.X", "0,0");
        attrs.setAttribute("milsym.modifier.W", "");
        attrs.setAttribute("milsym.modifier.T", "");
        final ISymbologyProvider.RendererHints hints = new ISymbologyProvider.RendererHints();
        hints.controlPoint = null;
        hints.shapeType = null;
        hints.strokeColor = -16711681;
        hints.strokeWidth = 3.0f;
        hints.altitudeMode = Feature.AltitudeMode.ClampToGround;
        hints.resolution = Double.NaN;
        hints.boundingBox = null;
        hints.iconSize = 0;
        hints.fontSize = 10;

        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();

        Collection<Feature> features = milsym.renderMultipointSymbol(code, points, attrs, hints);
        Assert.assertNotNull(features);
        Assert.assertFalse(features.isEmpty());

        for(Feature f : features) {
            Assert.assertFalse(hasFill(f.getStyle()));
        }
    }

    static boolean hasFill(Style s) {
        if(s instanceof BasicFillStyle) {
            return (((BasicFillStyle) s).getColor()&0xFF000000) != 0;
        } else if(s instanceof CompositeStyle) {
            for (int i = 0; i < ((CompositeStyle) s).getNumStyles(); i++)
                if (hasFill(((CompositeStyle) s).getStyle(i)))
                    return true;
            return false;
        } else if(s instanceof LevelOfDetailStyle) {
            return hasFill(((LevelOfDetailStyle) s).getStyle());
        } else {
            return false;
        }
    }
}
