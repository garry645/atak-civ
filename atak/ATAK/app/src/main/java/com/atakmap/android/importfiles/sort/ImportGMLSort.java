
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.GMLSpatialDb;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportGMLResolver;

/**
 * Imports Shapefiles
 *
 * @deprecated use {@link ImportGMLResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportGMLSort extends ImportResolver {

    private static final String TAG = "ImportGMLSort";

    private final static String GMLMATCH = "<gml";

    public ImportGMLSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".gml", FileSystemUtils.OVERLAYS_DIRECTORY, "GML",
                context.getDrawable(R.drawable.ic_shapefile));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file)) {
            Log.d(TAG, "No match: " + file.getAbsolutePath());
            return false;
        }

        try (InputStream fis = IOProviderFactory.getInputStream(file)) {
            return isGML(fis);
        } catch (IOException e) {
            Log.e(TAG, "Error checking if GPX: " + file.getAbsolutePath(), e);
        }

        return false;
    }

    private static boolean isGML(InputStream stream) {
        try {
            // read first few hundred bytes and search for known GML strings
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
                Log.d(TAG, "Failed to read .gml stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(GMLMATCH);
            if (!match) {
                Log.d(TAG, "Failed to match gml content");
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .gml", e);
            return false;
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(GMLSpatialDb.GML_CONTENT_TYPE,
                GMLSpatialDb.GML_FILE_MIME_TYPE);
    }

}
