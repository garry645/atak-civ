package com.atakmap.map.layer.raster.gpkg;


import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunkSpi;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.gpkg.TileTable;
import com.atakmap.map.gpkg.extensions.GriddedCoverage;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.raster.controls.TileMetadataControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileGrid;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.math.PointI;
import com.atakmap.spatial.GeometryTransformer;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GeoPackageTileContainerTest extends KernelJniTest {
    @Test
    public void open_existing_griddedcoverage() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            final int expectedWidth = 256;
            final int expectedHeight = 256;
            final double[] inTileData = new double[expectedWidth*expectedHeight];
            Arrays.fill(inTileData, 128d);

            GeoPackage gpkg = null;
            try {
                gpkg = GeoPackage.createNewGeoPackage(f.file);

                GriddedCoverage coverage = new GriddedCoverage();
                coverage.tile_matrix_set_name = "open_existing_griddedcoverage";
                coverage.datatype = "float";
                coverage.data_null_value = -999999.0;

                GriddedCoverage.insertGriddedCoverage(gpkg, coverage, 4979, 0, 0, null);
                coverage = GriddedCoverage.getPackageExtension(gpkg, coverage.tile_matrix_set_name);

                final TileTable.ZoomLevelRow zoomLevel = gpkg.getTileTable(coverage.tile_matrix_set_name).getInfoForZoomLevel(0);
                Assert.assertEquals(zoomLevel.tile_width, expectedWidth);
                Assert.assertEquals(zoomLevel.tile_height, expectedHeight);

                coverage.insertTile(0, 0, 0, inTileData, -100d, 2d);
            } finally {
                if(gpkg != null)
                    gpkg.close();
            }

            TileContainer container = GeoPackageTileContainer.SPI.open(f.file.getAbsolutePath(), null, true);
            Assert.assertNotNull(container);

            Assert.assertTrue(container instanceof Controls);
            final Controls ctrls = (Controls) container;

            final GriddedCoverage coverage = ctrls.getControl(GriddedCoverage.class);
            Assert.assertNotNull(coverage);
            final TilesMetadataControl tilesMetadata = ctrls.getControl(TilesMetadataControl.class);
            Assert.assertNotNull(tilesMetadata);

            final Map<String, Object> containerMetadata = tilesMetadata.getMetadata();
            Assert.assertNotNull(containerMetadata);
            Assert.assertEquals("terrain", containerMetadata.get("content"));

            final TileMetadataControl tileMetadata = ctrls.getControl(TileMetadataControl.class);
            Assert.assertNotNull(tileMetadata);

            final GriddedCoverage.TileData encodedTile = new GriddedCoverage.TileData();
            encodedTile.data = container.getTileData(0, 0,  0, null);
            Assert.assertNotNull(encodedTile.data);
            final Map<String, Object> encodedTileMetadata = tileMetadata.getTileMetadata(0, 0, 0);
            Assert.assertNotNull(encodedTileMetadata);
            encodedTile.offset = (Double)encodedTileMetadata.get("offset");
            encodedTile.scale = (Double)encodedTileMetadata.get("scale");


            final double[] outTileData = coverage.decodeTile(0, 0, 0, encodedTile);
            Assert.assertNotNull(outTileData);
            Assert.assertArrayEquals(inTileData, outTileData, 0.001);
        }
    }

    @Test
    public void create_griddedcoverage() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            TileContainer container = GeoPackageTileContainer.SPI.create(
                    "create_griddedcoverage",
                    f.file.getAbsolutePath(),
                    TileContainerFactory.createSpec(
                            null,
                            TileGrid.WGS84_3D,
                            0,
                            18,
                            Collections.singletonMap("content", "terrain")));

            Assert.assertNotNull(container);

            Assert.assertTrue(container instanceof Controls);
            final Controls ctrls = (Controls) container;

            final GriddedCoverage coverage = ctrls.getControl(GriddedCoverage.class);
            Assert.assertNotNull(coverage);
            final TilesMetadataControl tilesMetadata = ctrls.getControl(TilesMetadataControl.class);
            Assert.assertNotNull(tilesMetadata);

            final Map<String, Object> containerMetadata = tilesMetadata.getMetadata();
            Assert.assertNotNull(containerMetadata);
            Assert.assertEquals("terrain", containerMetadata.get("content"));
        }
    }

    @Test
    public void container_elevation_source() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            List<Map.Entry<PointI, Double>> tiles = Arrays.asList(
                    new AbstractMap.SimpleEntry<>(new PointI(0, 0, 0), Double.valueOf(-1)),
                    new AbstractMap.SimpleEntry<>(new PointI(0, 0, 1), Double.valueOf(100)),
                    new AbstractMap.SimpleEntry<>(new PointI(1, 0, 1), Double.valueOf(110)),
                    new AbstractMap.SimpleEntry<>(new PointI(0, 1, 1), Double.valueOf(101)),
                    new AbstractMap.SimpleEntry<>(new PointI(1, 1, 1), Double.valueOf(111)),
                    new AbstractMap.SimpleEntry<>(new PointI(3, 2, 2), Double.valueOf(232))
            );

            GeoPackage gpkg = null;
            try {
                gpkg = GeoPackage.createNewGeoPackage(f.file);

                GriddedCoverage coverage = new GriddedCoverage();
                coverage.tile_matrix_set_name = "open_existing_griddedcoverage";
                coverage.datatype = "float";
                coverage.offset = 0d;
                coverage.scale = 1d;
                coverage.data_null_value = -999999.0;

                GriddedCoverage.insertGriddedCoverage(gpkg, coverage, 4979, 0, 2, null);
                coverage = GriddedCoverage.getPackageExtension(gpkg, coverage.tile_matrix_set_name);

                for(Map.Entry<PointI, Double> tileEntry : tiles) {
                    final PointI tileIndex = tileEntry.getKey();

                    final TileTable.ZoomLevelRow zoomLevel = gpkg.getTileTable(coverage.tile_matrix_set_name).getInfoForZoomLevel(tileIndex.z);
                    final double[] inTileData = new double[zoomLevel.tile_width*zoomLevel.tile_height];
                    Arrays.fill(inTileData, tileEntry.getValue());

                    coverage.insertTile(tileIndex.z, tileIndex.x, tileIndex.y, inTileData);
                }
            } finally {
                if(gpkg != null)
                    gpkg.close();
            }

            TileContainer container = GeoPackageTileContainer.SPI.open(f.file.getAbsolutePath(), null, true);
            Assert.assertNotNull(container);

            TileMatrixElevationSource elevationSource = new TileMatrixElevationSource(container, (ElevationChunkSpi) null, false);

            for(Map.Entry<PointI, Double> tileEntry : tiles) {
                final PointI tileIndex = tileEntry.getKey();

                final Envelope tileBounds = GeometryTransformer.transform(TileMatrix.Util.getTileBounds(container, tileIndex.z, tileIndex.x, tileIndex.y), container.getSRID(), 4326);
                ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
                params.spatialFilter = new Point((tileBounds.minX+tileBounds.maxX)/2d, (tileBounds.minY+tileBounds.maxY)/2d);
                params.maxResolution = container.getZoomLevel()[tileIndex.z].resolution;
                try (ElevationSource.Cursor result = elevationSource.query(params)) {
                    Assert.assertTrue(result.moveToNext());
                    final ElevationChunk chunk = result.get();
                    Assert.assertNotNull(chunk);
                    final double elevation = chunk.sample(
                            (tileBounds.minY+tileBounds.maxY)/2d,
                            (tileBounds.minX+tileBounds.maxX)/2d);
                    Assert.assertEquals(tileEntry.getValue(), elevation, 0.001);
                }
            }
        }
    }
}
