package com.atakmap.coremap.maps.time;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class CoordinatedTimeTest
{
    @Test
    public void roundtrip() throws ParseException
    {
        CoordinatedTime t = new CoordinatedTime();
        final String cot_t = CoordinatedTime.toCot(t);
        Assert.assertNotNull(cot_t);
        final CoordinatedTime parsed_t = CoordinatedTime.fromCot(cot_t);
        Assert.assertNotNull(parsed_t);
        Assert.assertEquals(t.getMilliseconds(), parsed_t.getMilliseconds());
        Assert.assertEquals(0, t.millisecondDiff(parsed_t));
    }

    @Test
    public void gpsOffset_gps_delta_ahead()
    {
        try {
            final long offset = 60000L; // one minute ahead
            final long gpsTime = System.currentTimeMillis() + offset;
            CoordinatedTime.setCoordinatedTimeMillis(gpsTime);
            final long elapsed = (System.currentTimeMillis()-gpsTime);

            Assert.assertTrue(CoordinatedTime.isGPSTime());
            Assert.assertTrue(CoordinatedTime.getCoordinatedTimeOffset() >= elapsed);
        } finally {
            // reset
            CoordinatedTime.setCoordinatedTimeMillis(0L);
        }
    }

    @Test
    public void gpsOffset_gps_delta_behind()
    {
        try {
            final long offset = -60000L; // one minute behind
            final long gpsTime = System.currentTimeMillis() + offset;
            CoordinatedTime.setCoordinatedTimeMillis(gpsTime);
            final long elapsed = (System.currentTimeMillis()-gpsTime);

            Assert.assertTrue(CoordinatedTime.isGPSTime());
            Assert.assertTrue(CoordinatedTime.getCoordinatedTimeOffset() <= elapsed);
        } finally {
            // reset
            CoordinatedTime.setCoordinatedTimeMillis(0L);
        }
    }

    @Test
    public void gpsOffset_no_gps_delta()
    {
        CoordinatedTime.setCoordinatedTimeMillis(0L);

        Assert.assertFalse(CoordinatedTime.isGPSTime());
        Assert.assertEquals(0L, CoordinatedTime.getCoordinatedTimeOffset());
    }

    private void badStringThrows(String s) throws ParseException
    {
        final CoordinatedTime parsed_t = CoordinatedTime.fromCot(s);
        Assert.fail("Unexpected parse result from CoT datetime: " + s);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_year() throws ParseException
    {
        String time = "200X-09-15T21:00:00.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_month() throws ParseException
    {
        String time = "2009-X9-15T21:00:00.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_day() throws ParseException
    {
        String time = "2009-09-1XT21:00:00.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_hour() throws ParseException
    {
        String time = "2009-09-15T2X:00:00.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_minute() throws ParseException
    {
        String time = "2009-09-15T21:0X:00.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_second() throws ParseException
    {
        String time = "2009-09-15T21:00:0X.00Z";
        badStringThrows(time);
    }

    @Test(expected = ParseException.class)
    public void fromCot_invalid_millis() throws ParseException
    {
        String time = "2009-09-15T21:00:00.0XZ";
        badStringThrows(time);
    }
}
