
package gov.tak.api.engine.map.coords;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.GeomagneticField;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableMGRSPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.EngineLibrary;
import com.atakmap.map.elevation.ElevationManager;

/**
 * Landing zone for all of the static methods within GeoPoint.
 */
public final class GeoCalculations
{
    static
    {
        EngineLibrary.initialize();
    }

    static public final String TAG = "GeoCalculations";

    final static int HEMISPHERE_EAST = 0;
    final static int HEMISPHERE_WEST = 1;

    final static int CALC_SLANT = 0x02;
    final static int CALC_QUICK = 0x01;

    private GeoCalculations()
    {
    }

    /**
     * Given a source and a destination compute the bearing.
     *
     * @param source      the source point
     * @param destination the destination point
     * @return the bearing in degrees
     */
    public static double bearing(final IGeoPoint source,
                                 final IGeoPoint destination)
    {

        double retval = bearing(source.getLatitude(), source.getLongitude(),
                destination.getLatitude(), destination.getLongitude(), 0);
        if (retval < 0)
            retval += 360;
        return retval;
    }

    /**
     * Compute the straight line (as the crow flies) distance
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return value in meters for the straight line distance
     */
    public static double distance(final IGeoPoint start,
                                  final IGeoPoint destination)
    {
        return distance(start.getLatitude(), start.getLongitude(), 0d,
                destination.getLatitude(), destination.getLongitude(), 0d,
                0);
    }

    /**
     * Compute the haversine distance
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return value in meters for the straight line distance
     */
    public static double haversine(final IGeoPoint start,
                                   final IGeoPoint destination)
    {
        return distance(start.getLatitude(), start.getLongitude(), 0d,
                destination.getLatitude(), destination.getLongitude(), 0d,
                CALC_QUICK);
    }

    /**
     * Compute the slant distance to (hypotenuse)
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return value in meters for the slant distance
     */
    public static double slantDistance(final IGeoPoint start,
                                       final IGeoPoint destination)
    {
        return distance(start.getLatitude(),
                start.getLongitude(),
                toHae(start),
                destination.getLatitude(),
                destination.getLongitude(),
                toHae(destination),
                CALC_SLANT);
    }

    /**
     * Calculates a new IGeoPoint at the given azimuth and distance away from the source point
     *
     * @param src      the source point
     * @param azimuth  Azimuth in degrees True North
     * @param distance Meters
     * @return the point computed based on the src, azimuth and distance
     */
    public static IGeoPoint pointAtDistance(IGeoPoint src, double azimuth,
                                            double distance)
    {
        return pointAtDistance(src.getLatitude(), src.getLongitude(), azimuth,
                distance, 0);
    }

    /**
     * Computes the destination point from the {@linkplain IGeoPoint starting point}, given an
     * azimuth and distance in the direction of the destination point.
     *
     * <P>The <code>distance</code> parameter ALWAYS represents surface distance, whether or not
     * the <code>inclination</code> is <code>NAN</code>. This means that for computing slant points,
     * <code>distance</code> does not equal the <I>slant range</I>.
     *
     * @param src         Starting point of the calculation.
     * @param azimuth     Azimuth in degrees (True North)
     * @param distance    Surface distance between the start and destination point in meters
     * @param inclination from start point, <code>NAN</code> for surface only computation
     * @return Destination point, if the point is antipodal from the starting point then the
     * calculation could be off. Attempts to calculate the altitude from inclination.
     */
    public static IGeoPoint pointAtDistance(IGeoPoint src, double azimuth,
                                            double distance, double inclination)
    {

        return pointAtDistance(src.getLatitude(), src.getLongitude(), src.getAltitude(), azimuth,
                distance, inclination, 0);
    }

