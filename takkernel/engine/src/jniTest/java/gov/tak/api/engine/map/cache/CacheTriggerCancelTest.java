package gov.tak.api.engine.map.cache;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class CacheTriggerCancelTest extends KernelJniTest {

    @Test
    public void test_roundtrip_values() {
        CacheTriggerCancel event = new CacheTriggerCancel(1337);
        Assert.assertEquals(event.getTriggerId(), 1337);
    }

    public class TestSub {
        CacheTriggerCancel receivedEvent;
        @Subscribe
        public void onEvent(CacheTriggerCancel event) {
            receivedEvent = event;
        }
    }

    @Test
    public void test_eventbus_trip() {
        CacheTriggerCancel event = new CacheTriggerCancel(1337);
        EventBus eventBus = EventBus.getDefault();
        TestSub sub = new TestSub();
        eventBus.register(sub);
        EventBus.getDefault().post(event);
        eventBus.unregister(sub);
        Assert.assertEquals(sub.receivedEvent, event);
    }

}
