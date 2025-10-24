package com.atakmap.map.layer.feature;

import gov.tak.api.annotation.DontObfuscate;

/**
 * The definition of a feature. Feature properties may be recorded as raw,
 * unprocessed data of several well-defined types. Utilization of
 * unprocessed data may yield a significant performance advantage depending
 * on the intended storage.
 */
@DontObfuscate
public interface FeatureDefinition4 extends FeatureDefinition3
{
    /**
     * Returns the Traits associated with the feature.
     *
     * @return the traits
     */
    Feature.Traits getTraits();
} // FeatureDefinition4
