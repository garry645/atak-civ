
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

public class GMLContentResolver extends SpatialDbContentResolver {

    private static final Set<String> EXTS = new HashSet<>();
    static {
        EXTS.add("gml");
        EXTS.add("zip");
    }

    /** @deprecated use {@link #GMLContentResolver(MapView, FeatureDataStore2 } */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public GMLContentResolver(MapView mv, DataSourceFeatureDataStore db) {
        super(mv, db, EXTS);
    }

    public GMLContentResolver(MapView mv, FeatureDataStore2 db) {
        super(mv, db, EXTS);
    }

    @Override
    protected GMLContentHandler createHandler(File file,
            List<Long> featureSetIds, Envelope bounds) {
        if (file instanceof ZipVirtualFile) {
            ZipVirtualFile zvf = (ZipVirtualFile) file;
            file = zvf.getZipFile();
        }
        return new GMLContentHandler(_mapView, _datastore, file,
                featureSetIds, bounds);
    }
}
