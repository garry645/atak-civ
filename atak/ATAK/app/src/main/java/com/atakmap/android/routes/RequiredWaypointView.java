
package com.atakmap.android.routes;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.app.R;

class RequiredWaypointView extends LinearLayout {

    public final EditText _address;
    public final ImageButton _clear;
    public final ImageButton _coordinateEntry, _addressHistory, _mapSelect,
            _delete;
    public final ImageView _addWaypoint, _menu;
    public final TextView _routeCoord;

    public LinearLayout _extraOptions;

    public RequiredWaypointView(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.route_waypoint_row_layout,
                this, true);

        _address = findViewById(R.id.route_waypoint_address);
        _clear = findViewById(R.id.waypoint_clear);
        _coordinateEntry = findViewById(R.id.route_waypoint_coordinate_entry);
        _addressHistory = findViewById(R.id.route_waypoint_address_history);
        _delete = findViewById(R.id.route_waypoint_delete);
        _mapSelect = findViewById(R.id.route_waypoint_map_select);
        _addWaypoint = findViewById(R.id.route_waypoint_add);
        _menu = findViewById(R.id.waypoint_menu);
        _extraOptions = findViewById(R.id.waypointExtraOptions);
        _routeCoord = findViewById(R.id.route_start_coord);
    }
}
