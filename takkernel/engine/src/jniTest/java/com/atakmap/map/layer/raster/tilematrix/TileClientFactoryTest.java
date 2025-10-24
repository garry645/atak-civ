package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.geometry.Envelope;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

import gov.tak.test.KernelJniTest;

public class TileClientFactoryTest extends KernelJniTest {

    private static class MockClient implements TileClient {

        @Override
        public <T> T getControl(Class<T> controlClazz) {
            return null;
        }

        @Override
        public void getControls(Collection<Object> controls) {

        }

        @Override
        public void clearAuthFailed() {

        }

        @Override
        public void checkConnectivity() {

        }

        @Override
        public void cache(CacheRequest request, CacheRequestListener listener) {

        }

        @Override
        public int estimateTileCount(CacheRequest request) {
            return 0;
        }

        @Override
        public String getName() {
            return null;
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

    private static class MockSpi implements TileClientSpi {

        String name = "mock_name";
        int priority = 42;
        TileClient createResult;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public TileClient create(String path, String offlineCachePath, Options opts) {
            return createResult;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }
    }

    @Test
    public void register_unregister_smoke_test() {
        MockSpi spi = new MockSpi();
        TileClientFactory.registerSpi(spi);
        TileClientFactory.unregisterSpi(spi);
    }

    @Test
    public void create_marshal_test() {
        MockSpi spi = new MockSpi();
        MockClient client = new MockClient();
        spi.createResult = client;
        TileClientFactory.registerSpi(spi);
        try {
            TileClient createClient = TileClientFactory.create("my_path", "my_cache", new TileClientSpi.Options());
            Assert.assertNotNull(createClient);
            Assert.assertSame(client, createClient);
        } finally {
            TileClientFactory.unregisterSpi(spi);
        }
    }
}
