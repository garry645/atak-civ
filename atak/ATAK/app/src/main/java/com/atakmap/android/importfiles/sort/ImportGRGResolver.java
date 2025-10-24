
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.grg.GRGMapComponent;
import com.atakmap.android.grg.MCIAGRGLayerInfoSpi;
import com.atakmap.android.util.ResUtils;
import com.atakmap.app.R;
import com.atakmap.app.system.ResourceUtil;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.lang.Objects;
import com.atakmap.map.formats.cdn.StreamingContentDatasetDescriptorSpi;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.formats.kmz.KmzLayerInfoSpi;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.ImageryFileType;
import com.atakmap.map.layer.raster.gdal.GdalDatasetProjection2;
import com.atakmap.map.layer.raster.mbtiles.MBTilesInfo;
import com.atakmap.util.zip.IoUtils;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import org.gdal.gdal.Dataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gov.tak.api.importfiles.ImportResolver;

/**
 * Sorter class that handles GRG files and directories.
 */
public class ImportGRGResolver extends gov.tak.api.importfiles.ImportResolver {

    private static final String TAG = "ImportGRGSort";

    // Max Length 100MB
    private static final int MAX_GDAL_LENGTH = 100 * 1024 * 1024;

    public ImportGRGResolver(Context context) {
        super(null, FileSystemUtils.getItem("grg"),
                ResourceUtil.getString(context, R.string.civ_grg_file,
                        R.string.grg_file),
                ResUtils.getDrawable(context, R.drawable.ic_overlay_gridlines));
    }

    // This method currently handles four types of files: Small GeoTiff files,
    // KML/Z files that are properly formatted, small NITF files, and MCIAGRG style directories.
    @Override
    public boolean match(File file) {
        if (IOProviderFactory.isDirectory(file)) {
            if (MCIAGRGLayerInfoSpi.isMCIAGRG(file)) {
                return true;
            }
        } else {

            final String fname = file.getName();

            // allow for cognizant naming of a file to expose the fact that you may want it to
            // be rendered as an overlay on other map sources.
            if (fname.toLowerCase(LocaleUtil.getCurrent())
                    .endsWith(".ovr.sqlite"))
                return true;

            final StreamingTiles tiles = StreamingTiles.parse(file, 16 * 1024);
            if (tiles != null &&
                    tiles.overlay &&
                    StreamingContentDatasetDescriptorSpi.INSTANCE
                            .create(new DatasetDescriptorSpiArgs(
                                    file, null)) != null) {

                return true;
            }

            ImageryFileType.AbstractFileType type = ImageryFileType
                    .getFileType(file);

            // Unsupported file extension/mime type
            if (type == null)
                return false;

            // For PDF files, check if this is a GeoPDF
            if (type.getID() == ImageryFileType.PDF && !isGeoPDF(file))
                return false;

            // Check if path matches
            String path = type.getPath(file);
            if ("grg".equals(path)) {
                return true;
            }

            // If the file is a small nitf, it might be a GRG.
            if (type.getID() == ImageryFileType.GDAL &&
                    IOProviderFactory.length(file) < MAX_GDAL_LENGTH) {
                return true;
            }

            // The ImageryFileType getFileType method
            // returns "overlays" for KML/KMZ files.
            if (FileSystemUtils.OVERLAYS_DIRECTORY.equals(path)) {
                ZipFile zipFile = null;

                try {
                    if (FileSystemUtils.isZip(file)) {
                        zipFile = new ZipFile(file);
                        if (zipFile != null) {
                            Enumeration<? extends ZipEntry> entries = zipFile
                                    .entries();
                            ZipEntry zipEntry;
                            while (entries.hasMoreElements()
                                    && (zipEntry = entries
                                    .nextElement()) != null) {
                                if (zipEntry.getName()
                                        .toLowerCase(LocaleUtil.getCurrent())
                                        .endsWith(".kml")) {
                                    try (InputStream is = zipFile
                                            .getInputStream(zipEntry)) {
                                        if (KmzLayerInfoSpi.containsTag(is,
                                                "GroundOverlay")) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Failed to open KMZ file", ex);
                } finally {
                    IoUtils.close(zipFile);
                }
            }

            // See if it's an mbtiles file
            if (type.getID() == ImageryFileType.MBTILES) {
                MBTilesInfo mbTilesInfo = MBTilesInfo.get(file.getPath(), null);
                if (mbTilesInfo != null) {
                    return !Objects.equals(mbTilesInfo.content, "terrain");
                }
            }
        }

        return false;
    }

    @Override
    public boolean directoriesSupported() {
        return true;
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(GRGMapComponent.IMPORTER_CONTENT_TYPE,
                GRGMapComponent.IMPORTER_DEFAULT_MIME_TYPE);
    }

    static boolean checkDataset(Dataset dataset) {
        if (dataset != null && !FileSystemUtils.isEmpty(dataset.GetProjectionRef()) &&
                GdalDatasetProjection2.getInstance(dataset) != null)
            return true;
        return false;
    }

    static boolean isGeoPDF(File file) {
        Dataset dataset = null;
        try {
            dataset = GdalLibrary.openDatasetFromFile(file);
            if (checkDataset(dataset))
                return true;

            Hashtable<String, String> subdatasets = dataset
                    .GetMetadata_Dict("SUBDATASETS");
            if (subdatasets == null || subdatasets.isEmpty())
                return false;
            // build the list of subdatasets
            Iterator<Map.Entry<String, String>> subdatasetIter = subdatasets
                    .entrySet().iterator();
            Map.Entry<String, String> entry;
            while (subdatasetIter.hasNext()) {
                entry = subdatasetIter.next();
                if (entry.getKey().matches("SUBDATASET\\_\\d+\\_NAME")) {
                    Dataset subds = null;
                    try {
                        subds = GdalLibrary.openDatasetFromPath(entry.getValue());
                        if (checkDataset(subds))
                            return true;
                    } finally {
                        if (subds != null)
                            subds.delete();
                    }

                }
            }
            return false;

        } catch (Exception ignored) {
            return false;
        } finally {
            if (dataset != null)
                dataset.delete();
        }
    }

    @Override
    public void filterFoundResolvers(List<ImportResolver> importResolvers, File file) {

        // increase the priority of this sorter vs all of the others that could be
        // utilized
        if (importResolvers.remove(this)) {
            // shuffle to the front
            importResolvers.add(0, this);
        }
    }
}
