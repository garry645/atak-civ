
package com.atakmap.android.cot.detail;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import gov.tak.api.cot.detail.DetailConstants;

/**
 * Handler for shape 3D extrusion mode
 */
class ExtrudeModeDetailHandler extends CotDetailHandler {

    ExtrudeModeDetailHandler() {
        super(DetailConstants.EXTRUDE_MODE);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail) {
        // currently restricted to circles
        return item instanceof DrawingCircle;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        final String extrudeMode = item
                .getMetaString(DetailConstants.EXTRUDE_MODE, null);
        if (extrudeMode == null)
            return false;
        CotDetail extrudeModeDetail = new CotDetail(
                DetailConstants.EXTRUDE_MODE);
        extrudeModeDetail.setAttribute("value", extrudeMode);
        detail.addChild(extrudeModeDetail);

        return true;
    }

    @Override
    public CommsMapComponent.ImportResult toItemMetadata(MapItem item,
            CotEvent event,
            CotDetail detail) {
        Shape shape = (Shape) item;
        String name = detail.getElementName();
        String value = detail.getAttribute("value");
        if (name.equals(DetailConstants.EXTRUDE_MODE)) {
            shape.setMetaString(DetailConstants.EXTRUDE_MODE, value);
        }
        return CommsMapComponent.ImportResult.SUCCESS;
    }
}
