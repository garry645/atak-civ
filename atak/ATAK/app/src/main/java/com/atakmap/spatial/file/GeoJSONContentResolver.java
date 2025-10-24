
package com.atakmap.spatial.file;

import com.atakmap.android.maps.MapView;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

public class GeoJSONContentResolver extends SpatialDbContentResolver {

    private static final Set<String> EXTS = new HashSet<>();
    static {
        EXTS.add("geojson");
        EXTS.add("zip");
    }

    /** @deprecated use {@link #GeoJSONContentResolver(MapView, FeatureDataStore2 } */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public GeoJSONContentResolver(MapView mv, DataSourceFeatureDataStore db) {
        super(mv, db, EXTS);
    }

    public GeoJSONContentResolver(MapView mv, FeatureDataStore2 db) {
        super(mv, db, EXTS);
    }

    @Override
    protected GeoJSONContentHandler createHandler(File file,
            List<Long> featureSetIds, Envelope bounds) {
        if (file instanceof ZipVirtualFile) {
            ZipVirtualFile zvf = (ZipVirtualFile) file;
            file = zvf.getZipFile();
        }
        return new GeoJSONContentHandler(_mapView, _datastore, file,
                featureSetIds, bounds);
    }
}
