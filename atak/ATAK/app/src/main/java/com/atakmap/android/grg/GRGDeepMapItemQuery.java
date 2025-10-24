
package com.atakmap.android.grg;

import android.content.SharedPreferences;

import com.atakmap.android.features.FeatureDataStoreDeepMapItemQuery;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.LocalRasterDataStore;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class GRGDeepMapItemQuery extends FeatureDataStoreDeepMapItemQuery
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        MapItem.OnVisibleChangedListener {

    private final static int OVERLAY_FEATURE_CACHE_DEPTH = 4;
    private final static AtomicInteger hitTestId = new AtomicInteger(0);

    private boolean hitTestEnabled;
    private final RasterLayer2 grgLayer;
    private final RasterDataStore grgDataStore;
    private final ConcurrentLinkedQueue<Map<String, MapItem>> deepHitTestCache = new ConcurrentLinkedQueue<>();

    public GRGDeepMapItemQuery(FeatureLayer3 layer,
            AbstractDataStoreRasterLayer2 grgDataStore) {
        super(layer);

        this.grgLayer = grgDataStore;
        this.grgDataStore = grgDataStore.getDataStore();
    }

    @Override
    protected MapItem featureToMapItem(Feature feature) {

        DatasetDescriptor tsInfo;

        RasterDataStore.DatasetDescriptorCursor result = null;
        try {
            RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
            params.names = Collections.singleton(feature.getName());
            params.limit = 1;

            result = this.grgDataStore.queryDatasets(params);
            if (!result.moveToNext())
                return null;

            tsInfo = result.get();
        } finally {
            if (result != null)
                result.close();
        }

        ImageOverlay item = new ImageOverlay(tsInfo, getFeatureUID(
                this.spatialDb, feature.getId()),
                true);
        item.setMetaString("layerId", String.valueOf(tsInfo.getLayerId()));
        item.setMetaString("layerUri", tsInfo.getUri());
        item.setMetaString("layerName", tsInfo.getName());
        item.setMetaBoolean("mbbOnly", true);
        item.setTitle(tsInfo.getName());
        item.setMetaBoolean("closed_line", true);

        File layerFile = null;
        if (this.grgDataStore instanceof LocalRasterDataStore)
            layerFile = ((LocalRasterDataStore) this.grgDataStore)
                    .getFile(tsInfo);
        if (layerFile != null)
            item.setMetaString("file", layerFile.getAbsolutePath());

        item.addOnVisibleChangedListener(this);

        return item;
    }

    @Override
    public synchronized SortedSet<MapItem> deepHitTest(MapView mapView,
            HitTestQueryParameters params) {
        if (!this.hitTestEnabled)
            return null;
        return super.deepHitTest(mapView, params);
    }

    @Override
    public SortedSet<MapItem> deepHitTest(MapView mapView,
            HitTestQueryParameters params,
            Map<Layer2, Collection<HitTestControl>> controls) {
        if (!this.hitTestEnabled)
            return null;
        SortedSet<MapItem> result = super.deepHitTest(mapView, params,
                controls);
        SortedSet<MapItem> overlayFeatures = deepHitTest(mapView, params,
                controls, grgLayer,
                new FeatureDataStoreDeepMapItemQuery.HitTestVisitor2.ResultToMapItem<Feature>() {
                    @Override
                    public void transform(Collection<Feature> features,
                            Collection<MapItem> items) {
                        if (features.isEmpty())
                            return;
                        final int htid = hitTestId.incrementAndGet();
                        Map<String, MapItem> results = new HashMap<>();
                        for (Feature f : features) {
                            final MapItem item = featureToMapItem(f,
                                    "grg." + htid);
                            if (item != null) {
                                results.put(item.getUID(), item);
                                items.add(item);
                            }
                        }
                        // cache the results for expected subsequent `deepFindItems(uid=xxx)`
                        deepHitTestCache.add(results);
                        // evict older cache entries
                        Iterator it = deepHitTestCache.iterator();
                        while (it.hasNext() && deepHitTestCache
                                .size() > OVERLAY_FEATURE_CACHE_DEPTH) {
                            it.next();
                            it.remove();
                        }
                    }
                }, Feature.class);
        if (overlayFeatures != null)
            result.addAll(overlayFeatures);
        return result;
    }

    @Override
    protected List<MapItem> deepFindItemsImpl(Map<String, String> metadata,
            int limit) {
        // check the overlay feature cache first
        final String uid = metadata.get("uid");
        if (uid != null) {
            for (Map<String, MapItem> overlayFeatures : deepHitTestCache) {
                MapItem item = overlayFeatures.get(uid);
                if (item != null)
                    return Collections.singletonList(item);

            }
        }
        return super.deepFindItemsImpl(metadata, limit);
    }

    /**************************************************************************/

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences prefs,
            String key) {

        if (key == null)
            return;

        if ("prefs_layer_grg_map_interaction".equals(key)) {
            this.hitTestEnabled = prefs.getBoolean(
                    "prefs_layer_grg_map_interaction", true);
        }
    }

    @Override
    public void onVisibleChanged(MapItem item) {
        final String opt = item.getMetaString("layerName", null);
        if (opt == null)
            return;
        this.grgLayer.setVisible(opt, item.getVisible());
    }
}
