package com.atakmap.commoncommo;

/**
 * Enum representing transport methods for streaming connections
 */
public enum StreamingTransport {
    TCP(0),
    SSL(1),
    QUIC(2);
    
    private final int transport;
    
    private StreamingTransport(int transport) {
        this.transport = transport;
    }
    
    int getNativeVal() {
        return transport;
    }
}
