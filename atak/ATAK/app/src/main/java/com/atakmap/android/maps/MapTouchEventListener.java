
package com.atakmap.android.maps;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;

import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.navigation.views.NavView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.user.SelectPointButtonTool;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.CameraController;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapRenderer3;
import com.atakmap.math.PointD;

import java.util.UUID;

/**
 * Default map event listener
 */
class MapTouchEventListener implements
        MapEventDispatcher.MapEventDispatchListener {

    private static final String TAG = "MapTouchEventListener";

    private final MapView _mapView;
    private final AtakPreferences _prefs;

    private boolean atakDoubleTapToZoom;
    private boolean atakTapToggleActionBar;
    private boolean atakLongPressDropAPoint;
    private boolean atakLongPressRedX;

    MapTouchEventListener(MapView mapView) {
        _mapView = mapView;
        _prefs = AtakPreferences.getInstance(mapView
                .getContext());
        _prefs.registerListener(_prefListener);
        setPrefs();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener _prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {

            if (key == null)
                return;

            if (key.equals("atakDoubleTapToZoom") ||
                    key.equals("atakLongPressMap")) {
                setPrefs();
            }
        }
    };

    private void setPrefs() {
        atakDoubleTapToZoom = _prefs.get("atakDoubleTapToZoom", false);
        final String state = _prefs.get("atakLongPressMap", "actionbar");
        atakTapToggleActionBar = state.equals("actionbar");
        atakLongPressDropAPoint = state.equals("dropicon");
        atakLongPressRedX = state.equals("redx");
    }

    @Override
    public void onMapEvent(MapEvent event) {
        if (event == null || FileSystemUtils.isEmpty(event.getType()))
            return;

        if (MapEvent.MAP_SCALE.equals(event.getType())) {
            _mapView.getMapTouchController().onScaleEvent(event);
        } else if (MapEvent.MAP_SCROLL.equals(event.getType())) {
            PointF p = event.getPointF();
            boolean poleSmoothScroll = false; // default to legacy
            do {
                Bundle panExtras = event.getExtras();
                if (panExtras == null)
                    break;
                poleSmoothScroll = panExtras.getBoolean("poleSmoothScroll",
                        poleSmoothScroll);
                boolean doPanTo = panExtras.getBoolean("cameraPanTo", false);
                if (!doPanTo)
                    break;
                final float x = panExtras.getFloat("x", Float.NaN);
                final float y = panExtras.getFloat("y", Float.NaN);
                if (Float.isNaN(x) || Float.isNaN(y))
                    break;
                final GeoPoint originalFocus = panExtras
                        .getParcelable("originalFocus");
                final MapRenderer3 glglobe = _mapView.getRenderer3();

                // don't attempt pan-to when off world
                final MapRenderer2.InverseResult result = glglobe.inverse(
                        new PointD(x, y, 0d),
                        GeoPoint.createMutable(),
                        MapRenderer2.InverseMode.RayCast,
                        MapRenderer2.HINT_RAYCAST_IGNORE_SURFACE_MESH
                                | MapRenderer2.HINT_RAYCAST_IGNORE_TERRAIN_MESH,
                        MapRenderer2.DisplayOrigin.UpperLeft);

                if (result == MapRenderer2.InverseResult.None)
                    break;

                CameraController.Interactive.panTo(glglobe, originalFocus, x, y,
                        MapRenderer3.CameraCollision.AdjustFocus,
                        poleSmoothScroll, false);
                return;
            } while (false);
            CameraController.Interactive.panBy(_mapView.getRenderer3(), p.x,
                    p.y, MapRenderer3.CameraCollision.AdjustFocus,
                    poleSmoothScroll, false);
        } else if (MapEvent.MAP_LONG_PRESS.equals(event.getType())) {
            if (atakTapToggleActionBar) {
                NavView.getInstance().toggleButtons();
                if (MetricsApi.shouldRecordMetric()) {
                    Bundle b = new Bundle();
                    b.putString(MetricsUtils.FIELD_STATE,
                            NavView.getInstance().buttonsVisible() ? "showing"
                                    : "hidden");
                    b.putString(MetricsUtils.FIELD_ELEMENT_NAME, "ActionBar");
                    MetricsUtils.record(MetricsUtils.CATEGORY_MAPWIDGET,
                            MetricsUtils.EVENT_WIDGET_STATE,
                            "MapTouchEventListener", b);
                }

            } else if (atakLongPressDropAPoint)
                dropHostile(event);
            else if (atakLongPressRedX)
                dropRedX(event);
        } else if (MapEvent.MAP_DOUBLE_TAP.equals(event.getType())) {
            if (atakDoubleTapToZoom) {
                PointF p = event.getPointF();
                GeoPoint focus = null;
                Bundle panExtras = event.getExtras();
                if (panExtras != null)
                    focus = panExtras.getParcelable("originalFocus");
                if (focus == null) {
                    focus = GeoPoint.createMutable();
                    _mapView.getRenderer3().inverse(new PointD(p.x, p.y), focus,
                            MapRenderer2.InverseMode.RayCast,
                            MapRenderer2.HINT_RAYCAST_IGNORE_SURFACE_MESH
                                    | MapRenderer2.HINT_RAYCAST_IGNORE_TERRAIN_MESH,
                            MapRenderer2.DisplayOrigin.UpperLeft);
                }
                // XXX - don't allow further zoom in once collision occurs
                final MapRenderer3.CameraCollision collide = MapRenderer3.CameraCollision.Abort;
                if (focus != null && focus.isValid())
                    CameraController.Interactive.zoomBy(_mapView.getRenderer3(),
                            2d, focus, p.x, p.y, collide, false);
                else
                    CameraController.Interactive.zoomBy(_mapView.getRenderer3(),
                            2d, collide, false);
            }
        }
    }

    private void dropHostile(final MapEvent event) {
        PointF p = event.getPointF();

        GeoPointMetaData gp = _mapView.inverseWithElevation(p.x, p.y);
        if (gp == null || !gp.get().isValid()) {
            Log.w(TAG, "Cannot Create Point with no map coordinates");
            return;
        }

        final String uid = UUID.randomUUID().toString();
        final String type = _prefs.get("lastCoTTypeSet", "a-u-G");

        final boolean showNineLine = _prefs.get("autostart_nineline",
                false);

        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(gp)
                .setUid(uid)
                .setType(type)
                .showCotDetails(false)
                .setShowNineLine(showNineLine && type.startsWith("a-h"));
        if (_prefs.contains("lastIconsetPath"))
            mc = mc.setIconPath(_prefs.get("lastIconsetPath", ""));
        Marker placedPoint = mc.placePoint();

        if (placedPoint != null)
            Log.d(TAG, "Dropping new point " + uid + ", at " + gp);
        else
            Log.d(TAG, "Could not place the point");
    }

    private void dropRedX(MapEvent event) {
        PointF p = event.getPointF();
        GeoPointMetaData point = _mapView.inverseWithElevation(p.x, p.y);

        Bundle b = new Bundle();
        b.putParcelable("point", point.get());
        b.putBoolean("skipDetailClosure", true);
        // move the redx instead of dropping a point.
        ToolManagerBroadcastReceiver.getInstance().startTool(
                "com.atakmap.android.user.SELECTPOINTBUTTONTOOL", b);
        Tool t = ToolManagerBroadcastReceiver.getInstance()
                .getActiveTool();

        Marker marker = null;
        if (t instanceof SelectPointButtonTool) {
            marker = ((SelectPointButtonTool) t).getMarker();
        }
        if (marker != null)
            Log.d(TAG, "placing the redx at: " + point);
        else
            Log.d(TAG, "could not place the redx");
    }

}