    public static IGeoPoint pointAtDistance(IGeoPoint src, IGeoPoint dst,
                                            double weight)
    {
        final double srcLat = src.getLatitude();
        final double srcLng = src.getLongitude();
        final double dstLat = dst.getLatitude();
        final double dstLng = dst.getLongitude();

        // interpolate the surface location
        final IGeoPoint surface = pointAtDistance(
                srcLat,
                srcLng,
                bearing(srcLat, srcLng, dstLat, dstLng, 0),
                distance(srcLat, srcLng, 0, dstLat, dstLng, 0, 0) * weight,
                0);

        if (Double.isNaN(src.getAltitude()) && Double.isNaN(dst.getAltitude()))
            return surface;

        // interpolate the altitude
        double srcAlt = !Double.isNaN(src.getAltitude()) ? src.getAltitude() : 0d;
        double dstAlt = !Double.isNaN(dst.getAltitude()) ? dst.getAltitude() : 0d;

        return new GeoPoint(surface.getLatitude(), surface.getLongitude(),
                srcAlt + (dstAlt - srcAlt) * weight);
    }

    /**
     * Computes the midpoint of the line-of-bearing formed by the two points
     *
     * @param a the start point
     * @param b the end point
     * @return the computed mid point
     */
    public static IGeoPoint midpoint(IGeoPoint a, IGeoPoint b)
    {
        if (a != null && b != null)
            return midpoint(a.getLatitude(), a.getLongitude(), a.getAltitude(),
                    b.getLatitude(), b.getLongitude(), b.getAltitude(), 0);
        else if (a == null && b == null)
            return null;
        else if (a != null)
            return a;
        else if (b != null)
            return b;
        else
            throw new IllegalStateException();
    }

    /**
     * Computes the inclination from one point to another.
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return the value in degrees
     */
    public static double slantAngle(final IGeoPoint start,
                                    final IGeoPoint destination)
    {
        return slantAngle(start.getLatitude(),
                start.getLongitude(),
                toHae(start),
                destination.getLatitude(),
                destination.getLongitude(),
                toHae(destination),
                CALC_SLANT);
    }

    /**
     * Given a latitude, provides the approximate meters per degree at that latitude.
     *
     * @param latitude the latitude
     * @return the approximate meters per degree for the longitude
     */
    public static double approximateMetersPerDegreeLongitude(double latitude)
    {
        final double rlat = Math.toRadians(latitude);
        return 111412.84 * Math.cos(rlat) - 93.5 * Math.cos(3 * rlat);
    }

    /**
     * Given a latitude, provides the approximate meters per degree at that latitude.
     *
     * @param latitude the latitude
     * @return the approximate meters per degree for the latitude
     */
    public static double approximateMetersPerDegreeLatitude(double latitude)
    {
        final double rlat = Math.toRadians(latitude);
        return 111132.92 - 559.82 * Math.cos(2 * rlat)
                + 1.175 * Math.cos(4 * rlat);
    }

    /**
     * Queries the declination angle between True North and Magnetic North at
     * the given location at the given datetime.
     *
     * <P>True bearing is computed by adding the declination value to the magnetic bearing.
     *
     * @param point GeoPoint for the location of the Compass.
     * @return Bearing in degrees (True North)
     */
    public static double magneticDeclination(final IGeoPoint point)
    {
        return magneticDeclination(point, CoordinatedTime.currentTimeMillis());
    }

    /**
     * Queries the declination angle between True North and Magnetic North at
     * the given location at the given datetime.
     *
     * <P>True bearing is computed by adding the declination value to the magnetic bearing.
     *
     * @param point    GeoPoint for the location of the Compass.
     * @param datetime The datetime in epoch milliseconds UTC
     * @return Bearing in degrees (True North)
     */
    public static double magneticDeclination(final IGeoPoint point, final long datetime)
    {
        // Use the GMF around the initial point to find the declination
        GeomagneticField gmf = new GeomagneticField(
                (float) point.getLatitude(),
                (float) point.getLongitude(), 0f,
                datetime);
        return gmf.getDeclination();
    }

