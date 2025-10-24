package gov.tak.platform.client.dted;

import gov.tak.platform.system.SystemUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link Dt2FileFilter} class.
 *
 * @since 4.8.1
 */
public class Dt2FileFilterTest
{
    @Test
    public void Dt2FileFilter_accept()
    {
        if (SystemUtils.isOsWindows())
        {
            assertFileFilterHelper("C:\\sdcard\\dted\\w74\\n47.dt0", 0);
            assertFileFilterHelper("C:\\sdcard\\dted\\w74\\n47.dt1", 1);
            assertFileFilterHelper("C:\\sdcard\\dted\\w74\\n47.dt2", 2);
        } else
        {
            assertFileFilterHelper("/sdcard/dted/w74/n47.dt0", 0);
            assertFileFilterHelper("/sdcard/dted/w74/n47.dt1", 1);
            assertFileFilterHelper("/sdcard/dted/w74/n47.dt2", 2);
        }
    }

    private void assertFileFilterHelper(String path, int level)
    {
        File file = new File(path);
        Dt2FileFilter level0Filter = new Dt2FileFilter(0);
        Dt2FileFilter level1Filter = new Dt2FileFilter(1);
        Dt2FileFilter level2Filter = new Dt2FileFilter(2);
        assertEquals(level0Filter.accept(file), level == 0);
        assertEquals(level1Filter.accept(file), level == 1);
        assertEquals(level2Filter.accept(file), level == 2);
    }
}
