package com.atakmap.map.opengl;

import gov.tak.platform.commons.opengl.GLES30;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.interop.Interop;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.math.Matrix;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.util.ConfigOptions;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import gov.tak.api.engine.map.RenderContext;

/**
 * Provides a method for drawing anti aliased lines using the techniques described in
 * https://blog.mapbox.com/drawing-antialiased-lines-with-opengl-8766f34192dc
 */
public class GLAntiAliasedLine
{
    private final static Interop<RenderContext> RenderContext_interop = Interop.findInterop(RenderContext.class);
    public static final String TAG = "GLAntiAliasedLine";

    private static Buffer segmentGeom = null;

    private static boolean isMpu = false;

    public enum ConnectionType
    {
        // Causes the last point to be connected to the first point
        FORCE_CLOSE,
        // Draws the line as-is based on the given vertex data (line strip)
        AS_IS,
        // Draws line segments instead of a line strip
        SEGMENTS
    }

    /**
     * source coordinates, LLA (longitude, latitude, altitude)
     */
    private DoubleBuffer _lineStrip;
    private Feature.AltitudeMode _altMode;
    private ConnectionType _connType = ConnectionType.AS_IS;
    /**
     * source coordinates, current map projection
     */
    private FloatBuffer _forwardSegmentVerts;
    private ByteBuffer _normals;
    private ShortBuffer _pattern;
    private int _forwardSegmentsSrid;
    private int _forwardSegmentsTerrainVersion;

    // geometry centroid latitude,longitude
    private double _centroidLat;
    private double _centroidLng;
    // relative-to-center offsets in map projection, considered valid when
    // `_forwardSegmentsSrid == GLMapView.drawSrid`; recompute for current
    // projection otherwise
    private double _rtcX;
    private double _rtcY;
    private double _rtcZ;

    // records information for IDL crossing
    private boolean _crossesIDL;
    private int _primaryHemi;

    public GLAntiAliasedLine()
    {
        _forwardSegmentsSrid = -1;
        _forwardSegmentsTerrainVersion = -1;

        _rtcX = 0d;
        _rtcY = 0d;
        _rtcZ = 0d;

        _crossesIDL = false;

        _altMode = Feature.AltitudeMode.Absolute;
    }

    /**
     * Set the vertex data for the line that will be drawn.
     *
     * @param verts          The GeoPoints representing the line.
     * @param componentCount The number of components that each point contains, should be 2 or 3.
     * @param type           Specifies how the line's endpoints should be connected.
     */
    public void setLineData(GeoPoint[] verts, int componentCount, ConnectionType type)
    {
        double[] tmp = new double[verts.length * 3];
        for (int i = 0; i < verts.length; i++)
        {
            tmp[i * 3] = verts[i].getLongitude();
            tmp[i * 3 + 1] = verts[i].getLatitude();
            tmp[i * 3 + 2] = 0d;
            if (componentCount == 3 && !Double.isNaN(verts[i].getAltitude()))
                tmp[i * 3 + 2] = verts[i].getAltitude();
        }
        setLineData(DoubleBuffer.wrap(tmp), 3, type, Feature.AltitudeMode.Absolute);
    }

    /**
     * Set the vertex data for the line that will be drawn.
     *
     * @param verts          The points representing the line.
     * @param componentCount The number of components that each point contains, should be 2 or 3.
     * @param type           Specifies how the line's endpoints should be connected.
     */
    public void setLineData(FloatBuffer verts, int componentCount, ConnectionType type)
    {
        double[] tmp = new double[verts.limit()];
        for (int i = 0; i < verts.limit(); i++)
        {
            tmp[i] = verts.get(i);
        }
        setLineData(DoubleBuffer.wrap(tmp), componentCount, type, Feature.AltitudeMode.Absolute);
    }

