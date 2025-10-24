
package com.atakmap.android.location.framework;

import com.atakmap.coremap.maps.coords.GeoPoint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class LocationManagerTest {
    private LocationManager locationManager;
    private TestLocationProvider provider1;
    private TestLocationProvider provider2;

    @Before
    public void setup() {
        locationManager = new LocationManager();

        provider1 = new TestLocationProvider();
        provider2 = new TestLocationProvider();
    }

    @Test
    public void testRegisterProvider() {
        LocationManager.LocationProviderListChangeListener listener = Mockito
                .mock(
                        LocationManager.LocationProviderListChangeListener.class);
        locationManager.register(listener);

        locationManager.registerProvider(provider1, 0);

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertEquals(1, providers.size());
        Mockito.verify(listener).onLocationProviderListChange();
    }

    @Test
    public void testRegisterProvider_HighPriority() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.HIGHEST_PRIORITY);

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertEquals(2, providers.size());
        Assert.assertEquals(provider2, providers.get(0));
        Assert.assertEquals(provider1, providers.get(1));
    }

    @Test
    public void testGetLocationProvider_Exists() {
        locationManager.registerProvider(provider1,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.LOWEST_PRIORITY);

        LocationProvider result = locationManager
                .getLocationProvider(provider1.getUniqueIdentifier());

        Assert.assertEquals(provider1, result);
    }

    @Test
    public void testGetLocationProvider_DoesNotExist() {
        locationManager.registerProvider(provider1,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.LOWEST_PRIORITY);

        LocationProvider result = locationManager
                .getLocationProvider("FOO BAR");

        Assert.assertNull(result);
    }

    @Test
    public void testUnregisterProvider() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        LocationManager.LocationProviderListChangeListener listener = Mockito
                .mock(
                        LocationManager.LocationProviderListChangeListener.class);
        locationManager.register(listener);

        locationManager.unregisterProvider(provider1.getUniqueIdentifier());

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertTrue(providers.isEmpty());
        Mockito.verify(listener).onLocationProviderListChange();
    }

    @Test
    public void testSetProviderOrder_EmptyList() {
        locationManager.registerProvider(provider1,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.LOWEST_PRIORITY);

        locationManager.setProviderOrder(new ArrayList<>());

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertEquals(provider1, providers.get(0));
        Assert.assertEquals(provider2, providers.get(1));
    }

    @Test
    public void testSetProviderOrder_NormalList() {
        locationManager.registerProvider(provider1,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.LOWEST_PRIORITY);
        List<String> priorities = new ArrayList<>();
        priorities.add(provider2.getUniqueIdentifier());
        priorities.add(provider1.getUniqueIdentifier());

        locationManager.setProviderOrder(priorities);

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertEquals(provider2, providers.get(0));
        Assert.assertEquals(provider1, providers.get(1));
    }

    @Test
    public void testSetProviderOrder_ShortList() {
        TestLocationProvider provider3 = new TestLocationProvider();
        locationManager.registerProvider(provider1,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.LOWEST_PRIORITY);
        locationManager.registerProvider(provider3,
                LocationManager.LOWEST_PRIORITY);
        List<String> priorities = new ArrayList<>();
        priorities.add(provider2.getUniqueIdentifier());

        locationManager.setProviderOrder(priorities);

        List<LocationProvider> providers = locationManager
                .getLocationProviders();
        Assert.assertEquals(provider2, providers.get(0));
        Assert.assertEquals(provider1, providers.get(1));
        Assert.assertEquals(provider3, providers.get(2));
    }

    @Test
    public void testDispose() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.HIGHEST_PRIORITY);

        locationManager.dispose();

        Assert.assertTrue(provider1.isDisposed());
        Assert.assertTrue(provider2.isDisposed());
    }

    @Test
    public void testRegisterAndUnregisterListener() {
        LocationManager.LocationProviderListChangeListener listener = Mockito
                .mock(
                        LocationManager.LocationProviderListChangeListener.class);

        locationManager.register(listener);

        List<LocationManager.LocationProviderListChangeListener> listeners = locationManager
                .getListeners();
        Assert.assertEquals(1, listeners.size());

        locationManager.unregister(listener);

        listeners = locationManager.getListeners();
        Assert.assertTrue(listeners.isEmpty());
    }

    @Test
    public void testGetPreferred_NoneEnabled() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.HIGHEST_PRIORITY);

        LocationProvider provider = locationManager
                .getPreferredLocationProvider();

        Assert.assertNull(provider);
    }

    @Test
    public void testGetPreferred_NullLocation() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.HIGHEST_PRIORITY);
        provider1.setEnabled(true);

        LocationProvider provider = locationManager
                .getPreferredLocationProvider();

        Assert.assertNull(provider);
    }

    @Test
    public void testGetPreferred_Success() {
        locationManager.registerProvider(provider1,
                LocationManager.HIGHEST_PRIORITY);
        locationManager.registerProvider(provider2,
                LocationManager.HIGHEST_PRIORITY);
        provider1.setEnabled(true);
        provider1.setLastLocation(new TestLocation());

        LocationProvider provider = locationManager
                .getPreferredLocationProvider();

        Assert.assertEquals(provider1, provider);

    }

    private static class TestLocation implements Location {
        @Override
        public long getLocationDerivedTime() {
            return 0;
        }

        @Override
        public GeoPoint getPoint() {
            return null;
        }

        @Override
        public double getBearing() {
            return 0;
        }

        @Override
        public double getSpeed() {
            return 0;
        }

        @Override
        public double getBearingAccuracy() {
            return 0;
        }

        @Override
        public double getSpeedAccuracy() {
            return 0;
        }

        @Override
        public int getReliabilityScore() {
            return 0;
        }

        @Override
        public String getReliabilityReason() {
            return null;
        }

        @Override
        public LocationDerivation getDerivation() {
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
