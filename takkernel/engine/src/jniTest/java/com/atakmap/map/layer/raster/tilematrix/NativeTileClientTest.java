package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.geometry.Envelope;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import gov.tak.test.KernelJniTest;

public class NativeTileClientTest extends KernelJniTest {

    private static class MockCacheRequestListener implements CacheRequestListener {
        boolean requestStarted;
        boolean requestComplete;
        boolean requestProgress;
        int taskNum, numTasks, taskProgress, maxTaskProgress, totalProgress, maxTotalProgress;
        boolean requestError;
        String message;
        boolean fatal;
        boolean requestCanceled;

        @Override
        public void onRequestStarted() {
            requestStarted = true;
        }

        @Override
        public void onRequestComplete() {
            requestComplete = true;
        }

        @Override
        public void onRequestProgress(int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTotalProgress) {
            requestProgress = true;
            this.taskNum = taskNum;
            this.numTasks = numTasks;
            this.taskProgress = taskProgress;
            this.maxTaskProgress = maxTaskProgress;
            this.totalProgress = totalProgress;
            this.maxTotalProgress = maxTotalProgress;
        }

        @Override
        public boolean onRequestError(Throwable t, String message, boolean fatal) {
            this.message = message;
            this.fatal = fatal;
            this.requestError = true;
            return false;
        }

        @Override
        public void onRequestCanceled() {
            this.requestCanceled = true;
        }
    }

    @Test
    public void wrap_roundtrip_test() {
        String name = "name";
        int srid = 1;
        TileMatrix.ZoomLevel[] level = new TileMatrix.ZoomLevel[1];
        level[0] = new TileMatrix.ZoomLevel();
        TileMatrix.ZoomLevel l = level[0];
        l.level = 1;
        l.pixelSizeX = 2;
        l.pixelSizeY = 3;
        l.tileHeight = 4;
        l.tileWidth = 5;
        l.resolution = 6;
        double ox = 1.0;
        double oy = 2.0;
        Envelope env = new Envelope(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
        MockTileClientJNI managed = new MockTileClientJNI(name, srid, level, ox, oy, env);
        TileClient wrapped = new NativeTileClient(NativeTileClient.wrap(managed), null);

        Assert.assertEquals(name, wrapped.getName());
        Assert.assertEquals(srid, wrapped.getSRID());
        Assert.assertEquals(ox, wrapped.getOriginX(), 0.0);
        Assert.assertEquals(oy, wrapped.getOriginY(), 0.0);

        TileMatrix.ZoomLevel[] rz = wrapped.getZoomLevel();
        Assert.assertNotNull(rz);
        Assert.assertEquals(rz.length, 1);
        Assert.assertEquals(rz[0].level, l.level);
        Assert.assertEquals(rz[0].pixelSizeX, l.pixelSizeX, 0.0);
        Assert.assertEquals(rz[0].pixelSizeY, l.pixelSizeY, 0.0);
        Assert.assertEquals(rz[0].resolution, l.resolution, 0.0);
        Assert.assertEquals(rz[0].tileHeight, l.tileHeight);
        Assert.assertEquals(rz[0].tileWidth, l.tileWidth);

        Envelope env2 = wrapped.getBounds();
        Assert.assertTrue(env.equals(env2));

        wrapped.clearAuthFailed();
        Assert.assertFalse(managed.authFailed);

        CacheRequest request = new CacheRequest();
        request.cacheFile = new File("no_file_of_mine");
        request.canceled = false;
        request.countOnly = true;
        request.maxResolution = 1.0;
        request.minResolution = 2.0;
        request.expirationOffset = 100;

        MockCacheRequestListener listener = new MockCacheRequestListener();

        // noop, but smoke test
        wrapped.checkConnectivity();
        wrapped.estimateTileCount(request);

        // test listener callbacks
        wrapped.cache(request, listener);
        Assert.assertEquals(listener.message, managed.callback_msg);
        Assert.assertEquals(listener.fatal, managed.callback_fatal);
        Assert.assertTrue(listener.requestCanceled);
        Assert.assertTrue(listener.requestComplete);
        Assert.assertTrue(listener.requestError);
        Assert.assertTrue(listener.requestProgress);
        Assert.assertTrue(listener.requestStarted);
        Assert.assertEquals(listener.taskNum, 1);
        Assert.assertEquals(listener.numTasks, 2);
        Assert.assertEquals(listener.taskProgress, 3);
        Assert.assertEquals(listener.maxTaskProgress, 4);
        Assert.assertEquals(listener.totalProgress, 5);
        Assert.assertEquals(listener.maxTotalProgress, 6);

        Bitmap b = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        int c = 0;
        for (int y = 0; y < b.getHeight(); ++y)
            for (int x = 0; x < b.getWidth(); ++x)
                b.setPixel(x, y, c++);
        managed.tiles.put(MockTileMatrixJNI.getTileKey(1, 2, 3), b);
        Bitmap rb = wrapped.getTile(1, 2, 3, null);
        for (int y = 0; y < b.getHeight(); ++y)
            for (int x = 0; x < b.getWidth(); ++x)
                Assert.assertEquals(b.getPixel(x, y), rb.getPixel(x, y));


        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        managed.setTileData(4, 5, 6, data);
        Assert.assertArrayEquals(data, managed.getTileData(4, 5, 6, null));
    }
}
