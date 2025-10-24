package com.atakmap.map.elevation;

import android.graphics.Bitmap;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.WebMercatorProjection;
import com.atakmap.spatial.GeometryTransformer;

import java.nio.ByteBuffer;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use {@link com.atakmap.map.elevation.TileMatrixElevationSource} */
@Deprecated
@DeprecatedApi(since = "5.0", forRemoval = true, removeAt = "5.2")
public abstract class TiledElevationSource extends ElevationSource
{
    public enum TileScheme
    {
        /**
         * Google/OSM mercator based quadtree tiling scheme
         */
        WebMercator,
        /**
         * WGS84 based tiling scheme, with each hemisphere divided into its own quadtree
         */
        Flat,
        /**
         * WGS84 based quadtree tiling scheme, with level 0 bounds[-180,-180:180,180]
         */
        FlatQuad,
        /**
         * Custom defined tiling scheme
         */
        Custom,
    }
    private TileScheme scheme;
    private TileMatrixElevationSource impl;

    protected TiledElevationSource(String name,
                                   int tileGridSrid,
                                   Envelope bounds,
                                   double tileGridOriginX,
                                   double tileGridOriginY,
                                   TileScheme scheme,
                                   TileMatrix.ZoomLevel[] zoomLevels,
                                   boolean authoritative)
    {
        final TileMatrix matrix = new TilesImpl(
                this,
                name,
                tileGridSrid,
                bounds,
                tileGridOriginX,
                tileGridOriginY,
                zoomLevels);
        impl = new TileMatrixElevationSource(matrix, new ElevationChunkSpi() {
            @Override
            public ElevationChunk create(ByteBuffer data, Hints hints) {
                return getTerrainTile(hints.tileIndex.z, hints.tileIndex.x, hints.tileIndex.y);
            }

            @Override
            public String getMimeType() {
                return null;
            }
        }, authoritative);
        this.scheme = scheme;
    }

    public abstract ElevationChunk getTerrainTile(int zoom, int x, int y);

    /**
     * Returns the name of the tiled content.
     *
     * @return The name of the tilted content.
     */
    @Override
    public String getName()
    {
        return this.impl.getName();
    }

    /**
     * Returns the definition of the zoom levels associated with the content.
     *
     * @return The definition of the zoom levels associated with the content.
     */
    public TileMatrix.ZoomLevel[] getZoomLevel()
    {
        return this.impl.getZoomLevel();
    }

    public TileScheme getTileScheme()
    {
        return this.scheme;
    }

    public boolean isAuthoritative()
    {
        return this.impl.isAuthoritative();
    }

    public double getElevation(double latitude, double longitude, String[] type)
    {
        return this.impl.getElevation(latitude, longitude, type);
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
        return this.impl.getBounds();
    }

    @Override
    public Cursor query(QueryParameters params)
    {
        return new CursorImpl(params);
    }

    /*************************************************************************/

    public static class Factory
    {
        public static interface TileFetcher
        {
            public ElevationChunk get(int zoom, int x, int y);
        }

        public static TiledElevationSource createTerrainSource(String name,
                                                               TileScheme scheme,
                                                               boolean authoritative,
                                                               int minZoom,
                                                               int maxZoom,
                                                               final TileFetcher fetcher)
        {

            return createTerrainSource(name, scheme, authoritative, minZoom, maxZoom, null, fetcher);
        }

