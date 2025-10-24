
package com.atakmap.android.location;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.atakmap.android.location.framework.Location;
import com.atakmap.android.location.framework.LocationDerivation;
import com.atakmap.android.location.framework.LocationProvider;
import com.atakmap.android.location.framework.SatelliteDerivation;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.Permissions;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;

class NetworkLocationProvider extends LocationProvider
        implements LocationListener {

    public static final int GPS_TIMEOUT_MILLIS = 25000;

    private static final int gpsUpdateInterval = 1000;
    final private static double PRECISION_6 = 1000000;

    private final String TAG = "NetworkLocationProvider";

    private final Context context;

    private LocationManager locMgr;

    private boolean invalid = false;

    public NetworkLocationProvider(MapView mapView) {
        this.context = mapView.getContext();

    }

    @Override
    public String getSourceCategory() {
        return "NETWORK";
    }

    @Override
    public String getSource() {
        return "Network Signal Derived";
    }

    @Override
    public int getSourceColor() {
        return -1;
    }

    @Override
    public String getUniqueIdentifier() {
        return "internal-android-network";
    }

    @Override
    public String getTitle() {
        return "WiFi/Cell Signal Derived (NETD)";
    }

    @Override
    public String getDescription() {
        return "This provider determines location based on availability of cell tower and WiFi access points or also referred to as Network Signal Derived (NETD).";
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled)
            _startLocationGathering(context, gpsUpdateInterval);
        else
            _stopLocationGathering();
    }

    @Override
    public void dispose() {
        setEnabled(false);
    }

    @Override
    public boolean getEnabled() {
        return locMgr != null;
    }

    @SuppressLint({
            "MissingPermission"
    })
    synchronized private void _startLocationGathering(final Context context,
            final long interval) {

        // only try to start the location gathering if we haven't
        // started already

        if (locMgr == null &&
                Permissions.checkPermission(context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

            try {
                locMgr = (LocationManager) context
                        .getSystemService(Service.LOCATION_SERVICE);

                if (locMgr != null) {
                    final boolean gpsEnabled = locMgr
                            .isProviderEnabled(
                                    LocationManager.NETWORK_PROVIDER);
                    Log.d(TAG, "network location enabled: " + gpsEnabled);

                    locMgr.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            interval, 0f, this);
                } else {
                    throw new IllegalArgumentException("no location manager");
                }
            } catch (IllegalArgumentException iae) {
                Log.d(TAG,
                        "location updates are not available on this device: ",
                        iae);
            }
        }
    }

    @SuppressLint({
            "MissingPermission"
    })
    synchronized private void _stopLocationGathering() {
        if (locMgr != null) {
            locMgr.removeUpdates(this);
            locMgr = null;
        }
    }

    @Override
    public void onLocationChanged(@NonNull
    final android.location.Location loc) {
        try {
            // ATAK-9978 ATAK sent event with invalid location (NaN)
            // in the rare case that latitude or longitude
            // is Double.NaN - do not fire a update to the
            // location.
            if (Double.isNaN(loc.getLatitude()) ||
                    Double.isNaN(loc.getLongitude())) {
                return;
            }

            invalid = false;
            fireLocationChanged(new NetworkLocation(loc));

        } catch (Exception e) {
            Log.d(TAG, "error updating the location", e);
        }
    }

    @Override
    public void onProviderDisabled(@NonNull
    final String provider) {
        Log.d(TAG, "internal network derived disabled");
        invalid = true;
    }

    @Override
    public void onProviderEnabled(@NonNull
    final String provider) {
        Log.d(TAG, "internal network derived enabled");
    }

    @Override
    public void onStatusChanged(final String provider,
            final int status,
            final Bundle extras) {
        if (status == android.location.LocationProvider.OUT_OF_SERVICE) {
            Log.d(TAG, "internal network derived out of service");
            invalid = true;
        } else if (status == android.location.LocationProvider.TEMPORARILY_UNAVAILABLE) {
            Log.d(TAG, "internal network derived temporarily unavailable");
            invalid = true;
        } else {// LocationProvider.AVAILABLE
            // does not seem to be called when the program starts up and
            // the GPS fix is already present but stops receiving information
            // although this is good, a further fix was needed to be added
            // to the CotMapComponent to check for GPS expire.
            invalid = false;

        }
    }

    class NetworkLocation implements Location {

        private final android.location.Location l;
        private final GeoPoint point;

        NetworkLocation(android.location.Location l) {
            this.l = l;

            // note: http://gis.stackexchange.com/questions/8650/how-to-measure-the-accuracy-of-latitude-and-longitude/8674#8674
            // The fourth decimal place is worth up to 11 m: it can identify a parcel
            // of land. It is comparable to the typical accuracy of an uncorrected
            // GPS unit with no interference.
            //
            // The fifth decimal place is worth up to 1.1 m: it distinguish trees from
            // each other. Accuracy to this level with commercial GPS units can only be
            // achieved with differential correction.
            //
            // The sixth decimal place is worth up to 0.11 m: you can use this for
            // laying out structures in detail, for designing landscapes, building
            // roads. It should be more than good enough for tracking movements
            // of glaciers and rivers. This can be achieved by taking painstaking
            // measures with GPS, such as differentially corrected GPS.
            //
            // The seventh decimal place is worth up to 11 mm: this is good for much
            // surveying and is near the limit of what GPS-based techniques can achieve.

            // carry out to 6 digits
            final double locLat = Math.round(l.getLatitude() * PRECISION_6)
                    / PRECISION_6;
            // carry out to 6 digits
            final double locLon = Math.round(l.getLongitude() * PRECISION_6)
                    / PRECISION_6;

            final double alt = l.hasAltitude() ? l.getAltitude() : Double.NaN;

            final double accuracy = l.hasAccuracy() ? l.getAccuracy()
                    : Double.NaN;

            point = new GeoPoint(locLat, locLon,
                    EGM96.getHAE(locLat, locLon, alt),
                    GeoPoint.AltitudeReference.HAE,
                    accuracy, GeoPoint.UNKNOWN);
        }

        @Override
        public long getLocationDerivedTime() {
            return l.getTime();
        }

        @Override
        public GeoPoint getPoint() {
            return point;
        }

        @Override
        public double getBearing() {

            if (l.hasBearing())
                return l.getBearing();
            else
                return Double.NaN;
        }

        @Override
        public double getSpeed() {
            if (l.hasSpeed())
                return l.getSpeed();
            else
                return Double.NaN;
        }

        @Override
        public double getBearingAccuracy() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && l.hasBearingAccuracy())
                return l.getBearingAccuracyDegrees();
            else
                return Double.NaN;
        }

        @Override
        public double getSpeedAccuracy() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && l.hasSpeedAccuracy())
                return l.getSpeedAccuracyMetersPerSecond();
            else
                return Double.NaN;
        }

        @Override
        public int getReliabilityScore() {
            return 100;
        }

        @Override
        public String getReliabilityReason() {
            return "Stub";
        }

        @Override
        public LocationDerivation getDerivation() {
            return new NetworkDerivation(locMgr, l);
        }

        @Override
        public boolean isValid() {
            // within the GPS timeout window
            return SystemClock.elapsedRealtime()
                    - getLastUpdated() < GPS_TIMEOUT_MILLIS && !invalid;
        }
    }

    private static class NetworkDerivation implements SatelliteDerivation {

        NetworkDerivation(LocationManager locationManager,
                android.location.Location location) {

        }

        @Override
        public int getNumberSatellites() {
            return -1;
        }

        @Override
        public double getSNR(int sat) {
            return 0;
        }

        @Override
        public String getHorizontalSource() {
            return "NETD";
        }

        @Override
        public String getVerticalSource() {
            return "NETD";
        }

        @Override
        public int getFixQuality() {
            return 0;
        }
    }
}
