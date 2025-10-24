
package com.atakmap.android.location.framework;

public interface SatelliteDerivation extends LocationDerivation {

    /**
     * Return the total number of satellites in view.
     * @return the number of satellites in view
     */
    int getNumberSatellites();

    /**
     * Given a number less than or equal to the number of satellites, get the signal to
     * noise ratio
     * @param satNum the satellite number
     * @return the signal to noise ratio
     */
    double getSNR(int satNum);

    /**
     * The position status name using common NMEA categories, used mostly for user awareness
     * @return one of the following "PPS", "SPS", "RTK float Solution",
     * "RTK fix solution", "DGPS", "SBAS", "RTK", "OmniSTAR", "Invalid"
     */
    @Override
    String getHorizontalSource();

    /**
     * Returns the fix quality associated with the location.
     */
    int getFixQuality();

}
