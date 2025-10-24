package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.map.formats.msaccess.MsAccessDatabaseFactory;

import java.io.File;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports LPT files (local points from Falcon View) MS-Access based
 */
public class ImportLPTResolver extends ImportResolver {

    private static final String TAG = "ImportLPTSort";

    public ImportLPTResolver(String displayName, File destinationDir, Drawable icon) {
        super(".lpt", destinationDir, displayName, icon);
        contentType = "LPT";
        mimeType = "application/x-msaccess";
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
}
