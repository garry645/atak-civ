
package com.atakmap.android.toolbar.menu;

import android.view.View;

public interface IMenu {

    /**
     * Get the view for the menu
     * @return the view
     */
    View getView();

    /**
     * Get the identifier for the menu
     * @return the string identifier
     */
    String getIdentifier();

    /**
     * Return the preferred width for the menu
     * @return the preferred width
     */
    int getPreferredWidth();

    /**
     * Return the preferred height for the menu
     * @return the preferred height
     */
    int getPreferredHeight();
}
