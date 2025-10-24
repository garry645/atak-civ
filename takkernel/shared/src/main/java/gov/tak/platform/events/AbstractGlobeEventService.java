package gov.tak.platform.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.atakmap.coremap.log.Log;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.events.IGlobeEventService2;

/**
 * Abstract base for a typical IGlobeEventService
 */
public abstract class AbstractGlobeEventService implements IGlobeEventService2
{

    private static final String TAG = "AbstractGlobeEventService";
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final List<ConcurrentLinkedQueue<Listener>> stack = new ArrayList<>();
    private final List<Map<String, ConcurrentLinkedQueue<Listener>>> specificStack = new ArrayList<>();

    /**
     * Create a abstract globe event service
     */
    protected AbstractGlobeEventService()
    {
        stack.add(new ConcurrentLinkedQueue<>());
        specificStack.add(new HashMap<>());
    }

    @Override
    public void addGlobeEventListener(Listener listener)
    {
        ConcurrentLinkedQueue<Listener> listeners = getTopLevelListeners();
        listeners.add(listener);
    }

    @Override
    public void addSpecificGlobeEventListener(String eventType, Listener listener)
    {
        ConcurrentLinkedQueue<Listener> listeners = getTopLevelSpecificListeners(eventType, true);
        listeners.add(listener);
    }

    @Override
    public void removeGlobeEventListener(Listener listener)
    {
        ConcurrentLinkedQueue<Listener> listeners = getTopLevelListeners();
        listeners.remove(listener);
    }

    @Override
    public void removeSpecificGlobeEventListener(String eventType, Listener listener)
    {
        ConcurrentLinkedQueue<Listener> listeners = getTopLevelSpecificListeners(eventType, false);
        if (listeners != null)
            listeners.remove(listener);
    }

    @Override
    public void pushGlobeEventListeners()
    {
        rwlock.writeLock().lock();
        try
        {
            pushGlobeEventListenersNoSync();
        } finally
        {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void popGlobeEventListeners()
    {
        rwlock.writeLock().lock();
        try
        {
            popGlobeEventListenersNoSync();
        } finally
        {
            rwlock.writeLock().unlock();
        }
    }

    private void popGlobeEventListenersNoSync()
    {
        // don't pop the base ever
        if (stack.size() > 1)
            stack.remove(stack.size() - 1);

        // should be the same size
        if (specificStack.size() > 1)
            specificStack.remove(specificStack.size() - 1);
    }

    private void pushGlobeEventListenersNoSync()
    {
        int safeReturnSize = stack.size();
        try
        {
            // mimic behavior ATAK established of copying top level listeners

            ConcurrentLinkedQueue<Listener> top = new ConcurrentLinkedQueue<>(stack.get(stack.size() - 1));

            Map<String, ConcurrentLinkedQueue<Listener>> specificTop = new HashMap<>();
            for (Map.Entry<String, ConcurrentLinkedQueue<Listener>> entry : specificStack.get(specificStack.size() - 1).entrySet())
            {
                specificTop.put(entry.getKey(), new ConcurrentLinkedQueue<>(entry.getValue()));
            }

            stack.add(top);
            specificStack.add(specificTop);

        } catch (Throwable t)
        {

            // make sure both stack arrays are returned to safe sizes if anything throws
            if (stack.size() > safeReturnSize)
                stack.remove(stack.size() - 1);
            if (specificStack.size() > safeReturnSize)
                specificStack.remove(specificStack.size() - 1);

            // rethrow
            throw t;
        }
    }

    /**
     * Trigger callbacks on the listeners at the top of the stack. Callback stops when the event isHandled() returns
     * true.
     *
     * @param globeEvent the globe event
     */
    protected void raiseTopLevelGlobeEvent(@NonNull IGlobeEvent globeEvent)
    {
        callbackListeners(globeEvent, getTopLevelSpecificListeners(globeEvent.getType(), false));
        callbackListeners(globeEvent, getTopLevelListeners());
    }

    private ConcurrentLinkedQueue<Listener> getTopLevelListeners()
    {
        ConcurrentLinkedQueue<Listener> listeners = null;
        rwlock.readLock().lock();
        try
        {
            listeners = stack.get(stack.size() - 1);
        } finally
        {
            rwlock.readLock().unlock();
        }

        return listeners;
    }

    private ConcurrentLinkedQueue<Listener> getTopLevelSpecificListeners(@NonNull String eventType, final boolean ensure)
    {
        ConcurrentLinkedQueue<Listener> listeners = null;
        rwlock.readLock().lock();
        try
        {
            listeners = specificStack.get(specificStack.size() - 1).get(eventType);
        } finally
        {
            rwlock.readLock().unlock();
        }

        // if nothing turned up and ensuring, do again but with write lock
        if (listeners == null && ensure)
        {
            rwlock.writeLock().lock();
            try
            {
                Map<String, ConcurrentLinkedQueue<Listener>> listenerMap = specificStack.get(specificStack.size() - 1);

                // need to fully check again because another thread could have created between
                // last read and this write
                listeners = listenerMap.get(eventType);
                if (listeners == null)
                    listenerMap.put(eventType, listeners = new ConcurrentLinkedQueue<>());
            } finally
            {
                rwlock.writeLock().unlock();
            }
        }

        return listeners;
    }

    private int callbackListeners(IGlobeEvent globeEvent, ConcurrentLinkedQueue<Listener> listeners)
    {
        int callCount = 0;
        if (listeners != null)
        {
            for (Listener l : listeners)
            {
                try
                {
                    if (globeEvent.isHandled())
                        break;
                    l.onGlobeEvent(globeEvent);
                    ++callCount;
                } catch (Throwable t)
                {
                    Log.d(TAG, "Globe event listener throw: " + t);
                }
            }
        }
        return callCount;
    }
}
