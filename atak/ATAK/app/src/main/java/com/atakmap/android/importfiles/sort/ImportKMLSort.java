
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.KmlFileSpatialDb;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Imports KML Files
 *
 * @deprecated replaced by {@link gov.tak.api.importfiles.ImportKMLResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportKMLSort extends ImportResolver {

    private static final String TAG = "ImportKMLSort";

    private final static String KMLMATCH = "<kml";
    private final static String KMLMATCH_REGEX_WITHNS = "(?s).*<[^>]+:kml.*";

    public ImportKMLSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".kml", FileSystemUtils.OVERLAYS_DIRECTORY,
                context.getString(R.string.kml_file),
                context.getDrawable(R.drawable.ic_kml));

    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .kml, now lets see if it contains reasonable xml
        try (FileInputStream fis = IOProviderFactory.getInputStream(file)) {
            return isKml(fis);
        } catch (IOException e) {
            Log.e(TAG, "Error checking if KML: " + file.getAbsolutePath(), e);
        }

        return false;
    }

    /**
     * Given an input stream, determine if the input stream contains
     * a KML file determined via magic number
     * @param stream the input stream which will be closed when isKml
     *               exits.
     * @return true if the file is identified to be a KML file
     */
    public static boolean isKml(InputStream stream) {
        try {
            // read first few hundred bytes and search for known KML strings
            char[] buffer = new char[2048];
            BufferedReader reader = null;
            int numRead = -1;

            try {
                reader = new BufferedReader(new InputStreamReader(
                    stream));
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .kml stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(KMLMATCH)
                    || content.matches(KMLMATCH_REGEX_WITHNS);
            if (!match) {
                Log.d(TAG, "Failed to match kml content");
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .kml", e);
            return false;
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(KmlFileSpatialDb.KML_CONTENT_TYPE,
                KmlFileSpatialDb.KML_FILE_MIME_TYPE);
    }
}
