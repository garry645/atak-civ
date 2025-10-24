
package com.atakmap.android.grg;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.importexport.ImporterManager;
import com.atakmap.android.layers.ExternalLayerDataImporter;
import com.atakmap.android.layers.LayersMapComponent;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.RenderStack;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.MultiLayer;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.ogr.SchemaDefinitionRegistry;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.OutlinesFeatureDataStore2;
import com.atakmap.map.layer.raster.PersistentRasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.service.LayerAttributeExtension;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.spatial.file.KmlFileSpatialDb;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides for GRG layers to be handled on the screen.   This is not 
 * related to the Layer Manager which controls GRG visibility.
 */
public class GRGMapComponent extends AbstractMapComponent {

    private ExternalLayerDataImporter _externalGRGDataImporter;

    public static final String IMPORTER_CONTENT_TYPE = "External GRG Data";
    public static final String IMPORTER_DEFAULT_MIME_TYPE = "application/octet-stream";
    public static final String IMPORTER_TIFF_MIME_TYPE = "image/tiff";

    private final String[] _importerMimeTypes = new String[] {
            IMPORTER_DEFAULT_MIME_TYPE,
            IMPORTER_TIFF_MIME_TYPE,
            KmlFileSpatialDb.KMZ_FILE_MIME_TYPE,
            "image/nitf",
            "inode/directory"
    };
    private final String[] _importerHints = new String[] {
            null
    };

    private final static File DATABASE_FILE = FileSystemUtils
            .getItem("Databases/GRGs2.sqlite");

    private PersistentRasterDataStore grgDatabase;
    private GRGMapOverlay overlay;
    private GRGContentResolver contentResolver;
    private FeatureDataStore2 coverageDataStore;
    private FeatureLayer3 coveragesLayer;
    private DatasetRasterLayer2 rasterLayer;
    private MCIAGRGMapOverlay mciagrgMapOverlay;
    private GRGMapReceiver mapReceiver;

    private MultiLayer grgLayer;

    @Override
    public void onCreate(Context context, Intent intent, final MapView view) {
        DatasetDescriptorFactory2.register(new MCIAGRGLayerInfoSpi());

        SchemaDefinitionRegistry.register(MCIA_GRG.SECTIONS_SCHEMA_DEFN);
        SchemaDefinitionRegistry.register(MCIA_GRG.SUBSECTIONS_SCHEMA_DEFN);
        SchemaDefinitionRegistry.register(MCIA_GRG.BUILDINGS_SCHEMA_DEFN);

        this.grgDatabase = new PersistentRasterDataStore(DATABASE_FILE,
                LayersMapComponent.LAYERS_PRIVATE_DIR);

        _externalGRGDataImporter = new ExternalLayerDataImporter(context,
                this.grgDatabase,
                IMPORTER_CONTENT_TYPE, _importerMimeTypes, _importerHints);
        ImporterManager.registerImporter(_externalGRGDataImporter);

        // TODO: Marshal for grg layers?

        // validate catalog contents
        this.grgDatabase.refresh();

        rasterLayer = new DatasetRasterLayer2("GRG rasters",
                this.grgDatabase, 0);
        // mark the layer as containing overlays
        LayerAttributeExtension attrs = rasterLayer
                .getExtension(LayerAttributeExtension.class);
        if (attrs != null)
            attrs.setAttribute("overlay", Boolean.TRUE);

        coverageDataStore = new CoverageDataStore(rasterLayer,
                new FeatureSetDatabase2(null));
        this.coveragesLayer = new FeatureLayer3("GRG Outlines",
                coverageDataStore);

        AtakPreferences prefs = AtakPreferences.getInstance(context);

        final boolean outlines = prefs.get("grgs.outlines-visible", true);
        try {
            this.coverageDataStore.setFeatureSetsVisible(null, outlines);
        } catch (DataStoreException ignored) {
        }

        LayersMapComponent.loadLayerState(
                prefs.getSharedPrefs(),
                "grgs",
                this.rasterLayer,
                this.coverageDataStore,
                false);

        // Used to map GRG files to associated metadata
        this.contentResolver = new GRGContentResolver(view, this.grgDatabase,
                this.rasterLayer, this.coverageDataStore);
        URIContentManager.getInstance().registerResolver(this.contentResolver);

        // Overlay Manager
        this.overlay = new GRGMapOverlay(view, rasterLayer, this.grgDatabase,
                this.coveragesLayer);

        this.grgLayer = new MultiLayer("GRG");
        this.grgLayer.addLayer(rasterLayer);
        this.grgLayer.addLayer(this.coveragesLayer);

        view.addLayer(MapView.RenderStack.RASTER_OVERLAYS, this.grgLayer);

        view.getMapOverlayManager().addOverlay(this.overlay);
        view.getMapOverlayManager().addOverlay(
                mciagrgMapOverlay = new MCIAGRGMapOverlay(context,
                        this.grgDatabase));

        startGrgDiscoveryThread(context);

        this.mapReceiver = new GRGMapReceiver(view, coverageDataStore,
                grgDatabase, rasterLayer, overlay);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (this.rasterLayer != null) {
            AtakPreferences prefs = AtakPreferences.getInstance(context);

            LayersMapComponent.saveLayerState(prefs.getSharedPrefs(),
                    "grgs",
                    this.rasterLayer,
                    this.coverageDataStore);

            try {
                FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
                params.visibleOnly = true;
                params.limit = 1;
                prefs.set("grgs.outlines-visible", (this.coverageDataStore
                        .queryFeaturesCount(params) > 0));
            } catch (DataStoreException ignored) {
            }

        }

        URIContentManager.getInstance().unregisterResolver(
                this.contentResolver);
        this.contentResolver.dispose();

        if (this.overlay != null) {
            view.getMapOverlayManager().removeOverlay(this.overlay);
            view.getMapOverlayManager().removeOverlay(this.mciagrgMapOverlay);
            view.removeLayer(RenderStack.RASTER_OVERLAYS, this.grgLayer);

            this.overlay.dispose();
            this.overlay = null;
        }

        if (this.mapReceiver != null)
            this.mapReceiver.dispose();

        this.grgDatabase.dispose();
    }

