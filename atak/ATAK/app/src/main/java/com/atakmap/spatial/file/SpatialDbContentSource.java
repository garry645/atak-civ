
package com.atakmap.spatial.file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.features.FeatureDataStoreDeepMapItemQuery;
import com.atakmap.android.features.FeatureDataStoreMapOverlay;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.importexport.Importer;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.overlay.MapOverlay;
import com.atakmap.android.util.NotificationIdRecycler;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.R;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.control.DataSourceDataStoreControl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * A source of content for the spatial database. The content source will process directories and
 * files, as instructed, and populate the spatial database and update the catalog appropriately.
 * <P>
 * Implementors may expect that all directory and file processing will occur in a thread-safe manner
 * and that transactions will be applied to the spatial database and catalog externally.
 */
public abstract class SpatialDbContentSource implements Importer {

    /**
     * Flag indicating that the current file should be processed for content.
     */
    public final static int PROCESS_ACCEPT = 0;

    /**
     * Flag indicating that the current file should not undergo further processing.
     */
    public final static int PROCESS_REJECT = -1;

    /**
     * Flag indicating that processing should recurse into the current directory.
     */
    public final static int PROCESS_RECURSE = 1;

    private static final String TAG = "SpatialDbContentSource";

    public static final String ATAK_ACTION_HANDLE_DATA = "com.atakmap.map.HANDLE_DATA";
    public static final String ATAK_GROUP_NAME_EXTRA = "group";

    /**************************************************************************/

    protected final String groupName;
    protected final String type;

    /** @deprecated use {@link #datasource} /  {@link #datastore} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected final DataSourceFeatureDataStore database;
    protected final FeatureDataStore2 datastore;
    protected final DataSourceDataStoreControl datasource;

    protected final boolean notifyUserAfterImport;

    protected SpatialDbContentResolver contentResolver;

    private static final NotificationIdRecycler _notificationId;
    static {
        _notificationId = new NotificationIdRecycler(85000, 4);
    }

    protected SpatialDbContentSource(DataSourceFeatureDataStore database,
            String groupName, String type) {
        this(Adapters.adapt(database), database, groupName, type);
    }

    protected SpatialDbContentSource(FeatureDataStore2 database,
            String groupName, String type) {
        this(database, null, groupName, type);
    }

    private SpatialDbContentSource(FeatureDataStore2 datastore,
            DataSourceFeatureDataStore database,
            String groupName, String type) {
        this.datastore = datastore;
        this.datasource = getDataSource(datastore);
        this.database = database;
        this.groupName = groupName;
        this.type = type;

        this.notifyUserAfterImport = true;
    }

    /**
     * Returns the data base that backs the features.
     * @return <B>!!!ALWAYS RETURNS {@code null}!!!</B>
     * @deprecated to be removed without replacement
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected final DataSourceFeatureDataStore getDatabase() {
        return this.database;
    }

    /**
     * Dispose the importer - to be called when the map component is destroyed
     */
    public void dispose() {
        setContentResolver(null);
    }

    /**
     * Returns the type of the content.
     * 
     * @return  The type of the content
     */
    public final String getType() {
        return this.type;
    }

    /**
     * Returns the group name for the content.
     * 
     * @return The group name for the content.
     */
    public final String getGroupName() {
        return this.groupName;
    }

    /**
     * Returns the principal directory containing the content (e.g. atak/KML).
     * 
     * @return The principal directory containing the content.
     */
    public abstract String getFileDirectoryName();

    /**
     * Returns the MIME type associated with the content.
     * 
     * @return The MIME type associated with the content.
     */
    public abstract String getFileMimeType();

    /**
     * Returns the path to an icon appropriate for the content.
     * 
     * @return The path to an icon appropriate for the content. May be <code>null</code> to indicate
     *         no icon.
     */
    public abstract String getIconPath();

    /**
     * Returns the Resource ID of the icon drawable (used in notifications)
     * @return the resource id of the icon.
     */
    public abstract int getIconId();

    /**
     * Set the content resolver for this importer
     * The resolver will be notified of any adds, updates, or removals of
     * database content
     *
     * @param resolver Content resolver or null to dispose
     */
    public void setContentResolver(SpatialDbContentResolver resolver) {
        if (resolver == this.contentResolver)
            return;
        if (this.contentResolver != null) {
            URIContentManager.getInstance().unregisterResolver(
                    this.contentResolver);
            this.contentResolver.dispose();
        }
        this.contentResolver = resolver;
        if (resolver != null)
            URIContentManager.getInstance().registerResolver(resolver);
    }

    public SpatialDbContentResolver getContentResolver() {
        return this.contentResolver;
    }

    public MapOverlay createOverlay(Context context,
            FeatureDataStoreDeepMapItemQuery query) {
        return new FeatureDataStoreMapOverlay(
                context, this.datastore, this.getType(),
                this.getGroupName(), this.getIconPath(),
                query,
                this.getContentType(),
                this.getFileMimeType());
    }

