package com.atakmap.map.layer.feature.gpkg;

import java.util.Set;

import com.atakmap.database.DatabaseIface;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.FeatureDataStore.FeatureQueryParameters;
import com.atakmap.map.layer.feature.geometry.Geometry;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use {@link com.atakmap.map.layer.feature.gpkg.DefaultGeoPackageSchemaHandler3} */
@Deprecated
@DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public class DefaultGeoPackageSchemaHandler2 implements GeoPackageSchemaHandler
{
    private final DefaultGeoPackageSchemaHandler3 _impl;

    public DefaultGeoPackageSchemaHandler2(GeoPackage gpkg)
    {
        _impl = new DefaultGeoPackageSchemaHandler3(gpkg);
    }

    @Override
    public void addOnFeatureDefinitionsChangedListener(OnFeatureDefinitionsChangedListener l)
    {
    }

    @Override
    public Class<? extends Geometry> getGeometryType(String layerName)
    {
        return _impl.getGeometryType(layerName);
    }

    @Override
    public boolean getDefaultFeatureSetVisibility(String layer, String featureSet)
    {
        return _impl.getDefaultFeatureSetVisibility(layer, featureSet);
    }

    @Override
    public Set<String> getLayerFeatureSets(String remote)
    {
        return _impl.getLayerFeatureSets(remote);
    }

    @Override
    public double getMaxResolution(String layer)
    {
        return _impl.getMaxResolution(layer);
    }

    @Override
    public double getMinResolution(String name)
    {
        return _impl.getMinResolution(name);
    }

    @Override
    public String getSchemaType()
    {
        return _impl.getSchemaType();
    }

    @Override
    public long getSchemaVersion()
    {
        return _impl.getSchemaVersion();
    }

    @Override
    public boolean ignoreFeature(String layer, AttributeSet metadata)
    {
        return _impl.ignoreFeature(layer, metadata);
    }

    @Override
    public boolean ignoreLayer(String layer)
    {
        return _impl.ignoreLayer(layer);
    }

    @Override
    public boolean isFeatureVisible(String layer, AttributeSet metadata)
    {
        return _impl.isFeatureVisible(layer, metadata);
    }

    @Override
    public boolean isLayerVisible(String layer)
    {
        return _impl.isLayerVisible(layer);
    }

    @Override
    public GeoPackageFeatureCursor queryFeatures(String layerName, FeatureQueryParameters params)
    {
        return _impl.queryFeatures(layerName, Adapters.adapt(params, null));
    }

    @Override
    public int queryFeaturesCount(String layerName, FeatureQueryParameters params)
    {
        return _impl.queryFeaturesCount(layerName, Adapters.adapt(params, null));
    }

    @Override
    public void removeOnFeatureDefinitionsChangedListener(OnFeatureDefinitionsChangedListener l)
    {
    }

    /**************************************************************************/

    public static double estimateDisplayThreshold(DatabaseIface database, String tableName, String geometryColumn)
    {
        return DefaultGeoPackageSchemaHandler3.estimateDisplayThreshold(database, tableName, geometryColumn);
    }

    public static String guessFeatureNameColumn(DatabaseIface database, String table)
    {
        return DefaultGeoPackageSchemaHandler3.guessFeatureNameColumn(database, table);
    }

    public static int queryFeaturesCount(GeoPackage gpkg, String tableName, String nameColumn, String geometryColumn, int geometrySrid, String rtreeTableName, FeatureQueryParameters params)
    {
        return DefaultGeoPackageSchemaHandler3.queryFeaturesCount(gpkg, tableName, nameColumn, geometryColumn, geometrySrid, rtreeTableName, Adapters.adapt(params, null));
    }
}
