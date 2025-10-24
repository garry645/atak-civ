package gov.tak.platform.widgets.opengl;

import com.atakmap.lang.Unsafe;
import com.atakmap.opengl.GLTexture;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.ColorFilter;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.engine.Shader;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.widgets.IDrawableWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.opengl.IGLWidget;
import gov.tak.api.widgets.opengl.IGLWidgetSpi;
import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.commons.opengl.Matrix;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.marshal.MarshalManager;

public class GLDrawableWidget
        extends GLWidget implements
        IDrawableWidget.OnChangedListener, Drawable.Callback
{

    public final static IGLWidgetSpi SPI = new IGLWidgetSpi() {
        @Override
        public int getPriority() {
            return 2;
        }

        @Override
        public IGLWidget create(MapRenderer orthoView, IMapWidget subject)
        {
            if (subject instanceof IDrawableWidget) {
                IDrawableWidget drw = (IDrawableWidget) subject;
                GLDrawableWidget glMarkerWidget = new GLDrawableWidget(
                        drw, orthoView);
                glMarkerWidget.start();
                return glMarkerWidget;
            } else {
                return null;
            }
        }
    };

    private final IDrawableWidget _subject;

    private Drawable _drawable;
    private ColorFilter _colorFilter;
    protected boolean _needsRedraw;
    private Bitmap _bitmap;
    private GLTexture _texture;
    private FloatBuffer _vertTexCoords;

    public GLDrawableWidget(IDrawableWidget drw, MapRenderer ortho) {
        super(drw, ortho);
        _subject = drw;
        updateDrawable();
    }

    @Override
    public void start() {
        super.start();
        _subject.addChangeListener(this);
    }

    @Override
    public void stop() {
        super.stop();
        _subject.removeChangeListener(this);
    }

    @Override
    public void onDrawableChanged(IDrawableWidget widget) {
        updateDrawable();
    }

    @Override
    public void drawWidgetContent(DrawState state) {
        // Ensure the bitmap size matches the widget size
        checkBitmapSize();

        // Draw the drawable to the scratch bitmap
        if (_needsRedraw) {
            // clear the bitmap via scanline
            int[] scan = new int[(int)_width];
            for(int i = 0; i < (int)_height; i++)
                _bitmap.setPixels(scan, 0, (int)_width, 0, i, (int)_width, 1);

            if (_drawable != null) {
                _drawable.setColorFilter(_colorFilter);
                _drawable.setBounds(new Rect(0, 0, (int) _width, (int) _height));
                _drawable.draw(_bitmap);
            }

            // Need to regenerate the texture every time the bitmap changes
            generateTexture();
        }
        _needsRedraw = false;

        state = state.clone();

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        Matrix.translateM(state.modelMatrix, 0, 0f, -_pHeight, 0f);

        Shader shader = getTextureShader();

        int prevProgram = shader.useProgram(true);
        GLES30.glUniformMatrix4fv(shader.getUProjection(), 1, false, state.projectionMatrix, 0);
        GLES30.glUniformMatrix4fv(shader.getUModelView(), 1, false, state.modelMatrix, 0);
        GLES30.glUniform4f(shader.getUColor(), 1f, 1f, 1f, 1f);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glUniform1i(shader.getUTexture(), 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, _texture.getTexId());

        GLES30.glEnableVertexAttribArray(shader.getAVertexCoords());
        GLES30.glEnableVertexAttribArray(shader.getATextureCoords());
        GLES30.glVertexAttribPointer(shader.getAVertexCoords(), 2, GLES30.GL_FLOAT, false, 16, _vertTexCoords.position(0));
        GLES30.glVertexAttribPointer(shader.getATextureCoords(), 2, GLES30.GL_FLOAT, false, 16, _vertTexCoords.position(2));

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(shader.getATextureCoords());
        GLES30.glDisableVertexAttribArray(shader.getAVertexCoords());

        GLES30.glUseProgram(prevProgram);

        state.recycle();

        GLES30.glUseProgram(prevProgram);
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    @Override
    public void releaseWidget() {
        if (_bitmap != null) {
            //_bitmap.recycle();
            _bitmap = null;
        }
        if(_texture != null) {
            _texture.release();
            _texture = null;
        }
        if (_vertTexCoords != null) {
            Unsafe.free(_vertTexCoords);
            _vertTexCoords = null;
        }
    }

    protected void updateDrawable() {
        final Drawable dr = _subject.getDrawable();
        final ColorFilter cf = _subject.getColorFilter();
        _renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (_drawable != null)
                    _drawable.setCallback(null);
                _drawable = dr;
                if (_drawable != null)
                    _drawable.setCallback(GLDrawableWidget.this);
                _colorFilter = cf;
                _needsRedraw = true;
            }
        });
    }

    private void checkBitmapSize() {
        int bWidth = Math.max(1, (int) _width);
        int bHeight = Math.max(1, (int) _height);
        if (_bitmap != null && _bitmap.getWidth() == bWidth
                && _bitmap.getHeight() == bHeight)
            return;

//        if (_bitmap != null)
//            _bitmap.recycle();

        // Generate bitmap/canvas
        _bitmap = new Bitmap(bWidth, bHeight);
        _needsRedraw = true;
    }

    private void generateTexture() {
        // Generate texture buffer
        if (_texture == null || _texture.getTexHeight() < _width || _texture.getTexHeight() < _height) {
            if(_texture != null)
                _texture.release();

            _texture = new GLTexture((int)_width, (int)_height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
            // configure tex params
            _texture.setMagFilter(GLES30.GL_LINEAR);
            _texture.setMinFilter(GLES30.GL_NEAREST);
            _texture.setWrapS(GLES30.GL_CLAMP_TO_EDGE);
            _texture.setWrapT(GLES30.GL_CLAMP_TO_EDGE);
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, _texture.getTexId());
        _texture.load(MarshalManager.marshal(_bitmap, Bitmap.class, android.graphics.Bitmap.class));
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        if(_vertTexCoords == null)
            _vertTexCoords = Unsafe.allocateDirect(16, FloatBuffer.class);
        _vertTexCoords.clear();
        _vertTexCoords.put(0f); // xy[0]
        _vertTexCoords.put(0f);
        _vertTexCoords.put(0f); // uv[0]
        _vertTexCoords.put(_height/_texture.getTexHeight());
        _vertTexCoords.put(0f); // xy[1]
        _vertTexCoords.put(_height);
        _vertTexCoords.put(0f); // uv[1]
        _vertTexCoords.put(0f);
        _vertTexCoords.put(_width); // xy[0]
        _vertTexCoords.put(0f);
        _vertTexCoords.put(_width/_texture.getTexWidth()); // uv[0]
        _vertTexCoords.put(_height/_texture.getTexHeight());
        _vertTexCoords.put(_width); // xy[1]
        _vertTexCoords.put(_height);
        _vertTexCoords.put(_width/_texture.getTexWidth()); // uv[1]
        _vertTexCoords.put(0f);
        _vertTexCoords.flip();
    }

    @Override
    public void invalidate(@NonNull Drawable who) {
        _renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                _needsRedraw = true;
            }
        });
    }
}
