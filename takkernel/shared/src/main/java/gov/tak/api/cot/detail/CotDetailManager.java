package gov.tak.api.cot.detail;

import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.detail.ICotDetailHandler.ImportResult;
import gov.tak.api.symbology.MilSymDetailHandler;
import gov.tak.api.util.AttributeSet;

/**
 * Adds {@link CotDetail}s to {@link AttributeSet}s and processes {@link AttributeSet}s into
 * {@link CotDetail}s for all registered {@link ICotDetailHandler}s.
 *
 * @since 6.0.0
 */
public final class CotDetailManager {

    private static final String TAG = "CotDetailManager";

    // Detail handlers
    private final Map<String, Set<ICotDetailHandler2>> _handlerMap = new HashMap<>();

    private final Set<ICotDetailHandler2> _handlers = new HashSet<>();

    private final Map<ICotDetailHandler, ICotDetailHandler2> _adapters = new IdentityHashMap<>();

    public CotDetailManager() {
        registerDefaultHandlers();
    }

    public synchronized boolean hasHandler(String name) {
        for(ICotDetailHandler2 handler : _handlers)
            if(handler.getDetailNames().contains(name))
                return true;
        return false;
    }

    /**
     * Register a detail handler for the given detail element name.  Registration of a handler
     * object more than once is disallowed.
     *
     * @param handler CoT detail handler
     *
     * @deprecated use {@link #registerHandler(ICotDetailHandler2)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3.0", forRemoval = false, removeAt = "5.7.0")
    public synchronized void registerHandler(ICotDetailHandler handler) {
        if(_adapters.containsKey(handler))
            return;
        ICotDetailHandler2 adapted = new DetailHandlerAdapter(handler);
        _adapters.put(handler, adapted);
        registerHandler(adapted);
    }

    /**
     * Register a detail handler for the given detail element name.  Registration of a handler
     * object more than once is disallowed.
     *
     * @param handler CoT detail handler
     */
    public synchronized void registerHandler(ICotDetailHandler2 handler) {
        if (_handlers.contains(handler))
            return;
        Set<String> names = handler.getDetailNames();
        for (String key : names) {
            Set<ICotDetailHandler2> set = _handlerMap.get(key);
            if (set == null) {
                set = new LinkedHashSet<>();
                _handlerMap.put(key, set);
            }
            set.add(handler);
        }
        _handlers.add(handler);
    }

    /**
     * Unregister a detail handler
     *
     * @param handler CoT detail handler
     *
     * @deprecated use {@link #unregisterHandler(ICotDetailHandler2)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3.0", forRemoval = false, removeAt = "5.7.0")
    public synchronized void unregisterHandler(ICotDetailHandler handler) {
        final ICotDetailHandler2 adapted = _adapters.remove(handler);
        if (adapted == null)
            return;
        unregisterHandler(adapted);
    }

    /**
     * Unregister a detail handler
     *
     * @param handler CoT detail handler
     */
    public synchronized void unregisterHandler(ICotDetailHandler2 handler) {
        if (!_handlers.contains(handler))
            return;
        Set<String> names = handler.getDetailNames();
        for (String key : names) {
            Set<ICotDetailHandler2> set = _handlerMap.get(key);
            if (set != null && set.remove(handler) && set.isEmpty())
                _handlerMap.remove(key);
        }
        _handlers.remove(handler);
    }

    /**
     * Get a list of detail handlers
     *
     * @return List of CoT event detail handlers
     */
    private synchronized List<ICotDetailHandler2> getHandlers() {
        return new ArrayList<>(_handlers);
    }

    /**
     * Create and add CoT details to a given event
     *
     * @param attrs The {@link AttributeSet} to read from
     * @param event The {@link CotEvent} to add to
     * @return {@code true} if one or more {@link CotDetail}s were added, {@code false} if not
     *
     * @deprecated use {@link #addDetails(Object, AttributeSet, CotEvent)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3.0", forRemoval = false, removeAt = "5.7.0")
    public boolean addDetails(AttributeSet attrs, CotEvent event) {
        return addDetails(null, attrs, event);
    }

    /**
     * Create and add CoT details to a given event
     *
     * @param processItem The item being processed; owner of {@code attrs}
     * @param attrs The {@link AttributeSet} to read from
     * @param event The {@link CotEvent} to add to
     * @return {@code true} if one or more {@link CotDetail}s were added, {@code false} if not
     */
    public boolean addDetails(Object processItem, AttributeSet attrs, CotEvent event) {
        boolean ret = false;
        CotDetail root = event.getDetail();
        List<ICotDetailHandler2> handlers = getHandlers();
        for (ICotDetailHandler2 h : handlers) {
            if (h.isSupported(processItem, attrs, event, root)) {
                ret |= h.toCotDetail(processItem, attrs, event, root);
            }
        }

        return ret;
    }

