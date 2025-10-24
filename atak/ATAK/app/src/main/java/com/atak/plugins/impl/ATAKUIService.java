
package com.atak.plugins.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.menu.ActionBroadcastData;
import com.atakmap.android.tools.menu.ActionClickData;
import com.atakmap.android.tools.menu.ActionMenuData;
import com.atakmap.android.util.NotificationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;

class ATAKUIService implements IHostUIService {

    final MapView mapView;

    final Map<Pane, UIServiceDropDownReceiver> dropDownReceiverMap = new HashMap<>();

    final Map<String, BroadcastReceiver> toolbarItemReceiverMap = new HashMap<>();

    final Map<String, ActionMenuData> actionMenuDataMap = new HashMap<>();

    public ATAKUIService(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public void showPane(Pane pane, IPaneLifecycleListener listener) {

        final UIServiceDropDownReceiver receiver;
        if (dropDownReceiverMap.containsKey(pane)) {
            receiver = dropDownReceiverMap.get(pane);
            if (receiver != null && !receiver.isVisible())
                receiver.show();
            return;
        } else {
            receiver = new UIServiceDropDownReceiver(mapView);
            dropDownReceiverMap.put(pane, receiver);
        }
        // configure _retain_ based on hint; default to `false`
        receiver.setRetain(pane.getMetaBoolean(Pane.RETAIN, Boolean.FALSE));

        DropDown.OnStateListener stateListener = new DropDown.OnStateListener() {
            @Override
            public void onDropDownSelectionRemoved() {

            }

            @Override
            public void onDropDownClose() {
                if (listener != null)
                    listener.onPaneClose();
                removeFromCache(pane);
                receiver.dispose();
            }

            @Override
            public void onDropDownSizeChanged(double width, double height) {

            }

            @Override
            public void onDropDownVisible(boolean v) {
                if (listener != null)
                    listener.onPaneVisible(v);
            }
        };

        final Pane.Location paneLocation = pane.getMetaValue(
                Pane.RELATIVE_LOCATION, Pane.Location.class,
                Pane.Location.Default);

        double lwFraction;
        double lhFraction;
        double pwFraction;
        double phFraction;

        switch (paneLocation) {
            case Default:
            case Left:
            case Right:
                // Per `IPane.Location.Default`, the pane will switch relative location with
                // orientation and undergo resizing to fill the screen per the current orientation's
                // minor axis

                // landscape
                lwFraction = getPreferredPaneWidth(pane,
                        DropDownReceiver.THREE_EIGHTHS_WIDTH,
                        mapView.isPortrait() ? mapView.getWidth()
                                : mapView.getHeight());
                lhFraction = DropDownReceiver.FULL_HEIGHT;
                // portrait
                pwFraction = DropDownReceiver.FULL_WIDTH;
                phFraction = getPreferredPaneHeight(pane,
                        DropDownReceiver.HALF_HEIGHT,
                        mapView.isPortrait() ? mapView.getHeight()
                                : mapView.getWidth());
                break;
            case Bottom:
                // Per `IPane.Location.Right`, the pane will remain on the side regardless of
                // current orientation

                // landscape
                lwFraction = DropDownReceiver.FULL_WIDTH;
                lhFraction = getPreferredPaneHeight(pane,
                        DropDownReceiver.THREE_EIGHTHS_HEIGHT,
                        mapView.isPortrait() ? mapView.getWidth()
                                : mapView.getHeight());
                // portrait
                pwFraction = DropDownReceiver.FULL_WIDTH;
                phFraction = getPreferredPaneHeight(pane,
                        DropDownReceiver.HALF_HEIGHT,
                        mapView.isPortrait() ? mapView.getHeight()
                                : mapView.getWidth());
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (MarshalManager.marshal(pane, Pane.class, Fragment.class) != null) {
            Fragment fragment = MarshalManager.marshal(pane, Pane.class,
                    Fragment.class);
            receiver.showDropDown(fragment,
                    lwFraction,
                    lhFraction,
                    pwFraction,
                    phFraction,
                    false,
                    false,
                    stateListener);
        }
    }

    @Override
    public void closePane(Pane pane) {
        DropDownReceiver receiver = dropDownReceiverMap.get(pane);
        if (receiver == null)
            return;
        receiver.closeDropDown();
        removeFromCache(pane);
    }

    @Override
    public boolean isPaneVisible(Pane pane) {
        DropDownReceiver receiver = dropDownReceiverMap.get(pane);
        if (receiver == null)
            return false;
        return receiver.isVisible();
    }

    @Override
    public void queueEvent(Runnable runnable) {
        mapView.postOnActive(runnable);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(mapView.getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showPrompt(String prompt) {
        TextContainer.getInstance().displayPrompt(prompt);
    }

    @Override
    public void clearPrompt() {
        TextContainer.getInstance().closePrompt();
    }

    @Override
    public void showNotification(NotificationStatus status, String message,
            Throwable exception) {
        NotificationUtil.NotificationColor color = NotificationUtil.GREEN;
        int icon;
        switch (status) {
            case Error:
                icon = NotificationUtil.GeneralIcon.STATUS_DOT_RED.getID();
                break;
            case Warning:
                icon = NotificationUtil.GeneralIcon.STATUS_DOT_YELLOW.getID();
                break;
            case Information:
            default:
                icon = NotificationUtil.GeneralIcon.STATUS_DOT_GREEN.getID();
                break;
        }
        NotificationUtil.getInstance().postNotification(icon, color, message,
                "",
                exception == null ? "" : exception.toString());
    }

    @Override
    public void addToolbarItem(ToolbarItem toolbarItem) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                addToolbarItemImpl(toolbarItem);
            }
        });
    }

