
package com.atakmap.android.gridlines.graphics;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.graphics.GLSegmentFloatingLabel;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MGRSPoint;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;
import com.atakmap.coremap.maps.coords.MutableMGRSPoint;
import com.atakmap.coremap.maps.coords.Vector2D;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLAntiAliasedLine;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.math.Rectangle;
import com.atakmap.opengl.GLText;
import com.atakmap.util.DirectExecutorService;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

class GLGridTile {

    private GeoPoint[] _actualPolygon;
    private FloatBuffer _polyBuf;
    private FloatBuffer _polyBufProjected;
    private final MutableGeoBounds _bounds = new MutableGeoBounds(0, 0, 0, 0);

    public static final String TAG = "GLGridTile";

    static int debugDrawCount;

    MGRSPoint mgrsRef;
    GeoPoint sw, nw, ne, se;
    int subResolution;
    private GLSegmentFloatingLabel glTextEasting;
    private GLSegmentFloatingLabel glTextNorthing;
    int lastRenderPump;

    private void updatePolyBuf() {
        float[] data = new float[_actualPolygon.length * 2];
        int j = 0;
        double northBound = -Double.MAX_VALUE, eastBound = -Double.MAX_VALUE;
        double southBound = Double.MAX_VALUE, westBound = Double.MAX_VALUE;
        for (GeoPoint p : _actualPolygon) {
            data[j++] = (float) p.getLongitude();
            data[j++] = (float) p.getLatitude();
            northBound = Math.max(northBound, p.getLatitude());
            southBound = Math.min(southBound, p.getLatitude());
            eastBound = Math.max(eastBound, p.getLongitude());
            westBound = Math.min(westBound, p.getLongitude());
        }
        _bounds.set(northBound, westBound, southBound, eastBound);

        // Need to recalculate the northwest, southwest, and southeast
        // points for proper label placement
        nw = new GeoPoint(southBound, eastBound);
        sw = new GeoPoint(northBound, eastBound);
        se = new GeoPoint(northBound, westBound);
        for (GeoPoint p : _actualPolygon) {
            if (Math.hypot(p.getLongitude() - westBound,
                    p.getLatitude() - northBound) < Math.hypot(
                            nw.getLongitude() - westBound,
                            nw.getLatitude() - northBound))
                nw = p;
            if (Math.hypot(p.getLongitude() - westBound,
                    p.getLatitude() - southBound) < Math.hypot(
                            sw.getLongitude() - westBound,
                            sw.getLatitude() - southBound))
                sw = p;
            if (Math.hypot(p.getLongitude() - eastBound,
                    p.getLatitude() - southBound) < Math.hypot(
                            se.getLongitude() - eastBound,
                            se.getLatitude() - southBound))
                se = p;
        }

        ByteBuffer bb = com.atakmap.lang.Unsafe.allocateDirect(data.length
                * (Float.SIZE / 8));
        bb.order(ByteOrder.nativeOrder());
        _polyBuf = bb.asFloatBuffer();
        _polyBuf.put(data);
    }

    GeoPoint[] setActualPolygon(GeoPoint[] value) {
        _actualPolygon = value;
        updatePolyBuf();
        _antiAliasedLineRenderer.setLineData(_actualPolygon, 2,
                GLAntiAliasedLine.ConnectionType.FORCE_CLOSE);
        return _actualPolygon;
    }

    private GeoBounds getBounds() {
        if (_actualPolygon == null)
            _bounds.set(
                    Math.max(nw.getLatitude(), ne.getLatitude()),
                    Math.min(nw.getLongitude(), sw.getLongitude()),
                    Math.min(sw.getLatitude(), se.getLatitude()),
                    Math.max(ne.getLongitude(), se.getLongitude()));
        return _bounds;
    }

    boolean inView(GLMapView ortho) {
        GeoBounds bounds = getBounds();
        if (bounds.getNorth() < ortho.currentPass.southBound
                || bounds.getSouth() > ortho.currentPass.northBound)
            return false;
        if (ortho.currentPass.crossesIDL) {
            if (ortho.eastBoundUnwrapped > 180 && bounds.getWest() < 0
                    && bounds.getWest() + 360 <= ortho.eastBoundUnwrapped)
                return true;
            if (ortho.westBoundUnwrapped < -180 && bounds.getEast() > 0
                    && bounds.getEast() - 360 >= ortho.westBoundUnwrapped)
                return true;
        }
        return !(bounds.getWest() > ortho.eastBoundUnwrapped
                || bounds.getEast() < ortho.westBoundUnwrapped);
    }

