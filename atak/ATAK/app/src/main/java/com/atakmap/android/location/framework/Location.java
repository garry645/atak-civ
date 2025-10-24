
package com.atakmap.android.location.framework;

import com.atakmap.coremap.maps.coords.GeoPoint;

import gov.tak.api.annotation.NonNull;

/**
 * Interface for implementation of a Location that can be provided by
 * a LocationProvider.
 */
public interface Location {

    /**
     * The time in milliseconds since EPOCH for the reported location time or -1 if not
     * supported.
     * @return the reported location derived time.
     */
    long getLocationDerivedTime();

    /**
     * The geopoint representing the point including latitude, longitude,
     * altitude in meters MSL (set to Double.NaN if not known), and CE/LE
     * (set to Double.NaN if not known).
     * @return the latitude in decimal degrees
     */
    @NonNull
    GeoPoint getPoint();

    /**
     * The heading in degrees magnetic or Double.NaN if not known.
     * @return the heading
     */
    double getBearing();

    /**
     * The accuracy of the bearing if known otherwise NaN if the accuracy is unknown.
     * Returns the estimated bearing accuracy in degrees of this location at the 68th
     * percentile confidence level.  This means that there is 68% chance that the true
     * bearing at the time of this location falls within getBearing() ()} +/- this
     * uncertainty.
     * @return the accuracy of the bearing
     */
    double getBearingAccuracy();

    /**
     * The speed in meters per second or Double.NaN if not known.
     * @return the speed in meters per second
     */
    double getSpeed();

    /**
     * The accuracy of the speed in meters per second if known otherwise NaN
     * if the accuracy is unknown.
     * Returns the estimated speed accuracy in meters per second of this location
     * at the 68th percentile confidence level. This means that there is 68% chance
     * that the true speed at the time of this location falls within getSpeed() ()}
     * +/- this uncertainty.
     * @return the accuracy of the speed in meters per second
     */
    double getSpeedAccuracy();

    /**
     * Reliability is based on additional notions of if spoofing or jamming has
     * been detected and is a confidence score from 0 to 100 where 100 is considered
     * perfect reliability.    A score of zero might be obtained if the location
     * is considered valid but contains invalid values.
     */
    int getReliabilityScore();

    /**
     * The human readable reason for the reliability score.  This is freetext and used to
     * further amplify the meaning of the reliability score.
     * @return the human readable reason.
     */
    @NonNull
    String getReliabilityReason();

    /**
     * Derivation Method for the particular location event.
     */
    @NonNull
    LocationDerivation getDerivation();

    /**
     * The validity of the location as provided by the location provider.   The location provider
     * is responsible for determining what would identify this location as valid.   For example
     * if an NMEA derived location is not within a certain period of time, it is no longer marked
     * as valid.
     */
    boolean isValid();

}
