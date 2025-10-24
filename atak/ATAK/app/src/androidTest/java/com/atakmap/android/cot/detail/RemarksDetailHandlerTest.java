
package com.atakmap.android.cot.detail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RemarksDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("remarks");
        cd.setInnerText("abcd\nabcd");
        RemarksDetailHandler handler = new RemarksDetailHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        Assert.assertEquals("abcd\nabcd",
                m.getMetaString("remarks", null));
    }

}
