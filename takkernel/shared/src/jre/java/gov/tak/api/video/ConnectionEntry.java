package gov.tak.api.video;

import java.io.File;

public final class ConnectionEntry extends ConnectionEntryBase
{
    /**
     * Construct a connection entry that describes all of the fields that could potentially be
     * stored to describe a video source.
     *
     * @param alias a human readable alias for the video stream.
     * @param address the IPv4 address for the video stream.
     * @param port the port used for the specified video stream.
     * @param roverPort UDP sideband metadata traffic from a L3 Com Video Downlink Receiver, provide the
     *        additional rover port, -1 if there is no data.
     * @param path for rtsp and http, provide the path to use when connecting to the stream.
     * @param protocol the protocol for the video stream.
     * @param networkTimeout as specified in milliseconds (should be 5000).
     * @param bufferTime as specified in milliseconds (should be -1 unless required).
     * @param passphrase passphrase to access the source (empty string if not needed)
     * @param source the source of the saved data, either LOCAL_STORAGE or EXTERNAL (by default
     *            should be LOCAL_STORAGE).
     */
    public ConnectionEntry(final String alias,
                           final String address,
                           final int port,
                           final int roverPort,
                           final String path,
                           final Protocol protocol,
                           final int networkTimeout,
                           final int bufferTime,
                           final int rtspReliable,
                           final String passphrase,
                           final Source source) {
        super(alias, address, port, roverPort, path, protocol, networkTimeout, bufferTime, rtspReliable, passphrase, source);
    }

    /**
     * Construct a connection entry based on the file provided.
     * @param f the file
     */
    public ConnectionEntry(File f) {
        super(f);
    }

    /**
     * Creates a connection entry based on a provided uri.
     * @param alias the alias that is presented to the user that describes the connection entry
     * @param uri the uri to turn into a connection entry.
     */
    public ConnectionEntry(String alias, String uri) {
        super(alias, uri);
    }

    /**
     * Produces a completely undefined ConnectionEntry, a minimum set of values would need to be set
     * before this can even be used. When capable use the full constructor for the connection entry.
     */
    public ConnectionEntry()
    {
        super();
    }
}
