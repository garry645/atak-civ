
package com.atakmap.android.gpkg;

import com.atakmap.android.data.FileContentResolver;
import com.atakmap.android.maps.MapView;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.gpkg.GeoPackageFeatureDataStore;
import com.atakmap.map.layer.feature.gpkg.GeoPackageFeatureDataStore2;

import java.util.Collections;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Content resolver for geo package databases
 */
public class GeoPackageContentResolver extends FileContentResolver {

    private final MapView _mapView;

    GeoPackageContentResolver(MapView mapView) {
        super(Collections.singleton("gpkg"));
        _mapView = mapView;
    }

    /**
     * Add a geo package handler
     *
     * @param ds Geo package data store
     * @deprecated use {@link #addHandler(GeoPackageFeatureDataStore2)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public void addHandler(GeoPackageFeatureDataStore ds) {
        GeoPackage pkg;
        if (ds == null || (pkg = ds.getGeoPackage()) == null)
            return;

        // Build bounds using content rows (much faster than feature queries)
        // XXX - SQLiteException when attempting to queryFeatures...
        Envelope.Builder bounds = new Envelope.Builder();
        List<GeoPackage.ContentsRow> contents = pkg.getPackageContents();
        for (GeoPackage.ContentsRow row : contents) {
            if (row.min_x != null && row.min_y != null && row.max_x != null
                    && row.max_y != null) {
                bounds.add(row.min_x, row.min_y);
                bounds.add(row.max_x, row.max_y);
            }
        }

        // Add the handler
        addHandler(new GeoPackageContentHandler(_mapView, pkg.getFile(),
                Adapters.adapt(ds), bounds.build()));
    }

    public void addHandler(GeoPackageFeatureDataStore2 ds) {
        GeoPackage pkg;
        if (ds == null || (pkg = ds.getGeoPackage()) == null)
            return;

        // Build bounds using content rows (much faster than feature queries)
        // XXX - SQLiteException when attempting to queryFeatures...
        Envelope.Builder bounds = new Envelope.Builder();
        List<GeoPackage.ContentsRow> contents = pkg.getPackageContents();
        for (GeoPackage.ContentsRow row : contents) {
            if (row.min_x != null && row.min_y != null && row.max_x != null
                    && row.max_y != null) {
                bounds.add(row.min_x, row.min_y);
                bounds.add(row.max_x, row.max_y);
            }
        }

        // Add the handler
        addHandler(new GeoPackageContentHandler(_mapView, pkg.getFile(),
                ds, bounds.build()));
    }
}
