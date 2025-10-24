package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Dispatches CoT Events, rather than sorts to a directory
 */
public class ImportCotResolver extends ImportResolver {

    private static final String TAG = "ImportCotSort";

    private static final int PROBE_SIZE = 384;

    private final static String COTMATCH = "<event";
    private final static String COTMATCH2 = "<point";

    private final ThreadLocal<char[]> _charBuffer = new ThreadLocal<char[]>() {
        protected char[] initialValue() {
            return new char[PROBE_SIZE];
        }
    };

    public ImportCotResolver(String displayName, File destinationDir, Drawable icon) {
        super(".cot", destinationDir, displayName, icon);
        contentType = "CoT Event";
        mimeType = "application/cot+xml";
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;

        // it is a .cot, now lets see if it contains reasonable CoT
        try (FileInputStream fis = IOProviderFactory.getInputStream(file)) {
            return isCoT(fis, _charBuffer.get());
        } catch (IOException e) {
            Log.e(TAG, "Error checking if CoT: " + file.getAbsolutePath(), e);
        }

        return false;
    }

    private static boolean isCoT(final InputStream stream,
                                 final char[] buffer) {
        try {
            // read first few hundred bytes and search for known CoT strings
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));
            int numRead = -1;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .cot stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            return isCoT(content);
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .cot", e);
            return false;
        }
    }

    public static boolean isCoT(String content) {
        if (FileSystemUtils.isEmpty(content)) {
            Log.w(TAG, "Unable to match empty content");
            return false;
        }

        boolean match = content.contains(COTMATCH)
                && content.contains(COTMATCH2);
        if (!match) {
            Log.d(TAG, "Failed to match content from .cot: ");
        }

        return match;
    }

    /**
     * @param file the file to import
     * @return {@code true} if the import was successful
     */
    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        String event;
        try (FileInputStream fis = IOProviderFactory.getInputStream(file)) {
            event = FileSystemUtils.copyStreamToString(fis, true,
                    FileSystemUtils.UTF8_CHARSET, _charBuffer.get());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load CoT Event: " + file.getAbsolutePath(),
                    e);
            return false;
        }

        if (FileSystemUtils.isEmpty(event)) {
            Log.e(TAG, "Failed to load CoT Event: " + file.getAbsolutePath());
            return false;
        }

        return notifyBeginImportListeners(file, flags, event);
    }

    @Override
    public File getDestinationPath(File file) {
        return file;
    }
}
