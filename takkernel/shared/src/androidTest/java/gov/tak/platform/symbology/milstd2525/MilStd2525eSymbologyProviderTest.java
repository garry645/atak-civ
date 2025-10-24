package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Before;

import gov.tak.api.symbology.ISymbologyProvider;

public class MilStd2525eSymbologyProviderTest extends MilStd2525dSymbologyProviderTestBase
{
    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525eSymbologyProvider();
    }

    public MilStd2525eSymbologyProviderTest() {
        super(13);
    }

    @Override
    String expectedProviderName() {
        return "2525E";
    }

    @Before
    public void init()
    {
        NativeLoader.init(getTestContext());
        EngineLibrary.initialize();
        MilStd2525dSymbologyProvider.init(getTestContext());
    }
}
