package gov.tak.platform.events;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.events.IGlobeEventService;

public class AbstractGlobeEventServiceTest
{

    private static final String TEST_EVENT_TYPE = "test_type";

    private static class TestGlobeEventService extends AbstractGlobeEventService
    {
        public void triggerEvent()
        {
            GlobeEventBuilder builder = new GlobeEventBuilder();
            builder.setType(TEST_EVENT_TYPE);
            raiseTopLevelGlobeEvent(builder.build());
        }
    }

    private static class TestGlobeEventServiceListener implements IGlobeEventService.Listener
    {

        int callCount;
        boolean setHandled;

        @Override
        public void onGlobeEvent(IGlobeEvent globeEvent)
        {
            ++callCount;
            if (setHandled)
                globeEvent.setHandled(true);
        }
    }

    @Test
    public void concurrentManipTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        final boolean[] didRemove = {false};
        IGlobeEventService.Listener removeThyself = new IGlobeEventService.Listener()
        {
            @Override
            public void onGlobeEvent(IGlobeEvent globeEvent)
            {
                didRemove[0] = true;
                service.removeGlobeEventListener(this);
            }
        };
        TestGlobeEventServiceListener b = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener e = new TestGlobeEventServiceListener();

        service.addGlobeEventListener(b);
        service.addGlobeEventListener(removeThyself);
        service.addGlobeEventListener(e);

        service.triggerEvent();
        Assert.assertEquals(didRemove[0], true);
        Assert.assertEquals(b.callCount, 1);
        Assert.assertEquals(e.callCount, 1);
    }

    @Test
    public void concurrentPushTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        final boolean[] didPush = {false};
        IGlobeEventService.Listener pushTest = new IGlobeEventService.Listener()
        {
            @Override
            public void onGlobeEvent(IGlobeEvent globeEvent)
            {
                didPush[0] = true;
                service.pushGlobeEventListeners();
            }
        };
        TestGlobeEventServiceListener b = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener e = new TestGlobeEventServiceListener();

        service.addGlobeEventListener(b);
        service.addGlobeEventListener(pushTest);
        service.addGlobeEventListener(e);

        service.triggerEvent();
        Assert.assertEquals(didPush[0], true);
        Assert.assertEquals(b.callCount, 1);
        Assert.assertEquals(e.callCount, 1);
    }

    @Test
    public void concurrentPopTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        final boolean[] didPop = {false};
        IGlobeEventService.Listener pushTest = new IGlobeEventService.Listener()
        {
            @Override
            public void onGlobeEvent(IGlobeEvent globeEvent)
            {
                didPop[0] = true;
                service.popGlobeEventListeners();
            }
        };
        TestGlobeEventServiceListener b = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener e = new TestGlobeEventServiceListener();

        service.addGlobeEventListener(b);
        service.addGlobeEventListener(pushTest);
        service.addGlobeEventListener(e);

        service.triggerEvent();
        Assert.assertEquals(didPop[0], true);
        Assert.assertEquals(b.callCount, 1);
        Assert.assertEquals(e.callCount, 1);
    }

    @Test
    public void specificInvokeTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        TestGlobeEventServiceListener specificListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener dontFireListener = new TestGlobeEventServiceListener();

        TestGlobeEventServiceListener anyTypeListener = new TestGlobeEventServiceListener();
        service.addSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener);
        service.addSpecificGlobeEventListener("don't_fire_listener", dontFireListener);
        service.addGlobeEventListener(anyTypeListener);

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 1);
        Assert.assertEquals(dontFireListener.callCount, 0);
    }

    @Test
    public void testHandledSpecificFirst()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        TestGlobeEventServiceListener specificListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener dontFireListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener anyTypeListener = new TestGlobeEventServiceListener();

        service.addSpecificGlobeEventListener("don't_fire_listener", dontFireListener);
        service.addGlobeEventListener(anyTypeListener);
        service.addSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener);
        specificListener.setHandled = true;

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 0);
        Assert.assertEquals(dontFireListener.callCount, 0);
    }

    @Test
    public void pushPopSpecificTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        TestGlobeEventServiceListener specificListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener dontFireListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener anyTypeListener = new TestGlobeEventServiceListener();

        service.addSpecificGlobeEventListener("don't_fire_listener", dontFireListener);
        service.addGlobeEventListener(anyTypeListener);
        service.addSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener);

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 1);
        Assert.assertEquals(dontFireListener.callCount, 0);

        service.pushGlobeEventListeners();
        TestGlobeEventServiceListener specificListener2 = new TestGlobeEventServiceListener();
        service.addSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener2);

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 2);
        Assert.assertEquals(specificListener2.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 2);
        Assert.assertEquals(dontFireListener.callCount, 0);

        service.popGlobeEventListeners();
        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 3);
        Assert.assertEquals(specificListener2.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 3);
        Assert.assertEquals(dontFireListener.callCount, 0);
    }

    @Test
    public void specificRemoveTest()
    {
        TestGlobeEventService service = new TestGlobeEventService();
        TestGlobeEventServiceListener specificListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener dontFireListener = new TestGlobeEventServiceListener();
        TestGlobeEventServiceListener anyTypeListener = new TestGlobeEventServiceListener();

        service.addSpecificGlobeEventListener("don't_fire_listener", dontFireListener);
        service.addGlobeEventListener(anyTypeListener);
        service.addSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener);

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 1);
        Assert.assertEquals(dontFireListener.callCount, 0);

        service.removeSpecificGlobeEventListener(TEST_EVENT_TYPE, specificListener);

        service.triggerEvent();
        Assert.assertEquals(specificListener.callCount, 1);
        Assert.assertEquals(anyTypeListener.callCount, 2);
        Assert.assertEquals(dontFireListener.callCount, 0);
    }
}
