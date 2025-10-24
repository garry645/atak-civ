
package com.atakmap.android.features;

import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.util.Collections;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Helper methods for feature data store operations and queries
 */
public class FeatureDataStoreUtils {

    private static final String TAG = "FeatureDataStoreUtils";

    /**
     * For a given feature data store, query all feature sets and build
     * a bounds envelope which encompasses all features
     *
     * @param dataStore Feature data store
     * @param params Feature set query params (null to query all)
     * @return Bounds envelope or null if failed
     *
     * @deprecated use {@link #buildEnvelope(FeatureDataStore2, FeatureDataStore2.FeatureSetQueryParameters)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static Envelope buildEnvelope(FeatureDataStore dataStore,
            FeatureDataStore.FeatureSetQueryParameters params) {
        return buildEnvelope(Adapters.adapt(dataStore),
                Adapters.adapt(params, null));
    }

    /**
     * For a given feature data store, query all feature sets and build
     * a bounds envelope which encompasses all features
     *
     * @param dataStore Feature data store
     * @param params Feature set query params (null to query all)
     * @return Bounds envelope or null if failed
     */
    public static Envelope buildEnvelope(FeatureDataStore2 dataStore,
            FeatureDataStore2.FeatureSetQueryParameters params) {
        Envelope.Builder bounds = new Envelope.Builder();
        FeatureSetCursor fsc = null;
        try {
            // Get all feature sets for this file
            fsc = dataStore.queryFeatureSets(params);
            while (fsc != null && fsc.moveToNext()) {
                FeatureSet fs = fsc.get();
                if (fs == null)
                    continue;

                // Build features envelope
                addToEnvelope(dataStore, fs, bounds);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to build features envelope for "
                    + dataStore.getUri(), e);
        } finally {
            if (fsc != null)
                fsc.close();
        }
        return bounds.build();
    }

    /**
     * Given a datastore, build the envelope for it
     * @param dataStore the datastore
     * @return the envelope that contains the datastore
     *
     * @deprecated use {@link #buildEnvelope(FeatureDataStore2, FeatureDataStore2.FeatureSetQueryParameters)} with {@code null} query params
     */
    public static Envelope buildEnvelope(FeatureDataStore dataStore) {
        return buildEnvelope(dataStore,
                (FeatureDataStore.FeatureSetQueryParameters) null);
    }

    /**
     * Build an envelope for a single feature set by querying its features
     *
     * @param dataStore The associated data store
     * @param featureSet Feature set
     * @return Envelope bounds or null if failed
     *
     * @deprecated use {@link #buildEnvelope(FeatureDataStore2, FeatureDataStore2.FeatureSetQueryParameters)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static Envelope buildEnvelope(FeatureDataStore dataStore,
            FeatureSet featureSet) {
        Envelope.Builder bounds = new Envelope.Builder();
        addToEnvelope(dataStore, featureSet, bounds);
        return bounds.build();
    }

    /**
     * Add a feature set to an existing envelope builder
     *
     * @param dataStore The associated data store
     * @param featureSet Feature set
     * @param bounds Envelope bounds
     *
     * @deprecated use {@link #addToEnvelope(FeatureDataStore2, FeatureSet, Envelope.Builder)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static void addToEnvelope(FeatureDataStore dataStore,
            FeatureSet featureSet, Envelope.Builder bounds) {
        addToEnvelope(Adapters.adapt(dataStore), featureSet, bounds);
    }

    public static void addToEnvelope(FeatureDataStore2 dataStore,
            FeatureSet featureSet, Envelope.Builder bounds) {
        if (featureSet == null)
            return;
        FeatureCursor fec = null;
        try {
            FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
            params.featureSetFilter = new FeatureDataStore2.FeatureSetQueryParameters();
            params.featureSetFilter.ids = Collections.singleton(
                    featureSet.getId());
            fec = dataStore.queryFeatures(params);
            while (fec != null && fec.moveToNext()) {
                Feature feat = fec.get();
                if (feat == null || feat.getGeometry() == null)
                    continue;
                bounds.add(feat.getGeometry().getEnvelope());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add to feature set envelope: "
                    + featureSet.getName(), e);
        } finally {
            if (fec != null)
                fec.close();
        }
    }
}
