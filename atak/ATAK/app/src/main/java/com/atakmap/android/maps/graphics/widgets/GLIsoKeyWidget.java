
package com.atakmap.android.maps.graphics.widgets;

import android.graphics.Color;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.atakmap.android.elev.graphics.SharedDataModel;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.IsoKeyWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLText;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collection;

class GLIsoKeyWidget extends GLWidget2 {

    public final static GLWidgetSpi SPI = new GLWidgetSpi() {
        @Override
        public int getPriority() {
            // IsoWidget : MapWidget
            return 1;
        }

        @Override
        public GLWidget create(Pair<MapWidget, GLMapView> arg) {
            final MapWidget subject = arg.first;
            final GLMapView orthoView = arg.second;
            if (subject instanceof IsoKeyWidget) {
                IsoKeyWidget isoKey = (IsoKeyWidget) subject;
                GLIsoKeyWidget glIsoKey = new GLIsoKeyWidget(isoKey, orthoView);
                glIsoKey.startObserving(isoKey);
                return glIsoKey;
            } else {
                return null;
            }
        }
    };

    private final IsoKeyWidget subject;
    private final SurfaceRendererControl surfaceControl;
    private int drawVersion = -1;
    private int terrainVersion = -1;

    private GLIsoKeyWidget(IsoKeyWidget subject, GLMapView orthoView) {
        super(subject, orthoView);
        this.subject = subject;
        this.surfaceControl = orthoView
                .getControl(SurfaceRendererControl.class);
    }

    @Override
    public void releaseWidget() {
        stopObserving(subject);
    }

