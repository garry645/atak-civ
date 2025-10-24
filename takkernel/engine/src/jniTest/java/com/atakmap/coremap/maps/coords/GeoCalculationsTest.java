package com.atakmap.coremap.maps.coords;

import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

public class GeoCalculationsTest extends KernelJniTest
{

    @Test
    public void pointAtDistanceTest()
    {
        GeoPoint point = GeoCalculations.pointAtDistance(new GeoPoint(35, 42, 100), 45., 10000.);
        Assert.assertEquals(35.0637, point.getLatitude(), .001);
        Assert.assertEquals(42.077, point.getLongitude(), .001);
        Assert.assertEquals(GeoPoint.UNKNOWN, point.getAltitude(), .001);
    }

    @Test
    public void lineOfBearingIntersectTest()
    {
        GeoPoint start1 = new GeoPoint(35, 42, 100);
        GeoPoint start2 = new GeoPoint(40, 35, 100);
        GeoPoint point = GeoCalculations.lineOfBearingIntersect(start1, 5., start2, 91.);

        Assert.assertEquals(39.654, point.getLatitude(), .001);
        Assert.assertEquals(42.528, point.getLongitude(), .001);
        Assert.assertEquals(GeoPoint.UNKNOWN, point.getAltitude(), .001);
    }

    @Test
    public void distanceTest()
    {
        double distance = GeoCalculations.distanceTo(new GeoPoint(35, 42), new GeoPoint(35.06371235, 42.0775188));
        Assert.assertEquals(10000, distance, .01);
    }

    @Test
    public void slantDistanceTest()
    {

        double distance = GeoCalculations.slantDistanceTo(new GeoPoint(35, 42, 0.0), new GeoPoint(35.06371235, 42.0775188, 1000.0));
        Assert.assertEquals(10050.6498, distance, .001);
    }

    @Test
    public void bearingTest()
    {
        double bearing = GeoCalculations.bearingTo(new GeoPoint(35, 42, 30.0), new GeoPoint(35.0637, 42.077, 100.0));
        Assert.assertEquals(44.813, bearing, .001);
    }

    @Test
    public void midPointWGS84Test()
    {
        GeoPoint start = new GeoPoint(35, 42, 100);
        GeoPoint end = new GeoPoint(45, 43, 100);
        GeoPoint point = GeoCalculations.midPointWGS84(start, end);

        Assert.assertEquals(40.003, point.getLatitude(), .001);
        Assert.assertEquals(42.463, point.getLongitude(), .001);

    }

    @Test
    public void slantAngleTest()
    {
        GeoPoint start = new GeoPoint(35, 42, 100);
        GeoPoint end = new GeoPoint(45, 43, 100);

        double slant = GeoCalculations.inclinationTo(start, end);
        Assert.assertEquals(-24.445, slant, .001);
    }
}