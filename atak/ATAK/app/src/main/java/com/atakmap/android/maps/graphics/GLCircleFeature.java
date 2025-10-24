
package com.atakmap.android.maps.graphics;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.util.Circle;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.math.MathUtils;

public class GLCircleFeature extends GLPolylineFeature {

    public final static GLMapItemFeatureSpi SPI = new GLMapItemFeatureSpi() {
        @Override
        public boolean isSupported(MapItem item) {
            return item instanceof Polyline;
        }

        @Override
        public GLMapItemFeature create(GLMapItemFeatures features,
                MapItem object) {
            if (!(object instanceof Circle))
                return null;
            return new GLCircleFeature(features);
        }
    };

    GLCircleFeature(GLMapItemFeatures features) {
        super(features);
    }

    @Override
    public void startObservingImpl() {
        super.startObservingImpl();
        _subject.addOnMetadataChangedListener("extrudeMode", this);
    }

    @Override
    public void stopObservingImpl() {
        if (_subject != null)
            _subject.removeOnMetadataChangedListener("extrudeMode", this);
        super.stopObservingImpl();
    }

    @Override
    void validateFeatureImpl(int propertiesMask, Feature[] feature) {
        if (!hasHeight(_subject) || _subject
                .getMetaString("extrudeMode", "cylinder").equals("cylinder")) {
            super.validateFeatureImpl(propertiesMask, feature);
        } else {
            final long fsid = 1L;
            final long[] fids = _fids;
            final Circle circle = (Circle) _subject;

            final GeoPoint center = circle.getCenter().get();
            final double radius = circle.getRadius();

            // XXX - center marker?

            final double centerPtAlt = Double.isNaN(center.getAltitude()) ? 0d
                    : center.getAltitude();

            final String asset = "asset:/meshes/"
                    + _subject.getMetaString("extrudeMode", "cylinder")
                    + ".obj";

            // set the mesh scale factors based on the asset. assets are "unit"
            // sized, so dimensions in meters are applied as appropriate to the
            // axes
            float sx;
            float sy;
            float sz;
            switch (asset) {
                case "asset:/meshes/sphere.obj":
                    // sphere is uniform scaled by the diameter
                    sx = sy = sz = 2f * (float) radius;
                    break;
                case "asset:/meshes/cone_up.obj":
                case "asset:/meshes/cone_down.obj":
                case "asset:/meshes/cylinder.obj":
                case "asset:/meshes/dome.obj":
                default:
                    // horizontal scaled by diameter
                    sx = sy = 2f * (float) radius;
                    // vertical scaled by height
                    sz = (float) circle.getHeight();
                    break;
            }

            feature[0] = new Feature(
                    fsid,
                    fids[0],
                    null,
                    new Point(center.getLongitude(), center.getLatitude(),
                            centerPtAlt),
                    new MeshPointStyle(asset, circle.getFillColor(),
                            new float[] {
                                    sx, 0f, 0f, 0f,
                                    0f, sy, 0f, 0f,
                                    0f, 0f, sz, 0f,
                                    0f, 0f, 0f, 1f,
                            }),
                    null,
                    Feature.AltitudeMode.Absolute,
                    0d,
                    FeatureDataStore2.TIMESTAMP_NONE,
                    feature[0] != null ? feature[0].getVersion() + 1L
                            : FeatureDataStore2.FEATURE_ID_NONE);

            // XXX - do we want any surface (stroked) representation?
            feature[1] = null;
        }
    }

    @Override
    public HitTestResult postProcessHitTestResult(MapRenderer3 renderer,
            HitTestQueryParameters params, HitTestResult result) {

        if (!hasHeight(_subject) || _subject
                .getMetaString("extrudeMode", "cylinder").equals("cylinder")) {
            return super.postProcessHitTestResult(renderer, params, result);
        } else {
            return result;
        }
    }
}
