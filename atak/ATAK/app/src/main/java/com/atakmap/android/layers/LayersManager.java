
package com.atakmap.android.layers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.atakmap.android.contentservices.Service;
import com.atakmap.android.contentservices.ServiceFactory;
import com.atakmap.android.contentservices.ServiceListing;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.layers.wms.DownloadAndCacheBroadcastReceiver;
import com.atakmap.android.maps.CardLayer;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PanZoomReceiver;
import com.atakmap.android.maps.tilesets.mobac.WebMapLayer;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.CameraController;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainerSpi;
import com.atakmap.math.MathUtils;
import com.atakmap.util.Visitor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LayersManager
        implements View.OnClickListener, OnlineLayersDownloadManager.Callback {
    private static final String TAG = "LayersManager";

    enum DownloadWorkflowState {
        None,
        SelectRegion,
        Download,
    }

    public interface Listener {
        void onDownloadWorkflowStateChanged(LayersManager layers,
                DownloadWorkflowState state);
    }

    MapView mapView;

    OnlineLayersManagerView onlineView;
    OnlineLayersDownloadManager downloader;
    DownloadAndCacheBroadcastReceiver downloadRecv;
    Button selectButton;
    Switch offlineOnlyCheckbox;
    Button cancelButton;
    Button downloadButton;
    AlertDialog listDialog;

    MobileLayerSelectionAdapter adapter;

    String importMime;
    String importContent;
    File defaultDownloadDirectory = null;

    boolean downloadPromptForOverlay;

    Set<Listener> listeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private LayersManager(MapView mapView) {
        this.mapView = mapView;
    }

    public View getView() {
        return onlineView;
    }

    public OnlineLayersDownloadManager getDownloader() {
        return downloader;
    }

    public DownloadAndCacheBroadcastReceiver getDownloadReceiver() {
        return downloadRecv;
    }

    public Button getSelectButton() {
        return selectButton;
    }

    public Switch getOfflineOnlyCheckbox() {
        return offlineOnlyCheckbox;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getDownloadButton() {
        return downloadButton;
    }

    public LayerSelectionAdapter getAdapter() {
        return adapter;
    }

    void addListener(Listener l) {
        listeners.add(l);
    }

    void removeListener(Listener l) {
        listeners.remove(l);
    }

    void dispatchDownloadWorkflowState(DownloadWorkflowState state) {
        for (Listener l : listeners)
            l.onDownloadWorkflowStateChanged(this, state);
    }

    public void onClick(View v) {
        Context c = mapView.getContext();

        // Add WMS / WMTS map source
        if (v.getId() == R.id.addOnlineSource_btn) {

            final LayoutInflater inflater = LayoutInflater.from(c);
            final View view = inflater.inflate(R.layout.wmswfs_dialog, null,
                    false);
            final EditText input = view.findViewById(R.id.wmswfs_address);
            // Use the last string the user entered into the box, so that
            // they don't have to start from scratch in case they
            // fat-finger something
            final AtakPreferences prefs = AtakPreferences.getInstance(c);
            String lastEntry = prefs.get("pref_wms_add_last_entry", "");
            if (lastEntry != null) {
                input.setText(lastEntry);
                input.setSelection(lastEntry.length());
            }
            // pop up a dialog requesting the user to enter a URL to
            // a WMS or WMTS server
            AlertDialog.Builder b = new AlertDialog.Builder(c);
            b.setTitle(R.string.online_source_dialogue);
            b.setView(view);
            b.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d,
                                int whichButton) {
                            String s = input.getText().toString();
                            prefs.set("pref_wms_add_last_entry", s);
                            queryMapService(s);
                        }
                    });
            b.setNegativeButton(R.string.cancel, null);
            AlertDialog ad = b.create();
            ad.show();
            AlertDialogHelper.adjustWidth(ad, .90);
        }

        // Select region
        else if (v == selectButton) {
            downloader.promptSelectRegion();
            adapter.reset();
            cancelButton.setEnabled(true);
            cancelButton.setText(R.string.cancel);
            downloadButton.setEnabled(false);
            selectButton.setEnabled(false);
            dispatchDownloadWorkflowState(DownloadWorkflowState.SelectRegion);
            return;
        }

        // Download selected region
        else if (v == downloadButton) {
            if (adapter.isOfflineOnly()) {
                adapter.setOfflineOnly(false);
                offlineOnlyCheckbox.setChecked(true);
            }

            // TODO test connection to WMS server before proceeding

            // enforce tile limit
            int tileCount = adapter.getTileCount();
            if (tileCount <= 0) {
                Toast.makeText(c, R.string.download_tiles_min,
                        Toast.LENGTH_SHORT).show();
                return;
            } else if (tileCount > LayersManagerBroadcastReceiver.TILE_DOWNLOAD_LIMIT) {
                Toast.makeText(c, c.getString(R.string.download_limit,
                        LayersManagerBroadcastReceiver.TILE_DOWNLOAD_LIMIT),
                        Toast.LENGTH_SHORT).show();
                return;
            } else if (tileCount > LayersManagerBroadcastReceiver.TILE_DOWNLOAD_WARNING_LIMIT) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        downloader.context);
                builder.setTitle(R.string.warning);
                builder.setCancelable(false);
                builder.setMessage(R.string.warning_tile_limit);
                builder.setPositiveButton(R.string.continue_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                performWork(tileCount, c);
                            }
                        });
                builder.setNegativeButton(R.string.back, null);
                builder.show();
            } else {
                performWork(tileCount, c);
            }
        }

        // Cancel download
        else if (v == cancelButton) {
            cancelDownloadWorkflow();
            return;
        }
    }

    private void performWork(int tileCount, Context c) {

        selectButton.setEnabled(true);
        dispatchDownloadWorkflowState(DownloadWorkflowState.Download);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(c,
                android.R.layout.select_dialog_singlechoice);
        adapter.add(c.getString(R.string.create_new_tileset));
        adapter.add(c.getString(R.string.add_to_existing_tileset));

        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(R.string.choose_tileset_dest);
        b.setCancelable(true);
        b.setNegativeButton(R.string.cancel, null);
        b.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0)
                    createNewLayer();
                else if (which == 1)
                    promptExistingLayer();
            }
        });
        listDialog = b.create();
        listDialog.show();
    }

    void cancelDownloadWorkflow() {
        // cancel the current operation

        setExpandedDownloadState(false);

        // if selecting a rectangle, or a rect has been set but the download has not yet
        // been started
        cancelSelectDownloadRegion();

        // if downloading and no rect set
        if (downloader.isDownloading()) {
            cancelButton.setEnabled(true);
            downloader.stopDownload();
        }

        dispatchDownloadWorkflowState(DownloadWorkflowState.None);
    }

    void startDownload(final String layerTitle, String downloadPath) {
        setExpandedDownloadState(false);
        downloader.freezeRegionShape();
        final Map<LayerSelection, Pair<Double, Double>> toDownload = adapter
                .getLayersToDownload();
        for (Map.Entry<LayerSelection, Pair<Double, Double>> entry : toDownload
                .entrySet()) {
            LayerSelection ls = entry.getKey();
            Pair<Double, Double> minMax = entry.getValue();
            if (minMax != null) {
                double minRes = minMax.first;
                double maxRes = minMax.second;
                boolean isImagery = true;
                for(DatasetDescriptor desc : ls.getDescriptors()) {
                    final String content = desc.getExtraData("contentType");
                    if(content != null) {
                        switch(content) {
                            case "vector" :
                            case "terrain" :
                                isImagery = false;
                                break;
                            default :
                                break;
                        }
                    }
                }
                if (layerTitle.endsWith(".ovr") && isImagery) {
                    // imagery overlays need to be constrained to a subset of the requested set of resolutions.
                    minRes = Math.min(minMax.first, 300);
                    maxRes = Math.min(minMax.second, 300);
                }
                downloader.startDownload(layerTitle, downloadPath,
                        defaultDownloadDirectory,
                        ls,
                        minRes, maxRes,
                        importMime, importContent);
                cancelButton.setText(R.string.cancel);
                adapter.reset();
            }
        }
    }

    protected void createNewLayer() {
        // pop up dialog to get the name of the cache from the user
        final Context c = mapView.getContext();
        final View title = LayoutInflater.from(c).inflate(
                R.layout.download_layer, mapView, false);
        final View overlayPromptCheckbox = title
                .findViewById(
                        R.id.suggestNewLayerImportStrategy);
        overlayPromptCheckbox.setVisibility(
                downloadPromptForOverlay ? View.VISIBLE : View.GONE);
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(R.string.download_layer);
        b.setCancelable(false);
        b.setPositiveButton(R.string.ok, null);
        b.setNegativeButton(R.string.cancel, null);
        b.setView(title);
        final AlertDialog d = b.create();
        d.show();
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Action for 'OK' Button
                        String layerTitle = ((EditText) title
                                .findViewById(R.id.newLayerName))
                                        .getText().toString();
                        final boolean suggestedImportStrategy = ((CheckBox) title
                                .findViewById(
                                        R.id.suggestNewLayerImportStrategy))
                                                .isChecked();

                        if (layerTitle.length() == 0) {
                            Toast.makeText(c, R.string.no_layer_name_blank,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String sanitizedLayerTitle = FileSystemUtils
                                .sanitizeWithSpaces(layerTitle);

                        if (!sanitizedLayerTitle.equals(layerTitle)) {
                            Toast.makeText(c, R.string.layer_name_limit,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        layerTitle = sanitizedLayerTitle;

                        if (suggestedImportStrategy) {
                            if (!layerTitle.endsWith(".ovr")) {
                                layerTitle = layerTitle + ".ovr";
                            }
                        }

                        // verify unique name with checkUniqueName method
                        if (!isUniqueLayerName(layerTitle)) {
                            Toast.makeText(c,
                                    c.getString(R.string.layer_name_exist)
                                            + layerTitle,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        d.dismiss();
                        downloadButton.setEnabled(false);
                        onlineView.setListAdapter(adapter);
                        onlineView
                                .scrollToSelection(adapter
                                        .selectedIndex());
                        showDownloadProgress(true);
                        startDownload(layerTitle, null);
                    }
                });
    }

    protected void showDownloadProgress(boolean b) {
        final View mobileTools = onlineView.findViewById(R.id.mobileTools);
        final View progressView = onlineView
                .findViewById(R.id.downloadProgressLayout);
        if (b) {
            mobileTools.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
        } else {
            mobileTools.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.GONE);
        }
    }

    void promptExistingLayer() {
        final Context c = mapView.getContext();
        RasterDataStore dataStore = ((MobileImageryRasterLayer2) adapter
                .getLayer()).getDataStore();
        LinkedList<DatasetDescriptor> existingTilesets = new LinkedList<>();
        RasterDataStore.DatasetDescriptorCursor result = null;
        try {
            RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
            // filter on current selection
            params.imageryTypes = Collections
                    .singleton(adapter.layer
                            .getSelection());
            params.order = Collections.<RasterDataStore.DatasetQueryParameters.Order> singleton(
                    RasterDataStore.DatasetQueryParameters.Name.INSTANCE);
            params.remoteLocalFlag = RasterDataStore.DatasetQueryParameters.RemoteLocalFlag.LOCAL;

            result = dataStore.queryDatasets(params);
            DatasetDescriptor desc;
            while (result.moveToNext()) {
                desc = result.get();
                if (desc.getName().contains(".sqlite"))
                    existingTilesets.add(result.get());
            }
        } finally {
            if (result != null)
                result.close();
        }

        if (existingTilesets.isEmpty()) {
            Toast.makeText(c, R.string.no_comp_tileset,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayAdapter<String> existingTilesetsAdapter = new ArrayAdapter<>(
                c,
                android.R.layout.select_dialog_singlechoice);
        ArrayList<String> existingTilesetsPaths = new ArrayList<>(
                existingTilesets.size());
        for (DatasetDescriptor ls : existingTilesets) {
            existingTilesetsAdapter.add(ls.getName().substring(0,
                    ls.getName().indexOf(".sqlite")));
            existingTilesetsPaths.add(ls.getUri());
        }

        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(R.string.choose_tileset_dest);
        b.setCancelable(true);
        b.setNegativeButton(R.string.cancel, null);
        b.setAdapter(existingTilesetsAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String layerTitle = existingTilesetsAdapter
                                .getItem(which);
                        String downloadPath = existingTilesetsPaths.get(which);
                        if (FileSystemUtils.isEmpty(layerTitle)) {
                            layerTitle = c.getString(R.string.new_layer);
                            downloadPath = (defaultDownloadDirectory == null)
                                    ? null
                                    : new File(defaultDownloadDirectory,
                                            layerTitle + ".sqlite")
                                                    .getAbsolutePath();
                        }
                        showDownloadProgress(true);
                        downloadButton.setEnabled(false);
                        onlineView.setListAdapter(adapter);
                        startDownload(layerTitle, downloadPath);
                    }
                });
        b.show();
    }

    final LayerSelectionAdapter.OnItemSelectedListener mobileDownloadSelectionListener = new LayerSelectionAdapter.OnItemSelectedListener() {
        @Override
        public void onItemSelected(final LayerSelectionAdapter adapter) {
            final LayerSelection ls = adapter.getSelected();
            final boolean downloadable = isSelectionDownloadable(ls);
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    if (!downloader.hasRegionShape())
                        return;
                    downloadButton.setEnabled(downloadable);
                    ((MobileLayerSelectionAdapter) adapter)
                            .setExpandedLayout(downloadable);
                    onlineView.invalidate();

                    // XXX - courtesy for GoogleCRS84Quad datasets
                    if (!downloadable &&
                            (ls != null) &&
                            (ls.getTag() instanceof MobileLayerSelectionAdapter.MobileImagerySpec)
                            &&
                            !((MobileLayerSelectionAdapter.MobileImagerySpec) ls
                                    .getTag()).offlineOnly) {

                        Toast.makeText(
                                onlineView.getContext(),
                                "Download from " + ls.getName()
                                        + " not supported.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };

    public void cancelSelectDownloadRegion() {
        downloader.cancelRegionSelect();
        downloadButton.setEnabled(false);

        if (!downloader.isDownloading())
            cancelButton.setEnabled(false);
        else
            cancelButton.setText(R.string.cancel);

        adapter.reset();
        selectButton.setEnabled(true);
    }

    void cancelDownloadArea() {
        setExpandedDownloadState(true);
        // invalidate the list view and reload with the expanded views
        // calculate center point
        onlineView.setListAdapter(adapter);
        onlineView.invalidate();
    }

    @Override
    public void receiveDownloadArea() {
        // invalidate the list view and reload with the expanded views
        setExpandedDownloadState(true);

        onlineView.setListAdapter(adapter);
        onlineView.invalidate();
        onlineView.scrollToSelection(adapter
                .selectedIndex());

        mobileDownloadSelectionListener.onItemSelected(adapter);
    }

    @Override
    public void cancelDownloadSelect() {
        onKey(null, KeyEvent.KEYCODE_BACK, null);
    }

    /**
     * listen for the back button
     **/
    //@Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelSelectDownloadRegion();

            dispatchDownloadWorkflowState(DownloadWorkflowState.None);
            return true;
        }
        return false;
    }

    /**
     * @param state {@code 0} for success, {@code 1} for canceled and {@code -1} for error
     */
    public void onDownloadWorkflowComplete(int state) {
        final boolean isEnabled = false; // cancel button is disabled on all completion states
        if (cancelButton != null) {
            cancelButton.setEnabled(isEnabled);
            if (!isEnabled)
                cancelButton.setText(R.string.cancel);
        }
    }

    boolean isUniqueLayerName(String name) {
        return isUniqueLayerName(adapter.layer.getDataStore(), name);
    }

    public void setExpandedDownloadState(final boolean enabled) {
        if (enabled)
            adapter.addOnItemSelectedListener(mobileDownloadSelectionListener);
        else
            adapter.removeOnItemSelectedListener(
                    mobileDownloadSelectionListener);
        adapter.setExpandedLayout(enabled);
    }

    // Query a WMS or WMTS server to find what layers it provides.
    // This is done off of the main thread due to network access.
    void queryMapService(String servicePath) {
        servicePath = servicePath.trim();
        if (servicePath.isEmpty())
            return;

        MapServiceTask mapServiceTask = new MapServiceTask(mapView,
                new MapServiceTaskCallback() {
                    @Override
                    public void selectQueryLayersToAdd(String uri,
                            ServiceListing querier) {
                        LayersManager.this.selectQueryLayersToAdd(uri, querier);
                    }
                });
        mapServiceTask.execute(servicePath);
    }

    // Display a dialog to allow the user to select what layers they
    // wish to add.
    void selectQueryLayersToAdd(String uri, ServiceListing querier) {

        List<Service> sortedLayers = new ArrayList<>(querier.services);

        Collections.sort(sortedLayers, new Comparator<Service>() {
            @Override
            public int compare(Service lhs, Service rhs) {
                boolean lhsIsFolder = (lhs.getName().indexOf('/') >= 0);
                boolean rhsIsFolder = (rhs.getName().indexOf('/') >= 0);
                if (lhsIsFolder && !rhsIsFolder)
                    return 1;
                else if (!lhsIsFolder && rhsIsFolder)
                    return -1;
                else
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        final Context context = mapView.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.wmswfs_results, null,
                false);
        final EditText searchTerms = view.findViewById(R.id.search_filter);
        final ListView listView = view.findViewById(R.id.results);
        final ServiceAdapter adapter = new ServiceAdapter(context,
                sortedLayers);
        listView.setAdapter(adapter);

        searchTerms.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                adapter.getFilter().filter(s.toString());
            }
        });

        String title = mapView.getContext().getString(R.string.layers_on)
                + querier.title;
        new AlertDialog.Builder(mapView.getContext())
                .setTitle(title)
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                List<Service> layerList = adapter.getSelected();

                                // calling right into a static definition to see if these layers
                                // are able to be aggregated.
                                // right now this is a little muddy because it grossly makes
                                // this class away of the WebMapLayerService.   consult with 
                                // CL on if Service contains the static ability to check for 
                                // aggregation.

                                // runs this twice, first without a name to see if aggregation is 
                                // even possible.  This is to limit the polution until CL and I 
                                // formalize the interface changes.
                                Service aggregate = com.atakmap.android.maps.tilesets.mobac.WebMapLayerService
                                        .constructAggregate(null, layerList);
                                if (aggregate != null && layerList.size() > 1) {
                                    promptForAgregation(layerList);
                                } else {
                                    // for each layer the user checked, add that to
                                    // our list of sources
                                    for (Service layer : layerList) {
                                        doAddMapLayer(layer);
                                    }
                                }
                            }

                        })
                .setNegativeButton(R.string.cancel, null).show();
    }

    void promptForAgregation(final List<Service> layers) {
        new AlertDialog.Builder(mapView.getContext())
                .setCancelable(false)
                .setTitle(R.string.single_multip_layer_prompt)
                .setMessage(
                        R.string.layers_can_be_aggregated)
                .setPositiveButton(R.string.single,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                promptForName(layers);
                            }
                        })
                .setNegativeButton(R.string.multiple,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                for (Service layer : layers)
                                    doAddMapLayer(layer);
                            }
                        })
                .show();
    }

    void promptForName(final List<Service> layers) {

        final EditText input = new EditText(mapView.getContext());
        int maxLength = 26;
        input.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(maxLength)
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(
                mapView.getContext());

        builder.setTitle(R.string.agregated_layer_title);
        builder.setView(input)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                String name = input.getText().toString();
                                if (name.length() == 0)
                                    promptForName(layers);
                                else
                                    doAddMapLayer(
                                            com.atakmap.android.maps.tilesets.mobac.WebMapLayerService
                                                    .constructAggregate(name,
                                                            layers));

                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void shiftSelectedLayerZOrder(int shift) {
        final int selectedIndex = adapter.selectedIndex();
        if (selectedIndex < 0)
            return;
        LayerZOrderControl zorder = adapter.layer
                .getExtension(LayerZOrderControl.class);
        if (zorder == null)
            return;
        zorder.setPosition(adapter.layer.getSelection(),
                MathUtils.clamp(selectedIndex + shift, 0,
                        adapter.layer.getSelectionOptions().size() - 1));
        adapter.invalidate(true);
    }

    private void selectLayer(LayerSelectionAdapter adapter,
            LayerSelection layerSelection,
            boolean zoomOnSelect,
            boolean locked) {
        double targetScale = Double.NaN;
        GeoPoint targetPoint = null;

        if (zoomOnSelect) {
            double resolution = layerSelection.getMinRes();
            // XXX - apply some fudge factor as tiles aren't
            //       showing up immediately at the reported
            //       native resolution due to the fudging that
            //       occurs in the GLQuadTileNode. We didn't
            //       observe this with legacy since the levels
            //       got clamped, but the aggregate reports all
            //       levels available
            final double fudge = 0.33d;
            resolution *= fudge;

            final Geometry closestCov = LayerSelection.getCoverageNearest(
                    layerSelection, mapView.getCenterPoint().get());
            if (closestCov != null) {
                final double estRes = PanZoomReceiver
                        .estimateScaleToFitResolution(mapView,
                                closestCov.getEnvelope(),
                                (int) (mapView.getWidth() * 0.85d),
                                (int) (mapView.getHeight() * 0.85d));
                if (estRes < resolution)
                    resolution = estRes;
            }

            if (resolution < mapView.getMapResolution()) {
                targetScale = mapView
                        .mapResolutionAsMapScale(resolution);

                // if there is no 'target point', make sure that the
                // view will still show the layer once we're zoomed
                // in
                MapSceneModel model = mapView.getRenderer3().getMapSceneModel(
                        false, MapRenderer2.DisplayOrigin.UpperLeft);
                model = new MapSceneModel(mapView.getDisplayDpi(),
                        model.width,
                        model.height,
                        model.mapProjection,
                        model.mapProjection.inverse(model.camera.target, null),
                        model.focusx,
                        model.focusy,
                        model.camera.azimuth,
                        90d + model.camera.elevation,
                        resolution,
                        mapView.isContinuousScrollEnabled());

                PointF scratch = new PointF();

                scratch.x = 0;
                scratch.y = 0;
                GeoPoint upperLeft = model.inverse(scratch, null);
                if (upperLeft == null)
                    upperLeft = mapView.getCenterPoint().get();
                scratch.x = mapView.getWidth();
                scratch.y = 0;
                GeoPoint upperRight = model.inverse(scratch, null);
                if (upperRight == null)
                    upperRight = mapView.getCenterPoint().get();
                scratch.x = mapView.getWidth();
                scratch.y = mapView.getHeight();
                GeoPoint lowerRight = model.inverse(scratch, null);
                if (lowerRight == null)
                    lowerRight = mapView.getCenterPoint().get();
                scratch.x = 0;
                scratch.y = mapView.getHeight();
                GeoPoint lowerLeft = model.inverse(scratch, null);
                if (lowerLeft == null)
                    lowerLeft = mapView.getCenterPoint().get();

                final double north = MathUtils.max(
                        upperLeft.getLatitude(),
                        upperRight.getLatitude(),
                        lowerRight.getLatitude(),
                        lowerLeft.getLatitude());
                final double south = MathUtils.min(
                        upperLeft.getLatitude(),
                        upperRight.getLatitude(),
                        lowerRight.getLatitude(),
                        lowerLeft.getLatitude());
                final double east = MathUtils.max(
                        upperLeft.getLongitude(),
                        upperRight.getLongitude(),
                        lowerRight.getLongitude(),
                        lowerLeft.getLongitude());
                final double west = MathUtils.min(
                        upperLeft.getLongitude(),
                        upperRight.getLongitude(),
                        lowerRight.getLongitude(),
                        lowerLeft.getLongitude());

                if (!LayersMapComponent.intersects(
                        new Envelope[] {
                                new Envelope(
                                        west, south, 0d, east, north, 0d)
                        },
                        layerSelection.getBounds())) {

                    targetPoint = LayerSelection
                            .boundsGetCenterNearest(layerSelection,
                                    mapView.getCenterPoint().get());
                }
            } else if (!LayersMapComponent.isInView(mapView, layerSelection)) {
                targetPoint = LayerSelection
                        .boundsGetCenterNearest(layerSelection,
                                mapView.getCenterPoint().get());
            }
        }

        adapter.setSelected(layerSelection);
        adapter.setLocked(locked);

        // XXX - non-animated
        if (!Double.isNaN(targetScale) && targetPoint != null) {
            mapView.getMapController().panZoomTo(targetPoint,
                    targetScale,
                    true);
        } else if (targetPoint != null) {
            CameraController.Programmatic.panTo(mapView.getRenderer3(),
                    targetPoint, true);
        } else if (!Double.isNaN(targetScale)) {
            CameraController.Programmatic.zoomTo(mapView.getRenderer3(),
                    mapView.getMapResolution(targetScale), true);
        }

        adapter.sort();
    }

    private static class ServiceAdapter extends ArrayAdapter<Service> {

        private final List<Service> original = new ArrayList<>();
        private final List<Service> filtered = new ArrayList<>();
        private final Collection<Service> selected = new HashSet<>();

        private Filter filter;

        public ServiceAdapter(Context context, List<Service> services) {
            super(context, R.layout.wmswfs_item, services);
            this.filtered.addAll(services);
            this.original.addAll(services);
        }

        public List<Service> getSelected() {
            return new ArrayList<>(selected);
        }

        private static class ViewHolder {
            TextView textView;
            CheckBox checkbox;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new ServiceFilter();
            }
            return filter;
        }

        @Override
        public int getCount() {
            return filtered.size();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView,
                @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {

                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.wmswfs_item, null, false);

                holder = new ViewHolder();
                holder.checkbox = convertView.findViewById(R.id.selectCB);
                holder.textView = convertView.findViewById(R.id.result);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Service s = filtered.get(position);
            holder.textView.setText(s.getName());
            holder.checkbox
                    .setOnCheckedChangeListener(
                            new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(
                                        CompoundButton buttonView,
                                        boolean isChecked) {
                                    if (isChecked)
                                        selected.add(s);
                                    else
                                        selected.remove(s);
                                }
                            });
            holder.checkbox.setChecked(selected.contains(s));
            return convertView;
        }

        private class ServiceFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence term) {
                FilterResults results = new FilterResults();
                if (term == null || term.length() == 0) {
                    results.values = original;
                    results.count = original.size();
                } else {
                    String termLower = term.toString().toLowerCase(
                            LocaleUtil.getCurrent());
                    List<Service> filtered = new ArrayList<>();
                    for (Service service : original)
                        if (service.getName()
                                .toLowerCase(LocaleUtil.getCurrent())
                                .contains(termLower))
                            filtered.add(service);
                    results.values = filtered;
                    results.count = filtered.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {
                filtered.clear();
                notifyDataSetChanged();
                filtered.addAll((ArrayList<Service>) results.values);
                notifyDataSetChanged();
            }

        }

    }

    // The user wants to add this Layer as an online source.
    // Create a mobac XML file so it will be persisted, and add it to the
    // online layers dialog so we can see it now.
    private void doAddMapLayer(Service layer) {
        try {
            WebMapLayer.addToLayersDatabase(layer, mapView.getContext());
        } catch (Exception e) {
            Log.d(TAG, "error adding a layer to the database", e);
            Toast.makeText(
                    mapView.getContext(),
                    mapView.getContext().getString(
                            R.string.layer_select_error)
                            + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        // XXX - add to dialog. My android-fu has failed me temporarily.
    }

    static boolean isUniqueLayerName(RasterDataStore dataStore, String name) {
        RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
        params.names = Collections.singleton(name);
        if (dataStore.queryDatasetsCount(params) != 0)
            return false;

        // conscientious decision to just make sure we are not overwriting files of the same type in
        // the grg folder with the understanding that there could be GRG's imported from other places
        // with the same name.
        final File[] files = IOProviderFactory.listFiles(
                FileSystemUtils.getItem("grg"), new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return (s.equals(name + ".sqlite"));
                    }
                });
        return (files == null || files.length == 0);

    }

    public static boolean isSelectionDownloadable(LayerSelection ls) {
        if (ls == null)
            return false;
        final boolean quickCheck = (ls
                .getTag() instanceof MobileLayerSelectionAdapter.MobileImagerySpec
                && !((MobileLayerSelectionAdapter.MobileImagerySpec) ls
                        .getTag()).offlineOnly);
        if (!quickCheck)
            return false;
        return isDownloadable(
                ((MobileLayerSelectionAdapter.MobileImagerySpec) ls
                        .getTag()).desc);
    }

    /**
     * Given a Dataset Descriptor, determine if downloading is supported.
     * @param desc the dataset descriptor
     * @return true if downloading is supported
     */
    public static boolean isDownloadable(DatasetDescriptor desc) {
        if (desc == null)
            return false;

        try {
            // XXX - derive tile client
            TileClient client = null;
            try {
                client = TileClientFactory.create(desc.getUri(), null, null);
                if (client == null)
                    return false;

                final boolean[] retval = new boolean[] {
                        false
                };
                TileContainerFactory.visitCompatibleSpis(
                        new Visitor<Collection<TileContainerSpi>>() {
                            @Override
                            public void visit(
                                    Collection<TileContainerSpi> object) {
                                retval[0] = !object.isEmpty();
                            }
                        }, client);

                return retval[0];
            } finally {
                if (client != null)
                    client.dispose();
            }
        } catch (Exception e) {
            // see ATAK-15809 Playstore Crash: NativeProjection forward
            Log.e(TAG, "error occurred with descriptor: " + desc, e);
            return false;
        }
    }

    interface MapServiceTaskCallback {
        void selectQueryLayersToAdd(String uri, ServiceListing querier);
    }

    // Map service query uses the network, must use AsyncTask
    static class MapServiceTask
            extends AsyncTask<String, Void, Set<ServiceListing>> {

        private Exception error = null;
        private String url = null;
        private ProgressDialog pd;
        private final MapView mapView;
        private final MapServiceTaskCallback callback;

        public MapServiceTask(@NonNull MapView mapView,
                @NonNull MapServiceTaskCallback callback) {
            this.mapView = mapView;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mapView.getContext());
            pd.setTitle(mapView.getContext()
                    .getString(R.string.querying_the_server));
            pd.setMessage(mapView.getContext().getString(R.string.please_wait));
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

        @Override
        protected Set<ServiceListing> doInBackground(
                String... servicePath) {

            try {
                url = servicePath[0];
                return ServiceFactory.queryServices(url, false);
            } catch (Exception e) {
                // error should not occur, but better to be safe
                this.error = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Set<ServiceListing> results) {

            if (pd != null) {
                pd.dismiss();
            }
            // If one of our services can be reached, select that
            // layer. Otherwise, provide toasts that show the
            // errors that occurred for both of them.
            if (!results.isEmpty()) {
                final ServiceListing[] svcs = results
                        .toArray(new ServiceListing[0]);

                // if more than one server type was discovered, prompt the
                // user for what server they want
                if (svcs.length > 1) {
                    java.util.Arrays.sort(svcs,
                            new Comparator<ServiceListing>() {
                                @Override
                                public int compare(ServiceListing lhs,
                                        ServiceListing rhs) {
                                    return lhs.serverType
                                            .compareToIgnoreCase(
                                                    rhs.serverType);
                                }
                            });
                    final String[] svcNames = new String[svcs.length];
                    for (int i = 0; i < svcs.length; i++)
                        svcNames[i] = svcs[i].serverType;

                    new AlertDialog.Builder(mapView.getContext())
                            .setCancelable(false)
                            .setTitle(R.string.select_content_type_to_add)
                            .setItems(svcNames,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {

                                            callback.selectQueryLayersToAdd(url,
                                                    svcs[which]);
                                        }
                                    })
                            .setOnCancelListener(
                                    new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(
                                                DialogInterface dialog) {
                                        }
                                    })
                            .show();

                } else {
                    callback.selectQueryLayersToAdd(url, svcs[0]);
                }
            } else {
                String msg;
                if (error != null) {
                    msg = mapView.getContext().getString(
                            R.string.could_not_query_wms)
                            + error.getMessage();
                } else {
                    msg = "Failed to discover services on " + url;
                }

                Toast.makeText(
                        mapView.getContext(),
                        msg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class Builder {
        LayersManager _impl;
        MapView _mapView;
        String _showOfflinePrefKey;
        boolean _zoomOnClick;
        String _importMime, _importContent;
        File _defaultDownloadDirectory;

        MobileLayerSelectionAdapter _adapter;

        MobileImageryRasterLayer2 _layer;
        CardLayer _cardLayer;
        FeatureDataStore2 _outlinesDataStore;
        boolean _showVisibilityToggle;
        String _prefKeyLayerSelection;
        String _prefKeyOfflineOnly;

        boolean _downloadPromptForOverlay;
        boolean _showAddOnlineSourceButton;

        Builder(MapView view, MobileLayerSelectionAdapter adapter) {
            this(view, (MobileImageryRasterLayer2) adapter.layer);
            _adapter = adapter;
        }

        public Builder(MapView view, MobileImageryRasterLayer2 layer) {
            _mapView = view;
            _layer = layer;
            _impl = new LayersManager(_mapView);
            _showOfflinePrefKey = null;
            _zoomOnClick = true;
            _downloadPromptForOverlay = true;
            _showAddOnlineSourceButton = true;
        }

        public Builder setShowOfflinePreferenceKey(String prefKey) {
            _showOfflinePrefKey = prefKey;
            return this;
        }

        public Builder setZoomOnClick(boolean zoomOnClick) {
            _zoomOnClick = zoomOnClick;
            return this;
        }

        public Builder setDownloadPromptForOverlay(
                boolean downloadPromptForOverlay) {
            _downloadPromptForOverlay = downloadPromptForOverlay;
            return this;
        }

        public Builder setShowAddOnlineSourceButton(
                boolean showAddOnlineSourceButton) {
            _showAddOnlineSourceButton = showAddOnlineSourceButton;
            return this;
        }

        public Builder setDownloadImportParams(String importMime,
                String importContent) {
            _importMime = importMime;
            _importContent = importContent;
            return this;
        }

        public Builder setDefaultDownloadDirectory(File dir) {
            _defaultDownloadDirectory = dir;
            return this;
        }

        public Builder setOutlinesDataStore(FeatureDataStore2 outlines) {
            _outlinesDataStore = outlines;
            return this;
        }

        public Builder setPreferenceKeys(String prefKeyLayerSelection,
                String prefKeyOfflineOnly) {
            _prefKeyLayerSelection = prefKeyLayerSelection;
            _prefKeyOfflineOnly = prefKeyOfflineOnly;
            return this;
        }

        public Builder setShowVisibilityToggle(boolean showVisibilityToggle) {
            _showVisibilityToggle = showVisibilityToggle;
            return this;
        }

        public Builder setCardLayer(CardLayer cardLayer) {
            _cardLayer = cardLayer;
            return this;
        }

        protected AbstractLayersManagerView instantiateLayersManagerViewImpl(
                final LayerSelectionAdapter adapter, int viewId) {

            Context context = _mapView.getContext();
            AbstractLayersManagerView retval = (AbstractLayersManagerView) LayoutInflater
                    .from(context).inflate(viewId, null);

            // set the click listener for items in the list
            retval.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent,
                                View view,
                                int position, long id) {
                            if (parent != null) {
                                LayerSelection layerSelection = (LayerSelection) parent
                                        .getItemAtPosition(position);
                                _impl.selectLayer(adapter,
                                        layerSelection,
                                        _zoomOnClick,
                                        true);
                            }
                        }
                    });
            retval.setOnItemLongClickListener(
                    new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent,
                                View v,
                                int pos, long id) {
                            if (parent != null) {
                                LayerSelection layerSelection = (LayerSelection) parent
                                        .getItemAtPosition(pos);
                                showDetailsDialog(adapter, layerSelection);
                            }
                            return true;
                        }
                    });
            retval.setListAdapter(adapter);
            return retval;
        }

        private void showDetailsDialog(LayerSelectionAdapter adapter,
                LayerSelection selection) {
            if (adapter instanceof MobileLayerSelectionAdapter) {
                ((MobileLayerSelectionAdapter) adapter)
                        .showDetailsDialog(selection);
            }
        }

        public LayersManager build() {
            final Context c = _mapView.getContext();
            AtakPreferences _prefs = AtakPreferences.getInstance(c);

            if (_adapter != null) {
                _impl.adapter = _adapter;
            } else {
                _impl.adapter = new MobileLayerSelectionAdapter(
                        new CardLayer("ignored"),
                        _layer,
                        _outlinesDataStore,
                        _mapView,
                        c,
                        _showVisibilityToggle,
                        _prefKeyLayerSelection, _prefKeyOfflineOnly);
            }
            _impl.onlineView = (OnlineLayersManagerView) this
                    .instantiateLayersManagerViewImpl(_impl.adapter,
                            R.layout.layers_manager_online_view);

            View progressView = _impl.onlineView
                    .findViewById(R.id.downloadProgressLayout);
            View mobileTools = _impl.onlineView
                    .findViewById(R.id.mobileTools);
            _impl.downloader = new OnlineLayersDownloadManager(_mapView, _impl,
                    progressView, mobileTools);
            _impl.downloadRecv = new DownloadAndCacheBroadcastReceiver(
                    _impl.downloader, _impl);
            _impl.adapter.setDownloader(_impl.downloader);

            _impl.selectButton = _impl.onlineView
                    .findViewById(R.id.selectAreaBtn);

            _impl.importMime = _importMime;
            _impl.importContent = _importContent;
            _impl.defaultDownloadDirectory = _defaultDownloadDirectory;

            _impl.downloadPromptForOverlay = _downloadPromptForOverlay;

            final CheckBox showAllOffline = _impl.onlineView
                    .findViewById(R.id.showall);
            showAllOffline
                    .setOnCheckedChangeListener(
                            new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(
                                        CompoundButton buttonView,
                                        boolean isChecked) {
                                    if (_showOfflinePrefKey != null)
                                        _prefs.set(_showOfflinePrefKey,
                                                isChecked);
                                    _impl.adapter
                                            .setOnlyViewportDisplay(!isChecked);
                                }
                            });

            showAllOffline.setChecked(
                    _showOfflinePrefKey != null
                            ? _prefs.get(_showOfflinePrefKey, false)
                            : false);
            showAllOffline.setEnabled(true);

            final View moreBar = _impl.onlineView.findViewById(R.id.moreBar);
            final ImageButton more = _impl.onlineView
                    .findViewById(R.id.more);

            _impl.offlineOnlyCheckbox = _impl.onlineView.findViewById(
                    R.id.offlineModeSwitch);
            // set up online/offline switch
            _impl.offlineOnlyCheckbox.setTextOff(c.getString(R.string.local));
            _impl.offlineOnlyCheckbox.setTextOn(c.getString(R.string.online));
            _impl.offlineOnlyCheckbox.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final boolean isChecked = _impl.offlineOnlyCheckbox
                                    .isChecked();
                            _impl.adapter.setOfflineOnly(!isChecked);

                            if (!isChecked) {
                                more.setVisibility(View.GONE);
                                _impl.cancelDownloadWorkflow();
                                moreBar.setVisibility(View.GONE);
                                showAllOffline.setVisibility(View.VISIBLE);
                            } else {
                                more.setSelected(false);
                                more.setImageResource(R.drawable.arrow_right);
                                more.setVisibility(View.VISIBLE);
                                showAllOffline.setVisibility(View.GONE);
                            }
                        }
                    });

            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setSelected(!v.isSelected());
                    if (v.isSelected()) {
                        moreBar.setVisibility(View.VISIBLE);
                        more.setImageResource(R.drawable.arrow_down);
                    } else {
                        _impl.cancelDownloadWorkflow();
                        moreBar.setVisibility(View.GONE);
                        more.setImageResource(R.drawable.arrow_right);
                    }
                }
            });

            // set up the cancel button
            _impl.cancelButton = _impl.onlineView.findViewById(
                    R.id.cancelDownloadBtn);
            _impl.cancelButton.setOnClickListener(_impl);

            if (_impl.downloader.isDownloading()) {
                _impl.cancelButton.setEnabled(true);
                _impl.showDownloadProgress(true);
                _impl.cancelButton.setText(R.string.cancel);
            } else
                _impl.cancelButton.setEnabled(false);

            // set up the download button
            _impl.downloadButton = _impl.onlineView
                    .findViewById(R.id.downloadBtn);
            _impl.downloadButton.setOnClickListener(_impl);

            // set up the select area button
            _impl.selectButton.setOnClickListener(_impl);

            // set up the add source button
            Button addOnlineSourceButton = _impl.onlineView
                    .findViewById(R.id.addOnlineSource_btn);
            addOnlineSourceButton.setOnClickListener(_impl);
            addOnlineSourceButton.setVisibility(
                    _showAddOnlineSourceButton ? View.VISIBLE : View.GONE);

            _impl.offlineOnlyCheckbox.setChecked(
                    !_impl.adapter.isOfflineOnly());
            // dispatch state changes
            _impl.offlineOnlyCheckbox.callOnClick();

            ImageButton layerUpButton = _impl.onlineView
                    .findViewById(R.id.layers_mgr_layer_up);
            ImageButton layerDownButton = _impl.onlineView
                    .findViewById(R.id.layers_mgr_layer_down);
            if (_layer.getExtension(LayerZOrderControl.class) != null) {
                layerUpButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        _impl.shiftSelectedLayerZOrder(-1);
                    }
                });
                layerDownButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        _impl.shiftSelectedLayerZOrder(+1);
                    }
                });
            } else {
                layerUpButton.setVisibility(View.GONE);
                layerDownButton.setVisibility(View.GONE);
            }

            return _impl;
        }
    }
}
