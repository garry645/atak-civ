package gov.tak.platform.symbology.milstd2525;

import android.content.Context;

import gov.tak.api.annotation.DeprecatedApi;

public final class
MilStd2525dSymbologyProvider extends MilStd2525dSymbologyProviderBase
{
    public MilStd2525dSymbologyProvider() {
        super(11);
    }

    /** @deprecated use {@link MilStd2525#init(Context)} */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static void init(Context context) {

    }
}
