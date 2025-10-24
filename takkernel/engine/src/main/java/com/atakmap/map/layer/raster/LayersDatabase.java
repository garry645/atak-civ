
package com.atakmap.map.layer.raster;

import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.DatabaseInformation;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.map.layer.feature.datastore.FeatureSpatialDatabase;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.content.CatalogCurrency;
import com.atakmap.content.CatalogCurrencyRegistry;
import com.atakmap.content.CatalogDatabase;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabaseFactory2;

import gov.tak.platform.system.SystemUtils;

final class LayersDatabase extends CatalogDatabase
{

    public static final String TAG = "LayersDatabase";

    private final static int DATABASE_VERSION = 11;

    final static String TABLE_LAYERS = "layers";
    final static String COLUMN_LAYERS_ID = "id";
    final static String COLUMN_LAYERS_PATH = "path";
    final static String COLUMN_LAYERS_CATALOG_LINK = "cataloglink";
    final static String COLUMN_LAYERS_INFO = "info";
    final static String COLUMN_LAYERS_NAME = "name";
    final static String COLUMN_LAYERS_DATASET_TYPE = "datasettype";
    final static String COLUMN_LAYERS_PROVIDER = "provider";
    final static String COLUMN_LAYERS_SRID = "srid";
    final static String COLUMN_LAYERS_MAX_LAT = "maxlat";
    final static String COLUMN_LAYERS_MIN_LON = "minlon";
    final static String COLUMN_LAYERS_MIN_LAT = "minlat";
    final static String COLUMN_LAYERS_MAX_LON = "maxlon";
    final static String COLUMN_LAYERS_MIN_GSD = "mingsd";
    final static String COLUMN_LAYERS_MAX_GSD = "maxgsd";
    final static String COLUMN_LAYERS_REMOTE = "remote";
    final static String COLUMN_LAYERS_COVERAGE = "coverage";

    final static String TABLE_IMAGERY_TYPES = "imagerytypes";
    final static String COLUMN_IMAGERY_TYPES_NAME = "name";
    final static String COLUMN_IMAGERY_TYPES_LAYER_ID = "layerid";
    final static String COLUMN_IMAGERY_TYPES_GEOM = "geom";
    final static String COLUMN_IMAGERY_TYPES_MIN_GSD = "mingsd";
    final static String COLUMN_IMAGERY_TYPES_MAX_GSD = "maxgsd";

