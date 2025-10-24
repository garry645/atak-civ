
package com.atakmap.android.cot;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.List;

import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public class OpaqueHandler implements MarkerDetailHandler {

    static private OpaqueHandler _this;

    static {
        MarshalManager.registerMarshal(
                new AbstractMarshal(String.class, CotDetail.class) {
                    final String dummy = "<?xml version='1.0' encoding='UTF-8'?> "
                            +
                            "<event version='2.0' uid='X' type='Y' how='h-e'"
                            +
                            "time='2011-10-29T02:40:00.677Z' start='2011-10-29T02:40:00.677Z' "
                            +
                            "stale='2011-10-30T02:40:00.677Z'>"
                            +
                            "<point ce='9999999.0' le='9999999.0' hae='9999999.0' "
                            +
                            "lat='0.0' lon='0.0'/><detail>"
                            + "%s" +
                            "</detail></event>";

                    @Override
                    protected <T, V> T marshalImpl(V in) {
                        if (in instanceof String) {
                            String detail = (String) in;
                            final CotEvent ce = CotEvent
                                    .parse(String.format(dummy, detail));
                            if (ce.isValid() && ce.getDetail() != null) {
                                List<CotDetail> children = ce.getDetail()
                                        .getChildren();
                                if (!FileSystemUtils.isEmpty(children))
                                    return (T) children.get(0);
                            }
                            return null;
                        }

                        return (T) new CotDetail();
                    }
                }, String.class, CotDetail.class);
    }

    private OpaqueHandler() {
    }

    synchronized public static OpaqueHandler getInstance() {
        if (_this == null)
            _this = new OpaqueHandler();
        return _this;
    }

    @Override
    public void toCotDetail(final Marker marker, final CotDetail detail) {
        toCotDetail((MapItem) marker, detail);
    }

    public void toCotDetail(final MapItem marker, final CotDetail detail) {
        if (marker == null || !marker.hasMetaValue("opaque-details"))
            return;
        synchronized (this) {
            AttributeSet opaqueDetails = marker
                    .getMetaAttributeSet("opaque-details");
            if (opaqueDetails == null)
                return;

            for (final String name : opaqueDetails.getAttributeNames()) {
                if (opaqueDetails.getAttributeType(
                        name) == AttributeSet.AttributeType.STRING) {
                    CotDetail existing = detail.getFirstChildByName(0, name);
                    if (existing == null) {
                        final String detalString = opaqueDetails
                                .getStringAttribute(name);
                        CotDetail marshalledDetail = MarshalManager.marshal(
                                detalString, String.class, CotDetail.class);
                        detail.addChild(marshalledDetail);
                    }
                }
            }
        }
    }

    @Override
    public void toMarkerMetadata(Marker marker, CotEvent event,
            CotDetail detail) {
        toMarkerMetadata((MapItem) marker, event, detail);
    }

    /**
     * Helper function to take a marker with an unknown detail and store it as an
     * opaque detail until the appropriate plugin can be loaded to make use of it
     * @param marker the marker
     * @param event the cot event (unused in this method at this time)
     * @param detail the detail that is unknown
     */
    public void toMarkerMetadata(MapItem marker, CotEvent event,
            CotDetail detail) {

        synchronized (this) {
            AttributeSet opaqueDetails = marker
                    .getMetaAttributeSet("opaque-details");
            if (opaqueDetails == null)
                opaqueDetails = new AttributeSet();

            opaqueDetails.setAttribute(detail.getElementName(),
                    detail.toString());
            marker.setMetaAttributeSet("opaque-details", opaqueDetails);
        }
    }
}
