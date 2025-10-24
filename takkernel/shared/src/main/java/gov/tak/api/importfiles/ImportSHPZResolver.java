package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports archived Shapefiles
 */
public class ImportSHPZResolver extends ImportResolver {

    private static final String TAG = "ImportSHPZSort";

    public ImportSHPZResolver(String displayName, File destinationDir, Drawable icon) {
        super(".zip", destinationDir, displayName, icon);
        contentType = "Shapefile";
        mimeType = "application/octet-stream";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it contains a .shp
        return HasSHP(file);
    }

    /**
     * Search for a zip entry ending in .shp .shx .dbf
     *
     * @param file the file
     * @return {@code true} if it has a corresponding shape file structure.
     */
    public static boolean HasSHP(File file) {
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

            boolean bSHP = false, bSHX = false, bDBF = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(".shp")) {
                    final InputStream zis = zip.getInputStream(ze);
                    try {
                        if (ImportSHPResolver.isShp(zis)) {
                            bSHP = true;
                        } else {
                            Log.w(TAG,
                                    "Found invalid archived SHP file: "
                                            + ze.getName());
                        }
                    } finally {
                        zis.close();
                    }
                } else if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(".shx")) {
                    bSHX = true;
                } else if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(".dbf")) {
                    bDBF = true;
                }

                if (bSHP && bSHX && bDBF) {
                    // found what we needed, quit looping
                    break;
                }
            }

            if (!bSHP || !bSHX || !bDBF) {
                Log.w(TAG,
                        "Invalid archived Shapefile: "
                                + file.getAbsolutePath());
                return false;
            }

            Log.d(TAG, "Matched archived Shapefile: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.d(TAG,
                    "Failed to find SHP content in: " + file.getAbsolutePath(),
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed closing archived Shapefile: "
                                    + file.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }
}
