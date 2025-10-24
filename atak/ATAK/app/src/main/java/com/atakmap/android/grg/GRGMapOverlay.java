
package com.atakmap.android.grg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.atakmap.android.features.FeatureDataStoreMapOverlay;
import com.atakmap.android.hierarchy.HierarchyListAdapter;
import com.atakmap.android.hierarchy.HierarchyListFilter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.action.Action;
import com.atakmap.android.hierarchy.action.Delete;
import com.atakmap.android.hierarchy.action.Export;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.GroupDelete;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.items.AbstractChildlessListItem;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.image.ImageGalleryReceiver;
import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.importexport.send.SendDialog;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.missionpackage.export.MissionPackageExportWrapper;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.app.system.ResourceUtil;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.CameraController;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.LocalRasterDataStore;
import com.atakmap.map.layer.raster.PersistentRasterDataStore;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import gov.tak.api.util.Disposable;

public final class GRGMapOverlay extends FeatureDataStoreMapOverlay
        implements Disposable {

    private static final String TAG = "GRGMapOverlay";
    final static Set<String> SEARCH_FIELDS = new HashSet<>();

    static {
        SEARCH_FIELDS.add("layerId");
        SEARCH_FIELDS.add("layerUri");
        SEARCH_FIELDS.add("layerName");
    }

    private final MapView mapView;
    private final Context _context;

    private final AtakPreferences _prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener _prefListener;
    private final RasterLayer2 _layer;
    private final FeatureLayer3 _coveragesLayer;
    private final RasterDataStore _grgLayersDb;
    private GRGMapOverlayListModel _listModel;

    GRGMapOverlay(MapView view, RasterLayer2 layer,
            RasterDataStore grgLayersDb, FeatureLayer3 coveragesLayer) {
        super(view.getContext(),
                coveragesLayer.getDataStore(),
                null, // contentSource
                ResourceUtil.getString(view.getContext(),
                        R.string.image_overlay, R.string.grg),
                "android.resource://"
                        + view.getContext().getPackageName()
                        + "/" + R.drawable.ic_overlay_gridlines,
                new GRGDeepMapItemQuery(coveragesLayer,
                        (AbstractDataStoreRasterLayer2) layer),
                GRGMapComponent.IMPORTER_CONTENT_TYPE,
                GRGMapComponent.IMPORTER_DEFAULT_MIME_TYPE);

        this.mapView = view;
        _context = view.getContext();
        _layer = layer;
        _grgLayersDb = grgLayersDb;
        _coveragesLayer = coveragesLayer;

        _prefListener = (SharedPreferences.OnSharedPreferenceChangeListener) this
                .getQueryFunction();
        _prefs = AtakPreferences.getInstance(view.getContext());
        _prefs.registerListener(_prefListener);
        _prefListener.onSharedPreferenceChanged(_prefs.getSharedPrefs(),
                "prefs_layer_grg_map_interaction");
    }

    @Override
    public void dispose() {
        if (_prefs != null && _prefListener != null) {
            _prefs.unregisterListener(_prefListener);
        }
    }

    /**************************************************************************/
    // Map Overlay

    @Override
    public HierarchyListItem getListModel(BaseAdapter adapter, long actions,
            HierarchyListFilter filter) {
        GRGMapOverlayListModel lm = _listModel;
        if (lm == null)
            _listModel = new GRGMapOverlayListModel(adapter, filter);
        else
            lm.refresh(adapter, filter);
        return _listModel;
    }

    /**************************************************************************/
    // Overlay Manager List Model

    public class GRGMapOverlayListModel extends AbstractHierarchyListItem2
            implements Search, Export, GroupDelete, Visibility2,
            CompoundButton.OnCheckedChangeListener,
            SharedPreferences.OnSharedPreferenceChangeListener {

        private final static String TAG = "GRGMapOverlayListModel";

        private View _footer;
        private ToggleButton _vizBtn, _outlineBtn;
        private boolean grgRootVisibilityToggle = true;

        public GRGMapOverlayListModel(BaseAdapter listener,
                HierarchyListFilter filter) {
            this.asyncRefresh = true;
            refresh(listener, filter);
            _prefs.registerListener(this);
            onSharedPreferenceChanged(_prefs.getSharedPrefs(),
                    "grgRootVisibilityToggle");
        }

        @Override
        public <T extends Action> T getAction(Class<T> clazz) {
            if (grgRootVisibilityToggle)
                return super.getAction(clazz);
            else
                return null;
        }

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (key == null)
                return;

            if (key.equals("grgRootVisibilityToggle")) {
                grgRootVisibilityToggle = sharedPreferences.getBoolean(key,
                        grgRootVisibilityToggle);
            }

        }

        @Override
        public String getTitle() {
            return GRGMapOverlay.this.getName();
        }

        @Override
        public Drawable getIconDrawable() {
            return _context.getDrawable(R.drawable.ic_overlay_gridlines);
        }

        @Override
        public int getPreferredListIndex() {
            return 5;
        }

        @Override
        public int getDescendantCount() {
            return 0;
        }

        @Override
        public Object getUserObject() {
            return this;
        }

        @Override
        public View getExtraView() {
            return null;
        }

        @Override
        public View getFooterView() {
            if (_footer == null) {
                _footer = LayoutInflater.from(_context).inflate(
                        R.layout.grg_list_footer, mapView, false);
                _vizBtn = _footer.findViewById(
                        R.id.grg_visible_toggle);
                _outlineBtn = _footer.findViewById(
                        R.id.layer_outline_toggle);
            }

            _outlineBtn.setOnCheckedChangeListener(null);
            _outlineBtn.setChecked(_prefs.get("grgs.outlines-visible", true));
            _outlineBtn.setOnCheckedChangeListener(this);

            int viz = getVisibility();
            _vizBtn.setOnCheckedChangeListener(null);
            if (viz != Visibility2.INVISIBLE) {
                String text = _context.getString(viz == Visibility2.VISIBLE
                        ? R.string.visible_on
                        : R.string.visible_partial);
                _vizBtn.setTextOn(text);
            }
            _vizBtn.setChecked(viz != Visibility2.INVISIBLE);
            _vizBtn.setOnCheckedChangeListener(this);

            return _footer;
        }

        @Override
        public void refreshImpl() {

            synchronized (this) {
                if (refreshInProgress)
                    return;
                refreshInProgress = true;
            }

            // Get all GRGs
            final List<MapItem> mapItems = new ArrayList<>();
            long start = SystemClock.elapsedRealtime();

            final Collection<MapItem> result = getQueryFunction()
                    .deepFindItems(null, Double.NaN, null);
            if (result != null)
                mapItems.addAll(result);

            Log.d(TAG,
                    "refresh took: " + (SystemClock.elapsedRealtime() - start)
                            + "ms for " + mapItems.size() + " items");

            // Filter
            List<HierarchyListItem> filtered = new ArrayList<>();
            for (MapItem mi : mapItems) {
                if (mi instanceof ImageOverlay) {
                    GRGMapOverlayListItem item = new GRGMapOverlayListItem(
                            this.listener, (ImageOverlay) mi);
                    if (filter.accept(item))
                        filtered.add(item);
                }
            }

            // Sort
            sortItems(filtered);

            // Update
            updateChildren(filtered);

            refreshInProgress = false;
        }

        @Override
        public void dispose() {
            disposeChildren();
            _prefs.unregisterListener(this);
            _listModel = null;
        }

        @Override
        public boolean hideIfEmpty() {
            return true;
        }

        @Override
        public void onCheckedChanged(CompoundButton cb, boolean checked) {
            int i1 = cb.getId();
            if (i1 == R.id.layer_outline_toggle) {
                try {
                    _coveragesLayer.getDataStore().setFeatureSetsVisible(null,
                            checked);
                    _prefs.set("grgs.outlines-visible", checked);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set outline visibility", e);
                }
                getFooterView();
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        HierarchyListReceiver.REFRESH_HIERARCHY));

            } else if (i1 == R.id.grg_visible_toggle) {
                cb.setEnabled(false);
                grgVisibilityThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.elapsedRealtime();
                        setVisible(checked);
                        mapView.post(new Runnable() {
                            @Override
                            public void run() {
                                cb.setEnabled(true);
                                getFooterView();
                                AtakBroadcast.getInstance()
                                        .sendBroadcast(new Intent(
                                                HierarchyListReceiver.REFRESH_HIERARCHY));
                                Log.d(TAG,
                                        "grg visibility toggle took "
                                                + (SystemClock.elapsedRealtime()
                                                        - start)
                                                + "ms");
                            }
                        });
                    }
                });
            }

        }

        //**********************************************************************

        @Override
        public boolean setVisible(boolean visible) {
            List<Visibility> actions = getChildActions(Visibility.class);
            boolean ret = !actions.isEmpty();
            for (Visibility del : actions)
                ret &= del.setVisible(visible);
            return ret;
        }

        // Search
        @Override
        public Set<HierarchyListItem> find(String searchterm) {
            final String reterms = "*" + searchterm + "*";

            Set<Long> found = new HashSet<>();
            Set<HierarchyListItem> retval = new HashSet<>();

            List<HierarchyListItem> children = getChildren();
            if (FileSystemUtils.isEmpty(children)) {
                Log.d(TAG, "No items to search");
                return retval;
            }

            for (String field : SEARCH_FIELDS) {
                for (HierarchyListItem item : children) {
                    GRGMapOverlayListItem grg = (GRGMapOverlayListItem) item;
                    if (item == null || grg.getMapItem() == null) {
                        Log.w(TAG, "Skipping invalid item");
                        continue;
                    }

                    MapItem mp = grg.getMapItem();
                    if (!found.contains(mp.getSerialId())) {
                        //search metadata
                        if (MapGroup.matchItemWithMetaString(
                                mp, field, reterms)) {
                            retval.add(item);
                            found.add(mp.getSerialId());
                        }
                    }
                }
            }

            //also search filename
            for (HierarchyListItem item : children) {
                GRGMapOverlayListItem grg = (GRGMapOverlayListItem) item;
                if (item == null || grg.getMapItem() == null) {
                    Log.w(TAG, "Skipping invalid item");
                    continue;
                }

                //match all on GRG
                MapItem mp = grg.getMapItem();
                if (!found.contains(mp.getSerialId())) {
                    if ("grg".contains(searchterm.toLowerCase(LocaleUtil
                            .getCurrent()))) {
                        retval.add(item);
                        found.add(mp.getSerialId());
                    }
                }

                String fileString = mp.getMetaString("file", null);
                if (FileSystemUtils.isEmpty(fileString)) {
                    continue;
                }
                File file = new File(FileSystemUtils
                        .sanitizeWithSpacesAndSlashes(fileString));

                if (!found.contains(mp.getSerialId())) {
                    if (file.getName()
                            .toLowerCase(LocaleUtil.getCurrent())
                            .contains(
                                    searchterm.toLowerCase(LocaleUtil
                                            .getCurrent()))) {
                        retval.add(item);
                        found.add(mp.getSerialId());
                    }
                }
            }

            return retval;
        }

        /**************************************************************************/
        // Export

        @Override
        public boolean isSupported(Class<?> target) {
            return MissionPackageExportWrapper.class.equals(target);
        }

        @Override
        public Object toObjectOf(Class<?> target, ExportFilters filters)
                throws FormatNotSupportedException {
            if (super.getChildCount() <= 0 || !isSupported(target)) {
                //nothing to export
                return null;
            }

            if (MissionPackageExportWrapper.class.equals(target)) {
                return toMissionPackage(filters);
            }

            return null;
        }

        private MissionPackageExportWrapper toMissionPackage(
                ExportFilters filters)
                throws FormatNotSupportedException {
            MissionPackageExportWrapper f = new MissionPackageExportWrapper();
            List<HierarchyListItem> children = getChildren();
            for (HierarchyListItem item : children) {
                if (item instanceof MapItemUser)
                    toMissionPackage(f, ((GRGMapOverlayListItem) item)
                            .getMapItem(), filters);
            }

            if (FileSystemUtils.isEmpty(f.getFilepaths())) {
                return null;
            }

            return f;
        }

        private void toMissionPackage(MissionPackageExportWrapper f,
                MapItem item,
                ExportFilters filters) throws FormatNotSupportedException {

            if (item instanceof Exportable
                    &&
                    ((Exportable) item)
                            .isSupported(MissionPackageExportWrapper.class)) {
                MissionPackageExportWrapper itemWrapper = (MissionPackageExportWrapper) ((Exportable) item)
                        .toObjectOf(
                                MissionPackageExportWrapper.class, filters);
                if (itemWrapper != null) {
                    //just add files
                    f.getFilepaths().addAll(itemWrapper.getFilepaths());
                }
            }
        }
    }

    public static int getColor(ImageOverlay item, FeatureLayer3 layer) {
        int color = 0;

        if (item != null && layer != null) {

            FeatureQueryParameters params = new FeatureQueryParameters();
            params.names = Collections.singleton(item.toString());

            FeatureCursor c = null;
            try {
                c = layer.getDataStore().queryFeatures(params);
                Feature f;
                while (c.moveToNext()) {
                    f = c.get();
                    if (f.getStyle() instanceof BasicStrokeStyle) {
                        color = ((BasicStrokeStyle) f.getStyle())
                                .getColor();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to query color", e);
            } finally {
                if (c != null)
                    c.close();
            }
        }

        return color;
    }

    /**
     * HierarchyListItem for GRGs
     * Partially based on MapItemHierarchyListItem
     */
    public class GRGMapOverlayListItem extends AbstractChildlessListItem
            implements GoTo, Export, Delete, Visibility, MapItemUser,
            View.OnClickListener {

        private static final String TAG = "GRGMapOverlayListItem";
        private final ImageOverlay _item;
        private final DatasetDescriptor _info;
        private int _color = 0;

        GRGMapOverlayListItem(BaseAdapter listener, ImageOverlay item) {
            this.listener = listener;
            this._item = item;
            _info = _item.getLayerInfo();
        }

        @Override
        public boolean goTo(boolean select) {

            GeoPoint point = computeBestPointInBounds();
            if (point == null) {
                panAndZoomToFit();
            } else {
                CameraController.Programmatic.panTo(
                        mapView.getRenderer3(),
                        point, true);
            }

            if (select && _prefs.get("prefs_layer_grg_map_interaction", true)) {
                Intent showMenu = new Intent(MapMenuReceiver.SHOW_MENU);
                showMenu.putExtra("uid", getUID());
                AtakBroadcast.getInstance().sendBroadcast(showMenu);
            }
            return true;
        }

        @Override
        public String getTitle() {
            if (this._item == null) {
                Log.w(TAG, "Skipping invalid title");
                return "GRG";
            }

            return _item.getMetaString("layerName", _item.getUID());
        }

        @Override
        public String getDescription() {
            if (_item != null) {
                CoordinateFormat cf = CoordinateFormat.find(_prefs.get(
                        "coord_display_pref", _context.getString(
                                R.string.coord_display_pref_default)));
                return CoordinateFormatUtilities.formatToString(
                        _item.getCenter().get(), cf);
            }
            return null;
        }

        @Override
        public View getExtraView(View v, ViewGroup parent) {
            ExtraHolder h = v != null && v.getTag() instanceof ExtraHolder
                    ? (ExtraHolder) v.getTag()
                    : null;
            if (h == null) {
                h = new ExtraHolder();
                v = LayoutInflater.from(_context).inflate(
                        R.layout.grg_list_item_extra, parent, false);

                h.delete = v.findViewById(R.id.grg_delete);
                h.send = v.findViewById(R.id.grg_send);
                h.menu = v.findViewById(R.id.grg_menu);
                h.menu_open = v.findViewById(R.id.grg_menu_open);
                h.panto = v.findViewById(R.id.grg_panto);
                showExtraMenu(h, false);
                v.setTag(h);
            }
            if (this.listener instanceof HierarchyListAdapter
                    && ((HierarchyListAdapter) this.listener)
                            .getSelectHandler() != null) {
                // Hide send and delete buttons when selecting
                v.setVisibility(View.GONE);
                h.delete.setOnClickListener(null);
                h.send.setOnClickListener(null);
                h.menu_open.setOnClickListener(null);
                h.panto.setOnClickListener(null);
            } else {
                v.setVisibility(View.VISIBLE);
                h.delete.setOnClickListener(this);
                h.send.setOnClickListener(this);
                final ExtraHolder fh = h;
                h.menu_open.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showExtraMenu(fh, true);
                    }
                });
                h.panto.setOnClickListener(this);

            }
            if (h.id != _info) {
                showExtraMenu(h, false);
                h.id = _info;
            }
            return v;
        }

        private void showExtraMenu(ExtraHolder h, boolean show) {
            h.menu.setVisibility(show ? View.VISIBLE : View.GONE);
            h.menu_open.setVisibility(!show ? View.VISIBLE : View.GONE);
            if (show) {
                h.menu_open.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showExtraMenu(h, false);
                    }
                }, 5000);
            }
        }

        /**
         * Computes a point that is in the center of two overlapping bounds.  If the bounds do not
         * overlap, then null is returned.
         * @param b1 the first geobounds
         * @param b2 the second geobounds
         * @return the center point of the overlap or null if no overlap exists
         */
        private GeoPoint computeBestOverlapingCenterPoint(GeoBounds b1,
                GeoBounds b2) {
            if (b1.intersects(b2)) {
                // in the case of something something crossing the idl, for now return null
                // which will revert to legacy behavior
                if (b1.crossesIDL() || b2.crossesIDL())
                    return null;

                double north = Math.min(b1.getNorth() + 90, b2.getNorth() + 90);
                double south = Math.max(b1.getSouth() + 90, b2.getSouth() + 90);

                double east = Math.min(b1.getEast() + 180, b2.getEast() + 180);
                double west = Math.max(b1.getWest() + 180, b2.getWest() + 180);
                double lat = (north + south) / 2.0D;
                double lng = (east + west) / 2.0D;
                return new GeoPoint(lat - 90, lng - 180);
            }
            return null;
        }

        /**
         * The best point in bounds is computed by return the center of the map if the
         * center of the map is within the bounds of the grg or the center point of the
         * intersection between the map and the grg.   If neither of these exist, just return
         * null.
         * @return the best point to use for the grg based on the above information
         */
        private GeoPoint computeBestPointInBounds() {
            if (_info == null) {
                Log.w(TAG, "Skipping invalid zoom");
                return null;
            }
            // Zoom to fit GRG bounds
            Geometry g = _info.getCoverage(null);
            Envelope e = _info.getMinimumBoundingBox();

            // match the same logic from before to determine in bounds
            if (g != null) {
                Envelope ge = g.getEnvelope();
                GeoBounds gb = new GeoBounds(ge.minY, ge.minX, ge.maxY,
                        ge.maxX);
                if (gb.contains(mapView.getCenterPoint().get())) {
                    return mapView.getCenterPoint().get();
                }
                return computeBestOverlapingCenterPoint(mapView.getBounds(),
                        gb);
            } else if (e != null) {
                GeoBounds gb = new GeoBounds(e.minY, e.minX, e.maxY, e.maxX);
                if (gb.contains(mapView.getCenterPoint().get())) {
                    return mapView.getCenterPoint().get();
                }
                return computeBestOverlapingCenterPoint(mapView.getBounds(),
                        gb);
            }
            return null;
        }

        /**
         * This will pan to the GRG and produce a zoom level which is optimal for the entire product.
         * @return true if this is successful.
         */
        private boolean panAndZoomToFit() {
            if (_info == null) {
                Log.w(TAG, "Skipping invalid zoom");
                return false;
            }

            // Zoom to fit GRG bounds
            Geometry g = _info.getCoverage(null);
            Envelope e = _info.getMinimumBoundingBox();

            if (g != null) {
                ImageGalleryReceiver.zoomToBounds(g.getEnvelope());
            } else if (e != null) {
                ImageGalleryReceiver.zoomToBounds(e);
            } else {
                mapView.getMapController().dispatchOnPanRequested();
                final MapSceneModel sm = mapView.getRenderer3()
                        .getMapSceneModel(
                                false, MapRenderer2.DisplayOrigin.UpperLeft);
                // XXX - ideally, I think this eliminates the tilt, however,
                //       preserving legacy behavior
                mapView.getRenderer3().lookAt(
                        _item.getCenter().get(),
                        _item.getLayerInfo().getMinResolution(null),
                        sm.camera.azimuth,
                        90d + sm.camera.elevation,
                        true);
            }
            return true;
        }

        @Override
        public void onClick(View v) {
            final int i = v.getId();
            if (i == R.id.grg_panto) {
                Intent hideMenu = new Intent(MapMenuReceiver.HIDE_MENU);
                AtakBroadcast.getInstance().sendBroadcast(hideMenu);
                panAndZoomToFit();
            } else if (i == R.id.grg_delete) {
                promptDelete();
            } else if (i == R.id.grg_send) {
                Collection<String> paths = getFiles();
                if (paths.isEmpty()) {
                    Log.w(TAG, "Cannot send invalid data");
                    return;
                }
                SendDialog.Builder b = new SendDialog.Builder(mapView);
                for (String path : paths) {
                    File f = new File(FileSystemUtils
                            .sanitizeWithSpacesAndSlashes(path));
                    b.addFile(f, GRGMapComponent.IMPORTER_CONTENT_TYPE);
                }
                b.show();
            }
        }

        @Override
        public String getIconUri() {
            return "android.resource://" + _context.getPackageName()
                    + "/" + R.drawable.ic_overlay_gridlines;
        }

        @Override
        public int getIconColor() {
            if (_color == 0) {
                _color = getColor(_item, _coveragesLayer);
            }
            return _color;
        }

        @Override
        public boolean delete() {
            if (_item == null)
                return false;
            String name = _item.toString();
            RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
            params.names = Collections.singleton(name);
            Collection<String> pathsToDelete = getFiles();
            for (String path : pathsToDelete) {
                Log.d(TAG, "Deleting " + name + " at " + path);
                Intent i = new Intent(
                        ImportExportMapComponent.ACTION_DELETE_DATA);
                i.putExtra(ImportReceiver.EXTRA_CONTENT,
                        GRGMapComponent.IMPORTER_CONTENT_TYPE);
                i.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                        GRGMapComponent.IMPORTER_DEFAULT_MIME_TYPE);
                i.putExtra(ImportReceiver.EXTRA_URI, path);
                AtakBroadcast.getInstance().sendBroadcast(i);
            }
            return true;
        }

        @Override
        public boolean setVisible(boolean visible) {
            if (_layer.isVisible(_item.toString()) != visible) {
                _layer.setVisible(_item.toString(), visible);
                _info.setExtraData("visible", String.valueOf(visible));
                if (_grgLayersDb instanceof PersistentRasterDataStore) {
                    grgVisibilityBackfillThread.execute(new Runnable() {
                        @Override
                        public void run() {
                            ((PersistentRasterDataStore) _grgLayersDb)
                                    .updateExtraData(_info);
                        }
                    });
                }
            }
            return true;
        }

        @Override
        public boolean isVisible() {
            return _layer.isVisible(_item.toString());
        }

        @Override
        public Object getUserObject() {
            return _item;
        }

        @Override
        public String getUID() {
            if (this._item == null) {
                Log.w(TAG, "Skipping invalid UID");
                return null;
            }

            return this._item.getUID();
        }

        @Override
        public MapItem getMapItem() {
            return this._item;
        }

        /**************************************************************************/
        // Export

        @Override
        public boolean isSupported(Class<?> target) {
            return _item != null && _item.isSupported(target);
        }

        @Override
        public Object toObjectOf(Class<?> target, ExportFilters filters)
                throws FormatNotSupportedException {
            if (this._item == null || !isSupported(target)) {
                return false;
            }

            return _item.toObjectOf(target, filters);

        }

        private void promptDelete() {
            AlertDialog.Builder b = new AlertDialog.Builder(_context);
            b.setTitle(ResourceUtil.getString(_context, R.string.civ_delete_grg,
                    R.string.delete_grg));
            b.setIcon(R.drawable.ic_menu_delete);
            b.setMessage(_context.getString(R.string.delete) + getTitle()
                    + _context.getString(R.string.question_mark_symbol));
            b.setNegativeButton(R.string.cancel, null);
            b.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int i) {
                            delete();
                            AtakBroadcast.getInstance().sendBroadcast(
                                    new Intent(
                                            HierarchyListReceiver.REFRESH_HIERARCHY));
                        }
                    });
            b.show();
        }

        private Set<String> getFiles() {
            String name = _item.toString();
            RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
            params.names = Collections.singleton(name);

            Log.d(TAG, "files for " + name);
            Set<String> retval = new HashSet<>();
            RasterDataStore.DatasetDescriptorCursor result = null;
            try {
                result = _grgLayersDb.queryDatasets(params);
                while (result.moveToNext()) {
                    File file = null;
                    if (_grgLayersDb instanceof LocalRasterDataStore)
                        file = ((LocalRasterDataStore) _grgLayersDb)
                                .getFile(result.get());

                    if (file == null) {
                        file = new File(result.get().getUri());
                        String f = result.get().getUri();
                        Log.d(TAG, "uri: " + f);
                        if (FileSystemUtils.isFile(f)) {
                            file = new File(FileSystemUtils
                                    .sanitizeWithSpacesAndSlashes(f));
                        } else {
                            try {
                                Uri uri = Uri.parse(f);
                                if (uri != null && uri.getPath() != null) {
                                    if (FileSystemUtils.isFile(uri.getPath()))
                                        file = new File(FileSystemUtils
                                                .sanitizeWithSpacesAndSlashes(
                                                        uri.getPath()));
                                    else
                                        file = null;
                                }
                            } catch (Exception e) {
                                Log.w(TAG,
                                        "Exception occurred while obtaining file for "
                                                + name,
                                        e);
                            }
                        }

                    }

                    if (file != null)
                        retval.add(file.getAbsolutePath());
                }
            } finally {
                if (result != null)
                    result.close();
            }

            return retval;
        }
    }

    private static class ExtraHolder {
        Object id;
        View delete, send, menu, menu_open, panto;
    }

    private final Executor grgVisibilityThread = Executors
            .newSingleThreadExecutor(
                    new NamedThreadFactory("grgVisibilityThread"));

    private final Executor grgVisibilityBackfillThread = Executors
            .newSingleThreadExecutor(
                    new NamedThreadFactory("grgVisibilityBackfill"));

    private volatile boolean refreshInProgress = false;

}
