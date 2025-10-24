
package com.atakmap.android.video;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.video.export.VideoExportWrapper;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.annotations.FortifyFinding;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.util.Disposable;

/** @deprecated use {@link gov.tak.api.video.ConnectionEntry} */
@Deprecated
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public final class ConnectionEntry
        implements Serializable, Disposable, Exportable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "ConnectionEntry";

    public static final String EXTRA_CONNECTION_ENTRY = "CONNECTION_ENTRY";

    public enum Source {
        LOCAL_STORAGE,
        EXTERNAL;

        public static Source marshal(
                gov.tak.api.video.ConnectionEntry.Source other) {
            if (other == null)
                return null;
            switch (other) {
                case EXTERNAL:
                    return EXTERNAL;
                case LOCAL_STORAGE:
                    return LOCAL_STORAGE;
                default:
                    return LOCAL_STORAGE;
            }
        }

        public static gov.tak.api.video.ConnectionEntry.Source marshal(
                Source other) {
            if (other == null)
                return null;
            switch (other) {
                case EXTERNAL:
                    return gov.tak.api.video.ConnectionEntry.Source.EXTERNAL;
                case LOCAL_STORAGE:
                    return gov.tak.api.video.ConnectionEntry.Source.LOCAL_STORAGE;
                default:
                    return gov.tak.api.video.ConnectionEntry.Source.LOCAL_STORAGE;
            }
        }
    }

    public enum Protocol {
        RAW("raw"),
        UDP("udp"),
        RTSP("rtsp", 554),
        RTMP("rtmp", 1935),
        RTMPS("rtmps", 443),
        HTTP("http", 80),
        HTTPS("https", 443),
        FILE("file"),
        TCP("tcp"),
        RTP("rtp"),
        DIRECTORY("dir"),
        SRT("srt");

        private final String proto;
        private final int defaultPort;

        Protocol(final String proto) {
            this(proto, -1);
        }

        Protocol(final String proto, final int defaultPort) {
            this.proto = proto;
            this.defaultPort = defaultPort;
        }

        @NonNull
        public String toString() {
            return proto;
        }

        public String toURL() {
            return proto + "://";
        }

        public int getDefaultPort() {
            return this.defaultPort;
        }

        /**
         * Turns a string representation into a Protocol.
         */
        public static Protocol fromString(final String proto) {
            //            Log.d(TAG, "looking up: " + proto);
            for (Protocol p : Protocol.values()) {
                if (p.proto.equalsIgnoreCase(proto)) {
                    return p;
                }
            }
            android.util.Log.d(TAG, "using raw for: " + proto);
            return Protocol.RAW;
        }

        /**
         * Given a URL return the protocol that is represented
         */
        public static Protocol getProtocol(final String url) {
            if (url == null)
                return null;

            for (Protocol p : Protocol.values()) {
                if (url.startsWith(p.toURL())) {
                    return p;
                }
            }
            return Protocol.RAW;

        }

        public static Protocol marshal(
                gov.tak.api.video.ConnectionEntry.Protocol other) {
            return (other != null) ? fromString(other.toString()) : null;
        }

        public static gov.tak.api.video.ConnectionEntry.Protocol marshal(
                Protocol other) {
            return (other != null)
                    ? gov.tak.api.video.ConnectionEntry.Protocol
                            .fromString(other.toString())
                    : null;
        }
    }

    private gov.tak.api.video.ConnectionEntry impl;

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

        this(new gov.tak.api.video.ConnectionEntry(
                alias,
                address,
                port,
                roverPort,
                path,
                Protocol.marshal(protocol),
                networkTimeout,
                bufferTime,
                rtspReliable,
                passphrase,
                Source.marshal(source)));
    }

    /**
     * Construct a connection entry based on the file provided.
     * @param f the file
     */
    public ConnectionEntry(File f) {
        this(new gov.tak.api.video.ConnectionEntry(f));
    }

    /**
     * Creates a connection entry based on a provided uri.
     * @param alias the alias that is presented to the user that describes the connection entry
     * @param uri the uri to turn into a connection entry.
     */
    public ConnectionEntry(String alias, String uri) {
        this(new gov.tak.api.video.ConnectionEntry(alias, uri));
    }

    /**
     * Produces a completely undefined ConnectionEntry, a minimum set of values would need to be set
     * before this can even be used. When capable use the full constructor for the connection entry.
     */
    public ConnectionEntry() {
        this(new gov.tak.api.video.ConnectionEntry());
    }

    public ConnectionEntry(gov.tak.api.video.ConnectionEntry other) {
        this.impl = other;
    }

    /**
     * Construct a copy of the Connection Entry for using by the alternative video player and also
     * for use by the media processor so that the connection entry - if modified does not get messed
     * up.  This copy will have a different unique identifier.
     * @return a valid copy of the connection entry with a different unique identifier.
     */
    public ConnectionEntry copy() {
        ConnectionEntry retval = new ConnectionEntry();
        retval.copy(this);
        retval.setUID(retval.getUID());
        return retval;
    }

    /**
     * Constructs an exact copy of the ConnectionEntry and even contains the same uid.
     * @param from the connection entry to copy the details from.
     */
    public void copy(final ConnectionEntry from) {
        impl.copy(from.impl);
    }

    /**
     * If the connection entry contains embedded klv, ignore it if the flag is true.
     * @return ignore embedded klv if the flag is true.
     */
    public boolean getIgnoreEmbeddedKLV() {
        return impl.getIgnoreEmbeddedKLV();
    }

    /**
     * If the video stream contains metadata, set this flag to hint to the player to ignore any
     * klv processing.
     * @param state
     */
    public void setIgnoreEmbeddedKLV(final boolean state) {
        impl.setIgnoreEmbeddedKLV(state);
    }

    /**
     * Get the port for any sideband metadata - this would be KLV encoded metadata not carried in
     * the same udp stream as the video.
     * @return the port that contains the klv
     */
    public int getRoverPort() {
        return impl.getRoverPort();
    }

    /**
     * Set the port for any sideband metadata - this would be KLV encoded metadata not carried in
     * the same udp stream as the video.
     * @param port the port that contains the klv
     */
    public void setRoverPort(final int port) {
        impl.setRoverPort(port);
    }

    /**
     * The alias assigned to this connection entry.
     * @return the alias otherwise the empty string.
     */
    public String getAlias() {
        return impl.getAlias();
    }

    /**
     * Sets the alias for the connection entry.
     * @param alias the alias to set, if null is passed in a call to getAlias will return the
     *              empty string.
     */
    public void setAlias(final String alias) {
        impl.setAlias(alias);
    }

    /**
     * Get the UID for the alias. It is recommended but not required that this be a universally
     * unique identifer from the class UUID.
     * @return the UID for the alias.
     */
    public String getUID() {
        return impl.getUID();
    }

    /**
     * Set the UID for the alias. It is recommended but not required that this be a universally
     * unique identifer from the class UUID.
     * @param uid the unique identifier
     */
    public void setUID(final String uid) {
        impl.setUID(uid);
    }

    /**
     * Get the address associated with the alias.
     * @return the address for the Connection Entry.
     */
    public String getAddress() {
        return impl.getAddress();
    }

    /**
     * Get the address associated with the alias.
     * @return the address for the Connection Entry.
     */
    @FortifyFinding(finding = "Password Management: Hardcoded Password", rational = "XXHiddenXX is a UI password mask and not a password")
    public String getAddress(boolean forDisplay) {
        return impl.getAddress(forDisplay);
    }

    /**
     * Sets the address associated with the alias.
     * @param address the address for the Connection Entry.
     */
    public void setAddress(final String address) {
        impl.setAddress(address);
    }

    /**
     * Sets the preferred ip address for the interface to use.   This is not the ip address
     * associated with the video.   Null if using the system level preference for the video
     * traffic.    This is specific for multicast video traffic coming in from a single
     * radio.
     */
    public void setPreferredInterfaceAddress(String preferredInterfaceAddress) {
        impl.setPreferredInterfaceAddress(preferredInterfaceAddress);
    }

    /**
     * Gets the preferred ip address for the interface to use.
     * @return the ip address for the interface to use for the video traffic.   This is
     * primarily for multicast video traffic.
     */
    public String getPreferredInterfaceAddress() {
        return impl.getPreferredInterfaceAddress();
    }

    /**
     * The port for the video connection entry
     * @return the port for the entry
     */
    public int getPort() {
        return impl.getPort();
    }

    /**
     * The port for the video connection entry
     * @param port the port for the entry
     */
    public void setPort(final int port) {
        impl.setPort(port);
    }

    /**
     * The path for the video connection entry
     * @return the path for the entry
     */
    public String getPath() {
        return impl.getPath();
    }

    /**
     * Set the path for the connection entry
     * @param path the path
     */
    public void setPath(final String path) {
        impl.setPath(path);

    }

    /**
     * The protocol associated with the connection entry
     * @return the protocol described by the connection entry.
     */
    public Protocol getProtocol() {
        return Protocol.marshal(impl.getProtocol());
    }

    /**
     * Sets the protocol associated with the connection entry
     * @param protocol the protocol for the connection entry.
     */
    public void setProtocol(Protocol protocol) {
        impl.setProtocol(Protocol.marshal(protocol));
    }

    /**
     * Get the network timeout associated with the connection entry.
     * @return the network timeout in milliseconds
     */
    public int getNetworkTimeout() {
        return impl.getNetworkTimeout();
    }

    /**
     * Set the network timeout associated with the connection entry.
     * @param networkTimeout the network timeout in milliseconds
     */
    public void setNetworkTimeout(final int networkTimeout) {
        impl.setNetworkTimeout(networkTimeout);
    }

    /**
     * Get the time used to buffer an incoming stream.
     * @return the time used to buffer an incoming stream.
     */
    public int getBufferTime() {
        return impl.getBufferTime();
    }

    /**
     * Set the time used to buffer an incoming stream
     * @param bufferTime the time in milliseconds to buffer
     */
    public void setBufferTime(final int bufferTime) {
        impl.setBufferTime(bufferTime);
    }

    /**
     * Returns 1 if rtsp should be force negotiated as reliable (TCP).
     * @return 1 if the rtsp stream negotiation should be TCP
     */
    public int getRtspReliable() {
        return impl.getRtspReliable();
    }

    /**
     * Set 1 if rtsp should be force negotiated as reliable (TCP).
     * @param rtspReliable setting it to 1 will force RTSP to attempt to negotiate a TCP stream
     */
    public void setRtspReliable(final int rtspReliable) {
        impl.setRtspReliable(rtspReliable);
    }

    /**
     * Set to non-empty string if a passphrase is used to access the video
     * @param pass passphrase to access the video or empty string if not needed
     */
    public void setPassphrase(String pass) {
        impl.setPassphrase(pass);
    }

    /**
     * Returns non-empty string if one is to be used to access the video source.
     * @return passphrase for the source if used, else empty string
     */
    @FortifyFinding(finding = "Privacy Violation", rational = "according to the Fortify flow stack where this is leakage indicated, the password will never print")
    public String getPassphrase() {
        return impl.getPassphrase();
    }

    /**
     * Gets the source of the Connection Entry (Local or External) used to make sure that it is not
     * persisted incorrectly if it came from the removable card.
     * @return the source of the entry
     */
    public Source getSource() {
        return Source.marshal(impl.getSource());
    }

    /**
     * Sets the source of the connection entry.  This is used primarily when reading the connection
     * entry as it was persisted.
     * @param source the source.
     */
    public void setSource(final Source source) {
        impl.setSource(Source.marshal(source));
    }

    /**
     * Convenience method for checking if a connection entry is remote based
     * on its protocol
     *
     * @return True if remote connection entry
     */
    public boolean isRemote() {
        return impl.isRemote();
    }

    /**
     * Set list of children entries (only applies to DIRECTORY entries)
     *
     * @param c List of connection entries
     */
    public void setChildren(List<ConnectionEntry> c) {
        List<gov.tak.api.video.ConnectionEntry> children = null;
        if (c != null) {
            children = new ArrayList<>(c.size());
            for (ConnectionEntry e : c) {
                // XXX - legacy deferred child to VideoManager
                gov.tak.api.video.ConnectionEntry child = VideoManager
                        .getInstance().getConnectionEntry(e.getUID());
                if (child == null)
                    child = e.impl;
                children.add(child);
            }
        }
        impl.setChildren(children);
    }

    /**
     * Get children entries (only applies to DIRECTORY entries)
     * @return List of children entries or null if N/A
     */
    public List<ConnectionEntry> getChildren() {
        List<gov.tak.api.video.ConnectionEntry> c = impl.getChildren();
        if (c == null)
            return null;
        Set<String> uids = new HashSet<>();
        for (gov.tak.api.video.ConnectionEntry e : c)
            uids.add(e.getUID());
        return VideoManager.getInstance().getEntries(uids);
    }

    /**
     * Get the parent directory entry
     * @return Parent entry or null if none
     */
    public String getParentUID() {
        return impl.getParentUID();
    }

    /**
     * Called once this entry is removed from the manager and not to be used
     * from this point on
     */
    @Override
    public void dispose() {
        // no-op; handled by impl
    }

    /**
     * Set the associated local file for this entry
     * @param file Local file
     */
    public void setLocalFile(File file) {
        impl.setLocalFile(file);
    }

    public File getLocalFile() {
        return impl.getLocalFile();
    }

    /**
     * Set whether this video alias is temporary and should not be persisted
     * @param temp Temporary
     */
    public void setTemporary(boolean temp) {
        impl.setTemporary(temp);
    }

    /**
     * Is the video alias considered temporary.
     * @return if the video alias is considered temporary
     */
    public boolean isTemporary() {
        return impl.isTemporary();
    }

    public gov.tak.api.video.ConnectionEntry get() {
        return impl;
    }

    /**
     * Provide the legacy call to getURL which will show all of the sensitive bits and pieces.
     */
    public static String getURL(ConnectionEntry ce) {
        return getURL(ce, false);
    }

    /**
     * Given a connection entry, produces a well formed URL.
     * @param forDisplay true if URL will be displayed or logged or used in some way other than
     *                   an actual connection to the media.  This results in sensitive parts of the
     *                   URL being obscured.
     */
    @FortifyFinding(finding = "Password Management: Hardcoded Password", rational = "XXHiddenXX is a UI password mask and not a password. 'passphrase' is not a hardcoded password, rather an attribute/field name")
    public static String getURL(ConnectionEntry ce, boolean forDisplay) {

        if (ce == null)
            return null;

        return gov.tak.api.video.ConnectionEntry.getURL(ce.get(), forDisplay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final ConnectionEntry that = (ConnectionEntry) o;
        return impl.equals(that.impl);
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return impl.toString();
    }

    /**
     * Parses out a username and password combination from the provided
     * address or url
     * @param address the address of url
     * @return an array of length 3, with the username being in position 0,
     * password being in position 1 and the address or rest of the url being
     * in position 2.
     */
    public static String[] getUserPassIp(String address) {
        return gov.tak.api.video.ConnectionEntry.getUserPassIp(address);
    }

    @Override
    public boolean isSupported(Class<?> target) {
        return VideoExportWrapper.class.equals(target) && isValidExport();
    }

    @Override
    public Object toObjectOf(Class<?> target, ExportFilters filters)
            throws FormatNotSupportedException {
        if (!isValidExport())
            return null;

        if (VideoExportWrapper.class.equals(target)) {
            return new VideoExportWrapper(this);
        }

        return null;
    }

    private boolean isValidExport() {
        return FileSystemUtils.isFile(this.getLocalFile());
    }

    // explicitly implement serializable to enforce implementation binding

    static void writeUnicode(ObjectOutputStream out, String s)
            throws IOException {
        out.writeInt((s != null) ? s.length() : -1);
        if (s != null) {
            for (int i = 0; i < s.length(); i++)
                out.writeChar(s.charAt(i));
        }
    }

    static String readUnicode(ObjectInputStream in) throws IOException {
        final int length = in.readInt();
        if (length < 0)
            return null;
        else if (length == 0)
            return new String();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++)
            chars[i] = in.readChar();
        return new String(chars);
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        Parcel p = Parcel.obtain();
        impl.writeToParcel(p, 0);
        final byte[] parcelData = p.marshall();
        p.recycle();
        out.writeInt(parcelData.length);
        out.write(parcelData);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        final int len = in.readInt();
        if (len <= 0) {
            android.util.Log.e(TAG, "Bad parcel len=" + len);
            impl = new gov.tak.api.video.ConnectionEntry();
            return;
        }
        final byte[] parcelData = new byte[len];
        in.read(parcelData);

        Parcel p = Parcel.obtain();
        p.unmarshall(parcelData, 0, parcelData.length);
        p.setDataPosition(0);
        final gov.tak.api.video.ConnectionEntry entry = gov.tak.api.video.ConnectionEntry.CREATOR
                .createFromParcel(p);
        if (this.impl != null)
            this.impl.copy(entry);
        else
            this.impl = entry;
    }

    private void readObjectNoData() throws ObjectStreamException {
        // no-op
    }
}
