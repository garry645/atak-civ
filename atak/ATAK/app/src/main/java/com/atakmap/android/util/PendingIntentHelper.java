
package com.atakmap.android.util;

import android.app.PendingIntent;
import android.os.Build;

public class PendingIntentHelper {

    /**
     * As of Android 31 and higher a Pending Intent needs to have FLAG_MUTABLE
     * or FLAG_IMMUTABLE.   This will adapt a legacy flag so that it is set to be
     * IMMUTABLE.
     * @param flags the pending intent flag
     * @return return the flags with the FLAG_IMMUTABLE set.
     */
    public static int adaptFlags(int flags) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            flags |= PendingIntent.FLAG_IMMUTABLE;

        return flags;
    }

}
