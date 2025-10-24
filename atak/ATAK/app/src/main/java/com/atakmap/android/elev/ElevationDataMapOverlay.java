
package com.atakmap.android.elev;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.MapOverlayParent;
import com.atakmap.app.R;

final class ElevationDataMapOverlay extends MapOverlayParent {
    public ElevationDataMapOverlay(MapView mapView) {
        super(mapView,
                ElevationDataMapOverlay.class.getName(),
                mapView.getContext().getString(R.string.elevation_data),
                "resource://" + R.drawable.ic_overlay_dted,
                97,
                true);
    }
}