    /**
     * Set the vertex data for the line that will be drawn.
     *
     * @param verts          The points representing the line.
     * @param componentCount The number of components that each point contains, should be 2 or 3.
     * @param type           Specifies how the line's endpoints should be connected.
     */
    public void setLineData(DoubleBuffer verts, int componentCount, ConnectionType type, Feature.AltitudeMode altMode)
    {
        _altMode = altMode;
        _connType = type;
        _crossesIDL = false;
        _primaryHemi = 0;

        int capacity = verts.limit(); // > 0 ? verts.limit() : verts.capacity();
        if (capacity <= 3)
            return;

        int numPoints = capacity / componentCount;
        if (type == ConnectionType.FORCE_CLOSE)
            numPoints++;

        // capture surface MBB
        double minX = verts.get(0);
        double minY = verts.get(1);
        double maxX = minX;
        double maxY = minY;

        _allocateBuffers(numPoints);
        // prepare for write
        _lineStrip.rewind();
        for (int currVert = 0; currVert < numPoints * componentCount; currVert += componentCount)
        {
            final int pos = currVert % capacity;

            // start of line segment
            final double ax = verts.get(pos);
            final double ay = verts.get(pos + 1);
            final double az = componentCount == 3 ? verts.get(pos + 2) : 0d;

            _lineStrip.put(ax);
            _lineStrip.put(ay);
            _lineStrip.put(Double.isNaN(az) ? 0d : az);

            // update MBB
            final double x = ax;
            final double y = ay;
            if (x < minX)
                minX = x;
            else if (x > maxX)
                maxX = x;
            if (y < minY)
                minY = y;
            else if (y > maxY)
                maxY = y;
        }
        _lineStrip.flip();
        _forwardSegmentsSrid = -1;

        // update RTC
        _centroidLng = (minX + maxX) / 2d;
        _centroidLat = (minY + maxY) / 2d;

        final int idlInfo = GLAntiMeridianHelper.normalizeHemisphere(componentCount, _lineStrip, _lineStrip);
        _lineStrip.flip();

        _primaryHemi = (idlInfo & GLAntiMeridianHelper.MASK_PRIMARY_HEMISPHERE);
        _crossesIDL = (idlInfo & GLAntiMeridianHelper.MASK_IDL_CROSS) != 0;
    }

    /**
     * Draws the antialiased line specified by a previous call to setLineData().
     *
     * @param view  The GLMapView used for rendering.
     * @param red   The red component of the color that the line will be drawn with.
     * @param green The green component of the color that the line will be drawn with.
     * @param blue  The blue component of the color that the line will be drawn with.
     * @param width The width of the line to be drawn.
     */
    public void draw(GLMapView view, float red, float green, float blue,
                     float alpha, float width)
    {
        draw(view, 1, (short) 0xFFFF, red, green, blue, alpha, width);
    }

    /**
     * Draws the antialiased line specified by a previous call to setLineData().
     *
     * @param view         The GLMapView used for rendering.
     * @param red          The red component of the color that the line will be drawn with.
     * @param green        The green component of the color that the line will be drawn with.
     * @param blue         The blue component of the color that the line will be drawn with.
     * @param width        The width of the line to be drawn.
     * @param outlineRed   The red component of the outline color
     * @param outlineGreen The green component of the outline color
     * @param outlineBlue  The blue component of the outline color
     * @param outlineAlpha The alpha component of the outline color
     * @param outlineWidth The width of the outline, in pixels. If <code>0f</code>,
     *                     no outline is applied
     */
    public void draw(GLMapView view, float red, float green, float blue,
                     float alpha, float width, float outlineRed, float outlineGreen, float outlineBlue, float outlineAlpha, float outlineWidth)
    {
        draw(view, 1, (short) 0xFFFF, red, green, blue, alpha, width, outlineRed, outlineGreen, outlineBlue, outlineAlpha, outlineWidth);
    }

    /**
     * Draws the antialiased line specified by a previous call to
     * setLineData(), applying the specified pattern to the line.
     *
     * @param view    The GLMapView used for rendering.
     * @param factor  The number of pixels to be drawn for each pattern bit
     * @param pattern The bitmask pattern. Interpreted least-significant bit
     *                first. Each bit that is toggled will be colored per
     *                the specified <code>red</code>, <code>green</code>,
     *                <code>blue</code> and <code>alpha</code>; bits that are
     *                not toggled will be output as transparent.
     * @param red     The red component of the color that the line will be drawn with.
     * @param green   The green component of the color that the line will be drawn with.
     * @param blue    The blue component of the color that the line will be drawn with.
     * @param width   The width of the line to be drawn.
     */
    public void draw(GLMapView view, int factor, short pattern, float red, float green, float blue, float alpha, float width)
    {
        draw(view, factor, pattern, red, green, blue, alpha, width, 0f, 0f, 0f, 0f, 0f);
    }

