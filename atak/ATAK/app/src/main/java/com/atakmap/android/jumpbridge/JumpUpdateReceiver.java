
package com.atakmap.android.jumpbridge;

import android.util.Pair;

import com.atakmap.android.jumpbridge.Options.FieldOptions;
import com.atakmap.android.jumpbridge.Options.Unit;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * This class is used to get realtime jump information from JumpMaster for use with plugins
 * @deprecated
 * @see com.atakmap.android.databridge.DatasetProviderManager and the associated sample plugins
 * that should now be used to transmit data between plugins.
 */
@Deprecated
@DeprecatedApi(since = "5.1", forRemoval = true, removeAt = "5.4")
public abstract class JumpUpdateReceiver {

    public static final String JUMP_MARKERS_KEY = "Jump Markers";
    public static final String LANDING_PATTERN_KEY = "Landing Pattern";
    public static final String WIND_CONE_KEY = "Wind Cone";

    private final List<Pair<FieldOptions, Unit>> configMap = new ArrayList<>();

    /**
     * Add a field to listen for updates to.
     * 
     * @param fo - the field to be notified about (ex. FieldOptions.ALTITUDE)
     * @param u - the unit to use for that field (ex. Unit.FEET_AGL)
     */
    public void addField(FieldOptions fo, Unit u) {
        //check to see if that field and unit pair is already being listened for
        for (Pair<FieldOptions, Unit> pair : configMap) {
            if (pair.first == fo && pair.second == u) {
                return;
            }
        }

        configMap.add(new Pair<>(
                fo, u));
    }

    /**
     * Remove a field to the list of fields to give updates to.
     * 
     * @param fo - the field to be notified about (ex. FieldOptions.ALTITUDE)
     * @param u - the unit to use for that field (ex. Unit.FEET_AGL)
     */
    public void removeField(FieldOptions fo, Unit u) {
        for (Pair<FieldOptions, Unit> pair : configMap) {
            if (pair.first == fo && pair.second == u) {
                configMap.remove(pair);
                return;
            }
        }
    }

    /**
     * Returns the fields with units that this JumpUpdateReceiver instance is listening for
     */
    public List<Pair<FieldOptions, Unit>> getFields() {
        return configMap;
    }

    /**
     * This method is called when a Jump Starts and contains the markers in the Jump Plan
     * as well as the wind cone (if planned)
     *
     * @param dip - the Marker for the DIP
     * @param dipGroup - the the group that contains the markers belonging to this jump plan
     * @param markerMap - a map containing a list of Marker UIDs for each group of markers belonging
     *                  to the jump plan. For example the 1000ft markers that are part of the jump
     *                  plan will have the key "Jump Plan" and the value will be a list of the UIDs
     *                  of the Markers belonging to the Jump Plan.
     */
    public abstract void jumpPlanned(Marker dip, MapGroup dipGroup,
            HashMap<String, ArrayList<String>> markerMap);

    /**
     * This method is called when a Jump Starts
     * 
     * @param dip - the primary DIP marker
     */
    public abstract void jumpStarted(Marker dip, double startPlannedHeading);

    /**
     * This method is called when a Jump Ends
     */
    public abstract void jumpEnded();

    /**
     * This method is called when an update has been received for a field
     * this receiver has subscribed to.
     * 
     * @param fo - the field that's value was updated
     * @param val - the string value of the field without the units string
     */
    public abstract void updateField(FieldOptions fo, Unit u, String val);

    /**
     * This method will be called when the planned jumper heading is changed, due to a planned turn
     */
    public abstract void updatePlannedJumpHeading(double heading);
}
