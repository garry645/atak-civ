
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
import gov.tak.api.importfiles.ImportLPTResolver;

/**
 * Imports LPT files (local points from Falcon View) MS-Access based
 *
 * @deprecated use {@link ImportLPTResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportLPTSort extends ImportResolver {

    private static final String TAG = "ImportLPTSort";

    public ImportLPTSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".lpt", FileSystemUtils.OVERLAYS_DIRECTORY,
                context.getString(R.string.lpt_file),
                context.getDrawable(R.drawable.ic_falconview_lpt));
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;

        // it is a .lpt, now lets see if it has LPT points
        return HasPoints(file);
    }

    /**
     * Search for a MS Access table "Points"
     * 
     * @param file the file
     * @return true if the file has lpt points
     */
    private static boolean HasPoints(final File file) {

        if (file == null) {
            Log.e(TAG, "LPT file was null.");
            return false;
        }

        if (!IOProviderFactory.exists(file)) {
            Log.e(TAG, "LPT does not exist: " + file.getAbsolutePath());
            return false;
        }

        DatabaseIface msaccessDb = null;
        CursorIface cursor = null;
        try {
            msaccessDb = MsAccessDatabaseFactory.createDatabase(file);
            if (msaccessDb == null)
                return false;
            cursor = msaccessDb.query("select * from Points", null);
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
        return new Pair<>(FalconViewSpatialDb.LPT,
                FalconViewSpatialDb.MIME_TYPE);
    }
}
