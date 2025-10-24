
package com.atakmap.android.munitions.util;

import gov.tak.api.util.AttributeSet;

/**
 * Used by {@link MunitionsHelper} for looping through weapon categories
 */
public interface CategoryConsumer2 {

    /**
     * Called for each category in the target's weapon metadata
     * @param categoryName Category
     * @param map Weapon mapping
     */
    void forCategory(String categoryName, AttributeSet map);
}
