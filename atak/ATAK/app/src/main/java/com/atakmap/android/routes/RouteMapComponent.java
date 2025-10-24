
package com.atakmap.android.routes;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.cot.importer.CotImporterManager;
import com.atakmap.android.data.ClearContentRegistry;
import com.atakmap.android.drawing.DrawingToolsMapReceiver;
import com.atakmap.android.editableShapes.EditablePolylineReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.DoghouseReceiver;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.DefaultMapGroupOverlay;
import com.atakmap.android.routes.cot.RouteImporter;
import com.atakmap.android.routes.elevation.RouteElevationBroadcastReceiver;
import com.atakmap.android.routes.nav.NavigationCueHandler;
import com.atakmap.android.routes.routearound.RegionRemovalListener;
import com.atakmap.android.routes.routearound.RouteAroundRegionManager;
import com.atakmap.android.routes.routearound.RouteAroundRegionViewModel;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.android.widgets.AbstractWidgetMapComponent;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides for all of the routing capability within the architecture.   The
 * core capability is simple, but allows for augmentation to support more 
 * capabilities.  For a full turn by turn example, please see the vns plugin.
 */
public class RouteMapComponent extends AbstractWidgetMapComponent {

    RouteImporter _routeImporter;
    protected RouteMapReceiver _routeReceiver;
    protected EditablePolylineReceiver _editablePolylineReceiver;
    protected DoghouseReceiver _doghouseReceiver;
    protected NavigationCueHandler _cueHandler;

    protected final CotServiceRemote cotService_ = new CotServiceRemote();

    protected RouteToolbarBroadcastReceiver _routeToolbarReceiver;
    protected GoToMapTool _goTo;

    private RoutePlannerManager _routePlannerManager = new RoutePlannerManager();
    private final RouteRequiredWayPointHandler _requiredWaypointHandler = new RouteRequiredWayPointHandler();

