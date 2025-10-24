package gov.tak.api.video;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionEntry extends ConnectionEntryBase implements Parcelable
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

    private ConnectionEntry(Parcel in) {
        List<ConnectionEntry> children = null;
        setAddress(in.readString());
        setAlias(in.readString());
        setBufferTime(in.readInt());
        final int numChildren = in.readInt();
        if(numChildren >= 0) {
            children = new ArrayList<>(numChildren);
            for(int i = 0; i < numChildren; i++)
                children.add((ConnectionEntry) in.readParcelable(getClass().getClassLoader()));
        }
        setIgnoreEmbeddedKLV(in.readInt() != 0);
        setLocalFile((File)in.readSerializable());
        setNetworkTimeout(in.readInt());
        // parent UID will be adopted when `this` is added a child on the
        // parent when the stack unwinds
        final String parentUID = in.readString();
        setPassphrase(in.readString());
        setPath(in.readString());
        setPort(in.readInt());
        setPreferredInterfaceAddress(in.readString());
        setProtocol(Protocol.valueOf(in.readString()));
        setRoverPort(in.readInt());
        setRtspReliable(in.readInt());
        setUID(in.readString());
        setSource(Source.valueOf(in.readString()));
        setTemporary(in.readInt() != 0);

        // children are set last as that is the mechanism by which parent UID is set
        setChildren(children);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        final List<ConnectionEntry> children = getChildren();
        out.writeString(getAddress());
        out.writeString(getAlias());
        out.writeInt(getBufferTime());
        out.writeInt(children != null ? children.size() : -1);
        if(children != null) {
            for(ConnectionEntry ce : children)
                out.writeParcelable(ce, 0);
        }
        out.writeInt(getIgnoreEmbeddedKLV() ? 1 : 0);
        out.writeSerializable(getLocalFile());
        out.writeInt(getNetworkTimeout());
        out.writeString(getParentUID());
        out.writeString(getPassphrase());
        out.writeString(getPath());
        out.writeInt(getPort());
        out.writeString(getPreferredInterfaceAddress());
        out.writeString(getProtocol().name());
        out.writeInt(getRoverPort());
        out.writeInt(getRtspReliable());
        out.writeString(getUID());
        out.writeString(getSource().name());
        out.writeInt(isTemporary() ? 1 : 0);
    }

    public static final Parcelable.Creator<ConnectionEntry> CREATOR = new Parcelable.Creator<ConnectionEntry>()
    {
        @Override
        public ConnectionEntry createFromParcel(Parcel p)
        {
            return new ConnectionEntry(p);
        }

        @Override
        public ConnectionEntry[] newArray(int count)
        {
            return new ConnectionEntry[count];
        }
    };
}
