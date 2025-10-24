
package com.atakmap.android.radiolibrary.rover;

import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Enumeration;

public class MessageManager {

    // constants
    private static final String TAG = "MessageManager";

    private static final int INTERFACE_DOWN_POLL = 5000;

    public enum Status {
        STATUS_OK(1),
        STATUS_TIMEOUT(2),
        STATUS_ERROR(3),
        STATUS_STOPPED(4);

        private final int value;

        Status(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }
    }

    /** 
     * Listener for data received from the device.
     */
    public interface DataListener {

        void onSendStatus(final Status status);

        void onReceiveStatus(final Status status);

        void onReceiveData(final byte[] data);

        void log(String tag, String message);

    }

    static final SocketFactory defaultFactory = new SocketFactory() {
        public MulticastSocket createMulticastSocket() throws IOException {
            return new MulticastSocket();
        }

        public MulticastSocket createMulticastSocket(final int port)
                throws IOException {
            return new MulticastSocket(port);
        }

        public DatagramSocket createDatagramSocket() throws IOException {
            return new DatagramSocket();
        }
    };

    // factory for creating multicast sockets.   
    static private SocketFactory sFactory = defaultFactory;

    // listener
    private DataListener dataListener = null;

    private final String mciInputAddress;
    private final int mciInputPort;

    private final String mciOutputAddress;
    private final int mciOutputPort;

    private String addr;

    private final int receiveTimeout;
    private final int retryTimeout;
    private final int ttl;

    UDPListener udpListener;

    /**
     * Constructs a message manager capable of send/receive to a L3 Communication
     * video receiver.
     * @param mciOutputAddress the output address to send the multicast commands on.
     * @param mciOutputPort the output port to send the multicast commands on.
     * @param mciInputAddress the output address to receive the multicast replies on.
     * @param mciInputPort the output port to recieve the multicast replies on.
     * @param ttl the time to live used when sending a message.   ttl should usually be set to 1
     * @param receiveTimeout in milliseconds, in case the network hiccups, this should be set to 
     * between 5000 to 10000.   Can be set to 0, but not recommended.   A negative number will 
     * produce undefined results.
     * @param retryTimeout in milliseconds, should be a number between 5000 and 10000. Can be set to 
     * zero, but NOT recommended. Creates CPU consumption issues when errors continually happen.
     * 
     */
    public MessageManager(final String mciInputAddress,
            final int mciInputPort,
            final String mciOutputAddress,
            final int mciOutputPort,
            final int ttl,
            final int receiveTimeout,
            final int retryTimeout) {

        this.mciInputAddress = mciInputAddress;
        this.mciInputPort = mciInputPort;
        this.mciOutputAddress = mciOutputAddress;
        this.mciOutputPort = mciOutputPort;
        this.ttl = ttl;
        this.receiveTimeout = receiveTimeout;
        this.retryTimeout = retryTimeout;
    }

    /**
     * If the format is specifically in the form of a mac address it will be used to identify
     * the interface used to listen on.
     * @param addr is in the format XX:XX:XX:XX:XX:XX or XXX.XXX.XXX.XXX, if null, empty string or any is passed in
     * the system default network interface will be used.
     */
    public void setInterface(final String addr) {
        if (addr == null || addr.equals("any")) {
            this.addr = null;
        } else if (addr.contains(".")) {
            this.addr = addr;
        } else if (addr.contains(":")) {
            this.addr = addr;
        } else {
            this.addr = addr;
        }
    }

