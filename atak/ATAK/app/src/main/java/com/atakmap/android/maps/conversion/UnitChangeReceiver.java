
package com.atakmap.android.maps.conversion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.log.Log;

public class UnitChangeReceiver extends BroadcastReceiver {

    public static final String TAG = "UnitChangeReceiver";

    public static final String DIST_ADJUST = "com.atakmap.android.maps.DIST_UNIT_ADJUST";
    public static final String ANGL_ADJUST = "com.atakmap.android.maps.ANGL_UNIT_ADJUST";

    private final MapView _mapView;
    private final AtakPreferences _prefs;

    public UnitChangeReceiver(MapView mapView) {
        _mapView = mapView;
        _prefs = AtakPreferences.getInstance(_mapView
                .getContext());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null)
            return;

        Log.e(TAG, "Action: " + action);
        switch (action) {
            case ANGL_ADJUST: {
                int index = intent.getIntExtra("index", -1);
                if (index != -1) {
                    _prefs.set("rab_brg_units_pref", String.valueOf(index));
                } else {
                    int currentIndex = Integer.parseInt(_prefs.get(
                            "rab_brg_units_pref", "0"));
                    currentIndex++;
                    _prefs.set("rab_brg_units_pref",
                            String.valueOf(currentIndex % 2));
                }
                break;
            }
            case DIST_ADJUST: {
                int index = intent.getIntExtra("index", -1);
                if (index != -1) {
                    _prefs.set("rab_rng_units_pref", String.valueOf(index));
                } else {
                    int currentIndex = Integer.parseInt(_prefs.get(
                            "rab_rng_units_pref", "0"));
                    currentIndex++;
                    _prefs.set("rab_rng_units_pref",
                            String.valueOf(currentIndex % 3));
                }
                break;
            }
        }

    }

}
