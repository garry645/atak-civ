
package com.atakmap.android.cot.detail;

import com.atakmap.android.maps.MapItem;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.Set;

import gov.tak.api.util.AttributeSet;

/**
 * Key for storing generic item metadata
 */
public class MetaDetailHandler extends CotDetailHandler {

    MetaDetailHandler() {
        super("meta");
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        CotDetail meta = new CotDetail("meta");
        AttributeSet m = item.getMetaAttributeSet("meta");
        if (m != null) {
            Set<String> entryKeys = m.getAttributeNames();
            for (String k : entryKeys) {
                if (m.getAttributeValueType(k) == String.class) {
                    String v = m.getStringAttribute(k);
                    CotDetail entry = new CotDetail();
                    entry.setAttribute("key", k);
                    entry.setAttribute("value", v);
                    meta.addChild(entry);
                }
            }
            detail.addChild(meta);
            return true;
        }
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event,
            CotDetail detail) {
        if (item == null)
            return ImportResult.FAILURE;

        AttributeSet m = new AttributeSet();

        int len = detail.childCount();
        for (int i = 0; i < len; ++i) {
            CotDetail cd = detail.getChild(i);
            String key = cd.getAttribute("key");
            String value = cd.getAttribute("value");
            if (key != null && value != null)
                m.setAttribute(key, value);

        }
        if (!m.getAttributeNames().isEmpty()) {
            item.setMetaAttributeSet("meta", m);
        } else
            item.removeMetaData("meta");
        return ImportResult.SUCCESS;
    }
}
