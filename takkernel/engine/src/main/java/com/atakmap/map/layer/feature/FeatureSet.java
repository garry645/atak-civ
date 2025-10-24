package com.atakmap.map.layer.feature;

import gov.tak.api.annotation.DontObfuscate;

/**
 * A grouping of map features.
 *
 * @author Developer
 */
@DontObfuscate
public final class FeatureSet
{
    long id;
    long version;
    private final String provider;
    private final String type;
    private final String name;
    private final double minGsd;
    private final double maxGsd;

    public FeatureSet(String provider, String type, String name, double minGsd, double maxGsd)
    {
        this(FeatureDataStore.FEATURESET_ID_NONE, provider, type, name, minGsd, maxGsd, FeatureDataStore.FEATURESET_VERSION_NONE);
    }

    public FeatureSet(long id, String provider, String type, String name, double minGsd, double maxGsd, long version)
    {
        this.id = id;
        this.provider = provider;
        this.type = type;
        this.name = name;
        this.minGsd = minGsd;
        this.maxGsd = maxGsd;
        this.version = version;
    }

    public String getProvider()
    {
        return this.provider;
    }

    public String getType()
    {
        return this.type;
    }

    /**
     * Returns the name of the feature set.
     *
     * @return The name of the feature set.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Returns the ID of the feature set.
     *
     * @return The ID of the feature set.
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * Returns the minimum display resolution of the feature set, in meters per
     * pixel. Larger values equate to lower resolutions.
     *
     * @return The minimum display resolution of the feature set.
     */
    public double getMinResolution()
    {
        return this.minGsd;
    }

    /**
     * Returns the maximum display resolution of the feature set, in meters per
     * pixel. Smaller values equate to higher resolutions.
     *
     * @return The maximum display resolution of the feature set.
     */
    public double getMaxResolution()
    {
        return this.maxGsd;
    }

    public long getVersion()
    {
        return this.version;
    }
}
