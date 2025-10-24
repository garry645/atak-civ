package gov.tak.api.cot.event;

import static gov.tak.api.cot.CotFormatting.appendAttribute;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotFormatting;
import gov.tak.api.engine.map.coords.GeoPoint;

import java.io.IOException;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.util.AttributeSet;

/**
 * Represents a Cursor on Target event
 * <p>
 * Originally, the Cursor on Target specification was managed by Mitre, at this URL http://cot.mitre.org.  That site
 * is no longer functional, although mitre does maintain some of the publications.
 *
 * @see <a href="https://www.mitre.org/publications/technical-papers/cursorontarget-message-router-users-guide">Cursor on Target</a>
 * @since 6.0.0
 */
public class CotEvent
{
    /**
     * Default <i>version</i> attribute value (2.0)
     */
    public static final String VERSION_2_0 = "2.0";

    public static final String ACCESS = "access";
    public static final String CAVEAT = "caveat";
    public static final String RELEASABLE_TO = "releasableTo";
    public static final String HOW = "how";
    public static final String OPEX = "opex";
    public static final String QOS = "qos";
    public static final String STALE = "stale";
    public static final String START = "start";
    public static final String TIME = "time";
    public static final String TYPE = "type";
    public static final String UID = "uid";
    public static final String VERSION = "version";

    final static CotContentHandler cotHandler = new CotContentHandler();

    private final AttributeSet attributeSet = new AttributeSet();

    // required
    private String uid;
    private String type;
    private String version = VERSION_2_0;
    private IGeoPoint point = GeoPoint.ZERO_POINT; // for backwards compatibility bug #1029
    private CoordinatedTime time;
    private CoordinatedTime start;
    private CoordinatedTime stale;
    private String how;

    // optional
    private CotDetail detail;
    private String opex;
    private String qos;
    private String access;
    private String caveat;
    private String releasableTo;

    /**
     * Create a default state event. The default state is missing the required attributes 'uid',
     * 'type', 'time', 'start', and 'stale'; The required attributes must be set before the event is
     * valid (isValid()). The required inner point tag is defaulted to 0, 0 ({@link GeoPoint#ZERO_POINT}) and no
     * detail tag exists.
     */
    public CotEvent()
    {
    }

    /**
     * Copy constructor.
     */
    public CotEvent(final String uid, final String type, final String version, final IGeoPoint point,
                    final CoordinatedTime time, final CoordinatedTime start, final CoordinatedTime stale,
                    final String how, final CotDetail detail, final String opex, final String qos, final String access)
    {
        setUID(uid);
        setType(type);
        setVersion(version);
        setHow(how);
        this.point = new GeoPoint(point);
        this.time = time;
        this.start = start;
        this.stale = stale;
        this.qos = qos;
        this.opex = opex;
        this.access = access;

        if (detail != null) this.detail = new CotDetail(detail);
    }

    /**
     * Copy constructor. That is not crazy long
     */
    public CotEvent(final CotEvent event)
    {
        this(event.uid, event.type, event.version, event.point, event.time, event.start, event.stale,
                event.how, event.detail, event.opex, event.qos, event.access);

        caveat = event.caveat;
        releasableTo = event.releasableTo;
    }

    /**
     * Determine if the event is valid
     *
     * @return the validity of the event.
     */
    public boolean isValid()
    {
        return uid != null && !uid.trim().isEmpty() &&
                type != null && !type.trim().isEmpty() &&
                time != null &&
                start != null &&
                stale != null &&
                how != null && !how.trim().isEmpty() &&
                point != null && com.atakmap.coremap.maps.coords.GeoPoint.isValid(
                        point.getLatitude(), point.getLongitude());
    }

    /**
     * @return a String indicating the version number
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @return this event 'type' attribute
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return this event UID
     */
    public String getUID()
    {
        return uid;
    }

    /**
     * @return this event's point
     */
    public IGeoPoint getPoint()
    {
        return point;
    }

    /**
     * @return this event's root detail
     */
    public CotDetail getDetail()
    {
        return detail;
    }

    /**
     * @return The AttributeSet for this event
     */
    public AttributeSet getAttributeSet()
    {
        return attributeSet;
    }

