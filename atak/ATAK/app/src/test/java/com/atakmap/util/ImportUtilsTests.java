
package com.atakmap.util;

import com.atakmap.android.importfiles.ui.ImportUtils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportUtilsTests {

    @Test
    public void removeSubFilesFromSetRecursive_DoesNotContainSelectedSubfiles_SetUnchanged()
            throws IOException {
        final Set<File> files = new HashSet<>();
        final Map<String, File> filesMap = new HashMap<>();
        final File dir = Mockito.mock(File.class);
        final File subfile1 = Mockito.mock(File.class),
                subfile2 = Mockito.mock(File.class);
        final File[] subfiles = new File[] {
                subfile1,
                subfile2
        };

        Mockito.when(dir.getCanonicalPath())
                .thenReturn(File.separatorChar + "dir");
        Mockito.when(dir.listFiles()).thenReturn(subfiles);
        Mockito.when(dir.isDirectory()).thenReturn(true);

        Mockito.when(subfile1.getParentFile()).thenReturn(dir);
        Mockito.when(subfile1.isDirectory()).thenReturn(false);
        Mockito.when(subfile1.getName()).thenReturn("subfile1");
        final String canName = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getName();
        Mockito.when(subfile1.getCanonicalPath()).thenReturn(canName);

        Mockito.when(subfile2.getName()).thenReturn("subfile2");
        final String canName2 = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getCanonicalPath()
                + File.separatorChar + subfile2.getName();
        Mockito.when(subfile2.getCanonicalPath()).thenReturn(canName2);
        Mockito.when(subfile2.isDirectory()).thenReturn(false);
        Mockito.when(subfile2.getParentFile()).thenReturn(dir);

        files.add(dir);
        filesMap.put(dir.getCanonicalPath(), dir);

        Assert.assertEquals(1, files.size());
        Assert.assertEquals(1, filesMap.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertFalse(files.contains(subfile1));
        Assert.assertFalse(files.contains(subfile2));
        Assert.assertTrue(filesMap.containsKey(dir.getCanonicalPath()));
        Assert.assertFalse(filesMap.containsKey(subfile1.getCanonicalPath()));
        Assert.assertFalse(filesMap.containsKey(subfile2.getCanonicalPath()));

        ImportUtils.removeSubFilesFromSetRecursive(dir, filesMap, files);

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertFalse(files.contains(subfile1));
        Assert.assertFalse(files.contains(subfile2));
    }

    @Test
    public void removeSubFilesFromSetRecursive_ContainsSelectedSubfiles_SetHasSubFilesRemoved()
            throws IOException {
        final Set<File> files = new HashSet<>();
        final Map<String, File> filesMap = new HashMap<>();
        final File dir = Mockito.mock(File.class);
        final File subfile1 = Mockito.mock(File.class),
                subfile2 = Mockito.mock(File.class);
        final File[] subfiles = new File[] {
                subfile1,
                subfile2
        };

        Mockito.when(dir.getCanonicalPath())
                .thenReturn(File.separatorChar + "dir");
        Mockito.when(dir.listFiles()).thenReturn(subfiles);
        Mockito.when(dir.isDirectory()).thenReturn(true);

        Mockito.when(subfile1.getParentFile()).thenReturn(dir);
        Mockito.when(subfile1.getName()).thenReturn("subfile1");
        final String canName = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getName();
        Mockito.when(subfile1.getCanonicalPath()).thenReturn(canName);
        Mockito.when(subfile1.isDirectory()).thenReturn(false);

        Mockito.when(subfile2.getName()).thenReturn("subfile2");
        final String canName2 = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getCanonicalPath()
                + File.separatorChar + subfile2.getName();
        Mockito.when(subfile2.getCanonicalPath()).thenReturn(canName2);
        Mockito.when(subfile2.getParentFile()).thenReturn(dir);
        Mockito.when(subfile2.isDirectory()).thenReturn(false);

        files.add(dir);
        files.add(subfile1);
        files.add(subfile2);
        filesMap.put(dir.getCanonicalPath(), dir);
        filesMap.put(subfile1.getCanonicalPath(), subfile1);
        filesMap.put(subfile2.getCanonicalPath(), subfile2);

        Assert.assertEquals(3, files.size());
        Assert.assertEquals(3, filesMap.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertTrue(files.contains(subfile1));
        Assert.assertTrue(files.contains(subfile2));
        Assert.assertTrue(filesMap.containsKey(dir.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile1.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile2.getCanonicalPath()));

        ImportUtils.removeSubFilesFromSetRecursive(dir, filesMap, files);

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertFalse(files.contains(subfile1));
        Assert.assertFalse(files.contains(subfile2));
    }

    @Test
    public void removeSubFilesFromSetRecursive_ContainsSelectedSubfilesRecursivelyDeep_SetHasSubFilesRemoved()
            throws IOException {
        final Set<File> files = new HashSet<>();
        final Map<String, File> filesMap = new HashMap<>();
        final File dir = Mockito.mock(File.class);
        final File subfile1 = Mockito.mock(File.class),
                subfile2 = Mockito.mock(File.class),
                subfile3 = Mockito.mock(File.class);
        final File[] subfilesFirstLevel = new File[] {
                subfile1
        };

        final File[] subfilesSecondLevel = new File[] {
                subfile2
        };

        final File[] subfilesThirdLevel = new File[] {
                subfile3
        };

        Mockito.when(dir.getCanonicalPath())
                .thenReturn(File.separatorChar + "dir");
        Mockito.when(dir.listFiles()).thenReturn(subfilesFirstLevel);
        Mockito.when(dir.isDirectory()).thenReturn(true);

        Mockito.when(subfile1.getParentFile()).thenReturn(dir);
        Mockito.when(subfile1.getName()).thenReturn("subfile1");
        final String canName = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getName();
        Mockito.when(subfile1.getCanonicalPath()).thenReturn(canName);
        Mockito.when(subfile1.listFiles()).thenReturn(subfilesSecondLevel);
        Mockito.when(subfile1.isDirectory()).thenReturn(true);

        Mockito.when(subfile2.getName()).thenReturn("subfile2");
        final String canName2 = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getCanonicalPath()
                + File.separatorChar + subfile2.getName();
        Mockito.when(subfile2.getCanonicalPath()).thenReturn(canName2);
        Mockito.when(subfile2.isDirectory()).thenReturn(true);
        Mockito.when(subfile2.getParentFile()).thenReturn(subfile1);
        Mockito.when(subfile2.listFiles()).thenReturn(subfilesThirdLevel);

        Mockito.when(subfile3.getName()).thenReturn("subfile3");
        final String canName3 = dir.getCanonicalPath() + File.separatorChar
                + subfile2.getCanonicalPath()
                + File.separatorChar + subfile3.getName();
        Mockito.when(subfile3.getCanonicalPath()).thenReturn(canName3);
        Mockito.when(subfile3.isDirectory()).thenReturn(false);
        Mockito.when(subfile3.getParentFile()).thenReturn(subfile2);

        files.add(dir);
        files.add(subfile1);
        files.add(subfile3);
        filesMap.put(dir.getCanonicalPath(), dir);
        filesMap.put(subfile1.getCanonicalPath(), subfile1);
        filesMap.put(subfile3.getCanonicalPath(), subfile3);

        Assert.assertEquals(3, files.size());
        Assert.assertEquals(3, filesMap.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertTrue(files.contains(subfile1));
        Assert.assertTrue(files.contains(subfile3));
        Assert.assertTrue(filesMap.containsKey(dir.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile1.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile3.getCanonicalPath()));

        ImportUtils.removeSubFilesFromSetRecursive(dir, filesMap, files);

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertFalse(files.contains(subfile1));
        Assert.assertFalse(files.contains(subfile2));
        Assert.assertFalse(files.contains(subfile3));
    }

    @Test
    public void removeSubFilesFromSetRecursive_ContainsSelectedSubfilesRecursively_SetHasSubFilesRemoved()
            throws IOException {
        final Set<File> files = new HashSet<>();
        final Map<String, File> filesMap = new HashMap<>();
        final File dir = Mockito.mock(File.class);
        final File subfile1 = Mockito.mock(File.class),
                subfile2 = Mockito.mock(File.class);
        final File[] subfilesFirstLevel = new File[] {
                subfile1
        };

        final File[] subfilesSecondLevel = new File[] {
                subfile2
        };

        Mockito.when(dir.getCanonicalPath())
                .thenReturn(File.separatorChar + "dir");
        Mockito.when(dir.listFiles()).thenReturn(subfilesFirstLevel);
        Mockito.when(dir.isDirectory()).thenReturn(true);

        Mockito.when(subfile1.getParentFile()).thenReturn(dir);
        Mockito.when(subfile1.getName()).thenReturn("subfile1");
        final String canName = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getName();
        Mockito.when(subfile1.getCanonicalPath()).thenReturn(canName);
        Mockito.when(subfile1.listFiles()).thenReturn(subfilesSecondLevel);
        Mockito.when(subfile1.isDirectory()).thenReturn(true);

        Mockito.when(subfile2.getName()).thenReturn("subfile2");
        final String canName2 = dir.getCanonicalPath() + File.separatorChar
                + subfile1.getCanonicalPath()
                + File.separatorChar + subfile2.getName();
        Mockito.when(subfile2.getCanonicalPath()).thenReturn(canName2);
        Mockito.when(subfile2.isDirectory()).thenReturn(false);
        Mockito.when(subfile2.getParentFile()).thenReturn(subfile1);

        files.add(dir);
        files.add(subfile1);
        files.add(subfile2);
        filesMap.put(dir.getCanonicalPath(), dir);
        filesMap.put(subfile1.getCanonicalPath(), subfile1);
        filesMap.put(subfile2.getCanonicalPath(), subfile2);

        Assert.assertEquals(3, files.size());
        Assert.assertEquals(3, filesMap.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertTrue(files.contains(subfile1));
        Assert.assertTrue(files.contains(subfile2));
        Assert.assertTrue(filesMap.containsKey(dir.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile1.getCanonicalPath()));
        Assert.assertTrue(filesMap.containsKey(subfile2.getCanonicalPath()));

        ImportUtils.removeSubFilesFromSetRecursive(dir, filesMap, files);

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.contains(dir));
        Assert.assertFalse(files.contains(subfile1));
        Assert.assertFalse(files.contains(subfile2));
    }
}
