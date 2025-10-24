
package com.atakmap.android.video.cot;

import android.os.Bundle;

import com.atakmap.android.cot.importer.MarkerImporter;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

/**
 * Importer for video aliases
 */
public class VideoAliasImporter extends MarkerImporter {

    private final gov.tak.api.video.cot.VideoDetailHandler _handler;

    /** @deprecated use {@link #VideoAliasImporter(MapView, gov.tak.api.video.cot.VideoDetailHandler)}  */
    public VideoAliasImporter(MapView mapView, VideoDetailHandler handler) {
        super(mapView, (MapGroup) null, "b-i-v", true);
        _handler = handler._impl;
    }

    public VideoAliasImporter(MapView mapView,
            gov.tak.api.video.cot.VideoDetailHandler handler) {
        super(mapView, (MapGroup) null, "b-i-v", true);
        _handler = handler;
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event,
            Bundle extras) {
        // We only care about the details for this type of "marker"
        CotDetail video = event.findDetail("__video");
        if (video == null)
            return ImportResult.FAILURE;
        VideoDetailHandler.toItemMetadata(_handler, existing, event, video);
        extras.putShort("CotMapProcessed", (short) 1);
        return ImportResult.SUCCESS;
    }
}
