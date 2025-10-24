
package com.atakmap.map.layer.feature;

import static org.junit.Assert.assertTrue;

import gov.tak.test.KernelJniTest;
import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.coremap.io.DatabaseInformation;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.coremap.io.MockProvider;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.control.DataSourceDataStoreControl;
import com.atakmap.map.layer.feature.Adapters;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

public class PersistentDataSourceFeatureDataStoreTest
        extends KernelJniTest {

    public PersistentDataSourceFeatureDataStoreTest(){}
    @Test
    public void constructor_invokes_ioprovider() throws IOException {
        final boolean[] invoked = {
                false
        };
        final IOProvider provider = new MockProvider("provider",
                null) {
            @Override
            public DatabaseIface createDatabase(DatabaseInformation info) {
                invoked[0] = true;
                return Databases.openOrCreateDatabase(info.uri.getPath());
            }
        };
        IOProviderFactoryHelper.registerProvider(provider, false);
        try {
            try (FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile
                    .createTempFile(getTestContext()))
            {
                f.file.delete();
                f.file.mkdirs();
                PersistentDataSourceFeatureDataStore2 fds = new PersistentDataSourceFeatureDataStore2(
                        f.file);
                assertTrue(invoked[0]);
                fds.dispose();
            }
        } finally {
        }
    }

    @Test
    public void custom_io_reopen() throws IOException {
        final boolean[] invoked = {
                false
        };
        final IOProvider provider = new MockProvider("provider",
                null) {
            @Override
            public DatabaseIface createDatabase(DatabaseInformation info) {
                invoked[0] = true;
                return Databases.openOrCreateDatabase(info.uri.getPath());
            }
        };
        IOProviderFactoryHelper.registerProvider(provider, false);
        try {
            try (FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile
                    .createTempFile(getTestContext())) {
                f.file.delete();

                PersistentDataSourceFeatureDataStore2 fds;
                fds = new PersistentDataSourceFeatureDataStore2(f.file);
                assertTrue(invoked[0]);
                fds.dispose();

                // reopen
                invoked[0] = false;
                fds = new PersistentDataSourceFeatureDataStore2(f.file);
                assertTrue(invoked[0]);
                fds.dispose();
            }
        } finally {
        }
    }

    @Test
    public void datasource_access_through_adapter()
    {
        try (FileUtils.AutoDeleteFile f = FileUtils.AutoDeleteFile
                .createTempFile(getTestContext()))
        {
            f.file.delete();
            f.file.mkdirs();
            PersistentDataSourceFeatureDataStore2 fds = new PersistentDataSourceFeatureDataStore2(
                    f.file);
            FeatureDataStore2 adapted = Adapters.adapt(fds);
            Assert.assertTrue(adapted instanceof Controls);
            DataSourceDataStoreControl ctrl = ((Controls)adapted).getControl(DataSourceDataStoreControl.class);
            Assert.assertNotNull(ctrl);
            fds.dispose();
        }
    }
}
