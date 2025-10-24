
package com.atakmap.android.toolbars;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.NorthReference;

import java.util.List;
import java.util.UUID;

public class RangeAndBearingReceiver extends BroadcastReceiver {

    public static final String TAG = "RangeAndBearingReceiver";

    //public static final String CREATE = "com.atakmap.android.toolbars.RangeAndBearing.CREATE";
    //public static final String CREATE_UPDATE = "com.atakmap.android.toolbars.RangeAndBearing.CREATE_UPDATE";
    public static final String DESTROY = "com.atakmap.android.toolbars.RangeAndBearing.DESTROY";
    //public static final String REVERSE = "com.atakmap.android.toolbars.RangeAndBearing.REVERSE";
    public static final String RANGE_UNITS = "com.atakmap.android.toolbars.RangeAndBearing.RANGE_UNITS";
    public static final String BEARING_UNITS = "com.atakmap.android.toolbars.RangeAndBearing.BEARING_UNITS";
    public static final String CHANGECOLOR = "com.arrowmkaer.android.maps.toolbars.RangeAndBearing.CHANGECOLOR";
    public static final String PIN_DYNAMIC = "com.atakmap.android.toolbars.RangeAndBearing.PIN_DYNAMIC";
    public static final String TOGGLE_SLANT_RANGE = "com.atakmap.android.toolbars.RangeAndBearing.TOGGLE_SLANT_RANGE";
    //public static final String SHARE = "com.arrowmkaer.android.maps.toolbars.RangeAndBearing.SHARE";

    private final MapView _mapView;

    public RangeAndBearingReceiver(MapView mapView) {
        _mapView = mapView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;

        final Bundle extras = intent.getExtras();

        switch (action) {
            case TOGGLE_SLANT_RANGE:
                // Toggle slant range global preference
                AtakPreferences prefs = AtakPreferences
                        .getInstance(_mapView.getContext());
                String prefValue = prefs.get("rab_dist_slant_range", "clamped");
                if (prefValue.equals("slantrange"))
                    prefValue = "clamped";
                else
                    prefValue = "slantrange";
                prefs.set("rab_dist_slant_range", prefValue);

                // Toggle specific slant range
                /*if (intent.getExtras().containsKey("id")) {
                String id = (String) intent.getExtras().get("id");
                RangeAndBearingMapItem rb = RangeAndBearingMapItem
                        .getRABLine(id);
                if (rb != null) {
                    rb.toggleSlantRange();
                }
                }*/
                break;
            case DESTROY:
                if (intent.hasExtra("uid")) {
                    // Remove first connected line
                    String uid = intent.getStringExtra("uid");
                    MapItem mi = _mapView.getRootGroup().deepFindUID(uid);
                    if (mi != null && mi.getGroup() != null
                            && mi instanceof PointMapItem) {
                        List<RangeAndBearingMapItem> lines = RangeAndBearingMapItem
                                .getUsers((PointMapItem) mi);
                        if (!lines.isEmpty())
                            removeLine(lines.get(0));

                        MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                                MetricsUtils.EVENT_MAPITEM_REMOVED,
                                "RangeAndBearingReceiver",
                                mi, MetricsUtils.EVENT_STATUS_SUCCESS);

                        return;
                    }
                }
                if (intent.hasExtra("id")) {
                    String id = intent.getStringExtra("id");
                    RangeAndBearingMapItem rb = RangeAndBearingMapItem
                            .getRABLine(id);
                    if (rb != null) {
                        removeLine(rb);

                        MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                                MetricsUtils.EVENT_MAPITEM_REMOVED,
                                "RangeAndBearingReceiver",
                                rb, MetricsUtils.EVENT_STATUS_SUCCESS);

                    } else {
                        Log.w(TAG,
                                "There is no RangeAndBearingWidget with id '"
                                        + id + "'.");
                    }
                } else if (intent.hasExtra("group")) {
                    String groupName = intent.getStringExtra("group");
                    if (groupName != null) {
                        MapGroup group = _mapView.getRootGroup()
                                .findMapGroup(groupName);
                        if (group != null) {
                            MapItem line = group.findItem("type", "rb");
                            if (line != null) {
                                group.removeItem(line);

                                MetricsUtils.record(
                                        MetricsUtils.CATEGORY_MAPITEM,
                                        MetricsUtils.EVENT_MAPITEM_REMOVED,
                                        "RangeAndBearingReceiver",
                                        line,
                                        MetricsUtils.EVENT_STATUS_SUCCESS);

                            }
                        }
                    }
                } else {
                    Log.w(TAG, "The extra 'id' is required.");
                }

