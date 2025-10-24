
package com.atakmap.android.routes;

import android.app.AlertDialog;

import gov.tak.api.annotation.Nullable;

/**
 * An interface allowing ATAK plugins to specify automated route planners to be registered with
 *  ATAK core, so that they can hook into ATAK's core "Routes" tool as a method for users to
 *  automatically plan routes given a start and end point.
 * <p>
 * For enhanced route planners registered for a plugin which require more user intervention
 *  to create a route, and for which the scheme of generating a route from a start and end point
 *  does not make sense, see [ManualRoutePlannerInterface].
 * <p>
 * Can be registered/unregistered with ATAK by using RoutePlannerManager.
 */
public interface RoutePlannerInterface2 {
    /**
     * Gets the unique identifier of the planner.
     *
     * @return the unique identifier of the planner
     */
    String getUniqueIdenfier();

    /**
     * Gets the descriptive name of the planner.
     *
     * @return the descriptive name of the planner
     */
    String getDescriptiveName();

    /**
     * Planner requires a network to be used.
     *
     * @return true if an active network is required.
     */
    boolean isNetworkRequired();

    /**
     * Gets the RouteGenerationTask for this planner that is run when initially generating a route.
     * @param routeGenerationEventListener The listener that should be associated with this task.
     * @return A RouteGenerationTask for this planner.
     */
    RouteGenerationTask getRouteGenerationTask(
            RouteGenerationTask.RouteGenerationEventListener routeGenerationEventListener);

    /**
     * Gets the additional options specific for the planner that may effect the
     * results.
     */
    RoutePlannerOptionsView getOptionsView(AlertDialog parent);

    /**
     * Gets any additional options for the planner that are needed at the time of navigating a route.
     *
     * @return Null if the planner does not support additional options, the options otherwise
     */
    RoutePlannerOptionsView getNavigationOptions(AlertDialog parent);

    /**
     * Gets whether or not the planner is capable of supporting re-routing.
     */
    boolean isRerouteCapable();

    /**
     * Gets whether or not the planner is capable of supporting routing around regions.
     */
    boolean canRouteAroundRegions();

    /**
     * Gets the route method associated with this planner. If set (default is null),
     *  this planner will only be shown if the user selects the matching route method
     *  when planning a route.
     */
    @Nullable
    default Route.RouteMethod getRouteMethod() {
        return null;
    }
}
