
package com.atakmap.android.maps;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import gov.tak.api.engine.map.IGlobe;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.events.IGlobeEventService2;
import gov.tak.platform.events.GlobeEvent;
import gov.tak.platform.graphics.PointF;

/*
It is assumed that ATAK currently will only call addEventSource/removeEventSource during
the init/teardown lifecycle of the App, so no tracking of added/removed listeners is kept
to keep future added event source stacks "synced" with existing ones. I'm not sure there
is cleanly defined way to define them "synced" anyway. This assumption cuts back on complexity.
*/

/**
 * ATAK implementation of the IGlobeEventService Unified API. This is implemented with
 * with MapEventDispatcher so the listener stack is fully integrated.
 */
public class AtakGlobeEventService implements IGlobeEventService2 {

    public final String TAG = "AtakGlobeEventService";

    /**
     * Implementation to use when translating from view points to globe coordinates
     */
    public interface InverseImpl {
        gov.tak.api.engine.map.coords.GeoPoint inverse(float x, float y);
    }

    // eliminates duplicate translations of the same MapEvent
    private class TranslateFlyweight {
        WeakReference<MapEvent> mapEventRef;
        IGlobeEvent translatedGlobeEvent;
        EventSource eventSource;

        public synchronized IGlobeEvent getTranslatedGlobeEvent(
                MapEvent mapEvent) {
            if (mapEventRef == null || mapEventRef.get() != mapEvent) {
                translatedGlobeEvent = translateMapEvent(eventSource, mapEvent);
                mapEventRef = new WeakReference<>(mapEvent);
            }
            return translatedGlobeEvent;
        }
    }

    private class TranslateListener
            implements MapEventDispatcher.MapEventDispatchListener {

        private TranslateFlyweight flyweight;
        private Listener listener;

        @Override
        public void onMapEvent(MapEvent event) {
            IGlobeEvent globeEvent = flyweight.getTranslatedGlobeEvent(event);
            if (globeEvent != null)
                listener.onGlobeEvent(globeEvent);
        }
    }

    // a globe/view pair
    private class EventSource {

        MapEventDispatcher mapEventDispatcher;
        IGlobe globe;
        int globeId;
        InverseImpl inverseImpl;
        Object source;
        final TranslateFlyweight flyweight = new TranslateFlyweight();
        final WeakHashMap<Listener, TranslateListener> mappedListeners = new WeakHashMap<>();
        final Map<String, WeakHashMap<Listener, TranslateListener>> mappedSpecificListeners = new HashMap<>();

        public EventSource() {
            flyweight.eventSource = this;
        }

        gov.tak.api.engine.map.coords.GeoPoint inverse(float x, float y) {
            return inverseImpl.inverse(x, y);
        }
    }

    private final Object copyOnWriteSync = new Object();
    private List<EventSource> eventSources = new ArrayList<>();
    private static int nextGlobeId = 1;
    private static final WeakHashMap<IGlobe, Integer> globeIdCache = new WeakHashMap<>();

    @Override
    public void addGlobeEventListener(Listener listener) {
        addGlobeEventListenerImpl(null, listener);
    }

    @Override
    public void addSpecificGlobeEventListener(String eventType,
            Listener listener) {
        addGlobeEventListenerImpl(eventType, listener);
    }

    @Override
    public void removeGlobeEventListener(Listener listener) {
        removeGlobeEventListenerImpl(null, listener);
    }

    @Override
    public void removeSpecificGlobeEventListener(String eventType,
            Listener listener) {
        removeGlobeEventListenerImpl(eventType, listener);
    }

    @Override
    public void pushGlobeEventListeners() {
        synchronized (copyOnWriteSync) {
            // push all listeners
            for (EventSource eventSource : eventSources) {
                eventSource.mapEventDispatcher.pushListeners();
            }
        }
    }

    @Override
    public void popGlobeEventListeners() {
        synchronized (copyOnWriteSync) {
            // pop all listeners
            for (EventSource eventSource : eventSources) {
                eventSource.mapEventDispatcher.popListeners();
            }
        }
    }

    private static int getGlobeId(IGlobe globe) {
        synchronized (globeIdCache) {
            Integer globeId = globeIdCache.get(globe);
            if (globeId == null) {
                globeId = nextGlobeId++;
                globeIdCache.put(globe, globeId);
            }
            return globeId;
        }
    }

