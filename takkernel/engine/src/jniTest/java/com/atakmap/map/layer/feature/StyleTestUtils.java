
package com.atakmap.map.layer.feature;

import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.map.layer.feature.style.Style;

import org.junit.Assert;

public final class StyleTestUtils {
    private StyleTestUtils() {
    }

    static Style randomStyle(boolean composite) {
        int styleType = RandomUtils.rng().nextInt(composite ? 9 : 8);
        if (styleType == 0)
            return BasicFillStyleTests.random();
        else if (styleType == 1)
            return BasicPointStyleTests.random();
        else if (styleType == 2)
            return BasicStrokeStyleTests.random();
        else if (styleType == 3)
            return IconPointStyleTests.random();
        else if (styleType == 4)
            return LabelPointStyleTests.random();
        else if (styleType == 5)
            return LevelOfDetailStyleTests.random();
        else if (styleType == 6)
            return ArrowStrokeStyleTests.random();
        else if (styleType == 7)
            return MeshPointStyleTests.random();
        else if (styleType == 8)
            return CompositeStyleTests.random();
        else
            throw new IllegalStateException();
    }

    static void assertEqual(Style s1, Style s2) {
        if (s1 == null && s2 == null)
            return;
        Assert.assertNotNull(s1);
        Assert.assertNotNull(s2);

        Assert.assertEquals(s1.getClass(), s2.getClass());

        if (s1 instanceof BasicFillStyle) {
            BasicFillStyle i1 = (BasicFillStyle) s1;
            BasicFillStyle i2 = (BasicFillStyle) s2;
            Assert.assertEquals(i1.getColor(), i2.getColor());
        } else if (s1 instanceof BasicStrokeStyle) {
            BasicStrokeStyle i1 = (BasicStrokeStyle) s1;
            BasicStrokeStyle i2 = (BasicStrokeStyle) s2;
            Assert.assertEquals(i1.getColor(), i2.getColor());
            Assert.assertEquals(i1.getStrokeWidth(), i2.getStrokeWidth(), 0.0);
        } else if (s1 instanceof BasicPointStyle) {
            BasicPointStyle i1 = (BasicPointStyle) s1;
            BasicPointStyle i2 = (BasicPointStyle) s2;
            Assert.assertEquals(i1.getColor(), i2.getColor());
            Assert.assertEquals(i1.getSize(), i2.getSize(), 0.0);
        } else if (s1 instanceof IconPointStyle) {
            IconPointStyle i1 = (IconPointStyle) s1;
            IconPointStyle i2 = (IconPointStyle) s2;
            Assert.assertEquals(i1.getIconUri(), i2.getIconUri());
            Assert.assertEquals(i1.getColor(), i2.getColor());
            Assert.assertEquals(i1.getIconAlignmentX(), i2.getIconAlignmentX());
            Assert.assertEquals(i1.getIconAligmnentY(), i2.getIconAligmnentY());
            Assert.assertEquals(i1.getIconWidth(), i2.getIconWidth(), 0.0);
            Assert.assertEquals(i1.getIconHeight(), i2.getIconHeight(), 0.0);
            Assert.assertEquals(i1.getIconRotation(), i2.getIconRotation(),
                    0.0);
            Assert.assertEquals(i1.isRotationAbsolute(),
                    i2.isRotationAbsolute());
        } else if (s1 instanceof MeshPointStyle) {
            MeshPointStyle i1 = (MeshPointStyle) s1;
            MeshPointStyle i2 = (MeshPointStyle) s2;
            Assert.assertEquals(i1.getMeshUri(), i2.getMeshUri());
            Assert.assertEquals(i1.getColor(), i2.getColor());
            Assert.assertArrayEquals(i1.getTransform(), i2.getTransform(), 0.0f);
        } else if (s1 instanceof LabelPointStyle) {
            LabelPointStyle i1 = (LabelPointStyle) s1;
            LabelPointStyle i2 = (LabelPointStyle) s2;
            Assert.assertEquals(i1.getText(), i2.getText());
            Assert.assertEquals(i1.getTextColor(), i2.getTextColor());
            Assert.assertEquals(i1.getBackgroundColor(),
                    i2.getBackgroundColor());
            Assert.assertEquals(i1.getLabelAlignmentX(),
                    i2.getLabelAlignmentX());
            Assert.assertEquals(i1.getLabelAlignmentY(),
                    i2.getLabelAlignmentY());
            Assert.assertEquals(i1.getScrollMode(), i2.getScrollMode());
            Assert.assertEquals(i1.getLabelRotation(), i2.getLabelRotation(),
                    0.0);
            Assert.assertEquals(i1.isRotationAbsolute(),
                    i2.isRotationAbsolute());
        } else if (s1 instanceof CompositeStyle) {
            CompositeStyle i1 = (CompositeStyle) s1;
            CompositeStyle i2 = (CompositeStyle) s2;
            Assert.assertEquals(i1.getNumStyles(), i2.getNumStyles());
            for (int i = 0; i < i1.getNumStyles(); i++)
                assertEqual(i1.getStyle(i), i2.getStyle(i));
        } else if (s1 instanceof LevelOfDetailStyle) {
            LevelOfDetailStyle i1 = (LevelOfDetailStyle) s1;
            LevelOfDetailStyle i2 = (LevelOfDetailStyle) s2;
            assertEqual(i1.getStyle(), i2.getStyle());
            Assert.assertEquals(i1.getMinLevelOfDetail(), i2.getMinLevelOfDetail());
            Assert.assertEquals(i1.getMaxLevelOfDetail(), i2.getMaxLevelOfDetail());
        } else if (s1 instanceof ArrowStrokeStyle) {
            ArrowStrokeStyle i1 = (ArrowStrokeStyle) s1;
            ArrowStrokeStyle i2 = (ArrowStrokeStyle) s2;
            Assert.assertEquals(i1.getArrowRadius(), i2.getArrowRadius(), 0.0);
            Assert.assertEquals(i1.getFactor(), i2.getFactor());
            Assert.assertEquals(i1.getPattern(), i2.getPattern());
            Assert.assertEquals(i1.getColor(), i2.getColor());
            Assert.assertEquals(i1.getStrokeWidth(), i2.getStrokeWidth(), 0.0);
            Assert.assertEquals(i1.getExtrudeMode(), i2.getExtrudeMode());
            Assert.assertEquals(i1.getArrowHeadMode(), i2.getArrowHeadMode());
        } else {
            Assert.fail("Class type: " + s1.getClass() + " invalid");
        }
    }

    static void assertAngleInDegreesEqual(double expectedDegrees,
            double actualDegrees, double epsilon) {
        Assert.assertEquals(expectedDegrees % 360d, actualDegrees % 360d,
                epsilon);
    }
}