    /**
     * Convert a bearing from Magnetic North to True North
     *
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (Magnetic North)
     * @return Bearing in degrees (True North)
     */
    public static double convertFromMagneticToTrue(final IGeoPoint point,
                                                   final double angle)
    {
        return convertFromMagneticToTrue(point, angle, CoordinatedTime.currentTimeMillis());
    }

    /**
     * Convert a bearing from Magnetic North to True North
     *
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (Magnetic North)
     * @return Bearing in degrees (True North)
     */
    public static double convertFromMagneticToTrue(final IGeoPoint point,
                                                   final double angle,
                                                   long datetime)
    {

        // Use the GMF around the initial point to find the declination
        double dec = magneticDeclination(point, datetime);
        // Convert Azimuth into True North
        // Heading would be less than 0 or greater than 360!
        double truth = angle + (double) dec;
        if (truth >= 360d)
        {
            return truth - 360d;
        } else if (truth < 0d)
        {
            return truth + 360d;
        } else
        {
            return truth;
        }
    }

    /**
     * Convert a bearing from True North to Magnetic North
     *
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (True North)
     * @return Bearing in degrees (Magnetic North)
     */
    public static double convertFromTrueToMagnetic(final IGeoPoint point,
                                                   final double angle)
    {
        return convertFromTrueToMagnetic(point, angle, CoordinatedTime.currentTimeMillis());
    }

    /**
     * Convert a bearing from True North to Magnetic North
     *
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (True North)
     * @return Bearing in degrees (Magnetic North)
     */
    public static double convertFromTrueToMagnetic(final IGeoPoint point,
                                                   final double angle,
                                                   long datetime)
    {

        // Use the GMF around the initial point to find the declination
        double dec = magneticDeclination(point, datetime);
        // Convert Azimuth into True North
        // Heading would be less than 0 or greater than 360!
        double mag = angle - (double) dec;
        if (mag >= 360d)
        {
            return mag - 360d;
        } else if (mag < 0d)
        {
            return mag + 360d;
        } else
        {
            return mag;
        }
    }

    /*
     * Obtains the grid convergence used for a specific line of bearing.
     * @param sPoint the starting point for the line of bearing.
     * @param ePoint the end point for the line of bearing.
     * @return the grid deviation (grid convergence) for the provided line of
     * bearing.      The value is in angular degrees between [-180.0, 180.0)
     */
    public static double computeGridConvergence(IGeoPoint sPoint, IGeoPoint ePoint)
    {
        final double d = distance(sPoint, ePoint);

        MutableMGRSPoint alignedend = new MutableMGRSPoint(
                sPoint.getLatitude(), sPoint.getLongitude());
        alignedend.offset(0, d);
        double[] enddd = alignedend.toLatLng(null);
        return bearing(sPoint, new GeoPoint(enddd[0], enddd[1]));
    }

    /**
     * Obtains the grid convergence used for a specific line of bearing.
     *
     * @param sPoint   the starting point for the line of bearing.
     * @param angle    the angle of the line of bearing.
     * @param distance the length of the line of bearing.
     * @return the grid deviation (grid convergence) for the provided line of
     * bearing.   It is important to note that grid convergence cannot be acheived
     * unless the line of bearing has some length.  This will determine which grid
     * line is used to converge against.   The value is in angular degrees between [-180.0, 180.0)
     */
    public static double computeGridConvergence(final IGeoPoint sPoint,
                                                final double angle, final double distance)
    {
        final IGeoPoint ePoint = pointAtDistance(sPoint, angle, distance);
        return computeGridConvergence(sPoint, ePoint);
    }

    /**
     * Retrieve the MSL in meters from HAE
     *
     * @param lat - Latitude of location to retrieve
     * @param lon - Longitude of location to retrieve
     * @param alt - existing Altitude in HAE only
     * @return Altitude in MSL at specified point
     */
    public static double haeToMsl(final double lat, final double lon,
                                  final double alt)
    {

        double offset = geoidHeight(lat, lon);
        if (Double.isNaN(offset))
        {
            Log.e(TAG, "Bad computation for point: (lat: " + lat
                    + ",lon: "
                    + lon + ") hae=" + alt);
            return IGeoPoint.UNKNOWN;

        } else
        {
            // perplexed by what the source sould be. after a caculation is it truely really
            // from the original source, or should it say calculated.

            return alt - offset;
        }
    }

