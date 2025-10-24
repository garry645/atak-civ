
package com.atakmap.android.maps.graphics;

import android.util.Pair;

import com.atakmap.android.maps.MapItem;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.ClassResultFilter;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.control.ClampToGroundControl;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.control.FeatureEditControl;
import com.atakmap.map.layer.feature.opengl.GLBatchGeometryFeatureDataStoreRenderer;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.ConfigOptions;
import com.atakmap.util.Visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GLMapItemFeatures implements GLMapRenderable2, HitTestControl {
    MapRenderer renderer = null;
    GLLayer3 batch = null;
    MapItemsFeatureDataStore store = null;
    FeatureLayer3 layer;
    GLLabelManager labels;
    GLQuadtreeNode2 animations;
    /** label ID -> MapItem */
    Map<Integer, MapItem> labelIds = new ConcurrentHashMap<>();
    HitTestControl ctrl;
    FeatureEditControl editor;
    ClampToGroundControl clampToGroundControl;

    GLMapItemFeatures(MapRenderer renderer) {
        this.renderer = renderer;
        labels = ((GLMapView) renderer).getLabelManager();
        store = new MapItemsFeatureDataStore(this);
        layer = new FeatureLayer3("Map Items", store);
        animations = new GLQuadtreeNode2();

        synchronized (GLBatchGeometryFeatureDataStoreRenderer.SPI) {
            // XXX - better preservation of legacy UX. feature renderer constrain
            //       icon size for consistency with Google Earth via global
            //       setting. API should be updated to allow per-instance
            ConfigOptions.setOption("overlays.icon-dimension-constraint", 256);
            batch = (GLLayer3) GLBatchGeometryFeatureDataStoreRenderer.SPI
                    .create(Pair.<MapRenderer, Layer> create(renderer, layer));
            ConfigOptions.setOption("overlays.icon-dimension-constraint", null);
        }
        batch.start();

        // obtain controls
        renderer.visitControl(layer, new Visitor<HitTestControl>() {
            @Override
            public void visit(HitTestControl object) {
                ctrl = object;
            }
        }, HitTestControl.class);
        renderer.visitControl(layer, new Visitor<FeatureEditControl>() {
            @Override
            public void visit(FeatureEditControl object) {
                editor = object;
            }
        }, FeatureEditControl.class);

        clampToGroundControl = ((GLMapView) renderer)
                .getControl(ClampToGroundControl.class);
        // XXX - ew. unwind once `GLMapView` owns control as opposed to convoluted injection
        if (clampToGroundControl == null) {
            renderer.addOnControlsChangedListener(
                    new MapRenderer.OnControlsChangedListener() {
                        @Override
                        public void onControlRegistered(Layer2 layer,
                                MapControl ctrl) {
                            if (ctrl instanceof ClampToGroundControl) {
                                clampToGroundControl = (ClampToGroundControl) ctrl;
                                final MapRenderer.OnControlsChangedListener self = this;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        renderer.removeOnControlsChangedListener(
                                                self);
                                    }
                                }, "clampgroundcontrolschange").start();
                            }
                        }

                        @Override
                        public void onControlUnregistered(Layer2 layer,
                                MapControl ctrl) {
                        }
                    });
        }
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        animations.draw(view, renderPass);
        batch.draw(view, renderPass);
    }

    @Override
    public void release() {
        animations.release();
        batch.release();
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_SPRITES | GLMapView.RENDER_PASS_SURFACE;
    }

    @Override
    public void hitTest(final MapRenderer3 renderer,
            final HitTestQueryParameters params,
            final Collection<HitTestResult> results) {
        if (ctrl == null)
            return;
        Set<Long> hitItems = new HashSet<>();
        // querying only on feature IDs
        HitTestQueryParameters fparams = new HitTestQueryParameters(params);
        fparams.resultFilter = new ClassResultFilter(Long.class);
        // feature results
        Collection<HitTestResult> fresults = new ArrayList<>();
        ctrl.hitTest(renderer, fparams, fresults);
        for (HitTestResult fresult : fresults) {
            MapItem item = store.getItem((Long) fresult.subject);
            if (item == null)
                continue;
            if (!item.getClickable())
                continue;
            if (hitItems.contains(item.getSerialId()))
                continue;
            GLMapItemFeature feature = store.getFeature(item);
            HitTestResult result = new HitTestResult(item, fresult);
            if (feature != null)
                result = feature.postProcessHitTestResult(renderer, params,
                        result);
            results.add(result);
            hitItems.add(item.getSerialId());
        }

        // labels only hit-test
        Collection<Integer> labelids = new HashSet<>();
        labels.hitTest(params.point.x, params.point.y, params.size,
                params.limit, labelids);
        for (Integer labelid : labelids) {
            MapItem item = labelIds.get(labelid);
            if (item == null)
                continue;
            if (!item.getClickable())
                continue;
            if (hitItems.contains(item.getSerialId()))
                continue;
            GLMapItemFeature feature = store.getFeature(item);
            HitTestResult result = new HitTestResult(item, params.geo);
            if (feature != null)
                result = feature.postProcessHitTestResult(renderer, params,
                        result);
            results.add(result);
            hitItems.add(item.getSerialId());
        }
    }

    public boolean addItem(MapItem item) {
        return store.insert(item);
    }

    public void removeItem(MapItem item) {
        store.delete(item);
    }
}
