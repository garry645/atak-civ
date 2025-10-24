
package com.atakmap.android.tilecapture.reader;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.math.PointD;

/**
 * Read bitmap tiles from a mobac dataset, or really any provider which has
 * a {@link TileClient} implementation
 */
public class MobileTileReader extends DatasetTileReader {

    private final TileMatrix _client;

    public MobileTileReader(TileMatrix client) {
        super(client.getZoomLevel()[0].level,
                client.getZoomLevel().length,
                client.getZoomLevel()[0].tileWidth,
                client.getZoomLevel()[0].tileHeight);
        _client = client;
    }

    @Override
    public void getTilePoint(int level, PointD src, Point dst) {
        dst.x = (int) Math.floor(src.x / ((long) _tileWidth << (long) level));
        dst.y = (int) Math.floor(src.y / ((long) _tileHeight << (long) level));
    }

    @Override
    public void getSourcePoint(int level, int column, int row, PointD dst) {
        dst.x = column * (_tileWidth << level);
        dst.y = row * (_tileHeight << level);
    }

    @Override
    public TileBitmap getTileImpl(int level, int column, int row) {
        Bitmap bmp = _client.getTile(level, column, row, null);
        return bmp != null ? new TileBitmap(bmp, level, column, row) : null;
    }

    @Override
    public void dispose() {
        _client.dispose();
    }

    public TileMatrix getTiles() {
        return _client;
    }
}
