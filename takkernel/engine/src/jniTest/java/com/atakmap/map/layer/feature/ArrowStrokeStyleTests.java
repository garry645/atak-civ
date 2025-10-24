
package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.style.ArrowStrokeStyle.ArrowHeadMode;
import org.junit.Assert;
import org.junit.Test;

import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;

import gov.tak.test.KernelJniTest;

public class ArrowStrokeStyleTests extends KernelJniTest
{
    @Test
    public void ArrowStrokeStyle_constructor_roundtrip()
    {
        final float radius = RandomUtils.rng().nextFloat() * 100;
        final int factor = 4;
        final short pattern = (short) RandomUtils.rng().nextInt();
        final int color = RandomUtils.rng().nextInt();
        final float width = RandomUtils.rng().nextFloat() * 100;
        final int extrudeMode = RandomUtils.rng().nextInt(BasicStrokeStyle.EXTRUDE_ENDPOINT + 1);
        final ArrowHeadMode arrowHeadMode = ArrowHeadMode.values()[RandomUtils.rng().nextInt(ArrowHeadMode.values().length)];
        ArrowStrokeStyle style = new ArrowStrokeStyle(radius, factor, pattern, color, width, extrudeMode, arrowHeadMode);
        Assert.assertEquals(style.getArrowRadius(), radius, 0.0);
        Assert.assertEquals(style.getFactor(), factor);
        Assert.assertEquals(style.getPattern(), pattern);
        Assert.assertEquals(style.getColor(), color);
        Assert.assertEquals(style.getStrokeWidth(), width, 0.0);
        Assert.assertEquals(style.getExtrudeMode(), extrudeMode);
        Assert.assertEquals(style.getArrowHeadMode(), arrowHeadMode);
    }

    @Test(expected = RuntimeException.class)
    public void ArrowStrokeStyle_constructor_negative_radius_throws()
    {
        final int color = RandomUtils.rng().nextInt();
        ArrowStrokeStyle style = new ArrowStrokeStyle(-1, 2, (short) 0, 0, 15f, 0);
        Assert.fail();
    }

    @Test(expected = RuntimeException.class)
    public void ArrowStrokeStyle_constructor_negative_width_throws()
    {
        ArrowStrokeStyle style = new ArrowStrokeStyle(50, 2, (short) 0, 0, -1f, 0);
        Assert.fail();
    }

    static ArrowStrokeStyle random()
    {
        final float radius = RandomUtils.rng().nextFloat() * 100;
        final int factor = 4;
        final short pattern = (short) RandomUtils.rng().nextInt();
        final int color = RandomUtils.rng().nextInt();
        final float width = RandomUtils.rng().nextFloat() * 100;
        final int extrudeMode = RandomUtils.rng().nextInt(BasicStrokeStyle.EXTRUDE_ENDPOINT + 1);
        final ArrowHeadMode arrowHeadMode = ArrowHeadMode.values()[RandomUtils.rng().nextInt(ArrowHeadMode.values().length)];
        return new ArrowStrokeStyle(radius, factor, pattern, color, width, extrudeMode, arrowHeadMode);
    }
}
