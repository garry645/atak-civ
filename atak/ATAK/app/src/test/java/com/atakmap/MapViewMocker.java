
package com.atakmap;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.RootMapGroup;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.map.AtakMapView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.FontMetrics;
import gov.tak.platform.marshal.MarshalManager;

/**
 * Common Class that will initialize a basic MapView
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MapViewMocker {

    private static MapView mapViewMock;
    private static final Object lock = new Object();

    /**
     * Gets a Mocked MapView class
     *
     * @return A Mocked mapview class
     */
    public MapView getMapView() {
        synchronized (lock) {

            if (mapViewMock == null) {
                MockedStatic<Log> logMockedStatic = Mockito
                        .mockStatic(android.util.Log.class);
                Mockito.mockStatic(Environment.class);
                Mockito.mockStatic(MapItem.class);
                Mockito.mockStatic(Marker.class);
                Mockito.mockStatic(NetworkUtils.class);
                Mockito.when(NetworkUtils.getIP()).thenReturn("127.0.0.1");
                Mockito.when(NetworkUtils.isMulticastAddress(Mockito.any()))
                        .thenCallRealMethod();

                Mockito.mockStatic(FileSystemUtils.class);
                File fileMock = Mockito.mock(File.class);
                Mockito.when(fileMock.getAbsolutePath()).thenReturn("filePath");
                Mockito
                        .when(FileSystemUtils
                                .isEmpty((String) ArgumentMatchers.isNull()))
                        .thenReturn(true);
                Mockito.when(
                        FileSystemUtils.getItem(ArgumentMatchers.anyString()))
                        .thenReturn(fileMock);
                Mockito.mockStatic(MapView.class);

                Context contextMock = Mockito.mock(Context.class);
                mapViewMock = Mockito.mock(MapView.class);

                Mockito.mockStatic(MarshalManager.class);

                Mockito.mockStatic(FontMetrics.class);
                Mockito.when(
                        FontMetrics.intern(ArgumentMatchers.any(Font.class)))
                        .thenReturn((FontMetrics) null);
                Mockito.mockStatic(MapTextFormat.class);

                //Mockito.spy(MapTextFormat.class);
                try {
                    //  Mockito.doNothing().when(MapTextFormat.class,"initTypefaceFamilyMap");

                    Class<?> mockServiceClass = MapTextFormat.class;
                    Method field = mockServiceClass
                            .getDeclaredMethod("initTypefaceFamilyMap");
                    field.setAccessible(true);
                    Mockito.doNothing().when(field);

                } catch (Exception ignored) {
                }

                MapEventDispatcher mockMapEventDispatcher = Mockito
                        .mock(MapEventDispatcher.class);
                Mockito.when(mapViewMock.getMapEventDispatcher())
                        .thenReturn(mockMapEventDispatcher);
                Mockito.when(mapViewMock.getContext()).thenReturn(contextMock);

                Mockito.when(
                        MarshalManager.marshal(ArgumentMatchers.any(Font.class),
                                ArgumentMatchers.eq(Font.class),
                                ArgumentMatchers.eq(Typeface.class)))
                        .thenReturn(Typeface.DEFAULT);

                Typeface t = Mockito.mock(Typeface.class);
                int i = t.getStyle();

                MapTextFormat mapTextFormat = new MapTextFormat(t, 0);
                Mockito.mockStatic(AtakMapView.class);

                Mockito.when(AtakMapView.getDefaultTextFormat())
                        .thenReturn(mapTextFormat);

                RootMapGroup rootGroupMock = Mockito.mock(RootMapGroup.class);
                Mockito.when(rootGroupMock
                        .findMapGroup(ArgumentMatchers.anyString()))
                        .thenReturn(rootGroupMock);
                Mockito.when(mapViewMock.getRootGroup())
                        .thenReturn(rootGroupMock);

                Mockito.when(MapView.getMapView()).thenReturn(mapViewMock);

                Mockito.mockStatic(AtakBroadcast.class);
                AtakBroadcast.init(Mockito.mock(Context.class));
                Mockito.when(AtakBroadcast.getInstance())
                        .thenReturn(Mockito.mock(AtakBroadcast.class));

                Mockito.mockStatic(IOProviderFactory.class);
                FileOutputStream fos = Mockito.mock(FileOutputStream.class);
                try {
                    Mockito
                            .when(IOProviderFactory
                                    .getOutputStream(ArgumentMatchers
                                            .nullable(File.class)))
                            .thenReturn(fos);
                } catch (Exception ignored) {
                }

                Mockito.mockStatic(MimeTypeMap.class);
                MimeTypeMap mimeTypeMapMock = Mockito.mock(MimeTypeMap.class);
                Mockito.when(MimeTypeMap.getSingleton())
                        .thenReturn(mimeTypeMapMock);
            }

            return mapViewMock;
        }
    }

    /**
     * Sample test for mapview to satisy the test class since there must be at least one test in the
     * class. The test passing indicates there was no issue mocking mapview
     */
    @Test
    public void testMapView() {
        MapView mapView = getMapView();
        Assert.assertNotNull(mapView);
    }
}
