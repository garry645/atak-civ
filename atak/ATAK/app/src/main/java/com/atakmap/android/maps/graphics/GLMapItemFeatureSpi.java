
package com.atakmap.android.maps.graphics;

import com.atakmap.android.maps.MapItem;

interface GLMapItemFeatureSpi {
    GLMapItemFeature create(GLMapItemFeatures features, MapItem item);

    boolean isSupported(MapItem item);
}
