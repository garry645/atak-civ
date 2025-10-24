
package com.atakmap.spatial.file;

import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;
import java.util.Collections;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

public class GpxContentResolver extends SpatialDbContentResolver {

    /** @deprecated use {@link #GpxContentResolver(MapView, FeatureDataStore2 } */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public GpxContentResolver(MapView mv, DataSourceFeatureDataStore db) {
        super(mv, db, Collections.singleton("gpx"));
    }

    public GpxContentResolver(MapView mv, FeatureDataStore2 db) {
        super(mv, db, Collections.singleton("gpx"));
    }

    @Override
    protected GpxContentHandler createHandler(File file,
            List<Long> featureSetIds, Envelope bounds) {
        return new GpxContentHandler(_mapView, _datastore, file,
                featureSetIds, bounds);
    }
}