    private final static String SQL_INSERT_LAYER_STMT =
            "INSERT INTO " + TABLE_LAYERS +
                    "(" + COLUMN_LAYERS_PATH + ", " +         // 1
                    COLUMN_LAYERS_CATALOG_LINK + ", " + // 2
                    COLUMN_LAYERS_INFO + ", " +         // 3
                    COLUMN_LAYERS_NAME + ", " +         // 4
                    COLUMN_LAYERS_PROVIDER + ", " +     // 5
                    COLUMN_LAYERS_DATASET_TYPE + ", " + // 6
                    COLUMN_LAYERS_SRID + ", " +         // 7
                    COLUMN_LAYERS_MAX_LAT + ", " +      // 8
                    COLUMN_LAYERS_MIN_LON + ", " +      // 9
                    COLUMN_LAYERS_MIN_LAT + ", " +      // 10
                    COLUMN_LAYERS_MAX_LON + ", " +      // 11
                    COLUMN_LAYERS_MIN_GSD + ", " +      // 12
                    COLUMN_LAYERS_MAX_GSD + ", " +      // 13
                    COLUMN_LAYERS_REMOTE + ", " +      // 14
                    COLUMN_LAYERS_COVERAGE + ") " +     // 15
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GeomFromWKB(?, 4326))";

    private final static String SQL_INSERT_TYPE_STMT =
            "INSERT INTO " + TABLE_IMAGERY_TYPES +
                    "(" + COLUMN_IMAGERY_TYPES_LAYER_ID + ", " +  // 1
                    COLUMN_IMAGERY_TYPES_NAME + ", " +      // 2
                    COLUMN_IMAGERY_TYPES_GEOM + ", " +      // 3
                    COLUMN_IMAGERY_TYPES_MIN_GSD + ", " +   // 4
                    COLUMN_IMAGERY_TYPES_MAX_GSD + ") " +   // 5
                    "VALUES (?, ?, ?, ?, ?)";

    private final static String SQL_UPDATE_LAYER_EXTRAS_STMT =
            "UPDATE " + TABLE_LAYERS + " SET " + COLUMN_LAYERS_INFO
                    + " = ? WHERE " + COLUMN_LAYERS_ID + " = ?";

    /**************************************************************************/

    final PersistentRasterDataStore _callback;
    final DeferredCoverageMonitor _deferredCoverageMonitor;

    public LayersDatabase(File databaseFile, CatalogCurrencyRegistry r, PersistentRasterDataStore callback)
    {
        super(createDatabase(databaseFile), r);

        // evict any layers that are present without corresponding catalog entries
        this.database.execute("DELETE FROM layers WHERE cataloglink NOT IN (SELECT id FROM catalog)", null);

        _callback = callback;
        _deferredCoverageMonitor = new DeferredCoverageMonitor();

        Thread t = new Thread(_deferredCoverageMonitor);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public synchronized void close()
    {
        super.close();
        database = null;
        this.notifyAll();
    }

    /**
     * Responsible for renaming the Old DB if necessary, and returns the Database.
     *
     * @param databaseFile the file to use as the database.
     * @return the database.
     */
    private static DatabaseIface createDatabase(File databaseFile)
    {
        DatabaseIface db = IOProviderFactory.createDatabase(databaseFile,
                DatabaseInformation.OPTION_ENSURE_PARENT_DIRS);
        if (db == null && IOProviderFactory.exists(databaseFile))
        {
            IOProviderFactory.delete(databaseFile, IOProvider.SECURE_DELETE);
            db = IOProviderFactory.createDatabase(databaseFile);
        }
        return db;
    }

    private static int databaseVersion()
    {
        return (CATALOG_VERSION | (DATABASE_VERSION << 16));
    }

    @Override
    public void dropTables()
    {
        super.dropTables();

        this.database.execute("DROP TABLE IF EXISTS " + TABLE_LAYERS, null);
        this.database.execute("DROP TABLE IF EXISTS " + TABLE_IMAGERY_TYPES, null);

        if (Databases.getTableNames(this.database).contains("resources"))
        {
            final LinkedList<String> invalidResources = new LinkedList<String>();

            CursorIface result = null;
            try
            {
                result = this.database.query("SELECT path FROM resources", null);
                while (result.moveToNext())
                    invalidResources.add(result.getString(0));
            } finally
            {
                if (result != null)
                    result.close();
            }

            if (invalidResources.size() > 0)
            {
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        File f;
                        for (String s : invalidResources)
                        {
                            FileSystemUtils.delete(s);
                        }
                    }
                };
                t.setPriority(Thread.NORM_PRIORITY);
                t.setName("LayersDatabase-resource-cleanup-"
                        + Integer.toString(t.hashCode(), 16));
                t.start();
            }
        }

        this.database.execute("DROP TABLE IF EXISTS resources", null);
    }

    @Override
    protected boolean checkDatabaseVersion()
    {
        return (this.database.getVersion() == databaseVersion());
    }

    @Override
    protected void setDatabaseVersion()
    {
        this.database.setVersion(databaseVersion());
    }

    @Override
    public void buildTables()
    {
        CursorIface result;

        final int major = FeatureSpatialDatabase.getSpatialiteMajorVersion(this.database);
        final int minor = FeatureSpatialDatabase.getSpatialiteMinorVersion(this.database);

        final String initSpatialMetadataSql;
        if (major > 4 || (major == 4 && minor >= 2))
            initSpatialMetadataSql = "SELECT InitSpatialMetadata(1, \'WGS84\')";
        else if (major > 4 || (major == 4 && minor >= 1))
            initSpatialMetadataSql = "SELECT InitSpatialMetadata(1)";
        else
            initSpatialMetadataSql = "SELECT InitSpatialMetadata()";

        result = null;
        try
        {
            result = this.database.query(initSpatialMetadataSql, null);
            result.moveToNext();
        } finally
        {
            if (result != null)
                result.close();
        }

        super.buildTables();

        this.database
                .execute(
                        "CREATE TABLE " + TABLE_LAYERS + " (" +
                                COLUMN_LAYERS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                                COLUMN_LAYERS_PATH + " TEXT, " +
                                COLUMN_LAYERS_CATALOG_LINK + " INTEGER, " +
                                COLUMN_LAYERS_INFO + " BLOB, " +
                                COLUMN_LAYERS_NAME + " TEXT, " +
                                COLUMN_LAYERS_PROVIDER + " TEXT, " +
                                COLUMN_LAYERS_DATASET_TYPE + " TEXT, " +
                                COLUMN_LAYERS_SRID + " INTEGER, " +
                                COLUMN_LAYERS_MAX_LAT + " REAL, " +
                                COLUMN_LAYERS_MIN_LON + " REAL, " +
                                COLUMN_LAYERS_MIN_LAT + " REAL, " +
                                COLUMN_LAYERS_MAX_LON + " REAL, " +
                                COLUMN_LAYERS_MIN_GSD + " REAL, " +
                                COLUMN_LAYERS_MAX_GSD + " REAL, " +
                                COLUMN_LAYERS_REMOTE + " INTEGER)",
                        null);
        result = null;
        try
        {
            result = this.database.query("SELECT AddGeometryColumn(\'" + TABLE_LAYERS + "\', \'" + COLUMN_LAYERS_COVERAGE + "\', 4326, \'GEOMETRY\', \'XY\')", null);
            result.moveToNext();
        } finally
        {
            if (result != null)
                result.close();
        }
        this.database.execute("CREATE TABLE resources (path TEXT, link INTEGER)", null);
        this.database.execute("CREATE TABLE " + TABLE_IMAGERY_TYPES + " (" +
                COLUMN_IMAGERY_TYPES_LAYER_ID + " INTEGER, " +
                COLUMN_IMAGERY_TYPES_NAME + " TEXT, " +
                COLUMN_IMAGERY_TYPES_GEOM + " BLOB, " +
                COLUMN_IMAGERY_TYPES_MIN_GSD + " REAL, " +
                COLUMN_IMAGERY_TYPES_MAX_GSD + " REAL)", null);
    }

    @Override
    protected void validateCatalogNoSync(CatalogCursor result) {
        super.validateCatalogNoSync(result);

        // the catalog is validated, track and deferred coverage descriptors
        try(CursorIface cursor = queryLayersNoSync(new String[] {COLUMN_LAYERS_INFO}, null, null, null, null, null, null)) {
            boolean shouldNotify = false;
            while(cursor.moveToNext()) {
                try {
                    DatasetDescriptor desc = DatasetDescriptor.decode(cursor.getBlob(0));
                    if(hasDeferredCoverage(desc, null))
                        shouldNotify |= _deferredCoverageMonitor.deferredCoverageDescriptors.add(desc);
                } catch(IOException ignored) {}
            }
            if(shouldNotify)
                this.notifyAll();
        }
    }

    @Override
    protected boolean validateCatalogRowNoSync(CatalogCursor row)
    {
        // if external SD and doesn't exist, ignore eviction
        final File file = new File(FileSystemUtils.sanitizeWithSpacesAndSlashes(row.getPath()));
        if(!IOProviderFactory.exists(file) && isExternalStorage(file))
            return true;
        return super.validateCatalogRowNoSync(row);
    }

    protected boolean checkCatalogEntryExists(File derivedFrom)
    {
        CatalogCursor result = null;
        try
        {
            result = this.queryCatalog(derivedFrom);
            return result.moveToNext();
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    public synchronized void addLayers(File derivedFrom, Set<DatasetDescriptor> layers,
                                       File workingDir, CatalogCurrency currency) throws IOException
    {

        if (this.checkCatalogEntryExists(derivedFrom))
            throw new IllegalStateException("entry already exists: "
                    + derivedFrom.getAbsolutePath());

        //this.database.beginTransaction();
        try
        {
            // add the catalog entry
            final long catalogId = this.addCatalogEntryNoSync(derivedFrom, currency);

            long layerId = Databases.getNextAutoincrementId(this.database, TABLE_LAYERS);

            StatementIface insertLayerStmt = null;
            StatementIface insertTypeStmt = null;

            boolean shouldNotify = false;
            try
            {
                insertLayerStmt = this.database.compileStatement(SQL_INSERT_LAYER_STMT);
                insertTypeStmt = this.database.compileStatement(SQL_INSERT_TYPE_STMT);

                Iterator<DatasetDescriptor> iter = layers.iterator();
                DatasetDescriptor info;
                Envelope mbb;
                int idx;
                Geometry coverage;
                ByteBuffer coverageWkb;
                while (iter.hasNext())
                {
                    info = iter.next();
                    byte[] infoBlob = info.encode(layerId);
                    if(hasDeferredCoverage(info, null)) {
                        // NOTE: need to roundtrip the info blob for the ID
                        shouldNotify |= _deferredCoverageMonitor.deferredCoverageDescriptors.add(
                                DatasetDescriptor.decode(infoBlob));
                    }

                    coverage = info.getCoverage(null);
                    mbb = coverage.getEnvelope();
                    coverageWkb = ByteBuffer.wrap(new byte[coverage.computeWkbSize()]);
                    coverage.toWkb(coverageWkb);

                    idx = 1;
                    insertLayerStmt.clearBindings();
                    insertLayerStmt.bind(idx++, derivedFrom.getAbsolutePath());
                    insertLayerStmt.bind(idx++, catalogId);
                    insertLayerStmt.bind(idx++, infoBlob);
                    insertLayerStmt.bind(idx++, info.getName());
                    insertLayerStmt.bind(idx++, info.getProvider());
                    insertLayerStmt.bind(idx++, info.getDatasetType());
                    insertLayerStmt.bind(idx++, info.getSpatialReferenceID());
                    insertLayerStmt.bind(idx++, mbb.maxY);
                    insertLayerStmt.bind(idx++, mbb.minX);
                    insertLayerStmt.bind(idx++, mbb.minY);
                    insertLayerStmt.bind(idx++, mbb.maxX);
                    insertLayerStmt.bind(idx++, info.getMinResolution(null));
                    insertLayerStmt.bind(idx++, info.getMaxResolution(null));
                    insertLayerStmt.bind(idx++, info.isRemote() ? 1 : 0);
                    insertLayerStmt.bind(idx++, coverageWkb.array());

                    insertLayerStmt.execute();

                    for (String type : info.getImageryTypes())
                    {
                        idx = 1;
                        insertTypeStmt.clearBindings();
                        insertTypeStmt.bind(idx++, layerId);
                        insertTypeStmt.bind(idx++, type);
                        coverage = info.getCoverage(type);
                        coverageWkb = ByteBuffer.wrap(new byte[coverage.computeWkbSize()]);
                        coverage.toWkb(coverageWkb);
                        insertTypeStmt.bind(idx++, coverageWkb.array());
                        insertTypeStmt.bind(idx++, info.getMinResolution(type));
                        insertTypeStmt.bind(idx++, info.getMaxResolution(type));
                        insertTypeStmt.execute();
                    }

                    layerId++; // increment next autoincrement ID
                }
            } finally
            {
                insertLayerStmt.close();
                insertTypeStmt.close();
            }

            StatementIface stmt = null;
            try
            {
                stmt = this.database
                        .compileStatement("INSERT INTO resources (link, path) VALUES (?, ?)");
                stmt.bind(1, catalogId);
                stmt.bind(2, workingDir.getAbsolutePath());

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }

            if(shouldNotify)
                this.notifyAll();

            //this.database.setTransactionSuccessful();
        } finally
        {
            //this.database.endTransaction();
        }
    }

    /**
     * Update layer extras metadata in the database
     * For a complete update (name/geometry change), remove and re-insert instead
     *
     * @param info Dataset info
     * @throws IOException Database error
     */
    public synchronized void updateLayerExtras(DatasetDescriptor info) throws IOException
    {
        StatementIface updateStmt = null;
        try
        {
            long layerId = info.getLayerId();
            updateStmt = this.database.compileStatement(SQL_UPDATE_LAYER_EXTRAS_STMT);
            updateStmt.clearBindings();
            updateStmt.bind(1, info.encode(layerId));
            updateStmt.bind(2, layerId);
            updateStmt.execute();
        } finally
        {
            if (updateStmt != null)
                updateStmt.close();
        }
    }

    @Override
    public synchronized CursorIface query(String table, String[] columns, String selection,
                                          String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
    {

        return super.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    private CursorIface queryLayersNoSync(String[] columns, String selection,
                                          String[] selectionArgs,
                                          String groupBy, String having, String orderBy, String limit)
    {

        return super.query(TABLE_LAYERS, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    protected void onCatalogEntryRemoved(long catalogId, boolean automated)
    {
        if (catalogId > 0)
        {
            StatementIface stmt;

            stmt = null;
            try
            {
                stmt = this.database.compileStatement("DELETE FROM " + TABLE_IMAGERY_TYPES + " WHERE " + COLUMN_IMAGERY_TYPES_LAYER_ID + " IN (SELECT " + COLUMN_LAYERS_ID + " FROM " + TABLE_LAYERS + " WHERE " + COLUMN_LAYERS_CATALOG_LINK + " = ?)");
                stmt.bind(1, catalogId);
                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }

            // remove the invalid layers
            stmt = null;
            try
            {
                stmt = this.database.compileStatement("DELETE FROM " + TABLE_LAYERS + " WHERE " + COLUMN_LAYERS_CATALOG_LINK + " = ?");
                stmt.bind(1, catalogId);

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }

            // delete resources associated with deleted layers
            CursorIface result = null;
            try
            {
                result = this.database.query(
                        "SELECT path FROM resources WHERE link = " + String.valueOf(catalogId),
                        null);
                while (result.moveToNext())
                {
                    FileSystemUtils.delete(result.getString(0));
                }
            } finally
            {
                if (result != null)
                    result.close();
            }

            stmt = null;
            try
            {
                stmt = this.database.compileStatement("DELETE FROM resources WHERE link = ?");
                stmt.bind(1, catalogId);

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }
        } else
        {
            StatementIface stmt;

            stmt = null;
            try
            {
                stmt = this.database.compileStatement("DELETE FROM " + TABLE_IMAGERY_TYPES +
                        " WHERE " + COLUMN_IMAGERY_TYPES_LAYER_ID +
                        " IN (SELECT " + COLUMN_LAYERS_ID +
                        " FROM " + TABLE_LAYERS + " LEFT JOIN " + TABLE_CATALOG +
                        " on " + TABLE_LAYERS + "." + COLUMN_LAYERS_CATALOG_LINK + " = " +
                        TABLE_CATALOG + "." + COLUMN_CATALOG_ID +
                        " WHERE " + TABLE_CATALOG + "." + COLUMN_CATALOG_ID + " IS NULL)");

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }

            // remove all layers without a satisfied link
            stmt = null;
            try
            {
                stmt = this.database
                        .compileStatement("DELETE FROM layers WHERE cataloglink IN (SELECT cataloglink FROM layers LEFT JOIN "
                                + TABLE_CATALOG
                                + " on layers.cataloglink = "
                                + TABLE_CATALOG
                                + "."
                                + COLUMN_CATALOG_ID
                                + " WHERE "
                                + TABLE_CATALOG
                                + "."
                                + COLUMN_CATALOG_ID + " IS NULL)");

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }

            // delete resources associated with deleted layers
            CursorIface result = null;
            try
            {
                result = this.database.query(
                        "SELECT path FROM resources WHERE link IN (SELECT link FROM resources LEFT JOIN "
                                + TABLE_CATALOG + " on resources.link = " + TABLE_CATALOG + "."
                                + COLUMN_CATALOG_ID + " WHERE " + TABLE_CATALOG + "."
                                + COLUMN_CATALOG_ID + " IS NULL)", null);
                while (result.moveToNext())
                {
                    FileSystemUtils.delete(result.getString(0));
                }
            } finally
            {
                if (result != null)
                    result.close();
            }

            stmt = null;
            try
            {
                stmt = this.database
                        .compileStatement("DELETE FROM resources WHERE link IN (SELECT link FROM resources LEFT JOIN "
                                + TABLE_CATALOG
                                + " on resources.link="
                                + TABLE_CATALOG
                                + "."
                                + COLUMN_CATALOG_ID
                                + " WHERE "
                                + TABLE_CATALOG
                                + "."
                                + COLUMN_CATALOG_ID + " IS NULL)");

                stmt.execute();
            } finally
            {
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    /**************************************************************************/

    static boolean isExternalStorage(File f)
    {
        if(SystemUtils.isOsAndroid())
        {
            final String path = f.getPath();
            if(!path.startsWith("/storage/"))
                return false;
            return path.length() >= 19 &&
                    Character.digit(path.charAt(9), 16) != -1 &&
                    Character.digit(path.charAt(10), 16) != -1 &&
                    Character.digit(path.charAt(11), 16) != -1 &&
                    Character.digit(path.charAt(12), 16) != -1 &&
                    path.charAt(13) == '-' &&
                    Character.digit(path.charAt(14), 16) != -1 &&
                    Character.digit(path.charAt(15), 16) != -1 &&
                    Character.digit(path.charAt(16), 16) != -1 &&
                    Character.digit(path.charAt(17), 16) != -1;
        }
        else
        {
            return false;
        }
    }
    private static String sqliteFriendlyFloatingPointString(double v)
    {
        if (Double.isNaN(v))
            return "NULL"; // SQLite NULL will do what we want for comparisons
        else
            return String.valueOf(v);
    }

    private static boolean hasDeferredCoverage(DatasetDescriptor desc, MosaicDatabase2[] ref) {
        if(desc.getExtraData(".deferredCoverageResolved") != null)
            return false;
        else if(!(desc instanceof MosaicDatasetDescriptor))
            return false;

        MosaicDatabase2 mosaicdb = null;
        try {
            MosaicDatasetDescriptor mosaic = (MosaicDatasetDescriptor) desc;
            mosaicdb = MosaicDatabaseFactory2.create(mosaic.getMosaicDatabaseProvider());
            if(mosaicdb == null)
                return false;
            mosaicdb.open(mosaic.getMosaicDatabaseFile());
            final boolean retval = mosaicdb.getCoverage(".deferred") != null;
            if(ref != null) {
                ref[0] = mosaicdb;
                mosaicdb = null;
            }
            return retval;
        } finally {
            if(mosaicdb != null)
                mosaicdb.close();
        }
    }


    private static class SelectionBuilder
    {
        private StringBuilder selection;

        public SelectionBuilder()
        {
            this.selection = new StringBuilder();
        }

        public void append(String s)
        {
            if (this.selection.length() > 0)
                this.selection.append(" AND ");
            this.selection.append(s);
        }

        public String getSelection()
        {
            if (this.selection.length() < 1)
                return null;
            return this.selection.toString();
        }
    }

    private class DeferredCoverageMonitor implements Runnable {
        Set<DatasetDescriptor> deferredCoverageDescriptors = new HashSet<>();

        @Override
        public void run() {
            LinkedList<DatasetDescriptor> checking = new LinkedList<>();
            boolean checked = false;
            while(true) {
                synchronized(LayersDatabase.this) {
                    if(LayersDatabase.this.database == null) {
                        break;
                    }
                    deferredCoverageDescriptors.removeAll(checking);
                    checking.clear();
                    if(deferredCoverageDescriptors.isEmpty()) {
                        try {
                            LayersDatabase.this.wait();
                        } catch(InterruptedException ignored) {}
                        checked = false;
                        continue;
                    } else if(checked) {
                        // if we've previously checked, wait a short time
                        try {
                            LayersDatabase.this.wait(5000);
                        } catch(InterruptedException ignored) {}
                    }

                    checking.addAll(deferredCoverageDescriptors);
                    checked = true;
                }

                Iterator<DatasetDescriptor> iter = checking.iterator();
                while(iter.hasNext()) {
                    DatasetDescriptor desc = iter.next();
                    if(desc instanceof MosaicDatasetDescriptor) {
                        MosaicDatasetDescriptor mosaic = (MosaicDatasetDescriptor)desc;
                        MosaicDatabase2 db = null;
                        try {
                            db = MosaicDatabaseFactory2.create(mosaic.getMosaicDatabaseProvider());
                            if (db != null) {
                                db.open(mosaic.getMosaicDatabaseFile());
                                final MosaicDatabase2.Coverage deferredCoverage = db.getCoverage(".deferred");
                                if(deferredCoverage == null || deferredCoverage.geometry == null) {
                                    continue;
                                }
                            } else {
                                // unable to create mosaic DB, ignore
                                continue;
                            }
                        } finally {
                            if(db != null)
                                db.close();
                        }
                    }

                    iter.remove();
                }

                // invalidate cache for any deferred coverages that were resolved
                synchronized(LayersDatabase.this._callback) {
                    for(DatasetDescriptor desc : checking) {
                        if(desc instanceof MosaicDatasetDescriptor) {
                            Map<String, Pair<Double, Double>> resolutions = new HashMap<>();
                            Map<String, Geometry> coverages = new HashMap<>();

                            MosaicDatabase2 mosaicdb = null;
                            try {
                                mosaicdb = MosaicDatabaseFactory2.create(((MosaicDatasetDescriptor) desc).getMosaicDatabaseProvider());
                                if (mosaicdb == null)
                                    continue;

                                mosaicdb.open(((MosaicDatasetDescriptor) desc).getMosaicDatabaseFile());

                                for (String type : desc.getImageryTypes()) {
                                    MosaicDatabase2.Coverage coverage = mosaicdb.getCoverage(type);
                                    if (coverage != null) {
                                        resolutions.put(type, Pair.create(Double.valueOf(coverage.minGSD), Double.valueOf(coverage.maxGSD)));
                                        coverages.put(type, coverage.geometry);
                                    } else {
                                        resolutions.put(type, Pair.create(Double.valueOf(desc.getMinResolution(type)), Double.valueOf(desc.getMaxResolution(type))));
                                        coverages.put(type, desc.getCoverage(type));
                                    }
                                }
                                coverages.put(null, mosaicdb.getCoverage().geometry);
                            } finally {
                                if(mosaicdb != null)
                                    mosaicdb.close();
                            }

                            Map<String, String> extraData = new HashMap<>();
                            extraData.putAll(desc.getExtraData());
                            // mark deferred coverage as resolved
                            extraData.put(".deferredCoverageResolved", "1");

                            desc = new MosaicDatasetDescriptor(
                                    desc.getLayerId(),
                                    desc.getName(),
                                    desc.getUri(),
                                    desc.getProvider(),
                                    desc.getDatasetType(),
                                    ((MosaicDatasetDescriptor) desc).getMosaicDatabaseFile(),
                                    ((MosaicDatasetDescriptor) desc).getMosaicDatabaseProvider(),
                                    desc.getImageryTypes(),
                                    resolutions,
                                    coverages,
                                    desc.getSpatialReferenceID(),
                                    desc.isRemote(),
                                    desc.getWorkingDirectory(),
                                    extraData);
                        } else {
                            // currently only supported for mosaic datasets
                            continue;
                        }
                        try {
                            LayersDatabase.this.updateLayerExtras(desc);
                        } catch(IOException ignored) {}
                    }
                    if(!checking.isEmpty()) {
                        Set<Long> ids = new HashSet<>();
                        for(DatasetDescriptor desc : checking)
                            ids.add(desc.getLayerId());
                        _callback.invalidateCacheInfo(checking, true);
                    }
                }
            }
        }
    }
}
