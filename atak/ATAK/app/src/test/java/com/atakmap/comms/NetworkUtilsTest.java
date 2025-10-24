
package com.atakmap.comms;

import org.junit.Assert;
import org.junit.Test;

public class NetworkUtilsTest {

    @Test
    public void isValidMulticast() {
        Assert.assertTrue(NetworkUtils.isMulticastAddress("239.255.0.1"));
        Assert.assertFalse(NetworkUtils.isMulticastAddress("255.255.255.0"));
        Assert.assertFalse(NetworkUtils.isMulticastAddress("192.168.1.1"));
        Assert.assertTrue(NetworkUtils.isMulticastAddress("224.0.0.0"));
        Assert.assertFalse(NetworkUtils.isMulticastAddress("254.1.1.1"));
        Assert.assertFalse(NetworkUtils.isMulticastAddress("300.1.1.1"));
    }
}
