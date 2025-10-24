
package com.atakmap.android.jumpbridge;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

@Deprecated
@DeprecatedApi(since = "5.1", forRemoval = true, removeAt = "5.4")
public class JumpUpdateListeners {
    private static final List<JumpUpdateReceiver> jurList = new ArrayList<>();

    /**
     * Add a listener for JumpMaster updates.
     * @param jur the JumpUpdateReceiver to register.
     */
    public static void addReceiver(JumpUpdateReceiver jur) {
        jurList.add(jur);
    }

    /**
     * Remove a listener for JumpMaster updates.
     * @param jur the JumpUpdateReceiver to unregister.
     */
    public static void removeReceiver(JumpUpdateReceiver jur) {
        jurList.remove(jur);
    }

    /**
     * Obtain a copy of the currently registered jump update receivers.
     * @return the list of receivers
     */
    public static List<JumpUpdateReceiver> getReceivers() {
        List<JumpUpdateReceiver> retval;

        synchronized (jurList) {
            retval = new ArrayList<>(jurList);
        }

        return retval;
    }
}
