
package com.atakmap.android.routes;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.gui.ColorPalette.OnColorSelectedListener;
import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;

import java.util.List;

public class RoutePreferenceFragment extends AtakPreferenceFragment {

    public static final String TAG = "RoutePreferenceFragment";

    public static List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                RoutePreferenceFragment.class,
                R.string.routePreferences,
                R.drawable.ic_menu_settings);
    }

    public RoutePreferenceFragment() {
        super(R.xml.route_preferences, R.string.routePreferences);
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

        final Preference defaultRouteColor = findPreference(
                "defaultRouteColor");
        defaultRouteColor
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(
                                    Preference preference) {

                                final AtakPreferences _prefs = AtakPreferences
                                        .getInstance(
                                                getActivity());

                                AlertDialog.Builder b = new AlertDialog.Builder(
                                        getActivity());
                                b.setTitle(defaultRouteColor.getTitle());
                                int color = Color.WHITE;
                                try {
                                    color = Integer.parseInt(_prefs.get(
                                            "defaultRouteColor",
                                            Integer.toString(Color.WHITE)));
                                } catch (Exception e) {
                                    Log.d(TAG,
                                            "error occurred getting preference");
                                }
                                ColorPalette palette = new ColorPalette(
                                        getActivity());
                                palette.setColor(color);
                                b.setView(palette);
                                final AlertDialog alert = b.create();
                                OnColorSelectedListener l = new OnColorSelectedListener() {
                                    @Override
                                    public void onColorSelected(int color,
                                            String label) {
                                        _prefs.set("defaultRouteColor",
                                                Integer.toString(color));
                                        alert.dismiss();
                                    }
                                };
                                palette.setOnColorSelectedListener(l);
                                alert.show();
                                return true;
                            }
                        });

        ((PanEditTextPreference) findPreference("waypointBubble.Walking"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("waypointBubble.Driving"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("waypointBubble.Flying"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("waypointBubble.Swimming"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("waypointBubble.Watercraft"))
                .checkValidInteger();
    }

}
