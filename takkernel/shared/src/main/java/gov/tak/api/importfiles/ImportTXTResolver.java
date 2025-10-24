package gov.tak.api.importfiles;

import android.util.Pair;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.commons.graphics.Drawable;

/**
 * Sorts TXT & XML Files. Allows for augmentation by adding calls to addSignature.
 */
final public class ImportTXTResolver extends ImportResolver {

    private static final String TAG = "ImportTXTSort";

    public static final String CONTENT_TYPE = "TXT or XML File";

    public static class TxtType {

        public interface AfterAction {
            void doAction(File dst);
        }

        public String signature;
        public String folder;
        public AfterAction action;

        public TxtType(final String signature, final String folder,
                       final AfterAction action) {
            this.signature = signature;
            this.folder = folder;
            this.action = action;
        }

        @NonNull
        public String toString() {
            return signature + ": " + folder;
        }
    }

    /**
     * Enumeration of TXT & XML files including matching string and storage location
     */
    private static final List<TxtType> types = new ArrayList<>();

    /**
     * Adds a signature to the TXT and XML importer that comprises a signature, directory and an
     * action to be called if import succeeds.
     * @param signature the signature to look for within the file to match.
     * @param directory the directory to copy the file to if the signature was found.
     * @param action the action to execute after the file has been copied.
     */
    public static void addSignature(String signature, String directory,
                                    TxtType.AfterAction action) {
        types.add(new TxtType(signature, directory, action));
    }

    public ImportTXTResolver(String ext, File destinationDir, String displayName, Drawable icon) {
        super(ext, destinationDir, displayName, icon);
        mimeType = "application/xml";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .xml or .txt, now lets see if content inspection passes
        TxtType t = null;
        try (InputStream fis = IOProviderFactory.getInputStream(file)) {
            t = getType(fis);
        } catch (IOException e) {
            Log.e(TAG, "Failed to match TXT file: " + file.getAbsolutePath(),
                    e);
        }

        return t != null;
    }

    public static TxtType getType(InputStream stream) {
        try {
            // read first few hundred bytes and search for known strings
            char[] buffer = new char[1024];
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));
            int numRead = -1;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read txt stream");
                return null;
            }

            String content = String.valueOf(buffer, 0, numRead);
            for (TxtType t : types) {
                if (content.contains(t.signature)) {
                    Log.d(TAG, "Match TXT content: " + t);
                    return t;
                }
            }

            Log.d(TAG, "Failed to match TXT content");
            return null;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match txt", e);
            return null;
        }
    }

    /**
     * Defers to TxtType for the relative path. Returned file will have this Resolver's extension
     */
    @Override
    public File getDestinationPath(File file) {

        TxtType t = null;
        try (InputStream is = IOProviderFactory.getInputStream(file)) {
            t = getType(is);
        } catch (IOException e) {
            Log.e(TAG, "Failed to match TXT file: " + file.getAbsolutePath(),
                    e);
        }

        if (t == null) {
            Log.e(TAG, "Failed to match TXT file: " + file.getAbsolutePath());
            return null;
        }

        String fileName = file.getName();
        if (!FileSystemUtils.isEmpty(getExt())
                && !fileName.endsWith(getExt())) {
            Log.d(TAG, "Added extension to destination path: " + fileName);
            fileName += getExt();
        }

        return destinationDir != null ? new File(destinationDir, fileName) : new File("", fileName);
    }

    @Override
    protected void onFileSorted(File src, File dst, EnumSet<SortFlags> flags) {
        super.onFileSorted(src, dst, flags);

        //special case import actions
        TxtType t;
        try (InputStream fis = IOProviderFactory.getInputStream(dst)) {
            t = getType(fis);
            if (t != null && t.action != null)
                t.action.doAction(dst);
        } catch (IOException e) {
            Log.w(TAG,
                    "onFileSorted Failed to match TXT file: "
                            + dst.getAbsolutePath(),
                    e);
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(CONTENT_TYPE, mimeType);
    }
}
