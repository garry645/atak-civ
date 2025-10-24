
package com.atakmap.spatial;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;

import com.atakmap.map.layer.feature.geometry.Polygon;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * In theory, this shouldn't have to be an instrumented test.
 * However, the calculator is tied to the Android system through
 * the use of the `Environment` class for setting the default
 * temporary directory.
 */
public class SpatialCalculatorTest extends KernelJniTest
{
    @Test
    public void testIsDisposed() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();
        Assert.assertFalse(local.isDisposed());
        local.dispose();
        Assert.assertTrue(local.isDisposed());
    }

    /**
     * Test the point Z dimension passes through the calculator
     */
    @Test
    public void testPointZDimension() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .includePointZDimension()
                .inMemory().build();
        Point expected = new Point(0.0, 0.0, 100.0);
        long handle = local.createPoint(expected);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getZ(), actual.getZ(), 0.0);
        local.dispose();
    }

    @Test
    public void testGeoPointZDimension() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .includePointZDimension()
                .inMemory().build();
        GeoPoint expected = new GeoPoint(0.0, 0.0, 100.0);
        long handle = local.createPoint(expected);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getAltitude(), actual.getZ(), 0.01);
        local.dispose();
    }

    @Test
    public void testGeoPointWithInvalidAltitudeGetsClamped() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .includePointZDimension()
                .inMemory().build();
        GeoPoint geoPoint = new GeoPoint(0.0, 0.0, Double.NaN);
        long handle = local.createPoint(geoPoint);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        Assert.assertEquals(0.0d, actual.getZ(), 0.0);
        local.dispose();
    }

    @Test
    public void testPointZDimensionWith3DDisabled() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();
        Point pt = new Point(0.0, 0.0, 100.0);
        long handle = local.createPoint(pt);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        Assert.assertEquals(0.0, actual.getZ(), 0.0);
        local.dispose();

    }

    @Test
    public void clear_removes_geometry() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();
        Point pt = new Point(0.0, 0.0, 100.0);
        long handle = local.createPoint(pt);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        local.clear();
        Point cleared = (Point) local.getGeometry(handle);
        Assert.assertNull(cleared);
        local.dispose();
    }

    @Test
    public void endBatch_removes_geometry() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();
        local.beginBatch();
        Point pt = new Point(0.0, 0.0, 100.0);
        long handle = local.createPoint(pt);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        local.endBatch(false);
        Point cleared = (Point) local.getGeometry(handle);
        Assert.assertNull(cleared);
        local.dispose();
    }

    @Test
    public void delete_removes_geometry() {
        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();
        Point pt = new Point(0.0, 0.0, 100.0);
        long handle = local.createPoint(pt);
        Point actual = (Point) local.getGeometry(handle);
        Assert.assertNotNull(actual);
        local.deleteGeometry(handle);
        Point cleared = (Point) local.getGeometry(handle);
        Assert.assertNull(cleared);
        local.dispose();
    }

    @Test
    public void linestring_roundtrip() {
        double[] pts = new double[]
        {
            1, 2,
            3, 4,
            5, 6,
            -7, 8,
            9, -10,
            -11, -12,
        };

        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();

        LineString linestring = new LineString(2);
        for(int i = 0; i < pts.length / 2; i++)
            linestring.addPoint(pts[i*2], pts[i*2+1]);
        {
            GeoPoint[] ls = geopointsFromLinestring(linestring);
            long handle = local.createLineString(ls);
            final Geometry g = local.getGeometry(handle);
            Assert.assertEquals(linestring, g);
        }
        {
            GeoPoint[] ls = geopointsFromLinestring(linestring);
            long handle = local.createLineString(Arrays.asList(ls), SpatialCalculator.Values.GeoPoint);
            final Geometry g = local.getGeometry(handle);
            Assert.assertEquals(linestring, g);
        }

        {
            IGeoPoint[] ls = igeopointsFromLinestring(linestring);
            long handle = local.createLineString(ls);
            final Geometry g = local.getGeometry(handle);
            Assert.assertEquals(linestring, g);
        }
        {
            IGeoPoint[] ls = igeopointsFromLinestring(linestring);
            long handle = local.createLineString(Arrays.asList(ls), SpatialCalculator.Values.IGeoPoint);
            final Geometry g = local.getGeometry(handle);
            Assert.assertEquals(linestring, g);
        }

        local.dispose();
    }

    @Test
    public void polygon_roundtrip() {
        double[][] pts = new double[][]
        {
                // outer ring
                {
                        10, 10,
                        10, 20,
                        20, 20,
                        20, 10,
                        10, 10,
                },
                // inner ring
                {
                        12, 12,
                        12, 14,
                        14, 14,
                        14, 12,
                        12, 12,
                },
        };

        SpatialCalculator local = new SpatialCalculator.Builder()
                .inMemory().build();

        Polygon polygon = new Polygon(2);
        Polygon polygonWithInnerRings = new Polygon(2);
        for(int i = 0; i < pts.length; i++) {
            LineString linestring = new LineString(2);
            for (int j = 0; j < pts.length / 2; j++) {
                linestring.addPoint(pts[i][j * 2], pts[i][j * 2 + 1]);
            }
            if(i == 0)
                polygon.addRing((LineString)linestring.clone());
            polygonWithInnerRings.addRing(linestring);
        }

        {
            GeoPoint[][] poly = new GeoPoint[1+polygonWithInnerRings.getInteriorRings().size()][];
            poly[0] = geopointsFromLinestring(polygonWithInnerRings.getExteriorRing());
            int i = 1;
            for(LineString ring : polygonWithInnerRings.getInteriorRings())
                poly[i++] = geopointsFromLinestring(ring);
            {
                long handle = local.createPolygon(poly[0]);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygon, g);
            }
            if(poly.length > 1) {
                long handle = local.createPolygon(poly[0], Arrays.copyOfRange(poly, 1, poly.length));
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygonWithInnerRings, g);
            }
        }
        {
            GeoPoint[][] poly = new GeoPoint[1+polygonWithInnerRings.getInteriorRings().size()][];
            poly[0] = geopointsFromLinestring(polygonWithInnerRings.getExteriorRing());
            int i = 1;
            for(LineString ring : polygonWithInnerRings.getInteriorRings())
                poly[i++] = geopointsFromLinestring(ring);
            {
                long handle = local.createPolygon(Arrays.asList(poly[0]), SpatialCalculator.Values.GeoPoint);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygon, g);
            }
            if(poly.length > 1) {
                Collection<Collection<GeoPoint>> rings = new ArrayList<>(poly.length-1);
                for(int j = 1; j < poly.length; j++)
                    rings.add(Arrays.asList(poly[j]));
                long handle = local.createPolygon(Arrays.asList(poly[0]), rings, SpatialCalculator.Values.GeoPoint);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygonWithInnerRings, g);
            }
        }

        {
            IGeoPoint[][] poly = new IGeoPoint[1+polygonWithInnerRings.getInteriorRings().size()][];
            poly[0] = igeopointsFromLinestring(polygonWithInnerRings.getExteriorRing());
            int i = 1;
            for(LineString ring : polygonWithInnerRings.getInteriorRings())
                poly[i++] = igeopointsFromLinestring(ring);
            {
                long handle = local.createPolygon(poly[0]);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygon, g);
            }
            if(poly.length > 1) {
                long handle = local.createPolygon(poly[0], Arrays.copyOfRange(poly, 1, poly.length));
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygonWithInnerRings, g);
            }
        }
        {
            IGeoPoint[][] poly = new IGeoPoint[1+polygonWithInnerRings.getInteriorRings().size()][];
            poly[0] = igeopointsFromLinestring(polygonWithInnerRings.getExteriorRing());
            int i = 1;
            for(LineString ring : polygonWithInnerRings.getInteriorRings())
                poly[i++] = igeopointsFromLinestring(ring);
            {
                long handle = local.createPolygon(Arrays.asList(poly[0]), SpatialCalculator.Values.IGeoPoint);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygon, g);
            }
            if(poly.length > 1) {
                Collection<Collection<IGeoPoint>> rings = new ArrayList<>(poly.length-1);
                for(int j = 1; j < poly.length; j++)
                    rings.add(Arrays.asList(poly[j]));
                long handle = local.createPolygon(Arrays.asList(poly[0]), rings, SpatialCalculator.Values.IGeoPoint);
                final Geometry g = local.getGeometry(handle);
                Assert.assertEquals(polygonWithInnerRings, g);
            }
        }

        local.dispose();
    }

    static GeoPoint[] geopointsFromLinestring(LineString linestring) {
        GeoPoint[] ls = new GeoPoint[linestring.getNumPoints()];
        for(int i = 0; i < linestring.getNumPoints(); i++)
            ls[i] = new GeoPoint(linestring.getY(i), linestring.getX(i));
        return ls;
    }

    static IGeoPoint[] igeopointsFromLinestring(LineString linestring) {
        IGeoPoint[] ls = new IGeoPoint[linestring.getNumPoints()];
        for(int i = 0; i < linestring.getNumPoints(); i++)
            ls[i] = new gov.tak.api.engine.map.coords.GeoPoint(linestring.getY(i), linestring.getX(i));
        return ls;
    }
}
