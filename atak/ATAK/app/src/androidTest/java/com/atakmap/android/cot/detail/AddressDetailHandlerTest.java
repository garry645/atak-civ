
package com.atakmap.android.cot.detail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AddressDetailHandlerTest extends ATAKInstrumentedTest {

    private final String ADDRESS = "1600 Pennsylvania Avenue NW, Washington, DC 20500";

    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("address");
        cd.setAttribute("text", ADDRESS);
        cd.setAttribute("geocoder", "android");
        cd.setAttribute("time", CoordinatedTime.toCot(new CoordinatedTime()));
        AddressDetailHandler handler = new AddressDetailHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        Assert.assertEquals(ADDRESS, m.getMetaString("address_text", null));
    }

}
