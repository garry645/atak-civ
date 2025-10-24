
package com.atakmap.android.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import com.atak.plugins.impl.AtakPluginRegistry;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.update.http.ApkFileRequest;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.util.Disposable;

/**
 * Support checking for updates
 */
public class ApkUpdateReceiver extends BroadcastReceiver implements Disposable {
    private static final String TAG = "ApkUpdateReceiver";

    final public static String DOWNLOAD_APK = "com.atakmap.app.DOWNLOAD_APK";
    final public static String APP_ADDED = "com.atakmap.app.APP_ADDED";
    final public static String APP_REMOVED = "com.atakmap.app.APP_REMOVED";

    private MapView _mapView;
    private ApkDownloader _downloader;

    private final Set<String> _outstandingInstalls = new HashSet<>();

    public ApkUpdateReceiver(MapView mapView) {
        _mapView = mapView;
        _downloader = new ApkDownloader(mapView);
    }

    @Override
    public void dispose() {
        _mapView = null;
        if (_downloader != null) {
            _downloader.dispose();
            _downloader = null;
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        final String action = intent.getAction();

        if (action == null)
            return;

        switch (action) {
            case DOWNLOAD_APK:
                final String url = intent.getStringExtra("url");
                final String packageName = intent.getStringExtra("package");
                final String hash = intent.getStringExtra("hash");
                final String filename = intent.getStringExtra("filename");
                String apkDir = intent.getStringExtra("apkDir");
                final boolean http2 = intent.getBooleanExtra("http2", false);
                final String token = intent.getStringExtra("token");
                final boolean bInstall = intent.getBooleanExtra("install",
                        false);

                if (FileSystemUtils.isEmpty(url)
                        || FileSystemUtils.isEmpty(packageName)
                        || FileSystemUtils.isEmpty(filename)) {
                    Log.w(TAG, "Failed to download APK, no URL/filename");
                    NotificationUtil.getInstance().postNotification(
                            ApkDownloader.notificationId,
                            R.drawable.ic_network_error_notification_icon,
                            NotificationUtil.RED,
                            _mapView.getContext().getString(R.string.app_name)
                                    + " Update Failed",
                            "Download URL not set",
                            "Download URL not set");
                    return;
                }

                //start download
                if (apkDir == null) {
                    apkDir = FileSystemUtils
                            .getItem(
                                    RemoteProductProvider.REMOTE_REPO_CACHE_PATH)
                            .getAbsolutePath();
                    if (!new File(apkDir).mkdirs())
                        Log.d(TAG, "could not wrap: " + apkDir);
                }

                ApkFileRequest request = new ApkFileRequest(packageName, url,
                        filename,
                        apkDir, ApkDownloader.notificationId, bInstall, false,
                        hash, ApkDownloader.getBasicUserCredentials(url));
                request.setHttp2(http2);
                if (token != null)
                    request.setBearerToken(token);
                _downloader.downloadAPK(request);
                break;
            case Intent.ACTION_PACKAGE_ADDED: {
                final String pkg = getPackageName(intent);
                Log.d(TAG, "ACTION_PACKAGE_ADDED: " + pkg);
                final String appName = AppMgmtUtils.getAppNameOrPackage(context,
                        pkg);

                if (pkg != null && pkg.startsWith("com.atakmap.app.flavor.") ||
                        pkg != null && pkg
                                .startsWith(
                                        "com.atakmap.app.flavor.encryption.")) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                            ATAKApplication.getCurrentActivity());
                    alertDialog.setTitle(R.string.system_plugin_detected);
                    alertDialog.setMessage(
                            R.string.system_plugin_detected_message);
                    alertDialog.setPositiveButton(R.string.quit,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    AtakBroadcast.getInstance().sendBroadcast(
                                            new Intent(
                                                    "com.atakmap.app.QUITAPP")
                                                            .putExtra(
                                                                    "FORCE_QUIT",
                                                                    true));
                                }
                            });
                    runOnCurrent(context, new Runnable() {
                        @Override
                        public void run() {
                            alertDialog.show();
                        }
                    });
                }

