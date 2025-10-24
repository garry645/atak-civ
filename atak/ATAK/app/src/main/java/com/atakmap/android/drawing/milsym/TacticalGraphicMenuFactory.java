
package com.atakmap.android.drawing.milsym;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Association;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.SimpleRectangle;
import com.atakmap.android.menu.MapMenuButtonWidget;
import com.atakmap.android.menu.MapMenuHandler;
import com.atakmap.android.menu.MapMenuWidget;
import com.atakmap.android.routes.Route;
import com.atakmap.android.util.Circle;
import com.atakmap.android.vehicle.VehicleShape;
import com.atakmap.android.widgets.WidgetIcon;
import com.atakmap.app.R;

import gov.tak.api.widgets.IMapMenuButtonWidget;

final class TacticalGraphicMenuFactory implements MapMenuHandler {

    private static final String TAG = "TacticalGraphicMenuFactory";

    private final Context context;

    public TacticalGraphicMenuFactory(Context context) {
        this.context = context;
    }

    @Override
    public void updateMenu(MapItem mapItem, MapMenuWidget menuWidget) {
        if (mapItem instanceof Polyline && mapItem.hasMetaValue("milsym")) {
            addReversePathWidgetButton(menuWidget);
        }
    }

    static boolean isObjectSupported(Object o) {
        if (o instanceof MapItem) {
            MapItem item = getSubjectMapItem((MapItem) o);
            if (item != null)
                return (!(item instanceof Route
                        || item instanceof VehicleShape));
        }
        return false;
    }

    static MapItem getSubjectMapItem(MapItem mapItem) {
        if ((mapItem instanceof SimpleRectangle || mapItem instanceof Circle
                || mapItem instanceof Ellipse)
                && mapItem.hasMetaValue("shapeUID")) {
            String shapeUID = mapItem.getMetaString("shapeUID", "");
            return MapView.getMapView().getRootGroup().deepFindUID(shapeUID);
        }
        if (MilSymUtils.isDrawingShape(mapItem))
            return mapItem;
        if (mapItem instanceof Polyline)
            return mapItem;
        if (mapItem instanceof Association)
            return ((Association) mapItem).getParent();
        return null;
    }

    private void addReversePathWidgetButton(MapMenuWidget menuWidget) {
        MapMenuButtonWidget reverseButton = new MapMenuButtonWidget(context);
        reverseButton.setIcon(new WidgetIcon(
                new MapDataRef() {
                    @Override
                    public String toUri() {
                        return "android.resource://" + context.getPackageName()
                                + "/" + R.drawable.reverse_route;
                    }
                },
                new Point(16, 16),
                32, 32));
        reverseButton.setLayoutWeight(360f / (menuWidget.getChildCount() + 1));
        menuWidget.addChildWidget(reverseButton);
        reverseButton.setButtonSize(reverseButton.getButtonSpan(),
                menuWidget.getButtonWidth());
        reverseButton.setOrientation(reverseButton.getOrientationAngle(),
                menuWidget.getInnerRadius());
        reverseButton.setOnButtonClickHandler(
                new IMapMenuButtonWidget.OnButtonClickHandler() {
                    @Override
                    public boolean isSupported(Object o) {
                        return (o instanceof Polyline
                                && !(o instanceof DrawingCircle
                                        || o instanceof DrawingRectangle));
                    }

                    @Override
                    public void performAction(Object o) {
                        Intent reversePath = new Intent(
                                MilSymReceiver.ACTION_REVERSE_PATH);
                        reversePath.putExtra(MilSymReceiver.EXTRA_UID,
                                ((MapItem) o).getUID());
                        AtakBroadcast.getInstance().sendBroadcast(reversePath);
                        Intent i = new Intent();
                        i.setAction("com.atakmap.android.maps.HIDE_MENU");
                        AtakBroadcast.getInstance().sendBroadcast(i);
                    }
                });
    }
}
