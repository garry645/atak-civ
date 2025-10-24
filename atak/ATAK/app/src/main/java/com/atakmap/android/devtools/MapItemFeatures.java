
package com.atakmap.android.devtools;

import com.atakmap.android.maps.MapView;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.ConfigOptions;

final class MapItemFeatures extends DevToolToggle {
    MapItemFeatures() {
        super("Map Item Features", "mapitem.features.enabled");
    }

    @Override
    protected boolean isEnabled() {
        return (ConfigOptions.getOption("mapitem.features.enabled", 1) != 0);
    }

    @Override
    protected void setEnabled(boolean v) {
        if (isEnabled() == v)
            return;
        ConfigOptions.setOption("mapitem.features.enabled", v ? 1 : 0);
        final GLMapView view = MapView.getMapView().getGLSurface()
                .getGLMapView();
        if (view != null) {
            view.getRenderContext().queueEvent(new Runnable() {
                @Override
                public void run() {
                    view.release();
                }
            });
        }
    }
}
