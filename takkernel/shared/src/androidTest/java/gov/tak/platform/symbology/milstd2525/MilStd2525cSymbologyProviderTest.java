package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

public class MilStd2525cSymbologyProviderTest extends MilStd2525cSymbologyProviderTestBase
{
    @Before
    public void init()
    {
        NativeLoader.init(getTestContext());
        EngineLibrary.initialize();
        MilStd2525cSymbologyProvider.init(getTestContext());
    }
}
