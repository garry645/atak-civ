
package com.atakmap.android.toolbars;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

public class LaserBasketDisplayTool extends Tool implements
        MapEventDispatcher.MapEventDispatchListener, View.OnLongClickListener {
    public static final String TOOL_NAME = "laser_basket_display_tool";

    private final MapView _mapView;
    private final TextContainer _container;
    private String _fromMarkerUID;
    private boolean complete = false;
    private final ImageButton button;

    public LaserBasketDisplayTool(MapView mapView, ImageButton button) {
        super(mapView, TOOL_NAME);
        _mapView = mapView;
        _container = TextContainer.getInstance();

        ToolManagerBroadcastReceiver.getInstance()
                .registerTool(TOOL_NAME, this);

        this.button = button;

        button.setOnLongClickListener(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean state = button.isSelected();
                if (!state) {
                    userBeginTool();
                } else {
                    userEndTool();
                }
                button.setSelected(!state);
            }
        });

        AtakBroadcast.getInstance().registerReceiver(_backListener,
                new AtakBroadcast.DocumentedIntentFilter(
                        DropDownManager.CLOSE_DROPDOWN,
                        "Close R&B prompt on back button press"));

    }

    public boolean onLongClick(View view) {
        Toast.makeText(_mapView.getContext(), R.string.laser_basket_tip,
                Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        return true;
    }

    private void userBeginTool() {
        complete = false;
        _mapView.getMapEventDispatcher().pushListeners();
        _mapView.getMapEventDispatcher().clearListeners(MapEvent.MAP_CLICK);
        _mapView.getMapEventDispatcher()
                .clearListeners(MapEvent.MAP_LONG_PRESS);
        _mapView.getMapEventDispatcher().clearListeners(MapEvent.ITEM_CLICK);
        _mapView.getMapEventDispatcher().clearListeners(
                MapEvent.ITEM_LONG_PRESS);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_CLICK, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.MAP_LONG_PRESS, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_CLICK, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_LONG_PRESS, this);
        _container
                .displayPrompt(String.format(
                        _mapView.getContext().getString(
                                R.string.laser_basket_prompt),
                        _mapView.getContext().getString(R.string.friendly)));

        Intent laserIntent = new Intent(); // Remove any Laser Basket that may currently be
                                           // displayed
        laserIntent.setAction("com.atakmap.android.maps.DRAW_WEDGE"); // The user can then cancel
                                                                      // the tool if they desire
        AtakBroadcast.getInstance().sendBroadcast(laserIntent);

    }

    @Override
    public void onToolEnd() {
    }

    private void userEndTool() {
        _fromMarkerUID = null;

        if (!complete && button.isSelected()) {
            _mapView.getMapEventDispatcher().popListeners();
            _container.closePrompt();
            button.setSelected(false);
        }
        // Remove any Laser Basket that may currently be displayed
        Intent laserIntent = new Intent();
        laserIntent.setAction("com.atakmap.android.maps.DRAW_WEDGE");
        AtakBroadcast.getInstance().sendBroadcast(laserIntent);
    }

    @Override
    public void onMapEvent(MapEvent event) {
        if (event.getType().equals(MapEvent.ITEM_CLICK)) {
            MapItem item = event.getItem();
            final String uid = item.getUID();
            if (FileSystemUtils.isEmpty(uid)) {
                return;
            } // UID could not be retrieved

            final String type = item.getType();
            if ((item instanceof Marker) && !uid.equals(_fromMarkerUID)
                    && type != null
                    && type.startsWith("a-h")) {
                if (_fromMarkerUID == null) {
                    MapItem source = ATAKUtilities.findSelf(_mapView);
                    if (source == null) {
                        _container
                                .displayPrompt(_mapView.getContext().getString(
                                        R.string.laser_basket_prompt2)
                                        + _mapView.getContext().getString(
                                                R.string.friendly)
                                        + _mapView.getContext().getString(
                                                R.string.laser_basket_prompt3));
                        return;
                    }
                }
                Intent laserIntent = new Intent();
                laserIntent.setAction("com.atakmap.android.maps.DRAW_WEDGE");
                laserIntent.putExtra("source", _fromMarkerUID);
                laserIntent.putExtra("target", item.getUID());
                laserIntent.putExtra("addListeners", true);
                AtakBroadcast.getInstance().sendBroadcast(laserIntent);
                _container.closePrompt();

                complete = true;
                _mapView.getMapEventDispatcher().popListeners();
                // keep tool active until another tool starts or the user deactivates it
            } else if ((item instanceof Marker) && _fromMarkerUID == null
                    && type != null
                    && type.startsWith("a-f")) {
                _fromMarkerUID = uid;
                _container.displayPrompt(_mapView.getContext().getString(
                        R.string.laser_basket_prompt4));
            }
        }
    }

    @Override
    public void dispose() {
    }

    protected final BroadcastReceiver _backListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!complete && button.isSelected()) {
                userEndTool();
            }
        }
    };

}
