
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportListener;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.importexport.Marshal;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.Nullable;

/**
 * Matches a file based on filename, content, or other properties. Also imports or 
 * initiates import
 */
public abstract class ImportResolver {

    // Enum of flags to be used in an enum set that wll be passed into the sorters.
    // Note that if IMPORT_INPLACE, IMPORT_MOVE, or IMPORT_COPY are specified in the
    // same set, IMPORT_COPY takes precedence followed by IMPORT_MOVE followed by
    // IMPORT_INPLACE
    public enum SortFlags {
        SHOW_NOTIFICATIONS, // Show notifications related to the import process
        ZOOM_TO_FILE, // Zoom to the file when it's finished importing
        HIDE_FILE, // Turn file visibility off by default
        IMPORT_INPLACE, // leave the file in place and just reference it from ATAK
        IMPORT_MOVE, // signals that it is desirable to move the file into the appropriate location when importing, will fall back to copying if move is not successfull
        IMPORT_COPY // signals that it is desirable to copy the file into the appropriate location when importing
    }

    private static final String TAG = "ImportResolver";

    protected final String _ext;

    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected boolean _bValidateExt;

    /**
     * True to copy (leave original file in place), False to move file to new location (potentially
     * more efficient). Note if false and move fails, it will revert to copy attempt
     */
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected boolean _bCopyFile;

    protected final String _folderName;

    protected boolean _bFileSorted = false;

    /**
     * File extension to match one
     */
    protected FileFilter _filter;

    private String displayName;
    private Drawable icon;

    /**
     * Given an extension and a folder name, construct a resolver that will be responsible for
     * importing a file into ATAK
     * @param ext that would be considered valid or null if there is no extension required
     * @param folderName the destination folder to be used during importing or null if only in place
     *                   usage is supported.
     */
    public ImportResolver(@Nullable String ext, @Nullable String folderName,
            @NonNull
            final String displayName,
            @NonNull
            final Drawable icon) {
        this(ext, folderName, !FileSystemUtils.isEmpty(ext), false);
        this.displayName = displayName;
        this.icon = icon;
    }

    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public ImportResolver(String ext, String folderName, boolean validateExt,
            boolean copyFile) {
        this.displayName = getClass().getSimpleName();
        this._ext = FileSystemUtils.isEmpty(ext) ? null
                : ext.toLowerCase(LocaleUtil.getCurrent());
        this._folderName = folderName;
        setOptions(validateExt, copyFile);
    }

