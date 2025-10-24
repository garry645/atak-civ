package gov.tak.api.events;

import gov.tak.api.annotation.NonNull;

/**
 * Registration service for globe interaction events.
 *
 * @see IGlobeEvent
 */
public interface IGlobeEventService
{
    /**
     * The event listener interface
     */
    interface Listener
    {
        /**
         * A globe event occurred.
         *
         * @param globeEvent the event information
         * @note The implementation is free to employ a recycling scheme for IGlobeEvent instances.
         * If a record of the event should be cached outside the scope of the handler method,
         * IGlobeEvent.clone() will provide a safe to cache copy.
         */
        void onGlobeEvent(@NonNull IGlobeEvent globeEvent);
    }

    /**
     * Add an any-event-type globe event listener to the top of the listener stack
     *
     * @param listener the Listener instance to add
     */
    void addGlobeEventListener(@NonNull Listener listener);

    /**
     * Remove an any-event-type globe event listener from the top of the listener stack
     *
     * @param listener the Listener instance to remove or null to do nothing
     */
    void removeGlobeEventListener(Listener listener);

    /**
     * Push a new set of listeners on the stack. Subsequent calls to addGlobeEventListener() and
     * removeGlobeEventListener() will affect the newly pushed set.
     */
    void pushGlobeEventListeners();

    /**
     * Pop the current set of listeners off the stack. Subsequent call to addGlobeEventListener() and
     * removeGlobeEventListener() will affect the restored set at the top of the stack.
     */
    void popGlobeEventListeners();
}
