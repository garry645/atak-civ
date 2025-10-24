package com.atakmap.coremap.maps.coords;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.conversion.GeomagneticField;
import com.atakmap.util.ConfigOptions;
import com.atakmap.util.zip.IoUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import gov.tak.test.KernelJniTest;

public class GeomagneticFieldTest extends KernelJniTest
{
    @Test
    public void testDeclination() throws Throwable
    {
        Context context = getTestContext();
        extractPrivateResource(context, "wmm_cof", "world-magnetic-model-file");
        Date d = new Date(122, 4, 12, 0, 0, 0);
        // Use the GMF around the initial point to find the declination
        GeomagneticField gmf = new GeomagneticField(
                48.80299f,
                36.87427f,
                0f,
                d.getTime());
        final double declination = gmf.getDeclination();
        Assert.assertEquals(8.79306697845459d, declination, 0.001d);
    }

    private static void extractPrivateResource(Context context, String resourceName, String option) throws Throwable
    {
        InputStream stream = null;

        try
        {
            long s = SystemClock.uptimeMillis();
            // load from assets
            Resources r = context.getResources();
            final int id = r.getIdentifier(resourceName, "raw",
                    context.getPackageName());
            if (id != 0)
            {
                stream = r.openRawResource(id);
            }

            File cofFile = new File(context.getFilesDir(), resourceName);

            FileSystemUtils.copy(stream,
                    new FileOutputStream(cofFile));

            if (option != null)
                ConfigOptions.setOption(option, cofFile.getAbsolutePath());
        } catch (Throwable t)
        {
            throw new ExceptionInInitializerError(t);
        } finally
        {
            IoUtils.close(stream);
        }
    }
}
