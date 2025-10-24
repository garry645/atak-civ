package gov.tak.api.cot.event;

import static gov.tak.api.cot.CotParseUtils.getString;
import static gov.tak.api.cot.CotParseUtils.getStringOrThrow;
import static gov.tak.api.cot.CotParseUtils.timeOrDefault;

import com.atakmap.coremap.log.Log;

import gov.tak.api.cot.CotFormatting;
import gov.tak.api.engine.map.coords.GeoPoint;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import gov.tak.api.annotation.NonNull;

/**
 * SAX content handler for parsing CoT message XML.
 *
 * @since 6.0.0
 */
public class CotContentHandler implements ContentHandler
{
    private final static String TAG = "CotContentHandler";
    public static final String SAX_FEATURES = "http://xml.org/sax/features/";
    public static final String FEATURES_EXTERNAL_GENERAL_ENTITIES = SAX_FEATURES + "external-general-entities";
    public static final String FEATURES_EXTERNAL_PARAMETER_ENTITIES = SAX_FEATURES + "external-parameter-entities";

    private final Deque<CotDetail> detailStack = new ArrayDeque<>();
    private volatile boolean finishedDetail;
    private final StringBuilder innerTextBuilder = new StringBuilder();
    private volatile CotEvent event;
    private XMLReader reader;

    /**
     * Parse the given XML string into a {@link CotEvent}.
     * <p>
     * Makes use of internal buffers so must be synchronized to avoid concurrent use.
     *
     * @param xml XML to parse
     * @return a new CotEvent that can either be valid or invalid.
     */
    @NonNull
    synchronized CotEvent parseXML(@NonNull final String xml)
    {
        detailStack.clear();
        finishedDetail = false;
        innerTextBuilder.setLength(0);

        event = new CotEvent();
        try
        {
            if (reader == null)
            {
                reader = getSaxReader();
                reader.setContentHandler(this);
            }

            reader.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e)
        {
            Log.w("Invalid CoT message: {}", e.getMessage(), e);
            Log.d("message: {}", xml);
        }

        return event;
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
    {
        // we get all the bs whitespace here too
        boolean anyNonWhitespace = false;

        for (int i = start, end = start + length; i < end; i++)
        {
            if (!Character.isWhitespace(ch[i]))
            {
                anyNonWhitespace = true;
                break;
            }
        }

        // If the stack is non-empty, it is the value of a detail
        if (anyNonWhitespace && !detailStack.isEmpty())
        {
            innerTextBuilder.append(ch, start, length);
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attrs)
            throws SAXException
    {
        if (qName.equals("event") && detailStack.isEmpty())
        {
            event.setType(getStringOrThrow(attrs, CotEvent.TYPE, "event: missing type"));
            event.setVersion(getString(attrs, CotEvent.VERSION, CotEvent.VERSION_2_0));
            event.setUID(getStringOrThrow(attrs, CotEvent.UID, "event: missing uid"));
            event.setTime(timeOrDefault(attrs, CotEvent.TIME, "event: illegal or missing time"));
            event.setStart(timeOrDefault(attrs, CotEvent.START, "event: illegal or missing start"));
            event.setStale(timeOrDefault(attrs, CotEvent.STALE, "event: illegal or missing stale"));
            event.setHow(getString(attrs, CotEvent.HOW, ""));
            event.setOpex(getString(attrs, CotEvent.OPEX, null));
            event.setQos(getString(attrs, CotEvent.QOS, null));
            event.setAccess(getString(attrs, CotEvent.ACCESS, null));

            // these might not be clear in the case that a recycled event was passed in
            event.setPoint(GeoPoint.ZERO_POINT);
            event.setDetail(null);
        } else if (qName.equals("point") && detailStack.isEmpty())
        {
            event.setPoint(CotFormatting.cotPointXmlToGeoPoint(attrs));
        } else if (qName.equals(CotDetail.DETAIL) && detailStack.isEmpty() && !finishedDetail)
        {
            event.setDetail(pushDetail(CotDetail.DETAIL, attrs));
        } else if (!detailStack.isEmpty())
        {
            // inside of detail tag just get DOM'ed out
            pushDetail(qName, attrs);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName)
    {
        if (!detailStack.isEmpty())
        {
            final CotDetail detail = detailStack.pop();

            if (innerTextBuilder.length() > 0)
            {
                detail.setInnerText(innerTextBuilder.toString());
                innerTextBuilder.setLength(0);
            }
            if (detailStack.isEmpty())
            {
                finishedDetail = true;
            }
        }
    }

    private CotDetail pushDetail(final String name, final Attributes attrs)
    {
        final CotDetail detail = new CotDetail(name);

        // set attributes
        for (int i = 0; i < attrs.getLength(); ++i)
        {
            detail.setAttribute(attrs.getLocalName(i), attrs.getValue(i));
        }

        // add it to (any) parent
        if (!detailStack.isEmpty())
        {
            detailStack.peek().addChild(detail);
        }

        // push it on the stack
        detailStack.push(detail);

        return detail;
    }

    @Override
    public void startPrefixMapping(final String prefix, final String uri)
    {
    }

    @Override
    public void endPrefixMapping(final String prefix)
    {
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int length)
    {
    }

    @Override
    public void processingInstruction(final String target, final String data)
    {
    }

    @Override
    public void setDocumentLocator(final Locator locator)
    {
    }

    @Override
    public void skippedEntity(final String name)
    {
    }

    @Override
    public void startDocument()
    {
    }

    @Override
    public void endDocument()
    {
    }

    /**
     * Create a new SAX {@link XMLReader} properly configured to prevent XEE attacks.
     * <p>
     * Note that the returned reader has been configured to use a special error handler to prevent the default
     * behavior of writing directly to stderr.  Instead, the messages are logged using the logger for this class.
     * This ensures tha the messages are properly recorded in the log.
     *
     * @return new XMLReader
     * @since 3.0
     */
    public static XMLReader getSaxReader() throws ParserConfigurationException, SAXException
    {
        final XMLReader reader = getSaxParser().getXMLReader();
        reader.setErrorHandler(new LoggingSaxErrorHandler());
        return reader;
    }

    /**
     * Return a new {@link SAXParser} properly configured to prevent XEE attacks.
     *
     * @return new SAXParser
     * @since 3.0
     */
    public static SAXParser getSaxParser() throws SAXException, ParserConfigurationException
    {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

        saxParserFactory.setFeature(FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
        saxParserFactory.setFeature(FEATURES_EXTERNAL_GENERAL_ENTITIES, false);

        return saxParserFactory.newSAXParser();
    }

    /**
     * Special error handler to report using our logger instead of the default behavior, which writes
     * to {@link System#err}.
     *
     * @since 3.0
     */
    private static class LoggingSaxErrorHandler implements ErrorHandler
    {
        @Override
        public void warning(SAXParseException exception)
        {
            Log.w(TAG, makeMessage(exception));
        }

        @Override
        public void error(SAXParseException exception)
        {
            Log.e(TAG, makeMessage(exception));
        }

        @Override
        public void fatalError(SAXParseException exception)
        {
            Log.e("[FATAL] {}", makeMessage(exception));
        }

        private String makeMessage(SAXParseException exception)
        {
            return exception.getLineNumber() + ":" + exception.getColumnNumber() + ": " + exception.getMessage();
        }
    }
}
