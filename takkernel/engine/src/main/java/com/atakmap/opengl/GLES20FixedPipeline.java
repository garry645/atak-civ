
package com.atakmap.opengl;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Stack;

import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.commons.opengl.Matrix;

import gov.tak.api.engine.Shader;

import com.atakmap.annotations.DeprecatedApi;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.opengl.GLRenderGlobals;
import gov.tak.api.engine.map.RenderContext;

@Deprecated
@DeprecatedApi(since = "4.4", forRemoval = true, removeAt = "4.7")
public class GLES20FixedPipeline extends GLES30
{

    public final static String TAG = "GLES20FixedPipeline";

    public final static int GL_VERTEX_ARRAY = 0x8074;
    public final static int GL_TEXTURE_COORD_ARRAY = 0x8078;
    public final static int GL_COLOR_ARRAY = 0x8076;

    public final static int GL_PROJECTION = 0x1701;
    public final static int GL_MODELVIEW = 0x1700;
    public final static int GL_CURRENT_COLOR = 2816;

    public final static int GL_POLYGON_SMOOTH_HINT = 0x0C53;

    public final static String VECTOR_2D_VERT_SHADER_SRC;
    public final static String VECTOR_3D_VERT_SHADER_SRC;
    public final static String GENERIC_VECTOR_FRAG_SHADER_SRC;
    public final static String COLOR_POINTER_VECTOR_2D_VERT_SHADER_SRC;
    public final static String COLOR_POINTER_VECTOR_3D_VERT_SHADER_SRC;
    public final static String COLOR_POINTER_VECTOR_FRAG_SHADER_SRC;
    public final static String TEXTURE_2D_VERT_SHADER_SRC;
    public final static String POINT_2D_VERT_SHADER_SRC;
    public final static String TEXTURE_3D_VERT_SHADER_SRC;
    public final static String TEXTURE_3D_V2_VERT_SHADER_SRC;
    public final static String TEXTURE_3D_ONE_MATRIX_VERT_SHADER_SRC;
    public final static String POINT_3D_VERT_SHADER_SRC;
    public final static String GENERIC_TEXTURE_FRAG_SHADER_SRC;
    public final static String MODULATED_TEXTURE_FRAG_SHADER_SRC;
    public final static String POINT_FRAG_SHADER_SRC;

    static
    {
        try
        {
            VECTOR_2D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_vector2d_vsh");
            VECTOR_3D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_vector3d_vsh");
            GENERIC_VECTOR_FRAG_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_vector_fsh");
            COLOR_POINTER_VECTOR_2D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_color_pointer_vector2d_vsh");
            COLOR_POINTER_VECTOR_3D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_color_pointer_vector3d_vsh");
            COLOR_POINTER_VECTOR_FRAG_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_color_pointer_vector_fsh");
            TEXTURE_2D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_texture2d_vsh");
            POINT_2D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_point2d_vsh");
            TEXTURE_3D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_texture3d_vsh");
            TEXTURE_3D_V2_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_texture3d_v2_vsh");
            TEXTURE_3D_ONE_MATRIX_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_texture3d_1matrix_vsh");
            POINT_3D_VERT_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_point3d_vsh");
            GENERIC_TEXTURE_FRAG_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_texture_fsh");
            MODULATED_TEXTURE_FRAG_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_modulated_texture_fsh");
            POINT_FRAG_SHADER_SRC = GLSLUtil.loadShaderSource("gl20fp_point_fsh");
        } catch (Throwable t)
        {
            throw new ExceptionInInitializerError(t);
        }
    }

    /**************************************************************************/

    private final static int PROGRAM_COLOR_MASK = 0x01;
    private final static int PROGRAM_VERTEX_MASK = 0x06;
    private final static int PROGRAM_TEXCOORD_MASK = 0x18;

    /**************************************************************************/

    private static class ContextSpecifics
    {
        final MatrixStack modelView = new MatrixStack(32);
        final MatrixStack projection = new MatrixStack(2);
        final MatrixStack texture = new MatrixStack(2);

        MatrixStack current = modelView;

        final int MAX_SWAP_MATRICES = (modelView.limit + projection.limit + texture.limit);

        final float[][] swapMatrices = new float[MAX_SWAP_MATRICES][];
        int numSwapMatrices = 0;

        int activeTextureUnit = GL_TEXTURE0;
        float r;
        float g;
        float b;
        float a;
        float aMod = 1;
        final Stack<Float> aModStack = new Stack<Float>();
        final ArrayPointer texCoordPointer = new ArrayPointer();
        final ArrayPointer vertexPointer = new ArrayPointer();
        final ArrayPointer colorPointer = new ArrayPointer();
        float pointSize;

