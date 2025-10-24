
package com.atakmap.spatial.file;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Enumeration;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Support ingesting ESRI Shapefiles
 */
public class GeoJSONSpatialDb extends OgrSpatialDb {

    private final static FileFilter GEOJSON_FILTER = new FileFilter() {
        @Override
        public boolean accept(File arg0) {
            return (IOProviderFactory.isDirectory(arg0)
                    || arg0.getName().endsWith(".geojson"));
        }

    };

    public final static FeatureDataSource ZIPPED_GEOJSON_DATA_SOURCE = new RecursiveFeatureDataSource(
            "geojson-zipped", "ogr", GEOJSON_FILTER) {
        @Override
        public Content parse(File file) throws IOException {
            if (!(file instanceof ZipVirtualFile)) {
                do {
                    // check if it is a zip file
                    if (FileSystemUtils.isZipPath(file)) {
                        try {
                            // to to create, if we fail, we'll drop through to return null
                            file = new ZipVirtualFile(file);
                            break;
                        } catch (Throwable ignored) {
                        }
                        return null;
                    }
                } while (false);
            }
            return super.parse(file);
        }
    };

    public static final String TAG = "GeoJSONSpatialDb";

    public static final String GEOJSON_CONTENT_TYPE = "GeoJSON";
    public static final String GEOJSON_FILE_MIME_TYPE = "application/octet-stream";
    private static final String GROUP_NAME = "GeoJSON";
    private static final String ICON_PATH = "asset://icons/esri.png";
    public static final int GEOJSON_FILE_ICON_ID = R.drawable.ic_esri_file_notification_icon;
    public static final String GEOJSON_TYPE = "GeoJSON";

    /** @deprecated use {@link #GeoJSONSpatialDb(FeatureDataStore2)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public GeoJSONSpatialDb(DataSourceFeatureDataStore spatialDb) {
        super(spatialDb, GROUP_NAME, ICON_PATH, GEOJSON_TYPE);
    }

    public GeoJSONSpatialDb(FeatureDataStore2 spatialDb) {
        super(spatialDb, GROUP_NAME, ICON_PATH, GEOJSON_TYPE);
    }

    @Override
    public String getFileDirectoryName() {
        return FileSystemUtils.OVERLAYS_DIRECTORY;
    }

    @Override
    public int getIconId() {
        return GEOJSON_FILE_ICON_ID;
    }

    @Override
    public int processAccept(File file, int depth) {
        if (IOProviderFactory.isFile(file) && IOProviderFactory.canRead(file)) {
            String lc = file.getName().toLowerCase(LocaleUtil.getCurrent());
            if (lc.endsWith(".geojson"))
                return PROCESS_ACCEPT;
            else if (lc.endsWith(".zip") && containsGeoJSONFile(file))
                return PROCESS_ACCEPT;
        } else if (file.isDirectory()) {
            return PROCESS_RECURSE;
        }

        return PROCESS_REJECT;
    }

    /**
     * Determine if the zip file actually contains a .geojson file before trying to
     * classify it as a shape file.   Not all zip files in the overlay directory
     * are shape files.
     */
    private boolean containsGeoJSONFile(final File f) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(f);
            Enumeration zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                String fileName = ((ZipEntry) zipEntries.nextElement())
                        .getName();
                String lc = fileName.toLowerCase(LocaleUtil.getCurrent());
                if (lc.endsWith(".geojson"))
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "error processing: " + f);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
        return false;
    }

    @Override
    public String getFileMimeType() {
        return GEOJSON_FILE_MIME_TYPE;
    }

    @Override
    public String getContentType() {
        return GEOJSON_CONTENT_TYPE;
    }

    @Override
    public boolean processFile(File file) {
        if (FileSystemUtils.checkExtension(file, "zip"))
            try {
                file = new ZipVirtualFile(file);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
        return super.processFile(file);
    }

    @Override
    protected String getProviderHint(File file) {
        if (file instanceof ZipVirtualFile) {
            return ZIPPED_GEOJSON_DATA_SOURCE.getName();
        } else
            return super.getProviderHint(file);
    }
}
