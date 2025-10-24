package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.symbology.ISymbologyProvider;

public class MilStd2525eSymbologyTableTest extends MilStd2525dSymbologyTableTestBase
{
    final static String forwardLineOfTroopsId = "130025000014010000000000000000";
    final static String airCivilianFixedWingId = "130001000012010000000000000000";

    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525eSymbologyProvider();
    }

    @Override
    String getForwardLineOfTroopsId() {
        return forwardLineOfTroopsId;
    }

    @Override
    String getAirCivilianFixedWingId() {
        return airCivilianFixedWingId;
    }
}
