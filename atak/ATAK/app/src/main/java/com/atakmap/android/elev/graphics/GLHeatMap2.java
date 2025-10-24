
package com.atakmap.android.elev.graphics;

import android.util.Pair;

import com.atakmap.android.elev.HeatMapOverlay;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.elevation.ElevationHeatmapLayer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;

final class GLHeatMap2
        implements GLLayer3, HeatMapOverlay.OnHeatMapColorChangedListener {

    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            // HeatMapOverlay : Layer
            return GLHeatMap.SPI2.getPriority();
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer.getClass().equals(HeatMapOverlay.class))
                return GLLayerFactory
                        .adapt(new GLHeatMap2(surface, (HeatMapOverlay) layer));
            return null;
        }
    };

    final MapRenderer surface;
    final HeatMapOverlay subject;

    ElevationHeatmapLayer impl;
    GLLayer3 glimpl;
    boolean isDynamicRange;

    GLHeatMap2(MapRenderer surface, HeatMapOverlay subject) {
        this.surface = surface;
        this.subject = subject;
    }

    @Override
    public void onHeatMapColorChanged(HeatMapOverlay overlay) {
        final ElevationHeatmapLayer limpl = impl;
        if (limpl != null) {
            limpl.setValue(this.subject.getValue());
            limpl.setSaturation(this.subject.getSaturation());
            limpl.setAlpha(this.subject.getAlpha());
        }
        surface.requestRefresh();
    }

    /**************************************************************************/
    // GL Layer
    @Override
    public Layer getSubject() {
        return this.subject;
    }

    @Override
    public void start() {
        subject.addOnHeatMapColorChangedListener(this);
    }

    @Override
    public void stop() {
        subject.removeOnHeatMapColorChangedListener(this);
    }

    /**************************************************************************/
    // GL Asynchronous Map Renderable
    @Override
    public int getRenderPass() {
        return (glimpl != null) ? glimpl.getRenderPass()
                : GLMapView.RENDER_PASS_SURFACE | GLMapView.RENDER_PASS_SURFACE;
    }

    @Override
    public void draw(GLMapView view) {
        draw(view, GLMapView.RENDER_PASS_SURFACE);
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if (impl == null) {
            impl = new ElevationHeatmapLayer(subject.getName());
            impl.setAlpha(subject.getAlpha());
            impl.setSaturation(subject.getSaturation());
            impl.setValue(subject.getValue());

            isDynamicRange = impl.isDynamicRange();

            glimpl = GLLayerFactory.create4(surface, impl);
            glimpl.start();
        }
        if (glimpl == null)
            return;
        final SharedDataModel sdm = SharedDataModel.getInstance();
        if (sdm.isoDisplayMode.equals(SharedDataModel.HIDE))
            return;
        else if (sdm.isoDisplayMode.equals(SharedDataModel.ABSOLUTE)
                && isDynamicRange)
            impl.setAbsoluteRange(-800, 8850);
        else if (sdm.isoDisplayMode.equals(SharedDataModel.RELATIVE)
                && !isDynamicRange)
            impl.setDynamicRange();
        isDynamicRange = sdm.isoDisplayMode.equals(SharedDataModel.RELATIVE);
        glimpl.draw(view, renderPass);
    }

    @Override
    public void release() {
        if (glimpl != null) {
            glimpl.stop();
            glimpl.release();
        }
        if (impl != null)
            impl = null;
    }
}
