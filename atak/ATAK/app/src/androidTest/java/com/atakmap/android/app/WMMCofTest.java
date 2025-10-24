
package com.atakmap.android.app;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.util.WMMCof;
import com.atakmap.coremap.maps.conversion.GeomagneticField;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;

/**
 * Tests derived from existing tests from takkernel as well as the WMMHR2025_TEST_VALUES.txt file that
 * ships with the cof file.  For the test results compare to WMMHR2025_TEST_VALUES, everything is rounded
 * to the nearest 100th.
 */
@RunWith(AndroidJUnit4.class)
public class WMMCofTest extends ATAKInstrumentedTest {

    @Test
    public void correctVersion() {
        final Context appContext = ApplicationProvider.getApplicationContext();
        Assert.assertEquals("versionCheck", "WMMHR 2025", WMMCof.getVersion());
    }

    @Before
    public void preTest() {
        final Context appContext = ApplicationProvider.getApplicationContext();
        WMMCof.deploy(appContext);
    }

    @Test
    public void valueTest() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 80, (float) 0, (float) 0, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(1.27d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest2() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 0, (float) 120, (float) 0, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(-0.14d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest3() {
        GeomagneticField gmf = new GeomagneticField(
                (float) -80, (float) 240, (float) 0, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(68.70d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest4() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 80, (float) 0, (float) 100000, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(0.75d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest5() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 0, (float) 120, (float) 100000, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(-0.15d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest6() {
        GeomagneticField gmf = new GeomagneticField(
                (float) -80, (float) 240, (float) 100000, 1735689600000L);  // 01 Jan 2025
        Assert.assertEquals(68.16d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest7() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 80, (float) 0, (float) 0, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(2.59d, gmf.getDeclination(), 0.03); // slight variation
    }

    @Test
    public void valueTest8() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 0, (float) 120, (float) 0, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(-0.23d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest9() {
        GeomagneticField gmf = new GeomagneticField(
                (float) -80, (float) 240, (float) 0, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(68.42d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest10() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 80, (float) 0, (float) 100000, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(2.06d, gmf.getDeclination(), 0.03);  // slight variation
    }

    @Test
    public void valueTest11() {
        GeomagneticField gmf = new GeomagneticField(
                (float) 0, (float) 120, (float) 100000, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(-0.23d, gmf.getDeclination(), 0.01d);
    }

    @Test
    public void valueTest12() {
        GeomagneticField gmf = new GeomagneticField(
                (float) -80, (float) 240, (float) 100000, 1813017600000L);  // 15 Jun 2027
        Assert.assertEquals(67.88d, gmf.getDeclination(), 0.01d);
    }


    @Test
    public void declinationTest()
    {
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double declination = GeoCalculations.magneticDeclination(new GeoPoint(48.80299, 36.87427), d.getTime());
        Assert.assertEquals(8.79306697845459d, declination, 0.01d);
    }

    @Test
    public void convertMagneticToTrueTest() {
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double angle = GeoCalculations.convertFromMagneticToTrue(new GeoPoint(48.80299, 36.87427), 27d, d.getTime());
        Assert.assertEquals(35.79306697845459d, angle, 0.001d);
    }

    @Test
    public void convertTrueToMagneticTest()  {
        Date d = new Date(122, 4, 12, 0, 0, 0);
        final double angle = GeoCalculations.convertFromTrueToMagnetic(new GeoPoint(48.80299, 36.87427), 36, d.getTime());
        Assert.assertEquals(27.20693302154541d, angle, 0.001d);
    }


}
