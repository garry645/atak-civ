
package com.atakmap.map.layer.feature;

import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.math.Matrix;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import gov.tak.test.KernelJniTest;

public class MeshPointStyleTests extends KernelJniTest {
    private final float[] IDENTITY_MATRIX = {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
    };

    @Test
    public void MeshPointStyle_constructor_1_roundtrip() {
        final String uri = UUID.randomUUID().toString();
        final int color = RandomUtils.rng().nextInt();
        final float[] transform = RandomUtils.randomFloatArray(16);
        MeshPointStyle style = new MeshPointStyle(uri, color, transform);
        Assert.assertEquals(style.getColor(), color);
        Assert.assertEquals(style.getMeshUri(), uri);
        Assert.assertArrayEquals(style.getTransform(), transform, 0);
    }

    @Test(expected = RuntimeException.class)
    public void MeshPointStyle_constructor_1_null_uri_throws() {
        final String uri = null;
        final int color = RandomUtils.rng().nextInt();
        MeshPointStyle style = new MeshPointStyle(uri, color, IDENTITY_MATRIX);
        Assert.fail();
    }

    @Test(expected = RuntimeException.class)
    public void MeshPointStyle_constructor_1_invalid_transform_throws() {
        final String uri = null;
        final int color = RandomUtils.rng().nextInt();
        MeshPointStyle style = new MeshPointStyle(uri, color, new float[] { 1, 2, 3 });
        Assert.fail();
    }

    static MeshPointStyle random() {
        final int color = RandomUtils.rng().nextInt();
        final String uri = UUID.randomUUID().toString();
        final float[] transform = RandomUtils.randomFloatArray(16);
        return new MeshPointStyle(uri, color, transform);
    }
}
