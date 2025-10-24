package gov.tak.api.events;

import gov.tak.api.annotation.NonNull;

public interface IGlobeEventService2 extends IGlobeEventService
{

    /**
     * Add a specific-event-type globe event listener to the top of the listener stack
     *
     * @param eventType the specific event type
     * @param listener  the Listener instance to add
     */
    void addSpecificGlobeEventListener(@NonNull String eventType, @NonNull IGlobeEventService.Listener listener);

    /**
     * Remove a specific-event-type globe event listener from the top of the listener stack
     *
     * @param eventType the specific event type (null is ignored)
     * @param listener  the Listener instance to remove or null to do nothing
     */
    void removeSpecificGlobeEventListener(String eventType, Listener listener);
}
