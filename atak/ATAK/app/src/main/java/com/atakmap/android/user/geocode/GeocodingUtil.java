
package com.atakmap.android.user.geocode;

import android.location.Address;
import android.util.Pair;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;

/**
 * Task to take an address and convert it to a GeoPoint.
 */
public class GeocodingUtil {

    private static final String TAG = "GeocodingUtil";

    public static final int NO_LIMIT = -1;

    public interface ResultListener {
        /**
         * There are results available from the geocoding task
         * @param coder the geocoder used to obtain the results
         * @param originalAddress the original address passed in, will be null if the address
         *                        was not used.
         * @param originalPoint the original point passed in, will be null if the point was not
         *                      used.
         * @param addresses the addresses returned by the geocoder
         * @param error if an exception has occurred during the geocoding thread, return it.
         */
        void onResult(@NonNull GeocodeManager.Geocoder coder,
                @Nullable String originalAddress,
                @Nullable GeoPoint originalPoint,
                @NonNull List<Pair<String, GeoPoint>> addresses,
                @Nullable GeocodeManager.GeocoderException error);
    }

    /**
     * Given a specific geocoder, look up an address given a bounds.
     * @param geocoder the geocoder to use
     * @param bounds the bounds
     * @param address the address string
     * @param limit the limit if the geocoder supports limiting returns, pass in NO_LIMIT
     *              if you do not want to limit the returns from the geocoder
     * @param resultListener the results of the lookup.
     */
    public static void lookup(@NonNull
    final GeocodeManager.Geocoder geocoder,
            @NonNull
            final GeoBounds bounds,
            @NonNull
            final String address,
            final int limit,
            @NonNull
            final ResultListener resultListener) {
        lookupImpl("geocoder-lookup-address", geocoder, bounds, address, null,
                limit, resultListener);

    }

    /**
     * Given a specific geocoder, look up an address given a bounds.
     * @param geocoder the geocoder to use
     * @param point the geopoint to fina the address for
     * @param limit the limit if the geocoder supports limiting returns, pass in NO_LIMIT
     *              if you do not want to limit the returns from the geocoder
     * @param resultListener the results of the lookup.
     */
    public static void lookup(@NonNull
    final GeocodeManager.Geocoder geocoder,
            @NonNull
            final GeoPoint point,
            final int limit,
            @NonNull
            final ResultListener resultListener) {
        lookupImpl("geocoder-lookup-point", geocoder, null, null, point, limit,
                resultListener);
    }

    private static void lookupImpl(@NonNull String tag,
            @NonNull
            final GeocodeManager.Geocoder geocoder,
            @Nullable
            final GeoBounds bounds,
            @Nullable
            final String address,
            @Nullable
            final GeoPoint point,
            final int limit,
            @NonNull
            final ResultListener resultListener) {

        final Thread t = new Thread(tag) {
            public void run() {
                final List<Pair<String, GeoPoint>> results = new ArrayList<>();
                GeocodeManager.GeocoderException exception = null;
                try {
                    List<Address> ret = null;
                    if (address != null)
                        ret = geocoder.getLocation(address, bounds);
                    else if (point != null)
                        ret = geocoder.getLocation(point);

                    if (ret == null)
                        throw new GeocodeManager.GeocoderException(
                                "error occurred when using: "
                                        + geocoder.getTitle(),
                                new Exception());

                    Log.w(TAG,
                            geocoder.getTitle() + " address matches: "
                                    + ret.size());
                    for (final Address address : ret) {
                        results.add(new Pair<>(
                                GeocodeConverter.getAddressString(address),
                                new GeoPoint(address.getLatitude(),
                                        address.getLongitude())));
                    }
                } catch (GeocodeManager.GeocoderException e) {
                    exception = e;
                }
                resultListener.onResult(geocoder, null, point, results,
                        exception);
            }
        };
        t.start();
    }
}
