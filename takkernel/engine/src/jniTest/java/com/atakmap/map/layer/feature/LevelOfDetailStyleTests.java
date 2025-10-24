
package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import org.junit.Assert;
import org.junit.Test;

import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;

import gov.tak.test.KernelJniTest;

public class LevelOfDetailStyleTests extends KernelJniTest {

    @Test
    public void LevelOfDetailStyle_constructor_roundtrip() {
        BasicPointStyle childStyle = new BasicPointStyle(-1, 24f);
        final int lod1 = RandomUtils.rng().nextInt(LevelOfDetailStyle.MAX_LOD + 1);
        int lod2 = RandomUtils.rng().nextInt(LevelOfDetailStyle.MAX_LOD + 1);
        if (lod1 == lod2) lod2 = (lod2 + 1) % (LevelOfDetailStyle.MAX_LOD + 1);
        LevelOfDetailStyle style = new LevelOfDetailStyle(childStyle, Math.min(lod1, lod2), Math.max(lod1, lod2));
        Assert.assertEquals(style.getStyle(), childStyle);
        Assert.assertEquals(style.getMinLevelOfDetail(), Math.min(lod1, lod2));
        Assert.assertEquals(style.getMaxLevelOfDetail(), Math.max(lod1, lod2));
    }

    static LevelOfDetailStyle random() {
        BasicPointStyle childStyle = new BasicPointStyle(-1, 24f);
        final int lod1 = RandomUtils.rng().nextInt(LevelOfDetailStyle.MAX_LOD + 1);
        int lod2 = RandomUtils.rng().nextInt(LevelOfDetailStyle.MAX_LOD + 1);
        if (lod1 == lod2) lod2 = (lod2 + 1) % (LevelOfDetailStyle.MAX_LOD + 1);
        return new LevelOfDetailStyle(childStyle, Math.min(lod1, lod2), Math.max(lod1, lod2));
    }
}
