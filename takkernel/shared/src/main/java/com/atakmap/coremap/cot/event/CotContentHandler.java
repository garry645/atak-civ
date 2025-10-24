
package com.atakmap.coremap.cot.event;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.util.ResourcePool;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;

final class CotContentHandler {

    public static final String TAG = "CotContentHandler";

    private static XmlPullParserFactory parserFactory;

    final static class ParseContext {
        private final ArrayList<CotDetail> detailStack = new ArrayList<>();
        private boolean finishedDetail;
        private String innerTextBuilder;
        private CotEvent editor;
        private XmlPullParser parser;
    }

    final ResourcePool<ParseContext> _parsePool = new ResourcePool<>(16);

    CotEvent parseXML(final String xml) {
        ParseContext context = _parsePool.get();

        CotEvent editor = new CotEvent();
        try {
            if (context == null)
                context = new ParseContext();
            context.detailStack.clear();
            context.finishedDetail = false;
            context.innerTextBuilder = null;

            context.editor = editor;
            if (parserFactory == null) {
                parserFactory = XmlPullParserFactory.newInstance();
                try {
                    parserFactory
                            .setFeature(
                                    "http://xml.org/sax/features/external-parameter-entities",
                                    false);
                } catch (Exception ignored) {
                }

                try {
                    parserFactory
                            .setFeature(
                                    "http://xml.org/sax/features/external-general-entities",
                                    false);
                } catch (Exception ignored) {
                }
            }
            if (context.parser == null)
                context.parser = parserFactory.newPullParser();

            context.parser.setInput(new StringReader(xml));
            do {
                switch (context.parser.next()) {
                    case XmlPullParser.START_TAG:
                        startElement(context, context.parser.getName());
                        break;
                    case XmlPullParser.END_TAG:
                        endElement(context);
                        break;
                    case XmlPullParser.TEXT:
                        context.innerTextBuilder = context.parser.getText();
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        break;
                }
            } while (context.parser
                    .getEventType() != XmlPullParser.END_DOCUMENT);
        } catch (Throwable e) {
            Log.v(TAG, "Bad message encountered: " + xml);
            Log.e(TAG, "error: ", e);
        } finally {
            if (context != null)
                _parsePool.put(context);
        }
        return editor;
    }

    void endElement(final ParseContext context) {
        final int detailDepth = context.detailStack.size();
        if (detailDepth > 0) {
            // pop detail
            CotDetail detail = context.detailStack.remove(detailDepth - 1);
            // if it has no children and the innerTextBuilder is valid, then
            // set the inner text
            if (detail.getChildren().isEmpty()
                    && context.innerTextBuilder != null
                    && context.innerTextBuilder.length() > 0) {
                detail.setInnerText(context.innerTextBuilder.trim());
                context.innerTextBuilder = null;
            }
            // if the stack had a single element, it was the `detail` tag
            if (detailDepth == 1) {
                context.finishedDetail = true;
            }
        }
    }

