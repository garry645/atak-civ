
package com.atakmap.android.location.framework;

import android.os.SystemClock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.Disposable;

/**
 * An base implementation of a location provider able to be registered with the location manager.
 * The concrete implementation of this should provide for both polling as well as callback
 * for the last reported location.
 */
abstract public class LocationProvider implements Disposable {
    private final Set<LocationSubscriber> subscribers = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private Location lastReportedLocation = null;
    private long lastReportedTime = -1;

    public static final int DETAIL_HARDWARE_IDENTIFIER = 0;
    public static final int DETAIL_MANUFACTURER_IDENTIFIER = 1;
    public static final int DETAIL_RADIO_COTTYPE = 2;
    public static final int DETAIL_RADIO_UID = 3;

    public interface LocationSubscriber {

        /**
         * Callback for when the location changes or when the status changes.   For
         * example if the GPS is invalid, the onLocationChanged would be fired with
         * the location provider and the location object set with the isValid flag set
         * to false.
         * @param locationProvider the location provider for the location change
         * @param location the location
         */
        void onLocationChanged(@NonNull LocationProvider locationProvider,
                @NonNull Location location);

        /**
         * Called if there is raw data that is changing for the location provider
         * @param locationProvider the location provider for the raw data change
         * @param type the type of data such as NMEA, etc.
         * @param data the byte array of the data
         */
        void rawLocationCallback(@NonNull LocationProvider locationProvider,
                @NonNull String type, @NonNull byte[] data);
    }

    /**
     * Used to identify the provider when the user selects it for use in the system.
     * @return the suggested return is a constant unique identifier for the specific
     * implementation.
     */
    @NonNull
    public abstract String getUniqueIdentifier();

    /**
     * A freetext title used to describe to the user the actual name of the location
     * provider capability.
     * @return a text string not guaranteed to be unique.
     */
    @NonNull
    public abstract String getTitle();

    /**
     * A freetext description used to describe to the user the actual location provider.
     * @return a text string not guaranteed to be unique.
     */
    @NonNull
    public abstract String getDescription();

    /**
     * Provide a detail given a DETAIL_* or null if the detail is known known
     * @return the product identifier or null if not known.
     */
    public String getDetail(int detail) {
        return null;
    }

    /**
     * Based on the system real time clock and is only accurate during comparisons with
     * other location providers.   This should not be used to calculate time AND is not
     * impacted by changes to the system time.
     * @return a monotonic increasing number in milliseconds
     */
    public final long getLastUpdated() {
        return lastReportedTime;
    }

    /**
     * Return the source of the location provider which is required to be one of
     * the defined strings in the return.
     * @return one of "INTERNAL", "SERIAL", "NETWORK", "FUSED", "UNKNOWN", "USER"
     */
    @NonNull
    public abstract String getSourceCategory();

    /**
     * A human readable name for the source which preferably will be as short as possible.
     * A suggestive size is under 15 characters.
     * @return the human readable name
     */
    @NonNull
    public abstract String getSource();

    /**
     * A suggestive color for the source which will be used during rendering.
     * @return the suggestive ARGB color such as defined in java as Color.YELLOW
     * or -1 if the default should be used.
     */
    public abstract int getSourceColor();

    /**
     * Allow for the provider to be notified if it should be enabled or disabled.
     * This can be used to free up resources when the provider is not enabled.
     * @param enabled the status.
     */
    public abstract void setEnabled(boolean enabled);

    /**
     * Query the location provider to see if it is enabled or disabled.   This might
     * not always be a direct reflection of the call to setEnabled(boolean) as there
     * might be additional hardware required that is missing before a provider can
     * be considered enabled.
     * @return the status of the location provider.
     */
    public abstract boolean getEnabled();

    /**
     * Return the last location reported by the location provider.
     * @return the last location or null if no location ever reported.
     */
    public final Location getLastReportedLocation() {
        return lastReportedLocation;
    }

    /**
     * Register a subscriber for the location provider.
     * @param subscriber the subscriber
     */
    public final void register(LocationSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Gets the set of all registered LocationSubscribers
     * @return Set of registered LocationSubscribers
     */
    public final Set<LocationSubscriber> getSubscribers() {
        return new HashSet<>(subscribers);
    }

    /**
     * Unregister a subscriber for the location provider.
     * @param subscriber the subscriber
     */
    public final void unregister(LocationSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Call when a new location is provided.   Please note that a new location object
     * should be provided each time this is called.
     * @param location the most recent location
     */
    final protected void fireLocationChanged(final Location location) {
        lastReportedTime = SystemClock.elapsedRealtime();
        lastReportedLocation = location;
        for (final LocationSubscriber locationSubscriber : subscribers) {
            locationSubscriber.onLocationChanged(this, location);
        }
    }

    /**
     * Call when new raw data is provided.
     * @param type the type of the data to help a client better process it for example
     *             "NMEA"
     * @param data the byte array that is the data and it is up to the client to
     *             process it
     */
    final protected void fireDataChanged(final String type, final byte[] data) {
        for (final LocationSubscriber locationSubscriber : subscribers) {
            locationSubscriber.rawLocationCallback(this, type, data);
        }
    }
}
