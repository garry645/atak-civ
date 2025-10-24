
package com.atakmap.android.routes;

import androidx.annotation.Nullable;

/**
 * An interface allowing ATAK plugins to specify automated route planners to be registered with
 *  ATAK core, so that they can hook into ATAK's core "Routes" tool as a method for users to
 *  plan a route by launching a custom tool defined by the plugin.
 * <p>
 * This interface should only be used by "manual" tools (i.e. route planning tools in which the user
 *  manually enters all points on the route). If your tool is "automatic" (specifically, if it can
 *  be thought of as generating a route given just a start and end point, together with some settings),
 *  you should use [RoutePlannerInterface] instead.
 * <p>
 * Can be registered/unregistered with ATAK by using RoutePlannerManager.
 */
public interface ManualRoutePlannerInterface2 {
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
     * Gets the route method associated with this planner. If set (default is null),
     *  this planner will only be shown if the user selects the matching route method
     *  when planning a route.
     */
    @Nullable
    default Route.RouteMethod getRouteMethod() {
        return null;
    }

    /**
     * Action string used to broadcast an intent prompting the plugin registering
     *  the manual route planner to launch their implementation of the manual
     *  route planner.
     */
    String getAction();

}