    /**
     * Processes the specified file. The file is not currently cataloged in the database.
     * 
     * @param file      The file to be processed.
     */
    public boolean processFile(File file) {
        final boolean update = this.datasource.contains(file);

        boolean success = false;

        DataIngestReport report = new DataIngestReport();
        try {
            if (update)
                success = this.datasource.update(file);
            else
                success = this.datasource.add(file, this.getProviderHint(file));
            if (!success)
                report.fatalError("Failed to parse " + file.getName());
        } catch (Exception t) {
            report.fatalError("Failed to parse " + file.getName(), t);
            Log.d(TAG, "failed to parse: " + file.getName(), t);
        } finally {
            notifyUserOfError(report, file);
        }

        // Add handler to the content resolver
        if (success && this.contentResolver != null)
            this.contentResolver.addHandler(file);

        return success;
    }

    /**
     * Processes the specified file. If the file contains valid content, the features should be
     * added to the spatial database and a catalog entry created for the file.
     *
     * @param file The file to be processed.
     */
    protected abstract String getProviderHint(File file);

    /**
     * Returns a flag indicating whether or not the catalog entry is valid.
     * 
     * @param file A file. The catalog may or may not contain an entry for the file.
     * @return <code>false</code> if there is no catalog entry for the file or if the file fails
     */
    protected boolean checkValidEntry(File file) {
        if (!this.datasource.contains(file))
            return false;
        /*
        if (!result.getAppName().equals(this.getName()))
            return false;
        return this.isValidApp(file, result.getAppVersion(), result.getAppData());
        */
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new {@link MapGroup} for the source using the source's group name and icon. The
     * group's <code>ignoreOffscreen</code> metadata is set to <code>true</code>.
     * 
     * @param contentSource The source
     * @return A new {@link MapGroup} for the source.
     */
    public static MapGroup makeMapGroup(SpatialDbContentSource contentSource) {
        MapGroup group = new DefaultMapGroup(contentSource.getGroupName());
        group.setMetaString("iconUri", contentSource.getIconPath());
        group.setMetaBoolean("ignoreOffscreen", true);
        return group;
    }

    /**
     * Returns a flag instructing the process loop how to handle the file.
     * 
     * @param f The file to be processed
     * @param depth The current recursion depth
     * @return Returns one of {@link #PROCESS_ACCEPT}, {@link #PROCESS_RECURSE} or
     *         {@link #PROCESS_REJECT}.
     */
    public int processAccept(File f, int depth) {
        if (IOProviderFactory.isFile(f) && IOProviderFactory.canRead(f))
            return PROCESS_ACCEPT;
        else if (IOProviderFactory.isDirectory(f))
            return PROCESS_RECURSE;
        else
            return PROCESS_REJECT;
    }

    public static void notifyUserOfError(DataIngestReport report,
            File fromFile) {
        if (report != null
                && (report.gotFatalError() || report.gotContinuableError())) {
            notifyUserOfError(fromFile,
                    report.getNotificationTicker(),
                    report.getNotificationMessage());
        }
    }

    public static void notifyUserOfError(File file, String ticker,
            String message) {
        String filename = (file == null ? "[NULL]" : file.getName());
        notifyUserOfError("Error processing file: " + filename,
                filename + ": " + ticker, message);
    }

    protected static void notifyUserOfError(String allThree) {
        notifyUserOfError(allThree, allThree, allThree);
    }

    public static void notifyUserOfError(String title, String ticker,
            String message) {
        NotificationUtil.getInstance().postNotification(getNotificationId(),
                R.drawable.select_point_icon, NotificationUtil.WHITE, title,
                ticker, message);
    }

    /**************************************************************************/
    // Importer

    @Override
    public Set<String> getSupportedMIMETypes() {
        return Collections.singleton(this.getFileMimeType());
    }

    @Override
    public ImportResult importData(InputStream source, String mime,
            Bundle bundle) {
        return ImportResult.FAILURE;
    }

    @Override
    public ImportResult importData(Uri uri, String mime, Bundle b) {
        if (uri.getScheme() != null && !uri.getScheme().equals("file"))
            return ImportResult.FAILURE;

        boolean showMapNotifications = b != null && b.getBoolean(
                ImportReceiver.EXTRA_SHOW_NOTIFICATIONS);
        boolean zoomToFile = b != null && b.getBoolean(
                ImportReceiver.EXTRA_ZOOM_TO_FILE);
        boolean hideFile = b != null && b.getBoolean(
                ImportReceiver.EXTRA_HIDE_FILE);

        final String uriPath = uri.getPath();

        Log.d(TAG, "importData: " + uriPath);

        if (uriPath == null) {
            Log.d(TAG, "null uri path for: " + uri);
            return ImportResult.FAILURE;
        }

        File file;
        try {
            file = new File(FileSystemUtils.validityScan(uriPath));
        } catch (IOException ioe) {
            Log.d(TAG, "invalid file", ioe);
            return ImportResult.FAILURE;
        }

        final int notificationId = NotificationUtil.getInstance()
                .reserveNotifyId();
        if (showMapNotifications) {
            NotificationUtil.getInstance().postNotification(
                    notificationId,
                    NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                    NotificationUtil.BLUE,
                    "Starting Import: " + file.getName(), null, null, false);
        }

        //remove existing data if it exists
        boolean newlyAdded = true;
        if (this.datasource.contains(file)) {

            //get current visibility of file layer that is being updated
            //use this as hideFile so visibility of that layer after processing new file will match the
            //previous layer visibility
            URIContentHandler h = contentResolver.getHandler(file);
            if (h != null && h.isActionSupported(Visibility.class))
                hideFile = !((Visibility) h).isVisible();

            Log.d(TAG, "Removing existing file: " + file.getAbsolutePath());
            this.datasource.remove(file);
            newlyAdded = false;
        }

        //now import data
        boolean success = this.processFile(file);
        final ImportResult retval = success ? ImportResult.SUCCESS
                : ImportResult.FAILURE;
        if (notifyUserAfterImport) {
            Intent i = new Intent(ImportExportMapComponent.ZOOM_TO_FILE_ACTION);
            i.putExtra("filepath", file.getAbsolutePath());
            if (showMapNotifications) {
                if (success) {
                    NotificationUtil.getInstance().postNotification(
                            notificationId,
                            NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                            NotificationUtil.GREEN,
                            "Finished Import: " + file.getName(), null, i,
                            true);
                } else {
                    NotificationUtil.getInstance().postNotification(
                            notificationId,
                            NotificationUtil.GeneralIcon.SYNC_ERROR.getID(),
                            NotificationUtil.RED,
                            "Failed Import: " + file.getName(), null, null,
                            true);

                }
            }
            if (success && zoomToFile)
                AtakBroadcast.getInstance().sendBroadcast(i);
        }

        // Hide the file that was just imported, if applicable
        if (success && hideFile) {
            URIContentHandler h = contentResolver.getHandler(file);
            if (h != null && h.isActionSupported(Visibility.class))
                ((Visibility) h).setVisible(false);
        }

        // Remove existing files that failed to import from the content resolver
        if (this.contentResolver != null && !success && !newlyAdded)
            this.contentResolver.removeHandler(file);

        // For overlay manager refresh
        refreshOverlayManager();

        return retval;
    }

    @Override
    public boolean deleteData(Uri uri, String mime) {
        final String uriPath = uri.getPath();
        File file;

        Log.d(TAG, "importData: " + uriPath);

        if (uriPath == null) {
            Log.d(TAG, "null uri path for: " + uri);
            return false;
        }

        try {
            file = new File(FileSystemUtils.validityScan(uriPath));
        } catch (IOException ioe) {
            Log.d(TAG, "invalid file", ioe);
            return false;
        }

        //remove from UI handled by GLSpatialDbRenderer pulling its data from DB

        //remove from DB
        this.datasource.remove(file);

        // Update the content resolver
        if (this.contentResolver != null)
            this.contentResolver.removeHandler(file);

        // Notify OM
        refreshOverlayManager();

        return true;
    }

    /**
     * Reuse from just a few IDs to avoid many many notifications
     * 
     * @return the notification id
     */
    public static int getNotificationId() {
        return _notificationId.getNotificationId();
    }

    public void deleteAll() {
        if (this.datasource != null) {
            Set<File> filesToRemove = new HashSet<>();

            FeatureSetCursor result = null;
            try {
                FeatureDataStore2.FeatureSetQueryParameters params = new FeatureDataStore2.FeatureSetQueryParameters();
                params.types = Collections.singleton(this.type);

                result = this.datastore.queryFeatureSets(params);
                File f;
                while (result.moveToNext()) {
                    f = this.datasource.getFile(result.get());
                    if (f != null)
                        filesToRemove.add(f);
                }
            } catch (DataStoreException ignored) {
            } finally {
                if (result != null)
                    result.close();
            }

            for (File f : filesToRemove) {
                this.datasource.remove(f);
                if (this.contentResolver != null)
                    this.contentResolver.removeHandler(f);
            }

            refreshOverlayManager();
        }
    }

    private void refreshOverlayManager() {
        // Let overlay manager know the cache has been cleared
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(HierarchyListReceiver.REFRESH_HIERARCHY));
    }

    static DataSourceDataStoreControl getDataSource(FeatureDataStore2 store) {
        if (store instanceof DataSourceDataStoreControl)
            return (DataSourceDataStoreControl) store;
        if (store instanceof Controls) {
            final Controls controls = (Controls) store;
            final DataSourceDataStoreControl ctrl = controls
                    .getControl(DataSourceDataStoreControl.class);
            if (ctrl != null)
                return ctrl;
        }

        throw new IllegalArgumentException();
    }
}
