package com.atakmap.map.gpkg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.QueryIface;
import com.atakmap.map.gpkg.GeoPackage.ContentsRow;
import com.atakmap.map.layer.raster.osm.OSMUtils;

/**
 * Object class representing the tables required to access tile information from
 * a GeoPackage formatted SQLite database. For more information about the tables
 * related to tile data in a GeoPackage database, see section 2.2 of the GeoPackage
 * specification at http://www.opengeospatial.org/standards/geopackage.
 */
public class TileTable extends GeoPackageContentsTable
{
    private static final String TAG = "TileTable";

    private DatabaseIface database;
    private final String tileQuerySql;

    private int[] zoomLevels;
    private ZoomLevelRow[] zoomLevelRows;

    // Package Private visibility, should only be able to create
    // instances of this class from within the GeoPackage class.
    TileTable(DatabaseIface database, ContentsRow contents)
    {
        super(contents);
        this.database = database;
        this.tileQuerySql = "SELECT tile_data FROM '" + getName() + "' WHERE "
                + "zoom_level = ? AND "
                + "tile_column = ? AND "
                + "tile_row = ?";
    }

    // Stuff from the gpkg_tile_matrix_set table
    public static class TileMatrixSet
    {
        public String table_name;
        public int srs_id;
        public double min_x;
        public double min_y;
        public double max_x;
        public double max_y;
    }

    /**
     * Read the info for this data table from the "gpkg_tile_matrix_set" table. Primarily
     * contained within this info is a minimum bounding box for this data table that will
     * coverage of the layers within the data table.
     *
     * @return This data tables row from the gpkg_tile_matrix_set table.
     */
    public TileMatrixSet getTileMatrixSetInfo()
    {
        QueryIface result = null;
        try
        {
            result = database.compileQuery("SELECT * FROM gpkg_tile_matrix_set WHERE table_name = ?");
            result.bind(1, getName());

            TileMatrixSet returnSet = new TileMatrixSet();

            if (!result.moveToNext())
            {
                return null;
            }

            returnSet.table_name = result.getString(result.getColumnIndex("table_name"));
            returnSet.srs_id = result.getInt(result.getColumnIndex("srs_id"));
            returnSet.min_x = result.getDouble(result.getColumnIndex("min_x"));
            returnSet.min_y = result.getDouble(result.getColumnIndex("min_y"));
            returnSet.max_x = result.getDouble(result.getColumnIndex("max_x"));
            returnSet.max_y = result.getDouble(result.getColumnIndex("max_y"));

            return returnSet;
        } catch (Exception e)
        {
            Log.e(TAG, "Error reading TileMatrixSet Entry", e);
        } finally
        {
            if (result != null)
            {
                result.close();
            }
        }

        return null;
    }

    // Stuff from the gpkg_tile_matrix table
    public static class ZoomLevelRow
    {
        public final static ZoomLevelRow SRS_4326_Z0 = new ZoomLevelRow(0, 2, 1, 256, 256, 180d/256d, 180d/256d);
        public final static ZoomLevelRow CRS_4979_Z0 = new ZoomLevelRow(0, 2, 1, 256, 256, 180d/256d, 180d/256d);
        public final static ZoomLevelRow SRS_3857_Z0 = new ZoomLevelRow(0, 1, 1, 256, 256, OSMUtils.mapnikTileResolution(0), OSMUtils.mapnikTileResolution(0));
        public final static ZoomLevelRow SRS_3395_Z0 = new ZoomLevelRow(0, 1, 1, 256, 256, 156543.034, 156543.034);

        public int zoom_level;
        public int matrix_width;
        public int matrix_height;
        public int tile_width;
        public int tile_height;
        public double pixel_x_size;
        public double pixel_y_size;

        public ZoomLevelRow() {}

        public ZoomLevelRow(int zoomLevel, int matrixWidth, int matrixHeight, int tileWidth, int tileHeight, double pixelXSize, double pixelYSize) {
            zoom_level = zoomLevel;
            matrix_width = matrixWidth;
            matrix_height = matrixHeight;
            tile_width = tileWidth;
            tile_height = tileHeight;
            pixel_x_size = pixelXSize;
            pixel_y_size = pixelYSize;
        }
    }

