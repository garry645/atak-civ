package com.atakmap.map.layer.feature.opengl;

import com.atakmap.coremap.maps.coords.GeoPoint;

import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.geometry.Geometry;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use the batch feature renderering framework */
@Deprecated
@DeprecatedApi(since = "4.1", forRemoval = true, removeAt = "4.4")
public final class GLFeatureLayer
{
    private static final String TAG = "GLFeatureLayer";

    /** @deprecated use {@link Utils#hitTest(Geometry, GeoPoint, double, GeoPoint)} */
    @Deprecated
    @DeprecatedApi(since = "5.2", forRemoval = true, removeAt = "5.5")
    public static boolean hitTest(Geometry g, GeoPoint point, double radius)
    {
        return hitTest(g, point, radius, null);
    }

    /** @deprecated use {@link Utils#hitTest(Geometry, GeoPoint, double, GeoPoint)} */
    @Deprecated
    @DeprecatedApi(since = "5.2", forRemoval = true, removeAt = "5.5")
    public static boolean hitTest(Geometry g, GeoPoint point, double radius, GeoPoint touchPoint)
    {
        return Utils.hitTest(g, point, radius, touchPoint);
    }
} // GLFeatureLayer
