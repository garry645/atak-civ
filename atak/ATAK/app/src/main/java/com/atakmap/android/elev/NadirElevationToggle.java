
package com.atakmap.android.elev;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import gov.tak.api.util.Disposable;

class NadirElevationToggle implements Disposable, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "NadirElevationToggle";

    private final MobileImageryRasterLayer2 elevationLayer;
    private final MapView mapView;
    private final Map<String, Boolean> state = new HashMap<>();
    private final AtakPreferences pref;
    private final static String PREFERENCE_KEY = "elevationRenderOnNadir";

    private boolean elevationRenderOnNadir = true;


    NadirElevationToggle(final MapView view) {
        mapView = view;
        pref = AtakPreferences.getInstance(view.getContext());

        elevationLayer = findElevationLayer();

        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.MAP_TILT, medl);
        Log.d(TAG, "registering the elevation toggle");


        onSharedPreferenceChanged(pref.getSharedPrefs(), PREFERENCE_KEY);
        pref.registerListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {

        if (key == null)
            return;

        if (key.equals(PREFERENCE_KEY)) {
            elevationRenderOnNadir = pref.get(key, true);
            if (elevationRenderOnNadir) {
                Log.d(TAG, "restoring the elevation visualization");
                restoreElevationVisibility();
            } else {
                double tilt = mapView.getMapTilt();
                boolean currTilt = (Double.compare(tilt, 0d) == 0);
                if (currTilt) {
                    Log.d(TAG, "disabling the elevation visualization");
                    disableElevationVisibility();
                }
            }
        }
    }

    private final MapEventDispatcher.MapEventDispatchListener medl =
            new MapEventDispatcher.MapEventDispatchListener() {
        boolean prevTilt = true;
        @Override
        public void onMapEvent(MapEvent mapEvent) {
            if (!elevationRenderOnNadir) {
                double tilt = mapView.getMapTilt();
                boolean currTilt = (Double.compare(tilt, 0d) == 0);
                if (currTilt != prevTilt) {
                    if (prevTilt) {
                        Log.d(TAG, "restoring the elevation visualization");
                        restoreElevationVisibility();
                    } else {
                        Log.d(TAG, "disabling the elevation visualization");
                        disableElevationVisibility();
                    }
                    prevTilt = currTilt;
                }
            }
        }
    };


    private synchronized void restoreElevationVisibility() {
        Collection<String> selectionOptions = elevationLayer.getSelectionOptions();
        for (String s : selectionOptions) {
            Boolean b = state.get(s);
            if (b == null)
                b = true;

            elevationLayer.setVisible(s, b);
        }
    }

    private synchronized void disableElevationVisibility() {
        Collection<String> selectionOptions = elevationLayer.getSelectionOptions();
        for (String s : selectionOptions) {
            state.put(s, elevationLayer.isVisible(s));
            elevationLayer.setVisible(s, false);
        }
    }

    @Override
    public void dispose() {
        pref.unregisterListener(this);
        mapView.getMapEventDispatcher().removeMapEventListener(MapEvent.MAP_TILT, medl);
        restoreElevationVisibility();
        Log.d(TAG, "unregistering the elevation toggle");
    }

    /**
     * Note that this method uses reflection in order to grab the Elevation Layer.
     * Expect this to be formalized better in the future
     * @return the elevation layer
     */
    private MobileImageryRasterLayer2 findElevationLayer() {
        MapActivity activity = (MapActivity)mapView.getContext();
        MapComponent mc = activity.getMapComponent(ElevationMapComponent.class);
        if (mc instanceof ElevationMapComponent) {
            ElevationMapComponent emc = (ElevationMapComponent) mc;
            Field[] fields = ElevationMapComponent.class.getDeclaredFields();
            for (Field field: fields) {
                boolean accessible = field.isAccessible();
                if (!accessible)
                    field.setAccessible(true);
                try {
                    Object o = field.get(emc);
                    if (o instanceof MobileImageryRasterLayer2) {
                        return (MobileImageryRasterLayer2) o;
                    }
                } catch(Exception e) {
                    Log.e(TAG, "error with field: " + field);
                } finally {
                    if (!accessible)
                        field.setAccessible(false);
                }
            }
        }
        return null;
    }



}
