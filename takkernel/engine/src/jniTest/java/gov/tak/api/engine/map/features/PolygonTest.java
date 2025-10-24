package gov.tak.api.engine.map.features;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class PolygonTest extends KernelJniTest {

    @Test
    public void test_default() {
        Polygon p = new Polygon(2);
        Assert.assertEquals(p.getDimension(), 2);
        Assert.assertNull(p.getExteriorRing());
        Assert.assertEquals(p.getInteriorRings().size(), 0);
        Assert.assertTrue(p.getInteriorRings().isEmpty());
    }

    @Test
    public void test_set_exterior() {
        Polygon p = new Polygon(2);
        p.setExteriorRing(new LineString(2));
        Assert.assertNotNull(p.getExteriorRing());
        Assert.assertEquals(p.getInteriorRings().size(), 0);
        Assert.assertTrue(p.getInteriorRings().isEmpty());
    }

    @Test
    public void test_add_remove_ir() {
        Polygon p = new Polygon(2);
        LineString ir = new LineString(2);
        p.addInteriorRing(ir);
        Assert.assertTrue(p.getInteriorRings().contains(ir));
        Assert.assertTrue(p.getInteriorRings().remove(ir));
        Assert.assertFalse(p.getInteriorRings().contains(ir));
    }
}
