package com.atakmap.map.layer.control;

import com.atakmap.map.MapControl;

public interface StarsControl extends MapControl
{
    boolean isStarsEnabled();

    void setStarsEnabled(boolean enabled);
}
