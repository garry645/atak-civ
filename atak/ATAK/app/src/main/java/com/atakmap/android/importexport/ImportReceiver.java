
package com.atakmap.android.importexport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import gov.tak.api.importfiles.ImportResolver;

public class ImportReceiver extends BroadcastReceiver implements ImportResolver.FileSortedListener {

    public static final String TAG = "ImportReceiver";

    public final static String EXTRA_CONTENT = "contentType";
    public final static String EXTRA_MIME_TYPE = "mimeType";
    public final static String EXTRA_URI = "uri";
    public final static String EXTRA_URI_LIST = "uriList";
    public final static String EXTRA_SHOW_NOTIFICATIONS = "showNotifications";
    public final static String EXTRA_ZOOM_TO_FILE = "zoomToFile";
    public final static String EXTRA_HIDE_FILE = "hideFile";
    public final static String EXTRA_ADVANCED_OPTIONS = "advanced";

    ImportReceiver() {
    }

    @Override
    public void onFileSorted(ImportResolver.FileSortedNotification notification) {
        String uriStr = notification.getDstUri();
        String content = notification.getContentType();
        String mimeType = notification.getMimeType();
        Log.d(TAG, "ImportResolver.FileSortedListener.onFileSorted callback received: " +
                " uri=" + uriStr + " content=" + content + " mime=" + mimeType);

        List<Uri> uris = null;
        if (!FileSystemUtils.isEmpty(uriStr)) {
            Uri uri = null;
            try {
                uri = Uri.parse(uriStr);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse URI string: " + uriStr, e);
            }
            if (uri != null)
                uris = Collections.singletonList(uri);
        } else {
            Log.e(TAG, "Import requires URI or URI list.");
            return;
        }
        if (uris == null) {
            Log.e(TAG, "Unable to import from null Uri");
            return;
        }

        Bundle extras = new Bundle();
        extras.putString(ImportReceiver.EXTRA_URI, uriStr);
        extras.putString(ImportReceiver.EXTRA_CONTENT, content);
        extras.putString(ImportReceiver.EXTRA_MIME_TYPE, mimeType);

        Set<ImportResolver.SortFlags> sortedFlags = notification.getSortFlags();
        if (sortedFlags.contains(ImportResolver.SortFlags.SHOW_NOTIFICATIONS))
            extras.putBoolean(ImportReceiver.EXTRA_SHOW_NOTIFICATIONS, true);
        if (sortedFlags.contains(ImportResolver.SortFlags.ZOOM_TO_FILE))
            extras.putBoolean(ImportReceiver.EXTRA_ZOOM_TO_FILE, true);
        if (sortedFlags.contains(ImportResolver.SortFlags.HIDE_FILE))
            extras.putBoolean(ImportReceiver.EXTRA_HIDE_FILE, true);
        importParserPool.execute(new ImportParserThread(ACTION.IMPORT, uris, notification.getContentType(),
                notification.getMimeType(), extras));
    }

    /******************* PRIVATE PARSING THREAD ******************/
    private enum ACTION {
        IMPORT,
        DELETE
    }

    private static class ImportParserThread implements Runnable {

        private final ACTION action;
        private final List<Uri> uris;
        private String application;
        private String mime;
        private final Bundle bundle;

        public ImportParserThread(ACTION action, List<Uri> uris,
                String application, String mime, Bundle bundle) {
            this.action = action;
            this.uris = uris;
            this.application = application;
            this.mime = mime;
            this.bundle = bundle;
        }

