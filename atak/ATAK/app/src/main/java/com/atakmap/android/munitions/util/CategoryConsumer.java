
package com.atakmap.android.munitions.util;

import java.util.Map;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Used by {@link MunitionsHelper} for looping through weapon categories
 */
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public interface CategoryConsumer {

    /**
     * Called for each category in the target's weapon metadata
     * @param categoryName Category
     * @param map Weapon mapping
     */
    void forCategory(String categoryName, Map<String, Object> map);
}