        ContextSpecifics()
        {
            Matrix.setIdentityM(modelView.current, 0);
            Matrix.setIdentityM(projection.current, 0);
            Matrix.setIdentityM(texture.current, 0);
        }
    }

    private static class RenderContextMap
    {

        private final int CACHE_ARRAY_SIZE = 4;
        private final ContextSpecifics null_specifics = new ContextSpecifics();
        private final RenderContext contexts[] = new RenderContext[CACHE_ARRAY_SIZE];
        private final ContextSpecifics specifics[] = new ContextSpecifics[CACHE_ARRAY_SIZE];
        private final HashMap<RenderContext, ContextSpecifics> overflow = new HashMap<>();

        public ContextSpecifics getForContext(RenderContext context)
        {
            if (context == null)
                return null_specifics;
            for (int i = 0; i < CACHE_ARRAY_SIZE; ++i)
            {
                if (contexts[i] == context)
                    return specifics[i];
            }
            ContextSpecifics specific = overflow.get(context);
            if (specific == null)
                specific = put(context);
            return specific;
        }

        private ContextSpecifics put(RenderContext context)
        {
            ContextSpecifics specific = new ContextSpecifics();
            for (int i = 0; i < CACHE_ARRAY_SIZE; ++i)
            {
                if (contexts[i] == null)
                {
                    contexts[i] = context;
                    specifics[i] = specific;
                    return specific;
                }
            }
            overflow.put(context, specific);
            return specific;
        }
    }

    private final static ThreadLocal<RenderContextMap> _contextSpecifics = new ThreadLocal<RenderContextMap>()
    {
        @Override public RenderContextMap initialValue()
        {
            return new RenderContextMap();
        }
    };

    private static ContextSpecifics getContextSpecifics()
    {
        RenderContext renderContext = RenderContext.getCurrent();
        return _contextSpecifics.get().getForContext(renderContext);
    }

    /**************************************************************************/

    private GLES20FixedPipeline()
    {
    }

    /**************************************************************************/

