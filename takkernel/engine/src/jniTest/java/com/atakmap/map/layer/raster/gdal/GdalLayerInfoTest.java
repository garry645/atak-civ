
package com.atakmap.map.layer.raster.gdal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.coremap.io.MockIOProvider;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;

import gov.tak.test.KernelJniTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Set;

public class GdalLayerInfoTest extends KernelJniTest
{
    @BeforeClass
    public static void setup() {
        GdalLibrary.init();
    }

    @Before
    public void beforeTest() {
        IOProviderFactoryHelper.registerProvider(new DefaultIOProvider(), true);
    }

    @Test
    public void testNoFile() {
        MockIOProvider mockIOProvider = new MockIOProvider();
        IOProviderFactoryHelper.registerProvider(mockIOProvider, false);

        File f1 = new File("/sdcard/atak/notExist1");
        File f2 = new File("/sdcard/atak/notExist2");
        Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE
                .create(new DatasetDescriptorSpiArgs(f1, f2));
        assertNull(descs);
    }

    @Test
    public void testEmptyFile() {
        MockIOProvider mockIOProvider = new MockIOProvider();
        IOProviderFactoryHelper.registerProvider(mockIOProvider, false);

        try (FileUtils.AutoDeleteFile f1 = FileUtils.AutoDeleteFile
                .createTempFile(getTestContext());
             FileUtils.AutoDeleteFile f2 = FileUtils.AutoDeleteFile
                        .createTempFile(getTestContext())) {

            Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE
                    .create(new DatasetDescriptorSpiArgs(f1.file, f2.file));
            assertNull(descs);
        }
    }

    //XXX - revisit. this tests fails in all contexts
    /*@Test
    public void testGoodFiles() {
        // TODO create data or read existing data that is well-formed rather than temp file
        try (FileUtils.AutoDeleteFile f1 = FileUtils.AutoDeleteFile
                .createTempFile()) {
            File workingDir = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getCacheDir();
            Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE
                    .create(new DatasetDescriptorSpiArgs(f1.file, workingDir));
            assertNotNull(descs);
            // TODO create/use actual data and update the assert
        }
    }*/

}
