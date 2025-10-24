
package com.atakmap.android.toolbars;

import com.atakmap.android.cot.CotUtils;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import gov.tak.api.annotation.DeprecatedApi;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.NorthReference;

/** @deprecated see the kernel's {@code BullseyeDetailHandler} and ATAK's
 * {@code BullseyeDetailHandler2} */
@Deprecated
@DeprecatedApi(since = "5.2", forRemoval = true, removeAt = "5.5")
public class BullseyeDetailHandler extends CotDetailHandler {

    private final MapView _mapView;

    public BullseyeDetailHandler(MapView mapView) {
        super("bullseye");
        _mapView = mapView;
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail) {
        return item instanceof Marker;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        // persist the two cases, either it is an ordinary marker with a reference to a
        // bullseye or it is a bullseye directly.
        if (!(item instanceof BullseyeMapItem)) {
            if (item.hasMetaValue("bullseyeUID")) {
                CotDetail be = new CotDetail("bullseye");
                be.setAttribute("bullseyeUID",
                        item.getMetaString("bullseyeUID", null));
                return true;
            } else {
                return false;
            }
        }

        // if it is a bullseye persist separately
        BullseyeMapItem bullseyeMapItem = (BullseyeMapItem) item;

        CotDetail be = new CotDetail("bullseye");
        // older devices process a bullseye message as a marker and require that the bullseye uid
        // be different than the bullseye message.   Can deprecate after 6 release cycles.
        be.setAttribute("bullseyeUID", bullseyeMapItem.getUID() + ".COMPAT");
        be.setAttribute("title",
                bullseyeMapItem.getMetaString("title", "bullseye1"));

        be.setAttribute("centerMarkerUID",
                item.getMetaString("centerMarkerUID", null));
        be.setAttribute("edgeMarkerUID",
                item.getMetaString("edgeMarkerUID", null));
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
    public ImportResult toItemMetadata(MapItem item, CotEvent event,
            final CotDetail detail) {

        final String bullseyeUID = detail.getAttribute("bullseyeUID");
        CotUtils.setString(item, "bullseyeUID", bullseyeUID);
        if (bullseyeUID != null && bullseyeUID.endsWith(".COMPAT")) {
            CotUtils.setString(item, "bullseyeUID",
                    bullseyeUID.replace(".COMPAT", ""));
        }

        if (!(item instanceof BullseyeMapItem) && item instanceof Marker) {
            if (bullseyeUID != null) {
                createOrUpdateBullseye(_mapView, (Marker) item, detail);
            }

            if (Boolean.parseBoolean(detail.getAttribute("mils")))
                item.setMetaBoolean("mils_mag", true);
            else
                item.setMetaBoolean("deg_mag", true);

        } else if (item instanceof BullseyeMapItem) {
            CotUtils.setBoolean(item, "rangeRingVisible",
                    detail.getAttribute("rangeRingVisible"));

            item.setTitle(detail.getAttribute("title"));
            item.setMetaString("centerMarkerUID",
                    detail.getAttribute("centerMarkerUID"));
            item.setMetaString("edgeMarkerUID",
                    detail.getAttribute("edgeMarkerUID"));

            createOrUpdateBullseye(_mapView, (BullseyeMapItem) item, detail);
        }

        return ImportResult.SUCCESS;
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
        String mils = detail.getAttribute("mils");
        boolean hasRangeRings = Boolean.parseBoolean(detail
                .getAttribute("hasRangeRings"));
        bullseye.setMetaBoolean("hasRangeRings", hasRangeRings);
        String ringDistString = detail.getAttribute("ringDist");
        String ringNumString = detail.getAttribute("ringNum");
        NorthReference bearingRef = NorthReference.MAGNETIC;
        if (detail.getAttribute("bearingRef") != null
                &&
                NorthReference
                        .findFromAbbrev(
                                detail.getAttribute("bearingRef")) != null)
            bearingRef = NorthReference.findFromAbbrev(detail
                    .getAttribute("bearingRef"));

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
