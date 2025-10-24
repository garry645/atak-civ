
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.GeoJSONSpatialDb;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportGeoJsonResolver;

/**
 * Imports Shapefiles
 *
 * @deprecated use {@link ImportGeoJsonResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportGeoJsonSort extends ImportResolver {

    private static final String TAG = "ImportGeoJSONSort";

    private final static String GEOJSONMATCH = "FeatureCollection";

    public ImportGeoJsonSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".geojson", FileSystemUtils.OVERLAYS_DIRECTORY, "GeoJSON",
                context.getDrawable(R.drawable.ic_shapefile));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file)) {
            Log.d(TAG, "No match: " + file.getAbsolutePath());
            return false;
        }

        try (InputStream fis = IOProviderFactory.getInputStream(file)) {
            return isGeoJSON(fis);
        } catch (IOException e) {
            Log.e(TAG, "Error checking if geojson: " + file.getAbsolutePath(),
                    e);
        }

        return false;
    }

    private static boolean isGeoJSON(InputStream stream) {
        try {
            // read first few hundred bytes and search for known GeoJSON strings
            char[] buffer = new char[2048];
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));

            int numRead;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .geojson stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(GEOJSONMATCH);
            if (!match) {
                Log.d(TAG, "Failed to match geojson content");
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .geojson", e);
            return false;
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(GeoJSONSpatialDb.GEOJSON_CONTENT_TYPE,
                GeoJSONSpatialDb.GEOJSON_FILE_MIME_TYPE);
    }

}
