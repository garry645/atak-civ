
package com.atakmap.android.cot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.cotdetails.CoTInfoBroadcastReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;

class RadialMenuSendReceiver extends BroadcastReceiver {

    public static final String SEND_RADIAL_INTENT = "com.atakmap.android.cotdetails.SEND_RADIAL_INTENT";

    private static final String TAG = "RadialMenuSendReceiver";

    private final MapView _mapView;

    RadialMenuSendReceiver(MapView mapView) {
        _mapView = mapView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action == null || !action.equals(SEND_RADIAL_INTENT))
            return;

        //get uid of mapitem we will extract and find out what it is by the uid
        String uid = intent.getStringExtra("uid");

        if (!FileSystemUtils.isEmpty(uid)) {
            // Make sure the object is shared since the user hit "Send".
            final MapItem item = _mapView.getMapItem(uid);

            if (item == null)
                return;

            if (item instanceof PointMapItem || item instanceof Shape) {
                send(item);
            }
        }
    }

    private void send(MapItem mapItem) {

        if (mapItem == null)
            return;

        // Prompt the user to include marker attachments if any exist
        CoTInfoBroadcastReceiver.promptSendAttachments(mapItem, null, null,
                new Runnable() {
                    @Override
                    public void run() {
                        // Just send CoT marker
                        sendCoT(mapItem);
                    }
                });
    }

    protected void sendCoT(final MapItem mapItem) {
        if (mapItem == null)
            return;

        final MapItem itemToSend = ATAKUtilities.findAssocShape(mapItem);
        if (itemToSend != null) {
            final String uid = itemToSend.getUID();

            // there are still some single point icons that don't have shared set that are sharable
            // see b-m-p-w-GOTO
            itemToSend.setMetaBoolean("shared", true);

            final Intent contactList = new Intent(
                    ContactPresenceDropdown.SEND_LIST);
            contactList.putExtra("targetUID", uid);
            AtakBroadcast.getInstance().sendBroadcast(contactList);
        }
    }
}
