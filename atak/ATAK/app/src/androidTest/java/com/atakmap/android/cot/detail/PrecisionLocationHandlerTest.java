
package com.atakmap.android.cot.detail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PrecisionLocationHandlerTest extends ATAKInstrumentedTest {

    @Test
    public void toItemMetadata() {
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("precisionlocation");
        cd.setAttribute(GeoPointMetaData.GEOPOINT_SOURCE, "apple");
        cd.setAttribute(GeoPointMetaData.ALTITUDE_SOURCE, "banana");
        CotDetail detail = new CotDetail("detail");
        detail.addChild(cd);
        cotEvent.setDetail(detail);
        PrecisionLocationHandler handler = new PrecisionLocationHandler();
        handler.toItemMetadata(m, cotEvent, cd);
        GeoPointMetaData gpmd = m.getGeoPointMetaData();
        Assert.assertEquals("apple", gpmd.getGeopointSource());
        Assert.assertEquals("banana", gpmd.getAltitudeSource());
    }
}
