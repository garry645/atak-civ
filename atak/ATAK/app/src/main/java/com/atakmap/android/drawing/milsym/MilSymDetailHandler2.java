
package com.atakmap.android.drawing.milsym;

import com.atakmap.android.importexport.handlers.ParentMapItem;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Shape;

import java.util.HashSet;
import java.util.Set;

import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.symbology.MilSymDetailHandler;
import gov.tak.api.util.AttributeSet;

/**
 * Sets the "milsym":{@code id} KVP on {@link Shape} children of {@link ParentMapItem}s for
 * inbound CoT. Most "__milsym" detail handling (in both directions) is deferred to the
 * {@link MilSymDetailHandler}.
 */
final class MilSymDetailHandler2 implements ICotDetailHandler {
    private final Set<String> _detailNames;

    public MilSymDetailHandler2() {
        _detailNames = new HashSet<>();
        _detailNames.add(MilSymDetailHandler.DETAIL_MULTIPOINT);
    }

    @Override
    public Set<String> getDetailNames() {
        return _detailNames;
    }

    @Override
    public ImportResult toItemMetadata(AttributeSet attrs, CotEvent event,
            CotDetail detail) {
        String uid = attrs.getStringAttribute("uid", null);
        if (uid == null)
            return ImportResult.DEFERRED;

        MapItem mapItem = getMapItem(uid);
        if (mapItem == null)
            return ImportResult.DEFERRED;

        final String id = detail.getAttribute("id");
        if (id != null) {
            if (attrs instanceof ParentMapItem) {
                MapGroup mapGroup = ((ParentMapItem) mapItem)
                        .getChildMapGroup();
                for (MapItem child : mapGroup.getItems()) {
                    if (child instanceof Shape)
                        child.setMetaString("milsym", id);
                }
            }
        }

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(AttributeSet attrs, CotEvent event,
            CotDetail root) {
        return false;
    }

    @Override
    public boolean isSupported(AttributeSet attrs, CotEvent event,
            CotDetail detail) {
        return attrs.containsAttribute(MilSymDetailHandler.MILSYM_ATTR)
                || _detailNames.contains(detail.getElementName());
    }

    private MapItem getMapItem(String uid) {
        MapView mapView = MapView.getMapView();
        return mapView.getMapItem(uid);
    }
}
