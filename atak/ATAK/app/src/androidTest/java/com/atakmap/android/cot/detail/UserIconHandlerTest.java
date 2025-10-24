
package com.atakmap.android.cot.detail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserIconHandlerTest extends ATAKInstrumentedTest {
    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("iconset");
        cd.setAttribute("iconsetpath", "abcdabcd");
        UserIconHandler handler = new UserIconHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        Assert.assertEquals("abcdabcd",
                m.getMetaString(UserIcon.IconsetPath, null));
    }

}
