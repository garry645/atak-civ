
package com.atakmap.android.widgets;

import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.AtakMapView;

import gov.tak.api.util.Disposable;
import gov.tak.platform.util.LimitingThread;

/**
 * Text widget that is updated whenever the map focus point changes
 */
public class MapFocusTextWidget extends TextWidget implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        AtakMapView.OnMapMovedListener,
        Disposable {

    private UnitPreferences _prefs;
    private MapView mapView;

    public MapFocusTextWidget() {
        super("", 2);

        mapView = MapView.getMapView();
        mapView.addOnMapMovedListener(this);
        _prefs = new UnitPreferences(mapView.getContext());
        _prefs.registerListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        if (key == null)
            return;

        if (UnitPreferences.COORD_FMT.equals(key)) {
            updateMapFocusText.exec();
        }

    }

    @Override
    public boolean setVisible(boolean visible) {
        if (!isVisible() && visible)
            updateMapFocusText.exec();
        return super.setVisible(visible);
    }

    @Override
    public void onMapMoved(AtakMapView view, boolean animate) {
        updateMapFocusText.exec();
    }

    @Override
    public void dispose() {
        _prefs.unregisterListener(this);
        mapView.removeOnMapMovedListener(this);
    }

    private final LimitingThread updateMapFocusText = new LimitingThread(
            "mapfocus-thread", new Runnable() {
                @Override
                public void run() {
                    if (!isVisible())
                        return;
                    GeoPointMetaData _point = mapView.getPointWithElevation();
                    // Location + altitude w/ source
                    String text = _prefs.formatPoint(_point, false) + " "
                            + _prefs.formatAltitude(_point);
                    setText(text);
                }
            });
}
