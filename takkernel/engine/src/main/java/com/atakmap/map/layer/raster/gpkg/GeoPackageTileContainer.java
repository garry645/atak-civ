package com.atakmap.map.layer.raster.gpkg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Point;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Objects;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunkSpi;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.elevation.HeightmapSampler;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.gpkg.TileTable;
import com.atakmap.map.gpkg.TileTable.TileMatrixSet;
import com.atakmap.map.gpkg.TileTable.ZoomLevelRow;
import com.atakmap.map.gpkg.extensions.GriddedCoverage;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.raster.controls.ContainerInitializationControl;
import com.atakmap.map.layer.raster.controls.TileMetadataControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerSpi;
import com.atakmap.map.layer.raster.tilematrix.TileEncodeException;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.math.PointD;
import com.atakmap.math.Rectangle;
import com.atakmap.spatial.GeometryTransformer;

public class GeoPackageTileContainer implements TileContainer, Controls
{

    public static final String TAG = "GeoPackageTileContainer";

    public final static TileContainerSpi SPI = new TileContainerSpi()
    {

        @Override
        public String getName()
        {
            return "GeoPackage";
        }

        @Override
        public String getDefaultExtension()
        {
            return ".gpkg";
        }

        @Override
        public TileContainer create(String name, String path, TileMatrix spec)
        {
            // since we are creating, if the file exists delete it to overwrite
            File f = new File(path);
            if (IOProviderFactory.exists(f))
                FileSystemUtils.delete(f);

            // adopt the name from the spec if not defined
            if (name == null)
                name = spec.getName();

            // create a new geopackage at the specified location
            GeoPackage.createNewGeoPackage(path);

            Map<String, Object> metadata = Collections.emptyMap();
            do {
                if(spec == null)
                    break;
                if(!(spec instanceof Controls))
                    break;
                final TilesMetadataControl tilesMetadata = ((Controls)spec).getControl(TilesMetadataControl.class);
                if(tilesMetadata == null)
                    break;
                final Map<String, Object> m = tilesMetadata.getMetadata();
                if(m == null)
                    break;
                metadata = m;

            } while(false);

            final boolean isGriddedCoverage = Objects.equals(metadata.get("content"), "terrain");

            GeoPackage gpkg = null;
            String tableName = sanitizeTableName(name);
            try
            {
                gpkg = new GeoPackage(new File(path), false);

                if(isGriddedCoverage) {
                    final GriddedCoverage coverage = new GriddedCoverage();
                    coverage.tile_matrix_set_name = tableName;
                    if(Objects.equals("integer", metadata.get("2d-gridded-coverage.datatype")) ||
                            Objects.equals("float", metadata.get("2d-gridded-coverage.datatype"))) {

                        coverage.datatype = (String)metadata.get("2d-gridded-coverage.datatype");
                    }
                    switch(coverage.datatype) {
                        case "integer" :
                            coverage.data_null_value = 65535d;
                            break;
                        case "float" :
                            coverage.data_null_value = -9999d;
                            break;
                    }

                    final TileMatrix.ZoomLevel[] zoomLevels = spec.getZoomLevel();
                    GriddedCoverage.insertGriddedCoverage(
                            gpkg,
                            coverage,
                            spec.getSRID(),
                            zoomLevels[0].level,
                            zoomLevels[zoomLevels.length-1].level,
                            spec.getBounds());
                } else {
                    gpkg.insertSRS(spec.getSRID());

                    ZoomLevel[] specZoomLevels = spec.getZoomLevel();
                    Envelope specBounds = spec.getBounds();

                    // compute the maximum row/column in the tile grid for the
                    // lowest resolution zoom level
                    ZoomLevel specMinZoomLevel = specZoomLevels[0];
                    for (int i = 1; i < specZoomLevels.length; i++) {
                        if (specZoomLevels[i].resolution > specMinZoomLevel.resolution)
                            specMinZoomLevel = specZoomLevels[i];
                    }

                    Point maxTile = TileMatrix.Util.getTileIndex(spec,
                            specMinZoomLevel.level,
                            specBounds.maxX,
                            specBounds.minY);

                    PointD lr = TileMatrix.Util.getTilePoint(spec,
                            specMinZoomLevel.level,
                            maxTile.x,
                            maxTile.y,
                            specMinZoomLevel.tileWidth,
                            specMinZoomLevel.tileHeight);

                    // NOTE: bounds on the content row is informative
                    GeoPackage.ContentsRow content = new GeoPackage.ContentsRow();
                    content.data_type = GeoPackage.TableType.TILES;
                    content.description = name;
                    content.identifier = name;
                    content.min_x = null;
                    content.min_y = null;
                    content.max_x = null;
                    content.max_y = null;
                    content.srs_id = spec.getSRID();
                    content.table_name = tableName;

                    TileTable.TileMatrixSet matrix = new TileTable.TileMatrixSet();
                    matrix.table_name = content.table_name;
                    matrix.min_x = spec.getOriginX();
                    matrix.min_y = lr.y;
                    matrix.max_x = lr.x;
                    matrix.max_y = spec.getOriginY();
                    matrix.srs_id = content.srs_id;

                    // generrate the zoom levels

                    TileTable.ZoomLevelRow[] gpkgZoomLevels = new TileTable.ZoomLevelRow[specZoomLevels.length];
                    for (int i = 0; i < specZoomLevels.length; i++) {
                        gpkgZoomLevels[i] = new TileTable.ZoomLevelRow();
                        gpkgZoomLevels[i].zoom_level = specZoomLevels[i].level;
                        gpkgZoomLevels[i].tile_width = specZoomLevels[i].tileWidth;
                        gpkgZoomLevels[i].tile_height = specZoomLevels[i].tileHeight;
                        gpkgZoomLevels[i].pixel_x_size = specZoomLevels[i].pixelSizeX;
                        gpkgZoomLevels[i].pixel_y_size = specZoomLevels[i].pixelSizeY;

                        // REQ 45
                        gpkgZoomLevels[i].matrix_width = (int) Math.round((matrix.max_x - matrix.min_x) / (gpkgZoomLevels[i].tile_width * gpkgZoomLevels[i].pixel_x_size));
                        gpkgZoomLevels[i].matrix_height = (int) Math.round((matrix.max_y - matrix.min_y) / (gpkgZoomLevels[i].tile_height * gpkgZoomLevels[i].pixel_y_size));
                    }

                    gpkg.insertTileTable(content, matrix, gpkgZoomLevels);
                }
            } catch (Throwable t)
            {
                Log.e(TAG, "error", t);
                return null;
            } finally
            {
                gpkg.close();
            }

            try
            {
                return new GeoPackageTileContainer(path, tableName, false);
            } catch (IllegalStateException ise)
            {
                Log.e(TAG, "error loading: " + path + " [" + tableName + "] ", ise);
                return null;
            }
        }

        @Override
        public TileContainer open(String path, TileMatrix spec, boolean readOnly)
        {
            // verify that the file is a geopackage
            if (!GeoPackage.isGeoPackage(path))
                return null;

            // check for the tile table and confirm compatibility
            GeoPackage gpkg = null;
            String tableName = null;
            try
            {
                gpkg = new GeoPackage(new File(path), true);

                // if `spec` is `null`, open the first tiles table; non-`null` `spec` must be specified
                // to access other tables if GPKG contains more than one
                if (spec == null) {
                    for(GeoPackage.ContentsRow content : gpkg.getPackageContents()) {
                        if(content.data_type != GeoPackage.TableType.TILES && content.data_type != GeoPackage.TableType.GRIDDED_COVERAGE)
                            continue;
                        tableName = sanitizeTableName(content.table_name);
                        break;
                    }
                } else if(GeoPackageTileContainer.isCompatible(gpkg, spec)){
                    // find tile table
                    TileTable tiles = findTileTable(gpkg, spec.getName());
                    if (tiles == null)
                        return null;

                    tableName = sanitizeTableName(tiles.getName());
                }
                if(tableName == null)
                    return null;
            } finally
            {
                if (gpkg != null)
                    gpkg.close();
            }

            try
            {
                return new GeoPackageTileContainer(path, tableName, readOnly);
            } catch (IllegalStateException ise)
            {
                Log.e(TAG, "error loading: " + path + " [" + tableName + "] ", ise);
                return null;
            }
        }

        @Override
        public boolean isCompatible(TileMatrix spec)
        {
            // verify content type
            String contentType = "imagery";
            if(spec instanceof Controls) {
                final TilesMetadataControl ctrl = ((Controls)spec).getControl(TilesMetadataControl.class);
                if(ctrl != null) {
                    final Map<String, Object> metadata = ctrl.getMetadata();
                    final Object value = metadata.get("content");
                    if(value instanceof String)
                        contentType = (String)value;
                }
            }
            if(!Objects.equals(contentType, "imagery"))
                return false;

            // geopackage can represent any tile matrix
            return true;
        }
    };

