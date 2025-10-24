
package com.atakmap.android.radiolibrary.rover;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.DatagramSocket;

/**
 * The sole purpose of this is to be able to replace the default
 * socket factory in use by MessageManager package.
 */

public interface SocketFactory {
    /**
     * Implementation producing a unbound multicast socket.
     */
    MulticastSocket createMulticastSocket() throws IOException;

    /**
     * Implementation producing a multicast socket bound to a port.
     */
    MulticastSocket createMulticastSocket(int port) throws IOException;

    /**
     * Implementation producing a unbound datagram socket.
     */
    DatagramSocket createDatagramSocket() throws IOException;
}
