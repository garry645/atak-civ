package gov.tak.platform.client.dted;

import gov.tak.platform.system.SystemUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Unit tests for the {@link Dt2File} class.
 *
 * @since 4.8.1
 */
public class Dt2FileTest
{
    @Test
    public void Dt2File_construction()
    {
        if (SystemUtils.isOsWindows()) {
            assertDt2FileFields("C:\\sdcard\\dted\\w74\\n47.dt1", "C:\\sdcard\\dted", 1, 47, -74);
            assertDt2FileFields("C:\\sdcard\\dted\\e74\\s47.dt2", "C:\\sdcard\\dted", 2, -47, 74);
        } else
        {
            assertDt2FileFields("/sdcard/dted/w74/n47.dt1", "/sdcard/dted", 1, 47, -74);
            assertDt2FileFields("/sdcard/dted/e74/s47.dt2", "/sdcard/dted", 2, -47, 74);
        }
    }

    private void assertDt2FileFields(String path, String parent, int level, int lat, int lon)
    {
        assumeTrue(SystemUtils.isOsWindows());
        Dt2File dt2File = new Dt2File(path);
        assertEquals(level, dt2File.level);
        assertEquals(lat, dt2File.latitude);
        assertEquals(lon, dt2File.longitude);
        assertEquals(path, dt2File.path);
        assertEquals(parent, dt2File.parent);
    }
}