    private void validateZoomLevels() {
        if(zoomLevels != null)
            return;

        final TileMatrixSet info = getTileMatrixSetInfo();
        try(QueryIface result = database.compileQuery("SELECT zoom_level, matrix_width, matrix_height, tile_width, tile_height, pixel_x_size, pixel_y_size FROM gpkg_tile_matrix WHERE table_name = ?")) {
            result.bind(1, getName());

            ArrayList<ZoomLevelRow> zoomLevelRows = new ArrayList<>();
            while(result.moveToNext()) {
                ZoomLevelRow resultRow = new ZoomLevelRow();
                resultRow.zoom_level = result.getInt(0);
                resultRow.matrix_width = result.getInt(1);
                resultRow.matrix_height = result.getInt(2);
                resultRow.tile_width = result.getInt(3);
                resultRow.tile_height = result.getInt(4);
                resultRow.pixel_x_size = result.getDouble(5);
                resultRow.pixel_y_size = result.getDouble(6);

                // XXX - have observed an issue with GPKG generated by APASS, run
                //       test case ID /opt/tiles/gpkg_tile_matrix/data/data_values_width_height
                if (info != null)
                {
                    final double tileWidthProjUnits = resultRow.tile_width * resultRow.pixel_x_size;
                    final double tileHeightProjUnits = resultRow.tile_height * resultRow.pixel_y_size;

                    final double matrixWidthProjUnits = info.max_x - info.min_x;
                    final double matrixHeightProjUnits = info.max_y - info.min_y;

                    if (Double.compare(matrixWidthProjUnits, tileWidthProjUnits * resultRow.matrix_width) != 0)
                    {
                        final double expectedMatrixWidth = matrixWidthProjUnits / tileWidthProjUnits;
                        if (isBadPixelXYValue(resultRow.pixel_x_size) || Math.abs(expectedMatrixWidth - resultRow.matrix_width) > 1d)
                        {
                            final double pixel_x_size = matrixWidthProjUnits / (double) (resultRow.matrix_width * resultRow.tile_width);
                            Log.w(TAG, "Bad pixel_x_size encountered for level " + resultRow.zoom_level + ", computing value " + resultRow.pixel_x_size + " ---> " + pixel_x_size);
                            resultRow.pixel_x_size = pixel_x_size;
                        } else
                        {
                            Log.w(TAG, "Possible precision error for pixel_x_size encountered for level " + resultRow.zoom_level + ", tile registration errors may occur.");
                        }

                    }
                    if (Double.compare(matrixHeightProjUnits, tileHeightProjUnits * resultRow.matrix_height) != 0)
                    {
                        final double expectedMatrixHeight = matrixHeightProjUnits / tileHeightProjUnits;
                        if (isBadPixelXYValue(resultRow.pixel_y_size) || Math.abs(expectedMatrixHeight - resultRow.matrix_height) > 1d)
                        {
                            final double pixel_y_size = matrixHeightProjUnits / (double) (resultRow.matrix_height * resultRow.tile_height);
                            Log.w(TAG, "Bad pixel_y_size encountered for level " + resultRow.zoom_level + ", computing value " + resultRow.pixel_y_size + " ---> " + pixel_y_size);
                            resultRow.pixel_y_size = pixel_y_size;
                        } else
                        {
                            Log.w(TAG, "Possible precision error for pixel_y_size encountered for level " + resultRow.zoom_level + ", tile registration errors may occur.");
                        }
                    }
                }

                zoomLevelRows.add(resultRow);
            }

            this.zoomLevelRows = zoomLevelRows.toArray(new ZoomLevelRow[0]);
            this.zoomLevels = new int[this.zoomLevelRows.length];
            for(int i = 0; i < this.zoomLevelRows.length; i++)
                this.zoomLevels[i] = this.zoomLevelRows[i].zoom_level;
        }
    }
    /**
     * Get a list of the zoom levels provided by this GeoPackage file these are not guaranteed to
     * be contiguous, but they will be sorted in ascending order.
     *
     * @return Array containing a list of integer indexes representing zoom levels in the GeoPackage.
     */
    public int[] getZoomLevels()
    {
        validateZoomLevels();
        return zoomLevels;
    }

    /**
     * Get the info associated with a zoom level within this data table.
     *
     * @param zoomLevel Zoom level to retrieve info for.
     * @return ZoomLevelRow object containing the info for this zoom level,
     * or null if that zoom level does not exist.
     */
    public ZoomLevelRow getInfoForZoomLevel(int zoomLevel)
    {
        validateZoomLevels();
        for(ZoomLevelRow zoomLevelRow : zoomLevelRows)
            if(zoomLevelRow.zoom_level == zoomLevel)
                return zoomLevelRow;
        return null;
    }

    private static boolean isBadPixelXYValue(double value)
    {
        return value == 0d ||
                Double.isNaN(value) ||
                Double.isInfinite(value);
    }

    // Stuff from data table

    /**
     * Retrieve the binary data for a tile within this data table. This tile may be compressed
     * as either PNG or JPEG. The GeoPackage format does not specify a means for defining
     * what format tile data is stored in, so the binary data must be inspected for magic numbers
     * to determine it's type. Additionally, the GeoPackage format specifies that data does
     * not need to be stored in a consistent format across all tiles in the data set, so
     * it cannot be assumed that all tiles share the same format or bit depth.
     *
     * @param zoom Zoom level of the desired tile
     * @param x    Column number for the desired tile with in the tile matrix.
     * @param y    Row number for the desired tile within the tile matrix
     * @return Binary data representing the tile, in either PNG or JPEG format, or
     * null if that tile does not exist in the tile data table.
     */
    public byte[] getTile(int zoom, int x, int y)
    {
        QueryIface result = null;
        try
        {
            result = database.compileQuery(tileQuerySql);
            result.bind(1, zoom);
            result.bind(2, x);
            result.bind(3, y);

            if (!result.moveToNext())
            {
                return null;
            }

            return result.getBlob(0);
        } catch (Exception e)
        {
            Log.e(TAG, "Error reading ZoomLevelInfo Entry", e);
        } finally
        {
            if (result != null)
            {
                result.close();
            }
        }

        return null;
    }
}