    private void addGlobeEventListenerImpl(String eventType,
            Listener listener) {
        /*
         The complexity here is the avoid deadlocking that is possible from recursive calls
         (since handlers can call add/remove) as well as deadlocking that can occur from
         disjointed MapEventDispatcher control
         */

        // get the current state of the event source list
        List<EventSource> eventSourceList;
        synchronized (copyOnWriteSync) {
            eventSourceList = eventSources;
        }

        for (EventSource eventSource : eventSourceList) {

            TranslateListener translator = null;
            MapEventDispatcher dispatcher = null;

            synchronized (eventSource.mappedListeners) {

                WeakHashMap<Listener, TranslateListener> listenerMap;

                if (eventType == null) {
                    listenerMap = eventSource.mappedListeners;
                } else {
                    listenerMap = eventSource.mappedSpecificListeners
                            .get(eventType);
                    if (listenerMap == null)
                        eventSource.mappedSpecificListeners.put(eventType,
                                listenerMap = new WeakHashMap<>());
                }

                translator = listenerMap.get(listener);

                // behavior of not allowing multiple adds per eventType
                if (translator == null) {
                    translator = new TranslateListener();
                    translator.flyweight = eventSource.flyweight;
                    translator.listener = listener;
                    listenerMap.put(listener, translator);
                    dispatcher = eventSource.mapEventDispatcher;
                }
            }

            // do dispatcher add when not duplicate
            if (dispatcher != null) {
                String[] subTypes = getReverseTranslationTypes(eventType);
                if (subTypes == null && eventType == null)
                    eventSource.mapEventDispatcher
                            .addMapEventListener(translator);
                else if (subTypes != null) {
                    for (String type : subTypes)
                        eventSource.mapEventDispatcher.addMapEventListener(type,
                                translator);
                }
            }
        }
    }

    private void removeGlobeEventListenerImpl(String eventType,
            Listener listener) {

        List<EventSource> eventSourceList;
        synchronized (copyOnWriteSync) {
            eventSourceList = eventSources;
        }

        for (EventSource eventSource : eventSourceList) {
            TranslateListener translator = null;
            MapEventDispatcher dispatcher = null;

            synchronized (eventSource.mappedListeners) {
                if (eventType == null)
                    translator = eventSource.mappedListeners.remove(listener);
                else {
                    WeakHashMap<Listener, TranslateListener> weakMap = eventSource.mappedSpecificListeners
                            .get(eventType);
                    if (weakMap != null)
                        translator = weakMap.remove(listener);
                }

                if (translator != null)
                    dispatcher = eventSource.mapEventDispatcher;
            }

            if (dispatcher != null) {
                String[] subTypes = getReverseTranslationTypes(eventType);
                if (subTypes == null)
                    eventSource.mapEventDispatcher
                            .removeMapEventListener(translator);
                else {
                    for (String type : subTypes)
                        eventSource.mapEventDispatcher
                                .removeMapEventListener(type, translator);
                }
            }
        }
    }

    private static final String[] CLICK_SUB_TYPES = {
            MapEvent.ITEM_CLICK, MapEvent.MAP_CLICK
    };
    private static final String[] PRESS_SUB_TYPES = {
            MapEvent.ITEM_PRESS, MapEvent.MAP_PRESS
    };
    private static final String[] RELEASE_SUB_TYPES = {
            MapEvent.ITEM_RELEASE, MapEvent.MAP_RELEASE
    };
    private static final String[] DOUBLE_CLICK_SUB_TYPES = {
            MapEvent.ITEM_DOUBLE_TAP, MapEvent.MAP_DOUBLE_TAP
    };
    private static final String[] LONG_PRESS_SUB_TYPES = {
            MapEvent.ITEM_LONG_PRESS, MapEvent.MAP_LONG_PRESS
    };
    private static final String[] DRAGGING_SUB_TYPES = {
            MapEvent.ITEM_DRAG_STARTED, MapEvent.ITEM_DRAG_CONTINUED,
            MapEvent.ITEM_DRAG_DROPPED
    };
    private static final String[] VIEW_CHANGING_SUB_TYPES = {
            MapEvent.MAP_MOVED, MapEvent.MAP_RESIZED,
            MapEvent.MAP_ROTATE, MapEvent.MAP_TILT,
            MapEvent.MAP_SCROLL, MapEvent.MAP_SCALE,
            MapEvent.MAP_ZOOM, MapEvent.MAP_SETTLED
    };

    private static String[] getReverseTranslationTypes(String eventType) {

        if (eventType == null)
            return null;
        else if (eventType.equals(IGlobeEvent.CLICK))
            return CLICK_SUB_TYPES;
        else if (eventType.equals(IGlobeEvent.PRESS))
            return PRESS_SUB_TYPES;
        else if (eventType.equals(IGlobeEvent.RELEASE))
            return RELEASE_SUB_TYPES;
        else if (eventType.equals(IGlobeEvent.DOUBLE_CLICK))
            return DOUBLE_CLICK_SUB_TYPES;
        else if (eventType.equals((IGlobeEvent.LONG_PRESS)))
            return LONG_PRESS_SUB_TYPES;
        else if (eventType.equals((IGlobeEvent.DRAGGING)))
            return DRAGGING_SUB_TYPES;
        else if (eventType.equals((IGlobeEvent.VIEW_CHANGING)))
            return VIEW_CHANGING_SUB_TYPES;

        return null;
    }

