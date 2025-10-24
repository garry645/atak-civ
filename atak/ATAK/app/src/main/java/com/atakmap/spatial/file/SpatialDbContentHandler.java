
package com.atakmap.spatial.file;

import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore.FeatureSetQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Abstract handler for spatial feature databases with a specific
 * set of feature set IDs (KML, GPX, SHP)
 */
public abstract class SpatialDbContentHandler extends FeatureDbContentHandler {

    protected final HashSet<Long> _featureSetIds;

    /** @deprecated use {@link #SpatialDbContentHandler(MapView, FeatureDataStore2, File, List, Envelope)} */
    protected SpatialDbContentHandler(MapView mv, DataSourceFeatureDataStore db,
            File file, List<Long> featureSetIds, Envelope bounds) {
        super(mv, file, db, bounds);
        _featureSetIds = featureSetIds != null ? new HashSet<>(featureSetIds)
                : null;
    }

    protected SpatialDbContentHandler(MapView mv, FeatureDataStore2 db,
            File file, List<Long> featureSetIds, Envelope bounds) {
        super(mv, file, db, bounds);
        _featureSetIds = featureSetIds != null ? new HashSet<>(featureSetIds)
                : null;
    }

    @Override
    public int getTotalFeatureSetsCount() {
        return _featureSetIds.size();
    }

    @Override
    protected FeatureSetQueryParameters buildQueryParams() {
        FeatureSetQueryParameters params = super.buildQueryParams();
        params.ids = _featureSetIds;
        return params;
    }

    @Override
    protected FeatureDataStore2.FeatureSetQueryParameters buildQueryParams2() {
        FeatureDataStore2.FeatureSetQueryParameters params = super.buildQueryParams2();
        params.ids = _featureSetIds;
        return params;
    }

    /**
     * Get a list of all feature set IDs attached to this file
     *
     * @return List of feature set IDs
     */
    public List<Long> getFeatureSetIds() {
        return new ArrayList<>(_featureSetIds);
    }
}
