
package com.atakmap.coremap;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip file related utilities
 */
public class ZipUtils {

    private static final String TAG = "ZipUtils";

    /**
     * Check number of files/entries in ZIP file
     *
     * @param zip File
     * @return zip entry count
     */
    public static int zipEntryCount(File zip) {
        if (!FileSystemUtils.isFile(zip))
            return 0;

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            return zipFile.size();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get zip count for: " + zip, e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception ignore) {
                }
            }
        }

        return 0;
    }

    /**
     * Check number of files/entries in ZIP file
     * @param zip File
     * @param ignore   [optional] filter to ignore/skip, not counting matching files in the zip
     * @return zip entry count
     */
    public static int zipEntryCount(File zip, FilenameFilter ignore) {
        return zipEntryCount(zip, ignore, IOProviderFactory.getProvider());
    }

    /**
     * Check number of files/entries in ZIP file
     * @param zip File
     * @param ignore   [optional] filter to ignore/skip, not counting matching files in the zip
     * @param provider IO Provider to use
     * @return zip entry count
     */
    public static int zipEntryCount(File zip, FilenameFilter ignore,
            IOProvider provider) {
        if (zip == null || !provider.exists(zip)) {
            Log.w(TAG, "Cannot count zip entries for missing file: "
                    + (zip == null ? "NULL" : zip.getAbsolutePath()));
            return 0;
        }

        ZipInputStream zin = null;

        try {
            Log.d(TAG, "Counting entries in zip: " + zip.getAbsolutePath());

            // read in from zip
            zin = new ZipInputStream(provider.getInputStream(zip));
            java.util.zip.ZipEntry zinEntry;

            // iterate all zip entries
            int count = 0;
            while ((zinEntry = zin.getNextEntry()) != null) {
                if (zinEntry.isDirectory()) {
                    Log.d(TAG, "Skipping zip directory: " + zinEntry.getName());
                    continue;
                }

                if (ignore != null
                        //Use the zip parent, as parent for now
                        && ignore.accept(zip.getParentFile(),
                                zinEntry.getName())) {
                    Log.d(TAG, "Skipping file: " + zinEntry.getName());
                    continue;
                } else {
                    Log.d(TAG, "Counting file: " + zinEntry.getName());
                    count++;
                }
            } // end zin loop

            return count;
        } catch (IOException e) {
            Log.w(TAG, "Failed to count zip entries: " + zip.getAbsolutePath(),
                    e);
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                    Log.e(TAG,
                            "Failed to close zip file: "
                                    + zip.getAbsolutePath(),
                            e);
                }
            }
        }

        return 0;
    }

    /**
     * Compress the specified files into a ZIP file
     *
     * @param files      the files to zip.
     * @param dest     the destination file.
     * @param compress True to compress the ZIP, false to store
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipFiles(File[] files, File dest, boolean compress)
            throws IOException {
        if (dest == null) {
            Log.w(TAG, "Cannot zip to missing file");
            return null;
        }

        if (files == null || files.length < 1) {
            Log.w(TAG, "Cannot zip missing Directory file");
            return null;
        }

        try (ZipOutputStream zos = FileSystemUtils.getZipOutputStream(dest)) {

            // Don't compress the ZIP
            if (!compress)
                zos.setLevel(Deflater.NO_COMPRESSION);

            //loop and add all files
            for (File file : files) {
                if (!FileSystemUtils.isFile(file)) {
                    Log.w(TAG, "Skipping invalid file");
                    continue;
                }

                if (IOProviderFactory.isDirectory(file)) {
                    FileSystemUtils.addDirectory(zos, file, null, null);
                } else {
                    FileSystemUtils.addFile(zos, file);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Zip file", e);
            throw new IOException(e);
        }

        //validate the required files
        if (FileSystemUtils.isFile(dest)) {
            Log.d(TAG, "Exported: " + dest.getAbsolutePath());
            return dest;
        } else {
            Log.w(TAG,
                    "Failed to export valid zip: "
                            + dest.getAbsolutePath());
            return null;
        }
    }

    /**
     * Extract the zip file to the destination directory
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zip       the zip file
     * @param destDir   the destination directory to unzip to
     * @param overwrite true to overwrite existing files
     */
    public static void unzip(File zip, File destDir, boolean overwrite)
            throws IOException {
        FileSystemUtils.unzip(zip, destDir, overwrite);
    }

    /**
     * Extract the zip file to the destination directory
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zip       the zip file
     * @param destDir   the destination directory to unzip to
     * @param overwrite true to overwrite existing files
     */
    public static void unzip(File zip, File destDir, boolean overwrite,
            IOProvider provider)
            throws IOException {
        FileSystemUtils.unzip(zip, destDir, overwrite, provider);
    }

    /**
     * Compress the directory contents into a ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param dir      the directory to zip.
     * @param dest     the destination file.
     * @param compress True to compress the ZIP, false to store
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipDirectory(File dir, File dest, boolean compress)
            throws IOException {
        return FileSystemUtils.zipDirectory(dir, dest, compress);
    }

    /**
     * Compress the directory contents into a ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param dir      the directory to zip.
     * @param dest     the destination file.
     * @param compress True to compress the ZIP, false to store
     * @param ignore   [optional] filter to ignore/skip, not place matching files in the zip
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipDirectory(File dir, File dest, boolean compress,
            FilenameFilter ignore)
            throws IOException {
        return FileSystemUtils.zipDirectory(dir, dest, compress, ignore);
    }

    /**
     * Compress the directory contents into a ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param dir      the directory to zip.
     * @param dest     the destination file.
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipDirectory(File dir, File dest) throws IOException {
        return FileSystemUtils.zipDirectory(dir, dest);
    }

    /**
     * Compress the file list into a ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param files The list of files to be compressed.
     * @param dest the destination zip file.
     * @param compress True to compress the ZIP, false to store
     * @return the destination zip file if successful otherwise null
     * @throws IOException io exception if there is an exception writing the file.
     */
    public static File zipDirectory(List<File> files, File dest,
            boolean compress) throws IOException {
        return FileSystemUtils.zipDirectory(files, dest, compress);
    }

    /**
     * Compress the file list into a ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param files The list of files to be compressed.
     * @param dest the destination zip file.
     * @return the destination zip file if successful otherwise null
     * @throws IOException io exception if there is an exception writing the file.
     */
    public static File zipDirectory(List<File> files, File dest)
            throws IOException {
        return zipDirectory(files, dest, true);
    }

    /**
     * Creates a new buffered ZIP output stream
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param file File to write the ZIP content to
     * @return ZIP output stream
     * @throws IOException Stream failed to be opened
     */
    public static ZipOutputStream getZipOutputStream(File file)
            throws IOException {
        return FileSystemUtils.getZipOutputStream(file);
    }

    /**
     * Pull String/text from Zip Entry: MANIFEST/manifest.xml
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zip the zip file
     * @param zipEntry the entry
     * @return the string that describes the entry in the zip file.
     */
    public static String GetZipFileString(File zip, String zipEntry) {
        return FileSystemUtils.GetZipFileString(zip, zipEntry);
    }

    /**
     * See if the zip file has an entry with the specified entry/name
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zip the zip file
     * @param zipEntry the entry
     * @return true if the zip has a specific zip entry.
     */
    public static boolean ZipHasFile(File zip, String zipEntry) {
        return FileSystemUtils.ZipHasFile(zip, zipEntry);
    }

    /**
     * Check if a given file is a valid ZIP file
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param f File
     * @return True if the file exists and is a valid ZIP file
     */
    public static boolean isZip(File f) {
        return FileSystemUtils.isZip(f);
    }

    /**
     * Check if a given path contains ".zip"
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param path Path (case insensitive)
     * @return True if path contains ".zip"
     */
    public static boolean isZipPath(String path) {
        return FileSystemUtils.isZipPath(path);
    }

    /**
     * Check if a given filename contains ".zip"
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param f file to check
     * @return True if path contains ".zip"
     */
    public static boolean isZipPath(File f) {
        return FileSystemUtils.isZipPath(f);
    }

    /**
     * Add a file to a ZIP archive stream
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zos ZIP output stream
     * @param file File to compress
     * @param filename File name that will show up in the ZIP
     */
    public static void addFile(ZipOutputStream zos, File file,
            String filename) {
        FileSystemUtils.addFile(zos, file, filename);
    }

    /**
     * Add a file to a ZIP archive stream
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zos ZIP output stream
     * @param file File to compress
     */
    public static void addFile(ZipOutputStream zos, File file) {
        FileSystemUtils.addFile(zos, file);
    }

    /**
     * Add a directory to a ZIP archive stream
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zos ZIP output stream
     * @param dir Directory
     * @param parentPath The parent directory path (null if root)
     */
    public static void addDirectory(ZipOutputStream zos, File dir,
            String parentPath) {
        FileSystemUtils.addDirectory(zos, dir, parentPath);
    }

    /**
     * Add a directory to a ZIP archive stream
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param zos ZIP output stream
     * @param dir Directory
     * @param parentPath The parent directory path (null if root)
     * @param ignore [optional] filter to ignore/skip, not counting matching files in the zip
     */
    public static void addDirectory(ZipOutputStream zos, File dir,
            String parentPath, FilenameFilter ignore) {
        FileSystemUtils.addDirectory(zos, dir, parentPath, ignore);
    }

    /**
     * Delete files securely.
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param dir the directory to delete
     * @param ignore    [optional] filter to ignore/skip, not counting matching files in the zip
     */
    public static void deleteDirectory(File dir, FilenameFilter ignore) {
        FileSystemUtils.deleteDirectory(dir, ignore);
    }

    /**
     * Delete files securely.
     * Pass through to TAK Kernel's FileSystemUtils
     *
     * @param files the files to delete
     */
    public static void deleteFiles(File[] files) {
        if (files == null || files.length < 1)
            return;

        for (File file : files) {
            FileSystemUtils.deleteFile(file);
        }
    }
}
