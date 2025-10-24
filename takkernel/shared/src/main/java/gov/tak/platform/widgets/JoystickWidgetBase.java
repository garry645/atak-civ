package gov.tak.platform.widgets;

import com.atakmap.math.MathUtils;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.widgets.IJoystickWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.ui.MotionEvent;

abstract class JoystickWidgetBase extends LayoutWidget implements
        IJoystickWidget,
        IMapWidget.OnPressListener,
        IMapWidget.OnMoveListener,
        IMapWidget.OnUnpressListener {

    float _size;
    DrawableWidget _background;
    DrawableWidget _stick;

    int _pressedPointerId = MotionEvent.INVALID_POINTER_ID;

    Set<OnJoystickMotionListener> _listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    JoystickWidgetBase(int size) {
        _background = new DrawableWidget(createJoystickDrawable(size/2, 8));
        _background.setColor(-1);
        _stick = new DrawableWidget(createJoystickDrawable(size/4, 8));
        _stick.setColor(-1);

        setSize(size);

        super.addChildWidget(_background);
        super.addChildWidget(_stick);

        // only process events on the background area for simplicity of implementation
        _background.setTouchable(true);
        _stick.setTouchable(false);

        _background.addOnPressListener(this);
        _background.addOnUnpressListener(this);
        _background.addOnMoveListener(this);
    }

    @Override
    public void setSize(float size) {
        if(_size == size)
            return;

        _size = size;
        _background.setSize(
                _size+16,
                _size+16);
        _background.setColor(-1);
        _stick.setSize(
                _size/2+16,
                _size/2+16);
        _stick.setPoint(_background.getWidth()/2-_stick.getWidth()/2, _background.getHeight()/2-_stick.getHeight()/2);
        setSize(_background.getWidth(), _background.getHeight());
    }

    @Override
    public float getSize() {
        return _size;
    }

    @Override
    public void setColor(int color) {
        _background.setColor(color);
        _stick.setColor(color);
    }

    @Override
    public int getColor() {
        return _stick.getColorFilter().getColor();
    }

    @Override
    public void addOnJoystickMotionListener(OnJoystickMotionListener l) {
        _listeners.add(l);
    }

    @Override
    public void removeOnJoystickMotionListener(OnJoystickMotionListener l) {
        _listeners.remove(l);
    }

    @Override
    public void onMapWidgetPress(IMapWidget widget, MotionEvent event) {
        _pressedPointerId = event.getPointerId(0);
    }

    @Override
    public void onMapWidgetUnpress(IMapWidget widget, MotionEvent event) {
        if(_pressedPointerId != MotionEvent.INVALID_POINTER_ID) {
            _pressedPointerId = MotionEvent.INVALID_POINTER_ID;

            // return stick to center
            _stick.setPoint(_background.getWidth() / 2 - _stick.getWidth() / 2, _background.getHeight() / 2 - _stick.getHeight() / 2);
            for (JoystickWidget.OnJoystickMotionListener l : _listeners)
                l.onJoystickMotion((JoystickWidget) this, 0f, 0f);
        }
    }

    @Override
    public boolean onMapWidgetMove(IMapWidget widget, MotionEvent event) {
        if(_pressedPointerId != event.getPointerId(0))
            return false;

        final PointF backgroundPosition = _background.getAbsoluteWidgetPosition();
        final float neutralX = _background.getWidth()/2;
        final float neutralY = _background.getHeight()/2;
        float stickX = event.getX()-backgroundPosition.x;
        float stickY = event.getY()-backgroundPosition.y;
        double stickDistance = MathUtils.distance(stickX, stickY, neutralX, neutralY);
        final float maxDistance = _size/2;
        if(stickDistance > maxDistance) {
            final double dx = (stickX-neutralX) / stickDistance;
            final double dy = (stickY-neutralY) / stickDistance;

            stickX = neutralX + (float)(dx*maxDistance);
            stickY = neutralY + (float)(dy*maxDistance);
            stickDistance = maxDistance;
        }

        float stickDirection = (float)Math.toDegrees(Math.atan2(stickY-neutralY, stickX-neutralX) + Math.PI/2d);

        // move stick within bounds
        _stick.setPoint(stickX-_stick.getWidth()/2, stickY-_stick.getHeight()/2);

        for(JoystickWidget.OnJoystickMotionListener l : _listeners)
            l.onJoystickMotion((JoystickWidget) this, stickDirection, (float)stickDistance/maxDistance);

        return true;
    }

    abstract Drawable createJoystickDrawable(int radius, int inset);
}