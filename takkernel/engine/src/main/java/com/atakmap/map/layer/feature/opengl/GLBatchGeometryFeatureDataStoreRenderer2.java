package com.atakmap.map.layer.feature.opengl;

import android.util.Pair;

import com.atakmap.interop.Interop;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.control.FeatureEditControl2;
import com.atakmap.map.layer.feature.control.FeaturesLabelControl;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.ReadWriteLock;

import java.util.Collection;
import java.util.LinkedHashSet;

import gov.tak.api.engine.map.RenderContext;

// XXX - will replace impl for `GLBatchGeometryFeatureDataStore`
final class GLBatchGeometryFeatureDataStoreRenderer2 implements GLLayer3, Layer.OnLayerVisibleChangedListener
{
    final static Interop<FeatureDataStore4> FeatureDataStore_interop = Interop.findInterop(FeatureDataStore4.class);
    final static Interop<RenderContext> RenderContext_interop = Interop.findInterop(RenderContext.class);
    final static Interop<Geometry> Geometry_interop = Interop.findInterop(Geometry.class);
    final static Interop<Style> Style_interop = Interop.findInterop(Style.class);
    final static Interop<FeatureQueryParameters> FeatureQueryParameters_interop = Interop.findInterop(FeatureQueryParameters.class);

    final static NativePeerManager.Cleaner CLEANER = new NativePeerManager.Cleaner() {
        @Override
        protected void run(Pointer pointer, Object opaque) {
            destruct(pointer);
            FeatureDataStore2 dataStore = (FeatureDataStore2) opaque;
            if (dataStore != null) dataStore.dispose();
        }
    };

