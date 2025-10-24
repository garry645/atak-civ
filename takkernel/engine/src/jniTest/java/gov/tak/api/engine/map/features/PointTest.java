package gov.tak.api.engine.map.features;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class PointTest extends KernelJniTest {

    @Test
    public void test_round_trip_values() {
        Point p = new Point(1,2,3);
        Assert.assertEquals(p.getX(), 1, 0.0);
        Assert.assertEquals(p.getY(), 2, 0.0);
        Assert.assertEquals(p.getZ(), 3, 0.0);
    }

    @Test
    public void test_default_z() {
        Point p = new Point(1,2);
        Assert.assertEquals(p.getX(), 1, 0.0);
        Assert.assertEquals(p.getY(), 2, 0.0);
        Assert.assertEquals(p.getZ(), 0, 0.0);
    }

    @Test
    public void test_set3() {
        Point p = new Point(1,2, 3);
        p.set(4, 5, 6);
        Assert.assertEquals(p.getX(), 4, 0.0);
        Assert.assertEquals(p.getY(), 5, 0.0);
        Assert.assertEquals(p.getZ(), 6, 0.0);
    }

    @Test
    public void test_set2() {
        Point p = new Point(1,2, 3);
        p.set(4, 5);
        Assert.assertEquals(p.getX(), 4, 0.0);
        Assert.assertEquals(p.getY(), 5, 0.0);
        Assert.assertEquals(p.getZ(), 3, 0.0);
    }

    @Test
    public void test_equal_symmetry() {
        Point p = new Point(1, 2);
        com.atakmap.map.layer.feature.geometry.Point p2 = new com.atakmap.map.layer.feature.geometry.Point(1, 2);
        Assert.assertEquals(p, p2);
        Assert.assertEquals(p2, p);
    }
}
