
package com.atakmap.android.firstperson;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigation.NavButtonManager;
import com.atakmap.android.navigation.models.NavButtonModel;

public class FirstPersonMapComponent extends AbstractMapComponent {

    FirstPersonReceiver receiver = null;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {

        AtakBroadcast.DocumentedIntentFilter filter = new AtakBroadcast.DocumentedIntentFilter();
        filter.addAction(FirstPersonReceiver.FIRSTPERSON);
        filter.addAction(FirstPersonReceiver.STREET_VIEW);
        filter.addAction(FirstPersonReceiver.STREET_VIEW_MAP_CLICKED);
        AtakBroadcast.getInstance().registerReceiver(
                receiver = new FirstPersonReceiver(view), filter);

        // Remove First Person from the MPU5 specifically
        if (Build.MODEL.equals("MPU5")) {
            NavButtonModel buttonModel = NavButtonManager.getInstance()
                    .getModelByReference("firstperson.xml");
            NavButtonManager.getInstance().removeButtonModel(buttonModel);
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (receiver != null)
            AtakBroadcast.getInstance().unregisterReceiver(receiver);
    }

}
