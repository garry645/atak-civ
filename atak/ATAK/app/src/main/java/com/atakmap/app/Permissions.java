
package com.atakmap.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.log.Log;

public class Permissions {

    private final static String TAG = "Permissions";

    final static int REQUEST_ID = 90402;

    final static int LOCATION_REQUEST_ID = 90403;

    private static final String PREF_OVERRIDE_PERMISSION_KEY = "override_permission_request";
    private static final String PREF_OVERRIDE_BACKGROUND_LOCATION_KEY = "override_background_permission_request";

    final static String[] PermissionsList = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,

            // 31 - protection in place
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,

            // 29 - protection in place
            Manifest.permission.ACCESS_MEDIA_LOCATION,

            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,

            // 33 - protection in place
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,

            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.DISABLE_KEYGUARD,
            Manifest.permission.GET_TASKS,
            Manifest.permission.NFC,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,

            // 23 - protection in place
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,

            // 26 - protection in place
            Manifest.permission.REQUEST_DELETE_PACKAGES,

            // 26 - protections in place
            Manifest.permission.READ_PHONE_NUMBERS,

            // 33 - protection in place
            Manifest.permission.NEARBY_WIFI_DEVICES,

            // 33 - protection in place
            Manifest.permission.POST_NOTIFICATIONS,

