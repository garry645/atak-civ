
package com.atakmap.android.cot.detail;

import com.atakmap.MapViewMocker;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LinkDetailHandlerTest {

    @Test
    public void toItemMetadata() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("link");
        cd.setAttribute("relation", "r-u");
        cd.setAttribute("url", "https://google.com");
        cd.setAttribute("uid", "test");
        LinkDetailHandler handler = new LinkDetailHandler(mapView);
        handler.toItemMetadata(m, cotEvent, cd);
        ArrayList<String> associated = m
                .getMetaStringArrayList(UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);
        Assert.assertNotNull(associated);
        Assert.assertEquals(1, associated.size());
        UrlLinkDetailEntry entry = UrlLinkDetailEntry
                .fromStringRepresentation(associated.get(0));
        Assert.assertEquals("https://google.com", entry.getUrl());

    }
}
