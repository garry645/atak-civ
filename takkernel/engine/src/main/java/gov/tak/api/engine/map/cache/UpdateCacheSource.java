package gov.tak.api.engine.map.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import gov.tak.api.annotation.NonNull;

/**
 * Provides the cache service with an updated cache source to focus on
 */
public final class UpdateCacheSource {

    /**
     * Create an update
     *
     * @param cacheSources the source descriptor(s) (required)
     * @param defaultSinks the default sink, per each cache source (required)
     *
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public UpdateCacheSource(@NonNull String[] cacheSources, @NonNull String[] defaultSinks) {

        if (cacheSources == null || cacheSources.length == 0)
            throw new IllegalArgumentException("cache sources is required");
        if (defaultSinks == null)
            throw new IllegalArgumentException("default sink is required");
        if (cacheSources.length != defaultSinks.length)
            throw new IllegalArgumentException("mismatched source/sink");

        Map<String, String> srcs = new HashMap<>();
        for(int i = 0; i < cacheSources.length; i++)
            srcs.put(cacheSources[i], defaultSinks[i]);
        cacheSrcs = Collections.unmodifiableMap(srcs);

        if (cacheSrcs.containsKey(null))
            throw new IllegalArgumentException("cache sources invalid");
        if (cacheSrcs.containsValue(null))
            throw new IllegalArgumentException("default sinks sources invalid");
    }

    /**
     * The default sink of the cache source
     */
    @NonNull
    public String getDefaultSink(String source) {
        return cacheSrcs.get(source);
    }

    /**
     * The source descriptor(s)
     */
    public Collection<String> getCacheSources() {
        return cacheSrcs.keySet();
    }

    private Map<String, String> cacheSrcs;
}
