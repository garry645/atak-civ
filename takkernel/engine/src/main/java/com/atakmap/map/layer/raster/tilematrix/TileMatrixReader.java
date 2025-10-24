package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.SystemClock;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.AttributionControl;
import com.atakmap.map.layer.feature.control.SpatialFilterControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.TileCacheControl;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.tilepyramid.AbstractTilePyramidTileReader;
import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.layer.raster.tilereader.TileReaderSpi2;
import com.atakmap.util.IntervalMonitor;
import gov.tak.platform.system.SystemUtils;

import java.io.IOException;

public final class TileMatrixReader extends AbstractTilePyramidTileReader
{
    private static final Class<?>[] transferControls = new Class[]
    {
            AttributionControl.class,
            TileClientControl.class,
            SpatialFilterControl.class,
    };

    private final int maxZoom;
    private TileMatrix tiles;
    private TileClientControl control;
    private TileCacheControl cacheControl;
    private final IntervalMonitor refreshMonitor = new IntervalMonitor();
    private long version;

    public TileMatrixReader(String uri, TileMatrix tiles, TileReader.AsynchronousIO asyncIO) throws IOException
    {
        super(uri,
                null,
                asyncIO,
                getMaxZoom(tiles) + 1,
                getMaxZoomWidth(tiles),
                getMaxZoomHeight(tiles),
                tiles.getZoomLevel()[0].tileWidth,
                tiles.getZoomLevel()[0].tileHeight);

        this.tiles = tiles;
        this.maxZoom = getMaxZoom(tiles);

        if(tiles instanceof TileClient)
        {
            final TileClient client = (TileClient)tiles;
            this.control = client.getControl(TileClientControl.class);
            cacheControl = client.getControl(TileCacheControl.class);
            if (cacheControl != null)
            {
                this.registerControl(new TilesControlImpl(cacheControl));
            }

            for (Class<?> c : transferControls)
            {
                Object o = client.getControl(c);
                if (o != null)
                    this.registerControl(o);
            }
        }

        this.version = 0L;
    }

    /**************************************************************************/
    // Tile Reader
    @Override
    public void disposeImpl()
    {
        super.disposeImpl();

        this.tiles.dispose();
        this.tiles = null;
        this.readLock.notify();
    }

    @Override
    protected void cancel()
    {
    }

    @Override
    public long getTileVersion(int level, long tileColumn, long tileRow)
    {
        return this.version;
    }

    @Override
    protected Bitmap getTileImpl(int level, long tileColumn, long tileRow, TileReader.ReadResult[] code)
    {
        return getTileImpl(level, tileColumn, tileRow, null, code);
    }

    @Override
    protected Bitmap getTileImpl(int level, long tileColumn, long tileRow, BitmapFactory.Options opts, TileReader.ReadResult[] code)
    {
        if (level < 0)
            throw new IllegalArgumentException();

        // get the tile data
        final byte[] data = this.tiles.getTileData(this.maxZoom - level, (int) tileColumn, (int) tileRow, null);
        if (data == null)
        {
            code[0] = TileReader.ReadResult.ERROR;
            return null;
        }
        // decode the bitmap using client specified options
        final Bitmap retval = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        code[0] = (retval != null) ? TileReader.ReadResult.SUCCESS : TileReader.ReadResult.ERROR;
        return retval;

    }

    @Override
    public void start()
    {
        // before kicking off a bunch of tile readers, reset the connectivity
        // check in the map source to re-enable downloading if network
        // connectivity was restored
        if(this.tiles instanceof TileClient)
            ((TileClient)this.tiles).checkConnectivity();

        // check the refresh interval
        if (this.control != null && this.refreshMonitor.check(this.control.getCacheAutoRefreshInterval(), SystemClock.uptimeMillis()))
        {
            // mark all tiles as expired
            if (this.cacheControl != null)
                cacheControl.expireTiles(System.currentTimeMillis());
            this.version++;
        }
    }

    /*************************************************************************/

    private static int getMaxZoom(TileMatrix tiles)
    {
        TileMatrix.ZoomLevel[] zoomLevels = tiles.getZoomLevel();
        return zoomLevels[zoomLevels.length-1].level;
    }

    private static long getMaxZoomWidth(TileMatrix tiles)
    {
        TileMatrix.ZoomLevel[] zoomLevels = tiles.getZoomLevel();
        TileMatrix.ZoomLevel zoom0 = zoomLevels[0];
        final Envelope bounds = tiles.getBounds();
        Point x0 = TileMatrix.Util.getTileIndex(tiles, zoom0.level, bounds.minX, (bounds.minY+bounds.maxY)/2d);
        Point x1 = TileMatrix.Util.getTileIndex(tiles, zoom0.level, bounds.maxX-zoom0.pixelSizeX, (bounds.minY+bounds.maxY)/2d);

        final int ntx = (x1.x-x0.x) + 1;
        return ((long)ntx*zoom0.tileWidth)<<(long)(zoomLevels[zoomLevels.length-1].level-zoom0.level);
    }

