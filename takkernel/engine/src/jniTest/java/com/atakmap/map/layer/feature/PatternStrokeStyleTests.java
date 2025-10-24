
package com.atakmap.map.layer.feature;

import android.graphics.Color;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import org.junit.Assert;
import org.junit.Test;

import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;

import gov.tak.test.KernelJniTest;

public class PatternStrokeStyleTests extends KernelJniTest
{
    @Test
    public void PatternStrokeStyle_constructor_roundtrip()
    {
        final int factor = 4;
        final short pattern = (short) RandomUtils.rng().nextInt();
        final float r = RandomUtils.rng().nextFloat();
        final float g = RandomUtils.rng().nextFloat();
        final float b = RandomUtils.rng().nextFloat();
        final float a = RandomUtils.rng().nextFloat();
        final float width = RandomUtils.rng().nextFloat() * 100;
        final int extrudeMode = RandomUtils.rng().nextInt(BasicStrokeStyle.EXTRUDE_ENDPOINT + 1);
        PatternStrokeStyle style = new PatternStrokeStyle(factor, pattern, r, g, b, a, width, extrudeMode);
        Assert.assertEquals(style.getFactor(), factor);
        Assert.assertEquals(style.getPattern(), pattern);
        Assert.assertEquals(style.getColor(), Color.argb((int) (a * 255), (int) (r * 255), (int) (g * 255), (int) (b * 255)));
        Assert.assertEquals(style.getStrokeWidth(), width, 0.0);
        Assert.assertEquals(style.getExtrudeMode(), extrudeMode);
    }

    @Test(expected = RuntimeException.class)
    public void PatternStrokeStyle_constructor_negative_width_throws()
    {
        PatternStrokeStyle style = new PatternStrokeStyle(2, (short) 0, 0, 0, 0, 1, -1f, 0);
        Assert.fail();
    }

    static PatternStrokeStyle random()
    {
        final int factor = 4;
        final short pattern = (short) RandomUtils.rng().nextInt();
        final float r = RandomUtils.rng().nextFloat();
        final float g = RandomUtils.rng().nextFloat();
        final float b = RandomUtils.rng().nextFloat();
        final float a = RandomUtils.rng().nextFloat();
        final float width = RandomUtils.rng().nextFloat() * 100;
        final int extrudeMode = RandomUtils.rng().nextInt(BasicStrokeStyle.EXTRUDE_ENDPOINT + 1);
        return new PatternStrokeStyle(factor, pattern, r, g, b, a, width, extrudeMode);
    }
}
