
package com.atakmap.android.maps.graphics;

import android.graphics.Color;
import android.graphics.Typeface;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.Globe;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.AttributeSet;

class GLPolylineFeature extends GLMapItemFeature implements
        Shape.OnFillColorChangedListener,
        Shape.OnStrokeColorChangedListener,
        Shape.OnStyleChangedListener,
        Shape.OnStrokeWeightChangedListener,
        Shape.OnPointsChangedListener,
        Shape.OnBasicLineStyleChangedListener,
        Polyline.OnLabelsChangedListener,
        Polyline.OnLabelTextSizeChanged,
        MapItem.OnAltitudeModeChangedListener,
        Polyline.OnHeightStyleChangedListener,
        Shape.OnHeightChangedListener,
        MapItem.OnMetadataChangedListener {

    private final static String TAG = "GLPolylineFeature";

    public final static GLMapItemFeatureSpi SPI = new GLMapItemFeatureSpi() {
        @Override
        public boolean isSupported(MapItem item) {
            return item instanceof Polyline;
        }

        @Override
        public GLMapItemFeature create(GLMapItemFeatures features,
                MapItem object) {
            if (!(object instanceof Polyline))
                return null;
            return new GLPolylineFeature(features);
        }
    };

    final static String[] LABEL_META_KEYS = new String[] {
            "labels_on",
            "minRenderScale",
            "minLabelRenderResolution",
            "maxLabelRenderResolution",
            "centerPointLabel",
    };

    private final static double DEFAULT_MIN_RENDER_SCALE = (1.0d / 100000.0d);

    /**
     * <pre>
     * _feature[0] => primary feature
     * _feature[1] => surface feature for filled extrusions
     * </pre>
     */
    Polyline _subject;
    SegmentLabel _centerLabel = null;
    SegmentLabel _floatingLabel = null;
    Set<SegmentLabel> _segmentLabels = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    GLPolylineFeature(GLMapItemFeatures features) {
        this(features, 2);
    }

    protected GLPolylineFeature(GLMapItemFeatures features,
            int maxFeatureCount) {
        super(features, maxFeatureCount);
        if (maxFeatureCount < 2)
            throw new IllegalArgumentException();
    }

    @Override
    public void startObservingImpl() {
        _subject = (Polyline) super._subject;

        _subject.addOnFillColorChangedListener(this);
        _subject.addOnStrokeColorChangedListener(this);
        _subject.addOnStyleChangedListener(this);
        _subject.addOnStrokeWeightChangedListener(this);
        _subject.addOnPointsChangedListener(this);
        _subject.addOnBasicLineStyleChangedListener(this);
        _subject.addOnLabelsChangedListener(this);
        _subject.addOnLabelTextSizeChangedListener(this);
        _subject.addOnAltitudeModeChangedListener(this);
        _subject.addOnHeightStyleChangedListener(this);
        _subject.addOnHeightChangedListener(this);
        for (String metakey : LABEL_META_KEYS)
            _subject.addOnMetadataChangedListener(metakey, this);
    }

    @Override
    public void stopObservingImpl() {
        // NOTE: `_subject` in this scope refers to the member variable that
        // hides the same-named member variable in the superclass
        if (_subject != null) {
            _subject.removeOnFillColorChangedListener(this);
            _subject.removeOnStrokeColorChangedListener(this);
            _subject.removeOnStyleChangedListener(this);
            _subject.removeOnStrokeWeightChangedListener(this);
            _subject.removeOnPointsChangedListener(this);
            _subject.removeOnBasicLineStyleChangedListener(this);
            _subject.removeOnLabelsChangedListener(this);
            _subject.removeOnLabelTextSizeChangedListner(this);
            _subject.removeOnAltitudeModeChangedListener(this);
            _subject.removeOnHeightStyleChangedListener(this);
            _subject.removeOnHeightChangedListener(this);
            for (String metakey : LABEL_META_KEYS)
                _subject.removeOnMetadataChangedListener(metakey, this);
        }

        synchronized (this) {
            if (_feature != null) {
                for (int i = 0; i < _feature.length; i++) {
                    if (_feature[i] != null) {
                        final long fid = _feature[i].getId();
                        _callback.unreserveFeatureId(_subject, fid);
                    }
                    _feature[i] = null;
                }
            }

            if (_centerLabel != null) {
                _features.labels.removeLabel(_centerLabel.id);
                _features.labelIds.remove(_centerLabel.id);
                _centerLabel = null;
            }
            if (_floatingLabel != null) {
                _features.labels.removeLabel(_floatingLabel.id);
                _features.labelIds.remove(_floatingLabel.id);
                _floatingLabel = null;
            }
            for (SegmentLabel label : _segmentLabels) {
                _features.labels.removeLabel(label.id);
                _features.labelIds.remove(label.id);
            }
            _segmentLabels.clear();

            _subject = null;
        }
    }

    @Override
    public HitTestResult postProcessHitTestResult(MapRenderer3 renderer,
            HitTestQueryParameters params, HitTestResult result) {
        final Feature feature = _feature[0];
        if (feature == null)
            return result;
        // post-process the result to obtain the type of hit as well as the index to preserve
        // downstream functionality. This implementation is _succeed-fast_, in that the first
        // vertex hit will cause an exit; segment hit testing is skipped after a first
        // intersecting segment is identified

        LineString ls = getPolylineLineString(feature.getGeometry());

        Feature.AltitudeMode altitudeMode = _subject.getAltitudeMode();
        // check force clamp-to-ground
        if (_features.clampToGroundControl != null &&
                _features.clampToGroundControl.getClampToGroundAtNadir() &&
                ((GLMapView) renderer).currentScene.drawTilt == 0d) {
            altitudeMode = Feature.AltitudeMode.ClampToGround;
        }

        final int style = _subject.getStyle();
        final int fillColor = _subject.getFillColor();

        final boolean fill = (fillColor & 0xFF000000) != 0 &&
                MathUtils.hasBits(style,
                        Polyline.STYLE_CLOSED_MASK | Shape.STYLE_FILLED_MASK);

        PolylineHitTestPostProcessor.postProcessHitTestResult(renderer, params,
                ls, altitudeMode, fill, result);

        return result;
    }

    @Override
    void validateFeatureImpl(int propertiesMask, Feature[] feature) {
        toFeature(1L, _subject, _fids, feature);
    }

    @Override
    public void onVisibleChanged(MapItem item) {
        super.onVisibleChanged(item);
        final boolean visible = item.getVisible();

        synchronized (this) {
            // labels
            if (_centerLabel != null) {
                _features.labels.setVisible(_centerLabel.id, visible);
            }
            if (_floatingLabel != null) {
                _features.labels.setVisible(_floatingLabel.id, visible);
            }
            for (SegmentLabel label : _segmentLabels) {
                _features.labels.setVisible(label.id, visible);
            }
        }
    }

    @Override
    public void onHeightChanged(MapItem item) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);
    }

    @Override
    public void onAltitudeModeChanged(Feature.AltitudeMode altitudeMode) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);
    }

    @Override
    public void onHeightStyleChanged(Polyline p) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);
    }

    @Override
    public void onLabelsChanged(Polyline p) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onLabelTextSizeChanged(Polyline p) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onBasicLineStyleChanged(Shape p) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onStyleChanged(Shape s) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore3.PROPERTY_FEATURE_GEOMETRY |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);
    }

    @Override
    public void onStrokeColorChanged(Shape s) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onFillColorChanged(Shape s) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onStrokeWeightChanged(Shape s) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onPointsChanged(Shape s) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY |
                        FeatureDataStore3.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onMetadataChanged(MapItem item, String field) {
        super.onMetadataChanged(item, field);
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    static boolean hasHeight(Polyline polyline) {
        final double height = polyline.getHeight();
        final int heightStyle = polyline.getHeightStyle();
        return !Double.isNaN(height)
                && Double.compare(height, 0) != 0
                && heightStyle != Polyline.HEIGHT_STYLE_NONE;
    }

    private static void toFeature(long fsid, Polyline polyline,
            long[] fids, Feature[] feature) {
        final int style = polyline.getStyle();
        boolean fill = MathUtils.hasBits(style, Shape.STYLE_FILLED_MASK);
        final boolean stroke = MathUtils.hasBits(style,
                Shape.STYLE_STROKE_MASK);
        int fillColor = polyline.getFillColor();
        int strokeColor = polyline.getStrokeColor();
        final float strokeWeight = (float) polyline.getStrokeWeight();
        final int basicStyle = polyline.getBasicLineStyle();
        final double height = polyline.getHeight();
        final int heightStyle = polyline.getHeightStyle();
        final boolean hasHeight = hasHeight(polyline);

        boolean closed = (style & Polyline.STYLE_CLOSED_MASK) != 0;

        final boolean outlineStroke = ((style
                & Polyline.STYLE_OUTLINE_STROKE_MASK) != 0);
        final boolean outlineHalo = ((style
                & Polyline.STYLE_OUTLINE_HALO_MASK) != 0);
        final int basicLineStyle = basicStyle;

        // XXX - seems to match legacy observed, but property configuration does not seem fully
        //       formed
        // extruded wall
        if (hasHeight && heightStyle != 0 && !closed) {
            // force fill
            if (!fill) {
                fill = true;

                // default alpha from `GLPolyline.getExtrudedPolyColor()
                int defaultAlphaMask = 0x7F;
                int strokeAlphaMask = (strokeColor & 0xFF000000) >>> 24;

                // slight deviation from `GLPolyline` as we are selecting the minimum alpha value
                // of the stroke and the default
                fillColor = (Math.min(defaultAlphaMask, strokeAlphaMask) << 24)
                        | (strokeColor & 0x00FFFFFF);
            }
        }
        int renderColor = strokeColor;
        if (MathUtils.hasBits(polyline.getRenderHints(),
                Polyline.RENDER_FLAG_FADE_ITEM)) {
            renderColor = 0x32FFFFFF & strokeColor;
        }

        final int extrudeStrokeStyle = MathUtils.hasBits(heightStyle,
                Polyline.HEIGHT_STYLE_OUTLINE) ? BasicStrokeStyle.EXTRUDE_VERTEX
                        : BasicStrokeStyle.EXTRUDE_NONE;

        final boolean hasExtrudePlusSurfaceFeature = (hasHeight
                && heightStyle != 0 &&
                !MathUtils.hasBits(heightStyle,
                        Polyline.HEIGHT_STYLE_TOP_ONLY));

        Style s;
        if (basicStyle == Polyline.BASIC_LINE_STYLE_DASHED)
            s = new PatternStrokeStyle(1, (short) 0x3F3F,
                    Color.red(renderColor) / 255f,
                    Color.green(renderColor) / 255f,
                    Color.blue(renderColor) / 255f,
                    Color.alpha(renderColor) / 255f,
                    strokeWeight,
                    extrudeStrokeStyle);
        else if (basicStyle == Polyline.BASIC_LINE_STYLE_DOTTED)
            s = new PatternStrokeStyle(1, (short) 0x0303,
                    Color.red(renderColor) / 255f,
                    Color.green(renderColor) / 255f,
                    Color.blue(renderColor) / 255f,
                    Color.alpha(renderColor) / 255f,
                    strokeWeight,
                    extrudeStrokeStyle);
        else if (basicStyle == Polyline.BASIC_LINE_STYLE_OUTLINED) {
            BasicStrokeStyle bg = new BasicStrokeStyle(
                    0xFF000000 & renderColor, strokeWeight + 2f,
                    extrudeStrokeStyle);
            s = new CompositeStyle(new Style[] {
                    bg,
                    new BasicStrokeStyle(renderColor, strokeWeight,
                            extrudeStrokeStyle)
            });
        } else {
            s = new BasicStrokeStyle(renderColor, strokeWeight,
                    extrudeStrokeStyle);
        }
        final Style strokeStyle = s;

        int numStyles = 0;
        if (fill)
            numStyles++;
        if (outlineStroke)
            numStyles++;
        if (outlineHalo)
            numStyles += 2;

        ArrayList<Style> composite = (numStyles > 0)
                ? new ArrayList<>(numStyles + 1)
                : null;

        if (composite != null) {
            if (fill) {
                // the shape is closed. has the extruded-plus-surface-feature representation. and is
                // not opaque. halve the fill alpha so each the surface and extrusion contribute to
                // the translucency. This gives full alpha from the nadir view
                if (closed && hasExtrudePlusSurfaceFeature
                        && !MathUtils.hasBits(fillColor, 0xFF000000)) {
                    fillColor = ((fillColor >>> 1) & 0xFF000000)
                            | (fillColor & 0x00FFFFFF);
                }
                composite.add(new BasicFillStyle(fillColor));
            }
            if (outlineStroke) {
                composite
                        .add(new BasicStrokeStyle(strokeColor & 0xFF000000,
                                strokeWeight + 2f, extrudeStrokeStyle));
            }
            if (outlineHalo) {
                composite.add(new BasicStrokeStyle(
                        (strokeColor & 0x00FFFFFF)
                                | (strokeColor & 0xFF000000 >>> 3),
                        strokeWeight + 10f, extrudeStrokeStyle));
                composite.add(new BasicStrokeStyle(
                        (strokeColor & 0x00FFFFFF)
                                | (strokeColor & 0xFF000000 >>> 2),
                        strokeWeight + 4f, extrudeStrokeStyle));
            }

            composite.add(s);
            s = new CompositeStyle(
                    composite.toArray(new Style[0]));
        }

        final GeoPoint center = polyline.getCenter().get();
        final GeoPoint[] points = polyline.getPoints();

        final double centerPtAlt = Double.isNaN(center.getAltitude()) ? 0d
                : center.getAltitude();
        final LineString ls = new LineString(3);

        double ht = hasHeight && MathUtils.hasBits(heightStyle,
                Polyline.HEIGHT_STYLE_TOP_ONLY) ? height : 0d;
        for (GeoPoint gp : points) {
            ls.addPoint(gp.getLongitude(), gp.getLatitude(),
                    (Double.isNaN(gp.getAltitude()) ? centerPtAlt
                            : gp.getAltitude()) + ht);
        }
        if (points.length > 0 && closed) {
            final GeoPoint gp = points[0];
            ls.addPoint(gp.getLongitude(), gp.getLatitude(),
                    (Double.isNaN(gp.getAltitude()) ? centerPtAlt
                            : gp.getAltitude()) + ht);
        }

        feature[0] = new Feature(
                fsid,
                fids[0],
                null,
                closed ? new Polygon(ls) : ls,
                s,
                null,
                hasHeight && heightStyle != 0 ? Feature.AltitudeMode.Absolute
                        : polyline.getAltitudeMode(),
                MathUtils.hasBits(heightStyle, Polyline.HEIGHT_STYLE_TOP_ONLY)
                        ? 0d
                        : height,
                FeatureDataStore2.TIMESTAMP_NONE,
                feature[0] != null ? feature[0].getVersion() + 1L
                        : FeatureDataStore2.FEATURE_ID_NONE);

        // if the shape is extruded, add the surface representation as well
        if (hasHeight && heightStyle != 0 && !MathUtils.hasBits(heightStyle,
                Polyline.HEIGHT_STYLE_TOP_ONLY) &&
                !polyline.getCenter().getAltitudeSource()
                        .equals(GeoPointMetaData.USER)) {
            feature[1] = new Feature(
                    fsid,
                    fids[1],
                    null,
                    feature[0].getGeometry(),
                    s,
                    null,
                    Feature.AltitudeMode.ClampToGround,
                    0d,
                    FeatureDataStore2.TIMESTAMP_NONE,
                    feature[1] != null ? feature[1].getVersion() + 1L
                            : FeatureDataStore2.FEATURE_ID_NONE);
        } else {
            feature[1] = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private SegmentLabel buildTextLabel(
            GeoPoint startGeo,
            GeoPoint endGeo,
            String text) {

        final int labelTextSize = _subject.getLabelTextSize();
        final Typeface labelTypeface = _subject.getLabelTypeface();

        MapTextFormat textFormat = new MapTextFormat(labelTypeface,
                labelTextSize);
        SegmentLabel lbl = new SegmentLabel(_features.labels, text,
                textFormat, _subject.getVisible());

        _features.labels.setText(lbl.id, text);
        lbl.setAltitudeMode(_subject.getAltitudeMode());
        lbl.setGeoPoints(startGeo, endGeo);

        return lbl;
    }

    private SegmentLabel buildTextLabel(
            Geometry geom,
            String text) {

        final int labelTextSize = _subject.getLabelTextSize();
        final Typeface labelTypeface = _subject.getLabelTypeface();

        MapTextFormat textFormat = new MapTextFormat(labelTypeface,
                labelTextSize);
        SegmentLabel lbl = new SegmentLabel(_features.labels, text,
                textFormat, _subject.getVisible());

        _features.labels.setText(lbl.id, text);
        lbl.setAltitudeMode(_subject.getAltitudeMode());
        _features.labels.setGeometry(lbl.id, geom);

        return lbl;
    }

    private void validateFloatingLabel(String lineLabel) {
        final int labelTextSize = _subject.getLabelTextSize();
        final Typeface labelTypeface = _subject.getLabelTypeface();
        MapTextFormat textFormat = new MapTextFormat(labelTypeface,
                labelTextSize);

        if (_floatingLabel == null) {
            _floatingLabel = new SegmentLabel(_features.labels,
                    lineLabel, textFormat, _subject.getVisible());
            _features.labels.setHints(_floatingLabel.id,
                    _features.labels.getHints(_floatingLabel.id)
                            | GLLabelManager.HINT_DUPLICATE_ON_SPLIT);

        }

        // XXX - refresh text format
        _features.labels.setGeometry(_floatingLabel.id,
                _feature[0].getGeometry());
        _floatingLabel.setAltitudeMode(_subject.getAltitudeMode());

        _features.labels.setText(_floatingLabel.id,
                lineLabel);
        _floatingLabel.text = lineLabel;
        _floatingLabel.dirty = false;
    }

    /**
     * Display the label on the center of the middle visible segment
     */
    private void validateCenterSegmentLabel(String centerLabelText) {
        // get the text to display, or return if there is none

        final String clt = centerLabelText;

        if (clt == null || clt.isEmpty())
            return;

        _segmentLabels.add(
                buildTextLabel(
                        _feature[0].getGeometry(),
                        clt));
    }

    private void validateSegmentLabels() {
        // XXX - support recomputing `middle`

        AttributeSet segmentLabels = _subject.getLabels();

        // XXX - may be able to micro-optimize by using the old labels as a pool by marking as dirty
        //       then evicting all dirty at method exit. This may be more desirable if any kind of
        //       "blinking" occurs on updates during an add/remove cycle
        for (SegmentLabel lbl : _segmentLabels)
            lbl.release();
        _segmentLabels.clear();

        // check for show middle label flag. This will signal to draw the label in the middle
        // visible line segment
        if (_subject.hasMetaValue("centerLabel")) {
            String centerLabelText = null;
            if (segmentLabels != null) {
                AttributeSet labelBundle = null;
                for (String key : segmentLabels.getAttributeNames()) {
                    if (segmentLabels
                            .getAttributeValueType(key) == AttributeSet.class) {
                        labelBundle = segmentLabels
                                .getAttributeSetAttribute(key);
                    }
                }
                if (labelBundle != null)
                    centerLabelText = (String) labelBundle
                            .getStringAttribute("text");
            }
            validateCenterSegmentLabel(centerLabelText);
            return;
        }

        if (segmentLabels != null) {
            AttributeSet labelBundle;
            int segment;
            String text;
            GeoPoint curPoint = GeoPoint.createMutable();
            GeoPoint lastPoint = GeoPoint.createMutable();

            final LineString ls = getPolylineLineString(
                    _feature[0].getGeometry());
            int numPoints = ls.getNumPoints();
            if (MathUtils.hasBits(_subject.getStyle(),
                    Polyline.STYLE_CLOSED_MASK))
                numPoints--;

            double minGSD;
            for (String e : segmentLabels.getAttributeNames()) {
                labelBundle = segmentLabels.getAttributeSetAttribute(e);
                Number segNumber = ((Number) labelBundle
                        .getIntAttribute("segment"));
                if (segNumber != null)
                    segment = segNumber.intValue();
                else
                    segment = -1;

                if (segment < 0 || segment >= numPoints - 1)
                    continue;

                minGSD = OSMUtils.mapnikTileResolution(0);
                if (labelBundle.containsAttribute("min_gsd")) {
                    Number number = ((Number) labelBundle
                            .getDoubleAttribute("min_gsd"));
                    if (number != null)
                        minGSD = number.doubleValue();
                }

                text = (String) labelBundle.getStringAttribute("text");

                if (text == null || text.isEmpty())
                    continue;

                // only draw the text if the label fits within the distance between the end points
                // of the segment. This number is multiplied by 2.5 because circles are polylines
                // and it
                // keeps it so that text can be shown when displaying circles.
                // 4 was chosen because of the number of polylines that make up a circle at this
                // time.
                // It would probably be a good idea to construct a GLCircle in the future?
                // XXX - revisit for the next version

                curPoint.set(ls.getY(segment), ls.getX(segment),
                        ls.getDimension() == 3 ? ls.getZ(segment) : Double.NaN);
                lastPoint.set(ls.getY(segment + 1), ls.getX(segment + 1),
                        ls.getDimension() == 3 ? ls.getZ(segment + 1)
                                : Double.NaN);
                SegmentLabel lbl = buildTextLabel(curPoint,
                        lastPoint, text);
                lbl.setDisplayThresholds(minGSD,
                        OSMUtils.mapnikTileResolution(30));

                _segmentLabels.add(lbl);
            }
        }

    }

    @Override
    synchronized void validateLabel() {
        if (_subject == null)
            return;
        final Feature feature = _feature[0];
        if (feature == null) {
            // mark all feature properties dirty
            _featureDirty.set(
                    FeatureDataStore2.PROPERTY_FEATURE_NAME |
                            FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY |
                            FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                            FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                            FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE);
            return;
        }

        boolean labelsOn = _subject.hasMetaValue("labels_on");

        double centerLabelMinScale = _subject
                .getMetaDouble(
                        "minRenderScale",
                        DEFAULT_MIN_RENDER_SCALE);

        double minRes = _subject.getMetaDouble("minLabelRenderResolution",
                Polyline.DEFAULT_MIN_LABEL_RENDER_RESOLUTION);
        double maxRes = _subject.getMetaDouble("maxLabelRenderResolution",
                Polyline.DEFAULT_MAX_LABEL_RENDER_RESOLUTION);

        try {
            if (_subject.hasMetaValue("centerPointLabel")) {
                final String _text = _subject.getMetaString(
                        "centerPointLabel", "");
                final GeoPoint centerPoint = _subject.getCenter().get();
                if (_centerLabel == null) {
                    final int labelTextSize = _subject.getLabelTextSize();
                    final Typeface labelTypeface = _subject.getLabelTypeface();
                    MapTextFormat textFormat = new MapTextFormat(labelTypeface,
                            labelTextSize);
                    _centerLabel = new SegmentLabel(_features.labels,
                            _text, textFormat, _subject.getVisible());
                    _centerLabel.setGeoPoint(centerPoint);
                } else if (!_centerLabel.text.equals(_text)) {
                    _features.labels.setText(_centerLabel.id, _text);
                    _centerLabel.text = _text;
                }
            } else if (_centerLabel != null) {
                _centerLabel.release();
                _centerLabel = null;
            }

            validateSegmentLabels();
            if (labelsOn) {
                String lineLabel = _subject.getLineLabel();
                if (!isBlank(lineLabel))
                    validateFloatingLabel(lineLabel);
            } else if (_floatingLabel != null) {
                _floatingLabel.release();
                _floatingLabel = null;
            }
        } catch (Exception cme) {
            // catch and ignore - without adding performance penalty to the whole
            // metadata arch. It will clean up on the next draw.
            Log.e(TAG,
                    "concurrent modification of the segment labels occurred during display");
        }

        if (_centerLabel != null)
            _centerLabel.setDisplayThresholds(
                    Math.max(minRes,
                            Globe.getMapResolution(
                                    96d * GLRenderGlobals.getRelativeScaling(),
                                    centerLabelMinScale)),
                    maxRes);
    }

    private static class SegmentLabel {
        int id;
        String text;
        double textAngle;
        boolean visible;
        GLLabelManager _labelManager;
        boolean dirty;

        SegmentLabel(GLLabelManager labelManager, String text,
                MapTextFormat mapTextFormat, boolean visible) {
            this.text = text;
            _labelManager = labelManager;
            id = _labelManager.addLabel(text);
            this.visible = visible;
            _labelManager.setTextFormat(id, mapTextFormat);
            _labelManager.setFill(id, true);
            _labelManager.setBackgroundColor(id, Color.argb(153, 0, 0, 0));
            _labelManager.setVerticalAlignment(id,
                    GLLabelManager.VerticalAlignment.Middle);
            _labelManager.setVisible(id, visible);
        }

        void setGeoPoint(GeoPoint geo) {
            Point geom = new Point(geo.getLongitude(),
                    geo.getLatitude(),
                    geo.getAltitude());
            _labelManager.setGeometry(id, geom);
        }

        void setGeoPoints(GeoPoint start, GeoPoint end) {
            LineString geom = new LineString(3);
            geom.addPoints(new double[] {
                    start.getLongitude(), start.getLatitude(),
                    start.getAltitude(),
                    end.getLongitude(), end.getLatitude(), end.getAltitude(),
            }, 0, 2, 3);
            _labelManager.setGeometry(id, geom);
        }

        void setAltitudeMode(Feature.AltitudeMode altMode) {
            _labelManager.setAltitudeMode(id, altMode);
        }

        void setTextAngle(double textAngle) {
            this.textAngle = textAngle;
            _labelManager.setRotation(id, (float) textAngle, false);
        }

        void updateText(String t) {
            if (t == null && text != null ||
                    t != null && !t.equals(text)) {
                _labelManager.setText(id, t);
                text = t;
            }
        }

        void release() {
            if (id != GLLabelManager.NO_ID) {
                _labelManager.removeLabel(id);
                id = GLLabelManager.NO_ID;
            }
        }

        public void setVisible(boolean visible) {
            if (id != GLLabelManager.NO_ID && visible != this.visible) {
                _labelManager.setVisible(id, visible);
            }
            this.visible = visible;
        }

        void setDisplayThresholds(double minRes, double maxRes) {
            if (id != GLLabelManager.NO_ID) {
                _labelManager.setLevelOfDetail(id,
                        OSMUtils.mapnikTileLevel(minRes),
                        OSMUtils.mapnikTileLevel(maxRes));
            }
        }
    }

    static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    static LineString getPolylineLineString(Geometry g) {
        if (g instanceof LineString)
            return (LineString) g;
        else if (g instanceof Polygon)
            return ((Polygon) g).getExteriorRing();
        else
            return null;
    }
}
