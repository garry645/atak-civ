
package com.atakmap.android.toolbars;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;

import java.util.HashSet;
import java.util.Set;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.detail.ICotDetailHandler2;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;

class BullseyeDetailHandler2 implements ICotDetailHandler2 {

    private final MapView _mapView;
    private final Set<String> _detailNames;

    BullseyeDetailHandler2() {
        _mapView = MapView.getMapView();
        _detailNames = new HashSet<>();
        _detailNames.add("bullseye");
    }

    @Override
    public Set<String> getDetailNames() {
        return _detailNames;
    }

    @Override
    public ICotDetailHandler.ImportResult toItemMetadata(Object processItem,
            AttributeSet attrs,
            CotEvent event,
            CotDetail detail) {

        if (!(processItem instanceof MapItem))
            return ICotDetailHandler.ImportResult.FAILURE;
        MapItem mapItem = (MapItem) processItem;

        String bullseyeUID = detail.getAttribute("bullseyeUID");
        if (bullseyeUID == null)
            return ICotDetailHandler.ImportResult.FAILURE;

        if (!(mapItem instanceof BullseyeMapItem)
                && mapItem instanceof Marker) {
            createOrUpdateBullseye(_mapView, (Marker) mapItem, detail);

            if (Boolean.parseBoolean(detail.getAttribute("mils")))
                attrs.setAttribute("mils_mag", true);
            else
                attrs.setAttribute("deg_mag", true);

        } else if (mapItem instanceof BullseyeMapItem) {
            CotUtils.setBoolean(attrs, "rangeRingVisible",
                    detail.getAttribute("rangeRingVisible"));

            attrs.setAttribute("title", detail.getAttribute("title"));
            String centerMarkerUid = detail.getAttribute("centerMarkerUID");
            if (centerMarkerUid != null)
                attrs.setAttribute("centerMarkerUID",
                        detail.getAttribute("centerMarkerUID"));
            String edgeMarkerUID = detail.getAttribute("edgeMarkerUID");
            if (edgeMarkerUID != null)
                attrs.setAttribute("edgeMarkerUID",
                        detail.getAttribute("edgeMarkerUID"));

            createOrUpdateBullseye(_mapView, (BullseyeMapItem) mapItem, detail);
        }

        return ICotDetailHandler.ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(Object processItem, AttributeSet attrs,
            CotEvent event,
            CotDetail detail) {
        if (!(processItem instanceof MapItem))
            return false;

        // persist the two cases, either it is an ordinary marker with a reference to a
        // bullseye or it is a bullseye directly.
        MapItem mapItem = (MapItem) processItem;
        if (!(mapItem instanceof BullseyeMapItem)) {
            if (attrs.containsAttribute("bullseyeUID")) {
                CotDetail be = new CotDetail("bullseye");
                be.setAttribute("bullseyeUID",
                        attrs.getStringAttribute("bullseyeUID", ""));
                return true;
            } else {
                return false;
            }
        }

        // if it is a bullseye persist separately
        BullseyeMapItem bullseyeMapItem = (BullseyeMapItem) mapItem;

        CotDetail be = new CotDetail("bullseye");
        // older devices process a bullseye message as a marker and require that the bullseye uid
        // be different than the bullseye message.   Can deprecate after 6 release cycles.
        be.setAttribute("bullseyeUID", bullseyeMapItem.getUID() + ".COMPAT");
        be.setAttribute("title",
                bullseyeMapItem.getMetaString("title", "bullseye1"));

        be.setAttribute("edgeToCenter",
                String.valueOf(bullseyeMapItem.isShowingEdgeToCenter()));
        be.setAttribute("distance",
                String.valueOf(bullseyeMapItem.getRadius()));
        be.setAttribute("distanceUnits", bullseyeMapItem.getRadiusUnits()
                .getAbbrev());

        boolean ringsVisible = bullseyeMapItem
                .getMetaBoolean("rangeRingVisible", false);
        be.setAttribute("rangeRingVisible", String.valueOf(ringsVisible));
        double radiusRings = bullseyeMapItem.getRadiusRings();

        be.setAttribute("hasRangeRings", String.valueOf(radiusRings > 0.0));
        be.setAttribute("ringDist", Double.toString(radiusRings));
        be.setAttribute("ringNum", Integer.toString(bullseyeMapItem
                .getNumRings()));
        detail.addChild(be);

        return true;
    }

    @Override
    public boolean isSupported(Object processItem, AttributeSet attrs,
            CotEvent event,
            CotDetail detail) {
        return processItem instanceof Marker;
    }

    /**
     * Create a Bullseye overlay from the CotDetail saved to the center marker
     *
     * @param centerMarker the center marker for the Bullseye, if it is itself a bullseye
     *                     it will just return
     * @param detail the Detail that contains all the parameters of the Bullseye
     */
    static void createOrUpdateBullseye(MapView mapview, Marker centerMarker,
            CotDetail detail) {
        MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();

        if (rabGroup == null || mapview == null || centerMarker == null)
            return;

        String bullseyeUID = detail.getAttribute("bullseyeUID");

        //check for existing Overlay
        BullseyeMapItem bullseye;
        if (centerMarker instanceof BullseyeMapItem) {
            bullseye = (BullseyeMapItem) centerMarker;
        } else {
            MapItem mi = mapview.getMapItem(bullseyeUID);
            if (mi instanceof BullseyeMapItem) {
                bullseye = (BullseyeMapItem) mi;
            } else {
                bullseye = new BullseyeMapItem(mapview,
                        centerMarker.getGeoPointMetaData(), bullseyeUID);
                bullseye.setMetaString("centerMarkerUID",
                        centerMarker.getUID());
                bullseye.setMetaBoolean("addToObjList", false);
            }
        }

        String title = detail.getAttribute("title");
        String edgeToCenter = detail.getAttribute("edgeToCenter");
        String distanceString = detail.getAttribute("distance");
        String distUnitString = detail.getAttribute("distanceUnits");
        boolean hasRangeRings = Boolean.parseBoolean(detail
                .getAttribute("hasRangeRings"));
        bullseye.setMetaBoolean("hasRangeRings", hasRangeRings);
        String ringDistString = detail.getAttribute("ringDist");
        String ringNumString = detail.getAttribute("ringNum");

        double radiusInMeters = 0.0;
        double ringDist = 0.0;
        int ringNum = 1;
        try {
            radiusInMeters = Double.parseDouble(distanceString);
            ringDist = Double.parseDouble(ringDistString);
            ringNum = Integer.parseInt(ringNumString);
        } catch (Exception ignore) {
        }

        Span distUnits = Span.findFromAbbrev(distUnitString);
        if (distUnits == null)
            distUnits = Span.METER;

        bullseye.setPoint(centerMarker.getGeoPointMetaData());
        bullseye.setRadius(SpanUtilities.convert(radiusInMeters, Span.METER,
                distUnits), distUnits);

        bullseye.setVisible(centerMarker.getVisible());
        bullseye.setTitle(title);
        bullseye.setEdgeToCenterDirection(Boolean.parseBoolean(edgeToCenter));

        bullseye.setRadiusRings(ringDist);
        bullseye.setNumRings(ringNum);

    }
}
