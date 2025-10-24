package com.atakmap.map.layer.feature.opengl;

import com.atakmap.map.layer.feature.control.FeaturesLabelControl;

/**
 */
final class NativeFeaturesLabelControl implements FeaturesLabelControl
{
    long pointer;
    
    public NativeFeaturesLabelControl(long pointer)
    {
        this.pointer = pointer;
    }

    public String getName() {
        return "com.atakmap.map.layer.feature.control.FeaturesLabelControl";
    }

    public void setShapeCenterMarkersVisible(boolean enabled) {
        setShapeCenterMarkersVisible(pointer, enabled);
    }

    public boolean getShapeCenterMarkersVisible() {
        return getShapeCenterMarkersVisible(pointer);
    }

    public void setShapeLabelVisible(long fid, boolean visible) {
        setShapeLabelVisible(pointer, fid, visible);
    }

    public boolean getShapeLabelVisible(long fid) {
        return getShapeLabelVisible(pointer, fid);
    }

    public void setDefaultLabelBackground(int bgColor, boolean override) {
        setDefaultLabelBackground(pointer, bgColor, override);
    }
    public int getDefaultLabelBackgroundColor() {
        return getDefaultLabelBackgroundColor(pointer);
    }
    public boolean isDefaultLabelBackgroundOverride() {
        return isDefaultLabelBackgroundOverride(pointer);
    }

    public void setDefaultLabelLevelOfDetail(int minLod, int maxLod) {
        setDefaultLabelLevelOfDetail(pointer, minLod, maxLod);
    }
    public int getDefaultLabelMinLevelOfDetail() {
        return getDefaultLabelMinLevelOfDetail(pointer);
    }
    public int getDefaultLabelMaxLevelOfDetail() {
        return getDefaultLabelMaxLevelOfDetail(pointer);
    }

    static native void setShapeCenterMarkersVisible(long pointer, boolean enabled);
    static native boolean getShapeCenterMarkersVisible(long pointer);
    static native void setShapeLabelVisible(long pointer, long fid, boolean visible) ;
    static native boolean getShapeLabelVisible(long pointer, long fid) ;
    static native void setDefaultLabelBackground(long pointer, int bgColor, boolean override);
    static native int getDefaultLabelBackgroundColor(long pointer);
    static native boolean isDefaultLabelBackgroundOverride(long pointer);
    static native void setDefaultLabelLevelOfDetail(long pointer, int minLod, int maxLod);
    static native int getDefaultLabelMinLevelOfDetail(long pointer);
    static native int getDefaultLabelMaxLevelOfDetail(long pointer);

}
