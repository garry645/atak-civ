package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.DontObfuscate;

/**
 * {@link com.atakmap.map.layer.Layer Layer} subinterface for feature (point and
 * vector) data.
 *
 * <H2>Associated Extensions</H2>
 *
 * <H2>Associated Controls</H2>
 *
 * <UL>
 * <LI>{@link com.atakmap.map.hittest.HitTestControl HitTestControl} - Provides hit-testing mechanism</LI>
 * <UL>
 *
 * @author Developer
 */
public class FeatureLayer3 extends AbstractLayer
{

    /**************************************************************************/

    private final FeatureDataStore2 dataStore;
    private final Options options;

    /**
     * @deprecated Use {@link #FeatureLayer3(String, FeatureDataStore4)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public FeatureLayer3(String name, FeatureDataStore2 dataStore)
    {
        this(name, dataStore, null);
    }

    /**
     * @deprecated Use {@link #FeatureLayer3(String, FeatureDataStore4, FeatureQueryParameters)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public FeatureLayer3(String name, FeatureDataStore2 dataStore, FeatureQueryParameters filter)
    {
        super(name);

        this.dataStore = dataStore;
        this.options = new Options();
        options.filter = filter;
    }

    public FeatureLayer3(String name, FeatureDataStore4 dataStore)
    {
        this(name, dataStore, (Options) null);
    }

    /**
     * @deprecated Use {@link #FeatureLayer3(String, FeatureDataStore4, Options)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public FeatureLayer3(String name, FeatureDataStore4 dataStore, FeatureQueryParameters filter)
    {
        this(name, dataStore, new Options());
        options.filter = filter;
    }

    public FeatureLayer3(String name, FeatureDataStore4 dataStore, Options options)
    {
        super(name);

        this.dataStore = dataStore;
        this.options = options != null ? options : new Options();
    }

    /**
     * Returns the {@link FeatureDataStore2} that contains this layer's content.
     *
     * @return The {@link FeatureDataStore2} that contains this layer's content.
     */
    public FeatureDataStore2 getDataStore()
    {
        return this.dataStore;
    }

    /**
     * @deprecated Use {@link #getOptions()}.{@link Options#filter filter}
     */
    public FeatureQueryParameters getFilter() { return this.options.filter; }

    public Options getOptions()
    {
        return options;
    }

    @DontObfuscate
    public static class Options
    {
        public FeatureQueryParameters filter;
        public boolean labelsEnabled = true;
    }
} // FeatureLayer3
