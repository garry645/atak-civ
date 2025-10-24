package gov.tak.api.importfiles;

import android.util.Pair;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.util.Enumeration;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports archived GeoJSON files
 */
public class ImportGeoJsonZResolver extends ImportResolver {

    private static final String TAG = "ImportGeoJsonZSort";

    public ImportGeoJsonZResolver(String displayName, File destinationDir, Drawable icon) {
        super(".zip", destinationDir, displayName, icon);
        contentType = "GEOJSON_CONTENT_TYPE";
        mimeType = "application/octet-stream";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it contains a .geojson
        return HasGeoJSON(file);
    }

    /**
     *
     * @param file the file
     * @return true if it contains a geojson file
     */
    public static boolean HasGeoJSON(File file) {
        if (file == null) {
            Log.d(TAG, "ZIP file points to null.");
            return false;
        }

        if (!IOProviderFactory.exists(file)) {
            Log.d(TAG, "ZIP does not exist: " + file.getAbsolutePath());
            return false;
        }

        ZipFile zip = null;
        try {
            zip = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(".geojson")) {
                    Log.d(TAG, "Matched archived GeoJSONfile: "
                            + file.getAbsolutePath());
                    return true;
                }
            }

        } catch (Exception e) {
            Log.d(TAG,
                    "Failed to find GeoJSON content in: "
                            + file.getAbsolutePath(),
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed closing archived GeoJSONfile: "
                                    + file.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }
}