    /**
     * Given a latitude and longitude, convert the provided MSL ALtitude into the appropriate HAE representation.
     *
     * @param lat    the latitude
     * @param lon    the longitude
     * @param altMSL the altitude in MSL
     * @return altitude in HAE
     */
    public static double mslToHae(final double lat, final double lon,
                                  final double altMSL)
    {
        double offset = geoidHeight(lat, lon);

        if (Double.isNaN(offset))
        {
            Log.e(TAG, "Bad computation for point: (lat: " + lat
                    + ",lon: "
                    + lon + ") msl=" + altMSL);
            return IGeoPoint.UNKNOWN;

        } else
        {
            return altMSL + offset;
        }
    }

    public static IGeoPoint lineOfBearingIntersect(IGeoPoint aStart, double aBearing, IGeoPoint bStart, double bBearing)
    {
        return lineOfBearingIntersect(aStart.getLatitude(), aStart.getLongitude(), aBearing,
                bStart.getLatitude(), bStart.getLongitude(), bBearing);
    }

    /**
     * Given an array of points compute the average point
     *
     * @param points  the array of points
     * @param wrap180 true if the value should be wrapped to be within -180..180
     * @return the point that is the average.
     */
    public static IGeoPoint computeAverage(IGeoPoint[] points, boolean wrap180)
    {
        return computeAverage(points, 0, points.length, wrap180);
    }

    /**
     * Given an array of points compute the average point
     *
     * @param points the array of points
     * @return the point that is the average.
     */
    public static IGeoPoint computeAverage(IGeoPoint[] points)
    {
        return computeAverage(points, false);
    }

    /**
     * Find the point that is the average in an array of points
     *
     * @param points the array of points
     * @param offset the offset into the array of points
     * @param count  the number of points from the offset to consider
     * @return the average point
     */
    public static IGeoPoint computeAverage(IGeoPoint[] points, int offset,
                                                                          int count)
    {
        return computeAverage(points, offset, count, false);
    }

    /**
     * Find the point that is the average in an array of points
     *
     * @param points  the array of points
     * @param offset  the offset into the array of points
     * @param count   the number of points from the offset to consider
     * @param wrap180 if it is intended that these points will consider wrapping the IDL (continuous scrolling)
     * @return the average point
     */
    public static IGeoPoint computeAverage(IGeoPoint[] points, int offset,
                                                                          int count, boolean wrap180)
    {
        if (wrap180)
            wrap180 = crossesIDL(points, offset, count);

        count = Math.min(count, points.length - offset);
        if (count <= 0)
            return GeoPoint.ZERO_POINT;

        int hemi = -1;
        double avgLat = 0;
        double avgLong = 0;
        double div_size = 1d / count;
        for (int i = offset; i < offset + count; i++)
        {
            IGeoPoint p = points[i];
            if (wrap180)
            {
                if (hemi == -1)
                    hemi = getHemisphere(p);
                else
                    p = wrapLongitude(p, hemi);
            }
            avgLat += p.getLatitude() * div_size;
            avgLong += p.getLongitude() * div_size;
        }
        if (wrap180)
            avgLong = wrapLongitude(avgLong);
        return new GeoPoint(avgLat, avgLong);
    }

    static double toHae(IGeoPoint gp)
    {
        double alt = Double.isNaN(gp.getAltitude()) ? 0d : gp.getAltitude();
        if (gp.getAltitudeReference() == IGeoPoint.AltitudeReference.AGL)
        {
            final double terrainel = ElevationManager.getElevation(gp.getLatitude(), gp.getLongitude(), null);
            if (!Double.isNaN(terrainel))
                alt += terrainel;
        }
        return alt;
    }

