package com.atakmap.map.layer.feature.gpkg;

import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.spi.InteractiveServiceProvider;

import java.util.Set;

public interface GeoPackageSchemaHandler2
{
    interface Spi extends InteractiveServiceProvider<GeoPackageSchemaHandler2, GeoPackage>
    {
        int getPriority();
    }

    /**
     * Callback interface providing notification when the definition of features
     * (e.g., styling, geometry, attributes) has changed.
     */
    interface OnFeatureDefinitionsChangedListener
    {
        /**
         * This method is invoked when feature definitions have changed.
         *
         * @param handler The schema handler
         */
        void onFeatureDefinitionsChanged(GeoPackageSchemaHandler2 handler);
    }

    /**
     * Adds the specified {@link GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener}.
     *
     * @param l The listener
     */
    void addOnFeatureDefinitionsChangedListener(GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener l);

    boolean getDefaultFeatureSetVisibility(String layer, String featureSet);

    Class<? extends Geometry> getGeometryType(String layer);

    /**
     * Returns the feature sets to be associated with the specified feature
     * layer.
     *
     * @param layerName The layer name
     * @return The names of the feature sets to be associated with the
     * specified feature layer.
     */
    Set<String> getLayerFeatureSets(String layerName);

    /**
     * Returns the maximum display resolution of the layer, in meters per pixel.
     * Smaller values equate to higher resolutions.
     *
     * @return The maximum display resolution of the layer (or NaN for no
     * maximum).
     */
    double getMaxResolution(String layer);

    /**
     * Returns the minimum display resolution of the layer, in meters per pixel.
     * Larger values equate to lower resolutions.
     *
     * @return The minimum display resolution of the layer (or NaN for no
     * minimum).
     */
    double getMinResolution(String layer);

    /**
     * @return The type of the Extended GeoPackage.
     **/
    String getSchemaType();

    long getSchemaVersion();

    boolean ignoreFeature(String layer, AttributeSet metadata);

    boolean ignoreLayer(String layer);

    boolean isFeatureVisible(String layer, AttributeSet metadata);

    boolean isLayerVisible(String layer);

    /**
     * @param layer  The layer to be queried for features.
     * @param params The constraints on the features to be selected from the
     *               supplied layer (or null for all features).
     * @return A cursor for the features in the supplied layer that
     * satisfy the supplied FeatureQueryParameters.
     **/
    GeoPackageFeatureCursor queryFeatures(String layer, FeatureDataStore2.FeatureQueryParameters params);

    /**
     * @param layer  The layer to be queried for feature count.
     * @param params The constraints on the features to be selected from the
     *               supplied layer (or null for all features).
     * @return The number of features in the supplied layer that
     * satisfy the supplied FeatureQueryParameters.
     **/
    int queryFeaturesCount(String layer, FeatureDataStore2.FeatureQueryParameters params);

    /**
     * Removes the specified {@link GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener}.
     *
     * @param l The listener
     */
    void removeOnFeatureDefinitionsChangedListener(GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener l);
}
