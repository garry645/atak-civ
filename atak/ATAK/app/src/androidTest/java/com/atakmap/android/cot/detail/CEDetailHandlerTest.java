
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
public class CEDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {

        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("ce_human_input");
        cd.setInnerText("true");
        CEDetailHandler handler = new CEDetailHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        Assert.assertTrue(m.hasMetaValue("ce_human_input"));

    }
}
