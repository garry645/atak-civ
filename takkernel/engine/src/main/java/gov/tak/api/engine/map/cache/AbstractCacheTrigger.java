package gov.tak.api.engine.map.cache;

import java.util.concurrent.atomic.AtomicInteger;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.features.Geometry;

/**
 * Base definition of all trigger types of the caching service
 */
public abstract class AbstractCacheTrigger {

    /**
     * Create cache trigger providing all base values
     *
     * @param geometry      geometry defining the trigger region (required)
     * @param priority      the priority of the trigger from lowest to highest (optional)
     * @param minResolution minimum meters-per-pixel resolution of the trigger (optional)
     * @param maxResolution maximum meters-per-pixel resolution of the trigger (optional)
     * @param cacheSource   the associated cache source (source / sink are optional if both are null)
     * @param cacheSink     the associated cache sink (source / sink are optional if both are null)
     *
     * @throws IllegalArgumentException when required arguments are not provided
     */
    public AbstractCacheTrigger(@NonNull Geometry geometry, int priority,
                                double minResolution, double maxResolution, String cacheSource, String cacheSink) {

        if (geometry == null)
            throw new IllegalArgumentException("geometry required");

        trigId = generateUniqueTriggerId();
        geom = geometry.clone();
        priorityVal = priority;
        minRes = minResolution;
        maxRes = maxResolution;
        cacheSrc = cacheSource;
        this.cacheSink = cacheSink;
    }

    /**
     * The unique trigger id
     */
    public int getTriggerId() {
        return trigId;
    }

    /**
     * Required geometry of the operational area of the trigger
     */
    @NonNull
    public Geometry getGeometry() {
        return geom;
    }

    /**
     * Geometry of the operational area of the trigger
     */
    public int getPriority() {
        return priorityVal;
    }

    /**
     * Maximum resolution of the operational area of the trigger
     *
     * @return Double.NaN indicates non-specified
     */
    public double getMaxResolution() {
        return maxRes;
    }

    /**
     * Minimum resolution of the operational area of the trigger
     *
     * @return Double.NaN indicates non-specified
     */
    public double getMinResolution() {
        return minRes;
    }

    /**
     * Cache source associated with the trigger
     */
    public String getCacheSource() {
        return cacheSrc;
    }
    /**
     * Cache sink associated with the trigger
     */
    public String getCacheSink() {
        return cacheSink;
    }

    private static int generateUniqueTriggerId() {
        return nextTriggerId.addAndGet(1);
    }

    private static AtomicInteger nextTriggerId = new AtomicInteger(0);

    private int trigId;
    private Geometry geom;
    private int priorityVal;
    private double minRes;
    private double maxRes;
    private String cacheSrc;
    private String cacheSink;
}
