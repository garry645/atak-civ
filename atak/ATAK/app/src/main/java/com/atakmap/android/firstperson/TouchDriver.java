
package com.atakmap.android.firstperson;

import android.graphics.PointF;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

import gov.tak.api.engine.map.IMapRendererEnums;

class TouchDriver implements MapEventDispatcher.MapEventDispatchListener {
    private final static double SCROLL_SPEED_X = 0.0625d;
    private final static double SCROLL_SPEED_Y = -0.125;

    double _minZoom = 32d;

    MapView _mapView;
    MapRenderer3 _camera;

    GeoPoint _lookFrom;

    TouchDriver(MapView mapView) {
        _mapView = mapView;
        _camera = mapView.getRenderer3();

        _lookFrom = GeoPoint.createMutable();
    }

    /**
     * Activates the driver to start processing touch events to update the view.
     *
     * @param lookFrom  The _look from_ location. The provided point is considered _live_ and will
     *                  be updated when the driver is processing inputs.
     */
    public void start(GeoPoint lookFrom) {
        _lookFrom = lookFrom;

        // push all the dispatch listeners
        _mapView.getMapEventDispatcher().pushListeners();
        // clear all listeners
        _mapView.getMapEventDispatcher().clearListeners();
        // register on scroll to swivel camera on scroll motions
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_SCROLL, this);
        // register on scale to zoom camera on pinch
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_SCALE, this);
        // attach on other motions to mark as consumed
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_ROTATE, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_TILT, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_RELEASE, this);
    }

    public void stop() {
        _mapView.getMapEventDispatcher().popListeners();
    }

    @Override
    public void onMapEvent(MapEvent event) {
        final MapSceneModel sm = _camera.getMapSceneModel(
                false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        if (event.getType().equals(MapEvent.MAP_SCROLL)) {
            final PointF dxdy = event.getPointF();

            _camera.lookFrom(
                    _lookFrom,
                    sm.camera.azimuth + (dxdy.x * SCROLL_SPEED_X),
                    MathUtils.clamp(
                            sm.camera.elevation + (dxdy.y * SCROLL_SPEED_Y),
                            -90d, 89d),
                    IMapRendererEnums.CameraCollision.Ignore,
                    false);
        } else if (event.getType().equals(MapEvent.MAP_SCALE)) {
            final double lookFromTerrainElevation = ElevationManager
                    .getElevation(_lookFrom, null);

            double dx = (sm.camera.target.x - sm.camera.location.x)
                    * sm.displayModel.projectionXToNominalMeters;
            double dy = (sm.camera.target.y - sm.camera.location.y)
                    * sm.displayModel.projectionYToNominalMeters;
            double dz = (sm.camera.target.z - sm.camera.location.z)
                    * sm.displayModel.projectionZToNominalMeters;
            final double m = MathUtils.distance(dx, dy, dz, 0d, 0d, 0d);
            dx /= m;
            dy /= m;
            dz /= m;

            final double scale = event.getScaleFactor();
            final double translate = (scale - 1.0) * Math.max(
                    Math.abs(
                            _lookFrom.getAltitude() - lookFromTerrainElevation),
                    _minZoom);

            PointD newCamera = new PointD(
                    ((sm.camera.location.x
                            * sm.displayModel.projectionXToNominalMeters)
                            + (dx * translate))
                            / sm.displayModel.projectionXToNominalMeters,
                    ((sm.camera.location.y
                            * sm.displayModel.projectionYToNominalMeters)
                            + (dy * translate))
                            / sm.displayModel.projectionYToNominalMeters,
                    ((sm.camera.location.z
                            * sm.displayModel.projectionZToNominalMeters)
                            + (dz * translate))
                            / sm.displayModel.projectionZToNominalMeters);

            sm.mapProjection.inverse(newCamera, _lookFrom);
            _camera.lookFrom(
                    _lookFrom,
                    sm.camera.azimuth,
                    sm.camera.elevation,
                    IMapRendererEnums.CameraCollision.Ignore,
                    false);
        }

        // consume
        if (event.getExtras() != null)
            event.getExtras().putBoolean("eventNotHandled", false);
    }
}
