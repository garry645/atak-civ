package gov.tak.api.engine.map.cache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Constants and other utility methods related to the caching service
 */
public final class CacheUtils {
    public static final int LOW_PRIORITY = Integer.MIN_VALUE / 2;
    public static final int LOWEST_PRIORITY = Integer.MIN_VALUE;
    public static final int MED_PRIORITY = 0;
    public static final int HIGH_PRIORITY = Integer.MAX_VALUE / 2;
    public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;
    public static final int DEFAULT_PRIORITY = MED_PRIORITY;


}
