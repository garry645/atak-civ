package gov.tak.api.engine.map.cache;

/**
 * Represents the status of a download initiated in the caching service
 */
public final class DownloadTriggerStatus {

    /**
     * Create a status event
     *
     * @param triggerId the trigger id that references a previous DownloadCacheTrigger
     * @param downloadStatus the current download status
     * @param completedTileCount the current number of tiles downloaded
     * @param totalTileCount the total number of tiles associated with the download request
     * @param startTimeUTC the time in UTC when the download was initiated
     * @param estimatedBytesRemaining estimate of how many bytes remain pending in the download
     * @param estimatedSecondsRemaining estimate of how many seconds remain in the download given
     *                                  the current rate
     * @param currentBytesPerSecond the current download bytes-per-second rate
     */
    public DownloadTriggerStatus(int triggerId, DownloadStatus downloadStatus, int completedTileCount,
                                 int totalTileCount, long startTimeUTC, long estimatedBytesRemaining,
                                 double estimatedSecondsRemaining, double currentBytesPerSecond) {
        trigId = triggerId;
        dlStatus = downloadStatus;
        totTileCount = totalTileCount;
        comTileCount = completedTileCount;
        startTime = startTimeUTC;
        estBytesRemaining = estimatedBytesRemaining;
        estSecRemaining = estimatedSecondsRemaining;
        currBytesPerSec = currentBytesPerSecond;
    }

    /**
     * The unique trigger ID that references a previous DownloadCacheTrigger trigger ID
     */
    public int getTriggerId() {
        return trigId;
    }

    /**
     * Current status of the download
     */
    public DownloadStatus getDownloadStatus() {
        return dlStatus;
    }

    /**
     * The current number of tiles downloaded
     */
    public int getCompletedTileCount() {
        return comTileCount;
    }

    /**
     * The total number of tiles associated with the download request
     */
    public int getTotalTileCount() {
        return totTileCount;
    }

    /**
     * The time in UTC when the download was initiated
     */
    public long getStartTimeUTC() {
        return startTime;
    }

    /**
     * Estimate of how many bytes remain pending in the download
     */
    public long getEstimatedBytesRemaining() {
        return estBytesRemaining;
    }

    /**
     * Estimate of how many seconds remain in the download given the current rate
     */
    public double getEstimatedSecondsRemaining() {
        return estSecRemaining;
    }

    /**
     * The current download bytes-per-second rate
     */
    public double getCurrentBytesPerSecond() {
        return currBytesPerSec;
    }

    private int trigId;
    private DownloadStatus dlStatus;
    private int comTileCount;
    private int totTileCount;
    private long startTime;
    private long estBytesRemaining;
    private double estSecRemaining;
    private double currBytesPerSec;
}
