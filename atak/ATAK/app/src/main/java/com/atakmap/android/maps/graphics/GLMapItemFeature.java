
package com.atakmap.android.maps.graphics;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.MathUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract class GLMapItemFeature implements MapItem.OnVisibleChangedListener,
        MapItem.OnMetadataChangedListener {
    /** the bounds should be considered dirty if bitwise AND with the properties is non-zero */
    final static int BOUNDS_DIRTY_PROPS_MASK = FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY
            |
            FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
            FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE;

    final GLMapItemFeatures _features;
    final long[] _fids;
    final Feature[] _feature;
    final boolean[] _editFeature;
    MapItem _subject;
    Callback _callback;
    final AtomicBoolean _visible = new AtomicBoolean(false);
    final Envelope _bounds = new Envelope(Double.NaN, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN, Double.NaN);
    AtomicBoolean _boundsDirty = new AtomicBoolean(false);
    // NOTE: `FeatureDataStore2.PROPERTY_FEATURE_NAME` is used to indicate a dirty label
    AtomicInteger _featureDirty = new AtomicInteger(0);

    AtomicBoolean _editing = new AtomicBoolean(false);

    GLMapItemFeature(GLMapItemFeatures features, int maxFeatures) {
        _features = features;
        _feature = new Feature[maxFeatures];
        _fids = new long[maxFeatures];
        Arrays.fill(_fids, FeatureDataStore2.FEATURE_ID_NONE);
        _editFeature = new boolean[maxFeatures];
    }

    /**
     * Starts observing the specified {@link MapItem}. Changes to the state of the observed
     * {@link MapItem} shall result in appropriate method invocation on the supplied
     * {@link Callback} object.
     *
     * <P>The {@code GLMapItemFeature} shall treat the lifetime of the {@link MapItem} and
     * {@link Callback} as strictly bound to the entry of {@link #startObserving(MapItem, Callback)}
     * and {@link #stopObserving()}. Access to the supplied {@link MapItem} and {@link Callback}
     * should never be made after {@link #stopObserving()} has returned.
     *
     * @param item
     * @param callback
     */
    final synchronized void startObserving(@NonNull MapItem item,
            @NonNull Callback callback) {
        _subject = item;
        _callback = callback;
        for (int i = 0; i < _fids.length; i++)
            _fids[i] = _callback.reserveFeatureId(item);

        startObservingImpl();
        _subject.addOnVisibleChangedListener(this);
        _subject.addOnMetadataChangedListener("editing", this);
        _visible.set(_subject.getVisible());

        _boundsDirty.set(true);
        _featureDirty.set(
                FeatureDataStore2.PROPERTY_FEATURE_NAME |
                        FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY |
                        FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);

        _editing.set(false);

        // if subject is editing, start editing
        if (item.getEditing())
            onMetadataChanged(_subject, "editing");
    }

    abstract void startObservingImpl();

    /**
     * This method will NEVER be called concurrently with {@link #startObserving(MapItem, Callback)}
     */
    final synchronized void stopObserving() {
        stopObservingImpl();
        _subject.removeOnVisibleChangedListener(this);
        _subject.removeOnMetadataChangedListener("editing", this);
        // item was marked as editing on removal, effect stop editing
        if (_editing.compareAndSet(true, false)) {
            if (_features.editor != null) {
                for (long fid : _fids) {
                    if (fid == FeatureDataStore2.FEATURE_ID_NONE)
                        continue;
                    _features.editor.stopEditing(fid);
                }
            }
        }

        for (int i = 0; i < _fids.length; i++) {
            _callback.unreserveFeatureId(_subject, _fids[i]);
            _fids[i] = FeatureDataStore2.FEATURE_ID_NONE;
            _editFeature[i] = false;
        }

        _boundsDirty.set(false);
        _featureDirty.set(0);
    }

    abstract void stopObservingImpl();

    HitTestResult postProcessHitTestResult(MapRenderer3 renderer,
            HitTestQueryParameters params, HitTestResult result) {
        return result;
    }

    final synchronized Feature[] validateFeature() {
        final int dirty = _featureDirty.getAndSet(0);
        if (dirty == 0)
            return _feature;
        if (_subject == null)
            return _feature;

        validateFeatureImpl(dirty, _feature);

        for (int i = 0; i < _feature.length; i++) {
            if (_feature[i] == null)
                continue;
            if (_feature[i].getId() != _fids[i]) {
                _feature[i] = new Feature(
                        1L,
                        _fids[i],
                        _feature[i].getName(),
                        _feature[i].getGeometry(),
                        _feature[i].getStyle(),
                        _feature[i].getAttributes(),
                        _feature[i].getAltitudeMode(),
                        _feature[i].getExtrude(),
                        _feature[i].getTimestamp(),
                        1L);
            }
        }

        if (MathUtils.hasBits(dirty, FeatureDataStore2.PROPERTY_FEATURE_NAME))
            validateLabel();
        return _feature;
    }

    abstract void validateFeatureImpl(int propertiesMask, Feature[] features);

    abstract void validateLabel();

    final boolean isVisible() {
        return _visible.get();
    }

    final Envelope getBounds() {
        if (_boundsDirty.getAndSet(false)) {
            refreshBounds();
        }
        return _bounds;
    }

    void refreshBounds() {
        if (_subject instanceof PointMapItem) {
            final GeoPoint point = ((PointMapItem) _subject).getPoint();
            _bounds.minX = point.getLongitude();
            _bounds.minY = point.getLatitude();
            _bounds.minZ = point.getAltitude();
            _bounds.maxX = _bounds.minX;
            _bounds.maxY = _bounds.minY;
            _bounds.maxZ = _bounds.minZ;
        } else if (_subject instanceof Shape) {
            final GeoBounds point = ((Shape) _subject).getBounds(null);
            _bounds.minX = point.getWest();
            _bounds.minY = point.getSouth();
            _bounds.minZ = point.getMinAltitude();
            _bounds.maxX = point.getEast();
            _bounds.maxY = point.getNorth();
            _bounds.maxZ = point.getMaxAltitude();
        }
    }

    final void markDirty(int properties) {
        if (properties == FeatureDataStore2.PROPERTY_FEATURE_NAME) {
            // only the label has changed, validate immediately
            validateLabel();
        } else {
            do {
                final int dirtyProps = _featureDirty.get();
                // check if the feature is already marked dirty for the specified properties
                if (MathUtils.hasBits(dirtyProps, properties)) {
                    // mark the bounds dirty if needed. If the bounds were not dirty and are marked,
                    // notify the callback
                    if ((properties & BOUNDS_DIRTY_PROPS_MASK) != 0 &&
                            _boundsDirty.compareAndSet(false, true)) {
                        _callback.onItemChanged(_subject);
                    }
                    break;
                }
                // update the feature dirty flags and notify the callback
                if (_featureDirty.compareAndSet(dirtyProps,
                        dirtyProps | properties)) {
                    // mark the bounds as dirty based on the incoming property mask
                    _boundsDirty.compareAndSet(
                            false,
                            (properties & BOUNDS_DIRTY_PROPS_MASK) != 0);
                    _callback.onItemChanged(_subject);
                    break;
                }
            } while (true);

            if (_editing.get() && _features.editor != null) {
                Feature[] feature = new Feature[_feature.length];
                validateFeatureImpl(-1, feature);
                for (int i = 0; i < feature.length; i++) {
                    final Feature f = feature[i];
                    if (f == null) {
                        if (_editFeature[i]) {
                            _features.editor.stopEditing(_fids[i]);
                            _editFeature[i] = false;
                        }
                        continue;
                    }
                    if (_editFeature[i])
                        _features.editor.updateFeature(f.getId(),
                                properties,
                                f.getName(),
                                f.getGeometry(),
                                f.getStyle(),
                                f.getAltitudeMode(),
                                f.getExtrude());
                    else
                        _editFeature[i] = _features.editor
                                .startEditing(f.getId());
                }
            }
        }
    }

    @Override
    public void onVisibleChanged(MapItem item) {
        final boolean visible = _subject.getVisible();
        if (_visible.compareAndSet(!visible, visible))
            _callback.onItemChanged(_subject);
    }

    @Override
    public void onMetadataChanged(MapItem item, String field) {
        if (field.equals("editing")) {
            final boolean editing = item.getEditing();
            if (_editing.compareAndSet(!editing, editing)
                    && _features.editor != null) {
                for (int i = 0; i < _fids.length; i++) {
                    final long fid = _fids[i];
                    if (fid == FeatureDataStore2.FEATURE_ID_NONE)
                        continue;
                    if (editing) {
                        _editFeature[i] = _features.editor.startEditing(fid);
                    } else {
                        _features.editor.stopEditing(fid);
                        _editFeature[i] = false;
                    }
                }
            }
        }
    }

    interface Callback {
        void onItemChanged(MapItem item);

        long reserveFeatureId(MapItem item);

        void unreserveFeatureId(MapItem item, long fid);
    }
}
