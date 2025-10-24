package com.atakmap.map.layer.raster.controls;

import com.atakmap.map.MapControl;

public interface ImageryRelativeScaleControl extends MapControl {
    float getRelativeScale();

    /**
     *
     * @param factor    {@code 1f} corresponds to 100% at current DPI; {@code 2f} corresponds to
     *                  200% at current DPI; {@code 0.5f} corresponds to 50% at current DPI.
     *
     */
    void setRelativeScale(float factor);
}
