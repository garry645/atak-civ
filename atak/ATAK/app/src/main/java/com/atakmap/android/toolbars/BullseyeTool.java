
package com.atakmap.android.toolbars;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.ButtonTool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.android.util.ATAKUtilities;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.NorthReference;

import java.util.List;
import java.util.UUID;

import gov.tak.api.annotation.DeprecatedApi;

public class BullseyeTool extends ButtonTool implements
        MapEventDispatcher.MapEventDispatchListener {

    private static final String TAG = "BullseyeTool";
    public static final String TOOL_IDENTIFIER = "com.atakmap.android.toolbars.BullseyeTool";
    public static final String BULLSEYE_COT_TYPE = BullseyeMapItem.BULLSEYE_COT_TYPE;
    public static final int MAX_RADIUS = 1500000;

    protected final Context _context;
    protected GeoPointMetaData centerLoc = null;
    protected GeoPointMetaData edgeLoc = null;
    protected Marker centerMarker = null;
    protected Marker edgeMarker = null;
    private final AtakPreferences prefs;

    BullseyeTool(MapView mapView, ImageButton button) {
        super(mapView, button, TOOL_IDENTIFIER);
        _context = mapView.getContext();
        ToolManagerBroadcastReceiver.getInstance().registerTool(
                TOOL_IDENTIFIER, this);
        prefs = AtakPreferences.getInstance(_context);
    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        centerLoc = null;
        edgeLoc = null;
        centerMarker = null;
        edgeMarker = null;
        _mapView.getMapEventDispatcher().pushListeners();
        _mapView.getMapEventDispatcher().clearListeners(MapEvent.MAP_CLICK);
        _mapView.getMapEventDispatcher()
                .clearListeners(MapEvent.MAP_LONG_PRESS);
        _mapView.getMapEventDispatcher().clearListeners(MapEvent.ITEM_CLICK);
        _mapView.getMapEventDispatcher().clearListeners(
                MapEvent.ITEM_LONG_PRESS);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_CLICK, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_LONG_PRESS, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_CLICK, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_LONG_PRESS, this);
        _mapView.getMapTouchController().setToolActive(true);

        prompt(R.string.rb_bullseye);

        return true;
    }

    @Override
    public void onMapEvent(MapEvent event) {

        if (event.getType().equals(MapEvent.MAP_CLICK)
                || event.getType().equals(MapEvent.ITEM_CLICK)) {
            PointF pt = event.getPointF();

            if (centerLoc == null) {
                centerLoc = _mapView.inverseWithElevation(pt.x, pt.y);
                if (event.getType().equals(MapEvent.ITEM_CLICK)) {
                    if (event.getItem() != null
                            && event.getItem() instanceof Marker) {
                        centerMarker = (Marker) event.getItem();
                        centerLoc = centerMarker.getGeoPointMetaData();
                    }
                }

                _mapView.getMapEventDispatcher().removeMapEventListener(
                        MapEvent.MAP_LONG_PRESS, this);

                TextContainer.getInstance().closePrompt();
                prompt(R.string.rb_bullseye_edge);
            } else {
                //check max length
                GeoPointMetaData edgePt = _mapView.inverseWithElevation(pt.x,
                        pt.y);
                if (event.getType().equals(MapEvent.ITEM_CLICK)) {
                    if (event.getItem() != null
                            && event.getItem() instanceof Marker) {
                        edgeMarker = (Marker) event.getItem();
                        edgePt = edgeMarker.getGeoPointMetaData();
                    }
                }
                if (edgePt.get().distanceTo(
                        centerLoc.get()) > MAX_RADIUS) {
                    Toast.makeText(_context, R.string.bullseye_radius_large,
                            Toast.LENGTH_SHORT).show();
                    // just in case the edge marker was set
                    edgeMarker = null;
                    return;
                } else if (edgePt.get().distanceTo(centerLoc.get()) < 50) {
                    Toast.makeText(_context, R.string.bullseye_radius_small,
                            Toast.LENGTH_SHORT).show();
                    // just in case the edge marker was set
                    edgeMarker = null;
                    return;
                }

                edgeLoc = edgePt;

                finishTool(centerLoc, centerLoc.get().distanceTo(edgeLoc.get()),
                        null);
            }
        } else if (event.getType().equals(MapEvent.MAP_LONG_PRESS)) {
            GeoPointMetaData point = _mapView.inverseWithElevation(
                    event.getPointF().x, event.getPointF().y);
            displayCoordinateDialog(point);
        }
    }

    private void finishTool(GeoPointMetaData center, double radius,
            String coordFmt) {
        BullseyeMapItem bullseye = new BullseyeMapItem(_mapView, center,
                UUID.randomUUID()
                        .toString());
        bullseye.setType(BULLSEYE_COT_TYPE);
        bullseye.setMetaBoolean("archive", true);
        bullseye.setMetaString("how", "h-g-i-g-o");
        bullseye.setMetaBoolean("ignoreOffscreen", true);
        ATAKUtilities.setAuthorInformation(bullseye);
        if (centerMarker != null)
            bullseye.setMetaString("centerMarkerUID", centerMarker.getUID());
        if (edgeMarker != null)
            bullseye.setMetaString("edgeMarkerUID", edgeMarker.getUID());

        bullseye.setMovable(true);
        if (coordFmt != null) {
            bullseye.setMetaString("coordFormat", coordFmt);
        }

        bullseye.setRadiusRings(prefs.get("bullseyeRadiusRings", 500));
        bullseye.setEdgeToCenterDirection(
                prefs.get("bullseyeDirection", false));
        bullseye.setNumRings(prefs.get("bullseyeNumRings", 4));
        bullseye.setMetaBoolean("rangeRingVisible",
                prefs.get("bullseyeRingsVisible", false));
        bullseye.setMetaString("entry", "user");

        TextContainer.getInstance().closePrompt();
        createBullseye(_mapView, bullseye, radius);

        Intent i = new Intent(
                BullseyeDropDownReceiver.DROPDOWN_TOOL_IDENTIFIER);
        i.putExtra("edit", true);
        i.putExtra("marker_uid", bullseye.getUID());
        AtakBroadcast.getInstance().sendBroadcast(i);

        requestEndTool();
    }

    /**
     * Given a maker if it is bullseye it will just return the bullseye otherwise will create a
     * reference bullseye
     * @param marker the given marker
     * @param radiusInMeters the radius of the bullseye
     * @return a bullseye map item
     */
    static BullseyeMapItem createBullseye(MapView mapView, Marker marker,
            double radiusInMeters) {
        MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
        MapView mv = MapView.getMapView();
        if (rabGroup == null || mv == null || marker == null)
            return null;

        //check that marker doesnt already have a bullseye associated
        if (!(marker instanceof BullseyeMapItem)
                && marker.hasMetaValue("bullseyeUID")) {
            MapItem mi = mapView
                    .getMapItem(marker.getMetaString("bullseyeUID", ""));
            if (mi instanceof BullseyeMapItem)
                return (BullseyeMapItem) mi;
            else
                return null;
        }

        BullseyeMapItem bullseye;
        if (marker instanceof BullseyeMapItem)
            bullseye = (BullseyeMapItem) marker;
        else {
            bullseye = new BullseyeMapItem(mapView,
                    marker.getGeoPointMetaData(),
                    UUID.randomUUID().toString());
            bullseye.setMetaString("centerMarkerUID", marker.getUID());
            bullseye.setClickable(false);

        }

        marker.setMetaString("bullseyeUID", bullseye.getUID());
        marker.setMetaBoolean("bullseyeOverlay", true);

        final int numBullseye = getNumBullseye();
        String title = "Bullseye " + numBullseye;
        bullseye.setTitle(title);
        bullseye.setRadius(radiusInMeters, Span.METER);

        Context context = mv.getContext();
        AtakPreferences prefs = AtakPreferences.getInstance(context);
        NorthReference ref = NorthReference.findFromValue(Integer.parseInt(
                prefs.get("rab_north_ref_pref",
                        String.valueOf(NorthReference.MAGNETIC.getValue()))));
        bullseye.setNorthReference(ref);

        Angle unit = Angle.findFromValue(
                Integer.parseInt(prefs.get("rab_brg_units_pref",
                        String.valueOf(0))));
        bullseye.setBearingUnits(unit);

        int numRings = Parsers
                .parseInt(prefs.get("bullseyeNumRings", null), 4);
        double radiusRings = Parsers.parseDouble(
                prefs.get("bullseyeRadiusRings", null), 400.0);
        boolean direction = prefs.get("bullseyeDirection", false);
        bullseye.setNumRings(numRings);
        bullseye.setRadiusRings(radiusRings);
        bullseye.setEdgeToCenterDirection(direction);

        rabGroup.addItem(bullseye);

        bullseye.persist(mv.getMapEventDispatcher(), null,
                bullseye.getClass());

        return bullseye;
    }

    /**
     * Show the dialog to manually enter the center coordinate
     *
     * @param centerPoint - the point to populate the views with
     */
    public void displayCoordinateDialog(GeoPointMetaData centerPoint) {

        boolean movable = centerMarker == null || centerMarker.getMovable();

        CoordinateEntryCapability.getInstance(_context).showDialog(
                _context.getString(R.string.rb_coord_title),
                centerPoint,
                movable, _mapView.getPoint(), null, false,
                new CoordinateEntryCapability.ResultCallback() {
                    @Override
                    public void onResultCallback(String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {
                        // On click get the geopoint and elevation double in ft
                        CoordinateFormat cf = CoordinateFormat.find(pane);
                        if (cf.getDisplayName().equals(pane)
                                && cf != CoordinateFormat.ADDRESS)
                            cf = CoordinateFormat.MGRS;

                        finishTool(point, prefs.get("bullseyeDistance", 2000),
                                cf.getDisplayName());
                    }
                });

    }

    @DeprecatedApi(since = "4.8.1", removeAt = "5.1", forRemoval = true)
    public static void removeOverlay(Marker centerMarker,
            boolean removeCenter) {
        if (centerMarker instanceof BullseyeMapItem)
            ((BullseyeMapItem) centerMarker).removeOverlay(
                    (BullseyeMapItem) centerMarker, removeCenter);
    }

    private static int getNumBullseye() {
        MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
        if (rabGroup == null)
            return 1;

        List<MapItem> list = rabGroup.deepFindItems("bullseye", "true");
        if (list == null || list.isEmpty())
            return 1;
        else
            return list.size() + 1;
    }

    @Override
    public void onToolEnd() {
        _mapView.getMapTouchController().setToolActive(false);
        _mapView.getMapEventDispatcher().clearListeners();
        _mapView.getMapEventDispatcher().popListeners();
        TextContainer.getInstance().closePrompt();
    }

    synchronized protected void prompt(int stringId) {
        TextContainer.getInstance().displayPrompt(_context
                .getString(stringId));
    }

}