    @Override
    public void drawWidgetContent() {
        // obtain min/max for visible terrain tiles
        final int terrainVersion = orthoView.getTerrainVersion();
        if (SharedDataModel.getInstance().autoComputeMinMaxHeat &&
                surfaceControl != null &&
                (orthoView.currentScene.drawVersion != this.drawVersion
                        || terrainVersion != this.terrainVersion)) {

            this.drawVersion = orthoView.currentScene.drawVersion;
            this.terrainVersion = terrainVersion;

            final PointD nominalMetersScale = new PointD(
                    orthoView.currentScene.scene.displayModel.projectionXToNominalMeters,
                    orthoView.currentScene.scene.displayModel.projectionYToNominalMeters,
                    orthoView.currentScene.scene.displayModel.projectionZToNominalMeters);
            final double camtgt = MathUtils.distance(
                    orthoView.currentScene.scene.camera.location.x
                            * nominalMetersScale.x,
                    orthoView.currentScene.scene.camera.location.y
                            * nominalMetersScale.y,
                    orthoView.currentScene.scene.camera.location.z
                            * nominalMetersScale.z,
                    orthoView.currentScene.scene.camera.target.x
                            * nominalMetersScale.x,
                    orthoView.currentScene.scene.camera.target.y
                            * nominalMetersScale.y,
                    orthoView.currentScene.scene.camera.target.z
                            * nominalMetersScale.z);

            // establish min/max
            Collection<Envelope> aabbs = surfaceControl.getSurfaceBounds();
            double minEl = Double.NaN;
            double maxEl = Double.NaN;
            for (Envelope aabb_wgs84 : aabbs) {
                if (aabb_wgs84.maxZ == 19000.0)
                    continue;
                // exclude any tiles that are sufficiently far away
                PointD tileCentroid = new PointD(0d, 0d, 0d);
                orthoView.currentScene.scene.mapProjection.forward(
                        new GeoPoint((aabb_wgs84.minY + aabb_wgs84.maxY) / 2.0,
                                (aabb_wgs84.minX + aabb_wgs84.maxX) / 2.0,
                                (aabb_wgs84.minZ + aabb_wgs84.maxZ) / 2.0),
                        tileCentroid);
                final double slant = MathUtils.distance(
                        orthoView.currentScene.scene.camera.location.x
                                * nominalMetersScale.x,
                        orthoView.currentScene.scene.camera.location.y
                                * nominalMetersScale.y,
                        orthoView.currentScene.scene.camera.location.z
                                * nominalMetersScale.z,
                        tileCentroid.x * nominalMetersScale.x,
                        tileCentroid.y * nominalMetersScale.y,
                        tileCentroid.z * nominalMetersScale.z);

                PointD tileMin = new PointD(0d, 0d, 0d);
                orthoView.currentScene.scene.mapProjection
                        .forward(new GeoPoint(aabb_wgs84.minY, aabb_wgs84.minX,
                                aabb_wgs84.minZ), tileMin);
                final double radius = MathUtils.distance(
                        tileCentroid.x * nominalMetersScale.x,
                        tileCentroid.y * nominalMetersScale.y,
                        tileCentroid.z * nominalMetersScale.z,
                        tileMin.x * nominalMetersScale.x,
                        tileMin.y * nominalMetersScale.y,
                        tileMin.z * nominalMetersScale.z);

                if (Math.log((slant - radius) / camtgt) / Math.log(2.0) > 3.0)
                    continue;
                final double skirtHeight = 500.0;
                if (Double.isNaN(minEl)
                        || (aabb_wgs84.minZ + skirtHeight) < minEl)
                    minEl = aabb_wgs84.minZ + skirtHeight;
                if (Double.isNaN(maxEl) || aabb_wgs84.maxZ > maxEl)
                    maxEl = aabb_wgs84.maxZ;
            }
            if (!Double.isNaN(minEl))
                SharedDataModel.getInstance().minHeat = minEl;
            if (!Double.isNaN(maxEl))
                SharedDataModel.getInstance().maxHeat = maxEl;
        }
        float[] barSize = this.subject.getBarSize();

        float[] keyColor = new float[4];
        keyColor[0] = 0;
        keyColor[1] = 1;
        keyColor[2] = 0;
        keyColor[3] = 1;

        // Start at bottom-left corner
        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glTranslatef(0.0f, -_height, 0.0f);

        MapTextFormat mtf = MapView.getDefaultTextFormat();
        GLText glText = GLText.getInstance(mtf);
        float descent = glText.getDescent();
        float spacing = mtf.getBaselineSpacing();

        double minHeat, maxHeat;
        if (SharedDataModel.getInstance().isoDisplayMode
                .equals(SharedDataModel.ABSOLUTE)) {
            minHeat = SharedDataModel.isoScaleStart;
            maxHeat = SharedDataModel.getInstance().maxHeat;
        } else {
            minHeat = SharedDataModel.getInstance().minHeat;
            maxHeat = SharedDataModel.getInstance().maxHeat;
        }

        double curAlt = minHeat;
        double incAlt = (maxHeat - minHeat) / (IsoKeyWidget.NUM_LABELS - 1);

        if (GeoPoint.isAltitudeValid(minHeat) &&
                GeoPoint.isAltitudeValid(maxHeat)) {
            // Draw scale labels
            float textY = -spacing + descent;
            float textInc = (barSize[1] - spacing)
                    / (IsoKeyWidget.NUM_LABELS - 1);
            for (int j = 0; j < IsoKeyWidget.NUM_LABELS; j++) {
                String text = GLText.localize(String.valueOf((int) SpanUtilities
                        .convert(curAlt, Span.METER, Span.FOOT)));
                keyColor = rgbaFromElevation(curAlt, minHeat, maxHeat);
                GLES20FixedPipeline.glPushMatrix();
                GLES20FixedPipeline.glTranslatef(barSize[0] + _padding[LEFT],
                        textY, 0.0f);
                glText.drawSplitString(text, keyColor[0], keyColor[1],
                        keyColor[2], 1f);
                GLES20FixedPipeline.glPopMatrix();
                textY += textInc;
                curAlt += incAlt;
            }
        }

        // Draw top labels (mode and extra if specified)
        String[] topLabels = IsoKeyWidget.getTopLabels();
        float topLabelY = barSize[1] - spacing + descent;
        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glTranslatef(0f, topLabelY, 0f);
        if (!FileSystemUtils.isEmpty(topLabels[1])) {
            glText.drawSplitString(topLabels[1], 1f, 1f, 1f, 1f);
            GLES20FixedPipeline.glTranslatef(0f, spacing, 0f);
        }
        glText.drawSplitString(topLabels[0], 1f, 1f, 1f, 1f);
        GLES20FixedPipeline.glPopMatrix();

        // Draw scale bar
        float keyHeight = barSize[1] / SharedDataModel.isoScaleMarks;
        float keyY = 0;
        for (int x = 0; x < SharedDataModel.isoScaleMarks; x++) {
            double keyAlt = ((double) x
                    / (double) SharedDataModel.isoScaleMarks)
                    * (maxHeat - minHeat) + minHeat;
            keyColor = rgbaFromElevation(keyAlt, minHeat, maxHeat);

            FloatArray colorsFloatArray = new FloatArray(4 * 4)
                    .add(keyColor)
                    .add(keyColor)
                    .add(keyColor)
                    .add(keyColor);

            drawColoredRectangle(keyY, keyY + keyHeight, 0.0f, barSize[0],
                    colorsFloatArray.toArray());
            keyY += keyHeight;
        }

        GLES20FixedPipeline.glPopMatrix();
    }