    private static long getMaxZoomHeight(TileMatrix tiles)
    {
        TileMatrix.ZoomLevel[] zoomLevels = tiles.getZoomLevel();
        TileMatrix.ZoomLevel zoom0 = zoomLevels[0];
        final Envelope bounds = tiles.getBounds();
        Point y0 = TileMatrix.Util.getTileIndex(tiles, zoom0.level,(bounds.minX+bounds.maxX)/2d, bounds.maxY);
        Point y1 = TileMatrix.Util.getTileIndex(tiles, zoom0.level, (bounds.minX+bounds.maxX)/2d, bounds.minY+zoom0.pixelSizeY);

        final int nty = (y1.y-y0.y) + 1;
        return ((long)nty*zoom0.tileHeight)<<(long)(zoomLevels[zoomLevels.length-1].level-zoom0.level);
    }

    /*************************************************************************/

    private class TilesControlImpl implements TileCacheControl, TileCacheControl.OnTileUpdateListener
    {

        final TileCacheControl impl;
        OnTileUpdateListener listener;

        TilesControlImpl(TileCacheControl impl)
        {
            this.impl = impl;
            this.impl.setOnTileUpdateListener(this);
        }

        @Override
        public void onTileUpdated(int level, int x, int y)
        {
            final OnTileUpdateListener l = listener;
            if (l != null)
                l.onTileUpdated(maxZoom - level, x, y);

        }

        @Override
        public void prioritize(GeoPoint p)
        {
            impl.prioritize(p);
        }

        @Override
        public void abort(int level, int x, int y)
        {
            impl.abort(maxZoom - level, x, y);
        }

        @Override
        public boolean isQueued(int level, int x, int y)
        {
            return impl.isQueued(maxZoom - level, x, y);
        }

        @Override
        public void setOnTileUpdateListener(OnTileUpdateListener l)
        {
            listener = l;
        }

        @Override
        public void expireTiles(long expiry)
        {
            impl.expireTiles(expiry);
        }
    }

    static abstract class AbstractSpi implements TileReaderSpi2 {
        private final String _name;
        private final int _priority;

        public AbstractSpi(String name, int priority) {
            _name = name;
            _priority = priority;
        }
        @Override
        public final int getPriority() {
            return _priority;
        }

        @Override
        public final String getName() {
            return _name;
        }
    }

    public static class ClientSpi extends AbstractSpi {
        public ClientSpi(String name, int priority) {
            super(name, priority);
        }

        @Override
        public TileReader create(String uri, TileReaderFactory.Options options) {
            TileMatrix tiles = null;
            try
            {
                final String offlineCachePath = (options != null) ? options.cacheUri : null;
                final TileClientSpi.Options clientOpts = new TileClientSpi.Options();
                clientOpts.proxy = true;
                tiles = TileClientFactory.create(uri, offlineCachePath, clientOpts);
                if (!TileMatrix.Util.isQuadtreeable(tiles))
                    return null;
                final TileReader reader = new TileMatrixReader(uri, tiles, new TileReader.AsynchronousIO());
                tiles = null;
                return reader;
            } catch(Throwable t) {
                return null;
            } finally {
                if(tiles != null)
                    tiles.dispose();
            }
        }

        @Override
        public boolean isSupported(String uri) {
            TileMatrix tiles = null;
            try
            {
                tiles = TileClientFactory.create(uri, null, null);
                return (tiles != null);
            } catch(Throwable t) {
                return false;
            } finally {
                if(tiles != null)
                    tiles.dispose();
            }
        }
    }

    public static class ContainerSpi extends AbstractSpi {
        public ContainerSpi(String name, int priority) {
            super(name, priority);
        }

        @Override
        public TileReader create(String uri, TileReaderFactory.Options options) {
            TileMatrix tiles = null;
            try
            {
                uri = getContainerPath(uri);
                tiles = TileContainerFactory.open(uri, true, null);
                if (!TileMatrix.Util.isQuadtreeable(tiles))
                    return null;
                final TileReader reader = new TileMatrixReader(uri, tiles, new TileReader.AsynchronousIO());
                tiles = null;
                return reader;
            } catch(Throwable t) {
                return null;
            } finally {
                if(tiles != null)
                    tiles.dispose();
            }
        }

        @Override
        public boolean isSupported(String uri) {
            TileMatrix tiles = null;
            try
            {
                uri = getContainerPath(uri);
                tiles = TileContainerFactory.open(uri, true, null);
                return (tiles != null);
            } catch(Throwable t) {
                return false;
            } finally {
                if(tiles != null)
                    tiles.dispose();
            }
        }

        static String getContainerPath(String uri) {
            if(SystemUtils.isOsWindows()) {
                try {
                    String dbPath = uri.replace("file:/", "");
                    if(dbPath.indexOf('?') > 0)
                        dbPath = dbPath.substring(0, dbPath.indexOf('?'));
                    uri = dbPath;
                } catch(Throwable ignored) {}
            } else if(uri.charAt(0) != '/') {
                try {
                    uri = Uri.parse(uri).getPath();
                } catch(Throwable ignored) {}
            }
            return uri;
        }
    }
}
