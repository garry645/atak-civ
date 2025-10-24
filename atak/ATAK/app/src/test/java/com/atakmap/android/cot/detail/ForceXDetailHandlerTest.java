
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
public class ForceXDetailHandlerTest {

    @Test
    public void toItemMetadata() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();

        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("forcex");
        ForceXDetailHandler handler = new ForceXDetailHandler(mapView);
        handler.toItemMetadata(m, cotEvent, cd);

    }
}
