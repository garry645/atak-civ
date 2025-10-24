
package com.atakmap.android.elev;

import com.atakmap.android.layers.LayerZOrderControl;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.ATAKActivity;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceBuilder;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.control.ElevationSourceControl;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.service.OnlineImageryExtension;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.opengl.ElMgrTerrainRenderService;
import com.atakmap.util.ConfigOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

final class ElevationSourcesHandler implements
        RasterDataStore.OnDataStoreContentChangedListener,
        RasterLayer2.OnSelectionVisibleChangedListener,
        LayerZOrderControl.OnSelectionZOrderChangedListener,
        OnlineImageryExtension.OnOfflineOnlyModeChangedListener,
        ATAKActivity.OnShutDownListener {

    ElevationSourceControl _control;
    AtakPreferences _prefs;
    LayerZOrderControl _zOrderControl;
    Map<String, Integer> _zOrder = new ConcurrentHashMap<>();
    Set<String> _invisibleSelections = new ConcurrentSkipListSet<>();
    boolean _offlineOnly;
    boolean _disposing;

    static Set<File> _invocationCaches = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    ElevationSourcesHandler(ElevationSourceControl control,
            AtakPreferences prefs) {
        _control = control;
        _prefs = prefs;
        _offlineOnly = false;
        _disposing = false;
    }

    private void refresh(RasterLayer2 layer, LayerZOrderControl zorderControl) {
        List<ElevationSource> sources = new LinkedList<>();
        ElevationSourceManager.getSources(sources);

        if (layer != null) {
            _invisibleSelections.clear();
            for (String sel : layer.getSelectionOptions())
                if (!layer.isVisible(sel))
                    _invisibleSelections.add(sel);
        }
        if (zorderControl != null) {
            _zOrder.clear();
            for (ElevationSource source : sources)
                _zOrder.put(source.getName(),
                        zorderControl.getPosition(source.getName()));
        }

        Collections.sort(sources, new Comparator<ElevationSource>() {
            @Override
            public int compare(ElevationSource a, ElevationSource b) {
                Integer az = _zOrder.get(a.getName());
                Integer bz = _zOrder.get(b.getName());
                if (az != null && bz == null)
                    return -1;
                else if (az == null && bz != null)
                    return 1;
                else if (az != null && bz != null
                        && az.intValue() != bz.intValue())
                    return az.intValue() - bz.intValue();
                final int namecmp = a.getName()
                        .compareToIgnoreCase(b.getName());
                if (namecmp != 0)
                    return namecmp;
                return a.hashCode() - b.hashCode();
            }
        });

        final boolean persistUpdate = (layer != null || zorderControl != null);

        JSONArray elevationSourcesConfig = new JSONArray();
        Collection<String> sourcenames = new LinkedHashSet<>(sources.size());

        Map<String, RenderSource> renderSources = new LinkedHashMap<>();
        for (ElevationSource source : sources) {
            if (source instanceof Controls) {
                final TileClient client = ((Controls) source)
                        .getControl(TileClient.class);
                if (client != null) {
                    final TileClientControl ctrl = client
                            .getControl(TileClientControl.class);
                    if (ctrl != null)
                        ctrl.setOfflineOnlyMode(_offlineOnly);
                }
            }
            if (!_invisibleSelections.contains(source.getName())) {
                RenderSource renderSource = renderSources.get(source.getName());
                if (renderSource == null)
                    renderSources.put(source.getName(),
                            renderSource = new RenderSource());
                renderSource.append(source);
            }
            if (persistUpdate) {
                if (!sourcenames.contains(source.getName())) {
                    try {
                        JSONObject entry = new JSONObject();
                        entry.put("name", source.getName());
                        entry.put("visible", !_invisibleSelections
                                .contains(source.getName()));
                        entry.put("zorder", sourcenames.size());
                        elevationSourcesConfig.put(entry);
                    } catch (JSONException ignored) {
                    }
                    sourcenames.add(source.getName());
                }
            }
        }

        if (persistUpdate) {
            _prefs.set("elevationSourcesConfig",
                    elevationSourcesConfig.toString());
        }

        ArrayList<ElevationSource> cascade = new ArrayList<>(
                renderSources.size());
        for (RenderSource renderSource : renderSources.values())
            cascade.add(renderSource.get());
        _control.setElevationSource(ElevationSourceBuilder.cascade("Elevation",
                cascade.toArray(new ElevationSource[0])));
    }

    @Override
    public void onSelectionZOrderChanged(LayerZOrderControl ctrl) {
        refresh(null, ctrl);
    }

    @Override
    public void onDataStoreContentChanged(RasterDataStore dataStore) {
        refresh(null, _disposing ? null : _zOrderControl);
    }

    @Override
    public void onSelectionVisibleChanged(RasterLayer2 layer) {
        refresh(layer, null);
    }

    @Override
    public void onOfflineOnlyModeChanged(OnlineImageryExtension ext,
            boolean offlineOnly) {
        _offlineOnly = offlineOnly;
        refresh(null, null);
    }

    void start(RasterLayer2 binding) {
        binding.addOnSelectionVisibleChangedListener(this);
        _zOrderControl = binding
                .getExtension(LayerZOrderControl.class);
        if (_zOrderControl != null)
            _zOrderControl.addOnSelectionZOrderChangedListener(this);
        final OnlineImageryExtension online = binding
                .getExtension(OnlineImageryExtension.class);
        if (online != null) {
            online.addOnOfflineOnlyModeChangedListener(this);
            _offlineOnly = online.isOfflineOnlyMode();
        }
        if (binding instanceof AbstractDataStoreRasterLayer2)
            ((AbstractDataStoreRasterLayer2) binding).getDataStore()
                    .addOnDataStoreContentChangedListener(this);

        this.refresh(binding, _zOrderControl);
    }

    void stop(RasterLayer2 binding) {
        binding.removeOnSelectionVisibleChangedListener(this);
        if (_zOrderControl != null) {
            _zOrderControl.removeOnSelectionZOrderChangedListener(this);
            _zOrderControl = null;
        }
        final OnlineImageryExtension online = binding
                .getExtension(OnlineImageryExtension.class);
        if (online != null)
            online.removeOnOfflineOnlyModeChangedListener(this);
        if (binding instanceof AbstractDataStoreRasterLayer2)
            ((AbstractDataStoreRasterLayer2) binding).getDataStore()
                    .removeOnDataStoreContentChangedListener(this);
    }

    @Override
    public void onShutDown() {
        _disposing = true;

        // XXX - workaround for ATAK-19516. delete all caches that weren't observed being used
        //       during this invocation
        final String cachePath = ConfigOptions
                .getOption("elmgr.terrain-cache.dir", null);
        if (cachePath == null)
            return;
        final File cacheFilesDir = new File(cachePath);
        File[] cacheFiles = cacheFilesDir.listFiles();
        if (cacheFiles == null)
            return;
        for (File cacheFile : cacheFiles) {
            if (_invocationCaches.contains(cacheFile))
                continue;
            cacheFile.delete();
        }
    }

    final static class RenderSource {
        Collection<ElevationSource> sources = new LinkedList<>();
        ElevationSourceDataStore.SourceTraits traits;

        void append(ElevationSource source) {
            sources.add(source);
            if (traits == null)
                traits = new ElevationSourceDataStore.SourceTraits(source);
            else
                traits.append(source);
        }

        ElevationSource get() {
            ElevationSource source = ElevationSourceBuilder.multiplex(
                    traits.name, sources.toArray(new ElevationSource[0]));
            if (traits.category != ElevationSourceDataStore.SourceTraits.Category.Tiles) {
                final String cachePath = ConfigOptions
                        .getOption("elmgr.terrain-cache.dir", null);
                final File cacheFile = (cachePath != null)
                        ? new File(cachePath,
                                FileSystemUtils.sanitizeFilename(traits.name))
                        : null;
                if (cacheFile != null)
                    _invocationCaches.add(cacheFile);
                final int cacheLimit = ConfigOptions
                        .getOption("elmgr.terrain-cache.limit", 0);

                ArrayList<ElevationSource> sources = new ArrayList<>();
                if (traits.category == ElevationSourceDataStore.SourceTraits.Category.DTED) {
                    if (ConfigOptions.getOption("elmgr.lores-dted-enabled",
                            1) != 0) {
                        final ElevationSource loresel = ElMgrTerrainRenderService
                                .createDefaultLoResElevationSource();
                        if (loresel != null) {
                            sources.add(
                                    ElevationSourceBuilder.constrainResolution(
                                            loresel,
                                            Double.NaN,
                                            OSMUtils.mapnikTileResolution(5)));
                        }
                    }
                    sources.add(ElevationSourceBuilder.constrainResolution(
                            new ElevationSourceBuilder.CLOD(source)
                                    .setStrategy(
                                            ElevationManager.HeightmapStrategy.LowFillHoles)
                                    .setPointSampler(true)
                                    .enableCaching(cacheFile, cacheLimit, true)
                                    .build(),
                            OSMUtils.mapnikTileResolution(6),
                            OSMUtils.mapnikTileResolution(7)));
                }
                ElevationSource resConstrained = ElevationSourceBuilder
                        .constrainResolution(
                                new ElevationSourceBuilder.CLOD(source)
                                        .setStrategy(
                                                ElevationManager.HeightmapStrategy.HighestResolution)
                                        .setPointSampler(false)
                                        .enableCaching(cacheFile, cacheLimit,
                                                false)
                                        .build(),
                                OSMUtils.mapnikTileResolution(8),
                                Double.NaN);
                if (sources.isEmpty()) {
                    source = resConstrained;
                } else {
                    sources.add(resConstrained);
                    source = ElevationSourceBuilder.multiplex(source.getName(),
                            sources.toArray(new ElevationSource[0]));
                }

            }
            return source;
        }
    }
}
