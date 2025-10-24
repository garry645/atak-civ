
package com.atakmap.app.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.app.ATAKDatabaseHelper;
import com.atakmap.app.R;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.os.FileObserver;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class PreferenceManagementFragment extends AtakPreferenceFragment {
    private ListPreference loadPrefs;
    private ListPreference loadPartialPrefs;
    private PreferenceControl pc;
    private FileChecker prefFileObserver;
    private FileChecker partialPrefFileObserver;
    private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

    private final static String extension = ".pref";

    public PreferenceManagementFragment() {
        super(R.xml.preference_management, R.string.preference_management);
    }

    public static List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                PreferenceManagementFragment.class,
                R.string.preference_management,
                R.drawable.blank);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceID());

        pc = PreferenceControl.getInstance(getActivity());
        pc.connect();
        Preference savePrefs = findPreference("savePrefs");
        savePrefs
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference,
                                    Object newValue) {
                                Log.d(TAG, "saving the setting: " + newValue
                                        + extension);
                                pc.saveSettings(newValue + extension);
                                if (prefFileObserver != null)
                                    prefFileObserver.loadFiles();
                                return true;
                            }
                        });

        Preference loadPreference = findPreference("loadPrefs");
        loadPrefs = (ListPreference) loadPreference;
        loadPrefs
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference,
                                    Object newValue) {
                                pc.loadSettings((String) newValue, true);
                                return true;
                            }
                        });

        if (prefFileObserver != null) {
            prefFileObserver.stopWatching();
            prefFileObserver = null;
        }
        prefFileObserver = new FileChecker(PreferenceControl.DIRPATH);
        prefFileObserver.loadFiles();
        prefFileObserver.startWatching();

        // Attach a file observer to the preferences file and watch for changes
        if (partialPrefFileObserver != null) {
            partialPrefFileObserver.stopWatching();
            partialPrefFileObserver = null;
        }
        partialPrefFileObserver = new FileChecker(PreferenceControl.DIRPATH);
        partialPrefFileObserver.loadPartialFiles();
        partialPrefFileObserver.startWatching();

        Preference readyClone = findPreference("prepareForClone");
        readyClone
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(
                                    Preference preference) {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                                        getActivity());
                                alertBuilder
                                        .setIcon(
                                                com.atakmap.android.util.ATAKConstants
                                                        .getIconId());
                                alertBuilder
                                        .setTitle(R.string.preferences_text461)
                                        .setMessage(
                                                R.string.preferences_text462)
                                        .setPositiveButton(R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialog,
                                                            int which) {
                                                        dialog.dismiss();
                                                        prepareForClone();
                                                    }
                                                })
                                        .setNegativeButton(R.string.cancel,
                                                null);

                                AlertDialog dialog = alertBuilder.create();
                                dialog.setCancelable(false);
                                dialog.show();

                                return true;
                            }
                        });
        final AtakPreferences sp = AtakPreferences.getInstance(getActivity());
        spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {

                if (key == null)
                    return;

                switch (key) {
                    case "loadPrefs":
                        String s = sharedPreferences.getString(key, null);
                        if (s != null) {
                            ListPreference attribute = (ListPreference) findPreference(
                                    key);
                            attribute.setValue(s);
                        }
                        break;
                }
            }
        };

        sp.registerListener(spChanged);
    }

    private void prepareForClone() {
        final AtakPreferences sp = AtakPreferences.getInstance(getActivity());
        sp.remove("bestDeviceUID");
        sp.remove("locationCallsign");

        ATAKDatabaseHelper.removeDatabases();

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                getActivity());
        alertBuilder
                .setTitle(R.string.preferences_text463)
                .setMessage(
                        R.string.preferences_text464)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                Intent intent = new Intent();
                                intent.setAction("com.atakmap.app.QUITAPP");
                                intent.putExtra("FORCE_QUIT", true);
                                AtakBroadcast.getInstance().sendBroadcast(
                                        intent);
                            }
                        });

        AlertDialog dialog = alertBuilder.create();
        dialog.setCancelable(false);
        dialog.show();

    }

    private class FileChecker extends FileObserver {
        class ConfigFilter implements FilenameFilter {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(extension);
            }
        }

        FileChecker(String path) {
            super(path, FileObserver.CREATE | FileObserver.DELETE
                    | FileObserver.MOVED_FROM
                    | FileObserver.MOVED_TO); // We only care about modification events
        }

        @Override
        public void onEvent(int event, String path) {
            if (path != null && !path.contains("/")) {
                loadFiles();
                loadPartialFiles();
            }
        }

        void loadFiles() {
            File configDirectory = new File(PreferenceControl.DIRPATH);
            String[] files = IOProviderFactory.list(configDirectory,
                    new ConfigFilter());
            if (files == null || files.length == 0) {
                SpannableString msg = new SpannableString(
                        getString(R.string.preferences_text467));
                msg.setSpan(new ForegroundColorSpan(Color.BLACK), 0,
                        msg.length(), 0);
                // Not the best way to do it, but it works
                loadPrefs.setDialogMessage(msg);
            } else if (loadPrefs != null) {
                loadPrefs.setDialogMessage(null);
                loadPrefs.setEntries(files);
                loadPrefs.setEntryValues(files);
            }
        }

        void loadPartialFiles() {
            File configDirectory = new File(PreferenceControl.DIRPATH);
            String[] files = IOProviderFactory.list(configDirectory,
                    new ConfigFilter());
            if (files == null || files.length == 0) {
                SpannableString msg = new SpannableString(
                        getString(R.string.preferences_text467));
                msg.setSpan(new ForegroundColorSpan(Color.BLACK), 0,
                        msg.length(), 0);
                // Not the best way to do it, but it works
                if (loadPartialPrefs != null)
                    loadPartialPrefs.setDialogMessage(msg);
            } else if (loadPartialPrefs != null) {
                loadPartialPrefs.setDialogMessage(null);
                loadPartialPrefs.setEntries(files);
                loadPartialPrefs.setEntryValues(files);
            }
        }

    }

    @Override
    public void onDestroy() {
        if (prefFileObserver != null) {
            prefFileObserver.stopWatching();
        }
        pc.disconnect();
        super.onDestroy();
    }
}
