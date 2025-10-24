package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

public class MilStd2525cSymbologyTableTest extends MilStd2525cSymbologyTableTestBase
{
    @Before
    public void init()
    {
        NativeLoader.init(getTestContext());
        EngineLibrary.initialize();
        MilStd2525cSymbologyProvider.init(getTestContext());
    }
}
