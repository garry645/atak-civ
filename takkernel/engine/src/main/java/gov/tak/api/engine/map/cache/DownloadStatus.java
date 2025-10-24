package gov.tak.api.engine.map.cache;

/**
 * Status of a cache service download trigger
 */
public enum DownloadStatus {
    /**
     * The download is waiting to be started
     */
    QUEUED,

    /**
     * The download is currently pending
     */
    STARTED,

    /**
     * The download is fully complete
     */
    COMPLETED,

    /**
     * The download is incomplete and its source has become unreachable
     */
    FAILED,

    /**
     * The download was canceled
     */
    CANCELED
}
