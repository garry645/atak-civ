
package com.atakmap.android.cot.detail;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TracksDetailHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {
        AtakBroadcast.init(ApplicationProvider.getApplicationContext());
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("__bread_crumbs");
        cd.setAttribute("enabled", Boolean.toString(true));
        TracksDetailHandler handler = new TracksDetailHandler();
        handler.toItemMetadata(m, cotEvent, cd);

    }
}
