
package com.atakmap.android.video;

import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.radiolibrary.NetworkCaptureDialog;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;

public class VideoPreferenceFragment extends AtakPreferenceFragment {

    public VideoPreferenceFragment() {
        super(R.xml.video_preferences, R.string.videoPreferences);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.toolPreferences),
                getSummary());
    }

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        addPreferencesFromResource(getResourceID());

        final Preference recordUdpTool = findPreference("recordUdpTool");
        if (recordUdpTool != null)
            recordUdpTool.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    NetworkCaptureDialog ncd = new NetworkCaptureDialog(ATAKApplication.getCurrentActivity());
                    ncd.show();
                    return true;
                }
            });
    }
}
