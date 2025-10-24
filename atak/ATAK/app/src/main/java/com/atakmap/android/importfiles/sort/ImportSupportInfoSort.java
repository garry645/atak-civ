
package com.atakmap.android.importfiles.sort;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.ModifierApi;

/**
 * Sorts ATAK Support Files
 * @deprecated use {@link ImportSupportInfoResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportSupportInfoSort extends ImportResolver {

    private static final String TAG = "ImportSupportInfoSort";

    /**
     * Enumeration of support files
     */
    private enum TYPE {
        SUPPORTINF("support.inf", FileSystemUtils.SUPPORT_DIRECTORY),
        SPLASH("atak_splash.png", FileSystemUtils.SUPPORT_DIRECTORY);

        final String _filename;
        final String _folder;

        TYPE(String filename, String folder) {
            _filename = filename;
            _folder = folder;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s %s %s", super.toString(), _filename,
                    _folder);
        }
    }

    public ImportSupportInfoSort(boolean copyFile) {
        super("", "", "Support Info File",
                MapView.getMapView().getContext().getDrawable(
                        R.drawable.ic_menu_import_file));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        return getType(file) != null;
    }

    @ModifierApi(since = "5.3", modifiers = {
            "static"
    }, target = "5.6")
    public static TYPE getType(File file) {
        try {
            for (TYPE t : TYPE.values()) {
                if (t._filename.equalsIgnoreCase(file.getName())) {
                    Log.d(TAG, "Match Support Info content: " + t);
                    return t;
                }
            }

            //Log.d(TAG, "Failed to match ATAK Support Info content");
            return null;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match Support Info", e);
            return null;
        }
    }

    /**
     * Move to new location on same SD card Defer to TYPE for the relative path
     */
    @Override
    public File getDestinationPath(File file) {
        TYPE t = getType(file);
        if (t == null) {
            Log.e(TAG,
                    "Failed to match Support Info file: "
                            + file.getAbsolutePath());
            return null;
        }

        File folder = FileSystemUtils.isEmpty(t._folder)
                ? FileSystemUtils.getRoot()
                : FileSystemUtils.getItem(t._folder);
        return new File(folder, file.getName());
    }

}
