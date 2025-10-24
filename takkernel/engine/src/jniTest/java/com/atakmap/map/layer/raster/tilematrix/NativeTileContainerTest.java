package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.map.layer.feature.geometry.Envelope;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class NativeTileContainerTest extends KernelJniTest {
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
        boolean readOnly = true;
        Envelope env = new Envelope(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
        MockTileContainerJNI managed = new MockTileContainerJNI(name, srid, level, ox, oy, env, readOnly);
        TileContainer wrapped = new NativeTileContainer(NativeTileContainer.wrap(managed), null);

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
        Assert.assertEquals(readOnly, wrapped.isReadOnly());
        Assert.assertTrue(wrapped.hasTileExpirationMetadata());


        Bitmap b = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        int c = 0;
        for (int y = 0; y < b.getHeight(); ++y)
            for (int x = 0; x < b.getWidth(); ++x)
                b.setPixel(x, y, c++);
        try {
            wrapped.setTile(1,2,3, b, 4);
        } catch (TileEncodeException e) {
            Assert.fail(e.getMessage());
        }
        Bitmap rb = wrapped.getTile(1, 2, 3, null);
        for (int y = 0; y < b.getHeight(); ++y)
            for (int x = 0; x < b.getWidth(); ++x)
                Assert.assertEquals(b.getPixel(x, y), rb.getPixel(x, y));

        Assert.assertEquals(wrapped.getTileExpiration(1, 2, 3), 4);

        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        managed.setTile(4, 5, 6, data, 9);
        Assert.assertArrayEquals(data, managed.getTileData(4, 5, 6, null));
        Assert.assertEquals(managed.getTileExpiration(4, 5, 6), 9);
    }
}
