package gov.tak.api.video;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

public class ConnectionEntryTest {
    @Test
    public void Protocol_ctor_default_port() {
        EnumSet<ConnectionEntry.Protocol> explicitPortProtocols = EnumSet.of(
                ConnectionEntry.Protocol.HTTP,
                ConnectionEntry.Protocol.HTTPS,
                ConnectionEntry.Protocol.RTMP,
                ConnectionEntry.Protocol.RTMPS,
                ConnectionEntry.Protocol.RTSP);
        EnumSet<ConnectionEntry.Protocol> defaultPortProtocols = EnumSet.allOf(ConnectionEntry.Protocol.class);
        defaultPortProtocols.removeAll(explicitPortProtocols);

        for(ConnectionEntry.Protocol p : explicitPortProtocols)
            Assert.assertNotEquals(p.toString() + " is using default port", -1, p.getDefaultPort());
        for(ConnectionEntry.Protocol p : defaultPortProtocols)
            Assert.assertEquals(p.toString() + " is not using default port",-1, p.getDefaultPort());
    }

    @Test
    public void Protocol_fromString_roundtrip() {
        for(ConnectionEntry.Protocol protocol : ConnectionEntry.Protocol.values()) {
            final ConnectionEntry.Protocol fromString = ConnectionEntry.Protocol.fromString(protocol.toString());
            Assert.assertSame(protocol, fromString);
        }
    }

    @Test
    public void Protocol_fromString_miss() {
        final ConnectionEntry.Protocol fromString = ConnectionEntry.Protocol.fromString("foobar");
        Assert.assertSame(ConnectionEntry.Protocol.RAW, fromString);
    }

    @Test
    public void Protocol_getProtocol_roundtrip() {
        for(ConnectionEntry.Protocol protocol : ConnectionEntry.Protocol.values()) {
            final ConnectionEntry.Protocol fromString = ConnectionEntry.Protocol.getProtocol(protocol.toURL() + "path/to/file");
            Assert.assertSame(protocol, fromString);
        }
    }

    @Test
    public void Protocol_getProtocol_miss() {
        final ConnectionEntry.Protocol fromString = ConnectionEntry.Protocol.getProtocol("foobar://path/to/file");
        Assert.assertSame(ConnectionEntry.Protocol.RAW, fromString);
    }

    @Test
    public void ctor_args_roundtrip() {
        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = "foo:bar";
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

        Assert.assertEquals(alias, entry.getAlias());
        Assert.assertEquals(address, entry.getAddress());
        Assert.assertEquals(port, entry.getPort());
        Assert.assertEquals(roverPort, entry.getRoverPort());
        Assert.assertEquals(path, entry.getPath());
        Assert.assertEquals(protocol, entry.getProtocol());
        Assert.assertEquals(networkTimeout, entry.getNetworkTimeout());
        Assert.assertEquals(bufferTime, entry.getBufferTime());
        Assert.assertEquals(rtspReliable, entry.getRtspReliable());
        Assert.assertEquals(passphrase, entry.getPassphrase());
        Assert.assertEquals(source, entry.getSource());
    }

