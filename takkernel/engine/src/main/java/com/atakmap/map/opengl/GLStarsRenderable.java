/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package com.atakmap.map.opengl;

import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import gov.tak.api.engine.Shader;
import gov.tak.api.engine.math.IMatrix.MatrixOrder;
import gov.tak.platform.commons.opengl.GLES20;
import gov.tak.platform.commons.opengl.GLES30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders a star background based on a subset of ESA Hipparcos catalog.
 */
public class GLStarsRenderable implements GLMapRenderable2
{
    private final static int GL_PROGRAM_POINT_SIZE = 34370;

    /** The default name of the stars file. */
    protected static final String DEFAULT_STARS_FILE = "Hipparcos_Stars_Mag6x5044.dat";

    /** The float buffer holding the Cartesian star coordinates. */
    protected final AtomicReference<FloatBuffer> starsBufferRef = new AtomicReference<>();
    protected int numStars;
    /** The radius of the spherical shell containing the stars. */
    protected double radius;
    /** The star sphere longitudinal rotation. */
    protected double longitudeOffset;
    /** The star sphere latitudinal rotation. */
    protected double latitudeOffset;
    private boolean inited;
    private int vboId = -1;

    public GLStarsRenderable()
    {
    }

    @Override
    public void release()
    {
        if (vboId != -1)
        {
            int[] vboAry = {vboId};
            GLES30.glDeleteBuffers(1, vboAry, 0);
        }
    }

    @Override
    public int getRenderPass()
    {
        return GLMapView.RENDER_PASS_SPRITES;
    }

    @Override
    public void draw(GLMapView view, int renderPass)
    {
        if (!MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES)) return;

        if (!inited) {
            new Thread(this::loadStars, "StarsLoader").start();
            inited = true;
        }

        FloatBuffer starsBuffer = starsBufferRef.get();
        if (starsBuffer == null) return;

        if (vboId == -1) {
            int[] vboAry = new int[1];
            GLES30.glGenBuffers(1, vboAry, 0);
            if (vboAry[0] == -1) return;
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboAry[0]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, numStars * 6 * 4, starsBuffer, GLES30.GL_STATIC_DRAW);
            vboId = vboAry[0];
        }

        float[] matrixArrayProjF = new float[16];
        float[] matrixArrayModelViewF = new float[16];
        double[] matrixArrayD = new double[16];

        boolean restoreDepth = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES20.GL_DEPTH_TEST);
        GLES30.glEnable(GL_PROGRAM_POINT_SIZE);

        // Override the default projection matrix in order to extend the far clip plane to include the stars.
        double aspectRatio = ((double) view.currentPass.scene.width / view.currentPass.scene.height);
        perspectiveM(matrixArrayProjF, (float) view.currentPass.scene.camera.fov, (float) aspectRatio, 1, (float) (radius + 1));

        // Override the default modelview matrix in order to force the eye point to the origin, and apply the
        // latitude and longitude rotations for the stars dataset. Forcing the eye point to the origin causes the
        // stars to appear at an infinite distance, regardless of the view's eye point.
        Matrix m = (Matrix) view.currentPass.scene.camera.modelView.clone();
        m.translate(view.currentPass.scene.camera.location.x, view.currentPass.scene.camera.location.y, view.currentPass.scene.camera.location.z);
        m.rotate(longitudeOffset, 0, 1, 0);
        m.rotate(latitudeOffset, 1, 0, 0);
        m.get(matrixArrayD, MatrixOrder.COLUMN_MAJOR);
        for (int a = 0; a < 16; a++) matrixArrayModelViewF[a] = (float) matrixArrayD[a];

        Shader shader = Shader.create(Shader.FLAG_COLOR_POINTER | Shader.FLAG_POINT);
        int[] restoreProg = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, restoreProg, 0);
        GLES30.glUseProgram(shader.getHandle());

        final int uProjectionHandle = shader.getUProjection();
        final int uModelViewHandle = shader.getUModelView();
        final int uPointSize = shader.getUPointSize();
        GLES30.glUniformMatrix4fv(uProjectionHandle, 1, false, matrixArrayProjF, 0);
        GLES30.glUniformMatrix4fv(uModelViewHandle, 1, false, matrixArrayModelViewF, 0);
        GLES30.glUniform1f(uPointSize, 2);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);

        final int aVertexCoordsHandle = shader.getAVertexCoords();
        final int aColorPointerHandle = shader.getAColorPointer();
        GLES30.glEnableVertexAttribArray(aVertexCoordsHandle);
        GLES30.glEnableVertexAttribArray(aColorPointerHandle);
        GLES30.glVertexAttribPointer(aVertexCoordsHandle, 3, GLES20.GL_FLOAT, false, 6 * 4, 3 * 4);
        GLES30.glVertexAttribPointer(aColorPointerHandle, 3, GLES20.GL_FLOAT, false, 6 * 4, 0);

        GLES30.glDrawArrays(GLES20.GL_POINTS, 0, numStars);

        GLES30.glDisableVertexAttribArray(aVertexCoordsHandle);
        GLES30.glDisableVertexAttribArray(aColorPointerHandle);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glDisable(GL_PROGRAM_POINT_SIZE);
        if (restoreDepth) GLES30.glEnable(GLES20.GL_DEPTH_TEST);
        GLES30.glUseProgram(restoreProg[0]);
    }

    protected void loadStars()
    {
        ByteBuffer byteBuffer;

        try(InputStream starsStream = GLRenderGlobals.appContext.getAssets().open(DEFAULT_STARS_FILE))
        {
            // enough to hold the data.
            byte[] bytes = new byte[121060];
            int index = 0;
            int count;
            do {
                count = starsStream.read(bytes, index, bytes.length - index);
                if (count != -1) index += count;
            } while (count != -1 && bytes.length - index > 0);
            byteBuffer = ByteBuffer.allocateDirect(index).order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put(bytes, 0 , index);
            byteBuffer.flip();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        //Grab the radius from the first value in the buffer
        radius = byteBuffer.getFloat();

        //View the rest of the ByteBuffer as a FloatBuffer
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

        //Number of stars = limit / 6 floats per star -> (R,G,B,X,Y,Z)
        numStars = floatBuffer.limit() / 6;

        starsBufferRef.set(floatBuffer);
    }

    public static void perspectiveM(float[] m, float yFovInDegrees, float aspect, float n, float f)
    {
        final float angleInRadians = (float) (yFovInDegrees * Math.PI / 180.0);
        final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));

        m[0] = a / aspect;
        m[4] = 0f;
        m[8] = 0f;
        m[12] = 0f;

        m[1] = 0f;
        m[5] = a;
        m[9] = 0f;
        m[13] = 0f;

        m[2] = 0f;
        m[6] = 0f;
        m[10] = -((f + n) / (f - n));
        m[14] = -1f;

        m[3] = 0f;
        m[7] = 0f;
        m[11] = -((2f * f * n) / (f - n));
        m[15] = 0f;
    }
}
