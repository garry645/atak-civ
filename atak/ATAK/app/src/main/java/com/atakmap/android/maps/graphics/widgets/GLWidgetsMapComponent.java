
package com.atakmap.android.maps.graphics.widgets;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.graphics.GLMapItemFactory;
import com.atakmap.android.widgets.LayoutWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.android.widgets.WidgetsLayer;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.AtakMapView;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.Collections2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.marshal.MarshalManager;

public class GLWidgetsMapComponent extends AbstractMapComponent implements
        AtakMapView.OnActionBarToggledListener {
    private MapView _mapView;
    private GLMapSurface _mapSurface;
    private GLMapView _orthoWorldMap;
    private LayoutWidget _rootLayout;
    private WidgetsLayer _widgetsLayer;

    private static final String TAG = "GLWidgetsMapComponent";

    public void onCreate(Context context, Intent intent, MapView view) {
        _mapView = view;

        _widgetsLayer = new WidgetsLayer("Map Widgets",
                (RootLayoutWidget) _mapView
                        .getComponentExtra("rootLayoutWidget"));
        _rootLayout = _widgetsLayer.getRoot();

        _touchListener = new WidgetTouchHandler(view, _rootLayout);

        _mapView.addOnMapViewResizedListener(_resizedListener);
        _mapView.addOnActionBarToggledListener(this);
        _mapView.addOnTouchListenerAt(0, _touchListener);

        GLWidgetFactory.registerSpi(GLScrollLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLButtonWidget.SPI);
        GLWidgetFactory.registerSpi(GLMapMenuButtonWidget.SPI);
        GLWidgetFactory.registerSpi(GLArcWidget.SPI);
        GLWidgetFactory.registerSpi(GLIsoKeyWidget.SPI);
        GLWidgetFactory.registerSpi(GLGradientWidget.SPI);
        GLMapItemFactory.registerSpi(GLFahArrowWidget.GLITEM_SPI);
        GLWidgetFactory.registerSpi(GLCenterBeadWidget.SPI);
        GLWidgetFactory.registerSpi(GLMarkerDrawableWidget.SPI);
        GLWidgetFactory.registerSpi(GLDrawableWidget.SPI);

        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLRadialButtonWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory
                .registerSpi(gov.tak.platform.widgets.opengl.GLScaleWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLLinearLayoutWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLLayoutWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory
                .registerSpi(gov.tak.platform.widgets.opengl.GLTextWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLMarkerIconWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLCenterBeadWidget.SPI);
        gov.tak.platform.widgets.opengl.GLWidgetFactory.registerSpi(
                gov.tak.platform.widgets.opengl.GLDrawableWidget.SPI);

        _mapView.addLayer(MapView.RenderStack.WIDGETS, _widgetsLayer);

        _mapSurface = _mapView
                .findViewWithTag(GLMapSurface.LOOKUP_TAG);
        if (_mapSurface == null) {
            // register a callback to listen when it is set...
            view.setOnHierarchyChangeListener(
                    new ViewGroup.OnHierarchyChangeListener() {
                        @Override
                        public void onChildViewRemoved(final View parent,
                                final View child) {
                        }

                        @Override
                        public void onChildViewAdded(final View parent,
                                final View child) {
                            if (_mapSurface == null) {
                                if (parent == _mapView
                                        && child instanceof GLMapSurface) {
                                    _mapSurface = (GLMapSurface) child;
                                }
                            }
                        }
                    });
        } else {
            // _finishCreate();
        }
    }

    public LayoutWidget getRootLayout() {
        return _rootLayout;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        // clean up any static references

        _mapView.removeLayer(MapView.RenderStack.WIDGETS, _widgetsLayer);

        _mapView.removeOnMapViewResizedListener(_resizedListener);
        _mapView.removeOnActionBarToggledListener(this);
        _mapView.removeOnTouchListener(_touchListener);
    }

    private final AtakMapView.OnMapViewResizedListener _resizedListener = new AtakMapView.OnMapViewResizedListener() {
        @Override
        public void onMapViewResized(AtakMapView view) {
            if (_rootLayout != null) {
                Log.d(TAG, "GLWidgetsMapComponent resized: " +
                        view.getWidth() + "x" + view.getHeight());

                _rootLayout.setSize(view.getWidth(),
                        view.getHeight());
            }
        }
    };

    private View.OnTouchListener _touchListener;

    public static final class WidgetTouchHandler implements
            View.OnTouchListener {

        public WidgetTouchHandler(MapView mapView, LayoutWidget rootLayout) {
            _mapView = mapView;
            _rootLayout = rootLayout;
        }

        private Map<IMapWidget, gov.tak.platform.ui.MotionEvent> getHits(
                final View v, final MotionEvent aEvent) {
            Map<IMapWidget, gov.tak.platform.ui.MotionEvent> hits = new HashMap<>();
            for (int i = 0; i < aEvent.getPointerCount(); i++) {
                final gov.tak.platform.ui.MotionEvent event = MarshalManager
                        .marshal(getPointerSpecificMotionEvent(aEvent, i),
                                android.view.MotionEvent.class,
                                gov.tak.platform.ui.MotionEvent.class);
                IMapWidget hit = _rootLayout.seekWidgetHit(event, event.getX(),
                        event.getY());
                if (hit != null)
                    hits.put(hit, event);
            }
            return hits;
        }

        @Override
        public boolean onTouch(final View v, final MotionEvent aEvent) {
            if (v instanceof MapView) {
                MapView mv = (MapView) v;
                if (Build.MODEL.equals("MPU5")) {
                    if (mv.getMapTouchController().isLongPressDragging()
                            || mv.getMapTouchController().getInGesture())
                        // Prevent widgets from interfering with the long-press drag even
                        // and time it is in gesture on the MPU5 hardware
                        return false;
                } else if (mv.getMapTouchController().isLongPressDragging()) {
                    // Prevent widgets from interfering with the long-press drag event
                    return false;
                }
            }
            Map<IMapWidget, gov.tak.platform.ui.MotionEvent> hits = getHits(v,
                    aEvent);
            final gov.tak.platform.ui.MotionEvent event = MarshalManager
                    .marshal(aEvent, android.view.MotionEvent.class,
                            gov.tak.platform.ui.MotionEvent.class);

            // if there were no hits but previously pressed widgets, check for drags outside of
            // widget bounds
            if (hits.isEmpty() && !_pressedWidgets.isEmpty()
                    && aEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                boolean anyDragged = false;
                Iterator<IMapWidget> it = _pressedWidgets.iterator();
                while (it.hasNext()) {
                    IMapWidget pressedWidget = it.next();
                    // check if the widget treats the move event as a drag
                    final boolean widgetDragged = isWidgetDragged(aEvent,
                            pressedWidget);
                    // If this widget isn't handling move events then get rid of it
                    if (!widgetDragged) {
                        pressedWidget.onUnpress(event);
                        it.remove();
                    }
                    anyDragged |= widgetDragged;
                }
                // event is handled if any widget processed the move as a drag
                return anyDragged;
            }

            // evict any existing _pressed_ widgets that are either
            // 1) not in the hit list
            //       == OR ==
            // 2) don't process the current move event
            // The latter is important as it allows previously pressed widgets to retain pressed
            // state for drags outside of widget bounds
            {
                Iterator<IMapWidget> it = _pressedWidgets.iterator();
                while (it.hasNext()) {
                    final IMapWidget pressedWidget = it.next();
                    // check if the pressed widget was hit
                    if (hits.containsKey(pressedWidget))
                        continue;

                    // check if the pressed widget was dragged
                    if (aEvent.getActionMasked() == MotionEvent.ACTION_MOVE &&
                            isWidgetDragged(aEvent, pressedWidget)) {

                        continue;
                    }

                    // the widget is not currently hit and the event is not processed as a drag;
                    // evict
                    pressedWidget.onUnpress(event);
                    it.remove();
                }
            }
            if (!hits.isEmpty()) {
                switch (aEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        for (Map.Entry<IMapWidget, gov.tak.platform.ui.MotionEvent> hit : hits
                                .entrySet()) {
                            hit.getKey().onPress(hit.getValue());
                            _pressedWidgets.add(hit.getKey());

                            //start long press countdown
                            widTimer = new Timer("GLWidgetsMapComponent");
                            widTimer.schedule(widTask = new WidTimerTask(),
                                    ViewConfiguration.getLongPressTimeout());
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        for (Map.Entry<IMapWidget, gov.tak.platform.ui.MotionEvent> hit : hits
                                .entrySet()) {
                            if (!_pressedWidgets.contains(hit.getKey())
                                    && hit.getKey()
                                            .isEnterable()/* && hit != null */) {
                                hit.getKey().onPress(hit.getValue());
                                _pressedWidgets.add(hit.getKey());
                            } else {
                                hit.getKey().onMove(hit.getValue());
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        for (Map.Entry<IMapWidget, gov.tak.platform.ui.MotionEvent> hit : hits
                                .entrySet()) {
                            hit.getKey().onUnpress(hit.getValue());
                            if (_pressedWidgets.contains(hit.getKey())) {
                                if (widTask != null) {

                                    widTask.cancel();
                                    if (widTimer.purge() > 0) {
                                        //the long press task was canceled, so onClick
                                        hit.getKey().onClick(hit.getValue());
                                    } //otherwise, ignore
                                }
                            } else
                                return false; // TODO this is ugly
                        }
                        _pressedWidgets.clear();
                        break;
                }
            }
            return !hits.isEmpty();
        }

        private static MotionEvent getPointerSpecificMotionEvent(
                MotionEvent aEvent, int pointerIndex) {
            MotionEvent.PointerProperties pointerProps = new MotionEvent.PointerProperties();
            aEvent.getPointerProperties(pointerIndex, pointerProps);
            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            aEvent.getPointerCoords(pointerIndex, pointerCoords);
            return MotionEvent.obtain(
                    aEvent.getDownTime(),
                    aEvent.getEventTime(),
                    aEvent.getActionMasked(),
                    1,
                    new MotionEvent.PointerProperties[] {
                            pointerProps
                    },
                    new MotionEvent.PointerCoords[] {
                            pointerCoords
                    },
                    aEvent.getMetaState(),
                    aEvent.getButtonState(),
                    aEvent.getXPrecision(),
                    aEvent.getYPrecision(),
                    aEvent.getDeviceId(),
                    aEvent.getEdgeFlags(),
                    aEvent.getSource(),
                    aEvent.getFlags());
        }

        private static boolean isWidgetDragged(MotionEvent event,
                IMapWidget widget) {
            boolean widgetDragged = false;
            for (int i = 0; i < event.getPointerCount(); i++) {
                final gov.tak.platform.ui.MotionEvent pointerEvent = MarshalManager
                        .marshal(getPointerSpecificMotionEvent(event, i),
                                android.view.MotionEvent.class,
                                gov.tak.platform.ui.MotionEvent.class);

                // check for drag on against all pointers
                widgetDragged |= widget.onMove(pointerEvent);
                if (widgetDragged)
                    break;
            }
            return widgetDragged;
        }

        private final MapView _mapView;
        private final LayoutWidget _rootLayout;
        private final Collection<IMapWidget> _pressedWidgets = new ArrayList<>();
        private Timer widTimer;
        private WidTimerTask widTask;

        class WidTimerTask extends TimerTask {
            @Override
            public void run() {
                try {
                    final IMapWidget mw = Collections2
                            .firstOrNull(_pressedWidgets);
                    _mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mw != null)
                                mw.onLongPress();
                        }
                    });
                } catch (Exception ignore) {
                    // ATAK-17979 Playstore Crash: Collections2 FirstOrNull NoSuchElementException
                    // Acceptable fix due to lack of synchronization on the _pressedWidget colection
                    // since it would end up not running the mv long press anyways.
                }
            }
        }
    }

    /**
     * Note individual MapWidgets do not need to register for action bar events. They simply
     * need to implement AtakMapView.OnActionBarToggledListener amd they will be notified via
     * thier parent MapWidget/container
     * @param showing true when the action bar is showing.
     *
     */
    @Override
    public void onActionBarToggled(boolean showing) {
        _rootLayout.onActionBarToggled(showing);
    }

}
