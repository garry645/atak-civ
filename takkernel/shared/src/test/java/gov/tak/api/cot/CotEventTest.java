package gov.tak.api.cot;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.Test;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.engine.map.coords.GeoPoint;

/**
 * Validate {@link CotEvent} functionality.
 *
 * @since 6.0.0
 */
public class CotEventTest
{
    private static final String UID = "the-uid";
    private static final String TYPE = "a-f";
    private static final String HOW = CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT;
    private static final String OPEX = "opex";
    private static final String QOS = "qos";
    private static final String ACCESS = "access";
    private static final CotDetail DETAIL = new CotDetail();
    private static final GeoPoint POINT = new GeoPoint(1, 2);
    private static final CoordinatedTime TIME = new CoordinatedTime();
    private static final CoordinatedTime START = TIME.addMinutes(2);
    private static final CoordinatedTime STALE = START.addMinutes(2);
    private static final String CHILD_1 = "child1";
    private static final String CHILD_2 = "child2";

    private static final Offset<Double> EPSILON = Offset.offset(1e-6); // Comparison tolerance

    private static final String LAT_STR = "41.11903";
    private static final double LATITUDE = Double.parseDouble(LAT_STR);

    private static final String LON_STR = "-75.42835";
    private static final double LONGITUDE = Double.parseDouble(LON_STR);

    private static final String EVENT_TIME = "2021-04-03T14:40:00.677Z";

    public static final String VALID_POINT =
            "<point lat=\"" + LAT_STR + "\" lon=\"" + LON_STR + "\" ce=\"9999999\" le=\"9999999\" hae=\"9999999\"/>\n";

    private static final String INVALID_POINT = "<point ce=\"9999999\" le=\"9999999\" hae=\"9999999\"/>\n";

    public static final String START_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String START_EVENT = "<event version=\"2.0\" uid=\"" + UID + "\" type=\"" + TYPE + "\" " +
            " time=\"" + EVENT_TIME + "\" start=\"" + EVENT_TIME + "\" stale=\"" + EVENT_TIME + "\"" +
            " how=\"" + HOW + "\">\n";
    public static final String END_EVENT = "</event>";

    private static final String VALID_NO_DETAIL = START_XML + START_EVENT + VALID_POINT + END_EVENT;

    private static final String VALID_WITH_DETAIL = START_XML + START_EVENT +
            VALID_POINT +
            "<detail>" +
            "  <contact callsign=\"TAKX\"/>" +
            "  <color value=\"-16711681\"/>" +
            "  <precisionlocation geopointsrc=\"???\" altsrc=\"???\"/>" +
            "  <_flow-tags_ marti1=\"2014-10-28T22:40:15.341Z\"/>\n" +
            "</detail>" +
            END_EVENT;

    private static final String VALID_WITH_DETAIL_INNER_TEXT = START_XML + START_EVENT +
            VALID_POINT +
            "<detail>" +
            "    <link uid=\"21c57f66-d135-4180-a771-f2ede16629dc.Style\" type=\"b-x-KmlStyle\" relation=\"p-c\">\n" +
            "        <Style>\n" +
            "            <LineStyle>\n" +
            "                <color>ffffff00</color>\n" +
            "                <width>4.0</width>\n" +
            "            </LineStyle>\n" +
            "            <PolyStyle>\n" +
            "                <color>96ffffff</color>\n" +
            "            </PolyStyle>\n" +
            "        </Style>\n" +
            "    </link>\n" +
            "</detail>" +
            END_EVENT;

    private static final String INVALID_NOT_XML = "not xml";
    private static final String INVALID_NOT_AN_EVENT = START_XML + "<not-an-event/>";
    private static final String INVALID_INVALID_POINT = START_XML + START_EVENT + INVALID_POINT + END_EVENT;

    private static final String INVALID_NO_CLOSE_TAG =
            VALID_WITH_DETAIL.substring(0, VALID_WITH_DETAIL.length() - END_EVENT.length());

