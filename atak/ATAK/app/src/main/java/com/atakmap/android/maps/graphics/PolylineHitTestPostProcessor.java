
package com.atakmap.android.maps.graphics;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.Vector2D;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

abstract class PolylineHitTestPostProcessor {

    public static final String TAG = "PolylineHitTestPostProcessor";

    interface TouchBox {
        boolean contains(double x, double y);

        boolean intersects(double x0, double y0, double x1, double y1);
    }

    static abstract class LineString {
        final com.atakmap.map.layer.feature.geometry.LineString _linestring;

        LineString(
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            _linestring = linestring;
        }

        final int getNumPoints() {
            return _linestring.getNumPoints();
        }

        /**
         * Returns the {@code i}th vertex, in the coordinate system
         * @param i
         * @param xyz
         */
        abstract void get(int i, PointD xyz);

        /**
         * Returns the {@code i}th vertex, as a {@link GeoPoint}
         * @param i
         * @param lla
         */
        void get(int i, GeoPoint lla) {
            lla.set(_linestring.getY(i), _linestring.getX(i));
            if (_linestring.getDimension() == 3)
                lla.set(_linestring.getZ(i));
        }
    }

    abstract PointD getTouchPoint(HitTestQueryParameters params);

    abstract TouchBox getTouchBox(HitTestQueryParameters params);

    abstract LineString getLineString(
            com.atakmap.map.layer.feature.geometry.LineString linestring);

    final static class ScreenSpace extends PolylineHitTestPostProcessor {
        final MapRenderer3 _renderer;
        final Feature.AltitudeMode _altitudeMode;

        ScreenSpace(MapRenderer3 scene, Feature.AltitudeMode altitudeMode) {
            _renderer = scene;
            _altitudeMode = altitudeMode;
        }

        @Override
        PointD getTouchPoint(HitTestQueryParameters params) {
            return new PointD(params.point.x, params.point.y);
        }

        @Override
        TouchBox getTouchBox(final HitTestQueryParameters params) {
            return new TouchBox() {
                @Override
                public boolean contains(double x, double y) {
                    return params.rect.contains((float) x, (float) y);
                }

                @Override
                public boolean intersects(double x0, double y0, double x1,
                        double y1) {
                    final float bottom = (float) Math.min(y1, y0);
                    final float left = (float) Math.min(x1, x0);
                    final float top = (float) Math.max(y1, y0);
                    final float right = (float) Math.max(x1, x0);
                    return params.rect.intersects(left, bottom, right, top);
                }
            };
        }

        @Override
        LineString getLineString(
                final com.atakmap.map.layer.feature.geometry.LineString linestring) {
            if (_altitudeMode == Feature.AltitudeMode.Relative) {
                return linestring.getDimension() == 3
                        ? new ScreenSpaceLS3DRelative(_renderer, linestring)
                        : new ScreenSpaceLS2DRelative(_renderer, linestring);
            } else {
                return linestring.getDimension() == 3
                        ? new ScreenSpaceLS3D(_renderer, linestring)
                        : new ScreenSpaceLS2D(_renderer, linestring);
            }
        }
    }

    final static class Surface extends PolylineHitTestPostProcessor {
        final GeoPoint lla = GeoPoint.createMutable();

        Surface() {
        }

        @Override
        PointD getTouchPoint(HitTestQueryParameters params) {
            return new PointD(params.geo.getLongitude(),
                    params.geo.getLatitude());
        }

        @Override
        TouchBox getTouchBox(final HitTestQueryParameters params) {
            return new TouchBox() {
                @Override
                public boolean contains(double x, double y) {
                    return params.bounds.contains(y, x, Double.NaN);
                }

                @Override
                public boolean intersects(double x0, double y0, double x1,
                        double y1) {
                    final float south = (float) Math.min(y1, y0);
                    final float west = (float) Math.min(x1, x0);
                    final float north = (float) Math.max(y1, y0);
                    final float east = (float) Math.max(x1, x0);
                    return params.bounds.intersects(north, west, south, east);
                }
            };
        }

        @Override
        LineString getLineString(
                final com.atakmap.map.layer.feature.geometry.LineString linestring) {
            return new GeoLS2D(linestring);
        }
    }

    final static class GeoLS2D extends LineString {
        GeoLS2D(com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(linestring);
        }

        @Override
        public void get(int i, PointD xyz) {
            xyz.x = _linestring.getX(i);
            xyz.y = _linestring.getY(i);
        }
    }

    abstract static class ScreenSpaceLS extends LineString {
        final MapRenderer3 _renderer;
        final MapSceneModel _scene;
        final GeoPoint lla = GeoPoint.createMutable();

        ScreenSpaceLS(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(linestring);
            _renderer = renderer;
            _scene = _renderer.getMapSceneModel(true,
                    _renderer.getDisplayOrigin());
        }
    }

    final static class ScreenSpaceLS2D extends ScreenSpaceLS {
        ScreenSpaceLS2D(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(renderer, linestring);
        }

        @Override
        public void get(int i, PointD xyz) {
            lla.set(_linestring.getY(i), _linestring.getX(i));
            _scene.forward(lla, xyz);
        }
    }

    final static class ScreenSpaceLS3D extends ScreenSpaceLS {
        ScreenSpaceLS3D(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(renderer, linestring);
        }

        @Override
        public void get(int i, PointD xyz) {
            lla.set(_linestring.getY(i), _linestring.getX(i),
                    _linestring.getZ(i));
            _scene.forward(lla, xyz);
        }
    }

