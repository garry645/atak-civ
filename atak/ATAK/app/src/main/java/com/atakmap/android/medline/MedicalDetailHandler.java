
package com.atakmap.android.medline;

import com.atakmap.android.cot.CotUtils;
import com.atakmap.android.cot.MarkerDetailHandler;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.app.R;
import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Set;

import gov.tak.api.util.AttributeSet;

public class MedicalDetailHandler implements MarkerDetailHandler {

    public static final String TAG = "MedicalDetailHandler";

    MedicalDetailHandler() {
    }

    @Override
    public void toCotDetail(final Marker marker, final CotDetail detail) {
        if (marker.getType().startsWith("b-r-f-h-c"))
            _handleMedLineProperties(marker, detail);
    }

    static void resetMarker(final PointMapItem marker) {
        // these fields may or may not have values associated, when receive a new CoT Message,
        // clear and restart
        final String[] resetFields = new String[] {
                "urgent", "urgent_surgical", "priority", "routine",
                "convenience", //line 3 values
                "us_military", "us_civilian", "nonus_civilian", //line 8 values
                "nonus_military", "epw", "child", //line 8 values
                "litter", "ambulatory", //line 5 values
                "equipment_none", "hoist", "extraction_equipment", //line 4 values
                "ventilator", "equipment_other", //line 4 values (cont'd)
                "terrain_none", "terrain_slope", "terrain_slope_dir", //line 9 values
                "terrain_rough", "terrain_loose", "terrain_other" //line 9 values (cont'd)
        };
        for (String field : resetFields)
            marker.removeMetaData(field);
    }

    @Override
    public void toMarkerMetadata(final Marker marker,
            final CotEvent event,
            final CotDetail detail) {

        resetMarker(marker);

        String line9 = detail.getAttribute("obstacles");
        CotUtils.setString(marker, "obstacles", line9);

        for (CotAttribute attr : detail.getAttributes()) {
            if (attr.getValue().equalsIgnoreCase("true")) {
                marker.setMetaBoolean(attr.getName(), true);
            } else if (attr.getValue().equalsIgnoreCase("false")) {
                marker.setMetaBoolean(attr.getName(), false);
            } else if (!attr.getName().equals("zMists")) {
                marker.setMetaString(attr.getName(), attr.getValue());
            }
        }

        CotDetail zMists = detail.getFirstChildByName(0, "zMistsMap");
        if (zMists != null) {
            AttributeSet zMistsData = new AttributeSet();
            for (int c = 0; c < zMists.childCount(); c++) {
                CotDetail zMist = zMists.getChild(c);
                String title = zMist.getAttribute("title");
                AttributeSet entryData = new AttributeSet();
                entryData.setAttribute("z", zMist.getAttribute("z"));
                entryData.setAttribute("m", zMist.getAttribute("m"));
                entryData.setAttribute("i", zMist.getAttribute("i"));
                entryData.setAttribute("s", zMist.getAttribute("s"));
                entryData.setAttribute("t", zMist.getAttribute("t"));
                zMistsData.setAttribute(title, entryData);
            }
            marker.setMetaAttributeSet("zMists", zMistsData);
        }
    }

