package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

import gov.tak.api.symbology.ISymbologyProvider;

public class MilStd2525dSymbologyProviderTest extends MilStd2525dSymbologyProviderTestBase
{
    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525dSymbologyProvider();
    }
    @Before
    public void init()
    {
        NativeLoader.init(getTestContext());
        EngineLibrary.initialize();
        MilStd2525dSymbologyProvider.init(getTestContext());
    }
}
