package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports Shapefiles
 */
public class ImportGeoJsonResolver extends ImportResolver {

    private static final String TAG = "ImportGeoJSONSort";

    private final static String GEOJSONMATCH = "FeatureCollection";

    public ImportGeoJsonResolver(String displayName, File destinationDir, Drawable icon) {
        super(".geojson", destinationDir, displayName, icon);
        contentType = "GeoJSON";
        mimeType = "application/octet-stream";
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
}
