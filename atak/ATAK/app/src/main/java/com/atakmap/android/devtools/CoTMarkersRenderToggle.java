
package com.atakmap.android.devtools;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;

class CoTMarkersRenderToggle extends DevToolToggle {

    final MapGroup _cotGroup;

    CoTMarkersRenderToggle(MapView mapView) {
        super("Render CoT Markers", "CoTMarkers.Render.Enabled");
        _cotGroup = mapView.getRootGroup().findMapGroup("Cursor on Target");
    }

    @Override
    protected boolean isEnabled() {
        return _cotGroup != null && _cotGroup.getVisible();
    }

    @Override
    protected void setEnabled(boolean v) {
        if (_cotGroup != null)
            _cotGroup.setVisible(v);
    }
}