        @Override
        public void run() {
            Log.d(TAG, "processing: " + action + " " + bundle);
            for (Uri uri : this.uris) {
                try {
                    if (this.application == null || this.mime == null) {
                        if (this.application != null && this.mime == null) {
                            this.mime = MarshalManager
                                    .marshal(application, uri);
                        } else if (this.application == null) {
                            Pair<String, String> info = MarshalManager
                                    .marshal(uri);
                            if (info == null) {
                                Log.e(TAG,
                                        "unable to determine content for Uri "
                                                + uri);
                                return;
                            }

                            this.application = info.first;
                            this.mime = info.second;
                        }
                    }

                    Importer importer = ImporterManager.findImporter(
                            this.application, this.mime);
                    if (importer != null && this.mime != null) {
                        if (action == ACTION.DELETE) {
                            importer.deleteData(uri, mime);
                            Log.d(TAG, importer.getClass()
                                    + " delete from database: " + uri
                                    + " scheme: " + uri.getScheme());
                            //TODO NPE hard crash with ":" in filename
                            final String uriPath = uri.getPath();
                            if (uriPath == null) {
                                Log.d(TAG, "cannot delete file null uriPath: "
                                        + uri);
                                return;
                            }

                            File f = new File(FileSystemUtils
                                    .validityScan(uriPath));
                            if (uri.getScheme() != null
                                    && uri.getScheme().equals("file") ||
                                    IOProviderFactory.exists(f)) {
                                Log.d(TAG, "delete from file system: " + f);
                                FileSystemUtils.deleteFile(f);
                            } else {
                                Log.d(TAG, "cannot delete non-file: " + uri);
                            }
                        } else {
                            importer.importData(uri, mime, bundle);
                            Log.d(TAG, importer.getClass() + " import: " + uri);
                            Intent importComplete = new Intent(
                                    ImportExportMapComponent.IMPORT_COMPLETE);
                            importComplete.putExtra(EXTRA_URI, uri.toString());
                            AtakBroadcast.getInstance()
                                    .sendBroadcast(importComplete);
                        }
                    } else {
                        Log.e(TAG, "failed to import " + uri.toString()
                                + ", no Importer found.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "failed to open Uri for import " + uri, e);
                }

            }
            Log.d(TAG, "finished processing: " + action + " " + bundle);

            // Refresh Overlay Manager
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    HierarchyListReceiver.REFRESH_HIERARCHY));
        }
    }

    /**************************************************************************/
    // Broadcast Receiver

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "import intent received: " + intent.getAction()
                + " uri=" + intent.getStringExtra(EXTRA_URI)
                + " content=" + intent.getStringExtra(EXTRA_CONTENT)
                + " mime=" + intent.getStringExtra(EXTRA_MIME_TYPE));

        List<Uri> uris = null;
        String uriStr = intent.getStringExtra(EXTRA_URI);
        if (!FileSystemUtils.isEmpty(uriStr)) {
            Uri uri = null;
            try {
                uri = Uri.parse(uriStr);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse URI string: " + uriStr, e);
            }
            if (uri != null)
                uris = Collections.singletonList(uri);
        } else if (intent.hasExtra(EXTRA_URI_LIST)) {
            ArrayList<String> uriList = intent
                    .getStringArrayListExtra(EXTRA_URI_LIST);

            if (uriList != null) {
                uris = new LinkedList<>();
                Uri uri;
                for (String u : uriList) {
                    uri = Uri.parse(u);
                    if (uri != null)
                        uris.add(uri);
                }
            }
        } else {
            Log.e(TAG, "Import requires URI or URI list.");
            return;
        }
        if (uris == null || uris.isEmpty()) {
            Log.e(TAG, "Unable to import from null Uri");
            return;
        }

        final String content = intent.getStringExtra(EXTRA_CONTENT);
        final String mimeType = intent.getStringExtra(EXTRA_MIME_TYPE);

        ACTION action;
        if (ImportExportMapComponent.ACTION_IMPORT_DATA.equals(intent
                .getAction())) {
            action = ACTION.IMPORT;
        } else if (ImportExportMapComponent.ACTION_DELETE_DATA.equals(intent
                .getAction())) {
            action = ACTION.DELETE;
        } else {
            Log.w(TAG, "Ignoring action: " + intent.getAction());
            return;
        }

        importParserPool.execute(new ImportParserThread(action, uris, content,
                mimeType, intent.getExtras()));
    }

    /**
     * Remove list of URIs from import manager
     * Same as ACTION_DELETE_DATA except it's not an intent
     */
    public static void remove(List<Uri> uris, String content, String mimeType) {
        importParserPool.execute(
                new ImportParserThread(ACTION.DELETE, uris,
                        content, mimeType, null));
    }

    public static void remove(Uri uri, String content, String mimeType) {
        List<Uri> uris = new ArrayList<>();
        uris.add(uri);
        remove(uris, content, mimeType);
    }

    private static final ExecutorService importParserPool = Executors
            .newFixedThreadPool(5,
                    new NamedPriorityThreadFactory("ImportParserThread",
                            Thread.NORM_PRIORITY));

    // would like to make this available as part of the takkernel in the future.
    private static class NamedPriorityThreadFactory implements ThreadFactory {

        private int count = 0;
        private final String name;
        private final int priority;

        /**
         * Constructs a Thread factory with a specific name and count for each thread created to assist
         * in debugging.
         * .
         *
         * @param name the name of the threads created by this factory.
         */
        public NamedPriorityThreadFactory(final String name,
                final int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, name + "-" + count++);
            t.setPriority(priority);
            return t;
        }
    }
}