    void drawOrtho(GLMapSurface surface, GLMapView ortho, float red,
            float green, float blue, int renderPass) {

        final GLMapView.State state = ortho.currentPass;
        final long s = (((long) state.renderPump << 32L)
                | ((long) state.drawVersion & 0xFFFFFFFFL));
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES)
                && lastDrawVersionS != s) {
            lastDrawVersionS = s;

            int smallDim = Math.min(
                    Math.abs(ortho.getTop() - ortho.getBottom()),
                    Math.abs(ortho.getRight() - ortho.getLeft()));
            screenSpanMeters = state.drawMapResolution * smallDim
                    * Math.cos(state.drawLat * Math.PI / 180);
        }

        boolean drawSelf = true;
        this.lastRenderPump = ortho.currentPass.renderPump;
        if (screenSpanMeters < subResolution * 5.25 && subResolution >= 100
                && OSMUtils.mapnikTileLeveld(
                        ortho.currentScene.drawMapResolution, 0d)
                        - OSMUtils.mapnikTileLeveld(
                                ortho.currentPass.drawMapResolution, 0d) < 1) { //5.25 = Maximum number of grid squares along the small axis of the screen
            drawSelf = !_drawSubs(surface, ortho, red, green, blue, renderPass);
        }

        if (drawSelf) {
            debugDrawCount++;

            // draw the lines
            if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SURFACE)) {
                _antiAliasedLineRenderer.draw(ortho, 0f, 0f, 0f, 1f, 4f);
                _antiAliasedLineRenderer.draw(ortho, red, green, blue, 1f, 2f);
            }

            // XXX - limiting the tilt at which the labels get drawn. Note that
            //       legacy effectively never displayed labels on tilt anyway,
            //       so not a regression. It's currently too expensive to run
            //       the various calculations over every tile and the labels
            //       never align on the top edge once the horizon is in view

            // draw the labels
            if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES)
                    && ortho.currentPass.drawTilt < 40d) {
                if (projSrid != ortho.currentPass.drawSrid) {
                    swp = ortho.currentPass.scene.mapProjection.forward(sw,
                            null);
                    sep = ortho.currentPass.scene.mapProjection.forward(se,
                            null);
                    nwp = ortho.currentPass.scene.mapProjection.forward(nw,
                            null);
                    projSrid = ortho.currentPass.drawSrid;
                }
                if (lastDrawVersionM != ortho.currentPass.drawVersion) {
                    ortho.currentPass.scene.forward.transform(swp,
                            ortho.scratch.pointD);
                    swx = (float) ortho.scratch.pointD.x;
                    swy = (float) ortho.scratch.pointD.y;
                    ortho.currentPass.scene.forward.transform(sep,
                            ortho.scratch.pointD);
                    sex = (float) ortho.scratch.pointD.x;
                    sey = (float) ortho.scratch.pointD.y;
                    ortho.currentPass.scene.forward.transform(nwp,
                            ortho.scratch.pointD);
                    nwx = (float) ortho.scratch.pointD.x;
                    nwy = (float) ortho.scratch.pointD.y;
                    lastDrawVersionM = ortho.currentPass.drawVersion;
                }

                final float l = ortho.currentScene.left;
                final float r = ortho.currentScene.right;
                final float t = ortho.currentScene.top;
                final float b = ortho.currentScene.bottom;

                // quickly filter out any segments that are contained within the viewport
                final boolean containsSW = Rectangle.contains(l, b, r, t, swx,
                        swy);
                final boolean containsSE = Rectangle.contains(l, b, r, t, sex,
                        sey);
                final boolean containsNW = Rectangle.contains(l, b, r, t, nwx,
                        nwy);

                boolean northing = !containsSW || !containsSE;
                float wn = 0f;
                boolean easting = !containsSW || !containsNW;
                float we = 0f;

                // NOTE: bias is always against top if the segment intersects
                // both the left and top edges

                if (northing) {
                    // verify that we intersect the left or top edges
                    Vector2D lin = Vector2D.segmentToSegmentIntersection(
                            new Vector2D(swx, swy), new Vector2D(sex, sey),
                            new Vector2D(l, t), new Vector2D(l, b));
                    Vector2D tin = Vector2D.segmentToSegmentIntersection(
                            new Vector2D(swx, swy), new Vector2D(sex, sey),
                            new Vector2D(l, t), new Vector2D(r, t));

                    northing &= (lin != null || tin != null);

                    if (containsSW != containsSE) {
                        // only one edge intersects
                        wn = !containsSW ? 0f : 1f;
                    } else if (northing) {
                        // both edges intersect, we will weight towards the
                        // top-most or end-most endpoint
                        if (tin != null) {
                            wn = (swy >= sey) ? 0f : 1f;
                        } else if (lin != null) {
                            wn = (swx <= sex) ? 0f : 1f;
                        }
                    }
                }

                if (easting) {
                    Vector2D lie = Vector2D.segmentToSegmentIntersection(
                            new Vector2D(swx, swy), new Vector2D(nwx, nwy),
                            new Vector2D(l, t), new Vector2D(l, b));
                    Vector2D tie = Vector2D.segmentToSegmentIntersection(
                            new Vector2D(swx, swy), new Vector2D(nwx, nwy),
                            new Vector2D(l, t), new Vector2D(r, t));
                    easting &= (lie != null || tie != null);
                    if (containsSW != containsNW) {
                        // only one edge intersects
                        we = !containsSW ? 0f : 1f;
                    } else if (easting) {
                        // both edges intersect, we will weight towards the
                        // top-most or end-most endpoint
                        if (tie != null) {
                            we = (swy >= nwy) ? 0f : 1f;
                        } else if (lie != null) {
                            we = (swx <= nwx) ? 0f : 1f;
                        }
                    }
                }

                String text;

                if (easting) {
                    if (glTextEasting == null) {
                        glTextEasting = new GLSegmentFloatingLabel();
                        glTextEasting
                                .setTextFormat(MapView.getDefaultTextFormat());
                        glTextEasting.setBackgroundColor(0f, 0f, 0f, 0.6f);
                        glTextEasting.setRotateToAlign(false);
                        glTextEasting.setClampToGround(true);
                        glTextEasting.setSegmentPositionWeight(0f);
                        glTextEasting.setSegment(new GeoPoint[] {
                                sw, nw
                        });
                        glTextEasting.setInsets(0f, 0f, 0f, 0f);
                    }
                    text = mgrsRef.getEastingDescriptor();
                    if (subResolution == 10000) {
                        text = mgrsRef.getGridDescriptor();
                    }
                    GLText.localize(text);
                    glTextEasting.setText(text);
                    glTextEasting.setSegmentPositionWeight(we);
                    glTextEasting.draw(ortho);
                }
                if (northing) {
                    if (glTextNorthing == null) {
                        glTextNorthing = new GLSegmentFloatingLabel();
                        glTextNorthing
                                .setTextFormat(MapView.getDefaultTextFormat());
                        glTextNorthing.setBackgroundColor(0f, 0f, 0f, 0.6f);
                        glTextNorthing.setRotateToAlign(false);
                        glTextNorthing.setClampToGround(true);
                        glTextNorthing.setSegmentPositionWeight(0f);
                        glTextNorthing.setSegment(new GeoPoint[] {
                                sw, se
                        });
                        glTextNorthing.setInsets(0f, 0f, 0f, 0f);
                    }
                    text = mgrsRef.getNorthingDescriptor();
                    if (subResolution == 10000) {
                        text = mgrsRef.getGridDescriptor();
                    }
                    text = GLText.localize(text);
                    glTextNorthing.setText(text);
                    glTextNorthing.setSegmentPositionWeight(wn);
                    glTextNorthing.draw(ortho);
                }
            }
        }
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES)
                && !ortho.multiPartPass) {
            // multi-pass is complete, cull any out of view
            dumpSubs(ortho.currentPass.renderPump);
        }
    }

    private void dumpSubs(int renderPump) {
        if (_subs != null) {
            boolean validSubs = false;
            // if self was touched during last pump,
            if (lastRenderPump == renderPump) {
                // iterate across all child tiles, dumping any that weren't
                // touched during the current pump
                for (GLGridTile[] sub : _subs) {
                    if (sub == null)
                        continue;
                    for (int j = 0; j < sub.length; j++) {
                        if (sub[j] == null)
                            continue;
                        validSubs |= sub[j].lastRenderPump == renderPump;
                        sub[j].dumpSubs(renderPump);
                    }
                }
            }
            // if there are no valid children left, clear the structure
            if (!validSubs) {
                _subs = null;
                if (_loadingSubs != null) {
                    _loadingSubs.cancel(true);
                    _loadingSubs = null;
                }
            }
        }
    }

    private void _startLoadingSubs(final GLMapSurface surface) {
        GeoBounds bounds = getBounds();
        final double south = bounds.getSouth();
        final double west = bounds.getWest();
        final double north = bounds.getNorth();
        final double east = bounds.getEast();
        final int subRes = subResolution;
        final MGRSPoint ref = mgrsRef;
        RunnableFuture<GLGridTile[][]> loadJob = new RunnableFuture<GLGridTile[][]>() {
            GLGridTile[][] result;
            Throwable executionException;
            AtomicBoolean cancelToken = new AtomicBoolean(false);

            @Override
            public boolean cancel(boolean b) {
                if (result != null)
                    return false;
                cancelToken.set(true);
                return true;
            }

            @Override
            public boolean isCancelled() {
                return cancelToken.get();
            }

            @Override
            public boolean isDone() {
                return (result != null) || (executionException != null)
                        || cancelToken.get();
            }

            @Override
            public GLGridTile[][] get()
                    throws ExecutionException, InterruptedException {
                try {
                    return get(0, TimeUnit.MILLISECONDS);
                } catch (TimeoutException t) {
                    throw new IllegalStateException(t);
                }
            }

            @Override
            public synchronized GLGridTile[][] get(long l, TimeUnit timeUnit)
                    throws ExecutionException, InterruptedException,
                    TimeoutException {
                if (!this.isDone()) {
                    this.wait(TimeUnit.MILLISECONDS.convert(l, timeUnit));
                    if (!this.isDone())
                        throw new TimeoutException();
                }
                if (executionException instanceof InterruptedException)
                    throw (InterruptedException) executionException;
                else if (executionException != null)
                    throw new ExecutionException(executionException);
                return result;
            }

            @Override
            public synchronized void run() {
                if (isDone())
                    return;
                try {
                    result = _genTileGrid(south, west, north, east, ref, subRes,
                            cancelToken,
                            _genGridTileExecutor, _segmentLabelsExecutor);
                } catch (Throwable t) {
                    executionException = t;
                } finally {
                    this.notifyAll();
                    final SurfaceRendererControl ctrl = surface.getGLMapView()
                            .getControl(SurfaceRendererControl.class);
                    if (ctrl != null) {
                        // mark the surface as dirty
                        ctrl.markDirty(
                                new Envelope(west, south, 0d, east, north, 0d),
                                false);
                    } else {
                        surface.requestRender();
                    }
                }
            }
        };
        if (_genGridTileExecutor == null)
            _genGridTileExecutor = surface.getBackgroundMathExecutor();
        _genGridTileExecutor.execute(loadJob);
        _loadingSubs = loadJob;
    }

    private boolean _drawSubs(GLMapSurface surface, GLMapView ortho, float red,
            float green,
            float blue, int renderPass) {

        boolean result = false;

        if (_subs == null && _loadingSubs == null) {
            _startLoadingSubs(surface);
        }

        if (_loadingSubs != null && _loadingSubs.isDone()) {
            try {
                _subs = _loadingSubs.get();
            } catch (Exception e) {
                Log.e(TAG, "error: ", e);
            }
            _loadingSubs = null;
        }

        if (_subs != null) {
            result = true;
            for (GLGridTile[] _sub : _subs) {
                for (GLGridTile t : _sub) {
                    if (t.inView(ortho)) {
                        t.drawOrtho(surface, ortho, red, green, blue,
                                renderPass);
                    }
                }
            }
        }

        return result;
    }

    private static GLGridTile[][] _genTileGrid(double south, double west,
            double north,
            double east, MGRSPoint mgrsRef, int subRes,
            AtomicBoolean cancelToken,
            Executor genGridTileExecutor, Executor segmentLabelExecutor) {
        ArrayList<GLGridTile[]> grid = new ArrayList<>();

        GLGridTile[] row;

        MutableMGRSPoint currRef = new MutableMGRSPoint(mgrsRef);
        currRef.alignMeters(subRes, subRes);

        for (int j = 0; j < 10; ++j) {
            if (cancelToken.get())
                break;
            row = _genTileRow(new MutableMGRSPoint(currRef), west, east, south,
                    north, subRes, cancelToken, genGridTileExecutor,
                    segmentLabelExecutor);
            if (row.length > 0) {
                grid.add(row);
            }
            currRef.offset(0, subRes);
            currRef.alignYMeters(subRes);
        }

        return cancelToken.get() ? null
                : grid.toArray(new GLGridTile[grid.size()][]);
    }

    private static GLGridTile[] _genTileRow(MutableMGRSPoint currRef,
            double west, double east, double south, double north,
            int subRes,
            AtomicBoolean cancelToken,
            Executor genGridTileExecutor, Executor segmentLabelExecutor) {

        ArrayList<GLGridTile> row = new ArrayList<>();
        double[] ll = {
                0d, 0d
        };

        GLGridTile p = null;
        for (int i = 0; i < 10; ++i) {
            if (cancelToken.get())
                break;
            GLGridTile t;
            t = new GLGridTile();
            t.subResolution = subRes / 10;

            t.mgrsRef = new MGRSPoint(currRef);
            if (p != null) {
                t.sw = p.se;
            } else {
                t.sw = _toGeo(currRef, ll);
            }

            currRef.offset(0, subRes);
            currRef.alignYMeters(subRes);

            if (p != null) {
                t.nw = p.ne;
            } else {
                t.nw = _toGeo(currRef, ll);
            }

            currRef.offset(subRes, 0);
            currRef.alignXMeters(subRes);
            t.ne = _toGeo(currRef, ll);

            currRef.offset(0, -subRes);

            t.se = _toGeo(currRef, ll);

            p = t;

            GLGridTile clipped = GLZoneRegion._clipTile(t, south, west, north,
                    east);

            if (clipped != null) {
                // fill in on background thread based on actual to reduce per-frame overhead
                clipped._genGridTileExecutor = genGridTileExecutor;
                clipped._segmentLabelsExecutor = segmentLabelExecutor;
                segmentLabelExecutor.execute(
                        new SegmentLabelResolver(clipped, cancelToken));

                row.add(clipped);
            }
        }
        return row.toArray(new GLGridTile[0]);
    }

    private static GeoPoint _toGeo(MGRSPoint mgrsPoint, double[] out) {
        double[] ll = mgrsPoint.toLatLng(out);
        return new GeoPoint(ll[0], ll[1]);
    }

    static GeoPoint getPointWithElevation(GeoPoint g) {
        return new GeoPoint(g.getLatitude(), g.getLongitude(), ElevationManager
                .getElevation(g.getLatitude(), g.getLongitude(), null));
    }

    /**
     * Resolves elevations for segment labels
     */
    final static class SegmentLabelResolver implements RunnableFuture<Boolean> {
        Boolean result = null;
        Throwable executionException;
        final AtomicBoolean _cancelToken;

        final WeakReference<GLGridTile> _tile;

        SegmentLabelResolver(GLGridTile tile, AtomicBoolean cancelToken) {
            _tile = new WeakReference<>(tile);
            _cancelToken = cancelToken;
        }

        @Override
        public synchronized void run() {
            if (this.isDone())
                return;
            try {
                final GLGridTile tile = _cancelToken.get() ? null : _tile.get();
                if (tile != null) {
                    tile.sw = GLGridTile.getPointWithElevation(tile.sw);
                    tile.se = GLGridTile.getPointWithElevation(tile.se);
                    tile.ne = GLGridTile.getPointWithElevation(tile.ne);
                    tile.nw = GLGridTile.getPointWithElevation(tile.nw);
                    tile.projSrid = -1;
                    tile.labelsEnabled = true;
                }
                result = (tile != null) ? Boolean.TRUE : Boolean.FALSE;
            } catch (Throwable t) {
                executionException = t;
            } finally {
                this.notifyAll();
            }
        }

        @Override
        public boolean cancel(boolean b) {
            if (result != null)
                return false;
            _cancelToken.set(true);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return _cancelToken.get();
        }

        @Override
        public boolean isDone() {
            return (result != null) || (executionException != null)
                    || _cancelToken.get();
        }

        @Override
        public Boolean get() throws ExecutionException, InterruptedException {
            try {
                return get(0, TimeUnit.MILLISECONDS);
            } catch (TimeoutException t) {
                throw new IllegalStateException(t);
            }
        }

        @Override
        public synchronized Boolean get(long l, TimeUnit timeUnit)
                throws ExecutionException, InterruptedException,
                TimeoutException {
            if (!this.isDone()) {
                this.wait(TimeUnit.MILLISECONDS.convert(l, timeUnit));
                if (!this.isDone())
                    throw new TimeoutException();
                if (executionException instanceof InterruptedException)
                    throw (InterruptedException) executionException;
                else if (executionException != null)
                    throw new ExecutionException(executionException);
            }
            return result;
        }
    }

    private static double screenSpanMeters;
    private static long lastDrawVersionS = -1;

    private int lastDrawVersionM;
    private PointD swp;
    private PointD sep;
    private PointD nwp;
    private int projSrid = -1;
    private float swx, swy;
    private float sex, sey;
    private float nwx, nwy;
    private boolean labelsEnabled;

    private GLGridTile[][] _subs; // 10x10, except near zone boundaries
    private Future<GLGridTile[][]> _loadingSubs;
    private final GLAntiAliasedLine _antiAliasedLineRenderer = new GLAntiAliasedLine();
    Executor _genGridTileExecutor;
    Executor _segmentLabelsExecutor = new DirectExecutorService();
}