                break;
            case CHANGECOLOR:
                if (extras != null && extras.containsKey("id")
                        && extras.containsKey("color")) {
                    String id = (String) intent.getExtras().get("id");
                    int color = intent.getExtras().getInt("color");
                    RangeAndBearingMapItem rb = RangeAndBearingMapItem
                            .getRABLine(id);
                    if (rb != null) {
                        rb.setStrokeColor(color);
                        rb.persist(_mapView.getMapEventDispatcher(),
                                null,
                                this.getClass());
                    }
                }
                break;
            case BEARING_UNITS:
                if (extras != null && extras.containsKey("id")) {
                    String id = (String) intent.getExtras().get("id");
                    RangeAndBearingMapItem rb = RangeAndBearingMapItem
                            .getRABLine(id);
                    int units = intent.getIntExtra("units", -1);
                    int reference = intent.getIntExtra("reference", -1);
                    if (rb != null && units != -1 && reference != -1) {
                        rb.setBearingUnits(Angle.findFromValue(units));
                        rb.setNorthReference(NorthReference
                                .findFromValue(reference));
                    }
                }
                break;
            case RANGE_UNITS:
                if (extras != null && extras.containsKey("id")) {
                    String id = (String) intent.getExtras().get("id");
                    RangeAndBearingMapItem rb = RangeAndBearingMapItem
                            .getRABLine(id);
                    int units = intent.getIntExtra("units", -1);
                    if (rb != null && units != -1) {
                        try {
                            rb.setRangeUnits(units);
                        } catch (IllegalStateException ise) {
                            Log.e(TAG,
                                    "error setting the range units for a range and bearing arrow: "
                                            + rb.getUID(),
                                    ise);
                        }
                    }
                }
                break;
            case PIN_DYNAMIC:
                if (extras != null && extras.containsKey("id")) {
                    String id = (String) intent.getExtras().get("id");
                    RangeAndBearingMapItem rb = RangeAndBearingMapItem
                            .getRABLine(id);
                    if (rb != null) {
                        RangeAndBearingEndpoint point1 = new RangeAndBearingEndpoint(
                                rb
                                        .getPoint1Item()
                                        .getGeoPointMetaData(),
                                UUID.randomUUID()
                                        .toString());
                        point1.setMetaString("menu",
                                "menus/rab_endpoint_menu.xml");
                        RangeAndBearingMapComponent.getGroup().addItem(point1);
                        RangeAndBearingEndpoint point2 = new RangeAndBearingEndpoint(
                                rb
                                        .getPoint2Item()
                                        .getGeoPointMetaData(),
                                UUID.randomUUID()
                                        .toString());
                        point2.setMetaString("menu",
                                "menus/rab_endpoint_menu.xml");
                        RangeAndBearingMapComponent.getGroup().addItem(point2);
                        RangeAndBearingMapItem newRABLine = RangeAndBearingMapItem
                                .createOrUpdateRABLine(
                                        UUID.randomUUID().toString(),
                                        point1, point2);
                        ATAKUtilities.setAuthorInformation(newRABLine);
                        newRABLine.setStrokeColor(rb.getStrokeColor());
                        newRABLine.setMetaString("entry", "user");
                        RangeAndBearingMapComponent.getGroup()
                                .addItem(newRABLine);
                        newRABLine.persist(_mapView.getMapEventDispatcher(),
                                null, this.getClass());
                    }
                    //TODO: is this the correct way to kill the dynamic R+B tool?
                    Tool t = ToolManagerBroadcastReceiver.getInstance()
                            .getActiveTool();
                    if (t != null)
                        t.requestEndTool();
                }
                break;
        }
    }

    private void removeLine(RangeAndBearingMapItem rb) {
        if (rb != null && !rb.removeFromGroup())
            rb.dispose();
    }
}
