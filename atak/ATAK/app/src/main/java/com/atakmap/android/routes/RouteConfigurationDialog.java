
package com.atakmap.android.routes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.routes.routearound.RouteAroundRegionManager;
import com.atakmap.android.routes.routearound.RouteAroundRegionManagerView;
import com.atakmap.android.routes.routearound.RouteAroundRegionViewModel;
import com.atakmap.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Similar to {@link RouteCreationDialog}, except only allows the user to configure
 *  route planning options -- not including the start and end point of the route to be planned.
 */
// TODO: This should be refactored to better share code with RouteCreationDialog.java
public class RouteConfigurationDialog {

    private static final Comparator<String> SORT_PLANNERS = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    private final RouteAroundRegionManager _routeAroundManager;
    private final RouteAroundRegionViewModel _routeAroundVM;
    private final LinearLayout _routeAroundOptions;
    private final AtakPreferences _prefs;
    private final Context _context;
    private final MapView _mapView;
    private final Callback<RoutePlannerInterface2> callback;
    private final RouteAroundRegionManagerView regionManagerView;
    private final AlertDialog _configDialog;

    public RouteConfigurationDialog(Context context, MapView mapView,
            Callback<RoutePlannerInterface2> callback) {
        this._context = context;
        this._mapView = mapView;
        this.callback = callback;

        _prefs = AtakPreferences.getInstance(_context);
        _routeAroundManager = RouteAroundRegionManager.getInstance();
        _routeAroundVM = new RouteAroundRegionViewModel(_routeAroundManager);
        _routeAroundOptions = (LinearLayout) LayoutInflater.from(_context)
                .inflate(R.layout.route_around_layout, null);

        regionManagerView = new RouteAroundRegionManagerView(_mapView,
                new RouteAroundRegionViewModel(_routeAroundManager));

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

        _configDialog = getDialog();

        openRouteAroundManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                regionManagerView.setParentParentDialog(_configDialog);
                dialog.show();
            }
        });
    }

    public void show() {
        _configDialog.show();
    }

    private AlertDialog getDialog() {
        ScrollView routePlanView = (ScrollView) LayoutInflater.from(_context)
                .inflate(
                        R.layout.route_planner_options_layout, _mapView, false);

        LinearLayout _routePlanOptions = routePlanView
                .findViewById(R.id.route_plan_options);

        final Spinner planSpinner = routePlanView.findViewById(
                R.id.route_plan_method);

        // Load route planner interfaces into spinner
        MapComponent mc = ((MapActivity) _context).getMapComponent(
                RouteMapComponent.class);
        if (mc == null)
            return null;

        final boolean network = RouteMapReceiver.isNetworkAvailable();
        if (!network)
            Toast.makeText(_context,
                    R.string.network_not_available,
                    Toast.LENGTH_SHORT).show();

        RoutePlannerManager routePlanner = ((RouteMapComponent) mc)
                .getRoutePlannerManager();

        final ArrayList<RoutePlannerInterface2> _planners = new ArrayList<>(
                routePlanner.getRoutePlanners2());

        List<String> plannerNames = new ArrayList<>();
        for (RoutePlannerInterface2 k : _planners)
            if (!k.isNetworkRequired() || network)
                plannerNames.add(k.getDescriptiveName());

        Collections.sort(plannerNames, SORT_PLANNERS);

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

        // Find selected planner
        String plannerName = (String) planSpinner
                .getSelectedItem();
        RoutePlannerInterface2 _autoPlan = null;
        for (RoutePlannerInterface2 k : _planners) {
            if (plannerName.equals(k
                    .getDescriptiveName())) {
                _autoPlan = k;
                break;
            }
        }
        if (_autoPlan == null) {
            // Should never happen, but just in case
            Toast.makeText(_context,
                    R.string.route_plan_unknown_host,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        LinearLayout.LayoutParams lpView = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(R.string.configure_route_planner);
        b.setView(routePlanView);
        b.setPositiveButton(R.string.done,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String plannerName = (String) planSpinner
                                .getSelectedItem();
                        _prefs.set("lastUsedPlanner", plannerName);
                        RoutePlannerInterface2 planner = null;
                        for (RoutePlannerInterface2 k : _planners) {
                            if (plannerName.equals(k
                                    .getDescriptiveName())) {
                                planner = k;
                                break;
                            }
                        }
                        callback.accept(planner);
                    }
                });
        b.setNegativeButton(R.string.cancel, null);
        AlertDialog d = b.create();

        RouteCreationDialog.setupPlanSpinner(planSpinner, _planners,
                _routePlanOptions,
                d, _routeAroundOptions);

        _routePlanOptions.addView(
                _autoPlan.getOptionsView(d),
                lpView);

        return d;
    }

    public interface Callback<A> {
        void accept(A a);
    }
}
