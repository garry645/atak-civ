
package com.atakmap.android.missionpackage.lasso;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.Toast;

import com.atakmap.android.data.URIContentProvider;
import com.atakmap.android.data.URIHelper;
import com.atakmap.android.gui.TileButtonDialog;
import com.atakmap.android.importexport.ExportDialog;
import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.ExportMarshal;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.export.MissionPackageExportWrapper;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Intent receiver for the standalone lasso tool
 */
public class LassoSelectionReceiver extends BroadcastReceiver {

    private static final String TAG = "LassoSelectionReceiver";

    // General purpose intent to select items using the lasso
    private static final String SELECT = "com.atakmap.android.missionpackage.lasso.SELECT";

    private final MapView _mapView;
    private final Context _context;
    private final LassoContentProvider _provider;

    public LassoSelectionReceiver(MapView mapView,
            LassoContentProvider provider) {
        _mapView = mapView;
        _context = mapView.getContext();
        _provider = provider;

        DocumentedIntentFilter f = new DocumentedIntentFilter();
        f.addAction(SELECT, "Select contents using the lasso tool",
                new DocumentedExtra[] {
                        new DocumentedExtra("callback", "Callback intent", true,
                                Intent.class)
                });
        AtakBroadcast.getInstance().registerReceiver(this, f);
    }

    public void dispose() {
        AtakBroadcast.getInstance().unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {

            // Select content using the lasso tool
            case SELECT: {
                final Intent intentCallback = intent
                        .getParcelableExtra("callback");
                _provider.addContent("Lasso", null,
                        new URIContentProvider.Callback() {
                            @Override
                            public void onAddContent(
                                    URIContentProvider provider,
                                    List<String> uris) {
                                if (intentCallback != null) {
                                    intentCallback.putExtra("uris",
                                            uris.toArray());
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(intentCallback);
                                } else
                                    promptLassoAction(uris);
                            }
                        });
                break;
            }
        }
    }

    private final static LinkedHashMap<String, ExternalLassoCapability> externalCapabilities = new LinkedHashMap<>();

    /**
     * Externally provided Lasso Capability.
     */
    public interface ExternalLassoCapability {

        /**
         * Returns a unique identifier for the external lasso capability
         * @return the unique identifier
         */
        String getUniqueIdentifier();

        /**
         * Returns the drawable associated with the external lasso capability
         * @return the icon to use
         */
        Drawable getIcon();

        /**
         * The name of the external lasso capability
         * @return the name
         */
        String getTitle();

        /**
         * The callback used when processing the map items.
         * @param items the list of items (can be a MapItem or a File)
         */
        void process(List<Object> items);
    }

    /**
     * Registers an external lasso capability.
     * @param externalLassoCapability the external lasso capability.
     */
    synchronized public static void registerExternalLassoCapability(
            ExternalLassoCapability externalLassoCapability) {
        externalCapabilities.put(externalLassoCapability.getUniqueIdentifier(),
                externalLassoCapability);
    }

    /**
     * Unregisters an external lasso capability.
     * @param externalLassoCapability the external lasso capability.
     */
    synchronized public static void unregisterExternalLassoCapability(
            ExternalLassoCapability externalLassoCapability) {
        externalCapabilities
                .remove(externalLassoCapability.getUniqueIdentifier());
    }

