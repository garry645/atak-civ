
package com.atakmap.android.location;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.location.framework.Location;
import com.atakmap.android.location.framework.LocationDerivation;
import com.atakmap.android.location.framework.LocationProvider;
import com.atakmap.android.location.framework.SatelliteDerivation;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.preference.AtakPreferences;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.Permissions;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Pattern;

class InternalGPSLocationProvider extends LocationProvider implements
        GpsStatus.Listener, GpsStatus.NmeaListener, LocationListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int GPS_TIMEOUT_MILLIS = 12000;

    private static final int MIN_GPS_UPDATE_INTERVAL = 200;
    private static final int DEFAULT_GPS_UPDATE_INTERVAL = 1000;
    private static final String PREF_GPS_UPDATE_INTERVAL_KEY = "gps.update.interval";
    private int gpsUpdateInterval = DEFAULT_GPS_UPDATE_INTERVAL;
    private final AtakPreferences prefs;

    final private static double PRECISION_7 = 10000000;
    final private static double PRECISION_6 = 1000000;
    final private static double PRECISION_4 = 1000;
    private final Pattern comma = Pattern.compile(","); // thread safe

    private final String TAG = "InternalGPSLocationProvider";

    private final Context context;
    private final MapView _mapView;

    private LocationManager locMgr;

    private OnNmeaMessageListener newerNmeaListener;

    // used only in conjunction with the LocationListener and only guaranteed to be valid
    // if the Location.hasAltitude() is true.
    private double nmeaMSLAltitude = Double.NaN;

    private boolean invalid = false;

    public InternalGPSLocationProvider(MapView mapView) {
        this._mapView = mapView;
        this.context = mapView.getContext();
        prefs = AtakPreferences.getInstance(context);
        prefs.registerListener(this);
        gpsUpdateInterval = Parsers.parseInt(
                prefs.get(PREF_GPS_UPDATE_INTERVAL_KEY, ""),
                DEFAULT_GPS_UPDATE_INTERVAL);
    }

    @Override
    public String getSourceCategory() {
        return "INTERNAL";
    }

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public int getSourceColor() {
        return -1;
    }

    @Override
    public String getUniqueIdentifier() {
        return "internal-gps-chip";
    }

    @Override
    public String getTitle() {
        return "Internal GPS Device";
    }

    @Override
    public String getDescription() {
        return "The internal GPS chip on the device";
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled)
            _startLocationGathering(context,
                    Math.max(MIN_GPS_UPDATE_INTERVAL, gpsUpdateInterval));
        else
            _stopLocationGathering();
    }

    @Override
    public void dispose() {
        setEnabled(false);
        prefs.unregisterListener(this);
    }

    @Override
    public boolean getEnabled() {
        return locMgr != null;
    }

    @Override
    public String getDetail(final int id) {
        if (id == DETAIL_HARDWARE_IDENTIFIER) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return locMgr.getGnssHardwareModelName();
            } else {
                return "UNKNOWN";
            }
        } else if (id == DETAIL_MANUFACTURER_IDENTIFIER) {
            return Build.MANUFACTURER;
        }
        return null;
    }

    /**
     * Modifies the resolution of the GPS sampling.
     */
    @SuppressLint({
            "MissingPermission"
    })
    synchronized private void modifyGPSRequestInterval(int newInterval) {
        if (locMgr != null &&
                Permissions.checkPermission(context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            locMgr.removeUpdates(this);
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    newInterval, 0f, this);

        }
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
                            .isProviderEnabled(LocationManager.GPS_PROVIDER);
                    Log.d(TAG, "gps detected correct mode: " + gpsEnabled);

                    if (!gpsEnabled) {
                        HintDialogHelper
                                .showHint(
                                        context,
                                        context.getString(
                                                R.string.location_mode_title),
                                        context.getString(
                                                R.string.location_mode_desc),
                                        "gps.device.mode",
                                        new HintDialogHelper.HintActions() {
                                            @Override
                                            public void preHint() {

                                            }

                                            @Override
                                            public void postHint() {
                                                try {
                                                    final Intent intent = new Intent(
                                                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                    _mapView.getContext()
                                                            .startActivity(
                                                                    intent);
                                                } catch (ActivityNotFoundException ane) {
                                                    Log.d(TAG,
                                                            "no Settings.ACTION_LOCATION_SOURCE_SETTINGS activity found on this device");
                                                }
                                            }
                                        }, false);
                    }
                }

                if (locMgr != null) {
                    Log.d(TAG, "requesting gps location updates at " + interval
                            + "ms");
                    locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            interval, 0f, this);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        locMgr.registerGnssStatusCallback(
                                new GnssStatus.Callback() {
                                    @Override
                                    public void onStarted() {
                                    }

                                    @Override
                                    public void onStopped() {
                                    }

                                    @Override
                                    public void onFirstFix(int ttffMillis) {
                                    }

                                    @Override
                                    public void onSatelliteStatusChanged(
                                            @NonNull GnssStatus status) {

                                        // The current code mirrors the implementation of
                                        //
                                        // onGpsStatusChanged(
                                        //        GpsStatus.GPS_EVENT_SATELLITE_STATUS);
                                        //
                                        // leaving the implementation the same even though
                                        // it does nothing.

                                        int satelliteCount = 0;
                                        for (int i = 0; i < status
                                                .getSatelliteCount(); ++i) {
                                            if (status.usedInFix(i))
                                                satelliteCount++;
                                        }
                                    }
                                });
                    } else {
                        locMgr.addGpsStatusListener(this);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        locMgr.addNmeaListener(
                                newerNmeaListener = new OnNmeaMessageListener() {
                                    @Override
                                    public void onNmeaMessage(String message,
                                            long timestamp) {
                                        onNmeaReceived(timestamp, message);
                                    }
                                });
                    } else {
                        addNmeaListener(locMgr, this);
                    }
                } else {
                    throw new IllegalArgumentException("no location manager");
                }
            } catch (IllegalArgumentException iae) {
                Log.d(TAG,
                        "location updates are not available on this device: ",
                        iae);
                HintDialogHelper
                        .showHint(
                                context,
                                context.getString(R.string.tool_text38),
                                context.getString(R.string.tool_text39),
                                "device.gps.issue");
            } catch (SecurityException se) {
                Log.d(TAG,
                        "permisson not granted - location updates are not available on this device: ",
                        se);
            }
        }
    }

    @SuppressLint({
            "MissingPermission"
    })
    synchronized private void _stopLocationGathering() {
        if (locMgr != null) {
            locMgr.removeUpdates(this);
            removeNmeaListener(locMgr, this);

            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    if (newerNmeaListener != null)
                        locMgr.removeNmeaListener(newerNmeaListener);
                } catch (Exception e) {
                    Log.d(TAG, "error removing the newer nmea listener");
                }
            }
            locMgr = null;
        }
    }

    /**
     * Reflective calling of addNmeaLIstener(GpsStatus.NmeaListener) for compilation using Android 29
     * but for systems that need it (Android 21, 22, 23).
     *
     * @param locMgr   the location manager to use
     * @param listener the listener to register
     */
    private void addNmeaListener(final LocationManager locMgr,
            final GpsStatus.NmeaListener listener) {
        Log.d(TAG, "adding the GpsStatus.NmeaListener listener "
                + listener.getClass());
        try {
            final Class<?> c = locMgr.getClass();
            Method addNmeaListener = c.getMethod("addNmeaListener",
                    GpsStatus.NmeaListener.class);
            if (addNmeaListener != null)
                addNmeaListener.invoke(locMgr, listener);
            else
                Log.e(TAG,
                        "error occurred trying to find the addNmeaListener method");
        } catch (Exception e) {
            Log.e(TAG,
                    "error occurred trying to reflectively add GpsStatus.NmeaListener",
                    e);
        }
    }

    /**
     * Reflective calling of addNmeaLIstener(GpsStatus.NmeaListener) for compilation using Android 29
     * but for systems that need it (Android 21, 22, 23).
     *
     * @param locMgr   the location manager to use
     * @param listener the listener to register
     */
    private void removeNmeaListener(final LocationManager locMgr,
            final GpsStatus.NmeaListener listener) {
        Log.d(TAG, "removing the GpsStatus.NmeaListener listener "
                + listener.getClass());
        try {
            final Class<?> c = locMgr.getClass();
            Method removeNmeaListener = c.getMethod("removeNmeaListener",
                    GpsStatus.NmeaListener.class);
            if (removeNmeaListener != null)
                removeNmeaListener.invoke(locMgr, listener);
            else
                Log.e(TAG,
                        "error occurred trying to find the removeNmeaListener method");
        } catch (Exception e) {
            Log.e(TAG,
                    "error occurred trying to reflectively remove GpsStatus.NmeaListener",
                    e);
        }
    }

    @SuppressLint({
            "MissingPermission"
    })
    @Override
    public void onGpsStatusChanged(int status) {
        try {
            if (locMgr != null &&
                    Permissions.checkPermission(context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                GpsStatus gpsStatus = locMgr.getGpsStatus(null);
                if (gpsStatus != null) {
                    Iterable<GpsSatellite> satellites = gpsStatus
                            .getSatellites();
                    Iterator<GpsSatellite> sat = satellites.iterator();
                    //String lSatellites = null;
                    //int i = 0;

                    // not used at this time and brought in as part of the refactor

                    int satellitesInFix = 0;
                    while (sat.hasNext()) {
                        GpsSatellite satellite = sat.next();

                        if (satellite.usedInFix()) {
                            satellitesInFix++;
                        }
                        //lSatellites = "Satellite" + (i++) + ": "
                        //     + satellite.getPrn() + ","
                        //     + satellite.usedInFix() + ","
                        //     + satellite.getSnr() + ","
                        //     + satellite.getAzimuth() + ","
                        //     + satellite.getElevation()+ "\n\n";

                        //Log.d(TAG,lSatellites);
                    }
                    //Log.d(TAG, "sats in fix: " + satellitesInFix);
                }
            }
        } catch (Exception ioe) {
            Log.d(TAG, "error occurred", ioe);
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
            fireLocationChanged(new InternalGPSLocation(loc));

            // Legacy reasons //
            final MapView mapView = MapView.getMapView();
            final Location l = getLastReportedLocation();
            if (mapView != null) {
                final MetaDataHolder2 mapData = mapView.getMapData();
                mapData.setMetaBoolean("fineLocationAvailable", true);
                if (loc.hasSpeed())
                    mapData.setMetaDouble("fineLocationSpeed", loc.getSpeed());
                if (loc.hasBearing())
                    mapData.setMetaDouble("fineLocationBearing",
                            loc.getBearing());

                mapData.setMetaString("fineLocation",
                        l.getPoint().toStringRepresentation());
                mapData.setMetaLong("fineLocationTime", getLastUpdated());
            }

        } catch (Exception e) {
            Log.d(TAG, "error updating the location", e);
        }
    }

    @Override
    public void onProviderDisabled(@NonNull
    final String provider) {
        Log.d(TAG, "internal GPS disabled");
        invalid = true;
    }

    @Override
    public void onProviderEnabled(@NonNull
    final String provider) {
        Log.d(TAG, "internal GPS enabled");
    }

    @Override
    public void onStatusChanged(final String provider,
            final int status,
            final Bundle extras) {
        if (status == android.location.LocationProvider.OUT_OF_SERVICE) {
            Log.d(TAG, "internal GPS out of service");
            invalid = true;
        } else if (status == android.location.LocationProvider.TEMPORARILY_UNAVAILABLE) {
            Log.d(TAG, "internal GPS temporarily unavailable");
            invalid = true;
        } else {// LocationProvider.AVAILABLE
            // does not seem to be called when the program starts up and
            // the GPS fix is already present but stops receiving information
            // although this is good, a further fix was needed to be added
            // to the CotMapComponent to check for GPS expire.
            invalid = false;

        }
    }

    @Override
    public void onNmeaReceived(final long timestamp, final String nmea) {

        // try the three different variations of the GGA message
        if (nmea == null)
            return;

        if (nmea.startsWith("$GPGGA") ||
                nmea.startsWith("$GNGGA") ||
                nmea.startsWith("$GLGGA")) {

            String[] parts = comma.split(nmea, 0);
            if ((parts.length > 9) && (!parts[9].isEmpty())) {
                try {
                    nmeaMSLAltitude = Double.parseDouble(parts[9]);
                    // carry out to 4 digits
                    nmeaMSLAltitude = Math.round(nmeaMSLAltitude
                            * PRECISION_4)
                            / PRECISION_4;
                } catch (NumberFormatException nfe) {
                    // unable to parse the double
                }
            }
        }
        fireDataChanged("NMEA", nmea.getBytes(StandardCharsets.UTF_8));
    }

    class InternalGPSLocation implements Location {

        private final android.location.Location l;
        private final GeoPoint point;

        InternalGPSLocation(android.location.Location l) {
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

            final double alt = l.hasAltitude() ? nmeaMSLAltitude : Double.NaN;

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
            return new InternalGPSDerivation(locMgr, l);
        }

        @Override
        public boolean isValid() {
            // within the GPS timeout window
            return SystemClock.elapsedRealtime()
                    - getLastUpdated() < GPS_TIMEOUT_MILLIS && !invalid;
        }
    }

    private static class InternalGPSDerivation implements SatelliteDerivation {

        InternalGPSDerivation(LocationManager locationManager,
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
            return "GPS";
        }

        @Override
        public String getVerticalSource() {
            return "GPS";
        }

        @Override
        public int getFixQuality() {
            return 0;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            @Nullable String key) {
        if (key == null)
            return;
        if (key.equals(PREF_GPS_UPDATE_INTERVAL_KEY)) {
            gpsUpdateInterval = Parsers.parseInt(
                    prefs.get(PREF_GPS_UPDATE_INTERVAL_KEY, ""),
                    DEFAULT_GPS_UPDATE_INTERVAL);
            if (getEnabled()) {
                setEnabled(false);
                setEnabled(true);
            }
        }
    }
}
