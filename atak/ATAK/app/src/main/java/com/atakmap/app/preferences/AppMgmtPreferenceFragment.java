
package com.atakmap.app.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.importfiles.ui.ImportManagerFileBrowser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.android.update.AppMgmtActivity;
import com.atakmap.android.update.AppMgmtUtils;
import com.atakmap.android.update.FileSystemProductProvider;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.app.R;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.net.AtakCertificateDatabaseIFace;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

public class AppMgmtPreferenceFragment extends AtakPreferenceFragment {

    public static final String TAG = "AppMgmtPreferenceFragment";

    public static final String PREF_ATAK_UPDATE_LOCAL_PATH = "atakUpdateLocalPathString";

    public static java.util.List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                AppMgmtPreferenceFragment.class,
                R.string.app_mgmt_settings,
                R.drawable.ic_menu_plugins);
    }

    public AppMgmtPreferenceFragment() {
        super(R.xml.app_mgmt_preferences, R.string.app_mgmt_settings);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.app_mgmt_text1),
                getSummary());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final AtakPreferences _prefs = AtakPreferences.getInstance(null);
        final Activity activity = getActivity();
        addPreferencesFromResource(getResourceID());

        CheckBoxPreference appMgmtEnableUpdateServer = (CheckBoxPreference) findPreference(
                "appMgmtEnableUpdateServer");
        appMgmtEnableUpdateServer.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue) {
                        if (newValue instanceof Boolean) {
                            Boolean bChecked = (Boolean) newValue;
                            if (bChecked) {
                                String updateUrl = _prefs.get("atakUpdateServerUrl", getString(
                                        R.string.atakUpdateServerUrlDefault));

                                if (FileSystemUtils.isEmpty(updateUrl)) {
                                    Toast.makeText(activity,
                                                    R.string.please_set_the_sync_server_url,
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    return true;
                                }
                                new AlertDialog.Builder(activity)
                                        .setIcon(
                                                ATAKConstants
                                                        .getIconId())
                                        .setTitle(R.string.sync_updates)
                                        .setMessage(
                                                "Would you like to sync with " +
                                                        _prefs.get("atakUpdateServerUrl", getString(
                                                                        R.string.atakUpdateServerUrlDefault))
                                                        + "?")
                                        .setPositiveButton(
                                                R.string.sync,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialog,
                                                            int which) {
                                                        Log.d(TAG,
                                                                "User has enabled remote sync");

                                                        //lets sync now
                                                        activity.finish();

                                                        //send intent to sync, giving the pref state a chance to update
                                                        AtakBroadcast
                                                                .getInstance()
                                                                .sendBroadcast(
                                                                        new Intent(
                                                                                AppMgmtActivity.SYNC));

                                                    }
                                                })
                                        .setNegativeButton(R.string.later,
                                                null)
                                        .show();
                            } else {
                                //update server was disabled, lets re-sync now
                                Log.d(TAG,
                                        "User has disabled remote sync");

                                //close this pref UI so we can see sync dialog on previous activity
                                activity.finish();

                                AtakBroadcast
                                        .getInstance()
                                        .sendBroadcast(
                                                new Intent(
                                                        AppMgmtActivity.SYNC));
                            }
                        }

                        return true;
                    }
                });

        final TAKServer[] servers = TAKServerListener.getInstance().getServers();
        Preference atakQuickConfigUpdateServer =
                findPreference("atakQuickConfigUpdateServer");
        atakQuickConfigUpdateServer.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showTakServerPicker();
                return true;
            }
        });
        atakQuickConfigUpdateServer.setShouldDisableView(true);
        atakQuickConfigUpdateServer.setEnabled(servers != null && servers.length != 0);

        Preference atakUpdateServerUrl = findPreference("atakUpdateServerUrl");
        atakUpdateServerUrl.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            final Object newValue) {

                        if (newValue == null
                                || FileSystemUtils
                                .isEmpty((String) newValue)) {
                            Log.w(TAG, "URL is empty");
                            new AlertDialog.Builder(activity)
                                    .setIcon(
                                            R.drawable.importmgr_status_yellow)
                                    .setTitle(R.string.invalid_entry)
                                    .setMessage(
                                            R.string.please_enter_a_valid_url)
                                    .setPositiveButton(R.string.ok,
                                            null)
                                    .show();
                            return false;
                        }

                        final String url = (String) newValue;
                        if (FileSystemUtils
                                .isEquals(
                                        url,
                                        _prefs.get("atakUpdateServerUrl", getString(
                                                R.string.atakUpdateServerUrlDefault)))) {
                            Log.d(TAG, "URL has not changed");
                            return false;
                        }

                        Log.d(TAG, "User has updated remove server");

                        //close this pref UI so we can see sync dialog on previous activity
                       activity.finish();

                        MapView _mapView = MapView.getMapView();

                        if (_mapView != null) {
                            _mapView.post(new Runnable() {
                                @Override
                                public void run() {
                                    //send intent to sync, giving the pref state a chance to update
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(
                                                    new Intent(
                                                            AppMgmtActivity.SYNC)
                                                            .putExtra(
                                                                    "url",
                                                                    url));
                                }
                            });
                        }

                        return true;
                    }
                });

        Preference atakUpdateLocalPath = findPreference("atakUpdateLocalPath");
        atakUpdateLocalPath.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        promptDir();
                        return false;
                    }
                });

        Preference atakUpdateLocalPathReset = findPreference("atakUpdateLocalPathReset");
        atakUpdateLocalPathReset.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                        adb.setMessage(R.string.restore_update_local_path).
                                setTitle(R.string.reset).
                                setNegativeButton(R.string.cancel, null).
                                setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        _prefs.remove(PREF_ATAK_UPDATE_LOCAL_PATH);
                                    }
                                });
                        adb.show();
                        return false;
                    }
                });

        final Preference caLocation = findPreference("updateServerCaLocation");
        caLocation.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        NetworkConnectionPreferenceFragment.getCertFile(
                                activity,
                                getString(R.string.preferences_text412),
                                AtakCertificateDatabaseIFace.TYPE_UPDATE_SERVER_TRUST_STORE_CA,
                                false, null);
                        return false;
                    }
                });
    }

    private void promptDir() {
        final AtakPreferences _prefs = AtakPreferences.getInstance(null);

        final Activity context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        final ImportManagerFileBrowser importView = (ImportManagerFileBrowser) inflater
                .inflate(R.layout.import_manager_file_browser, null);

        final String _lastDirectory = _prefs.get(PREF_ATAK_UPDATE_LOCAL_PATH,
                        FileSystemUtils.getItem(
                                FileSystemProductProvider.LOCAL_REPO_PATH)
                                .getAbsolutePath());

        importView.setTitle(R.string.select_directory_to_import);
        importView.setStartDirectory(_lastDirectory);
        importView.setUseProvider(false);
        importView.allowAnyExtenstionType();
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setView(importView);
        b.setNegativeButton(R.string.cancel, null);
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User has selected items and touched OK. Import the data.
                List<File> selectedFiles = importView.getSelectedFiles();

                if (selectedFiles.isEmpty()) {
                    Toast.makeText(context,
                            R.string.no_import_directory,
                            Toast.LENGTH_SHORT).show();
                } else if (selectedFiles.size() > 1) {
                    Toast.makeText(context,
                            R.string.multiple_import_directory,
                            Toast.LENGTH_SHORT).show();
                } else {
                    setApkDir(context, selectedFiles.get(0));
                }
            }
        });
        final AlertDialog alert = b.create();

        // This also tells the importView to handle the back button presses
        // that the user provides to the alert dialog.
        importView.setAlertDialog(alert);

        // Show the dialog
        alert.show();

        AlertDialogHelper.adjustWidth(alert, .90);

        HintDialogHelper.showHint(getActivity(),
                getString(R.string.apk_directory_no_apks_hint_title),
                getString(R.string.apk_directory_no_apks_hint),
                "hint.atakUpdateLocalPath");
    }

    /**
     * Post backport, should generate a common listFiles(dir, filter, recurse)
     * within IOProviderFactor or outside in a helper class. See duplicative
     * implementation in FileSystemProductProvider
     * @param dir the directory to start searching from
     * @param recurse if the directory should be recursed
     * @return the list of files matched after recurse
     * @deprecated
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", removeAt = "5.6", forRemoval = true)
    List<File> listApks(File dir, boolean recurse) {
        ArrayList<File> apks = new ArrayList<>();
        File[] list = IOProviderFactory.listFiles(
                dir,
                AppMgmtUtils.APK_FilenameFilter);
        if(list != null)
            apks.addAll(Arrays.asList(list));
        if (recurse) {
            File[] subdirs = IOProviderFactory.listFiles(
                    dir,
                    new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isDirectory();
                        }
                    });
            if (subdirs != null) {
                for (File subdir : subdirs)
                    apks.addAll(listApks(subdir, recurse));
            }
        }
        return apks;
    }

    private void setApkDir(final Activity context, final File selected) {
        final boolean isZip = FileSystemUtils.isZip(selected);
        if (!FileSystemUtils.isFile(selected)
                || (!IOProviderFactory.isDirectory(selected) && !isZip)) {
            Toast.makeText(context,
                    R.string.no_import_directory,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<File> list = listApks(
                isZip ? new ZipVirtualFile(selected) : selected,
                isZip);
        if (list.isEmpty()) {
            Log.w(TAG, "setApkDir no APKs: " + selected.getAbsolutePath());
            new AlertDialog.Builder(getActivity())
                    .setIcon(
                            com.atakmap.android.util.ATAKConstants.getIconId())
                    .setTitle(getString(R.string.confirm_directory))
                    .setMessage(getString(R.string.apk_directory_no_apks, selected.getName()))
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface,
                                        int i) {
                                    setApkDir2(selected);
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            setApkDir2(selected);
        }
    }

    private void setApkDir2(File selected) {

        final AtakPreferences _prefs = AtakPreferences.getInstance(null);


        // Store the currently displayed directory
        _prefs.set(PREF_ATAK_UPDATE_LOCAL_PATH,
                        selected.getAbsolutePath());

        //remove old INF to be rebuilt from new custom dir
        File customInf = FileSystemUtils
                .getItem(FileSystemProductProvider.LOCAL_REPO_INDEX);
        if (FileSystemUtils.isFile(customInf)) {
            FileSystemUtils.delete(customInf);
        }

        //remove old INFZ to be rebuilt from new custom dir
        File customInfz = FileSystemUtils
                .getItem(FileSystemProductProvider.LOCAL_REPOZ_INDEX);
        if (FileSystemUtils.isFile(customInfz)) {
            FileSystemUtils.delete(customInfz);
        }

        Log.d(TAG, "Setting APK Dir: " + selected.getAbsolutePath());

        final Activity activity = getActivity();
        if (activity == null) {
            AtakBroadcast.getInstance()
                    .sendBroadcast(new Intent(AppMgmtActivity.SYNC));
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setIcon(
                        ATAKConstants.getIconId())
                .setTitle(R.string.sync_updates)
                .setMessage(
                        "Would you like to sync "
                                + _prefs.get(PREF_ATAK_UPDATE_LOCAL_PATH, "")
                                + "?")
                .setPositiveButton(
                        R.string.sync,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                Log.d(TAG,
                                        "User has enabled local dir sync");

                                //lets sync now
                                AppMgmtPreferenceFragment.this
                                        .getActivity()
                                        .finish();

                                AtakBroadcast.getInstance().sendBroadcast(
                                        new Intent(AppMgmtActivity.SYNC));
                            }
                        })
                .setNegativeButton(R.string.later,
                        null)
                .show();
    }


    private void showTakServerPicker() {

        AtakPreferences _prefs = AtakPreferences.getInstance(null);

        final TAKServer[] servers = TAKServerListener.getInstance().getServers();
        List<String> nameList = new ArrayList<>();
        for (TAKServer server: servers) {
            nameList.add(server.getDescription());
        }

        final Activity activity = getActivity();

        if (nameList.isEmpty()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,
                            R.string.no_tak_servers_configured, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // valid choices
        final String[] names = nameList.toArray(new String[0]);


        AlertDialog.Builder builder = new AlertDialog.Builder(activity); // Replace context with your activity's context
        builder.setTitle(R.string.configured_servers);

        String original = _prefs.get("atakUpdateServerUrl", null);


        builder.setCancelable(false);
        builder.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TAKServer server = servers[which];
                NetConnectString ncs = NetConnectString.fromString(server.getConnectString());
                String val = "https://" +  ncs.getHost() + ":8443/update";
                _prefs.set("atakUpdateServerUrl", val);
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AppMgmtPreferenceFragment.this
                        .getActivity()
                        .finish();
                AtakBroadcast.getInstance()
                        .sendBroadcast(new Intent(AppMgmtActivity.SYNC));
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (original == null)
                    _prefs.remove("atakUpdateServerUrl");
                else
                    _prefs.set("atakUpdateServerUrl", original);
            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }
}
