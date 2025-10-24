package gov.tak.api.ui;

import gov.tak.api.annotation.NonNull;

import java.util.List;

public interface IHostUIService {

    public interface IPaneLifecycleListener
    {
        /**
         * Called when the visibility of the panel changes.
         *
         * @param visible True if the panel is visible.
         */
        void onPaneVisible(boolean visible);

        /**
         * Called when the panel closes.
         */
        void onPaneClose();
    }

    /**
     * Shows the specified pane, if not previously showing.
     *
     * @param pane      A pane
     * @param listener  An optionally specified listener that will provide callbacks during the pane's lifecycle
     */
    void showPane(@NonNull Pane pane, IPaneLifecycleListener listener);

    /**
     * Closes the specified pane, if showing.
     *
     * @param pane  A pane
     */
    void closePane(@NonNull Pane pane);

    /**
     * Returns <code>true</code> if the specified pane is visible, <code>false</code> otherwise.
     *
     * @param pane  A pane
     *
     * @return  <code>true</code> if the specified pane is visible, <code>false</code> otherwise.
     */
    boolean isPaneVisible(@NonNull Pane pane);

    /**
     * Executes the specified {@link Runnable} on the UI thread.
     *
     * @param runnable The runnable to execute on the UI thread.
     */
    void queueEvent(@NonNull Runnable runnable);

    /**
     * Displays the specified message to the user as a host specific <I>toast</I>.
     *
     * @param message
     */
    void showToast(@NonNull String message);

    /**
     * Shows the specified message as an on screen prompt. The message will remain on the screen unless 1) replaced by a
     * subsequent call to <code>showPrompt(...)</code> or 2) {@link #clearPrompt()} is invoked.
     *
     * @param prompt The message
     */
    void showPrompt(@NonNull String prompt);

    /**
     * Clears any on screen prompt previously shown via {@link #showPrompt(String)}.
     */
    void clearPrompt();

    enum NotificationStatus {
        Information,
        Warning,
        Error
    }

    /**
     * Shows the specified notification.
     *
     * @param status    The notification type
     * @param message   The message
     * @param exception An optional {@link Throwable} associated with the notification
     */
    void showNotification(@NonNull NotificationStatus status, @NonNull String message, Throwable exception);

    /**
     * Adds the specified toolbar item
     *
     * @param toolbarItem   The item to be added
     */
    void addToolbarItem(@NonNull ToolbarItem toolbarItem);

    /**
     * Adds the specified toolbar menu
     *
     * @param toolbarItem   The button for the menu
     * @param menuItems     The menu items
     */
    void addToolbarItemMenu(@NonNull ToolbarItem toolbarItem, List<ToolbarItem> menuItems);

    /**
     * Removes the specified item from the toolbar
     *
     * @param toolbarItem   The item to be removed
     */
    void removeToolbarItem(@NonNull ToolbarItem toolbarItem);
}