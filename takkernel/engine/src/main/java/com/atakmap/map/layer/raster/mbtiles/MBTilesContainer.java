package com.atakmap.map.layer.raster.mbtiles;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.DatabaseInformation;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.map.formats.mapbox.TerrainRGBChunkSpi;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.ContainerInitializationControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.osm.OSMWebMercator;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerSpi;
import com.atakmap.map.layer.raster.tilematrix.TileEncodeException;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.Projection;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.math.PointI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.util.Disposable;

public final class MBTilesContainer implements TileContainer, Controls, TilesMetadataControl
{
    /** schema spis; prioritized high to low */
    final static List<DbSchema.Spi> _schemaSpis = new ArrayList<>();
    static {
        _schemaSpis.add(OpenMapTilerDbSchema.SPI);
        _schemaSpis.add(DefaultDbSchema.SPI);
    }

    private static DbSchema ds = new DefaultDbSchema();
    private static DbSchema.Spi _defaultSchema = DefaultDbSchema.SPI;

    private final static int MAX_NUM_LEVELS = 33;

    private final static TileMatrix.ZoomLevel[] MBTILES_ZOOM_LEVELS_3857 = TileMatrix.Util.createQuadtree(createLevel0(OSMWebMercator.INSTANCE, 1, 1), MAX_NUM_LEVELS);

    private final static Envelope BOUNDS_3857 = getMatrixBounds(OSMWebMercator.INSTANCE);

    public final static TileContainerSpi SPI = new TileContainerSpi()
    {
        @Override
        public String getName()
        {
            return "MBTiles";
        }

        @Override
        public String getDefaultExtension()
        {
            return ".mbtiles";
        }

        @Override
        public TileContainer create(String name, String path, TileMatrix spec)
        {
            // verify compatibility
            if (!isCompatible(spec))
                return null;

            // since we are creating, if the file exists delete it to overwrite
            File f = new File(path);
            if (IOProviderFactory.exists(f))
                FileSystemUtils.delete(f);

            // adopt the name from the spec if not defined
            if (name == null)
                name = spec.getName();

            DatabaseIface db = null;
            try
            {
                db = IOProviderFactory.createDatabase(f);
                Map<String, Object> metadata = null;
                if(spec instanceof Controls) {
                    final Controls ctrls = (Controls) spec;
                    final TilesMetadataControl config = ctrls.getControl(TilesMetadataControl.class);
                    if (config != null)
                        metadata = config.getMetadata();
                }
                ds.createTables(db, metadata);
                _defaultSchema.newInstance().createTables(db, metadata);
                final TileContainer retval = new MBTilesContainer(
                        f,
                        name,
                        (spec == null) ? 3857 : spec.getSRID(),
                        MAX_NUM_LEVELS,
                        db,
                        true);
                db = null;
                return retval;
            } finally
            {
                if (db != null)
                    db.close();
            }
        }

        @Override
        public TileContainer open(String path, TileMatrix spec, boolean readOnly)
        {
            File f = new File(path);
            if (!IOProviderFactory.exists(f) || !IOProviderFactory.isDatabase(f))
                return null;

            DatabaseIface db = null;
            try {
                db = IOProviderFactory.createDatabase(f, readOnly ? DatabaseInformation.OPTION_READONLY : 0);
                MBTilesInfo info = MBTilesInfo.get(db);
                if (info == null)
                    return null;

                // if a spec is specified, verify compatibility
                if (spec != null) {
                    // ensure spec shares same SRID
                    if (info.srid != spec.getSRID())
                        return null;
                    // check compatibility
                    if (!isCompatible(spec))
                        return null;
                }

                final TileContainer retval = new MBTilesContainer(f, info.name, info.srid, readOnly ? info.maxLevel+1 : MAX_NUM_LEVELS, db, false);
                db = null;
                return retval;
            } catch(Throwable t) {
                t.printStackTrace();
                return null;
            } finally
            {
                if (db != null)
                    db.close();
            }
        }

        @Override
        public boolean isCompatible(TileMatrix spec)
        {
            return MBTilesContainer.isCompatible(spec);
        }
    };

