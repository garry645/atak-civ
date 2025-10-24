
package com.atakmap.android.cot.detail;

import com.atakmap.android.maps.MapItem;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * Responsible for setting a marker to be archived if the archive tag is encountered.
 * <archive/>
 */
public class CreatorDetailHandler extends CotDetailHandler {

    public CreatorDetailHandler() {
        super("creator");
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        if (item.hasMetaValue("parent_uid")) {

            CotDetail cd = new CotDetail("creator");
            setDetail(cd, "uid",
                    item.getMetaString("parent_uid", null));
            setDetail(cd, "type",
                    item.getMetaString("parent_type", null));
            setDetail(cd, "callsign",
                    item.getMetaString("parent_callsign", null));
            setDetail(cd, "time",
                    item.getMetaString("production_time", null));
            detail.addChild(cd);
            return true;
        }
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event,
            CotDetail detail) {

        final String parent_uid = detail.getAttribute("uid");
        final String parent_type = detail.getAttribute("type");
        final String parent_callsign = detail.getAttribute("callsign");
        final String production_time = detail.getAttribute("time");

        setOrRemove(item, "parent_uid", parent_uid);
        setOrRemove(item, "parent_type", parent_type);
        setOrRemove(item, "parent_callsign", parent_callsign);
        setOrRemove(item, "production_time", production_time);
        return ImportResult.SUCCESS;
    }

    private void setOrRemove(MapItem item, String key, String val) {
        if (!FileSystemUtils.isEmpty(val))
            item.setMetaString(key, val);
        else
            item.removeMetaData(key);
    }

    private void setDetail(CotDetail cd, String key, String val) {
        if (!FileSystemUtils.isEmpty(val)) {
            cd.setAttribute(key, val);
        }
    }
}