    private void addToolbarItemImpl(ToolbarItem toolbarItem) {
        String toolbarItemId = toolbarItem.getIdentifier();
        String toolbarAction = toolbarItemId + ".ACTION";

        ActionBroadcastData abd = new ActionBroadcastData(
                toolbarAction,
                new ArrayList<>());

        List<ActionClickData> acdList = new ArrayList<>();
        acdList.add(new ActionClickData(abd, "click"));
        String embeddedIcon = PluginMapComponent.addPluginIcon(
                toolbarItem.getIdentifier(),
                MarshalManager.marshal(
                        toolbarItem.getIcon(),
                        gov.tak.api.commons.graphics.Drawable.class,
                        android.graphics.drawable.Drawable.class));

        final ActionMenuData amd = new ActionMenuData(
                toolbarItem.getIdentifier(),
                toolbarItem.getTitle(), embeddedIcon, embeddedIcon,
                embeddedIcon, "overflow",
                false, acdList, false, false, false);

        BroadcastReceiver toolbarItemReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (toolbarItem.getListener() != null) {
                    final long time = SystemClock.uptimeMillis();
                    MotionEvent evt = MotionEvent.obtain(
                            time, time, MotionEvent.ACTION_UP, 0, 0, 0);
                    toolbarItem.getListener().onItemEvent(toolbarItem, evt);
                }
            }
        };

        AtakBroadcast.getInstance().registerReceiver(toolbarItemReceiver,
                new AtakBroadcast.DocumentedIntentFilter(toolbarAction));
        toolbarItemReceiverMap.put(toolbarItemId, toolbarItemReceiver);

        Intent intent = new Intent(ActionBarReceiver.ADD_NEW_TOOLS);
        intent.putExtra("menus", new ActionMenuData[] {
                amd
        });
        AtakBroadcast.getInstance().sendBroadcast(intent);
        actionMenuDataMap.put(toolbarItemId, amd);
    }

    @Override
    public void addToolbarItemMenu(ToolbarItem toolbarItem,
            List<ToolbarItem> menuItems) {
        // XXX - navbar APIs are available to create child buttons, but not seeing that API
        //       exercised currently via `ActionMenuData` registration. Just add the root button for
        //       now...

        addToolbarItemImpl(toolbarItem);
    }

    @Override
    public void removeToolbarItem(ToolbarItem toolbarItem) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                removeToolbarItemImpl(toolbarItem);
            }
        });
    }

    public void removeToolbarItemImpl(ToolbarItem toolbarItem) {
        String toolbarItemId = toolbarItem.getIdentifier();
        BroadcastReceiver receiver = toolbarItemReceiverMap.get(toolbarItemId);
        if (receiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(receiver);
            toolbarItemReceiverMap.remove(toolbarItemId);
        }

        ActionMenuData amd = actionMenuDataMap.get(toolbarItemId);
        if (amd != null) {
            Intent intent = new Intent(ActionBarReceiver.REMOVE_TOOLS);
            intent.putExtra("menus", new ActionMenuData[] {
                    amd
            });
            AtakBroadcast.getInstance().sendBroadcast(intent);
            actionMenuDataMap.remove(toolbarItemId);
        }
    }

    static double getPreferredPaneWidth(Pane pane, double defaultValue,
            double appWidth) {
        double widthRatio = pane.getMetaDouble(Pane.PREFERRED_WIDTH_RATIO,
                Double.NaN);
        double widthPixels = pane.getMetaDouble(Pane.PREFERRED_WIDTH_PIXELS,
                Double.NaN);

        if (!Double.isNaN(widthPixels)) {
            return widthPixels / appWidth;
        } else if (!Double.isNaN(widthRatio)) {
            return widthRatio;
        } else {
            return defaultValue;
        }
    }

    static double getPreferredPaneHeight(Pane pane, double defaultValue,
            double appHeight) {
        double heightRatio = pane.getMetaDouble(Pane.PREFERRED_HEIGHT_RATIO,
                Double.NaN);
        double heightPixels = pane.getMetaDouble(Pane.PREFERRED_HEIGHT_PIXELS,
                Double.NaN);

        if (!Double.isNaN(heightPixels)) {
            return heightPixels / appHeight;
        } else if (!Double.isNaN(heightRatio)) {
            return heightRatio;
        } else {
            return defaultValue;
        }
    }

    static class UIServiceDropDownReceiver extends DropDownReceiver {

        protected UIServiceDropDownReceiver(MapView mapView) {
            super(mapView);
        }

        @Override
        protected void disposeImpl() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }

        void show() {
            unhideDropDown();
        }
    }

    private void removeFromCache(Pane pane) {
        dropDownReceiverMap.remove(pane);
    }
}
