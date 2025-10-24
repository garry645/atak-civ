
package com.atakmap.android.routes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import gov.tak.api.util.Disposable;

class RequiredWaypointViewManager implements View.OnClickListener, Disposable {
    private final CoordinateFormat _coordFormat;
    private GeoPointMetaData _resolvedPoint;
    private String _resolvedAddress;
    private final RouteCreationDialog _routeCreation;
    private final RequiredWaypointView _view;
    private final MapView _mapView;

    private Marker _marker;

    public RequiredWaypointViewManager(MapView mapView,
            RequiredWaypointView view, RouteCreationDialog routeCreation) {
        _mapView = mapView;
        _view = view;
        _routeCreation = routeCreation;

        AtakPreferences _prefs = AtakPreferences
                .getInstance(mapView.getContext());
        _coordFormat = CoordinateFormat.find(_prefs.get(
                "coord_display_pref", mapView.getContext().getString(
                        R.string.coord_display_pref_default)));

        _view._address.setOnClickListener(this);
        _view._clear.setOnClickListener(this);
        _view._coordinateEntry.setOnClickListener(this);
        _view._addressHistory.setOnClickListener(this);
        _view._delete.setOnClickListener(this);
        _view._mapSelect.setOnClickListener(this);
        _view._addWaypoint.setOnClickListener(this);
        _view._menu.setOnClickListener(this);

        //by default dont show the extra option layout
        _view._extraOptions.setVisibility(View.GONE);

        _view._address.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                _view._address.setTextColor(Color.WHITE);
                if (_resolvedPoint != null && s != null
                        && FileSystemUtils.isEquals(s.toString(),
                                _resolvedAddress)) {
                    String coordTxt = CoordinateFormatUtilities
                            .formatToString(_resolvedPoint.get(),
                                    _coordFormat);
                    _view._routeCoord.setText(coordTxt);
                } else
                    _view._routeCoord.setText("");
            }
        });
        _view._address
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        //call into lookup address to do reverse-geocoding
                        if (!FileSystemUtils.isEquals(_resolvedAddress,
                                _view._address.getText().toString())) {
                            routeCreation.lookupAddress(_view._address, null,
                                    false);
                        }
                    }
                });
    }

    /**
     * Gets the GeoPointMetaData where this waypoint is located at
     *
     * @return GeoPointMetaData where this waypoint is
     */
    public GeoPointMetaData getPoint() {
        return _resolvedPoint;
    }

    /**
     * Handles specific click events on registered ui elements
     *
     * @param view View item that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.waypoint_clear) {
            _view._address.setText("");
            _resolvedPoint = null;
        } else if (view.getId() == R.id.route_waypoint_coordinate_entry) {
            _routeCreation.showCoordEntryDialog(_view._address, _resolvedPoint);
        } else if (view.getId() == R.id.route_waypoint_address_history) {
            _routeCreation.showRecentAddresses(_view._address);
        } else if (view.getId() == R.id.route_waypoint_delete) {
            final String text = _view._address.getText().toString();

            if (FileSystemUtils.isEmpty(text)) {
                _routeCreation.removeWaypoint(RequiredWaypointViewManager.this);
                removeMarker();
            } else {
                AlertDialog.Builder b = new AlertDialog.Builder(
                        _mapView.getContext());
                b.setTitle(R.string.delete);

                b.setMessage(_mapView.getContext()
                        .getString(R.string.are_you_sure_delete2, text));
                b.setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                _routeCreation.removeWaypoint(
                                        RequiredWaypointViewManager.this);
                                removeMarker();
                            }
                        });
                b.setNegativeButton(R.string.cancel, null);
                b.show();
            }

        } else if (view.getId() == R.id.route_waypoint_map_select) {
            _routeCreation.startWaypointMapSelect(_view._address,
                    R.string.route_plan_map_click_waypoint);
        } else if (view.getId() == R.id.route_waypoint_add) {
            _routeCreation.addWaypointBetween(this);
        } else if (view.getId() == R.id.waypoint_menu) {
            view.setVisibility(View.GONE);
            _view._extraOptions.setVisibility(View.VISIBLE);

            //run a delayed runnable to be invoked and reset back to the default view state
            //for the options layout
            _mapView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _view._menu.setVisibility(View.VISIBLE);
                    _view._extraOptions.setVisibility(View.GONE);
                }
            }, 5000);
        }
    }

    /**
     * Updates the backing GeoPointMetaData for this required waypoint if the edittext that was
     * altered matches our view id
     *
     * @param et EditText the ui element that has the changed point address
     * @param pointMetaData The new point represented by the address edittext
     * @param addr String human readable location of the waypoint
     */
    public void updatePoint(EditText et, GeoPointMetaData pointMetaData,
            String addr) {
        if (et == _view._address) {
            _resolvedPoint = pointMetaData;
            _resolvedAddress = addr;
        }
    }

    /**
     * Gets the view element of the waypoint data
     *
     * @return View inflated view of this waypoint model
     */
    public View getView() {
        return _view;
    }

    /**
     * Creates the Marker or updates the position of the marker on the map
     *
     * @param editText EditText edittext that was changed
     * @param point GeoPointMetaData the point to set the Marker to
     */
    public void dropOrUpdateMarker(EditText editText, GeoPointMetaData point,
            int index) {
        if (editText == _view._address) {
            if (_marker == null) {
                PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                        point);
                mc.showCotDetails(false);
                mc.setType(Route.WAYPOINT_TYPE);
                mc.setCallsign(String.format("CP%s", index));
                mc.setNeverPersist(true);
                _marker = mc.placePoint();
                _marker.setClickable(false);
            }
            _marker.setPoint(point);
        }
        updateMarkerText(index);
    }

    public void updateMarkerText(int index) {
        if (_marker != null) {
            _marker.setTitle(String.format("CP%s", index));
        }
    }

    /**
     * Removes the Marker from the map representing this Required Waypoint
     */
    private void removeMarker() {
        if (_marker != null && _marker.getGroup() != null) {
            _marker.getGroup().removeItem(_marker);
            _marker.dispose();
        }
    }

    @Override
    public void dispose() {
        removeMarker();
    }
}
