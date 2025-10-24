
package com.atakmap.app.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.gui.PanListPreference;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.app.R;

import java.util.List;

public class ContactPreferenceFragment extends AtakPreferenceFragment {
    private PreferenceCategory alternateContactCategory;
    private PanListPreference saSipAddressAssignment;
    private PanEditTextPreference saSipAddress;
    private String _previousSaSipAddressAssignment = "";
    private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

    public ContactPreferenceFragment() {
        super(R.xml.contact_preferences, R.string.preferences_text87);
    }

    public static List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                ContactPreferenceFragment.class,
                R.string.contact_preferences,
                R.drawable.blank);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceID());
        final AtakPreferences sp = AtakPreferences.getInstance(getActivity());
        alternateContactCategory = (PreferenceCategory) findPreference(
                "alternateContactCategory");
        saSipAddressAssignment = (PanListPreference) findPreference(
                "saSipAddressAssignment");
        saSipAddress = (PanEditTextPreference) findPreference("saSipAddress");
        if (saSipAddressAssignment != null) {
            String value = saSipAddressAssignment.getValue();
            if (value == null) {
                value = getString(R.string.voip_assignment_disabled);
                saSipAddressAssignment.setValue(value);
            }
            changeVisibilityVoIP(value);

            saSipAddressAssignment
                    .setOnPreferenceChangeListener(
                            new Preference.OnPreferenceChangeListener() {

                                @Override
                                public boolean onPreferenceChange(
                                        Preference arg0,
                                        Object arg1) {
                                    changeVisibilityVoIP((String) arg1);
                                    return true;
                                }

                            });
        }
        spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {

                if (key == null)
                    return;

                if ("saSipAddressAssignment".equals(key)) {
                    String s = sharedPreferences.getString(key, null);
                    if (s != null) {
                        ListPreference attribute = (ListPreference) findPreference(
                                key);
                        attribute.setValue(s);
                    }
                }
            }
        };

        sp.registerListener(spChanged);

        final Preference urnPrefs = findPreference("saURN");
        ((PanEditTextPreference) urnPrefs).setValidIntegerRange(0, 16777215,
                true);

    }

    private void changeVisibilityVoIP(String selection) {
        if (selection.equals(_previousSaSipAddressAssignment)) {
            return;
        }

        if (selection
                .equals(getString(R.string.voip_assignment_manual_entry))) {
            _previousSaSipAddressAssignment = selection;
            alternateContactCategory.addPreference(saSipAddress);
        } else {
            //manual entry is hidden for all other assignment methods
            _previousSaSipAddressAssignment = selection;
            alternateContactCategory
                    .removePreference(saSipAddress);
        }
    }
}
