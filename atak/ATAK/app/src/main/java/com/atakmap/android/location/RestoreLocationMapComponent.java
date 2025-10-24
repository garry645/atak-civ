
package com.atakmap.android.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Timer;
import java.util.TimerTask;

public class RestoreLocationMapComponent extends AbstractMapComponent
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "RestoreLocationMapComponent";

    private AtakPreferences prefs;
    private Marker self;
    Timer timer = new Timer("RestoreLocationTimer");

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        prefs = AtakPreferences.getInstance(view.getContext());
        prefs.registerListener(this);
        self = view.getSelfMarker();

        String point = prefs.get("recordedLocation", null);
        if (point != null) {
            final GeoPoint gp = GeoPoint.parseGeoPoint(point);
            if (gp != null) {
                final MetaDataHolder2 data = view.getMapData();
                data.setMetaString("locationSourcePrefix", "mock");
                data.setMetaBoolean("mockLocationAvailable", true);
                data.setMetaString("mockLocation", gp.toStringRepresentation());
                data.setMetaLong("mockLocationTime",
                        SystemClock.elapsedRealtime() - 9800);

            }
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    record();
                }
            };

            timer.schedule(tt, 10000, 10000);

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key == null)
            return;

        if (key.equals("restoreRecordedLocation")) {
            record();
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        prefs.unregisterListener(this);
        timer.cancel();
        record();
    }

    private void record() {
        if (prefs.get("restoreRecordedLocation", false)) {
            GeoPoint gp = self.getPoint();
            if (!Double.isNaN(gp.getLatitude())
                    && !Double.isNaN(gp.getLongitude()))
                prefs.set("recordedLocation", gp.toStringRepresentation());
        } else {
            prefs.remove("recordedLocation");
        }
    }

}
