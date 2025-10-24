package com.atakmap.map.layer.raster;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import com.atakmap.math.PointD;

public class DefaultDatasetProjection2 implements DatasetProjection2
{

    private final static String TAG = "DefaultDatasetProjection";

    private final Projection mapProjection;
    private final Matrix img2proj;
    private final Matrix proj2img;
    private final ThreadLocal<PointD> projected = new ThreadLocal<PointD>() {
        @Override
        protected PointD initialValue() {
            return new PointD();
        }
    };

    private final double p2i_m00;
    private final double p2i_m01;
    private final double p2i_m03;
    private final double p2i_m10;
    private final double p2i_m11;
    private final double p2i_m13;
    private final double p2i_m30;
    private final double p2i_m31;
    private final double p2i_m33;
    private final boolean p2i_affine;

    private final double i2p_m00;
    private final double i2p_m01;
    private final double i2p_m03;
    private final double i2p_m10;
    private final double i2p_m11;
    private final double i2p_m13;
    private final double i2p_m30;
    private final double i2p_m31;
    private final double i2p_m33;
    private final boolean i2p_affine;
    
    public DefaultDatasetProjection2(ImageInfo info)
    {
        this(info.srid, info.width, info.height, info.upperLeft, info.upperRight, info.lowerRight, info.lowerLeft);
    }

    public DefaultDatasetProjection2(int srid, int width, int height, GeoPoint ul, GeoPoint ur, GeoPoint lr, GeoPoint ll)
    {
        this(srid, width & 0xFFFFFFFFL, height & 0xFFFFFFFFL, ul, ur, lr, ll);
    }

    public DefaultDatasetProjection2(int srid, long width, long height, GeoPoint ul, GeoPoint ur, GeoPoint lr, GeoPoint ll)
    {
        this.mapProjection = (srid != 4326) ? ProjectionFactory.getProjection(srid) : null;
        if (this.mapProjection == null && srid != 4326)
        {
            Log.w(TAG, "Failed to find EPSG:" + srid + ", defaulting to EPSG:4326; projection errors may result.");
        }

        PointD imgUL = new PointD(0, 0);
        PointD projUL = new PointD();
        lla2proj(ul, projUL);
        checkThrowUnusable(projUL);
        PointD imgUR = new PointD(width, 0);
        PointD projUR = new PointD();
        lla2proj(ur, projUR);
        checkThrowUnusable(projUR);
        PointD imgLR = new PointD(width, height);
        PointD projLR = new PointD();
        lla2proj(lr, projLR);
        checkThrowUnusable(projLR);
        PointD imgLL = new PointD(0, height);
        PointD projLL = new PointD();
        lla2proj(ll, projLL);
        checkThrowUnusable(projLL);

        this.img2proj = Matrix.mapQuads(imgUL, imgUR, imgLR, imgLL,
                projUL, projUR, projLR, projLL);

        Matrix p2i;
        try
        {
            p2i = this.img2proj.createInverse();
        } catch (NoninvertibleTransformException e)
        {
            Log.e(TAG, "Failed to invert img2proj, trying manual matrix construction");

            p2i = Matrix.mapQuads(projUL, projUR, projLR, projLL,
                    imgUL, imgUR, imgLR, imgLL);
        }
        this.proj2img = p2i;
        
        i2p_m00 = this.img2proj.get(0, 0);
        i2p_m01 = this.img2proj.get(0, 1);
        i2p_m03 = this.img2proj.get(0, 3);
        i2p_m10 = this.img2proj.get(1, 0);
        i2p_m11 = this.img2proj.get(1, 1);
        i2p_m13 = this.img2proj.get(1, 3);
        i2p_m30 = this.img2proj.get(3, 0);
        i2p_m31 = this.img2proj.get(3, 1);
        i2p_m33 = this.img2proj.get(3, 3);
        i2p_affine = (i2p_m30 == 0d && i2p_m31 == 0d && i2p_m33 == 1d);

        p2i_m00 = this.proj2img.get(0, 0);
        p2i_m01 = this.proj2img.get(0, 1);
        p2i_m03 = this.proj2img.get(0, 3);
        p2i_m10 = this.proj2img.get(1, 0);
        p2i_m11 = this.proj2img.get(1, 1);
        p2i_m13 = this.proj2img.get(1, 3);
        p2i_m30 = this.proj2img.get(3, 0);
        p2i_m31 = this.proj2img.get(3, 1);
        p2i_m33 = this.proj2img.get(3, 3);
        p2i_affine = (p2i_m30 == 0d && p2i_m31 == 0d && p2i_m33 == 1d);
    }

    private boolean proj2lla(PointD proj, GeoPoint lla) {
        if(this.mapProjection == null) {
            lla.set(proj.y, proj.x, proj.z);
            return true;
        } else {
            return this.mapProjection.inverse(proj, lla) != null;
        }
    }
    private boolean lla2proj(GeoPoint lla, PointD proj) {
        if(this.mapProjection == null) {
            proj.x = lla.getLongitude();
            proj.y = lla.getLatitude();
            proj.z = Double.isNaN(lla.getAltitude()) ? 0d : lla.getAltitude();
            return true;
        } else {
            return this.mapProjection.forward(lla, proj) != null;
        }
    }

    private void proj2img_transform(PointD proj, PointD img) {
        double img_x = proj.x*p2i_m00 + proj.y*p2i_m01 + p2i_m03;
        double img_y = proj.x*p2i_m10 + proj.y*p2i_m11 + p2i_m13;
        if(!p2i_affine) {
            final double m = proj.x * p2i_m30 + proj.y * p2i_m31 + p2i_m33;
            img_x /= m;
            img_y /= m;
        }
        img.x = img_x;
        img.y = img_y;
        img.z = 0d;
    }
    private void img2proj_transform(PointD img, PointD proj) {
        double proj_x = img.x*i2p_m00 + img.y*i2p_m01 + i2p_m03;
        double proj_y = img.x*i2p_m10 + img.y*i2p_m11 + i2p_m13;
        if(!i2p_affine) {
            final double m = img.x * i2p_m30 + img.y * i2p_m31 + i2p_m33;
            proj_x /= m;
            proj_y /= m;
        }
        proj.x = proj_x;
        proj.y = proj_y;
        proj.z = 0d;
    }
    
    @Override
    public boolean imageToGround(PointD image, GeoPoint ground)
    {
        final PointD p = this.projected.get();
        img2proj_transform(image, p);
        return proj2lla(p, ground);
    }

    @Override
    public boolean groundToImage(GeoPoint ground, PointD image)
    {
        if (!lla2proj(ground, image))
            return false;
        proj2img_transform(image, image);
        return true;
    }

    @Override
    public void release()
    {
    }

    static void checkThrowUnusable(PointD p)
    {
        checkThrowUnusable(p.x);
        checkThrowUnusable(p.y);
    }

    private static void checkThrowUnusable(double v)
    {
        if (Double.isInfinite(v) || Double.isNaN(v))
            throw new IllegalArgumentException();
    }
}
