
package com.atakmap.android.cot.detail;

import com.atakmap.MapViewMocker;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.tak.api.engine.map.coords.GeoCalculations;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CircleDetailHandlerTest {

    @Test
    public void toItemMetadata() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();
        Mockito.mockStatic(GeoCalculations.class);
        com.atakmap.coremap.maps.coords.GeoPoint geopointMock = Mockito
                .mock(com.atakmap.coremap.maps.coords.GeoPoint.class);
        Mockito.when(
                com.atakmap.coremap.maps.coords.GeoCalculations.pointAtDistance(
                        ArgumentMatchers.any(
                                com.atakmap.coremap.maps.coords.GeoPoint.class),
                        ArgumentMatchers.any(Double.class),
                        ArgumentMatchers.any(Double.class)))
                .thenReturn(geopointMock);

        DrawingCircle circle = new DrawingCircle(mapView);
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("circle");
        cd.setAttribute("numRings", "3");
        cd.setAttribute("radius", "100");
        CircleDetailHandler handler = new CircleDetailHandler(mapView);
        handler.toItemMetadata(circle, cotEvent, cd);
        Assert.assertEquals(3, circle.getNumRings());
        Assert.assertEquals(100f, circle.getRadius(), 0.001);

    }
}
