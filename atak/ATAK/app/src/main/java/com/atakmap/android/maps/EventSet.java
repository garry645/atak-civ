
package com.atakmap.android.maps;

import androidx.annotation.NonNull;

import com.atakmap.coremap.log.Log;
import com.atakmap.util.ReadWriteLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Set of events used to optimize away keeping lists of ConcurrentLinkedQueue
 * property listeners when implementing objects with multiple property listeners. The behavior of
 * adding/removing and invoking is exactly like ConcurrentLinkedQueue with respect to threading
 * and concurrent access. Using this class over using many ConcurrentLinkedQueues keeps
 * memory usage down dramatically. It is optimized to utilize bitfields for different event types
 * when multiple of the same listener instance is used (which is very often the case).
 * <p>
 * See MapItem, PointMapItem and Marker for usage example
 */
public class EventSet {

    private static final String TAG = "EventSet";
    private static final boolean THROW_ARGUMENT_EXCEPTIONS = false;

    // the list of events in the set
    private final ArrayList<Parameters5<?, ?, ?, ?, ?, ?>> events = new ArrayList<>();
    private final EventSet base;
    private final String name;

    private int nextId;

    /**
     * Check if an event set contains an event
     *
     * @param event the event
     * @return true if set contained
     */
    public boolean contains(Parameters5<?, ?, ?, ?, ?, ?> event) {
        EventSet n = this;
        do {
            if (n == event.owner)
                return true;
            n = n.base;
        } while (n != null);
        return false;
    }

    /**
     * Create a set of listeners compatible with the EventSet
     *
     * @return the newly created set
     */
    public Listeners createListeners() {
        return new Listeners();
    }

    /**
     * Set of listeners compatible with the EventSet
     */
    public class Listeners {

        private ArrayList<Observer> observers0;
        private ArrayList<ArrayList<Observer>> furtherLevels;
        private final ReadWriteLock lock = new ReadWriteLock();

        /**
         * Add a listener for a given event
         *
         * @param event the event
         * @param listener the listener to invoke
         * @param <Listener> the listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> void addListener(
                Parameters5<Listener, A, B, C, D, E> event, Listener listener) {

            if (!containsEvent(event))
                return;

            lock.acquireWrite();
            try {
                addListenerImpl(event.id, listener);
            } finally {
                lock.releaseWrite();
            }
        }

        /**
         * Add a listener for a given event if it does not already exist
         *
         * @param event the event
         * @param listener the listener to invoke
         * @param <Listener> the listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> boolean addUniqueListener(
                Parameters5<Listener, A, B, C, D, E> event, Listener listener) {

            if (!containsEvent(event))
                return false;

            lock.acquireWrite();
            boolean result = false;
            try {
                if (!containsListenerImpl(event.id, listener)) {
                    addListenerImpl(event.id, listener);
                    result = true;
                }
            } finally {
                lock.releaseWrite();
            }
            return result;
        }

        /**
         * Add a listener for a multiple properties
         *
         * @param events the events
         * @param listener the listener to invoke
         * @param <Listener> the listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> void addListenerToProperties(
                Listener listener,
                Parameters5<Listener, A, B, C, D, E>... events) {

            for (Parameters5<Listener, A, B, C, D, E> event : events) {
                if (!containsEvent(event))
                    return;
            }

            lock.acquireWrite();
            try {
                for (Parameters5<Listener, A, B, C, D, E> event : events) {
                    addListenerImpl(event.id, listener);
                }
            } finally {
                lock.releaseWrite();
            }
        }

        /**
         * Remove a listener for a event
         *
         * @param event the event
         * @param listener the listener to invoke
         * @param <Listener> the listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> void removeListener(
                Parameters5<Listener, A, B, C, D, E> event, Listener listener) {

            if (!containsEvent(event))
                return;

            lock.acquireWrite();
            try {
                removeListenerImpl(event.id, listener);
            } finally {
                lock.releaseWrite();
            }
        }

        /**
         * Remove a listener for a multiple properties
         *
         * @param events the events
         * @param listener the listener
         * @param <Listener> the listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> void removeListenerFromProperties(
                Listener listener,
                Parameters5<Listener, A, B, C, D, E>... events) {

            for (Parameters5<Listener, A, B, C, D, E> event : events) {
                if (!containsEvent(event))
                    return;
            }

            lock.acquireWrite();
            try {
                for (Parameters5<Listener, A, B, C, D, E> event : events) {
                    removeListenerImpl(event.id, listener);
                }
            } finally {
                lock.releaseWrite();
            }
        }

        /**
         * Clear all listeners
         */
        public void clear() {
            lock.acquireWrite();
            try {
                observers0 = null;
                furtherLevels = null;
            } finally {
                lock.releaseWrite();
            }
        }

