package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Set;
import java.util.List;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports archived DTED folders
 * The root directory in the zip file needs to be DTED
 *
 */
public class ImportDTEDZResolver extends ImportResolver {

    private static final String TAG = "ImportDTEDZSort";

    public static class ImportDTEDZv1Resolver extends ImportDTEDZResolver {

        /**
         * Construct an importer that works on zipped DTED files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         */
        public ImportDTEDZv1Resolver(String displayName, File destinationDir, Drawable icon) {

            super(".zip", destinationDir, displayName, icon);
        }
    }

    public static class ImportDTEDZv2Resolver extends ImportDTEDZResolver {

        /**
         * Construct an importer that works on zipped DTED files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         */
        public ImportDTEDZv2Resolver(String displayName, File destinationDir, Drawable icon) {

            super(".dpk", destinationDir, displayName, icon);

        }
    }

    protected ImportDTEDZResolver(String ext, File destinationDir, String displayName, Drawable icon) {
        super(ext, destinationDir, displayName, icon);
        contentType = "Zipped DTED";
        mimeType = "application/zip";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it contains a DTED directory
        // but no manifest.
        return hasDTED(file);
    }

    @Override
    public void filterFoundResolvers(List<ImportResolver> importResolvers, File file) {
        // this should be the only sorter used for zipped dted
        importResolvers.clear();
        importResolvers.add(this);
    }


    public static boolean containsDT(String s) {
        if (s == null)
            return false;

        s = s.toLowerCase(LocaleUtil.getCurrent());

        return s.endsWith(".dt3") || s.endsWith(".dt2") ||
                s.endsWith(".dt1") || s.endsWith(".dt0");

    }

    /**
     * Search for a zip entry DTED, but no MANIFEST directory
     *
     * @param file the zip file
     * @return true if the zip file has DTED entries
     */
    private boolean hasDTED(final File file) {
        if (file == null) {
            Log.d(TAG, "ZIP file points to null.");
            return false;
        }

        if (!IOProviderFactory.exists(file)) {
            Log.d(TAG, "ZIP does not exist: " + file.getAbsolutePath());
            return false;
        }

        ZipFile zip = null;
        try {
            zip = new ZipFile(file);

            boolean hasDTED = false;
            Enumeration<? extends com.atakmap.util.zip.ZipEntry> entries = zip
                    .entries();
            while (entries.hasMoreElements() && !hasDTED) {
                com.atakmap.util.zip.ZipEntry ze = entries.nextElement();
                String name = ze.getName();
                if (containsDT(name)) {
                    File f = new File(name);
                    String s = f.getName();
                    if (s.startsWith("n") || s.startsWith("s") ||
                            s.startsWith("w") || s.startsWith("e")){
                        hasDTED = true;
                    }else {
                        //check the actual file itself if we can read it or not for a valid dted file
                        //fallback to read the data in the dted file since its not marked correctly on the filename
                        InputStream zis = null;
                        try {
                            zis = zip.getInputStream(ze);
                            hasDTED = ImportDTEDResolver.readDtedFile(zis, name, destinationDir) != null;
                        } catch (IOException ioe) {
                            Log.e(TAG,
                                    "error reading the entry: " + ze.getName());
                        } finally {
                            IoUtils.close(zis);
                        }
                    }
                }
            }

            if (hasDTED)
                Log.d(TAG, "Matched archived DTED: " + file.getAbsolutePath());

            return hasDTED;

        } catch (Exception e) {
            Log.d(TAG,
                    "Failed to find DTED content in: " + file.getAbsolutePath(),
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed closing archived DTED: "
                                    + file.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }

    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        return notifyBeginImportListeners(file, flags, null);
    }

    @Override
    protected void onFileSorted(File src, File dst, EnumSet<SortFlags> flags) {
        notifyFileSortedListeners(src, dst, flags);
    }
}
