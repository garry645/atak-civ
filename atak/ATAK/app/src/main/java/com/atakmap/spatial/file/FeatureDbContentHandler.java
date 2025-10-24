
package com.atakmap.spatial.file;

import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore.FeatureSetQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Abstract handler for feature databases
 */
public abstract class FeatureDbContentHandler extends FileOverlayContentHandler
        implements Visibility2 {

    /** @deprecated to be removed without replacement */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected final FeatureDataStore _dataStore;
    private final FeatureDataStore2 _dataStore2;

    protected int _totalFeatureSets = -1;

    /** @deprecated use {@link #FeatureDbContentHandler(MapView, File, FeatureDataStore2, Envelope) */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected FeatureDbContentHandler(MapView mv, File file,
            FeatureDataStore db, Envelope bounds) {
        this(mv, file, Adapters.adapt(db), db, bounds);
    }

    protected FeatureDbContentHandler(MapView mv, File file,
            FeatureDataStore2 db, Envelope bounds) {
        this(mv, file, db, null, bounds);
    }

    private FeatureDbContentHandler(MapView mv, File file,
            FeatureDataStore2 db,
            FeatureDataStore legacy,
            Envelope bounds) {
        super(mv, file, bounds);
        _dataStore = legacy;
        _dataStore2 = db;
    }

    /**
     * Get the total number of feature sets owner by this handler
     * This is called post-initialization to allow sub-classes to override
     * Used for partial visibility calculations
     *
     * @return Number of feature sets
     */
    public int getTotalFeatureSetsCount() {
        if (_totalFeatureSets == -1) {
            try {
                _totalFeatureSets = _dataStore2.queryFeatureSetsCount(
                        buildQueryParams2());
            } catch (DataStoreException ignored) {
                return 0;
            }
        }
        return _totalFeatureSets;
    }

    /**
     * Build query parameters for the feature sets owned by this handler
     * If this handler covers all feature sets then leave as is
     *
     * @return Feature set query parameters
     * @deprecated use {@link #buildQueryParams2()}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected FeatureSetQueryParameters buildQueryParams() {
        return new FeatureSetQueryParameters();
    }

    protected FeatureDataStore2.FeatureSetQueryParameters buildQueryParams2() {
        return Adapters.adapt(buildQueryParams(),
                new FeatureDataStore2.FeatureSetQueryParameters());
    }

    @Override
    public boolean setVisibleImpl(boolean visible) {
        try {
            _dataStore2.setFeatureSetsVisible(buildQueryParams2(), visible);
            return true;
        } catch (DataStoreException e) {
            return false;
        }
    }

    @Override
    public int getVisibility() {
        if (!isConditionVisible())
            return INVISIBLE;
        try {
            FeatureDataStore2.FeatureSetQueryParameters params = buildQueryParams2();
            params.visibleOnly = true;
            int numVisible = _dataStore2.queryFeatureSetsCount(params);
            if (numVisible == 0)
                return INVISIBLE;
            else if (numVisible < getTotalFeatureSetsCount())
                return SEMI_VISIBLE;
        } catch (DataStoreException ignored) {
        }
        return VISIBLE;
    }

    @Override
    public boolean isVisible() {
        if (!isConditionVisible())
            return false;
        FeatureDataStore2.FeatureSetQueryParameters params = buildQueryParams2();
        params.visibleOnly = true;
        params.limit = 1;
        try {
            return _dataStore2.queryFeatureSetsCount(params) > 0;
        } catch (DataStoreException e) {
            return true;
        }
    }
}