    /**
     * Set the options for this resolver
     *
     * @param validateExt
     * @param copyFile
     */
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public void setOptions(boolean validateExt, boolean copyFile) {
        this._bValidateExt = validateExt;
        this._bCopyFile = copyFile;

        if (_ext != null && _bValidateExt) {
            _filter = new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    if (pathname == null)
                        return false;

                    return pathname.getName()
                            .toLowerCase(LocaleUtil.getCurrent())
                            .endsWith(_ext);
                }
            };
        } else {
            _filter = null;
        }
    }

    /**
     * Returns the extension currently supported by this resolver.
     * @return if null or empty then, this resolver does not define an extension.
     */
    public String getExt() {
        return _ext;
    }

    /**
     * Returns if the file is sorted or not.
     * @return true if the file is sorted
     */
    public boolean getFileSorted() {
        return _bFileSorted;
    }

    /**
     * Return true if this sort matches the specified file
     * 
     * @param file the file to read to see if the import resolver supports the file.
     * @return true if the import resolver is capable of handling the file.
     */
    public boolean match(final File file) {
        if (!_bValidateExt)
            return true;

        if (_filter == null)
            return false;

        return _filter.accept(file);
    }

    /**
     * Provides for a capability by which a set of resolvers can be trimmed by a found sorter
     * implementation.    This provides great power to the sorter to force itself to be used over
     * a group of other resolvers.    Plugin implementers should take great caution in using this
     * only when it is guaranteed to be the correct behavior.
     *
     * @param importResolvers the list of import resolvers that to filter.    The modification are
     *                        made directly to the importResolvers list.
     * @param file the file that is being considered for the resolvers.
     */
    public void filterFoundResolvers(final List<ImportResolver> importResolvers,
            File file) {
        // no default filtering occurs
    }

    /**
     * Initiate import of the specified file
     *
     * @param file File to import.
     * @return true if the import is successful, false otherwise
     */
    public boolean beginImport(File file) {
        return beginImport(file, new HashSet<>());
    }

    /**
     * Initiate import of the specified file
     * 
     * @param file File to import
     * @param sortFlags Enum Set of flags that should be used to modify import behavior.
     * @return True if the import is successful, false otherwise.
     */
    public boolean beginImport(File file, Set<SortFlags> sortFlags) {
        _bFileSorted = false;

        File dest = getDestinationPath(file);
        if (dest == null) {
            Log.w(TAG,
                    "failed to find SD card root for: "
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

        int id = NotificationUtil.getInstance().reserveNotifyId();

        final boolean isDirectory = IOProviderFactory.isDirectory(file);


        if (!isDirectory && sortFlags.contains(SortFlags.IMPORT_COPY)) {
            NotificationUtil.getInstance().postNotification(id,
                    NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                    NotificationUtil.BLUE,
                    String.format(LocaleUtil.US, "Attempting to copy file %s", file.getName()), null, null, false);
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
            } finally {
                NotificationUtil.getInstance().clearNotification(id);
            }
        } else if (!isDirectory && sortFlags.contains(SortFlags.IMPORT_MOVE)) {
            Log.d(TAG, "attempting to move: " + file + " to " + dest);
            if (!file.getAbsolutePath().equals(dest.getAbsolutePath())) {
                final boolean retval = FileSystemUtils.renameTo(file, dest);



                if (!retval) {
                    NotificationUtil.getInstance().postNotification(id,
                            NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                            NotificationUtil.BLUE,
                            String.format(LocaleUtil.US, "Failed to move, attempting to copy file %s", file.getName()), null, null, false);
                    try {
                        Log.d(TAG, "move failed, attempting to copy: " + file
                                + " to " + dest);
                        FileSystemUtils.copyFile(file, dest);
                    } catch (IOException e) {
                        Log.e(TAG, "failed to copy file: "
                                + dest.getAbsolutePath(), e);
                        return false;
                    } finally {
                        NotificationUtil.getInstance().clearNotification(id);
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
        File folder = FileSystemUtils.getItem(_folderName);

        // set if file extension, if applicable
        String fileName = file.getName();
        if (!FileSystemUtils.isEmpty(getExt())
                && !fileName.endsWith(getExt())) {
            Log.d(TAG, "Added extension to destination path: " + fileName);
            fileName += getExt();
        }

        return new File(folder, fileName);
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
            Set<SortFlags> sortedFlags) {

        _bFileSorted = true;

        // from the legacy InPlaceResolver and should not impact other resolvers
        final Pair<String, String> contentMIME = getContentMIME();
        if (contentMIME != null) {
            Log.d(TAG,
                    "begin onFileSorted " + getClass()
                            + " to process: " + dst.getAbsolutePath()
                            + " contentMime: " + contentMIME);
            Intent i = new Intent(ImportExportMapComponent.ACTION_IMPORT_DATA);
            i.putExtra(ImportReceiver.EXTRA_CONTENT, contentMIME.first);
            i.putExtra(ImportReceiver.EXTRA_MIME_TYPE, contentMIME.second);
            i.putExtra(ImportReceiver.EXTRA_URI, Uri.fromFile(dst).toString());
            if (sortedFlags.contains(SortFlags.SHOW_NOTIFICATIONS))
                i.putExtra(ImportReceiver.EXTRA_SHOW_NOTIFICATIONS, true);
            if (sortedFlags.contains(SortFlags.ZOOM_TO_FILE))
                i.putExtra(ImportReceiver.EXTRA_ZOOM_TO_FILE, true);
            if (sortedFlags.contains(SortFlags.HIDE_FILE))
                i.putExtra(ImportReceiver.EXTRA_HIDE_FILE, true);
            AtakBroadcast.getInstance().sendBroadcast(i);

        }

        MapView mv = MapView.getMapView();
        if (sortedFlags.contains(SortFlags.SHOW_NOTIFICATIONS) && mv != null) {
            Intent i = new Intent(ImportExportMapComponent.ZOOM_TO_FILE_ACTION);
            i.putExtra("filepath", dst.getAbsolutePath());
            Context ctx = mv.getContext();
            NotificationUtil.getInstance().postNotification(
                    NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                    NotificationUtil.BLUE,
                    ctx.getString(R.string.importmgr_finished_import,
                            src.getName()),
                    null, null, i);
            Log.d(TAG,
                    "begin onFileSorted, notification " + getClass()
                            + " for: " + dst.getAbsolutePath());
        }

        // Fire sort listeners
        List<ImportListener> importers = ImportExportMapComponent.getInstance()
                .getImportListeners();
        for (ImportListener l : importers)
            l.onFileSorted(src, dst);

    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Sorting %s, to %s", _ext, _folderName);
    }

    /**
     * Are directories supported by {@link #match(File)} and {@link #beginImport(File)}?
     * @return True if directories can be sorted by this class, false otherwise.
     */
    public boolean directoriesSupported() {
        return false;
    }

    /**
     * Get a human readable string that can be displayed to the user so that
     * they can differentiate between different sorter implementations. This
     * string MUST be unique to a sorter, and any duplicates will likely
     * be removed before the user gets a chance to see them.
     * @return Unique (by class, not instance), human readable name for this sorter. 
     */
    public String getDisplayableName() {
        return displayName;
    }

    /**
     * Get an icon that represents this sorter
     * @return Icon or null to use a generic icon
     */
    public Drawable getIcon() {
        if (icon != null)
            return icon;

        MapView mv = MapView.getMapView();
        return mv != null ? mv.getContext().getDrawable(
                R.drawable.ic_menu_import_file) : null;
    }

    /**
     * Get the content and MIME type
     * @return Pair containing the content type and MIME type respectively
     */
    public Pair<String, String> getContentMIME() {
        return null;
    }

    public static ImportResolver fromMarshal(final Marshal m, Drawable icon) {
        return new ImportResolver(null, null,
                m.getContentType(), icon) {
            @Override
            public boolean match(File f) {
                try {
                    return m.marshal(Uri.fromFile(f)) != null;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            protected void onFileSorted(File src, File dst,
                    Set<SortFlags> flags) {
                final Uri uri = Uri.fromFile(dst);
                String mime;
                try {
                    mime = m.marshal(uri);
                } catch (IOException e) {
                    mime = null;
                }
                if (mime == null) {
                    super.onFileSorted(src, dst, flags);
                    return;
                }

                Intent i = new Intent(
                        ImportExportMapComponent.ACTION_IMPORT_DATA);
                i.putExtra(ImportReceiver.EXTRA_CONTENT, m.getContentType());
                i.putExtra(ImportReceiver.EXTRA_MIME_TYPE, mime);
                i.putExtra(ImportReceiver.EXTRA_URI, uri.toString());
                if (flags.contains(SortFlags.SHOW_NOTIFICATIONS))
                    i.putExtra(ImportReceiver.EXTRA_SHOW_NOTIFICATIONS, true);
                if (flags.contains(SortFlags.ZOOM_TO_FILE))
                    i.putExtra(ImportReceiver.EXTRA_ZOOM_TO_FILE, true);
                if (flags.contains(SortFlags.HIDE_FILE))
                    i.putExtra(ImportReceiver.EXTRA_HIDE_FILE, true);
                AtakBroadcast.getInstance().sendBroadcast(i);
            }

            @Override
            public Pair<String, String> getContentMIME() {
                return new Pair<>(m.getContentType(),
                        ResourceFile.UNKNOWN_MIME_TYPE);
            }
        };
    }

    public static ImportResolver fromMarshal(Marshal m) {
        return fromMarshal(m, null);
    }
}
