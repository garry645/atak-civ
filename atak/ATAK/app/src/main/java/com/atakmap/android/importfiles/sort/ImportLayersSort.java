
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.layers.LayersMapComponent;
import com.atakmap.app.R;
import com.atakmap.lang.Objects;
import com.atakmap.map.formats.cdn.StreamingContentDatasetDescriptorSpi;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.ImageryFileType;
import com.atakmap.map.layer.raster.mbtiles.MBTilesInfo;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * @deprecated use {@link gov.tak.api.importfiles.ImportLayersResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportLayersSort extends ImportResolver {

    public ImportLayersSort(Context context) {
        super(null, "imagery",
                context.getString(R.string.imagery),
                context.getDrawable(R.drawable.ic_menu_maps));
    }

    @Override
    public String getExt() {
        // There are many different extensions, as well as directories without extensions
        // supported, so return null here.
        return null;
    }

    @Override
    public boolean match(File file) {

        // Check file type
        ImageryFileType.AbstractFileType fileType = ImageryFileType
                .getFileType(file);

        if (fileType != null) {
            switch (fileType.getID()) {
                case ImageryFileType.DTED:
                    // DTED is not considered an imagery layer in ATAK
                    return false;
                case ImageryFileType.PDF:
                    if (!ImportGRGSort.isGeoPDF(file))
                        return false;
                    break;
                case ImageryFileType.MBTILES:
                    MBTilesInfo mbTilesInfo = MBTilesInfo.get(file.getPath(),
                            null);
                    if (mbTilesInfo != null
                            && Objects.equals(mbTilesInfo.content, "terrain"))
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

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(
                LayersMapComponent.IMPORTER_CONTENT_TYPE,
                LayersMapComponent.IMPORTER_DEFAULT_MIME_TYPE);
    }
}
