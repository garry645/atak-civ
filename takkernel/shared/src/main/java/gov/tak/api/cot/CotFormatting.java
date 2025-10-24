package gov.tak.api.cot;

import static gov.tak.api.cot.CotConstants.UNKNOWN_VALUE;
import static gov.tak.api.cot.CotParseUtils.getDouble;
import static gov.tak.api.cot.CotParseUtils.getDoubleOrThrow;

import com.atakmap.coremap.log.Log;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.engine.map.coords.GeoPoint;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.engine.map.coords.IGeoPoint;

/**
 * Static utility methods for formatting and parsing standard CoT elements like date/times and geographic points.
 * <p>
 * CoT Date/times are formatted according to ISO 8601.  See {@link DateTimeFormatter#ISO_INSTANT}.
 *
 * @since 6.0.0
 */
public class CotFormatting
{
    private static final String TAG = "CotFormatting";

    /**
     * String form of the EPOCH, in CoT standard ISO 8601 format.
     */
    public static final String EPOCH = toCotTime(0);

    // Format for creating the CoT point XML.
    private static final String COT_POINT_FMT = "<point lat=\"%s\" lon=\"%s\" hae=\"%s\" ce=\"%.1f\" le=\"%.1f\"/>";

    /**
     * Produce a valid Cursor on Target formatted date string given the milliseconds since the epoch.
     * This is an ISO 8601 formatted date/time.
     *
     * @param epochMillis the milliseconds since the epoch to format
     * @return the formatted string
     * @see DateTimeFormatter#ISO_INSTANT
     */
    @NonNull
    public static String toCotTime(long epochMillis)
    {
        return CoordinatedTime.toCot(new CoordinatedTime(epochMillis));
    }

    /**
     * Produce a valid Cursor on Target formatted date string given a {@link CoordinatedTime} instance.
     * This is an ISO 8601 formatted date/time.
     *
     * @param coordinatedTime from which to obtain the milliseconds since the epoch to format
     * @return the formatted string
     * @see #toCotTime(long)
     */
    @NonNull
    public static String toCotTime(@NonNull CoordinatedTime coordinatedTime)
    {
        return toCotTime(coordinatedTime.getMilliseconds());
    }

    /**
     * Parse a CoT time String to milliseconds since the epoch.
     * Expected format is ISO 8601 form: {@code 2009-09-15T21:00:00.00Z}.  Fractional seconds are optional.
     *
     * @param cotTime A date/time from a CoT message
     * @return the millis since the epoch derived from the given CoT formatted date string
     * @throws ParseException If the given string can not be parsed according to the CoT format (ISO 8601).
     */
    public static long cotTimeToMillis(@NonNull String cotTime) throws ParseException
    {
        try
        {
            return CoordinatedTime.fromCot(cotTime).getMilliseconds();
        } catch (ParseException e)
        {
            Log.e(TAG, "Exception occurred parsing: " + cotTime, e);
            throw e;
        }
    }

    /**
     * Parse a CoT time String to a {@link CoordinatedTime}.
     * Expected format is ISO 8601 form: {@code 2009-09-15T21:00:00.00Z}.  Fractional seconds are optional.
     *
     * @param cotTime A date/time from a CoT message
     * @return the coordinated time derived from the given CoT formatted date string
     * @throws DateTimeParseException If the given string can not be parsed according to the CoT format (ISO 8601).
     */
    @NonNull
    public static CoordinatedTime cotTimeToCoordinatedTime(@NonNull String cotTime) throws ParseException
    {
        return new CoordinatedTime(cotTimeToMillis(cotTime));
    }

    /**
     * Construct a CoT {@code point} XML element from the given GeoPoint.
     *
     * @param geoPoint GeoPoint to encode as CoT XML
     * @return CoT XML {@code point} element
     */
    public static String toCotPointXml(@NonNull IGeoPoint geoPoint)
    {
        final double hae = geoPoint.getAltitude();
        final double ce = geoPoint.getCE();
        final double le = geoPoint.getLE();

        return String.format(Locale.US, COT_POINT_FMT, geoPoint.getLatitude(), geoPoint.getLongitude(),
                Double.isNaN(hae) ? UNKNOWN_VALUE : hae,
                Double.isNaN(ce) ? UNKNOWN_VALUE : ce,
                Double.isNaN(le) ? UNKNOWN_VALUE : le);
    }

