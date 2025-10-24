
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.GeoJSONSpatialDb;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.util.Enumeration;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportGeoJsonZResolver;

/**
 * Imports archived GeoJSON files
 *
 * @deprecated use {@link ImportGeoJsonZResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportGeoJsonZSort extends ImportResolver {

    private static final String TAG = "ImportGeoJsonZSort";

    public ImportGeoJsonZSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".zip", FileSystemUtils.OVERLAYS_DIRECTORY,
                "Zipped GeoJSON",
                context.getDrawable(R.drawable.ic_shapefile));
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

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(GeoJSONSpatialDb.GEOJSON_CONTENT_TYPE,
                GeoJSONSpatialDb.GEOJSON_FILE_MIME_TYPE);
    }
}
