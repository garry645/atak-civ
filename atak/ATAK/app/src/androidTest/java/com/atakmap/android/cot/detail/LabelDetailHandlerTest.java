
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
public class LabelDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void labelDetailHandlerTest() {
        LabelDetailHandler handler = new LabelDetailHandler();
        Marker m = new Marker("uid");
        m.setMetaBoolean("hideLabel", true);
        CotEvent ce = new CotEvent();
        CotDetail cotDetail = new CotDetail("details");
        handler.toCotDetail(m, ce, cotDetail);
        Assert.assertNotNull(cotDetail.getChild("hideLabel"));
    }
}
