package com.atakmap.android.layers.overlay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.items.AbstractChildlessListItem;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.map.MapRenderer3;

public final class NadirElevationToggleListItem extends AbstractChildlessListItem
        implements SharedPreferences.OnSharedPreferenceChangeListener, Visibility {

    private final static String PREFERENCE_KEY = "elevationRenderOnNadir";


    private final Context _context;
    private final MapRenderer3 _renderer;
    private final AtakPreferences _prefs;
    private boolean renderElevationAtNadir;

    public NadirElevationToggleListItem(MapView mapView) {
        _context = mapView.getContext();
        _renderer = mapView.getRenderer3();

        _prefs = AtakPreferences.getInstance(mapView.getContext());
        _prefs.registerListener(this);
    }


    @Override
    public String getTitle() {
        return "Render Elevation when Nadir";
    }

    @Override
    public String getUID() {
        return "elevationRenderOnNadir";
    }

    @Override
    public String getDescription() {
        return "Render the elevation when looking straight down (NADIR)";
    }

    @Override
    public String getIconUri() {
        return "gone";
    }

    @Override
    public Object getUserObject() {
        return this;
    }

    @Override
    public boolean setVisible(boolean visible) {
        boolean renderElevationAtNadir = _prefs.get(PREFERENCE_KEY, true);

        if (visible != renderElevationAtNadir) {
            _prefs.set(PREFERENCE_KEY, visible);
            return true;
        }
        return false;
    }

    @Override
    public boolean isVisible() {
        return _prefs.get(PREFERENCE_KEY, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (key == null)
            return;

        if (key.equals(PREFERENCE_KEY)) {
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    HierarchyListReceiver.REFRESH_HIERARCHY));
        }
    }


}