    /**
     * Find a detail element
     * Convenience method for {@link CotDetail#getFirstChildByName(int, String)}
     *
     * @param startIndex Child index to begin searching
     * @param name       Detail name
     * @return CoT detail or {@code null} if not found
     */
    public CotDetail findDetail(int startIndex, String name)
    {
        return detail == null
                ? null
                : detail.getFirstChildByName(startIndex, name);
    }

    public CotDetail findDetail(String name)
    {
        return findDetail(0, name);
    }

    /**
     * @return the time as a coordinated time.
     */
    public CoordinatedTime getTime()
    {
        return time;
    }

    /**
     * @return the event start time as a coordinated time.
     */
    public CoordinatedTime getStart()
    {
        return start;
    }

    /**
     * @return the stale time as a coordinated time.
     */
    public CoordinatedTime getStale()
    {
        return stale;
    }

    /**
     * @return the HOW
     */
    public String getHow()
    {
        return how;
    }

    /**
     * @return the OPEX
     */
    public String getOpex()
    {
        return opex;
    }

    /**
     * @return the QoS
     */
    public String getQos()
    {
        return qos;
    }

    /**
     * @return the Access flag
     */
    public String getAccess()
    {
        return access;
    }

    /**
     * @return the Caveat
     */
    public String getCaveat()
    {
        return caveat;
    }

    /**
     * @return the ReleasableTo
     */
    public String getReleasableTo()
    {
        return releasableTo;
    }

    /**
     * Set the version attribute. The initial (default) value is "2.0".
     *
     * @param version the version if it is other than 2.0
     */
    public void setVersion(String version)
    {
        if (version != null)
            version = version.trim();

        if (version == null || version.equals("")) {
            throw new IllegalArgumentException("version may not be nothing");
        }
        this.version = version;
    }

    /**
     * Set the CoT type (e.g. a-f-G).
     *
     * @param type the type of the CoT event
     */
    public void setType(String type)
    {
        if (type != null)
            type = type.trim();

        if (type == null || type.equals("")) {
            throw new IllegalArgumentException("type may not be nothing");
        }
        this.type = type;
    }

    /**
     * Set the unique identifier for the object the event describes
     *
     * @param uid the unique identifier.   Should be opaque and not used for interpretation.
     */
    public void setUID(String uid)
    {
        if (uid == null || uid.trim().equals("")) {
            throw new IllegalArgumentException("uid may not be nothing");
        }
        this.uid = uid;
    }

    /**
     * Set the point tag details
     *
     * @param point the point
     */
    public void setPoint(@NonNull IGeoPoint point)
    {
        this.point = point;
    }

    /**
     * Set the detail tag. This must be named "detail" or be {@code null}.
     *
     * @param detail the detail tag
     * @throws IllegalArgumentException if the CotDetail element name isn't "detail"
     */
    public void setDetail(CotDetail detail)
    {
        final String elementName = detail != null ? detail.getElementName() : null;

        if (detail != null && elementName != null && !elementName.equals(CotDetail.DETAIL))
        {
            throw new IllegalArgumentException("detail tag must be named 'detail' (got '" + elementName + "'");
        }
        this.detail = detail;
    }

    /**
     * Set the time this event was generated
     *
     * @param time the time based on coordinated time.
     */
    public void setTime(@NonNull final CoordinatedTime time)
    {
        this.time = time;
    }

    /**
     * Set the time this event starts scope
     *
     * @param start the start time of the event.
     */
    public void setStart(@NonNull final CoordinatedTime start)
    {
        this.start = start;
    }

    /**
     * Set the time this event leaves from scope
     *
     * @param stale the stale time of the event
     */
    public void setStale(@NonNull final CoordinatedTime stale)
    {
        this.stale = stale;
    }

    /**
     * Set the 'how' attribute of the event (e.g. m-g)
     *
     * @param how the how for the event.
     */
    public void setHow(String how)
    {
        if (how != null)
            how = how.trim();

        if (how == null || how.equals("")) {
            // we used to be less permissive and throw a IllegalStateException if the
            // how field was incorrect - now we should just flag it as machine-generated-garbage
            how = "m-g-g";
        }
        this.how = how;
    }