    /**
     * Creates a string version of a mac address in pseudo human readable form.
     * This is usually printed on the device.
     */
    static private String byteToMac(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i],
                    (i < mac.length - 1) ? ":" : ""));
        }
        return sb.toString();
    }

    /**
     * Given a NetworkInterface, return the MAC address in the XX:XX:XX:XX:XX:XX format
     * otherwise returns null.
     */
    static public String getMacAddress(final NetworkInterface ni) {
        try {
            final byte[] macBytes = ni.getHardwareAddress();
            if (macBytes != null)
                return byteToMac(macBytes);
        } catch (SocketException se) {
            System.out.println(
                    "error occurred obtaining the hardware address for: " + ni);
        }
        return null;
    }

    /**
     * Wait for the interface to exist and be in the up state.   If the service is
     * told to stop listening, this method will return a null value.    Otherwise
     * a valid network interface will be returned.
     * @param addr the name of the interface to look for in XX:XX:XX:XX:XX:XX or XXX.XXX.XXX.XXX form.
     * @param period the polling period for this interface.
     */
    private NetworkInterface waitForNetworkInterface(final String addr,
            final int period) {
        NetworkInterface ni = null;

        while (ni == null && isListening()) {
            try {
                ni = getInterface(addr);
                if ((ni != null) && ni.isUp()) {
                    if (dataListener != null)
                        dataListener.log(TAG, "mac address: " + addr
                                + " successfully resolved to: " + ni.getName());
                    return ni; // good to go, network interface exists and is up.
                }

            } catch (SocketException se) {
                // keep looping
            }
            ni = null; // could be found but not up, set to null to keep looping.
            try {
                Thread.sleep(period);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Get the network interface of the device described in the network map file. The configuration
     * of the actual interface should only be obtained from the NetworkInterface and not from the
     * preferred values.
     *
     * @param addr is in the format of XXX.XXX.XXX.XXX or XX:XX:XX:XX:XX:XX
     *
     * @return the interface that describes the specific interface as defined by the network map.
     *         The network interface may have a name such as eth0, eth1, eth2, wlan0, ppp0, rmnet0,
     *         etc to use, or null if the interface is not present.  The interface should be examined
     *         for the actual configuration of the device.
     */
    private NetworkInterface getInterface(final String addr) {
        if (addr.contains(".")) {
            return getInterfaceByAddress(addr);
        } else if (addr.contains(":")) {
            return getInterfaceByMac(addr);
        } else {
            return null;
        }
    }

    private NetworkInterface getInterfaceByMac(String macaddr) {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                final String ma = getMacAddress(iface);
                if ((ma != null) && ma.equalsIgnoreCase(macaddr)) {
                    return iface;
                }
            }
        } catch (SocketException se) {
            if (dataListener != null)
                dataListener.log(TAG,
                        "error discovering interface given by macaddress: "
                                + macaddr);
        }
        return null;
    }

    private NetworkInterface getInterfaceByAddress(String address) {
        try {
            byte[] addr = getByteAddress(address);
            if (addr != null)
                return NetworkInterface
                        .getByInetAddress(InetAddress.getByAddress(addr));
        } catch (UnknownHostException | SocketException se) {
            if (dataListener != null)
                dataListener.log(TAG,
                        "error discovering interface given by address: "
                                + address);
        }
        return null;
    }

    /**
     * Given an IP address, return the appropriate byte array.
     */
    static private byte[] getByteAddress(final String s) {
        String[] arr = s.split("\\.");
        byte[] b = new byte[4];
        if (arr.length != 4)
            return null;
        try {
            for (int i = 0; i < 4; ++i) {
                int val = Integer.parseInt(arr[i]);
                b[i] = (byte) val;
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    public void setListener(DataListener mdl) {
        this.dataListener = mdl;
    }

    /**
     * Change out the SocketFactory.
     * @param sf null to use the default socket factory.
     */
    public static void setMulticastFactory(SocketFactory sf) {
        if (sf != null)
            sFactory = sf;
        else
            sFactory = defaultFactory;

    }

    public boolean isListening() {
        synchronized (this) {
            if (udpListener != null) {
                return !udpListener.isCancelled();
            } else {
                return false;
            }
        }
    }

    public void startListening() {

        stopListening();

        synchronized (this) {
            if (udpListener == null || udpListener.isCancelled()) {
                udpListener = new UDPListener();
                new Thread(udpListener).start();
            }
        }
    }

    public void stopListening() {
        synchronized (this) {
            if (udpListener != null) {
                udpListener.cancel();
            }
        }
    }

    /**
     * Sends a message encoded as a byte[] over the specified interface.
     * If the interface is null, then the message will be sent 
     * to the default interface as selected by android.
     * @param data the byte array to be sent.
     */
    public void send(final byte[] data) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                sendMessage(data);
            }
        });
        t.start();

    }

    private void sendMessage(final byte[] data) {

        Status status = Status.STATUS_OK;

        MulticastSocket socket = null;
        DatagramPacket packet;

        InetAddress local = null;
        try {
            local = InetAddress.getByName(mciInputAddress); // command
        } catch (UnknownHostException e) {
            status = Status.STATUS_ERROR;
            Log.e(TAG, "error occurred during message sending", e);
        }

        try {
            socket = sFactory.createMulticastSocket(mciOutputPort); // status
            socket.setTimeToLive(ttl);
            if (addr != null) {
                try {
                    NetworkInterface ni = getInterface(addr);
                    if (ni != null)
                        socket.setNetworkInterface(ni);
                } catch (SocketException se) {
                    Log.e(TAG, "error occurred during message sending", se);
                }
            }
        } catch (IOException e) {
            status = Status.STATUS_ERROR;
            Log.e(TAG, "error occurred during message sending", e);
        }

        if (socket != null) {
            packet = new DatagramPacket(data, data.length, local, mciInputPort); // command

            try {

                socket.send(packet);
                dataListener.log(TAG,
                        "sending " + Arrays.toString(data) + " to "
                                + mciInputAddress + ":" + mciInputPort + " on: "
                                + addr);
            } catch (IOException e) {
                status = Status.STATUS_ERROR;
                Log.e(TAG, "error occurred during message sending", e);
            }
        }
        IoUtils.close(socket);

        // return
        dataListener.onSendStatus(status);
    }

    public class UDPListener implements Runnable {

        private boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void run() {

            DatagramSocket socket = null;
            InetAddress address;
            NetworkInterface ni = null;
            SocketAddress sockAddr = null;

            try {
                // create socket and set properties
                address = InetAddress.getByName(mciOutputAddress);
            } catch (UnknownHostException e1) {
                // unknown host error
                notifyReceiveStatus(Status.STATUS_ERROR);
                return;
            }

            final byte[] message = new byte[8 * 1024];
            final DatagramPacket p = new DatagramPacket(message,
                    message.length);

            // run
            while (!cancelled) {
                try {
                    if (socket == null) {
                        if (addr != null) {
                            ni = waitForNetworkInterface(addr,
                                    INTERFACE_DOWN_POLL);
                            sockAddr = new InetSocketAddress(mciOutputAddress,
                                    mciOutputPort);
                        }
                        socket = sFactory.createMulticastSocket(mciOutputPort);
                        socket.setSoTimeout(receiveTimeout);

                        if (address.isMulticastAddress()) {
                            if (ni != null) {
                                dataListener.log(TAG,
                                        "issuing a join: " + mciOutputAddress
                                                + ": " + mciOutputPort + " on: "
                                                + ni);
                                ((MulticastSocket) socket).joinGroup(sockAddr,
                                        ni);
                            } else {
                                ((MulticastSocket) socket).joinGroup(address);
                            }
                        }
                    }

                    // receive packet
                    socket.receive(p);

                    // notify
                    notifyReceiveStatus(Status.STATUS_OK);

                    if (dataListener != null) {
                        dataListener.onReceiveData(message);
                    }

                } catch (InterruptedIOException toe) {
                    if (dataListener != null)
                        dataListener.log(TAG,
                                "interrupted exception occurred: " + toe);

                    // timeout
                    if (!cancelled)
                        notifyReceiveStatus(Status.STATUS_TIMEOUT);
                    IoUtils.close(socket);
                    socket = null;
                } catch (IOException e1) {
                    if (dataListener != null)
                        dataListener.log(TAG,
                                "ioexception occurred for udp receive: " + e1);

                    Log.e(TAG, "error occurred during message udp receive", e1);

                    // receive error
                    notifyReceiveStatus(Status.STATUS_ERROR);
                    try {
                        Thread.sleep(retryTimeout);
                    } catch (Exception ignored) {
                    }
                    IoUtils.close(socket);
                    socket = null;
                } catch (Exception e2) {
                    // log
                    if (dataListener != null)
                        dataListener.log(TAG,
                                "exception occurred for udp receive: " + e2);
                    Log.e(TAG, "error occurred during message udp receive", e2);

                    // general error
                    notifyReceiveStatus(Status.STATUS_ERROR);
                    try {
                        Thread.sleep(retryTimeout);
                    } catch (Exception ignored) {
                    }

                    IoUtils.close(socket);
                    socket = null;
                }
            }

            // clean up
            IoUtils.close(socket);
            notifyReceiveStatus(Status.STATUS_STOPPED);
        }
    }

    //---------------------------------------------------
    // Listener Notifications
    //---------------------------------------------------

    private void notifySendStatus(Status status) {
        if (dataListener != null) {
            dataListener.onSendStatus(status);
        }
    }

    private void notifyReceiveStatus(Status status) {
        if (dataListener != null) {
            // send message
            dataListener.onReceiveStatus(status);
        }
    }
}
