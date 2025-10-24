
package com.atakmap.android.missionpackage.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.atakmap.android.filesharing.android.service.AndroidFileInfo;
import com.atakmap.android.filesharing.android.service.FileInfoPersistanceHelper;
import com.atakmap.android.gui.TileButtonDialog;
import com.atakmap.android.hierarchy.HierarchyListAdapter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.HierarchyListUserDelete;
import com.atakmap.android.hierarchy.action.Action;
import com.atakmap.android.hierarchy.action.Delete;
import com.atakmap.android.hierarchy.action.Delete2;
import com.atakmap.android.hierarchy.action.Export;
import com.atakmap.android.hierarchy.action.GroupDelete;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.layers.RegionShapeTool;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.missionpackage.MissionPackageMapComponent;
import com.atakmap.android.missionpackage.MissionPackageReceiver;
import com.atakmap.android.missionpackage.MissionPackageUtils;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.file.task.MissionPackageBaseTask;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.app.R;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.export.GPXExportWrapper;
import com.atakmap.spatial.file.export.KMZFolder;
import com.atakmap.spatial.file.export.OGRFeatureExportWrapper;
import com.ekito.simpleKML.model.Folder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MissionPackageHierarchyListItem extends
        AbstractHierarchyListItem2 implements Visibility2, Search,
        GroupDelete, Export, View.OnClickListener {

    private static final String TAG = "MissionPackageHierarchyListItem";

    private final MapView _mapView;
    private final Context _context;
    private final MissionPackageMapComponent _component;
    private final MissionPackageReceiver _receiver;
    private final MissionPackageMapOverlay _overlay;
    private final MissionPackageListGroup _group;
    private final MissionPackageManifest _manifest;

    private boolean _vizSupported = false;

    MissionPackageHierarchyListItem(MapView mapView,
            MissionPackageMapOverlay overlay, MissionPackageListGroup group) {
        _mapView = mapView;
        _context = mapView.getContext();
        _overlay = overlay;
        _component = overlay.getComponent();
        _receiver = _component.getReceiver();
        _group = group;
        _manifest = group.getManifest();
        this.asyncRefresh = true;
        this.reusable = true;
    }

    @Override
    public String getTitle() {
        if (!_group.isValid()) {
            Log.w(TAG, "Skipping invalid title");
            return _context.getString(R.string.mission_package_name);
        }

        return _manifest.getName();
    }

    @Override
    public String getDescription() {
        String desc = "";
        String user = MissionPackageUtils.abbreviateFilename(
                _group.getUserName(), 15);
        if (!FileSystemUtils.isEmpty(user))
            desc = user + ", ";
        int childCount = getChildCount();
        if (childCount == 1)
            desc += _context.getString(R.string.single_item);
        else
            desc += _context.getString(R.string.items, childCount);
        return desc;
    }

    @Override
    public int getDescendantCount() {
        return getChildCount();
    }

    @Override
    public boolean isChildSupported() {
        return true;
    }

    @Override
    public Drawable getIconDrawable() {
        return _context.getDrawable(_group.isModified()
                ? R.drawable.ic_missionpackage_modified
                : R.drawable.ic_menu_missionpackage);
    }

    @Override
    public String getAssociationKey() {
        return "missionpackagePreference";
    }

    public MissionPackageListGroup getGroup() {
        if (!_group.isValid()) {
            Log.w(TAG, "Skipping invalid user object");
            return null;
        }

        return _group;
    }

    @Override
    public MissionPackageListGroup getUserObject() {
        return getGroup();
    }

    @Override
    public View getExtraView(View v, ViewGroup parent) {
        ExtraHolder h = v != null && v.getTag() instanceof ExtraHolder
                ? (ExtraHolder) v.getTag()
                : null;
        if (h == null) {
            h = new ExtraHolder();
            v = LayoutInflater.from(_context).inflate(
                    R.layout.missionpackage_overlay_manifestitem,
                    parent, false);
            h.size = v.findViewById(
                    R.id.missionpackage_overlay_manifestitem_size);
            h.save = v.findViewById(R.id.save);
            h.send = v.findViewById(R.id.send);
            h.delete = v.findViewById(R.id.delete);
            v.setTag(h);
        }
        boolean multiSelect = this.listener instanceof HierarchyListAdapter
                && ((HierarchyListAdapter) this.listener)
                        .getSelectHandler() != null;

        //h.size.setText(R.string.dashdash);
        getManifestSize(_manifest, h.size);

        h.save.setOnClickListener(this);
        setViz(h.save, !multiSelect && _group.isModified());

        h.send.setOnClickListener(this);
        setViz(h.send, !multiSelect && !FileSystemUtils.isEmpty(
                _group.getItems()) && !_group.isModified());

        h.delete.setOnClickListener(this);
        setViz(h.delete, !multiSelect);

        return v;
    }

    private static class ExtraHolder {
        TextView size;
        ImageButton save, send, delete;
    }

    private final static Map<AndroidFileInfo, List<HierarchyListItem>> cachedMissionPackageItems = new ConcurrentHashMap<>();

    @Override
    public void refreshImpl() {

        //long start = System.currentTimeMillis();
        final FileInfoPersistanceHelper fiph = FileInfoPersistanceHelper
                .instance();

        List<HierarchyListItem> items = null;
        AndroidFileInfo afi = null;

        // only cache when the group is not in a modified/unsaved state
        if (fiph != null && !_group.isModified()) {

            afi = fiph.getFileInfoFromFilename(
                    new File(_group.getManifest().getPath()),
                    FileInfoPersistanceHelper.TABLETYPE.SAVED);
            if (afi != null)
                items = cachedMissionPackageItems.get(afi);

            for (AndroidFileInfo cachedfi : cachedMissionPackageItems
                    .keySet()) {
                if ((!cachedfi.equals(afi) && cachedfi.file().getPath()
                        .equals(_group.getManifest().getPath())) ||
                        !cachedfi.file().exists()) {
                    cachedMissionPackageItems.remove(cachedfi);
                }
            }
        }

        // Filter
        boolean vizSupported = false;
        List<HierarchyListItem> filtered = new ArrayList<>();
        if (_group != null && _group.isValid()) {

            if (items != null) {
                Log.d(TAG, "cache hit");
                for (HierarchyListItem listItem : items) {
                    if (listItem != null && this.filter.accept(listItem)) {
                        filtered.add(listItem);
                        if (!vizSupported && listItem.getAction(
                                Visibility.class) != null)
                            vizSupported = true;
                    }

                }
            } else {
                Log.d(TAG, "cache miss");
                items = new ArrayList<>();

                for (MissionPackageListItem child : _group.getItems()) {
                    HierarchyListItem listItem = null;
                    if (child instanceof MissionPackageListFileItem) {
                        listItem = new MissionPackageFileHierarchyListItem(
                                _overlay, _mapView, this.listener, _group,
                                (MissionPackageListFileItem) child);
                    } else if (child instanceof MissionPackageListMapItem) {
                        listItem = new MissionPackageMapItemHierarchyListItem(
                                _overlay, _mapView, this.listener, _group,
                                (MissionPackageListMapItem) child);
                    }

                    items.add(listItem);

                    if (listItem != null && this.filter.accept(listItem)) {
                        filtered.add(listItem);
                        if (!vizSupported && listItem.getAction(
                                Visibility.class) != null)
                            vizSupported = true;
                    }
                }
                if (afi != null)
                    cachedMissionPackageItems.put(afi, items);
            }
        }
        _vizSupported = vizSupported;

        // Sort
        sortItems(filtered);

        // Update
        updateChildren(filtered);
        //Log.d(TAG, "elapsed time: " + (System.currentTimeMillis()-start));
    }

    @Override
    public boolean hideIfEmpty() {
        // Only hide empty Mission Packages if a non-default filter is active
        return this.filter != null && !this.filter.isDefaultFilter();
    }

    @Override
    public String getUID() {
        if (!_group.isValid()) {
            Log.w(TAG, "Skipping invalid UID");
            return null;
        }

        return _manifest.getUID();
    }

    /**************************************************************************/

    @Override
    public void onClick(View v) {
        int i = v.getId();

        // Edit data package
        if (i == R.id.edit)
            new MissionPackageEditDialog(_mapView, _manifest).show();

        // Add items to package
        else if (i == R.id.create)
            _overlay.promptAddItems(_group);

        // Save data package
        else if (i == R.id.save) {
            Log.d(TAG, "Saving modified package " + _manifest.getName());
            _group.saveAndSend(_component, false, _overlay, null);
        }

        // Send package
        else if (i == R.id.send) {
            if (_component.checkFileSharingEnabled())
                sendConfirm();
        }

        // Delete package
        else if (i == R.id.delete) {
            if (v.getParent() == _overlay.getHeaderView())
                promptDeleteContents();
            else
                promptDeletePackage();
        }
    }

    @Override
    public View getHeaderView() {
        View header = _overlay.getHeaderView();
        View edit = header.findViewById(R.id.edit);
        edit.setOnClickListener(this);
        setViz(edit, true);

        View save = header.findViewById(R.id.save);
        save.setOnClickListener(this);
        setViz(save, _group.isModified());

        View create = header.findViewById(R.id.create);
        create.setOnClickListener(this);
        setViz(create, true);

        View delete = header.findViewById(R.id.delete);
        delete.setOnClickListener(this);
        setViz(delete, true);

        setViz(header.findViewById(R.id.download), false);
        setViz(header.findViewById(R.id.changes), false);

        return header;
    }

    private void setViz(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public <T extends Action> T getAction(Class<T> clazz) {
        if ((clazz.equals(Visibility.class)
                || clazz.equals(Visibility2.class)) && !_vizSupported)
            return null;
        return super.getAction(clazz);
    }

    @Override
    public boolean isSupported(Class<?> target) {
        return Folder.class.equals(target)
                || KMZFolder.class.equals(target)
                || GPXExportWrapper.class.equals(target)
                || OGRFeatureExportWrapper.class.equals(target);
    }

    @Override
    public Object toObjectOf(Class<?> target, ExportFilters filters)
            throws FormatNotSupportedException {
        List<HierarchyListItem> items = getChildren();
        if (Folder.class.equals(target))
            return _overlay.getFolder(items);
        else if (KMZFolder.class.equals(target)) {
            return toKmz(filters, items);
        } else if (GPXExportWrapper.class.equals(target))
            return _overlay.getGPX(items);
        else if (OGRFeatureExportWrapper.class.equals(target))
            return _overlay.getOGR(getTitle(), getChildren());
        return null;
    }

    @Override
    public List<Delete> getDeleteActions() {
        final List<Delete> ret = super.getDeleteActions();

        return Collections.singletonList(new Delete2() {
            @Override
            public boolean delete() {
                return delete(UUID.randomUUID().toString());
            }

            @Override
            public boolean delete(String transactionId) {
                TransactionRequest tr;
                synchronized (transactionRequestMap) {
                    tr = transactionRequestMap.get(transactionId);
                    if (tr == null) {
                        tr = new TransactionRequest(_mapView, transactionId);
                        transactionRequestMap.put(transactionId, tr);
                    }
                }
                boolean deleteAllContents = tr.deleteAllContentsPrompt();
                if (deleteAllContents) {
                    _mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            new RemoveContentsTask(_group, _overlay,
                                    _receiver, new MissionPackageBaseTask.Callback() {
                                @Override
                                public void onMissionPackageTaskComplete(MissionPackageBaseTask task, boolean success) {
                                    refreshImpl();
                                }
                            }).execute();
                        }
                    });
                    return true;
                } else {
                    //Log.d(TAG, "transaction: " + transactionId + " response: " + deleteAllContents);
                    MissionPackageHierarchyListItem.this.delete(true);

                    //if (_group != null && _group.getItems().isEmpty()) {
                    //    _overlay.deletePackage(_group, false, true);
                    //}
                    return true;
                }
            }
        });
    }

    @Override
    public boolean setVisible(boolean visible) {
        if (!_group.isValid()) {
            Log.w(TAG, "Skipping invalid setVisible");
            return false;
        }
        boolean ret = false;
        List<Visibility> vizActions = getChildActions(Visibility.class);
        for (Visibility v : vizActions)
            ret |= v.setVisible(visible);
        return ret;
    }

    /**
     * ******************************************************************
     */
    // Search
    @Override
    public Set<HierarchyListItem> find(String terms) {
        Set<String> found = new HashSet<>();
        Set<HierarchyListItem> retval = new HashSet<>();
        List<HierarchyListItem> children = getChildren();
        for (HierarchyListItem item : children) {
            if (item instanceof Search
                    && ((Search) item).find(terms) != null
                    && !found.contains(item.getUID())) {
                retval.add(item);
                found.add(item.getUID());
            }
        }
        return retval;
    }

    private void promptDeletePackage() {
        TileButtonDialog d = new TileButtonDialog(_mapView);
        d.setTitle(R.string.delete_mission_package);
        d.setMessage(
                R.string.mission_package_delete_package_and_remove_package_contents);
        d.addButton(R.drawable.ic_missionpackage_delete,
                R.string.leave_contents);
        d.addButton(R.drawable.ic_overlays_delete, R.string.remove_contents);
        d.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == -1)
                    return;
                switch (which) {

                    // Remove package and leave contents alone
                    case 0: {
                        delete(true);
                        break;
                    }

                    // Remove contents along with package
                    case 1: {
                        AlertDialog.Builder b = new AlertDialog.Builder(
                                _context);
                        b.setTitle(R.string.confirm_delete);
                        b.setMessage(
                                R.string.mission_package_contents_will_be_removed_from_device);
                        b.setNegativeButton(R.string.cancel, null);
                        b.setPositiveButton(R.string.remove_contents,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d,
                                            int w) {
                                        new RemoveContentsTask(_group, _overlay,
                                                _receiver, new MissionPackageBaseTask.Callback() {
                                            @Override
                                            public void onMissionPackageTaskComplete(MissionPackageBaseTask task, boolean success) {
                                                refreshImpl();
                                            }
                                        }).execute();
                                    }
                                });
                        b.show();
                        break;
                    }
                }
            }
        });
        d.show(true);
    }

    private void promptDeleteContents() {
        TileButtonDialog d = new TileButtonDialog(_mapView);
        d.setTitle(R.string.delete_contents);
        d.setMessage(R.string.select_deletion_method);
        d.addButton(R.drawable.ic_overlays_delete, R.string.multiselect);
        d.addButton(R.drawable.ic_lasso, R.string.lasso);
        d.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == -1)
                    return;
                switch (which) {

                    // Multi-select through OM
                    case 0: {
                        HierarchyListUserDelete hlud = new HierarchyListUserDelete();
                        hlud.setCloseHierarchyWhenComplete(false);
                        HierarchyListReceiver.getInstance()
                                .setSelectHandler(hlud);
                        break;
                    }

                    // Remove content using lasso
                    case 1: {
                        Bundle b = new Bundle();
                        b.putSerializable("mode", RegionShapeTool.Mode.LASSO);
                        Intent cb = new Intent(
                                MissionPackageReceiver.MISSIONPACKAGE_REMOVE_LASSO);
                        cb.putExtra(
                                MissionPackageApi.INTENT_EXTRA_MISSIONPACKAGEMANIFEST_UID,
                                _group.getManifest().getUID());
                        b.putParcelable("callback", cb);
                        ToolManagerBroadcastReceiver.getInstance().startTool(
                                RegionShapeTool.TOOL_ID, b);
                        break;
                    }
                }
            }
        });
        d.show(true);
    }

    private void delete(boolean singleDelete) {
        if (!singleDelete) {
            delete();
            return;
        }
        _overlay.remove(_group, false);
        _overlay.deletePackage(_group, true, true);
        refreshImpl();
    }

    private void sendConfirm() {
        // check if empty
        if (_manifest.isEmpty()) {
            Log.e(TAG, "Unable to send empty contents");
            _overlay.toast(R.string.mission_package_cannot_send_empty_package);
            return;
        }

        // check if over "NoGo"
        final long totalSizeInBytes = _manifest.getTotalSize(),
                highSize = _receiver.getHighThresholdInBytes(),
                lowSize = _receiver.getLowThresholdInBytes(),
                nogoSize = _receiver.getNogoThresholdInBytes();

        if (totalSizeInBytes > nogoSize) {
            // extract string out in the next release.
            final String message = String.format(
                    "The size of %1$s (%2$s) exceeds the maximum size (%3$s) set in the device preferences.  Would you like to send it anyway?",
                    _manifest.getName(),
                    MathUtils.GetLengthString(totalSizeInBytes),
                    MathUtils.GetLengthString(nogoSize));
            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    _context);
            builder.setTitle(R.string.warning);
            builder.setMessage(message);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.send,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG,
                                    "override defaults and forcing the send of: "
                                            + _manifest);
                            MissionPackageApi.Send(_context, _manifest, null,
                                    null, true);
                        }
                    });
            builder.show();
            return;
        }

        // no need to confirm send if size is "Green"
        if (totalSizeInBytes < lowSize) {
            MissionPackageApi.Send(_context, _manifest, null, null, true);
            return;
        }

        View v = LayoutInflater.from(_context).inflate(
                R.layout.missionpackage_send, _mapView, false);

        String title, warning;
        if (totalSizeInBytes < highSize) {
            title = _context.getString(R.string.large_mission_package);
            warning = _context.getString(
                    R.string.mission_package_large_file_size_transfer_warning);
        } else {
            title = _context.getString(R.string.very_large_mission_package);
            warning = _context.getString(
                    R.string.mission_package_very_large_file_size_transfer_warning);
        }

        Log.d(TAG, "Showing send dialog for package: " + _manifest);

        TextView nameText = v.findViewById(R.id.missionpackage_send_txtName);
        nameText.setText(MissionPackageUtils.abbreviateFilename(
                _manifest.getName(), 40));

        TextView sizeText = v.findViewById(
                R.id.missionpackage_send_txtTotalSize);
        sizeText.setText(_context.getString(R.string.mission_package_size,
                MathUtils.GetLengthString(totalSizeInBytes)));

        TextView textWarning = v
                .findViewById(R.id.missionpackage_send_txtSizeWarning);
        textWarning.setText(warning);
        if (totalSizeInBytes < lowSize)
            textWarning.setVisibility(TextView.GONE);
        else {
            textWarning.setVisibility(TextView.VISIBLE);
            textWarning.setTextColor(getSendWarningColor(
                    totalSizeInBytes, highSize, lowSize));
        }

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(_context.getString(R.string.mission_package_confirm, title));
        b.setView(v);
        b.setCancelable(false);
        b.setPositiveButton(R.string.send,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        MissionPackageApi.Send(_context, _manifest,
                                null, null, true);
                    }
                });
        b.setNegativeButton(R.string.cancel, null);
        b.show();
    }

    private static int getSendWarningColor(long size, long high, long low) {
        if (size < low)
            return Color.GREEN;
        else if (size < high)
            return Color.YELLOW;
        return Color.RED;
    }

    private final static Executor manifestExecutor = Executors
            .newFixedThreadPool(8,
                    new NamedThreadFactory("filesExecutor"));

    private void getManifestSize(MissionPackageManifest m, final TextView v) {
        manifestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final long totalSize = m.getTotalSize();
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        v.setText(MathUtils.GetLengthString(totalSize));

                        int warning = getSendWarningColor(totalSize,
                                _receiver.getHighThresholdInBytes(),
                                _receiver.getLowThresholdInBytes());

                        switch (warning) {
                            case Color.RED:
                                v.setTextColor(0xFFFF6666);
                                v.setTypeface(null, Typeface.BOLD);
                                break;
                            case Color.YELLOW:
                                v.setTextColor(0xFFFFFF66);
                                v.setTypeface(null, Typeface.NORMAL);
                                break;
                            default:
                                v.setTextColor(Color.LTGRAY);
                                v.setTypeface(null, Typeface.NORMAL);
                        }
                    }
                });
            }
        });
    }

    /**
     * Asynchronous task for removing a data package and its contents
     */
    private static class RemoveContentsTask extends MissionPackageBaseTask {

        private final Context _context;
        private final MissionPackageListGroup _group;
        private final MissionPackageMapOverlay _overlay;

        RemoveContentsTask(MissionPackageListGroup group,
                MissionPackageMapOverlay overlay,
                MissionPackageReceiver receiver, Callback callback) {
            super(group.getManifest(), receiver, true, callback);
            _context = getContext();
            _group = group;
            _overlay = overlay;
        }

        @Override
        public String getProgressDialogMessage() {
            return _context.getString(R.string.delete_items_busy);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.d(TAG, "Removing contents: " + _group);
            List<MissionPackageListItem> items = _group.getItems();
            int p = 0;
            int max = items.size() + 1;
            for (MissionPackageListItem item : items) {
                if (isCancelled())
                    return false;
                item.removeContent();
                publishProgress(new ProgressDialogUpdate(++p, max,
                        _context.getString(R.string.delete_items_busy)));
            }

            if (isCancelled())
                return false;

            publishProgress(new ProgressDialogUpdate(p, max,
                    _context.getString(R.string.deleting_mission_package)));
            _overlay.remove(_group, false);
            _overlay.deletePackage(_group, false, true);


            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

            if (_callback != null)
                _callback.onMissionPackageTaskComplete(this, aBoolean);

            dismissProgressDialog();
        }
    }

    private static class SaveAndSelectMapItemTask extends
            MissionPackageBaseTask {

        public final static String TAG = "SaveAndSelectMapItemTask";

        private final Context _context;

        private SaveAndSelectMapItemTask(Context ctx,
                MissionPackageManifest contents,
                MissionPackageReceiver receiver) {
            super(contents, receiver, true, null);
            _context = ctx;
        }

        @Override
        public String getProgressDialogMessage() {
            return _context.getString(R.string.compressing_please_wait);
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            Thread.currentThread().setName("SaveAndSelectMapItemTask");

            // work to be performed by background thread
            Log.d(TAG, "Executing: " + this);

            // launch select tool
            MissionPackageMapOverlay.startMapSelectTool(_context, _manifest);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // work to be performed by UI thread after work is complete
            Log.d(TAG, "onPostExecute");

            // close the progress dialog
            dismissProgressDialog();
        }
    }

    private KMZFolder toKmz(ExportFilters filters,
            List<HierarchyListItem> items)
            throws FormatNotSupportedException {
        final KMZFolder f = new KMZFolder();
        f.setName(_overlay.getFolder(items).getName());

        for (HierarchyListItem item : items)
            toKmz(f, item, filters);

        if (f.isEmpty()) {
            return null;
        }

        return f;
    }

    private void toKmz(KMZFolder f, HierarchyListItem item,
            ExportFilters filters) throws FormatNotSupportedException {
        if (item instanceof MissionPackageHierarchyListItem) {
            //recurse to get child overlay items
            List<HierarchyListItem> children = ((MissionPackageHierarchyListItem) item)
                    .getChildren();
            for (HierarchyListItem c : children)
                toKmz(f, c, filters);
        } else if (item.getUserObject() != null
                && item.getUserObject() instanceof MapItem) {
            if (item instanceof MissionPackageMapItemHierarchyListItem) {
                //recurse to get child overlay items
                List<HierarchyListItem> children = ((MissionPackageMapItemHierarchyListItem) item)
                        .getChildren();
                for (HierarchyListItem c : children)
                    toKmz(f, c, filters);
            }
            MapItem mapItem = (MapItem) item.getUserObject();
            //Attempt KMZ, fall back on KML
            if (mapItem instanceof Exportable
                    && ((Exportable) mapItem)
                            .isSupported(KMZFolder.class)) {
                KMZFolder itemFolder = (KMZFolder) ((Exportable) item)
                        .toObjectOf(KMZFolder.class, filters);
                if (itemFolder != null && !itemFolder.isEmpty()) {
                    f.getFeatureList().add(itemFolder);
                    if (itemFolder.hasFiles()) {
                        f.getFiles().addAll(itemFolder.getFiles());
                    }
                }
            } else if (mapItem instanceof Exportable
                    && ((Exportable) mapItem).isSupported(Folder.class)) {
                Folder itemFolder = (Folder) ((Exportable) item)
                        .toObjectOf(Folder.class, filters);
                if (itemFolder != null
                        && itemFolder.getFeatureList() != null
                        && !itemFolder.getFeatureList().isEmpty()) {
                    f.getFeatureList().add(itemFolder);
                }
            }
        }
    }

    private final static Map<String, TransactionRequest> transactionRequestMap = new HashMap<>();

    private static class TransactionRequest {
        final String transactionId;
        final MapView mapView;

        private Boolean deleteAllContents;
        private AlertDialog prompt;

        public TransactionRequest(final MapView mapView,
                final String transactionId) {
            this.mapView = mapView;
            this.transactionId = transactionId;
        }

        synchronized boolean deleteAllContentsPrompt() {
            if (deleteAllContents == null) {
                final AlertDialog.Builder adb = new AlertDialog.Builder(
                        mapView.getContext());
                adb.setTitle(R.string.delete_contents);
                adb.setCancelable(false);
                adb.setMessage(R.string.datapackage_remove_contents_warning);
                adb.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                deleteAllContents = true;
                            }
                        });
                adb.setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                deleteAllContents = false;
                            }
                        });

                mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog ad = adb.create();
                        ad.show();
                        prompt = ad;
                    }
                });
                while (prompt == null || prompt.isShowing()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignored) {
                    }
                }
                prompt = null;
            }

            return deleteAllContents;
        }
    }
}
