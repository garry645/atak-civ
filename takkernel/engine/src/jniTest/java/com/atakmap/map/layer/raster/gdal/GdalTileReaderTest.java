
package com.atakmap.map.layer.raster.gdal;

import android.content.Context;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.io.MockIOProvider;
import com.atakmap.map.gdal.GdalLibrary;

import gov.tak.test.KernelJniTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class GdalTileReaderTest extends KernelJniTest
{
    @BeforeClass
    public static void setup() {
        GdalLibrary.init();
    }

    @Test
    public void testHappyPath() {
        // TODO test with valid tileset data
    }

}