        private boolean containsEvent(Parameters5<?, ?, ?, ?, ?, ?> event) {
            if (!EventSet.this.contains(event)) {
                Log.e(TAG, "'" + event + "' does not belong to set '"
                        + EventSet.this + "'");
                if (THROW_ARGUMENT_EXCEPTIONS)
                    throw new IllegalArgumentException();
                return false;
            }
            return true;
        }

        private List<Observer> getObservers(int eventId) {
            int level = getEventLevel(eventId);
            if (level == 0)
                return observers0 != null ? observers0
                        : Collections.emptyList();
            int furtherIndex = level - 1;
            if (furtherLevels == null || furtherIndex >= furtherLevels.size())
                return Collections.emptyList();
            return furtherLevels.get(furtherIndex);
        }

        private List<Observer> ensureObservers(int eventId) {
            int level = getEventLevel(eventId);
            if (level == 0) {
                if (observers0 == null)
                    observers0 = new ArrayList<>();
                return observers0;
            }
            int furtherIndex = level - 1;
            if (furtherLevels == null)
                furtherLevels = new ArrayList<>();
            while (furtherIndex >= furtherLevels.size())
                furtherLevels.add(new ArrayList<>());
            return furtherLevels.get(furtherIndex);
        }

        private void addListenerImpl(int eventId, Object listener) {
            long bit = getEventBit(eventId);
            long oldBits = 0;
            Observer o = null;

            List<Observer> observers = ensureObservers(eventId);
            for (Observer po : observers) {
                long bits = 0;
                if (po.listener == listener
                        && ((bits = po.bits.get()) & bit) == 0) {
                    o = po;
                    oldBits = bits;
                }
            }

            if (o == null) {
                o = new Observer(listener);
                observers.add(o);
            }

            // writing bits is protected by the write-lock, so oldBits is valid until write released.
            // Any Invoke of gathered listeners will check bits right before invoking, so the
            // atomic replicates behavior of ConcurrentLinkedQueue when accessed concurrently
            o.bits.set(oldBits | bit);
        }

        private boolean containsListenerImpl(int eventId, Object listener) {
            long bit = getEventBit(eventId);

            List<Observer> observers = getObservers(eventId);
            for (Observer po : observers) {
                if (po.listener == listener && (po.bits.get() & bit) != 0) {
                    return true;
                }
            }
            return false;
        }

        private void removeListenerImpl(int eventId, Object listener) {
            long bit = getEventBit(eventId);

            List<Observer> observers = getObservers(eventId);
            int index = getLastIndexWithBits(bit, observers, listener);
            if (index != -1) {
                Observer l = observers.get(index);

                // writing bits is protected by write-lock, but any invoke of gathered listeners
                // will check bits before invoking, hence the atomic
                if (atomicRemoveBits(l.bits, bit) == 0)
                    observers.remove(index);
            }
        }

