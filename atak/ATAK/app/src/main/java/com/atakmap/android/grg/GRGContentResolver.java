
package com.atakmap.android.grg;

import android.graphics.Color;

import com.atakmap.android.data.FileContentHandler;
import com.atakmap.android.layers.AbstractLayerContentResolver;
import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDefinition2;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.LocalRasterDataStore;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.ModifierApi;

/**
 * Used to map GRG files to associated metadata (i.e. geo bounds, visibility)
 */
@ModifierApi(since = "5.3", modifiers = {
        "implements FeatureDataStore.OnDataStoreContentChangedListener"
}, target = "5.6")
public class GRGContentResolver extends AbstractLayerContentResolver implements
        FeatureDataStore.OnDataStoreContentChangedListener,
        FeatureDataStore2.OnDataStoreContentChangedListener {

    private final DatasetRasterLayer2 _rasterLayer;
    private final FeatureDataStore2 _outlinesDB;

    public GRGContentResolver(MapView mv, LocalRasterDataStore rasterDB,
            DatasetRasterLayer2 rasterLayer, FeatureDataStore2 outlinesDB) {
        super(mv, rasterDB);
        _rasterLayer = rasterLayer;
        _outlinesDB = outlinesDB;
        _outlinesDB.addOnDataStoreContentChangedListener(this);
    }

    @Override
    public void dispose() {
        _outlinesDB.removeOnDataStoreContentChangedListener(this);
        super.dispose();
    }

    @Override
    protected FileContentHandler createHandler(File f, DatasetDescriptor d) {
        return new GRGContentHandler(_mapView, f, d, _rasterLayer);
    }

    @Override
    protected void refresh() {
        super.refresh();
        updateMetadata();
    }

    /** @deprecated {@code FeatureDataStore.OnDataStoreContentChangedListener} implementation will be removed */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    @Override
    public void onDataStoreContentChanged(FeatureDataStore ds) {
        updateMetadata();
    }

    private void updateMetadata() {
        // Map file name to handler for quick lookup in the query
        List<FileContentHandler> handlers = getHandlers();
        Map<String, GRGContentHandler> nameToHandler = new HashMap<>();
        for (FileContentHandler h : handlers) {
            String name = h.getFile().getName();
            nameToHandler.put(name, (GRGContentHandler) h);
        }

        // Get the item's map UID and color
        // Requires finding the associated outline feature
        try (FeatureCursor fc = _outlinesDB.queryFeatures(null)) {
            while (fc != null && fc.moveToNext()) {
                Feature fe = fc.get();
                if (fe == null)
                    continue;

                // Name corresponds to GRG file name
                // Not perfect, but it's the best we got
                String name = fe.getName();
                GRGContentHandler h = nameToHandler.get(name);
                if (h == null)
                    continue;

                String uid = "spatialdb::" + _outlinesDB.getUri() + "::"
                        + fe.getId();
                Style s = fe.getStyle();
                int color = Color.WHITE;
                if (s instanceof BasicStrokeStyle)
                    color = ((BasicStrokeStyle) s).getColor();

                h.setUID(uid);
                h.setColor(color);
            }
        } catch (DataStoreException ignored) {
        }
    }

    @Override
    public void onDataStoreContentChanged(FeatureDataStore2 dataStore) {
        updateMetadata();
    }

    @Override
    public void onFeatureInserted(FeatureDataStore2 dataStore, long fid,
            FeatureDefinition2 def, long version) {
        updateMetadata();
    }

    @Override
    public void onFeatureUpdated(FeatureDataStore2 dataStore, long fid,
            int modificationMask, String name, Geometry geom, Style style,
            AttributeSet attribs, int attribsUpdateType) {
        updateMetadata();
    }

    @Override
    public void onFeatureDeleted(FeatureDataStore2 dataStore, long fid) {
        updateMetadata();
    }

    @Override
    public void onFeatureVisibilityChanged(FeatureDataStore2 dataStore,
            long fid, boolean visible) {
        updateMetadata();
    }
}
