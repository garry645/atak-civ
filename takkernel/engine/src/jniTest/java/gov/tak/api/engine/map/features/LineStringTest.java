package gov.tak.api.engine.map.features;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class LineStringTest extends KernelJniTest {

    @Test
    public void test_default_2D() {
        LineString ls = new LineString(2);
        Assert.assertEquals(ls.getDimension(), 2);
        // native implementation says empty LS is closed
        Assert.assertTrue(ls.isClosed());
        Point point = new Point(0,0,0);
        try {
            ls.get(point, 0);
        } catch(IndexOutOfBoundsException e) {
        } catch (Throwable t) {
            Assert.fail("wrong throw" + t);
        }
    }

    @Test
    public void test_default_3D() {
        LineString ls = new LineString(3);
        Assert.assertEquals(ls.getDimension(), 3);
        // native implementation says empty LS is closed
        Assert.assertTrue(ls.isClosed());
        Point point = new Point(0,0,0);
        try {
            ls.get(point, 0);
        } catch(IndexOutOfBoundsException e) {
        } catch (Throwable t) {
            Assert.fail("wrong throw" + t);
        }
    }

    @Test
    public void test_bad_dim() {
        try {
            LineString ls = new LineString(0);
        } catch(IllegalArgumentException e) {
        } catch (Throwable t) {
            Assert.fail("wrong throw" + t);
        }
    }

    @Test
    public void test_get_point() {
        LineString ls = new LineString(2);
        ls.addPoint(1, 2);
        ls.addPoint(3, 4);
        Point point = new Point(0,0);
        try {
            ls.get(point, 3);
        } catch(IndexOutOfBoundsException e) {
        } catch (Throwable t) {
            Assert.fail("wrong throw" + t);
        }
        ls.get(point, 0);
        Assert.assertEquals(point.getX(), 1, 0.0);
        Assert.assertEquals(point.getY(), 2, 0.0);
        Assert.assertEquals(point.getX(), ls.getX(0), 0.0);
        Assert.assertEquals(point.getY(), ls.getY(0), 0.0);
        ls.get(point,1);
        Assert.assertEquals(point.getX(), 3, 0.0);
        Assert.assertEquals(point.getY(), 4, 0.0);
        Assert.assertEquals(point.getX(), ls.getX(1), 0.0);
        Assert.assertEquals(point.getY(), ls.getY(1), 0.0);
    }

    @Test
    public void test_set_oob() {
        LineString ls = new LineString(2);
        try {
            ls.setX(0, 3);
        } catch(IndexOutOfBoundsException e) {
        } catch (Throwable t) {
            Assert.fail("wrong throw" + t);
        }
    }
}
