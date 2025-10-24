
package com.atakmap.android.maps.graphics;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.opengl.GLES20FixedPipeline;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

import gov.tak.api.engine.math.IMatrix;
import gov.tak.platform.commons.opengl.GLES30;

final class GLCrumbs implements GLMapRenderable2 {
    // crumb layout => 40 bytes/vertex
    //  float positionXYZ[3]    0...11
    //  ubyte color[4]          12..15
    //  ubyte rotation          16..
    //  ubyte drawLollipop      17..
    //  ubyte clampToGround     18..
    //  ubyte discard           19..
    //  float surfaceXYZ[3]     20..31
    //  ubyte color[4]          32..35
    //  ushort radius           36..37
    //  ubyte drawLollipop      38..
    //  ubyte discard           39..

    final static String CRUMB_VERTEX_SHADER = "#version 100\n" +
            "uniform mat4 uMVP;\n" +
            "uniform vec2 uViewportSize;\n" +
            "uniform vec3 uWorldRotation;\n" +
            "uniform float uClampToGround;\n" +
            "attribute vec3 aVertexCoords;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec3 aSurface;\n" +
            "attribute vec4 aColor;\n" +
            "attribute float aRotation;\n" +
            "attribute float aRadius;\n" +
            "attribute float aClampToGround;\n" +
            "attribute float aDiscard;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            // select surface vs 3D based on clamp-to-ground
            "  vec3 worldPos = mix(aPosition, aSurface, max(aClampToGround, uClampToGround));\n"
            +
            "  gl_Position = uMVP * vec4(worldPos.xyz, 1.0);\n" +
            // effect discard by pushing the position past the far plane
            "  gl_Position.w = mix(gl_Position.w, 0.0, aDiscard);\n" +
            // transform the instance data vertex, then translate by the world position in NDC
            "  vec2 vertexCoord = vec2(0.0);\n" +
            // rotate, z-axis (heading)
            "  float heading_rot = -1.0*radians(aRotation*360.0 - uWorldRotation.z);\n"
            +
            "  float sin_heading = sin(heading_rot);\n" +
            "  float cos_heading = cos(heading_rot);\n" +
            "  vertexCoord.x = (aVertexCoords.x*cos_heading)-(aVertexCoords.y*sin_heading);\n"
            +
            "  vertexCoord.y = (aVertexCoords.x*sin_heading)+(aVertexCoords.y*cos_heading);\n"
            +
            // rotate, x-axis (tilt)
            "  float tilt_rot = radians(uWorldRotation.x);\n" +
            "  float sin_tilt = sin(tilt_rot);\n" +
            "  float cos_tilt = cos(tilt_rot);\n" +
            "  vertexCoord.y = (vertexCoord.y*cos_tilt)-(aVertexCoords.z*sin_tilt);\n"
            +
            // scale
            "  vertexCoord *= aRadius*gl_Position.w/uViewportSize;\n" +
            // translate
            "  gl_Position.x += vertexCoord.x;\n" +
            "  gl_Position.y += vertexCoord.y;\n" +
            "  gl_PointSize = 4.0;\n" +
            "  vColor = aColor;\n" +
            "}\n";
    final static String LOLLIPOP_VERTEX_SHADER = "#version 100\n" +
            "uniform mat4 uMVP;\n" +
            "uniform float uDrawLollipop;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "attribute float aDrawLollipop;\n" +
            "attribute float aDiscard;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_Position = uMVP * vec4(aPosition.xyz, 1.0);\n" +
            // effect discard by pushing the position past the far plane
            "  gl_Position.w = mix(gl_Position.w, 0.0, aDiscard);\n" +
            // effect discard by pushing the position past the far plane
            "  gl_Position.w = mix(0.0, gl_Position.w, uDrawLollipop*aDrawLollipop);\n"
            +
            "  vColor = aColor;\n" +
            "}\n";
    final static String FRAGMENT_SHADER = "#version 100\n" +
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main(void) {\n" +
            "  gl_FragColor = vColor;\n" +
            "}\n";

    final static FloatBuffer _crumbInstanceGeometry = Unsafe.allocateDirect(9,
            FloatBuffer.class);
    static {
        _crumbInstanceGeometry.put(new float[] {
                0f, 1f, 0f,
                1f / 2f, -1f / 2f, 0f,
                -1f / 2f, -1f / 2f, 0f,
        });
        _crumbInstanceGeometry.flip();
    }
    final static int _bufferSize = 5120; // 5kb ~ 128 crumbs
    final static int _crumbVertexStride = 40;
    final static int _lollipopVertexStride = 20;
    final static int _crumbsPerBuffer = _bufferSize / _crumbVertexStride;

    final SortedMap<Integer, CrumbBuffer> _vertexBuffers = new TreeMap<>();
    int _totalBuffers;

    int _drawPump = -1;
    final CrumbShader _crumbShader;
    final LollipopShader _lollipopShader;
    boolean _lollipopsVisible;
    boolean _clampToGroundAtNadir;
    int _lastDrawSrid = 4978;

    GLCrumbs() {
        _crumbShader = new CrumbShader();
        _lollipopShader = new LollipopShader();
        _vertexBuffers.put(0,
                new CrumbBuffer(_bufferSize, new PointD(), 0, _lastDrawSrid));
        _totalBuffers = 1;
        _lollipopsVisible = true;
        _clampToGroundAtNadir = false;
    }

    void setLollipopsVisible(boolean v) {
        _lollipopsVisible = v;
    }

    void setClampToGroundAtNadir(boolean v) {
        _clampToGroundAtNadir = v;
    }

    synchronized int addCrumb(GeoPoint location, double rotation, double radius,
            int color, boolean drawLollipop, boolean clampToGround) {
        CrumbBuffer crumbBuffer = _vertexBuffers.get(_vertexBuffers.lastKey());
        if (crumbBuffer == null)
            return -1;

        if (crumbBuffer.data.remaining() < _crumbVertexStride) {
            // XXX - recompute relative to center when buffer is flushed???
            crumbBuffer = new CrumbBuffer(_bufferSize, new PointD(0d, 0d, 0d),
                    _totalBuffers * _crumbsPerBuffer, _lastDrawSrid);
            crumbBuffer.proj.forward(location, crumbBuffer.relativeToCenter);
            _vertexBuffers.put(_totalBuffers, crumbBuffer);
            _totalBuffers++;
        }
        if (crumbBuffer.numCrumbs == 0) {
            crumbBuffer.proj.forward(location, crumbBuffer.relativeToCenter);
        }
        final int id = crumbBuffer.firstCrumbId
                + crumbBuffer.data.position() / _crumbVertexStride;
        writeCrumb(crumbBuffer, crumbBuffer.data.position(), location, rotation,
                radius, color, drawLollipop, clampToGround);
        crumbBuffer.data
                .position(crumbBuffer.data.position() + _crumbVertexStride);
        crumbBuffer.numCrumbs++;
        return id;
    }

    synchronized void removeCrumb(int id) {
        final CrumbBuffer buffer = getCrumbBuffer(id);
        if (buffer == null)
            return;
        final int pos = (id % _crumbsPerBuffer) * _crumbVertexStride;
        if (buffer.data.get(pos + 36) == 0x0) {
            buffer.numCrumbs--;
            buffer.data.put(pos + 16, (byte) 0x1);
            buffer.data.put(pos + 36, (byte) 0x1);
            // no crumbs in buffer, evict
            if (buffer.numCrumbs < 1 && ((buffer.firstCrumbId
                    / _crumbsPerBuffer) != _vertexBuffers.lastKey())) {
                _vertexBuffers.remove(buffer.firstCrumbId / _crumbsPerBuffer);
                Unsafe.free(buffer.data);
            }
        }
    }

    synchronized int updateCrumb(int id, GeoPoint location, double rotation,
            double radius, int color, boolean drawLollipop,
            boolean clampToGround) {
        final CrumbBuffer buffer = getCrumbBuffer(id);
        if (buffer == null) {
            // the crumb was evicted, add a new record
            id = addCrumb(location, rotation, radius, color, drawLollipop,
                    clampToGround);
        } else {
            writeCrumb(buffer, (id % _crumbsPerBuffer) * _crumbVertexStride,
                    location, rotation, radius, color, drawLollipop,
                    clampToGround);
        }
        return id;
    }

    CrumbBuffer getCrumbBuffer(int id) {
        if (id < 0)
            return null;
        return _vertexBuffers.get(id / _crumbsPerBuffer);
    }

    void writeCrumb(CrumbBuffer buffer, int pos, GeoPoint location,
            double rotation, double radius, int color, boolean drawLollipop,
            boolean clampToGround) {
        // crumb layout => 40 bytes/vertex
        //  float positionXYZ[3]    0...11
        //  ubyte color[4]          12..15
        //  ubyte discard           16..
        //  ubyte drawLollipop      17..
        //  ubyte rotation          18..
        //  ubyte clampToGround     19..
        //  float surfaceXYZ[3]     20..31
        //  ubyte color[4]          32..35
        //  ubyte discard           36..
        //  ubyte drawLollipop      37..
        //  ushort radius           38..39

        ByteBuffer buf = buffer.data.duplicate();
        buf.order(buffer.data.order()); // ByteBuffer.duplicate() does NOT preserve order
        buf.position(pos);

        // micro-optimize against pulling terrain more than necessary
        double surfaceEl = ElevationManager.getElevation(location.getLatitude(),
                location.getLongitude(), null);
        if (Double.isNaN(surfaceEl))
            surfaceEl = 0d;
        final double pointEl = Double.isNaN(location.getAltitude())
                || location.getAltitude() < surfaceEl ? surfaceEl
                        : location.getAltitude();

        PointD ecef = new PointD(0d, 0d, 0d);
        buffer.proj.forward(new GeoPoint(location.getLatitude(),
                location.getLongitude(), pointEl), ecef);
        buf.putFloat((float) (ecef.x - buffer.relativeToCenter.x));
        buf.putFloat((float) (ecef.y - buffer.relativeToCenter.y));
        buf.putFloat((float) (ecef.z - buffer.relativeToCenter.z));
        buf.put((byte) ((color >> 16) & 0xFF));
        buf.put((byte) ((color >> 8) & 0xFF));
        buf.put((byte) (color & 0xFF));
        buf.put((byte) ((color >> 24) & 0xFF));
        buf.put((byte) 0x0); // discard
        buf.put(drawLollipop ? (byte) 0x1 : (byte) 0x0);
        if (rotation < 0)
            rotation += 360d;
        else if (rotation > 360d)
            rotation -= 360d;
        buf.put((byte) ((rotation / 360d) * 0xFF));
        buf.put(clampToGround ? (byte) 0x1 : (byte) 0x0);

        buffer.proj.forward(new GeoPoint(location.getLatitude(),
                location.getLongitude(), surfaceEl), ecef);
        buf.putFloat((float) (ecef.x - buffer.relativeToCenter.x));
        buf.putFloat((float) (ecef.y - buffer.relativeToCenter.y));
        buf.putFloat((float) (ecef.z - buffer.relativeToCenter.z));
        buf.put((byte) ((color >> 16) & 0xFF));
        buf.put((byte) ((color >> 8) & 0xFF));
        buf.put((byte) (color & 0xFF));
        buf.put((byte) ((color >> 24) & 0xFF));
        buf.put((byte) 0x0); // discard
        buf.put(drawLollipop ? (byte) 0x1 : (byte) 0x0);
        buf.putShort((short) radius);
    }

    @Override
    public synchronized void draw(GLMapView view, int renderPass) {
        if (!MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
            return;
        // already rendered
        if (_drawPump == view.currentPass.renderPump)
            return;
        _drawPump = view.currentPass.renderPump;
        _lastDrawSrid = view.currentPass.drawSrid;
        if (_vertexBuffers.isEmpty())
            return; // nothing to draw

        drawCrumbs(view);
        if (_lollipopsVisible)
            drawLollipops(view);
    }

    private void drawCrumbs(GLMapView view) {
        // init shader if needed
        if (_crumbShader.handle == GLES30.GL_NONE) {
            GLES20FixedPipeline.Program p = new GLES20FixedPipeline.Program();
            p.create(CRUMB_VERTEX_SHADER, FRAGMENT_SHADER);
            GLES30.glDeleteShader(p.vertShader);
            GLES30.glDeleteShader(p.fragShader);

            _crumbShader.handle = p.program;
            _crumbShader.uMVP = GLES30.glGetUniformLocation(_crumbShader.handle,
                    "uMVP");
            _crumbShader.uViewportSize = GLES30
                    .glGetUniformLocation(_crumbShader.handle, "uViewportSize");
            _crumbShader.uWorldRotation = GLES30.glGetUniformLocation(
                    _crumbShader.handle, "uWorldRotation");
            _crumbShader.uClampToGround = GLES30.glGetUniformLocation(
                    _crumbShader.handle, "uClampToGround");
            _crumbShader.aVertexCoords = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aVertexCoords");
            _crumbShader.aPosition = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aPosition");
            _crumbShader.aSurface = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aSurface");
            _crumbShader.aColor = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aColor");
            _crumbShader.aRotation = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aRotation");
            _crumbShader.aRadius = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aRadius");
            _crumbShader.aClampToGround = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aClampToGround");
            _crumbShader.aDiscard = GLES30
                    .glGetAttribLocation(_crumbShader.handle, "aDiscard");
        }

        GLES30.glUseProgram(_crumbShader.handle);

        // viewport size
        {
            int[] viewport = new int[4];
            GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, viewport, 0);
            GLES30.glUniform2f(_crumbShader.uViewportSize,
                    (float) viewport[2] / 2.0f, (float) viewport[3] / 2.0f);
        }
        // world rotation
        GLES30.glUniform3f(_crumbShader.uWorldRotation,
                (float) view.currentPass.drawTilt, 0f,
                (float) view.currentPass.drawRotation);
        final boolean enhancedDepthPerception = (!_clampToGroundAtNadir
                || view.currentPass.drawTilt != 0d);
        GLES30.glUniform1f(_crumbShader.uClampToGround,
                !enhancedDepthPerception ? 1f : 0f);

        GLES30.glEnableVertexAttribArray(_crumbShader.aVertexCoords);
        GLES30.glEnableVertexAttribArray(_crumbShader.aPosition);
        GLES30.glEnableVertexAttribArray(_crumbShader.aSurface);
        GLES30.glEnableVertexAttribArray(_crumbShader.aColor);
        GLES30.glEnableVertexAttribArray(_crumbShader.aRotation);
        GLES30.glEnableVertexAttribArray(_crumbShader.aRadius);
        GLES30.glEnableVertexAttribArray(_crumbShader.aClampToGround);
        GLES30.glEnableVertexAttribArray(_crumbShader.aDiscard);

        // instance geometry
        GLES30.glVertexAttribDivisor(_crumbShader.aVertexCoords, 0);
        // per-instance attributes
        GLES30.glVertexAttribDivisor(_crumbShader.aPosition, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aSurface, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aColor, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aRotation, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aRadius, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aClampToGround, 1);
        GLES30.glVertexAttribDivisor(_crumbShader.aDiscard, 1);

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        GLES30.glVertexAttribPointer(_crumbShader.aVertexCoords, 3,
                GLES30.GL_FLOAT, false, 12, _crumbInstanceGeometry);

        for (CrumbBuffer buffer : _vertexBuffers.values()) {
            buffer.reproject(view.currentPass.drawSrid);

            // configure uniforms
            view.scratch.matrix.set(view.currentPass.scene.camera.projection);
            view.scratch.matrix
                    .concatenate(view.currentPass.scene.camera.modelView);
            view.scratch.matrix.translate(buffer.relativeToCenter.x,
                    buffer.relativeToCenter.y, buffer.relativeToCenter.z);

            view.scratch.matrix.get(view.scratch.matrixD,
                    IMatrix.MatrixOrder.COLUMN_MAJOR);
            for (int i = 0; i < 16; i++)
                view.scratch.matrixF[i] = (float) view.scratch.matrixD[i];
            GLES30.glUniformMatrix4fv(_crumbShader.uMVP, 1, false,
                    view.scratch.matrixF, 0);

            Buffer vb = buffer.data.duplicate();

            // crumb layout => 40 bytes/vertex
            //  float positionXYZ[3]    0...11
            //  ubyte color[4]          12..15
            //  ubyte discard           16..
            //  ubyte drawLollipop      17..
            //  ubyte rotation          18..
            //  ubyte clampToGround     19..
            //  float surfaceXYZ[3]     20..31
            //  ubyte color[4]          32..35
            //  ubyte discard           36..
            //  ubyte drawLollipop      37..
            //  ushort radius           38..39

            GLES30.glVertexAttribPointer(_crumbShader.aPosition, 3,
                    GLES30.GL_FLOAT, false, _crumbVertexStride, vb.position(0));
            GLES30.glVertexAttribPointer(_crumbShader.aColor, 4,
                    GLES30.GL_UNSIGNED_BYTE, true, _crumbVertexStride,
                    vb.position(12));
            GLES30.glVertexAttribPointer(_crumbShader.aDiscard, 1,
                    GLES30.GL_UNSIGNED_BYTE, false, _crumbVertexStride,
                    vb.position(16));
            GLES30.glVertexAttribPointer(_crumbShader.aRotation, 1,
                    GLES30.GL_UNSIGNED_BYTE, true, _crumbVertexStride,
                    vb.position(18));
            GLES30.glVertexAttribPointer(_crumbShader.aClampToGround, 1,
                    GLES30.GL_UNSIGNED_BYTE, false, _crumbVertexStride,
                    vb.position(19));
            GLES30.glVertexAttribPointer(_crumbShader.aSurface, 3,
                    GLES30.GL_FLOAT, false, _crumbVertexStride,
                    vb.position(20));
            GLES30.glVertexAttribPointer(_crumbShader.aRadius, 1,
                    GLES30.GL_UNSIGNED_SHORT, false, _crumbVertexStride,
                    vb.position(38));

            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 3,
                    buffer.data.position() / _crumbVertexStride);
        }

        GLES30.glVertexAttribDivisor(_crumbShader.aVertexCoords, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aPosition, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aSurface, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aColor, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aRotation, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aRadius, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aClampToGround, 0);
        GLES30.glVertexAttribDivisor(_crumbShader.aDiscard, 0);

        GLES30.glDisableVertexAttribArray(_crumbShader.aVertexCoords);
        GLES30.glDisableVertexAttribArray(_crumbShader.aPosition);
        GLES30.glDisableVertexAttribArray(_crumbShader.aSurface);
        GLES30.glDisableVertexAttribArray(_crumbShader.aColor);
        GLES30.glDisableVertexAttribArray(_crumbShader.aRotation);
        GLES30.glDisableVertexAttribArray(_crumbShader.aRadius);
        GLES30.glDisableVertexAttribArray(_crumbShader.aClampToGround);
        GLES30.glDisableVertexAttribArray(_crumbShader.aDiscard);
    }

