
package com.atakmap.android.navigation.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.Overlay;
import com.atakmap.android.overlay.OverlayManager;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.widgets.CenterBeadWidget;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapFocusTextWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.MapWidget2;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.android.widgets.ScaleWidget;
import com.atakmap.android.widgets.TextWidget;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.opengl.GLRenderGlobals;

/**
 * Manages the scale bar and center designator widget
 */
public class NavWidgetsMapComponent extends AbstractMapComponent
        implements MapWidget2.OnWidgetSizeChangedListener,
        OverlayManager.OnServiceListener,
        Overlay.OnVisibleChangedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "NavWidgetsMapComponent";
    public static final String EYEALT_ENABLE_KEY = "map_eyealt_visible";

    private UnitPreferences _prefs;

    // Scale bar
    private LinearLayoutWidget beLayout;
    private ScaleWidget scale;

    // Center cross-hair
    private CenterBeadWidget cb;
    private MapFocusTextWidget cbText;
    private Overlay cbOverlay;

    private TextWidget eyeAltText;
    private Overlay eyeAltOverlay;

    private MapView mapView;

    private MarkerIconWidget dropCenterButton;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        mapView = view;
        _prefs = new UnitPreferences(view.getContext());

        RootLayoutWidget root = (RootLayoutWidget) view.getComponentExtra(
                "rootLayoutWidget");
        LinearLayoutWidget blLayout = root.getLayout(
                RootLayoutWidget.BOTTOM_LEFT);
        beLayout = root.getLayout(RootLayoutWidget.BOTTOM_EDGE);

        // Scale bar
        final MapTextFormat mt = MapView.getTextFormat(Typeface.DEFAULT_BOLD,
                -2);
        this.scale = new ScaleWidget(view, mt);
        this.scale.setName("Map Scale");
        this.scale.setPadding(8f, 4f, 8f, 4f);
        this.scale.setMargins(16f, 0f, 16f, 0f);

        boolean scale_vis = _prefs.get("map_scale_visible", true);
        boolean scale_rounding = _prefs.get("map_scale_rounding", false);
        this.scale.setVisible(scale_vis);
        this.scale.setRounding(scale_rounding);
        this.scale.setRangeUnits(_prefs.getRangeSystem());
        beLayout.addWidgetAt(0, this.scale);

        //Eye alt
        boolean eaVisible = _prefs.get(EYEALT_ENABLE_KEY, false);
        this.eyeAltText = new TextWidget("", 2);
        this.eyeAltText.setName("EyeAltTextWidget");
        this.eyeAltText.setMargins(16f, 16f, 0f, 16f);
        this.eyeAltText.setVisible(eaVisible);
        this.eyeAltText.setText("");
        blLayout.addWidgetAt(0, this.eyeAltText);

        // Center cross-hair
        boolean cbVisible = _prefs.get("map_center_designator", false);
        this.cb = new CenterBeadWidget();
        this.cb.setVisible(cbVisible);
        root.addWidget(this.cb);

        // Center coordinate text (corresponds with center cross-hair)
        this.cbText = new MapFocusTextWidget();
        this.cbText.setName("MapFocusTextWidget");
        this.cbText.setMargins(16f, 16f, 0f, 16f);
        this.cbText.setVisible(cbVisible);
        this.cbText.setText(" ");
        blLayout.addWidgetAt(0, this.cbText);

        Intent omIntent = new Intent("com.atakmap.android.overlay.SHARED");
        if (!OverlayManager.aquireService(context, omIntent, this)) {
            // try again but embed locally
            OverlayManager.aquireService(context, null, this);
        }

        dropCenterButton = new MarkerIconWidget();
        Icon.Builder builder = new Icon.Builder();
        builder.setAnchor(0, 0);
        builder.setColor(Icon.STATE_DEFAULT, Color.WHITE);
        builder.setSize(64, 64);
        builder.setImageUri(Icon.STATE_DEFAULT,
                "asset://icons/drop_crosshair.png");
        final Icon icon = builder.build();
        dropCenterButton.setIcon(icon);
        dropCenterButton.setVisible(false);
        root.addWidget(dropCenterButton);
        dropCenterButton.addOnClickListener(new MapWidget.OnClickListener() {
            @Override
            public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                Point focus = mapView.getMapController().getFocusPoint();
                float x = focus.x;
                float y = focus.y;
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(downTime,
                        eventTime,
                        MotionEvent.ACTION_DOWN, x, y, metaState);
                view.dispatchTouchEvent(motionEvent);

                motionEvent = MotionEvent.obtain(downTime + 100,
                        eventTime + 100,
                        MotionEvent.ACTION_UP, x, y, metaState);
                view.dispatchTouchEvent(motionEvent);
            }
        });

        mapView.getRenderer3().addOnCameraChangedListener(_cameraListener);

        beLayout.addOnWidgetSizeChangedListener(this);
        _prefs.registerListener(this);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        beLayout.removeOnWidgetSizeChangedListener(this);
        _prefs.unregisterListener(this);
        mapView.getRenderer3().removeOnCameraChangedListener(_cameraListener);
    }

    @Override
    public void onWidgetSizeChanged(MapWidget2 widget) {
        if (this.scale != null)
            this.scale.setMaxWidth(beLayout.getWidth());
    }

    @Override
    public void onOverlayManagerBind(OverlayManager manager) {
        cbOverlay = manager.registerOverlay("Center Designator");
        cbOverlay.setVisible(cb.isVisible());
        cbOverlay.setIconUri(ATAKUtilities.getResourceUri(
                R.drawable.ic_center_designator));
        cbOverlay.addOnVisibleChangedListener(this);

        eyeAltOverlay = manager.registerOverlay("Eye Altitude");
        setEyeAltOverlayViz();
        eyeAltOverlay.setIconUri("asset://icons/eyealtitude.png");
        eyeAltOverlay.addOnVisibleChangedListener(this);
    }

    @Override
    public void onOverlayManagerUnbind(OverlayManager manager) {
        if (cbOverlay != null) {
            cbOverlay.removeOnVisibleChangedListener(this);
            cbOverlay = null;
        }

        if (eyeAltOverlay != null) {
            eyeAltOverlay.removeOnVisibleChangedListener(this);
            eyeAltOverlay = null;
        }
    }

    @Override
    public void onOverlayVisibleChanged(Overlay overlay) {
        if (FileSystemUtils.isEquals(overlay.getOverlayId(),
                "Center Designator")) {
            final boolean current = _prefs.get("map_center_designator", false);
            if (current != overlay.getVisible()) {
                _prefs.set("map_center_designator", overlay.getVisible());
            }
        } else if (FileSystemUtils.isEquals(overlay.getOverlayId(),
                "Eye Altitude")) {
            final boolean current = _prefs.get(EYEALT_ENABLE_KEY, false);
            if (current != overlay.getVisible()) {
                _prefs.set(EYEALT_ENABLE_KEY, overlay.getVisible());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences prefs, final String key) {

        if (key == null)
            return;

        switch (key) {
            case "map_scale_visible":
                if (scale != null)
                    scale.setVisible(prefs.getBoolean(key, true));
                break;
            case "point_pick_center_designator":
                if (prefs.getBoolean("centerDesignatorDuringPointDrop",
                        false)) {
                    boolean ppcd_vis = prefs.getBoolean(key, false);
                    boolean mcd_vis = prefs.getBoolean("map_center_designator",
                            false);
                    if (cb != null) {
                        cb.setVisible(ppcd_vis || mcd_vis);
                        cbText.setVisible(ppcd_vis || mcd_vis);
                    }
                    dropCenterButton.setPoint((mapView.getWidth() / 2f)
                            - (GLRenderGlobals.getRelativeScaling() * 48
                                    / 2),
                            mapView.getHeight()
                                    - (mapView.isPortrait() ? 288 : 128));
                    dropCenterButton.setVisible(ppcd_vis);
                }
                break;

            case "map_center_designator":
                boolean visible = prefs.getBoolean(key, false);
                if (cb != null) {
                    cb.setVisible(visible);
                    cbText.setVisible(visible);
                }
                if (cbOverlay != null)
                    cbOverlay.setVisible(visible);
                break;
            case "map_scale_rounding":
                if (scale != null)
                    scale.setRounding(prefs.getBoolean(key, false));
                break;
            case "rab_rng_units_pref":
                if (scale != null)
                    scale.setRangeUnits(_prefs.getRangeSystem());
                break;
            case EYEALT_ENABLE_KEY:
            case "alt_display_agl":
                updateEyeAltButton();
                setEyeAltOverlayViz();
                break;
            case "fpvStreetViewUid":
            case "fpvStreetViewCallsign":
                updateEyeAltButton();
                break;
        }
    }

    private final MapRenderer2.OnCameraChangedListener2 _cameraListener = new MapRenderer2.OnCameraChangedListener2() {
        @Override
        public void onCameraChangeRequested(MapRenderer2 renderer) {
            //Log.d(TAG, "onCameraChangeRequested");
            updateEyeAltButton(renderer);
        }

        @Override
        public void onCameraChanged(MapRenderer2 renderer) {
            //no-op
            //Log.d(TAG, "onCameraChanged");
        }
    };

    private void setEyeAltOverlayViz() {
        setEyeAltOverlayViz(_prefs.get(EYEALT_ENABLE_KEY, false));
    }

    private void setEyeAltOverlayViz(boolean b) {
        if (eyeAltOverlay != null)
            eyeAltOverlay.setVisible(b);
    }

    private void updateEyeAltButton() {
        updateEyeAltButton(mapView.getRenderer3());
    }

    private void updateEyeAltButton(MapRenderer2 renderer) {
        String text = null;
        boolean eyeVisible = _prefs.get(EYEALT_ENABLE_KEY, false);
        if (eyeVisible) {
            text = getEyeAltText(renderer);
        }

        if (FileSystemUtils.isEmpty(text)) {
            eyeVisible = false;
            eyeAltText.setText("");
        } else {
            eyeAltText.setText(text);
        }

        eyeAltText.setVisible(eyeVisible);
    }

    private String getEyeAltText(MapRenderer2 renderer) {
        if (renderer == null) {
            Log.d(TAG, "getEyeAltText fetching renderer");
            renderer = mapView.getRenderer3();
        }

        MapSceneModel scene = renderer.getMapSceneModel(false,
                MapRenderer3.DisplayOrigin.UpperLeft);
        if (scene == null)
            return "--";
        GeoPoint eye = scene.mapProjection.inverse(scene.camera.location, null);
        if (eye == null)
            return "--";

        double alt = eye.getAltitude();
        if (Double.isNaN(alt) || Double.isInfinite(alt))
            return "--";

        String altString = "Eye Alt: "
                + new UnitPreferences(mapView.getContext()).formatAltitude(eye);

        String fpvStreetViewCallsign = _prefs.get("fpvStreetViewCallsign",
                null);
        if (!FileSystemUtils.isEmpty(fpvStreetViewCallsign)) {
            altString += " (" + fpvStreetViewCallsign + ")";
        }

        //Log.d(TAG, altString);
        return altString;
    }
}
