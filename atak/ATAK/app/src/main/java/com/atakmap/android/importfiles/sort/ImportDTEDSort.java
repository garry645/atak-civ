
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportDTEDResolver;

/**
 * Imports archived DTED folders
 * The root directory in the zip file needs to be DTED 
 *
 * @deprecated use {@link ImportDTEDResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportDTEDSort extends ImportResolver {

    private static final String TAG = "ImportDTEDSort";

    private final Context _context;

    public ImportDTEDSort(Context context) {
        super(null, FileSystemUtils.DTED_DIRECTORY, true,
                false);
        _context = context;
    }

    @Override
    public boolean match(File file) {
        // it is a .zip, now lets see if it contains a DTED directory 
        // but no manifest.
        return isDted(file.getName());
    }

    public boolean beginImport(File file) {
        return beginImport(file, Collections.emptySet());
    }

    @Override
    public boolean beginImport(File file, Set<SortFlags> flags) {
        if (file == null)
            return false;

        final File dest = getDestinationPath(file);
        if (dest == null)
            return false;

        // Nothing to do
        if (file.equals(dest))
            return true;

        File parent = dest.getParentFile();
        if (!IOProviderFactory.exists(parent)
                && !IOProviderFactory.mkdirs(parent)) {
            Log.e(TAG, "could not create: " + parent);
            return false;
        }

        if (!FileSystemUtils.renameTo(file, dest)) {
            // renameTo attempts to copy and delete if rename fails
            // If we get to this point then every rename strategy failed
            Log.e(TAG, "Failed to rename " + file + " to " + dest);
            return false;
        }

        onFileSorted(file, dest, flags);

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        if (file == null)
            return null;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return readDtedFile(fis, file.getName());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtils.close(fis);
        }
    }

    @Override
    public String getDisplayableName() {
        return _context.getString(R.string.dted_cell);
    }

    @Override
    public Drawable getIcon() {
        return _context.getDrawable(R.drawable.ic_overlay_dted);
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("DTED", "application/dted");
    }

    private static boolean isDted(String s) {
        if (s == null)
            return false;

        s = s.toLowerCase(LocaleUtil.getCurrent());

        return s.endsWith(".dt3") || s.endsWith(".dt2") ||
                s.endsWith(".dt1") || s.endsWith(".dt0");
    }

    /**
     * Reads the data on the .dt file for the latitude/longitude values to construct a proper file
     * object that contains the longitude parent folder with the proper n or s filename naming convention
     *
     * @param inputStream - InputStream of file/zip
     * @param fileName    - name of file
     * @return file object that represents the expected path of the dted file
     */
    protected static File readDtedFile(InputStream inputStream,
            String fileName) {

        try (InputStream is = inputStream) {
            char[] b = new char[24];
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is));
            int numRead;
            try {
                numRead = reader.read(b);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 24)
                return null;

            // based on the DTED standard see https://geoservice.dlr.de/web/dataguide/srtm/pdfs/SRTM-XSAR-DEM-DTED-1.1.pdf
            String filename = (b[19] + "" + b[13] + "" + b[14])
                    .toLowerCase(LocaleUtil.US);
            String basedir = (b[11] + "" + b[4] + "" + b[5] + "" + b[6])
                    .toLowerCase(LocaleUtil.US);
            String origName = fileName
                    .toLowerCase(LocaleUtil.getCurrent());
            String extension = origName.substring(origName.lastIndexOf('.'));

            return new File(FileSystemUtils.getItem("DTED"),
                    FileSystemUtils.sanitizeWithSpacesAndSlashes(
                            basedir + "/" + filename + extension));

        } catch (IOException e) {
            Log.e(TAG, "Error checking dted file: "
                    + fileName, e);
        }

        return null; //might be a bad dted file
    }
}
