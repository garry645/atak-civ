
package com.atakmap.android.offscreenindicators;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.SystemClock;

import com.atakmap.android.icons.Icon2525cIconAdapter;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.opengl.GLRenderGlobals;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.IMapRendererEnums;

public class OffscreenIndicatorController extends AbstractLayer implements
        MapEventDispatcher.MapEventDispatchListener,
        MapItem.OnZOrderChangedListener,
        MapRenderer3.OnControlsChangedListener {

    public static final String TAG = "OffscreenIndicatorController";

    private static final float HALO_BITMAP_SIZE = 24
            * GLRenderGlobals.getRelativeScaling();
    public static final float HALO_BORDER_SIZE = 48
            * GLRenderGlobals.getRelativeScaling();

    private final static int COLOR_FRIENDLY = Color.argb(255, 128, 224, 255);
    private final static int COLOR_HOSTILE = Color.argb(255, 255, 128, 128);
    private final static int COLOR_NEUTRAL = Color.argb(255, 170, 255, 170);
    private final static int COLOR_UNKNOWN = Color.argb(255, 225, 255, 128);
    private final static int COLOR_WAYPOINT = Color.argb(255, 241, 128, 33);
    private final static int DEFAULT_COLOR = Color.argb(255, 0, 0, 0);

    private final Set<Marker> markers = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    private MapView _mapView;
    private AtakPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private final Set<OnOffscreenIndicatorsEnabledListener> enabledListeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    private final Set<OnOffscreenIndicatorsThresholdListener> thresholdListeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    private final Set<OnItemsChangedListener> itemsChangedListeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    private boolean enabled = true;
    private final AtomicBoolean zDirty = new AtomicBoolean(false);

    /** display threshold, in meters */
    private double threshold = 0d;
    private double timeout = 0d;

    private HitTestControl hitTestControl = null;

    public OffscreenIndicatorController(MapView mapView, Context context) {
        super("Offscreen Indicators");

        prefs = AtakPreferences.getInstance(context);
        // do nothing
        // do nothing
        SharedPreferences.OnSharedPreferenceChangeListener changeListener = listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                if (key == null)
                    return;

                switch (key) {
                    case "toggle_offscreen_indicators":
                        enabled = prefs.get(key, true);
                        dispatchOnOffscreenIndicatorsEnabled();
                        break;
                    case "offscreen_indicator_dist_threshold":
                        try {
                            threshold = Double
                                    .parseDouble(prefs.get(key, "10"))
                                    * 1000;

                            dispatchOnOffscreenIndicatorsThresholdChanged();
                        } catch (NumberFormatException e) {
                            // do nothing
                        }

                        break;
                    case "offscreen_indicator_timeout_threshold":
                        try {
                            timeout = Double
                                    .parseDouble(prefs.get(key, "60"))
                                    * 1000;

                            dispatchOnOffscreenIndicatorsThresholdChanged();
                        } catch (NumberFormatException e) {
                            // do nothing
                        }
                        break;
                }

            }

        };
        prefs.registerListener(changeListener);

        enabled = prefs.get("toggle_offscreen_indicators", true);
        try {
            threshold = Double.parseDouble(
                    prefs.get("offscreen_indicator_dist_threshold", "10"))
                    * 1000;
        } catch (NumberFormatException e) {
            // TODO
        }
        try {
            timeout = Double.parseDouble(
                    prefs.get("offscreen_indicator_timeout_threshold", "60"))
                    * 1000;
        } catch (NumberFormatException e) {
            // TODO
        }

        _mapView = mapView;

        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_CLICK, this);
        _mapView.getRenderer3().addOnControlsChangedListener(this);

        this.zDirty.set(true);
    }

    public void addOnOffscreenIndicatorsEnabledListener(
            OnOffscreenIndicatorsEnabledListener l) {
        this.enabledListeners.add(l);
    }

    public void removeOnOffscreenIndicatorsEnabledListener(
            OnOffscreenIndicatorsEnabledListener l) {
        this.enabledListeners.remove(l);
    }

    private void dispatchOnOffscreenIndicatorsEnabled() {
        for (OnOffscreenIndicatorsEnabledListener l : this.enabledListeners)
            l.onOffscreenIndicatorsEnabled(this);
    }

    public void addOnOffscreenIndicatorsThresholdListener(
            OnOffscreenIndicatorsThresholdListener l) {
        this.thresholdListeners.add(l);
    }

    public void removeOnOffscreenIndicatorsThresholdListener(
            OnOffscreenIndicatorsThresholdListener l) {
        this.thresholdListeners.remove(l);
    }

    private void dispatchOnOffscreenIndicatorsThresholdChanged() {
        for (OnOffscreenIndicatorsThresholdListener l : this.thresholdListeners)
            l.onOffscreenIndicatorsThresholdChanged(this);
    }

    public void addOnItemsChangedListener(
            OnItemsChangedListener l) {
        this.itemsChangedListeners.add(l);
    }

    public void removeOnItemsChangedListener(
            OnItemsChangedListener l) {
        this.itemsChangedListeners.remove(l);
    }

    private void dispatchOnItemsChanged() {
        for (OnItemsChangedListener l : this.itemsChangedListeners)
            l.onItemsChanged(this);
    }

    public void dispose() {
        _mapView.getRenderer3().removeOnControlsChangedListener(this);
        _mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.MAP_CLICK, this);
        prefs.unregisterListener(listener);
        listener = null;
        prefs = null;
        this.markers.clear();
        _mapView = null;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        //this.dispatchOnOffscreenIndicatorsEnabled();
        prefs.set("toggle_offscreen_indicators", enabled);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void addMarker(Marker marker) {
        if (!this.markers.add(marker))
            return;
        marker.addOnZOrderChangedListener(this);
    }

    public void getMarkers(Set<Marker> markers) {
        markers.addAll(this.markers);
    }

    /**
     * Returns the threshold for indicators, in meters. Items that are further
     * than the returned distance from the center of the view will not have an
     * indicator displayed.
     * 
     * @return  The threshold for indicators in meters.
     */
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * Returns the timeout used before removing an current offscreen indicator
     * @return the timeout in milliseconds
     */
    public double getTimeout() {
        return this.timeout;
    }

    public void removeMarker(Marker marker) {
        if (!this.markers.remove(marker))
            return;
        marker.removeOnZOrderChangedListener(this);
        this.dispatchOnItemsChanged();
    }

    public float getHaloIconSize() {
        return HALO_BITMAP_SIZE;
    }

    public float getHaloBorderSize() {
        return HALO_BORDER_SIZE;
    }

    public int getArcColor(Marker marker) {
        int color = 0;
        if (marker.hasMetaValue("team")) {

            color = Icon2525cIconAdapter
                    .teamToColor(marker.getMetaString("team", "white"));

        } else if (marker.hasMetaValue("offscreen_arccolor")) {
            color = marker.getMetaInteger("offscreen_arccolor", DEFAULT_COLOR);
        } else if (marker.hasMetaValue("type")) {
            String type = marker.getType();
            if (type.startsWith("a-f")) {
                color = COLOR_FRIENDLY;
            } else if (type.startsWith("a-h")) {
                color = COLOR_HOSTILE;
            } else if (type.startsWith("a-n")) {
                color = COLOR_NEUTRAL;
            } else if (type.startsWith("a-u")) {
                color = COLOR_UNKNOWN;
            } else if (type.equals("b-m-p-w")) { // waypoint marker
                color = COLOR_WAYPOINT;
            }
        }
        return color;
    }

    /**************************************************************************/
    // Map Event Dispatch Listener

    @Override
    public void onMapEvent(MapEvent event) {

        if (!enabled)
            return;

        // XXX - this is technically a misuse of thread-safety requirements of the controls API, but
        // given that we know the implementation detail we will opt to not _invoke-and-wait_
        final HitTestControl ctrl = this.hitTestControl;
        if (ctrl == null)
            return;
        HitTestQueryParameters params = new HitTestQueryParameters(
                _mapView.getGLSurface(),
                event.getPointF().x, event.getPointF().y,
                1f,
                IMapRendererEnums.DisplayOrigin.UpperLeft);
        LinkedList<HitTestResult> hits = new LinkedList<>();
        ctrl.hitTest(_mapView.getRenderer3(), params, hits);
        if (hits.isEmpty())
            return;
        if (!(hits.getFirst().subject instanceof MapItem))
            return;
        final MapItem candidate = (MapItem) hits.getFirst().subject;
        if (candidate != null) {
            long interest = candidate.getMetaLong("offscreen_interest", -1);
            double intTime = timeout + interest;
            long clock = SystemClock.elapsedRealtime();
            boolean timedout = (timeout > 0d && intTime < clock);

            String uid = candidate.getUID();
            if (uid != null) {
                Intent focusIntent = new Intent();
                focusIntent.setAction("com.atakmap.android.maps.FOCUS");
                focusIntent.putExtra("uid", uid);
                AtakBroadcast.getInstance().sendBroadcast(focusIntent);

                Intent menuIntent = new Intent();
                menuIntent.setAction("com.atakmap.android.maps.SHOW_MENU");
                menuIntent.putExtra("uid", uid);
                AtakBroadcast.getInstance().sendBroadcast(menuIntent);

                Intent detailsIntent = new Intent();
                detailsIntent
                        .setAction("com.atakmap.android.maps.SHOW_DETAILS");
                detailsIntent.putExtra("uid", uid);
                AtakBroadcast.getInstance().sendBroadcast(detailsIntent);
            }
        }
    }

    /** @deprecated dead code; to be removed without replacement */
    @Deprecated
    @DeprecatedApi(since = "4.10", forRemoval = true, removeAt = "4.13")
    public static boolean isHitTestable(MapItem m,
            android.graphics.PointF point, float screenWidth,
            float screenHeight, GeoPointMetaData focusGeo, double threshold) {
        if (m.hasMetaValue("disable_offscreen_indicator")
                && m.getMetaBoolean("disable_offscreen_indicator", false)) {
            return false;
        }
        double distance = MapItem.computeDistance(m, focusGeo.get());
        if (Double.isNaN(distance) || distance > threshold)
            return false;

        PointF screen = point;
        if (screen.x >= 0 && screen.x < screenWidth
                && screen.y >= 0 && screen.y < screenHeight)
            return false;

        // if the touch occurred on the region inside of where the offscreen
        // indicator halos are rendered, ignore
        final float borderSize = (HALO_BORDER_SIZE + HALO_BITMAP_SIZE);
        if ((point.x > borderSize
                && point.x < (screenWidth - borderSize))
                &&
                (point.y > borderSize &&
                        point.y < (screenHeight - borderSize))) {

            return false;
        }

        return true;
    }

    /**************************************************************************/
    // MapRenderer3 OnControlsChangedListener

    @Override
    public void onControlRegistered(Layer2 layer, MapControl ctrl) {
        if (layer == this && ctrl instanceof HitTestControl)
            this.hitTestControl = (HitTestControl) ctrl;
    }

    @Override
    public void onControlUnregistered(Layer2 layer, MapControl ctrl) {
        if (layer == this && ctrl == this.hitTestControl)
            this.hitTestControl = null;
    }

    /**************************************************************************/
    // MapItem OnZOrderChangedListener

    @Override
    public void onZOrderChanged(MapItem item) {
        this.zDirty.set(true);
    }

    /**************************************************************************/

    public interface OnOffscreenIndicatorsEnabledListener {
        void onOffscreenIndicatorsEnabled(
                OffscreenIndicatorController controller);
    }

    public interface OnOffscreenIndicatorsThresholdListener {
        void onOffscreenIndicatorsThresholdChanged(
                OffscreenIndicatorController controller);
    }

    public interface OnItemsChangedListener {
        void onItemsChanged(OffscreenIndicatorController controller);
    }
}
