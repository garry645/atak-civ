package com.atakmap.map.elevation;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HeightmapSampler extends ElevationChunk.Factory.Sampler {
    final static class Sample {
        int ndvCount = 0;
        int count;
        double avg = 0d;
        double retval = 0d;
        double ndvWeight = 0d;

        double get(boolean discardNdv) {
            if (ndvCount == this.count)
                return Double.NaN;

            // if there are any NDVs, fill the voids with the average
            if(ndvCount > 0)
                retval = discardNdv ?
                        Double.NaN :
                        retval + (avg/(this.count-ndvCount))*ndvWeight;
            return retval;
        }

        void reset(int count) {
            this.count = count;
            ndvCount = 0;
            avg = 0d;
            retval = 0d;
            ndvWeight = 0d;
        }
    }
    final static class SampleContext {
        final PointD img = new PointD(0d, 0d, 0d);
        final double[] weight_x = new double[2];
        final double[] weight_y = new double[2];
        DoubleBuffer ll = null;
        double[] ll_arr;
        FloatBuffer xy = null;
        float[] xy_arr;

        Sample sample = new Sample();
    }

    final static class SampleContextTL extends ThreadLocal<SampleContext> {
        protected SampleContext initialValue() {
            return new SampleContext();
        }
    }

    static Map<Integer, Long> nativeProjections = new ConcurrentHashMap<>();

    final int width;
    final int height;
    final boolean isMsl;

    ElementAccess arr;
    double noDataValue;
    SampleContextTL sampleContext = new SampleContextTL();
    OnDisposedListener listener;

    double[] m3x3;
    long nativeProjection;
    int srid;
    boolean discardNdv;


    /**
     *
     * @param srid          The SRID
     * @param ul            Coordinate mapping to sample at {@code 0,0}
     * @param ur            Coordinate mapping to sample at {@code width-1,0}
     * @param lr            Coordinate mapping to sample at {@code width-1,height-1}
     * @param ll            Coordinate mapping to sample at {@code 0,height-1}
     * @param width         Width of the heightmap
     * @param height        Height of the heightmap
     * @param arr           The heightmap data
     * @param noDataValue   The value representing _no-value_
     * @param isMsl         {@code true} if samples are MSL, {@code false} if HAE
     * @param discardNdv    if {@code true} samples will be discard if any of the interpolation
     *                      values are <I>no-data-value</I>s; if {@code false}
     *                      <I>no-data-value</I>s will be unweighted during interpolation
     * @param listener      Callback function when sampler is disposed
     */
    public HeightmapSampler(ElementAccess arr, int width, int height, double noDataValue, int srid, GeoPoint ul, GeoPoint ur, GeoPoint lr, GeoPoint ll, boolean isMsl, boolean discardNdv, OnDisposedListener listener) {
        this.width = width;
        this.height = height;
        this.arr = arr;
        this.noDataValue = noDataValue;
        this.isMsl = isMsl;
        this.discardNdv = discardNdv;
        this.listener = listener;
        this.srid = srid;

        Projection pp = ProjectionFactory.getProjection(srid);
        final PointD pul = pp.forward(ul, new PointD());
        final PointD pur = pp.forward(ur, new PointD());
        final PointD plr = pp.forward(lr, new PointD());
        final PointD pll = pp.forward(ll, new PointD());

        Matrix mm = Matrix.mapQuads(
                pul.x, pul.y, pur.x, pur.y, plr.x, plr.y, pll.x, pll.y,
                0, 0, width-1, 0, width-1, height-1, 0, height-1
        );

        m3x3 = new double[] {
                mm.get(0, 0), mm.get(0, 1), mm.get(0, 3),
                mm.get(1, 0), mm.get(1, 1), mm.get(1, 3),
                mm.get(3, 0), mm.get(3, 1), mm.get(3, 3),
        };
        nativeProjection = nativeProjections.computeIfAbsent(srid, i -> (srid == 4326) ? 0L : GLMapView.OsrUtils.createProjection(srid));
    }

    @Override
    public boolean sample(double[] lla, int off, int len)
    {
        final SampleContext ctx = sampleContext.get();
        final int limit = len*2;
        if(ctx.xy_arr == null || ctx.xy_arr.length != limit)
            ctx.xy_arr = new float[limit];
        float[] xy = ctx.xy_arr;
        if(nativeProjection != 0L) {
            if(ctx.ll_arr == null || ctx.ll_arr.length < limit)
                ctx.ll_arr = new double[limit];
            for (int i = 0; i < len; i++) {
                ctx.ll_arr[i * 2] = lla[(i + off) * 3];
                ctx.ll_arr[i * 2 + 1] = lla[(i + off) * 3 + 1];
            }
            if (ctx.ll == null || ctx.ll.capacity() < limit)
                ctx.ll = ByteBuffer.allocateDirect(limit * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
            ctx.ll.clear();
            ctx.ll.put(ctx.ll_arr, 0, limit);
            ctx.ll.flip();

            if (ctx.xy == null || ctx.xy.capacity() < limit)
                ctx.xy = ByteBuffer.allocateDirect(limit * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            ctx.xy.clear();
            ctx.xy.limit(limit);
            GLMapView.OsrUtils.forward(
                    nativeProjection,
                    m3x3,
                    ctx.ll,
                    ctx.xy);
            ctx.xy.get(xy);
        } else {
            for (int i = 0; i < len; i++) {
                final double x = lla[(i + off) * 3];
                final double y = lla[(i + off) * 3+1];
                ctx.img.x = x*m3x3[0] + y*m3x3[1] + m3x3[2];
                ctx.img.y = x*m3x3[3] + y*m3x3[4] + m3x3[5];
                final double m = x*m3x3[6] + y*m3x3[7] + m3x3[8];
                ctx.img.x /= m;
                ctx.img.y /= m;

                xy[i*2] = (float) ctx.img.x;
                xy[i*2+1] = (float) ctx.img.y;
            }
        }

        boolean retval = true;
        for (int i = 0; i < len; i++)
        {
            final int idx = (i + off) * 3;
            if (Double.isNaN(lla[idx + 2]))
            {
                final double el = sample(xy[i*2], xy[i*2+1], ctx);
                if (Double.isNaN(el)) {
                    retval = false;
                } else {
                    lla[idx + 2] = el;
                    if(isMsl)
                        lla[idx + 2] += ElevationManager.getGeoidHeight(lla[idx + 1], lla[idx]);
                }
            }
        }
        return retval;
    }

    @Override
    public double sample(double latitude, double longitude) {
        double[] lla = new double[] {longitude, latitude, Double.NaN};
        return sample(lla, 0, 1) ? lla[2] : Double.NaN;
    }

    private double sample(double imgx, double imgy, SampleContext ctx) {
        // sample elevation at pixel

        // soft clamp to image space
        if(imgx < 0 && imgx > -1e-6)
            imgx = 0d;
        else if(imgx > (width-1) && (imgx-width-1) < 1e-6)
            imgx = width-1;
        if(imgy < 0 && imgy > -1e-6)
            imgy = 0d;
        else if(imgy > (height-1) && (imgy-height-1) < 1e-6)
            imgy = height-1;

        // handle out of bounds
        if (imgx < 0 || imgx >= width)
            return Double.NaN;
        if (imgy < 0 || imgy >= height)
            return Double.NaN;

        final int ix = (int) imgx;
        final int iy = (int) imgy;
        final int w = Math.min(2, width - ix);
        final int h = Math.min(2, height - iy);

        return ((w*h) < 4) ?
                sampleEdge(imgx, imgy, ctx) :
                sampleInterior(imgx, imgy, ctx);
    }

    private void sample(int ix, int iy, double weight, Sample sample) {
        final double e = this.arr.get(ix, iy);
        final double v = (e == noDataValue) ? Double.NaN : e;
        if(Double.isNaN(v)) {
            sample.ndvCount++;
            sample.ndvWeight += weight;
        } else {
            sample.avg += v;
            sample.retval += v * weight;
        }
    }

    private double sampleInterior(double imgx, double imgy, SampleContext ctx) {
        final int ix = (int) imgx;
        final int iy = (int) imgy;

        // interpolate the samples
        ctx.weight_x[1] = (imgx - ix);
        ctx.weight_x[0] = 1d - ctx.weight_x[1];
        ctx.weight_y[1] = (imgy - iy);
        ctx.weight_y[0] = 1d - ctx.weight_y[1];

        // processing 4 samples
        ctx.sample.reset(4);
        sample(ix, iy, ctx.weight_x[0] * ctx.weight_y[0], ctx.sample);
        sample(ix+1, iy, ctx.weight_x[1] * ctx.weight_y[0], ctx.sample);
        sample(ix, iy+1, ctx.weight_x[0] * ctx.weight_y[1], ctx.sample);
        sample(ix+1, iy+1, ctx.weight_x[1] * ctx.weight_y[1], ctx.sample);

        return ctx.sample.get(discardNdv);
    }

    private double sampleEdge(double imgx, double imgy, SampleContext ctx) {
        final int ix = (int) imgx;
        final int iy = (int) imgy;
        final int w = Math.min(2, width - ix);
        final int h = Math.min(2, height - iy);
        final int slim = (w*h);

        // interpolate the samples
        ctx.weight_x[1] = (imgx - ix);
        ctx.weight_x[0] = 1d - ctx.weight_x[1];
        ctx.weight_y[1] = (imgy - iy);
        ctx.weight_y[0] = 1d - ctx.weight_y[1];

        ctx.sample.reset(slim);
        for(int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final double weight = ctx.weight_x[x] * ctx.weight_y[y];
                sample(ix+x, iy+y, weight, ctx.sample);
            }
        }

        return ctx.sample.get(discardNdv);
    }

    @Override
    public void dispose()
    {
        if(listener != null)
            listener.onDisposed(this);
    }

    public interface ElementAccess {
        double get(int x, int y);

        final class Array {
            public static ElementAccess from(byte[] arr, int stride, boolean signed) {
                return new ElementAccess() {
                    @Override
                    public double get(int x, int y) {
                        final byte e = arr[(y*stride)+x];
                        return signed ? e : (e&0xFF);
                    }
                };
            }
            public static ElementAccess from(short[] arr, int stride, boolean signed) {
                return new ElementAccess() {
                    @Override
                    public double get(int x, int y) {
                        final short e = arr[(y*stride)+x];
                        return signed ? e : (e&0xFFFF);
                    }
                };
            }
            public static ElementAccess from(int[] arr, int stride, boolean signed) {
                return new ElementAccess() {
                    @Override
                    public double get(int x, int y) {
                        final int e = arr[(y*stride)+x];
                        return signed ? e : ((long)e&0xFFFFFFFFL);
                    }
                };
            }
            public static ElementAccess from(float[] arr, int stride) {
                return new ElementAccess() {
                    @Override
                    public double get(int x, int y) {
                        return arr[(y*stride)+x];
                    }
                };
            }

            public static ElementAccess from(double[] arr, int stride) {
                return new ElementAccess() {
                    @Override
                    public double get(int x, int y) {
                        return arr[(y*stride)+x];
                    }
                };
            }
        }
    }

    public interface OnDisposedListener {
        void onDisposed(ElevationChunk.Factory.Sampler sampler);
    }
}
