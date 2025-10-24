
package com.atakmap.android.routes;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.routes.routearound.RouteAroundRegionManager;
import com.atakmap.android.routes.routearound.RouteAroundRegionManagerView;
import com.atakmap.android.routes.routearound.RouteAroundRegionViewModel;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.MapClickTool;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.android.user.geocode.GeocodingUtil;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.Cont;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Dialog for creating routes
 */
public class RouteCreationDialog extends BroadcastReceiver implements
        View.OnClickListener {

    private static final String TAG = "RouteCreationDialog";
    private static final String MAP_CLICKED = "com.atakmap.android.maps.MAP_CLICKED";

    private static final List<Pair<String, GeoPointMetaData>> RECENT_ADDRESSES = new ArrayList<>();

    private final MapView _mapView;
    private final Context _context;

    private final AtakPreferences _prefs;
    private final CoordinateFormat _coordFormat;
    private final RouteMapReceiver _receiver;
    private final Route _route;

    private Marker spMarker, vdoMarker;

    private final String _unkAddr;

    // Address dialog
    private AlertDialog _addrDialog;
    private EditText _startAddr, _destAddr, _waypointAddr;
    private TextView _startCoord, _destCoord;
    private ImageView _addWaypointButton;
    private LinearLayout _routePlanOptions, _waypoints;

    private final List<RequiredWaypointViewManager> _requiredWaypoints = new ArrayList<>();

    private static RouteAroundRegionManager _routeAroundManager;
    private static RouteAroundRegionViewModel _routeAroundVM;
    private static LinearLayout _routeAroundOptions;

    private String _resolvedStartAddr, _resolvedDestAddr;
    private GeoPointMetaData _resolvedStartPoint, _resolvedDestPoint;
    private boolean _finishingLookup = false;
    private int _pendingLookups = 0;
    private List<RoutePlannerInterface2> _planners;
    private List<ManualRoutePlannerInterface2> _manualPlanners;
    private RoutePlannerInterface2 _autoPlan;
    private final File recentlyUsed = FileSystemUtils
            .getItem("tools/route/recentlyused.txt");
    private final LayoutInflater _inflater;

    public RouteCreationDialog(final MapView mapView) {
        _mapView = mapView;
        _context = mapView.getContext();
        _prefs = AtakPreferences.getInstance(_context);
        _receiver = RouteMapReceiver.getInstance();
        _route = _receiver.getNewRoute(UUID.randomUUID().toString());
        ATAKUtilities.setAuthorInformation(_route);

        _unkAddr = _context.getString(R.string.unknown_address);
        _inflater = LayoutInflater.from(_context);
        _coordFormat = CoordinateFormat.find(_prefs.get(
                "coord_display_pref", _context.getString(
                        R.string.coord_display_pref_default)));

        _routeAroundManager = RouteAroundRegionManager.getInstance();
        _routeAroundVM = new RouteAroundRegionViewModel(_routeAroundManager);
        _routeAroundOptions = (LinearLayout) _inflater
                .inflate(R.layout.route_around_layout, null);

        CheckBox avoidRouteAroundRegions = _routeAroundOptions
                .findViewById(R.id.chk_route_regions);
        CheckBox avoidGeofences = _routeAroundOptions
                .findViewById(R.id.chk_route_around_geo_fences);
        Button openRouteAroundManager = _routeAroundOptions
                .findViewById(R.id.btn_open_route_around);

        avoidRouteAroundRegions.setChecked(_prefs.get(
                RouteAroundRegionManagerView.OPT_AVOID_ROUTE_AROUND_REGIONS,
                false));
        avoidGeofences.setChecked(_prefs.get(
                RouteAroundRegionManagerView.OPT_AVOID_GEOFENCES, false));

        avoidRouteAroundRegions.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton x,
                            boolean checked) {
                        _prefs.set(
                                RouteAroundRegionManagerView.OPT_AVOID_ROUTE_AROUND_REGIONS,
                                checked);
                    }
                });

        avoidGeofences.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton x,
                            boolean checked) {
                        _prefs.set(
                                RouteAroundRegionManagerView.OPT_AVOID_GEOFENCES,
                                checked);
                    }
                });

        openRouteAroundManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteAroundRegionManagerView regionManagerView = new RouteAroundRegionManagerView(
                        mapView,
                        new RouteAroundRegionViewModel(_routeAroundManager));
                AlertDialog dialog = new AlertDialog.Builder(_context)
                        .setTitle(R.string.manage_route_around_regions)
                        .setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(
                                            DialogInterface dialog) {
                                        new RouteAroundRegionViewModel(
                                                _routeAroundManager)
                                                        .saveState();
                                    }
                                })
                        .setPositiveButton(R.string.done,
                                new AlertDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .setView(regionManagerView.createView(_context, null))
                        .create();
                regionManagerView.setParentDialog(dialog);
                regionManagerView.setParentParentDialog(_addrDialog);
                dialog.show();
            }
        });

        loadRecentlyUsed();

        if (!IOProviderFactory.exists(recentlyUsed.getParentFile())
                && !IOProviderFactory.mkdirs(recentlyUsed
                        .getParentFile())) {
            Log.d(TAG, "error making: " + recentlyUsed.getParentFile());
        }
    }

    /**
     * Load the recently used lookups.
     */
    private void loadRecentlyUsed() {
        String line;
        RECENT_ADDRESSES.clear();

        if (IOProviderFactory.exists(recentlyUsed)) {
            try (InputStream is = IOProviderFactory
                    .getInputStream(recentlyUsed);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is,
                                    FileSystemUtils.UTF8_CHARSET))) {
                while ((line = reader.readLine()) != null) {
                    String[] info = line.split("\t");
                    RECENT_ADDRESSES.add(new Pair<>(info[0],
                            GeoPointMetaData
                                    .wrap(GeoPoint.parseGeoPoint(info[1]))));
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "Unable to load recently used lookups due to an error",
                        e);
            }
        }
    }

    private void saveRecentlyUsed() {
        // Trim down list to most recently used 10 addresses
        while (RECENT_ADDRESSES.size() > 10) {
            RECENT_ADDRESSES.remove(RECENT_ADDRESSES.size() - 1);
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(
                IOProviderFactory.getFileWriter(recentlyUsed))) {

            for (Pair<String, GeoPointMetaData> item : RECENT_ADDRESSES) {
                bufferedWriter.write(item.first + "\t" + item.second + "\n");
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Unable to save recently used addresses due to an exception: ",
                    e);
        }
        // Ignored
    }

    /** DEPRECIATED: This is the old API. Use the version with 5 arguments (which is actually static) */
    static void setupPlanSpinner(final Spinner spinner,
            final List<RoutePlannerInterface2> _planners,
            final LinearLayout routePlanOptions,
            final AlertDialog addrDialog) {
        setupPlanSpinner(spinner, _planners, routePlanOptions, addrDialog,
                _routeAroundOptions);
    }

    /** 
     * Sets up the action for creating a spinner for both the address dialog and the reverse dialog
     */
    static void setupPlanSpinner(final Spinner spinner,
            final List<RoutePlannerInterface2> _planners,
            final LinearLayout routePlanOptions,
            final AlertDialog addrDialog,
            final LinearLayout routeAroundOptions) {
        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                    View selectedItemView, int position, long id) {

                if (selectedItemView instanceof TextView)
                    ((TextView) selectedItemView).setTextColor(Color.WHITE);

                LayoutParams lpView = new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT);

                String plannerName = (String) spinner
                        .getItemAtPosition(position);
                routePlanOptions.removeAllViews();
                for (RoutePlannerInterface2 k : _planners) {
                    if (plannerName != null && plannerName
                            .equals(k.getDescriptiveName())) {

                        final View optionsView = k
                                .getOptionsView(addrDialog);
                        if (optionsView.getParent() instanceof ViewGroup) {
                            Log.e(TAG,
                                    "optionsView already showing while attempting to show: "
                                            + plannerName);
                            ((ViewGroup) optionsView.getParent())
                                    .removeView(optionsView);
                        }

                        try {
                            routePlanOptions.addView(optionsView, lpView);
                        } catch (Exception e) {
                            Log.e(TAG,
                                    "error encountered trying to add the optionsView",
                                    e);
                            Toast.makeText(addrDialog.getContext(),
                                    "error occurred during planner (options) initialization for "
                                            + plannerName,
                                    Toast.LENGTH_LONG).show();
                        }

                        if (k.canRouteAroundRegions()) {
                            if (routeAroundOptions != null) {
                                if (routeAroundOptions
                                        .getParent() instanceof ViewGroup) {
                                    Log.e(TAG,
                                            "routeAroundOptions already showing while attempting to show: "
                                                    + plannerName);
                                    ((ViewGroup) routeAroundOptions.getParent())
                                            .removeView(routeAroundOptions);
                                }
                                try {
                                    routePlanOptions
                                            .addView(routeAroundOptions);
                                } catch (Exception e) {
                                    Log.e(TAG,
                                            "error encountered trying to add the routeAroundOptionsView",
                                            e);
                                    Toast.makeText(addrDialog.getContext(),
                                            "error occurred during planner (route around options) initialization for "
                                                    + plannerName,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                }
            }

        });

        String plannerName = (String) spinner.getSelectedItem();
        routePlanOptions.removeAllViews();
        for (RoutePlannerInterface2 k : _planners) {
            if (plannerName != null
                    && plannerName.equals(k.getDescriptiveName())) {
                routePlanOptions
                        .addView(k.getOptionsView(addrDialog));
                if (k.canRouteAroundRegions()) {
                    // just verify that the route around options is not currently
                    // in another view
                    if (routeAroundOptions != null) {
                        ViewParent vg = routeAroundOptions.getParent();
                        if (vg instanceof ViewGroup)
                            ((ViewGroup) vg).removeView(routeAroundOptions);

                        routePlanOptions.addView(routeAroundOptions);
                    }
                }
            }
        }

    }

    /**
     * Prompts the user to create a new route, then goes into edit mode
     *
     * @param manual True for manual creation mode
     *               False to prompt user for address routing
     * @param method The route method selected for the new route.
     */
    public void show(boolean manual, Route.RouteMethod method) {

        ToolManagerBroadcastReceiver.getInstance()
                .endCurrentTool();

        if (manual) {
            List<ManualRoutePlannerInterface2> validPlanners = getValidManualPlanners(
                    false, method);

            // If no custom planners, just launch ATAK's planner.
            if (validPlanners.isEmpty()) {
                onFinish((RoutePlannerInterface2) null);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(_context);

            List<String> validPlannerNames = new ArrayList<>();
            for (ManualRoutePlannerInterface2 planner : validPlanners) {
                validPlannerNames.add(planner.getDescriptiveName());
            }

            int manualPlanners = validPlanners.size();

            // Add an extra entry for built-ins
            validPlannerNames.add(manualPlanners,
                    _context.getString(R.string.builtin));

            builder.setSingleChoiceItems(
                    validPlannerNames.toArray(new String[0]),
                    -1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                int i) {
                            dialogInterface.dismiss();
                            if (i < manualPlanners) {
                                onFinish(validPlanners.get(i));
                            } else {
                                onFinish((RoutePlannerInterface2) null);
                            }
                        }
                    });

            builder.create().show();
        } else {
            Pair<AlertDialog.Builder, Cont<Route.RouteMethod>> detailsDialog = getDetailsDialog(
                    _route, _context, true);

            AlertDialog.Builder builder = detailsDialog.first;
            Cont<Route.RouteMethod> onRouteMethodUpdate = detailsDialog.second;

            builder.setPositiveButton(R.string.use_automatic_planner,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                int i) {
                            showAddressDialog(
                                    _route.getRouteMethod());
                        }
                    });
            builder.setNegativeButton(R.string.manual_entry,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                int i) {
                            show(true, _route.getRouteMethod());
                        }
                    });

            AlertDialog alertDialog = builder.create();

            alertDialog.setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (MetricsApi.shouldRecordMetric()) {
                                Bundle details = new Bundle();
                                details.putString(MetricsUtils.FIELD_STATUS,
                                        MetricsUtils.EVENT_STATUS_SUCCESS);
                                details.putString("routing_type", "cancelled");

                                MetricsUtils.record(
                                        MetricsUtils.CATEGORY_MAPITEM,
                                        MetricsUtils.EVENT_MAPITEM_ACTION,
                                        "RouteCreationDialog",
                                        details);
                            }
                            AtakBroadcast.getInstance().sendBroadcast(
                                    new Intent(RouteMapReceiver.MANAGE_ACTION));
                        }
                    });

            alertDialog.show();

            // needs to occur after show
            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(!getValidPlanners(false, method).isEmpty());

            // And also after updates
            onRouteMethodUpdate
                    .registerCallback(new Cont.Callback<Route.RouteMethod>() {
                        @Override
                        public void onInvoke(Route.RouteMethod value) {
                            Button button = alertDialog
                                    .getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setEnabled(
                                    !getValidPlanners(false, value).isEmpty());
                        }
                    });
        }
    }

    public void show() {
        // Spinner updates the route when selected, so this will always
        // be what the user selected.
        show(false, _route.getRouteMethod());
    }

    private void onFinish(RoutePlannerInterface2 rpi) {
        // Finalize route and show details
        _route.setMetaString("entry", "user");
        _route.setMetaBoolean("creating", true);
        _receiver.getRouteGroup().addItem(_route);
        _route.setVisible(true);
        _route.setColor(_prefs.get("route_last_selected_color",
                Color.WHITE));
        _receiver.showRouteDetails(_route, rpi, rpi == null);

        if (MetricsApi.shouldRecordMetric()) {
            Bundle details = new Bundle();
            details.putString(MetricsUtils.FIELD_STATUS,
                    MetricsUtils.EVENT_STATUS_SUCCESS);
            details.putString("routing_type", "automatic");
            MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                    MetricsUtils.EVENT_MAPITEM_ADDED, "RouteCreationDialog",
                    _route, details);
        }

        if (rpi == null) {
            _mapView.post(new Runnable() {
                @Override
                public void run() {
                    DropDownManager.getInstance().hidePane();
                }
            });
        }
    }

    private void onFinish(ManualRoutePlannerInterface2 rpi) {

        // Finalize route and show details
        _route.setMetaString("entry", "user");
        _route.setMetaBoolean("creating", true);
        _receiver.getRouteGroup().addItem(_route);
        _route.setVisible(true);
        _route.setColor(_prefs.get("route_last_selected_color",
                Color.WHITE));

        if (MetricsApi.shouldRecordMetric()) {
            Bundle details = new Bundle();
            details.putString(MetricsUtils.FIELD_STATUS,
                    MetricsUtils.EVENT_STATUS_SUCCESS);
            details.putString("routing_type", "manual");
            MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                    MetricsUtils.EVENT_MAPITEM_ADDED, "RouteCreationDialog",
                    _route, details);
        }

        if (rpi != null) {
            Intent intent = new Intent(rpi.getAction());
            intent.putExtra("routeUID", _route.getUID());
            AtakBroadcast.getInstance().sendBroadcast(
                    intent);
        } else {
            _mapView.post(new Runnable() {
                @Override
                public void run() {
                    DropDownManager.getInstance().hidePane();
                }
            });
        }
    }

    private List<String> getValidPlanners(boolean toast,
            Route.RouteMethod method) {
        final List<String> retval = new ArrayList<>();
        // Load route planner interfaces into spinner
        MapComponent mc = ((MapActivity) _context).getMapComponent(
                RouteMapComponent.class);
        if (mc == null)
            return retval;
        RoutePlannerManager routePlanner = ((RouteMapComponent) mc)
                .getRoutePlannerManager();
        _planners = new ArrayList<>(
                routePlanner.getRoutePlanners2());
        if (FileSystemUtils.isEmpty(_planners))
            return retval;

        final boolean network = RouteMapReceiver.isNetworkAvailable();
        if (!network && toast)
            Toast.makeText(_context,
                    "network not available",
                    Toast.LENGTH_SHORT).show();

        for (RoutePlannerInterface2 k : _planners) {
            final Route.RouteMethod plannerMethod = k
                    .getRouteMethod();
            final boolean shouldAddNetwork = !k.isNetworkRequired()
                    || network;
            final boolean supportsMethod = plannerMethod == null
                    | method == plannerMethod;

            if (shouldAddNetwork && supportsMethod)
                retval.add(k.getDescriptiveName());
        }
        return retval;
    }

    private List<ManualRoutePlannerInterface2> getValidManualPlanners(
            boolean toast,
            Route.RouteMethod method) {
        final List<ManualRoutePlannerInterface2> retval = new ArrayList<>();
        // Load route planner interfaces into spinner
        MapComponent mc = ((MapActivity) _context).getMapComponent(
                RouteMapComponent.class);
        if (mc == null)
            return retval;
        RoutePlannerManager routePlanner = ((RouteMapComponent) mc)
                .getRoutePlannerManager();
        _manualPlanners = new ArrayList<>(
                routePlanner.getManualRoutePlanners2());
        if (FileSystemUtils.isEmpty(_manualPlanners))
            return retval;

        final boolean network = RouteMapReceiver.isNetworkAvailable();
        if (!network && toast)
            Toast.makeText(_context,
                    "network not available",
                    Toast.LENGTH_SHORT).show();

        for (ManualRoutePlannerInterface2 k : _manualPlanners) {
            final Route.RouteMethod plannerMethod = k
                    .getRouteMethod();
            final boolean shouldAddNetwork = !k.isNetworkRequired()
                    || network;
            final boolean supportsMethod = plannerMethod == null
                    | method == plannerMethod;

            if (shouldAddNetwork && supportsMethod)
                retval.add(k);
        }
        return retval;
    }

    // Show the dialog for creating a route with a route planner.
    private boolean showAddressDialog(Route.RouteMethod method) {
        List<String> plannerNames = getValidPlanners(true, method);

        if (FileSystemUtils.isEmpty(plannerNames))
            return false;

        Collections.sort(plannerNames, RoutePlannerView.ALPHA_SORT);

        View addressEntryView = LayoutInflater.from(_context).inflate(
                R.layout.route_address_entry, _mapView, false);
        View routePlanView = LayoutInflater.from(_context).inflate(
                R.layout.route_planner_options_layout, _mapView, false);

        LinearLayout topLevelLayout = addressEntryView
                .findViewById(R.id.top_level_layout);
        topLevelLayout.addView(routePlanView);

        _startAddr = addressEntryView.findViewById(R.id.route_start_address);
        _destAddr = addressEntryView.findViewById(R.id.route_dest_address);
        _startCoord = addressEntryView.findViewById(R.id.route_start_coord);
        _destCoord = addressEntryView.findViewById(R.id.route_dest_coord);
        _addWaypointButton = addressEntryView
                .findViewById(R.id.requiredWaypointImageView);

        _routePlanOptions = routePlanView
                .findViewById(R.id.route_plan_options);
        _waypoints = addressEntryView.findViewById(R.id.waypointsLinearLayout);

        final Spinner planSpinner = routePlanView.findViewById(
                R.id.route_plan_method);

        if (!RouteMapReceiver.isNetworkAvailable()) {
            _startAddr.setVisibility(View.GONE);
            _destAddr.setVisibility(View.GONE);
        }
        addressEntryView.findViewById(R.id.route_start_address_clear)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_dest_address_clear)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_start_map_select)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_dest_map_select)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_start_menu)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_dest_menu)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_start_coordinate_entry)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_dest_coordinate_entry)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_start_address_history)
                .setOnClickListener(this);
        addressEntryView.findViewById(R.id.route_dest_address_history)
                .setOnClickListener(this);
        _addWaypointButton.setOnClickListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(_context,
                R.layout.spinner_text_view_dark, plannerNames);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        planSpinner.setAdapter(adapter);

        final String lastUsedPlanner = _prefs.get("lastUsedPlanner", null);
        int lastUsedPlannerPos = 0;
        for (int i = 0; i < plannerNames.size(); ++i)
            if (plannerNames.get(i).equals(lastUsedPlanner))
                lastUsedPlannerPos = i;

        planSpinner.setSelection(lastUsedPlannerPos);

        // For resetting the text color to white after highlighting invalid address
        _startAddr.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                _startAddr.setTextColor(Color.WHITE);
                if (_resolvedStartPoint != null && s != null
                        && FileSystemUtils.isEquals(s.toString(),
                                _resolvedStartAddr)) {
                    String coordTxt = CoordinateFormatUtilities
                            .formatToString(_resolvedStartPoint.get(),
                                    _coordFormat);
                    _startCoord.setText(coordTxt);

                } else
                    _startCoord.setText("");
            }
        });
        _startAddr.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!FileSystemUtils.isEquals(_resolvedStartAddr,
                        _startAddr.getText().toString()))
                    lookupAddress(_startAddr, null, false);
            }
        });
        _destAddr.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                _destAddr.setTextColor(Color.WHITE);
                if (_resolvedDestPoint != null && s != null
                        && FileSystemUtils.isEquals(s.toString(),
                                _resolvedDestAddr)) {
                    String coordTxt = CoordinateFormatUtilities
                            .formatToString(_resolvedDestPoint.get(),
                                    _coordFormat);
                    _destCoord.setText(coordTxt);
                } else
                    _destCoord.setText("");
            }
        });
        _destAddr.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!FileSystemUtils.isEquals(_resolvedDestAddr,
                        _destAddr.getText().toString()))
                    lookupAddress(_destAddr, null, false);
            }
        });

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(R.string.routes_text9);
        b.setView(addressEntryView);
        b.setPositiveButton(R.string.create, null);
        b.setNegativeButton(R.string.cancel, null);

        _addrDialog = b.create();

        setupPlanSpinner(planSpinner, _planners, _routePlanOptions,
                _addrDialog, _routeAroundOptions);

        _addrDialog.setCancelable(false);
        _addrDialog.show();
        AtakBroadcast.getInstance().registerReceiver(this,
                new DocumentedIntentFilter(MAP_CLICKED,
                        "Map click returned by tool"));
        _addrDialog
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        for (RequiredWaypointViewManager viewModel : _requiredWaypoints) {
                            viewModel.dispose();
                        }
                        _requiredWaypoints.clear();
                        dispose();
                    }
                });
        _addrDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Check input addresses
                        String startInput = _startAddr.getText().toString();
                        String destInput = _destAddr.getText().toString();
                        if (FileSystemUtils.isEmpty(startInput)) {
                            Toast.makeText(_context,
                                    R.string.route_plan_start_address_empty,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (FileSystemUtils.isEmpty(destInput)) {
                            Toast.makeText(_context,
                                    R.string.route_plan_dest_address_empty,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        //check the waypoint for valid data(ie a geopoint set to each required waypoint)
                        //set it based on
                        if (!_requiredWaypoints.isEmpty()) {
                            boolean requiredWaypointsAreValid = true;
                            for (int i = 0; i < _requiredWaypoints
                                    .size(); i++) {
                                RequiredWaypointViewManager model = _requiredWaypoints
                                        .get(i);
                                if (model.getPoint() == null) {
                                    requiredWaypointsAreValid = false;
                                    break;
                                }
                            }

                            if (!requiredWaypointsAreValid) {
                                Toast.makeText(_context,
                                        R.string.route_plan_waypoint_address_empty,
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        // Find selected planner
                        String plannerName = (String) planSpinner
                                .getSelectedItem();

                        _prefs.set("lastUsedPlanner", plannerName);

                        for (RoutePlannerInterface2 k : _planners) {
                            if (plannerName.equals(k.getDescriptiveName())) {
                                _autoPlan = k;
                                break;
                            }
                        }
                        if (_autoPlan == null) {
                            // Should never happen, but just in case
                            Toast.makeText(_context,
                                    R.string.route_plan_unknown_host,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        _finishingLookup = true;

                        // Check that input addresses still match the resolved addresses
                        if (!FileSystemUtils.isEquals(_resolvedStartAddr,
                                startInput))
                            lookupAddress(_startAddr, null, false);
                        if (!FileSystemUtils.isEquals(_resolvedDestAddr,
                                destInput))
                            lookupAddress(_destAddr, null, true);

                        if (_pendingLookups == 0)
                            onAddressResolved();
                    }
                });

        // Use self marker as default start address
        lookupAddress(_startAddr, null, false);

        return true;
    }

    private void dispose() {
        AtakBroadcast.getInstance().unregisterReceiver(this);

        // remove the markers created here when dialog is being dismissed
        if (spMarker != null && spMarker.getGroup() != null) {
            spMarker.getGroup().removeItem(spMarker);
            spMarker.dispose();
        }

        if (vdoMarker != null && vdoMarker.getGroup() != null) {
            vdoMarker.getGroup().removeItem(vdoMarker);
            vdoMarker.dispose();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.route_start_address_clear) {
            _resolvedStartPoint = null;
            _resolvedStartAddr = null;
            _startAddr.setText("");

            //remove marker since we have cleared out the address
            if (spMarker != null && spMarker.getGroup() != null) {
                spMarker.getGroup().removeItem(spMarker);
                spMarker.dispose();
            }

        } else if (i == R.id.route_start_map_select) {
            startMapSelect(_startAddr, R.string.route_plan_map_click_start);

        } else if (i == R.id.route_start_address_history) {
            showRecentAddresses(_startAddr);

        } else if (i == R.id.route_start_coordinate_entry) {
            showCoordEntryDialog(_startAddr, _resolvedStartPoint);

        } else if (i == R.id.route_start_menu) {
            v.setVisibility(View.GONE);
            View opt = ((ViewGroup) v.getParent())
                    .findViewById(R.id.route_start_options);
            opt.setVisibility(View.VISIBLE);
            //run a delayed runnable to be invoked and reset back to the default view state
            //for the options layout
            _mapView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    v.setVisibility(View.VISIBLE);
                    opt.setVisibility(View.GONE);
                }
            }, 5000);
        } else if (i == R.id.route_dest_menu) {
            v.setVisibility(View.GONE);
            View opt = ((ViewGroup) v.getParent())
                    .findViewById(R.id.route_end_options);
            opt.setVisibility(View.VISIBLE);
            //run a delayed runnable to be invoked and reset back to the default view state
            //for the options layout
            _mapView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    v.setVisibility(View.VISIBLE);
                    opt.setVisibility(View.GONE);
                }
            }, 5000);
        } else if (i == R.id.route_dest_address_clear) {
            // Destination address
            _resolvedDestPoint = null;
            _resolvedDestAddr = null;
            _destAddr.setText("");

            //remove marker since we have cleared out the address
            if (vdoMarker != null && vdoMarker.getGroup() != null) {
                vdoMarker.getGroup().removeItem(vdoMarker);
                vdoMarker.dispose();
            }

        } else if (i == R.id.route_dest_map_select) {

            startMapSelect(_destAddr, R.string.route_plan_map_click_dest);

        } else if (i == R.id.route_dest_address_history) {
            showRecentAddresses(_destAddr);

        } else if (i == R.id.requiredWaypointImageView) {
            addWaypoint();

        } else if (i == R.id.route_dest_coordinate_entry) {
            showCoordEntryDialog(_destAddr, _resolvedDestPoint);
        }
    }

    private void addWaypoint() {
        RequiredWaypointView view = new RequiredWaypointView(_context);
        _waypoints.addView(view);
        RequiredWaypointViewManager requiredWaypointView = new RequiredWaypointViewManager(
                _mapView, view, this);
        _requiredWaypoints.add(requiredWaypointView);

        //hide the add waypoint button since we have 1 entry
        _addWaypointButton.setVisibility(View.GONE);
    }

    /**
     * Creates and adds a RoutePlannerWaypointView to the UI between 2 existing waypoints, this view
     * will store the waypoint address for a required waypoint
     * @param idx - The 0 based index to insert the waypoint view/model
     */
    private void addWaypointBetween(int idx) {
        RequiredWaypointView view = new RequiredWaypointView(_context);
        _waypoints.addView(view, idx);
        RequiredWaypointViewManager requiredWaypointView = new RequiredWaypointViewManager(
                _mapView, view, this);
        _requiredWaypoints.add(idx, requiredWaypointView);
    }

    /**
     * Updates the ViewModel/View with the new point/address
     * @param et EditText - edittext that has the new address
     * @param pointMetaData - GeoPointMetaData point that represents the address
     * @param addr = String the human readable string location of the point
     */
    private void updateWaypointPoint(EditText et,
            GeoPointMetaData pointMetaData, String addr) {
        for (RequiredWaypointViewManager viewModel : _requiredWaypoints) {
            viewModel.updatePoint(et, pointMetaData, addr);
        }
    }

    /**
     * Removes a RequiredWaypointViewModel view from the UI, this removes the
     * required waypoint stop from the planned route
     * @param model RequiredWaypointViewModel - the view/model to remove from the UI,backing list
     */
    public void removeWaypoint(RequiredWaypointViewManager model) {
        _waypoints.removeView(model.getView());
        _requiredWaypoints.remove(model);

        //show the add waypoint button if we do not have any required waypoints lefts
        _addWaypointButton.setVisibility(
                !_requiredWaypoints.isEmpty() ? View.GONE : View.VISIBLE);

        // pass the edittext to the required waypoints and give them a chance to
        // update/create their marker. update all waypoint markers label text
        for (int i = 0; i < _requiredWaypoints.size(); i++) {
            RequiredWaypointViewManager viewModel = _requiredWaypoints.get(i);
            viewModel.updateMarkerText(i);
        }
    }

    /**
     * Adds a new blank waypoint view/model after the passed in waypoint
     * @param model - RequiredWaypointViewModel the model to insert a new waypoint after
     */
    public void addWaypointBetween(RequiredWaypointViewManager model) {
        int idx = _requiredWaypoints.indexOf(model);
        if (idx != -1) {
            addWaypointBetween(++idx);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Route point clicked on map
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(MAP_CLICKED)) {
            String routeUID = intent.getStringExtra("routeUID");
            if (_route == null || !FileSystemUtils.isEquals(
                    _route.getUID(), routeUID)) {
                return;
            }

            _addrDialog.show();
            DropDownManager.getInstance().unHidePane();

            GeoPoint point = GeoPoint.parseGeoPoint(
                    intent.getStringExtra("point"));
            if (point == null || !point.isValid())
                return;

            int textId = intent.getIntExtra("textId", 0);
            EditText et = textId != R.id.route_start_address
                    && textId != R.id.route_dest_address
                            ? _waypointAddr
                            : textId == R.id.route_start_address
                                    ? _startAddr
                                    : _destAddr;

            if (intent.hasExtra("accept")) {
                String address = intent.getStringExtra("address");
                if (intent.getBooleanExtra("accept", false)) {
                    // Finish route creation
                    updateAddress(et, GeoPointMetaData.wrap(point), address);
                    onAddressResolved();
                } else {
                    // Update the address and point but don't finish
                    if (et == _destAddr) {
                        _resolvedDestPoint = GeoPointMetaData.wrap(point);
                        _resolvedDestAddr = address;
                    } else if (et == _startAddr) {
                        _resolvedStartPoint = GeoPointMetaData.wrap(point);
                        _resolvedStartAddr = address;
                    } else {
                        updateAddress(et, GeoPointMetaData.wrap(point),
                                address);
                    }
                    et.setText(address);
                }
            } else
                lookupAddress(et, point, false);

            //reset this since we used this already
            _waypointAddr = null;
        }
    }

    /**
     * Creates or updates the location of a marker used to show the touch point for the specified
     * route point being either a SP(start) or VDO(end) point
     *
     * @param editText The specific edittext to know which point we are creating/editing
     * @param point geopoint of the location of the marker
     */
    private void dropMarker(EditText editText, GeoPointMetaData point) {
        if (editText == _startAddr) {
            if (spMarker == null) {
                spMarker = createGenericWaypoint(point.get(), "SP");
                spMarker.setIcon(new Icon.Builder().setImageUri(0, ATAKUtilities
                        .getResourceUri(R.drawable.nav_sp_icon)).setSize(35, 35)
                        .build());
            } else {
                spMarker.setPoint(point);
            }
        } else if (editText == _destAddr) {
            if (vdoMarker == null) {
                vdoMarker = createGenericWaypoint(point.get(), "VDO");
                vdoMarker.setIcon(new Icon.Builder()
                        .setImageUri(0, ATAKUtilities
                                .getResourceUri(R.drawable.nav_vdo_icon))
                        .setSize(35, 35).build());
            } else {
                vdoMarker.setPoint(point);
            }
        } else {
            // pass the edittext to the required waypoints and give them a chance to
            // update/create their marker update all waypoint markers label text
            for (int i = 0; i < _requiredWaypoints.size(); i++) {
                RequiredWaypointViewManager viewModel = _requiredWaypoints
                        .get(i);
                viewModel.dropOrUpdateMarker(editText, point, i);
            }
        }
    }

    /**
     * Creates a generic Route Waypoint type marker at the point provided
     *
     * @param point The geopoint the marker is placed
     * @param callsign The name of the marker
     */
    private Marker createGenericWaypoint(GeoPoint point, String callsign) {
        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                point);
        mc.showCotDetails(false);
        mc.setType(Route.WAYPOINT_TYPE);
        mc.setCallsign(callsign);
        mc.setNeverPersist(true); //dont persist this marker
        Marker marker = mc.placePoint();
        marker.setClickable(false);
        return marker;
    }

    /**
     * Geocoding of the start/dest address
     * @param et Text input used to request this lookup
     * @param src Source point to reverse geocode (null to geocode instead)
     */
    protected void lookupAddress(final EditText et, GeoPoint src,
            final boolean requireConfirmation) {

        String addrInput = et.getText().toString();
        if (src != null || addrInput.isEmpty()) {
            if (src == null) {
                Marker self = _mapView.getSelfMarker();
                src = (self != null && self.getGroup() != null) ? self
                        .getPoint()
                        : _mapView.getCenterPoint().get();
            }
            if (src != null) {
                final ProgressDialog pd = ProgressDialog.show(_context,
                        _context.getString(R.string.goto_dialog1),
                        _context.getString(R.string.goto_dialog2), true, false);
                _pendingLookups++;
                GeocodingUtil.lookup(
                        GeocodeManager.getInstance(_context)
                                .getSelectedGeocoder(),
                        src, GeocodingUtil.NO_LIMIT,
                        new GeocodingUtil.ResultListener() {
                            @Override
                            public void onResult(GeocodeManager.Geocoder coder,
                                    String originalAddress,
                                    GeoPoint originalPoint,
                                    List<Pair<String, GeoPoint>> addresses,
                                    GeocodeManager.GeocoderException error) {

                                final GeoPointMetaData pt = GeoPointMetaData
                                        .wrap(originalPoint);
                                final String addr;
                                if (!addresses.isEmpty()) {
                                    addr = addresses.get(0).first;
                                } else {
                                    addr = "";
                                }

                                _mapView.post(new Runnable() {
                                    public void run() {
                                        updateAddress(et, pt, addr);
                                        pd.dismiss();

                                        _pendingLookups--;
                                        onAddressResolved();
                                    }
                                });
                            }
                        });
            } else
                Toast.makeText(_context, R.string.goto_input_tip9,
                        Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = ProgressDialog.show(_context,
                _context.getString(R.string.goto_dialog1), addrInput,
                true, false);

        GeocodingUtil.lookup(
                GeocodeManager.getInstance(_context).getSelectedGeocoder(),
                _mapView.getBounds(), addrInput, GeocodingUtil.NO_LIMIT,
                new GeocodingUtil.ResultListener() {
                    @Override
                    public void onResult(GeocodeManager.Geocoder coder,
                            String originalAddress, GeoPoint originalPoint,
                            List<Pair<String, GeoPoint>> addresses,
                            GeocodeManager.GeocoderException error) {
                        pd.dismiss();
                        _pendingLookups--;
                        if (addresses.isEmpty())
                            return;

                        final String address = addresses.get(0).first;
                        final GeoPoint point = addresses.get(0).second;

                        if (requireConfirmation) {

                            Bundle bundle = new Bundle();
                            bundle.putString("address", address);
                            bundle.putParcelable("point", point);
                            bundle.putParcelable("callback",
                                    new Intent(MAP_CLICKED)
                                            .putExtra("routeUID",
                                                    _route.getUID())
                                            .putExtra("textId", et.getId()));
                            ToolManagerBroadcastReceiver.getInstance()
                                    .startTool(
                                            RouteConfirmationTool.TOOL_NAME,
                                            bundle);
                            if (ToolManagerBroadcastReceiver.getInstance()
                                    .getActiveTool() instanceof RouteConfirmationTool) {
                                _mapView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        _addrDialog.hide();
                                        DropDownManager.getInstance()
                                                .hidePane();
                                    }
                                });
                            }
                        } else {
                            if (point != null) {
                                _mapView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateAddress(et,
                                                GeoPointMetaData.wrap(point),
                                                address);
                                        onAddressResolved();
                                    }
                                });
                            }
                        }

                    }
                });
        _pendingLookups++;

    }

    /**
     * Updates the start or stop address field. Typically called after a geo-lookup--there is some
     * fallback logic for if the geo-lookup was unsuccessful (to include no internet connectivity)
     * @param et Either the start or stop EditText
     * @param gp The resulting point or null if none found
     * @param addr Textual description. Notionally a street address or empty
     */
    private void updateAddress(EditText et, GeoPointMetaData gp, String addr) {
        boolean failed = FileSystemUtils.isEmpty(addr) || gp == null
                || !gp.get().isValid();
        if (!failed) {
            // Update the recent addresses list
            List<Pair<String, GeoPointMetaData>> tmp = new ArrayList<>();
            for (int i = 0; i < RECENT_ADDRESSES.size(); ++i) {
                if (!RECENT_ADDRESSES.get(i).first.equals(addr)) {
                    tmp.add(RECENT_ADDRESSES.get(i));
                }
            }
            RECENT_ADDRESSES.clear();
            RECENT_ADDRESSES.addAll(tmp);
            RECENT_ADDRESSES.add(0, new Pair<>(addr, gp));
            saveRecentlyUsed();
        }

        // Show coordinate format when reverse geocode lookup fails but we have a point
        if (failed && gp != null && gp.get().isValid()) {
            addr = CoordinateFormatUtilities.formatToString(gp.get(),
                    _coordFormat);
        }

        // Update resolved start or destination address
        if (et.getId() == R.id.route_start_address) {
            if (failed && RouteMapReceiver.isNetworkAvailable()) {
                Toast.makeText(_context,
                        R.string.route_plan_start_address_not_found,
                        Toast.LENGTH_LONG).show();
            }

            _resolvedStartPoint = gp;
            _resolvedStartAddr = addr;
        } else if (et.getId() == R.id.route_dest_address) {
            if (failed && RouteMapReceiver.isNetworkAvailable()) {
                Toast.makeText(_context,
                        R.string.route_plan_dest_address_not_found,
                        Toast.LENGTH_LONG).show();
            }

            _resolvedDestPoint = gp;
            _resolvedDestAddr = addr;
        } else {
            updateWaypointPoint(et, gp, addr);
        }
        //drop marker on map for feedback of point clicked
        dropMarker(et, gp);

        if (!FileSystemUtils.isEmpty(addr))
            et.setText(addr);
        else
            et.setTextColor(0xFFFF6666);
    }

    private void onAddressResolved() {
        if (_pendingLookups == 0 && _finishingLookup) {
            if (_resolvedStartPoint != null
                    && _resolvedStartPoint.get().isValid()
                    && _resolvedDestPoint != null
                    && _resolvedDestPoint.get().isValid()) {

                // One last check that the start and destination aren't the same
                if (_resolvedStartPoint.get()
                        .distanceTo(_resolvedDestPoint.get()) < 1) {
                    Toast.makeText(_context,
                            R.string.route_plan_start_dest_same,
                            Toast.LENGTH_LONG).show();
                    _finishingLookup = false;
                    return;
                }

                //check the waypoint for valid data(ie a geopoint set to each required waypoint)
                //dont auto complete the route plan unless all waypoints have valid addresses
                if (!_requiredWaypoints.isEmpty()) {
                    boolean requiredWaypointsAreValid = true;
                    for (int i = 0; i < _requiredWaypoints.size(); i++) {
                        RequiredWaypointViewManager model = _requiredWaypoints
                                .get(i);
                        if (model.getPoint() == null) {
                            requiredWaypointsAreValid = false;
                            break;
                        }
                    }

                    if (!requiredWaypointsAreValid) {
                        Toast.makeText(_context,
                                R.string.route_plan_waypoint_address_empty,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                // Finishing up and all lookups completed successfully
                if (_addrDialog != null)
                    _addrDialog.dismiss();

                // Add start waypoint
                _route.addMarker(Route.createWayPoint(_resolvedStartPoint,
                        UUID.randomUUID().toString()));

                //fill in the required waypoints to the route
                for (RequiredWaypointViewManager requiredWaypoint : _requiredWaypoints) {
                    _route.addMarker(
                            Route.createWayPoint(requiredWaypoint.getPoint(),
                                    UUID.randomUUID().toString()));
                }

                //add destination waypoint
                _route.addMarker(Route.createWayPoint(_resolvedDestPoint,
                        UUID.randomUUID().toString()));

                // Show route details and plan route
                onFinish(_autoPlan);
            } else
                // Failed to find final address, cancel finishing state
                _finishingLookup = false;
        }
    }

    public void showRecentAddresses(final EditText et) {
        if (RECENT_ADDRESSES.isEmpty()) {
            Toast.makeText(_context, R.string.route_plan_no_recent_addresses,
                    Toast.LENGTH_LONG).show();
            return;
        }

        final String[] addresses = new String[RECENT_ADDRESSES.size()];
        for (int i = 0; i < addresses.length; ++i)
            addresses[i] = RECENT_ADDRESSES.get(i).first;

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(R.string.route_plan_recent_addresses);
        b.setItems(addresses, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                String addr = addresses[w];
                GeoPointMetaData gp = RECENT_ADDRESSES.get(w).second;
                if (gp != null) {
                    if (et == _startAddr) {
                        _resolvedStartAddr = addr;
                        _resolvedStartPoint = gp;
                    } else if (et == _destAddr) {
                        _resolvedDestAddr = addr;
                        _resolvedDestPoint = gp;
                    } else {
                        updateWaypointPoint(et, gp, addr);
                    }
                    dropMarker(et, gp);
                }
                et.setText(addr);
                d.dismiss();
            }
        });
        b.show();
    }

    public void startWaypointMapSelect(EditText et, final int prompt) {
        _waypointAddr = et;
        startMapSelect(et, prompt);
    }

    private void startMapSelect(final EditText et, final int prompt) {
        if (_addrDialog != null && _addrDialog.isShowing()) {
            Bundle bundle = new Bundle();
            bundle.putString("prompt", _context.getString(prompt));
            bundle.putParcelable("callback", new Intent(MAP_CLICKED)
                    .putExtra("routeUID", _route.getUID())
                    .putExtra("textId", et.getId()));
            ToolManagerBroadcastReceiver.getInstance().startTool(
                    MapClickTool.TOOL_NAME, bundle);
            if (ToolManagerBroadcastReceiver.getInstance()
                    .getActiveTool() instanceof MapClickTool) {
                _addrDialog.hide();
                DropDownManager.getInstance().hidePane();
            }
        }
    }

    /**
     * Show CoordinateEntry dialog to manually input location for route's start/destination
     *
     * @param et Either the Start or Stop address EditText
     * @param etGeoPointData GeoPointMetaData of the location defined in the respective
     *                       EditText; used to edit existing location, can be null
     */
    public void showCoordEntryDialog(final EditText et,
            final GeoPointMetaData etGeoPointData) {
        CoordinateEntryCapability.ResultCallback2 callback = new CoordinateEntryCapability.ResultCallback2() {
            @Override
            public void onResultCallback(String paneId, String pane,
                    GeoPointMetaData point,
                    String affiliation) {
                lookupAddress(et, point.get(), false);
            }
        };

        CoordinateEntryCapability coordEntry = CoordinateEntryCapability
                .getInstance(_context);
        coordEntry.showDialog("Enter Location",
                etGeoPointData,
                true,
                false,
                MapView.getMapView().getCenterPoint(),
                null,
                false,
                callback);
    }

    /**
     * Returns a dialog builder that contains a dialog to modify the route's basic details
     * (driving/walking, infil/exfil, primary/secondary)
     *
     * @param route The route to edit
     * @param context MapView context
     * @param creation boolean indicating if the route is being created or not
     *
     * @return Dialog builder, as well as a continuation allowing clients to listen for
     *  route method update events.
     */
    public static Pair<AlertDialog.Builder, Cont<Route.RouteMethod>> getDetailsDialog(
            final Route route,
            final Context context,
            boolean creation) {
        List<Cont.Callback<Route.RouteMethod>> callbacks = new ArrayList();
        final Cont<Route.RouteMethod> k = new Cont<Route.RouteMethod>() {
            @Override
            public void registerCallback(Callback<Route.RouteMethod> callback) {
                callbacks.add(callback);
            }
        };

        final AtakPreferences prefs = AtakPreferences.getInstance(context);

        // open route details dialog
        View view = LayoutInflater.from(context).inflate(
                R.layout.route_initialize_view, MapView.getMapView(), false);

        // get all the spinners
        final Spinner checkPointOrder = view
                .findViewById(R.id.check_point_order);
        final Spinner driveOrWalkS = view
                .findViewById(R.id.driving_walking_option);
        final Spinner infilOrExfilS = view
                .findViewById(R.id.infil_exfil_option);
        final Spinner primOrSecondaryS = view
                .findViewById(R.id.primary_secondary_option);

        /*
            checks if the tool is being created first time
            if being created grab the preference set by user
            for the travel type
         */
        if (!creation) {
            driveOrWalkS
                    .setSelection(route.getRouteMethod().id);
        } else {
            //get default route travel type from preferences
            driveOrWalkS.setSelection(Integer.parseInt(
                    prefs.get("default_route_travel_type", "0")));
            checkPointOrder.setVisibility(View.GONE);
            infilOrExfilS.setVisibility(View.GONE);
            primOrSecondaryS.setVisibility(View.GONE);
        }

        primOrSecondaryS.setSelection(route.getRouteType().id);
        infilOrExfilS.setSelection(route.getRouteDirection().id);
        checkPointOrder.setSelection(route.getRouteOrder().id);

        driveOrWalkS.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                            View arg1, int position, long id) {

                        if (position < 0)
                            return;

                        final Route.RouteMethod rm = Route.RouteMethod
                                .values()[position];
                        route.setRouteMethod(rm.text);
                        prefs.set("default_route_travel_type",
                                position + "");

                        if (MetricsApi.shouldRecordMetric()) {
                            Bundle b = new Bundle();
                            b.putString(MetricsUtils.FIELD_MAPITEM_TYPE,
                                    route.getUID());
                            b.putString("value", rm.text);
                            b.putString(MetricsUtils.FIELD_ELEMENT_NAME,
                                    "driving_walking_option");
                            b.putString(MetricsUtils.FIELD_INFO,
                                    MetricsUtils.EVENT_STATUS_SUCCESS);
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "RouteCreationDialog", b);
                        }

                        for (Cont.Callback<Route.RouteMethod> callback : callbacks)
                            callback.onInvoke(rm);
                    }

                });

        infilOrExfilS.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                            View arg1, int position, long id) {

                        if (route.getRouteDirection().id == position)
                            return;

                        if (position < 0)
                            return;

                        final Route.RouteDirection rd = Route.RouteDirection
                                .values()[position];

                        //Log.d(TAG, "SHB - selected direction: " + rd.text);
                        route.setRouteDirection(rd.text);
                        if (rd == Route.RouteDirection.Infil) {
                            checkPointOrder
                                    .setSelection(
                                            Route.RouteOrder.Ascending.id);
                        } else {
                            checkPointOrder
                                    .setSelection(
                                            Route.RouteOrder.Descending.id);
                        }

                        if (MetricsApi.shouldRecordMetric()) {
                            Bundle b = new Bundle();
                            b.putString(MetricsUtils.FIELD_MAPITEM_TYPE,
                                    route.getUID());
                            b.putString("value", rd.text);
                            b.putString(MetricsUtils.FIELD_ELEMENT_NAME,
                                    "infil_exfil_option");
                            b.putString(MetricsUtils.FIELD_INFO,
                                    MetricsUtils.EVENT_STATUS_SUCCESS);
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "RouteCreationDialog", b);
                        }
                    }

                });

        primOrSecondaryS.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                            View arg1, int position, long id) {
                        if (position < 0)
                            return;

                        final Route.RouteType rt = Route.RouteType
                                .values()[position];
                        //Log.d(TAG, "SHB - selected type: " + rt.text);
                        route.setRouteType(rt.text);

                        if (MetricsApi.shouldRecordMetric()) {
                            Bundle b = new Bundle();
                            b.putString(MetricsUtils.FIELD_MAPITEM_TYPE,
                                    route.getUID());
                            b.putString("value", rt.text);
                            b.putString(MetricsUtils.FIELD_ELEMENT_NAME,
                                    "primary_secondary_option");
                            b.putString(MetricsUtils.FIELD_INFO,
                                    MetricsUtils.EVENT_STATUS_SUCCESS);
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "RouteCreationDialog", b);
                        }
                    }

                });

        checkPointOrder.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                            View arg1, int position, long id) {

                        if (position < 0)
                            return;

                        final Route.RouteOrder ro = Route.RouteOrder
                                .values()[position];
                        //Log.d(TAG, "SHB - selected order: " + ro.text);
                        route.setRouteOrder(ro.text);

                        if (MetricsApi.shouldRecordMetric()) {
                            Bundle b = new Bundle();
                            b.putString(MetricsUtils.FIELD_MAPITEM_TYPE,
                                    route.getUID());
                            b.putString("value", ro.text);
                            b.putString(MetricsUtils.FIELD_ELEMENT_NAME,
                                    "check_point_order");
                            b.putString(MetricsUtils.FIELD_INFO,
                                    MetricsUtils.EVENT_STATUS_SUCCESS);
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "RouteCreationDialog", b);
                        }
                    }

                });

        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(creation ? R.string.route_select_type
                : R.string.route_select_details);
        adb.setView(view);
        adb.setPositiveButton(R.string.ok, null);
        //adb.setCancelable(false);

        return new Pair(adb, k);
    }
}
