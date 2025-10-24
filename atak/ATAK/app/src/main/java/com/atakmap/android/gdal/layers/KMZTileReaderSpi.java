
package com.atakmap.android.gdal.layers;

import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.layer.raster.tilereader.TileReaderSpi2;

import org.gdal.gdal.Dataset;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * @deprecated use {@link com.atakmap.map.formats.kmz.KMZTileReaderSpi}
 */
@Deprecated
@DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
public final class KMZTileReaderSpi implements TileReaderSpi2 {

    private final static String TAG = "KMZTileReaderSpi";

    public final static TileReaderSpi2 INSTANCE = new KMZTileReaderSpi();

    private final static com.atakmap.map.formats.kmz.KMZTileReaderSpi _impl = (com.atakmap.map.formats.kmz.KMZTileReaderSpi)com.atakmap.map.formats.kmz.KMZTileReaderSpi.INSTANCE;

    private KMZTileReaderSpi() {
    }

    @Override
    public int getPriority() {
        return _impl.getPriority();
    }

    @Override
    public String getName() {
        return _impl.getName();
    }

    @Override
    public TileReader create(String uri,
            TileReaderFactory.Options options) {
        return _impl.create(uri, options);
    }

    @Override
    public boolean isSupported(String uri) {
        return _impl.isSupported(uri);
    }
}
