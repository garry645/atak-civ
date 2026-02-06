
package com.atakmap.android.imagecapture;

import android.graphics.Color;
import android.graphics.PointF;
import android.util.Pair;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.graphics.AbstractGLMapItem2;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MutableUTMPoint;
import com.atakmap.coremap.maps.coords.UTMPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLText;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Render custom extends grid on map
 */
public class GLCustomGrid extends GLAbstractLayer2
        implements CustomGrid.OnChangedListener {

    private final CustomGrid subject;
    private int _currentDraw = 0;
    private boolean _recompute = true, _drawLabels, _visible;
    private FloatBuffer _gridVerts;
    private int _numJunctionVerts;
    private FloatBuffer _labelVerts;
    private String[] _labels;
    private int _xLines, _yLines;
    private GLText _glText;
    private float _strokeRed, _strokeGreen, _strokeBlue, _strokeAlpha;

    public final static GLLayerSpi2 SPI = new GLLayerSpi2() {

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> object) {
            if (!(object.second instanceof CustomGrid))
                return null;
            return new GLCustomGrid(object.first,
                    (CustomGrid) object.second);
        }

        @Override
        public int getPriority() {
            return 1;
        }
    };

    public GLCustomGrid(GLMapSurface surface, CustomGrid subject) {
        this(surface.getGLMapView(), subject);
    }

    protected GLCustomGrid(MapRenderer surface, CustomGrid subject) {
        super(surface, subject, GLMapView.RENDER_PASS_SURFACE
                | GLMapView.RENDER_PASS_SPRITES);
        this.subject = subject;
    }

    @Override
    protected void init() {
        super.init();
        this.subject.addOnChangedListener(this);
    }

    @Override
    public void release() {
        Unsafe.free(_gridVerts);
        Unsafe.free(_labelVerts);
        _gridVerts = _labelVerts = null;
        _numJunctionVerts = 0;
        super.release();
        this.subject.removeOnChangedListener(this);
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass) {
        if (!_visible)
            return;

        _recompute = _currentDraw != view.currentPass.drawVersion;
        _currentDraw = view.currentPass.drawVersion;

        projectVerts(view);
        if (_gridVerts == null)
            return;

        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SURFACE)) {
            GLES20FixedPipeline.glPushMatrix();
            GLES20FixedPipeline.glVertexPointer(2,
                    GLES20FixedPipeline.GL_FLOAT, 0, _gridVerts);
            GLES20FixedPipeline
                    .glEnableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
            GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_BLEND);
            GLES20FixedPipeline.glBlendFunc(GLES20FixedPipeline.GL_SRC_ALPHA,
                    GLES20FixedPipeline.GL_ONE_MINUS_SRC_ALPHA);

            GLES20FixedPipeline.glColor4f(
                    _strokeRed, _strokeGreen, _strokeBlue, _strokeAlpha);
            GLES20FixedPipeline.glLineWidth(
                    this.subject.getStrokeWeight());

            // render the grid lines
            GLES20FixedPipeline.glDrawArrays(
                    GLES20FixedPipeline.GL_LINES,
                    1,
                    _gridVerts.limit()/2 - _numJunctionVerts - 1);

            // render the zone junction lines
            if(_numJunctionVerts > 0) {
                // zone junctions are indicated by accentuated lines, choose highlight color based on
                // luminance contrast
                float bglum = 0.2126f * _strokeRed
                        + 0.7152f * _strokeGreen
                        + 0.0722f * _strokeBlue;

                GLES20FixedPipeline.glColor4f(
                        _strokeRed, _strokeGreen, _strokeBlue, _strokeAlpha);
                GLES20FixedPipeline.glLineWidth(
                        this.subject.getStrokeWeight()+2);

                // render the junction line
                GLES20FixedPipeline.glDrawArrays(
                        GLES20FixedPipeline.GL_LINES,
                        _gridVerts.limit()/2 - _numJunctionVerts,
                        _numJunctionVerts);

                GLES20FixedPipeline.glColor4f(
                        (bglum > 0.5f) ? 0f : 1f,
                        (bglum > 0.5f) ? 0f : 1f,
                        (bglum > 0.5f) ? 0f : 1f,
                        _strokeAlpha);
                GLES20FixedPipeline.glLineWidth(
                        this.subject.getStrokeWeight());

                // render the junction line highlight
                GLES20FixedPipeline.glDrawArrays(
                        GLES20FixedPipeline.GL_LINES,
                        _gridVerts.limit()/2 - (_numJunctionVerts),
                        _numJunctionVerts);
            }

            GLES20FixedPipeline.glDisable(GLES20FixedPipeline.GL_BLEND);
            GLES20FixedPipeline
                    .glDisableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
            GLES20FixedPipeline.glPopMatrix();
        }

        if (_drawLabels && MathUtils.hasBits(renderPass,
                GLMapView.RENDER_PASS_SPRITES)) {

            // Draw labels
            if (_glText == null)
                _glText = GLText.getInstance(MapView.getDefaultTextFormat());

            for (int i = 0, lbl = 0; i < _labels.length; i++, lbl += 2) {
                if (_labels[i] != null) {
                    float x = _labelVerts.get(lbl);
                    float y = _labelVerts.get(lbl + 1);
                    GLES20FixedPipeline.glPushMatrix();
                    GLES20FixedPipeline.glTranslatef(x, y, 0);
                    _glText.draw(_labels[i], _strokeRed, _strokeGreen,
                            _strokeBlue, _strokeAlpha);
                    GLES20FixedPipeline.glPopMatrix();
                }
            }
        }
    }

    private boolean withinView(PointF p, GLMapView view) {
        return p.x >= view.getLeft() && p.x < view.getRight()
                && p.y >= view.getBottom()
                && p.y <= view.getTop();
    }

    private void projectVerts(final GLMapView view) {
        if (!_recompute)
            return;

        // Free previous buffers
        Unsafe.free(_gridVerts);
        Unsafe.free(_labelVerts);
        _gridVerts = _labelVerts = null;
        _numJunctionVerts = 0;

        // Grid points buffer
        DoubleBuffer points = this.subject.getPointBuffer();

        // No points to draw
        if (points == null)
            return;

        GeoBounds bounds = this.subject.getBounds();

        // if bounds is null, then forward will fail, just treat this as if there
        // is no point buffer.
        if (bounds == null)
            return;

        _xLines = this.subject.getVerticalLineCount();
        _yLines = this.subject.getHorizontalLineCount();

        GLMapView.State state = view.currentPass;

        // Build labels
        _drawLabels = this.subject.isDrawingLabels(state.drawLat,
                state.drawMapResolution);
        _labels = this.subject.getLabels(true);
        if (_labels != null) {
            for (int i = 0; i < _labels.length; i++)
                _labels[i] = GLText.localize(_labels[i]);
        }

        // Grid line vertices
        final int numZonesEW;
        if(!bounds.crossesIDL()) {
            view.scratch.geo.set(bounds.getNorth(), bounds.getWest());
            final UTMPoint ul = UTMPoint.fromGeoPoint(view.scratch.geo);
            view.scratch.geo.set(bounds.getNorth(), bounds.getEast());
            final UTMPoint ur = UTMPoint.fromGeoPoint(view.scratch.geo);
            view.scratch.geo.set(bounds.getSouth(), bounds.getEast());
            final UTMPoint lr = UTMPoint.fromGeoPoint(view.scratch.geo);
            view.scratch.geo.set(bounds.getSouth(), bounds.getWest());
            final UTMPoint ll = UTMPoint.fromGeoPoint(view.scratch.geo);

            final int zoneWest = Math.min(ul.getLngZone(), ll.getLngZone());
            final int zoneEast = Math.min(ur.getLngZone(), lr.getLngZone());

            numZonesEW = zoneEast-zoneWest+1;
        } else {
            numZonesEW = 1;
        }

        _gridVerts = Unsafe.allocateDirect(
                ((_xLines+2)*4) + ((_yLines+2)*numZonesEW*4 + 2) + (numZonesEW*2*2),
                FloatBuffer.class);
        if(numZonesEW == 1 ||
                (this.subject.getGridJunctionStrategy()
                        == CustomGrid.GridJunctionStrategy.Extend)) {

            AbstractGLMapItem2.forward(view, points, 3, _gridVerts, 2, bounds);

            // swizzle the corners
            final float ulx = _gridVerts.get(0);
            final float uly = _gridVerts.get(1);
            final float urx = _gridVerts.get(2);
            final float ury = _gridVerts.get(3);
            final float lrx = _gridVerts.get(4);
            final float lry = _gridVerts.get(5);
            final float llx = _gridVerts.get(6);
            final float lly = _gridVerts.get(7);
            // ul {8, 9}

            _gridVerts.clear();
            _gridVerts.position(points.capacity()/3*2);
            // top edge
            _gridVerts.put(ulx).put(uly).put(urx).put(ury);
            // bottom edge
            _gridVerts.put(llx).put(lly).put(lrx).put(lry);

            _gridVerts.flip();
        } else {
            // reserved vertex[0]
            _gridVerts.position(2);

            GeoPoint rowEastBound = new GeoPoint(this.subject.getCorners()[1]);
            GeoPoint gridLowerRight = new GeoPoint(this.subject.getCorners()[2]);

            GeoPoint[] junctionLines = new GeoPoint[numZonesEW*2];

            MutableUTMPoint rowWesting = new MutableUTMPoint(UTMPoint.fromGeoPoint(this.subject.getCorners()[0]));
            final double spacing = this.subject.getSpacing();
            for(int j = -1; j <= _yLines; j++) {
                UTMPoint left = new UTMPoint(rowWesting);
                for(int i = 0; i < numZonesEW; i++) {
                    // zone E bound
                    final double zoneMaxLng = Math.min(
                            -180d + ((left.getLngZone()+i)*6),
                            rowEastBound.getLongitude());

                    left.toLatLng(view.scratch.matrixD);
                    final double leftLat = view.scratch.matrixD[0];
                    final double leftLng = view.scratch.matrixD[1];
                    view.scratch.geo.set(leftLat, leftLng, 0d);

                    // record the junction line top+bottom
                    if(i > 0 && (j == -1 || j == _yLines))
                        junctionLines[(i*2) + ((j == -1) ? 0 : 1)] = new GeoPoint(leftLat, leftLng);

                    // XXX - bad math: LOB distance, zone convergence angle
                    final double d = view.scratch.geo.distanceTo(
                            new GeoPoint(
                                    leftLat,
                                    zoneMaxLng -0.0000001d // ~1cm inside zone
                                    ));
                    UTMPoint right = new UTMPoint(
                            left.getZoneDescriptor(),
                            left.getEasting() + d,
                            left.getNorthing());

                    // insert the line

                    // NOTE: `view.scratch.geo` is seeded with `left`
                    view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
                    _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);

                    right.toLatLng(view.scratch.matrixD);
                    view.scratch.geo.set(view.scratch.matrixD[0], view.scratch.matrixD[1], 0d);
                    view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
                    _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);

                    // shift left to the next EW zone
                    left = UTMPoint.fromGeoPoint(
                            new GeoPoint(view.scratch.geo.getLatitude(), zoneMaxLng));
                }

                // shift south
                rowWesting.offset(0d, -spacing);

                rowEastBound = GeoCalculations.pointAtDistance(rowEastBound, rowEastBound.bearingTo(gridLowerRight), spacing);
            }

            for(int j = 0; j < (_xLines*2); j++) {
                final int pidx = (5*3) + (j*3);
                view.scratch.geo.set(points.get(pidx+1), points.get(pidx), 0d);
                view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
                _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);
            }

            // left edge
            view.scratch.geo.set(points.get(1), points.get(0), 0d);
            view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
            _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);
            view.scratch.geo.set(points.get(10), points.get(9), 0d);
            view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
            _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);
            // right edge
            view.scratch.geo.set(points.get(4), points.get(3), 0d);
            view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
            _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);
            view.scratch.geo.set(points.get(7), points.get(6), 0d);
            view.currentPass.scene.forward(view.scratch.geo, view.scratch.pointF);
            _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);

            // junction lines
            for(int i = 0; i < junctionLines.length/2; i++) {
                if(junctionLines[i*2] == null || junctionLines[i*2+1] == null)
                    continue;
                view.currentPass.scene.forward(junctionLines[i*2], view.scratch.pointF);
                _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);
                view.currentPass.scene.forward(junctionLines[i*2+1], view.scratch.pointF);
                _gridVerts.put(view.scratch.pointF.x).put(view.scratch.pointF.y);

                _numJunctionVerts += 2;
            }

            _gridVerts.flip();
        }

        // Label position vertices
        _labelVerts = Unsafe.allocateDirect(_labels.length * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        if (_glText == null)
            _glText = GLText.getInstance(MapView.getDefaultTextFormat());

        // Calculate label positions
        PointF p = new PointF();
        float labelHeight = _glText.getStringHeight();
        for (int i = 0; i < _labels.length; i++) {
            int labelIndex = this.subject.getLabelPositionIndex(i);
            float labelWidth = _glText.getStringWidth(_labels[i]);
            int ind1 = labelIndex * 3;
            view.scratch.geo.set(points.get(ind1+1), points.get(ind1));
            view.scratch.geo.set(view.getTerrainMeshElevation(view.scratch.geo.getLatitude(), view.scratch.geo.getLongitude()));
            view.currentPass.scene.forward(view.scratch.geo, p);
            boolean vertical = i < _xLines + 2;
            boolean inside = withinView(p, view);
            if (!inside) {
                int ind2 = ind1 + 3;
                if (labelIndex == 0 && i == 0)
                    ind2 = ind1 + 9;
                else if (labelIndex == 3)
                    ind2 = ind1 - 3;
                view.scratch.geo.set(points.get(ind2+1), points.get(ind2));
                view.scratch.geo.set(view.getTerrainMeshElevation(view.scratch.geo.getLatitude(), view.scratch.geo.getLongitude()));
                PointF[] l = new PointF[] {
                        new PointF(p.x, p.y),
                        view.currentPass.scene.forward(view.scratch.geo, (PointF)null)
                };
                boolean endInside = withinView(l[1], view);
                if (endInside)
                    p.set(l[1].x, l[1].y);
                if (vertical) {
                    p.x += endInside ? -labelWidth / 2 : 4;
                    p.y += endInside ? -labelHeight + 4
                            : (-labelHeight / 2) - 4;
                } else {
                    p.x += endInside ? 8 : -labelWidth + 4;
                    p.y += endInside ? -(labelHeight / 2) + 4 : 4;
                }
            } else {
                if (vertical) {
                    p.x -= labelWidth / 2;
                    p.y += 4;
                } else {
                    p.x -= labelWidth;
                    p.y -= labelHeight / 2;
                }
            }

            _labelVerts.put(p.x);
            _labelVerts.put(p.y);

            if (!withinView(p, view))
                _labels[i] = null;
        }

        _labelVerts.clear();

        int col = this.subject.getColor();
        _strokeRed = Color.red(col) / 255f;
        _strokeGreen = Color.green(col) / 255f;
        _strokeBlue = Color.blue(col) / 255f;
        _strokeAlpha = Color.alpha(col) / 255f;
    }

    @Override
    public void onChanged(CustomGrid grid) {
        _currentDraw = 0;
        _visible = grid.isVisible();
        renderContext.requestRefresh();
    }
}
