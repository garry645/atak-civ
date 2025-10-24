package com.atakmap.map.layer.feature.gpkg;

import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Geometry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class SchemaHandlerAdapter
{
    final static class Spi implements GeoPackageSchemaHandler2.Spi
    {
        final GeoPackageSchemaHandler.Factory _impl;

        Spi(GeoPackageSchemaHandler.Factory impl) {
            _impl = impl;
        }

        @Override
        public GeoPackageSchemaHandler2 create(GeoPackage object, Callback callback) {
            final GeoPackageSchemaHandler handler = _impl.create(object, callback);
            return (handler != null) ? new Forward(handler) : null;
        }

        @Override
        public GeoPackageSchemaHandler2 create(GeoPackage object) {
            final GeoPackageSchemaHandler handler = _impl.create(object);
            return (handler != null) ? new Forward(handler) : null;
        }

        @Override
        public int getPriority() {
            return _impl.getPriority();
        }
    }

    final static class Forward implements GeoPackageSchemaHandler2
    {
        final GeoPackageSchemaHandler _impl;
        final Set<OnFeatureDefinitionsChangedListener> _listeners;

        Forward(GeoPackageSchemaHandler impl) {
            _impl = impl;
            _listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

            _impl.addOnFeatureDefinitionsChangedListener(new GeoPackageSchemaHandler.OnFeatureDefinitionsChangedListener() {
                @Override
                public void onFeatureDefinitionsChanged(GeoPackageSchemaHandler handler) {
                    for (OnFeatureDefinitionsChangedListener l : _listeners)
                        l.onFeatureDefinitionsChanged(Forward.this);
                }
            });
        }

        @Override
        public void addOnFeatureDefinitionsChangedListener(GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener l) {
            _listeners.add(l);
        }

        @Override
        public boolean getDefaultFeatureSetVisibility(String layer, String featureSet) {
            return _impl.getDefaultFeatureSetVisibility(layer, featureSet);
        }

        @Override
        public Class<? extends Geometry> getGeometryType(String layer) {
            return _impl.getGeometryType(layer);
        }

        @Override
        public Set<String> getLayerFeatureSets(String layerName) {
            return _impl.getLayerFeatureSets(layerName);
        }

        @Override
        public double getMaxResolution(String layer) {
            return _impl.getMaxResolution(layer);
        }

        @Override
        public double getMinResolution(String layer) {
            return _impl.getMinResolution(layer);
        }

        @Override
        public String getSchemaType() {
            return _impl.getSchemaType();
        }

        @Override
        public long getSchemaVersion() {
            return _impl.getSchemaVersion();
        }

        @Override
        public boolean ignoreFeature(String layer, AttributeSet metadata) {
            return _impl.ignoreFeature(layer, metadata);
        }

        @Override
        public boolean ignoreLayer(String layer) {
            return _impl.ignoreLayer(layer);
        }

        @Override
        public boolean isFeatureVisible(String layer, AttributeSet metadata) {
            return _impl.isFeatureVisible(layer, metadata);
        }

        @Override
        public boolean isLayerVisible(String layer) {
            return _impl.isLayerVisible(layer);
        }

        @Override
        public GeoPackageFeatureCursor queryFeatures(String layer, FeatureDataStore2.FeatureQueryParameters params) {
            return _impl.queryFeatures(layer, Adapters.adapt(params, null));
        }

        @Override
        public int queryFeaturesCount(String layer, FeatureDataStore2.FeatureQueryParameters params) {
            return _impl.queryFeaturesCount(layer, Adapters.adapt(params, null));
        }

        @Override
        public void removeOnFeatureDefinitionsChangedListener(GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener l) {
            _listeners.remove(l);
        }
    }

    final static class Backward implements GeoPackageSchemaHandler
    {
        final GeoPackageSchemaHandler2 _impl;
        final Set<OnFeatureDefinitionsChangedListener> _listeners;

        Backward(GeoPackageSchemaHandler2 impl) {
            _impl = impl;
            _listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

            _impl.addOnFeatureDefinitionsChangedListener(new GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener() {
                @Override
                public void onFeatureDefinitionsChanged(GeoPackageSchemaHandler2 handler) {
                    for (OnFeatureDefinitionsChangedListener l : _listeners)
                        l.onFeatureDefinitionsChanged(Backward.this);
                }
            });
        }

        @Override
        public void addOnFeatureDefinitionsChangedListener(OnFeatureDefinitionsChangedListener l) {
            _listeners.add(l);
        }

        @Override
        public boolean getDefaultFeatureSetVisibility(String layer, String featureSet) {
            return _impl.getDefaultFeatureSetVisibility(layer, featureSet);
        }

        @Override
        public Class<? extends Geometry> getGeometryType(String layer) {
            return _impl.getGeometryType(layer);
        }

        @Override
        public Set<String> getLayerFeatureSets(String layerName) {
            return _impl.getLayerFeatureSets(layerName);
        }

        @Override
        public double getMaxResolution(String layer) {
            return _impl.getMaxResolution(layer);
        }

        @Override
        public double getMinResolution(String layer) {
            return _impl.getMinResolution(layer);
        }

        @Override
        public String getSchemaType() {
            return _impl.getSchemaType();
        }

        @Override
        public long getSchemaVersion() {
            return _impl.getSchemaVersion();
        }

        @Override
        public boolean ignoreFeature(String layer, AttributeSet metadata) {
            return _impl.ignoreFeature(layer, metadata);
        }

        @Override
        public boolean ignoreLayer(String layer) {
            return _impl.ignoreLayer(layer);
        }

        @Override
        public boolean isFeatureVisible(String layer, AttributeSet metadata) {
            return _impl.isFeatureVisible(layer, metadata);
        }

        @Override
        public boolean isLayerVisible(String layer) {
            return _impl.isLayerVisible(layer);
        }

        @Override
        public GeoPackageFeatureCursor queryFeatures(String layer, FeatureDataStore.FeatureQueryParameters params) {
            return _impl.queryFeatures(layer, Adapters.adapt(params, null));
        }

        @Override
        public int queryFeaturesCount(String layer, FeatureDataStore.FeatureQueryParameters params) {
            return _impl.queryFeaturesCount(layer, Adapters.adapt(params, null));
        }

        @Override
        public void removeOnFeatureDefinitionsChangedListener(GeoPackageSchemaHandler.OnFeatureDefinitionsChangedListener l) {
            _listeners.remove(l);
        }
    }
}