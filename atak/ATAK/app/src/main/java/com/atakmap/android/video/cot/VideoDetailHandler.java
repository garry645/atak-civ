
package com.atakmap.android.video.cot;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.Disposable;
import gov.tak.platform.marshal.MarshalManager;

/**
 * Video aliases which can optionally be attached to a marker
 * <__video 
 *      url = the url for the video
 *      uid = the uid of the video link if it already is known to exist
 *      spi = the uid of the spi (useful in the case of ghosting)
 *      sensor = the uid of the sensor (useful in the case of ghosting)
 *      extplayer = a packagename to be used to launch an external player
 *      buffer = the buffer to be used (default is -1)
 *      timeout = the timeout to be used (default is 5000 ms)
 * <p>
 * WARNING
 * -------
 * marker values video_spi_uid and video_sensor_uid are now being used by 
 * the uas tool.  do not change these.
 *
 * @deprecated use {@link gov.tak.api.video.cot.VideoDetailHandler}
 */
@Deprecated
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public class VideoDetailHandler extends CotDetailHandler
        implements MapEventDispatchListener, Disposable {

    public static final String TAG = "VideoDetailHandler";

    private final MapView _mapView;

    final gov.tak.api.video.cot.VideoDetailHandler _impl;

    public VideoDetailHandler(MapView mapView) {
        super("__video");
        _mapView = mapView;
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REMOVED, this);

        _impl = new gov.tak.api.video.cot.VideoDetailHandler(
                VideoManager.getInstance());
    }

    @Override
    public void dispose() {
        _mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.ITEM_REMOVED, this);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        return toCotDetail(_impl, item, event, detail);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event,
            CotDetail detail) {
        return toItemMetadata(_impl, item, event, detail);
    }

    @Override
    public void onMapEvent(MapEvent event) {
        // Item removed - remove matching alias if it's temporary
        MapItem item = event.getItem();
        if (item == null)
            return;

        String videoUID = item.getMetaString("videoUID", null);
        if (FileSystemUtils.isEmpty(videoUID))
            return;

        ConnectionEntry ce = VideoManager.getInstance().getEntry(videoUID);
        if (ce != null && ce.isTemporary())
            VideoManager.getInstance().removeEntry(ce);
    }
}
