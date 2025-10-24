package com.atakmap.map.formats.cdn;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.raster.AbstractDatasetDescriptorSpi;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpi;
import com.atakmap.map.layer.raster.ImageDatasetDescriptor;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileClientSpi;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.projection.Projection;
import com.atakmap.util.ConfigOptions;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class StreamingContentDatasetDescriptorSpi extends AbstractDatasetDescriptorSpi
{
    static
    {
        MobileImageryRasterLayer2.registerDatasetType("tiles");

        TileClientFactory.registerSpi(StreamingTileClient.SPI);
        TileReaderFactory.registerSpi(TileClientReader.SPI);
    }

    public final static DatasetDescriptorSpi INSTANCE = new StreamingContentDatasetDescriptorSpi();

    public final static String TYPE = "tak-cdn";

    public StreamingContentDatasetDescriptorSpi() {
        super(TYPE, 1);
    }

    @Override
    protected Set<DatasetDescriptor> create(File file, File workingDir, Callback callback) {
        final StreamingTiles content = StreamingTiles.parse(file, 16*1024);
        if(content == null)
            return null;

        // verify content is renderable
        if(!content.content.equals("imagery") && !content.content.equals("vector"))
            return null;

        if(content.srs == null)
            return null;

        if(!content.srs.matches("EPSG:\\d+"))
            return null;

        final int srid = Integer.parseInt(content.srs.substring(5));
        Projection layerProjection = MobileImageryRasterLayer2.getProjection(srid);
        if (layerProjection == null) {
            return null;
        }

        // XXX - use tile bounds and origin

        double north = layerProjection.getMaxLatitude();
        double west = layerProjection.getMinLongitude();
        double south = layerProjection.getMinLatitude();
        double east = layerProjection.getMaxLongitude();

        // starting with schema v3, validate that the client can be created
        if(content.schema >= 3) {
            TileClientSpi.Options opts = new TileClientSpi.Options();
            TileClient client = null;
            try {
                client = StreamingTileClient.SPI.create(file.getAbsolutePath(), null, opts);
                if (client == null)
                    return null;
            } finally {
                if(client != null)
                    client.dispose();
            }
        }

        Map<String, String> extras = new HashMap<>();

        // default on miss is the working dir IF the dataset is marked as downloadable
        String cacheDirPath = (content.downloadable && workingDir != null) ?
                workingDir.getAbsolutePath() : null;
        // prefer config option paths. If not downloadable, cache must reside in the application
        // private directory
        cacheDirPath = ConfigOptions.getOption(
                        content.downloadable ?
                            "imagery.offline-cache-dir" : "app.dirs.files",
                        cacheDirPath);
        if (cacheDirPath != null) {
            File cacheDir = new File(cacheDirPath);
            if (IOProviderFactory.exists(cacheDir)) {
                final File cacheFile = new File(cacheDir, content.name + ".sqlite");
                extras.put("offlineCache", cacheFile.getAbsolutePath());
            }
        }
        if(content.attribution != null)
            extras.put("attribution", content.attribution);
        if(content.overlay)
            extras.put("overlay", "1");
        if(content.content != null)
            extras.put("contentType", content.content);

        return Collections.singleton(new ImageDatasetDescriptor(
            content.name,
            file.getAbsolutePath(),
            TYPE,
            "tiles",
            content.name,
            0, 0,
            content.tileMatrix[content.tileMatrix.length-1].resolution,
            content.tileMatrix[content.tileMatrix.length-1].level+1,
            new GeoPoint(north, west), new GeoPoint(north, east), new GeoPoint(south, east), new GeoPoint(south, west),
            srid,
            true,
            false,
            workingDir,
            extras
        ));
    }

    @Override
    protected boolean probe(File file, Callback callback) {
        final StreamingTiles tiles = StreamingTiles.parse(file, 8192);
        if(tiles == null)
            return false;
        return tiles.content.equals("imagery") || tiles.content.equals("vector");
    }

    @Override
    public int parseVersion() {
        return 0;
    }
}
