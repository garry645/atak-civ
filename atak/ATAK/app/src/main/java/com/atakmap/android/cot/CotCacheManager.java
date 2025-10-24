
package com.atakmap.android.cot;

import android.os.Bundle;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.math.MathUtils;

import org.greenrobot.eventbus.EventBus;

import gov.tak.api.engine.map.cache.SmartCacheTrigger;
import gov.tak.api.engine.map.features.Geometry;
import gov.tak.api.engine.map.features.LineString;
import gov.tak.api.engine.map.features.Point;
import gov.tak.api.engine.map.features.Polygon;

/**
 *  Manage the raising of smart cache events in response to CoT traffic.
 */
final class CotCacheManager implements MapEventDispatchListener {

    public static final String TAG = CotCacheManager.class.getSimpleName();
    private final EventBus eventBus = EventBus.getDefault();

    public CotCacheManager() {
    }

    @Override
    public void onMapEvent(MapEvent event) {
        MapItem mapItem = event.getItem();
        final Bundle b = event.getExtras();

        // smart caching is only triggered for CoT received over the network
        if (b == null || !(b.containsKey("serverFrom")
                || b.containsKey("endpointFrom")))
            return;

        if (mapItem instanceof PointMapItem) {
            // XXX - temporarily disabling smart caching for point traffic pending a solution for
            //       scalability concerns
            if (true)
                return;
            PointMapItem marker = (PointMapItem) mapItem;
            GeoPoint markerPoint = marker.getPoint();
            Point location = new Point(markerPoint.getLongitude(),
                    markerPoint.getLatitude());
            raiseCacheEvent(location);
        } else if (mapItem instanceof Polyline &&
                !MathUtils.hasBits(((Polyline) mapItem).getStyle(),
                        Polyline.STYLE_CLOSED_MASK)) {

            Polyline polyline = (Polyline) mapItem;
            LineString lineString = new LineString(2);
            for (GeoPoint polylinePoint : polyline.getPoints()) {
                lineString.addPoint(polylinePoint.getLongitude(),
                        polylinePoint.getLatitude());
            }
            if (lineString.getNumPoints() > 1)
                raiseCacheEvent(lineString);
        } else if (mapItem instanceof Shape) {
            Shape shape = (Shape) mapItem;
            LineString lineString = new LineString(2);
            String shapeType = shape.getType();
            GeoPoint[] shapePoints = shape.getPoints();
            if (shapePoints.length == 0)
                return;

            for (GeoPoint shapePoint : shapePoints) {
                lineString.addPoint(shapePoint.getLongitude(),
                        shapePoint.getLatitude());
                if (shapeType.equals("u-d-r")
                        && lineString.getNumPoints() == 4) {
                    // Calling getPoints() for a rectangle returns 4 side points in addition to
                    // the corner points. I don't think we want them for this use case.
                    break;
                }
            }

            if (lineString.getNumPoints() >= 3) {
                // add the first point at the end to enclose the region
                lineString.addPoint(shapePoints[0].getLongitude(),
                        shapePoints[0].getLatitude());

                Polygon location = new Polygon(lineString);
                raiseCacheEvent(location);
            } else if (lineString.getNumPoints() == 2) {
                // simple segment
                raiseCacheEvent(lineString);
            } // else malformed
        }
    }

    private void raiseCacheEvent(Geometry triggerGeometry) {
        //TODO-- some reasonable rect geometry that defines the view extent passed to trigger

        SmartCacheTrigger trigger = new SmartCacheTrigger(triggerGeometry, 0,
                Double.NaN, Double.NaN, null, null);
        eventBus.post(trigger);
    }
}