    @Test
    public void ctor_file_roundtrip() {
        File f = new File("some/file");
        final ConnectionEntry entry = new ConnectionEntry(f);
        Assert.assertEquals(f.getName(), entry.getAlias());
        Assert.assertEquals(f.getAbsolutePath(), entry.getPath());
        Assert.assertEquals(ConnectionEntry.Protocol.FILE, entry.getProtocol());
    }

//    public ConnectionEntry(String alias, String uri) {
//        setAlias(alias);
//        setBufferTime(-1);
//        setNetworkTimeout(5000);
//
//        URI u = URI.create(uri);
//
//        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.getProtocol(uri);
//        int port = u.getPort();
//        if (port == -1)
//            port = protocol.getDefaultPort();
//        if (port < 0)
//            port = 1234;
//
//        final String userInfo = u.getUserInfo();
//
//        String host = u.getHost();
//        if (!FileSystemUtils.isEmpty(u.getUserInfo()))
//            host = u.getUserInfo() + "@" + host;
//
//        setProtocol(protocol);
//        setAddress(host);
//        setPort(port);
//
//        String query = u.getQuery();
//        if (FileSystemUtils.isEmpty(query))
//            setPath(u.getPath());
//        else if (protocol == ConnectionEntry.Protocol.SRT) {
//            // parse out passphrase
//            // retain additional query elements
//            final String[] pairs = query.split("&");
//            StringBuilder newQuery = new StringBuilder();
//            for (String nv : pairs) {
//                if (nv.startsWith("passphrase=")) {
//                    final String value = nv.replace("passphrase=", "");
//                    if (!FileSystemUtils.isEmpty(value))
//                        setPassphrase(value);
//                } else if (nv.startsWith("timeout=")) {
//                    final String value = nv.replace("timeout=", "");
//                    try {
//                        int timeout = Integer.parseInt(value);
//                        if (timeout > 0)
//                            setNetworkTimeout(timeout);
//                    } catch (NumberFormatException nfe) {
//                        Log.e(TAG, "error parsing port number: " + nv);
//                    }
//                } else {
//                    if (newQuery.length() > 0)
//                        newQuery.append("&");
//                    newQuery.append(nv);
//                }
//            }
//            if (newQuery.length() > 0)
//                setPath(u.getPath() + "?" + newQuery);
//            else
//                setPath(u.getPath());
//        } else {
//            setPath(u.getPath() + "?" + query);
//        }
//
//        if ((this.protocol == ConnectionEntry.Protocol.RTSP)) {
//            String q = u.getQuery();
//            if (q != null)
//                this.rtspReliable = (q.contains("tcp")) ? 1 : 0;
//        }
//    }

    @Test
    public void ctor_empty() {
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotNull(entry.getUID());
    }

    @Test
    public void copy_create() {
        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = "foo:bar";
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

        final ConnectionEntry copy = entry.copy();

        Assert.assertNotEquals(entry.getUID(), copy.getUID());

        Assert.assertEquals(copy.getAlias(), entry.getAlias());
        Assert.assertEquals(copy.getAddress(), entry.getAddress());
        Assert.assertEquals(copy.getPort(), entry.getPort());
        Assert.assertEquals(copy.getRoverPort(), entry.getRoverPort());
        Assert.assertEquals(copy.getPath(), entry.getPath());
        Assert.assertEquals(copy.getProtocol(), entry.getProtocol());
        Assert.assertEquals(copy.getNetworkTimeout(), entry.getNetworkTimeout());
        Assert.assertEquals(copy.getBufferTime(), entry.getBufferTime());
        Assert.assertEquals(copy.getRtspReliable(), entry.getRtspReliable());
        Assert.assertEquals(copy.getPassphrase(), entry.getPassphrase());
        Assert.assertEquals(copy.getSource(), entry.getSource());
    }

    @Test
    public void copy_roundtrip() {
        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = "foo:bar";
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
        final ConnectionEntry copy = new ConnectionEntry();
        copy.copy(entry);

        Assert.assertEquals(entry, copy);

        Assert.assertEquals(copy.getAlias(), entry.getAlias());
        Assert.assertEquals(copy.getAddress(), entry.getAddress());
        Assert.assertEquals(copy.getPort(), entry.getPort());
        Assert.assertEquals(copy.getRoverPort(), entry.getRoverPort());
        Assert.assertEquals(copy.getPath(), entry.getPath());
        Assert.assertEquals(copy.getProtocol(), entry.getProtocol());
        Assert.assertEquals(copy.getNetworkTimeout(), entry.getNetworkTimeout());
        Assert.assertEquals(copy.getBufferTime(), entry.getBufferTime());
        Assert.assertEquals(copy.getRtspReliable(), entry.getRtspReliable());
        Assert.assertEquals(copy.getPassphrase(), entry.getPassphrase());
        Assert.assertEquals(copy.getSource(), entry.getSource());
    }

    @Test
    public void getIgnoreEmbeddedKLV_roundtrip() {
        final ConnectionEntry entry = new ConnectionEntry();
        final boolean ignoreEmbeddedKLV = entry.getIgnoreEmbeddedKLV();
        entry.setIgnoreEmbeddedKLV(!ignoreEmbeddedKLV);
        Assert.assertEquals(!ignoreEmbeddedKLV, entry.getIgnoreEmbeddedKLV());
    }

