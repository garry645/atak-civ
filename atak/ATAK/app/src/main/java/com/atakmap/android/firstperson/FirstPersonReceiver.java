
package com.atakmap.android.firstperson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.CamLockerReceiver;
import com.atakmap.android.user.MapClickTool;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.elevation.ElevationManager;

/**
 * Broadcast receiver that can handle First Person events via intent.
 */
public class FirstPersonReceiver extends BroadcastReceiver {

    private final MapView _mapView;

    public static final String FIRSTPERSON = "com.atakmap.android.map.FIRSTPERSON";
    public static final String STREET_VIEW = "com.atakmap.android.map.FIRSTPERSON_STREET_VIEW";
    public static final String STREET_VIEW_MAP_CLICKED = "com.atakmap.android.map.STREET_VIEW_MAP_CLICKED";

    private static FirstPersonTool _firstPersonTool;

    FirstPersonReceiver(MapView mapView) {
        _mapView = mapView;
        _firstPersonTool = new FirstPersonTool(_mapView);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        // turn off cam lock while entering first person view
        final Intent stopCamLock = new Intent(CamLockerReceiver.UNLOCK_CAM);
        stopCamLock.putExtra("displayRelock", false);
        AtakBroadcast.getInstance().sendBroadcast(stopCamLock);

        final Bundle bundle = new Bundle();
        double fromAlt;
        double terrain;
        double mapItemHeight;

        switch (action) {
            case FIRSTPERSON:
                //Display FPV from current camera/eye location
                MapRenderer3 renderer = _mapView.getRenderer3();
                MapSceneModel scene = renderer.getMapSceneModel(false,
                        MapRenderer3.DisplayOrigin.UpperLeft);
                if (scene == null)
                    return;
                GeoPoint eye = scene.mapProjection
                        .inverse(scene.camera.location, null);
                if (eye == null)
                    return;

                fromAlt = eye.getAltitude();
                if (Double.isNaN(fromAlt))
                    fromAlt = 0d;
                terrain = ElevationManager.getElevation(eye, null);
                if (!Double.isNaN(terrain) && fromAlt < terrain)
                    fromAlt = terrain;
                if (fromAlt < 0d)
                    fromAlt = 0d;
                mapItemHeight = intent.getDoubleExtra("itemHeight",
                        Double.NaN);
                if (!Double.isNaN(mapItemHeight))
                    fromAlt += mapItemHeight;
                fromAlt += 2d;
                eye = new GeoPoint(eye.getLatitude(), eye.getLongitude(),
                        fromAlt);
                bundle.putString("fromPoint", eye.toStringRepresentation());
                // XXX - workaround for bounds check that `GeoPoint` should not be responsible for
                bundle.putDouble("fromAltitude", fromAlt);

                ToolManagerBroadcastReceiver.getInstance().startTool(
                        FirstPersonTool.TOOL_NAME, bundle);
                break;
            case STREET_VIEW:
                //select a point to snap to ground
                bundle.putString("prompt",
                        "Tap location for First Person View");
                bundle.putParcelable("callback",
                        new Intent(STREET_VIEW_MAP_CLICKED));
                ToolManagerBroadcastReceiver.getInstance().startTool(
                        MapClickTool.TOOL_NAME, bundle);
                break;
            case STREET_VIEW_MAP_CLICKED:
                //Display FPV from ground click location
                if (intent.hasExtra("point")) {
                    GeoPoint from = GeoPoint
                            .parseGeoPoint(intent.getStringExtra("point"));
                    if (from == null)
                        return;
                    fromAlt = from.getAltitude();
                    if (Double.isNaN(fromAlt))
                        fromAlt = 0d;
                    terrain = ElevationManager.getElevation(from, null);
                    if (!Double.isNaN(terrain) && fromAlt < terrain)
                        fromAlt = terrain;
                    if (fromAlt < 0d)
                        fromAlt = 0d;
                    mapItemHeight = intent.getDoubleExtra("itemHeight",
                            Double.NaN);
                    if (!Double.isNaN(mapItemHeight))
                        fromAlt += mapItemHeight;
                    fromAlt += 2d;
                    from = new GeoPoint(from.getLatitude(), from.getLongitude(),
                            fromAlt);
                    bundle.putString("fromPoint",
                            from.toStringRepresentation());
                    bundle.putString("itemUid",
                            intent.getStringExtra("itemUid"));

                    ToolManagerBroadcastReceiver.getInstance().startTool(
                            FirstPersonTool.TOOL_NAME, bundle);
                }
        }
    }
}
