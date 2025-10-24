package com.atakmap.map.gpkg.extensions;

import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.gpkg.TileTable;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class GriddedCoverageTest extends KernelJniTest {

    @Test
    public void encode_roundtrip() {
        test_encode_roundtrip( 0d, 0d, 1d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(-8d, 0d, 1d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(16d, 0d, 1d, 0d, 1d, 1d, 0xFFFF);

        test_encode_roundtrip(0d, 4d, 2d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(-8d, 4d, 2d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(16d, 4d, 2d, 0d, 1d, 1d, 0xFFFF);

        test_encode_roundtrip(0d, 0d, 1d, -12d, 5d, 1d, 0xFFFF);
        test_encode_roundtrip(-8d, 0d, 1d, -12d, 5d, 1d, 0xFFFF);
        test_encode_roundtrip(16d, 0d, 1d, -12d, 5d, 1d, 0xFFFF);

        test_encode_roundtrip(0d, 4d, 2d, -12d, 5d, 1d, 0xFFFF);
        test_encode_roundtrip(-8d, 4d, 2d, -12d, 5d, 1d, 0xFFFF);
        test_encode_roundtrip(16d, 4d, 2d, -12d, 5d, 1d, 0xFFFF);


        test_encode_roundtrip(Double.NaN, 0d, 1d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(Double.NaN, 4d, 2d, 0d, 1d, 1d, 0xFFFF);
        test_encode_roundtrip(Double.NaN, 0d, 1d, -12d, 5d, 1d, 0xFFFF);
        test_encode_roundtrip(Double.NaN, 4d, 2d, -12d, 5d, 1d, 0xFFFF);

        test_encode_roundtrip(16d, -900, 2d, -0d, 1d, 1d, 0xFFFF);
    }

    void test_encode_roundtrip(double height, double offset, double scale, double ancillary_offset, double ancillary_scale, double unitsScale, double data_null_value) {
        GriddedCoverage coverage = new GriddedCoverage();
        coverage.offset = offset;
        coverage.scale = scale;
        coverage.unitsToMeters = unitsScale;

        final double encoded = coverage.encodeHeight(height, ancillary_offset, ancillary_scale);
        final double decoded = coverage.decodeHeight(encoded, ancillary_offset, ancillary_scale);

        Assert.assertEquals(height, decoded, 0.001);
    }

    @Test
    public void insertExtension_roundtrip() {
        GeoPackage gpkg = null;
        try {
            gpkg = GeoPackage.createNewGeoPackage((File)null);

            final String tile_matrix_set_name = "insertExtension_roundtrip_tpudt";
            final String datatype = "float";
            final double scale = 11d;
            final double offset = 10d;
            final double precision = 0.1d;
            final double data_null_value = -999999.0;
            final String grid_cell_encoding = "grid-value-is-center";
            final String uom = "[foot_us]";
            final String field_name = "Height (Custom)";
            final String quantity_definition = "Height (Custom)";


            final GriddedCoverage coverage = new GriddedCoverage();
            coverage.tile_matrix_set_name = tile_matrix_set_name;
            coverage.datatype = datatype;
            coverage.scale = scale;
            coverage.offset = offset;
            coverage.precision = precision;
            coverage.data_null_value = data_null_value;
            coverage.grid_cell_encoding = grid_cell_encoding;
            coverage.uom = uom;
            coverage.field_name = field_name;
            coverage.quantity_definition = quantity_definition;

            final boolean extensionInserted = GriddedCoverage.insertGriddedCoverage(gpkg, coverage, 3395, 3, 10, null);
            Assert.assertTrue(extensionInserted);

            final GriddedCoverage inserted = GriddedCoverage.getPackageExtension(gpkg, tile_matrix_set_name);
            Assert.assertNotNull(inserted);
            Assert.assertNotNull(inserted.gpkg);
            Assert.assertNotNull(inserted.tiles);

            Assert.assertEquals(tile_matrix_set_name, inserted.tile_matrix_set_name);
            Assert.assertEquals(datatype, inserted.datatype);
            Assert.assertEquals(scale, inserted.scale, 0.0000001d);
            Assert.assertEquals(offset, inserted.offset, 0.0000001d);
            Assert.assertEquals(data_null_value, inserted.data_null_value, 0.0000001d);
            Assert.assertEquals(grid_cell_encoding, inserted.grid_cell_encoding);
            Assert.assertEquals(uom, inserted.uom);
            Assert.assertEquals(field_name, inserted.field_name);

            // verify tables
            Assert.assertTrue(gpkg.hasTable(tile_matrix_set_name));
            Assert.assertTrue(gpkg.hasTable(GriddedCoverage.TABLE_gpkg_2d_gridded_coverage_ancillary));
            Assert.assertTrue(gpkg.hasTable(GriddedCoverage.TABLE_gpkg_2d_gridded_tile_ancillary));
            Assert.assertNotNull(gpkg.getSRSInfo(3395));
        } finally {
            if(gpkg != null)
                gpkg.close();
        }
    }

    @Test
    public void insertTile_float_roundtrip() {
        insertTile_roundtrip("float");
    }

    @Test
    public void insertTile_integer_roundtrip() {
        insertTile_roundtrip("integer");
    }

    void insertTile_roundtrip(String datatype) {
        GeoPackage gpkg = null;
        try {
            gpkg = GeoPackage.createNewGeoPackage((File)null);

            GriddedCoverage coverage = new GriddedCoverage();
            coverage.tile_matrix_set_name = "insertTile_roundtrip_tpudt";
            coverage.datatype = datatype;
            coverage.scale = 0.31d;
            coverage.offset = -11000d;
            coverage.precision = 0.1d;
            coverage.data_null_value = -999999.0;
            coverage.grid_cell_encoding = "grid-value-is-center";
            coverage.uom = null;
            coverage.field_name = "Height";
            coverage.quantity_definition = "Height";

            GriddedCoverage.insertGriddedCoverage(gpkg, coverage, 4979, 0, 0, null);
            coverage = GriddedCoverage.getPackageExtension(gpkg, coverage.tile_matrix_set_name);

            final TileTable.ZoomLevelRow zoomLevel = gpkg.getTileTable(coverage.tile_matrix_set_name).getInfoForZoomLevel(0);
            double[] inTileData = new double[zoomLevel.tile_width*zoomLevel.tile_height];
            int el = -11000;
            for(int i = 0; i < inTileData.length; i++) {
                inTileData[i] = el++;
                if(el > 9000)
                    el = -11000;
            }

            coverage.insertTile(0, 0, 0, inTileData);

            double[] outTileData = coverage.getTile(0, 0, 0);
            Assert.assertNotNull(outTileData);
            Assert.assertArrayEquals(inTileData, outTileData, coverage.scale);
        } finally {
            if(gpkg != null)
                gpkg.close();
        }
    }
}
