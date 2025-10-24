
package com.atakmap.android.maps.graphics;

import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.app.R;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.opengl.GLRenderGlobals;

class GLEditablePolylineFeature extends GLPolylineFeature implements
        EditablePolyline.OnEditableChangedListener {

    public final static GLMapItemFeatureSpi SPI = new GLMapItemFeatureSpi() {
        @Override
        public boolean isSupported(MapItem item) {
            return item instanceof Polyline;
        }

        @Override
        public GLMapItemFeature create(GLMapItemFeatures features,
                MapItem object) {
            if (!(object instanceof EditablePolyline))
                return null;
            return new GLEditablePolylineFeature(features);
        }
    };
    private EditablePolyline _subject;

    GLEditablePolylineFeature(GLMapItemFeatures features) {
        super(features, 3);
    }

    @Override
    public void startObservingImpl() {
        super.startObservingImpl();
        _subject = (EditablePolyline) super._subject;
        _subject.addOnEditableChangedListener(this);
    }

    @Override
    public void stopObservingImpl() {
        super.stopObservingImpl();
        _subject.removeOnEditableChangedListener(this);

        synchronized (this) {
            _subject = null;
        }
    }

    @Override
    public HitTestResult postProcessHitTestResult(MapRenderer3 renderer,
            HitTestQueryParameters params, HitTestResult result) {
        result = super.postProcessHitTestResult(renderer, params, result);
        // Use the proper menu based on whether we hit a line or point
        String menu = _subject.getShapeMenu();
        if (_subject.getEditable()) {
            switch (result.type) {
                case FILL:
                    break;
                case LINE:
                    menu = _subject.getLineMenu();
                    break;
                case POINT:
                    menu = _subject.getCornerMenu();
                    break;
                default:
                    break;
            }
        }
        _subject.setRadialMenu(menu);
        return result;
    }

    @Override
    void validateFeatureImpl(int propertiesMask, Feature[] feature) {
        super.validateFeatureImpl(propertiesMask, feature);

        final EditablePolyline subject = _subject;
        if (subject == null)
            return;
        if (_subject == null)
            return;

        final boolean editable = _subject.getEditable();
        final Feature vertices = feature[2];
        if (!editable && vertices != null) {
            feature[2] = null;
        } else if (editable) {
            float pixelSize = (float) Math
                    .ceil(((int) Math.max(4, _subject.getStrokeWeight()) * 3))
                    / GLRenderGlobals.getRelativeScaling();
            feature[2] = toFeature(1L,
                    _fids[2], feature[0],
                    pixelSize,
                    (vertices != null) ? vertices.getVersion() + 1L : 1L);
        }
    }

    private static Feature toFeature(long fsid, long fid, Feature polyline,
            float vertexSize, long version) {
        final LineString linestring = getPolylineLineString(
                polyline.getGeometry());
        GeometryCollection vertices = new GeometryCollection(
                linestring.getDimension());
        for (int i = 0; i < linestring.getNumPoints(); i++) {
            Point p = new Point(0d, 0d);
            linestring.get(p, i);
            vertices.addGeometry(p);
        }

        return new Feature(
                fsid,
                fid,
                null,
                vertices,
                new IconPointStyle(-1, "resource://"
                        + R.drawable.polyline_vertex_cropped,
                        vertexSize, vertexSize,
                        0, 0,
                        0f,
                        false),
                null,
                polyline.getAltitudeMode(),
                polyline.getExtrude(),
                FeatureDataStore2.TIMESTAMP_NONE,
                version);
    }

    @Override
    public void onEditableChanged(EditablePolyline polyline) {
        markDirty(FeatureDataStore2.PROPERTY_FEATURE_STYLE);
    }
}
