package gov.tak.platform.symbology.milstd2525;

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
}
