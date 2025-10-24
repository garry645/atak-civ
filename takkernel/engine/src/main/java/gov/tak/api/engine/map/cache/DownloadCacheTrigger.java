package gov.tak.api.engine.map.cache;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.features.Geometry;

/**
 * Cache trigger for invoking a download of an area of interest from a given source
 * to a given sink
 */
public final class DownloadCacheTrigger extends AbstractCacheTrigger {
    /**
     * Create cache trigger providing all values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param priority      the priority of the trigger from lowest to highest (optional)
     * @param minResolution minimum meters-per-pixel resolution of the trigger (optional)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (required)
     * @param cacheSource   the associated cache source (required)
     * @param downloadSink  the download sink path (required)
     *
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public DownloadCacheTrigger(@NonNull Geometry geometry, int priority,
                                double minResolution, double maxResolution,
                                @NonNull String cacheSource, @NonNull String downloadSink) {
        super(geometry, priority, minResolution, maxResolution, cacheSource, downloadSink);

        if (Double.isNaN(maxResolution))
            throw new IllegalArgumentException("max resolution required");
        if (cacheSource == null)
            throw new IllegalArgumentException("cache source required");
        if (downloadSink == null)
            throw new IllegalArgumentException("download sink required");
    }

    /**
     * Create cache trigger providing all required values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (required)
     * @param cacheSource   the associated cache source (required)
     * @param downloadSink  the download sink path (required)
     *
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public DownloadCacheTrigger(@NonNull Geometry geometry, double maxResolution,
                                @NonNull String cacheSource, @NonNull String downloadSink) {
        this(geometry, CacheUtils.DEFAULT_PRIORITY, Double.NaN, maxResolution,
                cacheSource, downloadSink);
    }
}
