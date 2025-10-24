
package com.atakmap.android.maps.graphics;

import android.graphics.Color;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.Globe;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLText;

import gov.tak.api.commons.graphics.DisplaySettings;
import gov.tak.api.commons.graphics.IIcon;

final class GLMarkerFeature extends GLMapItemFeature implements
        MapItem.OnAltitudeModeChangedListener,
        PointMapItem.OnPointChangedListener,
        Marker.OnIconChangedListener,
        Marker.OnStateChangedListener,
        Marker.OnTitleChangedListener,
        Marker.OnTrackChangedListener,
        Marker.OnStyleChangedListener,
        Marker.OnSummaryChangedListener,
        MapItem.OnHeightChangedListener,
        Marker.OnLabelTextSizeChangedListener,
        Marker.OnLabelPriorityChangedListener {

    public final static GLMapItemFeatureSpi SPI = new GLMapItemFeatureSpi() {
        @Override
        public boolean isSupported(MapItem item) {
            return item instanceof Marker;
        }

        @Override
        public GLMapItemFeature create(GLMapItemFeatures features,
                MapItem object) {
            if (!(object instanceof Marker))
                return null;
            return new GLMarkerFeature(features);
        }
    };

    public static final String TAG = "GLMarkerFeature";

    private final static double DEFAULT_MIN_RENDER_SCALE = (1.0d / 100000.0d);

    Marker _subject;

    int _labelId = GLLabelManager.NO_ID;

    GLMarker2 _alert;

    GLMarkerFeature(GLMapItemFeatures features) {
        super(features, 1);
    }

    @Override
    public void startObservingImpl() {
        _subject = (Marker) super._subject;

        _subject.addOnAltitudeModeChangedListener(this);
        _subject.addOnPointChangedListener(this);
        _subject.addOnIconChangedListener(this);
        _subject.addOnStateChangedListener(this);
        _subject.addOnTitleChangedListener(this);
        _subject.addOnSummaryChangedListener(this);
        _subject.addOnTrackChangedListener(this);
        _subject.addOnStyleChangedListener(this);
        _subject.addOnLabelSizeChangedListener(this);
        _subject.addOnLabelPriorityChangedListener(this);
        _subject.addOnHeightChangedListener(this);
    }

    @Override
    public void stopObservingImpl() {
        _subject.removeOnAltitudeModeChangedListener(this);
        _subject.removeOnPointChangedListener(this);
        _subject.removeOnIconChangedListener(this);
        _subject.removeOnStateChangedListener(this);
        _subject.removeOnTitleChangedListener(this);
        _subject.removeOnSummaryChangedListener(this);
        _subject.removeOnTrackChangedListener(this);
        _subject.removeOnStyleChangedListener(this);
        _subject.removeOnLabelSizeChangedListner(this);
        _subject.removeOnLabelPriorityChangedListener(this);
        _subject.removeOnHeightChangedListener(this);

        synchronized (this) {
            // if alert is currently displayed, force removal
            validateAlert(false);

            if (_feature[0] != null) {
                _callback.unreserveFeatureId(_subject, _feature[0].getId());
                _feature[0] = null;
            }

            if (_labelId != GLLabelManager.NO_ID) {
                _features.labels.removeLabel(_labelId);
                _features.labelIds.remove(_labelId);
                _labelId = GLLabelManager.NO_ID;
            }

            _subject = null;
        }
    }

    @Override
    void validateFeatureImpl(int propertiesMask, Feature[] feature) {
        if (_subject == null)
            return;

        // XXX - re-use existing feature properties per dirty flag
        feature[0] = toFeature(1L, _fids[0], _subject,
                (feature[0] != null) ? feature[0].getVersion() + 1L : 1L);

        // update alert animation
        final boolean isAlert = MathUtils.hasBits(_subject.getStyle(),
                Marker.STYLE_ALERT_MASK);
        validateAlert(isAlert);
    }

    private void validateAlert(boolean isAlert) {
        if (isAlert && _alert == null) {
            // start the alert animation
            final AlertShadow alert = new AlertShadow(_subject);
            _subject.addOnPointChangedListener(alert);
            _subject.addOnStyleChangedListener(alert);

            _alert = new GLMarker2(_features.renderer, alert);
            _alert.startObserving();
            _features.animations.insertItem(_alert);
        } else if (!isAlert && _alert != null) {
            // stop the alert animation
            _features.animations.removeItem(_alert);
            _alert.stopObserving();
            final AlertShadow alert = (AlertShadow) _alert.getSubject();
            _alert = null;

            _subject.removeOnPointChangedListener(alert);
            _subject.removeOnStyleChangedListener(alert);
        }
    }

    // MapItem property callbacks

    @Override
    public void onIconChanged(Marker marker) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onStyleChanged(Marker marker) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onLabelSizeChanged(final Marker marker) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onPointChanged(PointMapItem item) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY |
                        FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onStateChanged(Marker marker) {
        markDirty(
                FeatureDataStore2.PROPERTY_FEATURE_STYLE |
                        FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onTitleChanged(final Marker marker) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onSummaryChanged(Marker marker) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_NAME);
    }

    @Override
    public void onLabelPriorityChanged(Marker marker) {
        if (_labelId != GLLabelManager.NO_ID)
            _features.labels.setPriority(_labelId,
                    marshal(marker.getLabelPriority()));
    }

    @Override
    public void onTrackChanged(Marker marker) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }

    @Override
    public void onHeightChanged(MapItem item) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY);
    }

    @Override
    public void onAltitudeModeChanged(Feature.AltitudeMode altitudeMode) {
        markDirty(
                FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_NAME);
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
        if (_labelId == GLLabelManager.NO_ID) {
            _labelId = _features.labels.addLabel();
            _features.labelIds.put(_labelId, _subject);
        }
        final boolean shouldMarquee = GLMapSurface.SETTING_shortenLabels
                && (_subject.getStyle() & Marker.STYLE_MARQUEE_TITLE_MASK) != 0;
        final int labelHints = _features.labels.getHints(_labelId);
        final int scrollingLabelHints = shouldMarquee
                ? labelHints | GLLabelManager.HINT_SCROLLING_TEXT
                : labelHints & ~GLLabelManager.HINT_SCROLLING_TEXT;
        if (labelHints != scrollingLabelHints)
            _features.labels.setHints(_labelId, scrollingLabelHints);

        MapTextFormat mapTextFormat = new MapTextFormat(
                _subject.getLabelTypeface(),
                _subject.getLabelTextSize());
        _features.labels.setTextFormat(_labelId, mapTextFormat);
        _features.labels.setFill(_labelId, true);
        _features.labels.setBackgroundColor(_labelId,
                Color.argb(153 /*=60%*/, 0, 0, 0));
        _features.labels.setPriority(_labelId,
                marshal(_subject.getLabelPriority()));

        // text render flag & label display thresholds

        final int textRenderFlag = _subject.getTextRenderFlag();
        // Text empty or labels set to never show
        final boolean isHide = (textRenderFlag == Marker.TEXT_STATE_NEVER_SHOW
                ||
                !GLMapSurface.SETTING_displayLabels);
        _features.labels.setVisible(_labelId, _subject.getVisible() && !isHide);

        // NOTE: naming for the display thresholds is adopted from legacy `GLMarker2` and `Marker`.
        // The use of "min" and "max" are misnomer and are reversed from common understanding
        // elsewhere.

        // Ensure map resolution is within range
        double maxZoomLevelRes = _subject.getMetaDouble(
                "minLabelRenderResolution",
                Marker.DEFAULT_MIN_LABEL_RENDER_RESOLUTION);
        double minZoomLevelRes = _subject.getMetaDouble(
                "maxLabelRenderResolution",
                Marker.DEFAULT_MAX_LABEL_RENDER_RESOLUTION);
        // Legacy min render scale
        if (_subject.hasMetaValue("minRenderScale")) {
            final double minScale = _subject.getMetaDouble("minRenderScale",
                    DEFAULT_MIN_RENDER_SCALE);
            minZoomLevelRes = Globe.getMapResolution(
                    GLRenderGlobals.getRelativeScaling() * 96d, minScale);
        }

        // Always show label
        if (textRenderFlag == Marker.TEXT_STATE_ALWAYS_SHOW) {
            minZoomLevelRes = OSMUtils.mapnikTileResolution(0);
            maxZoomLevelRes = 0d;
        }

        _features.labels.setLevelOfDetail(_labelId,
                OSMUtils.mapnikTileLevel(minZoomLevelRes),
                OSMUtils.mapnikTileLevel(maxZoomLevelRes));

        _features.labels.setGeometry(_labelId, feature.getGeometry());
        _features.labels.setAltitudeMode(_labelId, feature.getAltitudeMode());

        _features.labels.setColor(_labelId, _subject.getTextColor());
        if (_subject.getIconVisibility() == Marker.ICON_VISIBLE) {
            final IIcon icon = _subject.getIcon();
            double offsety = 24d;
            if (icon != null)  {
                double iconHeight = 40d;
                double anchorY = -(iconHeight / 2d);

                // reset to half icon height
                if(icon.getHeight() > 0)
                    iconHeight = icon.getHeight();
                // adjust by anchor
                if(icon.getAnchorY() != IIcon.ANCHOR_CENTER)
                    anchorY = icon.getAnchorY();

                // reset to the iconheight adjusted by the anchor, plus a small pad
                offsety = anchorY + iconHeight + 4d * DisplaySettings.getRelativeScaling();
            }
            _features.labels.setDesiredOffset(_labelId, 0d, offsety, 0d);
            _features.labels.setVerticalAlignment(_labelId,
                    GLLabelManager.VerticalAlignment.Top);
            _features.labels.setHitTestable(_labelId, false);
        } else {
            _features.labels.setDesiredOffset(_labelId, 0d, 0d, 0d);
            _features.labels.setVerticalAlignment(_labelId,
                    GLLabelManager.VerticalAlignment.Middle);
            _features.labels.setHitTestable(_labelId, true);
        }

        // NOTE: `GLLabelManager` performs localization as an implementation detail
        String text = _subject.getTitle();
        if (text == null)
            text = "";

        if (_labelId != GLLabelManager.NO_ID) {
            String fullText = text;
            final String extraLines = _subject.getSummary();
            if (extraLines != null && !extraLines.isEmpty()) {
                fullText = fullText + "\n" + extraLines;
            }

            _features.labels.setText(_labelId, fullText);
        }
    }

    @Override
    public void onVisibleChanged(MapItem item) {
        super.onVisibleChanged(item);

        synchronized (this) {
            if (_labelId != GLLabelManager.NO_ID) {
                final int textRenderFlag = _subject.getTextRenderFlag();
                // Text empty or labels set to never show
                final boolean isHide = (textRenderFlag == Marker.TEXT_STATE_NEVER_SHOW
                        ||
                        !GLMapSurface.SETTING_displayLabels);
                _features.labels.setVisible(_labelId,
                        _subject.getVisible() && !isHide);
            }
        }
    }

    private static Feature toFeature(long fsid, long fid, Marker marker,
            long version) {
        final GeoPoint lla = GeoPoint.createMutable();
        lla.set(marker.getPoint());
        if (Double.isNaN(lla.getAltitude()))
            lla.set(0d);
        final double height = marker.getHeight();
        if (!Double.isNaN(height))
            lla.set(lla.getAltitude() + height);
        Style[] style = new Style[2];
        int numStyles = 0;
        IIcon icon = marker.getIcon();
        do {
            if (icon == null
                    || marker.getIconVisibility() != Marker.ICON_VISIBLE) {
                style[numStyles++] = new BasicPointStyle(0, 1f);
                break;
            }
            final int state = marker.getState();
            final int styleMask = marker.getStyle();
            double iconRotation = Double.NaN;
            final boolean isRotate = MathUtils.hasBits(styleMask,
                    Marker.STYLE_ROTATE_HEADING_MASK);
            if (((styleMask & Marker.STYLE_ROTATE_HEADING_NOARROW_MASK) != 0 &&
                    isRotate) ||
                    marker.getUID().equals(MapView.getDeviceUid())) {

                // rotate the icon
                iconRotation = marker.getTrackHeading();
            } else if (isRotate) {
                final double heading = marker.getTrackHeading();
                if (!Double.isNaN(heading)) {
                    // apply heading arrow
                    final float arrowOffset = 64f;
                    style[numStyles++] = new IconPointStyle(
                            icon.getColor(state),
                            "asset://icons/track_heading_arrow.png",
                            arrowOffset,
                            arrowOffset,
                            0, 0,
                            (float) -heading,
                            true);
                }
            }

            int alignX = 0;
            int alignY = 0;
            int anchorX = 0;
            int anchorY = 0;

            if (icon.getAnchorX() != IIcon.ANCHOR_CENTER) {
                alignX = 1;
                anchorX = -(int) (icon.getAnchorX());
                // NOTE: `GLIcon` only applies relative scaling for anchor >= 0
                // Undo the scaling here for consistency with legacy
                // `GLMarker2` as feature renderer applies as an implementation
                // detail
                if (icon.getAnchorX() < 0)
                    anchorX /= GLRenderGlobals.getRelativeScaling();
            }

            if (icon.getAnchorY() != IIcon.ANCHOR_CENTER) {
                alignY = 1;
                anchorY = (int) (icon.getAnchorY());
                // NOTE: `GLIcon` only applies relative scaling for anchor >= 0
                // Undo the scaling here for consistency with legacy
                // `GLMarker2` as feature renderer applies as an implementation
                // detail
                if (icon.getAnchorY() < 0)
                    anchorY /= GLRenderGlobals.getRelativeScaling();
            }

            // apply icon
            style[numStyles++] = new IconPointStyle(
                    icon.getColor(state),
                    icon.getImageUri(state),
                    Math.max(0, icon.getWidth()),
                    Math.max(0, icon.getHeight()),
                    anchorX,
                    anchorY,
                    alignX, alignY,
                    Double.isNaN(iconRotation) ? 0f : (float) -iconRotation,
                    !Double.isNaN(iconRotation));
        } while (false);

        return new Feature(
                fsid,
                fid,
                null,
                new Point(lla.getLongitude(), lla.getLatitude(),
                        lla.getAltitude()),
                numStyles == 1 ? style[0] : new CompositeStyle(style),
                null,
                marker.getAltitudeMode(),
                -1,
                FeatureDataStore2.TIMESTAMP_NONE,
                version + 1L);
    }

    static GLLabelManager.Priority marshal(Marker.LabelPriority p) {
        if (p == Marker.LabelPriority.High)
            return GLLabelManager.Priority.High;
        else if (p == Marker.LabelPriority.Low)
            return GLLabelManager.Priority.Low;
        else
            return GLLabelManager.Priority.Standard;
    }

    final static class AlertShadow extends Marker implements
            PointMapItem.OnPointChangedListener, Marker.OnStyleChangedListener {
        final Marker _subject;

        AlertShadow(Marker subject) {
            super(subject.getPoint(), subject.getUID() + ".alert");

            _subject = subject;

            setStyle(_subject.getStyle());

            setIcon(null);
            setIconVisibility(Marker.ICON_GONE);
            setColor(0);
            setTitle(null);
            setTextRenderFlag(Marker.TEXT_STATE_NEVER_SHOW);
        }

        @Override
        public void onStyleChanged(Marker marker) {
            setStyle(marker.getStyle());
        }

        @Override
        public void onPointChanged(PointMapItem item) {
            setPoint(item.getPoint());
        }
    }
}
