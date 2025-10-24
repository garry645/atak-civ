package gov.tak.api.cot;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Validate CoordinatedTime functionality.
 *
 * @since 6.0.0
 */
public class CoordinatedTimeTest
{
    /**
     * COT Time per ISO 8601
     */
    private final static CoordinatedTime.SimpleDateFormatThread _COT_TIME_FORMAT =
            new CoordinatedTime.SimpleDateFormatThread("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.US, TimeZone.getTimeZone("UTC"));
    /**
     * COT Time per ISO 8601
     */
    private final static CoordinatedTime.SimpleDateFormatThread _LIBERAL_COT_TIME_FORMAT =
            new CoordinatedTime.SimpleDateFormatThread("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US,
                    TimeZone.getTimeZone("UTC"));

    // Allow for some clock ticks between creation and testing of "now" values
    public static final Offset<Long> CLOCK_SLOP = Offset.offset(5L);

    @Before
    public void cleanupBefore()
    {
        CoordinatedTime.setCoordinatedTimeMillis(0); // Ensure reset
    }

    @After
    public void cleanupAfter()
    {
        CoordinatedTime.setCoordinatedTimeMillis(0); // Ensure reset
    }

    @Test
    public void test_initial_gps_drift()
    {
        assertThat(CoordinatedTime.isGPSTime()).isFalse();
        assertThat(CoordinatedTime.getCoordinatedTimeOffset()).isEqualTo(0);
    }

    @Test
    public void test_set_gps_drift()
    {
        final long now = System.currentTimeMillis() + 100; // Just so it is not actually equal to "now"
        CoordinatedTime.setCoordinatedTimeMillis(now);

        assertThat(CoordinatedTime.isGPSTime()).isTrue();
        assertThat(CoordinatedTime.getCoordinatedTimeOffset()).isCloseTo(-100L, CLOCK_SLOP);
    }

    @Test
    public void test_construct_with_specific_time()
    {
        final long now = System.currentTimeMillis() + 100; // Just so it is not actually equal to "now"

        assertThat(new CoordinatedTime(now).getMilliseconds()).isEqualTo(now);
    }

    @Test
    public void test_construct_with_specific_time_ignores_gps_drift()
    {
        final long now = System.currentTimeMillis();

        CoordinatedTime.setCoordinatedTimeMillis(now - 100);

        assertThat(new CoordinatedTime(now).getMilliseconds()).isEqualTo(now);
    }

    @Test
    public void test_construct_with_current()
    {
        assertThat(new CoordinatedTime().getMilliseconds())
                .isCloseTo(System.currentTimeMillis(), CLOCK_SLOP);
    }

    @Test
    public void test_copy_constructor()
    {
        final CoordinatedTime source = new CoordinatedTime(1000);
        assertThat(new CoordinatedTime(source).getMilliseconds()).isEqualTo(source.getMilliseconds());
    }

    @Test
    public void test_diff()
    {
        final long t1 = System.currentTimeMillis();
        final long t2 = t1 + 100;

        // The difference is backwards compared to a normal compareTo.
        // If the given CT's time is ahead of the source CT's time, then the returned value is negative.
        assertThat(new CoordinatedTime(t1).millisecondDiff(new CoordinatedTime(t2))).isEqualTo(-100);
    }

    @Test
    public void test_add()
    {
        final long now = System.currentTimeMillis();

        final CoordinatedTime ct = new CoordinatedTime(now);

        assertThat(ct.addMilliseconds(100).getMilliseconds()).isEqualTo(now + 100);
        assertThat(ct.addSeconds(100).getMilliseconds()).isEqualTo(now + TimeUnit.SECONDS.toMillis(100));
        assertThat(ct.addMinutes(100).getMilliseconds()).isEqualTo(now + TimeUnit.MINUTES.toMillis(100));
        assertThat(ct.addHours(100).getMilliseconds()).isEqualTo(now + TimeUnit.HOURS.toMillis(100));
        assertThat(ct.addDays(100).getMilliseconds()).isEqualTo(now + TimeUnit.DAYS.toMillis(100));
    }

    @Test
    public void test_gps_drift()
    {
        long gpsTime;

        // Test GPS time behind system time
        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() - 1000);
        assertThat(CoordinatedTime.currentTimeMillis()).isCloseTo(gpsTime, CLOCK_SLOP);

        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() - 1000);
        assertThat(new CoordinatedTime().getMilliseconds()).isCloseTo(gpsTime, CLOCK_SLOP);

        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() - 1000);
        assertThat(CoordinatedTime.currentDate().getTime()).isCloseTo(gpsTime, CLOCK_SLOP);

        // Test GPS time ahead of system time
        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() + 100);
        assertThat(new CoordinatedTime().getMilliseconds()).isCloseTo(gpsTime, CLOCK_SLOP);

        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() + 100);
        assertThat(CoordinatedTime.currentTimeMillis()).isCloseTo(gpsTime, CLOCK_SLOP);

        CoordinatedTime.setCoordinatedTimeMillis(gpsTime = System.currentTimeMillis() + 100);
        assertThat(CoordinatedTime.currentDate().getTime()).isCloseTo(gpsTime, CLOCK_SLOP);
    }

    @Test
    public void test_to_cot() throws ParseException {
        // Test EPOCH
        assertThat(new CoordinatedTime(0).toCot()).isEqualTo(CotFormatting.EPOCH);
        assertThat(CotFormatting.toCotTime(new CoordinatedTime(0))).isEqualTo(CotFormatting.EPOCH);

        // Test whole seconds
        final String t1 = "2021-02-02T10:20:30Z";
        long millis = _LIBERAL_COT_TIME_FORMAT.parse(t1).getTime();

        String t1WithFractionalSecs = "2021-02-02T10:20:30.000Z";
        assertThat(new CoordinatedTime(millis).toCot()).isEqualTo(t1WithFractionalSecs);
        assertThat(CotFormatting.toCotTime(new CoordinatedTime(millis))).isEqualTo(t1WithFractionalSecs);

        // Test fractional seconds
        final String t2 = "2021-02-02T10:20:30.123Z";
        millis = _COT_TIME_FORMAT.parse(t2).getTime();

        assertThat(new CoordinatedTime(millis).toCot()).isEqualTo(t2);
        assertThat(CotFormatting.toCotTime(new CoordinatedTime(millis))).isEqualTo(t2);
    }

    @Test
    public void test_from_cot() throws ParseException {
        // Test EPOCH
        assertThat(CoordinatedTime.fromCot(CotFormatting.EPOCH).getMilliseconds()).isEqualTo(0);

        // Test whole seconds
        final String t1 = "2021-02-02T10:20:30Z";
        long millis = _LIBERAL_COT_TIME_FORMAT.parse(t1).getTime();

        String t1WithFractionalSecs = "2021-02-02T10:20:30.000Z";
        assertThat(CoordinatedTime.fromCot(t1).getMilliseconds()).isEqualTo(millis);
        assertThat(new CoordinatedTime(t1).getMilliseconds()).isEqualTo(millis);

        assertThat(CoordinatedTime.fromCot(t1).toCot()).isEqualTo(t1WithFractionalSecs);
        assertThat(new CoordinatedTime(t1).toCot()).isEqualTo(t1WithFractionalSecs);

        // Test fractional seconds
        final String t2 = "2021-02-02T10:20:30.123Z";
        millis = _COT_TIME_FORMAT.parse(t2).getTime();

        assertThat(CoordinatedTime.fromCot(t2).getMilliseconds()).isEqualTo(millis);
        assertThat(new CoordinatedTime(t2).getMilliseconds()).isEqualTo(millis);

        assertThat(CoordinatedTime.fromCot(t2).toCot()).isEqualTo(t2);
        assertThat(new CoordinatedTime(t2).toCot()).isEqualTo(t2);
    }

    @Test(expected = ParseException.class)
    public void test_from_invalid_cot_throws() throws ParseException
    {
        CoordinatedTime.fromCot("invalid date");
    }

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
        CoordinatedTime.fromCot(s);
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
