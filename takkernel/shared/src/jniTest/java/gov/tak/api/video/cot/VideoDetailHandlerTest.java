package gov.tak.api.video.cot;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.video.ConnectionEntry;
import gov.tak.api.video.IVideoConnectionManager;
import gov.tak.test.KernelTest;

public class VideoDetailHandlerTest extends KernelTest
{
    @Test
    public void detail_roundtrip_no_connection_manager() {
        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = ""; // NOTE: passphrase is not serialized in CoT
        final ConnectionEntry.Source source = ConnectionEntry.Source.EXTERNAL;

        final ConnectionEntry entry = new ConnectionEntry(alias,
                address,
                port,
                roverPort,
                path,
                protocol,
                networkTimeout,
                bufferTime,
                rtspReliable,
                passphrase,
                source);
        entry.setUID(UUID.randomUUID().toString());

        final long time = System.currentTimeMillis();

        CotDetail inRoot = new CotDetail();
        final CotDetail ceDetail = ConnectionEntryDetail.toCotDetail(entry);
        Assert.assertNotNull(ceDetail);

        final CotDetail video = new CotDetail(VideoDetailHandler.DETAIL);
        video.setAttribute("uid", entry.getUID());
        video.setAttribute("url", entry.getAddress());

        video.addChild(ceDetail);
        inRoot.addChild(video);

        CotEvent inEvent = new CotEvent(
                entry.getUID(),
                "b-m-p-s-p-i",
                CotEvent.VERSION_2_0,
                new GeoPoint(45, -135),
                new CoordinatedTime(time),
                new CoordinatedTime(time),
                new CoordinatedTime(time+100000L),
                "m-g",
                video,
                null,
                null,
                null);

        final AttributeSet attrs = new AttributeSet();

        VideoDetailHandler handler = new VideoDetailHandler(null);
        final ICotDetailHandler.ImportResult inResult = handler.toItemMetadata(null, attrs, inEvent, video);
        Assert.assertEquals(ICotDetailHandler.ImportResult.SUCCESS, inResult);

        CotDetail outRoot = new CotDetail();
        CotEvent outEvent = new CotEvent(
                entry.getUID(),
                "b-m-p-s-p-i",
                CotEvent.VERSION_2_0,
                new GeoPoint(45, -135),
                new CoordinatedTime(time),
                new CoordinatedTime(time),
                new CoordinatedTime(time+100000L),
                "m-g",
                outRoot,
                null,
                null,
                null);
        final boolean outResult = handler.toCotDetail(new Object(), attrs, outEvent, outRoot);
        Assert.assertTrue(outResult);

        ConnectionEntry parsedEntry = ConnectionEntryDetail.fromCotDetail(outRoot.getChild(0).getChild(0));
        Assert.assertNotNull(parsedEntry);
        entry.equals(parsedEntry);
        Assert.assertEquals(entry, parsedEntry);
    }

    @Test
    public void detail_roundtrip_with_connection_manager() {
        final IVideoConnectionManager cm = new IVideoConnectionManager() {
            Map<String, ConnectionEntry> entries = new HashMap<>();

            @Override
            public ConnectionEntry getConnectionEntry(String uid) {
                return entries.get(uid);
            }

            @Override
            public List<ConnectionEntry> getConnectionEntries(Set<String> uids, boolean remoteOnly) {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public void addConnectionEntry(ConnectionEntry entry) {
                entries.put(entry.getUID(), entry);
            }

            @Override
            public void addConnectionEntries(List<ConnectionEntry> entries) {
                for(ConnectionEntry e : entries) addConnectionEntry(e);
            }

            @Override
            public void removeConnectionEntry(String uid) {
                entries.remove(uid);
            }

            @Override
            public void removeConnectionEntry(ConnectionEntry entry) {
                removeConnectionEntry(entry.getUID());
            }

            @Override
            public void removeConnectionEntries(Set<String> uids) {
                for(String uid : uids) removeConnectionEntry(uid);
            }

            @Override
            public void addConnectionListener(ConnectionListener l) {}

            @Override
            public void removeConnectionListener(ConnectionListener l) {}

            @Override
            public File getConnectionEntriesDirectory() {
                return new File("/dev/null");
            }
        };

        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = ""; // NOTE: passphrase is not serialized in CoT
        final ConnectionEntry.Source source = ConnectionEntry.Source.EXTERNAL;

        final ConnectionEntry entry = new ConnectionEntry(alias,
                address,
                port,
                roverPort,
                path,
                protocol,
                networkTimeout,
                bufferTime,
                rtspReliable,
                passphrase,
                source);
        entry.setUID(UUID.randomUUID().toString());

        final long time = System.currentTimeMillis();

        CotDetail inRoot = new CotDetail();
        final CotDetail ceDetail = ConnectionEntryDetail.toCotDetail(entry);
        Assert.assertNotNull(ceDetail);

        final CotDetail video = new CotDetail(VideoDetailHandler.DETAIL);
        video.setAttribute("uid", entry.getUID());
        video.setAttribute("url", entry.getAddress());

        video.addChild(ceDetail);
        inRoot.addChild(video);

        CotEvent inEvent = new CotEvent(
                entry.getUID(),
                "b-m-p-s-p-i",
                CotEvent.VERSION_2_0,
                new GeoPoint(45, -135),
                new CoordinatedTime(time),
                new CoordinatedTime(time),
                new CoordinatedTime(time+100000L),
                "m-g",
                video,
                null,
                null,
                null);

        final AttributeSet attrs = new AttributeSet();

        VideoDetailHandler handler = new VideoDetailHandler(cm);
        final ICotDetailHandler.ImportResult inResult = handler.toItemMetadata(null, attrs, inEvent, video);
        Assert.assertEquals(ICotDetailHandler.ImportResult.SUCCESS, inResult);

        CotDetail outRoot = new CotDetail();
        CotEvent outEvent = new CotEvent(
                entry.getUID(),
                "b-m-p-s-p-i",
                CotEvent.VERSION_2_0,
                new GeoPoint(45, -135),
                new CoordinatedTime(time),
                new CoordinatedTime(time),
                new CoordinatedTime(time+100000L),
                "m-g",
                outRoot,
                null,
                null,
                null);
        attrs.removeAttribute(VideoDetailHandler.CONNECTION_ENTRY_ATTR);
        final boolean outResult = handler.toCotDetail(new Object(), attrs, outEvent, outRoot);
        Assert.assertTrue(outResult);

        ConnectionEntry parsedEntry = ConnectionEntryDetail.fromCotDetail(outRoot.getChild(0).getChild(0));
        Assert.assertNotNull(parsedEntry);
        entry.equals(parsedEntry);
        Assert.assertEquals(entry, parsedEntry);
    }
}
