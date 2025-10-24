
package com.atakmap.android.util.time;

import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * General purpose listener for when a time has changed
 * or when a moment has elapsed
 */
public interface TimeListener {

    /**
     * Called when the time is changed.
     * @param oldTime the time prior to the change
     * @param newTime the new time
     */
    void onTimeChanged(CoordinatedTime oldTime, CoordinatedTime newTime);
}
