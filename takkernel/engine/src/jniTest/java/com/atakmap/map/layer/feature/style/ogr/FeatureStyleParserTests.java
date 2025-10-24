package com.atakmap.map.layer.feature.style.ogr;

import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle.ArrowHeadMode;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.map.layer.feature.style.Style;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class FeatureStyleParserTests extends KernelJniTest
{
    private final float[] IDENTITY_MATRIX = {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
    };

    private void styleRoundtrip(String msg, Style s)
    {
        String ogr = FeatureStyleParser.pack(s);
        Assert.assertNotNull(msg, ogr);

        Style parsed = FeatureStyleParser.parse2(ogr);
        Assert.assertNotNull(msg, parsed);
        Assert.assertEquals(msg, s, parsed);
    }
    @Test
    public void IconPointStyle_roundtrip()
    {
        styleRoundtrip("IconPointStyle<color, uri>", new IconPointStyle(0x98765432, "scheme://some/uri/path.ext"));
        styleRoundtrip("IconPointStyle<color, uri, 32, 32, -50, 50, 0, 0, 0, false>", new IconPointStyle(0x98765432, "scheme://some/uri/path.ext", 32f, 32f, -50, 50, 0, 0, 0f, false));
        styleRoundtrip("IconPointStyle<color, uri, 32, 32, -50, 50, 1, -1, 45, true>", new IconPointStyle(0x98765432, "scheme://some/uri/path.ext", 32f, 32f, -50, 50, 1, -1, 45f, true));
        styleRoundtrip("IconPointStyle<color, uri, 2, -1, 1, 90, false>", new IconPointStyle(0x98765432, "scheme://some/uri/path.ext", 2f, -1, 1, 90f, false));
    }

    @Test
    public void MeshPointStyle_roundtrip()
    {
        styleRoundtrip("MeshPointStyle<uri, color, IDENTITY_MATRIX>", new MeshPointStyle("scheme://some/uri/path.ext", 0x98765432, IDENTITY_MATRIX));
    }

    @Test
    public void LabelPointStyle_roundtrip()
    {
        styleRoundtrip("LabelPointStyle<text, color, color, ON>", new LabelPointStyle("text", 0x98765432, 0x23456789, ScrollMode.ON));
        // minRenderResolution is not persisted - we use the default of 13 here.
        styleRoundtrip("LabelPointStyle<text, color, color, ON, 24, -50, 50, 0, 0, 90f, false, 13, 2f>", new LabelPointStyle("text", 0x98765432, 0x23456789, ScrollMode.ON, 24f, -50, 50, 0, 0, 90f, false, 13, 2f));
    }

    @Test
    public void BasicStrokeStyle_roundtrip()
    {
        styleRoundtrip("BasicStrokeStyle<color, 8, VERTEX>", new BasicStrokeStyle(0x98765432, 8f, BasicStrokeStyle.EXTRUDE_VERTEX));
        styleRoundtrip("BasicStrokeStyle<color, 4, ENDPOINT>", new BasicStrokeStyle(0x98765432, 4f, BasicStrokeStyle.EXTRUDE_ENDPOINT));
        styleRoundtrip("BasicStrokeStyle<color, 2, NONE>", new BasicStrokeStyle(0x98765432, 2f, BasicStrokeStyle.EXTRUDE_NONE));
    }

    @Test
    public void LevelOfDetailStyle_roundtrip()
    {
        BasicPointStyle pointStyle = new BasicPointStyle(-1, 24);
        styleRoundtrip("LevelOfDetailStyle<style, MIN_LOD, MAX_LOD>", new LevelOfDetailStyle(pointStyle, LevelOfDetailStyle.MIN_LOD, LevelOfDetailStyle.MAX_LOD));
        styleRoundtrip("LevelOfDetailStyle<style, MIN_LOD, 5>", new LevelOfDetailStyle(pointStyle, LevelOfDetailStyle.MIN_LOD, 5));
        styleRoundtrip("LevelOfDetailStyle<style, 5, 25>", new LevelOfDetailStyle(pointStyle, 5, 25));
        styleRoundtrip("LevelOfDetailStyle<style, 10, 15>", new LevelOfDetailStyle(pointStyle, 10, 15));
    }

    @Test
    public void ArrowStrokeStyle_roundtrip()
    {
        styleRoundtrip("ArrowStrokeStyle<15, 2, pattern, color, VERTEX, OnlyLast>", new ArrowStrokeStyle(15, 2, (short)0b000000000000000, 0x98765432, 5, BasicStrokeStyle.EXTRUDE_VERTEX, ArrowHeadMode.OnlyLast));
        styleRoundtrip("ArrowStrokeStyle<30, 4, pattern, color, ENDPOINT, OnlyLast>", new ArrowStrokeStyle(30, 4, (short)0b111110000111111, 0x98765432, 15, BasicStrokeStyle.EXTRUDE_ENDPOINT, ArrowHeadMode.OnlyLast));
        styleRoundtrip("ArrowStrokeStyle<500, 16, pattern, color, NONE, PerVertex>", new ArrowStrokeStyle(500, 16, (short)0b111111111111111, 0x98765432, 100, BasicStrokeStyle.EXTRUDE_NONE, ArrowHeadMode.PerVertex));
    }

    // ENGINE-707 caused an infinite loop so we add a timeout here
    @Test(timeout=1000)
    public void LabelPointStyle_text_with_double_quotes()
    {
        styleRoundtrip("LabelPointStyle<text with \"quotes\", color, color, DEFAULT>", new LabelPointStyle("text with \"quotes\"", 0x98765432, 0x23456789, ScrollMode.DEFAULT));
    }
}
