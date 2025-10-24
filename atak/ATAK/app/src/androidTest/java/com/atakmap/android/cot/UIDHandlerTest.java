
package com.atakmap.android.cot;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UIDHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void uidHandlerTest() {
        Marker m = new Marker("test");
        m.setType("self");
        m.setTitle("ace");
        CotDetail cd = new CotDetail("details");
        UIDHandler handler = new UIDHandler();
        handler.toCotDetail(m, cd);
        Assert.assertEquals("ace", cd.getChild("uid").getAttribute("Droid"));

    }
}
