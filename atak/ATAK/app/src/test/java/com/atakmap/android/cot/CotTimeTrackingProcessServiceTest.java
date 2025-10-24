
package com.atakmap.android.cot;

import com.atakmap.android.util.TimeTrackingProcessService;

import org.junit.Assert;
import org.junit.Test;

public class CotTimeTrackingProcessServiceTest {

    @Test
    public void simple_succession() {
        TimeTrackingProcessService service = new TimeTrackingProcessService();
        TimeTrackingProcessService.PendingToken token = service.begin("uid",
                10);
        Assert.assertNotNull(token);
        Assert.assertEquals(token.getUid(), "uid");
        Assert.assertEquals(token.getPendingTimestamp(), 10);
        Assert.assertEquals(token.getRecordedTimestamp(), 0);
        Assert.assertTrue(service.commit(token));
        TimeTrackingProcessService.PendingToken tokenNull = service.begin("uid",
                10);
        Assert.assertNull(tokenNull);
        TimeTrackingProcessService.PendingToken token2 = service.begin("uid",
                11);
        Assert.assertNotNull(token2);
        Assert.assertEquals(token2.getRecordedTimestamp(),
                token.getPendingTimestamp());
        Assert.assertEquals(token2.getPendingTimestamp(), 11);
        service.cancel(token2);
        TimeTrackingProcessService.PendingToken token3 = service.begin("uid",
                11);
        Assert.assertNotNull(token3);
        Assert.assertEquals(token3.getRecordedTimestamp(),
                token.getPendingTimestamp());
        Assert.assertEquals(token3.getPendingTimestamp(), 11);
    }

    @Test
    public void overshadow_callback() {
        TimeTrackingProcessService service = new TimeTrackingProcessService();
        boolean[] cb_called = {
                false
        };
        final TimeTrackingProcessService.PendingToken[] token = {
                null
        };
        token[0] = service.begin("uid", 10,
                new TimeTrackingProcessService.Callbacks() {
                    @Override
                    public void onPendingEventProcessOvershadowed(
                            TimeTrackingProcessService.PendingToken shadowedProcess) {
                        cb_called[0] = true;
                        Assert.assertNotNull(shadowedProcess);
                        Assert.assertSame(token[0], shadowedProcess);
                    }
                });
        TimeTrackingProcessService.PendingToken newerToken = service
                .begin("uid", 11);
        Assert.assertTrue(cb_called[0]);
    }
}