    /**
     * Given a map item and a cot event, process the cot event details into the appropriate
     * tags within the map item
     * @param item the map item to fill
     * @param event the cot event to use
     * @return the map item correctly reflects the values provided by the cot event.
     *
     * @deprecated use {@link #processDetails(Object, AttributeSet, CotEvent)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3.0", forRemoval = false, removeAt = "5.7.0")
    public ImportResult processDetails(AttributeSet item, CotEvent event) {
        return processDetails(null, item, event);
    }

    /**
     * Given a map item and a cot event, process the cot event details into the appropriate
     * tags within the map item
     * @param processItem The item being processed; owner of {@code attrs}
     * @param attrs the map item to fill
     * @param event the cot event to use
     * @return the map item correctly reflects the values provided by the cot event.
     */
    public ImportResult processDetails(Object processItem, AttributeSet attrs, CotEvent event) {
        CotDetail root = event.getDetail();
        if (root == null)
            return ImportResult.IGNORE;

        // Add all the sets first before calling the process method so we
        // don't run into potential deadlock
        List<ProcessSet> sets = new ArrayList<>();
        List<CotDetail> children = root.getChildren();
        synchronized (this) {
            for (CotDetail d : children) {
                if (d == null)
                    continue;
                String name = d.getElementName();

                // Regular handlers
                Set handlers = _handlerMap.get(name);
                if (handlers != null) {
                    Set<ICotDetailHandler2> copy = null;
                    for (Object it : handlers) {
                        final ICotDetailHandler2 h = (ICotDetailHandler2) it;
                        if (h.isSupported(processItem, attrs, event, d)) {
                            if (copy == null)
                                copy = new LinkedHashSet<>(handlers.size());
                            copy.add(h);
                        }
                    }
                    handlers = copy;
                }

                sets.add(new ProcessSet(d, handlers));
            }
        }

        // Now process the sets
        ImportResult res = ImportResult.IGNORE;
        for (ProcessSet ps : sets) {
            if (ps.handlers != null) {
                for (ICotDetailHandler2 h : ps.handlers) {
                    try {
                        ImportResult r = h.toItemMetadata(processItem, attrs, event,
                                ps.detail);
                        if (r == ImportResult.FAILURE)
                            Log.e(TAG,
                                    "Failed to process detail: " + ps.detail);
                        res = res.getHigherPriority(r);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to process detail: " + ps.detail, e);
                    }
                }
            }
        }
        return res;
    }

    private static class ProcessSet {

        private final CotDetail detail;
        private final Set<ICotDetailHandler2> handlers;

        ProcessSet(CotDetail detail, Set<ICotDetailHandler2> handlers) {
            this.detail = detail;
            this.handlers = handlers;
        }
    }

    private void registerDefaultHandlers() {
        registerHandler(new BullseyeDetailHandler());
        registerHandler(new MilSymDetailHandler());
        registerHandler(new GroupDetailHandler());
    }

    final static class DetailHandlerAdapter implements ICotDetailHandler2
    {

        final ICotDetailHandler _impl;

        DetailHandlerAdapter(ICotDetailHandler impl) {
            _impl = impl;
        }

        @Override
        public Set<String> getDetailNames() {
            return _impl.getDetailNames();
        }

        @Override
        public ImportResult toItemMetadata(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
            return _impl.toItemMetadata(attrs, event, detail);
        }

        @Override
        public boolean toCotDetail(Object processItem, AttributeSet attrs, CotEvent event, CotDetail root) {
            return _impl.toCotDetail(attrs, event, root);
        }

        @Override
        public boolean isSupported(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
            return _impl.isSupported(attrs, event, detail);
        }
    }
}