        private <Listener, A, B, C, D, E> void invokeImpl(
                Parameters5<Listener, A, B, C, D, E> event, A a, B b, C c, D d,
                E e) {

            // gather up listeners to invoke, avoid heap allocations unless necessary (to reduce garbage)
            Observer[] invokes = {
                    null, null, null
            }; // 3 listener slots on stack
            ArrayList<Observer> overflow = null; // overflow if needed
            long bit = getEventBit(event.id);

            lock.acquireRead();
            try {
                List<Observer> observers = getObservers(event.id);

                int index = 0;
                for (Observer o : observers) {
                    if ((o.bits.get() & bit) != 0) {
                        if (index >= invokes.length) {
                            if (overflow == null)
                                overflow = new ArrayList<>();
                            overflow.add(o);
                        } else {
                            invokes[index++] = o;
                        }
                    }
                }
            } finally {
                lock.releaseRead();
            }

            // invoke all gathered listeners (check bits first)

            for (Observer o : invokes) {
                if (o == null)
                    break;
                invokeListenerWithBitCheck(event, a, b, c, d, e, o);

            }
            if (overflow != null) {
                for (Observer o : overflow) {
                    invokeListenerWithBitCheck(event, a, b, c, d, e, o);
                }
            }
        }

        private <Listener, A, B, C, D, E> void invokeListenerWithBitCheck(
                Parameters5<Listener, A, B, C, D, E> event, A a, B b, C c, D d,
                E e, Observer o) {
            // check bits so behavior is similar to ConcurrentLinkedQueue supporting concurrent
            // removals

            long bit = getEventBit(event.id);
            if ((o.bits.get() & bit) != 0) {
                event.invoker.invoke((Listener) o.listener, a, b, c, d, e);
            }
        }

