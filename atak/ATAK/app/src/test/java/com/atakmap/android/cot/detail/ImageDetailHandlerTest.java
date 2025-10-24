
package com.atakmap.android.cot.detail;

import android.webkit.MimeTypeMap;

import com.atakmap.MapViewMocker;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.io.IOProviderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ImageDetailHandlerTest {

    @Test
    public void toItemMetadata() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();

        Marker m = new Marker("test");
        CotEvent cotEvent = new CotEvent();
        cotEvent.setUID("test");
        CotDetail cd = new CotDetail("image");
        ImageDetailHandler handler = new ImageDetailHandler(mapView);
        handler.toItemMetadata(m, cotEvent, cd);
    }
}