    private static final String TAG = "MBTilesContainer";

    public static int tileWriteBuffer = 0;

    private final File file;
    private final String name;
    final DatabaseIface db;
    private final int srid;
    private final TileMatrix.ZoomLevel[] zoomLevels;
    private Envelope bounds;
    private Map<Integer, Envelope> boundsUpdates = new HashMap<>();
    private final Envelope initBounds;
    private Thread tileWriteThread;
    private boolean disposing;
    private Map<PointI, byte[]> pendingTileWrites = new LinkedHashMap<>();
    private int bufferedTileSize = 0;

    QueryIface queryTileData;

    private ContainerInitializationControl initControl;
    private final Map<String, Object> metadata;

    final DbSchema schema;
    final boolean initContainer;


    MBTilesContainer(File file, String name, int srid, int numLevels, DatabaseIface db, boolean initContainer)
    {
        this.initContainer = initContainer;
        this.file = file;
        if(name ==  null)
            name = this.file.getName();
        this.name = name;
        switch (srid)
        {
            case 900913:
                srid = 3857;
            case 3857:
                if(numLevels == MAX_NUM_LEVELS) {
                    this.zoomLevels = MBTILES_ZOOM_LEVELS_3857;
                } else {
                    this.zoomLevels = new ZoomLevel[numLevels];
                    System.arraycopy(MBTILES_ZOOM_LEVELS_3857, 0, this.zoomLevels, 0, numLevels);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.srid = srid;
        this.db = db;

        DbSchema.Spi spi = _defaultSchema;
        for (DbSchema.Spi s : _schemaSpis) {
            if (s.matches(this.db)) {
                spi = s;
                break;
            }
        }
        schema = spi.newInstance();

        if(initContainer) {
            this.metadata = new HashMap<>();
            initControl = new ContainerInitializationControl() {
                @Override
                public void setMetadata(Map<String, Object> metadata) {
                    DefaultDbSchema.initMetadataValues(db, metadata);
                    MBTilesContainer.this.metadata.putAll(metadata);
                }
            };
        } else {
            this.metadata = new LinkedHashMap<>();
            try(QueryIface query = this.db.compileQuery("SELECT name, value FROM metadata")) {
                while(query.moveToNext()) {
                    Object value = null;
                    switch(query.getType(1)) {
                        case QueryIface.FIELD_TYPE_BLOB:
                            value = query.getBlob(1);
                            break;
                        case QueryIface.FIELD_TYPE_FLOAT:
                            value = Double.valueOf(query.getDouble(1));
                            break;
                        case QueryIface.FIELD_TYPE_INTEGER:
                            value = Integer.valueOf(query.getInt(1));
                            break;
                        case QueryIface.FIELD_TYPE_STRING:
                            value = query.getString(1);
                            break;
                        case QueryIface.FIELD_TYPE_NULL:
                        default :
                            break;
                    }
                    if(value != null) {
                        final String key = query.getString(0);
                        this.metadata.put(key, value);
                    }
                }
            }

            // check for well known metadata values
            if(metadata.containsKey("format")) {
                final Object value = metadata.get("format");
                if(value instanceof String) {
                    switch((String)value) {
                        case "pbf":
                            this.metadata.put("content", "vector");
                            break;
                        case TerrainRGBChunkSpi.MIME_TYPE:
                            this.metadata.put("content", "terrain");
                            break;
                    }
                }
            }

            int minZoom = -1;
            if(metadata.containsKey("minzoom")) {
                final Object value = metadata.get("minzoom");
                if(value instanceof Number)
                    minZoom = ((Number)value).intValue();
                else if(value instanceof String)
                    try {
                        minZoom = Integer.parseInt((String)value);
                    } catch(NumberFormatException ignored) {}
            }
            int maxZoom = -1;
            if(metadata.containsKey("maxzoom")) {
                final Object value = metadata.get("maxzoom");
                if(value instanceof Number)
                    maxZoom = ((Number)value).intValue();
                else if(value instanceof String)
                    try {
                        maxZoom = Integer.parseInt((String)value);
                    } catch(NumberFormatException ignored) {}
            }
            if(metadata.containsKey("bounds")) {
                if(minZoom == -1) {
                    try(CursorIface result = db.query("SELECT zoom_level FROM tiles ORDER BY zoom_level ASC LIMIT 1", null)) {
                        if(result.moveToNext())
                            minZoom = result.getInt(0);
                    }
                }
                if(maxZoom == -1) {
                    try(CursorIface result = db.query("SELECT zoom_level FROM tiles ORDER BY zoom_level DESC LIMIT 1", null)) {
                        if(result.moveToNext())
                            maxZoom = result.getInt(0);
                    }
                }
                final Object value = metadata.get("bounds");
                if(value instanceof String) {
                    final String[] wsen = ((String)value).split(",");
                    if(wsen.length == 4) {
                        try {
                            bounds = new Envelope(
                                    Double.parseDouble(wsen[0].trim()),
                                    Double.parseDouble(wsen[1].trim()),
                                    minZoom,
                                    Double.parseDouble(wsen[2].trim()),
                                    Double.parseDouble(wsen[3].trim()),
                                    maxZoom);
                        } catch(NumberFormatException ignored) {}
                    }
                }
            }
        }

        this.initBounds = (bounds == null) ? null : new Envelope(bounds);

        if(!db.isReadOnly() && tileWriteBuffer > 0) {
            tileWriteThread = new Thread(new TileWriterThread());
            tileWriteThread.setPriority(Thread.NORM_PRIORITY);
            tileWriteThread.setName("MBTiles-async-tile-writer-" + Integer.toString(tileWriteThread.hashCode(), 16));
            tileWriteThread.start();
        }
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
    public String getName()
    {
        return this.name;
    }

    @Override
    public int getSRID()
    {
        return this.srid;
    }

    @Override
    public TileMatrix.ZoomLevel[] getZoomLevel()
    {
        return this.zoomLevels;
    }

    @Override
    public double getOriginX()
    {
        return BOUNDS_3857.minX;
    }

    @Override
    public double getOriginY()
    {
        return BOUNDS_3857.maxY;
    }

    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
    {
        byte[] data = this.getTileData(zoom, x, y, error);
        if (data == null)
            return null;
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    @Override
    public synchronized byte[] getTileData(int zoom, int x, int y, Throwable[] error)
    {
        y = ((1<<zoom)-1)-y;

        try
        {
            if (queryTileData == null)
                queryTileData = this.db.compileQuery("SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1");

            queryTileData.clearBindings();
            queryTileData.bind(1, zoom);
            queryTileData.bind(2, x);
            queryTileData.bind(3, y);

            if (!queryTileData.moveToNext())
                return null;
            return queryTileData.getBlob(0);
        } catch (Throwable t)
        {
            if (error != null)
                error[0] = t;
            return null;
        } finally
        {
            if (queryTileData != null)
            {
                queryTileData.reset();
            }
        }
    }

    @Override
    public boolean isReadOnly()
    {
        return this.db.isReadOnly();
    }

    private Envelope updateBounds(Envelope b, int level, int x, int y) {
        if(b == null) {
            b = new Envelope(
                    OSMUtils.mapnikTileLng(level, x),
                    OSMUtils.mapnikTileLat(level, y+1),
                    level,
                    OSMUtils.mapnikTileLng(level, x+1),
                    OSMUtils.mapnikTileLat(level, y),
                    level);
        } else {
            b.minX = Math.min(b.minX, OSMUtils.mapnikTileLng(level, x));
            b.minY = Math.min(b.minY, OSMUtils.mapnikTileLat(level, y+1));
            b.minZ = Math.min(b.minZ, level);
            b.maxX = Math.max(b.maxX, OSMUtils.mapnikTileLng(level, x+1));
            b.maxY = Math.max(b.maxY, OSMUtils.mapnikTileLat(level, y));
            b.maxZ = Math.max(b.maxZ, level);
        }
        return b;
    }
    @Override
    public synchronized void setTile(int level, int x, int y, byte[] data, long expiration_)
    {
        if (this.isReadOnly())
            throw new UnsupportedOperationException("TileContainer is read-only");

        bounds = updateBounds(bounds, level, x, y);
        Envelope zb = boundsUpdates.get(level);
        if(zb == null)
            boundsUpdates.put(level, updateBounds(zb, level, x, y));
        else
            updateBounds(zb, level, x, y);

        y = ((1 << level) - 1) - y;
        // do not accept further initialization metadata after tile is written
        initControl = null;
        if(tileWriteThread == null) {
            schema.setTile(this.db, level, x, y, data);
        } else {
            pendingTileWrites.put(new PointI(x, y, level), data);
            bufferedTileSize += (data != null) ? data.length : 0;
            notify();
            if(bufferedTileSize >= tileWriteBuffer)
                Thread.yield();
        }
    }

    @Override
    public void setTile(int level, int x, int y, Bitmap data, long expiration) throws TileEncodeException
    {
        // convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) (data.getWidth() * data.getHeight() * 4 * 0.5));
        if (!data.compress(data.hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 75, bos))
            throw new TileEncodeException();
        setTile(level, x, y, bos.toByteArray(), expiration);
    }

    @Override
    public Envelope getBounds()
    {
        return BOUNDS_3857;
    }

    @Override
    public void dispose()
    {
        Thread waitFor;
        synchronized(this) {
            waitFor = tileWriteThread;
            tileWriteThread = null;
            this.disposing = true;
            this.notify();
        }
        if(waitFor != null) {
            try {
                waitFor.join();
            } catch (InterruptedException ignored) { }
        }

        this.schema.dispose();
        if (queryTileData != null) {
            queryTileData.close();
            queryTileData = null;
        }
        if(!db.isReadOnly() && bounds != null) {
            Envelope updateBounds = boundsUpdates.get((int)bounds.maxZ);
            if(updateBounds == null) {
                // there were no updates (at least at max zoom level), maintain the initial bounds
                updateBounds = initBounds;
            } else if(initBounds != null) {
                // update the initial bounds per the max zoom updates
                updateBounds.minX = Math.min(updateBounds.minX, initBounds.minX);
                updateBounds.minY = Math.min(updateBounds.minY, initBounds.minY);
                updateBounds.maxX = Math.max(updateBounds.maxX, initBounds.maxX);
                updateBounds.maxY = Math.max(updateBounds.maxY, initBounds.maxY);
            }

            setMetadata("bounds", String.format("%f,%f,%f,%f", updateBounds.minX, updateBounds.minY, updateBounds.maxX, updateBounds.maxY));
            setMetadata("minzoom", Integer.toString((int)bounds.minZ));
            setMetadata("maxzoom", Integer.toString((int)bounds.maxZ));
        }
        this.db.close();
    }

    void setMetadata(String name, String value) {
        final boolean hasMetadata;
        try(CursorIface result = db.query("SELECT 1 FROM metadata WHERE name = ? LIMIT 1", new String[] {name})) {
            hasMetadata = result.moveToNext();
        }

        db.execute(
                !hasMetadata ?
                    "INSERT INTO metadata (value, name) VALUES(?, ?)" :
                    "UPDATE metadata SET value = ? WHERE name = ?",
                new String[] {value, name});
    }

    @Override
    public <T> T getControl(Class<T> controlClazz) {
        if(File.class.equals(controlClazz))
            return (T) this.file;
        if(ContainerInitializationControl.class.equals(controlClazz))
            return (T) this.initControl;
        if(controlClazz.isAssignableFrom(getClass()))
            return (T) this;
        if(DatabaseIface.class.equals(controlClazz))
            return (T) this.db;
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls) {
        if(this.file != null)
            controls.add(this.file);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return (this.metadata != null) ? Collections.unmodifiableMap(this.metadata) : null;
    }

    final class TileWriterThread implements Runnable {
        @Override
        public void run() {
            while(true) {
                Map<PointI, byte[]> pendingTileWrites = null;
                synchronized(MBTilesContainer.this) {
                    if(disposing && bufferedTileSize == 0) {
                        break;
                    } else if(!disposing && bufferedTileSize < tileWriteBuffer) {
                        try {
                            MBTilesContainer.this.wait();
                        } catch (InterruptedException ignored) { }
                        continue;
                    }
                    pendingTileWrites = MBTilesContainer.this.pendingTileWrites;
                    MBTilesContainer.this.pendingTileWrites = new LinkedHashMap<>();
                    bufferedTileSize = 0;
                }

                db.beginTransaction();
                try {
                    int i = 0;
                    for (Map.Entry<PointI, byte[]> pendingTile : pendingTileWrites.entrySet()) {
                        final PointI tileIndex = pendingTile.getKey();
                        schema.setTile(db, tileIndex.z, tileIndex.x, tileIndex.y, pendingTile.getValue());
                        i++;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    /**************************************************************************/

    private static Envelope getMatrixBounds(Projection proj)
    {
        PointD ul = proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMinLongitude()), null);
        PointD ur = proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMaxLongitude()), null);
        PointD lr = proj.forward(new GeoPoint(proj.getMinLatitude(), proj.getMaxLongitude()), null);
        PointD ll = proj.forward(new GeoPoint(proj.getMinLatitude(), proj.getMinLongitude()), null);

        return new Envelope(MathUtils.min(ul.x, ur.x, lr.x, ll.x),
                MathUtils.min(ul.y, ur.y, lr.y, ll.y),
                0d,
                MathUtils.max(ul.x, ur.x, lr.x, ll.x),
                MathUtils.max(ul.y, ur.y, lr.y, ll.y),
                0);
    }

    private static TileMatrix.ZoomLevel createLevel0(Projection proj, int gridCols, int gridRows)
    {
        PointD upperLeft = proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMinLongitude()), null);
        PointD lowerRight = proj.forward(new GeoPoint(proj.getMinLatitude(), proj.getMaxLongitude()), null);

        // XXX - better resolution for 4326???

        TileMatrix.ZoomLevel retval = new TileMatrix.ZoomLevel();
        retval.level = 0;
        retval.resolution = OSMUtils.mapnikTileResolution(retval.level);
        retval.tileWidth = 256;
        retval.tileHeight = 256;
        retval.pixelSizeX = (lowerRight.x - upperLeft.x) / (retval.tileWidth * gridCols);
        retval.pixelSizeY = (upperLeft.y - lowerRight.y) / (retval.tileHeight * gridRows);
        return retval;
    }

    public static boolean isCompatibleSchema(DatabaseIface db)
    {
        for(DbSchema.Spi spi : _schemaSpis)
            if(spi.matches(db))
                return true;
        return false;
    }

    public static void createTables(DatabaseIface db, String content)
    {
        DbSchema schema = null;
        try {
            schema = _defaultSchema.newInstance();
            schema.createTables(db, (content != null) ? Collections.singletonMap("content", content) : null);
        } finally {
            if(schema != null)
                schema.dispose();
        }
    }

    public static boolean isCompatible(TileMatrix spec)
    {
        // we know how to create a default MBTiles container
        if(spec == null)
            return true;

        // verify compatible SRID and origin
        TileMatrix.ZoomLevel[] zoomLevels;
        Envelope bnds;
        switch (spec.getSRID())
        {
            case 900913:
            case 3857:
                zoomLevels = MBTILES_ZOOM_LEVELS_3857;
                bnds = BOUNDS_3857;
                break;
            default:
                return false;
        }

        // check compatibility of tiling
        TileMatrix.ZoomLevel[] specLevels = spec.getZoomLevel();
        final int limit = zoomLevels.length - 1;
        for (int i = 0; i < specLevels.length; i++)
        {
            // check for out of bounds level
            if (specLevels[i].level < 0 || specLevels[i].level > limit)
                return false;

            // NOTE: resolution is only 'informative' so we aren't going to
            // check it
            TileMatrix.ZoomLevel level = zoomLevels[specLevels[i].level];
            if (specLevels[i].tileWidth != level.tileWidth ||
                    specLevels[i].tileHeight != level.tileHeight)
            {

                return false;
            }

            // ensure pixel sizes are within tolerance of one
            if(Math.abs((specLevels[i].pixelSizeX/level.pixelSizeX*specLevels[i].tileWidth)-level.tileWidth) > 1)
                return false;
            if(Math.abs((specLevels[i].pixelSizeY/level.pixelSizeY*specLevels[i].tileHeight)-level.tileHeight) > 1)
                return false;
        }

        if (Math.abs(spec.getOriginX()-bnds.minX) > specLevels[specLevels.length-1].pixelSizeX || Math.abs(spec.getOriginY()-bnds.maxY) > specLevels[specLevels.length-1].pixelSizeY)
            return false;

        return true;
    }

    public static TileContainer openOrCreate(String path, String name, String content)
    {
        File f = new File(path);
        if (IOProviderFactory.exists(f))
        {
            TileContainer retval = SPI.open(path, null, false);
            if (retval != null)
                return retval;

            FileSystemUtils.delete(f);
        }

        if(name == null)
            name = f.getName();

        DatabaseIface db = null;
        try
        {
            db = IOProviderFactory.createDatabase(f);
            createTables(db, content);
            final TileContainer retval = new MBTilesContainer(f, name, 3857, MAX_NUM_LEVELS, db, true);
            db = null;
            return retval;
        } finally
        {
            if (db != null)
                db.close();
        }
    }

    public static void setTileWriteBufferSize(int size) {
        tileWriteBuffer = size;
    }

    public static void setDefaultSchema(String name) {
        if(name == null) {
            _defaultSchema = DefaultDbSchema.SPI;
        } else {
            for (DbSchema.Spi schema : _schemaSpis)
                if (schema.getName().equals(name))
                    _defaultSchema = schema;
        }
    }
    
    // XXX - R8 is placing these classes in different package when defined at top level. must be
    // retained as nested classes until issue can be further investigated and resolved.

    interface DbSchema extends Disposable {
        void createTables(DatabaseIface db, Map<String, Object> metadata);
        void setTile(DatabaseIface db, int zoom, int x, int y, byte[] data);

        interface Spi {
            String getName();
            boolean matches(DatabaseIface db);
            DbSchema newInstance();
        }
    }
    
    final static class DefaultDbSchema implements DbSchema {
        final static Map<String, Collection<String>> _schema = Collections.singletonMap("tiles", Arrays.asList("tile_data", "zoom_level", "tile_column", "tile_row"));

        final static MBTilesContainer.DbSchema.Spi SPI = new MBTilesContainer.DbSchema.Spi() {
            @Override
            public String getName() {
                return "default";
            }

            @Override
            public boolean matches(DatabaseIface db) {
                return Databases.matchesSchema(db, _schema, false);
            }

            @Override
            public DbSchema newInstance() {
                return new DefaultDbSchema();
            }
        };

        StatementIface insertTile;
        StatementIface updateTile;
        QueryIface queryTileExists;

        DefaultDbSchema() {
        }

        @Override
        public void createTables(DatabaseIface db, Map<String, Object> metadata) {
            Set<String> tables = Databases.getTableNames(db);
            if (!tables.contains("tiles"))
            {
                db.execute("CREATE TABLE tiles (tile_data BLOB, zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER)", null);
                db.execute("CREATE UNIQUE INDEX tile_index on tiles (zoom_level, tile_column, tile_row)", null);
            }
            if (!tables.contains("metadata"))
            {
                db.execute("CREATE TABLE metadata (name TEXT, value TEXT)", null);
                initMetadataValues(db, metadata);
            }
        }

        @Override
        public void setTile(DatabaseIface db,  int zoom, int x, int y, byte[] data) {
            StatementIface stmt = null;
            try
            {
                if (hasTile(db, zoom, x, y))
                {
                    if (updateTile == null)
                        updateTile = db.compileStatement("UPDATE tiles SET tile_data = ? WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?");
                    stmt = updateTile;
                } else
                {
                    if (insertTile == null)
                        insertTile = db.compileStatement("INSERT INTO tiles (tile_data, zoom_level, tile_column, tile_row) VALUES(?, ?, ?, ?)");
                    stmt = insertTile;
                }

                stmt.bind(1, data);
                stmt.bind(2, zoom);
                stmt.bind(3, x);
                stmt.bind(4, y);

                stmt.execute();
            } finally
            {
                if (stmt != null)
                {
                    stmt.clearBindings();
                }
            }
        }

        @Override
        public void dispose() {
            if (insertTile != null) {
                insertTile.close();
                insertTile = null;
            }
            if (updateTile != null) {
                updateTile.close();
                updateTile = null;
            }
            if (queryTileExists != null) {
                queryTileExists.close();
                queryTileExists = null;
            }
        }

        private boolean hasTile(DatabaseIface db, int level, int x, int y)
        {
            try
            {
                if (queryTileExists == null)
                    queryTileExists = db.compileQuery("SELECT 1 FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1");
                queryTileExists.clearBindings();
                queryTileExists.bind(1, level);
                queryTileExists.bind(2, x);
                queryTileExists.bind(3, y);
                return queryTileExists.moveToNext();
            } finally
            {
                if (queryTileExists != null)
                {
                    queryTileExists.reset();
                }
            }
        }

        static void initMetadataValues(DatabaseIface db, Map<String, Object> metadata) {
            metadata = (metadata == null) ? Collections.singletonMap("format", "png") : new LinkedHashMap<>(metadata);
            if(!metadata.containsKey("format") && metadata.containsKey("content")) {
                final Object content = metadata.get("content");
                if(content instanceof String) {
                    switch((String)content) {
                        case "vector" :
                            metadata.put("format", "pbf");
                            break;
                        case "terrain" :
                            metadata.put("format", TerrainRGBChunkSpi.MIME_TYPE);
                            break;
                        case "imagery" :
                            metadata.put("format", "png");
                            break;
                        default :
                            break;
                    }
                    if(metadata.get("format") != null)
                        metadata.remove("content");
                }
            }
            StatementIface stmt = null;
            try {
                stmt = db.compileStatement("INSERT INTO metadata (name, value) VALUES(?, ?)");
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    stmt.clearBindings();
                    final Object v = entry.getValue();
                    if (v == null) {
                        stmt.bindNull(2);
                    } else if (v instanceof String) {
                        stmt.bind(2, (String) v);
                    } else if (v instanceof Integer) {
                        stmt.bind(2, (Integer) v);
                    } else if (v instanceof Long) {
                        stmt.bind(2, (Long) v);
                    } else if (v instanceof Double) {
                        stmt.bind(2, (Double) v);
                    } else if (v instanceof byte[]) {
                        stmt.bind(2, (byte[]) v);
                    } else {
                        Log.w("DefaultDbSchema", "MBtiles metadata [" + entry.getKey() + "]; unsupported value type: " + v.getClass());
                        continue;
                    }
                    stmt.bind(1, entry.getKey());
                    stmt.execute();
                }
            } finally {
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    final static class OpenMapTilerDbSchema implements DbSchema {

        final static Map<String, Collection<String>> _schema = new HashMap<>();
        static {
            _schema.put("tiles_shallow", Arrays.asList("zoom_level", "tile_column", "tile_row", "tile_data_id"));
            _schema.put("tiles_data", Arrays.asList("tile_data_id", "tile_data"));
        }

        final static MBTilesContainer.DbSchema.Spi SPI = new MBTilesContainer.DbSchema.Spi() {

            @Override
            public String getName() {
                return "omt";
            }

            @Override
            public boolean matches(DatabaseIface db) {
                return DefaultDbSchema.SPI.matches(db) && Databases.matchesSchema(db, _schema, false);
            }

            @Override
            public DbSchema newInstance() {
                return new OpenMapTilerDbSchema();
            }
        };

        StatementIface _insertTilesDataStatement;
        StatementIface _insertTilesShallowStatement;
        QueryIface _queryTilesShallowTileDataId;
        QueryIface _queryNextTilesDataTileDataId;


        @Override
        public void createTables(DatabaseIface db, Map<String, Object> metadata) {
            Set<String> tables = Databases.getTableNames(db);
            if(!tables.contains("tiles_data")) {
                db.execute("CREATE TABLE tiles_data (tile_data_id INTEGER PRIMARY KEY, tile_data BLOB)", null);
            }
            if (!tables.contains("tiles_shallow"))
            {
                db.execute("CREATE TABLE tiles_shallow (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data_id INTEGER, PRIMARY KEY(zoom_level,tile_column,tile_row)) WITHOUT ROWID", null);
            }
            if(!tables.contains("tiles")) {
                db.execute("CREATE VIEW tiles AS SELECT tiles_shallow.zoom_level AS zoom_level, tiles_shallow.tile_column AS tile_column, tiles_shallow.tile_row AS tile_row, tiles_data.tile_data AS tile_data FROM tiles_shallow JOIN tiles_data ON tiles_shallow.tile_data_id = tiles_data.tile_data_id", null);
            }
            if (!tables.contains("metadata"))
            {
                db.execute("CREATE TABLE metadata (name TEXT, value TEXT)", null);
                DefaultDbSchema.initMetadataValues(db, metadata);
                db.execute("CREATE UNIQUE INDEX name on metadata (name)", null);
            }
        }

        @Override
        public void setTile(DatabaseIface db, int zoom, int x, int y, byte[] data) {
            // query to see if tile already exists
            long tileDataId = 0L;
            if (_queryTilesShallowTileDataId == null)
                _queryTilesShallowTileDataId = db.compileQuery("SELECT tile_data_id FROM tiles_shallow WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1");
            try {
                _queryTilesShallowTileDataId.bind(1, zoom);
                _queryTilesShallowTileDataId.bind(2, x);
                _queryTilesShallowTileDataId.bind(3, y);

                if (_queryTilesShallowTileDataId.moveToNext())
                    tileDataId = _queryTilesShallowTileDataId.getLong(0);
            } finally {
                _queryTilesShallowTileDataId.reset();
                _queryTilesShallowTileDataId.clearBindings();
            }
            // insert new `tiles_shallow` record if does not exist
            if(tileDataId <= 0) {
                if(_queryNextTilesDataTileDataId == null)
                    _queryNextTilesDataTileDataId = db.compileQuery("SELECT max(tile_data_id) FROM tiles_data");
                try {
                    tileDataId = _queryNextTilesDataTileDataId.moveToNext() ? _queryNextTilesDataTileDataId.getLong(0)+1 : 1;
                } finally {
                    _queryNextTilesDataTileDataId.reset();
                }

                if(_insertTilesShallowStatement == null)
                    _insertTilesShallowStatement = db.compileStatement("INSERT INTO tiles_shallow (zoom_level, tile_column, tile_row, tile_data_id) VALUES(?, ?, ?, ?)");
                try {
                    _insertTilesShallowStatement.bind(1, zoom);
                    _insertTilesShallowStatement.bind(2, x);
                    _insertTilesShallowStatement.bind(3, y);
                    _insertTilesShallowStatement.bind(4, tileDataId);

                    _insertTilesShallowStatement.execute();
                } finally {
                    _insertTilesShallowStatement.clearBindings();
                }
            }

            if(_insertTilesDataStatement == null)
                _insertTilesDataStatement = db.compileStatement("INSERT INTO tiles_data (tile_data_id, tile_data) VALUES(?, ?) ON CONFLICT(tile_data_id) DO UPDATE SET tile_data=excluded.tile_data");
            try {
                _insertTilesDataStatement.bind(1, tileDataId);
                _insertTilesDataStatement.bind(2, data);

                _insertTilesDataStatement.execute();
            } finally {
                _insertTilesDataStatement.clearBindings();
            }
        }

        @Override
        public void dispose() {
            if(_insertTilesDataStatement != null) {
                _insertTilesDataStatement.close();
                _insertTilesDataStatement = null;
            }
            if(_insertTilesShallowStatement != null) {
                _insertTilesShallowStatement.close();
                _insertTilesShallowStatement = null;
            }
            if(_queryTilesShallowTileDataId != null) {
                _queryTilesShallowTileDataId.close();
                _queryTilesShallowTileDataId = null;
            }
            if(_queryNextTilesDataTileDataId != null) {
                _queryNextTilesDataTileDataId.close();
                _queryNextTilesDataTileDataId = null;
            }
        }
    }
}
