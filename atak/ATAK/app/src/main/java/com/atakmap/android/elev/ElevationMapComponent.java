
package com.atakmap.android.elev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;

import com.atakmap.android.contentservices.cdn.StreamingTilesMarshal;
import com.atakmap.android.contentservices.cdn.TiledGeospatialContentMarshal;
import com.atakmap.android.elev.dt2.Dt2ElevationData;
import com.atakmap.android.elev.dt2.GLDt2OutlineOverlay;
import com.atakmap.android.elev.tiles.TiledElevationImporter;
import com.atakmap.android.elev.tiles.TiledElevationSources;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImporterManager;
import com.atakmap.android.importexport.MarshalManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.android.overlay.MapOverlay;
import com.atakmap.android.overlay.MapOverlayBuilder;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ResUtils;
import com.atakmap.app.ATAKActivity;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.formats.dted.DtedElevationSource;
import com.atakmap.map.formats.srtm.SrtmElevationSource;
import com.atakmap.map.layer.control.ElevationSourceControl;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.util.Collections2;
import com.atakmap.util.ConfigOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;
import gov.tak.platform.client.dted.Dt2FileWatcher;

public class ElevationMapComponent extends AbstractMapComponent {

    final public static String TAG = "ElevationMapComponent";

    final public static String DTED_OUTLINE_VISIBLE_PREF_KEY = "dted_outlines_visible";

    private MapView _mapView;
    private final Set<ElevationSource> dt2dbs = Collections2
            .newIdentityHashSet();
    private MapOverlay _elevationDataOverlays;
    private GLDt2OutlineOverlay.Instance _outlineOverlay;
    private ElevationDownloader _downloader;
    private ElevationContentChangedDispatcher _dt2Content;

    private TiledElevationSources _elevationTileSources;

    private TiledElevationImporter _tiledElevationImporter;

    private static Dt2FileWatcher dtedWatcher;

    private MobileImageryRasterLayer2 _elevationLayer;
    private ElevationSourcesHandler _elevationSourcesHandler;

    private final static int DT2_SCANNER_EXTRA_THREAD_SLEEP = 50000;

