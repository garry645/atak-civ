package gov.tak.api.engine.map.cache;

/**
 * Event indicating a service cache trigger cancellation
 */
public class CacheTriggerCancel {

    /**
     * Create a cancel event with the given unique trigger ID
     *
     * @param triggerId the unique trigger ID to cancel
     */
    public CacheTriggerCancel(int triggerId) {
        trigId = triggerId;
    }

    /**
     * @return the unique trigger ID
     */
    public int getTriggerId() {
        return trigId;
    }

    private int trigId;
}
