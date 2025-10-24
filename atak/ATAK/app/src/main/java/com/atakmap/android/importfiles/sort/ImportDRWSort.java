
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.map.formats.msaccess.MsAccessDatabaseFactory;
import com.atakmap.spatial.file.FalconViewSpatialDb;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportDRWResolver;

/**
 * Imports DRW files (local points from Falcon View) MS-Access based
 *
 * @deprecated use {@link ImportDRWResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportDRWSort extends ImportResolver {

    private static final String TAG = "ImportDRWSort";

    public ImportDRWSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".drw", FileSystemUtils.OVERLAYS_DIRECTORY,
                context.getString(R.string.drw_file),
                context.getDrawable(R.drawable.ic_falconview_drw));
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;

        // it is a .drw, now lets see if it has DRW shapes
        return hasDrawing(file);
    }

    /**
     * Search for a MS Access table "Points"
     * 
     * @param file the file
     * @return true if the file contains a drw file
     */
    private static boolean hasDrawing(final File file) {

        if (file == null || !IOProviderFactory.exists(file)) {
            Log.e(TAG,
                    "DRW does not exist: "
                            + (file == null ? "null" : file.getAbsolutePath()));
            return false;
        }

        DatabaseIface msaccessDb = null;
        CursorIface cursor = null;
        try {
            msaccessDb = MsAccessDatabaseFactory.createDatabase(file);
            if (msaccessDb == null)
                return false;
            cursor = msaccessDb.query("select * from Main", null);
            return cursor.moveToNext();
        } finally {
            if (cursor != null)
                cursor.close();
            if (msaccessDb != null)
                msaccessDb.close();
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(FalconViewSpatialDb.DRW,
                FalconViewSpatialDb.MIME_TYPE);
    }
}
