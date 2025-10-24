
package com.atakmap.android.location;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.atakmap.android.location.framework.Location;
import com.atakmap.android.location.framework.LocationDerivation;
import com.atakmap.android.location.framework.LocationProvider;
import com.atakmap.android.location.framework.SatelliteDerivation;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.preference.AtakPreferences;
import gov.tak.platform.lang.Parsers;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import gov.tak.api.annotation.DeprecatedApi;

@Deprecated
@DeprecatedApi(since = "5.0", removeAt = "5.4", forRemoval = true)
class MapDataLocationProvider extends LocationProvider
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int GPS_TIMEOUT_MILLIS = 12000;

    private static final String TAG = "MapDataLocationProvider";
    private final MetaDataHolder2 mapData;
    private MapDataPollingThread mapDataPollThread = null;
    private long lastUpdateTime = -1;

    private static final int MIN_GPS_UPDATE_INTERVAL = 200;
    private static final int DEFAULT_GPS_UPDATE_INTERVAL = 1000;
    private static final String PREF_GPS_UPDATE_INTERVAL_KEY = "gps.update.interval";
    private int gpsUpdateInterval = DEFAULT_GPS_UPDATE_INTERVAL;
    private final AtakPreferences prefs;

    public MapDataLocationProvider(MetaDataHolder2 mapData) {
        this.mapData = mapData;
        prefs = AtakPreferences.getInstance(null);
        prefs.registerListener(this);
        gpsUpdateInterval = Parsers.parseInt(
                prefs.get(PREF_GPS_UPDATE_INTERVAL_KEY, ""),
                DEFAULT_GPS_UPDATE_INTERVAL);
    }

    @Override
    public String getUniqueIdentifier() {
        return "legacy-map-bundle-provider";
    }

    @Override
    public String getTitle() {
        return "Legacy Location Provider";
    }

    @Override
    public String getDescription() {
        return "This is to help with deprecation of using the map bundle to store location information";
    }

    @Override
    public String getSource() {
        return ((MapDataLocation) getLastReportedLocation()).locationSource;
    }

    @Override
    public String getSourceCategory() {
        return null;
    }

    @Override
    public int getSourceColor() {
        String color = ((MapDataLocation) getLastReportedLocation()).locationSourceColor;
        if (color == null)
            return -1;
        else
            return Color.parseColor(
                    ((MapDataLocation) getLastReportedLocation()).locationSourceColor);
    }

    @Override
    public synchronized void setEnabled(boolean enabled) {
        if (mapDataPollThread != null) {
            mapDataPollThread.cancel();
            mapDataPollThread = null;
        }

        if (enabled) {
            mapDataPollThread = new MapDataPollingThread();
            mapDataPollThread.setName("MapDataPollingThread");
            mapDataPollThread.start();
        }
    }

    @Override
    public boolean getEnabled() {
        return mapDataPollThread != null && mapDataPollThread.isAlive();
    }

    @Override
    public void dispose() {
        setEnabled(false);
        prefs.unregisterListener(this);
    }

    @Override
    public String getDetail(final int detail) {
        final Location loc = getLastReportedLocation();
        if (loc != null) {
            if (detail == DETAIL_RADIO_COTTYPE)
                return ((MapDataLocation) loc).locationParentType;
            else if (detail == DETAIL_RADIO_UID)
                return ((MapDataLocation) loc).locationParentUID;
        }
        return super.getDetail(detail);
    }

    private class MapDataPollingThread extends Thread {
        private volatile boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public void run() {
            while (!cancelled) {
                try {
                    final String effectivePrefix = mapData
                            .get("locationSourcePrefix");

                    // do not process fake, fine or null prefixes
                    if (effectivePrefix != null
                            && !effectivePrefix.equals("fake")
                            && !effectivePrefix.equals("fine")) {
                        final long updateTime = mapData
                                .getMetaLong(effectivePrefix + "LocationTime",
                                        -1);
                        if (updateTime > lastUpdateTime) {
                            lastUpdateTime = updateTime;
                            MapDataLocation location = new MapDataLocation();
                            location.updateTime = updateTime;
                            location.point = GeoPoint
                                    .parseGeoPoint(mapData.getMetaString(
                                            effectivePrefix + "Location",
                                            null));
                            location.locationParentUID = mapData.getMetaString(
                                    effectivePrefix + "LocationParentUID",
                                    null);
                            location.locationParentType = mapData.getMetaString(
                                    effectivePrefix + "LocationParentType",
                                    null);
                            location.locationAvailable = mapData.getMetaBoolean(
                                    effectivePrefix + "LocationAvailable",
                                    true);
                            location.locationSrc = mapData
                                    .getMetaString(
                                            effectivePrefix + "LocationSrc",
                                            null);
                            location.locationAltSrc = mapData.getMetaString(
                                    effectivePrefix + "LocationAltSrc", null);
                            location.gpsTime = mapData
                                    .getMetaLong(effectivePrefix + "GPSTime",
                                            -1);
                            location.locationSourceColor = mapData
                                    .getMetaString(
                                            effectivePrefix
                                                    + "LocationSourceColor",
                                            null);
                            location.bearing = mapData.getMetaDouble(
                                    effectivePrefix + "LocationBearing",
                                    Double.NaN);
                            location.speed = mapData.getMetaDouble(
                                    effectivePrefix + "LocationSpeed",
                                    Double.NaN);
                            location.locationSource = mapData.getMetaString(
                                    effectivePrefix + "LocationSource", null);
                            location.fixQuality = mapData
                                    .getMetaInteger(
                                            effectivePrefix + "FixQuality", -1);
                            location.numSatellites = mapData.getMetaInteger(
                                    effectivePrefix + "NumSatellites", -1);
                            if (location.point != null)
                                fireLocationChanged(location);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error reading the map data location", e);
                }
                try {
                    Thread.sleep(gpsUpdateInterval);
                } catch (InterruptedException ignore) {
                    cancelled = true;
                }
            }

        }
    }

    class MapDataLocation implements Location {
        private String locationParentUID;
        private String locationParentType;
        private boolean locationAvailable;
        private String locationSrc;
        private String locationAltSrc;
        private long gpsTime;
        private String locationSourceColor;
        private double bearing;
        private double speed;
        private String locationSource;
        private int fixQuality;
        private int numSatellites;
        private long updateTime;
        private GeoPoint point;

        @Override
        public long getLocationDerivedTime() {
            return gpsTime;
        }

        @Override
        public GeoPoint getPoint() {
            return point;
        }

        @Override
        public double getBearing() {
            return bearing;
        }

        @Override
        public double getSpeed() {
            return speed;
        }

        @Override
        public double getBearingAccuracy() {
            return Double.NaN;
        }

        @Override
        public double getSpeedAccuracy() {
            return Double.NaN;
        }

        @Override
        public int getReliabilityScore() {
            return 100;
        }

        @Override
        public String getReliabilityReason() {
            return "Not Yet Implemented";
        }

        @Override
        public LocationDerivation getDerivation() {
            return new MapDataSatelliteDerivation(this);
        }

        @Override
        public boolean isValid() {
            // within the GPS timeout window
            return SystemClock.elapsedRealtime()
                    - getLastUpdated() < GPS_TIMEOUT_MILLIS;
        }
    }

    private static class MapDataSatelliteDerivation
            implements SatelliteDerivation {

        MapDataLocation location;

        MapDataSatelliteDerivation(MapDataLocation location) {
            this.location = location;
        }

        @Override
        public int getNumberSatellites() {
            return location.numSatellites;
        }

        @Override
        public double getSNR(int sat) {
            return 0;
        }

        @Override
        public String getHorizontalSource() {
            return location.locationSrc;
        }

        @Override
        public int getFixQuality() {
            return location.fixQuality;
        }

        @Override
        public String getVerticalSource() {
            return location.locationAltSrc;
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
