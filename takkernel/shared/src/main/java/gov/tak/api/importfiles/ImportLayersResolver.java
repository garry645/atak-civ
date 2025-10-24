package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.lang.Objects;
import com.atakmap.map.formats.cdn.StreamingContentDatasetDescriptorSpi;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.ImageryFileType;
import com.atakmap.map.layer.raster.gdal.GdalDatasetProjection2;
import com.atakmap.map.layer.raster.mbtiles.MBTilesInfo;

import org.gdal.gdal.Dataset;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import gov.tak.api.commons.graphics.Drawable;

public class ImportLayersResolver extends ImportResolver {

    public ImportLayersResolver(String displayName, File destinationDir, Drawable icon) {
        super(null, destinationDir, displayName, icon);
        contentType = "External Native Data";
        mimeType = "application/octet-stream";
    }

    @Override
    public String getExt() {
        // There are many different extensions, as well as directories without extensions
        // supported, so return null here.
        return null;
    }

    private static boolean checkDataset(Dataset dataset) {
        return dataset != null && !FileSystemUtils.isEmpty(dataset.GetProjectionRef()) &&
                GdalDatasetProjection2.getInstance(dataset) != null;
    }

    private static boolean isGeoPDF(File file) {
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
    public boolean match(File file) {

        // Check file type
        ImageryFileType.AbstractFileType fileType = ImageryFileType
                .getFileType(file);

        if(fileType != null) {
            switch(fileType.getID()) {
                case ImageryFileType.DTED :
                    // DTED is not considered an imagery layer
                    return false;
                case ImageryFileType.PDF:
                    if(!isGeoPDF(file))
                        return false;
                    break;
                case ImageryFileType.MBTILES:
                    MBTilesInfo mbTilesInfo = MBTilesInfo.get(file.getPath(), null);
                    if (mbTilesInfo != null && Objects.equals(mbTilesInfo.content, "terrain"))
                        return false;
                    // XXX - check if marked as overlay
                    break;
                case ImageryFileType.GPKG:
                    GeoPackage gpkg = new GeoPackage(file, true);
                    boolean hasImageryTiles = false;
                    for(GeoPackage.ContentsRow content : gpkg.getPackageContents()) {
                        hasImageryTiles |= (content.data_type == GeoPackage.TableType.TILES);
                    }
                    if (!hasImageryTiles)
                        return false;
                    // XXX - check if marked as overlay
                    break;

            }
        }

        final StreamingTiles tiles = StreamingTiles.parse(file, 16 * 1024);
        if (tiles != null &&
                tiles.overlay &&
                StreamingContentDatasetDescriptorSpi.INSTANCE
                        .create(new DatasetDescriptorSpiArgs(
                                file, null)) != null) {

            return false;
        }

        // Check if any of the imagery SPIs support
        return DatasetDescriptorFactory2.isSupported(file);
    }

    @Override
    public boolean directoriesSupported() {
        return true;
    }
}
