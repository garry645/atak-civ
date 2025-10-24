
package com.atakmap.android.video.manager;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.AccessUtils;
import gov.tak.platform.lang.Parsers;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.coremap.xml.XMLUtils;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.util.zip.IoUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.video.cot.ConnectionEntryDetail;

/**
 * Handles serialization and deserialization of connection entry XML
 */
public class VideoXMLHandler {

    private static final String TAG = "VideoXMLHandler";
    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>";

    private final DocumentBuilder _docBuilder;

    public VideoXMLHandler() {
        DocumentBuilder builder = null;
        try {
            DocumentBuilderFactory factory = XMLUtils
                    .getDocumenBuilderFactory();

            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create document builder", e);
        }
        _docBuilder = builder;
    }

    /**
     * Parse a connection entry or list of connection entries
     * Works for both cases
     * Synchronized to prevent reading and writing at the same time
     *
     * @param file Connection entry file
     * @return List of connection entries
     */
    public synchronized List<ConnectionEntry> parse(File file) {
        final List<ConnectionEntry> ret = new ArrayList<>();

        if (!IOProviderFactory.isFile(file)) {
            Log.d(TAG, "file does not exist: " + file);
            return ret;
        }

        try (FileInputStream is = IOProviderFactory.getInputStream(file)) {
            ret.addAll(parseImpl(is));
            for (ConnectionEntry e : ret)
                e.setLocalFile(file);
        } catch (Exception e) {
            Log.e(TAG, "failed to parse connection entry file: " + file, e);
        }
        return ret;
    }

