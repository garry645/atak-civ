package gov.tak.api.engine.map.coords;

public interface IGeoPoint
{
    /**
     * The value indicating unknown
     */
    double UNKNOWN = Double.NaN;

    enum AltitudeReference
    {
        HAE,
        AGL;

        public static AltitudeReference get(String name)
        {
            for (AltitudeReference ref : AltitudeReference.values())
            {
                if (ref.name().equals(name))
                    return ref;
            }
            return HAE;
        }
    }

    /**
     * Get the latitude position
     *
     * @return latitude in decimal degrees
     */
    double getLatitude();

    /**
     * Get the longitude position
     *
     * @return decimal degrees
     */
    double getLongitude();

    /**
     * Set the latitude in decimal degrees.  The value specified must be in the range of
     * <code>-90.0 to +90.0</code>.
     *
     * @param lat Latitude
     * @throws IllegalArgumentException If the value is out of bounds
     */
    void setLatitude(double lat) throws IllegalArgumentException;

    /**
     * Set the longitude in decimal degrees.  The value specified must be in the range of
     * <code>-180.0 to +180.0</code>.
     *
     * @param lon Longitude
     * @throws IllegalArgumentException If the value is out of bounds
     */
    void setLongitude(double lon) throws IllegalArgumentException;

    /**
     * Returns the altitude for the GeoPoint.
     */
    double getAltitude();

    /**
     * Returns the altitude reference for the GeoPoint.
     */
    AltitudeReference getAltitudeReference();

    /**
     * Set the altitude in meters HAE.
     *
     * @param altitude New altitude value
     */
    void setAltitude(double altitude);

    /**
     * Returns the circular error of 90 percent for the horizontal.
     *
     * @return the circular error, Double.NaN if invalid
     */
    double getCE();

    /**
     * Returns the linear error of 90 percent for the vertical.
     *
     * @return the linear error, Double.NaN if invalid
     */
    double getLE();
}
