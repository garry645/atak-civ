
package com.atakmap.map.layer.raster;

import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import gov.tak.test.KernelJniTest;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.math.PointD;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class DefaultDatasetProjection2Test extends KernelJniTest {
    @BeforeClass
    public static void initGdal() {
        GdalLibrary.init();
    }

    @Test
    public void test_web_mercator_bounds() {
        final int srid = 3857;
        final int width = -2147483648;
        final int height = -2147483648;
        final GeoPoint ul = new GeoPoint(85.05112877980659, -180.0);
        final GeoPoint ur = new GeoPoint(85.05112877980659, 180.0);
        final GeoPoint lr = new GeoPoint(-85.05112877980659, 180.0);
        final GeoPoint ll = new GeoPoint(-85.05112877980659, -180.0);

        DefaultDatasetProjection2 imprecise = new DefaultDatasetProjection2(
                srid, width, height, ul, ur, lr, ll);

        final long tileSrcHeight = 2147483648L;
        final long tileSrcWidth = 2147483648L;
        final long tileSrcX = 0L;
        final long tileSrcY = 0L;

        double minLat = 90;
        double maxLat = -90;
        double minLng = 180;
        double maxLng = -180;

        PointD scratchP = new PointD(0d, 0d, 0d);
        GeoPoint scratchG = GeoPoint.createMutable();

        scratchP.x = tileSrcX;
        scratchP.y = tileSrcY;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX + tileSrcWidth;
        scratchP.y = tileSrcY;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX + tileSrcWidth;
        scratchP.y = tileSrcY + tileSrcHeight;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX;
        scratchP.y = tileSrcY + tileSrcHeight;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        assertEquals(ul.getLatitude(), maxLat, 0.000001d);
        assertEquals(ul.getLongitude(), minLng, 0.000001d);
        assertEquals(lr.getLatitude(), minLat, 0.000001d);
        assertEquals(lr.getLongitude(), maxLng, 0.000001d);
    }

    // regression checks
    static boolean legacy_i2g(int srid, int width, int height, GeoPoint ul, GeoPoint ur, GeoPoint lr, GeoPoint ll, PointD image, GeoPoint ground) {
        Projection proj = ProjectionFactory.getProjection(srid);
        if (proj == null)
        {
            proj = EquirectangularMapProjection.INSTANCE;
        }
        final Projection mapProjection = proj;

        PointD imgUL = new PointD(0, 0);
        PointD projUL = mapProjection.forward(ul, null);
        DefaultDatasetProjection2.checkThrowUnusable(projUL);
        PointD imgUR = new PointD(width, 0);
        PointD projUR = mapProjection.forward(ur, null);
        DefaultDatasetProjection2.checkThrowUnusable(projUR);
        PointD imgLR = new PointD(width, height);
        PointD projLR = mapProjection.forward(lr, null);
        DefaultDatasetProjection2.checkThrowUnusable(projLR);
        PointD imgLL = new PointD(0, height);
        PointD projLL = mapProjection.forward(ll, null);
        DefaultDatasetProjection2.checkThrowUnusable(projLL);

        final Matrix img2proj = Matrix.mapQuads(imgUL, imgUR, imgLR, imgLL,
                projUL, projUR, projLR, projLL);

        PointD p = new PointD(0d, 0d);
        img2proj.transform(image, p);
        return (mapProjection.inverse(p, ground) != null);
    }

    @Test
    public void regression_check_utm() {
        //-108.0, 28.98, -102.0, 84.0
        DatasetProjection2 p = new DefaultDatasetProjection2(
                26913,
                10240, 10240,
                new GeoPoint(38.1, -106.1),
                new GeoPoint(37.9, -105.1),
                new GeoPoint(36.9, -104.9),
                new GeoPoint(37.1, -105.9));

        double[][] xy_pts = new double[][]{
            {4215.068323209087,4261.007080670661},
            {2977.9436349327807,9631.646484906667},
            {973.5311992188213,6204.328019663035},
            {3424.623329843339,5296.537866126017},
            {1361.7603194544313,10159.033871610183},
            {2820.3454011965346,4664.963026822332},
            {6034.354920328809,8369.53383147342},
            {3767.157247393775,10200.07551611176},
            {2257.4860266021074,1937.9099868025367},
            {9986.785503203022,6450.689650709245},
        };
        GeoPoint[] lla_expected = new GeoPoint[]
        {
            new GeoPoint(37.60603126236498,-105.60340420790826),
            new GeoPoint(37.102774971862196,-105.61954999471948),
            new GeoPoint(37.47900474676312,-105.88315255873539),
            new GeoPoint(37.52038804706909,-105.6605158198011),
            new GeoPoint(37.08180906407843,-105.76774400919342),
            new GeoPoint(37.593765793300136,-105.73203050462672),
            new GeoPoint(37.1678613398271,-105.34548257951853),
            new GeoPoint(37.03114220016424,-105.53117047974082),
            new GeoPoint(37.86954532960123,-105.84044466947908),
            new GeoPoint(37.27859178913892,-104.99862977502005),
        };

        for(int i = 0; i < xy_pts.length; i++) {
            double imgx = xy_pts[i][0];
            double imgy = xy_pts[i][1];

            GeoPoint lla = GeoPoint.createMutable();
            final boolean i2g = p.imageToGround(new PointD(imgx, imgy), lla);
            Assert.assertTrue("I2G failed {" + imgx + "," + imgy + "}", i2g);
            Assert.assertEquals("Regression check failed {" + imgx + "," + imgy + "} => {" + lla.getLatitude() + "," + lla.getLongitude() + "}", lla_expected[i].getLatitude(), lla.getLatitude(), 0.000001);
            Assert.assertEquals("Regression check failed {" + imgx + "," + imgy + "} => {" + lla.getLatitude() + "," + lla.getLongitude() + "}", lla_expected[i].getLongitude(), lla.getLongitude(), 0.000001);
        }

        for(int i = 0; i < lla_expected.length; i++)
            System.out.println("new GeoPoint(" + lla_expected[i].getLatitude() + "," + lla_expected[i].getLongitude() + "),");
    }

    // round trips
    @Test
    public void roundtrip_webmercator() {
        roundtrip_nw_hemi(3857);
        roundtrip_ne_hemi(3857);
        roundtrip_se_hemi(3857);
        roundtrip_sw_hemi(3857);
    }

    @Test
    public void roundtrip_equirectangular() {
        roundtrip_nw_hemi(4326);
        roundtrip_ne_hemi(4326);
        roundtrip_se_hemi(4326);
        roundtrip_sw_hemi(4326);
    }

    @Test
    public void roundtrip_utm() {
        //-108.0, 28.98, -102.0, 84.0
        DatasetProjection2 p = new DefaultDatasetProjection2(
                26913,
                10240, 10240,
                new GeoPoint(38.1, -106.1),
                new GeoPoint(37.9, -105.1),
                new GeoPoint(36.9, -104.9),
                new GeoPoint(37.1, -105.9));
        roundtrip_impl(p, 10240, 10240);
    }

    private void roundtrip_nw_hemi(int srid) {
        DatasetProjection2 p = new DefaultDatasetProjection2(
                srid,
                10240, 10240,
                new GeoPoint(38.1, -117.9),
                new GeoPoint(37.9, -116.9),
                new GeoPoint(36.9, -117.1),
                new GeoPoint(37.1, -118.1));
        roundtrip_impl(p, 10240, 10240);
    }

    private void roundtrip_ne_hemi(int srid) {
        DatasetProjection2 p = new DefaultDatasetProjection2(
                srid,
                10240, 10240,
                new GeoPoint(38.1, 117.1),
                new GeoPoint(37.9, 118.1),
                new GeoPoint(36.9, 117.9),
                new GeoPoint(37.1, 116.9));
        roundtrip_impl(p, 10240, 10240);
    }

    private void roundtrip_sw_hemi(int srid) {
        DatasetProjection2 p = new DefaultDatasetProjection2(
                srid,
                10240, 10240,
                new GeoPoint(-37.1, -117.9),
                new GeoPoint(-36.9, -116.9),
                new GeoPoint(-37.9, -117.1),
                new GeoPoint(-38.1, -118.1));
        roundtrip_impl(p, 10240, 10240);
    }

    private void roundtrip_se_hemi(int srid) {
        DatasetProjection2 p = new DefaultDatasetProjection2(
                srid,
                10240, 10240,
                new GeoPoint(-37.1, 117.1),
                new GeoPoint(-36.9, 118.1),
                new GeoPoint(-37.9, 117.9),
                new GeoPoint(-38.1, 116.9));
        roundtrip_impl(p, 10240, 10240);
    }

    private void roundtrip_impl(DatasetProjection2 impl, int width, int height) {
        Random r = new Random(0x12345678);
        for(int i = 0; i < 10000; i++) {
            double imgx = width*r.nextDouble();
            double imgy = height*r.nextDouble();

            GeoPoint lla = GeoPoint.createMutable();
            final boolean i2g = impl.imageToGround(new PointD(imgx, imgy), lla);
            Assert.assertTrue("I2G failed {" + imgx + "," + imgy + "}", i2g);
            PointD img = new PointD();
            final boolean g2i = impl.groundToImage(lla, img);
            Assert.assertTrue("G2I failed {" + lla.getLongitude() + "," + lla.getLatitude() + "}", g2i);
            Assert.assertEquals("Roundtrip failed {" + imgx + "," + imgy + "} => {" + img.x + "," + img.y + "}", imgx, img.x, 0.000001);
            Assert.assertEquals("Roundtrip failed {" + imgx + "," + imgy + "} => {" + img.x + "," + img.y + "}", imgy, img.y, 0.000001);
        }
    }
}
