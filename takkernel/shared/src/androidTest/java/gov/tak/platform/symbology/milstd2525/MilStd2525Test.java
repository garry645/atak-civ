package gov.tak.platform.symbology.milstd2525;

import android.content.Context;

import gov.tak.test.KernelTest;

import org.junit.Before;

public class MilStd2525Test extends MilStd2525TestBase
{
    @Before
    public void init()
    {

        Context context = getTestContext();
        gov.tak.platform.symbology.milstd2525.MilStd2525.init(context);
    }
}