    abstract static class ScreenSpaceLSRelative extends ScreenSpaceLS {
        final GLMapView _view;

        ScreenSpaceLSRelative(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(renderer, linestring);
            _view = (GLMapView) renderer;
        }

        final double getElevation(double lat, double lng) {
            return _view.getTerrainMeshElevation(lat, lng);
        }

        @Override
        final void get(int i, GeoPoint lla) {
            super.get(i, lla);
            lla.set(lla.getAltitude()
                    + getElevation(lla.getLatitude(), lla.getLongitude()));
        }
    }

    final static class ScreenSpaceLS3DRelative extends ScreenSpaceLSRelative {
        ScreenSpaceLS3DRelative(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(renderer, linestring);
        }

        @Override
        public void get(int i, PointD xyz) {
            final double lat = _linestring.getY(i);
            final double lng = _linestring.getX(i);
            final double alt = _linestring.getZ(i) + getElevation(lat, lng);
            lla.set(lat, lng, alt);
            _scene.forward(lla, xyz);
        }
    }

    final static class ScreenSpaceLS2DRelative extends ScreenSpaceLSRelative {
        ScreenSpaceLS2DRelative(MapRenderer3 renderer,
                com.atakmap.map.layer.feature.geometry.LineString linestring) {
            super(renderer, linestring);
        }

        @Override
        public void get(int i, PointD xyz) {
            final double lat = _linestring.getY(i);
            final double lng = _linestring.getX(i);
            final double alt = getElevation(lat, lng);
            lla.set(lat, lng, alt);
            _scene.forward(lla, xyz);
        }
    }

    /**
     * Post processes a hit-test result from the features renderer to populate the various fields
     * used by downstream consumers.
     *
     * @param renderer      The current scene model
     * @param params        The hit-test parameters
     * @param linestring    The hit geometry being processed
     * @param altitudeMode  The altitude mode used for rendering the geometry
     * @param hasFill       A flag indicating whether or not the geometry is filled
     * @param result        The result
     * @return  {@code result}
     */
    static HitTestResult postProcessHitTestResult(MapRenderer3 renderer,
            HitTestQueryParameters params,
            com.atakmap.map.layer.feature.geometry.LineString linestring,
            Feature.AltitudeMode altitudeMode, boolean hasFill,
            HitTestResult result) {
        // seed the result location at the touch point
        result.point = new GeoPoint(params.geo);

        // if the shape is filled, assume fill
        if (hasFill)
            result.type = HitTestResult.Type.FILL;

        final PolylineHitTestPostProcessor processor;
        switch (altitudeMode) {
            case ClampToGround:
                processor = new Surface();
                break;
            case Absolute:
            case Relative:
            default:
                processor = new ScreenSpace(renderer, altitudeMode);
                break;
        }

        final TouchBox touchbox = processor.getTouchBox(params);
        final LineString ls = processor.getLineString(linestring);
        final PointD touchPoint = processor.getTouchPoint(params);

        long ss = System.currentTimeMillis();

        // PROFILING NOTES -- S9TE for surface hit-test
        //  - obtaining the current point is most expensive at 30-35%
        //  - vertex contained and  segment bounds intersect are nominally comparable at 7-8%

        PointD p = new PointD(0d, 0d, 0d);
        ls.get(0, p);
        if (touchbox.contains(p.x, p.y)) {
            result.index = 0;
            result.type = HitTestResult.Type.POINT;
        } else {
            for (int i = 1; i < ls.getNumPoints(); i++) {
                final double x0 = p.x;
                final double y0 = p.y;
                ls.get(i, p);
                final double x1 = p.x;
                final double y1 = p.y;
                // check for vertex touchbox hit
                if (touchbox.contains(x1, y1)) {
                    result.index = i;
                    result.type = HitTestResult.Type.POINT;
                    break;
                } else if (result.index >= 0) {
                    // segment hit-testing is done
                    continue;
                }

                // XXX - segment hit-testing should take into account per-feature tessellation

                // check segment MBB/touchbox hit
                if (!touchbox.intersects(x0, y0, x1, y1))
                    continue;

                // check segment/touchbox hit; we know that neither vertex is contained within
                // the touchbox, so we will find the closest point on the segment to the touch
                // point and see if that is contained in the touchbox

                // adapted from `GLBatchLineString`

                // Find the nearest point on this line based on the point we touched
                Vector2D nearest = Vector2D.nearestPointOnSegment(
                        new Vector2D(touchPoint.x, touchPoint.y),
                        new Vector2D(x0, y0),
                        new Vector2D(x1, y1));
                float nx = (float) nearest.x;
                float ny = (float) nearest.y;
                if (touchbox.contains(nx, ny)) {
                    result.index = i - 1;
                    result.type = HitTestResult.Type.LINE;

                    // interpolate the segment location
                    GeoPoint lla0 = GeoPoint.createMutable();
                    GeoPoint lla1 = GeoPoint.createMutable();
                    ls.get(i - 1, lla0);
                    ls.get(i, lla1);
                    result.point = GeoCalculations.pointAtDistance(
                            lla0,
                            lla1,
                            MathUtils.distance(x0, y0, nx, ny)
                                    / MathUtils.distance(x0, y0, x1, y1));
                }
            }
        }

        long es = System.currentTimeMillis();
        Log.i(TAG,
                "GLPolylineFeature.postProcessHit ["
                        + ls.getClass().getSimpleName() + "] "
                        + ls.getNumPoints() + " in " + (es - ss) + "ms");
        return result;
    }
}
