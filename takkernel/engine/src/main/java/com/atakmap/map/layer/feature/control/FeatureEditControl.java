package com.atakmap.map.layer.feature.control;

import com.atakmap.map.MapControl;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;

/**
 *
 */
public interface FeatureEditControl extends MapControl
{
    boolean startEditing(long fid);
    void stopEditing(long fid);
    @Deprecated
    void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, Feature.AltitudeMode altitudeMode, double extrude);
}