    public synchronized List<ConnectionEntry> parse(String xml) {
        List<ConnectionEntry> ret = new ArrayList<>();
        if (_docBuilder == null)
            return ret;
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(
                    xml.getBytes(FileSystemUtils.UTF8_CHARSET));
            return parseImpl(is);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse connection entry XML", e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception ignore) {
            }
        }
        return ret;
    }

    private List<ConnectionEntry> parseImpl(InputStream is)
            throws IOException, SAXException {
        List<ConnectionEntry> ret = new ArrayList<>();
        if (_docBuilder == null)
            return ret;
        List<gov.tak.api.video.ConnectionEntry> entries = parse(
                _docBuilder.parse(is));
        if (!entries.isEmpty()) {
            ((ArrayList) ret).ensureCapacity(entries.size());
            for (gov.tak.api.video.ConnectionEntry entry : entries)
                ret.add(new ConnectionEntry(entry));
        }
        return ret;
    }

    private List<gov.tak.api.video.ConnectionEntry> parse(Document dom) {
        List<gov.tak.api.video.ConnectionEntry> ret = new ArrayList<>();
        if (dom == null)
            return ret;
        NodeList children = dom.getChildNodes();
        if (children == null)
            return ret;
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element))
                continue;
            List<gov.tak.api.video.ConnectionEntry> entries = parse(
                    (Element) n);
            if (entries != null)
                ret.addAll(entries);
        }
        return ret;
    }

    /** @deprecated use {@link #write(gov.tak.api.video.ConnectionEntry, File)} */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public File write(ConnectionEntry entry, File file) {
        return write(entry.get(), file);
    }

    /**
     * Serialize a single connection entry to an XML file
     * Synchronized to prevent reading and writing at the same time
     *
     * @param entry Connection entry
     * @param file XML file
     * @return File or null if failed
     */
    public synchronized File write(gov.tak.api.video.ConnectionEntry entry,
            File file) {
        FileOutputStream fos = null;
        try {
            String xml = serialize(entry, null);
            if (FileSystemUtils.isEmpty(xml))
                return null;
            if (IOProviderFactory.exists(file))
                FileSystemUtils.delete(file);
            fos = IOProviderFactory.getOutputStream(file);
            FileSystemUtils.write(fos, xml);
            fos = null;

            // Save any passphrase to the auth database
            if (entry
                    .getProtocol() == gov.tak.api.video.ConnectionEntry.Protocol.SRT
                    && entry.getPassphrase() != null
                    && !entry.getPassphrase().isEmpty())
                AtakAuthenticationDatabase.saveCredentials(
                        AtakAuthenticationCredentials.TYPE_videoPassword,
                        entry.getUID(), "", entry.getPassphrase(), false);

            return file;
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize connection entry: " + entry, e);
        } finally {
            IoUtils.close(fos);
        }
        return null;
    }

    private static List<gov.tak.api.video.ConnectionEntry> parse(Element el) {
        if (el == null)
            return null;

        if (el.getTagName().equalsIgnoreCase("videoConnections")) {
            List<gov.tak.api.video.ConnectionEntry> ret = new ArrayList<>();
            NodeList multiple = el.getChildNodes();
            for (int i = 0; i < multiple.getLength(); i++) {
                Node n = multiple.item(i);
                if (!(n instanceof Element))
                    continue;
                gov.tak.api.video.ConnectionEntry entry = parseFeed(
                        (Element) n);
                if (entry != null)
                    ret.add(entry);
            }
            return ret;
        } else {
            gov.tak.api.video.ConnectionEntry entry = parseFeed(el);
            if (entry != null)
                return Collections.singletonList(entry);
        }
        return null;
    }

    private static gov.tak.api.video.ConnectionEntry parseFeed(Element feed) {
        if (!feed.getTagName().equalsIgnoreCase("feed"))
            return null;
        gov.tak.api.video.ConnectionEntry.Protocol proto = null;
        String alias = null;
        String uid = null;
        String address = null;
        int port = -1;
        int roverPort = -1;
        boolean ignoreKLV = false;
        String preferredMacAddress = null;
        String preferredInterfaceAddress = null;
        String path = null;
        int buffer = -1;
        int timeout = 5000;
        int rtspReliable = 0;
        NodeList children = feed.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element))
                continue;
            Element el = (Element) n;
            String k = el.getTagName();
            String v = el.getTextContent();

            // XXX - break break break break break break... Yuck
            // Switch blocks could've been nice if the breaks were implicit
            // after one or more lines of code per case
            switch (k) {
                case "protocol":
                    proto = gov.tak.api.video.ConnectionEntry.Protocol
                            .fromString(v);
                    break;
                case "alias":
                    alias = v;
                    break;
                case "uid":
                    uid = v;
                    break;
                case "address":
                    address = v;
                    break;
                case "port":
                    port = parseInt(v);
                    break;
                case "roverPort":
                    roverPort = parseInt(v);
                    break;
                case "ignoreEmbeddedKLV":
                    ignoreKLV = Boolean.parseBoolean(v);
                    break;
                case "preferredMacAddress":
                    preferredMacAddress = v;
                    break;
                case "preferredInterfaceAddress":
                    preferredInterfaceAddress = v;
                    break;
                case "path":
                    path = v;
                    break;
                case "buffer":
                    buffer = parseInt(v);
                    break;
                case "timeout":
                    timeout = parseInt(v);
                    break;
                case "rtspReliable":
                    rtspReliable = parseInt(v);
                    break;
            }
        }
        if (proto == null || FileSystemUtils.isEmpty(alias)
                || FileSystemUtils.isEmpty(uid))
            return null;
        gov.tak.api.video.ConnectionEntry.Source source = proto == gov.tak.api.video.ConnectionEntry.Protocol.FILE
                || proto == gov.tak.api.video.ConnectionEntry.Protocol.DIRECTORY
                        ? gov.tak.api.video.ConnectionEntry.Source.LOCAL_STORAGE
                        : gov.tak.api.video.ConnectionEntry.Source.EXTERNAL;
        String pass = "";
        if (proto == gov.tak.api.video.ConnectionEntry.Protocol.SRT) {
            AtakAuthenticationCredentials creds = AtakAuthenticationDatabase
                    .getCredentials(
                            AtakAuthenticationCredentials.TYPE_videoPassword,
                            uid);
            if (creds != null)
                pass = creds.password;
        }
        gov.tak.api.video.ConnectionEntry entry = new gov.tak.api.video.ConnectionEntry(
                alias, address,
                port, roverPort, path, proto, timeout,
                buffer, rtspReliable, pass, source);
        entry.setPreferredInterfaceAddress(preferredInterfaceAddress);
        entry.setUID(uid);
        entry.setIgnoreEmbeddedKLV(ignoreKLV);
        return entry;
    }

    private static int parseInt(String v) {
        return Parsers.parseInt(v, -1);
    }

    /** @deprecated use {@link #serialize(gov.tak.api.video.ConnectionEntry, StringBuilder)} */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public static String serialize(ConnectionEntry e, StringBuilder sb) {
        return serialize(e.get(), sb);
    }

    /**
     * Serialize a connection entry to XML
     *
     * @param e Connection entry
     * @param sb String builder (null if this is a singular entry)
     * @return Feed XML
     */
    public static String serialize(gov.tak.api.video.ConnectionEntry e,
            StringBuilder sb) {
        boolean single = false;
        if (sb == null) {
            sb = new StringBuilder();
            sb.append(XML_HEADER).append("\n");
            single = true;
        }
        sb.append("<feed>\n");
        add(sb, "protocol", e.getProtocol().toString());
        add(sb, "alias", e.getAlias());
        add(sb, "uid", e.getUID());
        add(sb, "address", e.getAddress());
        add(sb, "port", e.getPort());
        add(sb, "roverPort", e.getRoverPort());
        add(sb, "ignoreEmbeddedKLV", e.getIgnoreEmbeddedKLV());
        add(sb, "preferredMacAddress", "");
        add(sb, "preferredInterfaceAddress", e.getPreferredInterfaceAddress());
        add(sb, "path", e.getPath());
        add(sb, "buffer", e.getBufferTime());
        add(sb, "timeout", e.getNetworkTimeout());
        add(sb, "rtspReliable", e.getRtspReliable());
        sb.append("</feed>\n");
        return single ? sb.toString() : null;
    }

    public static String serialize(List<ConnectionEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append(XML_HEADER).append("\n");
        sb.append("<videoConnections>\n");
        for (ConnectionEntry entry : entries)
            serialize(entry, sb);
        sb.append("</videoConnections>\n");
        return sb.toString();
    }

    private static void add(StringBuilder sb, String k, Object v) {
        sb.append("<").append(k);
        if (v != null) {
            String str = String.valueOf(v);
            if (!FileSystemUtils.isEmpty(str)) {
                sb.append(">");
                if (v instanceof String)
                    str = CotEvent.escapeXmlText(str);
                sb.append(str);
                sb.append("</").append(k).append(">\n");
                return;
            }
        }
        sb.append("/>\n");
    }

    public static CotEvent toCotEvent(ConnectionEntry ce) {
        CotEvent cotEvent = new CotEvent();
        cotEvent.setUID(ce.getUID());
        cotEvent.setType("b-i-v");
        cotEvent.setVersion("2.0");
        cotEvent.setHow("m-g");

        AccessUtils.setAccessDefault(cotEvent);

        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addHours(1));

        CotDetail detail = new CotDetail("detail");

        CotDetail callsign = new CotDetail("contact");
        callsign.setAttribute("callsign", ce.getAlias());

        detail.addChild(callsign);

        CotDetail link = new CotDetail("link");
        link.setAttribute("uid", ce.getUID());
        link.setAttribute("production_time", new CoordinatedTime().toString());
        link.setAttribute("relationship", "p-p");
        link.setAttribute("parent_callsign", MapView.getMapView()
                .getDeviceCallsign());

        detail.addChild(link);

        CotDetail vid = new CotDetail("__video");
        vid.addChild(toCotDetail(ce));
        detail.addChild(vid);
        cotEvent.setDetail(detail);
        return cotEvent;
    }

    public static CotDetail toCotDetail(ConnectionEntry ce) {
        return gov.tak.platform.marshal.MarshalManager.marshal(
                ConnectionEntryDetail.toCotDetail(ce.get()),
                gov.tak.api.cot.event.CotDetail.class,
                CotDetail.class);
    }
}
