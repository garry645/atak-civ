
package com.atakmap.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfHelper {

    public static final String TAG = "PdfHelper";

    /** 
    * Helper function used by most users of the PdfHelper to see if adobe or another pdf reader is 
    * installed.
    * @param context context used to check if something is installed - the atak context or the
     *      preference fragment context but not the plugin context
    * @param pkg the package name
    */
    public static boolean isInstalled(final Context context, final String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not installed: " + pkg, e);
        }
        return false;
    }

    /**
     * Helper function that is used to launch Adobe or some other PDF reader.
     * @param context the context used to launch the application - the atak context or the
     *                preference fragment context but not the plugin context
     * @param file the file to use when launching the activity
     */
    public static void launchAdobe(final Context context, final String file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            com.atakmap.android.util.FileProviderHelper.setDataAndType(context,
                    intent, new File(file), "application/pdf");

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "error launching a pdf viewer", e);
        }
    }

    /**
     * Checks to see if adobe is installed and present. Issues a warning
     * otherwise.
     * @param context the context use the show the warning, the atak context or the
     *                preference fragment context but not the plugin context
     * @param file the string representation of the file
     */
    public static void checkAndWarn(final Context context, final String file) {
        final AtakPreferences _prefs = AtakPreferences.getInstance(context);
        boolean displayHint = _prefs.get("atak.hint.missingadobe", true);

        if (!isInstalled(context, "com.adobe.reader") && displayHint) {

            View v = LayoutInflater.from(context)
                    .inflate(com.atakmap.app.R.layout.hint_screen, null);
            TextView tv = v
                    .findViewById(com.atakmap.app.R.id.message);
            final CheckBox showAgain = v
                    .findViewById(R.id.showAgain);
            showAgain.setChecked(true);

            tv.setText(
                    R.string.acrobat_warning);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.acrobat_warning_title)
                    .setView(v).setCancelable(false)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    _prefs.set("atak.hint.missingadobe",
                                            !showAgain.isChecked());
                                    launchAdobe(context, file);
                                }
                            })
                    .create().show();
        } else {
            launchAdobe(context, file);
        }
    }

    /**
     * This provides for a universal extract only if newer and showing of the document from the
     * asset of a plugin.  In this call the version is set by the version code of the plugin context
     * being used.
     * @param pContext the plugin context
     * @param appContext the app context
     * @param document the document to extract from asset to include any folder structure information
     * @param output the output file to extract to
     * @return true if extracted successfully
     */
    public static boolean extractAndShow(Context pContext,
            Context appContext,
            String document,
            String output,
            boolean showPdf) {

        final PackageManager manager = appContext.getPackageManager();
        final String pkg = pContext.getPackageName();

        try {
            PackageInfo pInfo = manager.getPackageInfo(pkg,
                    PackageManager.GET_ACTIVITIES);
            return extractAndShow(pContext, appContext, document,
                    pInfo.versionCode, output, showPdf);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "version code not found: " + pkg);
            return false;
        }
    }

    /**
     * This provides for a universal extract only if newer and showing of the document from the
     * asset of a plugin.
     * @param pContext the plugin context
     * @param appContext the app context
     * @param document the document to extract from asset to include any folder structure information
     * @param version the version of the document to be extracted as to make sure it is not extracted
     *                each time.   This is a strict equality check.
     * @param output the output file to extract to
     * @return true if extracted successfully
     */
    public static boolean extractAndShow(Context pContext,
            Context appContext,
            String document,
            long version,
            String output,
            boolean showPdf) {

        final File outputFile = new File(output);
        final File pDir = outputFile.getParentFile();
        final AtakPreferences pref = AtakPreferences.getInstance(appContext);
        final String prefKey = "pdfhelper.document-"
                + pContext.getApplicationInfo().packageName + "://" + document;

        if (pDir == null) {
            Log.e(TAG, "directory cannot be determined");
            return false;
        }

        if (!pDir.exists()) {
            Log.d(TAG, "directory is not present: " + pDir);
            if (pDir.mkdirs()) {
                Log.d(TAG, pDir.getPath() + " was created " + pDir);
            } else {
                Log.d(TAG, pDir.getPath() + " error creating " + pDir);
                return false;
            }
        }

        boolean extract = !outputFile.exists() ||
                pref.get(prefKey, -1L) != version;

        if (extract) {
            boolean success = false;
            //output requires a relative path from the getRoot()
            try (FileOutputStream fileOutputStream = new FileOutputStream(
                    output)) {
                success = FileSystemUtils.copyFromAssets(pContext, document,
                        fileOutputStream);
            } catch (IOException e) {
                Log.e(TAG, "failed to copy assets file: " + document + " to "
                        + output);
            }
            if (!success) {
                Log.e(TAG, "unable to extract " + document + " to " + output);
                return false;
            }
            pref.set(prefKey, version);
        }

        if (showPdf)
            checkAndWarn(appContext, output);

        return true;

    }

}