    private static boolean testEventTypeTranslation(String testType,
            String[] tests) {
        for (String test : tests) {
            if (test.equals(testType))
                return true;
        }
        return false;
    }

    private static String translateMapEventType(String fromType) {
        if (testEventTypeTranslation(fromType, CLICK_SUB_TYPES))
            return IGlobeEvent.CLICK;
        if (testEventTypeTranslation(fromType, PRESS_SUB_TYPES))
            return IGlobeEvent.PRESS;
        if (testEventTypeTranslation(fromType, RELEASE_SUB_TYPES))
            return IGlobeEvent.RELEASE;
        if (testEventTypeTranslation(fromType, DOUBLE_CLICK_SUB_TYPES))
            return IGlobeEvent.DOUBLE_CLICK;
        if (testEventTypeTranslation(fromType, LONG_PRESS_SUB_TYPES))
            return IGlobeEvent.LONG_PRESS;
        if (testEventTypeTranslation(fromType, DRAGGING_SUB_TYPES))
            return IGlobeEvent.DRAGGING;
        if (testEventTypeTranslation(fromType, VIEW_CHANGING_SUB_TYPES))
            return IGlobeEvent.VIEW_CHANGING;
        return null;
    }

    private IGlobeEvent translateMapEvent(EventSource eventSource,
            MapEvent event) {

        final String fromType = event.getType();
        String toType = translateMapEventType(event.getType());

        if (toType != null) {
            PointF point = new PointF(event.getPointF().x, event.getPointF().y);
            return createGlobeEvent(eventSource, point, toType);
        }

        return null;
    }

    private GlobeEvent createGlobeEvent(EventSource eventSource, PointF point,
            String globeEventType) {

        GeoPoint eventGlobeCoordinate = eventSource.inverse(point.x, point.y);

        Object[] targets = {
                eventSource.globe
        };
        GeoPoint[] targetCoordinates = {
                eventGlobeCoordinate
        };

        // INFO:  For future reference, targets should only be common API objects. A perfect
        //        candidate here is the planned MapItem unified API. Until then, ATAK provides
        //        nothing beyond the IGlobe.

        return new GlobeEvent(globeEventType, eventSource.source,
                eventSource.globeId,
                IGlobeEvent.MAIN_POINTER_ID, 0, eventGlobeCoordinate, point,
                targets,
                targetCoordinates);
    }

    /**
     * Add an event source to the implementation.
     *
     * @param source the instance provided for the IGlobeEvent.getSource() call
     * @param globe the IGlobe instance for the main event target and globe id
     * @param mapEventDispatcher the event dispatcher that triggers the globe events
     * @param inverseImpl the view point to globe coordinate implementation
     */
    public void addEventSource(@NonNull Object source, @NonNull IGlobe globe,
            @NonNull MapEventDispatcher mapEventDispatcher,
            @NonNull InverseImpl inverseImpl) {

        EventSource eventSource = new EventSource();
        eventSource.source = source;
        eventSource.globe = globe;
        eventSource.globeId = getGlobeId(globe);
        eventSource.mapEventDispatcher = mapEventDispatcher;
        eventSource.inverseImpl = inverseImpl;

        synchronized (copyOnWriteSync) {
            List<EventSource> newList = new ArrayList<>(eventSources);
            newList.add(eventSource);
            eventSources = newList;
        }
    }

    /**
     * Remove an event source
     *
     * @param source the same event source instance passed to addEventSource
     */
    public void removeEventSource(Object source) {

        List<Integer> removeIndices = new ArrayList<>(8);
        List<EventSource> oldSources;

        synchronized (copyOnWriteSync) {
            List<EventSource> newList = new ArrayList<>();
            oldSources = eventSources;
            for (int i = 0; i < eventSources.size(); ++i) {
                if (eventSources.get(i).source == source) {
                    removeIndices.add(i);
                } else {
                    newList.add(eventSources.get(i));
                }
            }
            eventSources = newList;
        }

        for (int i : removeIndices) {
            EventSource eventSource = oldSources.remove(i);

            // remove all translators
            Collection<TranslateListener> translators;
            synchronized (eventSource.mappedListeners) {
                translators = eventSource.mappedListeners.values();
            }
            for (TranslateListener translator : translators) {
                eventSource.mapEventDispatcher
                        .removeMapEventListener(translator);
            }
        }
    }
}
