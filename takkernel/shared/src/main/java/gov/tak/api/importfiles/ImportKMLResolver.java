package gov.tak.api.importfiles;

import android.util.Pair;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports KML Files
 */
public class ImportKMLResolver extends ImportResolver {

    private static final String TAG = "ImportKMLSort";

    private final static String KMLMATCH = "<kml";
    private final static String KMLMATCH_REGEX_WITHNS = "(?s).*<[^>]+:kml.*";

    public ImportKMLResolver(String displayName, File destinationDir, Drawable icon) {
        super(".kml", destinationDir, displayName, icon);
        contentType = "KML";
        mimeType = "application/vnd.google-earth.kml+xml";
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

    static boolean isKml(InputStream stream) {
        try {
            // read first few hundred bytes and search for known KML strings
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
}
