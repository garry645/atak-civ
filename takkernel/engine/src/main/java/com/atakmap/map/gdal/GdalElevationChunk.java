package com.atakmap.map.gdal;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetProjection2;
import com.atakmap.map.layer.raster.gdal.GdalDatasetProjection2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;

import org.gdal.gdalconst.gdalconst;
import org.gdal.gdal.Dataset;
import org.gdal.osr.SpatialReference;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import gov.tak.api.annotation.DeprecatedApi;

public final class GdalElevationChunk extends ElevationChunk.Factory.Sampler
{
    private final static Set<Integer> MSL_SRIDS = new HashSet<>();
    static {
        // derived from EPSG dataset from epsg.org
        final int[] mslSrid = new int[]
        {
            3855, 3886, 3900, 3901, 3902, 3903, 4097, 4098, 4099, 4100, 4440, 4458, 5193, 5195, 5214,
            5237, 5317, 5318, 5498, 5499, 5500, 5554, 5555, 5556, 5597, 5598, 5600, 5601, 5602, 5603,
            5604, 5605, 5606, 5607, 5608, 5609, 5610, 5611, 5613, 5615, 5616, 5617, 5618, 5619, 5620,
            5621, 5628, 5698, 5699, 5701, 5702, 5703, 5705, 5707, 5708, 5709, 5710, 5711, 5712, 5713,
            5714, 5716, 5717, 5718, 5719, 5720, 5721, 5722, 5723, 5724, 5725, 5726, 5727, 5728, 5729,
            5730, 5731, 5732, 5733, 5735, 5736, 5737, 5738, 5740, 5741, 5742, 5743, 5744, 5745, 5746,
            5747, 5748, 5749, 5750, 5751, 5752, 5753, 5754, 5755, 5756, 5757, 5758, 5759, 5760, 5761,
            5762, 5763, 5764, 5765, 5766, 5767, 5768, 5769, 5770, 5771, 5772, 5773, 5774, 5775, 5776,
            5777, 5778, 5779, 5780, 5781, 5782, 5783, 5784, 5785, 5786, 5787, 5788, 5790, 5791, 5792,
            5793, 5794, 5795, 5796, 5797, 5798, 5799, 5829, 5843, 5845, 5846, 5847, 5848, 5849, 5850,
            5851, 5852, 5853, 5854, 5855, 5856, 5857, 5868, 5869, 5870, 5871, 5872, 5874, 5941, 5942,
            5945, 5946, 5947, 5948, 5949, 5950, 5951, 5952, 5953, 5954, 5955, 5956, 5957, 5958, 5959,
            5960, 5961, 5962, 5963, 5964, 5965, 5966, 5967, 5968, 5969, 5970, 5971, 5972, 5973, 5974,
            5975, 5976, 6130, 6131, 6132, 6144, 6145, 6146, 6147, 6148, 6149, 6150, 6151, 6152, 6153,
            6154, 6155, 6156, 6157, 6158, 6159, 6160, 6161, 6162, 6163, 6164, 6165, 6166, 6167, 6168,
            6169, 6170, 6171, 6172, 6173, 6174, 6175, 6176, 6178, 6179, 6180, 6181, 6182, 6183, 6184,
            6185, 6186, 6187, 6190, 6349, 6360, 6638, 6639, 6640, 6641, 6642, 6643, 6644, 6647, 6649,
            6650, 6651, 6652, 6653, 6654, 6655, 6656, 6657, 6658, 6659, 6660, 6661, 6662, 6663, 6664,
            6665, 6693, 6694, 6695, 6696, 6697, 6700, 6893, 6916, 6917, 6927, 7400, 7404, 7405, 7406,
            7407, 7409, 7410, 7411, 7414, 7415, 7416, 7417, 7418, 7419, 7420, 7421, 7422, 7423, 7446,
            7447, 7651, 7652, 7699, 7700, 7707, 7832, 7837, 7839, 7841, 7888, 7889, 7890, 7954, 7955,
            7956, 7962, 7968, 7979, 8050, 8052, 8089, 8228, 8266, 8267, 8349, 8350, 8357, 8360, 8370,
            8434, 8675, 8690, 8691, 8700, 8701, 8702, 8703, 8704, 8705, 8706, 8707, 8708, 8709, 8710,
            8711, 8712, 8713, 8714, 8715, 8716, 8717, 8718, 8719, 8720, 8721, 8722, 8723, 8724, 8725,
            8726, 8727, 8728, 8729, 8730, 8731, 8732, 8733, 8734, 8735, 8736, 8737, 8738, 8739, 8740,
            8741, 8742, 8743, 8744, 8745, 8746, 8747, 8748, 8749, 8750, 8751, 8752, 8753, 8754, 8755,
            8756, 8757, 8758, 8759, 8760, 8761, 8762, 8763, 8764, 8765, 8766, 8767, 8768, 8769, 8770,
            8771, 8772, 8773, 8774, 8775, 8776, 8777, 8778, 8779, 8780, 8781, 8782, 8783, 8784, 8785,
            8786, 8787, 8788, 8789, 8790, 8791, 8792, 8793, 8794, 8795, 8796, 8797, 8798, 8799, 8800,
            8801, 8802, 8803, 8804, 8805, 8806, 8807, 8808, 8809, 8810, 8811, 8812, 8813, 8814, 8815,
            8841, 8881, 8904, 8911, 8912, 9130, 9245, 9255, 9274, 9279, 9286, 9303, 9306, 9335, 9351,
            9368, 9374, 9388, 9389, 9390, 9392, 9393, 9394, 9395, 9396, 9397, 9398, 9399, 9400, 9401,
            9402, 9422, 9423, 9424, 9425, 9426, 9427, 9428, 9429, 9430, 9449, 9450, 9451, 9452, 9457,
            9458, 9462, 9463, 9464, 9471, 9500, 9501, 9502, 9503, 9504, 9505, 9506, 9507, 9508, 9509,
            9510, 9511, 9512, 9513, 9514, 9515, 9516, 9517, 9518, 9519, 9520, 9521, 9522, 9523, 9524,
            9525, 9526, 9527, 9528, 9529, 9530, 9531, 9532, 9533, 9534, 9535, 9536, 9537, 9538, 9539,
            9540, 9541, 9542, 9543, 9544, 9650, 9651, 9656, 9657, 9663, 9666, 9669, 9675, 9681, 9705,
            9707, 9711, 9714, 9715, 9721, 9722, 9723, 9724, 9725, 9742, 9762, 9767, 9785, 9870, 9881,
            9897, 9907, 9920, 9922, 9923, 9927, 9928, 9929, 9930, 9931, 9932, 9933, 9934, 9935, 9944,
            9948, 9949, 9950, 9951, 9952, 9953, 9968, 9973, 9978, 10162, 10163, 10164, 10165, 10166, 10167,
            10168, 10169, 10170, 10171, 10172, 10173, 10174, 10184, 10189, 10190, 10195, 10200, 10208, 10213, 10218,
            10223, 10228, 10236, 10241, 10245, 10246, 10276, 10281, 10293, 10318, 10352, 10353, 10354, 10355, 10356,
            10357, 10365, 10472, 20000, 20001, 20003, 20034, 20035, 20036, 20037, 20038, 20043,
        };
        for(int srid : mslSrid)
            MSL_SRIDS.add(srid);
    }

