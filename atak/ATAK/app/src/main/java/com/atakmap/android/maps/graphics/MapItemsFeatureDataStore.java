
package com.atakmap.android.maps.graphics;

import android.util.Pair;

import com.atakmap.android.maps.MapItem;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDefinition3;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.datastore.AbstractReadOnlyFeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class MapItemsFeatureDataStore extends AbstractReadOnlyFeatureDataStore2
        implements GLMapItemFeature.Callback {
    private static final int _initialDictionaryCapacity = 100003;

    private AtomicLong _currentFid = new AtomicLong(1);
    private boolean _disposed = false;
    /**
     * Atomic bitwise flag used to both signal and monitor the state of content
     * changed dispatch and servicing.
     * <UL>
     *     <LI>Low order two bits indicate if a content-changed dispatch is requested
     *     <LI>High order 30 bits indicate the number of executing queries
     * </UL>
     *
     * <P>Note: atomics are intetionally used here to eliminate monitor
     * overhead. Tracking of multiple states allows for minimal event dispatch
     * (and overhead) for situations where such dispatch would be redundant
     * (e.g. issuing multiple _content-changed_ callbacks while a qury is
     * processing).
     */
    private AtomicInteger _contentChanged = new AtomicInteger(0);
    private final Object _contentChangedLock = new Object();
    private ConcurrentHashMap<MapItem, Record> _content = new ConcurrentHashMap<>(
            _initialDictionaryCapacity);
    private ConcurrentHashMap<Long, MapItem> _fids = new ConcurrentHashMap<>(
            _initialDictionaryCapacity);
    private final GLMapItemFeatures _features;

    private volatile Pair<Integer, ArrayList<Record>> cachedRecords;

    private final ConcurrentLinkedQueue<Record> markedForDelete = new ConcurrentLinkedQueue<>();

    MapItemsFeatureDataStore(GLMapItemFeatures features) {
        super(0, 0);
        _features = features;
        cachedRecords = new Pair<>(_content.hashCode(),
                new ArrayList<>(_content.values()));

        Thread t = new Thread(new ContentChangedDispatcher(),
                "MapItemsFeatureDSChanged");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    boolean insert(MapItem item) {
        GLMapItemFeature feature = GLMapItemFeatureFactory.create(_features,
                item);
        if (feature == null)
            return false;
        // NOTE: the logic here is micro-optimized per extensive profiling.
        // Insert of the record is done atomically to prevent potential race
        // without the need for monitor synchronization. The overhead
        // associated with `GLMapItemFeatureFactory.create` is low
        // (implementation for SPIs is an `instanceof` check)

        // insert a new `GLMapItemFeature` record for the item if none exists.
        // If the item is marked as _editing_, start observing immediately;
        // else defer observation until the first query.
        final boolean editing = item.getEditing();
        if (_content.putIfAbsent(item,
                new Record(item, feature, editing)) == null) {
            if (editing)
                feature.startObserving(item, this);
            onItemChanged(item);
            return true;
        } else {
            return false;
        }
    }

    void delete(MapItem item) {
        Record feature = _content.remove(item);
        if (feature != null) {
            markedForDelete.add(feature);
            onItemChanged(item);
        }
    }

    MapItem getItem(long fid) {
        return _fids.get(fid);
    }

    GLMapItemFeature getFeature(MapItem item) {
        final Record r = _content.get(item);
        return (r != null) ? r.glfeature : null;
    }

    @Override
    public FeatureCursor queryFeatures(FeatureQueryParameters params) {
        // new query is executing, content will be up-to-date; reset the
        // bits signaling a content change dispatch is requested
        do {
            final int bs = _contentChanged.get();
            final int numQueries = (bs >> 2) & 0x3FFFFFFF;
            if (_contentChanged.compareAndSet(bs, ((numQueries + 1) << 2)))
                break;
        } while (true);

        if (!markedForDelete.isEmpty()) {
            Iterator<Record> iterator = markedForDelete.iterator();
            while (iterator.hasNext()) {
                final Record feature = iterator.next();
                if (feature.observing.getAndSet(false))
                    feature.glfeature.stopObserving();
                iterator.remove();
            }
        }

        ArrayList<Record> records;
        if (params.ids != null) {
            records = new ArrayList<>(params.ids.size());
            for (Long fid : params.ids) {
                final MapItem item = _fids.get(fid);
                if (item == null)
                    continue;
                final Record record = _content.get(item);
                if (record == null)
                    continue;
                records.add(record);
            }
        } else {
            final int v = _content.hashCode();
            if (v == cachedRecords.first) {
                records = cachedRecords.second;
            } else {
                records = new ArrayList<>(_content.values());
                cachedRecords = new Pair<>(v, records);
            }
        }
        return new FeatureCursorImpl(records,
                params.spatialFilter != null
                        ? params.spatialFilter.getEnvelope()
                        : null,
                params.ids != null
                        ? params.ids
                        : null);
    }

    @Override
    public FeatureSetCursor queryFeatureSets(FeatureSetQueryParameters params) {
        return FeatureSetCursor.EMPTY;
    }

    @Override
    public boolean hasTimeReference() {
        return false;
    }

    @Override
    public long getMinimumTimestamp() {
        return TIMESTAMP_NONE;
    }

    @Override
    public long getMaximumTimestamp() {
        return TIMESTAMP_NONE;
    }

    @Override
    public String getUri() {
        return "mapitems";
    }

    @Override
    public boolean hasCache() {
        return false;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public long getCacheSize() {
        return 0;
    }

    @Override
    public void dispose() {
        synchronized (_contentChangedLock) {
            _disposed = true;
            _contentChangedLock.notify();
        }
    }

    @Override
    public void onItemChanged(MapItem item) {
        // mark and notify content changed if unset
        do {
            final int bs = _contentChanged.get();
            //  nothing to do if any of the content changed dispatch request
            //  bits are toggled
            if ((bs & 0x3) != 0)
                break;
            // toggle the low-order content change dispatch request bit and
            // notify
            if (_contentChanged.compareAndSet(bs, bs | 0x1)) {
                synchronized (_contentChangedLock) {
                    _contentChangedLock.notify();
                }
                break;
            }
        } while (true);
    }

    @Override
    public long reserveFeatureId(MapItem item) {
        final long fid = _currentFid.getAndIncrement();
        _fids.put(fid, item);
        return fid;
    }

    @Override
    public void unreserveFeatureId(MapItem item, long fid) {
        _fids.remove(fid);
    }

    class ContentChangedDispatcher implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (_contentChangedLock) {
                    if (_disposed)
                        break;

                    // evaluate the current state of _content-changed_ dispatch
                    // request and wait/dispatch as appropriate.
                    do {
                        final int bs = _contentChanged.get();
                        // if not dirty, wait indefinite
                        if (_contentChanged.compareAndSet(0, 0)) {
                            try {
                                _contentChangedLock.wait();
                            } catch (InterruptedException ignored) {
                            }
                            continue;
                        }
                        // if dirty and not _content-changed_ is not yet
                        // dispatched, dispatch. flip the low-order two bits to
                        // signal that the dispatch occurred but content is
                        // still dirty.
                        if ((bs & 0x3) == 0x1) {
                            if (_contentChanged.compareAndSet(bs, bs ^ 0x3))
                                break;
                            else
                                continue;
                        }
                        // if dirty+dispatched but we not in query, wait and
                        // dispatch after 500ms. this handles the case where
                        // the renderer may have dropped a _content-changed_
                        // event during processing and the query that will
                        // reset the dirty state is never invoked.
                        if (_contentChanged.compareAndSet(0x2, 0x2)) {
                            try {
                                _contentChangedLock.wait(500);
                            } catch (InterruptedException ignored) {
                            }
                            // toggle back to dirty but not dispatched if the
                            // state has not changed. this will trigger a new dispatch
                            _contentChanged.compareAndSet(bs, bs ^ 0x3);
                            continue;
                        }
                        // if dirty+dispatched and in query, wait indefinite.
                        // we will get notified when all executing queries have
                        // completed if any dirty bits are set.
                        final int numQueries = (bs >> 2) & 0x3FFFFFFF;
                        if (numQueries > 0 && MathUtils.hasBits(bs, 0x2)) {
                            if (_contentChanged.compareAndSet(bs, bs)) {
                                try {
                                    _contentChangedLock.wait(500);
                                } catch (InterruptedException ignored) {
                                }
                            }
                            continue;
                        }

                        // this should represent an illegal state, however, for
                        // the sake of robustness treat this case as resetting
                        // to an original content change dispatch request
                        if ((bs & 0x3) == 0x3 &&
                                _contentChanged.compareAndSet(bs,
                                        bs & 0xFFFFFFFC)) {

                            break;
                        }
                    } while (true);
                }

                dispatchContentChanged();
            }
        }
    }

    final static class Record {
        final MapItem item;
        final GLMapItemFeature glfeature;
        final AtomicBoolean observing;

        Record(MapItem item, GLMapItemFeature glfeature, boolean observing) {
            this.item = item;
            this.glfeature = glfeature;
            this.observing = new AtomicBoolean(observing);
        }
    }

    class FeatureCursorImpl implements FeatureCursor, FeatureDefinition3 {
        final List<Record> _records;
        final Envelope _spatialFilter;
        final Set<Long> _idFilter;

        int _ridx = 0;
        int _fidx = 0;
        Feature[] _recordFeatures = new Feature[0];
        Feature _row = null;
        boolean _closed = false;

        FeatureCursorImpl(List<Record> records, Envelope spatialFilter,
                Set<Long> idFilter) {
            _records = records;
            _spatialFilter = spatialFilter;
            _idFilter = idFilter;
        }

        @Override
        public boolean moveToNext() {
            do {
                if (_closed)
                    return false;
                if (_fidx < _recordFeatures.length) {
                    _row = _recordFeatures[_fidx++];
                    if (_row == null)
                        continue;
                    if (_idFilter != null && !_idFilter.contains(_row.getId()))
                        continue;
                    return true;
                }
                if (_ridx == _records.size())
                    return false;
                final Record record = _records.get(_ridx++);
                if (record.observing.compareAndSet(false, true))
                    record.glfeature.startObserving(record.item,
                            MapItemsFeatureDataStore.this);
                if (!record.glfeature.isVisible())
                    continue;
                final Envelope rmbb = record.glfeature.getBounds();
                if (_spatialFilter != null &&
                        !Rectangle.intersects(
                                _spatialFilter.minX, _spatialFilter.minY,
                                _spatialFilter.maxX, _spatialFilter.maxY,
                                rmbb.minX, rmbb.minY,
                                rmbb.maxX, rmbb.maxY)) {

                    continue;
                }
                _recordFeatures = record.glfeature.validateFeature();
                _fidx = 0;
                _row = null;
            } while (true);
        }

        @Override
        public void close() {
            _closed = true;
            // if all active queries are complete
            do {
                final int bs = _contentChanged.get();
                final int numQueries = (bs >> 2) & 0x3FFFFFFF;
                if (numQueries == 0) {
                    // something went wrong with managing number of queries,
                    // set to dirty and notify to resume processing
                    _contentChanged.compareAndSet(bs, 1);
                    break;
                }
                // update the mask for the new query count while retaining the
                // current dirty state and notify
                if (_contentChanged.compareAndSet(bs,
                        ((numQueries - 1) << 2) | (bs & 0x3))) {
                    // NOTE: no notification is made here due to potential
                    // deadlock due to out-of-order lock acquisition
                    break;
                }
            } while (true);
        }

        @Override
        public boolean isClosed() {
            return _closed;
        }

        @Override
        public Object getRawGeometry() {
            return _row.getGeometry();
        }

        @Override
        public int getGeomCoding() {
            return FeatureDefinition3.GEOM_ATAK_GEOMETRY;
        }

        @Override
        public String getName() {
            return _row.getName();
        }

        @Override
        public int getStyleCoding() {
            return FeatureDefinition3.STYLE_ATAK_STYLE;
        }

        @Override
        public Object getRawStyle() {
            return _row.getStyle();
        }

        @Override
        public AttributeSet getAttributes() {
            return _row.getAttributes();
        }

        @Override
        public Feature get() {
            return _row;
        }

        @Override
        public long getId() {
            return _row.getId();
        }

        @Override
        public long getVersion() {
            return _row.getVersion();
        }

        @Override
        public long getFsid() {
            return _row.getFeatureSetId();
        }

        @Override
        public long getTimestamp() {
            return _row.getTimestamp();
        }

        @Override
        public Feature.AltitudeMode getAltitudeMode() {
            return _row.getAltitudeMode();
        }

        @Override
        public double getExtrude() {
            return _row.getExtrude();
        }
    }
}
