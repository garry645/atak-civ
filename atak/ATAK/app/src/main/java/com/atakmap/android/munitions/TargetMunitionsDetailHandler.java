
package com.atakmap.android.munitions;

import com.atakmap.android.cot.MarkerDetailHandler;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.Set;

import gov.tak.api.util.AttributeSet;

/**
 *
 * Class responsible from handling any target specific munitions
 */
public class TargetMunitionsDetailHandler implements MarkerDetailHandler {

    private static final String TAG = "TargetMunitionsDetailHandler";
    final static public String TARGET_MUNITIONS_VISIBLE = "target_munitions_visible";

    @Override
    public void toCotDetail(final Marker marker, final CotDetail detail) {
        if (!marker.hasMetaValue("targetMunitions"))
            return;
        CotDetail munitions = new CotDetail("targetMunitions");
        AttributeSet munitionsData = marker
                .getMetaAttributeSet("targetMunitions");
        if (munitionsData == null)
            return;

        Set<String> attributeNames = munitionsData.getAttributeNames();
        for (String attributeName : attributeNames) {
            String c = attributeName;
            AttributeSet categoryData = munitionsData
                    .getAttributeSetAttribute(attributeName);
            CotDetail category = new CotDetail("category");
            category.setAttribute("name", c);
            if (categoryData == null)
                continue;

            Set<String> attributeNames2 = categoryData.getAttributeNames();
            for (String attributeName2 : attributeNames2) {
                String w = attributeName2;
                AttributeSet weaponData = categoryData
                        .getAttributeSetAttribute(attributeName2);
                CotDetail weapon = new CotDetail("weapon");
                if (weaponData != null) {
                    weapon.setAttribute("name", w);
                    weapon.setAttribute("description",
                            weaponData.getStringAttribute("description"));
                    weapon.setAttribute("prone",
                            String.valueOf(
                                    weaponData.getIntAttribute("prone")));
                    weapon.setAttribute("standing",
                            String.valueOf(
                                    weaponData.getIntAttribute("standing")));
                    category.addChild(weapon);
                }
            }
            munitions.addChild(category);
        }
        boolean b = marker.getMetaBoolean(TARGET_MUNITIONS_VISIBLE,
                false);
        munitions.setAttribute("munitionVisibility", String.valueOf(b));
        detail.addChild(munitions);
    }

    @Override
    public void toMarkerMetadata(Marker marker, CotEvent event,
            CotDetail detail) {
        if (detail == null)
            return;
        //Log.d(TAG, "checking hostile has munitions");
        String s = detail.getAttribute("munitionVisibility");
        boolean b = Boolean.parseBoolean(s);
        marker.setMetaBoolean(TARGET_MUNITIONS_VISIBLE, b);
        //Log.d(TAG, "hostile has munitions" + munitions);
        AttributeSet munitionsData = new AttributeSet();
        for (int c = 0; c < detail.childCount(); c++) {
            AttributeSet categoryData = new AttributeSet();
            CotDetail category = detail.getChild(c);
            if (category != null) {
                String categoryName = category.getAttribute("name");
                for (int w = 0; w < category.childCount(); w++) {
                    AttributeSet weaponData = new AttributeSet();
                    CotDetail weapon = category.getChild(w);
                    if (weapon != null) {
                        String weaponName = weapon.getAttribute("name");
                        weaponData.setAttribute("name", weaponName);
                        weaponData.setAttribute("description",
                                weapon.getAttribute("description"));
                        weaponData.setAttribute("prone",
                                Integer.parseInt(weapon.getAttribute("prone")));
                        weaponData.setAttribute("standing",
                                Integer.parseInt(
                                        weapon.getAttribute("standing")));
                        weaponData.setAttribute("category", categoryName);
                        categoryData.setAttribute(weaponName, weaponData);
                        _sendRedIntent(marker, weaponName,
                                categoryName,
                                weapon.getAttribute("prone"),
                                weapon.getAttribute("standing"),
                                weapon.getAttribute("description"));
                    }
                }
                munitionsData.setAttribute(categoryName, categoryData);
            }
        }
        marker.setMetaAttributeSet("targetMunitions", munitionsData);
    }

    /**
     * Function that sends the intent to make a new range ring
     * @param marker
     * @param weaponName
     * @param categoryName
     * @param prone
     * @param standing
     * @param description
     */
    private void _sendRedIntent(Marker marker, String weaponName,
            String categoryName,
            String prone, String standing, String description) {
        try {
            int standingInt = Integer.parseInt(standing);
            int proneInt;
            if (prone == null) {
                proneInt = standingInt;
            } else {
                proneInt = Integer.parseInt(prone);
            }
            //Log.d(TAG, "received a request to show danger close: " + weaponName);
            DangerCloseReceiver.getInstance().createDangerClose(marker,
                    weaponName, categoryName, description, proneInt,
                    standingInt, false, "", false);

        } catch (NumberFormatException nfe) {
            Log.e(TAG, "exception parsing standing: " + standing + "or prone: "
                    + prone);
        }
    }
}
