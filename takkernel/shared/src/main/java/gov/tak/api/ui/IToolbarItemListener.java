package gov.tak.api.ui;

import gov.tak.platform.ui.MotionEvent;

/**
 * Listener that is notified when actions are taken on a toolbar item.
 */
public interface IToolbarItemListener
{
    /**
     * Called when a motion event (e.g. click, long press, hover, etc.) occurs for the associated toolbar item.
     *
     * @param item      The item's identifier. May be {@code null} if no identifier was provided.
     * @param event     The motion event that triggered the callback
     */
    void onItemEvent(ToolbarItem item, MotionEvent event);
}
