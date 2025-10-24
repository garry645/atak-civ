package gov.tak.api.engine.map.coords;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.util.ConfigOptions;
import com.atakmap.util.zip.IoUtils;

import gov.tak.test.KernelJniTest;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

public class GeoCalculationsTest extends KernelJniTest
{
    public GeoCalculationsTest()
    {
        new gov.tak.api.engine.map.coords.GeoPoint(0, 0);
    }

    @Test
    public void pointAtDistanceTest()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 100);
        IGeoPoint point = gov.tak.api.engine.map.coords.GeoCalculations.pointAtDistance(start, 45., 10000.);
        Assert.assertEquals(35.0637, point.getLatitude(), .001);
        Assert.assertEquals(42.077, point.getLongitude(), .001);
        Assert.assertEquals(GeoPoint.UNKNOWN, point.getAltitude(), .001);
    }

    @Test
    public void lineOfBearingIntersectTest()
    {
        IGeoPoint start1 = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 100);
        IGeoPoint start2 = new gov.tak.api.engine.map.coords.GeoPoint(40, 35, 100);
        IGeoPoint point = gov.tak.api.engine.map.coords.GeoCalculations.lineOfBearingIntersect(start1, 5., start2, 91.);

        Assert.assertEquals(39.654, point.getLatitude(), .001);
        Assert.assertEquals(42.528, point.getLongitude(), .001);
        Assert.assertEquals(GeoPoint.UNKNOWN, point.getAltitude(), .001);
    }

    @Test
    public void distanceTest()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(35.06371235, 42.0775188);
        double distance = gov.tak.api.engine.map.coords.GeoCalculations.distance(start, end);
        Assert.assertEquals(10000, distance, .01);
    }

    @Test
    public void slantDistanceTest()
    {

        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 0.0);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(35.06371235, 42.0775188, 1000.0);
        double distance = gov.tak.api.engine.map.coords.GeoCalculations.slantDistance(start, end);
        Assert.assertEquals(10050.6498, distance, .001);
    }

    @Test
    public void bearingTest()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 30.0);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(35.0637, 42.077, 100.);
        double bearing = gov.tak.api.engine.map.coords.GeoCalculations.bearing(start, end);
        Assert.assertEquals(44.813, bearing, .001);
    }

    @Test
    public void midPointWGS84Test()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 100);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(45, 43, 100);
        IGeoPoint point = gov.tak.api.engine.map.coords.GeoCalculations.midpoint(start, end);

        Assert.assertEquals(40.003, point.getLatitude(), .001);
        Assert.assertEquals(42.463, point.getLongitude(), .001);

    }

    @Test
    public void haversineTest()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 100);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(35.06371235, 42.0775188, 100);

        double distance = gov.tak.api.engine.map.coords.GeoCalculations.haversine(start, end);
        Assert.assertEquals(10011.5355, distance, .001);
    }

    @Test
    public void slantAngleTest()
    {
        IGeoPoint start = new gov.tak.api.engine.map.coords.GeoPoint(35, 42, 100);
        IGeoPoint end = new gov.tak.api.engine.map.coords.GeoPoint(45, 43, 100);

        double slant = gov.tak.api.engine.map.coords.GeoCalculations.slantAngle(start, end);
        Assert.assertEquals(-24.445, slant, .001);
    }

    @Test
    public void declinationTest() throws Throwable
    {
        Context context = getTestContext();
        extractPrivateResource(context, "wmm_cof", "world-magnetic-model-file");
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double declination = GeoCalculations.magneticDeclination(new GeoPoint(48.80299, 36.87427), d.getTime());
        Assert.assertEquals(8.79306697845459d, declination, 0.001d);
    }

    @Test
    public void convertMagneticToTrueTest() throws Throwable
    {
        Context context = getTestContext();
        extractPrivateResource(context, "wmm_cof", "world-magnetic-model-file");
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double angle = GeoCalculations.convertFromMagneticToTrue(new GeoPoint(48.80299, 36.87427), 27d, d.getTime());
        Assert.assertEquals(35.79306697845459d, angle, 0.001d);
    }

    @Test
    public void convertTrueToMagneticTest() throws Throwable
    {
        Context context = getTestContext();
        extractPrivateResource(context, "wmm_cof", "world-magnetic-model-file");
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double angle = GeoCalculations.convertFromTrueToMagnetic(new GeoPoint(48.80299, 36.87427), 36, d.getTime());
        Assert.assertEquals(27.20693302154541d, angle, 0.001d);
    }


    private static void extractPrivateResource(Context context, String resourceName, String option) throws Throwable
    {
        InputStream stream = null;

        try
        {
            long s = SystemClock.uptimeMillis();
            // load from assets
            Resources r = context.getResources();
            final int id = r.getIdentifier(resourceName, "raw",
                    context.getPackageName());
            if (id != 0)
            {
                stream = r.openRawResource(id);
            }

            File cofFile = new File(context.getFilesDir(), resourceName);

            FileSystemUtils.copy(stream,
                    new FileOutputStream(cofFile));

            if (option != null)
                ConfigOptions.setOption(option, cofFile.getAbsolutePath());
        } catch (Throwable t)
        {
            throw new ExceptionInInitializerError(t);
        } finally
        {
            IoUtils.close(stream);
        }
    }
}