            "com.atakmap.app.ALLOW_TEXT_SPEECH",

    };

    final static String[] dummyPermissionList = new String[] {
            Manifest.permission.ACCESS_WIFI_STATE
    };

    final static String[] locationPermissionsList = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,

            // 29 - protection in place
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    };

    static boolean checkPermissions(final Activity a) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        int result = 0;
        for (String permission : PermissionsList) {
            if (isValidPermission(permission)) {
                int perm = a.checkSelfPermission(permission);
                result += perm;
                if (perm == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "permission not granted: " + permission);
                }
            }
        }
        AtakPreferences prefs = AtakPreferences.getInstance(a);
        boolean override = prefs.get(PREF_OVERRIDE_PERMISSION_KEY, false);

        if (result != PackageManager.PERMISSION_GRANTED && !override) {
            Log.d(TAG,
                    "permissions have not been granted for all of the things and user has not selected override");
            // if not overriding then reset the decision for the background_permission
            prefs.set(PREF_OVERRIDE_BACKGROUND_LOCATION_KEY, false);
            showRationalAndRequest(a);

            return false;
        } else {

            if (!override || checkPermission(a,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!prefs.get(PREF_OVERRIDE_BACKGROUND_LOCATION_KEY,
                            false)) {

                        if (PackageManager.PERMISSION_GRANTED != a
                                .checkCallingOrSelfPermission(
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            Log.d(TAG,
                                    "permission not granted for background location listening");
                            showLocationBackgroundWarning(a);
                            return false;
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    showFileSystemWarning(a);
                    return false;
                }
            }

            Log.d(TAG, "permissions have been granted for all of the things");
            return true;
        }

    }

    @TargetApi(23)
    private static void showRationalAndRequest(final Activity a) {
        LayoutInflater li = LayoutInflater.from(a);
        View v = li.inflate(R.layout.permission_rationale, null);

        LinearLayout ll = v.findViewById(R.id.rationale);
        generateAndAddRational(a, ll, R.string.perm_text_a,
                R.string.perm_rationale_a);
        generateAndAddRational(a, ll, R.string.perm_text_b,
                R.string.perm_rationale_b);
        generateAndAddRational(a, ll, R.string.perm_text_c,
                R.string.perm_rationale_c);
        generateAndAddRational(a, ll, R.string.perm_text_d,
                R.string.perm_rationale_d);
        generateAndAddRational(a, ll, R.string.perm_text_e,
                R.string.perm_rationale_e);
        generateAndAddRational(a, ll, R.string.perm_text_f,
                R.string.perm_rationale_f);
        generateAndAddRational(a, ll, R.string.perm_text_g,
                R.string.perm_rationale_g);
        generateAndAddRational(a, ll, R.string.perm_text_h,
                R.string.perm_rationale_h);
        if (!BuildConfig.FLAVOR.equalsIgnoreCase("civSmall"))
            generateAndAddRational(a, ll, R.string.perm_text_i,
                    R.string.perm_rationale_i);

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.perm_rationale);
        builder.setView(v);
        builder.setIcon(R.drawable.ic_atak_launcher);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.i_understand,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        a.requestPermissions(PermissionsList, REQUEST_ID);
                    }
                });
        builder.setNegativeButton(R.string.perm_override_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showPermissionWarning(a);
                    }
                });
        final AlertDialog ad = builder.create();
        ad.show();
        setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE, Color.RED);
        setButtonColor(ad, DialogInterface.BUTTON_POSITIVE, Color.GREEN);
    }

    private static void generateAndAddRational(Activity a, LinearLayout ll,
            int permText, int permRationale) {
        LayoutInflater li = LayoutInflater.from(a);
        View v = li.inflate(R.layout.permission_rationale_item, null);
        ((TextView) v.findViewById(R.id.permission_summary)).setText(permText);
        ((TextView) v.findViewById(R.id.permission_rationale))
                .setText(permRationale);
        ll.addView(v);

    }

    @TargetApi(23)
    private static void showPermissionWarning(Activity a) {

        LayoutInflater li = LayoutInflater.from(a);
        View v = li.inflate(R.layout.permission_override, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.perm_override_title);
        builder.setView(v);
        builder.setIcon(R.drawable.ic_atak_launcher);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.i_understand,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AtakPreferences prefs = AtakPreferences.getInstance(a);
                        prefs.set(PREF_OVERRIDE_PERMISSION_KEY, true);
                        a.requestPermissions(PermissionsList, REQUEST_ID);
                    }
                });
        builder.setPositiveButton(R.string.go_back,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showRationalAndRequest(a);
                    }
                });
        final AlertDialog ad = builder.show();
        ad.show();
        setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE, Color.RED);
        setButtonColor(ad, DialogInterface.BUTTON_POSITIVE, Color.GREEN);

    }

    @TargetApi(30)
    private static void showFileSystemWarning(final Activity a) {
        LayoutInflater li = LayoutInflater.from(a);
        View v = li.inflate(R.layout.storage_permission_guidance, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.file_system_access_changes);
        builder.setView(v);
        builder.setIcon(R.drawable.ic_database);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.i_understand,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Uri uri = Uri
                                .parse("package:" + BuildConfig.APPLICATION_ID);
                        final Intent intent = new Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                uri);
                        a.startActivityForResult(intent, REQUEST_ID);
                    }
                });

        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        a.finish();
                    }
                });

        final AlertDialog ad = builder.create();
        ad.show();
        setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE, Color.RED);
        setButtonColor(ad, DialogInterface.BUTTON_POSITIVE, Color.GREEN);

    }

    @TargetApi(23)
    private static void showLocationBackgroundWarning(final Activity a) {
        LayoutInflater li = LayoutInflater.from(a);
        View v = li.inflate(R.layout.background_location, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.use_your_location_title);
        builder.setView(v);
        builder.setIcon(R.drawable.ic_menu_mylocation);

        builder.setCancelable(false);
        builder.setPositiveButton(R.string.i_understand,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            View view = LayoutInflater.from(a)
                                    .inflate(
                                            R.layout.location_permission_warning,
                                            null);

                            AlertDialog.Builder ab = new AlertDialog.Builder(a);
                            ab.setTitle(R.string.android_11_warning);
                            ab.setView(view);
                            ab.setCancelable(false);
                            ab.setNegativeButton(R.string.perm_override_title,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            AtakPreferences prefs = AtakPreferences
                                                    .getInstance(null);
                                            prefs.set(
                                                    PREF_OVERRIDE_BACKGROUND_LOCATION_KEY,
                                                    true);
                                            a.requestPermissions(
                                                    dummyPermissionList,
                                                    REQUEST_ID);
                                        }
                                    });

                            ab.setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            a.requestPermissions(
                                                    locationPermissionsList,
                                                    LOCATION_REQUEST_ID);
                                        }
                                    });
                            final AlertDialog ad = ab.create();
                            ad.show();
                            setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE,
                                    Color.RED);
                            setButtonColor(ad, DialogInterface.BUTTON_POSITIVE,
                                    Color.GREEN);

                        } else {
                            a.requestPermissions(locationPermissionsList,
                                    LOCATION_REQUEST_ID);
                        }
                    }
                });
        AlertDialog ad = builder.create();
        try {
            ad.show();
            setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE, Color.RED);
            setButtonColor(ad, DialogInterface.BUTTON_POSITIVE, Color.GREEN);

        } catch (Exception ignored) {
        }
    }

    static void displayNeverAskAgainDialog(final Activity a) {

        View view = LayoutInflater.from(a).inflate(
                R.layout.general_permission_guidance,
                null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final int result = a.checkSelfPermission(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            if (result != PackageManager.PERMISSION_GRANTED) {
                view = LayoutInflater.from(a).inflate(
                        R.layout.location_permission_guidance,
                        null);
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.required_missing_permissions);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.i_understand,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setAction(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", a.getPackageName(),
                                null);
                        intent.setData(uri);
                        a.startActivityForResult(intent, REQUEST_ID);
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            builder.setNeutralButton(R.string.perm_override_title,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            a.requestPermissions(dummyPermissionList,
                                    REQUEST_ID);
                        }
                    });
        }
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        a.finish();
                    }
                });
        AlertDialog ad = builder.show();
        ad.show();
        setButtonColor(ad, DialogInterface.BUTTON_NEGATIVE, Color.WHITE);
        setButtonColor(ad, DialogInterface.BUTTON_NEUTRAL, Color.RED);
        setButtonColor(ad, DialogInterface.BUTTON_POSITIVE, Color.GREEN);
    }

    /**
     * Check to make sure that the required permission has been granted.
     * @param context the context
     * @param permission the permission
     * @return true if the permission has been granted.
     */
    public static boolean checkPermission(final Context context,
            final String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        final int result = context.checkSelfPermission(permission);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            Log.e(TAG, "permission denied: " + permission, new Exception());
            return false;
        }

    }

    /**
     * Handles the mechanics of the permission request.
     * @param requestCode must be Permissions.REQUEST_ID
     * @param permissions the list of permissions requested
     * @param grantResults the results for each of the permissions
     * @return true if the permissions have all been granted
     */
    static boolean onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult called: " + requestCode);

        AtakPreferences prefs = AtakPreferences.getInstance(null);
        boolean override = prefs.get(PREF_OVERRIDE_PERMISSION_KEY, false);
        if (override)
            return true;

        switch (requestCode) {
            case Permissions.LOCATION_REQUEST_ID:
            case Permissions.REQUEST_ID:
                if (grantResults.length > 0) {
                    boolean b = true;
                    for (int i = 0; i < grantResults.length; ++i) {
                        if (isValidPermission(permissions[i])) {
                            b = b && (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                                Log.d(TAG,
                                        "onRequestPermissionResult not granted: "
                                                + permissions[i]);
                        }
                    }

                    return b;

                }
        }
        return false;

    }

    private static boolean isValidPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        .equals(permission))
            return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                && Manifest.permission.REQUEST_DELETE_PACKAGES
                        .equals(permission))
            return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                && Manifest.permission.READ_PHONE_NUMBERS
                        .equals(permission))
            return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && (Manifest.permission.WRITE_EXTERNAL_STORAGE
                        .equals(permission) ||
                        Manifest.permission.READ_EXTERNAL_STORAGE
                                .equals(permission)))
            return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                (Manifest.permission.READ_MEDIA_IMAGES.equals(permission) ||
                        Manifest.permission.READ_MEDIA_VIDEO.equals(permission)
                        ||
                        Manifest.permission.READ_MEDIA_AUDIO
                                .equals(permission)))
            return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                (Manifest.permission.ACCESS_MEDIA_LOCATION.equals(permission)))
            return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                (Manifest.permission.NEARBY_WIFI_DEVICES.equals(permission)))
            return false;

        // for Android 31 and above you need to request different bluetooth permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                (Manifest.permission.BLUETOOTH_SCAN.equals(permission) ||
                        Manifest.permission.BLUETOOTH_ADVERTISE
                                .equals(permission)
                        ||
                        Manifest.permission.BLUETOOTH_CONNECT
                                .equals(permission))) {
            return false;
        }

        // for android S and above do not request BLUETOOTH ADMIN or BLUETOOTH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (Manifest.permission.BLUETOOTH_ADMIN.equals(permission) ||
                        Manifest.permission.BLUETOOTH.equals(permission))) {
            return false;
        }

        // for android S and above request notifications
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            return false;
        }

        return true;
    }

    /**
     * Request the base permissions for atak only if override is not set.
     * @param a the activity
     */
    @TargetApi(23)
    public static void requestBasePermissions(Activity a) {
        AtakPreferences prefs = AtakPreferences.getInstance(a);
        boolean override = prefs.get(PREF_OVERRIDE_PERMISSION_KEY, false);
        if (!override)
            a.requestPermissions(PermissionsList, REQUEST_ID);
        else
            a.requestPermissions(dummyPermissionList, REQUEST_ID);
    }

    /**
     * Request the location permissions for atak only if override is not set.
     * @param a the activity
     */
    @TargetApi(23)
    public static void requestLocationPermissions(Activity a) {
        AtakPreferences prefs = AtakPreferences.getInstance(a);
        boolean override = prefs.get(PREF_OVERRIDE_BACKGROUND_LOCATION_KEY,
                false);
        if (!override)
            a.requestPermissions(locationPermissionsList, LOCATION_REQUEST_ID);
        else
            a.requestPermissions(dummyPermissionList, REQUEST_ID);

    }

    private static void setButtonColor(final AlertDialog ad, final int id,
            final int color) {
        Button button = ad.getButton(id);
        if (button != null)
            button.setTextColor(color);
    }
}
