
package com.atakmap.android.routes.elevation.model;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.atakmap.android.elev.ViewShedReceiver;
import com.atakmap.android.elev.ViewShedReceiver.VsdLayer;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.routes.elevation.AnalysisPanelPresenter;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPoint.AltitudeReference;
import com.atakmap.map.Globe;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.math.MathUtils;

import java.util.UUID;

public class SeekerMarker implements OnSharedPreferenceChangeListener {
    public static final int MIN_VSD_ELEV = 2;

    private Icon _centerIcon = null;
    private final MapView _mapView;
    private Marker _marker;
    private boolean bCenterMap;
    private boolean showViewshed;
    private boolean quickDraw = false;
    private final ViewShedReceiver vsdRec;
    private VsdLayer layer;
    // Auto centering suppression is needed to fix bug 5976.
    // Auto centering was triggering while dragging vertices around in the map view
    private boolean _suppressAutoCentering = false;

    private final AtakPreferences prefs;

    public SeekerMarker(MapView mapView) {
        this._mapView = mapView;

        prefs = AtakPreferences.getInstance(mapView
                .getContext());
        this.bCenterMap = prefs.get("elevProfileCenterOnSeeker", true);
        this.showViewshed = prefs.get(
                AnalysisPanelPresenter.PREFERENCE_SHOW_VIEWSHED, false);

        prefs.registerListener(this);
        vsdRec = ViewShedReceiver.getInstance();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp,
            String key) {

        if (key == null)
            return;

        if (key.equals("elevProfileCenterOnSeeker")) {
            bCenterMap = prefs.get(
                    "elevProfileCenterOnSeeker", true);
        } else if (key
                .equals(AnalysisPanelPresenter.PREFERENCE_SHOW_VIEWSHED)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_ALT)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_CIRCLE)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_RADIUS)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_MODEL)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY_SEEN)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY_TOGETHER)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY_UNSEEN)
                ||
                key.equals(
                        AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY)) {
            showViewshed = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_SHOW_VIEWSHED, false);
            if (showViewshed && _marker != null)
                draw(_marker.getPoint(), Feature.AltitudeMode.Absolute);
            else {
                if (layer != null) {
                    layer.uninstall();
                    layer = null;
                }
            }
        }
    }

    public void make(GeoPoint p) {
        if (_centerIcon == null)
            _centerIcon = new Icon("asset:/icons/seekermarker.png");

        if (_marker != null)
            clear();

        String _uid = UUID.randomUUID().toString();

        _marker = new Marker(_uid);
        _marker.setIcon(_centerIcon);
        _marker.setVisible(true);
        _marker.setShowLabel(false);
        _marker.setTitle("Seeker");
        _marker.setType("seeker");
        _marker.setMetaBoolean("force_only_touch", true);
        _marker.setMetaBoolean("addToObjList", false);
        _marker.setMetaBoolean("adapt_marker_icon", false);
        _marker.setMetaBoolean("ignoreOffscreen", false);
        _marker.setMovable(false);
        _marker.setMetaBoolean("removable", false);
        _marker.setPoint(p);
        // HACK HACK HACK
        _marker.setZOrder(_marker.getZOrder() + 1);

        _mapView.getRootGroup().addItem(_marker);
    }

    public void setQuickDraw(boolean qd) {
        quickDraw = qd;
    }

    public void draw(GeoPoint p, Feature.AltitudeMode mode) {
        synchronized (this) {
            if (_marker == null)
                make(p);
        }
        _marker.setAltitudeMode(mode);
        draw(p);
    }

    public void draw(GeoPoint p) {
        synchronized (this) {
            if (_marker == null)
                make(p);
        }
        _marker.setPoint(p);

        if (bCenterMap && !_suppressAutoCentering) {
            double zoomScale = _mapView.getMapScale();
            final double maxGsd = ATAKUtilities.getMaximumFocusResolution(p);
            final double gsd = Globe.getMapResolution(_mapView.getDisplayDpi(),
                    zoomScale);
            if (gsd < maxGsd) {
                zoomScale = Globe.getMapScale(_mapView.getDisplayDpi(), maxGsd);
            }
            _mapView.getMapController().panZoomTo(p,
                    zoomScale, true);
        }

        if (showViewshed && p.isAltitudeValid()) {
            double groundElev = ATAKUtilities.getElevation(
                    p.getLatitude(),
                    p.getLongitude(), null);
            if (!GeoPoint.isAltitudeValid(groundElev)) {
                if (layer != null) {
                    layer.uninstall();
                    layer = null;
                }
                return;
            }

            int unseen_opacity = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY_UNSEEN,
                    50);
            int seen_opacity = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_OPACITY_SEEN,
                    50);
            unseen_opacity = MathUtils.clamp(unseen_opacity, 1, 99);
            seen_opacity = MathUtils.clamp(seen_opacity, 1, 99);

            boolean circle = true;

            double radius = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_RADIUS,
                    7000);
            int elevationModel = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_MODEL,
                    ElevationData.MODEL_TERRAIN);

            double heightAboveReference = prefs.get(
                    AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_ALT,
                    2d);

            double markerAltAGL = 0;
            if (p.getAltitudeReference() != AltitudeReference.AGL) {
                if (GeoPoint.isAltitudeValid(groundElev)) {
                    double altM = EGM96
                            .getAGL(p, groundElev);
                    if (GeoPoint.isAltitudeValid(altM))
                        markerAltAGL = altM;
                }
            } else {
                markerAltAGL = p.getAltitude();
            }
            double heightAboveGround = heightAboveReference;
            if (markerAltAGL > 0)
                heightAboveGround += markerAltAGL;

            if (heightAboveGround < MIN_VSD_ELEV)
                heightAboveGround = MIN_VSD_ELEV;

            double pointAlt = heightAboveGround;

            GeoPoint refPoint = new GeoPoint(p.getLatitude(),
                    p.getLongitude(),
                    pointAlt, AltitudeReference.AGL,
                    GeoPoint.UNKNOWN,
                    GeoPoint.UNKNOWN);

            if (layer == null)
                layer = new ViewShedReceiver.VsdLayer();

            if (layer.getViewshed().getPointOfInterest() == null)
                layer.getViewshed().setPointOfInterest(refPoint);
            else if (layer.getViewshed().getPointOfInterest()
                    .getLatitude() != refPoint
                            .getLatitude()
                    ||
                    layer.getViewshed().getPointOfInterest()
                            .getLongitude() != refPoint
                                    .getLongitude()
                    ||
                    layer.getViewshed().getPointOfInterest()
                            .getAltitude() != refPoint
                                    .getAltitude())
                layer.getViewshed().setPointOfInterest(refPoint);

            if (layer.getViewshed().getRadius() != radius)
                layer.getViewshed().setRadius(radius);
            if (quickDraw)
                layer.getViewshed().setResolution(101);
            else
                layer.getViewshed().setResolution(501);

            if (layer.getViewshed().getOpacityUnseen() != unseen_opacity)
                layer.getViewshed().setOpacityUnseen(unseen_opacity);
            if (layer.getViewshed().getOpacitySeen() != seen_opacity)
                layer.getViewshed().setOpacitySeen(seen_opacity);

            if (layer.getViewshed().getElevationModel() != elevationModel)
                layer.getViewshed().setElevationModel(elevationModel);

            if (layer.getViewshed().getCircle() != circle)
                layer.getViewshed().setCircle(circle);

            if (!layer.getVisible()) {
                layer.install();
            }
        } else {
            if (layer != null) {
                layer.uninstall();
                layer = null;
            }
        }

    }

    public void clear() {
        Intent i = new Intent(ViewShedReceiver.ACTION_DISMISS_VIEWSHED);
        if (_marker != null)
            i.putExtra(ViewShedReceiver.VIEWSHED_UID, _marker.getUID());

        AtakBroadcast.getInstance().sendBroadcast(i);

        if (_marker != null) {
            _mapView.getRootGroup().removeItem(_marker);
            _marker = null;
        }

        if (layer != null) {
            layer.uninstall();
            layer = null;
        }
    }

    public boolean getSuppressAutoCentering() {
        return _suppressAutoCentering;
    }

    public boolean setSuppressAutoCentering(boolean value) {
        return _suppressAutoCentering = value;
    }

}
