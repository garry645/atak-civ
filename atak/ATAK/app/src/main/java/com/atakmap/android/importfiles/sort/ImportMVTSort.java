
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.spatial.file.MvtSpatialDb;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportMVTResolver;

/**
 * @deprecated use {@link ImportMVTResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportMVTSort extends ImportResolver {

    private static final String TAG = "ImportMVTSort";

    public ImportMVTSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".mvt", FileSystemUtils.OVERLAYS_DIRECTORY,
                context.getString(R.string.mvt_file),
                context.getDrawable(R.drawable.ic_mvt));
    }

    public ImportMVTSort(Context context, String extension, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(extension, FileSystemUtils.OVERLAYS_DIRECTORY,
                context.getString(R.string.mvt_file),
                context.getDrawable(R.drawable.ic_mvt));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        FeatureDataSource.Content ds = null;
        try {
            ds = FeatureDataSourceContentFactory.parse(file,
                    MvtSpatialDb.MVT_TYPE);
            return (ds != null);
        } catch (Throwable t) {
            return false;
        } finally {
            if (ds != null)
                ds.close();
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(MvtSpatialDb.MVT_CONTENT_TYPE,
                MvtSpatialDb.MVT_FILE_MIME_TYPE);
    }
}