    private void drawLollipops(GLMapView view) {
        // init shader if needed
        if (_lollipopShader.handle == GLES30.GL_NONE) {
            GLES20FixedPipeline.Program p = new GLES20FixedPipeline.Program();
            p.create(LOLLIPOP_VERTEX_SHADER, FRAGMENT_SHADER);
            GLES30.glDeleteShader(p.vertShader);
            GLES30.glDeleteShader(p.fragShader);

            _lollipopShader.handle = p.program;
            _lollipopShader.uMVP = GLES30
                    .glGetUniformLocation(_lollipopShader.handle, "uMVP");
            _lollipopShader.uDrawLollipop = GLES30.glGetUniformLocation(
                    _lollipopShader.handle, "uDrawLollipop");
            _lollipopShader.aPosition = GLES30
                    .glGetAttribLocation(_lollipopShader.handle, "aPosition");
            _lollipopShader.aColor = GLES30
                    .glGetAttribLocation(_lollipopShader.handle, "aColor");
            _lollipopShader.aDrawLollipop = GLES30.glGetAttribLocation(
                    _lollipopShader.handle, "aDrawLollipop");
            _lollipopShader.aDiscard = GLES30
                    .glGetAttribLocation(_lollipopShader.handle, "aDiscard");
        }

        GLES30.glUseProgram(_lollipopShader.handle);

        final boolean enhancedDepthPerception = (!_clampToGroundAtNadir
                || view.currentPass.drawTilt == 0d);
        GLES30.glUniform1f(_lollipopShader.uDrawLollipop,
                enhancedDepthPerception && _lollipopsVisible ? 1f : 0f);

        GLES30.glEnableVertexAttribArray(_lollipopShader.aPosition);
        GLES30.glEnableVertexAttribArray(_lollipopShader.aColor);
        GLES30.glEnableVertexAttribArray(_lollipopShader.aDrawLollipop);
        GLES30.glEnableVertexAttribArray(_lollipopShader.aDiscard);

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);

