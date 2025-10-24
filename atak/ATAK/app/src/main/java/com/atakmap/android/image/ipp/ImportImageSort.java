
package com.atakmap.android.image.ipp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;

import com.atakmap.android.image.ExifHelper;
import com.atakmap.android.image.quickpic.QuickPicReceiver;
import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.importfiles.sort.ImportVideoResolver;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.MapPointTool;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.filesystem.HashingUtils;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.util.Disposable;

/**
 * Sorts JPEG and PNG files, drops a point on the map,
 * and attaches the image Requires EXIF location to be set
 *
 * @deprecated use {@link ImportImageResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportImageSort extends ImportResolver
        implements MapPointTool.DropSelectCallback, Disposable {

    static class ImportInfo {
        public File file;
        public Set<SortFlags> flags;
    }

    private static final String TAG = "ImportImageSort";
    private final MapPointTool dropSelectTool;
    private volatile boolean active;
    private final MapView mapView;

    private final Map<String, ImportInfo> infoMap = new HashMap<>();

    public ImportImageSort(MapView mapView) {
        super(null, null, false, false);
        this.mapView = mapView;
        dropSelectTool = new MapPointTool(mapView, this);
    }

    @Override
    public boolean match(File file) {
        String fname = file.getName().toLowerCase(LocaleUtil.getCurrent());
        // if the provided file is does not contain metadata, then kick off the
        return (fname.endsWith(".png") ||
                fname.endsWith(".gif") ||
                fname.endsWith(".webm") ||
                fname.endsWith(".tif") ||
                fname.endsWith(".jpg")) && !isExif(file);
    }

    private static boolean isExif(final File file) {

        TiffImageMetadata exif = ExifHelper.getExifMetadata(file);
        if (exif != null) {
            try {
                if (exif.getGPS() != null)
                    return true;
            } catch (Exception ignore) {
            }
        }
        Log.d(TAG, "Failed to read valid image exif");
        return false;
    }

    /**
     * Send intent so CoT will be dispatched internally within ATAK Also sort file to proper
     * location
     * 
     * @param file the file to import
     * @return true if the file was imported successfully
     */
    @Override
    public boolean beginImport(File file) {
        return beginImport(file, Collections.emptySet());
    }

    @Override
    public boolean beginImport(File file, Set<SortFlags> flags) {

        String id = HashingUtils.md5sum(file);

        MapItem mi = mapView.getMapItem(id);

        if (mi != null) {

            return true;
        }

        ImportInfo ii = new ImportInfo();
        ii.file = file;
        ii.flags = flags;
        infoMap.put(id, ii);

        synchronized (this) {
            if (!active) {
                active = true;
                launchTool(id);
            }
        }

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        return file;
    }

    @Override
    public String getDisplayableName() {
        return mapView.getContext().getString(R.string.jpeg_image);
    }

    @Override
    public Drawable getIcon() {
        return mapView.getContext().getDrawable(R.drawable.camera);
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("JPEG Image", "image/jpeg");
    }

    @Override
    public void onToolEnd(String id) {
        synchronized (infoMap) {
            Thread t = new Thread("ImportImageSortEnd") {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignored) {
                    }
                    Set<String> keys = infoMap.keySet();

                    final ImportInfo ii = (id != null) ? infoMap.get(id) : null;
                    if (!keys.isEmpty()) {
                        if (ii == null) {
                            launchTool(keys.iterator().next());
                        } else {
                            final AlertDialog.Builder ad = new AlertDialog.Builder(
                                    mapView.getContext());
                            ad.setTitle(R.string.cancel);
                            ad.setMessage(mapView.getContext().getString(
                                    R.string.cancel_import_placement,
                                    ii.file.getName()));
                            ad.setNegativeButton(R.string.retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialogInterface,
                                                int i) {
                                            launchTool(id);
                                        }
                                    });
                            ad.setPositiveButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialogInterface,
                                                int i) {
                                            infoMap.remove(id);
                                            if (!keys.isEmpty()) {
                                                launchTool(
                                                        keys.iterator().next());
                                            } else
                                                active = false;
                                        }
                                    });
                            mapView.post(new Runnable() {
                                @Override
                                public void run() {
                                    ad.show();
                                }
                            });
                        }
                    } else {
                        active = false;
                    }
                }
            };
            t.start();

        }
    }

    @Override
    public void onResult(String id, GeoPointMetaData gpm, String uid) {
        ImportInfo ii;

        synchronized (infoMap) {
            ii = infoMap.remove(id);
        }

        if (ii != null) {
            quickPlace(id, ii.file, ii.flags, gpm);
        }
    }

    private void launchTool(String id) {
        Bundle extras = new Bundle();
        ImportInfo ii = infoMap.get(id);
        if (ii != null) {
            extras.putString("id", id);
            extras.putString("prompt",
                    mapView.getContext().getString(
                            R.string.select_location_place,
                            ii.file.getName()));
            ToolManagerBroadcastReceiver.getInstance().startTool(
                    MapPointTool.TOOL_IDENTIFIER, extras);
        }
    }

    private boolean quickPlace(String uid, File file, Set<SortFlags> flags,
            GeoPointMetaData point) {

        // sort file
        File dest = new File(AttachmentManager.getFolderPath(uid),
                file.getName());

        File destParent = dest.getParentFile();
        if (destParent == null) {
            Log.w(TAG,
                    "Destination has no parent file: "
                            + dest.getAbsolutePath());
            return false;
        }

        if (!IOProviderFactory.exists(destParent)) {
            if (!IOProviderFactory.mkdirs(destParent)) {
                Log.w(TAG,
                        "Failed to create directory: "
                                + destParent.getAbsolutePath());
                return false;
            } else {
                Log.d(TAG,
                        "Created directory: " + destParent.getAbsolutePath());
            }
        }

        // Note we attempt to place new file on same SD card, so copying should be minimal
        // until user feedback, just force copy

        if (_bCopyFile || true)
            try {
                FileSystemUtils.copyFile(file, dest);
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy file: " + dest.getAbsolutePath(), e);
                return false;
            }
        else {
            if (!FileSystemUtils.renameTo(file, dest)) {
                return false;
            }
        }

        // Create marker
        new PlacePointTool.MarkerCreator(point)
                .setUid(uid)
                .setCallsign(file.getName())
                .setType(QuickPicReceiver.QUICK_PIC_IMAGE_TYPE)
                .showCotDetails(false)
                .placePoint();

        // it is important that an addition post execution invalidates any caching of the
        // attachment manager in any case.
        AttachmentManager.notifyAttachmentChange(uid);

        onFileSorted(file, dest, flags);

        return true;
    }

    @Override
    public void dispose() {
        dropSelectTool.dispose();
        infoMap.clear();
    }
}