    /**
     * @param opex the 'opex' attribute of the event
     */
    public void setOpex(String opex)
    {
        this.opex = opex;
    }

    /**
     * @param qos the 'qos' (quality of service) attribute of the event
     */
    public void setQos(String qos)
    {
        this.qos = qos;
    }

    /**
     * @param access the 'access' attribute of the event
     */
    public void setAccess(String access)
    {
        this.access = access;
    }

    /**
     * Set the 'caveat' attribute of the event. Note: This is new as of MIL-STD-6090 and is optional
     *
     * @param caveat the `caveat` attribute of the event { CUI, NOFORN, REL TO }
     */
    public void setCaveat(String caveat)
    {
        this.caveat = caveat;
    }

    /**
     * Set the 'releasableTo' attribute of the event. Note: This is new as of MIL-STD-6090 and is optional
     *
     * @param releasableTo the `releasableTo` attribute of the event. Is a comma delineated list of tricodes.
     */
    public void setReleasableTo(String releasableTo)
    {
        this.releasableTo = releasableTo;
    }

    /**
     * Retrieve the callsign for this event.
     *
     * @return The callsign of this event or {@code null} if it doesn't exist
     */
    public String getCallsign()
    {
        String callsign = null;

        if (detail != null)
        {
            final CotDetail contact = detail.getFirstChildByName(DetailConstants.CONTACT);
            if (contact != null)
            {
                callsign = contact.getAttribute(DetailConstants.ATTR_CALLSIGN);
            }
        }
        return callsign;
    }

    /**
     * Given a string buffer, produce a well formed CoT message.
     *
     * @param b the string buffer.
     */
    public void buildXml(final StringBuffer b)
    {
        try
        {
            buildXmlImpl(b);
        } catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Given a string buffer, produce a well formed CoT message.
     *
     * @param b the string builder.
     */
    public void buildXml(final StringBuilder b)
    {
        try
        {
            buildXmlImpl(b);
        } catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Appends the xml representation to the given appendable.
     */
    public void buildXml(Appendable b) throws IOException
    {
        buildXmlImpl(b);
    }

    private void buildXmlImpl(Appendable b) throws IOException
    {
        b.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event");
        appendAttribute(VERSION, version, b);
        appendAttribute(UID, uid, b);
        appendAttribute(TYPE, type, b);
        appendAttribute(TIME, time, b);
        appendAttribute(START, start, b);
        appendAttribute(STALE, stale, b);
        appendAttribute(HOW, how, b);
        appendAttribute(OPEX, opex, b);
        appendAttribute(QOS, qos, b);
        appendAttribute(ACCESS, access, b);

        // as of MIL-STD-6090 caveat and releasableTo are new but optional
        appendAttribute(CAVEAT, caveat, b);
        appendAttribute(RELEASABLE_TO, releasableTo, b);

        b.append(">");

        if (point != null) b.append(CotFormatting.toCotPointXml(point));
        if (detail != null) detail.buildXml(b);

        b.append("</event>");
    }

    /**
     * Parse an event from an XML string
     *
     * @param xml CoT XML to parse
     * @return a CoT Event that can either be valid or invalid.
     */
    @NonNull
    public static CotEvent parse(final String xml)
    {
        return cotHandler.parseXML(xml);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        buildXml(sb);
        return sb.toString();
    }

    /**
     * Escapes the characters in a String using XML entities.
     *
     * @param innerText Text to process
     * @return new escaped String, or {@code null} if the input is {@code null}
     */
    public static String escapeXmlText(final String innerText) {

        if (innerText == null) {
            return "";
        }

        final int len = innerText.length();

        boolean found = false;
        for (int i = 0; i < len && !found; ++i) {
            final char ch = innerText.charAt(i);
            switch (ch) {
                case '&':
                case '<':
                case '>':
                case '"':
                case '\'':
                case '\n':
                    found = true;
                default:
            }

        }
        if (!found)
            return innerText;

        final StringBuilder sb = new StringBuilder((int) (len * 1.5));
        for (int i = 0; i < len; ++i) {
            final char ch = innerText.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                case '\n':
                    sb.append("&#10;");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