        for (CrumbBuffer buffer : _vertexBuffers.values()) {
            buffer.reproject(view.currentPass.drawSrid);

            // configure uniforms
            view.scratch.matrix.set(view.currentPass.scene.camera.projection);
            view.scratch.matrix
                    .concatenate(view.currentPass.scene.camera.modelView);
            view.scratch.matrix.translate(buffer.relativeToCenter.x,
                    buffer.relativeToCenter.y, buffer.relativeToCenter.z);

            view.scratch.matrix.get(view.scratch.matrixD,
                    IMatrix.MatrixOrder.COLUMN_MAJOR);
            for (int i = 0; i < 16; i++)
                view.scratch.matrixF[i] = (float) view.scratch.matrixD[i];
            GLES30.glUniformMatrix4fv(_lollipopShader.uMVP, 1, false,
                    view.scratch.matrixF, 0);

            Buffer vb = buffer.data.duplicate();

            // crumb layout => 40 bytes/vertex
            //  float positionXYZ[3]    0...11
            //  ubyte color[4]          12..15
            //  ubyte discard           16..
            //  ubyte drawLollipop      17..
            //  ubyte rotation          18..
            //  ubyte clampToGround     19..
            //  float surfaceXYZ[3]     20..31
            //  ubyte color[4]          32..35
            //  ubyte discard           36..
            //  ubyte drawLollipop      37..
            //  ushort radius           38..39

            GLES30.glVertexAttribPointer(_lollipopShader.aPosition, 3,
                    GLES30.GL_FLOAT, false, _lollipopVertexStride,
                    vb.position(0));
            GLES30.glVertexAttribPointer(_lollipopShader.aColor, 4,
                    GLES30.GL_UNSIGNED_BYTE, true, _lollipopVertexStride,
                    vb.position(12));
            GLES30.glVertexAttribPointer(_lollipopShader.aDiscard, 1,
                    GLES30.GL_UNSIGNED_BYTE, false, _lollipopVertexStride,
                    vb.position(16));
            GLES30.glVertexAttribPointer(_lollipopShader.aDrawLollipop, 1,
                    GLES30.GL_UNSIGNED_BYTE, false, _lollipopVertexStride,
                    vb.position(17));

            GLES30.glDrawArrays(GLES30.GL_LINES, 0,
                    buffer.data.position() / _lollipopVertexStride);
        }

