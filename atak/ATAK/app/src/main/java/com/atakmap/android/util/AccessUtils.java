
package com.atakmap.android.util;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.cot.event.CotEvent;

public class AccessUtils {

    public static final String MAPITEM_ACCESS_PREF_KEY = "mapitem.access";
    public static final String MAPITEM_CAVEAT_PREF_KEY = "mapitem.caveat";
    public static final String MAPITEM_RELTO_PREF_KEY = "mapitem.releasableTo";

    public static final String GEOCHAT_ACCESS_PREF_KEY = "geochat.access";
    public static final String GEOCHAT_CAVEAT_PREF_KEY = "geochat.caveat";
    public static final String GEOCHAT_RELTO_PREF_KEY = "geochat.releasableTo";

    /**
     * Sets the access default for a given map item if nothing is set.
     * @param item the provided map item
     */
    public static void setAccessDefault(MapItem item) {
        // basic settings that will / can be overriden by users of the Marker class
        AtakPreferences _prefs = AtakPreferences.getInstance(null);
        if (!item.hasMetaValue("access"))
            item.setMetaString("access",
                    _prefs.get(MAPITEM_ACCESS_PREF_KEY, "Undefined"));

        if (!item.hasMetaValue("caveat"))
            item.setMetaString("caveat",
                    _prefs.get(MAPITEM_CAVEAT_PREF_KEY, null));

        if (!item.hasMetaValue("releasableTo"))
            item.setMetaString("releasableTo",
                    _prefs.get(MAPITEM_RELTO_PREF_KEY, null));

    }

    /**
     * Given a Cot Event and a map item, set the map items values to reflect the cot event.
     * @param cotEvent the cot event
     * @param item the item to populate.
     */
    public static void setAccessDefault(CotEvent cotEvent, MapItem item) {
        AtakPreferences _prefs = AtakPreferences.getInstance(null);
        cotEvent.setAccess(item.getMetaString("access",
                _prefs.get("mapitem.access", "Undefined")));
        cotEvent.setCaveat(item.getMetaString("caveat",
                _prefs.get("mapitem.caveat", null)));
        cotEvent.setReleasableTo(item.getMetaString("releasableTo",
                _prefs.get("mapitem.releasableTo", null)));
    }

    /**
     * Given a CoT Event with no other information, pull the appropriate access, caveat, and rel to
     * from the preferences
     * @param cotEvent the cot event to populate
     */
    public static void setAccessDefault(CotEvent cotEvent) {
        AtakPreferences _prefs = AtakPreferences.getInstance(null);
        cotEvent.setAccess(_prefs.get("mapitem.access", "Undefined"));
        cotEvent.setCaveat(_prefs.get("mapitem.caveat", null));
        cotEvent.setReleasableTo(_prefs.get("mapitem.releasableTo", null));
    }

    /**
     * Given a CoT Event, set the values for the map item
     * @param event the cot event to use
     * @param item the map item to populate
     */
    public static void setAccessFromCot(CotEvent event, MapItem item) {
        item.setMetaString("access", event.getAccess());
        item.setMetaString("caveat", event.getCaveat());
        item.setMetaString("releasableTo", event.getReleasableTo());
    }

}
