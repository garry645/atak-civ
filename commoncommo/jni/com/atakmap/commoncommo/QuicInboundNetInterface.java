package com.atakmap.commoncommo;

/**
 * Class representing a quic-based listening NetInterface.
 * QUIC interfaces listen on all local physical devices and are
 * unique by local listening port number.
 */
public class QuicInboundNetInterface extends NetInterface {
    private final int localPort;

    QuicInboundNetInterface(long nativePtr, int localPort)
    {
        super(nativePtr);
        this.localPort = localPort;
    }

    /**
     * Returns the local port in use by this QuicInboundNetInterface
     */    
    public int getPort()
    {
        return localPort;
    }
}