        GLES30.glDisableVertexAttribArray(_lollipopShader.aPosition);
        GLES30.glDisableVertexAttribArray(_lollipopShader.aColor);
        GLES30.glDisableVertexAttribArray(_lollipopShader.aDiscard);
        GLES30.glDisableVertexAttribArray(_lollipopShader.aDrawLollipop);
    }

    @Override
    public void release() {
        for (CrumbBuffer buffer : _vertexBuffers.values())
            Unsafe.free(buffer.data);
        _vertexBuffers.clear();

        if (_crumbShader.handle != GLES30.GL_NONE) {
            GLES30.glDeleteProgram(_crumbShader.handle);
            _crumbShader.handle = GLES30.GL_NONE;
        }
        if (_lollipopShader.handle != GLES30.GL_NONE) {
            GLES30.glDeleteProgram(_lollipopShader.handle);
            _lollipopShader.handle = GLES30.GL_NONE;
        }
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_SPRITES;
    }

    final static class CrumbBuffer {
        final ByteBuffer data;
        final PointD relativeToCenter;
        int numCrumbs;
        final int firstCrumbId;
        Projection proj;
        private int _srid;

        CrumbBuffer(int capacity, PointD relativeToCenter, int firstCrumbId,
                int srid) {
            data = Unsafe.allocateDirect(capacity, ByteBuffer.class);
            this.relativeToCenter = new PointD(relativeToCenter);
            this.numCrumbs = 0;
            this.firstCrumbId = firstCrumbId;
            _srid = srid;
            this.proj = ProjectionFactory.getProjection(srid);
        }

        void reproject(int srid) {
            if (srid == _srid)
                return;
            Projection p2 = ProjectionFactory.getProjection(srid);
            final double ortcx = relativeToCenter.x;
            final double ortcy = relativeToCenter.y;
            final double ortcz = relativeToCenter.z;
            GeoPoint lla = GeoPoint.createMutable();
            proj.inverse(relativeToCenter, lla);
            p2.forward(lla, relativeToCenter);
            PointD xyz = new PointD(0d, 0d, 0d);
            for (int i = 0; i < _crumbsPerBuffer; i++) {
                xyz.x = data.getFloat((i * _crumbVertexStride)) + ortcx;
                xyz.y = data.getFloat((i * _crumbVertexStride) + 4) + ortcy;
                xyz.z = data.getFloat((i * _crumbVertexStride) + 8) + ortcz;
                proj.inverse(xyz, lla);
                p2.forward(lla, xyz);
                data.putFloat((i * _crumbVertexStride),
                        (float) (xyz.x - relativeToCenter.x));
                data.putFloat((i * _crumbVertexStride) + 4,
                        (float) (xyz.y - relativeToCenter.y));
                data.putFloat((i * _crumbVertexStride) + 8,
                        (float) (xyz.z - relativeToCenter.z));

                xyz.x = data.getFloat((i * _crumbVertexStride) + 20) + ortcx;
                xyz.y = data.getFloat((i * _crumbVertexStride) + 24) + ortcy;
                xyz.z = data.getFloat((i * _crumbVertexStride) + 28) + ortcz;
                proj.inverse(xyz, lla);
                p2.forward(lla, xyz);
                data.putFloat((i * _crumbVertexStride) + 20,
                        (float) (xyz.x - relativeToCenter.x));
                data.putFloat((i * _crumbVertexStride) + 24,
                        (float) (xyz.y - relativeToCenter.y));
                data.putFloat((i * _crumbVertexStride) + 28,
                        (float) (xyz.z - relativeToCenter.z));
            }
            proj = p2;
            _srid = srid;
        }
    }

    final static class CrumbShader {
        int handle = GLES30.GL_NONE;
        int uMVP = -1;
        int uViewportSize = -1;
        int uWorldRotation = -1;
        int uClampToGround = -1;
        int aVertexCoords = -1;
        int aPosition = -1;
        int aSurface = -1;
        int aColor = -1;
        int aRotation = -1;
        int aRadius = -1;
        int aClampToGround = -1;
        int aDiscard = -1;
    }

    final static class LollipopShader {
        int handle = GLES30.GL_NONE;
        int uMVP = -1;
        int uDrawLollipop = -1;
        int aPosition = -1;
        int aColor = -1;
        int aDrawLollipop = -1;
        int aDiscard = -1;
    }
}
