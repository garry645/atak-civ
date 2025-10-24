
package com.atakmap.android.cot.detail;

import com.atakmap.android.cot.MarkerDetailHandler;
import com.atakmap.android.cot.OpaqueHandler;
import com.atakmap.android.cot.UIDHandler;
import com.atakmap.android.emergency.EmergencyDetailHandler;
import com.atakmap.android.geofence.data.GeoFenceDetailHandler;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.routes.cot.MarkerIncludedRouteDetailHandler;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.detail.ICotDetailHandler2;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;

public class CotDetailManager {

    static {
        MarshalManager.registerMarshal(CotMarshals.ATAKMAP_GOVTAK_COT_EVENT,
                CotEvent.class, gov.tak.api.cot.event.CotEvent.class);
        MarshalManager.registerMarshal(CotMarshals.ATAKMAP_GOVTAK_COT_DETAIL,
                CotDetail.class, gov.tak.api.cot.event.CotDetail.class);
        MarshalManager.registerMarshal(CotMarshals.GOVTAK_ATAKMAP_COT_DETAIL,
                gov.tak.api.cot.event.CotDetail.class, CotDetail.class);
    }

    private static final String TAG = "CotDetailManager";

    private static CotDetailManager _instance;

    public static CotDetailManager getInstance() {
        return _instance;
    }

    private final gov.tak.api.cot.detail.CotDetailManager _kernelDetailManager;
    private final MapView _mapView;

    // Detail handlers
    private final Map<String, Set<CotDetailHandler>> _handlerMap = new HashMap<>();
    private final Set<CotDetailHandler> _handlers = new HashSet<>();

    // Marker-specific detail handlers (legacy; use CotDetailHandler instead)
    private final Map<String, Set<MarkerDetailHandler>> _markerHandlerMap = new HashMap<>();
    private final Set<MarkerDetailHandler> _markerHandlers = new HashSet<>();

    public CotDetailManager(MapView mapView) {
        _mapView = mapView;
        _kernelDetailManager = new gov.tak.api.cot.detail.CotDetailManager();
        if (_instance == null)
            _instance = this;
        registerDefaultHandlers();
    }

