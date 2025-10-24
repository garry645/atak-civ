
package com.atakmap.android.fires;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Collection;
import java.util.HashSet;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class SearchHelper {

    /**
     * Uses a limited number of map groups in order to find the single point CoT map items
     * @param mapView the map view to make use of
     * @param point the point to use for measuring the distance
     * @param distance the distance
     * @return the collection of map items
     */
    public static Collection<MapItem> deepFindItems(MapView mapView,
            GeoPoint point,
            double distance) {

        final Collection<MapItem> ret = new HashSet<>();

        if (mapView == null)
            return ret;

        final MapGroup rootGroup = mapView.getRootGroup();
        if (rootGroup == null)
            return ret;

        for (MapGroup cg : rootGroup.getChildGroups()) {
            final String fName = cg.getFriendlyName();
            if (fName != null) {

                // blacklist out the groups that should not be searched
                switch (fName) {
                    case "Mission":
                    case "Airspace":
                    case "SPIs":
                    case "Drawing Objects":
                    case "Quick Pic":
                    case "Weapons":
                    case "GRG":
                    case "Navigation":
                    case "RouteOwnedWaypoints":
                    case "Doghouses":
                    case "Track History":
                    case "KML":
                    case "Shapefile":
                    case "GPX":
                    case "GML":
                    case "GeoJSON":
                    case "LPT":
                    case "DRW":
                    case "Mapbox Vector Tiles":
                    case "Range & Bearing":
                    case "FIRES":
                    case "Pairing Lines":
                    case "import_drawing_kml":
                    case "MapItemSelectTool_selectors":
                    case "Geo Fence Breaches":
                    case "WFS":
                    case "Resection":
                    case "Saved Entries":
                    case "Geopackage":
                    case "Nine Line":
                    case "Five Line":
                    case "Link Lines":
                        break;
                    default:
                        ret.addAll(cg.deepFindItems(point, distance));
                }
            }
        }
        if (mapView.getSelfMarker().getPoint().distanceTo(point) < distance)
            ret.add(mapView.getSelfMarker());

        ret.addAll(rootGroup.findItems(point, distance));

        return ret;
    }

}
