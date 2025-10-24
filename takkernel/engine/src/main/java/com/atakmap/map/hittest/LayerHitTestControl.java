package com.atakmap.map.hittest;

import com.atakmap.map.MapRenderer3;

import java.util.Collection;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Extension of {@link HitTestControl} that provides a default implementation
 * of the hit-testing loop provided the implementation also provides a list of
 * objects that need to be hit-tested.
 */
public interface LayerHitTestControl extends HitTestControl
{

    /**
     * Defines a pattern for providing a default implementation for an interface method.
     */
    final class Default {
         public static void hitTest(LayerHitTestControl lhtc, MapRenderer3 renderer, HitTestQueryParameters params, Collection<HitTestResult> results)
         {

            // Pull the list of items that should be hit-tested
            Collection<?> objects = lhtc.getHitTestList();
        
            // Hit-test items
            for (Object r : objects)
            {
                if (params.hitLimit(results))
                    break;
                if (r instanceof HitTestable)
                {
                    // Hit-test a single item
                    HitTestable t = (HitTestable) r;
                    HitTestResult result = t.hitTest(renderer, params);
                    if (result != null)
                        results.add(result);
                } else if (r instanceof HitTestControl)
                {
                    // Hit-test that may return multiple items
                    ((HitTestControl) r).hitTest(renderer, params, results);
                }
            }
        }
    }

    /**
     * Get the list of objects that should be hit tested
     * Note: This list MUST be thread-safe
     *
     * @return List of objects to hit test
     */
    Collection<?> getHitTestList();


    /**
     * Removal of the default implementation, all implementations will be required to provide an
     * implementation based on the super interface definition.   The default implementation is
     * defined {@link Default#hitTest(LayerHitTestControl, MapRenderer3, HitTestQueryParameters, Collection)}
     * @param renderer GL instance of the map view
     * @param params   Query parameters
     * @param results  Results where hit renderables are stored
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    @Override
    default void hitTest(MapRenderer3 renderer, HitTestQueryParameters params, Collection<HitTestResult> results)
    {
        Default.hitTest(this, renderer, params, results);
    }
}
