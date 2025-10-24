
package com.atakmap.android.importfiles.sort;

import android.app.Notification;
import android.content.Context;
import android.util.Pair;

import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;
import com.atakmap.util.zip.ZipEntry;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportDTEDZResolver;

/**
 * Imports archived DTED folders
 * The root directory in the zip file needs to be DTED 
 *
 * @deprecated use {@link ImportDTEDZResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportDTEDZSort extends ImportResolver {

    private static final String TAG = "ImportDTEDZSort";

    private final Context _context;

    public static class ImportDTEDZv1Sort extends ImportDTEDZSort {

        /**
         * Construct an importer that works on zipped DTED files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         *
         * @param context     the extension to be used
         * @param validateExt if the extension needs to be validated
         * @param copyFile    if the file needs to be copied
         * @param bStrict     strict if the mission package is required to have a manifest
         */
        public ImportDTEDZv1Sort(Context context, boolean validateExt,
                boolean copyFile, boolean bStrict) {

            super(context, ".zip", validateExt, copyFile, bStrict);

        }
    }

    public static class ImportDTEDZv2Sort extends ImportDTEDZSort {

        /**
         * Construct an importer that works on zipped DTED files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         *
         * @param context     the extension to be used
         * @param validateExt if the extension needs to be validated
         * @param copyFile    if the file needs to be copied
         * @param bStrict     strict if the mission package is required to have a manifest
         */
        public ImportDTEDZv2Sort(Context context, boolean validateExt,
                boolean copyFile, boolean bStrict) {

            super(context, ".dpk", validateExt, copyFile, bStrict);

        }
    }

    protected ImportDTEDZSort(Context context, String ext, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(ext, FileSystemUtils.DTED_DIRECTORY,
                context.getString(R.string.zipped_dted),
                context.getDrawable(R.drawable.ic_overlay_dted));
        _context = context;
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

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("Zipped DTED", "application/zip");
    }

    private static boolean containsDT(String s) {
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
    private static boolean hasDTED(final File file) {
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
                            s.startsWith("w") || s.startsWith("e")) {
                        hasDTED = true;
                    } else {
                        //check the actual file itself if we can read it or not for a valid dted file
                        //fallback to read the data in the dted file since its not marked correctly on the filename
                        InputStream zis = null;
                        try {
                            zis = zip.getInputStream(ze);
                            hasDTED = ImportDTEDSort.readDtedFile(zis,
                                    name) != null;
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
    public boolean beginImport(File file, Set<SortFlags> flags) {
        return installDTED(file);
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        installDTED(src);
    }

    private boolean installDTED(File dtedFile) {
        byte[] buffer = new byte[4096];
        String entry;
        boolean error = false;

        int notificationId = NotificationUtil.getInstance().reserveNotifyId();

        NotificationUtil.getInstance().postNotification(
                notificationId,
                NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                NotificationUtil.BLUE,
                _context.getString(R.string.importing_dted), null, null, true);

        Notification.Builder builder = NotificationUtil.getInstance()
                .getNotificationBuilder(notificationId);
        ZipFile zFile = null;
        try {
            zFile = new ZipFile(dtedFile);
            // create output directory is not exists
            File folder = FileSystemUtils
                    .getItem(FileSystemUtils.DTED_DIRECTORY);
            if (!IOProviderFactory.exists(folder)) {
                if (!IOProviderFactory.mkdir(folder)) {
                    Log.d(TAG, "could not wrap: " + folder);
                }
            }

            // get the zipped file list entry
            Enumeration<? extends ZipEntry> entries = zFile.entries();

            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();

                entry = ze.getName();
                File f = new File(entry);

                String filename = f.getName();
                File newFile = null;

                if (f.getParentFile() != null) {
                    String parent = f.getParentFile().getName();
                    if (parent.startsWith("e") || parent.startsWith("w") ||
                            parent.startsWith("E") || parent.startsWith("W"))
                        filename = parent + "/" + filename;
                }

                if (containsDT(filename)) {

                    String filenamelc = filename
                            .toLowerCase(LocaleUtil.getCurrent());

                    if ((filenamelc.length() > 4 &&
                            (filenamelc.startsWith("e")
                                    || filenamelc.startsWith("w"))
                            &&
                            (filenamelc.charAt(4) == 'n'
                                    || filenamelc.charAt(4) == 's'))) {
                        String ew = filename.substring(0, 4);
                        String ns = filename.substring(4, 7);
                        char lastChar = filename.charAt(filename.length() - 1);

                        newFile = new File(folder,
                                FileSystemUtils
                                        .sanitizeWithSpacesAndSlashes(
                                                (ew + "/" + ns + ".dt"
                                                        + lastChar)
                                                                .toLowerCase(
                                                                        LocaleUtil
                                                                                .getCurrent())));

                    } else if (filename.startsWith("e")
                            || filename.startsWith("w") ||
                            filename.startsWith("E")
                            || filename.startsWith("W")) {

                        newFile = new File(folder,
                                FileSystemUtils.sanitizeWithSpacesAndSlashes(
                                        filename.toLowerCase(
                                                LocaleUtil.getCurrent())));
                        //Log.d(TAG, "dted file encountered: " + filename + " creating: " + newFile);
                    } else if (filename.startsWith("n")
                            || filename.startsWith("s") ||
                            filename.startsWith("N")
                            || filename.startsWith("S")) {
                        String[] s = filename.split("_");
                        String ext = filename.substring(filename.indexOf("."));
                        if (s.length > 1) {
                            newFile = new File(folder,
                                    FileSystemUtils
                                            .sanitizeWithSpacesAndSlashes(
                                                    (s[1] + "/" + s[0] + ext)
                                                            .toLowerCase(
                                                                    LocaleUtil
                                                                            .getCurrent())));
                        }
                        //Log.d(TAG, "usgs file encountered: " + filename + " creating: " + newFile);
                    } else {
                        //fallback to read the data in the dted file since its not marked correctly on the filename
                        InputStream zis = null;
                        try {
                            zis = zFile.getInputStream(ze);
                            newFile = ImportDTEDSort.readDtedFile(zis,
                                    filename);
                        } catch (IOException ioe) {
                            Log.e(TAG,
                                    "error reading the entry: " + ze.getName());
                        } finally {
                            IoUtils.close(zis);
                        }
                    }
                }

                if (newFile != null) {

                    if (count % 10 == 0) {
                        final File pFile = newFile.getParentFile();

                        builder.setContentText(String.format(
                                _context.getString(
                                        R.string.importmgr_processing_file),
                                pFile != null ? pFile.getName() : "null",
                                newFile.getName()));
                        try {
                            Notification summaryNotification = builder.build();
                            NotificationUtil.getInstance().postNotification(
                                    notificationId,
                                    summaryNotification, true);
                        } catch (ConcurrentModificationException cme) {
                            // ATAK-17986 Playstore Crash: ImportDTEDZSort ConcurrentModification
                        }
                    }
                    count++;

                    // create all non exists folders
                    // else you will hit FileNotFoundException 
                    // for compressed folder
                    final String pFile = newFile.getParent();
                    if (pFile != null && !(IOProviderFactory
                            .mkdirs(new File(pFile)))) {
                        Log.d(TAG, "could not make: " + newFile.getParent());
                    }

                    try (FileOutputStream fos = IOProviderFactory
                            .getOutputStream(newFile)) {

                        int len;
                        InputStream zis = null;
                        try {
                            zis = zFile.getInputStream(ze);
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG,
                                    "error reading the entry: " + ze.getName());
                        } finally {
                            IoUtils.close(zis);
                        }

                    } catch (IOException ex) {
                        // skip this file
                        Log.d(TAG, "error occurred during unzipping", ex);
                    }
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "error", ex);
            error = true;
        } finally {
            NotificationUtil.getInstance().clearNotification(notificationId);
            NotificationUtil
                    .getInstance()
                    .postNotification(
                            notificationId,
                            NotificationUtil.GeneralIcon.SYNC_SUCCESS.getID(),
                            NotificationUtil.GREEN,
                            String.format(
                                    _context.getString(
                                            R.string.importmgr_dted_import_completed),
                                    ((error)
                                            ? _context
                                                    .getString(
                                                            R.string.importmgr_with_errors)
                                            : "")),
                            null, null, true);
            IoUtils.close(zFile);
        }
        return !error;
    }
}
