package gov.tak.platform.ui;

import org.junit.Assert;
import org.junit.Test;

public class MotionEventTest {
    @Test
    public void MotionEvent_obtain_pointerProperties_Roundtrip() {
        final MotionEvent.PointerProperties pointerProps = new MotionEvent.PointerProperties();
        pointerProps.id = 5;
        pointerProps.toolType = 4;

        MotionEvent me = MotionEvent.obtain(
                -1L,
                -1L,
                MotionEvent.ACTION_POINTER_DOWN,
                1,
                new MotionEvent.PointerProperties[] {pointerProps},
                new MotionEvent.PointerCoords[] {new MotionEvent.PointerCoords()},
                0,
                0,
                0f,
                0f,
                0,
                0,
                0,
                0);

        Assert.assertEquals(pointerProps.id, me.getPointerId(0));
        Assert.assertEquals(pointerProps.toolType, me.getToolType(0));
    }

    @Test
    public void MotionEvent_obtain_overheadValues_Roundtrip() {
        final long downTime = 1;
        final long eventTime = 2;
        final int action = MotionEvent.ACTION_BUTTON_PRESS;
        final int pointerCount = 1;
        final int metaState = 4;
        final int buttonState = 5;
        final int clickGestureCount = 6;
        final float xPrecision = 7.f;
        final float yPrecision = 8.f;
        final int deviceId = 9;
        final int edgeFlags = 10;
        final int source = 11;
        final int flags = 12;

        MotionEvent me = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                pointerCount,
                new MotionEvent.PointerProperties[] {new MotionEvent.PointerProperties()},
                new MotionEvent.PointerCoords[] {new MotionEvent.PointerCoords()},
                metaState,
                buttonState,
                clickGestureCount,
                xPrecision,
                yPrecision,
                deviceId,
                edgeFlags,
                source,
                flags);

        Assert.assertEquals(downTime, me.getDownTime());
        Assert.assertEquals(eventTime, me.getEventTime());
        Assert.assertEquals(action, me.getAction());
        Assert.assertEquals(pointerCount, me.getPointerCount());
        Assert.assertEquals(metaState, me.getMetaState());
        Assert.assertEquals(buttonState, me.getButtonState());
        Assert.assertEquals(clickGestureCount, me.getClickGestureCount());
        Assert.assertEquals(xPrecision, me.getXPrecision(), 0.f);
        Assert.assertEquals(yPrecision, me.getYPrecision(), 0.f);
        Assert.assertEquals(deviceId, me.getDeviceId());
        Assert.assertEquals(edgeFlags, me.getEdgeFlags());
        Assert.assertEquals(source, me.getSource());
        Assert.assertEquals(flags, me.getFlags());
    }
}