    /**
     * Draws the antialiased line specified by a previous call to
     * setLineData(), applying the specified pattern to the line, with an optional outline.
     *
     * @param view         The GLMapView used for rendering.
     * @param factor       The number of pixels to be drawn for each pattern bit
     * @param pattern      The bitmask pattern. Interpreted least-significant bit
     *                     first. Each bit that is toggled will be colored per
     *                     the specified <code>red</code>, <code>green</code>,
     *                     <code>blue</code> and <code>alpha</code>; bits that are
     *                     not toggled will be output as transparent.
     * @param red          The red component of the color that the line will be drawn with.
     * @param green        The green component of the color that the line will be drawn with.
     * @param blue         The blue component of the color that the line will be drawn with.
     * @param width        The width of the line to be drawn.
     * @param outlineRed   The red component of the outline color
     * @param outlineGreen The green component of the outline color
     * @param outlineBlue  The blue component of the outline color
     * @param outlineAlpha The alpha component of the outline color
     * @param outlineWidth The width of the outline, in pixels. If <code>0f</code>,
     *                     no outline is applied
     */
    public void draw(GLMapView view, int factor, short pattern, float red, float green, float blue, float alpha, float width, float outlineRed, float outlineGreen, float outlineBlue, float outlineAlpha, float outlineWidth)
    {
        if (_forwardSegmentVerts == null)
            return;

        final boolean depthEnabled = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
        final short acap = (depthEnabled && _lineStrip.limit() > 6) ? (short)0 : (short)1;

        AntiAliasingProgram program = AntiAliasingProgram.get();
        // if the projection has changed or the geometry is relative to terrain
        // and the terrain has changed, we need to refresh
        final int terrainVersion = view.getTerrainVersion();
        if (_forwardSegmentsSrid != view.currentPass.drawSrid ||
                (_altMode == Feature.AltitudeMode.Relative && _forwardSegmentsTerrainVersion != terrainVersion))
        {

            view.scratch.geo.set(_centroidLat, _centroidLng, 0d);
            view.currentPass.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
            _rtcX = view.scratch.pointD.x;
            _rtcY = view.scratch.pointD.y;
            _rtcZ = view.scratch.pointD.z;



            // prepare to fill the buffers
            _forwardSegmentVerts.clear();
            _normals.clear();
            for (int i = 0; i < (_lineStrip.limit() / 3) - 1; i++)
            {
                // obtain the source position as LLA
                final double lat0 = _lineStrip.get(i * 3 + 1);
                final double lng0 = _lineStrip.get(i * 3);
                double alt0 = _lineStrip.get(i * 3 + 2);
                if (_altMode == Feature.AltitudeMode.Relative)
                {
                    final double terrainEl = view.getTerrainMeshElevation(lat0, lng0);
                    if (!Double.isNaN(terrainEl))
                        alt0 += terrainEl;
                }
                view.scratch.geo.set(lat0, lng0, alt0);
                // transform to the current map projection
                view.currentPass.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);

                // set vertex position as map projection coordinate, relative to center
                final float ax = (float) (view.scratch.pointD.x - _rtcX);
                final float ay = (float) (view.scratch.pointD.y - _rtcY);
                final float az = (float) (view.scratch.pointD.z - _rtcZ);

                // obtain the source position as LLA
                final double lat1 = _lineStrip.get((i + 1) * 3 + 1);
                final double lng1 = _lineStrip.get((i + 1) * 3);
                double alt1 = _lineStrip.get((i + 1) * 3 + 2);
                if (_altMode == Feature.AltitudeMode.Relative)
                {
                    final double terrainEl = view.getTerrainMeshElevation(lat1, lng1);
                    if (!Double.isNaN(terrainEl))
                        alt1 += terrainEl;
                }
                view.scratch.geo.set(lat1, lng1, alt1);
                // transform to the current map projection
                view.currentPass.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);

                // set vertex position as map projection coordinate, relative to center
                final float bx = (float) (view.scratch.pointD.x - _rtcX);
                final float by = (float) (view.scratch.pointD.y - _rtcY);
                final float bz = (float) (view.scratch.pointD.z - _rtcZ);

                final short bcap =
                    ((_connType == ConnectionType.SEGMENTS) || // cap if lines
                            (i == (_lineStrip.limit() / 3) - 2)) ?  // cap if last segment
                            (short)1 : (short)-1;

                // emit the per-instance attribute data
                _forwardSegmentVerts.put(ax);
                _forwardSegmentVerts.put(ay);
                _forwardSegmentVerts.put(az);
                _forwardSegmentVerts.put(bx);
                _forwardSegmentVerts.put(by);
                _forwardSegmentVerts.put(bz);
                _normals.putShort((short)acap); // cap
                _normals.putShort((short)(acap*bcap));

                // Skip to next segment
                if (_connType == ConnectionType.SEGMENTS)
                    i++;
            }
            // prepare the buffers for draw
            _forwardSegmentVerts.flip();
            _normals.flip();