    /**
     * Register a detail handler for the given detail element name.  Registration of a handler
     * object more than once is disallowed.
     *
     * @param handler CoT detail handler
     */
    public synchronized void registerHandler(CotDetailHandler handler) {
        if (_handlers.contains(handler))
            return;
        Set<String> names = handler.getDetailNames();
        for (String key : names) {
            Set<CotDetailHandler> set = _handlerMap.get(key);
            if (set == null) {
                set = new HashSet<>();
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
     */
    public synchronized void unregisterHandler(CotDetailHandler handler) {
        if (!_handlers.contains(handler))
            return;
        Set<String> names = handler.getDetailNames();
        for (String key : names) {
            Set<CotDetailHandler> set = _handlerMap.get(key);
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
    private synchronized List<CotDetailHandler> getHandlers() {
        return new ArrayList<>(_handlers);
    }

    /**
     * Register a marker-specific detail handler
     * This is here for legacy compatibility - detail handlers should
     * extend {@link CotDetailHandler} instead
     *
     * @param detailName Detail name used to lookup the handler
     * @param handler Marker handler
     */
    public synchronized void registerHandler(String detailName,
            MarkerDetailHandler handler) {
        if (_markerHandlers.contains(handler))
            return;
        Set<MarkerDetailHandler> set = _markerHandlerMap.get(detailName);
        if (set == null) {
            set = new HashSet<>();
            _markerHandlerMap.put(detailName, set);
        }
        set.add(handler);
        _markerHandlers.add(handler);
    }

    /**
     * Unregister a marker-specific detail handler
     * This is here for legacy compatibility - detail handlers should
     * extend {@link CotDetailHandler} instead
     *
     * @param handler Marker handler
     */
    public synchronized void unregisterHandler(MarkerDetailHandler handler) {
        if (!_markerHandlers.contains(handler))
            return;
        for (Set<MarkerDetailHandler> handlers : _markerHandlerMap.values())
            handlers.remove(handler);
        _markerHandlers.remove(handler);
    }

    /**
     * Registers an {@code ICotDetailHandler}
     *
     * @param detailHandler gov.tak {@code ICotDetailHandler} to unregister with the kernel
     *                      detail manager
     *
     * @deprecated use {@link #registerHandler(ICotDetailHandler2)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public void registerHandler(ICotDetailHandler detailHandler) {
        _kernelDetailManager.registerHandler(detailHandler);
    }

    /**
     * Registers an {@code ICotDetailHandler2}
     *
     * @param detailHandler gov.tak {@code ICotDetailHandler2} to unregister with the kernel
     *                      detail manager
     */
    public void registerHandler(ICotDetailHandler2 detailHandler) {
        _kernelDetailManager.registerHandler(detailHandler);
    }

    /**
     * Unregisters an {@code ICotDetailHandler}
     *
     * @param detailHandler gov.tak {@code ICotDetailHandler} to unregister with the kernel
     *                      detail manager
     *
     * @deprecated use {@link #unregisterHandler(ICotDetailHandler2)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public void unregisterHandler(ICotDetailHandler detailHandler) {
        _kernelDetailManager.unregisterHandler(detailHandler);
    }

    /**
     * Unregisters an {@code ICotDetailHandler2}
     *
     * @param detailHandler gov.tak {@code ICotDetailHandler2} to unregister with the kernel
     *                      detail manager
     */
    public void unregisterHandler(ICotDetailHandler2 detailHandler) {
        _kernelDetailManager.unregisterHandler(detailHandler);
    }

    /**
     * Get a list of all registered marker handlers
     *
     * @return List of marker handlers
     */
    private synchronized List<MarkerDetailHandler> getMarkerHandlers() {
        return new ArrayList<>(_markerHandlers);
    }

    /**
     * Create and add CoT details to a given event
     *
     * @param item The item to read from
     * @param event The CoT event to add to
     * @return True if one or more details were added, false if not
     */
    public boolean addDetails(MapItem item, CotEvent event) {
        boolean ret = addKernelDetails(item, event);

        CotDetail root = event.getDetail();
        List<CotDetailHandler> handlers = getHandlers();
        for (CotDetailHandler h : handlers) {
            if (h.isSupported(item, event, root))
                ret |= h.toCotDetail(item, event, root);
        }
        if (item instanceof Marker) {
            Marker marker = (Marker) item;
            List<MarkerDetailHandler> markerHandlers = getMarkerHandlers();
            for (MarkerDetailHandler h : markerHandlers)
                h.toCotDetail(marker, root);
        }

        // Include any leftover opaque details in the root node
        OpaqueHandler.getInstance().toCotDetail(item, root);

        return ret;
    }

    private boolean addKernelDetails(MapItem item, CotEvent event) {
        AttributeSet attrs = MarshalManager.marshal(
                item, MetaDataHolder2.class, AttributeSet.class);
        gov.tak.api.cot.event.CotEvent govEvent = MarshalManager.marshal(
                event, CotEvent.class, gov.tak.api.cot.event.CotEvent.class);
        boolean ret = _kernelDetailManager.addDetails(item, attrs, govEvent);
        CotDetail detail = MarshalManager.marshal(
                govEvent.getDetail(), gov.tak.api.cot.event.CotDetail.class,
                CotDetail.class);
        event.setDetail(detail);
        return ret;
    }

    /**
     * Given a map item and a cot event, process the cot event details into the appropriate
     * tags within the map item
     * @param item the map item to fill
     * @param event the cot event to use
     * @return the map item correctly reflects the details provided by the cot event.   This
     * should not process the outer event information such as point, stale time, etc.
     */
    public ImportResult processDetails(MapItem item, CotEvent event) {
        CotDetail root = event.getDetail();
        if (root == null)
            return ImportResult.FAILURE;

        ImportResult res = processKernelDetails(item, event);

        // Add all the sets first before calling the process method so we
        // don't run into potential deadlock
        Marker marker = item instanceof Marker ? (Marker) item : null;
        List<ProcessSet> sets = new ArrayList<>();
        List<CotDetail> children = root.getChildren();

        synchronized (this) {
            for (CotDetail d : children) {
                if (d == null)
                    continue;
                String name = d.getElementName();

                // Regular handlers
                Set<CotDetailHandler> handlers = _handlerMap.get(name);
                if (handlers != null) {
                    Set<CotDetailHandler> copy = null;
                    for (CotDetailHandler h : handlers) {
                        if (h.isSupported(item, event, d)) {
                            if (copy == null)
                                copy = new HashSet<>(handlers.size());
                            copy.add(h);
                        }
                    }
                    handlers = copy;
                }

                // Marker handlers
                Set<MarkerDetailHandler> markerHandlers = null;
                if (marker != null) {
                    markerHandlers = _markerHandlerMap.get(name);
                    if (markerHandlers != null)
                        markerHandlers = new HashSet<>(markerHandlers);
                }

                sets.add(new ProcessSet(d, handlers, markerHandlers));
            }
        }

        // Now process the sets
        for (ProcessSet ps : sets) {
            // Check if this detail has any handlers
            if (ps.handlers == null
                    && ps.markerHandlers == null
                    && !_kernelDetailManager.hasHandler(ps.detail.getElementName())) {

                // If not then it might be unhandled
                // Stick it in the opaque details
                //Log.d(TAG, "Unhandled detail: " + d.getElementName());
                OpaqueHandler.getInstance().toMarkerMetadata(item, event,
                        ps.detail);
            }

            if (ps.handlers != null) {
                for (CotDetailHandler h : ps.handlers) {
                    try {
                        ImportResult r = h.toItemMetadata(item, event,
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
            if (marker != null && ps.markerHandlers != null) {
                for (MarkerDetailHandler h : ps.markerHandlers)
                    h.toMarkerMetadata(marker, event, ps.detail);
            }
        }
        return res;
    }

    private ImportResult processKernelDetails(MapItem item, CotEvent event) {
        AttributeSet attrs = MarshalManager.marshal(
                item, MetaDataHolder2.class, AttributeSet.class);
        gov.tak.api.cot.event.CotEvent govEvent = MarshalManager.marshal(
                event, CotEvent.class, gov.tak.api.cot.event.CotEvent.class);
        ICotDetailHandler.ImportResult kernelImportRes = _kernelDetailManager
                .processDetails(item, attrs, govEvent);
        return adaptImportResult(kernelImportRes);
    }

    private static class ProcessSet {

        private final CotDetail detail;
        private final Set<CotDetailHandler> handlers;
        private final Set<MarkerDetailHandler> markerHandlers;

        ProcessSet(CotDetail detail, Set<CotDetailHandler> handlers,
                Set<MarkerDetailHandler> markerHandlers) {
            this.detail = detail;
            this.handlers = handlers;
            this.markerHandlers = markerHandlers;
        }
    }

    private void registerDefaultHandlers() {
        // TODO: Can we consolidate some of these together?
        // i.e. ShapeDetailHandler and CircleDetailHandler,
        // ServicesDetailHandler and RequestDetailHandler, all the team SA
        // details, etc.
        registerHandler(new ContactDetailHandler());
        registerHandler(new PrecisionLocationHandler());
        registerHandler(new TakVersionDetailHandler());
        registerHandler(new LinkDetailHandler(_mapView));
        registerHandler(new AddressDetailHandler());
        registerHandler(new ShapeDetailHandler(_mapView));
        registerHandler(new ImageDetailHandler(_mapView));
        registerHandler(new RequestDetailHandler());
        registerHandler(new ServicesDetailHandler());
        registerHandler(new GeoFenceDetailHandler());
        registerHandler(new TrackDetailHandler());
        registerHandler(new RemarksDetailHandler());
        registerHandler(new ArchiveDetailHandler());
        registerHandler(new CreatorDetailHandler());
        registerHandler(new StatusDetailHandler());
        registerHandler(SPIDetailHandler.getInstance());
        registerHandler(new TadilJHandler());
        registerHandler(new UserIconHandler());
        registerHandler(new ColorDetailHandler());
        registerHandler(new MetaDetailHandler());
        registerHandler(new HeightDetailHandler());
        registerHandler(new CEDetailHandler());
        registerHandler(new CircleDetailHandler(_mapView));
        registerHandler(new TracksDetailHandler());
        registerHandler(new SensorDetailHandler());
        registerHandler(new EmergencyDetailHandler());
        registerHandler(new ForceXDetailHandler(_mapView));
        registerHandler(new LabelDetailHandler());
        registerHandler(new StrokeFillDetailHandler());
        registerHandler(new MarkerIncludedRouteDetailHandler());
        registerHandler(new ExtrudeModeDetailHandler());

        // This uses special "injection" logic, which seems unnecessary...
        // XXX - I'm not going to mess with it right now
        registerHandler("uid", new UIDHandler());
    }

    private ImportResult adaptImportResult(
            gov.tak.api.cot.detail.ICotDetailHandler.ImportResult importResult) {
        switch (importResult) {
            case FAILURE:
                return ImportResult.FAILURE;
            case DEFERRED:
                return ImportResult.DEFERRED;
            case IGNORE:
                return ImportResult.IGNORE;
            default:
                return ImportResult.SUCCESS;
        }
    }
}
