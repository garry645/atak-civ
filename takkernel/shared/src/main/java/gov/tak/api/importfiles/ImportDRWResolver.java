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
 * Imports DRW files (local points from Falcon View) MS-Access based
 */
public class ImportDRWResolver extends ImportResolver {

    private static final String TAG = "ImportDRWSort";

    public ImportDRWResolver(String displayName, File destinationDir, Drawable icon) {
        super(".drw", destinationDir, displayName, icon);
        contentType = "DRW";
        mimeType = "application/x-msaccess";
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
}