        private int getLastIndexWithBits(long bits, List<Observer> observers,
                Object listener) {
            int result = -1;
            for (int i = 0; i < observers.size(); ++i) {
                Observer l = observers.get(i);
                if (l.listener == listener && (l.bits.get() & bits) == bits)
                    result = i;
            }
            return result;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    /**
     * Listener invoker with 0 arguments
     *
     * @param <Listener> the listener type
     */
    public interface Invoker0<Listener>
            extends Invoker5<Listener, Void, Void, Void, Void, Void> {

        void invoke(Listener l);

        default void invoke(Listener l, Void a, Void b, Void c, Void d,
                Void e) {
            invoke(l);
        }
    }

    /**
     * Listener invoker with 1 argument
     *
     * @param <Listener> the listener type
     * @param <A> argument 1 type
     */
    public interface Invoker1<Listener, A>
            extends Invoker5<Listener, A, Void, Void, Void, Void> {

        void invoke(Listener l, A a);

        default void invoke(Listener l, A a, Void b, Void c, Void d, Void e) {
            invoke(l, a);
        }
    }

    /**
     * Listener invoker with 2 arguments
     *
     * @param <Listener> the listener type
     * @param <A> argument 1 type
     * @param <B> argument 2 type
     */
    public interface Invoker2<Listener, A, B>
            extends Invoker5<Listener, A, B, Void, Void, Void> {
        void invoke(Listener l, A a, B b);

        default void invoke(Listener l, A a, B b, Void c, Void d, Void e) {
            invoke(l, a, b);
        }
    }

    /**
     * Listener invoker with 3 arguments
     *
     * @param <Listener> the listener type
     * @param <A> argument 1 type
     * @param <B> argument 2 type
     * @param <C> argument 3 type
     */
    public interface Invoker3<Listener, A, B, C>
            extends Invoker5<Listener, A, B, C, Void, Void> {
        void invoke(Listener l, A a, B b, C c);

        default void invoke(Listener l, A a, B b, C c, Void d, Void e) {
            invoke(l, a, b, c);
        }
    }

    /**
     * Listener invoker with 4 arguments
     *
     * @param <Listener> the listener type
     * @param <A> argument 1 type
     * @param <B> argument 2 type
     * @param <C> argument 3 type
     * @param <D> argument 4 type
     */
    public interface Invoker4<Listener, A, B, C, D>
            extends Invoker5<Listener, A, B, C, D, Void> {
        void invoke(Listener l, A a, B b, C c, D d);

        default void invoke(Listener l, A a, B b, C c, D d, Void e) {
            invoke(l, a, b, c, d);
        }
    }

    /**
     * Listener invoker with 4 arguments
     *
     * @param <Listener> the listener type
     * @param <A> argument 1 type
     * @param <B> argument 2 type
     * @param <C> argument 3 type
     * @param <D> argument 4 type
     * @param <E> argument 5 type
     */
    public interface Invoker5<Listener, A, B, C, D, E> {
        void invoke(Listener l, A a, B b, C c, D d, E e);
    }

    /**
     * Event with 5 arguments
     *
     * @param <Listener> the listener type
     * @param <A> type of argument 1
     * @param <B> type of argument 2
     * @param <C> type of argument 3
     * @param <D> type of argument 4
     * @param <E> type of argument 5
     */
    public static class Parameters5<Listener, A, B, C, D, E> {
        private final int id;
        private final Invoker5<Listener, A, B, C, D, E> invoker;

        private final EventSet owner;

        private Parameters5(EventSet owner, int id,
                Invoker5<Listener, A, B, C, D, E> invoker) {
            this.id = id;
            this.invoker = invoker;
            this.owner = owner;
        }

        public void invoke(EventSet.Listeners listeners, A a, B b, C c, D d,
                E e) {
            listeners.invokeImpl(this, a, b, c, d, e);
        }
    }

    /**
     * Event with 4 arguments
     *
     * @param <Listener> the listener type
     * @param <A> type of argument 1
     * @param <B> type of argument 2
     * @param <C> type of argument 3
     * @param <D> type of argument 4
     */
    public static class Parameters4<Listener, A, B, C, D>
            extends Parameters5<Listener, A, B, C, D, Void> {

        private Parameters4(EventSet owner, int id,
                Invoker4<Listener, A, B, C, D> invoker) {
            super(owner, id, invoker);
        }

        public void invoke(EventSet.Listeners listeners, A a, B b, C c, D d) {
            listeners.invokeImpl(this, a, b, c, d, null);
        }
    }

    /**
     * Event with 3 arguments
     *
     * @param <Listener> the listener type
     * @param <A> type of argument 1
     * @param <B> type of argument 2
     * @param <C> type of argument 3
     */
    public static class Parameters3<Listener, A, B, C>
            extends Parameters5<Listener, A, B, C, Void, Void> {

        private Parameters3(EventSet owner, int id,
                Invoker3<Listener, A, B, C> invoker) {
            super(owner, id, invoker);
        }

        public void invoke(EventSet.Listeners listeners, A a, B b, C c) {
            listeners.invokeImpl(this, a, b, c, null, null);
        }
    }

    /**
     * Event with 2 arguments
     *
     * @param <Listener> the listener type
     * @param <A> type of argument 1
     * @param <B> type of argument 2
     */
    public static class Parameters2<Listener, A, B>
            extends Parameters5<Listener, A, B, Void, Void, Void> {

        private Parameters2(EventSet owner, int id,
                Invoker2<Listener, A, B> invoker) {
            super(owner, id, invoker);
        }

        public void invoke(EventSet.Listeners listeners, A a, B b) {
            listeners.invokeImpl(this, a, b, null, null, null);
        }
    }

    /**
     * Event with 1 argument
     *
     * @param <Listener> the listener type
     * @param <A> type of argument 1
     */
    public static class Parameters1<Listener, A>
            extends Parameters5<Listener, A, Void, Void, Void, Void> {

        private Parameters1(EventSet owner, int id,
                Invoker1<Listener, A> invoker) {
            super(owner, id, invoker);
        }

        public void invoke(EventSet.Listeners listeners, A a) {
            listeners.invokeImpl(this, a, null, null, null, null);
        }
    }

    /**
     * Event with 0 arguments
     *
     * @param <Listener> the listener type
     */
    public static class Parameters0<Listener>
            extends Parameters5<Listener, Void, Void, Void, Void, Void> {

        private Parameters0(EventSet owner, int id,
                Invoker0<Listener> invoker) {
            super(owner, id, invoker);
        }

        public void invoke(EventSet.Listeners listeners) {
            listeners.invokeImpl(this, null, null, null, null, null);
        }
    }

    /**
     * Builder of a EventSet
     */
    public static class Builder {

        private EventSet def;

        /**
         * Create a new builder starting with no previous properties
         */
        public Builder(String name) {
            def = new EventSet(name);
        }

        /**
         * Create a new builder starting with a previous set of properties (derive)
         *
         * @param base the base event set
         */
        public Builder(EventSet base, String name) {
            def = new EventSet(base, name);
        }

        /**
         * Add an event with an invoker with 5 arguments
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         * @param <E> type of argument 5
         */
        public <Listener, A, B, C, D, E> Parameters5<Listener, A, B, C, D, E> addEvent(
                Invoker5<Listener, A, B, C, D, E> invoker) {
            int id = def.nextId++;
            Parameters5<Listener, A, B, C, D, E> result = new Parameters5<>(def,
                    id, invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Add an event with an invoker with 4 arguments
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         * @param <D> type of argument 4
         */
        public <Listener, A, B, C, D> Parameters4<Listener, A, B, C, D> addEvent(
                Invoker4<Listener, A, B, C, D> invoker) {
            int id = def.nextId++;
            Parameters4<Listener, A, B, C, D> result = new Parameters4<>(def,
                    id, invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Add an event with an invoker with 3 arguments
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         * @param <C> type of argument 3
         */
        public <Listener, A, B, C> Parameters3<Listener, A, B, C> addEvent(
                Invoker3<Listener, A, B, C> invoker) {
            int id = def.nextId++;
            Parameters3<Listener, A, B, C> result = new Parameters3<>(def, id,
                    invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Add an event with an invoker with 2 arguments
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         * @param <A> type of argument 1
         * @param <B> type of argument 2
         */
        public <Listener, A, B> Parameters2<Listener, A, B> addEvent(
                Invoker2<Listener, A, B> invoker) {
            int id = def.nextId++;
            Parameters2<Listener, A, B> result = new Parameters2<>(def, id,
                    invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Add an event with an invoker with 1 argument
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         * @param <A> type of argument 1
         */
        public <Listener, A> Parameters1<Listener, A> addEvent(
                Invoker1<Listener, A> invoker) {
            int id = def.nextId++;
            Parameters1<Listener, A> result = new Parameters1<>(def, id,
                    invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Add an event with an invoker with 0 arguments
         *
         * @param invoker the invoker
         * @return event instance
         *
         * @param <Listener> listener type
         */
        public <Listener> Parameters0<Listener> addEvent(
                Invoker0<Listener> invoker) {
            int id = def.nextId++;
            Parameters0<Listener> result = new Parameters0<>(def, id, invoker);
            def.events.add(result);
            return result;
        }

        /**
         * Get a build of the EventSet as currently configured
         *
         * @return new instance of an EventSet
         */
        public EventSet build() {
            EventSet result = def;
            def = new EventSet(def);
            return result;
        }
    }

    private EventSet(String name) {
        this.name = name;
        this.base = null;
    }

    private EventSet(EventSet other) {
        this.name = other.name;
        this.base = other.base;
        this.nextId = other.nextId;
    }

    private EventSet(EventSet base, String name) {
        this.name = name;
        this.base = base;
        this.nextId = base.nextId;
    }

    // Observer is a single listener instance and atomic bits that represent the set of properties
    // that the listener will invoke on.
    private static class Observer {

        Observer(Object listener) {
            this.listener = listener;
        }

        final AtomicLong bits = new AtomicLong();

        final Object listener;
    }

    private static long atomicRemoveBits(final AtomicLong dst,
            final long bits) {
        long oldValue = dst.get();
        long newValue = oldValue & ~bits;
        while (oldValue != newValue && !dst.compareAndSet(oldValue, newValue)) {
            oldValue = dst.get();
            newValue = oldValue & ~bits;
        }
        return newValue;
    }

    private static int getEventLevel(int eventId) {
        return eventId / 64;
    }

    private static long getEventBit(int eventId) {
        return (1L << (eventId & 63));
    }
}