    // IO Abstraction

    private void startGrgDiscoveryThread(Context context) {

        // XXX - periodic discovery/refresh?

        Thread t = new Thread(new GRGDiscovery(context, this.grgDatabase));
        t.setName("GRG-discovery-thread");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    // ----

    public RasterLayer2 getLayer() {
        return this.rasterLayer;
    }

    public FeatureLayer3 getCoverageLayer() {
        return this.coveragesLayer;
    }

    // XXX - workaround pending ENGINE-665
    final static class CoverageDataStore extends OutlinesFeatureDataStore2 {
        private final static int[] COLOR_ARRAY = new int[] {
                0xFF00FF00,
                0xFFFFFF00,
                0xFFFF00FF,
                0xFF0000FF,
                0xFF00FFFF,
                0xFFFF0000,
                0xFF007F00,
                0xFF7FFF00,
                0xFF7F00FF,
                0xFF00FFFF,
                0xFF7F0000,
                0xFFFF7F00,
                0xFFFF007F,
                0xFF007FFF,
                0xFF7F7F00,
                0xFF7F007F,
                0xFF007F7F,
                0xFF7F7F7F,
        };

        private final static AttributeSet EMPTY_ATTR = new AttributeSet();

        private final FeatureDataStore2 impl;

        public CoverageDataStore(RasterLayer2 layer, FeatureDataStore2 impl) {
            super(layer, 0, false, impl);

            this.impl = impl;
        }

        @Override
        protected void refreshImpl() {
            if (this.impl == null) {
                super.refreshImpl();
                return;
            }

            Map<String, Boolean> featureVis = new HashMap<>();
            Map<String, Integer> featureCol = new HashMap<>();
            FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
            params.featureSetFilter = new FeatureSetQueryParameters();
            params.featureSetFilter.ids = Collections
                    .singleton(1L);

            FeatureCursor result = null;
            try {
                result = impl.queryFeatures(params);
                Feature f;
                while (result.moveToNext()) {
                    f = result.get();
                    featureVis.put(f.getName(),
                            this.isFeatureVisible(f.getId()));
                    Style s = f.getStyle();
                    if (s instanceof BasicStrokeStyle)
                        featureCol.put(f.getName(),
                                ((BasicStrokeStyle) s).getColor());
                }
            } catch (DataStoreException dataStoreException) {
                Log.e(TAG, "queryFeatures failed", dataStoreException);
            } finally {
                if (result != null)
                    result.close();
            }

            // obtain the selections OUTSIDE of the synchronized block
            Map<String, Geometry> sel = this.getSelections();

            // lock the database and update the geometry
            synchronized (this) {
                try {
                    this.acquireModifyLock(true);
                    try {
                        Utils.deleteFeatures(this, params);

                    } catch (DataStoreException dataStoreException) {
                        Log.e(TAG, "deleteFeatures failed", dataStoreException);
                    }

                    for (Map.Entry<String, Geometry> entry : sel.entrySet()) {
                        String s = entry.getKey();
                        Geometry cov = entry.getValue();

                        if (cov != null) {
                            Integer c = featureCol.get(s);
                            if (c == null) {
                                c = getColor(s);
                                featureCol.put(s, c);
                            }
                            int color = c;

                            // XXX - stroke width
                            Feature feature = new Feature(
                                    1, s, cov, new BasicStrokeStyle(color,
                                            2 * GLRenderGlobals
                                                    .getRelativeScaling()),
                                    EMPTY_ATTR);

                            try {
                                this.insertFeature(feature);

                            } catch (DataStoreException dataStoreException) {
                                Log.e(TAG, "insertFeature failed",
                                        dataStoreException);

                            }
                        }
                    }

                    for (Map.Entry<String, Boolean> entry : featureVis
                            .entrySet()) {
                        params = new FeatureDataStore2.FeatureQueryParameters();
                        params.names = Collections.singleton(entry.getKey());

                        try {
                            this.setFeaturesVisible(params,
                                    entry.getValue());
                        } catch (DataStoreException dataStoreException) {
                            Log.e(TAG, "setFeaturesVisible failed",
                                    dataStoreException);

                        }
                    }
                } catch (InterruptedException ignored) {

                } finally {
                    this.releaseModifyLock();
                }
            }
        }

        private Integer getColor(String s) {
            if (FileSystemUtils.isEmpty(s))
                return 0;

            return COLOR_ARRAY[Math.abs(s.hashCode() % COLOR_ARRAY.length)];
        }
    }
}
