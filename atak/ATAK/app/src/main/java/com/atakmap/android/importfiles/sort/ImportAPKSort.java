
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.update.AppMgmtUtils;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Sorts Android APK files, and initiates install
 *
 * @deprecated use {@link ImportAPKResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportAPKSort extends ImportResolver {

    private static final String TAG = "ImportAPKSort";
    private static final String MATCHER_FILE = "AndroidManifest.xml";

    private final Context _context;

    public ImportAPKSort(Context context, boolean validateExt) {
        super(".apk",
                FileSystemUtils.getItem(FileSystemUtils.TMP_DIRECTORY)
                        .getAbsolutePath(),
                context.getString(R.string.android_apk_file),
                context.getDrawable(R.drawable.ic_android_display_settings));
        this._context = context;
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .infz, now lets see if it has a product.inf
        boolean bMatch = isApk(file);
        Log.d(TAG, "APK " + (bMatch ? "found" : "not found"));
        return bMatch;
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        super.onFileSorted(src, dst, flags);
        Log.d(TAG, "Sorted, now initiating repo sync");

        //kick off install
        //TODO add to FileSystemProductProvider?
        AppMgmtUtils.install(_context, dst);
    }

    @Override
    public boolean beginImport(File file, Set<SortFlags> sortFlags) {
        // force copy
        sortFlags.remove(SortFlags.IMPORT_MOVE);
        sortFlags.remove(SortFlags.IMPORT_INPLACE);
        sortFlags.add(SortFlags.IMPORT_COPY);

        return super.beginImport(file, sortFlags);
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("Android App",
                "application/vnd.android.package-archive");
    }

    /**
     * Check if the specified zip has an Android Manifest
     *
     * @param zip the file
     * @return if it it is an apk
     */
    private static boolean isApk(File zip) {
        return FileSystemUtils.ZipHasFile(zip, MATCHER_FILE);
    }
}
