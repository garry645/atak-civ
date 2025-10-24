package gov.tak.api.cot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;

/**
 * Validate {@link CotFormatting} functionality.
 *
 * @since 6.0.0
 */
public class CotFormattingTest
{
    private static final String EPOCH_TIME = "1970-01-01T00:00:00.000Z";
    private static final String FIXED_TIME = "2021-04-01T02:03:04.567Z";
    private static final long FIXED_TIME_MILLIS;

    static {
        try {
            FIXED_TIME_MILLIS = new CoordinatedTime.SimpleDateFormatThread(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US,
                    TimeZone.getTimeZone("UTC")).parse(FIXED_TIME).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_to_cot_time()
    {
        assertThat(CotFormatting.toCotTime(FIXED_TIME_MILLIS)).isEqualTo(FIXED_TIME);
        assertThat(CotFormatting.toCotTime(new CoordinatedTime(FIXED_TIME_MILLIS))).isEqualTo(FIXED_TIME);
        assertThat(CotFormatting.toCotTime(0)).isEqualTo(CotFormatting.EPOCH);
    }

    @Test
    public void test_cot_time_to_millis() throws ParseException {
        assertThat(CotFormatting.cotTimeToMillis(FIXED_TIME)).isEqualTo(FIXED_TIME_MILLIS);
        assertThat(CotFormatting.cotTimeToMillis(EPOCH_TIME)).isEqualTo(0);
    }

    @Test
    public void test_cot_time_to_coordinated_time() throws ParseException {
        assertThat(CotFormatting.cotTimeToCoordinatedTime(FIXED_TIME).getMilliseconds())
                .isEqualTo(new CoordinatedTime(FIXED_TIME_MILLIS).getMilliseconds());
    }

    @Test
    public void test_to_cot_point_xml()
    {
        assertThat(CotFormatting.toCotPointXml(GeoPoint.UNKNOWN_POINT))
                .isEqualTo("<point lat=\"NaN\" lon=\"NaN\" hae=\"9999999.0\" ce=\"9999999.0\" le=\"9999999.0\"/>");

        assertThat(CotFormatting.toCotPointXml(GeoPoint.ZERO_POINT))
                .isEqualTo("<point lat=\"0.0\" lon=\"0.0\" hae=\"9999999.0\" ce=\"9999999.0\" le=\"9999999.0\"/>");

        assertThat(CotFormatting.toCotPointXml(new GeoPoint(1, 2, 3)))
                .isEqualTo("<point lat=\"1.0\" lon=\"2.0\" hae=\"3.0\" ce=\"9999999.0\" le=\"9999999.0\"/>");

        assertThat(CotFormatting.toCotPointXml(new GeoPoint(1, 2, 3, GeoPoint.AltitudeReference.HAE, 5.222, 6.333)))
                .isEqualTo("<point lat=\"1.0\" lon=\"2.0\" hae=\"3.0\" ce=\"5.2\" le=\"6.3\"/>");
    }

    @Test
    public void test_cot_point_xml_to_geopoint() throws SAXException
    {
        final AttributesImpl attrs = new AttributesImpl();

        attrs.addAttribute("", "", "lat", "", "1.0");
        attrs.addAttribute("", "", "lon", "", "2.0");

        IGeoPoint iGeoPoint = CotFormatting.cotPointXmlToGeoPoint(attrs);
        com.atakmap.coremap.maps.coords.GeoPoint geoPoint = new com.atakmap.coremap.maps.coords.GeoPoint(iGeoPoint);

        assertThat(geoPoint.isValid()).isTrue();
        assertThat(geoPoint.isAltitudeValid()).isFalse();
        assertThat(geoPoint.getLatitude()).isEqualTo(1.0);
        assertThat(geoPoint.getLongitude()).isEqualTo(2.0);
        assertThat(geoPoint.getAltitude()).isNaN();
        assertThat(iGeoPoint.getAltitudeReference()).isEqualTo(IGeoPoint.AltitudeReference.HAE);
        assertThat(geoPoint.getCE()).isNaN();
        assertThat(geoPoint.getLE()).isNaN();

        attrs.addAttribute("", "", "hae", "", "3.0");
        iGeoPoint = CotFormatting.cotPointXmlToGeoPoint(attrs);
        geoPoint = new com.atakmap.coremap.maps.coords.GeoPoint(iGeoPoint);
        assertThat(geoPoint.isValid()).isTrue();
        assertThat(geoPoint.isAltitudeValid()).isTrue();
        assertThat(geoPoint.getLatitude()).isEqualTo(1.0);
        assertThat(geoPoint.getLongitude()).isEqualTo(2.0);
        assertThat(geoPoint.getAltitude()).isEqualTo(3.0);
        assertThat(iGeoPoint.getAltitudeReference()).isEqualTo(GeoPoint.AltitudeReference.HAE);
        assertThat(geoPoint.getCE()).isNaN();
        assertThat(geoPoint.getLE()).isNaN();

        attrs.addAttribute("", "", "ce", "", "4.0");
        attrs.addAttribute("", "", "le", "", "5.0");
        iGeoPoint = CotFormatting.cotPointXmlToGeoPoint(attrs);
        geoPoint = new com.atakmap.coremap.maps.coords.GeoPoint(iGeoPoint);
        assertThat(geoPoint.isValid()).isTrue();
        assertThat(geoPoint.isAltitudeValid()).isTrue();
        assertThat(geoPoint.getLatitude()).isEqualTo(1.0);
        assertThat(geoPoint.getLongitude()).isEqualTo(2.0);
        assertThat(geoPoint.getAltitude()).isEqualTo(3.0);
        assertThat(iGeoPoint.getAltitudeReference()).isEqualTo(GeoPoint.AltitudeReference.HAE);
        assertThat(geoPoint.getCE()).isEqualTo(4.0);
        assertThat(geoPoint.getLE()).isEqualTo(5.0);
    }

    @Test
    public void test_append_attribute() throws IOException
    {
        final StringBuilder sb = new StringBuilder();

        CotFormatting.appendAttribute("null", (String) null, sb);
        assertThat(sb.length()).isEqualTo(0); // null value quietly ignored

        CotFormatting.appendAttribute("null", (CoordinatedTime) null, sb);
        assertThat(sb.length()).isEqualTo(0); // null value quietly ignored

        CotFormatting.appendAttribute("foo", "bar", sb);
        assertThat(sb.toString()).isEqualTo(" foo='bar'");

        CotFormatting.appendAttribute("baz", "blah", sb);
        assertThat(sb.toString()).isEqualTo(" foo='bar' baz='blah'");

        sb.setLength(0);
        CotFormatting.appendAttribute("time", new CoordinatedTime(FIXED_TIME_MILLIS), sb);

        assertThat(sb.toString()).isEqualTo(" time='" + FIXED_TIME + "'");
    }

    @Test
    public void test_escape_xml_text()
    {
        // Quick validation since escapeXmlText is just delegating to Apache StringEscapeUtils
        assertThat(CotFormatting.escapeXmlText("normal text")).isEqualTo("normal text");
        assertThat(CotFormatting.escapeXmlText("this < that")).isEqualTo("this &lt; that");
        assertThat(CotFormatting.escapeXmlText("<elem>")).isEqualTo("&lt;elem&gt;");
    }

    @Test
    public void test_cot_point_to_geopoint_missing_attrs_throws()
    {
        assertThatExceptionOfType(SAXException.class)
                .isThrownBy(() -> CotFormatting.cotPointXmlToGeoPoint(new AttributesImpl()));
    }
}
