
package com.atakmap.android.elev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.elev.graphics.GLViewShed2;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapOverlayManager;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.MapOverlayRenderer;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPoint.AltitudeReference;
import com.atakmap.map.elevation.ElevationData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ViewShedReceiver extends BroadcastReceiver {

    private final static String TAG = "ViewShedReceiver";

    /**
     * Action to show the viewshed overlay. The {@link Intent} should include
     * one of the extras:
     * 
     * <UL>
     *   <LI>{@link #EXTRA_ELEV_THRESHOLD}</LI>
     *   <LI>{@link #EXTRA_ELEV_POINT}</LI>
     * </UL>
     * 
     * In the event that more than one is included, the threshold elevation
     * argument will be selected using the above order.
     * 
     * <P>This action may be posted after the viewshed is made visible to update
     * the viewshed elevation threshold value.
     */
    public final static String ACTION_SHOW_VIEWSHED = "com.atakmap.android.elev.ViewShedReceiver.SHOW_VIEWSHED";

    public final static String ACTION_SHOW_VIEWSHED_LINE = "com.atakmap.android.elev.ViewShedReceiver.SHOW_VIEWSHED_LINE";

    /**
     * Action to dismiss the viewshed overlay.
     */
    public final static String ACTION_DISMISS_VIEWSHED = "com.atakmap.android.elev.ViewShedReceiver.DISMISS_VIEWSHED";
    public final static String ACTION_DISMISS_VIEWSHED_LINE = "com.atakmap.android.elev.ViewShedReceiver.DISMISS_VIEWSHED_LINE";

    /**
     * Action to update the viewshed intensity.
     */
    public final static String UPDATE_VIEWSHED_INTENSITY = "com.atakmap.android.elev.ViewShedReceiver.UPDATE_VIEWSHED_INTENSITY";

    /**
     * The point of interest for viewshed analysis. The associated value should
     * be an {@link GeoPoint} instance. If the associated point does not have a
     * valid altitude, the elevation model will be queried.
     */
    public final static String EXTRA_ELEV_POINT = "point";
    /**
     * The point of interest for viewshed analysis. The associated value should
     * be an {@link GeoPoint} instance. If the associated point does not have a
     * valid altitude, the elevation model will be queried.
     */
    public final static String EXTRA_ELEV_POINT2 = "point2";

    /**
     * The radius in meters for how far around the center point the viewshed
     * should be calculated.
     */
    public final static String EXTRA_ELEV_RADIUS = "radius";
    public final static String EXTRA_RESOLUTION = "resolution";
    public final static String EXTRA_LINE_SAMPLES = "samples";
    public final static String EXTRA_ELEVATION_MODEL = "elevationmodel";

    /**
     * The opacity value for the viewshed overlay
     */
    public final static String EXTRA_ELEV_OPACITY_SEEN = "opacity_seen";
    public final static String EXTRA_ELEV_OPACITY_UNSEEN = "opacity_unseen";

    public final static String EXTRA_ELEV_OPACITY = "opacity";

    public final static String EXTRA_ELEV_CIRCLE = "circle";

    /**
     * The UID of the {@link MapItem} of interest for viewshed analysis. The
     * associated value should be an instance of {@link String} that is the
     * {@link MapItem} UID. If the {@link GeoPoint} associated with the
     * {@link MapItem} does not have a valid altitude, the elevation
     * model will be queried at the point.
     */
    public final static String VIEWSHED_UID = "uid";
    public final static String VIEWSHED_UID2 = "uid2";

    public final static String SHOWING_LINE = "showingLine";

    /**
     * The elevation threshold value for the viewshed, in meters MSL. A value of
     * {@link Double#NaN} indicates an invalid value.
     */
    public final static String EXTRA_ELEV_THRESHOLD = "threshold";

    /** viewshed related preferences */
    public static final String VIEWSHED_PREFERENCE_COLOR_INTENSITY_KEY = "viewshed_prefs_color_intensity";
    public static final String VIEWSHED_PREFERENCE_COLOR_INTENSITY_SEEN_KEY = "viewshed_prefs_color_seen_intensity";
    public static final String VIEWSHED_PREFERENCE_COLOR_INTENSITY_UNSEEN_KEY = "viewshed_prefs_color_unseen_intensity";
    public static final String VIEWSHED_PREFERENCE_CIRCULAR_VIEWSHED = "viewshed_prefs_circular_viewshed";
    public static final String VIEWSHED_PREFERENCE_RADIUS_KEY = "viewshed_prefs_radius";

    public static final String VIEWSHED_PREFERENCE_HEIGHT_ABOVE_KEY = "viewshed_prefs_height_above_meters";

    public static final String VIEWSHED_LINE_UID_SEPERATOR = "*";

    public static class VsdLayer {
        private final MapView mapview = MapView.getMapView();
        private ViewShedLayer2 viewShed;
        private MapOverlayRenderer viewShedRenderer;
        private boolean visible = false;

        public VsdLayer() {
            this.viewShed = new ViewShedLayer2();
            this.viewShedRenderer = new MapOverlayRenderer(
                    MapView.RenderStack.MAP_SURFACE_OVERLAYS,
                    new GLViewShed2(mapview.getGLSurface()
                            .getGLMapView(),
                            this.viewShed));
        }

        public ViewShedLayer2 getViewshed() {
            return viewShed;
        }

        public boolean getVisible() {
            return visible;
        }

        public void install() {
            MapOverlayManager.installOverlayRenderer(mapview,
                    this.viewShedRenderer);
            this.visible = true;
        }

        public void uninstall() {
            MapOverlayManager.uninstallOverlayRenderer(mapview,
                    this.viewShedRenderer);
            this.visible = false;
            this.viewShed = null;
            this.viewShedRenderer = null;
        }
    }

    private static ViewShedReceiver instance;
    private static MapView mapView;
    private static final HashMap<String, ArrayList<VsdLayer>> singleVsdLayerMap = new HashMap<>();
    private static final HashMap<String, ArrayList<VsdLayer>> vsdLineLayerMap = new HashMap<>();

    public static synchronized ViewShedReceiver getInstance() {
        if (instance == null)
            instance = new ViewShedReceiver(MapView.getMapView());
        return instance;
    }

    private ViewShedReceiver(MapView mv) {
        mapView = mv;
    }

    public static HashMap<String, ArrayList<VsdLayer>> getSingleVsdLayerMap() {
        return singleVsdLayerMap;
    }

    public static HashMap<String, ArrayList<VsdLayer>> getVsdLineLayerMap() {
        return vsdLineLayerMap;
    }

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action == null)
            return;

        final String uid = intent.getStringExtra(VIEWSHED_UID);

        if (FileSystemUtils.isEmpty(uid))
            return;

        switch (action) {
            case ACTION_SHOW_VIEWSHED: {
                //allow for multiple here
                if (!singleVsdLayerMap.containsKey(uid)) {
                    ArrayList<VsdLayer> layerList = new ArrayList<>();
                    layerList.add(new VsdLayer());
                    singleVsdLayerMap.put(uid, layerList);
                }

                List<VsdLayer> vsdlist = singleVsdLayerMap.get(uid);
                if (FileSystemUtils.isEmpty(vsdlist))
                    return;

                VsdLayer layer = vsdlist.get(0);

                GeoPoint gp = intent.getParcelableExtra(
                        EXTRA_ELEV_POINT);
                if (gp != null) {
                    GeoPoint poi = layer.getViewshed().getPointOfInterest();
                    if (poi == null)
                        layer.getViewshed().setPointOfInterest(gp);
                    else if (poi.distanceTo(gp) > 1 ||
                            (int) EGM96.getMSL(poi) != (int) EGM96.getMSL(gp))
                        layer.getViewshed().setPointOfInterest(gp);
                }
                if (intent.hasExtra(EXTRA_ELEV_RADIUS)) {
                    if (layer.getViewshed().getRadius() != intent
                            .getDoubleExtra(
                                    EXTRA_ELEV_RADIUS, 1000))
                        layer.getViewshed().setRadius(intent.getDoubleExtra(
                                EXTRA_ELEV_RADIUS, 1000));
                }
                if (intent.hasExtra(EXTRA_RESOLUTION)) {
                    layer.getViewshed().setResolution(intent.getIntExtra(
                            EXTRA_RESOLUTION, 201));
                }
                if (intent.hasExtra(EXTRA_ELEVATION_MODEL)) {
                    layer.getViewshed().setElevationModel(intent.getIntExtra(
                            EXTRA_ELEVATION_MODEL,
                            ElevationData.MODEL_TERRAIN));
                }
                setOpacity(intent, layer.getViewshed());

                if (intent.hasExtra(EXTRA_ELEV_CIRCLE)) {
                    if (layer.getViewshed().getCircle() != intent
                            .getBooleanExtra(
                                    EXTRA_ELEV_CIRCLE, false))
                        layer.getViewshed().setCircle(intent.getBooleanExtra(
                                EXTRA_ELEV_CIRCLE, false));
                } else if (AtakPreferences.getInstance(mapView
                        .getContext()).get(
                                ViewShedReceiver.VIEWSHED_PREFERENCE_CIRCULAR_VIEWSHED,
                                false)) {
                    layer.getViewshed().setCircle(true);
                }

                layer.getViewshed().setCircle(true); // force a circle

                if (!layer.getVisible()) {
                    layer.install();
                }
                break;
            }
            case ACTION_SHOW_VIEWSHED_LINE:
                if (intent.hasExtra(EXTRA_ELEV_POINT)
                        && intent.hasExtra(EXTRA_ELEV_POINT2) &&
                        intent.hasExtra(VIEWSHED_UID2)) {
                    String uidDest = intent.getStringExtra(VIEWSHED_UID2);

                    if (vsdLineLayerMap.containsKey(uid
                            + VIEWSHED_LINE_UID_SEPERATOR + uidDest)) {
                        ArrayList<VsdLayer> layerList = vsdLineLayerMap.get(uid
                                + VIEWSHED_LINE_UID_SEPERATOR + uidDest);
                        if (layerList != null && !layerList.isEmpty()) {
                            for (VsdLayer layer : layerList) {
                                if (layer != null) {
                                    layer.uninstall();
                                }
                            }
                        }
                        vsdLineLayerMap.remove(uid + VIEWSHED_LINE_UID_SEPERATOR
                                + uidDest);
                    }
                    GeoPoint gp1 = intent
                            .getParcelableExtra(EXTRA_ELEV_POINT);
                    GeoPoint gp2 = intent
                            .getParcelableExtra(EXTRA_ELEV_POINT2);
                    int samples = intent.getIntExtra(EXTRA_LINE_SAMPLES, 4);
                    int opacitySeen = 50;
                    int opacityUnseen = 50;
                    if (intent.hasExtra(EXTRA_ELEV_OPACITY_SEEN))
                        opacitySeen = intent
                                .getIntExtra(EXTRA_ELEV_OPACITY_SEEN, 50);

                    if (intent.hasExtra(EXTRA_ELEV_OPACITY_UNSEEN))
                        opacityUnseen = intent
                                .getIntExtra(EXTRA_ELEV_OPACITY_UNSEEN, 50);

                    if (intent.hasExtra(EXTRA_ELEV_OPACITY)) {
                        opacityUnseen = intent.getIntExtra(EXTRA_ELEV_OPACITY,
                                50);
                        opacitySeen = intent.getIntExtra(EXTRA_ELEV_OPACITY,
                                50);
                    }

                    if (gp1 == null || gp2 == null)
                        return;

                    double distance = gp1.distanceTo(gp2);
                    double bearing = gp1.bearingTo(gp2);
                    double altChange = gp2.getAltitude()
                            - gp1.getAltitude();
                    double startAltVal = gp1.getAltitude();
                    double radius = 1000;
                    if (intent.hasExtra(EXTRA_ELEV_RADIUS)) {
                        radius = intent.getDoubleExtra(EXTRA_ELEV_RADIUS, 1000);
                    }
                    showViewshed(uid + VIEWSHED_LINE_UID_SEPERATOR + uidDest,
                            gp1,
                            radius, opacitySeen, opacityUnseen);
                    for (int i = 0; i < samples - 1; i++) {
                        GeoPoint gp = GeoCalculations.pointAtDistance(gp1,
                                bearing, (distance / (samples - 1) * (i + 1)));
                        double altval = startAltVal
                                + ((altChange / (samples - 1)) * (i + 1));
                        gp = new GeoPoint(gp.getLatitude(), gp.getLongitude(),
                                altval, AltitudeReference.AGL,
                                GeoPoint.UNKNOWN, GeoPoint.UNKNOWN);
                        showViewshed(
                                uid + VIEWSHED_LINE_UID_SEPERATOR + uidDest,
                                gp, radius,
                                opacitySeen, opacityUnseen);
                    }

                }
                break;
            case UPDATE_VIEWSHED_INTENSITY:
                if (!intent.hasExtra(EXTRA_ELEV_OPACITY_SEEN) &&
                        !intent.hasExtra(EXTRA_ELEV_OPACITY) &&
                        !intent.hasExtra(EXTRA_ELEV_OPACITY_UNSEEN))
                    return;

                if (intent.hasExtra(SHOWING_LINE)) {
                    ArrayList<VsdLayer> layerList = vsdLineLayerMap.get(uid);
                    if (layerList != null && !layerList.isEmpty()) {
                        for (VsdLayer layer : layerList) {
                            if (layer != null) {
                                if (layer.getViewshed() != null) {
                                    setOpacity(intent, layer.getViewshed());
                                }
                            }
                        }
                    }
                } else {
                    if (!singleVsdLayerMap.containsKey(uid))
                        return;

                    List<VsdLayer> vsdLayers = singleVsdLayerMap.get(uid);
                    if (FileSystemUtils.isEmpty(vsdLayers))
                        return;

                    VsdLayer layer = vsdLayers.get(0);
                    if (layer.getViewshed() != null) {
                        setOpacity(intent, layer.getViewshed());
                    }
                }
                break;
            case ACTION_DISMISS_VIEWSHED: {
                if (!singleVsdLayerMap.containsKey(uid))
                    return;

                List<VsdLayer> vsdLayers = singleVsdLayerMap.get(uid);
                if (FileSystemUtils.isEmpty(vsdLayers))
                    return;

                VsdLayer layer = vsdLayers.get(0);
                layer.uninstall();
                singleVsdLayerMap.remove(uid);
                break;
            }
            case ACTION_DISMISS_VIEWSHED_LINE:
                ArrayList<VsdLayer> layerList = vsdLineLayerMap.get(uid);
                if (layerList != null && !layerList.isEmpty()) {
                    for (VsdLayer layer : layerList) {
                        if (layer != null) {
                            layer.uninstall();
                        }
                    }
                }
                vsdLineLayerMap.remove(uid);

                break;
        }
    }

    private void setOpacity(Intent intent, ViewShedLayer2 vs) {
        if (intent.hasExtra(EXTRA_ELEV_OPACITY)) {
            vs.setOpacitySeen(
                    intent.getIntExtra(
                            EXTRA_ELEV_OPACITY_SEEN, 50));
            vs.setOpacitySeen(
                    intent.getIntExtra(
                            EXTRA_ELEV_OPACITY_UNSEEN, 50));
        } else if (intent.hasExtra(EXTRA_ELEV_OPACITY_SEEN)
                && intent.hasExtra(EXTRA_ELEV_OPACITY_UNSEEN)) {
            vs.setOpacitySeen(intent.getIntExtra(
                    EXTRA_ELEV_OPACITY_SEEN, 50));
            vs.setOpacityUnseen(intent.getIntExtra(
                    EXTRA_ELEV_OPACITY_UNSEEN, 50));
        }

    }

    private void showViewshed(String uid, GeoPoint gp, double radius,
            int opacitySeen, int opacityUnseen) {
        ArrayList<VsdLayer> vsdLayers = vsdLineLayerMap.get(uid);
        if (vsdLayers == null) {
            vsdLayers = new ArrayList<>();
            vsdLineLayerMap.put(uid, vsdLayers);
        }
        VsdLayer layer = new VsdLayer();
        vsdLayers.add(layer);

        if (layer.getViewshed().getPointOfInterest() == null)
            layer.getViewshed().setPointOfInterest(gp);
        else if (layer.getViewshed().getPointOfInterest().getLatitude() != gp
                .getLatitude()
                ||
                layer.getViewshed().getPointOfInterest().getLongitude() != gp
                        .getLongitude()
                ||
                layer.getViewshed().getPointOfInterest().getAltitude() != gp
                        .getAltitude())
            layer.getViewshed().setPointOfInterest(gp);

        if (layer.getViewshed().getRadius() != radius)
            layer.getViewshed().setRadius(radius);

        if (layer.getViewshed().getOpacitySeen() != opacitySeen)
            layer.getViewshed().setOpacitySeen(opacitySeen);

        if (layer.getViewshed().getOpacityUnseen() != opacityUnseen)
            layer.getViewshed().setOpacityUnseen(opacityUnseen);

        if (!layer.getVisible()) {
            layer.install();
        }
    }

}
