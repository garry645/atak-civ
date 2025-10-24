
package com.atakmap.android.location.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.Disposable;

/**
 * Provides for a central management for location providers within the application.
 */
public class LocationManager implements Disposable {

    private static LocationManager _instance;

    private final List<LocationProvider> locationProviderList = new ArrayList<>();

    private final ConcurrentLinkedQueue<LocationProviderListChangeListener> subscribers = new ConcurrentLinkedQueue<>();

    // the meta key used to store a reference to the location provider used to generate
    // the geopoint - stored as part of the geopoint metadata.
    public static final String LOCATION_PROVIDER_REFERENCE = "location.provider.uid";

    public static final int HIGHEST_PRIORITY = 0;
    public static final int LOWEST_PRIORITY = 1;

    public interface LocationProviderListChangeListener {
        /**
         * Called whenever the list of providers has changed or if the sorting
         * order of the providers has been modified.
         */
        void onLocationProviderListChange();
    }

    /**
     * Protected access to support testing.
     */
    protected LocationManager() {
    }

    /**
     * Returns the singleton instance of the Location Manager
     * @return the location manager.
     */
    @NonNull
    public synchronized static LocationManager getInstance() {
        if (_instance == null)
            _instance = new LocationManager();

        return _instance;
    }

    /**
     * Registers a location provider based and adds it as the lowest priority and if the
     * provider is already registered, this will do nothing.
     * @param provider the location provider to register
     * @param priority one of HIGHEST_PRIORITY or LOWEST_PRIORITY.   All other values will
     *                 map to LOWEST_PRIORITY
     */
    public void registerProvider(@NonNull LocationProvider provider,
            int priority) {
        synchronized (locationProviderList) {
            if (!locationProviderList.contains(provider))
                if (priority == HIGHEST_PRIORITY)
                    locationProviderList.add(0, provider);
                else
                    locationProviderList.add(provider);
        }

        fireListChanged();
    }

    /**
     * Unregisters a location provider based on the unique identifier.
     * @param uid the unique identifier of the location provider to unregister
     */
    public void unregisterProvider(@NonNull
    final String uid) {
        synchronized (locationProviderList) {
            LocationProvider remove = null;
            for (LocationProvider locationProvider : locationProviderList) {
                if (locationProvider.getUniqueIdentifier().equals(uid)) {
                    remove = locationProvider;
                    break;
                }
            }
            locationProviderList.remove(remove);
        }
        fireListChanged();
    }

    /**
     * Returns a snapshot of the current location providers.
     * @return the list of location providers
     */
    @NonNull
    public List<LocationProvider> getLocationProviders() {
        synchronized (locationProviderList) {
            return new ArrayList<>(locationProviderList);
        }
    }

    /**
     * Returns a single provider based on the uid, or null if the provider does not exist.
     * @return the location provider based on the uid or null
     */
    public LocationProvider getLocationProvider(@NonNull
    final String uid) {
        synchronized (locationProviderList) {
            for (LocationProvider locationProvider : locationProviderList) {
                if (locationProvider.getUniqueIdentifier().equals(uid)) {
                    return locationProvider;
                }
            }
        }
        return null;
    }

    /**
     * Sets the provider order based on the list of provided UID's.  Any ones not specified are
     * shifted to the end.   The act of sorting based on location heuristics is completely outside
     * the scope of the manager.
     * @param uids the list of provider uid's.
     */
    public void setProviderOrder(@NonNull
    final List<String> uids) {
        synchronized (locationProviderList) {
            List<LocationProvider> ordered = new ArrayList<>();
            for (String uid : uids) {
                LocationProvider lp = null;
                for (LocationProvider locationProvider : locationProviderList) {
                    if (locationProvider.getUniqueIdentifier().equals(uid)) {
                        lp = locationProvider;
                        break;
                    }
                }
                locationProviderList.remove(lp);
                ordered.add(lp);
            }
            ordered.addAll(locationProviderList);

            locationProviderList.clear();
            locationProviderList.addAll(ordered);
        }
        fireListChanged();
    }

    /**
     * Returns the highest priority provider where the location is valid.   This should
     * be polled at the desired frequency to determine the best location provider to make
     * use of.   Can return null if no provider has a valid location.
     * @return the location provider with the highest priority valid location otherwise null
     */
    public LocationProvider getPreferredLocationProvider() {
        synchronized (locationProviderList) {
            for (LocationProvider locationProvider : locationProviderList) {
                if (locationProvider.getEnabled()) {
                    Location location = locationProvider
                            .getLastReportedLocation();
                    if (location != null && location.isValid())
                        return locationProvider;
                }
            }
        }
        return null;
    }

    /**
     * Register a subscriber for the location provider list.
     * @param subscriber the subscriber
     */
    public final void register(LocationProviderListChangeListener subscriber) {
        synchronized (subscribers) {
            if (!subscribers.contains(subscriber))
                subscribers.add(subscriber);
        }
    }

    /**
     * Unregister a subscriber for the location provider.
     * @param subscriber the subscriber
     */
    public final void unregister(
            LocationProviderListChangeListener subscriber) {
        synchronized (subscribers) {
            subscribers.remove(subscriber);
        }
    }

    protected List<LocationProviderListChangeListener> getListeners() {
        return new ArrayList<>(subscribers);
    }

    @Override
    public void dispose() {
        synchronized (locationProviderList) {
            for (LocationProvider lp : locationProviderList)
                lp.dispose();
        }
    }

    final protected void fireListChanged() {
        for (final LocationProviderListChangeListener s : subscribers) {
            s.onLocationProviderListChange();
        }
    }
}