    @Test
    public void test_default_constructor()
    {
        final CotEvent event = new CotEvent();

        assertThat(event.isValid()).isFalse();
        assertThat(event.getVersion()).isEqualTo(CotEvent.VERSION_2_0);
        assertThat(event.getPoint()).isEqualTo(GeoPoint.ZERO_POINT);

        assertThat(event.getUID()).isNull();
        assertThat(event.getType()).isNull();
        assertThat(event.getDetail()).isNull();
        assertThat(event.getTime()).isNull();
        assertThat(event.getStart()).isNull();
        assertThat(event.getStale()).isNull();
        assertThat(event.getHow()).isNull();
        assertThat(event.getOpex()).isNull();
        assertThat(event.getQos()).isNull();
        assertThat(event.getAccess()).isNull();
        assertThat(event.getReleasableTo()).isNull();
        assertThat(event.getCaveat()).isNull();
    }

    @Test
    public void test_full_constructor()
    {
        final CotEvent event = new CotEvent(UID, TYPE, CotEvent.VERSION_2_0, POINT, TIME, START, STALE,
                HOW, DETAIL, OPEX, QOS, ACCESS);

        assertThat(event.isValid()).isTrue();

        assertThat(event.getUID()).isEqualTo(UID);
        assertThat(event.getType()).isEqualTo(TYPE);
        assertThat(event.getVersion()).isEqualTo(CotEvent.VERSION_2_0);
        assertThat(event.getPoint()).isEqualTo(POINT);
        assertThat(event.getDetail()).isNotNull();
        assertThat(event.getDetail()).isNotSameAs(DETAIL); // should be a copy
        assertThat(event.getTime()).isEqualTo(TIME);
        assertThat(event.getStart()).isEqualTo(START);
        assertThat(event.getStale()).isEqualTo(STALE);
        assertThat(event.getHow()).isEqualTo(HOW);
        assertThat(event.getOpex()).isEqualTo(OPEX);
        assertThat(event.getQos()).isEqualTo(QOS);
        assertThat(event.getAccess()).isEqualTo(ACCESS);
    }

    @Test
    public void test_copy_constructor()
    {
        final CotEvent source = new CotEvent(UID, TYPE, CotEvent.VERSION_2_0, POINT, TIME, START, STALE,
                HOW, DETAIL, OPEX, QOS, ACCESS);
        final CotEvent event = new CotEvent(source);

        assertThat(event.isValid()).isTrue();

        assertThat(event.getUID()).isEqualTo(source.getUID());
        assertThat(event.getType()).isEqualTo(source.getType());
        assertThat(event.getVersion()).isEqualTo(source.getVersion());
        assertThat(event.getPoint()).isEqualTo(source.getPoint());
        assertThat(event.getDetail()).isNotNull();
        assertThat(event.getDetail()).isNotSameAs(source.getDetail()); // should be a copy
        assertThat(event.getTime()).isEqualTo(source.getTime());
        assertThat(event.getStart()).isEqualTo(source.getStart());
        assertThat(event.getStale()).isEqualTo(source.getStale());
        assertThat(event.getHow()).isEqualTo(source.getHow());
        assertThat(event.getOpex()).isEqualTo(source.getOpex());
        assertThat(event.getQos()).isEqualTo(source.getQos());
        assertThat(event.getAccess()).isEqualTo(source.getAccess());
        assertThat(event.getReleasableTo()).isEqualTo(source.getReleasableTo());
        assertThat(event.getCaveat()).isEqualTo(source.getCaveat());
    }

    @Test
    public void test_find_detail()
    {
        final CotEvent event = new CotEvent();

        assertThat(event.findDetail(CHILD_1)).isNull();
        assertThat(event.findDetail(CHILD_2)).isNull();

        final CotDetail detail = new CotDetail();
        final CotDetail child1 = new CotDetail(CHILD_1);
        final CotDetail child2 = new CotDetail(CHILD_2);

        detail.addChild(child1);
        detail.addChild(child2);

        event.setDetail(detail);

        assertThat(event.findDetail(CHILD_1)).isEqualTo(child1);
        assertThat(event.findDetail(0, CHILD_1)).isEqualTo(child1);
        assertThat(event.findDetail(CHILD_2)).isEqualTo(child2);
        assertThat(event.findDetail(0, CHILD_2)).isEqualTo(child2);
    }

    @Test
    public void test_invalid_point_makes_event_invalid()
    {
        final CotEvent event = new CotEvent(UID, TYPE, CotEvent.VERSION_2_0, POINT, TIME, START, STALE,
                HOW, DETAIL, OPEX, QOS, ACCESS);

        assertThat(event.isValid()).isTrue();

        event.setPoint(GeoPoint.UNKNOWN_POINT);
        assertThat(event.isValid()).isFalse();
    }

