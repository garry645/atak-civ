
package com.atakmap.android.widgets;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;

public abstract class AbstractWidgetMapComponent extends AbstractMapComponent {

    public static final String ROOT_LAYOUT_EXTRA = "rootLayoutWidget";

    private WidgetsLayer layer;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        LayoutWidget rootLayoutWidget = (LayoutWidget) view
                .getComponentExtra(ROOT_LAYOUT_EXTRA);
        _rootLayoutWidget = new LayoutWidget();
        _rootLayoutWidget.setName("Component Root");
        rootLayoutWidget.addWidget(_rootLayoutWidget);
        onCreateWidgets(context, intent, view);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        onDestroyWidgets(context, view);
        if (this.layer != null)
            view.removeLayer(MapView.RenderStack.WIDGETS, layer);
    }

    protected abstract void onCreateWidgets(Context context, Intent intent,
            MapView view);

    protected abstract void onDestroyWidgets(Context context, MapView view);

    public LayoutWidget getRootLayoutWidget() {
        return _rootLayoutWidget;
    }

    private LayoutWidget _rootLayoutWidget;
}