    private FeaturesLabelControl featuresLabelControl;

    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            return 4;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> object) {
            FeatureDataStore4 datastore = null;
            if (object.second instanceof FeatureLayer3) {
                FeatureDataStore2 datastore2 = ((FeatureLayer3) object.second).getDataStore();
                if (datastore2 instanceof FeatureDataStore4)
                {
                    datastore = (FeatureDataStore4) datastore2;
                } else {
                    datastore = Adapters.adapt(datastore2);
                }
            }
            if(datastore == null)
                return null;
            FeatureLayer3.Options options = ((FeatureLayer3) object.second).getOptions();
            long dspointer = 0L;
            boolean wrappedDataStore = false;
            do {
                dspointer = FeatureDataStore_interop.getPointer(datastore);
                if(dspointer != 0)
                    break;
                // XXX - do not wrap non-native datastores pending performance improvements
                datastore = FeatureDataStore_interop.create(FeatureDataStore_interop.wrap(datastore, false));
                dspointer = FeatureDataStore_interop.getPointer(datastore);
                wrappedDataStore = true;
            } while(false);
            if(dspointer == 0L)
                return null;
            synchronized(this) {
                final Pointer glpointer = GLBatchGeometryFeatureDataStoreRenderer2.create(RenderContext_interop.getPointer(LegacyAdapters.getRenderContext(object.first)), dspointer, options);
                return new GLBatchGeometryFeatureDataStoreRenderer2(object.first, object.second, glpointer, datastore, wrappedDataStore);
            }
        }
    };

    final MapRenderer renderer;
    final Layer subject;
    final Pointer pointer;
    final ReadWriteLock rwlock = new ReadWriteLock();

    final FeatureDataStore2 datastore;

    boolean visible;

    final HitTestControl hitTestControl;
    final FeatureEditControl2 editControl;

    GLBatchGeometryFeatureDataStoreRenderer2(MapRenderer renderer, Layer subject, Pointer pointer, FeatureDataStore2 datastore, boolean disposeDatastore)
    {
        NativePeerManager.register(this, pointer, rwlock, disposeDatastore ? datastore : null, CLEANER);

        this.renderer = renderer;
        this.subject = subject;
        this.pointer = pointer;
        this.datastore = datastore;

        hitTestControl = new HitTestControlImpl();
        editControl = new FeatureEditControl2Impl();
        featuresLabelControl = new NativeFeaturesLabelControl(getFeaturesLabelControl(pointer.raw));
    }

    @Override
    public Layer getSubject() {
        return subject;
    }

    @Override
    public void start() {
        subject.addOnLayerVisibleChangedListener(this);
        onLayerVisibleChanged(subject);
        rwlock.acquireRead();
        try {
            if(pointer.raw != 0)
                start(pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
        renderer.registerControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), hitTestControl);
        renderer.registerControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), editControl);
        renderer.registerControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), featuresLabelControl);
    }

    @Override
    public void stop() {
        subject.removeOnLayerVisibleChangedListener(this);
        rwlock.acquireRead();
        try {
            if(pointer.raw != 0)
                stop(pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
        renderer.unregisterControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), hitTestControl);
        renderer.unregisterControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), editControl);
        renderer.unregisterControl(com.atakmap.map.layer.LegacyAdapters.adapt(subject), featuresLabelControl);

        this.featuresLabelControl = null;
    }

    @Override
    public void draw(GLMapView view) {
        draw(view, -1);
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if(visible) {
            rwlock.acquireRead();
            try {
                if(pointer.raw != 0)
                    draw(pointer.raw, view, renderPass);
            } finally {
                rwlock.releaseRead();
            }
        }
    }

    @Override
    public void release() {
        rwlock.acquireRead();
        try {
            if(pointer.raw != 0)
                release(pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public int getRenderPass() {
        rwlock.acquireRead();
        try {
            return (pointer.raw != 0) ? getRenderPass(pointer.raw) : 0;
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void onLayerVisibleChanged(Layer layer) {
        visible = layer.isVisible();
    }

    final class HitTestControlImpl implements HitTestControl {

        @Override
        public void hitTest(MapRenderer3 renderer, HitTestQueryParameters params, Collection<HitTestResult> results) {
            if(params.resultFilter != null && !params.resultFilter.acceptClass(Long.class))
                return;
            Collection<Long> fids = new LinkedHashSet<>();
            rwlock.acquireRead();
            try {
                if(pointer.raw == 0)
                    return;
                MapSceneModel sm = renderer.getMapSceneModel(true, renderer.getDisplayOrigin());
                GLBatchGeometryFeatureDataStoreRenderer2.hitTest(pointer.raw, fids, params.point.x, sm.height-params.point.y, params.geo.getLatitude(), params.geo.getLongitude(), sm.gsd, params.size, params.limit);
                for(Long fid : fids)
                    results.add(new HitTestResult(fid));
            } finally {
                rwlock.releaseRead();
            }
        }
    }

    private final class FeatureEditControl2Impl implements FeatureEditControl2
    {

        @Override
        public boolean startEditing(long fid) {
            rwlock.acquireRead();
            try {
                return (pointer.raw != 0) ?
                        GLBatchGeometryFeatureDataStoreRenderer2.startEditing(pointer.raw, fid) : false;
            } finally {
                rwlock.releaseRead();
            }
        }

        @Override
        public void stopEditing(long fid) {
            rwlock.acquireRead();
            try {
                if (pointer.raw != 0)
                    GLBatchGeometryFeatureDataStoreRenderer2.stopEditing(pointer.raw, fid);
            } finally {
                rwlock.releaseRead();
            }
        }

        @Override
        public void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, Feature.AltitudeMode altitudeMode, double extrude) {
            Feature.Traits traits = new Feature.Traits();
            traits.altitudeMode = altitudeMode;
            traits.extrude = extrude;
            updateFeature(fid, updatePropertyMask, name, geometry, style, traits);
        }

        @Override
        public void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, Feature.Traits traits) {
            rwlock.acquireRead();
            try {
                if (pointer.raw != 0)
                    GLBatchGeometryFeatureDataStoreRenderer2.updateFeature(
                            pointer.raw,
                            fid,
                            updatePropertyMask,
                            name,
                            Geometry_interop.getPointer(geometry),
                            Style_interop.getPointer(style),
                            traits);
            } finally {
                rwlock.releaseRead();
            }
        }
    }

    static native Pointer create(long renderContext, long featureDataStore, FeatureLayer3.Options options);
    static native void destruct(Pointer pointer);
    static native void draw(long pointer, GLMapView view, int renderPass);
    static native void release(long pointer);
    static native int getRenderPass(long pointer);
    static native void start(long pointer);
    static native void stop(long pointer);

    static native void hitTest(long pointer, Collection<Long> fids, float screenX, float screenY, double lat, double lng, double gsd, float radius, int limit);

    static native boolean startEditing(long pointer, long fid);
    static native void stopEditing(long pointer, long fid);
    static native void updateFeature(long pointer, long fid, int updatePropertyMask, String name, long geometryptr, long styleptr, Feature.Traits traits);

    static native long getFeaturesLabelControl(long pointer);
}