            // mark segment vertex positions valid for current SRID and terrain
            _forwardSegmentsSrid = view.currentPass.drawSrid;
            _forwardSegmentsTerrainVersion = terrainVersion;
        }

        if(segmentGeom == null)
        {
            ShortBuffer geom = Unsafe.allocateDirect(12, ShortBuffer.class);
            geom.put(new short[]
            {
                // Triangle 1
                (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00,
                (byte) 0x00, (byte) 0xFF,
                // Triangle 2
                (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00,
                (byte) 0x00, (byte) 0x00
            });
            geom.flip();
            segmentGeom = geom;

            // check for MPU for workarounds
            isMpu = (ConfigOptions.getOption("mpu", 0) != 0);
        }
        // sanity check the factor
        if (factor < 1)
            factor = 1;

        // XXX - employ some workarounds for nuances with the MPU RDC GL driers
        if(isMpu)
            factor = ((pattern&0xFFFF) == 0xFFFF) ? 0 : 1;

        if(_pattern == null)
            _pattern = Unsafe.allocateDirect(12, ShortBuffer.class);
        _pattern.clear();
        for(int i = 0; i < 6; i++) {
            _pattern.put(pattern); // pattern
            _pattern.put((short) factor); // factor
        }
        _pattern.flip();

        GLES30.glUseProgram(program.handle);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // instanced data
        GLES30.glEnableVertexAttribArray(program.a_normal);
        GLES30.glVertexAttribDivisor(program.a_normal, 0);
        GLES30.glEnableVertexAttribArray(program.a_dir);
        GLES30.glVertexAttribDivisor(program.a_dir, 0);
        GLES30.glEnableVertexAttribArray(program.a_pattern);
        GLES30.glVertexAttribDivisor(program.a_pattern, 0);
        GLES30.glEnableVertexAttribArray(program.a_factor);
        GLES30.glVertexAttribDivisor(program.a_factor, 0);

        // per-instance attributes
        GLES30.glEnableVertexAttribArray(program.a_vertexCoord0);
        GLES30.glVertexAttribDivisor(program.a_vertexCoord0, 1);
        GLES30.glEnableVertexAttribArray(program.a_vertexCoord1);
        GLES30.glVertexAttribDivisor(program.a_vertexCoord1, 1);
        GLES30.glEnableVertexAttribArray(program.a_cap);
        GLES30.glVertexAttribDivisor(program.a_cap, 1);

        // constant attributes
        if(program.a_minLevelOfDetail >= 0) {
            GLES30.glDisableVertexAttribArray(program.a_minLevelOfDetail);
            GLES30.glVertexAttribDivisor(program.a_minLevelOfDetail, 0);
        }
        if(program.a_maxLevelOfDetail >= 0) {
            GLES30.glDisableVertexAttribArray(program.a_maxLevelOfDetail);
            GLES30.glVertexAttribDivisor(program.a_maxLevelOfDetail, 0);
        }

        // instance data
        GLES30.glVertexAttribPointer(program.a_normal, 1, GLES30.GL_UNSIGNED_BYTE, true, 4, segmentGeom.position(0));
        GLES30.glVertexAttribPointer(program.a_dir, 1, GLES30.GL_UNSIGNED_BYTE, true, 4, segmentGeom.position(2));
        segmentGeom.position(0);
        if(isMpu)
            GLES30.glVertexAttribPointer(program.a_pattern, 1, GLES30.GL_UNSIGNED_SHORT, false, 4, _pattern.position(0));
        else
            GLES30.glVertexAttribIPointer(program.a_pattern, 1, GLES30.GL_UNSIGNED_SHORT, 4, _pattern.position(0));
        GLES30.glVertexAttribPointer(program.a_factor, 1, GLES30.GL_UNSIGNED_SHORT, false, 4, _pattern.position(1));
        _pattern.position(0);

        // per-instance attributes
        GLES30.glVertexAttribPointer(program.a_vertexCoord0, 3, GLES30.GL_FLOAT, false, 24, _forwardSegmentVerts.position(0));
        GLES30.glVertexAttribPointer(program.a_vertexCoord1, 3, GLES30.GL_FLOAT, false, 24, _forwardSegmentVerts.position(3));
        _forwardSegmentVerts.position(0);
        GLES30.glVertexAttribPointer(program.a_cap, 2, GLES30.GL_SHORT, false, 4, _normals.position(0));
        _normals.position(0);

        // constant attributes
        if(program.a_minLevelOfDetail >= 0)
            GLES30.glVertexAttrib1f(program.a_minLevelOfDetail, 0f);
        if(program.a_maxLevelOfDetail >= 0)
            GLES30.glVertexAttrib1f(program.a_maxLevelOfDetail, 32f);

        _setUniforms(view, program, width, red, green, blue, alpha, outlineWidth, outlineRed, outlineGreen, outlineBlue, outlineAlpha);

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 6, _forwardSegmentVerts.limit()/6);
        if (acap == 0) // double-draw
        {
            boolean[] depthWriteEnabled = new boolean[1];
            GLES30.glGetBooleanv(GLES30.GL_DEPTH_WRITEMASK, depthWriteEnabled, 0);
            // turn off depth writes
            if(depthWriteEnabled[0])
                GLES30.glDepthMask(false);
            // turn off cap/join vertex attribute
            GLES30.glDisableVertexAttribArray(program.a_cap);
            // enable joins on all segments
            GLES30.glVertexAttrib2f(program.a_cap, 1.f, 1.f);
            // draw lines
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 6, _forwardSegmentVerts.limit()/6);
            // re-enable cap/join vertex attribute
            GLES30.glEnableVertexAttribArray(program.a_cap);
            // restore depth writes
            if(depthWriteEnabled[0])
                GLES30.glDepthMask(true);
        }
        // If the geometry crosses the IDL and unwrapping is occuring draw
        // again without the unwrap. This addresses the case where the geometry
        // may cross into the secondary hemisphere without crossing the IDL
        /*
        if (_crossesIDL && GLAntiMeridianHelper.getUnwrap(view, _crossesIDL, _primaryHemi) != 0d)
        {
            view.scratch.matrix.set(view.scene.forward);
            view.scratch.matrix.translate(_rtcX, _rtcY, _rtcZ);
            view.scratch.matrix.get(view.scratch.matrixD, Matrix.MatrixOrder.COLUMN_MAJOR);
            for (int i = 0; i < 16; i++)
            {
                view.scratch.matrixF[i] = (float) view.scratch.matrixD[i];
            }
            GLES30.glUniformMatrix4fv(program.uModelView, 1, false, view.scratch.matrixF, 0);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, _forwardSegmentVerts.limit() / 6);
        }
         */

        GLES30.glDisableVertexAttribArray(program.a_vertexCoord0);
        GLES30.glDisableVertexAttribArray(program.a_vertexCoord1);
        GLES30.glDisableVertexAttribArray(program.a_normal);
        GLES30.glDisableVertexAttribArray(program.a_dir);
        GLES30.glDisableVertexAttribArray(program.a_cap);
        GLES30.glDisableVertexAttribArray(program.a_pattern);
        GLES30.glDisableVertexAttribArray(program.a_factor);

        GLES30.glVertexAttribDivisor(program.a_vertexCoord0, 0);
        GLES30.glVertexAttribDivisor(program.a_vertexCoord1, 0);
        GLES30.glVertexAttribDivisor(program.a_normal, 0);
        GLES30.glVertexAttribDivisor(program.a_dir, 0);
        GLES30.glVertexAttribDivisor(program.a_cap, 0);
        GLES30.glVertexAttribDivisor(program.a_pattern, 0);
        GLES30.glVertexAttribDivisor(program.a_factor, 0);
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    /**
     * Sets the uniforms for drawing the lines.
     *
     * @param view    The GLMapView.
     * @param program The program whose uniforms we want to set.
     * @param width   The width of the line we're going to draw.
     * @param sr      The stroke red component.
     * @param sg      The stroke green component.
     * @param sb      The stroke blue component.
     * @param sa      The stroke alpha component.
     * @param owidth  The width of the outline
     * @param or      The stroke red component.
     * @param og      The stroke green component.
     * @param ob      The stroke blue component.
     * @param oa      The stroke alpha component.
     */
    private void _setUniforms(GLMapView view, AntiAliasingProgram program, float width, float sr, float sg, float sb, float sa, float owidth, float or, float og, float ob, float oa)
    {
        // set Model-View as current scene forward
        view.scratch.matrix.set(view.currentPass.scene.forward);
        // apply hemisphere shift if necessary
        final double unwrap = GLAntiMeridianHelper.getUnwrap(view, _crossesIDL, _primaryHemi);
        view.scratch.matrix.translate(unwrap, 0d, 0d);

        // apply the RTC offset to translate from local to world coordinate system (map projection)
        view.scratch.matrix.translate(_rtcX, _rtcY, _rtcZ);
        view.scratch.matrix.get(view.scratch.matrixD, Matrix.MatrixOrder.COLUMN_MAJOR);
        for (int i = 0; i < 16; i++)
        {
            view.scratch.matrixF[i] = (float) view.scratch.matrixD[i];
        }
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION, view.scratch.matrixF, 16);
        gov.tak.platform.commons.opengl.Matrix.multiplyMM(view.scratch.matrixF, 32, view.scratch.matrixF, 16, view.scratch.matrixF, 0);
        GLES30.glUniformMatrix4fv(program.u_mvp, 1, false, view.scratch.matrixF, 32);

        GLES30.glDisableVertexAttribArray(program.a_halfStrokeWidth);
        GLES30.glVertexAttrib1f(program.a_halfStrokeWidth, width / view.currentPass.relativeScaleHint / 2f);
        GLES30.glDisableVertexAttribArray(program.a_color);
        GLES30.glVertexAttrib4f(program.a_color, sr, sg, sb, sa);

        // apply outline
        GLES30.glVertexAttrib1f(program.a_outlineWidth, owidth / view.currentPass.relativeScaleHint);
        if (owidth > 0f)
            GLES30.glVertexAttrib4f(program.a_outlineColor, or, og, ob, oa);
        else // no outline, specify stroke color to be used in anti-alias region
            GLES30.glVertexAttrib4f(program.a_outlineColor, sr, sg, sb, sa);

        // viewport size
        {
            int[] viewport = new int[4];
            GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, viewport, 0);
            GLES30.glUniform2f(program.u_viewportSize, (float) viewport[2] / 2.0f, (float) viewport[3] / 2.0f);
        }
        if(program.u_hitTest >= 0)
            GLES30.glUniform1i(program.u_hitTest, 0);
    }

    public void release()
    {
        _freeBuffers();
    }

    private void _freeBuffers()
    {
        Unsafe.free(_lineStrip);
        _lineStrip = null;
        Unsafe.free(_forwardSegmentVerts);
        _forwardSegmentVerts = null;
        Unsafe.free(_normals);
        _normals = null;
        Unsafe.free(_pattern);
        _pattern = null;
    }

    /**
     * Allocate the buffers needed for drawing the line.
     *
     * @param numPoints The number of source points.
     */
    private void _allocateBuffers(int numPoints)
    {
        // Vertex positions are 3 component, so make sure we allocate enough even if the data we're
        // given only has 2 components
        int vertBufferCapacity = numPoints*6*4; // 6 verts per segment, 6 floats each vert
        int normalBufferCapacity = numPoints*8; // 6 verts per segment, 8 bytes each vert
        if (_lineStrip == null || _lineStrip.capacity() < (3 * numPoints))
        {
            _freeBuffers();
            _lineStrip = Unsafe.allocateDirect(3 * numPoints, DoubleBuffer.class);
            _forwardSegmentVerts = Unsafe.allocateDirect(vertBufferCapacity, FloatBuffer.class);
            _normals = Unsafe.allocateDirect(normalBufferCapacity, ByteBuffer.class);
        }
        _lineStrip.limit(3 * numPoints);
        _forwardSegmentVerts.limit(vertBufferCapacity);
        _normals.limit(normalBufferCapacity);
    }

    /**
     * Helper class that creates a static instance of the shader program and contains the locations
     * of uniform and attribute variables.
     */
    private static class AntiAliasingProgram
    {
        static AntiAliasingProgram instance;

        public int handle = GLES30.GL_NONE;
        
        public int u_mvp =  -1;
        public int u_viewportSize = -1;
        public int u_hitTest = -1;
        public int u_levelOfDetail = -1;
        public int a_vertexCoord0 = -1;
        public int a_vertexCoord1 = -1;
        public int a_texCoord = -1;
        public int a_color = -1;
        public int a_outlineColor = -1;
        public int a_normal = -1;
        public int a_halfStrokeWidth = -1;
        public int a_outlineWidth = -1;
        public int a_dir = -1;
        public int a_pattern = -1;
        public int a_factor = -1;
        public int a_cap = -1;
        public int a_minLevelOfDetail = -1;
        public int a_maxLevelOfDetail = -1;


        /**
         * Compiles the antialiasing shader program.
         */
        private AntiAliasingProgram(int handle)
        {
            this.handle = handle;

            // cache the uniform locations.
            this.u_mvp =  GLES30.glGetUniformLocation(this.handle, "u_mvp");
            this.u_viewportSize = GLES30.glGetUniformLocation(this.handle, "u_viewportSize");
            this.u_hitTest = GLES30.glGetUniformLocation(this.handle, "u_hitTest");
            this.u_levelOfDetail = GLES30.glGetUniformLocation(this.handle, "uLevelOfDetail");
            this.a_vertexCoord0 = GLES30.glGetAttribLocation(this.handle, "a_vertexCoord0");
            this.a_vertexCoord1 = GLES30.glGetAttribLocation(this.handle, "a_vertexCoord1");
            this.a_texCoord = GLES30.glGetAttribLocation(this.handle, "a_texCoord");
            this.a_color = GLES30.glGetAttribLocation(this.handle, "a_color");
            this.a_outlineColor = GLES30.glGetAttribLocation(this.handle, "a_outlineColor");
            this.a_normal = GLES30.glGetAttribLocation(this.handle, "a_normal");
            this.a_halfStrokeWidth = GLES30.glGetAttribLocation(this.handle, "a_halfStrokeWidth");
            this.a_outlineWidth = GLES30.glGetAttribLocation(this.handle, "a_outlineWidth");
            this.a_dir = GLES30.glGetAttribLocation(this.handle, "a_dir");
            this.a_pattern = GLES30.glGetAttribLocation(this.handle, "a_pattern");
            this.a_factor = GLES30.glGetAttribLocation(this.handle, "a_factor");
            this.a_cap = GLES30.glGetAttribLocation(this.handle, "a_cap");
            this.a_minLevelOfDetail = GLES30.glGetAttribLocation(this.handle, "aMinLevelOfDetail");
            this.a_maxLevelOfDetail = GLES30.glGetAttribLocation(this.handle, "aMaxLevelOfDetail");
        }

        /**
         * Retrieves a static instance of the antialiasing program.
         *
         * @return The static antialiasing program.
         */
        static AntiAliasingProgram get()
        {
            if (instance == null)
            {
                instance = new AntiAliasingProgram(getAntiAliasedLinesShader(RenderContext.getCurrent()));
            }
            return instance;
        }

        /**
         * Compiles the given vertex and fragment shader sources into a shader program.
         *
         * @param vSource The vertex shader source.
         * @param fSource The fragment shader source.
         * @return The handle to the shader program, or GL_NONE if an error occurred.
         */
        private static int createProgram(String vSource, String fSource)
        {
            int vsh = GLES20FixedPipeline.GL_NONE;
            int fsh = GLES20FixedPipeline.GL_NONE;
            int handle = GLES20FixedPipeline.GL_NONE;
            try
            {
                vsh = GLES20FixedPipeline.loadShader(GLES30.GL_VERTEX_SHADER, vSource);
                fsh = GLES20FixedPipeline.loadShader(GLES30.GL_FRAGMENT_SHADER, fSource);
                handle = GLES20FixedPipeline.createProgram(vsh, fsh);
            } finally
            {
                if (vsh != GLES30.GL_NONE)
                    GLES30.glDeleteShader(vsh);
                if (fsh != GLES30.GL_NONE)
                    GLES30.glDeleteShader(fsh);
            }
            return handle;
        }

    }

    public static int getAntiAliasedLinesShader(RenderContext ctx) {
        return getAntiAliasedLinesShader(RenderContext_interop.getPointer(ctx));
    }

    static native int getAntiAliasedLinesShader(long ctxptr);
}