    /**
     * @return a {@link IGeoPoint} constructed from the attributes of a CoT {@code point} element.
     */
    @NonNull
    public static IGeoPoint cotPointXmlToGeoPoint(@NonNull Attributes attrs) throws SAXException
    {
        final double lat = getDoubleOrThrow(attrs, "lat", "point: illegal or missing lat");
        final double lon = getDoubleOrThrow(attrs, "lon", "point: illegal or missing lon");
        final double hae = getDouble(attrs, "hae", GeoPoint.UNKNOWN);
        final double le = getDouble(attrs, "le", GeoPoint.UNKNOWN);
        final double ce = getDouble(attrs, "ce", GeoPoint.UNKNOWN);

        return new GeoPoint(lat, lon, getUsableValue(hae), GeoPoint.AltitudeReference.HAE,
                getUsableValue(ce), getUsableValue(le));
    }

    /**
     * Append a String value as an attribute to the given appendable, in the form {@code " name='value'"}.
     * If the given value is {@code null}, then nothing is appended.
     *
     * @param name  Name of attribute
     * @param value Value of attribute
     * @param a     Appendable to append to
     */
    public static void appendAttribute(@NonNull String name, @Nullable String value, @NonNull Appendable a) throws IOException
    {
        if (value != null)
        {
            a.append(' ').append(name).append("='").append(escapeXmlText(value)).append("'");
        }
    }

    /**
     * Append a {@link CoordinatedTime} as an attribute to the given appendable, in the form {@code " name='value'"},
     * where the string value is produced using {@link #toCotTime(CoordinatedTime)}.
     * If the given time is {@code null}, then nothing is appended.
     *
     * @param name Name of attribute
     * @param time Time from which to produce the attribute value
     * @param a    Appendable to append to
     * @see #toCotTime(CoordinatedTime)
     */
    public static void appendAttribute(@NonNull String name, @Nullable CoordinatedTime time, @NonNull Appendable a) throws IOException
    {
        if (time != null)
        {
            appendAttribute(name, toCotTime(time), a);
        }
    }

    /**
     * Escapes the characters in a String using XML entities.
     *
     * @param text Text to process
     * @return new escaped String, or {@code null} if the input is {@code null}
     */
    public static String escapeXmlText(@Nullable final String text)
    {
        return CotEvent.escapeXmlText(text);
    }

    /**
     * Given a value, test to see if it is invalid.  If so, return {@link GeoPoint#UNKNOWN}, otherwise return the
     * given value.
     *
     * @param value Value to test
     * @return the given value if it is valid, or {@link GeoPoint#UNKNOWN} if not
     * @see #isInvalidValue(double)
     */
    private static double getUsableValue(double value)
    {
        return isInvalidValue(value) ? GeoPoint.UNKNOWN : value;
    }

    /**
     * Test if a given value is "invalid", meaning it is a NaN or it is equal to {@link CotConstants#UNKNOWN_VALUE}.
     *
     * @param value Value to test
     * @return true if the value is invalid
     */
    private static boolean isInvalidValue(double value)
    {
        return Double.isNaN(value) || value == UNKNOWN_VALUE;
    }

    /**
     * Formats the provided CoT event as a "pretty printed" XML string, with opening elements on a new line with
     * appropriate indentation.
     * @param event The event to format
     * @return The CoT event as a formatted XML string.
     * @throws IllegalStateException if the event could not be formatted
     * @since 3.2
     */
    public static String formatEvent(@NonNull CotEvent event) throws IllegalStateException
    {
        try (StringWriter stringWriter = new StringWriter())
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(event.toString())));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            Source source = new DOMSource(document);
            transformer.transform(source, new StreamResult(stringWriter));

            return stringWriter.toString();
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e)
        {
            throw new IllegalStateException("Error formatting CoT event", e);
        }
    }

    private CotFormatting()
    {
        /* no instances */
    }
}
