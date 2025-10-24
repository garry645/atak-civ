package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

import gov.tak.api.symbology.ISymbologyProvider;

public class MilStd2525dSymbologyTableTest extends MilStd2525dSymbologyTableTestBase
{
    final static String forwardLineOfTroopsId = "10002500001401000000";
    final static String airCivilianFixedWingId = "10000100001201000000";

    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525dSymbologyProvider();
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
