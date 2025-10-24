package gov.tak.api.importfiles;

import android.net.Uri;
import android.util.Pair;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.commons.graphics.Drawable;

/**
 * Matches a file based on filename, content, or other properties. Also imports or
 * initiates import
 */
public abstract class ImportResolver {
    /**
     * Enum of flags to be used in an enum set that wll be passed into the sorters.
     * Note that if IMPORT_INPLACE, IMPORT_MOVE, or IMPORT_COPY are specified in the
     * same set, IMPORT_COPY takes precedence followed by IMPORT_MOVE followed by
     * IMPORT_INPLACE
     */
    public enum SortFlags {
        /**
         * Show notifications related to the import process.
         */
        SHOW_NOTIFICATIONS,
        /**
         * Zoom to the file when it's finished importing.
         */
        ZOOM_TO_FILE,
        /**
         * Turn file visibility off by default
         */
        HIDE_FILE,
        /**
         * leave the file in place.
         */
        IMPORT_INPLACE,
        /**
         * Signals that it is desirable to move the file into the appropriate location when
         * importing, will fall back to copying if move is not successful
         */
        IMPORT_MOVE,
        /**
         * Signals that it is desirable to copy the file into the appropriate location when
         * importing
         */
        IMPORT_COPY
    }

    private static final String TAG = "ImportResolver";

    private final Set<MatchListener> _matchListeners;
    private final Set<FileSortedListener> _fileSortedListeners;
    private final Set<ImportListener> _importListeners;
    private final Set<BeginImportListener> _beginImportListeners;
    private final String displayName;
    private final gov.tak.api.commons.graphics.Drawable icon;

    protected final String ext;

    protected final File destinationDir;

    protected final ThreadLocal<Boolean> fileSorted = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    protected String contentType = "";
    protected String mimeType = "";

    public interface MatchListener {
        /**
         * @return true on success
         */
        boolean onMatch(File file);
    }

    /**
     * Listener fired at the beginning of a non-overridden
     * {@link ImportResolver#onFileSorted(File, File, EnumSet)} call.
     *
     * @see ImportResolver#addFileSortedListener(FileSortedListener)
     */
    public interface FileSortedListener {
        /**
         * @return true on success
         */
        void onFileSorted(FileSortedNotification notification);
    }

    /**
     * Only fired by some {@link ImportResolver} implementations; refer to their documentation.
     */
    public interface BeginImportListener {
        /**
         * @return {@code true} on success
         */
        boolean onBeginImport(File file, EnumSet<SortFlags> sortFlags, Object opaque);
    }

    /**
     * Listener fired when an import finishes successfully.
     */
    public interface ImportListener {
        void onFileImported(File src, File dst, EnumSet<ImportResolver.SortFlags> sortFlags);
    }

