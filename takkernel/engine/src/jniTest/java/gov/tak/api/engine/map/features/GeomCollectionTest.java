package gov.tak.api.engine.map.features;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class GeomCollectionTest extends KernelJniTest {
    @Test
    public void test_default() {
        GeometryCollection g = new GeometryCollection(2);
        Assert.assertEquals(g.getDimension(), 2);
        Assert.assertEquals(g.getGeometries().size(), 0);
        Assert.assertTrue(g.getGeometries().isEmpty());
    }

    @Test
    public void test_set_exterior() {
        GeometryCollection g = new GeometryCollection(2);
        g.addGeometry(new LineString(2));
        Assert.assertEquals(g.getGeometries().size(), 1);
        Assert.assertFalse(g.getGeometries().isEmpty());
    }

    @Test
    public void test_add_remove_ir() {
        GeometryCollection g = new GeometryCollection(2);
        LineString ir = new LineString(2);
        Geometry added = g.addGeometry(ir);
        Assert.assertTrue(g.getGeometries().contains(added));
        Assert.assertTrue(g.getGeometries().remove(added));
        Assert.assertFalse(g.getGeometries().contains(added));
    }
}