    @Override
    protected void onCreateWidgets(final Context context, final Intent intent,
            final MapView view) {

        MapGroup parentGroup = view.getRootGroup();
        MapGroup routeGroup = parentGroup.findMapGroup("Route");
        routeGroup.setMetaBoolean("addToObjList", true);
        MapGroup navGroup = new DefaultMapGroup("Navigation");

        // Set routes to not have offscreen markers. Have to remove and readd the group to get this
        // setting to be read (currently) by ObserverNode
        routeGroup.setMetaBoolean("ignoreOffscreen", true);

        // XXX - MAPOVERLAY
        parentGroup.removeGroup(routeGroup);
        parentGroup.addGroup(routeGroup);

        // remove offscreen indicators for orphan waypoints, in case they exist
        MapGroup parentGroup2 = view.getRootGroup().findMapGroup(
                "Cursor on Target");
        MapGroup linkGroup2 = parentGroup2.findMapGroup("Waypoint");

        linkGroup2.setMetaBoolean("ignoreOffscreen", true);
        parentGroup2.removeGroup(linkGroup2);
        parentGroup2.addGroup(linkGroup2);

        view.getMapOverlayManager().addOverlay(
                new DefaultMapGroupOverlay(view, navGroup));

        MapGroup waypointGroup = new DefaultMapGroup("RouteOwnedWaypoints");
        waypointGroup.setMetaBoolean("ignoreOffscreen", true);
        waypointGroup.setMetaBoolean("addToObjList", false);
        view.getMapOverlayManager().addOverlay(
                new DefaultMapGroupOverlay(view, waypointGroup));

        executor.submit(new Runnable() {
            @Override
            public void run() {
                // GoTo map tool
                _goTo = GoToMapTool.getInstance(view);

                _routeReceiver = new RouteMapReceiver(view, routeGroup,
                        waypointGroup, navGroup, context);

                _doghouseReceiver = DoghouseReceiver.newInstance(view);
                DocumentedIntentFilter showDoghouseFilter = new DocumentedIntentFilter();
                showDoghouseFilter.addAction(
                        DoghouseReceiver.TOGGLE_DOGHOUSE_VISIBLE_ACTION);
                AtakBroadcast.getInstance().registerReceiver(_doghouseReceiver,
                        showDoghouseFilter);

                _routeImporter = new RouteImporter(view, _routeReceiver);
                _cueHandler = new NavigationCueHandler();
                CotDetailManager.getInstance().registerHandler(_cueHandler);

                DocumentedIntentFilter showFilter = new DocumentedIntentFilter();
                showFilter.addAction(DrawingToolsMapReceiver.LABEL_ACTION);
                showFilter.addAction(RouteMapReceiver.DELETE_ACTION);
                showFilter.addAction(RouteMapReceiver.SHOW_ACTION);
                showFilter.addAction(RouteMapReceiver.SHARE_ACTION);
                showFilter.addAction(RouteEditTool.TOOL_IDENTIFIER);
                showFilter.addAction(RouteMapReceiver.ROUTE_TRANSFER);
                showFilter.addAction(RouteMapReceiver.INSERT_WAYPOINT);
                showFilter.addAction(RouteMapReceiver.MANAGE_ACTION);
                showFilter.addAction(RouteMapReceiver.START_NAV);
                showFilter.addAction(RouteMapReceiver.END_NAV);
                showFilter.addAction(RouteMapReceiver.ADD_PT_FROM_BEARING);

                showFilter.addAction(ToolManagerBroadcastReceiver.BEGIN_TOOL);
                showFilter.addAction(RouteMapReceiver.UNDO);

                showFilter.addAction(RouteMapReceiver.EDIT_CUES_ACTION);
                showFilter.addCategory("com.atakmap.android.maps.INTEGRATION");
                showFilter.addAction(
                        "com.atakmap.android.maps.TOOLSELECTOR_READY");
                showFilter.addAction(
                        "com.atakmap.android.maps.EXTERNAL_PREFS_READY");
                showFilter.addAction(
                        "com.atakmap.android.maps.ROUTE_NOTI_CLICKED");

                showFilter.addAction(ToolbarBroadcastReceiver.SET_TOOLBAR);
                showFilter.addAction(ToolbarBroadcastReceiver.OPEN_TOOLBAR);

                showFilter.addAction(RouteMapReceiver.ROUTE_IMPORT);
                showFilter.addAction(RouteMapReceiver.ROUTE_EXPORT);
                showFilter.addAction(RouteMapReceiver.PROCESSING_DONE);

                showFilter.addAction(
                        RouteMapReceiver.ACTION_ROUTE_IMPORT_FINISHED);

                AtakBroadcast.getInstance().registerReceiver(_routeReceiver,
                        showFilter);
                CotImporterManager.getInstance()
                        .registerImporter(_routeImporter);

                CotDetailManager.getInstance()
                        .registerHandler(_requiredWaypointHandler);

                // Initialize association set common code
                _editablePolylineReceiver = EditablePolylineReceiver
                        .init(view, context);

                // Initialize the route creation toolbar
                _routeToolbarReceiver = new RouteToolbarBroadcastReceiver(view,
                        _routeReceiver);
                DocumentedIntentFilter filter = new DocumentedIntentFilter();
                filter.addAction(ToolManagerBroadcastReceiver.END_TOOL);
                filter.addAction(ToolManagerBroadcastReceiver.BEGIN_TOOL);

                _routeReceiver.addTools(_routeToolbarReceiver._editTool);

                AtakBroadcast.getInstance().registerReceiver(
                        _routeToolbarReceiver,
                        filter);

                // Setup route drop down receiver
                RouteElevationBroadcastReceiver.initialize(view, parentGroup);

                RegionRemovalListener regionRemovalListener = new RegionRemovalListener(
                        new RouteAroundRegionViewModel(
                                RouteAroundRegionManager.getInstance()));

                view.getMapEventDispatcher().addMapEventListener(
                        MapEvent.ITEM_REMOVED,
                        regionRemovalListener);

                ClearContentRegistry.getInstance()
                        .registerListener(dataMgmtReceiver);

                view.post(new Runnable() {
                    @Override
                    public void run() {
                        ToolsPreferenceFragment.register(
                                new ToolsPreferenceFragment.ToolPreference(
                                        context.getString(
                                                R.string.routePreferences),
                                        context.getString(
                                                R.string.routes_text31),
                                        "routePreference",
                                        context.getResources().getDrawable(
                                                R.drawable.nav_routes),
                                        new RoutePreferenceFragment()));
                    }
                });

            }
        });
    }

    @Override
    protected void onDestroyWidgets(Context context, MapView view) {
        CotImporterManager.getInstance().unregisterImporter(_routeImporter);
        CotDetailManager.getInstance().unregisterHandler(_cueHandler);
        CotDetailManager.getInstance()
                .unregisterHandler(_requiredWaypointHandler);
        AtakBroadcast.getInstance().unregisterReceiver(_routeReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(_routeToolbarReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(_doghouseReceiver);
        ClearContentRegistry.getInstance().unregisterListener(dataMgmtReceiver);

        _editablePolylineReceiver.dispose();
        cotService_.disconnect();
        _routeReceiver.dispose();
        _doghouseReceiver.dispose();
        _goTo.dispose();
    }

    protected final ClearContentRegistry.ClearContentListener dataMgmtReceiver = new ClearContentRegistry.ClearContentListener() {
        @Override
        public void onClearContent(boolean clearmaps) {

            // Delete the serialization file for the route around region manager.
            FileSystemUtils
                    .delete(RouteAroundRegionViewModel.SERIALIZATION_FILE);

        }
    };

    public RouteMapReceiver getRouteMapReceiver() {
        return _routeReceiver;
    }

    /**
     * This method is to support testing at a later date.
     **/
    protected void setRoutePlannerManager(RoutePlannerManager plannerManager) {
        _routePlannerManager = plannerManager;
    }

    /**
     * Gets the Route Planner Manager.
     *
     * @return
     */
    public RoutePlannerManager getRoutePlannerManager() {
        return _routePlannerManager;
    }

    final ExecutorService executor = Executors
            .newSingleThreadExecutor(new NamedThreadFactory(
                    "routeInitialization"));
}
