package com.atakmap.map.formats.mapbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.math.MathUtils;
import com.atakmap.util.ResourcePool;

import gov.tak.api.engine.map.coords.GeoCalculations;

final class TerrainRGB extends ElevationChunk.Factory.Sampler
{
    final int srid;
    final boolean orthometric;
    final int tileColumn;
    final int tileRow;
    final int tileZoom;

    // data and/or the dataArray* members will be null, depending on constructor form used
    Bitmap data;
    byte[] dataArray;
    int dataArrayOffset;
    int dataArrayLen;
    ResourcePool<Bitmap> recyclePool;

    public TerrainRGB(Bitmap data, int srid, boolean orthometric, int x, int y, int z, ResourcePool<Bitmap> recycleOnDispose)
    {
        this.data = data;

        this.srid = srid;
        this.orthometric = orthometric;
        this.tileColumn = x;
        this.tileRow = y;
        this.tileZoom = z;
        this.recyclePool = recycleOnDispose;
    }
    // Lazily creates Bitmap from dataArray, off, len against the recyclePool on first sample call
    public TerrainRGB(byte[] dataArray, int off, int len, int srid, boolean orthometric, int x, int y, int z, ResourcePool<Bitmap> recyclePool)
    {
        this.dataArray = dataArray;
        this.dataArrayOffset = off;
        this.dataArrayLen = len;

        this.srid = srid;
        this.orthometric = orthometric;
        this.tileColumn = x;
        this.tileRow = y;
        this.tileZoom = z;
        this.recyclePool = recyclePool;
    }

    @Override
    public double sample(double latitude, double longitude)
    {
        final double x;
        final double y;
        if(srid == 3857) {
            x = OSMUtils.mapnikPixelXd(tileZoom, tileColumn, longitude);
            y = OSMUtils.mapnikPixelYd(tileZoom, tileRow, latitude);
        } else if(srid == 4326) {
            final double tileSizeDeg = 180d / (double)(1<<tileZoom);
            x = ((longitude - (-180d+(tileColumn*tileSizeDeg))) / tileSizeDeg) * 255d;
            y = ((90d-(tileRow*tileSizeDeg) - latitude) / tileSizeDeg) * 255d;
        } else if(srid == 3395) {
            x = WorldMercator.tilePixelXd(tileZoom, tileColumn, longitude);
            y = WorldMercator.tilePixelYd(tileZoom, tileRow, latitude);
        } else {
            x = -1d;
            y = -1d;
        }
        if (x < 0d || x > 255d || y < 0d || y > 255d)
            return Double.NaN;

        final double weight_l = 1d - (x - (int) x);
        final double weight_r = (x - (int) x);
        final double weight_t = 1d - (y - (int) y);
        final double weight_b = (y - (int) y);

        if (data == null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inBitmap = recyclePool.get();
            opts.inMutable = true;
            data = BitmapFactory.decodeByteArray(dataArray, dataArrayOffset, dataArrayLen, opts);
            if (data == null)
                return Double.NaN;
        }

        final int lx = MathUtils.clamp((int) x, 0, data.getWidth() - 1);
        final int uy = MathUtils.clamp((int) y, 0, data.getHeight() - 1);
        final int rx = MathUtils.clamp((int) Math.ceil(x), 0, data.getWidth() - 1);
        final int ly = MathUtils.clamp((int) Math.ceil(y), 0, data.getHeight() - 1);

        final double ul = decode(data, lx, uy);
        final double ur = decode(data, rx, uy);
        final double lr = decode(data, rx, ly);
        final double ll = decode(data, lx, ly);

        final double height = (ul * weight_l * weight_t) +
                (ur * weight_r * weight_t) +
                (lr * weight_r * weight_b) +
                (ll * weight_l * weight_b);
        return orthometric ? GeoCalculations.mslToHae(latitude, longitude, height) : height;
    }

    @Override
    public void dispose()
    {
        this.dataArray = null;
        if(this.data == null)
            return;
        if (this.recyclePool == null || !this.recyclePool.put(data))
            data.recycle();
        this.data = null;
    }

    static double decode(Bitmap bitmap, int x, int y)
    {
        final int v = bitmap.getPixel(x, y);
        if(Color.alpha(v) == 0)
            return Double.NaN;
        final int R = Color.red(v);
        final int G = Color.green(v);
        final int B = Color.blue(v);
        return -10000 + ((R * 256 * 256 + G * 256 + B) * 0.1);
    }
}