    private final static String[] NO_VERT_CS = new String[2];

    final static class SampleContext {
        final PointD img = new PointD(0d, 0d, 0d);
        final double[] weight_x = new double[2];
        final double[] weight_y = new double[2];
        final ByteBuffer arr = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());

        int ndvCount = 0;
        int count;
        double avg = 0d;
        double retval = 0d;
        double ndvWeight = 0d;

        double get() {
            if (ndvCount == this.count)
                return Double.NaN;

            // if there are any NDVs, fill the voids with the average
            if(ndvCount > 0)
                retval += (avg/(this.count-ndvCount))*ndvWeight;
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

    private Dataset dataset;
    private GdalDatasetProjection2 proj;
    private final int width;
    private final int height;
    private final double noDataValue;
    private final boolean isMsl;

    private final int srid;
    private final int dataType;
    private final double[] m3x3;

    private final ThreadLocal<SampleContext> sampleContext = new ThreadLocal<SampleContext>() {
        protected SampleContext initialValue() {
            return new SampleContext();
        }
    };

    private GdalElevationChunk(Dataset dataset, GdalDatasetProjection2 proj, boolean assumeHae)
    {
        this.dataset = dataset;
        this.proj = proj;
        this.width = dataset.GetRasterXSize();
        this.height = dataset.GetRasterYSize();

        // query the "No Data Value"
        Double[] ndv = new Double[1];
        dataset.GetRasterBand(1).GetNoDataValue(ndv);
        noDataValue = (ndv[0] != null) ? ndv[0] : Double.NaN;
        isMsl = !assumeHae && isMsl(dataset);
        dataType = dataset.GetRasterBand(1).getDataType();

        GeoPoint ul = GeoPoint.createMutable();
        proj.imageToGround(new PointD(0, 0), ul);
        GeoPoint ur = GeoPoint.createMutable();
        proj.imageToGround(new PointD(width-1, 0), ur);
        GeoPoint lr = GeoPoint.createMutable();
        proj.imageToGround(new PointD(width-1, height-1), lr);
        GeoPoint ll = GeoPoint.createMutable();
        proj.imageToGround(new PointD(0, height-1), ll);

        srid = proj.getNativeSpatialReferenceID();
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
    }

    @Override
    public boolean sample(double[] lla, int off, int len)
    {
        final SampleContext ctx = sampleContext.get();
        final int limit = len*2;
        float[] xy = new float[limit];
        if (srid != 4326) {
            double[] ll_arr = new double[limit];
            for (int i = 0; i < len; i++) {
                ll_arr[i * 2] = lla[(i + off) * 3];
                ll_arr[i * 2 + 1] = lla[(i + off) * 3 + 1];
            }
            DoubleBuffer ll_buf = null;
            FloatBuffer xy_buf = null;
            long nativeProjection = 0L;
            try {
                ll_buf = Unsafe.allocateDirect(limit, DoubleBuffer.class);
                ll_buf.put(ll_arr);
                ll_buf.flip();

                nativeProjection = GLMapView.OsrUtils.createProjection(srid);
                xy_buf = Unsafe.allocateDirect(limit, FloatBuffer.class);
                GLMapView.OsrUtils.forward(
                        nativeProjection,
                        m3x3,
                        ll_buf,
                        xy_buf);
                xy_buf.get(xy);
            } finally {
                if(ll_buf != null)
                    Unsafe.free(ll_buf);
                if(xy_buf != null)
                    Unsafe.free(xy_buf);
                if(nativeProjection != 0L)
                    GLMapView.OsrUtils.destroyProjection(nativeProjection);
            }
        } else {
            for (int i = 0; i < len; i++) {
                final double x = lla[(i + off) * 3];
                final double y = lla[(i + off) * 3 + 1];
                ctx.img.x = x * m3x3[0] + y * m3x3[1] + m3x3[2];
                ctx.img.y = x * m3x3[3] + y * m3x3[4] + m3x3[5];
                final double m = x * m3x3[6] + y * m3x3[7] + m3x3[8];
                ctx.img.x /= m;
                ctx.img.y /= m;

                xy[i * 2] = (float) ctx.img.x;
                xy[i * 2 + 1] = (float) ctx.img.y;
            }
        }

        boolean retval = true;
        for (int i = 0; i < len; i++) {
            final int idx = (i + off) * 3;
            if (Double.isNaN(lla[idx + 2])) {
                final double el = sample(xy[i*2], xy[i*2+1], ctx);
                if (Double.isNaN(el)) {
                    retval = false;
                } else {
                    lla[idx + 2] = el;
                    if (isMsl)
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

        ctx.arr.clear();

        // read the pixel
        final int success = dataset.ReadRaster_Direct(
                ix, iy, w, h, // src x,y,w,h
                w, h, // dst w,h
                dataType,
                ctx.arr,
                new int[] {
                        1
                } // bands
        );
        if (success != gdalconst.CE_None)
            return Double.NaN;

        return ((w*h) < 4) ?
                sampleEdge(ctx.arr, imgx-ix, imgy-iy, w, h, ctx) :
                sampleInterior(ctx.arr, imgx-ix, imgy-iy, ctx);
    }

    private void sample(ByteBuffer arr, int idx, double weight, SampleContext sample) {
        final double e = getElement(arr, idx);
        final double v = (e == noDataValue) ? Double.NaN : e;
        if(Double.isNaN(v)) {
            sample.ndvCount++;
            sample.ndvWeight += weight;
        } else {
            sample.avg += v;
            sample.retval += v * weight;
        }
    }

    private double sampleInterior(ByteBuffer arr, double imgx, double imgy, SampleContext ctx) {
        final int ix = (int) imgx;
        final int iy = (int) imgy;

        // interpolate the samples
        ctx.weight_x[1] = (imgx - ix);
        ctx.weight_x[0] = 1d - ctx.weight_x[1];
        ctx.weight_y[1] = (imgy - iy);
        ctx.weight_y[0] = 1d - ctx.weight_y[1];

        // processing 4 samples
        ctx.reset(4);
        sample(arr, 0, ctx.weight_x[0] * ctx.weight_y[0], ctx);
        sample(arr, 1, ctx.weight_x[1] * ctx.weight_y[0], ctx);
        sample(arr, 2, ctx.weight_x[0] * ctx.weight_y[1], ctx);
        sample(arr, 3, ctx.weight_x[1] * ctx.weight_y[1], ctx);

        return ctx.get();
    }

    /**
     *
     * @param arr
     * @param arrx  column in the _sample array_, {@code [0, 1]}
     * @param arry  row in the _sample array_, {@code [0, 1]}
     * @param w     number of columns in the sample array
     * @param h     number of rows in the sample array
     * @param ctx
     * @return
     */
    private double sampleEdge(ByteBuffer arr, double arrx, double arry, int w, int h, SampleContext ctx) {
        final int ix = (int) arrx;
        final int iy = (int) arry;
        final int slim = (w*h);

        // interpolate the samples
        ctx.weight_x[1] = (arrx - ix);
        ctx.weight_x[0] = 1d - ctx.weight_x[1];
        ctx.weight_y[1] = (arry - iy);
        ctx.weight_y[0] = 1d - ctx.weight_y[1];

        ctx.reset(slim);
        for(int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final double weight = ctx.weight_x[x] * ctx.weight_y[y];
                sample(arr, (y*w)+x, weight, ctx);
            }
        }

        return ctx.get();
    }

    private double getElement(ByteBuffer arr, int idx) {
        if (dataType == gdalconst.GDT_Byte)
            return arr.get(idx) & 0xFF;
        else if (dataType == gdalconst.GDT_UInt16)
            return arr.getShort(idx*2) & 0xFFFF;
        else if (dataType == gdalconst.GDT_Int16)
            return arr.getShort(idx*2);
        else if (dataType == gdalconst.GDT_UInt32)
            return ((long) arr.getInt(idx*4) & 0xFFFFFFFFL);
        else if (dataType == gdalconst.GDT_Int32)
            return arr.getInt(idx*4);
        else if (dataType == gdalconst.GDT_Float32)
            return arr.getFloat(idx*4);
        else if (dataType == gdalconst.GDT_Float64)
            return arr.getDouble(idx*8);
        else
            return Double.NaN;
    }

    @Override
    public void dispose()
    {
        if (this.proj != null)
        {
            this.proj.release();
            this.proj = null;
        }
        if (this.dataset != null)
        {
            this.dataset.delete();
            this.dataset = null;
        }
    }

    /** @deprecated use {@link #create(String, String, double, int, boolean)} */
    @Deprecated
    @DeprecatedApi(since = "5.2", removeAt = "5.5", forRemoval = true)
    public static ElevationChunk create(String path, String type, double resolution, int hints)
    {
        return create(path, type, resolution, hints, false);
    }

    public static ElevationChunk create(String path, String type, double resolution, int hints, boolean autoDetectMsl)
    {
        File f = new File(path);
        if (path.startsWith("/vsimem/")) {
            return create(GdalLibrary.openDatasetFromPath(path), true, type, resolution, hints, autoDetectMsl);
        } else if (IOProviderFactory.exists(f)) {
            if(path.toLowerCase(LocaleUtil.getCurrent()).endsWith(".zip")) {
                f = new ZipVirtualFile(path);
                if(f.isDirectory()) {
                    final File[] children = f.listFiles();
                    if(children.length == 1) {
                        f = children[0];
                    }
                }
            }
            return create(GdalLibrary.openDatasetFromFile(f), true, type, resolution, hints, autoDetectMsl);
        } else {
            return null;
        }
    }

    /** @deprecated use {@link #create(Dataset, boolean, String, double, int, boolean)} */
    @Deprecated
    @DeprecatedApi(since = "5.2", removeAt = "5.5", forRemoval = true)
    public static ElevationChunk create(Dataset dataset, boolean deleteOnFail, String type, double resolution, int hints)
    {
        return create(dataset, deleteOnFail, type, resolution, hints, false);
    }

    public static ElevationChunk create(Dataset dataset, boolean deleteOnFail, String type, double resolution, int hints, boolean autoDetectMsl)
    {
        if (dataset == null)
            return null;

        final GdalDatasetProjection2 proj = GdalDatasetProjection2.getInstance(dataset);
        if (proj == null)
        {
            if (deleteOnFail)
                dataset.delete();
            return null;
        }

        GeoPoint ul = GeoPoint.createMutable();
        proj.imageToGround(new PointD(0.5, 0.5), ul);
        GeoPoint ur = GeoPoint.createMutable();
        proj.imageToGround(new PointD(dataset.GetRasterXSize()-0.5, 0.5), ur);
        GeoPoint lr = GeoPoint.createMutable();
        proj.imageToGround(new PointD(dataset.GetRasterXSize()-0.5, dataset.GetRasterYSize()-0.5), lr);
        GeoPoint ll = GeoPoint.createMutable();
        proj.imageToGround(new PointD(0.5, dataset.GetRasterYSize()-0.5), ll);

        final GdalElevationChunk retval = new GdalElevationChunk(dataset, proj, !autoDetectMsl);
        try
        {
            if (type == null)
                type = retval.dataset.GetDriver().GetDescription();
        } catch (NullPointerException ignored)
        {
            // on the insiginifcant chance that GetDriver() returns null, just continue without
            // the type set.
        }
        if (Double.isNaN(resolution))
            resolution = DatasetDescriptor.computeGSD(retval.width, retval.height, ul, ur, lr, ll);
        return ElevationChunk.Factory.create(type, retval.dataset.GetDescription(), hints, resolution, (Polygon) DatasetDescriptor.createSimpleCoverage(ul, ur, lr, ll), Double.NaN, Double.NaN, true, retval);
    }

    private static String extractWkt(String s) {
        int in = 0;
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '[') {
                in++;
            } else if(s.charAt(i) == ']') {
                in--;
                if(in == 0)
                    return s.substring(0, i+1);
            }
        }
        return null;
    }

    /**
     *
     * @param dataset
     * @return  {@code retval[0]} = VERT_CS, {@code retval[1]} = VERTCRS
     */
    private static String[] getVertCS(Dataset dataset) {
        final String driver = dataset.GetDriver().GetDescription();
        String wkt = dataset.GetProjectionRef();

        if(wkt != null && driver.equals("GTiff")) {
            Dataset geotiff = null;
            try {
                String desc = dataset.GetDescription();
                geotiff = GdalLibrary.openImpl(
                        desc,
                        gdalconst.GA_ReadOnly,
                        new Vector(Collections.singleton(driver)),
                        null);
                if(geotiff != null) {
                    GdalLibrary.setThreadLocalConfigOption("GTIFF_REPORT_COMPD_CS", "YES");
                    wkt = geotiff.GetProjectionRef();
                    GdalLibrary.setThreadLocalConfigOption("GTIFF_REPORT_COMPD_CS", null);
                }
            } finally {
                if(geotiff != null)
                    geotiff.delete();
            }
        }

        if(wkt == null)
            return NO_VERT_CS;

        final String[] retval = new String[2];
        final int vert_csStart = wkt.indexOf("VERT_CS");
        if(vert_csStart == 0)
            retval[0] = wkt;
        else if(vert_csStart > 0)
            retval[0] = extractWkt(wkt.substring(vert_csStart));
        if(retval[0] == null) {
            final String verticalSrs = dataset.GetMetadataItem("VERTICAL_SRS");
            if(verticalSrs != null && verticalSrs.matches("EPSG:\\d+")) {
                SpatialReference srs = new SpatialReference();
                srs.ImportFromEPSG(Integer.parseInt(verticalSrs.substring(5)));
                retval[0] = srs.ExportToWkt();
            }
        }
        final int vertcrsStart = wkt.indexOf("VERTCRS");
        if(vertcrsStart == 0)
            retval[1] = wkt;
        else if(vertcrsStart > 0)
            retval[1] = extractWkt(wkt.substring(vertcrsStart));
        return retval;
    }

    /**
     * Returns {@code true} if the specified dataset has an associated Vertical Coordinate System,
     * indicating elevation data, {@code false} otherwise.
     *
     * @param dataset   A dataset
     *
     * @return  {@code true} if the dataset has a vertical coordinate system, {@code false}
     *          otherwise
     */
    public static boolean hasVertCS(Dataset dataset) {
        // check driver for dataset types known to be elevation data
        switch(dataset.GetDriver().GetDescription()) {
            case "SRTMHGT" :
            case "DTED" :
                return true;
            default :
                break;
        }

        // check for AW3D -- AW2D30 Product Description 2.2
        final File file = new File(dataset.GetDescription());
        if(file.getName().matches("ALPSMLC\\d{2}_[NS]\\d{3}[EW]\\d{3}_DSM\\.tif"))
            return true;

        final String[] vertcs = getVertCS(dataset);
        return (vertcs[0] != null || vertcs[1] != null);
    }

    public static boolean isMsl(Dataset dataset) {
        final String driver = dataset.GetDriver().GetDescription();
        // check driver for dataset types known to be MSL
        switch(driver) {
            case "SRTMHGT" :
            case "DTED" :
                return true;
            default :
                break;
        }

        // check for AW3D -- AW2D30 Product Description 2.1, 2.2
        final File file = new File(dataset.GetDescription());
        if(file.getName().matches("ALPSMLC\\d{2}_[NS]\\d{3}[EW]\\d{3}_DSM\\.tif"))
            return true;

        final String[] vertcs = getVertCS(dataset);
        final String vertcrs = vertcs[1];
        final String  vert_cs = vertcs[0];
        if(vertcrs != null && vertcrs.toLowerCase(LocaleUtil.getCurrent()).contains("gravity-related height"))
            return true;
        if(vert_cs != null && MSL_SRIDS.contains(GdalLibrary.getSpatialReferenceID(new SpatialReference(vert_cs))))
            return true;
        return false;

    }
}
