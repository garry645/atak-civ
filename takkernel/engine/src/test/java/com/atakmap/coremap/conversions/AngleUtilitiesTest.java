
package com.atakmap.coremap.conversions;

import org.junit.Test;
import static org.junit.Assert.*;

public class AngleUtilitiesTest {

    @Test
    public void testAngleConversion() {
        assertEquals("45 degrees to radians",
                AngleUtilities.convert(45, Angle.DEGREE, Angle.RADIAN),
                0.7853981633974483, 0.00000000001);
        String format = AngleUtilities.format(45);
        assertEquals("formatting 45 degrees", format, "45" + Angle.DEGREE.getAbbrev());
        assertEquals("formatting 45 degrees", AngleUtilities.format(45, 2),
                "45.00 "+ Angle.DEGREE.getAbbrev());

    }
}
