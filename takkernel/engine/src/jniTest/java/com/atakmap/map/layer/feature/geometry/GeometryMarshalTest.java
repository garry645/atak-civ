package com.atakmap.map.layer.feature.geometry;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.platform.marshal.MarshalManager;
import gov.tak.test.KernelJniTest;

public class GeometryMarshalTest extends KernelJniTest {
    @Test
    public void test_geometry_marshal_to_and_from_api() {
        LineString ls = new LineString(2);
        gov.tak.api.engine.map.features.Geometry apiGeom = MarshalManager.marshal(ls, Geometry.class, gov.tak.api.engine.map.features.Geometry.class);
        Assert.assertNotNull(apiGeom);
        Geometry toGeom = MarshalManager.marshal(apiGeom, gov.tak.api.engine.map.features.Geometry.class, Geometry.class);
        Assert.assertNotNull(toGeom);
        Assert.assertEquals(ls, toGeom);
    }

    @Test
    public void test_geometry_marshal_null() {
        Geometry nullGeom = null;
        gov.tak.api.engine.map.features.Geometry apiGeom = MarshalManager.marshal(nullGeom, Geometry.class, gov.tak.api.engine.map.features.Geometry.class);
        Assert.assertNull(apiGeom);
    }

    @Test
    public void test_geometry_api_marshal_null() {
        gov.tak.api.engine.map.features.Geometry nullApiGeom = null;
        Geometry geom = MarshalManager.marshal(nullApiGeom, gov.tak.api.engine.map.features.Geometry.class, Geometry.class);
        Assert.assertNull(geom);
    }
}