    @Test
    public void getRoverPort() {
        ConnectionEntry entry = new ConnectionEntry();
        final int roverPort = entry.getRoverPort();
        entry.setRoverPort(~roverPort);
        Assert.assertEquals(~roverPort, entry.getRoverPort());
    }

    @Test
    public void setAlias() {
        final String alias = "somealias";
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(alias, entry.getAlias());
        entry.setAlias(alias);
        Assert.assertEquals(alias, entry.getAlias());
    }

    @Test
    public void setUID() {
        final String uid = "1234567890abcdefghij";
        final ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(uid, entry.getUID());
        entry.setUID(uid);
        Assert.assertEquals(uid, entry.getUID());
    }

    @Test
    public void setAddress() {
        final String address = "rtsp://user@pass:78.65.43.21:9876/with/some/path?qp1=xyz&qp2=123";
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(address, entry.getAddress());
        entry.setAddress(address);
        Assert.assertEquals(address, entry.getAddress());
    }

    @Test
    public void setPreferredInterfaceAddress() {
        final String preferredInterfaceAddress = "12:34:56:ab:cd:ef";
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(preferredInterfaceAddress, entry.getPreferredInterfaceAddress());
        entry.setPreferredInterfaceAddress(preferredInterfaceAddress);
        Assert.assertEquals(preferredInterfaceAddress, entry.getPreferredInterfaceAddress());
    }

    @Test
    public void setPort() {
        ConnectionEntry entry = new ConnectionEntry();
        final int port = entry.getPort();
        entry.setPort(~port);
        Assert.assertEquals(~port, entry.getPort());
    }

    @Test
    public void setPath_roundtrip() {
        final String path = "some/random/path";
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(path, entry.getPath());
        entry.setPath(path);
        Assert.assertEquals(path, entry.getPath());
    }

    @Test
    public void setProtocol_roundtrip() {
        ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.SRT;
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(protocol, entry.getPassphrase());
        entry.setProtocol(protocol);
        Assert.assertEquals(protocol, entry.getProtocol());
    }

    @Test
    public void setNetworkTimeout_roundtrip() {
        final ConnectionEntry entry = new ConnectionEntry();
        final int networkTimeout = entry.getNetworkTimeout();
        entry.setNetworkTimeout(~networkTimeout);
        Assert.assertEquals(~networkTimeout, entry.getNetworkTimeout());
    }

    @Test
    public void setBufferTime() {
        ConnectionEntry entry = new ConnectionEntry();
        final int bufferTime = entry.getBufferTime();
        entry.setBufferTime(~bufferTime);
        Assert.assertEquals(~bufferTime, entry.getBufferTime());
    }

    @Test
    public void setRtspReliable() {
        ConnectionEntry entry = new ConnectionEntry();
        final int rtspReliable = entry.getRtspReliable();
        entry.setRtspReliable(~rtspReliable);
        Assert.assertEquals(~rtspReliable, entry.getRtspReliable());
    }

    @Test
    public void setPassphrase() {
        final String pass = "passphrase1234";
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNotEquals(pass, entry.getPassphrase());
        entry.setPassphrase(pass);
        Assert.assertEquals(pass, entry.getPassphrase());
    }

    @Test
    public void setSource() {
        EnumSet<ConnectionEntry.Source> sources = EnumSet.allOf(ConnectionEntry.Source.class);
        for(ConnectionEntry.Source s : sources) {
            ConnectionEntry entry  = new ConnectionEntry();
            entry.setSource(s);
            Assert.assertEquals(s, entry.getSource());
        }
    }

    @Test
    public void isRemote() {
        EnumSet<ConnectionEntry.Protocol> localProtocols = EnumSet.of(ConnectionEntry.Protocol.FILE, ConnectionEntry.Protocol.DIRECTORY);
        EnumSet<ConnectionEntry.Protocol> remoteProtocols = EnumSet.allOf(ConnectionEntry.Protocol.class);
        remoteProtocols.removeAll(localProtocols);

        for(ConnectionEntry.Protocol p : localProtocols) {
            ConnectionEntry entry = new ConnectionEntry();
            entry.setProtocol(p);
            Assert.assertFalse(entry.isRemote());
        }
        for(ConnectionEntry.Protocol p : remoteProtocols) {
            ConnectionEntry entry = new ConnectionEntry();
            entry.setProtocol(p);
            Assert.assertTrue(entry.isRemote());
        }
    }

