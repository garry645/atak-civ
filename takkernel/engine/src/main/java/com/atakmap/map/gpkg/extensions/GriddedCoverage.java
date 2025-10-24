package com.atakmap.map.gpkg.extensions;

import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.lang.Objects;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.gpkg.TileTable;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.gpkg.GeoPackageTileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileGrid;
import com.atakmap.math.PointD;
import com.atakmap.math.Statistics;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class GriddedCoverage {

    static {
        GdalLibrary.init();
    }

    public enum GridCellEncoding {
        GridValueIsCenter,
        GridValueIsArea,
        GridValueIsCorner,
    }

    public final static String NAME = "gpkg_2d_gridded_coverage";
    public final static String DEFINITION = "http://docs.opengeospatial.org/is/17-066r1/17-066r1.html";

    public final static String TABLE_gpkg_2d_gridded_coverage_ancillary = "gpkg_2d_gridded_coverage_ancillary";
    public final static String TABLE_gpkg_2d_gridded_tile_ancillary = "gpkg_2d_gridded_tile_ancillary";

    public final static String WKT_definition_12_063_4979 = "GEODCRS[\"WGS 84\",DATUM[\"World Geodetic System 1984\",  ELLIPSOID[\"WGS 84\",6378137,298.257223563,LENGTHUNIT[\"metre\",1.0]]],CS[ellipsoidal,3],  AXIS[\"latitude\",north,ORDER[1],ANGLEUNIT[\"degree\",0.01745329252]],  AXIS[\"longitude\",east,ORDER[2],ANGLEUNIT[\"degree\",0.01745329252]],  AXIS[\"ellipsoidal height\",up,ORDER[3],LENGTHUNIT[\"metre\",1.0]],ID[\"EPSG\",4979]]";

    GeoPackage gpkg;
    TileTable tiles;
    double unitsToMeters = 1d;

    public String tile_matrix_set_name;
    public String datatype = "integer";
    public double scale = 1d;
    public double offset = 0d;
    public double precision = 1d;
    public double data_null_value = Double.NaN;
    public String grid_cell_encoding = "grid-value-is-center";
    public String uom;
    public String field_name = "Height";
    public String quantity_definition = "Height";


    public GriddedCoverage() {
    }


    public void insertTile(int zoom, int x, int y, double[] heightmap) {
        insertTile(zoom, x, y, heightmap, 0d, 1d);
    }
    public void insertTile(int zoom, int x, int y, float[] heightmap) {
        insertTile(zoom, x, y, heightmap, 0d, 1d);
    }

    public void insertTile(int zoom, int x, int y, double[] heightmap, double offset, double scale) {
        if(Double.isNaN(offset) || Double.isNaN(scale))
            throw new IllegalArgumentException();
        final Statistics tileStats = new Statistics();
        final byte[] tileData = encodeHeightmap(zoom, heightmap, offset, scale, tileStats);
        if(tileData != null)
            insertTile(zoom, x, y, tileData, offset, scale, tileStats);
    }

    public void insertTile(int zoom, int x, int y, float[] heightmapf, double offset, double scale) {
        final double[] heightmap = new double[heightmapf.length];
        for(int i = 0; i < heightmapf.length; i++)
            heightmap[i] = heightmapf[i];
        insertTile(zoom, x, y, heightmap, offset, scale);
    }

    synchronized void insertTile(int zoom, int x, int y, byte[] data, double offset, double scale, Statistics tileStats) {
        long tpudt_id;
        try(QueryIface result = gpkg.getDatabase().compileQuery("SELECT id FROM '" + tile_matrix_set_name + "' WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1")) {
            tpudt_id = result.moveToNext() ? result.getLong(0) : 0;
        }

        StatementIface stmt;
        stmt = null;
        try {
            stmt = (tpudt_id == 0) ?
                    gpkg.getDatabase().compileStatement("INSERT INTO '" + tile_matrix_set_name + "' (tile_data, zoom_level, tile_column, tile_row) VALUES(?, ?, ?, ?)") :
                    gpkg.getDatabase().compileStatement("UPDATE '" + tile_matrix_set_name + "' SET tile_data = ? WHERE id = ?");

            stmt.bind(1, data);
            if(tpudt_id == 0) {
                stmt.bind(2, zoom);
                stmt.bind(3, x);
                stmt.bind(4, y);
            } else {
                stmt.bind(2, tpudt_id);
            }
            stmt.execute();
            if(tpudt_id == 0)
                tpudt_id = Databases.lastInsertRowId(gpkg.getDatabase());
        } finally {
            if(stmt != null)
                stmt.close();
        }

        stmt = null;
        try {
            stmt = gpkg.getDatabase().compileStatement("INSERT INTO '" + TABLE_gpkg_2d_gridded_tile_ancillary + "' " +
                    "(tpudt_name, " +
                    "tpudt_id, " +
                    "scale, " +
                    "offset, " +
                    "min, " +
                    "max, " +
                    "mean, " +
                    "std_dev) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(tpudt_name, tpudt_id) DO UPDATE SET " +
                    "scale=excluded.scale, " +
                    "offset=excluded.offset, " +
                    "min=excluded.min, " +
                    "max=excluded.max, " +
                    "mean=excluded.mean, " +
                    "std_dev=excluded.std_dev");

            stmt.bind(1, tile_matrix_set_name);
            stmt.bind(2, tpudt_id);
            stmt.bind(3, scale);
            stmt.bind(4, offset);
            stmt.bind(5, tileStats.minimum);
            stmt.bind(6, tileStats.maximum);
            stmt.bind(7, tileStats.mean);
            stmt.bind(8, tileStats.stddev);

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    byte[] encodeHeightmap(int zoom, double[] heightmap, double ancillary_offset, double ancillary_scale, Statistics statistics) {
        final org.gdal.gdal.Driver memdriver = gdal.GetDriverByName("MEM");
        final int gdt;
        final org.gdal.gdal.Driver outdriver;
        final String[] createOptions;
        switch(datatype) {
            case "integer" :
                gdt = gdalconstConstants.GDT_UInt16;
                outdriver = gdal.GetDriverByName("PNG");
                createOptions = new String[0];
                break;
            case "float" :
                gdt = gdalconstConstants.GDT_Float32;
                outdriver = gdal.GetDriverByName("GTiff");
                // apply compression
                createOptions = new String[]
                        {
                                "PREDICTOR=3",
                                "COMPRESS=DEFLATE",
                        };
                break;
            default :
                return null;
        }
        if(outdriver == null)
            return null;

        final TileTable.ZoomLevelRow zoomLevel = tiles.getInfoForZoomLevel(zoom);

        final String memBufferPath = "/vsimem/" + UUID.randomUUID() + ".tmp";
        try {
            Dataset ds = memdriver.Create("", zoomLevel.tile_width, zoomLevel.tile_height, 1, gdt);
            if (ds == null)
                return null;

            // GDAL will handle data conversion; use of `float` prevents zero'ing of signed short values
            float[] data = new float[zoomLevel.tile_width * zoomLevel.tile_height];
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) encodeHeight(heightmap[i], ancillary_offset, ancillary_scale);
                if(statistics != null && !Double.isNaN(heightmap[i]))
                    statistics.observe(data[i]);
            }
            ds.GetRasterBand(1).WriteRaster(0, 0, zoomLevel.tile_width, zoomLevel.tile_height, data);

            ds.FlushCache();
            Dataset out = outdriver.CreateCopy(memBufferPath, ds, createOptions);
            out.FlushCache();
            out.delete();
            ds.delete();

            // read byte data
            return GdalLibrary.GetMemFileBuffer(memBufferPath);
        } finally {
            gdal.Unlink(memBufferPath);
        }
    }

    public double[] getTile(int zoom, int x, int y) {
        return decodeTile(zoom, x, y, getTileData(zoom, x, y));
    }

    public double[] decodeTile(int zoom, int x, int y, TileData tile) {
        if (tile == null)
            return null;

        final TileTable.ZoomLevelRow zoomLevel = tiles.getInfoForZoomLevel(zoom);
        Dataset dataset = null;
        double[] heightmap;
        double noDataValue = Double.NaN;
        try {
            dataset = GdalLibrary.openDatasetFromMemory(tile.data);
            if(dataset == null)
                return null;
            if(zoomLevel.tile_width != dataset.GetRasterXSize() || zoomLevel.tile_height != dataset.GetRasterYSize())
                return null;
            heightmap = new double[zoomLevel.tile_width*zoomLevel.tile_height];
            final Band band = dataset.GetRasterBand(1);
            Double[] ndv = new Double[1];
            band.GetNoDataValue(ndv);
            if(ndv[0] != null)
                noDataValue = ndv[0];
            band.ReadRaster(
                    0, 0,
                    zoomLevel.tile_width, zoomLevel.tile_height,
                    heightmap);
        } finally {
            if(dataset != null)
                dataset.delete();
        }

        if(Double.isNaN(noDataValue))
            noDataValue = data_null_value;

        for(int i = 0; i < heightmap.length; i++) {
            if(heightmap[i] == noDataValue)
                heightmap[i] = Double.NaN;
            else
                heightmap[i] = ((heightmap[i] * tile.scale + tile.offset) * this.scale + this.offset) * unitsToMeters;
        }

        return heightmap;
    }

    public TileData getTileData(int zoom, int x, int y) {
        try(QueryIface query = gpkg.getDatabase().compileQuery(
                "SELECT tiles.tile_data, ancillary.scale, ancillary.offset " +
                        "FROM '" + tile_matrix_set_name + "' AS tiles " +
                        "JOIN 'gpkg_2d_gridded_tile_ancillary' AS ancillary " +
                        "ON tiles.id = ancillary.tpudt_id " +
                        "WHERE "
                        + "tiles.zoom_level = ? AND "
                        + "tiles.tile_column = ? AND "
                        + "tiles.tile_row = ? LIMIT 1")) {

            query.bind(1, zoom);
            query.bind(2, x);
            query.bind(3, y);

            if(!query.moveToNext())
                return null;
            final TileData tile = new TileData();
            tile.data = query.getBlob(0);
            tile.scale = query.getDouble(1);
            tile.offset = query.getDouble(2);
            return (tile.data == null) ? null : tile;
        }
    }

    double encodeHeight(double height, double ancillary_offset, double ancillary_scale) {
        return Double.isNaN(height) ?
                data_null_value :
                ((((height/unitsToMeters)-offset)/scale)-ancillary_offset)/ancillary_scale;
    }

    double decodeHeight(double height, double ancillary_offset, double ancillary_scale) {
        return (height == data_null_value) ?
                    Double.NaN :
                    ((height * ancillary_scale + ancillary_offset) * this.scale + this.offset) * unitsToMeters;
    }

    public static GriddedCoverage getPackageExtension(GeoPackage gpkg, String tableName) {
        GriddedCoverage coverage = new GriddedCoverage();
        boolean hasCoverageAncillary = false;
        boolean hasTileAncillary = false;
        for(GeoPackage.ExtensionsRow extension : gpkg.getPackageExtensions()) {
            if(extension.extension_name.equals(NAME)) {
                if(Objects.equals(extension.table_name, TABLE_gpkg_2d_gridded_coverage_ancillary)) {
                    try(QueryIface result = gpkg.getDatabase().compileQuery("SELECT datatype, scale, offset, precision, data_null, grid_cell_encoding, uom, field_name, quantity_definition FROM " + TABLE_gpkg_2d_gridded_coverage_ancillary + " WHERE tile_matrix_set_name = ?")) {
                        result.bind(1, tableName);
                        if(result.moveToNext()) {
                            coverage.datatype = result.getString(0);
                            coverage.scale = result.getDouble(1);
                            coverage.offset = result.getDouble(2);
                            coverage.precision = result.getDouble(3);
                            coverage.data_null_value = result.getDouble(4);
                            coverage.grid_cell_encoding = result.getString(5);
                            coverage.uom = result.getString(6);
                            coverage.field_name = result.getString(7);
                            coverage.quantity_definition = result.getString(8);
                            hasCoverageAncillary = true;
                        }
                    }
                } else if(Objects.equals(extension.table_name, TABLE_gpkg_2d_gridded_tile_ancillary)) {
                    hasTileAncillary = true;
                } else if(Objects.equals(extension.table_name, tableName) && Objects.equals(extension.column_name, "tile_data")) {
                    coverage.tile_matrix_set_name = extension.table_name;

                    coverage.gpkg = gpkg;
                    coverage.tiles = gpkg.getTileTable(coverage.tile_matrix_set_name);
                    final UnitsOfMeasure.Length uom = UnitsOfMeasure.Length.fromCode(coverage.uom);
                    if(uom != null)
                        coverage.unitsToMeters = uom.toMeters;
                }
            }
        }
        return (hasCoverageAncillary && hasTileAncillary && coverage.tile_matrix_set_name != null) ?
                    coverage : null;
    }

    public static List<GriddedCoverage> getPackageExtensions(GeoPackage gpkg) {
        List<GriddedCoverage> extensions = new LinkedList<>();
        for(GeoPackage.ExtensionsRow extension : gpkg.getPackageExtensions()) {
            if(extension.extension_name.equals(NAME) && Objects.equals(extension.column_name, "tile_data")) {
                final GriddedCoverage griddedCoverage = getPackageExtension(gpkg, extension.table_name);
                if(griddedCoverage != null)
                    extensions.add(griddedCoverage);
            }
        }
        return extensions;
    }

    /**
     * Inserts the {@code 2d-gridded-coverage} extension, creating the associated Tile Matrix Set using a default quadtree grid definition for the specified SRID.
     * <P>The following SRIDs are supported:
     * <UL>
     *     <LI>{@code 3395}</LI>
     *     <LI>{@code 3857}</LI>
     *     <LI>{@code 4326}</LI>
     *     <LI>{@code 4979}</LI>
     * </UL>
     * @param gpkg      The geopackage
     * @param coverage  The coverage definition
     * @param srid      The SRID
     * @param minZoom   The min zoom level (inclusive)
     * @param maxZoom   The max zoom level (inclusive)
     * @param bounds    The bounds (informative; optional)
     * @return  {@code false} if the SRID is not recognized and the extension was not created
     */
    public static boolean insertGriddedCoverage(GeoPackage gpkg, GriddedCoverage coverage, int srid, int minZoom, int maxZoom, Envelope bounds) {
        final PointD gridOrigin;
        final TileTable.ZoomLevelRow zoom0;
        switch(srid) {
            case 4326 :
                gridOrigin = TileGrid.WGS84.origin;
                zoom0 = TileTable.ZoomLevelRow.SRS_4326_Z0;
                break;
            case 4979 :
                gridOrigin = TileGrid.WGS84_3D.origin;
                zoom0 = TileTable.ZoomLevelRow.CRS_4979_Z0;
                break;
            case 3857 :
                gridOrigin = TileGrid.WebMercator.origin;
                zoom0 = TileTable.ZoomLevelRow.SRS_3857_Z0;
                break;
            case 3395 :
                gridOrigin = TileGrid.WorldMercator.origin;
                zoom0 = TileTable.ZoomLevelRow.SRS_3395_Z0;
                break;
            default :
                return false;
        }

        TileTable.ZoomLevelRow[] zoomLevels = GeoPackage.createQuadtree(
                zoom0,
                maxZoom+1
        );
        if(minZoom > 0)
            zoomLevels = Arrays.copyOfRange(zoomLevels, minZoom, maxZoom);

        TileTable.TileMatrixSet matrix = new TileTable.TileMatrixSet();
        matrix.table_name = coverage.tile_matrix_set_name;
        matrix.min_x = gridOrigin.x;
        matrix.min_y = gridOrigin.y - zoomLevels[0].matrix_height*zoomLevels[0].tile_height*zoomLevels[0].pixel_y_size;
        matrix.max_x = gridOrigin.x + zoomLevels[0].matrix_width*zoomLevels[0].tile_width*zoomLevels[0].pixel_x_size;
        matrix.max_y = gridOrigin.y;
        matrix.srs_id = srid;

        GriddedCoverage.insertGriddedCoverage(gpkg, coverage, matrix, zoomLevels, bounds);
        return true;
    }

    /**
     *
     * @param gpkg          The geopackage
     * @param coverage      The coverage definition
     * @param matrix        The tile matrix
     * @param zoomLevels    The zoom levels definition
     * @param bounds        The bounds of the tiles, in the matrix SRS. Bounds are informative only and not required.
     */
    public static void insertGriddedCoverage(GeoPackage gpkg, GriddedCoverage coverage, TileTable.TileMatrixSet matrix, TileTable.ZoomLevelRow[] zoomLevels, Envelope bounds) {
        // insert 4979 (if not present)
        CrsWkt.insertExtension(gpkg);
        final CrsWkt crsWkt = CrsWkt.getPackageExtension(gpkg);
        if(crsWkt == null)
            throw new IllegalStateException();

        final CrsWkt.CRS crs4979 = new CrsWkt.CRS();
        crs4979.srs_name = "WGS 84 3D";
        crs4979.srs_id = 4979;
        crs4979.organization = "EPSG";
        crs4979.organization_coordsys_id = 4979;
        crs4979.definition = "undefined";
        crs4979.description = null;
        crs4979.definition_12_063 = WKT_definition_12_063_4979;

        crsWkt.insertSRS(gpkg, crs4979);
        if(matrix.srs_id != 4979)
            gpkg.insertSRS(matrix.srs_id);

        // NOTE: bounds on the content row is informative
        GeoPackage.ContentsRow content = new GeoPackage.ContentsRow();
        content.data_type = GeoPackage.TableType.GRIDDED_COVERAGE;
        content.description = coverage.tile_matrix_set_name;
        content.identifier = GeoPackageTileContainer.sanitizeTableName(coverage.tile_matrix_set_name);
        if(bounds != null) {
            content.min_x = bounds.minX;
            content.min_y = bounds.minY;
            content.max_x = bounds.maxX;
            content.max_y = bounds.maxY;
        }
        content.srs_id = matrix.srs_id;
        content.table_name = coverage.tile_matrix_set_name;

        // insert the content row and tiles table
        gpkg.insertTileTable(content, matrix, zoomLevels);

        // insert the extensions
        GeoPackage.ExtensionsRow[] extensions =
        {
            new GeoPackage.ExtensionsRow.Builder(NAME, DEFINITION).setTable(TABLE_gpkg_2d_gridded_coverage_ancillary).setScope(GeoPackage.ScopeType.READ_WRITE).build(),
            new GeoPackage.ExtensionsRow.Builder(NAME, DEFINITION).setTable(TABLE_gpkg_2d_gridded_tile_ancillary).setScope(GeoPackage.ScopeType.READ_WRITE).build(),
            new GeoPackage.ExtensionsRow.Builder(NAME, DEFINITION).setTable(coverage.tile_matrix_set_name).setColumnName("tile_data").setScope(GeoPackage.ScopeType.READ_WRITE).build(),
        };
        for(GeoPackage.ExtensionsRow extension : extensions)
            gpkg.insertExtension(extension, false);

        // create the tables
        if(!gpkg.hasTable(TABLE_gpkg_2d_gridded_coverage_ancillary))
            createCoverageAncillaryTable(gpkg.getDatabase());
        if(!gpkg.hasTable(TABLE_gpkg_2d_gridded_tile_ancillary))
            createTileAncillaryTable(gpkg.getDatabase());

        // add row to the coverage ancillary table
        StatementIface stmt = null;
        try {
            stmt = gpkg.getDatabase().compileStatement("INSERT INTO " + TABLE_gpkg_2d_gridded_coverage_ancillary + "(tile_matrix_set_name, datatype, scale, offset, precision, data_null, grid_cell_encoding, uom, field_name, quantity_definition) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.bind(1, coverage.tile_matrix_set_name);
            stmt.bind(2, coverage.datatype);
            stmt.bind(3, coverage.scale);
            stmt.bind(4, coverage.offset);
            stmt.bind(5, coverage.precision);
            stmt.bind(6, coverage.data_null_value);
            stmt.bind(7, coverage.grid_cell_encoding);
            stmt.bind(8, coverage.uom);
            stmt.bind(9, coverage.field_name);
            stmt.bind(10, coverage.quantity_definition);

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    static void createCoverageAncillaryTable(DatabaseIface db) {
        db.execute("CREATE TABLE 'gpkg_2d_gridded_coverage_ancillary' (\n" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                "tile_matrix_set_name TEXT NOT NULL UNIQUE,\n" +
                "datatype TEXT NOT NULL DEFAULT 'integer',\n" +
                "scale REAL NOT NULL DEFAULT 1.0,\n" +
                "offset REAL NOT NULL DEFAULT 0.0,\n" +
                "precision REAL DEFAULT 1.0,\n" +
                "data_null REAL,\n" +
                "grid_cell_encoding TEXT DEFAULT 'grid-value-is-center',\n" +
                "uom TEXT,\n" +
                "field_name TEXT DEFAULT 'Height',\n" +
                "quantity_definition TEXT DEFAULT 'Height',\n" +
                "CONSTRAINT fk_g2dgtct_name FOREIGN KEY('tile_matrix_set_name')\n" +
                "REFERENCES\n" +
                "gpkg_tile_matrix_set ( table_name )\n" +
                "CHECK (datatype in ('integer','float')));", null);
    }

    static void createTileAncillaryTable(DatabaseIface db) {
        db.execute("CREATE TABLE gpkg_2d_gridded_tile_ancillary (\n" +
                " id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                " tpudt_name TEXT NOT NULL,\n" +
                " tpudt_id INTEGER NOT NULL,\n" +
                " scale REAL NOT NULL DEFAULT 1.0,\n" +
                " offset REAL NOT NULL DEFAULT 0.0,\n" +
                " min REAL DEFAULT NULL,\n" +
                " max REAL DEFAULT NULL,\n" +
                " mean REAL DEFAULT NULL,\n" +
                " std_dev REAL DEFAULT NULL,\n" +
                " CONSTRAINT fk_g2dgtat_name FOREIGN KEY (tpudt_name) REFERENCES\n" +
                "gpkg_contents(table_name),\n" +
                "UNIQUE (tpudt_name, tpudt_id));", null);
    }

    public final static class TileData {
        public byte[] data = null;
        public double scale = 1d;
        public double offset = 0d;
    }
}