                // AtakPluginRegistry.get could be called while ATAK is still initializing and
                // because the order of the PluginMapComponent initialization is after ApkkUpdateReceiver
                // we can safely assum that AtakPluginRegistry will be null.    Protect against the
                // null case.

                final AtakPluginRegistry apr = AtakPluginRegistry.get();
                if (apr == null) {
                    Log.d(TAG, "installation occurred during startup: " + pkg);
                    return;
                }

                //first check if this is an unloaded plugin
                if (!apr.isPluginLoaded(pkg)
                        && apr.isPlugin(pkg)) {
                    //see if ATAK initiated the install
                    boolean bWaitingAppInstall = removeInstalling(pkg);
                    if (bWaitingAppInstall) {
                        //it is a plugin we were waiting on, go ahead and load into ATAK
                        Log.d(TAG, "Loading plugin into ATAK: " + appName);
                        if (apr.loadPlugin(pkg)) {
                            toast(context, "Loaded plugin: " + appName,
                                    Toast.LENGTH_SHORT);
                        }
                    } else {
                        // if the AppMgmtActivity is showing use the activity context,
                        // otherwise use the other context
                        final Context c = AppMgmtActivity
                                .getActivityContext() != null
                                        ? AppMgmtActivity.getActivityContext()
                                        : context;

                        //prompt user to load plugin into ATAK
                        AlertDialog.Builder dialog = new AlertDialog.Builder(c)
                                .setTitle(
                                        String.format(context
                                                .getString(
                                                        R.string.load_plugins),
                                                appName))
                                .setMessage(
                                        String.format(
                                                context.getString(
                                                        R.string.plugin_prompt),
                                                context
                                                        .getString(
                                                                R.string.app_name),
                                                appName))
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                Log.d(TAG,
                                                        "Loading sideloaded plugin into ATAK, per user");
                                                if (apr.loadPlugin(pkg)) {

                                                    AtakBroadcast
                                                            .getInstance()
                                                            .sendBroadcast(
                                                                    new Intent(
                                                                            ProductProviderManager.PRODUCT_REPOS_REFRESHED));
                                                    if (apr.isPluginLoaded(
                                                            pkg)) {
                                                        toast(c,
                                                                String.format(
                                                                        context.getString(
                                                                                R.string.loaded_plugins),
                                                                        appName),
                                                                Toast.LENGTH_SHORT);
                                                    }
                                                } else {
                                                    incompatiblePluginWarning(c,
                                                            pkg);
                                                }
                                            }
                                        })
                                .setNegativeButton(R.string.cancel, null);

                        Drawable icon = AppMgmtUtils.getAppDrawable(context,
                                pkg);
                        if (icon != null)
                            dialog.setIcon(
                                    AppMgmtUtils.getDialogIcon(context, icon));
                        else
                            dialog.setIcon(R.drawable.ic_menu_plugins);
                        dialog.setCancelable(false);

                        runOnCurrent(context, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    dialog.show();
                                } catch (Exception ignored) {
                                    // bandaid for ATAK-18401 Playstore Crash: ApkUpdateReceiver RuntimeException
                                }
                            }
                        });
                    }
                }

                //whether plugin or app, initiated by ATAK or not. Notify listeners of app so they can refresh
                AtakBroadcast.getInstance().sendBroadcast(new Intent(APP_ADDED)
                        .putExtra("package", pkg));
                break;
            }
            case Intent.ACTION_PACKAGE_REMOVED: {
                final String pkg = getPackageName(intent);
                Log.d(TAG, "ACTION_PACKAGE_REMOVED: " + pkg);

                //if it was a plugin, clear out plugin registry
                AtakPluginRegistry apr = AtakPluginRegistry.get();
                if (apr != null && apr.unloadPlugin(pkg)) {
                    toast(
                            context,
                            context.getString(R.string.plugin_uninstalled)
                                    + AppMgmtUtils
                                            .getAppNameOrPackage(context, pkg),
                            Toast.LENGTH_LONG);
                }

                AtakBroadcast.getInstance()
                        .sendBroadcast(new Intent(APP_REMOVED)
                                .putExtra("package", pkg));
                break;
            }
        }
    }

    private void incompatiblePluginWarning(final Context c, final String pkg) {

        //now prompt user to fully uninstall
        String label = AppMgmtUtils.getAppNameOrPackage(c,
                pkg);
        if (FileSystemUtils.isEmpty(label)) {
            label = "Plugin";
        }

        final Drawable icon = AppMgmtUtils.getAppDrawable(c, pkg);
        //final int sdk = AppMgmtUtils.getTargetSdkVersion(c, pkg);
        final String api = AtakPluginRegistry.getPluginApiVersion(
                c, pkg, true);
        final boolean sig = AtakPluginRegistry.verifySignature(c, pkg);

        String reason = "";

        final String versionBrand = ATAKConstants.getVersionBrand();
        if (!ATAKConstants.getPluginApi(true).equals(api) &&
                !ATAKConstants.getPluginApi(true).replace(versionBrand, "CIV")
                        .equals(api)) {
            reason = String.format(c.getString(R.string.reason1), api,
                    ATAKConstants.getPluginApi(true));
        }
        if (!sig) {
            if (!FileSystemUtils.isEmpty(reason))
                reason = reason + "\n\n\n";
            reason = reason + c.getString(R.string.reason2) + "\n";
        }
        if (FileSystemUtils.isEmpty(reason)) {
            reason = c.getString(R.string.reason3);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(c)
                .setTitle(String.format(
                        c.getString(R.string.load_failure_title), label))
                .setMessage(reason)
                .setPositiveButton(R.string.uninstall,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                AppMgmtUtils.uninstall(
                                        c,
                                        pkg);
                            }
                        })
                .setNegativeButton(R.string.cancel, null);
        if (icon != null)
            builder.setIcon(AppMgmtUtils.getDialogIcon(c, icon));
        else
            builder.setIcon(R.drawable.ic_menu_plugins);
        AlertDialog dialog = builder.create();

        runOnCurrent(c, new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });

    }

    private static String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        return uri != null ? uri.getSchemeSpecificPart() : null;
    }

    /**
     * Add to the list of outstanding apps that are being installed
     * @param product the product information to queue up for installation
     */
    public void addInstalling(final ProductInformation product) {
        Log.d(TAG, "addInstalling: " + product.getPackageName());
        synchronized (_outstandingInstalls) {
            _outstandingInstalls.add(product.getPackageName());
        }
    }

    /**
     * Remove the specified app from list of those outstanding to be installed
     * @param pkg the package to remove
     * @return  true if app was in the list
     */
    private boolean removeInstalling(final String pkg) {
        Log.d(TAG, "removeInstalling: " + pkg);
        synchronized (_outstandingInstalls) {
            return _outstandingInstalls.remove(pkg);
        }
    }

    /**
     * Helper method for toasting in this class
     * @param context runs on the currently active context which could be either the mapview
     *                or the settings activity
     * @param msg the message to toast
     * @param length the length or duration of the toast - see SHORT or LONG
     */
    private void toast(final Context context, final String msg,
            final int length) {
        runOnCurrent(context, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, length).show();
            }
        });
    }

    /**
     * Runs a runnable on the currently showing activity either MapView or Settings
     * @param context the context that is currently available and would be considered the default
     *                if the app mgmt activity is not open.
     * @param r the runnable
     */
    private void runOnCurrent(Context context, final Runnable r) {
        final Activity currentContext = ATAKApplication.getCurrentActivity();
        currentContext.runOnUiThread(r);
    }
}
