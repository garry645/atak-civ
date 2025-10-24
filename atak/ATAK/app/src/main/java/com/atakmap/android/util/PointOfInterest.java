
package com.atakmap.android.util;

import com.atakmap.android.icons.IconsMapAdapter;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.app.R;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.util.UUID;

public class PointOfInterest {
    private static PointOfInterest _instance;
    private final static String spiUID = UUID.randomUUID().toString();

    private PointOfInterest() {
    }

    /**
     * Retrieves a PointOfInterest which manages the SPI for the FastMGRS capability.
     * @return the point of interest instance
     */
    synchronized static public PointOfInterest getInstance() {
        if (_instance == null)
            _instance = new PointOfInterest();
        return _instance;
    }

    /**
     * Sets the point which will ensure that the point of interest marker exists, is added to the
     * map at the desired location and is visible.   The result of this call is a reference to the
     * marker
     * @param gp the geospatial coordiates
     * @return the marker
     */
    public Marker setPoint(final GeoPointMetaData gp) {
        Marker poi = getSPI();
        poi.setVisible(true);
        poi.setPoint(gp);
        return poi;
    }

    synchronized private Marker getSPI() {
        MapView mapView = MapView.getMapView();
        Marker mi = (Marker) mapView.getMapItem(spiUID);
        if (mi == null) {
            Marker blankMarker = new Marker(GeoPoint.ZERO_POINT, spiUID);
            blankMarker.setMetaBoolean("nevercot", true);
            blankMarker.setZOrder(-2000d);
            blankMarker.setMetaString("callsign", mapView.getContext()
                    .getString(R.string.poi_title));
            blankMarker.setMetaBoolean("editable", false);
            blankMarker.setMovable(false);
            blankMarker.setMetaBoolean("removable", true);
            blankMarker.setType("b-m-p-s-p-i");
            IconsMapAdapter iconAdapter = new IconsMapAdapter(
                    mapView.getContext());

            iconAdapter.adaptMarkerIcon(blankMarker);
            blankMarker.setVisible(true);
            blankMarker.setMetaString("menu", "menus/poi.xml");
            final MapGroup mg = mapView.getRootGroup().findMapGroup("SPIs");
            mg.addItem(blankMarker);
            return blankMarker;
        } else {
            return mi;
        }
    }

}
