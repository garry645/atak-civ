
package com.atakmap.util;

import com.atakmap.android.util.HorizonUtilities;
import com.atakmap.coremap.conversions.ConversionFactors;

import org.junit.Assert;
import org.junit.Test;

public class HorizonUtilitiesTest {
    @Test
    public void test_getHorizontalDistance() {

        // comparison against values in: https://sites.math.washington.edu/~conroy/m120-general/horizon.pdf
        Assert.assertEquals(2.7383 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        5 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(3.8725 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        10 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(5.4765 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        20 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(8.6592 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        50 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(12.246 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        100 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(27.383 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        500 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(38.725 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        1000 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(122.459 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        10000 * ConversionFactors.FEET_TO_METERS),
                1);
        Assert.assertEquals(387.249 * ConversionFactors.MILES_TO_METERS,
                HorizonUtilities.getHorizonDistance(
                        100000 * ConversionFactors.FEET_TO_METERS),
                5);
    }

    @Test
    public void test_getHorizontalDrop() {
        // comparison against values in: https://earthcurvature.com/
        Assert.assertEquals(0.08, HorizonUtilities.getCurvatureDrop(1000d),
                .01d);
        Assert.assertEquals(0.31, HorizonUtilities.getCurvatureDrop(2000d),
                .01d);
        Assert.assertEquals(1.96, HorizonUtilities.getCurvatureDrop(5000d),
                .01d);
        Assert.assertEquals(7.85, HorizonUtilities.getCurvatureDrop(10000d),
                .01d);
        Assert.assertEquals(31.39, HorizonUtilities.getCurvatureDrop(20000d),
                .01d);
        Assert.assertEquals(196.20, HorizonUtilities.getCurvatureDrop(50000d),
                .01d);
        Assert.assertEquals(784.79, HorizonUtilities.getCurvatureDrop(100000d),
                .01d);
        Assert.assertEquals(3138.97, HorizonUtilities.getCurvatureDrop(200000d),
                .025d);
        Assert.assertEquals(19610.09,
                HorizonUtilities.getCurvatureDrop(500000d), .175d);
        Assert.assertEquals(78319.62,
                HorizonUtilities.getCurvatureDrop(1000000d), .70d);

    }

}