    private final ByteBuffer pointer = com.atakmap.lang.Unsafe.allocateDirect(
            8 * 4)
            .order(ByteOrder.nativeOrder());
    private final FloatBuffer pointerf = pointer.asFloatBuffer();

    private void drawColoredRectangle(
            float bottom, float top, float left, float right, float[] colors) {
        pointerf.clear();
        pointerf.put(left);
        pointerf.put(bottom);
        pointerf.put(left);
        pointerf.put(top);
        pointerf.put(right);
        pointerf.put(top);
        pointerf.put(right);
        pointerf.put(bottom);
        pointerf.rewind();

        FloatBuffer colorPointer = com.atakmap.lang.Unsafe
                .allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorPointer.put(colors);
        colorPointer.position(0);

        GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_BLEND);
        GLES20FixedPipeline
                .glEnableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
        GLES20FixedPipeline
                .glEnableClientState(GLES20FixedPipeline.GL_COLOR_ARRAY);
        GLES20FixedPipeline.glVertexPointer(2, GLES20FixedPipeline.GL_FLOAT, 0,
                pointer);

        GLES20FixedPipeline.glColorPointer(4,
                GLES20FixedPipeline.GL_FLOAT,
                0, colorPointer);

        GLES20FixedPipeline.glDrawArrays(GLES20FixedPipeline.GL_TRIANGLE_FAN,
                0, 4);
        GLES20FixedPipeline.glDisable(GLES20FixedPipeline.GL_BLEND);
        GLES20FixedPipeline
                .glDisableClientState(GLES20FixedPipeline.GL_COLOR_ARRAY);
    }

    private float[] rgbaFromElevation(double elevation, double minElev,
            double maxElev) {
        // Special case for NaN (no elevation data available)
        if (Double.isNaN(elevation)) {
            // completely transparent
            return new float[] {
                    0f, 0f, 0f, 0f
            };
        }
        // We use HSV because it's convenient to make hue represent elevation
        // because it spans the entire primary color spectrum with a single field
        // (rather than using the three separate fields in RGB)
        //
        // We normalize using (puts elevation in the [0-1] range):
        // (elevation - minElev) / (maxElev - minElev)
        //
        // We multiple the normalized value by 255.0 because:
        // Hue expects to be a value between [0 .. 360)
        // but RED wraps in the HSV spectrum,
        // so we'll only use [0 .. 255)
        //
        // We use 255.0f - X because we want:
        // RED to be highest elevation and
        // BLUE to be the lowest
        //
        float hue = 255.0f - (float) ((elevation - minElev)
                / (maxElev - minElev) * 255.0);
        // We use android.graphics.Color to convert from HSV to RGB
        int color = Color.HSVToColor(((int) (255f * (float) 1)),
                new float[] {
                        hue, 1, 1
                });
        // We convert to RGB because that's what OpenGL wants
        return new float[] {
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                1
        };
    }

    private static class FloatArray {
        private final float[] data;
        private int position = 0;

        FloatArray(int size) {
            this.data = new float[size];
        }

        public FloatArray add(float toAdd) {
            this.data[position++] = toAdd;
            return this;
        }

        public FloatArray add(float[] toAdd) {
            for (float aToAdd : toAdd) {
                this.data[position++] = aToAdd;
            }
            return this;
        }

        public float[] toArray() {
            return data;
        }

        @NonNull
        public String toString() {
            StringBuilder ret = new StringBuilder("{ ");
            for (int i = 0; i < data.length; i++) {
                if (i >= position)
                    ret.append("N/A, ");
                ret.append(data[i]).append(", ");
            }
            ret.append("}");
            return ret.toString();
        }
    }

}
