package com.atakmap.map.layer.feature;

import com.atakmap.database.IteratorCursor;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicPointStyle;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import gov.tak.test.KernelJniTest;

public class FeatureCursorInterop extends KernelJniTest
{
    com.atakmap.interop.Interop<FeatureCursor> FeatureCursor_interop = com.atakmap.interop.Interop.findInterop(FeatureCursor.class);

    @Test
    public void roundtrip()
    {
        Feature[] features = new Feature[]
        {
            new Feature(10L, 101L, "10.101", new Point(1, 2, 3), new BasicPointStyle(0x12345678, 14), new AttributeSet(), Feature.AltitudeMode.Absolute, 0d, FeatureDataStore2.TIMESTAMP_NONE, 1L),
            new Feature(10L, 102L, "10.102", new Point(10, 20, 30), new BasicPointStyle(0xAABBCCDD, 24), new AttributeSet(), Feature.AltitudeMode.Relative, -1d, 1234567L, 2L),
            new Feature(10L, 103L, "10.103", new Point(101, 102, 103), new BasicPointStyle(0x01020304, 12), new AttributeSet(), Feature.AltitudeMode.ClampToGround, 10d, 7654321L, 3L),
            new Feature(20L, 104L, "20.104", new Point(4, 5), new BasicPointStyle(0xA9B8C7D6, 22), new AttributeSet(), Feature.AltitudeMode.Absolute, 100d, 0L, 4L),
            new Feature(30L, 105L, "30.105", new Point(6, 7), new BasicPointStyle(0x12345678, 16), new AttributeSet(), Feature.AltitudeMode.Relative, 1000d, FeatureDataStore2.TIMESTAMP_NONE, 5L),
            new Feature(40L, 106L, "40.106", new Point(8, 9), new BasicPointStyle(0x12345678, 26), new AttributeSet(), Feature.AltitudeMode.ClampToGround, 0d, FeatureDataStore2.TIMESTAMP_NONE, 6L),
        };

        FeatureCursor cursor = new IteratorFeatureCursor(Arrays.asList(features).iterator());
        Pointer wrappedPointer = FeatureCursor_interop.wrap(cursor);
        Assert.assertNotNull(wrappedPointer);
        Assert.assertNotEquals(0L, wrappedPointer.raw);
        FeatureCursor wrapped = FeatureCursor_interop.create(wrappedPointer);

        for(int i = 0; i < features.length; i++) {
            Assert.assertTrue(wrapped.moveToNext());
            Assert.assertEquals(features[i].getName(), features[i].getId(), wrapped.getId());
            Assert.assertEquals(features[i].getName(), features[i].getFeatureSetId(), wrapped.getFsid());
            Assert.assertEquals(features[i].getName(), features[i].getVersion(), wrapped.getVersion());
            FeatureDefinitionInteropTests.assertEquals(features[i].getName(), (FeatureDefinition3) cursor, (FeatureDefinition3) wrapped);
        }
        Assert.assertFalse(wrapped.moveToNext());
    }

    final static class IteratorFeatureCursor extends IteratorCursor<Feature> implements FeatureCursor, FeatureDefinition3
    {

        IteratorFeatureCursor(Iterator<Feature> features)
        {
            super(features);
        }

        @Override
        public Object getRawGeometry() {
            return getRowData().getGeometry();
        }

        @Override
        public int getGeomCoding() {
            return FeatureDefinition.GEOM_ATAK_GEOMETRY;
        }

        @Override
        public String getName() {
            return getRowData().getName();
        }

        @Override
        public int getStyleCoding() {
            return FeatureDefinition.STYLE_ATAK_STYLE;
        }

        @Override
        public Object getRawStyle() {
            return getRowData().getStyle();
        }

        @Override
        public AttributeSet getAttributes() {
            return getRowData().getAttributes();
        }

        @Override
        public Feature get() {
            return getRowData();
        }

        @Override
        public long getId() {
            return getRowData().getId();
        }

        @Override
        public long getVersion() {
            return getRowData().getVersion();
        }

        @Override
        public long getFsid() {
            return getRowData().getFeatureSetId();
        }

        @Override
        public long getTimestamp() {
            return getRowData().getTimestamp();
        }

        @Override
        public Feature.AltitudeMode getAltitudeMode() {
            return getRowData().getAltitudeMode();
        }

        @Override
        public double getExtrude() {
            return getRowData().getExtrude();
        }
    }
}
