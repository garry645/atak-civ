
package com.atakmap.android.maps;

import android.graphics.PointF;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.api.engine.map.IGlobe;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.events.IGlobeEventService;

import org.mockito.Mockito;

public class AtakGlobeEventServiceTest {

    private static class RunnerBase {
        final AtakGlobeEventService service = new AtakGlobeEventService();
        final MapEventDispatcher dispatcher = new MapEventDispatcher();
        final Object source = new Object();
        final AtakGlobeEventService.InverseImpl inverseImpl = Mockito
                .mock(AtakGlobeEventService.InverseImpl.class);
        final IGlobe globe = Mockito.mock(IGlobe.class);

        protected RunnerBase() {
            service.addEventSource(source, globe, dispatcher, inverseImpl);
        }
    }

    private static class DispatchRunner extends RunnerBase {

        final ExpectsGlobeEventListener listener;

        public DispatchRunner(String expectType) {
            this(expectType, false);
        }

        public DispatchRunner(String expectType, boolean specific) {
            if (specific)
                service.addSpecificGlobeEventListener(expectType,
                        listener = new ExpectsGlobeEventListener(expectType));
            else
                service.addGlobeEventListener(
                        listener = new ExpectsGlobeEventListener(expectType));
        }

        public void run(MapEvent mapEvent) {

            GeoPoint globeCoordExpected = new GeoPoint(1.0, 2.0);
            Mockito.when(inverseImpl.inverse(mapEvent.getPointF().x,
                    mapEvent.getPointF().y))
                    .thenReturn(globeCoordExpected);

            dispatcher.dispatch(mapEvent);
            Assert.assertEquals(listener.getTriggered(), 1);
            Assert.assertNotNull(listener.getLastGlobeEvent());
            Assert.assertNotNull(
                    listener.getLastGlobeEvent().getGlobeCoordinate());
            Assert.assertEquals(
                    listener.getLastGlobeEvent().getViewPosition().x,
                    mapEvent.getPointF().x, 0.0f);
            Assert.assertEquals(
                    listener.getLastGlobeEvent().getViewPosition().y,
                    mapEvent.getPointF().y, 0.0f);
            Assert.assertEquals(globeCoordExpected,
                    listener.getLastGlobeEvent().getGlobeCoordinate());

            IGlobeEvent globeEvent = listener.getLastGlobeEvent();
            IGlobe mockGlobe = globe;
            IGlobe foundGlobe = null;
            for (int i = 0; i < listener.getLastGlobeEvent()
                    .getTargetCount(); ++i) {
                Object target = globeEvent.getTargetObject(i);
                Assert.assertNotNull(target);
                if (IGlobe.class.isAssignableFrom(target.getClass())) {
                    foundGlobe = (IGlobe) target;
                }
            }
            Assert.assertEquals(foundGlobe, mockGlobe);
        }

        public void runExpectNoTrigger(MapEvent mapEvent) {
            dispatcher.dispatch(mapEvent);
            Assert.assertEquals(listener.getTriggered(), 0);
        }

        public void destroy() {
            service.removeEventSource(source);
        }
    }

    @Test
    public void mapClickTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_CLICK);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.CLICK);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemClickTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_CLICK);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.CLICK);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapPressTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.PRESS);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemPressTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.PRESS);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapReleaseTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_RELEASE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.RELEASE);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemReleaseTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_RELEASE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.RELEASE);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapDoubleTapTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_DOUBLE_TAP);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DOUBLE_CLICK);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemDoubleTapTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DOUBLE_TAP);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DOUBLE_CLICK);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapLongPress() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_LONG_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.LONG_PRESS);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemLongPress() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_LONG_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.LONG_PRESS);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragStartedTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_STARTED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragContinuedTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_CONTINUED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragDroppedTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_DROPPED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapMovedTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_MOVED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapResizedTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_RESIZED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapRotateTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ROTATE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapTiltTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_TILT);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapScrollTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_SCROLL);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapScaleTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_SCALE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapZoomTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemClickSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_CLICK);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.CLICK, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapPressSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.PRESS, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemPressSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.PRESS, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapReleaseSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_RELEASE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.RELEASE, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemReleaseSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_RELEASE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.RELEASE, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapDoubleTapSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_DOUBLE_TAP);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DOUBLE_CLICK,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemDoubleTapSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DOUBLE_TAP);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DOUBLE_CLICK,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapLongPressSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_LONG_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.LONG_PRESS,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void itemLongPressSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_LONG_PRESS);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.LONG_PRESS,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragStartedTestSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_STARTED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragContinuedTestSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_CONTINUED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void dragDroppedSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.ITEM_DRAG_DROPPED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.DRAGGING, true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapMovedSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_MOVED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapResizedSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_RESIZED);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapRotateSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ROTATE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapTiltSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_TILT);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapScrollSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_SCROLL);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapScaleSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_SCALE);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void mapZoomSpecificTest() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void testConcurrentListenerRemoval() {
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.CLICK);
        IGlobeEventService.Listener removeThyself = new IGlobeEventService.Listener() {
            @Override
            public void onGlobeEvent(IGlobeEvent globeEvent) {
                runner.service.removeGlobeEventListener(this);
            }
        };

        IGlobeEventService.Listener before = Mockito
                .mock(IGlobeEventService.Listener.class);
        IGlobeEventService.Listener after = Mockito
                .mock(IGlobeEventService.Listener.class);

        runner.service.addGlobeEventListener(before);
        runner.service.addGlobeEventListener(removeThyself);
        runner.service.addGlobeEventListener(after);

        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_CLICK);
        b.setPoint(new PointF(1.0f, 2.0f));
        runner.run(b.build());
    }

    @Test
    public void testDispatcherPushRun() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.dispatcher.pushListeners();
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void testServicePushRun() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.service.pushGlobeEventListeners();
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void testPushRemovePopRun() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.dispatcher.pushListeners();
        runner.service.removeGlobeEventListener(runner.listener);
        runner.runExpectNoTrigger(b.build());
        runner.dispatcher.popListeners();
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void testRemoveEventSource() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING);
        runner.service.removeEventSource(runner.source);
        runner.runExpectNoTrigger(b.build());
        runner.destroy();
    }

    @Test
    public void testSubscribeSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.run(b.build());
        runner.destroy();
    }

    @Test
    public void testUnsubscribeSpecific() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        runner.service.removeSpecificGlobeEventListener(
                IGlobeEvent.VIEW_CHANGING, runner.listener);
        runner.runExpectNoTrigger(b.build());
        runner.destroy();
    }

    @Test
    public void testMultiSubscribe() {
        MapEvent.Builder b = new MapEvent.Builder(MapEvent.MAP_ZOOM);
        b.setPoint(new PointF(1.0f, 2.0f));
        DispatchRunner runner = new DispatchRunner(IGlobeEvent.VIEW_CHANGING,
                true);
        IGlobeEventService.Listener noTriggerListener = Mockito
                .mock(IGlobeEventService.Listener.class);
        IGlobeEventService.Listener generalTriggerListener = Mockito
                .mock(IGlobeEventService.Listener.class);
        runner.service.addSpecificGlobeEventListener("no_event_type",
                noTriggerListener);
        runner.service.addGlobeEventListener(generalTriggerListener);
        runner.run(b.build());
        Mockito.verify(noTriggerListener, Mockito.never())
                .onGlobeEvent(runner.listener.getLastGlobeEvent());
        Mockito.verify(generalTriggerListener, Mockito.times(1))
                .onGlobeEvent(runner.listener.getLastGlobeEvent());
        runner.destroy();
    }
}
