
package com.atakmap.android.drawing.milsym;

import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.ClassResultFilter;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.Visitor;

import java.util.Collection;
import java.util.HashSet;

final class GLMilSymLayer implements GLLayer3, HitTestControl {
    private final MapRenderer surface;
    private final MilSymLayer subject;
    private final FeatureLayer3 subjectShadow;
    private final GLLayer3 impl;
    private HitTestControl hittest;

    GLMilSymLayer(MapRenderer surface, MilSymLayer subject) {
        this.surface = surface;
        this.subject = subject;
        this.subjectShadow = new FeatureLayer3(subject.getName(),
                subject.datastore);
        impl = GLLayerFactory.create4(surface, this.subjectShadow);
    }

    @Override
    public void hitTest(MapRenderer3 mapRenderer3,
            HitTestQueryParameters params,
            Collection<HitTestResult> collection) {
        if (hittest == null)
            return;
        if (params.resultFilter != null
                && !params.resultFilter.acceptClass(MapItem.class))
            return;
        Collection<HitTestResult> features = new HashSet<>();
        // swizzle the result filter for use with the features renderer hit-test
        HitTestQueryParameters fparams = new HitTestQueryParameters(params);
        if (fparams.resultFilter != null)
            fparams.resultFilter = new ClassResultFilter(Long.class);
        hittest.hitTest(mapRenderer3, fparams, features);

        // translate features to map items
        for (HitTestResult result : features) {
            try {
                Feature f = Utils.getFeature(subject.datastore,
                        (Long) result.subject);
                if (f == null)
                    continue;
                FeatureSet fs = Utils.getFeatureSet(subject.datastore,
                        f.getFeatureSetId());
                if (fs == null)
                    continue;
                MapItem i = MapView.getMapView().getRootGroup()
                        .deepFindUID(fs.getName());
                if (i != null && !((i instanceof EditablePolyline)
                        && i.getEditable())) {
                    result.point = new GeoPoint(params.geo);
                    collection.add(new HitTestResult(i, result));
                }
            } catch (DataStoreException ignored) {
            }
        }
    }

    @Override
    public void start() {
        if (impl != null) {
            impl.start();
            surface.visitControl(subjectShadow, new Visitor<HitTestControl>() {
                @Override
                public void visit(HitTestControl c) {
                    hittest = c;
                }
            }, HitTestControl.class);
            surface.registerControl(subject, this);
        }
    }

    @Override
    public void stop() {
        if (impl != null) {
            surface.unregisterControl(subject, this);
            hittest = null;
            impl.stop();
        }
    }

    @Override
    public Layer getSubject() {
        return subject;
    }

    @Override
    public void draw(GLMapView glMapView) {
        if (impl != null)
            impl.draw(glMapView);
    }

    @Override
    public void draw(GLMapView glMapView, int i) {
        if (impl != null)
            impl.draw(glMapView, i);
    }

    @Override
    public void release() {
        if (impl != null)
            impl.release();
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_SURFACE | GLMapView.RENDER_PASS_SPRITES;
    }
}
