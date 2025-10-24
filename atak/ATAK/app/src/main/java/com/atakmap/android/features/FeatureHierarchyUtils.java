
package com.atakmap.android.features;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Point;

public class FeatureHierarchyUtils {

    private final static ElevationManager.QueryParameters DTM_FILTER = new ElevationManager.QueryParameters();
    static {
        DTM_FILTER.elevationModel = ElevationData.MODEL_TERRAIN;
    }

    /**
     * Given a point compute the altitude based on the current terrain.
     * @param p the provided point
     * @return the GeoPoint with metadata.
     */
    static GeoPointMetaData getAltitude(Point p,
            Feature.AltitudeMode altitudeMode, boolean resolveRelative) {

        GeoPoint.AltitudeReference altRef = (altitudeMode == Feature.AltitudeMode.Relative)
                ? GeoPoint.AltitudeReference.AGL
                : GeoPoint.AltitudeReference.HAE;

        double alt = p.getZ();

        // add in the currently supplied altitude from the feature
        if (altitudeMode == Feature.AltitudeMode.Relative) {
            if (resolveRelative) {
                if (Double.isNaN(alt))
                    alt = 0d;
                alt += ElevationManager.getElevation(
                        p.getY(), p.getX(), DTM_FILTER);
            } else if (alt == 0d) {
                alt = Double.NaN;
            } else {
                altRef = GeoPoint.AltitudeReference.AGL;
            }
        }

        GeoPoint gp = new GeoPoint(p.getY(), p.getX(), alt, altRef);

        return GeoPointMetaData.wrap(gp);
    }
}
