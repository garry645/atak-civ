
package com.atakmap.android.drawing.milsym;

import android.graphics.Color;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.Vector2D;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;

import java.util.ArrayList;
import java.util.Arrays;

import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.platform.marshal.MarshalManager;

final class MilSymUtils {

    static public boolean isDrawingShape(MapItem item) {
        return item instanceof DrawingCircle || item instanceof DrawingEllipse
                || item instanceof DrawingRectangle;
    }

    public static boolean isEditing(MapItem mapItem) {
        if (mapItem instanceof EditablePolyline)
            return mapItem.getEditable() || mapItem.hasMetaValue("creating");
        if (isDrawingShape(mapItem))
            return mapItem.getEditing();
        return false;
    }

    static Class<?> defaultShape(ISymbologyProvider symbology, String code) {
        final ShapeType clazz = symbology.getDefaultSourceShape(code);
        switch (clazz) {
            case Point:
                return Marker.class;
            case LineString:
            case Polygon:
                return DrawingShape.class;
            case Rectangle:
                return DrawingRectangle.class;
            case Ellipse:
                return DrawingEllipse.class;
            case Circle:
                return DrawingCircle.class;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static Style applyDashPattern(Style style, short pattern) {
        if (style instanceof BasicStrokeStyle) {
            BasicStrokeStyle bs = (BasicStrokeStyle) style;
            return new PatternStrokeStyle(
                    (int) Math.ceil(bs.getStrokeWidth()),
                    pattern,
                    Color.red(bs.getColor()) / 255f,
                    Color.green(bs.getColor()) / 255f,
                    Color.blue(bs.getColor()) / 255f,
                    Color.alpha(bs.getColor()) / 255f,
                    bs.getStrokeWidth());
        } else if (style instanceof CompositeStyle) {
            CompositeStyle c = (CompositeStyle) style;
            final Style[] applied = new Style[c.getNumStyles()];
            for (int i = 0; i < c.getNumStyles(); i++) {
                applied[i] = applyDashPattern(c.getStyle(i), pattern);
            }
            return new CompositeStyle(applied);
        } else {
            return style;
        }
    }

    static boolean isInsideBounds(IGeoPoint point, Vector2D[] bounds) {
        return Vector2D.polygonContainsPoint(
                new Vector2D(point.getLatitude(), point.getLongitude()),
                bounds);
    }

    static boolean isFullyInsideBounds(IGeoPoint[] controlPoints,
            Vector2D[] bounds) {
        for (IGeoPoint controlPoint : controlPoints) {
            if (!isInsideBounds(controlPoint, bounds))
                return false;
        }
        return true;
    }

    public static ArrayList<ArrayList<IGeoPoint>> clipPoints(
            IGeoPoint[] controlPoints, Envelope screenBounds) {
        Vector2D[] vecBounds = new Vector2D[] {
                new Vector2D(screenBounds.maxY, screenBounds.minX),
                new Vector2D(screenBounds.minY, screenBounds.minX),
                new Vector2D(screenBounds.minY, screenBounds.maxX),
                new Vector2D(screenBounds.maxY, screenBounds.maxX),
                new Vector2D(screenBounds.maxY, screenBounds.minX),
        };

        IGeoPoint[] bounds = new IGeoPoint[] {
                new GeoPoint(screenBounds.maxY, screenBounds.minX),
                new GeoPoint(screenBounds.minY, screenBounds.minX),
                new GeoPoint(screenBounds.minY, screenBounds.maxX),
                new GeoPoint(screenBounds.maxY, screenBounds.maxX),
                new GeoPoint(screenBounds.maxY, screenBounds.minX),

        };

        ArrayList<ArrayList<IGeoPoint>> results = new ArrayList<>();

        if (isFullyInsideBounds(controlPoints, vecBounds)) {
            results.add(new ArrayList<>(Arrays.asList(controlPoints)));
            return null;
        }

        Vector2D[] cp = new Vector2D[controlPoints.length];
        for (int i = 0; i < cp.length; ++i) {
            cp[i] = new Vector2D(controlPoints[i].getLatitude(),
                    controlPoints[i].getLongitude());
        }

        IGeoPoint[] segment = new IGeoPoint[2];

        int k = 0;

        for (int i = 0; i < controlPoints.length - 1; ++i) {
            segment[0] = controlPoints[i];
            segment[1] = controlPoints[i + 1];
            if (isFullyInsideBounds(segment, vecBounds)) {
                if (results.size() < k)
                    results.set(k, new ArrayList<>());
                else if (results.size() == k)
                    results.add(new ArrayList<>());

                results.get(k).add(segment[0]);
                results.get(k).add(segment[1]);
            } else {
                boolean p0Inside = false;
                boolean p1Inside = false;
                ArrayList<IGeoPoint> temp = new ArrayList<>();
                if (isInsideBounds(segment[0], vecBounds)) {
                    temp.add(segment[0]);
                    p0Inside = true;
                }

                for (int j = 0; j < 4; ++j) {
                    com.atakmap.coremap.maps.coords.GeoPoint intersection = GeoCalculations
                            .findIntersection(
                                    MarshalManager.marshal(segment[0],
                                            IGeoPoint.class,
                                            com.atakmap.coremap.maps.coords.GeoPoint.class),
                                    MarshalManager.marshal(segment[1],
                                            IGeoPoint.class,
                                            com.atakmap.coremap.maps.coords.GeoPoint.class),
                                    MarshalManager.marshal(bounds[j],
                                            IGeoPoint.class,
                                            com.atakmap.coremap.maps.coords.GeoPoint.class),
                                    MarshalManager.marshal(bounds[j + 1],
                                            IGeoPoint.class,
                                            com.atakmap.coremap.maps.coords.GeoPoint.class));
                    if (intersection != null) {
                        temp.add(MarshalManager.marshal(
                                intersection,
                                com.atakmap.coremap.maps.coords.GeoPoint.class,
                                IGeoPoint.class));
                    }
                }

                if (isInsideBounds(segment[1], vecBounds)) {
                    temp.add(segment[1]);
                    p1Inside = true;
                }

                if (!temp.isEmpty()) {
                    boolean starting = false;
                    if (p0Inside || results.size() <= k) {
                        starting = true;
                    } else if (p1Inside && !p0Inside) {
                        starting = true;
                        if (results.size() > k)
                            ++k;
                    }
                    if (results.size() <= k)
                        results.add(new ArrayList<>());

                    if (starting)
                        results.get(k).add(temp.get(0));
                    if (temp.size() > 1)
                        results.get(k).add(temp.get(1));
                }
            }
        }
        return results;
    }
}
