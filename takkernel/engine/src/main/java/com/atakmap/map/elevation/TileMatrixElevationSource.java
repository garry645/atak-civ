package com.atakmap.map.elevation;

import android.graphics.Point;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.control.SpatialFilterControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.raster.controls.TileCacheControl;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.controls.TileMetadataControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.PointD;
import com.atakmap.math.PointI;
import com.atakmap.spatial.GeometryTransformer;
import com.atakmap.util.Collections2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TileMatrixElevationSource extends ElevationSource implements Controls
{
    static final Class<?>[] transferControlTypes = new Class[]
    {
            SpatialFilterControl.class,
            TileClientControl.class,
    };
    private final TileMatrix tiles;
    private final String name;
    private final int tileGridSrid;
    private final Envelope bounds;
    private final double tileGridOriginX;
    private final double tileGridOriginY;
    private final TileMatrix.ZoomLevel[] zoomLevels;
    private final boolean authoritative;
    private final Projection projection;
    private final ElevationChunkSpi chunkSpi;

    private Map<String, Object> metadata;
    private TileMetadataControl tileMetadataControl;
    private final Set<OnContentChangedListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TileMatrixElevationSource(
            TileMatrix tiles,
            String chunkMimeType,
            boolean authoritative)
    {
        this(tiles,
                (chunkMimeType != null) ?
                    new ChunkFactoryProxySpi(chunkMimeType) :
                    null,
             authoritative);
    }

    public TileMatrixElevationSource(
            TileMatrix tiles,
            ElevationChunkSpi chunkSpi,
            boolean authoritative)
    {
        this.tiles = tiles;
        this.chunkSpi = (chunkSpi != null) ? chunkSpi : getChunkSpi(this.tiles);
        this.name = tiles.getName();
        this.tileGridSrid = tiles.getSRID();
        this.bounds = GeometryTransformer.transform(tiles.getBounds(), this.tileGridSrid, 4326);
        this.tileGridOriginX = tiles.getOriginX();
        this.tileGridOriginY = tiles.getOriginY();
        this.zoomLevels = tiles.getZoomLevel();
        this.authoritative = authoritative;
        this.projection = ProjectionFactory.getProjection(this.tileGridSrid);

        // hook up content changed dispatcher based on available controls
        if(this.tiles instanceof Controls) {
            final Controls ctrls = (Controls) this.tiles;
            final TileCacheControl cacheControl = ctrls.getControl(TileCacheControl.class);
            if(cacheControl != null) {
                cacheControl.setOnTileUpdateListener(new TileCacheControl.OnTileUpdateListener() {
                    @Override
                    public void onTileUpdated(int level, int x, int y) {
                        for (OnContentChangedListener l : listeners)
                            l.onContentChanged(TileMatrixElevationSource.this);
                    }
                });
            }

            final TilesMetadataControl metadataControl = ctrls.getControl(TilesMetadataControl.class);
            if(metadataControl != null)
                this.metadata = metadataControl.getMetadata();

            this.tileMetadataControl = ctrls.getControl(TileMetadataControl.class);
        }
    }

    ElevationChunk getTerrainTile(int zoom, int x, int y)
    {
        byte[] tileData = this.tiles.getTileData(zoom, x, y, null);
        if(tileData == null)
            return null;
        final Map<String, Object> tileMetadata = (this.tileMetadataControl != null) ?
                this.tileMetadataControl.getTileMetadata(zoom, x, y) : null;
        ElevationChunkSpi.Hints hints = new ElevationChunkSpi.Hints();
        hints.tileIndex = new PointI(x, y, zoom);
        hints.srid = this.tileGridSrid;
        hints.extras = Collections2.combine(this.metadata, tileMetadata);
        return chunkSpi.create(ByteBuffer.wrap(tileData), hints);
    }

    /**
     * Returns the name of the tiled content.
     *
     * @return The name of the tilted content.
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Returns the definition of the zoom levels associated with the content.
     *
     * @return The definition of the zoom levels associated with the content.
     */
    public TileMatrix.ZoomLevel[] getZoomLevel()
    {
        return this.zoomLevels;
    }

    public boolean isAuthoritative()
    {
        return this.authoritative;
    }

    double getElevation(double latitude, double longitude, String[] type)
    {
        PointD p = this.projection.forward(new GeoPoint(latitude, longitude), null);

        for (int i = this.zoomLevels.length - 1; i >= 0; i--)
        {
            Point tilexy = TileMatrix.Util.getTileIndex(this.tileGridOriginX, this.tileGridOriginY, this.zoomLevels[i], p.x, p.y);
            ElevationChunk tile = null;
            try {
                tile = getTerrainTile(this.zoomLevels[i].level, tilexy.x, tilexy.y);
                if (tile == null)
                    continue;
                final double el = tile.sample(latitude, longitude);
                if (Double.isNaN(el))
                    continue;
                if (type != null)
                    type[0] = tile.getType();
                return el;
            } finally {
                if(tile != null)
                    tile.dispose();
            }
        }

        return Double.NaN;
    }

    /**
     * Returns the approximate bounds of the content, in WGS84 coordinates. The
     * 'x' component shall refer to Longitude and the 'y' component shall refer
     * to Latitude.
     *
     * @return
     */
    @Override
    public Envelope getBounds()
    {
        return this.bounds;
    }

    @Override
    public ElevationSource.Cursor query(ElevationSource.QueryParameters params)
    {
        return new CursorImpl(params);
    }

    @Override
    public void addOnContentChangedListener(OnContentChangedListener l) {
        listeners.add(l);
    }

    @Override
    public void removeOnContentChangedListener(OnContentChangedListener l) {
        listeners.remove(l);
    }

    @Override
    public <T> T getControl(Class<T> controlClazz) {
        if(controlClazz.isAssignableFrom(this.tiles.getClass()))
            return (T) this.tiles;
        if(this.tiles instanceof Controls) {
            for (Class<?> transferControlType : transferControlTypes) {
                if (controlClazz.isAssignableFrom(transferControlType)) {
                    final T ctrl = ((Controls)this.tiles).getControl(controlClazz);
                    if(ctrl != null)
                        return ctrl;
                }
            }
        }
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls) {
        controls.add(this.tiles);
        if(this.tiles instanceof Controls) {
            for (Class<?> transferControlType : transferControlTypes) {
                final Object ctrl = ((Controls)this.tiles).getControl(transferControlType);
                if(ctrl != null && ctrl != this.tiles)
                    controls.add(ctrl);
            }
        }
    }

    static ElevationChunkSpi getChunkSpi(TileMatrix tiles) {
        if(tiles instanceof Controls) {
            Controls ctrls = (Controls)tiles;
            final ElevationChunkSpi tileSpi = ctrls.getControl(ElevationChunkSpi.class);
            if(tileSpi != null)
                return tileSpi;
        }

        return new ChunkFactoryProxySpi(null);
    }

    final static class TileIterator implements Iterator<Point>
    {

        int x;
        int y;
        /**
         * start tile index, inclusive
         */
        final Point start;
        /**
         * end tile index, exclusive
         */
        final Point end;

        TileIterator(int tileGridSrid, double tileGridOriginX, double tileGridOriginY, TileMatrix.ZoomLevel level, Geometry aoi, Envelope bounds)
        {
            Envelope aoib;
            if (aoi == null)
            {
                aoib = bounds;
            } else
            {
                aoib = aoi.getEnvelope();
                aoib.minX = Math.max(bounds.minX, aoib.minX);
                aoib.minY = Math.max(bounds.minY, aoib.minY);
                aoib.maxX = Math.min(bounds.maxX, aoib.maxX);
                aoib.maxY = Math.min(bounds.maxY, aoib.maxY);
            }

            // transform to native SRID
            if (tileGridSrid != 4326)
            {
                aoib = GeometryTransformer.transform(GeometryFactory.fromEnvelope(aoib), 4326, tileGridSrid).getEnvelope();
            }

            start = TileMatrix.Util.getTileIndex(tileGridOriginX, tileGridOriginY, level, aoib.minX, aoib.maxY);
            end = TileMatrix.Util.getTileIndex(tileGridOriginX, tileGridOriginY, level, aoib.maxX, aoib.minY);
            x = start.x;
            y = start.y;

            // make exclusive
            end.x = end.x + 1;
            end.y = end.y + 1;
        }

        @Override
        public boolean hasNext()
        {
            return (y < end.y) && (x < end.x);
        }

        @Override
        public Point next()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            final Point retval = new Point(x++, y);
            if (x == end.x)
            {
                x = start.x;
                y++;
            } else if (x > end.x)
            {
                throw new IllegalStateException();
            }
            return retval;
        }

        @Override
        public void remove()
        {
        }
    }

    protected class CursorImpl implements ElevationSource.Cursor
    {

        ArrayList<TileMatrix.ZoomLevel> levels;
        Iterator<TileMatrix.ZoomLevel> levelIter;
        int zoomLevel;
        Point tile;
        Iterator<Point> tileIter;
        ElevationSource.QueryParameters filter;

        ElevationChunk rowData;

        protected CursorImpl(ElevationSource.QueryParameters filter)
        {
            this.levelIter = null;
            this.zoomLevel = -1;
            this.tile = null;
            this.tileIter = null;
            this.filter = filter;
            this.rowData = null;

            this.levels = new ArrayList<TileMatrix.ZoomLevel>(Arrays.asList(zoomLevels));
            Collections.sort(this.levels, new Comparator<TileMatrix.ZoomLevel>()
            {
                @Override
                public int compare(TileMatrix.ZoomLevel lhs, TileMatrix.ZoomLevel rhs)
                {
                    return rhs.level - lhs.level;
                }
            });

            // cap to minimum resolution
            if (filter != null && !Double.isNaN(filter.minResolution))
            {
                while (!this.levels.isEmpty())
                {
                    if (this.levels.get(this.levels.size() - 1).resolution > filter.minResolution)
                        this.levels.remove(this.levels.size() - 1);
                    else
                        break;
                }
            }
            // cap to maximum resolution
            double maxResolution = filter.maxResolution;
            // use `targetResolution` if specified and it is more restrictive than `maxResolution`
            if(Double.isNaN(maxResolution) || (!Double.isNaN(filter.maxResolution) && filter.targetResolution > filter.maxResolution)) {
                // include at least zoom 0 when `targetResolution` is specified
                maxResolution = Math.min(filter.targetResolution, levels.get(0).resolution);
            }
            if (filter != null && !Double.isNaN(maxResolution))
            {
                while (!this.levels.isEmpty())
                {
                    if (this.levels.get(0).resolution < maxResolution)
                        this.levels.remove(0);
                    else
                        break;
                }
            }

            // if ascending, reverse
            if (filter != null && filter.order != null)
            {
                for (ElevationSource.QueryParameters.Order order : filter.order)
                {
                    switch (order)
                    {
                        case ResolutionAsc:
                        case ResolutionDesc:
                            break;
                        case CEAsc:
                        case CEDesc:
                        case LEAsc:
                        case LEDesc:
                        default:
                            continue;
                    }
                    if (order == ElevationSource.QueryParameters.Order.ResolutionAsc)
                        Collections.reverse(this.levels);
                    break;
                }
            }
        }

        @Override
        public ElevationChunk get()
        {
            if(this.rowData == null)
                this.rowData = getTerrainTile(zoomLevel, tile.x, tile.y);
            return this.rowData;
        }

        @Override
        public double getResolution()
        {
            return get().getResolution();
        }

        @Override
        public boolean isAuthoritative()
        {
            return get().isAuthoritative();
        }

        @Override
        public double getCE()
        {
            return get().getCE();
        }

        @Override
        public double getLE()
        {
            return get().getLE();
        }

        @Override
        public String getUri()
        {
            return get().getUri();
        }

        @Override
        public String getType()
        {
            return get().getType();
        }

        @Override
        public Geometry getBounds()
        {
            return get().getBounds();
        }

        @Override
        public int getFlags()
        {
            return get().getFlags();
        }

        @Override
        public boolean moveToNext()
        {
            if(this.rowData != null) {
                this.rowData.dispose();
                this.rowData = null;
            }
            if (this.levelIter == null)
            {
                if (this.levels.isEmpty())
                    return false;
                this.levelIter = this.levels.iterator();
                TileMatrix.ZoomLevel level = this.levelIter.next();
                this.zoomLevel = level.level;
                TileIterator ti = new TileIterator(
                        tileGridSrid,
                        tileGridOriginX,
                        tileGridOriginY,
                        level,
                        this.filter != null ? this.filter.spatialFilter : null,
                        bounds);
                final int dx = (ti.end.x-ti.start.x);
                final int dy = (ti.end.y-ti.start.y);
                this.tileIter = dx > 10 || dy > 10 || (dx*dy > 64) ? Collections2.emptyIterator() : ti;
            }
            do
            {
                while (!this.tileIter.hasNext())
                {
                    if (!this.levelIter.hasNext())
                        return false;
                    TileMatrix.ZoomLevel level = this.levelIter.next();
                    this.zoomLevel = level.level;
                    TileIterator ti = new TileIterator(
                            tileGridSrid,
                            tileGridOriginX,
                            tileGridOriginY,
                            level,
                            this.filter != null ? this.filter.spatialFilter : null,
                            bounds);
                    final int dx = (ti.end.x-ti.start.x);
                    final int dy = (ti.end.y-ti.start.y);
                    this.tileIter = dx > 10 || dy > 10 || (dx*dy > 64) ? Collections2.emptyIterator() : ti;
                    continue;
                }

                this.tile = this.tileIter.next();
                if (this.get() == null)
                    continue;
                if (!accept(this, this.filter))
                    continue;

                return true;
            } while (true);
        }

        @Override
        public void close()
        {
            if(this.rowData != null) {
                this.rowData.dispose();
                this.rowData = null;
            }
        }

        @Override
        public boolean isClosed()
        {
            return false;
        }
    }

    final static class ChunkFactoryProxySpi implements ElevationChunkSpi {
        final String mimeType;

        ChunkFactoryProxySpi(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public ElevationChunk create(ByteBuffer data, ElevationChunkSpi.Hints hints) {
            return ElevationChunk.Factory.create(data, hints, mimeType);
        }

        @Override
        public String getMimeType() {
            return null;
        }
    };
}
