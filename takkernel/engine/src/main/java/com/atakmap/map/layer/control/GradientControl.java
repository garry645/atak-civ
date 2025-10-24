package com.atakmap.map.layer.control;

import com.atakmap.map.MapControl;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface GradientControl extends MapControl {

    int RELATIVE_MODE = 0;
    int ABSOLUTE_MODE = 1;

    int[] getGradientColors();
    String[] getLineItemStrings();

    int getMode();
    void setMode(int mode);
}
