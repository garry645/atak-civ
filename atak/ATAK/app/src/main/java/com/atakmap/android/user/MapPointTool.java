
package com.atakmap.android.user;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class MapPointTool extends Tool implements
        MapEventDispatcher.MapEventDispatchListener {

    public static final String TOOL_IDENTIFIER = "com.atakmap.android.user.MapPointTool";

    private final Context _context;
    private final TextContainer _container;
    private final ActionBarView _layout;
    private final DropSelectCallback callback;

    private String id;
    private String type;

    private boolean running = false;

    public interface DropSelectCallback {
        /**
         * The callback to be implemented to be used when the map click has occurred.
         * @param gpm the geopoint for the map selection
         * @param uid only returned if an object is selected
         */
        void onResult(String id, GeoPointMetaData gpm, String uid);

        /**
         * Called when the tool has been ended.
         */
        void onToolEnd(String id);
    }

    public MapPointTool(@NonNull MapView mapView,
            @NonNull DropSelectCallback callback) {
        super(mapView, TOOL_IDENTIFIER);
        _context = mapView.getContext();
        _container = TextContainer.getInstance();
        ToolManagerBroadcastReceiver.getInstance().registerTool(
                TOOL_IDENTIFIER, this);

        LayoutInflater inflater = LayoutInflater.from(mapView.getContext());
        _layout = (ActionBarView) inflater.inflate(R.layout.map_point_tool,
                mapView, false);
        _layout.setPosition(ActionBarView.TOP_RIGHT);
        _layout.setEmbedState(ActionBarView.FLOATING);

        _layout.findViewById(R.id.endButton)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestEndTool();
                    }
                });

        this.callback = callback;
    }

    public void dispose() {
        requestEndTool();
        ToolManagerBroadcastReceiver.getInstance()
                .unregisterTool(TOOL_IDENTIFIER);

    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        synchronized (this) {
            _mapView.getMapEventDispatcher().pushListeners();
            _mapView.getMapEventDispatcher().clearListeners(MapEvent.MAP_CLICK);
            _mapView.getMapEventDispatcher()
                    .clearListeners(MapEvent.ITEM_CLICK);
            _mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.MAP_CLICK, this);
            _mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.ITEM_CLICK, this);

            String prompt = extras.getString("prompt");
            id = extras.getString("id");
            type = extras.getString("type");

            _container.displayPrompt(!FileSystemUtils.isEmpty(prompt) ? prompt
                    : _context.getString(
                            com.atakmap.app.R.string.map_click_select_prompt));
            _mapView.getMapTouchController().skipDeconfliction(true);

            _mapView.post(new Runnable() {
                @Override
                public void run() {
                    ActionBarReceiver.getInstance().setToolView(_layout);
                }
            });
            running = true;
        }
        return true;
    }

    @Override
    public void onToolEnd() {
        synchronized (this) {
            if (running) {
                _container.closePrompt();
                _mapView.getMapEventDispatcher().popListeners();
                _mapView.getMapTouchController().skipDeconfliction(false);
                ActionBarReceiver.getInstance().setToolView(null);
                callback.onToolEnd(id);
            }
            running = false;
        }

    }

    @Override
    public void onMapEvent(MapEvent event) {
        final String type = event.getType();
        if (type.equals(MapEvent.MAP_CLICK)
                || type.equals(MapEvent.ITEM_CLICK)) {
            PointF point = event.getPointF();
            GeoPointMetaData gpm = _mapView.inverseWithElevation(point.x,
                    point.y);
            final MapItem item = event.getItem();
            callback.onResult(id, gpm, (item != null) ? item.getUID() : null);
            requestEndTool();
        }
    }

}
