package com.atakmap.map.layer.feature.control;

import com.atakmap.map.MapControl;

/**
 */
public interface FeaturesLabelControl extends MapControl
{
    void setShapeCenterMarkersVisible(boolean enabled);
    boolean getShapeCenterMarkersVisible();
    void setShapeLabelVisible(long fid, boolean visible);
    boolean getShapeLabelVisible(long fid);
    void setDefaultLabelBackground(int bgColor, boolean override);
    int getDefaultLabelBackgroundColor();
    boolean isDefaultLabelBackgroundOverride();
    void setDefaultLabelLevelOfDetail(int minLod, int maxLod);
    int getDefaultLabelMinLevelOfDetail();
    int getDefaultLabelMaxLevelOfDetail();
}
