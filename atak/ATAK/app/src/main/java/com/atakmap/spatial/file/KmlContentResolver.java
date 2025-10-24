
package com.atakmap.spatial.file;

import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

public class KmlContentResolver extends SpatialDbContentResolver {

    private static final Set<String> EXTS = new HashSet<>();
    static {
        EXTS.add(KmlFileSpatialDb.KML_TYPE);
        EXTS.add(KmlFileSpatialDb.KMZ_TYPE);
    }

    /** @deprecated use {@link #KmlContentResolver(MapView, FeatureDataStore2} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public KmlContentResolver(MapView mv, DataSourceFeatureDataStore db) {
        super(mv, db, EXTS);
    }

    public KmlContentResolver(MapView mv, FeatureDataStore2 db) {
        super(mv, db, EXTS);
    }

    @Override
    protected KmlContentHandler createHandler(File file,
            List<Long> featureSetIds, Envelope bounds) {
        return new KmlContentHandler(_mapView, _datastore, file,
                featureSetIds, bounds);
    }
}
