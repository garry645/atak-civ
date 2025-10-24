package com.atakmap.map.layer.feature;

import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FeatureDefinition4InteropTests extends KernelJniTest
{
    final static Interop<FeatureDefinition4> FeatureDefinition_interop = Interop.findInterop(FeatureDefinition4.class);

    @Test
    public void roundtrip()
    {
        Feature.Traits traits1 = new Feature.Traits();
        traits1.altitudeMode = Feature.AltitudeMode.Relative;
        traits1.extrude = -1d;
        traits1.lineMode = Feature.LineMode.Rhumb;
        Feature.Traits traits2 = new Feature.Traits();
        traits2.altitudeMode = Feature.AltitudeMode.ClampToGround;
        traits2.extrude = 10;
        traits1.lineMode = Feature.LineMode.GreatCircle;
        final FeatureDefinition4[] definitions =
        {
            createDefinition(
                    "GatakSogrAemptyTS1234567AMrelativeEX-1",
                    FeatureDefinition4.GEOM_ATAK_GEOMETRY,
                    new com.atakmap.map.layer.feature.geometry.Point(1, 2, 3),
                    FeatureDefinition4.STYLE_OGR,
                    FeatureStyleParser.pack(new com.atakmap.map.layer.feature.style.BasicPointStyle(0xFFAABBCC, 32f)),
                    new AttributeSet(),
                    1234567L,
                    traits1
            ),
            createDefinition(
                    "GblobSatakArandomeTSnoneAMclampEX10",
                    FeatureDefinition4.GEOM_SPATIALITE_BLOB,
                    GeometryFactory.toSpatiaLiteBlob(new com.atakmap.map.layer.feature.geometry.Point(1, 2, 3), 4326),
                    FeatureDefinition4.STYLE_ATAK_STYLE,
                    new com.atakmap.map.layer.feature.style.BasicPointStyle(0xFFAABBCC, 32f),
                    AttributeSetTests.randomAttributeSet(),
                    FeatureDataStore2.TIMESTAMP_NONE,
                    traits2
            ),
        };
        for(FeatureDefinition4 d : definitions)
            roundtripImpl(d);
    }

    private void roundtripImpl(FeatureDefinition4 definition)
    {
        Pointer wrappedPointer = FeatureDefinition_interop.wrap(definition);
        Assert.assertNotNull(wrappedPointer);
        Assert.assertNotEquals(0L, wrappedPointer.raw);
        FeatureDefinition4 wrapped = FeatureDefinition_interop.create(wrappedPointer);

        assertEquals(definition.getName(), definition, wrapped);
    }

    static void assertEquals(String message, FeatureDefinition4 expected, FeatureDefinition4 actual)
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
        Assert.assertEquals(message, expected.getTraits(), actual.getTraits());
    }

    static FeatureDefinition4 createDefinition(String name, int geomCoding, Object rawGeom, int styleCoding, Object rawStyle, AttributeSet attrs, long timestamp, Feature.Traits traits)
    {
        FeatureDefinition4 definition = Mockito.mock(FeatureDefinition4.class);
        Mockito.when(definition.getName()).thenReturn(name);
        Mockito.when(definition.getRawGeometry()).thenReturn(rawGeom);
        Mockito.when(definition.getGeomCoding()).thenReturn(geomCoding);
        Mockito.when(definition.getRawStyle()).thenReturn(rawStyle);
        Mockito.when(definition.getStyleCoding()).thenReturn(styleCoding);
        Mockito.when(definition.getAttributes()).thenReturn(attrs);
        Mockito.when(definition.getTimestamp()).thenReturn(timestamp);
        Mockito.when(definition.getTraits()).thenReturn(traits);
        Mockito.when(definition.get()).thenReturn(
                new Feature(FeatureDataStore2.FEATURESET_ID_NONE,
                        FeatureDataStore2.FEATURE_ID_NONE,
                        name,
                        Feature.getGeometry(geomCoding, rawGeom),
                        Feature.getStyle(styleCoding, rawStyle),
                        attrs,
                        traits,
                        timestamp,
                        FeatureDataStore2.FEATURE_VERSION_NONE));
        return definition;
    }
}
