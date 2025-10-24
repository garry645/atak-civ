
package com.atakmap.app.preferences;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.android.preference.UnitDisplayPreferenceFragment;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;

public class BasicDisplayPrefFragment extends AtakPreferenceFragment {

    public BasicDisplayPrefFragment() {
        super(R.xml.basic_display_settings, R.string.basic_display_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceID());

        Preference eyealtitude = findPreference("map_eyealt_visible");
        eyealtitude.setIcon(
                new BitmapDrawable(ATAKUtilities
                        .getUriBitmap("asset://icons/eyealtitude.png")));
        Preference rabPreference = findPreference("unitPreferences");
        rabPreference
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(
                                    Preference preference) {
                                showScreen(new UnitDisplayPreferenceFragment());
                                return true;
                            }
                        });
    }

    public static java.util.List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                BasicDisplayPrefFragment.class,
                R.string.basic_display_settings,
                R.drawable.ic_android_display_settings);
    }
}
