
package com.atakmap.android.targetbubble.graphics;

import android.util.Pair;

import com.atakmap.android.targetbubble.MapTargetBubble;
import com.atakmap.android.targetbubble.MapTargetBubble.OnCrosshairColorChangedListener;
import com.atakmap.android.targetbubble.MapTargetBubble.OnLocationChangedListener;
import com.atakmap.android.targetbubble.MapTargetBubble.OnScaleChangedListener;
import gov.tak.api.annotation.IncubatingApi;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.util.Visitor;

import gov.tak.platform.layers.opengl.GLMagnifierLayer;
import gov.tak.platform.layers.MagnifierLayer;
import gov.tak.platform.marshal.MarshalManager;

public class GLMapTargetBubble implements OnLocationChangedListener,
        OnScaleChangedListener,
        OnCrosshairColorChangedListener,
        GLLayer3,
        MapControl {

    static {
        GLLayerFactory.register(GLCrosshairLayer.SPI);
    }

    public final static GLLayerSpi2 SPI2 = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            // MapTargetBubble : Layer
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer instanceof MapTargetBubble)
                return new GLMapTargetBubble(surface, (MapTargetBubble) layer);
            return null;
        }
    };

    /**
     * also refered to as the reticle tool
     */

    public static final String TAG = "GLMapTargetBubble";

    private final MapTargetBubble subject;
    private boolean initialized;
    private final MapRenderer renderCtx;
    private GLMagnifierLayer impl;
    private GLCrosshairLayer crosshair;

    // GLMapView interface, for backwards API compatibility
    protected float focusx;
    protected float focusy;
    protected int _top;
    private boolean legacyCrosshair;
    private float initialFocusX;
    private float initialFocusY;

    public GLMapTargetBubble(final MapRenderer surface,
            MapTargetBubble subject) {
        this.impl = new GLMagnifierLayer(
                surface,
                MarshalManager.marshal(subject, MapTargetBubble.class,
                        MagnifierLayer.class));

        this.renderCtx = surface;
        this.subject = subject;

        this.legacyCrosshair = subject.isLegacyCrosshair();
    }

    @IncubatingApi(since = "4.3")
    public MapRenderer2 getRenderer() {
        return this.impl.getRenderer();
    }

    // GLMapView interface, for backwards API compatibility
    public final <T extends MapControl> boolean visitControl(Layer2 layer,
            Visitor<T> visitor, Class<T> ctrlClazz) {
        return this.impl.visitControl(layer, visitor, ctrlClazz);
    }

    protected final void queueEvent(Runnable r) {
        this.renderCtx.queueEvent(r);
    }

    @Override
    public void onMapTargetBubbleLocationChanged(final MapTargetBubble bubble) {
        // handled by `impl`
    }

    @Override
    public void onMapTargetBubbleScaleChanged(MapTargetBubble bubble) {
        // handled by `impl`
    }

    @Override
    public void onMapTargetBubbleCrosshairColorChanged(MapTargetBubble bubble) {
    }

    @Override
    public void draw(GLMapView view) {
        this.draw(view, -1);
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if (!MathUtils.hasBits(renderPass, getRenderPass()))
            return;

        if (!this.initialized) {
            this.crosshair = new GLCrosshairLayer((GLMapView) getRenderer(),
                    this.subject.getCrosshair(), legacyCrosshair);
            this.crosshair.start();
            this.initialFocusX = view.currentPass.focusx;
            this.initialFocusY = view.currentPass.focusy;
            this.initialized = true;
        }

        this.impl.draw(view, renderPass);
        final GLMapView renderer = (GLMapView) getRenderer();
        focusx = renderer.currentPass.focusx;
        focusy = renderer.currentPass.focusy;
        _top = renderer.currentPass.top;

        crosshair.draw(view, GLMapView.RENDER_PASS_UI);
    }

    @Override
    public void release() {
        impl.release();
        if (crosshair != null) {
            crosshair.stop();
            crosshair.release();
            this.crosshair = null;
        }

        this.initialized = false;
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_UI;
    }

    /**************************************************************************/
    // GL Layer

    @Override
    public Layer getSubject() {
        return this.subject;
    }

    @Override
    public void start() {
        this.subject.addOnLocationChangedListener(this);
        this.subject.addOnScaleChangedListener(this);
        this.subject.addOnCrosshairColorChangedListener(this);
        this.renderCtx.registerControl(this.subject, this);

        this.impl.start();
    }

    @Override
    public void stop() {
        this.impl.stop();

        this.renderCtx.unregisterControl(this.subject, this);
        this.subject.removeOnCrosshairColorChangedListener(this);
        this.subject.removeOnLocationChangedListener(this);
        this.subject.removeOnScaleChangedListener(this);
    }
}
