package gov.tak.api.engine.map.cache;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class DownloadTriggerStatusTest extends KernelJniTest {

    @Test
    public void test_roudtrip_values() {
        int triggerId = 1337;
        DownloadStatus downloadStatus = DownloadStatus.STARTED;
        int completedTileCount = 100;
        int totalTileCount = 1000;
        long startTimeUTC = 500;
        long estimatedBytesRemaining = 1024 * 50;
        double estimatedSecondsRemaining = 600.0;
        double currentBytesPerSecond = 1024.0;
        DownloadTriggerStatus status = new DownloadTriggerStatus(triggerId, downloadStatus,
                completedTileCount, totalTileCount, startTimeUTC, estimatedBytesRemaining,
                estimatedSecondsRemaining, currentBytesPerSecond);
        Assert.assertEquals(status.getTriggerId(), triggerId);
        Assert.assertEquals(status.getDownloadStatus(), downloadStatus);
        Assert.assertEquals(status.getCompletedTileCount(), completedTileCount);
        Assert.assertEquals(status.getTotalTileCount(), totalTileCount);
        Assert.assertEquals(status.getStartTimeUTC(), startTimeUTC);
        Assert.assertEquals(status.getEstimatedBytesRemaining(), estimatedBytesRemaining);
        Assert.assertEquals(status.getEstimatedSecondsRemaining(), estimatedSecondsRemaining,
                0.0);
        Assert.assertEquals(status.getCurrentBytesPerSecond(), currentBytesPerSecond, 0.0);
    }
}
