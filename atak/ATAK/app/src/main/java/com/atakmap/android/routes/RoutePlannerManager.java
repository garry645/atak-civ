
package com.atakmap.android.routes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.Nullable;

/**
 * Class to manage Route Planners.
 */
public class RoutePlannerManager {
    private final Map<String, RoutePlannerInterface2> _routePlanners = new ConcurrentHashMap<>();
    private final Map<String, ManualRoutePlannerInterface2> _manualRoutePlanners = new ConcurrentHashMap<>();

    /**
     * Gets the first Route Planner with the given Descriptive Name
     *
     * @param name the descriptive name that will be searched for
     * @return null if not matching Route Planner found
     */
    public RoutePlannerInterface2 getPlannerByName(String name) {
        RoutePlannerInterface2 planner = null;
        if (name == null) {
            throw new IllegalArgumentException("Name may not be null");
        }

        for (Map.Entry<String, RoutePlannerInterface2> k : _routePlanners
                .entrySet()) {
            if (name.equals(k.getValue().getDescriptiveName())) {
                planner = k.getValue();
                break;
            }
        }

        return planner;
    }

    /**
     * Registers the Route Planner with the given id.
     *
     * @param id the identifier for a route planner
     * @param planner the route planner
     * @return The previously registered Route Planner with the same id, if any
     */
    public RoutePlannerInterface2 registerPlanner(String id,
            RoutePlannerInterface2 planner) {
        return _routePlanners.put(id, planner);
    }

    /**
     * Unregisters the Route Planner with the given id.
     *
     * @param id the identifier for the route planner
     * @return The Route Planner that has been unregistered, if any
     */
    public RoutePlannerInterface2 unregisterPlanner(String id) {
        return _routePlanners.remove(id);
    }

    /**
     * Gets a set of all id->Route Planner mapping entries representing all registered Route
     * Planners. The set is decoupled from the manager once it is returned.
     *
     * @return the set of all route planners
     */
    public Set<RoutePlannerInterface2> getRoutePlanners2() {
        Set<RoutePlannerInterface2> rpis = new HashSet<>();
        for (RoutePlannerInterface2 rpi : _routePlanners.values())
            rpis.add((RoutePlannerInterface2) rpi);
        return rpis;
    }

    /**
     * Gets a set of all id->Route Planner mapping entries containing only entries that support
     * re-routing. The set is decoupled from the manager once it is returned.
     *
     * @return the set of all reroute planners
     */
    public Set<RoutePlannerInterface2> getReroutePlanners2() {
        Set<RoutePlannerInterface2> rpis = new HashSet<>();
        for (RoutePlannerInterface2 rpi : _routePlanners.values())
            if (rpi.isRerouteCapable())
                rpis.add((RoutePlannerInterface2) rpi);
        return rpis;
    }

    /**
     * Gets the Manual Route Planner registered with the given id.
     *
     * @param id the identifier for the specific manual route planner
     * @return null if no matching Manual Route Planner
     */
    @Nullable
    public ManualRoutePlannerInterface2 getManualPlanner(String id) {
        return _manualRoutePlanners.get(id);
    }

    /**
     * Registers the Manual Route Planner with the given id.
     *
     * @param id the identifier for a manual route planner
     * @param planner the manual route planner
     * @return The previously registered Manual Route Planner with the same id, if any
     */
    public ManualRoutePlannerInterface2 registerManualPlanner(String id,
            ManualRoutePlannerInterface2 planner) {
        return _manualRoutePlanners.put(id,
                planner);
    }

    /**
     * Unregisters the Manual Route Planner with the given id.
     *
     * @param id the identifier for the manual route planner
     * @return The Manual Route Planner that has been unregistered, if any
     */
    public ManualRoutePlannerInterface2 unregisterManualPlanner(String id) {
        return _manualRoutePlanners.remove(id);
    }

    /**
     * Gets a set of all id->Manual Route Planner mapping entries representing all registered Route
     * Planners. The set is decoupled from the manager once it is returned.
     *
     * @return the set of all manual route planners
     */
    public Set<ManualRoutePlannerInterface2> getManualRoutePlanners2() {
        Set<ManualRoutePlannerInterface2> rpis = new HashSet<>();
        for (ManualRoutePlannerInterface2 rpi : _manualRoutePlanners.values())
            rpis.add((ManualRoutePlannerInterface2) rpi);
        return rpis;
    }

    /**
     * Gets the number of registered Route Planners.
     *
     * @return The number of registered Route Planners.
     */
    public int getCount() {
        return _routePlanners.size();
    }
}
