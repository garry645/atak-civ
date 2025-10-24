
package com.atakmap.app.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.app.R;
import com.atakmap.app.SettingsActivity;
import com.atakmap.comms.CommsProviderFactory;
import com.atakmap.coremap.filesystem.FileSystemUtils;

public class CallSignAndDeviceFragment extends AtakPreferenceFragment {

    public CallSignAndDeviceFragment() {
        super(R.xml.callsign_device_preferences,
                R.string.callsign_and_device_preferences);
    }

    public static java.util.List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                CallSignAndDeviceFragment.class,
                R.string.callsign_and_device_preferences,
                R.drawable.ic_menu_network);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceID());
        Preference callSignPrefs = findPreference("callSignPrefs");
        callSignPrefs.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new CallSignPreferenceFragment());
                        return true;
                    }
                });

        Preference devicePrefs = findPreference("devicePrefs");
        devicePrefs.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new DevicePreferenceFragment());
                        return true;
                    }
                });

        Preference bluetooth = findPreference("bluetooth");
        bluetooth.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new BluetoothPrefsFragment());
                        return true;
                    }
                });

        Preference contactPrefs = findPreference("contactPrefs");
        contactPrefs.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new ContactPreferenceFragment());
                        return true;
                    }
                });

        Preference reportingPrefs = findPreference("reportingPrefs");
        reportingPrefs.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new ReportingPreferenceFragment());
                        return true;
                    }
                });

        Preference encryptPassword = findPreference("encryptPassword");
        encryptPassword.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new EncryptionPreferenceFragment());
                        return true;
                    }
                });

        Preference prefManage = findPreference("prefManage");
        prefManage.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showScreen(new PreferenceManagementFragment());
                        return true;
                    }
                });
    }

    public static void promptIdentity(final Context context) {
        final AtakPreferences prefs = AtakPreferences.getInstance(context);

        View view = LayoutInflater.from(context).inflate(
                R.layout.callsign_identity, MapView.getMapView(), false);

        final EditText identityCallsign = view
                .findViewById(R.id.identityCallsign);
        final Spinner identityTeam = view
                .findViewById(R.id.identityTeam);
        final Spinner identityRole = view
                .findViewById(R.id.identityRole);

        if (!CommsProviderFactory.getProvider().isCallsignConfigurable()) {
            identityCallsign.setEnabled(false);
            identityTeam.setEnabled(false);
        }

        String name = prefs.get("locationCallsign", "");
        identityCallsign.setText(name);
        identityCallsign.setSelection(name.length());

        String locationTeam = prefs.get("locationTeam", "Cyan");
        String[] colors = context.getResources()
                .getStringArray(R.array.squad_values);
        identityTeam.setSelection(0);
        if (colors != null && !FileSystemUtils.isEmpty(locationTeam)) {
            for (int i = 0; i < colors.length; i++) {
                if (FileSystemUtils.isEquals(locationTeam, colors[i])) {
                    identityTeam.setSelection(i);
                    break;
                }
            }
        }

        final String[] roles = context.getResources()
                .getStringArray(R.array.role_values);
        final String atakRoleType = prefs.get("atakRoleType", roles[0]);

        identityRole.setSelection(0);
        if (roles != null && !FileSystemUtils.isEmpty(atakRoleType)) {
            for (int i = 0; i < roles.length; i++) {
                if (FileSystemUtils.isEquals(atakRoleType, roles[i])) {
                    identityRole.setSelection(i);
                    break;
                }
            }
        }

        identityTeam.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int position, long id) {
                        if (position < 0)
                            return;
                        String[] colors = context.getResources()
                                .getStringArray(R.array.squad_values);
                        final int pos = identityTeam.getSelectedItemPosition();
                        prefs.set("locationTeam", colors[pos]);
                    }
                });
        identityRole.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int position, long id) {
                        if (position < 0)
                            return;

                        final String myRole = identityRole.getSelectedItem()
                                .toString();
                        prefs.set("atakRoleType", myRole);
                    }
                });

        DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String callsign = String.valueOf(identityCallsign.getText());
                if (!callsign.isEmpty()) {
                    prefs.set("locationCallsign", callsign);
                }
                if (which == DialogInterface.BUTTON_NEGATIVE)
                    SettingsActivity.start(DevicePreferenceFragment.class);
            }
        };

        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(R.string.identity_title);
        adb.setIcon(R.drawable.my_prefs_settings);
        adb.setView(view);
        adb.setPositiveButton(R.string.done, onClick);
        adb.setNegativeButton(R.string.more, onClick);
        adb.setCancelable(true);
        adb.show();
    }

}
