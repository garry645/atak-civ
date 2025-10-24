
package com.atakmap.android.video.overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.data.URIHelper;
import com.atakmap.android.hierarchy.HierarchyListAdapter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.Action;
import com.atakmap.android.hierarchy.action.Delete;
import com.atakmap.android.hierarchy.action.Export;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.importexport.send.SendDialog;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.missionpackage.export.MissionPackageExportWrapper;
import com.atakmap.android.video.AddEditAlias;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.ConnectionEntry.Protocol;
import com.atakmap.android.video.export.VideoExportWrapper;
import com.atakmap.android.video.manager.VideoContentHandler;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.partech.pgscmedia.MediaException;
import com.partech.pgscmedia.MediaFormat;
import com.partech.pgscmedia.MediaProcessor;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * List item representing a video alias
 */
public class VideoBrowserHierarchyListItem extends AbstractHierarchyListItem2
        implements Search, Delete, Export, View.OnClickListener,
        FOVFilter.Filterable {

    private final static String TAG = "VideoBrowserHierarchyListItem";
    private final static String VIDEO_FILE_PREFIX = "/atak/tools/videos";

    protected final MapView _mapView;
    protected final Context _context;
    protected final ConnectionEntry _entry;
    protected final URIContentHandler _handler;
    protected final VideoFolderHierarchyListItem _parent;

    VideoBrowserHierarchyListItem(MapView mapView, ConnectionEntry entry,
            VideoFolderHierarchyListItem parent) {
        _mapView = mapView;
        _context = mapView.getContext();
        _entry = entry;
        _parent = parent;

        URIContentHandler h = null;
        if (entry != null)
            h = URIContentManager.getInstance().getHandler(
                    entry.getLocalFile(), "Video");

        if (h == null && entry != null) {
            h = new VideoContentHandler(mapView, entry);
        }

        _handler = h;

        this.asyncRefresh = true;
    }

    @Override
    public String getTitle() {
        return _entry.getAlias();
    }

    @Override
    public String getDescription() {
        return _entry.getProtocol() == Protocol.FILE
                && _entry.getLocalFile() != null
                        ? getFileDescription()
                        : _entry.getAddress(true);
    }

    private String getFileDescription() {
        File file = _entry.getLocalFile();
        if (!FileSystemUtils.isFile(file)
                || !FileSystemUtils.isFile(file.getParentFile()))
            return "";

        String path = file.getParentFile().getAbsolutePath();

        String locationId = "[EXT]";

        if (path.startsWith("/storage/emulated/0"))
            locationId = "[INT]";

        if (path.indexOf(VIDEO_FILE_PREFIX) > 0) {
            //chop off the android file system mount details, and do not display the filename
            path = path.substring(path.indexOf(VIDEO_FILE_PREFIX));
        }

        path = String.format("(%s) %s %s",
                MathUtils.GetLengthString(file.length()), locationId, path);
        return path;
    }

    @Override
    public String getUID() {
        return _entry.getUID();
    }

    @Override
    public boolean isMultiSelectSupported() {
        return false;
    }

    @Override
    public Drawable getIconDrawable() {
        return _context.getDrawable(_entry.getProtocol() == Protocol.FILE
                ? R.drawable.ic_video_alias
                : R.drawable.ic_video_remote);
    }

    @Override
    public ConnectionEntry getUserObject() {
        return _entry;
    }

    @Override
    public View getExtraView(View v, ViewGroup parent) {
        ExtraHolder h = v != null && v.getTag() instanceof ExtraHolder
                ? (ExtraHolder) v.getTag()
                : null;
        if (h == null) {
            h = new ExtraHolder();
            v = LayoutInflater.from(_context).inflate(
                    R.layout.video_list_item_extra, parent, false);
            h.menu = v.findViewById(R.id.video_menu);
            h.menu_open = v.findViewById(R.id.video_menu_open);
            h.send = v.findViewById(R.id.send);
            h.edit = v.findViewById(R.id.edit);
            h.save = v.findViewById(R.id.save);
            h.details = v.findViewById(R.id.details);
            h.delete = v.findViewById(R.id.delete);
            v.setTag(h);
        }
        h.send.setOnClickListener(this);
        h.edit.setOnClickListener(this);
        h.save.setOnClickListener(this);
        h.details.setOnClickListener(this);
        h.delete.setOnClickListener(this);
        h.menu.setOnClickListener(this);

        boolean temp = _entry.isTemporary();
        boolean selecting = isSelecting();
        boolean showMenu = !selecting && _parent != null
                && _parent.isShowingMenu(this);
        boolean isFolder = _entry.getProtocol() == Protocol.DIRECTORY;
        boolean isFile = _entry.getProtocol() == Protocol.FILE;

        h.menu_open.setVisibility(showMenu ? View.VISIBLE : View.GONE);
        h.menu.setVisibility(selecting || showMenu ? View.GONE : View.VISIBLE);
        h.send.setVisibility(selecting || temp ? View.GONE : View.VISIBLE);
        h.save.setVisibility(temp ? View.VISIBLE : View.GONE);
        h.details.setVisibility(isFile ? View.VISIBLE : View.GONE);
        h.delete.setVisibility(selecting ? View.GONE : View.VISIBLE);
        h.delete.setEnabled(!temp);
        h.edit.setVisibility(selecting || isFolder || isFile || temp
                ? View.GONE
                : View.VISIBLE);
        return v;
    }

    private static class ExtraHolder {
        ImageButton menu, send, edit, save, details, delete;
        LinearLayout menu_open;
    }

    private boolean isSelecting() {
        return this.listener instanceof HierarchyListAdapter
                && ((HierarchyListAdapter) this.listener)
                        .getSelectHandler() != null;
    }

    @Override
    protected void refreshImpl() {
    }

    @Override
    public boolean hideIfEmpty() {
        return true;
    }

    @Override
    public int getDescendantCount() {
        return 0;
    }

    @Override
    public boolean isChildSupported() {
        return false;
    }

    /**************************************************************************/

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Send video alias
        if (id == R.id.send) {
            SendDialog.Builder b = new SendDialog.Builder(_mapView);
            b.setName(getTitle());
            b.setIcon(getIconDrawable());
            if (_entry.isRemote()) {
                // If this is a remote entry we only need the XML
                b.setURI(URIHelper.getURI(_entry));
            } else {
                // Otherwise if this is a file or folder we need to wrap in MP
                addFiles(b, _entry);
            }
            b.show();
        }

        // Edit entry details
        else if (id == R.id.edit) {
            AddEditAlias dialog = new AddEditAlias(_context);
            dialog.addEditConnection(VideoManager.refreshConnectionEntry(_entry));
        }

        // Save temporary video alias
        else if (id == R.id.save) {
            _entry.setTemporary(false);
            VideoManager.getInstance().addEntry(_entry);
            notifyListener(false);
        }

        // View file details
        else if (id == R.id.details) {
            showDetails();
        }

        // Delete alias
        else if (id == R.id.delete)
            promptDelete();

        // Expand out video options
        else if (id == R.id.video_menu) {
            if (_parent != null)
                _parent.showMenu(this);
        }
    }

    public static class VideoTrackData {
        public long duration = -1;
        public float bitRate = -1;
        public float frameRate = -1;
        public MediaFormat[] format = null;

        public boolean hasDuration() {
            return duration != -1;
        }

        public boolean hasBitrate() {
            return bitRate != -1;
        }

        public boolean hasFramerate() {
            return frameRate != -1;
        }

        public boolean hasFormat() {
            return format != null;
        }
    }

    public static VideoTrackData getVideoTrackData(File file) {
        if (!FileSystemUtils.isFile(file)) {
            Log.w(TAG, "getVideoTrackData invalid file");
            return null;
        }

        Log.d(TAG, "getVideoTrackData: " + file.getAbsolutePath());

        MediaProcessor processor = null;
        try {
            VideoTrackData data = new VideoTrackData();
            processor = new MediaProcessor(file);
            data.duration = processor.getDuration();
            data.bitRate = processor.getBitRate();
            data.frameRate = processor.getFrameRate();
            data.format = processor.getTrackInfo();
            Log.d(TAG, file.getAbsolutePath() + " duration: " + data.duration);
            return data;
        } catch (MediaException e) {
            Log.w(TAG, "Failed to get duration", e);
        } finally {
            if (processor != null) {
                try {
                    processor.stop();
                    processor.destroy();
                    processor = null;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to close video processor", e);
                }
            }
        }

        return null;
    }

    private void showDetails() {

        File file = _entry.getLocalFile();
        if (!FileSystemUtils.isFile(file)) {
            Log.w(TAG, "showDetails, no file: " + _entry);
            return;
        }

        View view = LayoutInflater.from(_context).inflate(
                R.layout.alias_file, null, false);
        ((TextView) view.findViewById(R.id.alias_path))
                .setText(file.getParent());
        ((TextView) view.findViewById(R.id.alias_name)).setText(file.getName());
        ((TextView) view.findViewById(R.id.alias_size))
                .setText(MathUtils.GetLengthString(file.length()));
        ((TextView) view.findViewById(R.id.alias_id)).setText(_entry.getUID());

        view.findViewById(R.id.alias_hash_layout).setVisibility(View.GONE);
        view.findViewById(R.id.alias_length_layout).setVisibility(View.GONE);
        view.findViewById(R.id.alias_bitrate_layout).setVisibility(View.GONE);
        view.findViewById(R.id.alias_framerate_layout).setVisibility(View.GONE);
        view.findViewById(R.id.alias_more)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View alias_more) {
                        alias_more.setEnabled(false);
                        postRefresh(new Runnable() {
                            @Override
                            public void run() {
                                final String md5 = HashingUtils.md5sum(file);
                                Log.d(TAG, "md5: " + md5);

                                final VideoTrackData data = getVideoTrackData(
                                        file);
                                _mapView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        alias_more.setVisibility(View.GONE);
                                        if (!FileSystemUtils.isEmpty(md5)) {
                                            view.findViewById(
                                                    R.id.alias_hash_layout)
                                                    .setVisibility(
                                                            View.VISIBLE);
                                            ((TextView) view.findViewById(
                                                    R.id.alias_hash))
                                                            .setText(md5);
                                        }

                                        if (data != null) {
                                            if (data.hasDuration()) {
                                                String str = MathUtils
                                                        .GetTimeRemainingString(
                                                                data.duration);
                                                Log.d(TAG,
                                                        "Got duration: " + str);
                                                view.findViewById(
                                                        R.id.alias_length_layout)
                                                        .setVisibility(
                                                                View.VISIBLE);
                                                ((TextView) view.findViewById(
                                                        R.id.alias_length))
                                                                .setText(
                                                                        FileSystemUtils
                                                                                .isEmpty(
                                                                                        str) ? "" + data.duration : str);

                                            }
                                            if (data.hasBitrate()) {
                                                Log.d(TAG, "Got bitrate: "
                                                        + data.bitRate);
                                                view.findViewById(
                                                        R.id.alias_bitrate_layout)
                                                        .setVisibility(
                                                                View.VISIBLE);
                                                double bytesMillis = data.bitRate
                                                        / (1000 * 8);
                                                ((TextView) view.findViewById(
                                                        R.id.alias_bitrate))
                                                                .setText(
                                                                        MathUtils
                                                                                .GetDownloadSpeedString(
                                                                                        bytesMillis));
                                                //((TextView)view.findViewById(R.id.alias_bitrate)).setText(""+(data.bitRate) + "/sec");
                                            }
                                            if (data.hasFramerate()) {
                                                Log.d(TAG, "Got frameRate: "
                                                        + data.frameRate);
                                                view.findViewById(
                                                        R.id.alias_framerate_layout)
                                                        .setVisibility(
                                                                View.VISIBLE);
                                                ((TextView) view.findViewById(
                                                        R.id.alias_framerate))
                                                                .setText(""
                                                                        + (Math.round(
                                                                                data.frameRate))
                                                                        + "/sec");
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                });

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(file.getName());
        b.setView(view);
        b.setPositiveButton(R.string.ok, null);
        b.show();
    }

    protected static void addFiles(SendDialog.Builder b,
            ConnectionEntry entry) {
        Protocol p = entry.getProtocol();
        List<ConnectionEntry> children = entry.getChildren();
        if (p != Protocol.DIRECTORY || children == null) {
            File f;
            if (p == Protocol.FILE)
                f = new File(entry.getPath());
            else
                f = entry.getLocalFile();
            if (FileSystemUtils.isFile(f))
                b.addFile(f);
        } else {
            for (ConnectionEntry c : children)
                addFiles(b, c);
        }
    }

    @Override
    public <T extends Action> T getAction(Class<T> clazz) {
        if (clazz.equals(GoTo.class) && isSelecting()) {
            // Do not open the video while multi-selecting (ATAK-10970)
            return null;
        }
        if (_handler != null && _handler.isActionSupported(clazz))
            return clazz.cast(_handler);
        return super.getAction(clazz);
    }

    @Override
    public boolean isSupported(Class<?> target) {
        return VideoExportWrapper.class.equals(target)
                || MissionPackageExportWrapper.class.equals(target);
    }

    @Override
    public Object toObjectOf(Class<?> target, ExportFilters filters)
            throws FormatNotSupportedException {
        if (_entry == null)
            return null;

        if (VideoExportWrapper.class.equals(target)) {
            VideoExportWrapper mp = new VideoExportWrapper();
            File file = _entry.getLocalFile();
            if (!FileSystemUtils.isFile(file))
                return mp;
            if (IOProviderFactory.isDirectory(file)) {
                Log.d(TAG, "Adding directory: " + file.getAbsolutePath());

                File[] files = IOProviderFactory.listFiles(file);
                if (!FileSystemUtils.isEmpty(files)) {
                    for (File f : files) {
                        Log.d(TAG,
                                "Adding child file: " + file.getAbsolutePath());
                        mp.add(new ConnectionEntry(f));
                    }
                }
            } else {
                Log.d(TAG, "Adding file: " + file.getAbsolutePath());
                mp.add(_entry);
            }
            return mp;
        } else if (MissionPackageExportWrapper.class.equals(target)) {
            MissionPackageExportWrapper mp = new MissionPackageExportWrapper();
            File file = _entry.getLocalFile();
            if (!FileSystemUtils.isFile(file))
                return mp;
            if (IOProviderFactory.isDirectory(file)) {
                File[] files = IOProviderFactory.listFiles(file);
                if (!FileSystemUtils.isEmpty(files)) {
                    for (File f : files)
                        mp.addFile(f);
                }
            } else
                mp.addFile(file);
            return mp;
        }
        return null;
    }

    @Override
    public boolean delete() {
        // Remove the entry / file
        VideoManager.getInstance().removeEntry(_entry);
        return true;
    }

    protected void promptDelete() {
        if (!_entry.isRemote()) {
            // Check if the file can be deleted
            File f = _entry.getLocalFile();
            if (f == null || !FileSystemUtils.canWrite(f.getParentFile())) {
                Toast.makeText(_context,
                        R.string.cannot_delete_sdcard_file_msg,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        Protocol proto = _entry.getProtocol();
        String title;
        String message = _context.getString(R.string.are_you_sure_delete)
                + getTitle();
        if (proto.equals(Protocol.FILE)) {
            title = _context.getString(R.string.remove_file);
            message += _context.getString(R.string.video_text17);
        } else if (proto.equals(Protocol.DIRECTORY)) {
            title = _context.getString(R.string.video_text18);
            message += _context.getString(R.string.video_text19);
        } else {
            title = _context.getString(R.string.alias_del_title);
            message += _context.getString(R.string.video_text20);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(title);
        b.setMessage(message);
        b.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        delete();
                    }
                });
        b.setNegativeButton(R.string.cancel, null);
        b.show();
    }

    @Override
    public boolean accept(FOVFilter.MapState fov) {
        // Don't show videos when FOV filter is active
        // unless we have a way of getting an associated point from a video
        return false;
    }

    @Override
    public Set<HierarchyListItem> find(String terms) {
        Set<HierarchyListItem> retval = new HashSet<>();
        terms = terms.toLowerCase(LocaleUtil.getCurrent());

        //search self
        if (_entry != null) {
            if (find(_entry.getAlias(), terms)
                    || find(_entry.getAddress(), terms)
                    || find(_entry.getPath(), terms)
                    || (_entry.getLocalFile() != null
                            && find(_entry.getLocalFile().getName(), terms)))
                retval.add(this);
        }

        //search children
        List<HierarchyListItem> children = getChildren();
        for (HierarchyListItem i : children) {
            Object o = i.getUserObject();
            if (!(o instanceof ConnectionEntry))
                continue;
            ConnectionEntry conn = (ConnectionEntry) o;
            if (find(conn.getAlias(), terms)
                    || find(conn.getAddress(), terms)
                    || find(conn.getPath(), terms)
                    || (conn.getLocalFile() != null
                            && find(conn.getLocalFile().getName(), terms)))
                retval.add(i);

            //recursive find into child folders
            if (conn.getProtocol() == Protocol.DIRECTORY
                    && i instanceof AbstractHierarchyListItem2) {
                if (i.isChildSupported()) {
                    List<HierarchyListItem> children2 = ((AbstractHierarchyListItem2) i)
                            .getChildren();
                    for (HierarchyListItem child2 : children2) {
                        if (child2 instanceof Search) {
                            Set<HierarchyListItem> retval2 = ((Search) child2)
                                    .find(terms);
                            if (!FileSystemUtils.isEmpty(retval2)) {
                                Log.d(TAG, "Adding children matches: "
                                        + retval2.size());
                                retval.addAll(retval2);
                            }
                        }
                    }
                }
            }
        }
        return retval;
    }

    protected boolean find(String text, String terms) {
        return text != null && text.toLowerCase(
                LocaleUtil.getCurrent()).contains(terms);
    }
}
