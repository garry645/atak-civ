package gov.tak.platform.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.commons.graphics.ColorBlendMode;
import gov.tak.api.commons.graphics.ColorFilter;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.widgets.IDrawableWidget;

public class DrawableWidget extends MapWidget implements IDrawableWidget
{
    private Drawable _drawable;
    private ColorFilter _colorFilter;
    private final ConcurrentLinkedQueue<OnChangedListener> _changeListeners;

    public DrawableWidget() {
        _changeListeners = new ConcurrentLinkedQueue<>();
    }

    public DrawableWidget(Drawable drawable) {
        this();
        setColor(-1);
        setDrawable(drawable);
    }

    @Override
    public void setDrawable(Drawable drawable) {
        if (_drawable != drawable) {
            _drawable = drawable;
            fireChangeListeners();
        }
    }

    @Override
    public Drawable getDrawable() {
        return _drawable;
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        _colorFilter = filter;
        fireChangeListeners();
    }

    public void setColor(int color, ColorBlendMode mode) {
        setColorFilter(new ColorFilter(color, mode));
    }

    public void setColor(int color) {
        setColor(color, ColorBlendMode.Multiply);
    }

    @Override
    public ColorFilter getColorFilter() {
        return _colorFilter;
    }

    @Override
    public void addChangeListener(DrawableWidget.OnChangedListener l) {
        synchronized (_changeListeners) {
            _changeListeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(DrawableWidget.OnChangedListener l) {
        synchronized (_changeListeners) {
            _changeListeners.remove(l);
        }
    }

    private List<OnChangedListener> getChangeListeners() {
        synchronized (_changeListeners) {
            return new ArrayList<>(_changeListeners);
        }
    }

    private void fireChangeListeners() {
        for (DrawableWidget.OnChangedListener l : getChangeListeners())
            l.onDrawableChanged(this);
    }
}