    /**
     * Set the current GL Matrix mode.
     *
     * @param mode One of either GL_PROJECTION, GL_TEXTURE or GL_MODELVIEW.
     */
    public static void glMatrixMode(int mode)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        switch (mode)
        {
            case GL_PROJECTION:
                contextSpecifics.current = contextSpecifics.projection;
                break;
            case GL_TEXTURE:
                contextSpecifics.current = contextSpecifics.texture;
                break;
            case GL_MODELVIEW:
                contextSpecifics.current = contextSpecifics.modelView;
                break;
        }
    }

    /**
     * Select active texture unit<br>
     * <br>
     * Selects which texture unit subsequent texture state calls will affect. The number of texture
     * units an implementation supports is implementation dependent, but must be at least 8.
     *
     * @param texture Specifies which texture unit to make active. The number of texture units is
     *                implementation dependent, but must be at least 8. texture must be one of
     *                GL_TEXTUREi, where i ranges from 0 to (GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS - 1).
     *                The initial value is GL_TEXTURE0.
     */
    public static void glActiveTexture(int texture)
    {
        GLES30.glActiveTexture(texture);
        getContextSpecifics().activeTextureUnit = texture;
    }

    public static void glOrthoi(int left, int right, int bottom, int top, int zNear, int zFar)
    {
        glOrthof(left, right, bottom, top, zNear, zFar);
    }

    public static void glOrthof(float left, float right, float bottom, float top, float zNear,
                                float zFar)
    {
        Matrix.orthoM(getContextSpecifics().current.current, 0, left, right, bottom, top, zNear, zFar);
    }

    /**
     * Save the current state of the matrix that can be restored via a glPopMatrix.
     */
    public static void glPushMatrix()
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        contextSpecifics.current.matrices.push(contextSpecifics.current.current);
        final float[] newMatrix = getSwapMatrix();
        System.arraycopy(contextSpecifics.current.current, 0, newMatrix, 0, 16);
        contextSpecifics.current.current = newMatrix;
    }

    /**
     * Restore the matrix to the previous state saved during the glPushMatrix.
     */
    public static void glPopMatrix()
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.current.matrices.size() < 1)
            throw new IllegalStateException();
        setSwapMatrix(contextSpecifics.current.current);
        contextSpecifics.current.current = contextSpecifics.current.matrices.pop();
    }

    /**
     * Take the current matrix and apply a translation around the x, y, z.
     *
     * @param tx the translation for the x axis
     * @param ty the translation for the y axis
     * @param tz the translation for the z axis
     */
    public static void glTranslatef(float tx, float ty, float tz)
    {
        Matrix.translateM(getContextSpecifics().current.current, 0, tx, ty, tz);
    }

    /**
     * Take the current matrix and apply a rotation around the x, y, z
     *
     * @param angle the angle of rotation
     * @param x     the location to apply rotation around the x axis
     * @param y     the location to apply rotation around the y axis
     * @param z     the location to apply rotation around the z axis
     */
    public static void glRotatef(float angle, float x, float y, float z)
    {
        Matrix.rotateM(getContextSpecifics().current.current, 0, angle, x, y, z);
    }

    /**
     * Sets the current state of the matrix to the identity matrix, overwriting any values in the
     * current matrix.
     */
    public static void glLoadIdentity()
    {
        Matrix.setIdentityM(getContextSpecifics().current.current, 0);
    }

    /**
     * Take the current matrix and apply a scale in the x, y, and z direction
     *
     * @param sx the scalar to apply in the x direction
     * @param sy the scalar to apply in the y direction
     * @param sz the scalar to apply in the z direction
     */
    public static void glScalef(float sx, float sy, float sz)
    {
        Matrix.scaleM(getContextSpecifics().current.current, 0, sx, sy, sz);
    }

    /**
     * Take the current matrix and multiply it with the provided matrix.
     *
     * @param m      the matrix to be multiplied with the current matrix
     * @param offset the offset into the provided float array acting as the matrix.
     */
    public static void glMultMatrixf(float[] m, int offset)
    {
        final float[] tmp = getSwapMatrix();
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        System.arraycopy(contextSpecifics.current.current, 0, tmp, 0, 16);
        Matrix.multiplyMM(contextSpecifics.current.current, 0, tmp, 0, m, offset);
        setSwapMatrix(tmp);
    }

    private static float[] getSwapMatrix()
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.numSwapMatrices > 0)
            return contextSpecifics.swapMatrices[--contextSpecifics.numSwapMatrices];
        return new float[16];
    }

    private static void setSwapMatrix(float[] m)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.numSwapMatrices < contextSpecifics.MAX_SWAP_MATRICES)
            contextSpecifics.swapMatrices[contextSpecifics.numSwapMatrices++] = m;
    }

    public static void glGetFloatv(int pname, FloatBuffer params)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (pname == GL_MODELVIEW)
            params.put(contextSpecifics.modelView.current);
        else if (pname == GL_PROJECTION)
            params.put(contextSpecifics.projection.current);
        else if (pname == GL_TEXTURE)
            params.put(contextSpecifics.texture.current);
        else
            GLES30.glGetFloatv(pname, params);
    }

    public static void glGetFloatv(int pname, float[] f, int off)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (pname == GL_MODELVIEW)
            System.arraycopy(contextSpecifics.modelView.current, 0, f, off, 16);
        else if (pname == GL_PROJECTION)
            System.arraycopy(contextSpecifics.projection.current, 0, f, off, 16);
        else if (pname == GL_TEXTURE)
            System.arraycopy(contextSpecifics.texture.current, 0, f, off, 16);
        else if (pname == GL_CURRENT_COLOR)
        {
            f[0] = contextSpecifics.r;
            f[1] = contextSpecifics.g;
            f[2] = contextSpecifics.b;
            f[3] = contextSpecifics.a;
        } else
            GLES30.glGetFloatv(pname, f, off);
    }

    public static void glLoadMatrixf(FloatBuffer params)
    {
        if (params.remaining() < 16)
            throw new BufferOverflowException();
        else if (params.remaining() > 16)
            throw new BufferUnderflowException();
        params.get(getContextSpecifics().current.current);
    }

    /**
     * Overwrite the current matrix with a provided matrix.
     *
     * @param f   the provided matrix
     * @param off the offset into the floating point array for the start of the matrix.
     */
    public static void glLoadMatrixf(float[] f, int off)
    {
        if (off < 0 || (f.length - off) < 16)
            throw new IllegalArgumentException();
        System.arraycopy(f, off, getContextSpecifics().current.current, 0, 16);
    }

    public static void glLineWidth(float w)
    {
        GLES30.glLineWidth(w * GLRenderGlobals.getRelativeScaling());
    }

    /**************************************************************************/

    public static void glColor4f(float red, float green, float blue, float alpha)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        contextSpecifics.r = red;
        contextSpecifics.g = green;
        contextSpecifics.b = blue;
        contextSpecifics.a = alpha;
    }

    public static void glColor4x(int red, int green, int blue, int alpha)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        contextSpecifics.r = Math.max(Math.min(red / 65536.0f, 1.0f), 0.0f);
        contextSpecifics.g = Math.max(Math.min(green / 65536.0f, 1.0f), 0.0f);
        contextSpecifics.b = Math.max(Math.min(blue / 65536.0f, 1.0f), 0.0f);
        contextSpecifics.a = Math.max(Math.min(alpha / 65536.0f, 1.0f), 0.0f);
    }

    public static void glPushAlphaMod(float alpha)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        contextSpecifics.aModStack.push(contextSpecifics.aMod);
        contextSpecifics.aMod *= alpha;
    }

    public static void glPopAlphaMod()
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (!contextSpecifics.aModStack.empty())
            contextSpecifics.aMod = contextSpecifics.aModStack.pop();
    }

    public static float getAlphaMod()
    {
        return getContextSpecifics().aMod;
    }

    /**
     * Sets the ArrayPointer to the specified threadLocals.
     *
     * @param state New state for the ArrayPointer. Options are GL_VERTEX_ARRAY,
     *              GL_TEXTURE_COORD_ARRAY, GL_COLOR_ARRAY.
     */
    public static void glEnableClientState(int state)
    {
        ArrayPointer p = null;
        if (state == GL_VERTEX_ARRAY)
        {
            p = getContextSpecifics().vertexPointer;
        } else if (state == GL_TEXTURE_COORD_ARRAY)
        {
            p = getContextSpecifics().texCoordPointer;
        } else if (state == GL_COLOR_ARRAY)
        {
            p = getContextSpecifics().colorPointer;
        }

        if (p != null)
            p.enabled = true;
    }

    public static void glDisableClientState(int state)
    {
        ArrayPointer p = null;
        if (state == GL_VERTEX_ARRAY)
        {
            p = getContextSpecifics().vertexPointer;
        } else if (state == GL_TEXTURE_COORD_ARRAY)
        {
            p = getContextSpecifics().texCoordPointer;
        } else if (state == GL_COLOR_ARRAY)
        {
            p = getContextSpecifics().colorPointer;
        }

        if (p != null)
        {
            p.enabled = false;
            p.pointer = null;
        }
    }

    /**
     * Define an array of vertex data.
     *
     * @param size   Specifies the number of coordinates per vertex. Must be 2, 3, or 4. The initial
     *               value is 4.
     * @param type   Specifies the data type of each coordinate in the array. Symbolic constants
     *               GL_SHORT, GL_INT, GL_FLOAT, or GL_DOUBLE are accepted. The initial value is
     *               GL_FLOAT.
     * @param stride Specifies the byte offset between consecutive vertices. If stride is 0, the
     *               vertices are understood to be tightly packed in the array. The initial value is 0.
     * @param buffer ByteBuffer passed in as vertex data. Must use a native order, direct Buffer. <br>
     *               Always Use: <br>
     *               ByteBuffer bb = ByteBuffer.allocateDirect(capacity);<br>
     *               bb.order(ByteOrder.nativeOrder);
     */
    public static void glVertexPointer(int size, int type, int stride, Buffer buffer)
    {
        setArrayPointer(getContextSpecifics().vertexPointer, size, type, stride, buffer);
    }

    /**
     * VBO
     */
    public static void glVertexPointer(int size, int type, int stride, int position)
    {
        setArrayPointer(getContextSpecifics().vertexPointer, size, type, stride, position);
    }

    public static void glTexCoordPointer(int size, int type, int stride, Buffer buffer)
    {
        setArrayPointer(getContextSpecifics().texCoordPointer, size, type, stride, buffer);
    }

    /**
     * VBO
     */
    public static void glTexCoordPointer(int size, int type, int stride, int position)
    {
        setArrayPointer(getContextSpecifics().texCoordPointer, size, type, stride, position);
    }

    public static void glColorPointer(int size, int type, int stride, Buffer buffer)
    {
        setArrayPointer(getContextSpecifics().colorPointer, size, type, stride, buffer);
    }

    public static void glColorPointer(int size, int type, int stride, int position)
    {
        setArrayPointer(getContextSpecifics().colorPointer, size, type, stride, position);
    }

    private static void setArrayPointer(ArrayPointer p, int size, int type, int stride,
                                        Buffer buffer)
    {
        p.size = size;
        p.type = type;
        p.stride = stride;
        p.pointer = buffer;
        p.position = buffer.position();
    }

    private static void setArrayPointer(ArrayPointer p, int size, int type, int stride, int position)
    {
        p.size = size;
        p.type = type;
        p.stride = stride;
        p.position = position;
        p.pointer = null;
    }

    public static void glPointSize(int size)
    {
        getContextSpecifics().pointSize = size;
    }

    public static void glDrawArrays(int mode, int first, int count)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.vertexPointer.enabled && (contextSpecifics.texCoordPointer.enabled || mode == GLES30.GL_POINTS))
            drawBoundTexture(mode, contextSpecifics.activeTextureUnit - GLES30.GL_TEXTURE0, first, count,
                    contextSpecifics.vertexPointer, contextSpecifics.texCoordPointer);
        else if (contextSpecifics.vertexPointer.enabled)
            drawGenericVector(mode, first, count, contextSpecifics.vertexPointer);
        else
            throw new IllegalStateException();
    }

    /**
     * Render primitives from array data.
     *
     * @param mode    Specifies what kind of primitives to render. Symbolic constants GL_POINTS,
     *                GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, and
     *                GL_TRIANGLES are accepted.
     * @param count   Specifies the number of elements to be rendered.
     * @param type    Specifies the type of the values in indices. Must be GL_UNSIGNED_BYTE or
     *                GL_UNSIGNED_SHORT.
     * @param indices Specifies a pointer to the location where the indices are stored.
     */
    public static void glDrawElements(int mode, int count, int type, Buffer indices)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.texCoordPointer.enabled || mode == GLES30.GL_POINTS)
            drawBoundTexture(mode, contextSpecifics.activeTextureUnit - GLES30.GL_TEXTURE0, count, type, indices,
                    contextSpecifics.vertexPointer, contextSpecifics.texCoordPointer);
        else if (contextSpecifics.vertexPointer.enabled)
            drawGenericVector(mode, count, type, indices, contextSpecifics.vertexPointer);
        else
            throw new IllegalStateException();
    }

    /**************************************************************************/

    public static int loadShader(int type, String source)
    {
        final int retval = GLES30.glCreateShader(type);
        if (retval == GLES30.GL_FALSE)
            throw new RuntimeException();
        GLES30.glShaderSource(retval, source);
        GLES30.glCompileShader(retval);

        int[] success = new int[1];
        GLES30.glGetShaderiv(retval, GLES30.GL_COMPILE_STATUS, success, 0);
        if (success[0] == 0)
        {
            Log.d(TAG, "FAILED TO LOAD SHADER: " + source);
            final String msg = GLES30.glGetShaderInfoLog(retval);
            GLES30.glDeleteShader(retval);
            throw new RuntimeException(msg);
        }
        return retval;
    }

    public static int createProgram(int vertShader, int fragShader)
    {
        int retval = GLES30.glCreateProgram();
        if (retval == GLES30.GL_FALSE)
            throw new RuntimeException();
        GLES30.glAttachShader(retval, vertShader);
        GLES30.glAttachShader(retval, fragShader);
        GLES30.glLinkProgram(retval);

        int[] success = new int[1];
        GLES30.glGetProgramiv(retval, GLES30.GL_LINK_STATUS, success, 0);
        if (success[0] == 0)
        {
            final String msg = GLES30.glGetProgramInfoLog(retval);
            GLES30.glDeleteProgram(retval);
            throw new RuntimeException(msg);
        }
        return retval;
    }

    private static Shader getGenericVectorProgram(int size, boolean colorPointer)
    {
        int flags = 0;
        if (size == 2)
            flags |= Shader.FLAG_2D;
        if (colorPointer)
            flags |= Shader.FLAG_COLOR_POINTER;

        return Shader.create(flags);
    }

    static public void useFixedPipelineMatrices(Shader shader)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        shader.setProjection(contextSpecifics.projection.current);
        shader.setModelView(contextSpecifics.modelView.current);
    }

    static public void useFixedPipelineColor(Shader shader)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        shader.setColor4f(contextSpecifics.r, contextSpecifics.g, contextSpecifics.b, contextSpecifics.a);
    }

    private static Shader getGenericTextureProgram(int vSize, boolean point)
    {
        if (vSize < 0 || vSize > 3)
            throw new UnsupportedOperationException();

        int flags = 0;
        flags |= Shader.FLAG_TEXTURED;
        if (point)
            flags |= Shader.FLAG_POINT;
        if (vSize == 2)
            flags |= Shader.FLAG_2D;

        return Shader.create(flags);
    }

    private static void drawGenericVector(int mode, int first, int count, ArrayPointer vPointer)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.colorPointer.enabled)
            drawGenericVector(mode, first, count, vPointer, contextSpecifics.colorPointer);
        else
            drawGenericVector(mode, first, count, vPointer, contextSpecifics.r, contextSpecifics.g, contextSpecifics.b, contextSpecifics.a * contextSpecifics.aMod);
    }

    private static void drawGenericVector(int mode, int first, int vertexCount,
                                          ArrayPointer vPointer, float r, float g, float b, float a)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        final Shader shader = getGenericVectorProgram(vPointer.size, false);
        GLES30.glUseProgram(shader.getHandle());

        GLES30.glUniformMatrix4fv(shader.getUProjection(), 1, false, contextSpecifics.projection.current, 0);

        GLES30.glUniformMatrix4fv(shader.getUModelView(), 1, false, contextSpecifics.modelView.current, 0);

        GLES30.glEnableVertexAttribArray(shader.getAVertexCoords());

        if (vPointer.pointer != null)
        {
            vPointer.pointer.position(vPointer.position);
            GLES30.glVertexAttribPointer(shader.getAVertexCoords(), vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.pointer);
        } else
        {
            GLES30.glVertexAttribPointer(shader.getAVertexCoords(), vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.position);
        }

        GLES30.glUniform4f(shader.getUColor(), r, g, b, a);

        GLES30.glDrawArrays(mode, first, vertexCount);

        GLES30.glDisableVertexAttribArray(shader.getAVertexCoords());
    }

    private static void drawGenericVector(int mode, int first, int vertexCount,
                                          ArrayPointer vPointer, ArrayPointer colorPointer)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        final Shader shader = getGenericVectorProgram(vPointer.size, true);
        GLES30.glUseProgram(shader.getHandle());

        GLES30.glUniformMatrix4fv(shader.getUProjection(), 1, false, contextSpecifics.projection.current, 0);

        GLES30.glUniformMatrix4fv(shader.getUModelView(), 1, false, contextSpecifics.modelView.current, 0);

        GLES30.glEnableVertexAttribArray(shader.getAVertexCoords());

        if (vPointer.pointer != null)
        {
            vPointer.pointer.position(vPointer.position);
            GLES30.glVertexAttribPointer(shader.getAVertexCoords(), vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.pointer);
        } else
        {
            GLES30.glVertexAttribPointer(shader.getAVertexCoords(), vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.position);
        }

        final int aColorPointerHandle = shader.getAColorPointer();

        GLES30.glEnableVertexAttribArray(aColorPointerHandle);

        if (colorPointer.pointer != null)
        {
            colorPointer.pointer.position(colorPointer.position);
            GLES30.glVertexAttribPointer(aColorPointerHandle, colorPointer.size, colorPointer.type,
                    true, colorPointer.stride, colorPointer.pointer);
        } else
        {
            GLES30.glVertexAttribPointer(aColorPointerHandle, colorPointer.size, colorPointer.type,
                    true, colorPointer.stride, colorPointer.position);
        }

        GLES30.glDrawArrays(mode, first, vertexCount);

        GLES30.glDisableVertexAttribArray(shader.getAVertexCoords());
        GLES30.glDisableVertexAttribArray(aColorPointerHandle);
    }

    private static void drawGenericVector(int mode, int count, int type, Buffer indices,
                                          ArrayPointer vPointer)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        if (contextSpecifics.colorPointer.enabled)
            drawGenericVector(mode, count, type, indices, vPointer, contextSpecifics.colorPointer);
        else
            drawGenericVector(mode, count, type, indices, vPointer, contextSpecifics.r, contextSpecifics.g, contextSpecifics.b, contextSpecifics.a * contextSpecifics.aMod);
    }

    private static void drawGenericVector(int mode, int count, int type, Buffer indices,
                                          ArrayPointer vPointer, float r, float g, float b, float a)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        final Shader shader = getGenericVectorProgram(vPointer.size, false);
        GLES30.glUseProgram(shader.getHandle());

        final int uProjectionHandle = shader.getUProjection();
        GLES30.glUniformMatrix4fv(uProjectionHandle, 1, false, contextSpecifics.projection.current, 0);

        final int uModelViewHandle = shader.getUModelView();
        GLES30.glUniformMatrix4fv(uModelViewHandle, 1, false, contextSpecifics.modelView.current, 0);

        final int aVertexCoordsHandle = shader.getAVertexCoords();
        GLES30.glEnableVertexAttribArray(aVertexCoordsHandle);

        if (vPointer.pointer != null)
        {
            vPointer.pointer.position(vPointer.position);
            GLES30.glVertexAttribPointer(aVertexCoordsHandle, vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.pointer);
        } else
        {
            // vbo
            GLES30.glVertexAttribPointer(aVertexCoordsHandle, vPointer.size, vPointer.type, false,
                    vPointer.stride, vPointer.position);
        }

        final int uColorHandle = shader.getUColor();
        GLES30.glUniform4f(uColorHandle, r, g, b, a);

        GLES30.glDrawElements(mode, count, type, indices);

        GLES30.glDisableVertexAttribArray(aVertexCoordsHandle);
    }

    private static void drawGenericVector(int mode, int count, int type, Buffer indices,
                                          ArrayPointer vPointer, ArrayPointer colorPointer)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        final Shader shader = getGenericVectorProgram(vPointer.size, false);
        GLES30.glUseProgram(shader.getHandle());

        final int uProjectionHandle = shader.getUProjection();
        GLES30.glUniformMatrix4fv(uProjectionHandle, 1, false, contextSpecifics.projection.current, 0);

        final int uModelViewHandle = shader.getUModelView();
        GLES30.glUniformMatrix4fv(uModelViewHandle, 1, false, contextSpecifics.modelView.current, 0);

        final int aVertexCoordsHandle = shader.getAVertexCoords();
        GLES30.glEnableVertexAttribArray(aVertexCoordsHandle);

        vPointer.pointer.position(vPointer.position);
        GLES30.glVertexAttribPointer(aVertexCoordsHandle, vPointer.size, vPointer.type, false,
                vPointer.stride, vPointer.pointer);

        final int aColorPointerHandle = shader.getAColorPointer();
        GLES30.glEnableVertexAttribArray(aColorPointerHandle);

        colorPointer.pointer.position(colorPointer.position);
        GLES30.glVertexAttribPointer(aColorPointerHandle, colorPointer.size, colorPointer.type,
                true, colorPointer.stride, colorPointer.pointer);

        GLES30.glDrawElements(mode, count, type, indices);

        GLES30.glDisableVertexAttribArray(aVertexCoordsHandle);
        GLES30.glDisableVertexAttribArray(aColorPointerHandle);
    }

    private static void drawBoundTexture(int mode, int textureUnit, int first, int vertexCount,
                                         ArrayPointer vertexCoords, ArrayPointer textureCoords)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        float alpha = contextSpecifics.a * contextSpecifics.aMod;
        final Shader shader = getGenericTextureProgram(vertexCoords.size, mode == GL_POINTS);
        GLES30.glUseProgram(shader.getHandle());

        final int uProjectionHandle = shader.getUProjection();
        GLES30.glUniformMatrix4fv(uProjectionHandle, 1, false, contextSpecifics.projection.current, 0);

        final int uModelViewHandle = shader.getUModelView();
        GLES30.glUniformMatrix4fv(uModelViewHandle, 1, false, contextSpecifics.modelView.current, 0);

        if (vertexCoords.size == 3)
        {
            final int uTextureMatrixHandle = shader.getUTextureMx();
            GLES30.glUniformMatrix4fv(uTextureMatrixHandle, 1, false, contextSpecifics.texture.current, 0);
        }

        final int uTextureHandle = shader.getUTexture();
        GLES30.glUniform1i(uTextureHandle, textureUnit);

        final int uColorHandle = shader.getUColor();
        if (mode != GLES30.GL_POINTS)
            GLES30.glUniform4f(uColorHandle, contextSpecifics.r, contextSpecifics.g, contextSpecifics.b, alpha);
        else
            GLES30.glUniform4f(uColorHandle, 1f, 1f, 1f, 1f);

        if (mode == GLES30.GL_POINTS)
        {
            final int uPointSizeHandle = shader.getUPointSize();
            GLES30.glUniform1f(uPointSizeHandle, contextSpecifics.pointSize);
        }

        final int aVertexCoordsHandle = shader.getAVertexCoords();
        final int aTextureCoordsHandle = shader.getATextureCoords();

        if (vertexCoords.pointer != null)
        {
            vertexCoords.pointer.position(vertexCoords.position);
            GLES30.glVertexAttribPointer(aVertexCoordsHandle, vertexCoords.size, vertexCoords.type,
                    false, vertexCoords.stride, vertexCoords.pointer);
        } else
        {
            GLES30.glVertexAttribPointer(aVertexCoordsHandle, vertexCoords.size, vertexCoords.type, false, vertexCoords.stride, vertexCoords.position);
        }
        GLES30.glEnableVertexAttribArray(aVertexCoordsHandle);
        // Texture coords aren't needed for GL_POINTS
        if (mode != GL_POINTS)
        {
            if (textureCoords.pointer != null)
            {
                textureCoords.pointer.position(textureCoords.position);
                GLES30.glVertexAttribPointer(aTextureCoordsHandle, textureCoords.size,
                        textureCoords.type, false, textureCoords.stride, textureCoords.pointer);
            } else
            {
                GLES30.glVertexAttribPointer(aTextureCoordsHandle, textureCoords.size,
                        textureCoords.type, false, textureCoords.stride, textureCoords.position);
            }
            GLES30.glEnableVertexAttribArray(aTextureCoordsHandle);
        }

        GLES30.glDrawArrays(mode, first, vertexCount);

        GLES30.glDisableVertexAttribArray(aVertexCoordsHandle);
        if (mode != GL_POINTS)
        {
            GLES30.glDisableVertexAttribArray(aTextureCoordsHandle);
        }
    }

    private static void drawBoundTexture(int mode, int textureUnit, int count, int type,
                                         Buffer indices, ArrayPointer vertexCoords, ArrayPointer textureCoords)
    {
        final ContextSpecifics contextSpecifics = getContextSpecifics();
        float alpha = contextSpecifics.a * contextSpecifics.aMod;
        final Shader shader = getGenericTextureProgram(vertexCoords.size, mode == GL_POINTS);
        GLES30.glUseProgram(shader.getHandle());

        final int uProjectionHandle = shader.getUProjection();
        GLES30.glUniformMatrix4fv(uProjectionHandle, 1, false, contextSpecifics.projection.current, 0);

        final int uModelViewHandle = shader.getUModelView();
        GLES30.glUniformMatrix4fv(uModelViewHandle, 1, false, contextSpecifics.modelView.current, 0);

        if (vertexCoords.size == 3)
        {
            final int uTextureMatrixHandle = shader.getUTextureMx();
            GLES30.glUniformMatrix4fv(uTextureMatrixHandle, 1, false, contextSpecifics.texture.current, 0);
        }

        final int uTextureHandle = shader.getUTexture();
        GLES30.glUniform1i(uTextureHandle, textureUnit);

        final int uColorHandle = shader.getUColor();
        // XXX - consistency with legacy behavior for point sprite rendering
        if (mode != GLES30.GL_POINTS)
            GLES30.glUniform4f(uColorHandle, contextSpecifics.r, contextSpecifics.g, contextSpecifics.b, alpha);
        else
            GLES30.glUniform4f(uColorHandle, 1f, 1f, 1f, 1f);

        final int uPointSizeHandle = shader.getUPointSize();
        GLES30.glUniform1f(uPointSizeHandle, contextSpecifics.pointSize);

        final int aVertexCoordsHandle = shader.getAVertexCoords();
        final int aTextureCoordsHandle = shader.getATextureCoords();

        vertexCoords.pointer.position(vertexCoords.position);
        GLES30.glVertexAttribPointer(aVertexCoordsHandle, vertexCoords.size, vertexCoords.type,
                false, vertexCoords.stride, vertexCoords.pointer);
        GLES30.glEnableVertexAttribArray(aVertexCoordsHandle);

        textureCoords.pointer.position(textureCoords.position);
        GLES30.glVertexAttribPointer(aTextureCoordsHandle, textureCoords.size, textureCoords.type,
                false, textureCoords.stride, textureCoords.pointer);
        GLES30.glEnableVertexAttribArray(aTextureCoordsHandle);

        GLES30.glDrawElements(mode, count, type, indices);

        GLES30.glDisableVertexAttribArray(aVertexCoordsHandle);
        GLES30.glDisableVertexAttribArray(aTextureCoordsHandle);
    }

    /**************************************************************************/

    public static class Program
    {
        public int program;
        public int vertShader;
        public int fragShader;

        public Program()
        {
            this.program = 0;
            this.vertShader = 0;
            this.fragShader = 0;
        }

        public void create(String vertSrc, String fragSrc)
        {
            final int vertShader = loadShader(GLES30.GL_VERTEX_SHADER, vertSrc);
            final int fragShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragSrc);

            try
            {
                this.create(vertShader, fragShader);
            } catch (RuntimeException e)
            {
                GLES30.glDeleteShader(vertShader);
                GLES30.glDeleteShader(fragShader);
            }
        }

        public void create(int vertShader, int fragShader)
        {
            this.program = createProgram(vertShader, fragShader);
        }

        public void destroy()
        {
            this.destroy(true);
        }

        public void destroy(boolean shaders)
        {
            if (this.program != 0)
            {
                GLES30.glDeleteProgram(this.program);
                this.program = 0;
            }
            if (shaders)
            {
                if (this.vertShader != 0)
                    GLES30.glDeleteShader(this.vertShader);
                if (this.fragShader != 0)
                    GLES30.glDeleteShader(this.fragShader);
            }
            this.vertShader = 0;
            this.fragShader = 0;
        }
    }

    private static class ArrayPointer
    {
        public int size;
        public int type;
        public int stride;
        public Buffer pointer;
        public boolean enabled;
        public int position;

        public ArrayPointer()
        {
            this.size = 0;
            this.type = 0;
            this.stride = 0;
            this.pointer = null;
            this.enabled = false;
        }
    }

    private static class MatrixStack
    {
        public final Stack<float[]> matrices;
        public float[] current;
        public final int limit;

        public MatrixStack(int limit)
        {
            this.matrices = new Stack<float[]>();
            this.current = new float[16];
            this.limit = limit;
        }
    }
}
