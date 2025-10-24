package com.atakmap.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.ConfigOptions;
import com.atakmap.util.zip.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Manage the WMM to include reading the WMM version from "wmm_cof" under takkernel/engine/res/raw
 */
public class WMMCof {

    private static final String TAG = "WMM";

    private static String wmmVersion = null;
    private static boolean initialized = false;

    private WMMCof() { }

    /**
     * For plugins that need to know the current version of the WMM deployed by ATAK for use.
     * Will return null prior to initialization.    This includes plugins that produce forms
     * as an output of surveying for example.
     */
    public static String getVersion() {
        return wmmVersion;
    }


    /**
     * The cof file version
     * @param cofFile to read from
     * @return WMM 2015, WMM 2020, WMM 2025
     */
    private static String getVersion(File cofFile) {

        InputStream stream = null;
        try {
            stream = new FileInputStream(cofFile);
            byte[] header = FileSystemUtils.read(stream, 1024, true);
            String s = new String(header, FileSystemUtils.UTF8_CHARSET);
            int len = 8;
            int wmmIdx = s.indexOf("WMM-");

            // check to see if it is the high res wmm 2025
            if (wmmIdx < 0) {
                wmmIdx = s.indexOf("WMMHR-");
                len = 10;
            }

            // if less than zero invalid file
            if (wmmIdx > 0) {
                wmmVersion = s.substring(wmmIdx, wmmIdx + len).replace("-", " ");
            } else {
                Log.e(TAG, "malformed wmm_conf file");
            }
        } catch (Exception e) {
            Log.e(TAG, "error reading the internally supplied wmm_conf file", e);
        }
        if (wmmVersion == null)
            wmmVersion = "WMM 2015";

        return wmmVersion;
    }

    /**
     * Function to deploy the current World Mag Model file for use by the client.   This is
     * called once during ATAK initiation and should not be called again.
     * @param context the application context
     */
    public synchronized static void deploy(Context context) {
        if (!initialized)
            extractPrivateResource(context,"wmm_cof", "world-magnetic-model-file", false);
        initialized = true;
    }

    private static void extractPrivateResource(Context context, String resourceName, String option,
                                        boolean useIOAbstraction) {
        InputStream stream = null;
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            long s = SystemClock.uptimeMillis();
            // load from assets
            Resources r = context.getResources();
            final int id = r.getIdentifier(resourceName, "raw",
                    context.getPackageName());
            if (id != 0) {
                stream = r.openRawResource(id);
            }

            if (stream == null)
                throw new ExceptionInInitializerError();

            File cofFile = new File(context.getFilesDir(), resourceName);
            /*Check if the flag to utilize IOAbstraction is enabled
             * Loading this with Encryption causes invalid returns
             */

            if (useIOAbstraction) {
                FileSystemUtils.copy(stream,
                        IOProviderFactory.getOutputStream(cofFile));
            } else {
                FileSystemUtils.copy(stream,
                        fos = new FileOutputStream(cofFile));
            }

            long e = SystemClock.uptimeMillis();

            Log.v(TAG, "Extracted resource " + getVersion(cofFile) + " [" + resourceName + "] in " + (e - s)
                    + "ms");

            if (option != null)
                ConfigOptions.setOption(option, cofFile.getAbsolutePath());
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        } finally {
            IoUtils.close(stream);
            IoUtils.close(fos);
        }
    }
}
