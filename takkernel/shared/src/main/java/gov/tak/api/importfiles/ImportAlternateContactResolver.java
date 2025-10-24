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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Matches contact info in a CSV, based on configured callsign
 */
public class ImportAlternateContactResolver extends ImportResolver {

    private static final String TAG = "ImportAlternateContactResolver";

    private final static String CONTACT_MATCH = "::ALTERNATE CONTACT v2";
    private static final String COMMENT = "::";
    private static final String SPLIT = ",";

    final ThreadLocal<char[]> _charBuffer = new ThreadLocal<char[]>() {
        protected char[] initialValue() {
            return new char[FileSystemUtils.CHARBUFFERSIZE];
        }
    };

    public ImportAlternateContactResolver(String displayName, File destinationDir, Drawable icon) {
        super(".csv", destinationDir, displayName, icon);
        contentType = "Contact Info";
        mimeType = "text/csv";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        try (InputStream is = IOProviderFactory.getInputStream(file)) {
            return isContact(is, _charBuffer.get());
        } catch (IOException e) {
            Log.e(TAG,
                    "Error checking contact info: " + file.getAbsolutePath(),
                    e);
        }

        return false;
    }

    private static boolean isContact(InputStream stream, char[] buffer)
            throws IOException {

        BufferedReader reader = null;
        try {
            // read first few hundred bytes and search for known strings
            reader = new BufferedReader(new InputStreamReader(
                    stream));
            int numRead = reader.read(buffer);
            if (numRead < 1) {
                Log.d(TAG, "Failed to read .csv stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            return isContact(content);
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .csv", e);
            return false;
        } finally {
            IoUtils.close(reader);
        }
    }

    private static boolean isContact(String content) {
        if (FileSystemUtils.isEmpty(content)) {
            Log.w(TAG, "Unable to match empty content");
            return false;
        }

        boolean match = content.contains(CONTACT_MATCH);
        if (!match) {
            Log.d(TAG, "Failed to match content from .csv: ");
        }

        return match;
    }

    /**
     * Parse CSV to find contact info
     *
     * @param file the file to import with the contact information
     * @return true if the import was successful
     */
    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        notifyBeginImportListeners(file, flags, null);
        this.onFileSorted(file, file, flags);
        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        return file;
    }
}