    /**
     * Given a GeoPoint return an integer that describes it is in HEMISPHERE_WEST or HEMISPHERE_EAST.
     *
     * @param gp the GeoPoint to test
     * @return the integer describing which hemisphere.
     */
    static int getHemisphere(IGeoPoint gp)
    {
        return gp.getLongitude() < 0d ? HEMISPHERE_WEST : HEMISPHERE_EAST;
    }

    /**
     * Given a GeoPoint return a GeoPoint that is wrapped to the appropriate hemisphere.
     *
     * @param gp     a GeoPoint
     * @param toHemi the integer flag designating HEMISPHERE_WEST or HEMISPHERE_EAST
     * @return the GeoPoint with the longitude wrapped to the correct hemisphere, more positive is
     * east and more negative is west.
     */
    static IGeoPoint wrapLongitude(IGeoPoint gp, int toHemi)
    {
        int fromHemi = getHemisphere(gp);
        if (fromHemi == toHemi)
            return gp;
        double lng = gp.getLongitude();
        if (fromHemi == HEMISPHERE_WEST)
            lng += 360d;
        else if (fromHemi == HEMISPHERE_EAST)
            lng -= 360d;
        return new GeoPoint(gp.getLatitude(), lng, gp.getAltitude(),
                gp.getAltitudeReference(), gp.getCE(),
                gp.getLE());
    }

    /**
     * @param longitude
     * @return
     */
    static double wrapLongitude(double longitude)
    {
        if (longitude < -180d)
            return longitude + 360;
        else if (longitude > 180d)
            return longitude - 360;
        return longitude;
    }

    /**
     * Determine if an array of points crosses the IDL.
     *
     * @param points the array of points
     * @param offset the offset within the array
     * @param count  the number of points to consider from the offset
     * @return true if the designated points cross the IDL
     */
    static boolean crossesIDL(IGeoPoint[] points, int offset, int count)
    {
        count = Math.min(count, points.length - offset);
        if (count <= 0)
            return false;

        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
        for (int i = offset; i < offset + count; i++)
        {
            IGeoPoint p = points[i];
            if (p.getLongitude() < minLng)
                minLng = p.getLongitude();
            if (p.getLongitude() > maxLng)
                maxLng = p.getLongitude();
        }
        if (minLng < -180 || maxLng > 180)
            return true;

        // The problem here is that we don't know if the group of points is
        // supposed to wrap over the IDL or cover most of the planet
        // For now just assume the smaller span is correct
        return maxLng - minLng > 180;
    }

    /**
     * Determine if an array of points crosses the IDL.
     *
     * @param points the array of points
     * @return true if the designated points cross the IDL
     */
    static boolean crossesIDL(IGeoPoint[] points)
    {
        return crossesIDL(points, 0, points.length);
    }

    static native double distance(double lat1, double lng1, double alt1,
                                  double lat2, double lng2, double alt2, int flags);

    static native double slantAngle(double lat1, double lng1, double alt1,
                                    double lat2, double lng2, double alt2, int flags);

    static native double bearing(double lat1, double lng1, double lat2,
                                 double lng2, int flags);

    static native GeoPoint midpoint(double lat1, double lng1, double alt1,
                                    double lat2, double lng2, double alt2, int flags);

    static native GeoPoint pointAtDistance(double lat, double lng,
                                           double azimuth, double distance, int flags);

    static native GeoPoint pointAtDistance(double lat, double lng, double alt,
                                           double azimuth, double distance, double inclination, int flags);

    static native GeoPoint lineOfBearingIntersect(double lat1, double lng1, double brg1,
                                                  double lat2, double lng2, double brg2);

    /**
     * Retrieve the offset between the HAE and MSL values
     *
     * @param latitude  Latitude of location to retrieve
     * @param longitude Longitude of location to retrieve
     * @return Delta between the HAE and MSL values, or Double.NaN if the area is not valid.
     */
    public static double geoidHeight(final double latitude,
                                     final double longitude)
    {
        return ElevationManager.getGeoidHeight(latitude, longitude);
    }
}
