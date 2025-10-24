
package com.atakmap.android.util;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class HorizonUtilities {

    private final static double RADIAN_CURVE = Math.toRadians(360d / 40030d);

    /**
     * Given a height above sea level (msl) in meters, this will return the maximum distance to the
     * horizon based on the simplified version of the formula described in
     * <a href="https://sites.math.washington.edu/~conroy/m120-general/horizon.pdf">Horizon Calculator</a>
     * which is off by maximally 0.1991% at 30480 meters msl.
     * @param elev the elevation in meters msl
     * @return the distance to the horizon in meters where the curvature of the earth occludes visibility
     */
    public static double getHorizonDistance(final double elev) {
        return 3569.72d * Math.sqrt(elev);
    }

    /**
     * Given a distance in meters, calculate the drop based on the curvature of the earth.
     * Uses a simplistic formula for computing the drop in height over a distance as described by
     * <a href="https://earthcurvature.com/">Earth Curvature</a>.
     * @param distance the distance in meters
     * @return the drop in meters based on the distance
     */
    public static double getCurvatureDrop(final double distance) {
        return 6371000d * (1 - Math.cos(RADIAN_CURVE * (distance / 1000)));
    }

}
