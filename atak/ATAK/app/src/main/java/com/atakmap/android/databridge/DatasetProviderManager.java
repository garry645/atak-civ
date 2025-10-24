
package com.atakmap.android.databridge;

import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.annotation.NonNull;

public class DatasetProviderManager {

    private final ConcurrentHashMap<String, DatasetProvider> datasetProviders = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DatasetProviderListener> datasetProviderListeners = new ConcurrentLinkedQueue<>();
    private static DatasetProviderManager _instance;

    private static final String TAG = "DatasetProviderManager";

    /**
     * Used when a dataset provider has been added or removed.   The listener much check
     * with the manager to make which has occurred.
     */
    public interface DatasetProviderListener {
        /**
         * Fired when the state of the dataset provider has changed (added or removed).
         * @param uid the uid of the provider that has been added or removed.
         */
        void onDatasetProviderChanged(String uid);
    }

    private DatasetProviderManager() {
    }

    /**
     * Obtains the DatasetProvider Manager
     * @return the provider manager
     */
    public static synchronized DatasetProviderManager getInstance() {
        if (_instance == null)
            _instance = new DatasetProviderManager();
        return _instance;
    }

    /**
     * Register a listener for changes to the dataset providers
     * @param datasetProviderListener the dataset provider listener to be registered
     */
    public void registerDatasetProviderListener(
            @NonNull DatasetProviderListener datasetProviderListener) {
        datasetProviderListeners.add(datasetProviderListener);
    }

    /**
     * Unregister a dataset Provider to be available
     * @param datasetProviderListener the dataset provider
     */
    public void unregisterDatasetProviderListener(
            @NonNull DatasetProviderListener datasetProviderListener) {
        datasetProviderListeners.remove(datasetProviderListener);
    }

    /**
     * Register a dataset Provider to be available
     * @param datasetProvider the dataset provider
     */
    public void registerDatasetProvider(
            @NonNull DatasetProvider datasetProvider) {
        datasetProviders.put(datasetProvider.getUID(), datasetProvider);
        fireDatasetProviderChanged(datasetProvider);
    }

    /**
     * Unregister a dataset Provider to be available
     * @param datasetProvider the dataset provider
     */
    public void unregisterDatasetProvider(
            @NonNull DatasetProvider datasetProvider) {
        datasetProviders.remove(datasetProvider.getUID());
        fireDatasetProviderChanged(datasetProvider);
    }

    /**
     * Get the total number of dataset providers currently registered with the system
     * @return the collection of dataset providers.
     */
    public Collection<DatasetProvider> getDatasetProviders() {
        return new ArrayList<>(datasetProviders.values());
    }

    /**
     * Get the dataset provider that is described by the given uid
     * @param uid the uid of the desired provider
     * @return the dataset provider
     */
    public DatasetProvider getDatasetProvider(@NonNull String uid) {
        return datasetProviders.get(uid);
    }

    private void fireDatasetProviderChanged(DatasetProvider provider) {
        for (DatasetProviderListener dpl : datasetProviderListeners) {
            try {
                dpl.onDatasetProviderChanged(provider.getUID());
            } catch (Exception e) {
                Log.e(TAG, "error calling provider changed listener", e);
            }
        }
    }

}
