
package com.atakmap.android.contact;

import android.net.Uri;

import com.atakmap.MapViewMocker;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.DatabaseInformation;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FilteredContactsManagerTest {

    @Test
    public void testFilteredContactManager_ATAK_16490() {
        MapViewMocker mapViewMocker = new MapViewMocker();
        mapViewMocker.getMapView();
        File fileMock = Mockito.mock(File.class);
        Mockito.when(fileMock.getAbsolutePath())
                .thenReturn("/sdcard/atak/Databases/filteredcontacts.sqlite");
        Mockito.when(fileMock.getParentFile())
                .thenReturn(new File("/sdcard/atak/Databases"));
        Mockito.mockStatic(Uri.class);
        Uri uri = Mockito.mock(Uri.class);
        Mockito.when(Uri.parse(ArgumentMatchers.anyString())).thenReturn(uri);

        Mockito.when(Uri.fromFile(fileMock))
                .thenReturn(uri);
        Mockito.when(uri.getScheme()).thenReturn("memory");

        Mockito.when(FileSystemUtils.getItem(ArgumentMatchers.anyString()))
                .thenReturn(fileMock);

        DatabaseIface dbImpl = Mockito.mock(DatabaseIface.class);
        CursorIface cursorIface = Mockito.mock(CursorIface.class);
        Mockito
                .when(dbImpl.query(ArgumentMatchers.anyString(),
                        ArgumentMatchers.nullable(String[].class)))
                .thenReturn(cursorIface);
        Mockito
                .when(IOProviderFactory.createDatabase(
                        ArgumentMatchers.any(DatabaseInformation.class)))
                .thenReturn(dbImpl);

        FilteredContactsManager fcm = FilteredContactsManager.getInstance();
        fcm.isContactFiltered((Contact) null);
    }

}
