
package com.atakmap.coremap.filesystem;

import android.content.Context;

public class RemovableStorageHelper
{

    public static void init(Context ctx)
    {
    }

    /**
     * Returns the actual removable storage directory as provided by utilizing the cached directory
     *
     * @return the value of "ANDROID_STORAGE" as an environment variable, otherwise if on Android 11
     * returns the appropriate removable storage directories.
     */
    public static String[] getRemovableStorageDirectory()
    {
        return new String[0];
    }
}