    private NadirElevationToggle nadirElevationToggle;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        _mapView = view;
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_ADDED,
                _itemAddedListener);

        final AtakPreferences prefs = AtakPreferences.getInstance(context);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        _mapView.getContext().getString(
                                R.string.elevationPreferences),
                        "Adjust Elevation Display Preferences",
                        "dtedPreference",
                        _mapView.getContext().getResources().getDrawable(
                                R.drawable.nav_elevation),
                        new ElevationOverlaysPreferenceFragment()));

        ElevationManager.registerDataSpi(Dt2ElevationData.SPI);

        _dt2Content = new ElevationContentChangedDispatcher("DTED");
        ElevationSourceManager.attach(_dt2Content);

        // Initialize DTED watcher
        List<File> rootDirs = Arrays.asList(FileSystemUtils.getItems("DTED"));
        dtedWatcher = new Dt2FileWatcher(rootDirs) {
            @Override
            public void scan() {
                super.scan();
                if (Build.MODEL.equals("MPU5")) {
                    Log.d(TAG, "running on a mpu5, throttling performance");
                    try {
                        Thread.sleep(DT2_SCANNER_EXTRA_THREAD_SLEEP);
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        String[] dt2paths = findDtedPaths();
        for (String dt2path : dt2paths) {
            dt2dbs.add(DtedElevationSource.create(dt2path));
        }

        for (ElevationSource db : dt2dbs)
            ElevationSourceManager.attach(db);

        String[] srtmPaths = findDataPaths("SRTM");
        for (String srtmPath : srtmPaths)
            SrtmElevationSource.mountDirectory(new File(srtmPath));

        // DTED tile outlines
        GLLayerFactory.register(GLDt2OutlineOverlay.SPI);
        _outlineOverlay = new GLDt2OutlineOverlay.Instance(view);
        _outlineOverlay
                .setVisible(prefs.get( DTED_OUTLINE_VISIBLE_PREF_KEY, false));
        _mapView.addLayer(MapView.RenderStack.VECTOR_OVERLAYS, _outlineOverlay);

        // Automatic DTED downloader
        _downloader = new ElevationDownloader(view);

        dtedWatcher.addListener(new Dt2FileWatcher.Listener() {
            @Override
            public void onDtedFilesUpdated() {
                _dt2Content.contentChanged();
            }
        });

        // no reason this thread needs to run at anything higher than min priority
        dtedWatcher.setPriority(Thread.MIN_PRIORITY);
        dtedWatcher.start();

        // offline terrain cache (LRU) configuration
        // XXX - remove any legacy cache
        final File legacyCache = FileSystemUtils
                .getItem("Databases/terraincache.db");
        if (IOProviderFactory.exists(legacyCache))
            IOProviderFactory.delete(legacyCache);

        final File offlineCacheDir = new File(context.getCacheDir(),
                "terraincache");
        if (!IOProviderFactory.exists(offlineCacheDir))
            IOProviderFactory.mkdirs(offlineCacheDir);
        ConfigOptions.setOption("elmgr.terrain-cache.dir",
                offlineCacheDir.getAbsolutePath());
        ConfigOptions.setOption("elmgr.terrain-cache.limit",
                String.valueOf(40 * 1024 * 1024)); // 40MB

        // streaming terrain cache configuration
        FileSystemUtils.ensureDataDirectory("terraincache", false);
        ConfigOptions.setOption("terrain.offline-cache-dir",
                FileSystemUtils.getItem("terraincache").getAbsolutePath());

        // streaming terrain sources
        String cacheDir = ConfigOptions.getOption("terrain.offline-cache-dir",
                null);
        if (cacheDir == null) {
            cacheDir = _mapView.getContext().getCacheDir().getAbsolutePath()
                    + "/terraincache/";
            if (!new File(cacheDir).exists())
                if (!new File(cacheDir).mkdirs()) {
                    Log.e(TAG, "could not create the terrain cache directory");
                }
        }
        _elevationTileSources = new TiledElevationSources(
                FileSystemUtils.getItem("Databases/streamingterrain.db")
                        .getAbsolutePath(),
                cacheDir);

        MarshalManager.registerMarshal(
                TiledGeospatialContentMarshal.INSTANCE_TERRAIN);
        _tiledElevationImporter = new TiledElevationImporter(
                _mapView.getContext(),
                _elevationTileSources);
        ImporterManager.registerImporter(_tiledElevationImporter);
        ImportExportMapComponent.getInstance().addImporterClass(
                MarshalManager
                        .fromMarshal(
                                TiledGeospatialContentMarshal.INSTANCE_TERRAIN,
                                ResUtils.getDrawable(context, R.drawable.nav_elevation)));

        _elevationDataOverlays = new MapOverlayBuilder()
                .setIdentifier(ElevationMapComponent.class.getName()
                        + ".ElevationDataManager")
                .setName(view.getContext().getString(R.string.elevation_data))
                .setListItem(new HierarchyListItem.Builder("Elevation Manager")
                        .setAction(new GoTo() {
                            @Override
                            public boolean goTo(boolean select) {
                                AtakBroadcast.getInstance()
                                        .sendBroadcast(new Intent(
                                                ElevationManagerDropdownReceiver.ACTION_SHOW_MANAGER));
                                return true;
                            }
                        })
                        .setIcon("resource://" + R.drawable.ic_overlay_dted, -1)
                        .setPreferredListIndex(97)
                        .build())
                .build();

        _mapView.getMapOverlayManager().addOverlay(_elevationDataOverlays);

        for (ElevationSource elevationTiles : _elevationTileSources.get())
            ElevationSourceManager.attach(elevationTiles);

        _elevationLayer = new MobileImageryRasterLayer2(
                "Elevation",
                new ElevationSourceDataStore(),
                new RasterDataStore.DatasetQueryParameters(),
                false);

        final String elevationSourcesConfig = prefs
                .get("elevationSourcesConfig", null);
        refreshElevationLayersZOrder(elevationSourcesConfig);
        _elevationSourcesHandler = new ElevationSourcesHandler(_mapView
                .getRenderer3().getControl(ElevationSourceControl.class),
                prefs);
        if (context instanceof ATAKActivity)
            ((ATAKActivity) context)
                    .addOnShutDownListener(_elevationSourcesHandler);
        else
            throw new IllegalStateException();
        _elevationSourcesHandler.start(_elevationLayer);
        AtakBroadcast.getInstance().registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String action = intent.getAction();
                        if (action == null)
                            return;

                        if (action
                                .equals("com.atakmap.app.COMPONENTS_CREATED")) {
                            refreshElevationLayersZOrder(
                                    elevationSourcesConfig);
                        }
                    }
                },
                new AtakBroadcast.DocumentedIntentFilter(
                        "com.atakmap.app.COMPONENTS_CREATED"));

        AtakBroadcast.getInstance().registerReceiver(
                new ElevationManagerDropdownReceiver(_mapView, _elevationLayer,
                        _outlineOverlay),
                new AtakBroadcast.DocumentedIntentFilter(
                        ElevationManagerDropdownReceiver.ACTION_SHOW_MANAGER));

        nadirElevationToggle = new NadirElevationToggle(_mapView);
    }

    /**
     * Returns the instance of the Dt2FileWatcher
     * @return the instance
     */
    public static Dt2FileWatcher getDtedWatcher() {
        return dtedWatcher;
    }

    public static String[] findDtedPaths() {
        return findDataPaths("DTED");
    }

    private static String[] findDataPaths(String dataDirName) {
        String[] mountPoints = FileSystemUtils.findMountPoints();
        String[] dtedPaths = new String[mountPoints.length];

        int i = 0;
        for (String mountPoint : mountPoints) {
            dtedPaths[i] = mountPoint + File.separator + dataDirName
                    + File.separator;
            i++;
        }
        return dtedPaths;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {

        if (nadirElevationToggle != null)
            nadirElevationToggle.dispose();


        if (_tiledElevationImporter != null) {
            ImporterManager.unregisterImporter(_tiledElevationImporter);
            _tiledElevationImporter = null;
            MarshalManager
                    .unregisterMarshal(StreamingTilesMarshal.INSTANCE_TERRAIN);
        }

        if (_elevationSourcesHandler != null) {
            _elevationSourcesHandler.stop(_elevationLayer);
            _elevationLayer.getDataStore().dispose();
            _mapView.getRenderer3()
                    .getControl(ElevationSourceControl.class)
                    .setElevationSource(null);
        }

        for (ElevationSource db : dt2dbs) {
            ElevationSourceManager.detach(db);
            if (db instanceof gov.tak.api.util.Disposable)
                ((gov.tak.api.util.Disposable) db).dispose();
        }
        dt2dbs.clear();

        ElevationSourceManager.detach(_dt2Content);
        ElevationManager.unregisterDataSpi(Dt2ElevationData.SPI);

        if (_outlineOverlay != null) {
            _mapView.removeLayer(MapView.RenderStack.VECTOR_OVERLAYS,
                    _outlineOverlay);
            GLLayerFactory.register(GLDt2OutlineOverlay.SPI);
        }

        _downloader.dispose();

        if (_elevationTileSources != null) {
            for (ElevationSource source : _elevationTileSources.get())
                ElevationSourceManager.detach(source);
            _elevationTileSources.dispose();
            _elevationTileSources = null;
        }
    }

    private void refreshElevationLayersZOrder(
            final String elevationSourcesConfig) {
        if (elevationSourcesConfig != null) {
            ArrayList<Pair<String, int[]>> config = new ArrayList<>();
            try {
                JSONArray elevationSources = new JSONArray(
                        elevationSourcesConfig);
                for (int i = 0; i < elevationSources.length(); i++) {
                    JSONObject entry = elevationSources.optJSONObject(i);
                    if (entry == null)
                        continue;
                    String name = entry.getString("name");
                    boolean visible = entry.getBoolean("visible");
                    int zOrder = entry.getInt("zorder");
                    config.add(Pair.create(name, new int[] {
                            zOrder, visible ? 1 : 0
                    }));
                }
            } catch (JSONException ignored) {
            }

            Collections.sort(config, new Comparator<Pair<String, int[]>>() {
                @Override
                public int compare(Pair<String, int[]> a,
                        Pair<String, int[]> b) {
                    if (a.second[0] != b.second[0])
                        return a.second[0] - b.second[0];
                    return a.first.compareToIgnoreCase(b.first);
                }
            });
            for (int i = 0; i < config.size(); i++) {
                Pair<String, int[]> entry = config.get(i);
                _elevationLayer.setVisible(entry.first, entry.second[1] != 0);
                ((ElevationSourceDataStore) _elevationLayer.getDataStore())
                        .setPosition(entry.first, entry.second[0]);
            }
        }
    }

    private final OnPointChangedListener _onPointChangedListener = new OnPointChangedListener() {

        @Override
        public void onPointChanged(final PointMapItem item) {
            final GeoPoint gp = item.getPoint();
            if (gp != null && item.getGroup() != null
                    && !gp.isAltitudeValid()) {
                GeoPointMetaData gpm = ElevationManager
                        .getElevationMetadata(gp);

                if (gpm.get().isAltitudeValid()) {
                    GeoPoint newgp = new GeoPoint(gp.getLatitude(),
                            gp.getLongitude(), gp.getAltitude(),
                            gp.getAltitudeReference(), gp.getCE(),
                            gp.getLE());
                    item.setPoint(newgp);
                    AttributeSet as = new AttributeSet();
                    AttributeSetUtils.putAll(as, gpm.getMetaData(), true);
                    item.putAll(as);
                    // Log.d(TAG, "new point encountered, elevation looked up: " + a);

                    // Notify other components of the need to persist
                    item.persist(_mapView.getMapEventDispatcher(), null,
                            this.getClass());
                }
            }
        }
    };

    private final MapEventDispatcher.MapEventDispatchListener _itemAddedListener = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(MapEvent event) {
            if (event.getItem() instanceof PointMapItem) {
                final PointMapItem item = (PointMapItem) event.getItem();
                String entry = item.getMetaString("entry", null);
                if (entry != null && entry.equals("user")) {
                    // Log.d(TAG,
                    // "new point encountered, looking up elevation if the elevation is not valid");
                    // process the lookup.
                    _onPointChangedListener.onPointChanged(item);
                    // register a listener to process future lookups
                    item.addOnPointChangedListener(_onPointChangedListener);
                }
            }
        }
    };




}
