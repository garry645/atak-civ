
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
public class TrackDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("track");
        cd.setAttribute("course", "30.2");
        cd.setAttribute("speed", "100");
        TrackDetailHandler handler = new TrackDetailHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        Assert.assertEquals(100, m.getTrackSpeed(), .0001);
        Assert.assertEquals(30.2, m.getTrackHeading(), .0001);

    }

}
