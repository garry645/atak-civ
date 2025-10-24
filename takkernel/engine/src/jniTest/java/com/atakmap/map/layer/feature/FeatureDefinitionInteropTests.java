package com.atakmap.map.layer.feature;

import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import gov.tak.test.KernelJniTest;

public class FeatureDefinitionInteropTests extends KernelJniTest
{
    final static Interop<FeatureDefinition3> FeatureDefinition_interop = Interop.findInterop(FeatureDefinition3.class);

    @Test
    public void roundtrip()
    {
        final FeatureDefinition3[] definitions =
        {
            createDefinition(
                    "GatakSogrAemptyTS1234567AMrelativeEX-1",
                    FeatureDefinition3.GEOM_ATAK_GEOMETRY,
                    new com.atakmap.map.layer.feature.geometry.Point(1, 2, 3),
                    FeatureDefinition3.STYLE_OGR,
                    FeatureStyleParser.pack(new com.atakmap.map.layer.feature.style.BasicPointStyle(0xFFAABBCC, 32f)),
                    new AttributeSet(),
                    1234567L,
                    Feature.AltitudeMode.Relative,
                    -1d
            ),
            createDefinition(
                    "GblobSatakArandomeTSnoneAMclampEX10",
                    FeatureDefinition3.GEOM_SPATIALITE_BLOB,
                    GeometryFactory.toSpatiaLiteBlob(new com.atakmap.map.layer.feature.geometry.Point(1, 2, 3), 4326),
                    FeatureDefinition3.STYLE_ATAK_STYLE,
                    new com.atakmap.map.layer.feature.style.BasicPointStyle(0xFFAABBCC, 32f),
                    AttributeSetTests.randomAttributeSet(),
                    FeatureDataStore2.TIMESTAMP_NONE,
                    Feature.AltitudeMode.ClampToGround,
                    10
            ),
        };
        for(FeatureDefinition3 d : definitions)
            roundtripImpl(d);
    }

    private void roundtripImpl(FeatureDefinition3 definition)
    {
        Pointer wrappedPointer = FeatureDefinition_interop.wrap(definition);
        Assert.assertNotNull(wrappedPointer);
        Assert.assertNotEquals(0L, wrappedPointer.raw);
        FeatureDefinition3 wrapped = FeatureDefinition_interop.create(wrappedPointer);

        assertEquals(definition.getName(), definition, wrapped);
    }

    static void assertEquals(String message, FeatureDefinition3 expected, FeatureDefinition3 actual)
    {
        if(expected.getRawGeometry() instanceof byte[])
            Assert.assertArrayEquals(message, (byte[])expected.getRawGeometry(), (byte[])actual.getRawGeometry());
        else
            Assert.assertEquals(message, expected.getRawGeometry(), actual.getRawGeometry());
        Assert.assertEquals(message, expected.getGeomCoding(), actual.getGeomCoding());
        Assert.assertEquals(message, expected.getName(), actual.getName());
        Assert.assertEquals(message, expected.getStyleCoding(), actual.getStyleCoding());
        Assert.assertEquals(message, expected.getRawStyle(), actual.getRawStyle());
        Assert.assertEquals(message, expected.getAttributes(), actual.getAttributes());
        Assert.assertEquals(message, expected.getTimestamp(), actual.getTimestamp());
        Assert.assertEquals(message, expected.getAltitudeMode(), actual.getAltitudeMode());
        Assert.assertEquals(message, expected.getExtrude(), actual.getExtrude(), 0d);
    }

    static FeatureDefinition3 createDefinition(String name, int geomCoding, Object rawGeom, int styleCoding, Object rawStyle, AttributeSet attrs, long timestamp, Feature.AltitudeMode altMode, double extrude)
    {
        FeatureDefinition3 definition = Mockito.mock(FeatureDefinition3.class);
        Mockito.when(definition.getName()).thenReturn(name);
        Mockito.when(definition.getRawGeometry()).thenReturn(rawGeom);
        Mockito.when(definition.getGeomCoding()).thenReturn(geomCoding);
        Mockito.when(definition.getRawStyle()).thenReturn(rawStyle);
        Mockito.when(definition.getStyleCoding()).thenReturn(styleCoding);
        Mockito.when(definition.getAttributes()).thenReturn(attrs);
        Mockito.when(definition.getTimestamp()).thenReturn(timestamp);
        Mockito.when(definition.getAltitudeMode()).thenReturn(altMode);
        Mockito.when(definition.getExtrude()).thenReturn(extrude);
        Mockito.when(definition.get()).thenReturn(
                new Feature(FeatureDataStore2.FEATURESET_ID_NONE,
                        FeatureDataStore2.FEATURE_ID_NONE,
                        name,
                        Feature.getGeometry(geomCoding, rawGeom),
                        Feature.getStyle(styleCoding, rawStyle),
                        attrs,
                        altMode,
                        extrude,
                        timestamp,
                        FeatureDataStore2.FEATURE_VERSION_NONE));
        return definition;
    }
}
