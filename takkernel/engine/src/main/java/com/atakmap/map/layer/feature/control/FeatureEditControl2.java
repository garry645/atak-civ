package com.atakmap.map.layer.feature.control;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;

/**
 *
 */
public interface FeatureEditControl2 extends FeatureEditControl
{
    void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, Feature.Traits traits);
}
