package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.util.Visitor;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

import gov.tak.test.KernelJniTest;

public class TileContainerFactoryTest extends KernelJniTest {

    private static class MockSpi implements TileContainerSpi {

        private String name;

        MockSpi(String name) {
            this.name = name;
        }

        TileContainer mockContainer;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDefaultExtension() {
            return "ext";
        }

        @Override
        public TileContainer create(String name, String path, TileMatrix spec) {
            return mockContainer;
        }

        @Override
        public TileContainer open(String path, TileMatrix spec, boolean readOnly) {
            return mockContainer;
        }

        @Override
        public boolean isCompatible(TileMatrix spec) {
            return this.name.equals("2");
        }
    }

    static class MockContainer implements TileContainer {

        MockContainer() {}

        boolean readOnly = false;
        String name = "mock_name";
        byte[] expect_tile_data;
        int expect_tile_level;
        int expect_tile_x;
        int expect_tile_y;
        long expect_tile_expiration;
        Bitmap expect_bitmap;

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void setTile(int level, int x, int y, byte[] data, long expiration) {
            Assert.assertEquals(level, expect_tile_level);
            Assert.assertEquals(x, expect_tile_x);
            Assert.assertEquals(y, expect_tile_y);
            Assert.assertArrayEquals(data, expect_tile_data);
            Assert.assertEquals(expiration, expect_tile_expiration);
        }

        @Override
        public void setTile(int level, int x, int y, Bitmap data, long expiration) throws TileEncodeException {

        }

        @Override
        public boolean hasTileExpirationMetadata() {
            return false;
        }

        @Override
        public long getTileExpiration(int level, int x, int y) {
            return 0;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSRID() {
            return 0;
        }

        @Override
        public ZoomLevel[] getZoomLevel() {
            return new ZoomLevel[0];
        }

        @Override
        public double getOriginX() {
            return 0;
        }

        @Override
        public double getOriginY() {
            return 0;
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
            return null;
        }

        @Override
        public void dispose() {

        }
    }

    @Test
    public void test_register_and_visit() {
        // Mockito doesn't like mocking the spi for some reason. Something with the native bindings.
        TileContainerSpi spi1 = new MockSpi("1");
        TileContainerSpi spi2 = new MockSpi("2");
        TileContainerFactory.registerSpi(spi1);
        TileContainerFactory.registerSpi(spi2);
        try {
            boolean[] visits = {false, false};
            TileContainerFactory.visitSpis(new Visitor<Collection<TileContainerSpi>>() {
                @Override
                public void visit(Collection<TileContainerSpi> object) {
                    if (object.contains(spi1))
                        visits[0] = true;
                    if (object.contains(spi2))
                        visits[1] = true;
                }
            });
            Assert.assertTrue(visits[0]);
            Assert.assertTrue(visits[1]);
            visits[0] = visits[1] = false;
            TileContainerFactory.visitCompatibleSpis(new Visitor<Collection<TileContainerSpi>>() {
                @Override
                public void visit(Collection<TileContainerSpi> object) {
                    if (object.contains(spi1))
                        visits[0] = true;
                    if (object.contains(spi2))
                        visits[1] = true;
                }
            }, new MockContainer());
            Assert.assertFalse(visits[0]);
            Assert.assertTrue(visits[1]);
        } finally {
            TileContainerFactory.unregisterSpi(spi1);
            TileContainerFactory.unregisterSpi(spi2);
        }
    }

    @Test
    public void test_open() {
        MockContainer mockContainer = new MockContainer();
        MockSpi spi1 = new MockSpi("2");
        spi1.mockContainer = mockContainer;
        TileContainerFactory.registerSpi(spi1);
        try {
            TileContainer container = TileContainerFactory.open("my_path", true, "2");
            Assert.assertNotNull(container);
            Assert.assertSame(mockContainer, container);
            Assert.assertEquals(container.getName(), "mock_name");
        } finally {
            TileContainerFactory.unregisterSpi(spi1);
        }
    }

    @Test
    public void test_wrapped_native() {
        MockSpi spi = new MockSpi("2");
        Pointer pointer = NativeTileContainerSpi.wrap(spi);
        TileContainerSpi wrapped_spi = NativeTileContainerSpi.create(pointer, null);
        Assert.assertNotNull(wrapped_spi);
        Assert.assertNotSame(spi, wrapped_spi);
        Assert.assertEquals(wrapped_spi.getName(), spi.getName());
        Assert.assertEquals(wrapped_spi.getDefaultExtension(), spi.getDefaultExtension());
    }
}
