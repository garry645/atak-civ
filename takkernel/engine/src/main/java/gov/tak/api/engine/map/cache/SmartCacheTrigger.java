package gov.tak.api.engine.map.cache;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.features.Geometry;

/**
 * Cache trigger for acting as a hint to the smart-caching lane of an area of interest
 * for a given cache source or generally
 */
public final class SmartCacheTrigger extends AbstractCacheTrigger {
    /**
     * Create cache trigger providing all values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param priority      the priority of the trigger from lowest to highest (optional)
     * @param minResolution minimum meters-per-pixel resolution of the trigger (optional)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (optional)
     * @param cacheSource   the associated cache source (source / sink are optional if both are null)
     * @param cacheSink     the associated cache sink (source / sink are optional if both are null)
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public SmartCacheTrigger(@NonNull Geometry geometry, int priority, double minResolution,
                             double maxResolution, String cacheSource, String cacheSink) {
        super(geometry, priority, minResolution, maxResolution, cacheSource, cacheSink);
    }
    /**
     * Create cache trigger providing all values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param priority      the priority of the trigger from lowest to highest (optional)
     * @param minResolution minimum meters-per-pixel resolution of the trigger (optional)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (optional)
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public SmartCacheTrigger(@NonNull Geometry geometry, int priority, double minResolution,
                             double maxResolution) {
        super(geometry, priority, minResolution, maxResolution, null, null);
    }
    /**
     * Create cache trigger providing all values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param priority      the priority of the trigger from lowest to highest (optional)
     * @param minResolution minimum meters-per-pixel resolution of the trigger (optional)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (optional)
     * @param cacheSource   the associated cache source (sink will be defaulted)
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public SmartCacheTrigger(@NonNull Geometry geometry, int priority, double minResolution,
                             double maxResolution, String cacheSource) {
        super(geometry, priority, minResolution, maxResolution, cacheSource, null);
    }

}