    /**
     * Prompt what to do with selected lasso content
     * @param uris Content URIs
     */
    private void promptLassoAction(final List<String> uris) {

        // Parse URIs
        List<Object> contents = new ArrayList<>(uris.size());
        for (String uri : uris) {
            File f = URIHelper.getFile(uri);
            MapItem mi = URIHelper.getMapItem(_mapView, uri);
            if (f != null)
                contents.add(f);
            else if (mi != null)
                contents.add(mi);
        }

        TileButtonDialog d = new TileButtonDialog(_mapView);
        d.setTitle(R.string.select_lasso_action);

        d.addButton(R.drawable.export_menu_default, R.string.export);
        d.addButton(R.drawable.ic_menu_delete, R.string.delete);
        d.addButton(R.drawable.send_square, R.string.send);

        // note this is the number of buttons already added, if you add more in the future
        // make sure that this number is incremented
        final int internalButtonCount = 3;

        final List<ExternalLassoCapability> curr;

        synchronized (LassoSelectionReceiver.class) {
            curr = new ArrayList<>(externalCapabilities.values());
        }

        for (ExternalLassoCapability externalLassoCapability : externalCapabilities
                .values()) {
            d.addButton(externalLassoCapability.getIcon(),
                    externalLassoCapability.getTitle());
        }

        d.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    // cancel button
                    case -1:
                        break;

                    // Export
                    case 0:
                        promptExport(contents);
                        break;

                    // Delete
                    case 1:
                        promptDelete(contents);
                        break;

                    // Send
                    case 2:
                        promptSend(contents);
                        break;

                    // registered capabilities
                    default:
                        final int externalCapabilityIndex = which
                                - internalButtonCount;
                        if (externalCapabilityIndex < curr.size()) {
                            final ExternalLassoCapability elc = curr
                                    .get(externalCapabilityIndex);
                            if (elc != null)
                                elc.process(contents);
                        }
                        break;
                }
            }
        });
        d.show(true);
    }

    /**
     * Prompt to export contents to a specific file type
     * @param contents Contents (either {@link MapItem} or {@link File})
     */
    private void promptExport(final List<Object> contents) {
        ExportDialog d = new ExportDialog(_mapView);
        d.setCallback(new ExportDialog.Callback() {
            @Override
            public void onExporterSelected(ExportMarshal marshal) {

                // Convert map items and files to exportables
                List<Exportable> exports = new ArrayList<>(contents.size());
                for (Object o : contents) {
                    if (o instanceof MapItem && o instanceof Exportable
                            && !marshal.filterItem((MapItem) o))
                        exports.add((Exportable) o);
                    else if (o instanceof File)
                        exports.add(new FileExportable((File) o));
                }

                // Nothing to export
                if (exports.isEmpty()) {
                    Toast.makeText(_context, _context.getString(
                            R.string.mission_package_no_items_support_export,
                            marshal.getContentType()), Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                // Export contents
                try {
                    marshal.execute(exports);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute export marshal", e);
                }
            }
        });
        d.show();
    }

    /**
     * Prompt to delete contents
     * @param contents Contents (either {@link MapItem} or {@link File})
     */
    private void promptDelete(final List<Object> contents) {

        String contentLabel = _context.getString(R.string.items,
                contents.size());
        if (contents.size() == 1) {
            Object item = contents.get(0);
            if (item instanceof File)
                contentLabel = ((File) item).getName();
            else if (item instanceof MapItem)
                contentLabel = ATAKUtilities.getDisplayName((MapItem) item);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(R.string.are_you_sure);
        b.setMessage(_context.getString(R.string.confirmation_remove_details,
                contentLabel));
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                new DeleteContentsTask(_context, contents).execute();
            }
        });
        b.setNegativeButton(R.string.cancel, null);
        b.show();
    }

    /**
     * Prompt to send contents
     * @param contents  Contents (either {@link MapItem} or {@link File})
     */
    private void promptSend(final List<Object> contents) {
        final ArrayList<String> uids = new ArrayList<>();
        for (Object o : contents) {
            if (o instanceof MapItem)
                uids.add(((MapItem) o).getUID());
        }
        // Nothing to export
        if (uids.isEmpty()) {
            Toast.makeText(_context, _context.getString(
                    R.string.mission_package_no_items_support_export),
                    Toast.LENGTH_LONG)
                    .show();
        } else { // wrap everything up and send it
            MissionPackageManifest manifest = MissionPackageApi
                    .CreateTempManifest("transfer", true, true, null);
            manifest.addMapItems(uids.toArray(new String[0]));
            for (String uid : uids) {
                manifest.addMapItem(uid);
                List<File> attachments = AttachmentManager
                        .getAttachments(uid);
                for (File attachment : attachments)
                    manifest.addFile(attachment, uid);
            }
            // Go straight to the contact screen
            MissionPackageApi.prepareSend(manifest, null, false);
        }
    }

    private static class DeleteContentsTask
            extends AsyncTask<Void, Integer, Void> {

        private final ProgressDialog _pd;
        private final List<Object> _contents;

        DeleteContentsTask(Context context, List<Object> contents) {
            _contents = contents;
            _pd = new ProgressDialog(context);
            _pd.setMessage(context.getString(R.string.delete_items_busy));
            _pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _pd.setMax(contents.size());
            _pd.setCanceledOnTouchOutside(false);
            _pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(false);
                }
            });
        }

        @Override
        protected Void doInBackground(Void... v) {
            int p = 0;
            for (final Object o : _contents) {
                if (isCancelled())
                    break;
                if (o instanceof File)
                    FileSystemUtils.delete((File) o);
                else if (o instanceof MapItem) {
                    // logic from MapItemHierarchyListItem
                    final MapItem item = (MapItem)o;
                    if (item.getMetaBoolean("removable", true)) {
                        // Remove from map group
                        item.removeFromGroup();
                    } else if (item.hasMetaValue("deleteAction")) {
                        // Special delete action
                        Intent delete = new Intent(item
                                .getMetaString("deleteAction", ""));
                        delete.putExtra("targetUID", item.getUID());
                        AtakBroadcast.getInstance().sendBroadcast(delete);
                    }
                }
                publishProgress(++p);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            _pd.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void v) {
            _pd.dismiss();
        }
    }

    private static class FileExportable implements Exportable {

        private final File _file;

        FileExportable(File file) {
            _file = file;
        }

        @Override
        public boolean isSupported(Class<?> target) {
            return MissionPackageExportWrapper.class.equals(target);
        }

        @Override
        public Object toObjectOf(Class<?> target, ExportFilters filters)
                throws FormatNotSupportedException {
            if (MissionPackageExportWrapper.class.equals(target)) {
                MissionPackageExportWrapper export = new MissionPackageExportWrapper();
                export.getFilepaths().add(_file.getAbsolutePath());
                return export;
            }
            return null;
        }
    }
}
