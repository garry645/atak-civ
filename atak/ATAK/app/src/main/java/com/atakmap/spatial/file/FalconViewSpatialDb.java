
package com.atakmap.spatial.file;

import android.util.SparseArray;

import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.map.formats.msaccess.MsAccessDatabaseFactory;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Spatial database interface for LPT/DRW files
 */
public class FalconViewSpatialDb extends SpatialDbContentSource {

    private static final String TAG = "FalconViewSpatialDb";

    public static final String LPT = "LPT", DRW = "DRW";
    public static final String MIME_TYPE = "application/x-msaccess";

    /** @deprecated use {@link #FalconViewSpatialDb(FeatureDataStore2, String)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public FalconViewSpatialDb(DataSourceFeatureDataStore database,
            String ext) {
        super(database, ext.toUpperCase(LocaleUtil.getCurrent()),
                ext.toLowerCase(LocaleUtil.getCurrent()));
    }

    public FalconViewSpatialDb(FeatureDataStore2 database,
            String ext) {
        super(database, ext.toUpperCase(LocaleUtil.getCurrent()),
                ext.toLowerCase(LocaleUtil.getCurrent()));
    }

    @Override
    public String getFileDirectoryName() {
        return FileSystemUtils.OVERLAYS_DIRECTORY;
    }

    @Override
    public String getFileMimeType() {
        return MIME_TYPE;
    }

    @Override
    public String getIconPath() {
        return ATAKUtilities.getResourceUri(getIconId());
    }

    @Override
    public int getIconId() {
        return getGroupName().equals(LPT) ? R.drawable.ic_falconview_lpt
                : R.drawable.ic_falconview_drw;
    }

    @Override
    protected String getProviderHint(File file) {
        return "falconview";
    }

    @Override
    public String getContentType() {
        return getGroupName();
    }

    @Override
    public int processAccept(File file, int depth) {
        // Only accept files that end in either .lpt or .drw
        if (IOProviderFactory.isFile(file) && IOProviderFactory.canRead(file)) {
            if (FileSystemUtils.checkExtension(file, getType()))
                return PROCESS_ACCEPT;
        } else if (IOProviderFactory.isDirectory(file)) {
            return PROCESS_RECURSE;
        }

        return PROCESS_REJECT;
    }

    /**
     * Deprecated - former internal implementation detail. Use MsAccessDatabaseFactory.createDatabase(file)
     */
    @DeprecatedApi(since = "5.4", forRemoval = true)
    public DatabaseIface createDatabase(File file) {
        return MsAccessDatabaseFactory.createDatabase(file);
    }

}
