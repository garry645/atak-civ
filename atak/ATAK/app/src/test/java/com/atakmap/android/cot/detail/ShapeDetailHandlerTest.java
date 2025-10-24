
package com.atakmap.android.cot.detail;

import com.atakmap.MapViewMocker;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ShapeDetailHandlerTest {

    @Test
    public void toItemMetadata() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();
        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        cotEvent.setUID("test");
        CotDetail cd = new CotDetail("shape");
        CotDetail ellipse = new CotDetail("ellispe");
        ellipse.setAttribute("major", "5.0");
        ellipse.setAttribute("minor", "3.0");
        ellipse.setAttribute("angle", "90");
        cd.addChild(ellipse);
        ShapeDetailHandler handler = new ShapeDetailHandler(mapView);
        handler.toItemMetadata(m, cotEvent, cd);

    }
}
