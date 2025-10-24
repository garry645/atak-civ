package com.atakmap.coremap.maps.coords;

import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

public class DistanceCalculationsTest extends KernelJniTest
{
    @Test
    public void haversineTest()
    {
        double distance = DistanceCalculations.haversine(35, 42, 35.06371235, 42.0775188);
        Assert.assertEquals(10011.5355, distance, .001);
    }
}