    /**
     * Given an extension and a folder name, construct a resolver that will be responsible for
     * importing a file
     * @param ext that would be considered valid or null if there is no extension required
     * @param destinationDir the destination directory to be used during importing
     */
    public ImportResolver(@Nullable String ext, @Nullable File destinationDir,
                          @NonNull
                          final String displayName,
                          @NonNull
                          final gov.tak.api.commons.graphics.Drawable icon) {
        this.ext = ext;
        this.destinationDir = destinationDir;
        this.displayName = !FileSystemUtils.isEmpty(displayName) ? displayName : getClass().getSimpleName();
        this.icon = icon;
        _matchListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        _fileSortedListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        _importListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        _beginImportListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    /**
     * Returns the extension currently supported by this resolver.
     * @return if null or empty then, this resolver does not define an extension.
     */
    public String getExt() {
        return ext;
    }

    /**
     * Returns if the file is sorted or not.
     * @return {@code true} if the file is sorted. Note: this value only applies for the last
     * {@link #beginImport(File, EnumSet)} on the same thread used to call this ({@link #getFileSorted()}).
     */
    public boolean getFileSorted() {
        return fileSorted.get();
    }

    /**
     * Return true if this sort matches the specified file
     *
     * @param file the file to read to see if the import resolver supports the file.
     * @return true if the import resolver is capable of handling the file.
     */
    public boolean match(final File file) {
        return !FileSystemUtils.isEmpty(ext) && file != null &&
                file.getName().toLowerCase(LocaleUtil.getCurrent()).endsWith(ext);
    }

    /**
     * Provides for a capability by which a set of resolvers can be trimmed by a found sorter
     * implementation.    This provides great power to the sorter to force itself to be used over
     * a group of other resolvers.    Plugin implementers should take great caution in using this
     * only when it is guaranteed to be the correct behavior.
     *
     * @param importResolvers the list of import resolvers to filter. The modification are made
     *                        directly to the importResolvers list.
     * @param file the file that is being considered for the resolvers.
     */
    public void filterFoundResolvers(final List<ImportResolver> importResolvers,
                                     File file) {
        // no default filtering occurs
    }

    /**
     * Initiate import of the specified file
     *
     * @param file File to import
     * @param sortFlags Enum Set of flags that should be used to modify import behavior.
     * @return True if the import is successful, false otherwise.
     */
    public boolean beginImport(File file, EnumSet<SortFlags> sortFlags) {
        fileSorted.set(false);

        File dest = getDestinationPath(file);
        if (dest == null) {
            Log.w(TAG,
                    "failed to find destination path for: "
                            + file.getAbsolutePath());
            return false;
        }

        File destParent = dest.getParentFile();
        if (destParent == null) {
            Log.w(TAG,
                    "destination has no parent file: "
                            + dest.getAbsolutePath());
            return false;
        }

        if (!IOProviderFactory.exists(destParent)) {
            if (!IOProviderFactory.mkdirs(destParent)) {
                Log.w(TAG,
                        "failed to create directory: "
                                + destParent.getAbsolutePath());
                return false;
            } else {
                Log.d(TAG,
                        "created directory: " + destParent.getAbsolutePath());
            }
        }

        final boolean isDirectory = IOProviderFactory.isDirectory(file);

        if (!isDirectory && sortFlags.contains(SortFlags.IMPORT_COPY)) {
            Log.d(TAG, "attempting to copy: " + file + " to " + dest);
            try {
                if (!file.getAbsolutePath().equals(dest.getAbsolutePath())) {
                    FileSystemUtils.copyFile(file, dest);
                }
                onFileSorted(file, dest, sortFlags);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "failed to copy file: " + dest.getAbsolutePath(), e);
                return false;
            }
        } else if (!isDirectory && sortFlags.contains(SortFlags.IMPORT_MOVE)) {
            Log.d(TAG, "attempting to move: " + file + " to " + dest);
            if (!file.getAbsolutePath().equals(dest.getAbsolutePath())) {
                final boolean retval = FileSystemUtils.renameTo(file, dest);
                if (!retval) {
                    try {
                        Log.d(TAG, "move failed, attempting to copy: " + file
                                + " to " + dest);
                        FileSystemUtils.copyFile(file, dest);
                    } catch (IOException e) {
                        Log.e(TAG, "failed to copy file: "
                                + dest.getAbsolutePath(), e);
                        return false;
                    }
                }
            }
            if (IOProviderFactory.exists(dest)) {
                try {
                    onFileSorted(file, dest, sortFlags);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG,
                            "very bad error with the file: " + file
                                    + " sorted to: " + dest +
                                    " with flags: "
                                    + Arrays.toString(sortFlags.toArray())
                                    + " with: " + getClass(),
                            e);
                    return false;
                }
            } else {
                return false;
            }
        } else {
            // in place
            try {
                Log.d(TAG,
                        "attempting to use in place: " + file + " to " + dest);
                onFileSorted(file, file, sortFlags);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "very bad error with the file: " + file
                        + " sorted to: " + dest +
                        " with flags: " + Arrays.toString(sortFlags.toArray())
                        + " with: " + getClass(), e);
                return false;
            }
        }
    }

    /**
     * Perform any resolver specific cleanup
     */
    public void finalizeImport() {
    }

    /**
     * Returns the destination path if the file were to be moved or copied otherwise would be
     * unused if the file is left in place
     * @param file the source file
     * @return the destination file with the correct destination directory.
     */
    public File getDestinationPath(File file) {
        // set if file extension, if applicable
        String fileName = file.getName();
        if (!FileSystemUtils.isEmpty(getExt())
                && !fileName.endsWith(getExt())) {
            Log.d(TAG, "Added extension to destination path: " + fileName);
            fileName += getExt();
        }

        return new File(destinationDir, fileName);
    }

    /**
     * Invoked after the file has been successfully copied or moved to
     * initiate import.
     *
     * <P>
     * The default implementation returns immediately.
     *
     * @param src The original file
     * @param dst The file resulting from the copy or move.
     * @param sortedFlags Enum set of flags that the user wishes to use to modify
     * sort behavior.
     */
    protected void onFileSorted(final File src, final File dst,
                                EnumSet<SortFlags> sortedFlags) {
        fileSorted.set(true);

        // from the legacy InPlaceResolver and should not impact other resolvers
        final Pair<String, String> contentMIME = getContentMIME();
        if (contentMIME != null)
            notifyFileSortedListeners(src, dst, sortedFlags);

        notifyImportListeners(src, dst, sortedFlags);
    }

    /**
     * @return {@code false} if any listeners fail
     */
    protected boolean notifyMatchListeners(File file) {
        boolean success = true;
        for (ImportResolver.MatchListener l : getMatchListeners()) {
            success &= l.onMatch(file);
        }
        return success;
    }

    /**
     * @return {@code false} if any listeners fail
     */
    protected boolean notifyBeginImportListeners(File file, EnumSet<SortFlags> sortedFlags, Object opaque) {
        boolean success = true;
        for (BeginImportListener l : getBeginImportListeners()) {
            success &= l.onBeginImport(file, sortedFlags, opaque);
        }
        return success;
    }

    protected void notifyFileSortedListeners(File src, File dst, EnumSet<SortFlags> sortedFlags) {
        Log.d(TAG,
                "begin onFileSorted " + getClass()
                        + " to process: " + dst.getAbsolutePath()
                        + " contentMime: " + getContentMIME());
        FileSortedNotification notification = new FileSortedNotification(getContentMIME().first,
                getContentMIME().second, Uri.fromFile(src).toString(), Uri.fromFile(dst).toString(), sortedFlags);
        for (FileSortedListener l : getFileSortedListeners()) {
            l.onFileSorted(notification);
        }
    }

    protected void notifyImportListeners(File src, File dst, EnumSet<SortFlags> sortedFlags) {
        for (ImportListener l : getImportListeners()) {
            l.onFileImported(src, dst, sortedFlags);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Sorting %s, to %s", ext, destinationDir);
    }

    /**
     * Are directories supported by {@link #match(File)} and {@link #beginImport(File, EnumSet)}?
     * @return True if directories can be sorted by this class, false otherwise.
     */
    public boolean directoriesSupported() {
        return false;
    }

    /**
     * Get a human readable string that can be displayed to the user so that
     * they can differentiate between different sorter implementations.
     * @return Unique (by class, not instance), human readable name for this sorter.
     */
    public String getDisplayableName() {
        return displayName;
    }

    public Drawable getIcon() {
        return icon;
    }

    /**
     * Get the content and MIME type
     * @return Pair containing the content type and MIME type respectively
     */
    public Pair<String, String> getContentMIME() {
        return new Pair<>(contentType, mimeType);
    }

    public static final class FileSortedNotification {
        private final String contentType;
        private final String mimeType;
        private final String srcUri;
        private final String dstUri;
        private final EnumSet<ImportResolver.SortFlags> sortFlags;

        public FileSortedNotification(String contentType, String mimeType, String srcUri,
                                      String dstUri, EnumSet<ImportResolver.SortFlags> sortFlags) {
            this.contentType = contentType;
            this.mimeType = mimeType;
            this.srcUri = srcUri;
            this.dstUri = dstUri;
            this.sortFlags = sortFlags;
        }

        public String getContentType() {
            return contentType;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getSrcUri() {
            return srcUri;
        }

        public String getDstUri() {
            return dstUri;
        }

        public EnumSet<ImportResolver.SortFlags> getSortFlags() {
            return sortFlags;
        }
    }

    public void addMatchListener(MatchListener listener) {
        _matchListeners.add(listener);
    }

    public void addFileSortedListener(FileSortedListener listener) {
        _fileSortedListeners.add(listener);
    }

    public void addFileImportedListener(ImportListener listener) {
        _importListeners.add(listener);
    }

    public void addBeginImportListener(BeginImportListener listener) {
        _beginImportListeners.add(listener);
    }

    public void removeMatchListener(MatchListener listener) {
        _matchListeners.remove(listener);
    }

    public void removeFileSortedListener(FileSortedListener listener) {
        _fileSortedListeners.remove(listener);
    }

    public void removeFileImportedListener(ImportListener listener) {
        _importListeners.remove(listener);
    }

    public void removeBeginImportListener(BeginImportListener listener) {
        _beginImportListeners.remove(listener);
    }

    public Set<MatchListener> getMatchListeners() {
        return _matchListeners;
    }

    public Set<FileSortedListener> getFileSortedListeners() {
        return _fileSortedListeners;
    }

    public Set<ImportListener> getImportListeners() {
        return _importListeners;
    }

    public Set<BeginImportListener> getBeginImportListeners() {
        return _beginImportListeners;
    }
}
