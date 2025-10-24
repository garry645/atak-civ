package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;

import java.io.File;

import gov.tak.api.commons.graphics.Drawable;

public class ImportMVTResolver extends ImportResolver {

    private static final String TAG = "ImportMVTSort";

    public ImportMVTResolver(String displayName, File destinationDir, Drawable icon) {
        super(".mvt", destinationDir, displayName, icon);
        contentType = "MVT";
        mimeType = "application/vnd.mapbox-vector-tile";
    }

    public ImportMVTResolver(String extension, File destinationDir, String displayName, Drawable icon) {
        super(extension, destinationDir, displayName, icon);
        contentType = "MVT";
        mimeType = "application/vnd.mapbox-vector-tile";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        FeatureDataSource.Content ds = null;
        try {
            ds = FeatureDataSourceContentFactory.parse(file, "MVT");
            return (ds != null);
        } catch (Throwable t) {
            return false;
        } finally {
            if (ds != null)
                ds.close();
        }
    }
}
