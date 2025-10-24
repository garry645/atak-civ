
package com.atakmap.android.cot.importer;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.Set;

/**
 * Importer for user icon markers
 */
public class UserIconMarkerImporter extends MarkerImporter {

    private final Context _context;

    public UserIconMarkerImporter(MapView mapView) {
        super(mapView, "Icons", (Set<String>) null, false);
        _context = mapView.getContext();
    }

    @Override
    public ImportResult importData(CotEvent event, Bundle extras) {
        CotDetail userIcon = event.findDetail("usericon");
        if (userIcon != null) {
            String path = userIcon.getAttribute("iconsetpath");
            if (UserIcon.IsValidIconsetPath(path, false, _context)) {
                ImportResult result = super.importData(event, extras);
                if (result == ImportResult.SUCCESS) {
                    MapItem mi = _mapView.getMapItem(event.getUID());
                    if (mi != null)
                        fixGroup(mi, extras);
                }
                return result;

            }
        }
        return ImportResult.IGNORE;
    }

    private void fixGroup(MapItem item, Bundle extras) {

        String path = item.getMetaString(UserIcon.IconsetPath, null);
        if (!UserIcon.IsValidIconsetPath(path, false, _context))
            return;
        MapGroup mapGroup = UserIcon.GetOrAddSubGroup(_group, path, _context);
        if (mapGroup == null)
            mapGroup = _group;

        super.addToGroup(item, mapGroup);

        item.removeMetaData("filterOverlayId");
        item.refresh(_mapView.getMapEventDispatcher(), extras,
                this.getClass()); // notify listeners of the change
    }
}
