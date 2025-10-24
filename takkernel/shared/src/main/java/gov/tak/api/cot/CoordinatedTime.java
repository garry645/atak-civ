package gov.tak.api.cot;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.ResourcePool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Instances of this class provide support for a GPS time delta for shifting values from
 * {@link System#currentTimeMillis()} accordingly when creating new CoordinatedTime instances.
 * <p>
 * Methods are provided for parsing/formatting according to the CoT standard ISO 1601 format, but all those
 * operations are forwarded to {@link CotFormatting}.
 *
 * @since 6.0.0
 */
public class CoordinatedTime
{
    /**
     * Allows for a Thread Safe usage of any SimpleDateFormat construct.
     */
    public static class SimpleDateFormatThread extends
            ThreadLocal<SimpleDateFormat>
    {
        final String format;
        final Locale locale;
        final TimeZone tz;

        /**
         * Constructs a thread safe version of the SimpleDateFormatter with the
         * format, locale and timezone specified.
         *
         * @param format @see java.text.SimpleDateFormat for a list of correct formats.
         * @param locale the locale to be used, can be null to use the default locale.
         * @param tz     the time zone to use, can be null to use the default timezone.
         */
        public SimpleDateFormatThread(String format, Locale locale,
                                      TimeZone tz)
        {
            this.format = format;
            this.locale = locale;
            this.tz = tz;
        }

        /**
         * Constructs a thread safe version of the SimpleDateFormatter with the
         * format and locale specified.  This constructor uses the default timezone.
         *
         * @param format @see java.text.SimpleDateFormat for a list of correct formats.
         * @param locale the locale to be used, can be null to use the default locale.
         */
        public SimpleDateFormatThread(String format, Locale locale)
        {
            this(format, locale, null);
        }

        /**
         * Constructs a thread safe version of the SimpleDateFormatter with the
         * format specified.  This constructor uses the default locale and timezone.
         *
         * @param format @see java.text.SimpleDateFormat for a list of correct formats.
         */
        public SimpleDateFormatThread(String format)
        {
            this(format, null, null);
        }

        @Override
        protected SimpleDateFormat initialValue()
        {
            SimpleDateFormat sdf;
            if (locale != null)
                sdf = new SimpleDateFormat(format, locale);
            else
                sdf = new SimpleDateFormat(format);

            if (tz != null)
                sdf.setTimeZone(tz);
            return sdf;
        }

        /**
         * see SimpleDateFormat.format(Date)
         */
        public String format(Date date)
        {
            return this.get().format(date);
        }

        /**
         * see SimpleDateFormat.setTimeZone(TimeZone)
         */
        public void setTimeZone(TimeZone tz)
        {
            this.get().setTimeZone(tz);
        }

        /**
         * see SimpleDateFormat.parse(String)
         */
        public Date parse(String source) throws ParseException
        {
            return this.get().parse(source);
        }
    }

    /**
     * COT Time per ISO 8601
     */
    private final static SimpleDateFormatThread _COT_TIME_FORMAT = new SimpleDateFormatThread(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US, TimeZone.getTimeZone("UTC"));

    /**
     * COT Time per ISO 8601
     * @since 6.0.0
     */
    private final static SimpleDateFormatThread _LIBERAL_COT_TIME_FORMAT =
            new SimpleDateFormatThread("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US,
                    TimeZone.getTimeZone("UTC"));

    private static final String TAG = "CoordinatedTime";

    private static long gpsTimeDelta = 0;

    private final long timestamp;

    /**
     * Set the global offset between the current system time in milliseconds since the epoch and
     * the GPS time milliseconds since the epoch.
     * <p>
     * Setting the gps time to &lt;= 0 indicates no GPS time exists and the coordinated time will be the local system time.
     *
     * @param gpsTime Relative GPS time, this value is subtracted from the current system time
     */
    public static void setCoordinatedTimeMillis(final long gpsTime)
    {
        gpsTimeDelta = gpsTime > 0
                ? System.currentTimeMillis() - gpsTime
                : 0;
    }

    /**
     * Indicate if GPS time has been set.
     *
     * @see #setCoordinatedTimeMillis(long)
     */
    public static boolean isGPSTime()
    {
        return gpsTimeDelta != 0;
    }

    /**
     * If GPS time has been set, provide the offset from the GPS time and the local device time.
     * If GPS has not been set, the offset will be 0.
     * <p>
     * Note: If GPS time is behind the system time, the number will be negative.
     */
    public static long getCoordinatedTimeOffset()
    {
        return gpsTimeDelta;
    }

    /**
     * Obtains the current date:
     * <ol>
     *     <li>based on system time measured as the number of milliseconds since the epoch.</li>
     *     <li>or if a GPS delta has been set, the system time corrected to GPS time</li>
     * </ol>
     * note: GPS time is not used directly, a correction is used instead, due to the poor granularity of GPS time.
     */
    public static Date currentDate()
    {
        return new Date(currentTimeMillis());
    }

    /**
     * Obtains the current time in millis since the EPOCH, adjusted for GPS drift if available.
     *
     * @return the time in milliseconds from EPOCH
     */
    public static long currentTimeMillis()
    {
        return System.currentTimeMillis() - gpsTimeDelta;
    }

    /**
     * For creating new timestamps within the system, this method should be used directly.
     * <p>
     * Create a CoT time that represents the current time:
     * <ol>
     *     <li>based on system time measured as the number of milliseconds since the epoch</li>
     *     <li>or if a GPS delta has been set, the system time corrected to GPS time</li>
     * </ol>
     * note: GPS time is not used directly, a correction is used instead, due to the poor granularity of GPS time.
     */
    public CoordinatedTime()
    {
        this(currentTimeMillis());
    }

    /**
     * Copy Constructor.
     */
    public CoordinatedTime(CoordinatedTime source)
    {
        timestamp = source.timestamp;
    }

    /**
     * Create a CoT time in milliseconds since January 1st 1970 00:00:00 UTC.
     *
     * @param milliseconds offset in milliseconds from Unix Epoch
     */
    public CoordinatedTime(final long milliseconds)
    {
        timestamp = milliseconds;
    }

    /**
     * Construct a CoordinatedTime from the CoT date/time string, using ISO 8601 format: {@code 2009-09-15T21:00:00.00Z}.
     *
     * @param cotTime CoT Time string to parse
     */
    public CoordinatedTime(String cotTime)
    {
        try {
            timestamp = CotFormatting.cotTimeToMillis(cotTime);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The difference, in milliseconds, between this coordinated time and the given one.
     * If the given time is "in the future" compared to this one, then the result will be &gt; 0.
     *
     * @param other The coordinated time to compare
     * @return Difference in milliseconds
     */
    public long millisecondDiff(CoordinatedTime other)
    {
        return timestamp - other.timestamp;
    }

    /**
     * Create a new CoT time that is a positive or negative number of milliseconds from this Cot time.
     *
     * @param milliseconds number of milliseconds to add (may be negative)
     * @return a new CoordinatedTime with the given number of milliseconds added
     */
    public CoordinatedTime addMilliseconds(final long milliseconds)
    {
        return new CoordinatedTime(timestamp + milliseconds);
    }

    /**
     * Create a new CoT time that is a positive or negative number of seconds from this Cot time.
     *
     * @param seconds number of seconds to add (may be negative)
     * @return a new Coordinated time with the given number of seconds added.
     */
    public CoordinatedTime addSeconds(final int seconds)
    {
        return addMilliseconds(TimeUnit.SECONDS.toMillis(seconds));
    }

    /**
     * Create a new CoT time that is a positive or negative number of minutes from this Cot time.
     *
     * @param minutes number of minutes to add (may be negative)
     * @return a new Coordinated time with the given number of minutes added.
     */
    public CoordinatedTime addMinutes(final int minutes)
    {
        return addMilliseconds(TimeUnit.MINUTES.toMillis(minutes));
    }

    /**
     * Create a new CoT time that is a positive or negative number of hours from this Cot time.
     *
     * @param hours number of hours to add (may be negative)
     * @return a new Coordinated time with the given number of hours added.
     */
    public CoordinatedTime addHours(final int hours)
    {
        return addMilliseconds(TimeUnit.HOURS.toMillis(hours));
    }

    /**
     * Create a new CoT time that is a positive or negative number of days from this Cot time.
     *
     * @param days number of days to add (may be negative)
     * @return a new Coordinated time with the given number of days added.
     */
    public CoordinatedTime addDays(final int days)
    {
        return addMilliseconds(TimeUnit.DAYS.toMillis(days));
    }

    /**
     * Get this CoT time in milliseconds since the epoch, January 1st 1970 00:00:00 UTC
     *
     * @return the time in milliseconds from EPOCH
     */
    public long getMilliseconds()
    {
        return timestamp;
    }

    /**
     * @return a valid Cursor on Target formatted date string from this CoordinatedTime object.
     * @see CotFormatting#toCotTime(long)
     */
    public String toCot()
    {
        return formatTime(new Date(timestamp));
    }

    /**
     * Produce a valid Cursor on Target formatted date string from a CoordinatedTime object.
     *
     * @param time the coordinated time to format for use within a cursor on target message.
     * @return the formatted string.
     */
    public static String toCot(final CoordinatedTime time)
    {
        return formatTime(new Date(time.getMilliseconds()));
    }

    private static File directory = null;

    /**
     * Designate for refactoring. Set a reasonable log directory for bad_time, until this is
     * refactored.
     */
    static public void setLoggerDirectory(final File dir)
    {
        directory = dir;
        Log.d(TAG, "publish bad_time messages to: " + directory);
    }

    /**
     * Format a CoT time string from a java Date
     *
     * @param date the date to format
     * @return the formatted data.
     */
    private static String formatTime(final Date date)
    {
        String time = "2009-09-15T21:00:00.00Z"; // A not so random date, done in case the date
        // library is sick - Andrew
        try
        {
            time = _COT_TIME_FORMAT.format(date);
        } catch (IllegalArgumentException e)
        {
            String msg = "Error in formatTime - the input time is probably not formated correctly or is not a valid time"
                    + "\r\n" + date.toString();

            // XXX Designate for refactoring. Should happen higher up?
            Log.e(TAG, msg);
            if (directory != null)
            {
                if (!IOProviderFactory.exists(directory))
                {
                    if (!IOProviderFactory.mkdirs(directory))
                    {
                        Log.w(TAG, "Failed to create: " + directory);
                    }
                }

                try (OutputStream is = IOProviderFactory
                        .getOutputStream(new File(directory,
                                "bad_time.txt"), true);
                     OutputStreamWriter osw = new OutputStreamWriter(
                             is, StandardCharsets.UTF_8);
                     PrintWriter w = new PrintWriter(osw))
                {
                    w.append(msg).append("\r\n");
                    e.printStackTrace(w);
                } catch (FileNotFoundException e1)
                {
                    Log.e(TAG,
                            "File not found while attempting to write bad_time.txt",
                            e1);
                } catch (IOException e2)
                {
                    Log.e(TAG,
                            "Encountered IO issue while attempting to write bad_time.txt",
                            e2);
                }
            }
        }
        return time;
    }

    /**
     * Parse a CoT time String in the form: 2009-09-15T21:00:00.00Z without using simple date
     * parser. Thread-safe
     * <p>
     * Note: This will parse the CoT time string in the format 2009-09-15T21:00:00.00Z and will
     * permissively allow for 2009-09-15T21:00:00.000Z and 2009-09-15T21:00:00.0Z which are not
     * part of the spec.
     *
     * @param date A date from a CoT message.
     * @return the coordinated time derived from the CoT message date.
     * @throws ParseException if there is not a valid CoT time encountered.
     */
    public static CoordinatedTime fromCot(final String date)
            throws ParseException
    {
        ParseContext ctx = ParseContext.pool.get();
        try
        {
            if(ctx == null)
                ctx = new ParseContext();
            final int datelen = date.length();
            date.getChars(0, Math.min(datelen, ctx.datech.length), ctx.datech, 0);
            if (datelen > 19)
            {
                int y = (toDigit(ctx.datech[0])*1000) +
                        (toDigit(ctx.datech[1])*100) +
                        (toDigit(ctx.datech[2])*10) +
                        toDigit(ctx.datech[3]);
                int m = (toDigit(ctx.datech[5])*10) +
                        toDigit(ctx.datech[6]);
                --m;
                int d = (toDigit(ctx.datech[8])*10) +
                        toDigit(ctx.datech[9]);
                int h = (toDigit(ctx.datech[11])*10) +
                        toDigit(ctx.datech[12]);
                int mm = (toDigit(ctx.datech[14])*10) +
                        toDigit(ctx.datech[15]);
                int s = (toDigit(ctx.datech[17])*10) +
                        toDigit(ctx.datech[18]);

                int ms = 0;

                if (datelen > 23)
                {
                    ms = (toDigit(ctx.datech[20])*100) +
                            (toDigit(ctx.datech[21])*10) +
                            toDigit(ctx.datech[22]);
                } else if (datelen > 22)
                {
                    ms = (toDigit(ctx.datech[20])*100) +
                            (toDigit(ctx.datech[21])*10);
                } else if (datelen > 21)
                {
                    ms = (toDigit(ctx.datech[20])*100);
                }

                ctx.CachedCalendar.set(y, m, d, h, mm, s);

                // Log.d(TAG, "original date = " + date );
                // Log.d(TAG, "y = " + y + " m = " + m + " d = " + d + " h = " + h + " mm = " + mm +
                //            " s = " + s + " ms " + ms);

                return new CoordinatedTime(ctx.CachedCalendar.getTime().getTime()
                        + ms);
            }
        } catch (Exception e)
        {
            Log.d(TAG, "exception occurred parsing: " + date, e);
        } finally {
            if(ctx != null)
                ParseContext.pool.put(ctx);
        }

        // fall back to the original implementation.
        return fromCotFallback(date);
    }

    /**
     * Parse a CoT time String. The parser is somewhat lenient in that it accepts a string with or
     * without the millisecond decimal portion.
     *
     * @param formattedTime the formatted time as a string
     * @return the coordinated time
     * @throws ParseException if the formatted time does not comply with the CoT specification.
     */
    private static CoordinatedTime fromCotFallback(final String formattedTime)
            throws ParseException
    {
        Date date;
        try
        {
            date = _COT_TIME_FORMAT.parse(formattedTime);
        } catch (Exception ex)
        {
            date = _LIBERAL_COT_TIME_FORMAT.parse(formattedTime);
        }
        return new CoordinatedTime(date.getTime());
    }


    static int toDigit(char c) {
        if(c < '0' || c > '9') throw new NumberFormatException();
        else return c-'0';
    }

    static class ParseContext
    {
        final static ResourcePool<ParseContext> pool = new ResourcePool<>(16);

        final Calendar CachedCalendar;
        final char[] datech = new char[24];

        ParseContext()
        {
            CachedCalendar = new GregorianCalendar();
            CachedCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
            CachedCalendar.setLenient(false);
            CachedCalendar.clear();
        }
    }

    /**
     * Get the string representation of this CoT time
     *
     * @see #toCot()
     */
    public String toString()
    {
        return toCot();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return timestamp == ((CoordinatedTime) o).timestamp;
    }

    @Override
    public int hashCode()
    {
        return (int) (timestamp ^ (timestamp >>> 32));
    }
}
