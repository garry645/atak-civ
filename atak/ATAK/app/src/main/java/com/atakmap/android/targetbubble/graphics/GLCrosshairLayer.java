
package com.atakmap.android.targetbubble.graphics;

import android.util.Pair;

import com.atakmap.android.maps.graphics.GLIcon;
import com.atakmap.android.maps.graphics.GLImageCache;
import com.atakmap.android.targetbubble.CrosshairLayer;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;

import gov.tak.platform.marshal.MarshalManager;

final class GLCrosshairLayer extends GLAbstractLayer2
        implements CrosshairLayer.OnCrosshairColorChangedListener {
    final static GLLayerSpi2 SPI = new GLLayerSpi2() {
        @Override
        public int getPriority() {
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> object) {
            if (object.second instanceof CrosshairLayer)
                return new GLCrosshairLayer(object.first,
                        (CrosshairLayer) object.second);
            return null;
        }
    };

    private final GLLayer3 legacyCrosshair;

    GLCrosshairLayer(MapRenderer surface, CrosshairLayer subject) {
        this(surface, subject, false);
    }

    GLCrosshairLayer(MapRenderer surface, CrosshairLayer subject,
            boolean legacyCrosshair) {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
        this.legacyCrosshair = legacyCrosshair
                ? (GLLayer3) gov.tak.platform.layers.opengl.GLCrosshairLayer.SPI
                        .create(
                                Pair.create(
                                        surface,
                                        MarshalManager.marshal(
                                                subject,
                                                CrosshairLayer.class,
                                                gov.tak.platform.layers.CrosshairLayer.class)))
                : null;
    }

    @Override
    public void start() {
        super.start();
        if (legacyCrosshair != null)
            legacyCrosshair.start();
    }

    @Override
    public void stop() {
        if (legacyCrosshair != null)
            legacyCrosshair.stop();
        super.stop();
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass) {
        if (!MathUtils.hasBits(renderPass, getRenderPass()))
            return;

        if (legacyCrosshair != null) {
            legacyCrosshair.draw(view, renderPass);
        } else {
            final float width = view.currentPass.right - view.currentPass.left;
            final float height = view.currentPass.top - view.currentPass.bottom;

            // It is important to note that 'focusy' is relative to origin at
            // UPPER left, not LOWER left
            final float fx = view.currentPass.focusx - view.currentPass.left;
            final float fy = view.currentPass.focusy;
            GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_BLEND);
            GLES20FixedPipeline.glBlendFunc(
                    GLES20FixedPipeline.GL_SRC_ALPHA,
                    GLES20FixedPipeline.GL_ONE_MINUS_SRC_ALPHA);
            GLES20FixedPipeline.glPushMatrix();
            String iconUri = ATAKUtilities
                    .getResourceUri(R.drawable.large_reticle_white);
            GLImageCache.Entry iconEntry = GLRenderGlobals
                    .get(view)
                    .getImageCache()
                    .fetchAndRetain(iconUri, true);
            float reticleScreenPercent = 0.4f;
            int reticleSize = Math.min((int) (width * reticleScreenPercent),
                    (int) (height * reticleScreenPercent));
            int reticleSizeHalf = reticleSize / 2;
            GLIcon icon = new GLIcon(reticleSize, reticleSize,
                    reticleSizeHalf, reticleSizeHalf - 4);
            icon.updateCacheEntry(iconEntry);
            GLES20FixedPipeline.glTranslatef((view.currentPass.left + fx),
                    (view.currentPass.top - fy), 0f);
            icon.draw();
            GLES20FixedPipeline.glPopMatrix();
            GLES20FixedPipeline.glDisable(GLES20FixedPipeline.GL_BLEND);
        }
    }

    @Override
    public void release() {
        if (legacyCrosshair != null)
            legacyCrosshair.release();
    }

    @Override
    public void onCrosshairColorChanged(CrosshairLayer layer, int color) {
        // deferred to `impl`
    }
}
