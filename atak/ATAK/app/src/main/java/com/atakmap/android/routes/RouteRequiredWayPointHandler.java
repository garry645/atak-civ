
package com.atakmap.android.routes;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to preserve in a route an optional list attribute indicating which way points in the route,
 * if any, are required. Required way points will be indicated in a Route instance by the presence
 * of a true meta boolean value keyed on IS_WAY_POINT_REQUIRED.
 */
public class RouteRequiredWayPointHandler extends CotDetailHandler {

    public static final String IS_WAY_POINT_REQUIRED = "isWayPointRequired";
    public static final String REACHED_WAYPOINT = "reached_waypoint";

    public static final String REQUIRED_WAYPOINTS_DETAIL = "__nav_required_waypoints";
    public static final String UID = "uid";
    public static final String WAYPOINT = "_way_point";

    public RouteRequiredWayPointHandler() {
        super(REQUIRED_WAYPOINTS_DETAIL);
    }

    @Override
    public CommsMapComponent.ImportResult toItemMetadata(MapItem mapItem,
            CotEvent cotEvent, CotDetail cotDetail) {

        if (mapItem instanceof Route) {
            Route route = (Route) mapItem;

            Set<String> requiredWayPointIds = new HashSet<String>();
            int nodeCount = cotDetail.childCount();

            // Make a set of IDs for all of the required way points
            for (int i = 0; i < nodeCount; i++) {
                CotDetail wayPointDetail = cotDetail.getChild(i);

                String uid = wayPointDetail.getAttribute(UID);

                requiredWayPointIds.add(uid);
            }

            // Mark way points in the route as required if their ID is in our
            // required way points set
            List<PointMapItem> items = route.getPointMapItems();
            for (PointMapItem item : items) {
                if (item == null) {
                    continue;
                }

                if (Route.WAYPOINT_TYPE.equals(item.getType())) {
                    if (requiredWayPointIds.contains(item.getUID())) {
                        item.setMetaBoolean(IS_WAY_POINT_REQUIRED, true);
                    }
                }
            }

            return CommsMapComponent.ImportResult.SUCCESS;
        } else {
            return CommsMapComponent.ImportResult.IGNORE;
        }
    }

    @Override
    public boolean toCotDetail(MapItem mapItem, CotEvent cotEvent,
            CotDetail cotDetail) {
        if (mapItem instanceof Route) {
            Route route = (Route) mapItem;

            // Look for way points in the route
            List<PointMapItem> items = route.getPointMapItems();

            for (PointMapItem item : items) {

                if (Route.WAYPOINT_TYPE.equals(item.getType())) {
                    boolean isRequired = item
                            .getMetaBoolean(IS_WAY_POINT_REQUIRED, false);

                    if (isRequired) {

                        CotDetail wayPointsDetail = null;
                        if ((wayPointsDetail = cotDetail.getFirstChildByName(0,
                                REQUIRED_WAYPOINTS_DETAIL)) == null) {

                            //add the requiredwaypoint cot detail node to the main cotdetail node
                            wayPointsDetail = new CotDetail(
                                    REQUIRED_WAYPOINTS_DETAIL);
                            cotDetail.addChild(wayPointsDetail);
                        }

                        CotDetail wayPoint = new CotDetail(WAYPOINT);
                        wayPoint.setAttribute(UID, item.getUID());
                        wayPointsDetail.addChild(wayPoint);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
