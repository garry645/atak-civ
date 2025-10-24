
package com.atakmap.android.databridge;

import java.util.List;

import gov.tak.api.annotation.NonNull;

/**
 * The callback to be used when data is available.
 */
public interface DatasetProviderCallback {

    enum Status {
        COMPLETE,
        ONGOING,
        ERROR
    }

    /**
     * Returns the data as a result of the query
     *
     * @param tag a client supplied tag that is returned as part of the onData callback.
     *            This is the same tag that is passed in during the subscription and
     *            can be used for bookkeeping by the client and should not be used for any
     *            purpose by the provider.
     * @param provider the provider that is performing the callback
     * @param query the list of query parameters
     * @param data the corresponding data that matches the query
     * @param status attempts to describe the state of the data callback in such
     *               cases as a provider of live data, the return might be ongoing
     *               where as in more static scenarios the return might be COMPLETE
     * @param msg a human readable message that might further elaborate the status
     *            most notably with regards to error returns.
     *
     */
    void onData(@NonNull String tag, @NonNull DatasetProvider provider,
            @NonNull List<DatasetQueryParam> query, @NonNull List<Dataset> data,
            @NonNull Status status, @NonNull String msg);

}
