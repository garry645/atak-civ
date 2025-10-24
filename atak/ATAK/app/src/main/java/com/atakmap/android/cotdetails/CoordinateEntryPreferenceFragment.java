
package com.atakmap.android.cotdetails;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;

public final class CoordinateEntryPreferenceFragment
        extends AtakPreferenceFragment {

    public static final String TAG = "CoordinateEntryPreferenceFragment";

    public CoordinateEntryPreferenceFragment() {
        super(R.xml.coordentry_preference_fragment,
                R.string.coordEntryPreference_summary);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.toolPreferences),
                getSummary());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.addPreferencesFromResource(getResourceID());

        final Preference pullElevationMode = findPreference(
                "pull_elevation_mode");

        pullElevationMode.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showPicker(getActivity());
                        return true;
                    }
                });
    }

    public static void showPicker(Context context) {

        final AlertDialog.Builder adb = new AlertDialog.Builder(context);

        int selected = 1;

        final AtakPreferences prefs = AtakPreferences.getInstance(context);
        final String[] options = context.getResources()
                .getStringArray(R.array.pull_elevation_modes_values);
        String userPref = prefs.get("pull_elevation_mode", "best");
        for (int i = 0; i < options.length; ++i) {
            if (options[i].equals(userPref))
                selected = i;
        }

        adb.setSingleChoiceItems(R.array.pull_elevation_modes_names, selected,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int n) {
                        prefs.set("pull_elevation_mode", options[n]);
                    }

                });
        adb.setNegativeButton(R.string.ok, null);
        adb.setTitle(R.string.pull_elevation_pref_title);
        adb.show();
    }
}
