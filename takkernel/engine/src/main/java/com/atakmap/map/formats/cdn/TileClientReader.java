package com.atakmap.map.formats.cdn;

import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientSpi;
import com.atakmap.map.layer.raster.tilematrix.TileMatrixReader;
import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.layer.raster.tilereader.TileReaderSpi2;

import java.io.File;
import java.io.IOException;

final class TileClientReader
{
    public final static TileReaderSpi2 SPI = new TileMatrixReader.ClientSpi("tiles", 1) {
        @Override
        public TileReader create(String uri, TileReaderFactory.Options options) {
            final StreamingTiles content = StreamingTiles.parse(new File(uri), 16*1024);
            if(content == null)
                return null;
            // requires quadtree
            if(!content.isQuadtree)
                return null;
            // XXX - requires bounds match grid

            try {
                final String offlineCachePath = (options != null) ? options.cacheUri : null;
                final TileClientSpi.Options clientOpts = new TileClientSpi.Options();
                clientOpts.proxy = true;
                TileClient client = StreamingTileClient.SPI.create(uri, offlineCachePath, clientOpts);
                if(client == null)
                    return null;
                return new TileMatrixReader(uri, client, new TileReader.AsynchronousIO());
            } catch(IOException e) {
                return null;
            }
        }

        @Override
        public boolean isSupported(String uri) {
            return StreamingTiles.parse(new File(uri), 16*1024) != null;
        }
    };
}
