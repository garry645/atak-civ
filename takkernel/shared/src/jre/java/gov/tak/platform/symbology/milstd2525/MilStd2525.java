package gov.tak.platform.symbology.milstd2525;

import gov.tak.platform.commons.resources.JavaResourceManager;

public final class MilStd2525 extends MilStd2525Base
{
    private MilStd2525(){}

    static void init() {
        init(new JavaResourceManager());
    }
}