    private static void _handleMedLineProperties(final Marker marker,
            final CotDetail detail) {

        Log.d(TAG,
                "special handling med9line for mapitem ["
                        + marker.getUID()
                        + " ]: "
                        +
                        marker.getMetaString(
                                "callsign",
                                MapView.getMapView().getContext()
                                        .getString(R.string.missing_callsign)));

        CotDetail medTags = new CotDetail();
        medTags.setElementName("_flow-tags_");
        medTags.setAttribute("AndroidMedicalLine",
                new CoordinatedTime().toString());
        detail.addChild(medTags);

        CotUtils.checkSetString(medTags, marker, "obstacles",
                "obstacles");

        CotDetail medLine = new CotDetail();
        medLine.setElementName("_medevac_");

        medLine.setAttribute("casevac",
                String.valueOf(marker.getMetaBoolean("casevac", false)));
        //Title
        if (marker.getMetaString("title", null) != null)
            medLine.setAttribute("title",
                    marker.getMetaString("title", null));

        if (marker.getMetaString("medline_remarks", null) != null)
            medLine.setAttribute("medline_remarks",
                    marker.getMetaString("medline_remarks", null));

        //Line 2 Freq Value
        if (marker.getMetaString("freq", null) != null)
            medLine.setAttribute("freq",
                    marker.getMetaString("freq", null));

        //Line 3 Values
        if (marker.getMetaString("urgent", null) != null)
            medLine.setAttribute("urgent",
                    marker.getMetaString("urgent", null));
        if (marker.getMetaString("urgent_surgical", null) != null)
            medLine.setAttribute("urgent_surgical",
                    marker.getMetaString("urgent_surgical", null));
        if (marker.getMetaString("priority", null) != null)
            medLine.setAttribute("priority",
                    marker.getMetaString("priority", null));
        if (marker.getMetaString("routine", null) != null)
            medLine.setAttribute("routine",
                    marker.getMetaString("routine", null));
        if (marker.getMetaString("convenience", null) != null)
            medLine.setAttribute("convenience",
                    marker.getMetaString("convenience", null));

        //Line 4 Values
        if (marker.getMetaBoolean("equipment_none", false))
            medLine.setAttribute("equipment_none", "true");
        if (marker.getMetaBoolean("hoist", false))
            medLine.setAttribute("hoist", "true");
        if (marker.getMetaBoolean("extraction_equipment", false))
            medLine.setAttribute("extraction_equipment", "true");
        if (marker.getMetaBoolean("ventilator", false))
            medLine.setAttribute("ventilator", "true");
        if (marker.getMetaBoolean("equipment_other", false))
            medLine.setAttribute("equipment_other", "true");
        if (marker.getMetaString("equipment_detail", null) != null)
            medLine.setAttribute("equipment_detail",
                    marker.getMetaString("equipment_detail", null));

        //Line 5 Values
        if (marker.getMetaString("litter", null) != null)
            medLine.setAttribute("litter",
                    marker.getMetaString("litter", null));
        if (marker.getMetaString("ambulatory", null) != null)
            medLine.setAttribute("ambulatory",
                    marker.getMetaString("ambulatory", null));

        //Line 6 Values
        if (marker.getMetaString("security", null) != null)
            medLine.setAttribute("security",
                    marker.getMetaString("security", null));

        //Line 7 Values
        if (marker.getMetaString("hlz_marking", null) != null)
            medLine.setAttribute("hlz_marking",
                    marker.getMetaString("hlz_marking", null));
        if (marker.getMetaString("hlz_other", null) != null)
            medLine.setAttribute("hlz_other",
                    marker.getMetaString("hlz_other", null));

        //Line 8 Values
        if (marker.getMetaString("us_military", null) != null)
            medLine.setAttribute("us_military",
                    marker.getMetaString("us_military", null));
        if (marker.getMetaString("us_civilian", null) != null)
            medLine.setAttribute("us_civilian",
                    marker.getMetaString("us_civilian", null));
        if (marker.getMetaString("nonus_civilian", null) != null)
            medLine.setAttribute("nonus_civilian",
                    marker.getMetaString("nonus_civilian", null));
        if (marker.getMetaString("nonus_military", null) != null)
            medLine.setAttribute("nonus_military",
                    marker.getMetaString("nonus_military", null));
        if (marker.getMetaString("epw", null) != null)
            medLine.setAttribute("epw", marker.getMetaString("epw", null));
        if (marker.getMetaString("child", null) != null)
            medLine.setAttribute("child", marker.getMetaString("child", null));

        //Line 9 Values
        if (marker.getMetaBoolean("terrain_none", false))
            medLine.setAttribute("terrain_none", "true");
        if (marker.getMetaBoolean("terrain_slope", false))
            medLine.setAttribute("terrain_slope", "true");
        if (marker.getMetaBoolean("terrain_rough", false))
            medLine.setAttribute("terrain_rough", "true");
        if (marker.getMetaBoolean("terrain_loose", false))
            medLine.setAttribute("terrain_loose", "true");
        if (marker.getMetaBoolean("terrain_other", false))
            medLine.setAttribute("terrain_other", "true");
        if (marker.getMetaString("terrain_other_detail", null) != null)
            medLine.setAttribute("terrain_other_detail",
                    marker.getMetaString("terrain_other_detail", null));

        if (marker.hasMetaValue("terrain_slope_dir")) {
            medLine.setAttribute("terrain_slope_dir",
                    marker.getMetaString("terrain_slope_dir", "N"));
        }

        //ZMIST Data
        if (marker.hasMetaValue("zMists")) {
            CotDetail zMists = new CotDetail("zMistsMap");
            AttributeSet zMistsData = marker.getMetaAttributeSet("zMists");
            if (zMistsData != null) {
                Set<String> attrNames = zMistsData.getAttributeNames();
                for (String entry : attrNames) {
                    AttributeSet zMistAttrs = zMistsData
                            .getAttributeSetAttribute(entry);
                    if (zMistAttrs != null) {
                        CotDetail zMist = new CotDetail("zMist");
                        zMist.setAttribute("title", entry);
                        zMist.setAttribute("z",
                                toString(
                                        zMistAttrs.getStringAttribute("z"),
                                        ""));
                        zMist.setAttribute("m",
                                toString(
                                        zMistAttrs.getStringAttribute("m"),
                                        ""));
                        zMist.setAttribute("i",
                                toString(
                                        zMistAttrs.getStringAttribute("i"),
                                        ""));
                        zMist.setAttribute("s",
                                toString(
                                        zMistAttrs.getStringAttribute("s"),
                                        ""));
                        zMist.setAttribute("t",
                                toString(
                                        zMistAttrs.getStringAttribute("t"),
                                        ""));
                        zMists.addChild(zMist);
                    }
                }
                medLine.addChild(zMists);
            }
        }

        //HLZ values
        if (marker.getMetaString("zone_protected_coord", null) != null)
            medLine.setAttribute("zone_protected_coord",
                    marker.getMetaString("zone_protected_coord", null));

        if (marker.getMetaString("zone_prot_marker", null) != null) {
            medLine.setAttribute("zone_prot_marker",
                    marker.getMetaString("zone_prot_marker", null));
        }
        if (marker.getMetaString("zone_prot_selection", null) != null) {
            medLine.setAttribute("zone_prot_selection",
                    marker.getMetaString("zone_prot_selection", null));
        }
        if (marker.getMetaString("marked_by", null) != null)
            medLine.setAttribute("marked_by",
                    marker.getMetaString("marked_by", null));
        if (marker.getMetaString("obstacles", null) != null)
            medLine.setAttribute("obstacles",
                    marker.getMetaString("obstacles", null));
        if (marker.getMetaString("winds_are_from", null) != null)
            medLine.setAttribute("winds_are_from",
                    marker.getMetaString("winds_are_from", null));
        if (marker.getMetaString("friendlies", null) != null)
            medLine.setAttribute("friendlies",
                    marker.getMetaString("friendlies", null));
        if (marker.getMetaString("enemy", null) != null)
            medLine.setAttribute("enemy",
                    marker.getMetaString("enemy", null));
        if (marker.getMetaString("hlz_remarks", null) != null)
            medLine.setAttribute("hlz_remarks",
                    marker.getMetaString("hlz_remarks", null));

        detail.addChild(medLine);

    }

    /**
     * Call the object toString() or if the object is null return the fallback.
     * @param o the object to stringify
     * @param fallback the value to fallback to if the object is null
     * @return the corresponding string
     */
    static String toString(Object o, String fallback) {
        if (o == null)
            return fallback;
        return o.toString();
    }

}
