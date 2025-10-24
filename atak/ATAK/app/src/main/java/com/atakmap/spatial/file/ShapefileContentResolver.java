
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

public class ShapefileContentResolver extends SpatialDbContentResolver {

    private static final Set<String> EXTS = new HashSet<>();
    static {
        EXTS.add("shp");
        EXTS.add("shpz");
        EXTS.add("zip");
    }

    /** @deprecated use {@link #ShapefileContentResolver(MapView, FeatureDataStore2 } */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public ShapefileContentResolver(MapView mv, DataSourceFeatureDataStore db) {
        super(mv, db, EXTS);
    }

    public ShapefileContentResolver(MapView mv, FeatureDataStore2 db) {
        super(mv, db, EXTS);
    }

    @Override
    public void addHandler(File f) {
        // The underlying zip file is required to query feature sets properly
        if (f instanceof ZipVirtualFile)
            f = ((ZipVirtualFile) f).getZipFile();
        super.addHandler(f);
    }

    @Override
    protected ShapefileContentHandler createHandler(File file,
            List<Long> featureSetIds, Envelope bounds) {
        if (file instanceof ZipVirtualFile) {
            ZipVirtualFile zvf = (ZipVirtualFile) file;
            file = zvf.getZipFile();
        }
        return new ShapefileContentHandler(_mapView, _datastore, file,
                featureSetIds, bounds);
    }
}
