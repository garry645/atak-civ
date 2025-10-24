package com.atakmap.map.layer.raster.mbtiles;

import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.lang.Objects;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.ContainerInitializationControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.MockTileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.math.PointI;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MBTilesContainerTest extends KernelJniTest {
    @Test
    public void create_with_null_spec_creates_db() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            TileContainer tiles = MBTilesContainer.SPI.create("test", f.getPath(), null);
            Assert.assertNotNull(tiles);
            tiles.dispose();

            Assert.assertTrue(f.file.exists());
            DatabaseIface db = Databases.openDatabase(f.getPath(), true);
            Assert.assertTrue(MBTilesContainer.isCompatibleSchema(db));
            db.close();
        }
    }

    @Test
    public void create_with_invalid_spec_returns_null() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            TileMatrix.ZoomLevel z0 = new TileMatrix.ZoomLevel();
            z0.tileWidth = 256;
            z0.tileHeight = 256;
            z0.pixelSizeX = 180d / z0.tileWidth;
            z0.pixelSizeY = 180d / z0.tileHeight;
            z0.resolution = OSMUtils.mapnikTileResolution(1);
            TileMatrix mx = new MockTileContainer(
                    "mock",
                    4326,
                    TileMatrix.Util.createQuadtree(z0, 20),
                    -180d, -90d,
                    new Envelope(-180d, -90d, 0d, 180d, 90d, 0d),
                    true);
            TileContainer tiles = MBTilesContainer.SPI.create("test", f.getPath(), mx);
            Assert.assertNull(tiles);
        }
    }

    static MBTilesContainer createSchemaContainer(File f, MBTilesContainer.DbSchema.Spi spi, Map<String, Object> metadata) {
        if(spi != null) {
            DatabaseIface db = Databases.openOrCreateDatabase(f.getAbsolutePath());
            MBTilesContainer.DbSchema schema = null;
            try {
                schema = spi.newInstance();
                schema.createTables(db, metadata);
            } finally {
                if (schema != null)
                    schema.dispose();
            }
            return new MBTilesContainer(f, spi.getName(), 3857, 20, db, false);
        } else {
            TileContainer container = MBTilesContainer.SPI.open(f.getAbsolutePath(), null, false);
            if(container == null)
                container = MBTilesContainer.SPI.create("<null>", f.getAbsolutePath(), null);
            Assert.assertNotNull(container);
            Assert.assertTrue(container instanceof MBTilesContainer);
            if(metadata != null) {
                ContainerInitializationControl ctrl = ((MBTilesContainer) container).getControl(ContainerInitializationControl.class);
                Assert.assertNotNull(ctrl);
                ctrl.setMetadata(metadata);
            }
            return (MBTilesContainer) container;
        }
    }

    void schema_create_roundtrip(File f, MBTilesContainer.DbSchema.Spi spi) {
        try(TestScript script = new TestScript(f, spi)) {
            script
                    .insert(0, 0, 0)
                    .insert(1, 0, 0)
                    .insert(2, 1, 0)
                    .insert(3, 2, 0)
                    .insert(3, 2, 1)
                    .insert(3, 3, 0)
                    .insert(3, 3, 1)
                    .verifyContainer()
                    .verifySchema()
                .closeContainer()
                    .verifyContainer()
                    .verifySchema();
        }
    }

    @Test
    public void schema_create_roundtrip() {
        for(MBTilesContainer.DbSchema.Spi spi : MBTilesContainer._schemaSpis) {
            try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
                schema_create_roundtrip(f.file, spi);
            }
        }
    }

    @Test
    public void spi_create_roundtrip() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            schema_create_roundtrip(f.file, null);
        }
    }

    void schema_update_roundtrip(File f, MBTilesContainer.DbSchema.Spi spi) {
        try(TestScript script = new TestScript(f, spi)) {
            script
                    .insert(0, 0, 0)
                    .insert(1, 0, 0)
                    .insert(2, 1, 0)
                    .insert(3, 2, 0)
                    .insert(3, 2, 1)
                    .insert(3, 3, 0)
                    .insert(3, 3, 1)
                    .verifyContainer()
                    .verifySchema()
                .closeContainer()
                    .verifyContainer()
                    .verifySchema()
                    .update(2, 1, 0)
                    .update(3, 2, 0)
                    .verifyContainer()
                .closeContainer()
                    .verifyContainer()
                    .verifySchema();
        }
    }

    @Test
    public void schema_update_roundtrip() {
        for(MBTilesContainer.DbSchema.Spi spi : MBTilesContainer._schemaSpis) {
            try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
                schema_update_roundtrip(f.file, spi);
            }
        }
    }

    @Test
    public void spi_update_roundtrip() {
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            schema_update_roundtrip(f.file, null);
        }
    }

    static class TestScript implements AutoCloseable {
        Map<PointI, byte[]> tiles = new HashMap<>();
        File f;
        MBTilesContainer.DbSchema.Spi spi;
        MBTilesContainer container;
        final String assertTag;

        TestScript(File f, MBTilesContainer.DbSchema.Spi spi) {
            this.f = f;
            this.spi = spi;
            this.assertTag = (spi != null) ? "[schema: " + spi.getName() + "]" : "[schema: <null>]";
        }

        private void ensureContainer() {
            if(container == null)
                container = createSchemaContainer(f, spi, null);
            Assert.assertFalse(assertTag, container.db.isReadOnly());
        }

        TestScript closeContainer() {
            if(container != null) {
                container.dispose();
                container = null;
            }
            return this;
        }

        TestScript verifySchema() {
            if(spi == null)
                return this;
            if(container != null) {
                Assert.assertTrue(spi.matches(container.db));
            } else {
                DatabaseIface db = null;
                try {
                    Assert.assertTrue(spi.matches(db));
                } finally {
                    if(db != null)
                        db.close();
                }
            }
            return this;
        }

        TestScript verifyContainer() {
            ensureContainer();

            for(Map.Entry<PointI, byte[]> entry : tiles.entrySet()) {
                final PointI tileIndex = entry.getKey();
                byte[] data = container.getTileData(tileIndex.z, tileIndex.x, tileIndex.y, null);
                if(entry.getValue() == null) {
                    Assert.assertNull(assertTag, data);
                } else {
                    Assert.assertArrayEquals(assertTag, entry.getValue(), data);
                }
            }

            return this;
        }

        private TestScript setTile(String msg, int zoom, int x, int y) {
            ensureContainer();
            byte[] data = new String(assertTag + "::" + msg + " {" + zoom + "," + x + "," + y + "}").getBytes();
            container.setTile(zoom, x, y, data, -1L);
            tiles.put(new PointI(x, y, zoom), data);
            return this;
        }

        TestScript insert(int zoom, int x, int y) {
            return setTile("insert", zoom, x, y);
        }

        TestScript update(int zoom, int x, int y) {
            Assert.assertTrue(assertTag, tiles.containsKey(new PointI(x, y, zoom)));
            return setTile("update", zoom, x, y);
        }

        @Override
        public void close()  {
            closeContainer();
        }
    }

    void schema_create_metadata_roundtrip(File f, MBTilesContainer.DbSchema.Spi spi, Map<String, Object> metadata) {
        MBTilesContainer container = createSchemaContainer(f, spi, metadata);
        TilesMetadataControl ctrl = container.getControl(TilesMetadataControl.class);
        final String assertTag = (spi != null) ? "[schema: " + spi.getName() + "]" : "[schema: <null>]";
        Assert.assertNotNull(assertTag, ctrl);
        assertContainsAll(assertTag, metadata, ctrl.getMetadata());
    }

    @Test
    public void schema_create_metadata_roundtrip() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("format", "pbf");
        metadata.put("type", "overlay");
        metadata.put("name", "METADATA ROUNDTRIP");
        for(MBTilesContainer.DbSchema.Spi spi : MBTilesContainer._schemaSpis) {
            try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
                schema_create_metadata_roundtrip(f.file, spi, metadata);
            }
        }
    }

    @Test
    public void spi_create_metadata_roundtrip() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("format", "pbf");
        metadata.put("type", "overlay");
        metadata.put("name", "METADATA ROUNDTRIP");
        try(FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile.createTempFile(getTestContext())) {
            schema_create_metadata_roundtrip(f.file, null, metadata);
        }
    }

    static <K, V> void assertContainsAll(String msg, Map<K, V> expected, Map<K, V> actual) {
        boolean containsAll = true;
        for(Map.Entry<K, V> entry : expected.entrySet()) {
            if(!actual.containsKey(entry.getKey()) || !Objects.equals(actual.get(entry.getKey()), entry.getValue())) {
                containsAll = false;
                break;
            }
        }
        if(!containsAll)
            Assert.assertEquals(msg, expected, actual);
    }
}
