
package com.atakmap.android.gui.coordinateentry;

import android.view.View;

import androidx.annotation.Nullable;

import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import gov.tak.api.util.Disposable;

public interface CoordinateEntryPane extends Disposable {

    class CoordinateException extends Exception {
        final String msg;

        public CoordinateException(String msg, Exception e) {
            initCause(e);
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }
    }

    interface OnChangedListener {
        /**
         * Fires when there are any changes made to the coordinate via the entry page that
         * would result in a change to the location.   This should only be fired if the changes
         * are a result of human entry.
         */
        void onChange(CoordinateEntryPane coordinateEntryPane);
    }

    /**
     * Returns the uid associated with the CoordinateEntryCapability.
     * @return the unique identifier
     */
    String getUID();

    /**
     * Returns the name of the coordinate entry panel.   This should be a human readable
     * name that accurately describes the capability.
     * @return the name of the coordinate entry panel
     */
    String getName();

    /**
     * The graphical user experience used to enter the information or render the current geopoint
     * @return the view that is inflated.
     */
    View getView();

    /**
     * Called when the pane is visible with the original point provided when the dialog is
     * originally shown and the current point being used as a workspace.   Can be called while
     * the view is visible for example when the elevation is updated or when the display is cleared.
     * @param currentPoint the new point based on the user manipulation.  Please note that if the
     *                     current point is NULL, then the expectation is that the is cleared.
     * @param editable if the original point is editable or not
     */
    void onActivate(@Nullable GeoPointMetaData currentPoint, boolean editable);

    /**
     * The result of manipulation when the screen is no longer showing.   This can be called
     * when the user is changing the panels or when the overall coordinate entry dialog is
     * dismissed.
     * @return the geopoint that is a result of the human entry on the screen and can be null
     * if not filled in or if there is an error with the human input.
     * @throws CoordinateException a coordinate exception if the current screen information
     * cannot be turned into a geopoint.
     */
    GeoPointMetaData getGeoPointMetaData() throws CoordinateException;

    /**
     * Autofill can be called to clear out the current entered information and fill in the
     * most granular parts of the screen
     * @param point the geopoint can be null
     */

    void autofill(@Nullable GeoPointMetaData point);

    /**
     * Given a coordinate, produce a stringified representation of the geopoint based on the
     * selected coordinate formatting rules without an altitude.   This can be called no matter
     * the current state of the pane and should not impact the current state of a displayed
     * pane.
     * @param point the geopoint to pass in - please note that this can be null
     * @return a human readable string if one exists or null if none exists.
     */
    String format(@Nullable GeoPointMetaData point);

    /**
     * Sets a listener on the pane for when the current pane no longer matches the original
     * input via human input.
     * @param onChangedListener the on change listener.
     */
    void setOnChangedListener(OnChangedListener onChangedListener);

}
