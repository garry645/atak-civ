package gov.tak.api.video.cot;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.detail.ICotDetailHandler2;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.video.IVideoConnectionManager;

/**
 * Handles {@code <video/>} and child {@code <ConnectionEntry/>} CoT details. Sample {@code <video/>} detail:
 * <pre>
 * <__video
 *      url = the url for the video
 *      uid = the uid of the video link if it already is known to exist
 *      spi = the uid of the spi (useful in the case of ghosting)
 *      sensor = the uid of the sensor (useful in the case of ghosting)
 *      extplayer = a packagename to be used to launch an external player
 *      buffer = the buffer to be used (default is -1)
 *      timeout = the timeout to be used (default is 5000 ms)
 * </pre>
 * Contained {@code final String}s with an {@code _ATTR} suffix represent the keys this handler reads
 * from and writes to on the supplied process item's underlying {@link AttributeSet}. Some, but not
 * all CoT attribute KVPs will share the same key on the underlying {@link AttributeSet}.
 * <p/>
 * Similar to the CoT detail, the {@link AttributeSet} expected by {@link VideoDetailHandler#toCotDetail(Object, AttributeSet, CotEvent, CotDetail)}
 * and mutated by {@link VideoDetailHandler#toItemMetadata(Object, AttributeSet, CotEvent, CotDetail)}
 * will have a nested child (another {@link AttributeSet}) representing the connection entry, mapped
 * to by a {@link VideoDetailHandler#CONNECTION_ENTRY_ATTR} key.
 */
public final class VideoDetailHandler implements ICotDetailHandler2 {
    private static final String TAG = "CotDetailManager";

    public static final String DETAIL = "__video";
    public static final String CONNECTION_ENTRY_DETAIL = "ConnectionEntry";

    public static final String VIDEO_UID_ATTR = "videoUID";
    public static final String VIDEO_URL_ATTR = "videoUrl";
    public static final String VIDEO_EXT_PLAYER_ATTR = "video_extplayer";
    public static final String BUFFER_ATTR = "buffer";
    public static final String TIMEOUT_ATTR = "timeout";
    public static final String CONNECTION_ENTRY_ATTR = "connectionEntry";
    /**
     * Used by the UAS tool. Don't remove.
     */
    public static final String VIDEO_SPI_UID_ATTR = "video_spi_uid";
    /**
     * Used by the UAS tool. Don't remove.
     */
    public static final String VIDEO_SENSOR_UID_ATTR = "video_sensor_uid";

    public static final String CONNECTION_ENTRY_UID_ATTR = "uid";
    public static final String CONNECTION_ENTRY_ALIAS_ATTR = "alias";
    public static final String CONNECTION_ENTRY_ADDRESS_ATTR = "address";
    public static final String CONNECTION_ENTRY_PORT_ATTR = "port";
    public static final String CONNECTION_ENTRY_ROVER_PORT_ATTR = "roverPort";
    public static final String CONNECTION_ENTRY_PATH_ATTR = "path";
    public static final String CONNECTION_ENTRY_PROTOCOL_ATTR = "protocol";
    public static final String CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR = "networkTimeout";
    public static final String CONNECTION_ENTRY_BUFFER_TIME_ATTR = "bufferTime";
    public static final String CONNECTION_ENTRY_RTSP_RELIABLE_ATTR = "rtspReliable";
    public static final String CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR = "ignoreEmbeddedKLV";

    private final Set<String> _details;

    private IVideoConnectionManager _connectionManager;

    public VideoDetailHandler(IVideoConnectionManager connectionManager) {
        _connectionManager = connectionManager;

        _details = new HashSet<>();
        _details.add(DETAIL);
    }

    @Override
    public Set<String> getDetailNames() {
        return _details;
    }


