
package com.atakmap.coremap.filesystem;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.android.androidtest.util.RandomUtils;
import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.ZipVirtualFile;

import gov.tak.platform.system.SystemUtils;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class FileSystemUtilsTest extends KernelJniTest
{

    private final static String TAG = "FileSystemUtilsTest";

    @Before
    public void beforeTest() {
        IOProviderFactoryHelper.registerProvider(new DefaultIOProvider(), true);
    }

    @Test
    public void FileSystemUtils_isEmpty() {
        String string = "";
        assertTrue(FileSystemUtils.isEmpty(string));

        string = null;
        assertTrue(FileSystemUtils.isEmpty(string));

        String[] string_array = null;
        assertTrue(FileSystemUtils.isEmpty(string_array));

        string_array = new String[] {};
        assertTrue(FileSystemUtils.isEmpty(string_array));

        List<String> string_list = null;
        assertTrue(FileSystemUtils.isEmpty(string_list));

        string_list = new ArrayList<>();
        assertTrue(FileSystemUtils.isEmpty(string_list));

    }

    @Test
    public void FileSystemUtils_getFileTreeData_invalid_file()
            throws IOException {
        Context appContext = getTestContext();
        File f = File.createTempFile("testfile", ".dat",
                appContext.getCacheDir());
        try {
            f.deleteOnExit();
            if (IOProviderFactory.exists(f)) {
                if (!f.delete()) {
                    throw new IllegalStateException();
                }
            }

            FileSystemUtils.FileTreeData ftd = new FileSystemUtils.FileTreeData();
            final long size = ftd.size;
            final long lastModified = ftd.lastModified;
            final long numFiles = ftd.numFiles;
            FileSystemUtils.getFileData(f, ftd, Long.MAX_VALUE);
            // should be no-op
            assertEquals(size, ftd.size);
            assertEquals(lastModified, ftd.lastModified);
            assertEquals(0, ftd.numFiles);
        } finally {
            f.delete();
        }
    }

    @Test
    public void FileSystemUtils_getFileTreeData_single_file()
            throws IOException {
        Context appContext = getTestContext();
        File f = File.createTempFile("testfile", ".dat",
                appContext.getCacheDir());
        try {
            f.deleteOnExit();

            try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                raf.setLength(5678);
            }
            f.setLastModified(1234);

            FileSystemUtils.FileTreeData ftd = new FileSystemUtils.FileTreeData();
            FileSystemUtils.getFileData(f, ftd, Long.MAX_VALUE);
            assertEquals(IOProviderFactory.length(f), ftd.size);
            assertEquals(IOProviderFactory.lastModified(f), ftd.lastModified);
            assertEquals(1, ftd.numFiles);
        } finally {
            f.delete();
        }
    }

    @Test
    public void FileSystemUtils_getFileTreeData_directory() throws IOException {
        Context appContext = getTestContext();
        File f = FileSystemUtils.createTempDir("testfile", ".dir",
                appContext.getCacheDir());
        try {
            File[] files = new File[] {
                    createFile(File.createTempFile("testfile", ".dat", f), 100,
                            100000),
                    createFile(File.createTempFile("testfile", ".dat", f), 200,
                            100000),
                    createFile(File.createTempFile("testfile", ".dat", f), 300,
                            200000),
            };
            // NOTE: the actual FS may not be storing last modified at full
            // precision, so we need to utilize the actual value, not the
            // specified one
            long greatestLastModified = IOProviderFactory
                    .lastModified(files[0]);
            for (int i = 1; i < files.length; i++)
                if (IOProviderFactory
                        .lastModified(files[i]) > greatestLastModified)
                    greatestLastModified = IOProviderFactory
                            .lastModified(files[i]);

            FileSystemUtils.FileTreeData ftd = new FileSystemUtils.FileTreeData();
            FileSystemUtils.getFileData(f, ftd, Long.MAX_VALUE);
            assertEquals(600, ftd.size);
            assertEquals(greatestLastModified, ftd.lastModified);
            assertEquals(files.length, ftd.numFiles);
        } finally {
            FileSystemUtils.delete(f);
        }
    }

    @Test
    public void FileSystemUtils_getFileTreeData_directory_with_limit()
            throws IOException {
        Context appContext = getTestContext();
        File f = FileSystemUtils.createTempDir("testfile", ".dir",
                appContext.getCacheDir());
        try {
            // NOTE: file iteration will not be guaranteed, so simply ensure
            // we are getting a subset of the input
            File[] files = new File[] {
                    createFile(File.createTempFile("testfile", ".dat", f), 100,
                            100000),
                    createFile(File.createTempFile("testfile", ".dat", f), 100,
                            100000),
                    createFile(File.createTempFile("testfile", ".dat", f), 100,
                            100000),
            };

            final long limit = 2;
            FileSystemUtils.FileTreeData ftd = new FileSystemUtils.FileTreeData();
            FileSystemUtils.getFileData(f, ftd, limit);
            assertEquals(200, ftd.size);
            assertEquals(IOProviderFactory.lastModified(files[0]),
                    ftd.lastModified);
            assertEquals(limit, ftd.numFiles);
        } finally {
            FileSystemUtils.delete(f);
        }
    }

    private static File createFile(File f, int len, long lastModified)
            throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            raf.setLength(len);
        }
        f.setLastModified(lastModified);
        return f;
    }

    @Test
    public void FileSystemUtils_FileNameInvalid_constructor_roundtrip() {
        final String message = "test message";
        FileSystemUtils.FileNameInvalid ex = new FileSystemUtils.FileNameInvalid(
                message);
        assertEquals(message, ex.getMessage());
    }

    @Test
    public void FileSystemUtils_findFile_null_dir_returns_null() {
        File dir = null;
        String base = "base";
        String[] exts = new String[] {
                ".ext"
        };
        final File result = FileSystemUtils.findFile(dir, base, exts);
        assertNull(result);
    }

    @Test
    public void FileSystemUtils_findFile_null_base_returns_null()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            String base = null;
            String[] exts = new String[] {
                    ".ext"
            };
            final File result = FileSystemUtils.findFile(dir.file, base, exts);

            assertNull(result);
        }
    }

    @Test
    public void FileSystemUtils_findFile_null_exts_returns_null()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            String base = null;
            String[] exts = new String[] {
                    ".ext"
            };
            final File result = FileSystemUtils.findFile(dir.file, base, exts);

            assertNull(result);
        }
    }

    @Test
    public void FileSystemUtils_findFile_no_matching_exts_returns_null()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            String base = "base";
            String[] exts = new String[] {
                    ".ext"
            };

            File f = new File(dir.file, base + ".dat");
            IOProviderFactory.createNewFile(f);

            final File result = FileSystemUtils.findFile(dir.file, base, exts);

            assertNotEquals(f, result);
        }
    }

    @Test
    public void FileSystemUtils_findFile_matching_ext_returns_file()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            String base = "base";
            String[] exts = new String[] {
                    ".ext"
            };

            File f = new File(dir.file, base + exts[0]);
            IOProviderFactory.createNewFile(f);

            final File result = FileSystemUtils.findFile(dir.file, base, exts);

            assertEquals(f, result);
        }
    }

    @Test
    public void FileSystemUtils_findFile_matching_last_ext_returns_file()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            String base = "base";
            String[] exts = new String[] {
                    ".ext0", ".ext1", ".ext2", ".ext3", ".ext4"
            };

            File f = new File(dir.file, base + exts[4]);
            IOProviderFactory.createNewFile(f);

            final File result = FileSystemUtils.findFile(dir.file, base, exts);

            assertEquals(f, result);
        }
    }

    @Test
    public void FileSystemUtils_canWrite_writable_dir_returns_true()
            throws IOException {
        Context appContext = getTestContext();
        File dir = appContext.getCacheDir();
        assertTrue(FileSystemUtils.canWrite(dir));
    }

    @Test
    public void FileSystemUtils_canWrite_writable_not_existing_dir_returns_false()
            throws IOException {
        Context appContext = getTestContext();
        File dir = new File(appContext.getCacheDir(), ".doesnotexist");
        assertFalse(FileSystemUtils.canWrite(dir));
    }

    @Test
    public void FileSystemUtils_canWrite_writable_not_writable_dir_returns_false()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile dir = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("testfile", ".dir",
                        appContext.getCacheDir()))) {

            final boolean success = IOProviderFactory.setWritable(dir.file, false, true);
            // XXX - also observing this fail on linux
            // https://bugs.openjdk.org/browse/JDK-6728842
            //Assert.assertTrue(SystemUtils.isOsWindows() || success);
            if(SystemUtils.isOsAndroid()/* || success*/)
                assertFalse(FileSystemUtils.canWrite(dir.file));
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_canWrite_writable_null_dir_throws()
            throws IOException {
        FileSystemUtils.canWrite(null);
        fail();
    }

    @Test
    public void FileSystemUtils_read__File_roundtrip() throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile f = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()))) {

            byte[] arr = new byte[256];
            for (int i = 0; i < arr.length; i++)
                arr[i] = (byte) i;

            try (FileOutputStream fos = IOProviderFactory
                    .getOutputStream(f.file)) {
                fos.write(arr);
            }

            byte[] result = FileSystemUtils.read(f.file);
            assertArrayEquals(arr, result);
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_read__File_null_file_throws()
            throws IOException {
        FileSystemUtils.read((File) null);
        fail();
    }

    @Test
    public void FileSystemUtils_read__InputStream_roundtrip()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile f = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()))) {

            byte[] arr = new byte[256];
            for (int i = 0; i < arr.length; i++)
                arr[i] = (byte) i;

            try (FileOutputStream fos = IOProviderFactory
                    .getOutputStream(f.file)) {
                fos.write(arr);
            }

            try (FileInputStream fis = IOProviderFactory
                    .getInputStream(f.file)) {
                byte[] result = FileSystemUtils.read(fis);
                assertArrayEquals(arr, result);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_read__InputStream_null_file_throws()
            throws IOException {
        FileSystemUtils.read((InputStream) null);
        fail();
    }

    @Test
    public void FileSystemUtils_copyFile_roundtrip() throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile orig = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()));
                FileUtils.AutoDeleteFile copy = new FileUtils.AutoDeleteFile(
                        File.createTempFile("testfile", ".dat",
                                appContext.getCacheDir()))) {

            byte[] arr = new byte[256];
            for (int i = 0; i < arr.length; i++)
                arr[i] = (byte) i;

            try (FileOutputStream fos = IOProviderFactory
                    .getOutputStream(orig.file)) {
                fos.write(arr);
            }

            FileSystemUtils.copyFile(orig.file, copy.file, new byte[8192]);
            assertEquals(IOProviderFactory.length(orig.file),
                    IOProviderFactory.length(copy.file));

            byte[] copyData = FileSystemUtils.read(copy.file);
            assertArrayEquals(arr, copyData);
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_copyFile_null_source_throws()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile copy = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()))) {
            FileSystemUtils.copyFile(null, copy.file, new byte[8192]);
            fail();
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_copyFile_null_destination_throws()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile orig = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()))) {
            FileSystemUtils.copyFile(orig.file, null, new byte[8192]);
            fail();
        }
    }

    @Test(expected = RuntimeException.class)
    public void FileSystemUtils_copyFile_null_buffer_throws()
            throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile orig = new FileUtils.AutoDeleteFile(
                File.createTempFile("testfile", ".dat",
                        appContext.getCacheDir()));
                FileUtils.AutoDeleteFile copy = new FileUtils.AutoDeleteFile(
                        File.createTempFile("testfile", ".dat",
                                appContext.getCacheDir()))) {

            byte[] arr = new byte[256];
            for (int i = 0; i < arr.length; i++)
                arr[i] = (byte) i;

            try (FileOutputStream fos = IOProviderFactory
                    .getOutputStream(orig.file)) {
                fos.write(arr);
            }

            FileSystemUtils.copyFile(orig.file, copy.file, (byte[]) null);
            fail();
        }
    }

    @Test
    public void FileSystemUtils_deleteDirectory_false() throws IOException {
        Context appContext = getTestContext();
        File directory = new File(appContext.getFilesDir(), "runtime_test");

        IOProviderFactory.mkdir(directory);

        File f = File.createTempFile("testfile", ".dat",
                directory);
        byte[] arr = new byte[256];
        for (int i = 0; i < arr.length; i++)
            arr[i] = (byte) i;

        try (FileOutputStream fos = IOProviderFactory.getOutputStream(f)) {
            fos.write(arr);
        }
        assertTrue("tempfile exists", IOProviderFactory.exists(f));
        assertTrue("tempfile directory exists",
                IOProviderFactory.exists(directory));

        FileSystemUtils.deleteDirectory(directory, false);
        assertFalse("tempfile removed", IOProviderFactory.exists(f));
        assertFalse("tempfile directory removed",
                IOProviderFactory.exists(directory));

    }

    @Test
    public void FileSystemUtils_unzip_path_separators() throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile src = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("src", ".dir", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile linuxDst = new FileUtils.AutoDeleteFile(
                        FileSystemUtils.createTempDir("linux_dst", ".dir", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile windowsDst = new FileUtils.AutoDeleteFile(
                        FileSystemUtils.createTempDir("windows_dst", ".dir", appContext.getCacheDir()));
            ) {

            // test windows-created zip file
            final String windowsZipFname = "FileSystemUtils_unzip_path_separators_windows.zip";
            File windowsZip = new File(src.file.getAbsolutePath(), windowsZipFname);
            try(InputStream windowsData = appContext.getResources().getAssets().open("windows_slash.zip");
                OutputStream out = IOProviderFactory.getOutputStream(windowsZip)) {

                FileSystemUtils.copy(windowsData, out);
            }

            FileSystemUtils.unzip(windowsZip, windowsDst.file, false);
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(windowsZip), windowsDst.file);

            // test linux-created zip file
            final String linuxZipFname = "FileSystemUtils_unzip_path_separators_linux.zip";
            File linuxZip = new File(src.file.getAbsolutePath(), linuxZipFname);
            try(InputStream linuxData = appContext.getResources().getAssets().open("unix_slash.zip");
                OutputStream out = IOProviderFactory.getOutputStream(linuxZip)) {

                FileSystemUtils.copy(linuxData, out);
            }

            FileSystemUtils.unzip(linuxZip, linuxDst.file, false);
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(linuxZip), linuxDst.file);

            // test across platforms
            FileUtils.assertRelativeTreeEquals(windowsDst.file, linuxDst.file);
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(windowsZip), linuxDst.file);
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(linuxZip), windowsDst.file);
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(linuxZip), new ZipVirtualFile(windowsZip));
        }
    }

    @Test
    public void FileSystemUtils_unzip_nested() throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile src = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("src_testfile", ".dir", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile zip = new FileUtils.AutoDeleteFile(
                        File.createTempFile("zip_testfile", ".zip", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile dst = new FileUtils.AutoDeleteFile(
                        FileSystemUtils.createTempDir("dst_testfile", ".dir", appContext.getCacheDir()))) {

            File sub = new File(src.file, "subdir");
            IOProviderFactory.mkdir(sub);

            // stage some content to create a zip file
            try (FileOutputStream fos = new FileOutputStream(
                    new File(sub, "dat256.dat"))) {
                fos.write(RandomUtils.randomByteArray(256 * 1024));
            }
            try (FileOutputStream fos = new FileOutputStream(
                    new File(sub, "dat512.dat"))) {
                fos.write(RandomUtils.randomByteArray(512 * 1024));
            }
            try (FileOutputStream fos = new FileOutputStream(
                    new File(sub, "dat1024.dat"))) {
                fos.write(RandomUtils.randomByteArray(1024 * 1024));
            }
            // zip the contents
            FileSystemUtils.zipDirectory(src.file, zip.file);

            // unzip to new directory
            FileSystemUtils.unzip(zip.file, dst.file, false);

            // compare the zip with the output
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(zip.file), dst.file);
            // compare the source with the output
            FileUtils.assertRelativeTreeEquals(src.file, dst.file);
        }
    }

    @Test
    public void FileSystemUtils_unzip() throws IOException {
        Context appContext = getTestContext();
        try (FileUtils.AutoDeleteFile src = new FileUtils.AutoDeleteFile(
                FileSystemUtils.createTempDir("src_testfile", ".dir", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile zip = new FileUtils.AutoDeleteFile(
                        File.createTempFile("zip_testfile", ".zip", appContext.getCacheDir()));
                FileUtils.AutoDeleteFile dst = new FileUtils.AutoDeleteFile(
                        FileSystemUtils.createTempDir("dst_testfile", ".dir", appContext.getCacheDir()))) {

            // stage some content to create a zip file
            try (FileOutputStream fos = new FileOutputStream(
                    new File(src.file, "dat256.dat"))) {
                fos.write(RandomUtils.randomByteArray(256 * 1024));
            }
            try (FileOutputStream fos = new FileOutputStream(
                    new File(src.file, "dat512.dat"))) {
                fos.write(RandomUtils.randomByteArray(512 * 1024));
            }
            try (FileOutputStream fos = new FileOutputStream(
                    new File(src.file, "dat1024.dat"))) {
                fos.write(RandomUtils.randomByteArray(1024 * 1024));
            }
            // zip the contents
            FileSystemUtils.zipDirectory(src.file, zip.file);

            // unzip to new directory
            FileSystemUtils.unzip(zip.file, dst.file, false);

            // compare the zip with the output
            FileUtils.assertRelativeTreeEquals(new ZipVirtualFile(zip.file), dst.file);
            // compare the source with the output
            FileUtils.assertRelativeTreeEquals(src.file, dst.file);
        }
    }

    @Test
    public void sanitizeWithSpacesAndSlashesAndroid()
    {
        assumeTrue(SystemUtils.isOsAndroid());
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/sdcard/test"));
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/sdcard/my test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("/my sdcard/test")); // Bad root FS
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/sdcard/te*st")); // Illegal char
        assertFalse(sanitizeWithSpacesAndSlashesHelper("../sdcard/test")); // .. in path
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/sdcard\\test")); // mixed separator chars

        // Wrong OS
        assertFalse(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/usr/local/test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support/test"));
    }

    @Test
    public void sanitizeWithSpacesAndSlashesWindows()
    {
        assumeTrue(SystemUtils.isOsWindows());
        assertTrue(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\test"));
        assertTrue(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\my test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\te*st"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("..\\my sdcard\\test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard/my test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("/sdcard/test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/usr/local/test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support/test"));
    }

    @Test
    public void sanitizeWithSpacesAndSlashesLinux()
    {
        assumeTrue(SystemUtils.isOsLinux() & !SystemUtils.isOsAndroid());
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/usr/local/test"));
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/usr/local/my test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("/usr/local/te*st"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("../usr/local/test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/usr/local\\test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\test"));
    }

    @Test
    public void sanitizeWithSpacesAndSlashesMac()
    {
        assumeTrue(SystemUtils.isOsMac());
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support/test"));
        assertTrue(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support/my test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support/te*st"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("../Library/Application Support/test"));
        assertFalse(sanitizeWithSpacesAndSlashesHelper("/Library/Application Support\\test"));

        assertFalse(sanitizeWithSpacesAndSlashesHelper("C:\\sdcard\\test"));
    }

    private boolean sanitizeWithSpacesAndSlashesHelper(String s) {
        return s.equals(FileSystemUtils.sanitizeWithSpacesAndSlashes(s));
    }

    @Test
    public void FileSystemUtils_isAcceptableInFileName() throws IOException {
        Assert.assertTrue(isAcceptableHelper("valid.zip"));

        Assert.assertTrue(isAcceptableHelper("valid#.zip"));
        Assert.assertTrue(isAcceptableHelper("valid:.zip"));
        Assert.assertTrue(isAcceptableHelper("valid1.zip"));
        Assert.assertTrue(isAcceptableHelper("valid-2.zip"));

        Assert.assertTrue(!isAcceptableHelper("valid\\.zip"));
        Assert.assertTrue(!isAcceptableHelper("valid*.zip"));
        Assert.assertTrue(!isAcceptableHelper("valid?.zip"));
        Assert.assertTrue(!isAcceptableHelper("valid<.zip"));
        Assert.assertTrue(!isAcceptableHelper("valid>.zip"));

        // crylic
        Assert.assertTrue(isAcceptableHelper("ОД Д.zip"));
        Assert.assertTrue(isAcceptableHelper("ОД Д Г.zip"));

        // arabic
        Assert.assertTrue(isAcceptableHelper(".zipعرض"));
        Assert.assertTrue(isAcceptableHelper(".zip٤٦عرض"));

        // korean
        Assert.assertTrue(isAcceptableHelper("보여주기.zip"));
        Assert.assertTrue(isAcceptableHelper("보여주기 둘.zip"));

        // french
        Assert.assertTrue(isAcceptableHelper("àèù.zip"));

    }

    private boolean isAcceptableHelper(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (!FileSystemUtils.isAcceptableInFilename(s.charAt(i)))
                return false;
        }
        return true;
    }
}
