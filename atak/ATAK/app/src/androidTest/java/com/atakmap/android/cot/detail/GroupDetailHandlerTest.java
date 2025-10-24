
package com.atakmap.android.cot.detail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import gov.tak.api.cot.detail.GroupDetailHandler;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GroupDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("group");
        cd.setAttribute("name", "red");
        cd.setAttribute("role", "sniper");
        GroupDetailHandler gdh = new GroupDetailHandler();
        CotDetailHandler.toItemMetadata(gdh, m, cotEvent, cd);
        Assert.assertEquals("Red", m.getMetaString("team", null));
        Assert.assertEquals("sniper", m.getMetaString("atakRoleType", null));
    }
}
