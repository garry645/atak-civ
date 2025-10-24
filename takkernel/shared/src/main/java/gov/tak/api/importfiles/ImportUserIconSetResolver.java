package gov.tak.api.importfiles;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Does not move the input file. Must contain at least one image, and a valid iconset.xml in root of
 * zip file.
 */
public class ImportUserIconSetResolver extends ImportResolver {

    private static final String TAG = "ImportUserIconSetSort";
    public static final String ICONSET_XML = "iconset.xml";
    private final static String ICONSET_XML_MATCH = "<iconset";

    public ImportUserIconSetResolver(String displayName, File destinationDir, Drawable icon) {
        super(".zip", destinationDir, displayName, icon);
        contentType = "User Icon Set";
        mimeType = "application/zip";
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it contains an iconset.xml
        return hasIconset(file, true);
    }

    /**
     * Search for a zip entry matching iconset.xml and at least one .png
     *
     * @param file
     * @param requireXml
     * @return
     */
    public static boolean hasIconset(File file, boolean requireXml) {

        if (file == null || !IOProviderFactory.exists(file)) {
            Log.d(TAG,
                    "ZIP does not exist: "
                            + (file == null ? "null" : file.getAbsolutePath()));
            return false;
        }

        ZipFile zip = null;
        try {
            zip = new ZipFile(file);

            boolean bIconsetXml = false, bHasImage = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().toLowerCase(LocaleUtil.getCurrent())
                        .endsWith(ICONSET_XML)) {
                    if (isIconsetXml(zip.getInputStream(ze))) {
                        bIconsetXml = true;
                    } else {
                        Log.w(TAG,
                                "Found invalid archived Zip file: "
                                        + ze.getName());
                    }
                } else if (IconFilenameFilter.accept(null,
                        ze.getName())) {
                    bHasImage = true;
                }

                if (bIconsetXml && bHasImage) {
                    // found what we needed, quit looping
                    break;
                }
            }

            if (!bHasImage) {
                Log.w(TAG,
                        "Invalid iconset (no image): "
                                + file.getAbsolutePath());
                return false;
            }

            if (requireXml && !bIconsetXml) {
                Log.w(TAG,
                        "Invalid iconset (XML required): "
                                + file.getAbsolutePath());
                return false;
            }

            Log.d(TAG, "Matched iconset: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.d(TAG,
                    "Failed to find iconset content in: "
                            + file.getAbsolutePath(),
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed closing iconset: " + file.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }

    public static final FilenameFilter IconFilenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            String fn = filename.toLowerCase(LocaleUtil.getCurrent());
            return fn.endsWith(".jpg")
                    || fn.endsWith(".jpeg")
                    || fn.endsWith(".png")
                    || fn.endsWith(".bmp")
                    || fn.endsWith(".webp")
                    || fn.endsWith(".gif");
        }
    };

    private static boolean isIconsetXml(InputStream input) {
        try {
            // read first few hundred bytes and search for known iconset strings
            char[] buffer = new char[1024];
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    input));
            int numRead = -1;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read iconset stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(ICONSET_XML_MATCH);
            if (!match) {
                Log.d(TAG, "Failed to match iconset content");
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match iconset", e);
            return false;
        }
    }

    @Override
    protected void onFileSorted(File src, File unused, EnumSet<SortFlags> flags) {
        notifyFileSortedListeners(src, unused, flags);
    }

    @Override
    public void filterFoundResolvers(final List<ImportResolver> importResolvers,
                                     File file) {
        // Remove data package sorters
        for (int i = 0; i < importResolvers.size(); i++) {
            ImportResolver ir = importResolvers.get(i);
            if (ir instanceof ImportMissionPackageResolver)
                importResolvers.remove(i--);
        }
    }
}
