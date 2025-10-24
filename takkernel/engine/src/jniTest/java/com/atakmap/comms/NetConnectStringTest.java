
package com.atakmap.comms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import gov.tak.test.KernelJniTest;

public class NetConnectStringTest extends KernelJniTest {

    @org.junit.Test
    public void getProto() {
        final NetConnectString ncs = new NetConnectString("udp",
                "192.168.130.1", 3456);
        assertEquals("udp", ncs.getProto());

    }

    @org.junit.Test
    public void getHost() {
        final NetConnectString ncs = new NetConnectString("udp",
                "192.168.130.1", 3456);
        assertEquals("192.168.130.1", ncs.getHost());
    }

    @org.junit.Test
    public void getPort() {
        final NetConnectString ncs = new NetConnectString("udp",
                "192.168.130.1", 3456);
        assertEquals(3456, ncs.getPort());
    }

    @org.junit.Test
    public void getCallsign() {
        final NetConnectString ncs = new NetConnectString("udp",
                "192.168.130.1", 3456);
        ncs.setCallsign("testCallsign");
        assertEquals("testCallsign", ncs.getCallsign());
    }

    @org.junit.Test
    public void setCallsign() {
        final NetConnectString ncs = new NetConnectString("udp",
                "192.168.130.1", 3456);
        ncs.setCallsign("testCallsign");
        assertEquals("testCallsign", ncs.getCallsign());
    }

    @org.junit.Test
    public void fromString() {
        final NetConnectString ncs = NetConnectString
                .fromString("udp://192.168.130.1:3456");
        final NetConnectString cmp = new NetConnectString("udp",
                "192.168.130.1", 3456);
        assertEquals(ncs, cmp);
    }

    @org.junit.Test
    public void matches() {
        final NetConnectString ncs = NetConnectString
                .fromString("udp://192.168.130.1:3456");
        assertTrue(ncs.matches("udp", "192.168.130.1", 3456));
    }

    @org.junit.Test
    public void isNull() {
        final NetConnectString ncs = NetConnectString
                .fromString("udp::ZT34");
        assertNull(ncs);
    }

    @org.junit.Test
    public void malformed() {
        final NetConnectString ncs = NetConnectString
                .fromString("udp:ZT34");
        assertNull(ncs);
    }

    @org.junit.Test
    public void hasCallsign() {
        final NetConnectString ncs = NetConnectString
                .fromString("192.168.130.1:3456:udp:DAE");
        assertEquals("DAE", ncs.getCallsign());
    }

    @org.junit.Test
    public void serverContact1() {
        final NetConnectString ncs = NetConnectString
                .fromString("*:-1:stcp:GRAY KNIGHT");
        assertEquals("*", ncs.getHost());
        assertEquals(-1, ncs.getPort());
        assertEquals("stcp", ncs.getProto());
        assertEquals("GRAY KNIGHT", ncs.getCallsign());
    }

    @org.junit.Test
    public void serverContact2() {
        final NetConnectString ncs = NetConnectString
                .fromString("*:-1:stcp");
        assertEquals("*", ncs.getHost());
        assertEquals(-1, ncs.getPort());
        assertEquals("stcp", ncs.getProto());
        assertNull(ncs.getCallsign());
    }

    @org.junit.Test
    public void serverContact3() {
        final NetConnectString ncs = NetConnectString
                .fromString("192.168.50.1:-1:stcp");
        assertNull(ncs);
    }

    @org.junit.Test
    public void serverContact4() {
        final NetConnectString ncs = NetConnectString
                .fromString("*:0:stcp");
        assertNull(ncs);
    }

    @org.junit.Test
    public void serverContact5() {
        final NetConnectString ncs = NetConnectString
                .fromString("*:1:stcp");
        assertNull(ncs);
    }

}
