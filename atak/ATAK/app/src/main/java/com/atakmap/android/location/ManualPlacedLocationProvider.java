
package com.atakmap.android.location;

import android.graphics.Color;
import android.os.SystemClock;

import com.atakmap.android.location.framework.Location;
import com.atakmap.android.location.framework.LocationDerivation;
import com.atakmap.android.location.framework.LocationProvider;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class ManualPlacedLocationProvider extends LocationProvider {
    public static final int GPS_TIMEOUT_MILLIS = 2000;

    private static final String TAG = "ManualPlacedLocationProvider";

    public ManualPlacedLocationProvider() {
    }

    @Override
    public String getUniqueIdentifier() {
        return "manual-placed-provider";
    }

    @Override
    public String getTitle() {
        return "Manual Placement Provider";
    }

    @Override
    public String getDescription() {
        return "Allow for manual placement of a point";
    }

    @Override
    public String getSource() {
        return "NO GPS";
    }

    @Override
    public String getSourceCategory() {
        return "USER";
    }

    @Override
    public int getSourceColor() {
        return Color.RED;
    }

    @Override
    public synchronized void setEnabled(boolean enabled) {
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public void dispose() {
    }

    public void setLocation(GeoPointMetaData point) {
        Location l = new ManualEntryLocation(point);
        fireLocationChanged(l);

        // Legacy reasons //
        final MapView mapView = MapView.getMapView();
        if (mapView != null) {
            final MetaDataHolder2 mapData = mapView.getMapData();
            mapData.setMetaBoolean("fakeLocationAvailable", true);
            mapData.setMetaString("fakeLocation",
                    l.getPoint().toStringRepresentation());
            mapData.setMetaLong("fakeLocationTime", getLastUpdated());
        }
    }

    class ManualEntryLocation implements Location {
        GeoPointMetaData point;

        ManualEntryLocation(GeoPointMetaData point) {
            this.point = point;
        }

        @Override
        public long getLocationDerivedTime() {
            return -1;
        }

        @Override
        public GeoPoint getPoint() {
            return point.get();
        }

        @Override
        public double getBearing() {
            return Double.NaN;
        }

        @Override
        public double getSpeed() {
            return Double.NaN;
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
            return "Human Entered";
        }

        @Override
        public LocationDerivation getDerivation() {
            return new LocationDerivation() {
                @Override
                public String getHorizontalSource() {
                    return GeoPointMetaData.USER;
                }

                @Override
                public String getVerticalSource() {
                    return point.getAltitudeSource();
                }
            };
        }

        @Override
        public boolean isValid() {
            // within the GPS timeout window
            return SystemClock.elapsedRealtime()
                    - getLastUpdated() < GPS_TIMEOUT_MILLIS;
        }
    }

}