        /**
         * @param name          The name of the source
         * @param scheme        The tiling scheme
         * @param authoritative <code>true</code> if the elevation source data is authoritative,
         *                      <code>false</code> otherwise
         * @param minZoom       The minimum tile zoom level present in the source data
         * @param maxZoom       The maximum tile zoom level present in the source data
         * @param dataBounds    Specifies the data containing region in the tile grid, expressed
         *                      as x=longitude, y=latitude.
         * @param fetcher       The tile fetcher implementation
         * @return
         */
        public static TiledElevationSource createTerrainSource(String name,
                                                               TileScheme scheme,
                                                               boolean authoritative,
                                                               int minZoom,
                                                               int maxZoom,
                                                               Envelope dataBounds,
                                                               final TileFetcher fetcher)
        {
            TileMatrix.ZoomLevel min;
            Envelope gridBounds;
            int srid;
            switch (scheme)
            {
                case WebMercator:
                    min = new TileMatrix.ZoomLevel();
                    min.level = minZoom;
                    min.resolution = OSMUtils.mapnikTileResolution(minZoom);
                    min.pixelSizeX = min.resolution;
                    min.pixelSizeY = min.resolution;
                    min.tileWidth = 256;
                    min.tileHeight = 256;
                    gridBounds = new Envelope(
                            WebMercatorProjection.INSTANCE.forward(new GeoPoint(OSMUtils.mapnikTileLat(0, 1), OSMUtils.mapnikTileLng(0, 0)), null).x,
                            WebMercatorProjection.INSTANCE.forward(new GeoPoint(OSMUtils.mapnikTileLat(0, 1), OSMUtils.mapnikTileLng(0, 0)), null).y,
                            0d,
                            WebMercatorProjection.INSTANCE.forward(new GeoPoint(OSMUtils.mapnikTileLat(0, 0), OSMUtils.mapnikTileLng(0, 1)), null).x,
                            WebMercatorProjection.INSTANCE.forward(new GeoPoint(OSMUtils.mapnikTileLat(0, 0), OSMUtils.mapnikTileLng(0, 1)), null).y,
                            0d);
                    srid = 3857;
                    break;
                case Flat:
                    min = new TileMatrix.ZoomLevel();
                    min.level = minZoom;
                    min.resolution = OSMUtils.mapnikTileResolution(minZoom + 1);
                    min.pixelSizeX = 180d / (double) (256 << minZoom);
                    min.pixelSizeY = 180d / (double) (256 << minZoom);
                    min.tileWidth = 256;
                    min.tileHeight = 256;
                    gridBounds = new Envelope(-180d, -90d, 0d, 180d, 90d, 0d);
                    srid = 4326;
                    break;
                case FlatQuad:
                    min = new TileMatrix.ZoomLevel();
                    min.level = minZoom;
                    min.resolution = OSMUtils.mapnikTileResolution(minZoom);
                    min.pixelSizeX = 360d / (double) (256 << minZoom);
                    min.pixelSizeY = 360d / (double) (256 << minZoom);
                    min.tileWidth = 256;
                    min.tileHeight = 256;
                    gridBounds = new Envelope(-180d, -180d, 0d, 180d, 180d, 0d);
                    srid = 4326;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            if (dataBounds == null)
                dataBounds = gridBounds;
            else if (srid != 4326)
                dataBounds = GeometryTransformer.transform(GeometryFactory.fromEnvelope(dataBounds), 4326, srid).getEnvelope();

            TileMatrix.ZoomLevel[] levels = TileMatrix.Util.createQuadtree(min, maxZoom - minZoom + 1);

            return new TiledElevationSource(name, srid, dataBounds, gridBounds.minX, gridBounds.maxY, scheme, levels, authoritative)
            {
                @Override
                public ElevationChunk getTerrainTile(int zoom, int x, int y)
                {
                    return fetcher.get(zoom, x, y);
                }

                @Override
                public void addOnContentChangedListener(OnContentChangedListener l)
                {
                }

                @Override
                public void removeOnContentChangedListener(OnContentChangedListener l)
                {
                }
            };
        }
    }
    protected class CursorImpl implements Cursor
    {
        ElevationSource.Cursor impl;

        protected CursorImpl(QueryParameters filter)
        {
            impl = TiledElevationSource.this.impl.query(filter);
        }

        @Override
        public ElevationChunk get()
        {
            return impl.get();
        }

        @Override
        public double getResolution()
        {
            return impl.getResolution();
        }

        @Override
        public boolean isAuthoritative()
        {
            return impl.isAuthoritative();
        }

        @Override
        public double getCE()
        {
            return impl.getCE();
        }

        @Override
        public double getLE()
        {
            return impl.getLE();
        }

        @Override
        public String getUri()
        {
            return impl.getUri();
        }

        @Override
        public String getType()
        {
            return impl.getType();
        }

        @Override
        public Geometry getBounds()
        {
            return impl.getBounds();
        }

        @Override
        public int getFlags()
        {
            return impl.getFlags();
        }

        @Override
        public boolean moveToNext()
        {
            return impl.moveToNext();
        }

        @Override
        public void close()
        {
            impl.close();
        }

        @Override
        public boolean isClosed()
        {
            return impl.isClosed();
        }
    }

    final static class TilesImpl implements TileMatrix
    {
        final TiledElevationSource source;
        final String name;
        final int tileGridSrid;
        final Envelope bounds;
        final double tileGridOriginX;
        final double tileGridOriginY;
        final TileMatrix.ZoomLevel[] zoomLevels;

        TilesImpl(TiledElevationSource source,
                   String name,
                   int tileGridSrid,
                   Envelope bounds,
                   double tileGridOriginX,
                   double tileGridOriginY,
                   TileMatrix.ZoomLevel[] zoomLevels)
        {
            this.source = source;
            this.name = name;
            this.tileGridSrid = tileGridSrid;
            this.tileGridOriginX = tileGridOriginX;
            this.tileGridOriginY = tileGridOriginY;
            this.bounds = bounds;
            this.zoomLevels = zoomLevels;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSRID() {
            return tileGridSrid;
        }

        @Override
        public ZoomLevel[] getZoomLevel() {
            return zoomLevels;
        }

        @Override
        public double getOriginX() {
            return tileGridOriginX;
        }

        @Override
        public double getOriginY() {
            return tileGridOriginY;
        }

        @Override
        public Bitmap getTile(int zoom, int x, int y, Throwable[] error) {
            return null;
        }

        @Override
        public byte[] getTileData(int zoom, int x, int y, Throwable[] error) {
            return new byte[0];
        }

        @Override
        public Envelope getBounds() {
            return bounds;
        }

        @Override
        public void dispose() {}
    }
}
