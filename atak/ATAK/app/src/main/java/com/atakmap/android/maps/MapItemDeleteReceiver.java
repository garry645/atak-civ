
package com.atakmap.android.maps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.metrics.MetricsUtils;

public class MapItemDeleteReceiver extends BroadcastReceiver {

    private final MapGroup root;

    public MapItemDeleteReceiver(MapView mapView) {
        this.root = mapView.getRootGroup();
    }

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        MapItem toRemove = null;
        if (arg1.hasExtra("serialId")) {
            final long serialId = arg1.getLongExtra("serialId", -1L);
            toRemove = MapGroup.deepFindItemWithSerialId(this.root, serialId);
        } else if (arg1.hasExtra("metaKey") && arg1.hasExtra("metaValue")) {
            final String key = arg1.getStringExtra("metaKey");
            final String value = arg1.getStringExtra("metaValue");
            toRemove = MapGroup.deepFindItemWithMetaString(this.root, key,
                    value);
        }
        if (toRemove != null) {
            toRemove.removeFromGroup();
            MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                    MetricsUtils.EVENT_MAPITEM_REMOVED, "MapItemDeleteReceiver",
                    toRemove, MetricsUtils.EVENT_STATUS_SUCCESS);
        }
    }
}