    @Test
    public void setChildren() {
        Collection<ConnectionEntry> children = Arrays.asList(
                new ConnectionEntry("child1", "file://path/to/video1"),
                new ConnectionEntry("child2", "file://path/to/video2"),
                new ConnectionEntry("child3", "file://path/to/video3")
        );
        for(ConnectionEntry child : children)
            child.setUID(UUID.randomUUID().toString());

        for(ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());

        ConnectionEntry parent = new ConnectionEntry();
        parent.setUID(UUID.randomUUID().toString());
        parent.setProtocol(ConnectionEntry.Protocol.DIRECTORY);
        parent.setChildren(children);

        for(ConnectionEntry child : children) {
            Assert.assertEquals(parent.getUID(), child.getParentUID());
        }

        parent.setChildren(null);
        Assert.assertTrue(parent.getChildren() == null || parent.getChildren().isEmpty());
        for(ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());

//        if (this.protocol != ConnectionEntry.Protocol.DIRECTORY)
//            return;
//        synchronized (this) {
//            if (children == null && this.children != null) {
//                // all children are stale; unlink
//                for (ConnectionEntry e : this.children.values()) {
//                    if (e.parentUID.equals(this.uid))
//                        e.parentUID = null;
//                }
//                this.children = null;
//            } else if (children != null) {
//                Map<String, ConnectionEntry> oldChildren = this.children;
//                this.children = new LinkedHashMap<>();
//                for (ConnectionEntry e : children) {
//                    if (e.isRemote())
//                        continue; // Remote entries are always at top-level (ATAK-10652)
//                    this.children.put(e.getUID(), e);
//                    e.parentUID = this.uid;
//                    if (oldChildren != null)
//                        oldChildren.remove(e.getUID());
//                }
//                if (oldChildren != null) {
//                    // unlink any stale children
//                    for (ConnectionEntry e : oldChildren.values())
//                        if (e.parentUID.equals(this.uid))
//                            e.parentUID = null;
//                }
//            }
//        }
    }

    @Test
    public void setChildren_remote_parent_is_noop() {
        Collection<ConnectionEntry> children = Arrays.asList(
                new ConnectionEntry("child1", "file://path/to/video1"),
                new ConnectionEntry("child2", "file://path/to/video2"),
                new ConnectionEntry("child3", "file://path/to/video3")
        );
        for(ConnectionEntry child : children)
            child.setUID(UUID.randomUUID().toString());

        for (ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());

        ConnectionEntry parent = new ConnectionEntry("mcast", "udp://239.1.2.3:1234");
        parent.setUID(UUID.randomUUID().toString());
        parent.setProtocol(ConnectionEntry.Protocol.UDP);
        parent.setChildren(children);

        Assert.assertTrue(parent.getChildren() == null || parent.getChildren().isEmpty());
        for (ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());
    }

    @Test
    public void setChildren_remote_children_is_noop() {
        Collection<ConnectionEntry> children = Arrays.asList(
                new ConnectionEntry("child1", "udp://239.1.1.1:1234"),
                new ConnectionEntry("child2", "udp://239.1.1.2:1234"),
                new ConnectionEntry("child3", "udp://239.1.1.3:1234")
        );
        for(ConnectionEntry child : children)
            child.setUID(UUID.randomUUID().toString());

        for (ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());

        ConnectionEntry parent = new ConnectionEntry("video_dir", "file://path/to/video/dir");
        parent.setUID(UUID.randomUUID().toString());
        parent.setProtocol(ConnectionEntry.Protocol.DIRECTORY);
        parent.setChildren(children);

        Assert.assertTrue(parent.getChildren().isEmpty());
        for (ConnectionEntry child : children)
            Assert.assertNull(child.getParentUID());
    }

    @Test
    public void setLocalFile() {
        final File file = new File("localfile");
        ConnectionEntry entry = new ConnectionEntry();
        Assert.assertNull(entry.getLocalFile());
        entry.setLocalFile(file);
        Assert.assertEquals(file, entry.getLocalFile());
    }

