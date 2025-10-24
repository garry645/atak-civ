package gov.tak.api.video;

import android.os.Parcel;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelTest;

public class ConnectionEntryAndroidTest extends KernelTest {
    @Test
    public void parcel_roundtrip()
    {
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

        Parcel p = Parcel.obtain();
        entry.writeToParcel(p, 0);
        p.setDataPosition(0);
        final ConnectionEntry parsed = ConnectionEntry.CREATOR.createFromParcel(p);

        Assert.assertEquals(entry, parsed);
    }
}
