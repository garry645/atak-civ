
package com.atakmap.net;

import android.content.Context;

import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;

import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AtakAuthenticationDatabaseTest extends KernelJniTest
{

    @Test
    public void storeAndRetrieve() {
        Context appContext = getTestContext();

        if(AtakAuthenticationDatabase.initialized)
            AtakAuthenticationDatabase.dispose();

        AtakAuthenticationCredentials aac = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.test");
        assertNull(aac);

        AtakAuthenticationDatabase.initialize(appContext);
        AtakAuthenticationDatabase.clear();
        AtakAuthenticationDatabase.dispose();

        AtakAuthenticationDatabase.saveCredentials(
                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                "com.atakmap.app.test", "apple", "core",
                false);

        aac = AtakAuthenticationDatabase.getCredentials(
                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                "com.atakmap.app.test");
        assertNull(aac);

        AtakAuthenticationDatabase.initialize(appContext);

        AtakAuthenticationDatabase.saveCredentials(
                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                "com.atakmap.app.test", "apple", "core",
                false);

        aac = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.test");
        Assert.assertNotNull(aac);
        assertEquals("core", aac.password);
        assertEquals("apple", aac.username);

        AtakAuthenticationCredentials aac1 = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.test1");
        assertNull(aac1);

        AtakAuthenticationDatabase.saveCredentials(
                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                "com.atakmap.app.test", "apple1", "core1",
                false);

        AtakAuthenticationCredentials aac2 = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.test");
        assertNotNull(aac2);
        assertNotEquals("core", aac2.password);
        assertNotEquals("apple", aac2.username);

    }

}
