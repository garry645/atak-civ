
package com.atakmap.android.conversions;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CoordinateFormatUtilitiesTest extends ATAKInstrumentedTest {

    // 6    0.000001    0d 00m 0.0036s    individual humans    111 mm    102 mm    78.7 mm    43.5 mm
    private final double DELTA = .000005d;

    private static GeoPoint[] points = new GeoPoint[] {
            GeoPoint.ZERO_POINT,
            new GeoPoint(72.4999595, 42.44949449),
            new GeoPoint(42.443223342, 72.556665),
            new GeoPoint(-1.13453556, 43.222262),
            new GeoPoint(-55.000, -55.000)
    };

    private boolean equals(GeoPoint a, GeoPoint b) {
        return Math.abs(a.getLatitude() - b.getLatitude()) < DELTA;
    }

    @Test
    public void roundTripDMS() {
        for (GeoPoint point : points) {
            String coord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DMS);
            GeoPoint rtPoint = CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.DMS);
            Assert.assertTrue(equals(point, rtPoint));
            String rtCoord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DMS);
            Assert.assertEquals(coord, rtCoord);
        }
    }

    @Test
    public void roundTripDM() {
        for (GeoPoint point : points) {
            String coord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DM);
            GeoPoint rtPoint = CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.DM);
            Assert.assertTrue(equals(rtPoint, point));
            String rtCoord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DM);
            Assert.assertEquals(coord, rtCoord);
        }
    }

    @Test
    public void roundTripDD() {
        for (GeoPoint point : points) {
            String coord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DD);
            GeoPoint rtPoint = CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.DD);
            Assert.assertTrue(equals(rtPoint, point));
            String rtCoord = CoordinateFormatUtilities.formatToString(point,
                    CoordinateFormat.DD);
            Assert.assertEquals(coord, rtCoord);
        }
    }
}
