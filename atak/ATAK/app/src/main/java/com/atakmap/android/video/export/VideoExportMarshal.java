
package com.atakmap.android.video.export;

import android.content.Context;
import android.widget.Toast;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.importexport.ExportFileMarshal;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.overlay.MapOverlay;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.overlay.VideoBrowserHierarchyListItem;
import com.atakmap.android.video.overlay.VideoBrowserMapOverlay;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports videos to Zip
 */
public class VideoExportMarshal extends ExportFileMarshal {
    private static final String TAG = "VideoExportMarshal";

    private final List<ConnectionEntry> exports;

    /**
     * Builds a Marshall capable of exporting videos as a zip file.
     * @param context the context used to look up resources and icons.
     */
    public VideoExportMarshal(Context context) {
        super(context, ResourceFile.MIMEType.ZIP.EXT,
                ResourceFile.MIMEType.ZIP.MIME,
                R.drawable.ic_menu_video);

        // remove the requirement that exported map items support the target
        // class VideoExportWrapper
        getFilters().clear();

        exports = new ArrayList<>();
    }

    @Override
    protected String filePrefix() {
        return context.getString(R.string.app_name) + "-video";
    }

    @Override
    public File getFile() {
        return new File(
                FileSystemUtils.getItem(FileSystemUtils.EXPORT_DIRECTORY),
                filename);
    }

    @Override
    protected boolean marshal(Exportable export) throws IOException,
            FormatNotSupportedException {
        if (export == null
                || !export.isSupported(VideoExportWrapper.class)) {
            Log.d(TAG, "Skipping unsupported export "
                    + (export == null ? "" : export.getClass().getName()));
            return false;
        }

        VideoExportWrapper folder = (VideoExportWrapper) export
                .toObjectOf(
                        VideoExportWrapper.class, getFilters());
        if (folder == null || folder.isEmpty()) {
            Log.d(TAG, "Skipping empty folder");
            return false;
        }
        Log.d(TAG, "Adding folder");
        this.exports.addAll(folder.getExports());

        Log.d(TAG, "Added video count: " + folder.getExports().size());
        return true;
    }

    @Override
    public Class<?> getTargetClass() {
        return VideoExportWrapper.class;
    }

    @Override
    protected void finalizeMarshal() throws IOException {
        synchronized (this) {
            if (this.isCancelled) {
                Log.d(TAG, "Cancelled, in finalizeMarshal");
                return;
            }
        }

        // delete existing file, and then serialize out to file
        File file = getFile();
        if (IOProviderFactory.exists(file)) {
            FileSystemUtils.deleteFile(file);
        }

        List<File> files = getFiles();
        if (FileSystemUtils.isEmpty(files)) {
            throw new IOException("No video files to export");
        }

        //TODO sits at 94% during serialization to KMZ/zip. Could serialize during marshall above
        File zip = FileSystemUtils.zipDirectory(files, file);
        if (zip == null) {
            throw new IOException("Failed to serialize Zip");
        }

        synchronized (this) {
            if (this.isCancelled) {
                Log.d(TAG, "Cancelled, in finalizeMarshal");
                return;
            }
        }
        if (hasProgress()) {
            this.progress.publish(94);
        }

        Log.d(TAG, "Exported: " + file.getAbsolutePath());
    }

    @Override
    public void postMarshal() {
        synchronized (this) {
            if (this.isCancelled) {
                Log.w(TAG, "Cancelled, but in postMarshal");
                return;
            }
        }
        final File file = getFile();
        if (!IOProviderFactory.exists(file)) {
            Log.d(TAG, "Export failed: " + file.getAbsolutePath());
            NotificationUtil.getInstance().postNotification(
                    R.drawable.ic_network_error_notification_icon,
                    NotificationUtil.RED,
                    context.getString(R.string.importmgr_export_failed,
                            getContentType()),
                    context.getString(R.string.importmgr_failed_to_export,
                            getContentType()),
                    context.getString(R.string.importmgr_failed_to_export,
                            getContentType()));
        }
        Toast.makeText(context,
                context.getString(R.string.importmgr_exported_file,
                        file.getAbsolutePath()),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Allow only videos
     */
    @Override
    public boolean accept(HierarchyListItem item) {
        //Log.d(TAG, "filterListItem " + item.getClass().getName() + ", " + item.getTitle());
        return super.accept(item);
    }

    @Override
    public boolean filterGroup(MapGroup group) {
        //Log.d(TAG, "filterGroup: " + group.getFriendlyName());
        //return super.filterGroup(group);
        return true;
    }

    @Override
    public boolean filterItem(MapItem item) {
        //Log.d(TAG, "filterItem: " + item.getUID());
        //return super.filterItem(item);
        return true;
    }

    /**
     * Allow only videos
     */
    @Override
    public boolean filterListItemImpl(HierarchyListItem item) {
        //Log.d(TAG, "filterListItemImpl " + item.getClass().getName() + ", " + item.getTitle());
        //return super.filterListItemImpl(item);

        if (item instanceof VideoBrowserMapOverlay.ListModel) {
            return false;
        }

        if (item instanceof VideoBrowserHierarchyListItem) {
            VideoBrowserHierarchyListItem vi = (VideoBrowserHierarchyListItem) item;
            ConnectionEntry o = vi.getUserObject();
            if (o != null) {
                ConnectionEntry conn = o;
                if (conn.getProtocol() == ConnectionEntry.Protocol.DIRECTORY
                        || conn.getProtocol() == ConnectionEntry.Protocol.FILE) {
                    File f = conn.getLocalFile();
                    return !FileSystemUtils.isFile(f);
                }
            }
        }

        return true;
    }

    @Override
    public boolean filterOverlay(MapOverlay overlay) {
        //Log.d(TAG, "filterOverlay: " + overlay.getName());
        //        return super.filterOverlay(overlay);
        return true;
    }

    private List<File> getFiles() {
        if (FileSystemUtils.isEmpty(exports)) {
            return null;
        }

        List<File> files = new ArrayList<>();
        for (ConnectionEntry export : exports) {
            if (export == null
                    || !FileSystemUtils.isFile(export.getLocalFile())) {
                Log.w(TAG, "Skipping invalid export");
                continue;
            }

            files.add(export.getLocalFile());
        }

        return files;
    }
}
