package gov.tak.platform.client.dted;

import gov.tak.test.util.FileUtils.AutoDeleteFile;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the {@link Dt2FileWatcher} class.
 *
 * @since 4.8.1
 */
public class Dt2FileWatcherTest
{
    private static final String DTED_TILE_PATH = "/dted/w74/n47.dt2";

    @Test
    public void Dt2FileWatcher_discover_new_file()
    {
        try (TestDt2FileWatcher watcher = TestDt2FileWatcher.create())
        {
            assertEquals(0, watcher.getFileCount());
            watcher.addFile(DTED_TILE_PATH);
            assertEquals(1, watcher.getFileCount());
        }
    }

    @Test
    public void Dt2FileWatcher_discover_deleted_file()
    {
        try (TestDt2FileWatcher watcher = TestDt2FileWatcher.create())
        {
            watcher.addFile(DTED_TILE_PATH);
            assertEquals(1, watcher.getFileCount());
            watcher.removeFile(DTED_TILE_PATH);
            assertEquals(0, watcher.getFileCount());
        }
    }

    @Test
    public void Dt2FileWatcher_coverage()
    {
        try (TestDt2FileWatcher watcher = TestDt2FileWatcher.create())
        {
            watcher.addFile(DTED_TILE_PATH);
            int idx = Dt2FileWatcher.getCoverageIndex(47, -74);
            assertFalse(watcher.getCoverage(0).get(idx));
            assertFalse(watcher.getCoverage(1).get(idx));
            assertTrue(watcher.getCoverage(2).get(idx));
            assertFalse(watcher.getCoverage(2).get(idx - 1));
            assertTrue(watcher.getFullCoverage().get(idx));
            assertFalse(watcher.getFullCoverage().get(idx - 1));
        }
    }

    private static class TestDt2FileWatcher extends Dt2FileWatcher implements AutoCloseable
    {
        private static TestDt2FileWatcher create()
        {
            File cacheDir = null;
            try
            {
                cacheDir = File.createTempFile("TestDt2FileWatcher", "");
                cacheDir.delete();
                cacheDir.mkdirs();
            } catch (IOException e)
            {
                fail("Could not create temporary dted directory.");
            }
            AutoDeleteFile autoDeleteFile = AutoDeleteFile.createTempDir(cacheDir);
            return new TestDt2FileWatcher(autoDeleteFile);
        }

        private final AutoDeleteFile autoDeleteFile;

        public TestDt2FileWatcher(AutoDeleteFile autoDeleteFile)
        {
            super(Collections.singletonList(new File(autoDeleteFile.getPath())));
            this.autoDeleteFile = autoDeleteFile;
        }

        public void addFile(String path)
        {
            File tileFile = new File(autoDeleteFile.getPath(), path);
            tileFile.getParentFile().mkdirs();
            try
            {
                tileFile.createNewFile();
            } catch (IOException e)
            {
                fail("IO Exception occurred: " + e);
            }
            scan();
        }

        public void removeFile(String path)
        {
            File tileFile = new File(autoDeleteFile.getPath(), path);
            tileFile.delete();
            scan();
        }

        @Override
        public void close()
        {
            autoDeleteFile.close();
        }
    }
}
