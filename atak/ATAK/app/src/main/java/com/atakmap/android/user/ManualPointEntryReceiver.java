
package com.atakmap.android.user;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class ManualPointEntryReceiver extends BroadcastReceiver {

    private final MapView mapView;

    public ManualPointEntryReceiver(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public void onReceive(final Context ignoreCtx, final Intent intent) {
        final Context context = mapView.getContext();

        MapItem item = this.mapView.getMapItem(intent.getStringExtra("uid"));

        if (!(item instanceof PointMapItem))
            return; // Shouldn't happen!

        final PointMapItem pointItem = (PointMapItem) item;

        final GeoPointMetaData point = pointItem.getGeoPointMetaData();

        CoordinateEntryCapability.getInstance(context).showDialog(
                "",
                point,
                true, mapView.getPoint(), null, false,
                new CoordinateEntryCapability.ResultCallback() {
                    @Override
                    public void onResultCallback(String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {
                        String type;
                        type = pointItem.getType();
                        switch (type) {
                            case "corner_u-d-r": {
                                Intent intent = new Intent(
                                        "com.atakmap.android.maps.MANUAL_POINT_RECTANGLE_EDIT");
                                intent.putExtra("type", "corner_u-d-r");
                                intent.putExtra("uid", pointItem.getUID());
                                intent.putExtra("lat",
                                        String.valueOf(point.get()
                                                .getLatitude()));
                                intent.putExtra("lon",
                                        String.valueOf(point.get()
                                                .getLongitude()));
                                intent.putExtra("alt",
                                        String.valueOf(point.get()
                                                .getAltitude()));
                                intent.putExtra("from", "manualPointEntry");
                                AtakBroadcast.getInstance().sendBroadcast(
                                        intent);
                                break;
                            }
                            case "side_u-d-r": {
                                Intent intent = new Intent(
                                        "com.atakmap.android.maps.MANUAL_POINT_RECTANGLE_EDIT");
                                intent.putExtra("type", "side_u-d-r");
                                intent.putExtra("uid", pointItem.getUID());
                                intent.putExtra("lat",
                                        String.valueOf(point.get()
                                                .getLatitude()));
                                intent.putExtra("lon",
                                        String.valueOf(point.get()
                                                .getLongitude()));
                                intent.putExtra("alt",
                                        String.valueOf(point.get()
                                                .getAltitude()));
                                intent.putExtra("from", "manualPointEntry");
                                AtakBroadcast.getInstance().sendBroadcast(
                                        intent);
                                break;
                            }
                            default:
                                pointItem.setPoint(point);
                                break;
                        }
                    }
                });
    }

    // Start the tool off with MGRS and save the latest used style as the user goes along
    // TODO persist this setting on restart?

}
