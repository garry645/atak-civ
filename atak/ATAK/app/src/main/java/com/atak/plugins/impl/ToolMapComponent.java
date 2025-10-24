
package com.atak.plugins.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ViewGroup;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.menu.ActionBroadcastData;
import com.atakmap.android.tools.menu.ActionClickData;
import com.atakmap.android.tools.menu.ActionMenuData;
import com.atakmap.coremap.log.Log;

import java.util.Collections;
import java.util.List;

import gov.tak.platform.ui.MotionEvent;

/**
 * Map Component that manages the loading of a IToolbarItem.
 */
final class ToolMapComponent extends AbstractMapComponent {

    public static final String TAG = "ToolMapComponent";

    final IToolbarItem ITool;
    private final String packageName;
    private MapView mapView;
    BroadcastReceiver receiver;
    ActionMenuData actionMenuData;

    /**
     * Construct a tool map component
     * @param ITool the toolbar item
     * @param packageName the associated package name
     */
    public ToolMapComponent(final IToolbarItem ITool,
            final String packageName) {
        this.ITool = ITool;
        this.packageName = packageName;
    }

    @Override
    public synchronized void onCreate(final Context context,
            final Intent intent,
            final MapView mapViewFinal) {

        this.mapView = mapViewFinal;

        if (receiver == null)
            loadToolDescriptorPlugin(ITool, packageName);
    }

    @Override
    protected synchronized void onDestroyImpl(Context context, MapView view) {
        if (receiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(receiver);
            receiver = null;
        }
        if (actionMenuData != null) {
            Intent intent = new Intent(ActionBarReceiver.REMOVE_TOOLS);
            intent.putExtra("menus", new ActionMenuData[] {
                    actionMenuData
            });
            AtakBroadcast.getInstance().sendBroadcast(intent);
            actionMenuData = null;
        }
    }

    private synchronized void loadToolDescriptorPlugin(
            IToolbarItem ITool, String packageName) {
        try {
            Log.d(TAG, "Loading new IToolbarItem plugin: " + ITool);
            final String shortDesc = ITool.getShortDescription();
            if (shortDesc != null) {
                final String intentAction = "com.atak.plugin.selected." +
                        packageName + "." + ITool.getClass();

                receiver = createReceiver();
                AtakBroadcast.getInstance().registerReceiver(receiver,
                        new AtakBroadcast.DocumentedIntentFilter(intentAction));

                actionMenuData = createActionMenuData();
                Intent intent = new Intent(ActionBarReceiver.ADD_NEW_TOOLS);
                intent.putExtra("menus", new ActionMenuData[] {
                        actionMenuData
                });
                AtakBroadcast.getInstance().sendBroadcast(intent);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Exception while loading tool plugin "
                    + ITool, ex);
        }
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context pluginContext, Intent intent) {
                Log.d(TAG, "Received intent to open IToolbarItem "
                        + ITool.getShortDescription());

                Bundle extrasToSendToPlugin = (intent.getExtras() != null)
                        ? intent.getExtras()
                        : new Bundle();

                final ViewGroup toDisplay = new android.widget.LinearLayout(
                        pluginContext);

                // Tell the plugin that it's activated!
                try {
                    final long time = SystemClock.uptimeMillis();
                    MotionEvent evt = MotionEvent.obtain(
                            time, time, MotionEvent.ACTION_UP, 0, 0, 0);
                    ITool.onItemEvent(evt);
                } catch (Exception e) {
                    Log.w(TAG, "Problem executing IToolbarItem plugin "
                            + ITool, e);
                }
            }
        };
    }

    private ActionMenuData createActionMenuData() {
        final String intentAction = "com.atak.plugin.selected." +
                packageName + "." + ITool.getClass();

        String iconId = PluginMapComponent.addPluginIcon(ITool);

        List<ActionClickData> temp = Collections
                .singletonList(new ActionClickData(
                        new ActionBroadcastData(intentAction, null),
                        ActionClickData.CLICK));

        return new ActionMenuData(
                "plugin://" + packageName + "/"
                        + ITool.getClass().getName(),
                ITool.getShortDescription(), iconId, iconId, iconId,
                "overflow", true, temp, false, false, false);
    }
}
