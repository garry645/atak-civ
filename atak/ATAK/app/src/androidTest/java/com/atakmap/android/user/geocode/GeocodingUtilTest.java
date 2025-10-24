
package com.atakmap.android.user.geocode;

import android.content.Context;
import android.location.Address;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class GeocodingUtilTest extends ATAKInstrumentedTest {

    private final Object lock = new Object();

    boolean wait;
    List<Pair<String, GeoPoint>> retval = null;
    GeoPoint op;

    @Test
    public void geocodingTest() {
        Context ctx = ApplicationProvider.getApplicationContext();
        synchronized (lock) {
            retval = null;
            op = null;
            wait = true;

            GeocodingUtil.lookup(geocoder, new GeoPoint(0, 0),
                    GeocodingUtil.NO_LIMIT, new GeocodingUtil.ResultListener() {
                        @Override
                        public void onResult(GeocodeManager.Geocoder coder,
                                String originalAddress, GeoPoint originalPoint,
                                List<Pair<String, GeoPoint>> addresses,
                                GeocodeManager.GeocoderException error) {
                            retval = addresses;
                            op = originalPoint;
                            wait = false;
                        }
                    });

            while (wait) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }
            Assert.assertEquals(1, retval.size());
            Assert.assertEquals(op, new GeoPoint(0, 0));
            Assert.assertEquals(retval.get(0).second, new GeoPoint(0, 0));
            Assert.assertEquals("100 WrongWay Street Boondocks, Nowhere",
                    retval.get(0).first);
        }
    }

    final GeocodeManager.Geocoder geocoder = new GeocodeManager.Geocoder() {
        @Override
        public String getUniqueIdentifier() {
            return "test";
        }

        @Override
        public String getTitle() {
            return "test";
        }

        @Override
        public String getDescription() {
            return "test";
        }

        @Override
        public boolean testServiceAvailable() {
            return true;
        }

        @Override
        public List<Address> getLocation(GeoPoint geoPoint) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(geoPoint.getLatitude());
            a.setLongitude(geoPoint.getLongitude());
            return new ArrayList<>(Collections.singleton(a));
        }

        @Override
        public List<Address> getLocation(String address, GeoBounds bounds) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(0);
            a.setLongitude(0);
            return new ArrayList<>(Collections.singleton(a));
        }
    };
}
