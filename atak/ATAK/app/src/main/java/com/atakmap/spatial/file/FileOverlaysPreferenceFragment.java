
package com.atakmap.spatial.file;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.app.R;

import java.util.List;

public class FileOverlaysPreferenceFragment
        extends AtakPreferenceFragment {
    public static List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                FileOverlaysPreferenceFragment.class,
                R.string.fileOverlayPreferences,
                R.drawable.ic_menu_settings);
    }

    public FileOverlaysPreferenceFragment() {
        super(R.xml.file_overlay_preferences, R.string.fileOverlayPreferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        addPreferencesFromResource(getResourceID());
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.toolPreferences),
                getSummary());
    }

}
