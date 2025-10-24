
package com.atakmap.android.maps;

/**
 * A Shape that has the ability to toggle the center point
 */
public interface CenterPointControl {

    /**
     * Returns the current state of the center point marker visibility
     * @return true if it is visible when the shape is visible
     */
    boolean isCenterPointVisible();

    /**
     * Allow for toggling the visibility of the center point marker.
     * Please note that this does not toggle the center marker if the center marker is non-default
     * @param visible true if the center point should be drawn when the shape is visible
     */
    void setCenterPointVisible(boolean visible);

    /**
     * Gets the center point label visibility.
     */
    boolean isCenterPointLabelVisible();

    /**
     * Set the center point label visibility
     * Please note that this does not toggle the center label if the center marker is non-default
     * @param visible true if the center point label should be drawn when the shape is visible
     */
    void setCenterPointLabelVisible(boolean visible);

}
