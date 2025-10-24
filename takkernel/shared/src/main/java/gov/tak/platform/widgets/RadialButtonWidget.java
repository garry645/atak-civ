
package gov.tak.platform.widgets;

import com.atakmap.math.MathUtils;
import gov.tak.api.commons.graphics.DisplaySettings;
import gov.tak.api.util.Visitor;
import gov.tak.platform.binding.PropertyInfo;
import gov.tak.platform.ui.MotionEvent;

import gov.tak.api.widgets.IRadialButtonWidget;
import gov.tak.platform.config.ConfigEnvironment;

import org.w3c.dom.Node;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RadialButtonWidget extends AbstractButtonWidget implements IRadialButtonWidget
{

    private float _angle;
    private float _radius;

    private float _span;
    private float _width;

    private final ConcurrentLinkedQueue<OnSizeChangedListener> _onSizeChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnOrientationChangedListener> _onOrientationChanged = new ConcurrentLinkedQueue<>();

    public RadialButtonWidget()
    {
        // defaults which are also reflected in DataParser fallbacks
        _angle = 0f;
        _radius = 100f;
        _span = 360f / 8f;
        _width = 100f;
    }

    @Deprecated
    public static class Factory extends AbstractButtonWidget.Factory
    {
        @Override
        public MapWidget createFromElem(ConfigEnvironment config,
                                        Node defNode)
        {
            RadialButtonWidget widget = new RadialButtonWidget();
            configAttributes(widget, config, defNode.getAttributes());
            return widget;
        }
    }

    public float getOrientationAngle()
    {
        return _angle;
    }

    public float getOrientationRadius()
    {
        return _radius;
    }

    public float getButtonSpan()
    {
        return _span;
    }

    public float getButtonWidth()
    {
        return _width;
    }

    public void addOnSizeChangedListener(OnSizeChangedListener l)
    {
        _onSizeChanged.add(l);
    }

    public void removeOnSizeChangedListener(OnSizeChangedListener l)
    {
        _onSizeChanged.remove(l);
    }

    public void addOnOrientationChangedListener(
            IRadialButtonWidget.OnOrientationChangedListener l)
    {
        _onOrientationChanged.add(l);
    }

    public void removeOnOrientationChangedListener(
            IRadialButtonWidget.OnOrientationChangedListener l)
    {
        _onOrientationChanged.remove(l);
    }

    public void setOrientation(float angle, float radius)
    {
        if (_angle != angle || _radius != radius)
        {
            _angle = angle;
            _radius = radius;
            onOrientationChanged();
        }
    }

    /**
     *
     * @param button    The button
     * @param x         The `x` coordinate of the click, relative to the widget origin
     * @param y         The `y` coordinate of the click, relative to the widget origin
     * @return  {@code true} if it was an expander button click, {@code false} otherwise
     */
    static boolean isExpanderHit(IRadialButtonWidget button, float x, float y)
    {
        final float radius = ((IRadialButtonWidget)button).getOrientationRadius();
        final float width = ((IRadialButtonWidget)button).getButtonWidth();
        final float angle = ((IRadialButtonWidget)button).getOrientationAngle();

        // XXX - using implementation detail from `GLRadialButtonWidget` and icon image pixels
        float density = DisplaySettings.getDpi() / 80f;
        float expanderIconRadius = 16/4 * density;

        final float expanderX = (radius+width)*(float)Math.cos(Math.toRadians(-angle));
        final float expanderY = (radius+width)*(float)Math.sin(Math.toRadians(-angle));

        final float d = (float) MathUtils.distance(expanderX, expanderY, x, y);

        return d <= expanderIconRadius;
    }

    @Override
    public MapWidget seekWidgetHit(MotionEvent event, float x, float y)
    {
        MapWidget hit = null;

        float d = (float) Math.sqrt(x * x + y * y);
        if (d >= _radius && d < _radius + _width)
        {

            double low = _angle - _span / 2;
            double high = _angle + _span / 2;

            double angle = -Math.atan2(y, x) * 180d / Math.PI;
            if (low >= 0d && angle < 0d)
            {
                angle = 360d + angle;
            }

            if (angle >= low && angle <= high)
            {
                hit = this;
            }
        } else if(isExpanderHit(this, x, y)) {
            hit = this;
        }

        return hit;
    }

    public void setButtonSize(float span, float width)
    {
        if (_span != span || _width != width)
        {
            _span = span;
            _width = width;
            onSizeChanged();
        }
    }

    public void onSizeChanged()
    {
        for (OnSizeChangedListener l : _onSizeChanged)
        {
            l.onRadialButtonSizeChanged(this);
        }
    }

    protected void onOrientationChanged()
    {
        for (OnOrientationChangedListener l : _onOrientationChanged)
        {
            l.onRadialButtonOrientationChanged(this);
        }
    }

    @Override
    protected void applyPropertyChange(String propertyName, Object newValue)
    {
        if (PROPERTY_ORIENTATION.canAssignValue(propertyName, newValue))
        {
            Orientation orientation = (Orientation) newValue;
            this.setOrientation(orientation.getAngle(), orientation.getRadius());
        } else if (PROPERTY_BUTTON_SIZE.canAssignValue(propertyName, newValue))
        {
            ButtonSize buttonSize = (ButtonSize) newValue;
            this.setButtonSize(buttonSize.getSpan(), buttonSize.getWidth());
        } else
        {
            super.applyPropertyChange(propertyName, newValue);
        }
    }

    @Override
    public void visitPropertyInfos(Visitor<PropertyInfo> visitor)
    {
        super.visitPropertyInfos(visitor);
        visitor.visit(PROPERTY_ORIENTATION);
        visitor.visit(PROPERTY_BUTTON_SIZE);
    }
}