    private String tileTableName;
    private GeoPackage gpkg;
    private TileTable tiles;
    private int srid;
    private ZoomLevel[] zoomLevels;
    private TileMatrixSet matrix;
    private Envelope bounds;
    private Envelope matrixBounds;
    private String identifier;

    private Collection<Object> controls = new HashSet<>();

    private GriddedCoverage coverage;

    public GeoPackageTileContainer(String path, String tileTableName, boolean readOnly)
    {
        this.tileTableName = tileTableName;
        this.identifier = this.tileTableName;

        this.gpkg = new GeoPackage(new File(path), readOnly);
        this.tiles = this.gpkg.getTileTable(tileTableName);
        if (this.tiles == null)
            throw new IllegalStateException("tiles == null");

        this.matrix = this.tiles.getTileMatrixSetInfo();
        if (this.matrix == null)
            throw new IllegalStateException("matrix == null");

        this.srid = GeoPackageLayerInfoSpi.getSRID(this.gpkg, this.matrix);

        final boolean pixelSizeIsDeg = (this.srid == 4326 || this.srid == 4979);

        int[] levelIndices = tiles.getZoomLevels();
        this.zoomLevels = new ZoomLevel[levelIndices.length];
        for (int i = 0; i < levelIndices.length; i++)
        {
            TileTable.ZoomLevelRow lvl = this.tiles.getInfoForZoomLevel(levelIndices[i]);
            if (lvl == null)
                throw new IllegalStateException("level == null for zoom level" + i);

            this.zoomLevels[i] = new ZoomLevel();
            this.zoomLevels[i].level = lvl.zoom_level;
            this.zoomLevels[i].pixelSizeX = lvl.pixel_x_size;
            this.zoomLevels[i].pixelSizeY = lvl.pixel_y_size;
            if (pixelSizeIsDeg)
                this.zoomLevels[i].resolution = lvl.pixel_x_size * 111319d;
            else
                this.zoomLevels[i].resolution = Math.sqrt(lvl.pixel_x_size * lvl.pixel_y_size);
            this.zoomLevels[i].tileWidth = lvl.tile_width;
            this.zoomLevels[i].tileHeight = lvl.tile_height;
        }

        this.matrixBounds = new Envelope(this.matrix.min_x, this.matrix.min_y, 0d, this.matrix.max_x, this.matrix.max_y, 0d);

        for (GeoPackage.ContentsRow contents : this.gpkg.getPackageContents())
        {
            if (contents.data_type != GeoPackage.TableType.TILES)
                continue;
            if (!contents.table_name.equals(this.tileTableName))
                continue;

            if (contents.identifier != null)
                this.identifier = contents.identifier;

            if (contents.min_x == null ||
                    contents.min_y == null ||
                    contents.max_x == null ||
                    contents.max_y == null)
            {

                continue;
            }

            this.bounds = new Envelope();
            this.bounds.minX = contents.min_x;
            this.bounds.minY = contents.min_y;
            this.bounds.minZ = 0d;
            this.bounds.maxX = contents.max_x;
            this.bounds.maxY = contents.max_y;
            this.bounds.maxZ = 0d;

            break;
        }

        this.coverage = GriddedCoverage.getPackageExtension(this.gpkg, this.tileTableName);
        if(this.coverage != null && Objects.equals(this.coverage.field_name, "Height")) {
            this.controls.add(this.coverage);
            this.controls.add(new MetadataControlImpl());
            this.controls.add(new ElevationChunkSpiImpl());
        }

        this.controls.add(this);
    }

