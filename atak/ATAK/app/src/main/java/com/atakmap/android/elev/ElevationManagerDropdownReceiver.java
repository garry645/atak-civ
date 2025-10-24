
package com.atakmap.android.elev;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.contentservices.cdn.TiledGeospatialContentMarshal;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.layers.LayerSelection;
import com.atakmap.android.layers.LayerSelectionAdapter;
import com.atakmap.android.layers.LayersManager;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDefinition2;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.OutlinesFeatureDataStore2;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;

import java.util.Collections;

import gov.tak.platform.graphics.Color;

final class ElevationManagerDropdownReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {
    final static String ACTION_SHOW_MANAGER = "com.atakmap.android.elev.SHOW_ELEVATION_MANAGER";

    LayersManager _manager;
    MobileImageryRasterLayer2 _elevationLayer;
    Layer _dtedOutlinesOverlay;

    ElevationManagerDropdownReceiver(MapView mapView,
            MobileImageryRasterLayer2 elevationLayer,
            Layer dtedOutlinesOverlay) {
        super(mapView);

        _elevationLayer = elevationLayer;
        _dtedOutlinesOverlay = dtedOutlinesOverlay;
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_SHOW_MANAGER:
                if (_manager == null) {
                    OutlinesFeatureDataStore2 outlines = new OutlinesFeatureDataStore2(
                            _elevationLayer, Color.GREEN, false);
                    // sync outlines visibility state
                    {
                        FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
                        params.names = Collections.singleton("DTED");
                        try {
                            outlines.setFeaturesVisible(params,
                                    _dtedOutlinesOverlay.isVisible());
                        } catch (DataStoreException ignored) {
                        }
                    }
                    outlines.addOnDataStoreContentChangedListener(
                            new FeatureDataStore2.OnDataStoreContentChangedListener() {
                                @Override
                                public void onDataStoreContentChanged(
                                        FeatureDataStore2 dataStore) {
                                    FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
                                    params.names = Collections
                                            .singleton("DTED");
                                    params.limit = 1;
                                    params.visibleOnly = true;
                                    try {
                                        final boolean isVisible = dataStore
                                                .queryFeaturesCount(
                                                        params) != 0;
                                        if (isVisible != _dtedOutlinesOverlay
                                                .isVisible()) {
                                            _dtedOutlinesOverlay
                                                    .setVisible(isVisible);
                                            AtakPreferences
                                                    .getInstance(getMapView()
                                                            .getContext())
                                                    .set(ElevationMapComponent.DTED_OUTLINE_VISIBLE_PREF_KEY,
                                                            isVisible);
                                        }
                                    } catch (DataStoreException ignored) {
                                    }
                                }

                                @Override
                                public void onFeatureInserted(
                                        FeatureDataStore2 dataStore, long fid,
                                        FeatureDefinition2 def, long version) {
                                }

                                @Override
                                public void onFeatureUpdated(
                                        FeatureDataStore2 dataStore, long fid,
                                        int modificationMask, String name,
                                        Geometry geom, Style style,
                                        AttributeSet attribs,
                                        int attribsUpdateType) {
                                }

                                @Override
                                public void onFeatureDeleted(
                                        FeatureDataStore2 dataStore, long fid) {
                                }

                                @Override
                                public void onFeatureVisibilityChanged(
                                        FeatureDataStore2 dataStore, long fid,
                                        boolean visible) {
                                }
                            });

                    FileSystemUtils.ensureDataDirectory("elevation", false);

                    _manager = new LayersManager.Builder(getMapView(),
                            _elevationLayer)
                                    .setOutlinesDataStore(outlines)
                                    .setPreferenceKeys("elevation.selected",
                                            "elevation.offlineOnly")
                                    .setShowVisibilityToggle(true)
                                    .setZoomOnClick(false)
                                    .setDownloadPromptForOverlay(false)
                                    .setShowAddOnlineSourceButton(false)
                                    .setShowOfflinePreferenceKey(
                                            "elevation.show_all_offline")
                                    .setDefaultDownloadDirectory(FileSystemUtils
                                            .getItem("elevation"))
                                    .setDownloadImportParams(
                                            TiledGeospatialContentMarshal.DEFAULT_MIME_TYPE_TERRAIN,
                                            TiledGeospatialContentMarshal.INSTANCE_TERRAIN
                                                    .getContentType())
                                    .build();
                    _manager.getAdapter().addSelectionAlias("Surface Models",
                            new LayerSelectionAdapter.SelectionAliasFilter() {
                                @Override
                                public boolean matches(
                                        LayerSelection selection) {
                                    for (DatasetDescriptor desc : selection
                                            .getDescriptors()) {
                                        ElevationSource elsrc = desc
                                                .getLocalData("elevationSource",
                                                        ElevationSource.class);
                                        if (elsrc == null)
                                            continue;
                                        if (!(elsrc instanceof Controls))
                                            continue;
                                        MosaicDatabase2 mosaicdb = ((Controls) elsrc)
                                                .getControl(
                                                        MosaicDatabase2.class);
                                        if (mosaicdb == null)
                                            continue;
                                        if (mosaicdb.getType().equals("dsm"))
                                            return true;
                                    }
                                    return false;
                                }
                            });
                }

                onDropDownVisible(true);

                if (isTablet()) {
                    showDropDown(_manager.getView(), FIVE_TWELFTHS_WIDTH,
                            FULL_HEIGHT,
                            FULL_WIDTH,
                            FIVE_TWELFTHS_HEIGHT, this);
                } else {
                    showDropDown(_manager.getView(), HALF_WIDTH, FULL_HEIGHT,
                            FULL_WIDTH,
                            HALF_HEIGHT, this);
                }
                break;
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
        _manager.getAdapter().setVisible(v);
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
        _manager.setExpandedDownloadState(false);
        _manager.getAdapter().reset();
        _manager.cancelSelectDownloadRegion();
        setRetain(false);
    }
}