    /**
     * @param processItem If {@code null}, no <ConnectionEntry> {@link CotDetail} will be generated.
     * @param attrs If the client doesn't register it's own {@link AttributeSet} to
     * {@link ConnectionEntryDetail} {@link gov.tak.api.marshal.IMarshal},
     *              an attempt to build a {@code <ConnectionEntry>} {@link CotDetail} will be made by
     *              checking the {@code attrs} for a nested {@link AttributeSet} containing the required
     *              attributes.
     *
     * @return
     */
    @Override
    public boolean toCotDetail(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
        if (processItem != null) {
            String videoUid = attrs.getStringAttribute(VIDEO_UID_ATTR, null);
            String videoUrl = attrs.getStringAttribute(VIDEO_URL_ATTR, null);
            boolean hasVideoUid = !FileSystemUtils.isEmpty(videoUid);
            boolean hasVideoUrl = !FileSystemUtils.isEmpty(videoUrl);
            if (!hasVideoUid && !hasVideoUrl)
                return false;

            CotDetail video = new CotDetail("__video");

            if (hasVideoUid)
                video.setAttribute("uid", videoUid);
            if (hasVideoUrl)
                video.setAttribute("url", videoUrl);
            detail.addChild(video);

            // prefer the connection entry from the manager, else use the item's metadata
            CotDetail ceDetail = null;
            if(videoUid != null && _connectionManager != null) {
                gov.tak.api.video.ConnectionEntry entry = _connectionManager.getConnectionEntry(videoUid);
                if(entry != null)
                    ceDetail = ConnectionEntryDetail.toCotDetail(entry);
            }
            if (ceDetail == null && attrs.containsAttribute(CONNECTION_ENTRY_ATTR)) {
                AttributeSet ceAttrSet = attrs.getAttributeSetAttribute(CONNECTION_ENTRY_ATTR);
                ceDetail = new CotDetail(CONNECTION_ENTRY_DETAIL);
                ceDetail.setAttribute("address", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_ADDRESS_ATTR, ""));
                ceDetail.setAttribute("uid", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_UID_ATTR, ""));
                ceDetail.setAttribute("alias", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_ALIAS_ATTR, ""));
                ceDetail.setAttribute("port", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_PORT_ATTR, "-1"));
                ceDetail.setAttribute("roverPort", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_ROVER_PORT_ATTR, "-1"));
                ceDetail.setAttribute("rtspReliable", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_RTSP_RELIABLE_ATTR, "0"));
                ceDetail.setAttribute("ignoreEmbeddedKLV", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR, "false"));
                ceDetail.setAttribute("path", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_PATH_ATTR, ""));
                ceDetail.setAttribute("protocol", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_PROTOCOL_ATTR, ""));
                ceDetail.setAttribute("networkTimeout", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR, "5000"));
                ceDetail.setAttribute("bufferTime", ceAttrSet.getStringAttribute(CONNECTION_ENTRY_BUFFER_TIME_ATTR, "-1"));
            }

            if (ceDetail != null)
                video.addChild(ceDetail);

            return true;
        }

        return false;
    }

    @Override
    public ICotDetailHandler.ImportResult toItemMetadata(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
        CotDetail ceDetail = detail.getFirstChildByName(0, CONNECTION_ENTRY_DETAIL);
        if (ceDetail != null) {
            do {
                gov.tak.api.video.ConnectionEntry entry = ConnectionEntryDetail.fromCotDetail(ceDetail);
                if(entry == null) {
                    attrs.removeAttribute(CONNECTION_ENTRY_ATTR);
                    break;
                }
                // Video requires UID
                String uid = entry.getUID();
                if (FileSystemUtils.isEmpty(uid)) {
                    uid = event.getUID();
                    if (FileSystemUtils.isEmpty(uid))
                        break;
                    entry.setUID(uid);
                }

                // XXX - derive from datamodel object?
                // update attrs
                boolean existingCeKvp = attrs.containsAttribute(CONNECTION_ENTRY_ATTR);
                AttributeSet ceAttrSet = existingCeKvp ? attrs.getAttributeSetAttribute(CONNECTION_ENTRY_ATTR) : new AttributeSet();
                pullString(ceDetail, CONNECTION_ENTRY_ADDRESS_ATTR, ceAttrSet, CONNECTION_ENTRY_ADDRESS_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_UID_ATTR, ceAttrSet, CONNECTION_ENTRY_UID_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_ALIAS_ATTR, ceAttrSet, CONNECTION_ENTRY_ALIAS_ATTR);
                pullInt(ceDetail, CONNECTION_ENTRY_PORT_ATTR, ceAttrSet, CONNECTION_ENTRY_PORT_ATTR, -1);
                pullInt(ceDetail, CONNECTION_ENTRY_ROVER_PORT_ATTR, ceAttrSet, CONNECTION_ENTRY_ROVER_PORT_ATTR, -1);
                pullInt(ceDetail, CONNECTION_ENTRY_RTSP_RELIABLE_ATTR, ceAttrSet, CONNECTION_ENTRY_RTSP_RELIABLE_ATTR, 0);
                pullString(ceDetail, CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR, ceAttrSet, CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_PATH_ATTR, ceAttrSet, CONNECTION_ENTRY_PATH_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_PROTOCOL_ATTR, ceAttrSet, CONNECTION_ENTRY_PROTOCOL_ATTR);
                pullInt(ceDetail, CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR, ceAttrSet, CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR, 5000);
                pullInt(ceDetail, CONNECTION_ENTRY_BUFFER_TIME_ATTR, ceAttrSet, CONNECTION_ENTRY_BUFFER_TIME_ATTR, -1);
                if (!existingCeKvp)
                    attrs.setAttribute(CONNECTION_ENTRY_ATTR, ceAttrSet);

                if(_connectionManager == null)
                    break;
                gov.tak.api.video.ConnectionEntry existing = _connectionManager
                        .getConnectionEntry(uid);

                // Empty URL = force removal
                if (existing != null && existing.isTemporary()
                        && FileSystemUtils.isEmpty(entry.getAddress())) {
                    _connectionManager.removeConnectionEntry(existing);
                    entry = null;
                } else if (existing == null && processItem != null
                        || existing != null && existing.isTemporary()) {
                    entry.setTemporary(true);
                    entry.setLocalFile(new File(
                            _connectionManager.getConnectionEntriesDirectory(),
                            uid + ".xml"));
                } else if (existing != null)
                    entry.setLocalFile(existing.getLocalFile());

                if (entry != null) {
                    _connectionManager.addConnectionEntry(entry);
                }
            } while(false);

            String uid = ceDetail.getAttribute("uid");
            if (FileSystemUtils.isEmpty(uid))
                uid = event.getUID();
            if (!FileSystemUtils.isEmpty(uid)) {
                boolean existingCeKvp = attrs.containsAttribute(CONNECTION_ENTRY_ATTR);
                // Video requires UID
                AttributeSet ceAttrSet = existingCeKvp ? attrs.getAttributeSetAttribute(CONNECTION_ENTRY_ATTR) : new AttributeSet();
                pullString(ceDetail, CONNECTION_ENTRY_ADDRESS_ATTR, ceAttrSet, CONNECTION_ENTRY_ADDRESS_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_UID_ATTR, ceAttrSet, CONNECTION_ENTRY_UID_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_ALIAS_ATTR, ceAttrSet, CONNECTION_ENTRY_ALIAS_ATTR);
                pullInt(ceDetail, CONNECTION_ENTRY_PORT_ATTR, ceAttrSet, CONNECTION_ENTRY_PORT_ATTR, -1);
                pullInt(ceDetail, CONNECTION_ENTRY_ROVER_PORT_ATTR, ceAttrSet, CONNECTION_ENTRY_ROVER_PORT_ATTR, -1);
                pullInt(ceDetail, CONNECTION_ENTRY_RTSP_RELIABLE_ATTR, ceAttrSet, CONNECTION_ENTRY_RTSP_RELIABLE_ATTR, 0);
                pullString(ceDetail, CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR, ceAttrSet, CONNECTION_ENTRY_IGNORE_EMBEDDED_KLV_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_PATH_ATTR, ceAttrSet, CONNECTION_ENTRY_PATH_ATTR);
                pullString(ceDetail, CONNECTION_ENTRY_PROTOCOL_ATTR, ceAttrSet, CONNECTION_ENTRY_PROTOCOL_ATTR);
                pullInt(ceDetail, CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR, ceAttrSet, CONNECTION_ENTRY_NETWORK_TIMEOUT_ATTR, 5000);
                pullInt(ceDetail, CONNECTION_ENTRY_BUFFER_TIME_ATTR, ceAttrSet, CONNECTION_ENTRY_BUFFER_TIME_ATTR, -1);
                if (!existingCeKvp)
                    attrs.setAttribute(CONNECTION_ENTRY_ATTR, ceAttrSet);
            }
        }

        if (attrs != null) {
            pullString(detail, "url", attrs, VIDEO_URL_ATTR);
            pullString(detail, "uid", attrs, VIDEO_UID_ATTR);
            pullString(detail, "spi", attrs, VIDEO_SPI_UID_ATTR);
            pullString(detail, "sensor", attrs, VIDEO_SENSOR_UID_ATTR);
            pullString(detail, "extplayer", attrs, VIDEO_EXT_PLAYER_ATTR);
            pullInt(detail, BUFFER_ATTR, attrs, BUFFER_ATTR, -1);
            pullInt(detail, TIMEOUT_ATTR, attrs, TIMEOUT_ATTR, 5000);
        }
        return ICotDetailHandler.ImportResult.SUCCESS;
    }

    @Override
    public boolean isSupported(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
        return attrs.containsAttribute(VIDEO_UID_ATTR) || attrs.containsAttribute(VIDEO_URL_ATTR) ||
                detail.getElementName().equals(DETAIL);
    }

    /**
     * Allow for the pulling of a {@code String} from a {@link CotDetail} and setting the value as part of the
     * associated {@code AttributeSet}.
     */
    private void pullString(CotDetail detail, String attributeName,
                            AttributeSet attrs, String metaDataName) {
        final String val = detail.getAttribute(attributeName);
        if (val != null) {
            attrs.setAttribute(metaDataName, val);
        } else {
            attrs.removeAttribute(metaDataName);
        }
    }

    /**
     * Allow for the pulling of a {@code int} from a {@link CotDetail} and setting the value as part of the
     * associated {@code AttributeSet}.
     */
    private void pullInt(CotDetail detail, String attributeName,
                         AttributeSet attrs, String metaDataName, int def) {
        final String val = detail.getAttribute(attributeName);
        if (val != null) {
            try {
                int vfy = Integer.parseInt(val);
                attrs.setAttribute(metaDataName, Integer.toString(vfy));
            } catch (Exception e) {
                attrs.setAttribute(metaDataName, Integer.toString(def));
                Log.e(TAG,
                        "could not parse " + attributeName + " value: " + val +
                                " defaulting to " + def);
            }
        } else {
            attrs.removeAttribute(metaDataName);
        }
    }
}
