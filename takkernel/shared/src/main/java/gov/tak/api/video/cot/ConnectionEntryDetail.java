package gov.tak.api.video.cot;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.video.ConnectionEntry;
import gov.tak.platform.lang.Parsers;

/**
 * Provides conversion services to/from {@link ConnectionEntry}
 */
public final class ConnectionEntryDetail {

    private ConnectionEntryDetail() {}

    public static CotDetail toCotDetail(ConnectionEntry ce) {
        // add the xml as a detail
        CotDetail aliasDetail = new CotDetail(VideoDetailHandler.CONNECTION_ENTRY_DETAIL);

        // attach each part of the connection entry as it's own string
        // it's much easier to turn back into a ConnectionEntry Object than
        // trying to decode the string val of the Connection Entry
        aliasDetail.setAttribute("address", ce.getAddress());
        aliasDetail.setAttribute("uid", ce.getUID());
        aliasDetail.setAttribute("alias", ce.getAlias());
        aliasDetail.setAttribute("port", String.valueOf(ce.getPort()));
        aliasDetail
                .setAttribute("roverPort", String.valueOf(ce.getRoverPort()));
        aliasDetail.setAttribute("rtspReliable",
                String.valueOf(ce.getRtspReliable()));
        aliasDetail.setAttribute("ignoreEmbeddedKLV",
                String.valueOf(ce.getIgnoreEmbeddedKLV()));
        aliasDetail.setAttribute("path", ce.getPath());
        aliasDetail.setAttribute("protocol", ce.getProtocol().toString());
        aliasDetail.setAttribute("networkTimeout",
                String.valueOf(ce.getNetworkTimeout()));
        aliasDetail.setAttribute("bufferTime",
                String.valueOf(ce.getBufferTime()));

        return aliasDetail;
    }

    public static ConnectionEntry fromCotDetail(CotDetail ce) {
        if(ce == null)
            return null;

        String uid = ce.getAttribute("uid");
        ConnectionEntry entry = new ConnectionEntry(
                ce.getAttribute("alias"),
                ce.getAttribute("address"),
                Parsers.parseInt(ce.getAttribute("port"), -1),
                Parsers.parseInt(ce.getAttribute("roverPort"), -1),
                ce.getAttribute("path"),
                ConnectionEntry.Protocol
                        .fromString(ce.getAttribute("protocol")),
                Parsers.parseInt(ce.getAttribute("networkTimeout"), 5000),
                Parsers.parseInt(ce.getAttribute("bufferTime"), -1),
                Parsers.parseInt(ce.getAttribute("rtspReliable"), 0),
                "",
                ConnectionEntry.Source.EXTERNAL);
        entry.setIgnoreEmbeddedKLV(Boolean.parseBoolean(
                ce.getAttribute("ignoreEmbeddedKLV")));
        // Video requires UID
        if (!FileSystemUtils.isEmpty(uid))
            entry.setUID(uid);
        return entry;
    }
}