    void startElement(ParseContext context, final String localName)
            throws SAXException {

        try {
            if (!context.detailStack.isEmpty()) {
                // inside of detail tag just get DOM'ed out
                _pushDetail(context, localName, context.parser);
            } else if (localName.equals("event")) {
                final int nattr = context.parser.getAttributeCount();
                for (int i = 0; i < nattr; i++) {
                    final String attr = context.parser.getAttributeName(i);
                    switch (attr) {
                        case "type":
                            context.editor
                                    .setType(_stringOrThrow(context.parser, i,
                                            "event: missing type"));
                            break;
                        case "version":
                            context.editor.setVersion(_stringOrFallback(
                                    context.parser, i, "2.0"));
                            break;
                        case "uid":
                            context.editor.setUID(
                                    _stringOrThrow(context.parser, i,
                                            "event: missing uid"));
                            break;
                        case "time":
                            context.editor
                                    .setTime(_timeOrDefault(context.parser, i,
                                            "event: illegal or missing time"));
                            break;
                        case "start":
                            context.editor
                                    .setStart(_timeOrDefault(context.parser, i,
                                            "event: illegal or missing start"));
                            break;
                        case "stale":
                            context.editor
                                    .setStale(_timeOrDefault(context.parser, i,
                                            "event: illegal or missing stale"));
                            break;
                        case "how":
                            context.editor.setHow(
                                    _stringOrFallback(context.parser, i, ""));
                            break;
                        case "opex":
                            context.editor.setOpex(
                                    _stringOrFallback(context.parser, i, null));
                            break;
                        case "qos":
                            context.editor.setQos(
                                    _stringOrFallback(context.parser, i, null));
                            break;
                        case "access":
                            context.editor.setAccess(
                                    _stringOrFallback(context.parser, i, null));
                            break;
                        case "caveat":
                            context.editor.setCaveat(
                                    _stringOrFallback(context.parser, i, null));
                            break;
                        case "releasableTo":
                            context.editor.setReleasableTo(
                                    _stringOrFallback(context.parser, i, null));
                            break;
                        default:
                            break;
                    }
                }
                // these might not be clear in the case that a recycled event was passed in
                context.editor.setPoint(CotPoint.ZERO);
                context.editor.setDetail(null);
            } else if (localName.equals("point")) {
                // if (_parsedPoint || _eventEditor == null) {
                // throw new CotIllegalException("illegal point tag");
                // }
                double lat = Double.NaN;
                double lon = Double.NaN;
                double hae = Double.NaN;
                double le = Double.NaN;
                double ce = Double.NaN;
                final int nattr = context.parser.getAttributeCount();
                for (int i = 0; i < nattr; i++) {
                    final String attr = context.parser.getAttributeName(i);
                    switch (attr) {
                        case "lat":
                            lat = _doubleOrThrow(context.parser, i,
                                    "point: illegal or missing lat");
                            break;
                        case "lon":
                            lon = _doubleOrThrow(context.parser, i,
                                    "point: illegal or missing lon");
                            break;
                        case "hae":
                            hae = _doubleOrFallback(context.parser, i,
                                    CotPoint.UNKNOWN);
                            break;
                        case "le":
                            le = _doubleOrFallback(context.parser, i,
                                    CotPoint.UNKNOWN);
                            break;
                        case "ce":
                            ce = _doubleOrFallback(context.parser, i,
                                    CotPoint.UNKNOWN);
                            break;
                        default:
                            break;
                    }
                }

                // some systems are starting to spit out Double.NaN incorrectly
                // for those values that are unknown correctly parse them

                if (Double.isNaN(hae))
                    hae = CotPoint.UNKNOWN;
                if (Double.isNaN(le))
                    le = CotPoint.UNKNOWN;
                if (Double.isNaN(ce))
                    ce = CotPoint.UNKNOWN;

                context.editor.setPoint(new CotPoint(lat, lon, hae, ce, le));

            } else if (localName.equals("detail")
                    && !context.finishedDetail) {

                CotDetail detail = _pushDetail(context, "detail",
                        context.parser);
                context.editor.setDetail(detail);
            }
        } catch (CotIllegalException e) {
            throw new SAXException(e.toString());
        }

    }

    private CotDetail _pushDetail(ParseContext context, final String name,
            final XmlPullParser attrs) {
        CotDetail detail = new CotDetail();

        // set name and attributes
        detail.setElementName(name);
        final int nattr = attrs.getAttributeCount();
        for (int i = 0; i < nattr; ++i) {
            String attrName = attrs.getAttributeName(i);
            String attrValue = attrs.getAttributeValue(i);
            detail.setAttribute(attrName, attrValue);
        }

        // add it to (any) parent
        final int detailDepth = context.detailStack.size();
        if (detailDepth > 0) {
            // peek
            CotDetail parentDetail = context.detailStack.get(detailDepth - 1);
            parentDetail.addChild(detail);
        }

        // push it on the stack
        context.detailStack.add(detail);

        return detail;
    }

    private static CoordinatedTime _timeOrDefault(final XmlPullParser attrs,
            final int name, final String msg) {
        try {
            return CoordinatedTime.fromCot(attrs.getAttributeValue(name));
        } catch (Exception ex) {
            Log.e(TAG, "_timeOrDefault" + msg);
            return new CoordinatedTime();
        }
    }

    private static String _stringOrThrow(final XmlPullParser attrs,
            final int name,
            final String msg)
            throws CotIllegalException {
        String value = attrs.getAttributeValue(name);
        if (value == null) {
            throw new CotIllegalException(msg);
        }
        return value;
    }

    private static String _stringOrFallback(final XmlPullParser attrs,
            final int name,
            final String fallback) {
        String value = attrs.getAttributeValue(name);
        if (value == null) {
            value = fallback;
        }
        return value;
    }

    private static double _doubleOrThrow(final XmlPullParser attrs,
            final int name,
            final String msg)
            throws CotIllegalException {
        try {
            return Double.parseDouble(attrs.getAttributeValue(name));
        } catch (Exception ex) {
            throw new CotIllegalException(msg);
        }
    }

    private static double _doubleOrFallback(final XmlPullParser attrs,
            final int name,
            final double fallback) {
        try {
            return Double.parseDouble(attrs.getAttributeValue(name));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
