
package gov.tak.platform.widgets.opengl;

import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.graphics.GLTriangle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.map.AtakMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.opengl.GLNinePatch;
import com.atakmap.opengl.GLText;

import java.util.concurrent.atomic.AtomicBoolean;

import gov.tak.api.engine.Shader;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.IMapRendererEnums.InverseMode;
import gov.tak.api.engine.map.IMapRendererEnums.InverseResult;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.IScaleWidget2;
import gov.tak.api.widgets.opengl.IGLWidgetSpi;
import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.commons.opengl.Matrix;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.marshal.MarshalManager;

/**
 * Renderer for the scale bar
 * See interface {@link IScaleWidget2}
 */
public final class GLScaleWidget extends GLShapeWidget implements
        IScaleWidget2.OnDisplayChangedListener
{

    public final static IGLWidgetSpi SPI = new IGLWidgetSpi()
    {
        @Override
        public int getPriority()
        {
            // ScaleWidget : MapWidget
            return 1;
        }

        @Override
        public GLWidget create(MapRenderer renderContext, IMapWidget subject)
        {
            if (subject instanceof IScaleWidget2)
            {
                IScaleWidget2 scaleWidget = (IScaleWidget2) subject;
                return new GLScaleWidget(scaleWidget, renderContext);
            } else
            {
                return null;
            }
        }
    };

    private static final float LINE_WIDTH = 2f;

    private final IScaleWidget2 _subject;
    private final GLTriangle.Fan _triangle;
    private final float[] _verts;
    private MapTextFormat _mapTextFormat;
    private String _text = "";
    private double _scale = Double.NaN;
    private float _textWidth = 0;
    private float _minWidth, _maxWidth;
    private int _rangeUnits = Span.METRIC;
    private boolean _useRounding = false;

    private final AtomicBoolean measurementDirty = new AtomicBoolean(true);
    private double measurement;
    private float lastX, lastY, lastWidth;

    private final MapRenderer.OnCameraChangedListener changedListener = new MapRenderer.OnCameraChangedListener() {
        @Override
        public void onCameraChanged(MapRenderer renderer) {
            measurementDirty.set(true);
        }
    };

    public GLScaleWidget(IScaleWidget2 subject, MapRenderer orthoView)
    {
        super(subject, orthoView);
        _subject = subject;
        _triangle = new GLTriangle.Fan(2, 4);
        _verts = new float[] {
                0, 0,
                0, 0,
                0, 0,
                0, 0
        };

    }

    @Override
    public void start()
    {
        super.start();
        _subject.addOnDisplayChangedListener(this);
        onDisplayChanged(_subject);

        // make sure to mark the measurement as being dirty when starting
        measurementDirty.set(true);
        _mapRenderer.addOnCameraChangedListener(changedListener);

    }

    @Override
    public void stop()
    {
        super.stop();
        _subject.removeOnDisplayChangedListener(this);
        _mapRenderer.removeOnCameraChangedListener(changedListener);

    }

    @Override
    public void drawWidgetContent(DrawState drawState)
    {
        if (getRenderContext() == null || _mapTextFormat == null
                || !subject.isVisible() || _padding == null)
            return;

        // Refresh text
        update();

        if (Double.isNaN(_scale))
            return;

        GLText glText = GLText.getInstance(_renderContext, _mapTextFormat);

        float left = _padding[LEFT];
        float right = left + _width;
        float bottom = -_pHeight;
        float top = 0;

        float npWidth = _textWidth + _padding[LEFT] + _padding[RIGHT];
        float npX = left + (_width - npWidth) / 2;
        float centerY = bottom + (_pHeight / 2);

        _verts[0] = left;
        _verts[1] = top;

        _verts[2] = left;
        _verts[3] = centerY;

        _verts[4] = right;
        _verts[5] = centerY;

        _verts[6] = right;
        _verts[7] = bottom;

        _triangle.setPoints(_verts);

        Shader shader = getDefaultShader();
        int prevProgram = shader.useProgram(true);

        shader.setModelView(drawState.modelMatrix);
        shader.setProjection(drawState.projectionMatrix);

        shader.setColor4f(0f, 0f, 0f, 1f);
        GLES30.glLineWidth(LINE_WIDTH + 2);
        _triangle.draw(shader, GLES30.GL_LINE_STRIP);

        shader.setColor4f(1f, 1f, 1f, 1f);
        GLES30.glLineWidth(LINE_WIDTH);
        _triangle.draw(shader, GLES30.GL_LINE_STRIP);

        // not saving a global reference to GLText because the MapTextFormat changes on this
        // widget
        GLNinePatch ninePatch = GLRenderGlobals.get(getRenderContext()).getMediumNinePatch();
        if (ninePatch != null)
        {
            DrawState ninePatchDrawState = drawState.clone();
            Matrix.translateM(ninePatchDrawState.modelMatrix, 0, npX, -_pHeight, 0);

            shader.setColor4f(0f, 0f, 0f, 0.9f);
            shader.setModelView(ninePatchDrawState.modelMatrix);
            shader.setProjection(ninePatchDrawState.projectionMatrix);
            ninePatch.draw(shader, npWidth, _pHeight);

            ninePatchDrawState.recycle();
        }
        GLES30.glUseProgram(prevProgram);
        DrawState textDrawState = drawState.clone();
        Matrix.translateM(textDrawState.modelMatrix, 0, npX + _padding[LEFT], -_pHeight
                        + _padding[BOTTOM] + _mapTextFormat
                        .getBaselineOffsetFromBottom(),
                0f);

        glText.draw(_text, 1f, 1f, 1f, 1f, textDrawState.projectionMatrix, textDrawState.modelMatrix);

        textDrawState.recycle();
    }

    @Override
    public void onDisplayChanged(IScaleWidget2 widget)
    {
        final MapTextFormat fmt = widget.getTextFormat();
        final boolean useRounding = widget.isRounded();
        final float minWidth = widget.getMinWidth();
        final float maxWidth = widget.getMaxWidth();
        final int rangeUnits = widget.getUnits();
        final float[] padding = widget.getPadding();
        runOrQueueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                _mapTextFormat = fmt;
                _useRounding = useRounding;
                _minWidth = minWidth;
                _maxWidth = maxWidth;
                _rangeUnits = rangeUnits;
                _padding = padding;
                update();
            }
        });
    }

    @Override
    public void releaseWidget()
    {
        stop();
    }

    /**
     * Update the widget's display
     * <p>
     * This is performed on the GL thread in order to get a more accurate
     * measurement. Previously this code used {@link AtakMapView#addOnMapMovedListener(AtakMapView.OnMapMovedListener)}
     * which gives outdated results when an inverse is performed.
     */
    private void update()
    {

        if (_padding == null)
            return;

        float barWidth = _useRounding ? Math.max(_minWidth,
                _maxWidth - _padding[LEFT] - _padding[RIGHT]) : _minWidth;

        double meters = measure(barWidth);
        double ppm = barWidth/meters;
        if (!Double.isNaN(meters))
        {
            if (_useRounding)
            {
                String text = SpanUtilities.formatType(_rangeUnits, meters,
                        Span.METER);
                Span displayUnit = Span.findFromAbbrev(text
                        .substring(text.lastIndexOf(" ") + 1));
                if (displayUnit == null)
                    displayUnit = Span.METER;

                double converted = SpanUtilities.convert(meters,
                        Span.METER, displayUnit);
                int decimalPlaces = converted < 1 ? (int) Math.ceil(-Math
                        .log10(converted)) : 0;
                double exp10 = SpanUtilities.convert(Math.pow(10, Math.floor(
                        Math.log10(converted))), displayUnit, Span.METER);
                barWidth = (float) (exp10 * ppm);
                // The initial pass calculates the largest base 10 value that will produce a barWidth < maxWidth
                // but that can produce a barWidth < minWidth. If so, do a binary search to find an appropriate
                // value that falls within the requested barWidth range.
                if (barWidth < _minWidth) {
                    // Bump initial starting point by a factor of 10 to go over the maxWidth and work down
                    double[] measureResults = binarySearchBarWidth(exp10 * 10, ppm);
                    barWidth = (float) measureResults[0];
                    meters = measureResults[1] + 1e-6;
                } else {
                    meters = exp10 + 1e-6;
                }
                _text = SpanUtilities.formatType(_rangeUnits, meters, Span.METER,
                        "#.##");
            } else
                _text = SpanUtilities.formatType(_rangeUnits, meters, Span.METER);
            _text = GLText.localize(_text);
            if (_text.startsWith("0 "))
                _text = "<1 " + _text.substring(2);
        } else
            _text = "NaN";

        _scale = meters;
        _textWidth = _mapTextFormat.measureTextWidth(_text);
        float textHeight = _mapTextFormat.measureTextHeight(_text);
        _subject.setSize(barWidth, textHeight);
        _subject.update(_text, _scale);
    }

    private double[] binarySearchBarWidth(double startingMeters, double ppm)
    {
        double bottomRange = 0;
        double target = startingMeters;
        double topRange = target * 2;
        double barWidth = target * ppm;
        while ((barWidth < _minWidth || barWidth > _maxWidth))
        {
            if (barWidth < _minWidth)
            {
                bottomRange = target;
            } else
            {
                topRange = target;
            }
            target = bottomRange + ((topRange - bottomRange) / 2f);
            barWidth = target * ppm;
            if (topRange - bottomRange <= 1.0) break;
        }
        return new double[] {barWidth, target};
    }

    /**
     * Measure the distance covered by the scale bar
     *
     * @param width Width in pixels
     * @return Distance in meters
     */
    private double measure(float width)
    {
        PointF pt = subject.getAbsoluteWidgetPosition();
        float x = pt.x + _padding[LEFT];
        float y = pt.y + _pHeight / 2;
        if (measurementDirty.getAndSet(false) ||
                Double.compare(x,lastX) != 0 ||
                Double.compare(y,lastY) != 0 ||
                Double.compare(width, lastWidth) != 0)
        {
            lastX = x;
            lastY = y;
            lastWidth = width;

            PointD p = new PointD(x, y, 0);
            GeoPoint p1 = GeoPoint.createMutable();
            GeoPoint p2 = GeoPoint.createMutable();

            InverseResult r1 = _mapRenderer.inverse(p, p1, InverseMode.RayCast,
                    IMapRendererEnums.HINT_RAYCAST_IGNORE_SURFACE_MESH, DisplayOrigin.UpperLeft);
            p.x += width;
            InverseResult r2 = _mapRenderer.inverse(p, p2, InverseMode.RayCast,
                    IMapRendererEnums.HINT_RAYCAST_IGNORE_SURFACE_MESH, DisplayOrigin.UpperLeft);
            if (r1 == InverseResult.None || r2 == InverseResult.None)
                measurement = Double.NaN;
            else
                measurement = GeoCalculations.distanceTo(
                    MarshalManager.marshal(p1, GeoPoint.class, com.atakmap.coremap.maps.coords.GeoPoint.class),
                    MarshalManager.marshal(p2, GeoPoint.class, com.atakmap.coremap.maps.coords.GeoPoint.class));

        }
        return measurement;
    }
}
