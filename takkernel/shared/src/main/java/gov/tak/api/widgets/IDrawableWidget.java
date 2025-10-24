package gov.tak.api.widgets;

import gov.tak.api.commons.graphics.ColorFilter;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.platform.widgets.DrawableWidget;

public interface IDrawableWidget extends IMapWidget
{
    interface OnChangedListener
    {
        void onDrawableChanged(IDrawableWidget widget);
    }

    void setDrawable(Drawable drawable);
    Drawable getDrawable();
    void setColorFilter(ColorFilter filter);
    ColorFilter getColorFilter();
    void addChangeListener(OnChangedListener l);
    void removeChangeListener(OnChangedListener l);
}
