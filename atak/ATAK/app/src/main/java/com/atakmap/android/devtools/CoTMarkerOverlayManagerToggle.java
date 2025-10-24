
package com.atakmap.android.devtools;

import android.widget.BaseAdapter;
import android.widget.Toast;

import com.atakmap.android.hierarchy.HierarchyListFilter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.MapOverlay;
import com.atakmap.android.overlay.MapOverlayParent;

import java.util.List;

class CoTMarkerOverlayManagerToggle extends DevToolToggle {

    private final MapView mapView;

    private static class InterposerMapOverlayParent extends MapOverlayParent {

        private boolean muted;

        public InterposerMapOverlayParent(MapView mapView,
                MapOverlayParent cloneFromOverlay) {

            super(mapView, cloneFromOverlay.getIdentifier(),
                    cloneFromOverlay.getName(),
                    //XXX-- ideally these could be fetched from overlay
                    "asset://icons/affiliations.png", 2, false);
            List<MapOverlay> overlays = cloneFromOverlay.getOverlays();
            cloneFromOverlay.clear();
            for (MapOverlay overlay : overlays) {
                add(overlay);
            }
        }

        public void setMuted(boolean muted) {
            this.muted = muted;
        }

        public boolean getMuted() {
            return this.muted;
        }

        @Override
        public HierarchyListItem getListModel(BaseAdapter adapter,
                long capabilities, HierarchyListFilter filter) {

            // ignore hierarchy if muted
            if (this.muted)
                return null;

            return super.getListModel(adapter, capabilities, filter);
        }

        @Override
        public HierarchyListItem getListModel(BaseAdapter callback,
                long actions, Sort sort) {

            // ignore hierarchy if muted
            if (this.muted)
                return null;

            return super.getListModel(callback, actions, sort);
        }
    }

    CoTMarkerOverlayManagerToggle(MapView mapView) {
        super("CoT Markers In Overlay Manager",
                "CoTMarkers.OverlayManager.Enabled");
        this.mapView = mapView;
    }

    private InterposerMapOverlayParent getInterposer(boolean ensure) {
        InterposerMapOverlayParent interposer = null;
        synchronized (mapView.getMapOverlayManager()) {
            MapOverlay markerroot = mapView.getMapOverlayManager()
                    .getOverlay("markerroot");
            if (markerroot instanceof InterposerMapOverlayParent) {
                interposer = (InterposerMapOverlayParent) markerroot;
            } else if (markerroot instanceof MapOverlayParent && ensure) {
                mapView.getMapOverlayManager().removeOverlay(markerroot);
                interposer = new InterposerMapOverlayParent(mapView,
                        (MapOverlayParent) markerroot);
                mapView.getMapOverlayManager().addOverlay(interposer);
            }
        }
        return interposer;
    }

    @Override
    protected boolean isEnabled() {
        InterposerMapOverlayParent interposer = getInterposer(false);
        if (interposer == null)
            return true;
        return !interposer.getMuted();
    }

    @Override
    protected void setEnabled(boolean v) {
        InterposerMapOverlayParent interposer = getInterposer(true);
        if (interposer != null) {
            interposer.setMuted(!v);
            Toast.makeText(this.mapView.getContext(),
                    "Reopen Overlay Manager to apply",
                    Toast.LENGTH_LONG).show();
        }
    }
}
