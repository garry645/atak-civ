package com.atakmap.android.importfiles.callbacks;

import android.app.Notification;
import android.content.Context;

import com.atakmap.android.maps.MapView;
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
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Set;

import gov.tak.api.importfiles.ImportDTEDResolver;
import gov.tak.api.importfiles.ImportDTEDZResolver;
import gov.tak.api.importfiles.ImportResolver;

final class DTEDZCallback implements ImportResolver.BeginImportListener, ImportResolver.FileSortedListener {

    private static final String TAG = "DTEDZCallback";

    private final Context _context;

    public DTEDZCallback() {
        _context = MapView.getMapView().getContext();
    }

    @Override
    public boolean onBeginImport(File file, EnumSet<ImportResolver.SortFlags> sortFlags, Object opaque) {
        return installDTED(file);
    }

    @Override
    public void onFileSorted(ImportResolver.FileSortedNotification notification) {
        installDTED(new File(notification.getDstUri()));
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

                if (ImportDTEDZResolver.containsDT(filename)) {

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
                    }else {
                        //fallback to read the data in the dted file since its not marked correctly on the filename
                        InputStream zis = null;
                        try {
                            zis = zFile.getInputStream(ze);
                            newFile = ImportDTEDResolver.readDtedFile(zis, filename, FileSystemUtils.getItem(FileSystemUtils.DTED_DIRECTORY));
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
