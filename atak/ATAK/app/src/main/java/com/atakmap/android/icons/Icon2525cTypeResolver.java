
package com.atakmap.android.icons;

import android.content.Context;

import com.atakmap.app.R;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.CotUtils;
import gov.tak.platform.symbology.milstd2525.MilStd2525;

public class Icon2525cTypeResolver {

    private static final String TAG = "Icon2525cTypeResolver";

    /**
     * Given a CoT type, generate a 2525c identifier
     * @param type the CoT type
     * @return an empty string if the CoT type is not mappable
     *
     * @deprecated use {@link CotUtils#mil2525cFromCotType(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String mil2525cFromCotType(String type) {
        return CotUtils.mil2525cFromCotType(type);
    }

    /**
     * Obtain a CoT type from a given 2525C symbol identifier
     * @param type the 2525C symbol identifier
     * @return the cot type
     *
     * @deprecated use {@link CotUtils#cotTypeFromMil2525C(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String cotTypeFromMil2525C(final String type) {
        return CotUtils.cotTypeFromMil2525C(type);
    }

    /**
     * Currently a direct copy of what was used for hostiles
     * No attempt has been made to change it.
     */
    public static String getShortHumanName(final String type,
            final Context context) {
        String targ = context.getString(R.string.not_recognized);
        if (type.startsWith("a-h-A")) {
            targ = context.getResources().getString(R.string.aircraft);
        } else if (type.startsWith("a-h-G-U-C-F")) {
            targ = context.getResources()
                    .getString(R.string.artillery);
        } else if (type.startsWith("a-h-G-I")) {
            targ = context.getResources().getString(R.string.building);
        } else if (type.startsWith("a-h-G-E-X-M")) {
            targ = context.getResources().getString(R.string.mine);
        } else if (type.startsWith("a-h-S")) {
            targ = context.getResources().getString(R.string.ship);
        } else if (type.startsWith("a-h-G-U-C-I-d")) {
            targ = context.getResources().getString(R.string.sniper);
        } else if (type.startsWith("a-h-G-E-V-A-T")) {
            targ = context.getResources().getString(R.string.tank);
        } else if (type.startsWith("a-h-G-U-C-I")) {
            targ = context.getResources().getString(R.string.troops);
        } else if (type.startsWith("a-h-G-E-V")) {
            targ = context.getResources().getString(R.string.cot_type_vehicle);
        } else {
            targ = context.getResources().getString(R.string.ground);
        }
        return targ;
    }

    /**
     * Given a 2525 type, this will return a human friendly name.  
     * @param type the COT type.
     * @param context the context to use.
     * @deprecated
     * @see CotDescriptions#getHumanName
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", removeAt = "5.9", forRemoval = true)
    public static String getHumanName(final String type,
            final Context context) {
        return CotDescriptions.getHumanName(context, type);
    }

    /**
     * Obtain a 2525D symbol identifier from a 2525C symbol identifier
     * @param type the 2525C symbol identifier
     * @return the 2525D symbol identifier or null if no mapping exists
     *
     * @deprecated use {@link MilStd2525#get2525DFrom2525C(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String get2525DFrom2525C(final String type) {
        return MilStd2525.get2525DFrom2525C(type);
    }

    /**
     * Obtain a 2525C symbol identifier from a cot type.
     * @param cotType the cot type to be looked up
     * @return the 2525C symbol
     *
     * @deprecated use {@link CotUtils#mil2525cFromCotType(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public String get2525CFromCoTType(String cotType) {
        return CotUtils.mil2525cFromCotType(cotType);
    }

    /**
     * Obtain a 2525D symbol identifier from a cot type.
     * @param cotType the cot type to be looked up
     * @return the 2525D symbol or null if no mapping exists
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public String get2525DFromCoTType(String cotType) {
        return get2525DFrom2525C(
                CotUtils.mil2525cFromCotType(cotType));
    }

    /**
     * Obtain a CoT type from a given 2525C symbol identifier
     * @param type the 2525C symbol identifier
     * @return the cot type
     *
     * @deprecated use {@link CotUtils#cotTypeFromMil2525C(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public String getCotTypeFromMil2525C(final String type) {
        return CotUtils.cotTypeFromMil2525C(type);
    }

    /**
     * Obtain a CoT type from a given 2525D symbol identifier
     * @param type the 2525D symbol identifier
     * @return the cot type
     *
     * @deprecated use {@link CotUtils#cotTypeFromMil2525C(String)} and {@link MilStd2525#get2525CFrom2525D(String, boolean)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public String getCotTypeFromMil2525D(final String type) {
        String charlie = get2525CFrom2525D(type);
        if (charlie != null)
            return getCotTypeFromMil2525C(charlie);
        return null;
    }

    /**
     * Obtain a 2525C symbol identifier from a 2525D symbol identifier
     * @param type the 2525D symbol identifier
     * @return the 2525C symbol identifier or null if no mapping exists
     *
     * @deprecated use {@link MilStd2525#get2525CFrom2525D(String, boolean)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public synchronized String get2525CFrom2525D(final String type) {
        return MilStd2525.get2525CFrom2525D(type);
    }
}