    @Test
    public void setTemporary() {
        ConnectionEntry entry = new ConnectionEntry();
        final boolean temp = entry.isTemporary();
        entry.setTemporary(!temp);
        Assert.assertEquals(!temp, entry.isTemporary());
    }

    @Test
    public void getURL() {
        ConnectionEntry ce = new ConnectionEntry("myalias",
                "udp://239.255.0.1:2000");
        Assert.assertEquals("239.255.0.1:2000", ConnectionEntry.getURL(ce));
    }

    @Test
    public void getRTSPReliableFromUri() {
        ConnectionEntry ce = new ConnectionEntry("myalias",
                "rtsp://192.168.1.100:554/axis-media/media.amp?tcp");
        Assert.assertEquals(1, ce.getRtspReliable());
        ce = new ConnectionEntry("myalias",
                "rtsp://192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals(0, ce.getRtspReliable());
    }

    @Test
    public void getRTSPReliableFromUri2() {
        ConnectionEntry ce = new ConnectionEntry("myalias",
                "rtsp://192.168.1.100:554/axis-media/media.amp?joe&tcp");
        Assert.assertEquals(1, ce.getRtspReliable());
    }

    @Test
    public void getRtspGetUserPassFromUri1() {
        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "username:password@192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals("username", userPassIp[0]);
        Assert.assertEquals("password", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);
    }

    @Test
    public void getRtspGetUserPassFromUriBad1() {
        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "username:password@192.168.1.100:554/axis-media|media.amp");
        Assert.assertEquals("username", userPassIp[0]);
        Assert.assertEquals("password", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);
    }

    @Test
    public void getRtspGetUserPassFromUri2() {

        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "rtsp://username:@192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals("username", userPassIp[0]);
        Assert.assertEquals("", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);
    }

    @Test
    public void getRtspGetUserPassFromUri3() {
        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "rtsp://@192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals("", userPassIp[0]);
        Assert.assertEquals("", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);

    }

    @Test
    public void getRtspGetUserPassFromUri4() {
        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "rtsp://192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals("", userPassIp[0]);
        Assert.assertEquals("", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);

    }

    @Test
    public void getRtspGetUserPassFromUri5() {
        String[] userPassIp = ConnectionEntry.getUserPassIp(
                "rtsp://:@192.168.1.100:554/axis-media/media.amp");
        Assert.assertEquals("", userPassIp[0]);
        Assert.assertEquals("", userPassIp[1]);
        Assert.assertEquals("192.168.1.100", userPassIp[2]);

    }

    @Test
    public void connectionEntryFromBadUri() {
        ConnectionEntry ce = new ConnectionEntry("test",
                "gopher://eighties-4eva.net/video-stream");
        Assert.assertEquals(ce.getProtocol().toString(), "raw");
    }

    @Test
    public void connectionEntrySrt() {
        ConnectionEntry ce = new ConnectionEntry("test",
                "srt://compass.vidterra.com:1935?streamid=play/68b7c23ce50a8816a208aaadf1c41d51");
        Assert.assertEquals(ce.getProtocol(), ConnectionEntry.Protocol.SRT);
        Assert.assertEquals(ce.getPath(),
                "?streamid=play/68b7c23ce50a8816a208aaadf1c41d51");

        ConnectionEntry ce1 = new ConnectionEntry("test",
                "srt://gw.vidterra.com:5999?passphrase=Password123");
        Assert.assertEquals(ce1.getProtocol(), ConnectionEntry.Protocol.SRT);
        Assert.assertEquals(ce1.getPassphrase(), "Password123");
        Assert.assertEquals(ce1.getPath(), "");

        ConnectionEntry ce2 = new ConnectionEntry("test",
                "https://compass.vidterra.com/stream/239.67.53.0:8208");
        Assert.assertEquals(ce2.getProtocol(), ConnectionEntry.Protocol.HTTPS);
        Assert.assertEquals(ce2.getPath(), "/stream/239.67.53.0:8208");

        ConnectionEntry ce3 = new ConnectionEntry("test",
                "srt://compass.vidterra.com:1935?passphrase=Password123&streamid=play/68b7c23ce50a8816a208aaadf1c41d51");
        Assert.assertEquals(ce3.getProtocol(), ConnectionEntry.Protocol.SRT);
        Assert.assertEquals(ce3.getPassphrase(), "Password123");
        Assert.assertEquals(ce3.getPath(),
                "?streamid=play/68b7c23ce50a8816a208aaadf1c41d51");

    }

