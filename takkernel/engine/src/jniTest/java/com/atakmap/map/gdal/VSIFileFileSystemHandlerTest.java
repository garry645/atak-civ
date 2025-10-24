
package com.atakmap.map.gdal;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.io.IOProviderFactoryHelper;

import gov.tak.test.KernelJniTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

public class VSIFileFileSystemHandlerTest extends KernelJniTest
{
    public VSIFileFileSystemHandlerTest()
    {
        super();
        GdalLibrary.init();
    }

    @Test
    public void testRoundtripRelativePath() {
        final DebugFileIOProvider dbgprovider = new DebugFileIOProvider(
                new DefaultIOProvider());
        IOProviderFactoryHelper.registerProvider(dbgprovider, false);
        try {
            final String relativePath = "relative/path/to/file.dat";
            org.gdal.gdal.gdal
                    .Open(VSIFileFileSystemHandler.PREFIX + relativePath);

            ArrayList<DebugFileIOProvider.InvocationDebugRecord> records = new ArrayList<>();
            dbgprovider.getInvocationRecord(records);
            final DebugFileIOProvider.InvocationDebugRecord expectedRecord = new DebugFileIOProvider.InvocationDebugRecord();
            expectedRecord.methodName = "getChannel";
            expectedRecord.parameters.put("f", new File(relativePath));
            expectedRecord.parameters.put("mode", "r");

            assertTrue(records.contains(expectedRecord));
        } finally {
        }
    }

    @Test
    public void testRoundtripAbsolutePath() {
        final DebugFileIOProvider dbgprovider = new DebugFileIOProvider(
                new DefaultIOProvider());
        IOProviderFactoryHelper.registerProvider(dbgprovider, false);
        try {
            final String absolutePath = "/absolute/path/to/file.dat";
            org.gdal.gdal.gdal
                    .Open(VSIFileFileSystemHandler.PREFIX + absolutePath);

            ArrayList<DebugFileIOProvider.InvocationDebugRecord> records = new ArrayList<>();
            dbgprovider.getInvocationRecord(records);

            final DebugFileIOProvider.InvocationDebugRecord expectedRecord = new DebugFileIOProvider.InvocationDebugRecord();
            expectedRecord.methodName = "getChannel";
            expectedRecord.parameters.put("f", new File(absolutePath));
            expectedRecord.parameters.put("mode", "r");

            assertTrue(records.contains(expectedRecord));
        } finally {
        }
    }
}
