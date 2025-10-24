package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.symbology.ISymbologyProvider;


public class MilStd2525dSymbologyProviderTest extends MilStd2525dSymbologyProviderTestBase
{
    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525dSymbologyProvider();
    }
}
