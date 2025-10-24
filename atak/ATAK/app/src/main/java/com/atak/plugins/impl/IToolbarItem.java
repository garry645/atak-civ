
package com.atak.plugins.impl;

import android.graphics.drawable.Drawable;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.platform.ui.MotionEvent;

@DontObfuscate
public interface IToolbarItem {

    /**
     * @return the short name for this tool.
     * return getDescription() if unsure
     */
    String getShortDescription();

    /**
     * The icon used to populate the Tool within the Toolbar.   If this is a layer drawable the
     * first layer is applied the shadowing and the subsequent layers can be controlled by the
     * plugin for overlay purposes such as message counts or other on screen indicators.
     * @return The icon for this tool/layer/item
     */
    Drawable getIcon();

    /**
     * @return the name for this tool/layer/item
     */
    String getDescription();

    /**
     * Called when a motion event (e.g. click, long press, hover, etc.) occurs for the associated toolbar item.
     *
     * @param event     The motion event that triggered the callback
     */
    void onItemEvent(MotionEvent event);

}
