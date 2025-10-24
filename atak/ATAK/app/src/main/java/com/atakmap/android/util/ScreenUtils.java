
package com.atakmap.android.util;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Utility methods for converting pixels to and from display pixels.
 */
public class ScreenUtils {

    /**
     * Given a number of pixels convert to display pixels
     * @param px the pixel size
     * @param context the context
     * @return the display pixel size
     */
    public static float convertPixelsToDp(float px, Context context) {
        return px
                / ((float) context.getResources().getDisplayMetrics().densityDpi
                        / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Given a number of pixels convert to display pixels
     * @param dp the display pixel size
     * @param context the context
     * @return the pixel size
     */
    public static float convertDpToPixels(float dp, Context context) {
        return dp
                * ((float) context.getResources().getDisplayMetrics().densityDpi
                        / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Get the ratio of the screen.
     * @param context the context
     * @return the ration as a number expessed as a float.
     */
    public static float getScreenRatio(Context context) {
        DisplayMetrics displayMetrics = context.getResources()
                .getDisplayMetrics();
        if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
            return (float) displayMetrics.widthPixels
                    / (float) displayMetrics.heightPixels;
        }
        return (float) displayMetrics.heightPixels
                / (float) displayMetrics.widthPixels;

    }
}