    @Test
    public void connectionEntrySrt2() {
        ConnectionEntry ce = new ConnectionEntry("test",
                "srt://34.219.213.241:9005?timeout=12000000&passphrase=DemoOfSanta");
        Assert.assertEquals(ce.getProtocol(), ConnectionEntry.Protocol.SRT);
        Assert.assertEquals(ce.getNetworkTimeout(), 12000);
        Assert.assertEquals(ce.getPassphrase(), "DemoOfSanta");
        Assert.assertEquals(ce.getPort(), 9005);
    }

    @Test
    public void connectionEntrySrt3() {
        ConnectionEntry ce2 = new ConnectionEntry("test",
                "srt://1.2.3.4:9000?passphrase=Pass%23word123&streamid=%23!::t=stream,m=request,r=teststream");
        Assert.assertEquals(ce2.getPath(),
                "?streamid=#!::t=stream,m=request,r=teststream");
        Assert.assertEquals(ce2.getPassphrase(), "Pass#word123");

    }
    
    @Test
    public void connectionEntrySrt4() {
        String url = "srt://63.250.55.72:8890?streamid=#!::m=request,r=uas5&passphrase=Takistheway123";
        ConnectionEntry ce = new ConnectionEntry("test", url);
        Assert.assertEquals("?streamid=#!::m=request,r=uas5",ce.getPath());

        String expectedUrl = "srt://63.250.55.72:8890?streamid=#!::m=request,r=uas5&timeout=5000000&passphrase=Takistheway123";
        Assert.assertEquals(expectedUrl, ConnectionEntry.getURL(ce));
    }

