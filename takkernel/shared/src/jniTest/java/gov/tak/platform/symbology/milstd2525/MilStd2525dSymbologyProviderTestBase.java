package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.EnumSet;

import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;

abstract class MilStd2525dSymbologyProviderTestBase extends MilStd2525SymbologyProviderTestBase {
    final static String forwardLineOfTroopsId = "10002500001401000000";
    final static String airCivilianFixedWingId = "10000100001201000000";

    final int version;

    public MilStd2525dSymbologyProviderTestBase() {
        this(11);
    }
    public MilStd2525dSymbologyProviderTestBase(int version) {
        this.version = version;
    }
    String getForwardLineOfTroopsId() {
        if(this.version != 11) {
            return this.version + forwardLineOfTroopsId.substring(2);
        }
        return forwardLineOfTroopsId;
    }

    String getAirCivilianFixedWingId() {
        if(this.version != 11) {
            return this.version + airCivilianFixedWingId.substring(2);
        }
        return airCivilianFixedWingId;

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
        return "2525D";
    }

    @Override
    EnumSet<Affiliation> supportedAffiliations() {
        return EnumSet.allOf(Affiliation.class);
    }

    @Test
    public void getDefaultSourceShape_unit() {
        ISymbologyProvider milsym = newInstance();

        final ShapeType shape = milsym.getDefaultSourceShape(getAirCivilianFixedWingId());
        Assert.assertNotNull(shape);
        Assert.assertEquals(ShapeType.Point, shape);
    }
    @Test
    public void getDefaultSourceShape_multipoint() {
        ISymbologyProvider milsym = newInstance();
        final ShapeType shape = milsym.getDefaultSourceShape(getForwardLineOfTroopsId());
        Assert.assertNotNull(shape);
        Assert.assertEquals(ShapeType.LineString, shape);
    }

    //    Collection<Modifier> getModifiers(String symbolCode);
    //    Collection<Feature> renderMultipointSymbol(@NonNull String code, @NonNull  IGeoPoint[] points, @Nullable AttributeSet attrs, @Nullable  RendererHints hints);
    //    Bitmap renderSinglePointIcon(@NonNull  String code, @Nullable AttributeSet attrs, @Nullable RendererHints hints);

    @Test
    public void getSymbolTable() {
        ISymbologyProvider milsym = newInstance();
        final ISymbolTable table = milsym.getSymbolTable();
        Assert.assertNotNull(table);
    }

    @Test
    public void affiliationRoundtrip() {
        affiliationRoundtrip(getAirCivilianFixedWingId());
    }

    @Test
    public void amplifierRoundtrip() {
        amplifierRoundtrip(getAirCivilianFixedWingId());
    }

    @Test
    public void hqTfDummyRoundtrip() {
        hqTfDummyMaskRoundtrip(getAirCivilianFixedWingId());
    }
}
