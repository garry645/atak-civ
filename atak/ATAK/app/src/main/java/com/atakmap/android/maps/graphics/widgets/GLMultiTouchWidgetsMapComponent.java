
package com.atakmap.android.maps.graphics.widgets;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.LayoutWidget;
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

final class GLMultiTouchWidgetsMapComponent {

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
                final MotionEvent pointerEvent = MotionEvent.obtain(
                        aEvent.getDownTime(),
                        aEvent.getEventTime(),
                        aEvent.getAction(),
                        aEvent.getX(i),
                        aEvent.getY(i),
                        aEvent.getMetaState());

                final gov.tak.platform.ui.MotionEvent event = MarshalManager
                        .marshal(pointerEvent, android.view.MotionEvent.class,
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
            if (v instanceof MapView && ((MapView) v).getMapTouchController()
                    .isLongPressDragging())
                // Prevent widgets from interfering with the long-press drag event
                return false;
            Map<IMapWidget, gov.tak.platform.ui.MotionEvent> hits = getHits(v,
                    aEvent);
            final gov.tak.platform.ui.MotionEvent event = MarshalManager
                    .marshal(aEvent, android.view.MotionEvent.class,
                            gov.tak.platform.ui.MotionEvent.class);

            if (hits.isEmpty() && !_pressedWidgets.isEmpty()
                    && event.getAction() == MotionEvent.ACTION_MOVE) {
                boolean anyUsed = false;
                Iterator<IMapWidget> it = _pressedWidgets.iterator();
                while (it.hasNext()) {
                    IMapWidget pressedWidget = it.next();
                    boolean widgetUsed = false;
                    for (int i = 0; i < aEvent.getPointerCount(); i++) {
                        final MotionEvent pointerEvent = MotionEvent.obtain(
                                aEvent.getDownTime(),
                                aEvent.getEventTime(),
                                aEvent.getAction(),
                                aEvent.getX(i),
                                aEvent.getY(i),
                                aEvent.getMetaState());

                        final gov.tak.platform.ui.MotionEvent pevent = MarshalManager
                                .marshal(pointerEvent,
                                        android.view.MotionEvent.class,
                                        gov.tak.platform.ui.MotionEvent.class);

                        widgetUsed |= pressedWidget.onMove(pevent);
                        if (widgetUsed)
                            break;
                    }
                    // If this widget isn't handling move events then get rid of it
                    if (!widgetUsed) {
                        pressedWidget.onUnpress(event);
                        it.remove();
                    }
                    anyUsed |= widgetUsed;
                }
                return anyUsed;
            }
            {
                Iterator<IMapWidget> it = _pressedWidgets.iterator();
                while (it.hasNext()) {
                    final IMapWidget pressedWidget = it.next();
                    if (!hits.containsKey(pressedWidget)) {
                        pressedWidget.onUnpress(event);
                        it.remove();
                    }
                }
            }
            if (!hits.isEmpty()) {
                switch (event.getAction()) {
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

        private final MapView _mapView;
        private final LayoutWidget _rootLayout;
        private Collection<IMapWidget> _pressedWidgets = new ArrayList<>();
        private Timer widTimer;
        private WidTimerTask widTask;

        class WidTimerTask extends TimerTask {
            @Override
            public void run() {
                final IMapWidget mw = Collections2.firstOrNull(_pressedWidgets);
                _mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mw != null)
                            mw.onLongPress();
                    }
                });
            }
        }

    }

}
