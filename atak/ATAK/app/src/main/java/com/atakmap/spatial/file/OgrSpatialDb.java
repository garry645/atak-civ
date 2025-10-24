
package com.atakmap.spatial.file;

import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Support injesting ESRI Shapefiles
 */
public abstract class OgrSpatialDb extends SpatialDbContentSource {

    protected final String iconPath;

    /** @deprecated use {@link #OgrSpatialDb(FeatureDataStore2, String, String, String)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected OgrSpatialDb(DataSourceFeatureDataStore spatialDb,
            String groupName, String iconPath, String type) {
        super(spatialDb, groupName, type);

        this.iconPath = iconPath;
    }

    protected OgrSpatialDb(FeatureDataStore2 spatialDb,
            String groupName, String iconPath, String type) {
        super(spatialDb, groupName, type);

        this.iconPath = iconPath;
    }

    @Override
    public String getIconPath() {
        return this.iconPath;
    }

    @Override
    protected String getProviderHint(File file) {
        return "ogr";
    }
}
