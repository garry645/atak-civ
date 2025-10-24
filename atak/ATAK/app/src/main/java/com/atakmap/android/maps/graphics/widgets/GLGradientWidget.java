
package com.atakmap.android.maps.graphics.widgets;

import android.opengl.GLES30;
import android.util.Pair;

import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.GradientWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLText;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import gov.tak.api.engine.Shader;
import gov.tak.platform.graphics.Color;

public class GLGradientWidget extends GLWidget2
        implements GradientWidget.OnLegendContentChangedListener {

    public final static GLWidgetSpi SPI = new GLWidgetSpi() {
        @Override
        public int getPriority() {
            // GradientWidget : MapWidget
            return 1;
        }

        @Override
        public GLWidget create(Pair<MapWidget, GLMapView> arg) {
            final MapWidget subject = arg.first;
            final GLMapView orthoView = arg.second;
            if (subject instanceof GradientWidget) {
                GradientWidget gradientWidget = (GradientWidget) subject;
                GLGradientWidget glGradientWidget = new GLGradientWidget(
                        gradientWidget, orthoView);
                glGradientWidget.startObserving(gradientWidget);
                gradientWidget
                        .addOnLegendContentChangedListener(glGradientWidget);
                return glGradientWidget;
            } else {
                return null;
            }
        }
    };

    private final GradientWidget subject;
    private int[] gradientColors;
    private String legendText;
    private final int[] legendVbo = new int[1];

    private GLGradientWidget(GradientWidget subject, GLMapView orthoView) {
        super(subject, orthoView);
        this.subject = subject;
        onLegendContentChangedListener(subject);
    }

    private static void addToBar(FloatBuffer fb, int color, float posY,
            float barWidth) {
        float colorR = Color.red(color) / 255.f;
        float colorG = Color.green(color) / 255.f;
        float colorB = Color.blue(color) / 255.f;
        float colorA = Color.alpha(color) / 255.f;
        fb.put(0.f);
        fb.put(posY);
        fb.put(colorR);
        fb.put(colorG);
        fb.put(colorB);
        fb.put(colorA);
        fb.put(barWidth);
        fb.put(posY);
        fb.put(colorR);
        fb.put(colorG);
        fb.put(colorB);
        fb.put(colorA);
    }

    @Override
    public void drawWidgetContent() {

        if (legendText == null || gradientColors.length == 0)
            return;

        final float[] barSize = this.subject.getBarSize();

        MapTextFormat mtf = MapView.getDefaultTextFormat();
        GLText glText = GLText.getInstance(mtf);

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        final float textWidth = glText.getStringWidth(legendText);
        final float legendBoundsLeft = _padding[LEFT];
        final float legendBoundsBottom = _padding[BOTTOM];

        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glTranslatef(legendBoundsLeft, legendBoundsBottom,
                0.f);

        Shader shader = Shader.create(Shader.FLAG_COLOR_POINTER,
                orthoView.getRenderContext());

        GLES30.glUseProgram(shader.getHandle());

        // uniforms
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION,
                orthoView.scratch.matrixF, 0);
        GLES30.glUniformMatrix4fv(shader.getUProjection(), 1, false,
                orthoView.scratch.matrixF, 0);
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_MODELVIEW,
                orthoView.scratch.matrixF, 0);
        GLES30.glUniformMatrix4fv(shader.getUModelView(), 1, false,
                orthoView.scratch.matrixF, 0);
        GLES30.glUniform4f(shader.getUColor(), 1.f, 1.f, 1.f, 1.f);
        // attributes
        GLES30.glEnableVertexAttribArray(shader.getAVertexCoords());
        GLES30.glEnableVertexAttribArray(shader.getAColorPointer());
        if (legendVbo[0] == 0) {
            GLES30.glGenBuffers(1, legendVbo, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, legendVbo[0]);

            FloatBuffer fb = Unsafe
                    .allocateDirect(4 * (gradientColors.length + 1) * 12)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            float posY = 0.f;
            final float charHeight = _height / gradientColors.length;
            final float barWidth = barSize[0];//16.f;

            addToBar(fb, gradientColors[0], posY, barWidth);
            posY -= charHeight;
            for (int colorIndex = 0; colorIndex < gradientColors.length; ++colorIndex) {
                addToBar(fb, gradientColors[colorIndex], posY, barWidth);
                posY -= charHeight;
            }
            fb.position(0);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, fb.capacity() * 4, fb,
                    GLES30.GL_STATIC_DRAW);
        } else {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, legendVbo[0]);
        }
        GLES30.glVertexAttribPointer(shader.getAVertexCoords(), 2,
                GLES30.GL_FLOAT, false, 24, 0);
        GLES30.glVertexAttribPointer(shader.getAColorPointer(), 4,
                GLES30.GL_FLOAT, false, 24, 8);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0,
                (gradientColors.length + 1) * 2);

        GLES30.glDisableVertexAttribArray(shader.getAVertexCoords());
        GLES30.glDisableVertexAttribArray(shader.getAColorPointer());
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES20FixedPipeline.glTranslatef(barSize[0] + _padding[LEFT], 0.f, 0.f);
        glText.drawSplitString(legendText, gradientColors);
        GLES20FixedPipeline.glPopMatrix();
    }

    @Override
    public void releaseWidget() {

    }

    private static int mixColorComponents(int c1, int c2, double mix) {
        final double mix1 = 1.0 - mix;
        final double mix2 = 1.0 - mix1;
        return Math.min((int) ((((double) c1 / 255.0) * mix1
                + ((double) c2 / 255.0) * mix2) * 255), 255);
    }

    private static int interpolateColors(int color1, int color2, double mix) {
        int r = mixColorComponents(Color.red(color1), Color.red(color2), mix);
        int g = mixColorComponents(Color.green(color1), Color.green(color2),
                mix);
        int b = mixColorComponents(Color.blue(color1), Color.blue(color2), mix);
        int a = mixColorComponents(Color.alpha(color1), Color.alpha(color2),
                mix);
        return a << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | b & 0xff;
    }

    @Override
    public void onLegendContentChangedListener(GradientWidget widget) {

        int[] colors = widget.getArgbColorKeys();
        final String[] lineItems = widget.getLineItemStrings();

        // resample colors for each line (if needed)

        int[] resampledColors = new int[lineItems.length];
        final double scaleFactor = (double) colors.length
                / resampledColors.length;

        for (int i = 0; i < resampledColors.length; ++i) {
            double reach = (double) i * scaleFactor;
            int index1 = (int) Math.floor(reach);
            int index2 = Math.min(index1 + 1, colors.length - 1);
            resampledColors[i] = interpolateColors(colors[index1],
                    colors[index2], reach - Math.floor(reach));
        }

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String lineItem : lineItems) {
            sb.append(delim);
            sb.append(lineItem);
            delim = "\n";
        }

        this.orthoView.queueEvent(new Runnable() {
            @Override
            public void run() {

                if (legendVbo[0] != 0) {
                    GLES30.glDeleteBuffers(1, legendVbo, 0);
                    legendVbo[0] = 0;
                }

                GLGradientWidget.this.gradientColors = resampledColors;
                GLGradientWidget.this.legendText = sb.toString();
            }
        });
    }
}