    @Test
    public void test_out_of_range_point_makes_event_invalid()
    {
        final CotEvent event = new CotEvent(UID, TYPE, CotEvent.VERSION_2_0, POINT, TIME, START, STALE,
                HOW, DETAIL, OPEX, QOS, ACCESS);

        assertThat(event.isValid()).isTrue();

        event.setPoint(new GeoPoint(-900, -900));
        assertThat(event.isValid()).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_detail_throws()
    {
        new CotEvent().setDetail(new CotDetail("not_detail"));
    }

    @Test
    public void test_parse_invalid_does_not_throw()
    {
        assertThat(CotEvent.parse(INVALID_NOT_XML).isValid()).isFalse();
        assertThat(CotEvent.parse(INVALID_NOT_AN_EVENT).isValid()).isFalse();

        // Test that an invalid point element still results in a valid event, since the point is allowed to be null
        checkValidEvent(CotEvent.parse(INVALID_INVALID_POINT), false);

        // Test an event that is missing the final </event> tag, which should still result in a VALID event
        // because all the other required content is present.
        checkValidEvent(CotEvent.parse(INVALID_NO_CLOSE_TAG), true);
    }

    @Test
    public void test_parse_valid_no_detail()
    {
        checkValidEvent(CotEvent.parse(VALID_NO_DETAIL), true);
    }

    @Test
    public void test_parse_valid_with_detail()
    {
        final CotEvent event = CotEvent.parse(VALID_WITH_DETAIL);
        checkValidEvent(event, true);

        final CotDetail detail = event.getDetail();
        assertThat(detail).isNotNull();
        assertThat(detail.childCount()).isEqualTo(4);

        final CotDetail child = detail.getChild(0);
        assertThat(child.getElementName()).isEqualTo(DetailConstants.CONTACT);
        assertThat(child.getAttribute(DetailConstants.ATTR_CALLSIGN)).isEqualTo("TAKX");

        assertThat(detail.getFirstChildByName(DetailConstants.CONTACT)).isSameAs(child);
    }

    @Test
    public void test_detail_with_inner_text()
    {
        final CotEvent event = CotEvent.parse(VALID_WITH_DETAIL_INNER_TEXT);
        checkValidEvent(event, true);

        final CotDetail detail = event.getDetail();
        assertThat(detail).isNotNull();
        assertThat(detail.childCount()).isEqualTo(1);

        final CotDetail link = detail.getChild(0);
        assertThat(link.getElementName()).isEqualTo("link");

        final CotDetail style = link.getFirstChildByName("Style");
        final CotDetail lineStyle = style.getFirstChildByName("LineStyle");
        final CotDetail polyStyle = style.getFirstChildByName("PolyStyle");

        final String colorText = lineStyle.getFirstChildByName("color").getInnerText();
        assertThat(colorText).isEqualTo("ffffff00");

        final String widthText = lineStyle.getFirstChildByName("width").getInnerText();
        assertThat(widthText).isEqualTo("4.0");

        final String polyColorText = polyStyle.getFirstChildByName("color").getInnerText();
        assertThat(polyColorText).isEqualTo("96ffffff");
    }

    /**
     * Check the expected values on a valid event parsed from XML.
     */
    private void checkValidEvent(CotEvent event, boolean checkPoint)
    {
        assertThat(event.isValid()).isTrue();
        assertThat(event.getUID()).isEqualTo(UID);
        assertThat(event.getType()).isEqualTo(TYPE);
        assertThat(event.getHow()).isEqualTo(HOW);

        assertThat(CotFormatting.toCotTime(event.getTime())).isEqualTo(EVENT_TIME);

        if (checkPoint)
        {
            final com.atakmap.coremap.maps.coords.GeoPoint geoPoint = new com.atakmap.coremap.maps.coords.GeoPoint(event.getPoint());
            assertThat(geoPoint.isValid()).isTrue();
            assertThat(geoPoint.getLatitude()).isCloseTo(LATITUDE, EPSILON);
            assertThat(geoPoint.getLongitude()).isCloseTo(LONGITUDE, EPSILON);
            assertThat(geoPoint.isAltitudeValid()).isFalse();
        }
    }
}
