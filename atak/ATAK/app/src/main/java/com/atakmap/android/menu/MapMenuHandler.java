package com.atakmap.android.menu;

import com.atakmap.android.maps.MapItem;

/**
 * Interface for the modifying {@link MapMenuWidget} instances immediately before they are shown.
 *
 * @see MapMenuFactory
 * @see MapMenuReceiver#registerMapMenuHandler(MapMenuHandler)
 */
public interface MapMenuHandler {

    /**
     * Updates a {@link }MapMenuWidget} for a {@link MapItem} before it is presented to the user.
     *
     * <P>This method should be a no-op if the specified {@link MapItem} is not supported/not of
     * interest.
     *
     * @param item      The item
     * @param itemMenu  The menu that will be shown. The menu may be updated by this handler.
     */
    void updateMenu(MapItem item, MapMenuWidget itemMenu);
}