
package com.atakmap.android.databridge;

import java.util.List;

import gov.tak.api.annotation.NonNull;

public interface DatasetProvider {

    /**
     * The uid that describes this specific dataset provider.    It is highly recommended
     * that this uid is immutable for the lifespan of this dataset provider.    It is up to
     * the provider to distribute changes to the dataset provider under the same uid if the
     * do not break previous compatibility.
     * @return the uid for the dataset provider
     */
    String getUID();

    /**
     * A human readable name for the dataset provider.
     * @return the human readable name
     */
    String getName();

    /**
     * A human readable description of what the dataset provider provides
     * @return the human readable description
     */
    String getDescription();

    /**
     * The package name for the plugin that supplies this dataset provider.
     * @return the package name for the plugin
     */
    String getPackageName();

    /**
     * The definition of the dataset as a list of dataset definitions
     * @return the list of dataset definitions
     */
    List<DatasetDefinition> getDefinitions();

    /**
     * Allow for a client to subscribe to a dataset given a query
     * @param query the query to use when subscribing to the dataset.
     * @param dataProviderCallback the callback to be used when data matches that query
     */
    void subscribe(@NonNull String tag, @NonNull List<DatasetQueryParam> query,
            @NonNull DatasetProviderCallback dataProviderCallback);

    /**
     * Unsubscribes the dataset provider
     * @param dataProviderCallback the callback used during registration
     */
    void unsubscribe(@NonNull DatasetProviderCallback dataProviderCallback);
}