    @Override
    public String getName()
    {
        return this.identifier;
    }

    @Override
    public int getSRID()
    {
        return this.srid;
    }

    @Override
    public ZoomLevel[] getZoomLevel()
    {
        return this.zoomLevels;
    }

    @Override
    public double getOriginX()
    {
        return this.matrix.min_x;
    }

    @Override
    public double getOriginY()
    {
        return this.matrix.max_y;
    }

    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
    {
        byte[] data = getTileData(zoom, x, y, error);
        if (data == null)
            return null;
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    @Override
    public byte[] getTileData(int zoom, int x, int y, Throwable[] error)
    {
        if(this.gpkg == null)
            return null;
        try
        {
            return this.tiles.getTile(zoom, x, y);
        } catch (Throwable t)
        {
            if (error != null)
                error[0] = t;
            return null;
        }
    }

    @Override
    public Envelope getBounds()
    {
        if (this.bounds != null)
            return this.bounds;
        else
            return this.matrixBounds;
    }

    @Override
    public synchronized void dispose()
    {
        if(this.gpkg != null) {
            this.gpkg.close();
            this.gpkg = null;
        }
    }

    @Override
    public synchronized boolean isReadOnly()
    {
        return (this.gpkg == null) || this.gpkg.getDatabase().isReadOnly();
    }

    @Override
    public synchronized void setTile(int level, int x, int y, byte[] data, long expiration)
    {
        if(this.gpkg == null)
            return;

        if (this.isReadOnly())
            throw new UnsupportedOperationException();

        this.gpkg.insertTile(tileTableName, level, x, y, data);

        Envelope tileBounds = Util.getTileBounds(this, level, x, y);
        if (this.bounds == null)
        {
            this.bounds = tileBounds;
        } else if (!Rectangle.contains(this.bounds.minX, this.bounds.minY,
                this.bounds.maxX, this.bounds.maxY,
                tileBounds.minX, tileBounds.minY,
                tileBounds.maxX, tileBounds.maxY))
        {

            this.bounds.minX = Math.min(tileBounds.minX, this.bounds.minX);
            this.bounds.minY = Math.min(tileBounds.minY, this.bounds.minY);
            this.bounds.maxX = Math.max(tileBounds.maxX, this.bounds.maxX);
            this.bounds.maxY = Math.max(tileBounds.maxY, this.bounds.maxY);
        } else
        {
            return;
        }

        this.gpkg.updateContentBounds(tileTableName,
                this.bounds.minX, this.bounds.minY,
                this.bounds.maxX, this.bounds.maxY);
    }

    @Override
    public void setTile(int level, int x, int y, Bitmap data, long expiration)
            throws TileEncodeException
    {

        // convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) (data.getWidth() * data.getHeight() * 4 * 0.5));
        if (!data.compress(data.hasAlpha() ? CompressFormat.PNG : CompressFormat.JPEG, 75, bos))
            throw new TileEncodeException();
        this.setTile(level, x, y, bos.toByteArray(), expiration);
    }

    @Override
    public boolean hasTileExpirationMetadata()
    {
        return false;
    }

    @Override
    public long getTileExpiration(int level, int x, int y)
    {
        return -1L;
    }

    @Override
    public <T> T getControl(Class<T> controlClazz) {
        for(Object ctrl : this.controls)
            if(controlClazz.isAssignableFrom(ctrl.getClass()))
                return (T) ctrl;
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls) {
        controls.addAll(this.controls);
    }

    public static String sanitizeTableName(String s)
    {
        s = s.replaceAll("[^a-zA-Z\\d:\\-\\.]", "_");
        while (s.contains("__"))
            s = s.replace("__", "_");
        return s;
    }

    private static TileTable findTileTable(GeoPackage gpkg, String identifier)
    {
        for (GeoPackage.ContentsRow contents : gpkg.getPackageContents())
        {
            if (contents.data_type != GeoPackage.TableType.TILES)
                continue;
            if (contents.identifier != null && contents.identifier.equals(identifier))
                return gpkg.getTileTable(contents.table_name);
        }
        return null;
    }

    private static boolean isCompatible(GeoPackage gpkg, TileMatrix spec) {
        // find tile table
        TileTable tiles = findTileTable(gpkg, spec.getName());
        if (tiles == null)
            return false;

        TileMatrixSet matrix = tiles.getTileMatrixSetInfo();
        if (matrix == null)
            return false;

        // confirm SRID
        if (spec.getSRID() != GeoPackageLayerInfoSpi.getSRID(gpkg, matrix))
            return false;

        // confirm origin
        if (spec.getOriginX() != matrix.min_x || spec.getOriginY() != matrix.max_y)
            return false;

        // confirm zoom levels
        int[] tileZoomLevelIdx = tiles.getZoomLevels();
        for (int i = 0; i < tileZoomLevelIdx.length; i++)
        {
            ZoomLevelRow tileZoomLevel = tiles.getInfoForZoomLevel(tileZoomLevelIdx[i]);
            if (tileZoomLevel == null)
                return false;

            ZoomLevel specZoomLevel = TileMatrix.Util.findZoomLevel(spec, tileZoomLevel.zoom_level);
            if (specZoomLevel == null)
                return false;

            if (tileZoomLevel.tile_width != specZoomLevel.tileWidth)
                return false;
            if (tileZoomLevel.tile_height != specZoomLevel.tileHeight)
                return false;

            // NOTE: small errors with precision may result in the pixel
            // resolutions not being strictly comparable. Compute the
            // tile dimensions in projection units and then verify that
            // any precision error is within one pixel.
            final double gpkgTileWidth = tileZoomLevel.tile_width * tileZoomLevel.pixel_x_size;
            final double gpkgTileHeight = tileZoomLevel.tile_height * tileZoomLevel.pixel_y_size;
            final double specTileWidth = specZoomLevel.tileWidth * specZoomLevel.pixelSizeX;
            final double specTileHeight = specZoomLevel.tileHeight * specZoomLevel.pixelSizeY;

            if (Math.abs(gpkgTileWidth - specTileWidth) > Math.min(tileZoomLevel.pixel_x_size, specZoomLevel.pixelSizeX))
                return false;
            if (Math.abs(gpkgTileHeight - specTileHeight) > Math.min(tileZoomLevel.pixel_y_size, specZoomLevel.pixelSizeY))
                return false;
        }
        return true;
    }

    final class MetadataControlImpl implements TilesMetadataControl, TileMetadataControl {
        @Override
        public Map<String, Object> getTileMetadata(int zoom, int x, int y) {
            GriddedCoverage.TileData tile = coverage.getTileData(zoom, x, y);
            if(tile == null || (tile.scale == 1d && tile.offset == 0d))
                return null;
            Map<String, Object> tileAncillary = new HashMap<>();
            tileAncillary.put("scale", tile.scale);
            tileAncillary.put("offset", tile.offset);
            return tileAncillary;
        }

        @Override
        public Map<String, Object> getMetadata() {
            // identify as terrain when the `2d-gridded-coverage` extension is present
            return Collections.singletonMap("content", "terrain");
        }
    }

    final class ElevationChunkSpiImpl implements ElevationChunkSpi {
        @Override
        public ElevationChunk create(ByteBuffer data, Hints hints) {
            final GriddedCoverage.TileData tile = new GriddedCoverage.TileData();
            if(data.hasArray() && data.position() == 0) {
                tile.data = data.array();
            } else {
                tile.data = new byte[data.remaining()];
                data.get(tile.data);
            }
            if(hints.extras.containsKey("scale"))
                tile.scale = (Double)hints.extras.get("scale");
            if(hints.extras.containsKey("offset"))
                tile.offset = (Double)hints.extras.get("offset");
            final ZoomLevel zoom = getZoomLevel()[hints.tileIndex.z];
            final double[] heightmap = coverage.decodeTile(hints.tileIndex.z, hints.tileIndex.x, hints.tileIndex.y, tile);
            final int chunkSrid = (getSRID() == 4979) ? 4326 : getSRID();
            final Envelope bounds = GeometryTransformer.transform(
                    TileMatrix.Util.getTileBounds(zoom, getOriginX(), getOriginY(), hints.tileIndex.x, hints.tileIndex.y),
                    chunkSrid,
                    4326);

            return ElevationChunk.Factory.create(
                    getName(),
                    (String)null,
                    ElevationData.MODEL_TERRAIN,
                    zoom.resolution,
                    (Polygon)GeometryFactory.fromEnvelope(bounds),
                    Double.NaN, Double.NaN,
                    false,
                    new HeightmapSampler(
                        HeightmapSampler.ElementAccess.Array.from(heightmap, zoom.tileWidth),
                        zoom.tileWidth, zoom.tileHeight,
                        Double.NaN,
                        chunkSrid,
                        new GeoPoint(bounds.maxY, bounds.minX),
                        new GeoPoint(bounds.maxY, bounds.maxX),
                        new GeoPoint(bounds.minY, bounds.maxX),
                        new GeoPoint(bounds.minY, bounds.minX),
                        false,
                        true,
                        null));
        }

        @Override
        public String getMimeType() {
            // internal only
            return null;
        }
    }
}
