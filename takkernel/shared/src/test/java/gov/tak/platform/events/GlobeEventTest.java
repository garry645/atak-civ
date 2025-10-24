package gov.tak.platform.events;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.platform.graphics.PointF;

public class GlobeEventTest
{

    @Test
    public void simpleTest()
    {
        String type = "test_type";
        int pointerId = 42;
        int eventButtons = GlobeEvent.BUTTON_LEFT | GlobeEvent.BUTTON_RIGHT | GlobeEvent.BUTTON_MIDDLE;
        PointF viewPoint = new PointF(1.0f, 2.0f);
        GeoPoint globeCoordinate = new GeoPoint(0.0f, 1.0f, 2.0f);
        GeoPoint firstTargetCoordinate = new GeoPoint(4.0f, 5.0f, 6.0f);
        GeoPoint secondTargetCoordinate = new GeoPoint(7.0f, 8.0f, 9.0f);
        Object[] targets = {new Object(), new Object()};
        GeoPoint[] targetCoords = {firstTargetCoordinate, secondTargetCoordinate};
        Object source = "source";
        int globeId = 1337;
        GlobeEvent event = new GlobeEvent(type, source, globeId, pointerId, eventButtons,
                globeCoordinate, viewPoint, targets, targetCoords);
        Assert.assertEquals(event.getType(), type);
        Assert.assertEquals(event.getButtons(), eventButtons);
        Assert.assertEquals(event.getPointerId(), pointerId);
        Assert.assertEquals(event.getGlobeCoordinate(), globeCoordinate);
        Assert.assertEquals(event.getTargetCount(), 2);
        Assert.assertEquals(event.getTargetObject(0), targets[0]);
        Assert.assertEquals(event.getTargetObject(1), targets[1]);
        Assert.assertEquals(event.getTargetCoordinate(0), targetCoords[0]);
        Assert.assertEquals(event.getTargetCoordinate(1), targetCoords[1]);
        Assert.assertEquals(event.getViewPosition(), viewPoint);
        Assert.assertEquals(event.getGlobeId(), globeId);
        Assert.assertEquals(event.getSource(), source);

        IGlobeEvent copy = event.clone();

        Assert.assertEquals(event.getType(), copy.getType());
        Assert.assertEquals(event.getButtons(), copy.getButtons());
        Assert.assertEquals(event.getPointerId(), copy.getPointerId());
        Assert.assertEquals(event.getGlobeCoordinate(), copy.getGlobeCoordinate());
        Assert.assertEquals(event.getTargetCount(), copy.getTargetCount());
        Assert.assertEquals(event.getTargetObject(0), copy.getTargetObject(0));
        Assert.assertEquals(event.getTargetObject(1), copy.getTargetObject(1));
        Assert.assertEquals(event.getTargetCoordinate(0), copy.getTargetCoordinate(0));
        Assert.assertEquals(event.getTargetCoordinate(1), copy.getTargetCoordinate(1));
        Assert.assertEquals(event.getViewPosition(), copy.getViewPosition());
        Assert.assertEquals(event.getGlobeId(), globeId);
        Assert.assertEquals(event.getSource(), source);
    }
}