    @Test
    public void connectionEntryRtspAccess() {
        ConnectionEntry ce = new ConnectionEntry("test",
                "rtsp://192.168.50.112:554/mydevice/rtsp/live?access=iK8bnq1xfxcR_vvVukvwpy3TNCZgIT7KS5A_MU3W8Pm0Am5Czod4MVXTbTggFCQENi-eBBqiEyejE2lFfeByzL-3CUBWKraRsM_2HsgwG0nuZ0wRnq2AJDFsdLDx2moB0");
        Assert.assertEquals(ConnectionEntry.Protocol.RTSP, ce.getProtocol());
        Assert.assertEquals(ce.getPort(), 554);
        Assert.assertEquals(ce.getPath(),
                "/mydevice/rtsp/live?access=iK8bnq1xfxcR_vvVukvwpy3TNCZgIT7KS5A_MU3W8Pm0Am5Czod4MVXTbTggFCQENi-eBBqiEyejE2lFfeByzL-3CUBWKraRsM_2HsgwG0nuZ0wRnq2AJDFsdLDx2moB0");

        ConnectionEntry ce1 = new ConnectionEntry("test",
                "rtsp://192.168.50.112/mydevice/rtsp/live?access=iK8bnq1xfxcR_vvVukvwpy3TNCZgIT7KS5A_MU3W8Pm0Am5Czod4MVXTbTggFCQENi-eBBqiEyejE2lFfeByzL-3CUBWKraRsM_2HsgwG0nuZ0wRnq2AJDFsdLDx2moB0");
        Assert.assertEquals(ConnectionEntry.Protocol.RTSP, ce1.getProtocol());
        Assert.assertEquals(ce1.getPort(), 554);
        Assert.assertEquals(ce1.getPath(),
                "/mydevice/rtsp/live?access=iK8bnq1xfxcR_vvVukvwpy3TNCZgIT7KS5A_MU3W8Pm0Am5Czod4MVXTbTggFCQENi-eBBqiEyejE2lFfeByzL-3CUBWKraRsM_2HsgwG0nuZ0wRnq2AJDFsdLDx2moB0");

    }

//    /**
//     * Provide the legacy call to getURL which will show all of the sensitive bits and pieces.
//     */
//    public static String getURL(ConnectionEntry ce) {
//        return getURL(ce, false);
//    }
//
//    /**
//     * Given a connection entry, produces a well formed URL.
//     * @param forDisplay true if URL will be displayed or logged or used in some way other than
//     *                   an actual connection to the media.  This results in sensitive parts of the
//     *                   URL being obscured.
//     */
//    @FortifyFinding(finding = "Password Management: Hardcoded Password", rational = "XXHiddenXX is a UI password mask and not a password. 'passphrase' is not a hardcoded password, rather an attribute/field name")
//    public static String getURL(ConnectionEntry ce, boolean forDisplay) {
//        String url = ce.getAddress(forDisplay);
//
//        // remove the null from the file entries
//        if (url == null)
//            url = "";
//
//        ConnectionEntry.Protocol p = ce.getProtocol();
//
//        if (p == ConnectionEntry.Protocol.RAW) {
//            url = ce.getAddress();
//            if (url == null)
//                return "";
//            else
//                return url.trim();
//        } else if (p == ConnectionEntry.Protocol.UDP || p == ConnectionEntry.Protocol.RTSP
//                || p == ConnectionEntry.Protocol.FILE) {
//            // original code did nothing
//        } else {
//            // construct a proper URL
//            url = p.toURL() + ce.getAddress(forDisplay);
//        }
//
//        if (ce.getPort() != -1)
//            url += ":" + ce.getPort();
//
//        if (ce.getPath() != null && !ce.getPath().trim().isEmpty()) {
//            if (!ce.getPath().startsWith("/")) {
//                url += "/";
//            }
//            url += ce.getPath();
//        }
//
//        String sep = "?";
//        if (url.contains("?")) {
//            sep = "&";
//        }
//
//        if (ce.getNetworkTimeout() > 0) {
//            if (p == ConnectionEntry.Protocol.RTP) {
//                // value of the timeout needs to be milliseconds
//                url = url + sep + "timeout=" + (ce.getNetworkTimeout());
//                sep = "&";
//            } else if (p == ConnectionEntry.Protocol.RTMP || p == ConnectionEntry.Protocol.RTMPS) {
//                // value of the timeout needs to be seconds
//                url = url + sep + "timeout="
//                        + ce.getNetworkTimeout() / 1000;
//                sep = "&";
//            } else if (p == ConnectionEntry.Protocol.HTTP || p == ConnectionEntry.Protocol.HTTPS) {
//                // value of the timeout needs to be microseconds
//                url = url + sep + "timeout=" + (ce.getNetworkTimeout() * 1000)
//                        + "&seekable=0";
//                sep = "&";
//            } else if (p == ConnectionEntry.Protocol.SRT) {
//                // value of the timeout needs to be microseconds
//                url = url + sep + "timeout=" + (ce.getNetworkTimeout() * 1000);
//                sep = "&";
//            }
//        }
//
//        if ((p == ConnectionEntry.Protocol.RTSP) && (ce.getRtspReliable() == 1)) {
//            url = url + sep + "tcp";
//            Log.d(TAG, "rtsp reliable communications requested");
//        }
//        if (p == ConnectionEntry.Protocol.SRT && (!ce.getPassphrase().isEmpty())) {
//            if (forDisplay) {
//                url = url + sep + "passphrase=XXHiddenXX";
//            } else {
//                url = url + sep + "passphrase=" + ce.getPassphrase();
//            }
//        }
//        return url.trim();
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o)
//            return true;
//        if (o == null || getClass() != o.getClass())
//            return false;
//        ConnectionEntry that = (ConnectionEntry) o;
//        return port == that.port &&
//                roverPort == that.roverPort &&
//                ignoreEmbeddedKLV == that.ignoreEmbeddedKLV &&
//                networkTimeout == that.networkTimeout &&
//                bufferTime == that.bufferTime &&
//                rtspReliable == that.rtspReliable &&
//                Objects.equals(alias, that.alias) &&
//                Objects.equals(uid, that.uid) &&
//                Objects.equals(address, that.address) &&
//                Objects.equals(preferredInterfaceAddress,
//                        that.preferredInterfaceAddress)
//                &&
//                Objects.equals(path, that.path) &&
//                protocol == that.protocol &&
//                Objects.equals(passphrase, that.passphrase) &&
//                source == that.source &&
//                Objects.equals(localFile, that.localFile) &&
//                Objects.equals(childrenUIDs(), that.childrenUIDs()) &&
//                Objects.equals(parentUID, that.parentUID);
//    }
//
//    /**
//     * Parses out a username and password combination from the provided
//     * address or url
//     * @param address the address of url
//     * @return an array of length 3, with the username being in position 0,
//     * password being in position 1 and the address or rest of the url being
//     * in position 2.
//     */
//    public static String[] getUserPassIp(String address) {
//        final String[] retval = new String[] {
//                "", "", address
//        };
//
//        if (!address.contains("://"))
//            address = "scheme://" + address;
//
//        String up;
//        String host;
//
//        try {
//            final URI u = URI.create(address);
//            up = u.getUserInfo();
//            host = u.getHost();
//        } catch (Exception uriException) {
//            try {
//                address = address.replace("scheme://", "http://");
//                URL url = new URL(address);
//                up = url.getUserInfo();
//                host = url.getHost();
//            } catch (Exception urlException) {
//                return retval;
//            }
//        }
//        if (!FileSystemUtils.isEmpty(up)) {
//            String[] upList = up.split(":");
//            if (upList.length > 0)
//                retval[0] = upList[0];
//            if (upList.length > 1)
//                retval[1] = upList[1];
//        }
//        retval[2] = host;
//        return retval;
//    }

    @Test
    public void Builder_roundtrip() {
        final String alias = "alias1";
        final String address = "12.34.56.78";
        final int port = 34;
        final int roverPort = 56;
        final String path = "path/of/the/test";
        final ConnectionEntry.Protocol protocol = ConnectionEntry.Protocol.UDP;
        final int networkTimeout = 78;
        final int bufferTime = 90;
        final int rtspReliable = 999;
        final String passphrase = "foo:bar";
        final ConnectionEntry.Source source = ConnectionEntry.Source.EXTERNAL;
        final String uid = UUID.randomUUID().toString();
        final String preferredNetworkInterface  = "wlan0";
        final boolean ignoreEmbeddedKLV = true;
        final File localFile = new File("foobar");
        final Collection<ConnectionEntry> children = null;
        final String parentUID = UUID.randomUUID().toString();
        final boolean temporary = true;

        final ConnectionEntry entry = new ConnectionEntry.Builder()
                    .setAlias(alias)
                    .setAddress(address)
                    .setPort(port)
                    .setRoverPort(roverPort)
                    .setPath(path)
                    .setProtocol(protocol)
                    .setNetworkTimeout(networkTimeout)
                    .setBufferTime(bufferTime)
                    .setRtspReliable(rtspReliable)
                    .setPassphrase(passphrase)
                    .setSource(source)
                    .setUID(uid)
                    .setPreferredInterfaceAddress(preferredNetworkInterface)
                    .setIgnoreEmbeddedKLV(ignoreEmbeddedKLV)
                    .setLocalFile(localFile)
                    .setChildren(children)
                    .setParentUID(parentUID)
                    .setTemporary(temporary)
                .build();

        Assert.assertEquals(alias, entry.getAlias());
        Assert.assertEquals(address, entry.getAddress());
        Assert.assertEquals(port, entry.getPort());
        Assert.assertEquals(roverPort, entry.getRoverPort());
        Assert.assertEquals(path, entry.getPath());
        Assert.assertEquals(protocol, entry.getProtocol());
        Assert.assertEquals(networkTimeout, entry.getNetworkTimeout());
        Assert.assertEquals(bufferTime, entry.getBufferTime());
        Assert.assertEquals(rtspReliable, entry.getRtspReliable());
        Assert.assertEquals(passphrase, entry.getPassphrase());
        Assert.assertEquals(source, entry.getSource());
        Assert.assertEquals(uid, entry.getUID());
        Assert.assertEquals(preferredNetworkInterface, entry.getPreferredInterfaceAddress());
        Assert.assertEquals(ignoreEmbeddedKLV, entry.getIgnoreEmbeddedKLV());
        Assert.assertEquals(localFile, entry.getLocalFile());
        Assert.assertEquals(children, entry.getChildren());
        Assert.assertEquals(parentUID, entry.getParentUID());
        Assert.assertEquals(temporary, entry.isTemporary());
    }
}
