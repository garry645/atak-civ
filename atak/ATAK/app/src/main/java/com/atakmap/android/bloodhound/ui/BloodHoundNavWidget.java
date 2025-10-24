
package com.atakmap.android.bloodhound.ui;

import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.Toast;

import com.atakmap.android.bloodhound.BloodHoundTool;
import com.atakmap.android.bloodhound.util.BloodHoundToolLink;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.routes.RouteNavigator;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;

/** A widget to display when the bloodhound tool is in route mode
 * that starts the navigation interface for the bloodhound route. */
public class BloodHoundNavWidget extends MarkerIconWidget implements
        MapWidget.OnClickListener, MapWidget.OnLongPressListener {

    /****************************** FIELDS *************************/
    public static final String TAG = "BloodHoundButtonTool";

    private final MapView _mapView;
    private final LinearLayoutWidget layoutWidget;
    private final BloodHoundTool _bloodhoundTool;

    final String imageUriEnabled;
    final String imageUriDisabled;

    // Indicates toggle state of the button or if the button is fully inactive
    private WidgetState widgetState = WidgetState.Off;

    /****************************** CONSTRUCTOR *************************/
    public BloodHoundNavWidget(final MapView mapView,
            BloodHoundTool toolbarButton) {
        super();

        this.setName("Bloodhound Nav Widget");
        _mapView = mapView;
        _bloodhoundTool = toolbarButton;

        // Configure the layout of the widget
        RootLayoutWidget root = (RootLayoutWidget) _mapView
                .getComponentExtra("rootLayoutWidget");
        this.layoutWidget = root.getLayout(RootLayoutWidget.BOTTOM_LEFT)
                .getOrCreateLayout("BL_H/BL_V/Bloodhound_V/BH_V/BH_H");
        this.layoutWidget.setVisible(false);
        this.layoutWidget.setMargins(16f, 0f, 0f, 16f);

        // Construct the widget
        imageUriEnabled = "android.resource://"
                + _mapView.getContext().getPackageName() + "/"
                + R.drawable.bloodhound_nav_lit;

        // Construct the widget
        imageUriDisabled = "android.resource://"
                + _mapView.getContext().getPackageName() + "/"
                + R.drawable.bloodhound_nav_unlit;

        // Start turned off
        setWidgetState(WidgetState.Off);

        this.addOnClickListener(this);
        this.addOnLongPressListener(this);

        this.layoutWidget.addWidget(this);
    }

    public void stop() {
        this.layoutWidget.setVisible(false);
    }

    /**
     * Sets the widget's UI properties according to the given state
     * @param state state of the widget
     */
    private void setVisualState(WidgetState state) {
        String imageUri;

        switch (state) {
            case On: {
                imageUri = imageUriEnabled;
                break;
            }
            default:
            case Off: {
                imageUri = imageUriDisabled;
                break;
            }
        }

        Icon.Builder builder = new Icon.Builder();
        builder.setAnchor(0, 0);
        builder.setColor(Icon.STATE_DEFAULT, Color.WHITE);
        builder.setSize(48, 48);
        builder.setImageUri(Icon.STATE_DEFAULT, imageUri);
        final Icon icon = builder.build();
        this.setIcon(icon);
    }

    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        switch (widgetState) {
            case Disabled: {
                if (_bloodhoundTool.getStartItem() != MapView.getMapView()
                        .getSelfMarker()
                        && _bloodhoundTool._routeWidget.isEnabled()) {
                    Toast.makeText(_mapView.getContext(),
                            R.string.bloodhound_must_be_hounding_start,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(_mapView.getContext(),
                            R.string.bloodhound_must_be_in_route_mode,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case On: {
                // We are on, wanting to go inactive
                RouteNavigator.getInstance().stopNavigating();
                setWidgetState(WidgetState.Off);
                break;
            }
            default:
            case Off: {
                // We are off, wanting to go active
                if (_bloodhoundTool.getStartItem() == MapView.getMapView()
                        .getSelfMarker()) {
                    // Open up navigation interface for the route.
                    final BloodHoundToolLink link = _bloodhoundTool.getlink();
                    if (link.isRoute()) {
                        if (RouteNavigator.getInstance()
                                .startNavigating(link.route, 0)) {
                            setWidgetState(WidgetState.On);
                        }
                    }
                } else {
                    Log.e(TAG,
                            "Internal logic violated: Nav widget should not be enabled if startPoint is not self marker.");
                }
                break;
            }
        }
    }

    @Override
    public void onMapWidgetLongPress(MapWidget widget) {
        Toast.makeText(_mapView.getContext(),
                R.string.bloodhound_open_navigation,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean setVisible(boolean visible) {
        super.setVisible(BloodHoundRouteWidget.hasRoutingCapability());
        return this.layoutWidget.setVisible(visible);
    }

    /**
     * Sets the state of widget. Manages internal state as well as visual state
     * @param state State to set
     */
    public void setWidgetState(WidgetState state) {
        widgetState = state;
        setVisualState(state);
    }

    /**
     * Possible states of this widget
     */
    public enum WidgetState {
        /**
         * Widget is on, able to be interacted with
         */
        On,
        /**
         * Widget is off, able to be interacted with
         */
        Off,
        /**
         * Widget is disabled, not able to be interacted with
         */
        Disabled
    }
}
