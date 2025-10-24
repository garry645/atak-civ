
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.GMLSpatialDb;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.util.Enumeration;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportGMLZResolver;
import gov.tak.api.importfiles.ImportSHPResolver;

/**
 * Imports archived GML files
 *
 * @deprecated use {@link ImportGMLZResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportGMLZSort extends ImportResolver {

    private static final String TAG = "ImportGMLZSort";

    public ImportGMLZSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(".zip", FileSystemUtils.OVERLAYS_DIRECTORY,
                "Zipped GML",
                context.getDrawable(R.drawable.ic_shapefile));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it contains a .gml
        return HasGML(file);
    }

    /**
     * 
     * @param file the file
     * @return true if the file contains a gml dataset
     */
    public static boolean HasGML(File file) {
        if (file == null) {
            Log.d(TAG, "ZIP file points to null.");
            return false;
        }

        if (!IOProviderFactory.exists(file)) {
            Log.d(TAG, "ZIP does not exist: " + file.getAbsolutePath());
            return false;
        }

        ZipFile zip = null;
        try {
            zip = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(".gml")) {
                    Log.d(TAG, "Matched archived GMLfile: "
                            + file.getAbsolutePath());
                    return true;
                }
            }

        } catch (Exception e) {
            Log.d(TAG,
                    "Failed to find GML content in: " + file.getAbsolutePath(),
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed closing archived GMLfile: "
                                    + file.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(GMLSpatialDb.GML_CONTENT_TYPE,
                GMLSpatialDb.GML_FILE_MIME_TYPE);
    }
}
