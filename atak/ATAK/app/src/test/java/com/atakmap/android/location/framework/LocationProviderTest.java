
package com.atakmap.android.location.framework;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

public class LocationProviderTest {
    private LocationProvider locationProvider;

    @Before
    public void setup() {
        locationProvider = new TestLocationProvider();
    }

    @Test
    public void testDoubleRegisterSubscriber() {
        LocationProvider.LocationSubscriber subscriber1 = new TestLocationSubscriber();

        locationProvider.register(subscriber1);
        locationProvider.register(subscriber1);

        Set<LocationProvider.LocationSubscriber> subscribers = locationProvider
                .getSubscribers();

        Assert.assertEquals(1, subscribers.size());
    }

    @Test
    public void testDoubleUnregisterSubscriber() {
        LocationProvider.LocationSubscriber subscriber1 = new TestLocationSubscriber();

        locationProvider.unregister(subscriber1);
        locationProvider.unregister(subscriber1);
        Set<LocationProvider.LocationSubscriber> subscribers = locationProvider
                .getSubscribers();

        Assert.assertTrue(subscribers.isEmpty());
    }

    private static class TestLocationSubscriber
            implements LocationProvider.LocationSubscriber {

        @Override
        public void onLocationChanged(LocationProvider locationProvider,
                Location location) {

        }

        @Override
        public void rawLocationCallback(LocationProvider locationProvider,
                String type, byte[] data) {

        }
    }

}
