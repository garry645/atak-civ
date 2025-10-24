package gov.tak.platform.layers.opengl;

import android.util.Pair;

import com.atakmap.android.maps.graphics.GLTriangle;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;

import gov.tak.platform.graphics.Color;
import gov.tak.platform.layers.CrosshairLayer;

public final class GLCrosshairLayer extends GLAbstractLayer2
        implements CrosshairLayer.OnCrosshairColorChangedListener {
    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {
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

    GLTriangle.Strip _crossHairLine;
    float colorR;
    float colorG;
    float colorB;
    float colorA;

    GLCrosshairLayer(MapRenderer surface, CrosshairLayer subject) {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
    }

    @Override
    public void start() {
        super.start();
        ((CrosshairLayer) this.subject)
                .addOnCrosshairColorChangedListener(this);
        this.onCrosshairColorChanged((CrosshairLayer) this.subject,
                ((CrosshairLayer) this.subject).getCrosshairColor());
    }

    @Override
    public void stop() {
        ((CrosshairLayer) this.subject)
                .removeOnCrosshairColorChangedListener(this);
        super.stop();
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass) {
        if (!MathUtils.hasBits(renderPass, getRenderPass()))
            return;
        final float width = view.currentPass.right - view.currentPass.left;
        final float height = view.currentPass.top - view.currentPass.bottom;

        // It is important to note that 'focusy' is relative to origin at
        // UPPER left, not LOWER left
        final float fx = view.currentPass.focusx - view.currentPass.left;
        final float fy = view.currentPass.focusy;

        final float pinLength = Math.min(width, height);
        if (_crossHairLine == null) {
            _crossHairLine = new GLTriangle.Strip(2, 4);
            _crossHairLine.setX(0, 0f);
            _crossHairLine.setY(0, 0f);

            _crossHairLine.setX(1, 0f);
            _crossHairLine.setY(1, 1f);

            _crossHairLine.setX(2, 1f);
            _crossHairLine.setY(2, 0f);

            _crossHairLine.setX(3, 1f);
            _crossHairLine.setY(3, 1f);
        }

        GLES20FixedPipeline.glPushMatrix();

        GLES20FixedPipeline.glColor4f(colorR, colorG, colorB, colorA);

        // the crosshair is centered relative to the focus

        // RIGHT
        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left + fx + pinLength / 8,
                view.currentPass.top - fy - 1f, 0f);
        GLES20FixedPipeline.glScalef(width - fx, 3f, 1f);
        _crossHairLine.draw();

        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left + fx + pinLength / 32,
                view.currentPass.top - fy, 0f);
        GLES20FixedPipeline.glScalef(width - fx, 1, 1f);
        _crossHairLine.draw();

        // LEFT
        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left - pinLength / 8,
                view.currentPass.top - fy - 1f, 0f);
        GLES20FixedPipeline.glScalef(fx, 3f, 1f);
        _crossHairLine.draw();

        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left - pinLength / 32,
                view.currentPass.top - fy, 0f);
        GLES20FixedPipeline.glScalef(fx, 1f, 1f);
        _crossHairLine.draw();

        // TOP
        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left + fx - 1f,
                view.currentPass.top - fy + pinLength / 8,
                0f);
        GLES20FixedPipeline.glScalef(3f, fy, 1f);
        _crossHairLine.draw();

        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(view.currentPass.left + fx,
                view.currentPass.top - fy + pinLength / 32,
                0f);
        GLES20FixedPipeline.glScalef(1f, fy, 1f);
        _crossHairLine.draw();

        // BOTTOM
        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(
                view.currentPass.left + fx - 1f,
                view.currentPass.bottom - pinLength / 8, 0f);
        GLES20FixedPipeline.glScalef(3f, height - fy, 1f);
        _crossHairLine.draw();

        GLES20FixedPipeline.glLoadIdentity();
        GLES20FixedPipeline.glTranslatef(view.currentPass.left + fx,
                view.currentPass.bottom - pinLength / 32, 0f);
        GLES20FixedPipeline.glScalef(1f, height - fy, 1f);
        _crossHairLine.draw();

        GLES20FixedPipeline.glPopMatrix();

        // XXX - red center dot
    }

    @Override
    public void release() {
        _crossHairLine = null;
    }

    @Override
    public void onCrosshairColorChanged(CrosshairLayer layer, int color) {
        this.colorR = Color.red(color) / 255f;
        this.colorG = Color.green(color) / 255f;
        this.colorB = Color.blue(color) / 255f;
        this.colorA = Color.alpha(color) / 255f;
    }
}