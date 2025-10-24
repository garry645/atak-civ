package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

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

    @Before
    public void init()
    {
        NativeLoader.init(getTestContext());
        EngineLibrary.initialize();
        MilStd2525dSymbologyProvider.init(getTestContext());
    }
}
