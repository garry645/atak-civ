
package com.atakmap.android.imagecapture;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.Toast;

import com.atakmap.android.gridlines.GridLinesMapComponent;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapItem.OnGroupChangedListener;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.android.tilecapture.imagery.ImageryCapturePP;
import com.atakmap.android.tilecapture.imagery.MapItemCapturePP;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MGRSPoint;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;
import com.atakmap.coremap.maps.coords.MutableUTMPoint;
import com.atakmap.coremap.maps.coords.UTMPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.math.MathUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.marshal.MarshalManager;

/**
 * A customizable MGRS-aligned grid
 */

public class CustomGrid extends AbstractLayer implements Capturable {

    enum GridJunctionStrategy {
        Extend,
        MultiGrid,
    }

    private static final String TAG = "CustomGrid";
    public static final String SPACING_PREF = "grg_grid_spacing";
    public static final String COLOR_PREF = "grg_grid_color";
    public static final String UNITS_PREF = "grg_grid_units";
    public static final String LABELS_PREF = "grg_grid_labels";

    private static final DecimalFormat LABEL_FMT = LocaleUtil
            .getDecimalFormat("00000");

    private final static GridJunctionStrategy DEFAULT_GRID_JUNCTION_STRATEGY = GridJunctionStrategy
            .Extend;

    public static final int FLOOR = 0;
    public static final int ROUND = 1;
    public static final int CEIL = 2;

    private final MapView _mapView;
    private GeoPoint _topLeft, _topRight, _bottomRight, _bottomLeft;
    private UTMPoint _topLeftUTM, _topRightUTM, _bottomRightUTM, _bottomLeftUTM;
    private Marker _centerMarker;
    private DoubleBuffer _points;
    private final MutableGeoBounds _bounds = new MutableGeoBounds(0, 0, 0, 0);
    private int _pointCount;
    private long _pointPtr;
    private String[] _labels;
    private double _mSpacing = 100;
    private float _strokeWeight = 2f;
    private int _placeRows, _placeCols;
    private int _xLines, _yLines;
    private int _color = Color.WHITE;
    private boolean _showLabels = true;
    private int _precision = 0;
    private boolean _visible = true;
    private final List<OnChangedListener> _listeners = new ArrayList<>();

    private GridJunctionStrategy _gridJunctionStrategy = DEFAULT_GRID_JUNCTION_STRATEGY;

    /**
     * Construct a custom grid overlay.
     * @param mapView the mapview
     * @param uid the uid
     */
    public CustomGrid(MapView mapView, String uid) {
        super(uid);
        _mapView = mapView;
    }

    /**
     * Sets the four corners of the customized grid.
     * @param corner1 upper left
     * @param corner2 lower right
     */
    public synchronized void setCorners(GeoPointMetaData corner1,
            GeoPointMetaData corner2) {
        alignCorners(corner1, corner2);
    }

    /**
     * Unset the coordinates for the layer.
     */
    public synchronized void clear() {
        setCorners(null, null);
    }

    /**
     * Return true if the coordinates for the layer are valid.
     * @return true if the coordinates are valid.
     */
    public synchronized boolean isValid() {
        return _topLeft != null && _topRight != null
                && _bottomLeft != null && _bottomRight != null;
    }

    @Override
    public synchronized void setVisible(boolean visible) {
        if (_visible != visible) {
            _visible = visible;
            onChanged(false);
        }
        if (_centerMarker != null)
            _centerMarker.setVisible(visible);
    }

    @Override
    public synchronized boolean isVisible() {
        return _visible;
    }

    /**
     * Sets the color of the grid lines used for the Custom grid
     * @param color the color as an integer value where the alpha is ignored.
     */
    public synchronized void setColor(int color) {
        if (Color.alpha(color) == 0)
            color |= 0xFF000000;
        if (_color != color) {
            _color = color;
            onChanged(false);
        }
    }

    /**
     * Returns the color used for the grid lines.   The color will not have any
     * alpha property.
     * @return the color
     */
    public synchronized int getColor() {
        return _color;
    }

    /**
     * Sets the stroke weight of the custom Grid
     * @param strokeWeight the weight
     */
    public void setStrokeWeight(float strokeWeight) {
        _strokeWeight = strokeWeight;
    }

    public float getStrokeWeight() {
        return _strokeWeight;
    }

    public synchronized void setShowLabels(boolean show) {
        if (_showLabels != show) {
            _showLabels = show;
            onChanged(false);
        }
    }

    public synchronized boolean showLabels() {
        return _showLabels;
    }

    public synchronized void setSpacing(double meters) {
        if (Double.compare(meters, _mSpacing) != 0 && meters > 0) {
            _mSpacing = meters;
            _precision = 0;
            onChanged(true);
        }
    }

    /**
     * Return grid spacing
     * @return Spacing between cells in meters
     */
    public synchronized double getSpacing() {
        return _mSpacing;
    }

    public synchronized int getVerticalLineCount() {
        return _xLines;
    }

    public synchronized int getHorizontalLineCount() {
        return _yLines;
    }

    public synchronized int getNumColumns() {
        return _xLines + 1;
    }

    public synchronized int getNumRows() {
        return _yLines + 1;
    }

    public synchronized GeoBounds getBounds() {
        if (!isValid())
            return null;
        _bounds.set(getCorners(), _mapView.isContinuousScrollEnabled());
        return _bounds;
    }

    public synchronized GeoPoint[] getCorners() {
        return new GeoPoint[] {
                _topLeft, _topRight, _bottomRight, _bottomLeft
        };
    }

    public GeoPoint getCenter() {
        GeoBounds bounds = getBounds();
        return bounds != null ? bounds.getCenter(null) : GeoPoint.ZERO_POINT;
    }

    public synchronized Marker getCenterMarker() {
        return _centerMarker;
    }

    synchronized GridJunctionStrategy getGridJunctionStrategy() {
        return _gridJunctionStrategy;
    }

    synchronized void setGridJunctionStrategy(GridJunctionStrategy strategy) {
        _gridJunctionStrategy = strategy;
    }

    /**
     * Define grid extends based on center and size
     * @param c Grid center
     * @param cols Number of grid columns
     * @param rows Number of grid rows
     * @return True if placement successful, false otherwise
     */
    public boolean place(GeoPoint c, int cols, int rows) {
        double s = getSpacing();
        _placeCols = cols;
        _placeRows = rows;
        double hWidth = (cols * s) / 2, hHeight = (rows * s) / 2;

        // Required in case we cross equator and northing doesn't
        // round equally into spacing
        double nGCF = gcf(10000000, s);

        UTMPoint tl, tr, br, bl;
        if(_gridJunctionStrategy == GridJunctionStrategy.MultiGrid) {
            // Calculate top left
            GeoPoint left = GeoCalculations.pointAtDistance(c, 270, hWidth);
            UTMPoint tl_utm = UTMPoint.fromGeoPoint(
                    GeoCalculations.pointAtDistance(left, 0, hHeight));
            if (tl_utm.getZoneDescriptor() == null)
                return false;
            tl_utm = new UTMPoint(tl_utm.getZoneDescriptor(),
                    round(tl_utm.getEasting(), s, ROUND),
                    round(tl_utm.getNorthing(), nGCF, ROUND));
            tl = tl_utm;

            // Calculate top right - walk to point while maintaining alignment
            // Less efficient than a single computeDest call, but more accurate
            tr = tl;
            UTMPoint tr_utm = tl_utm;
            for (int i = 1; i <= cols; i++) {
                tr_utm = UTMPoint.fromGeoPoint(GeoCalculations
                        .pointAtDistance(tr.toGeoPoint(), 90, s));
                if (tr_utm.getZoneDescriptor() == null)
                    return false;
                tr_utm = new UTMPoint(tr_utm.getZoneDescriptor(),
                        round(tr_utm.getEasting(), s, ROUND),
                        round(tr_utm.getNorthing(), nGCF, ROUND));
                tr = tr_utm;
            }

            // Calculate bottom right
            br = tr;
            UTMPoint br_utm = tr_utm;
            for (int i = 1; i <= rows; i++) {
                br_utm = UTMPoint.fromGeoPoint(GeoCalculations
                        .pointAtDistance(br.toGeoPoint(), 180, s));
                if (br_utm.getZoneDescriptor() == null)
                    return false;
                br_utm = new UTMPoint(br_utm.getZoneDescriptor(),
                        round(br_utm.getEasting(), s, ROUND),
                        round(br_utm.getNorthing(), nGCF, ROUND));
                br = br_utm;
            }

            // Calculate bottom left
            bl = tl;
            UTMPoint bl_utm = tl_utm;
            for (int i = 1; i <= rows; i++) {
                bl_utm = UTMPoint.fromGeoPoint(GeoCalculations
                        .pointAtDistance(bl.toGeoPoint(), 180, s));
                if (bl_utm.getZoneDescriptor() == null)
                    return false;
                bl_utm = new UTMPoint(bl_utm.getZoneDescriptor(),
                        round(bl_utm.getEasting(), s, ROUND),
                        round(bl_utm.getNorthing(), nGCF, ROUND));
                bl = bl_utm;
            }

            tr = tr_utm;
            bl = bl_utm;
            br = br_utm;
        } else { // extend grid
            // the center is considered the reference point and defines the primary major grid. The
            // grid is then _extended_ in the same major grid per NGA STND 0037 v2.0.0
            UTMPoint center = UTMPoint.fromGeoPoint(c);

            // grid align the center
            center = new UTMPoint(center.getZoneDescriptor(),
                    round(center.getEasting(), s, ROUND),
                    round(center.getNorthing(), nGCF, ROUND));

            // derive the corners by extending the grid by the requested spacing and cell count
            // along major grid neatlines extending from the grid aligned reference point
            tl = new UTMPoint(center.getZoneDescriptor(),
                    center.getEasting() - hWidth,
                    center.getNorthing() + hHeight);
            tr = new UTMPoint(center.getZoneDescriptor(),
                    center.getEasting() + hWidth,
                    center.getNorthing() + hHeight);
            br = new UTMPoint(center.getZoneDescriptor(),
                    center.getEasting() + hWidth,
                    center.getNorthing() - hHeight);
            bl = new UTMPoint(center.getZoneDescriptor(),
                    center.getEasting() - hWidth,
                    center.getNorthing() - hHeight);
        }

        // Check that grid isn't wrapping around the world
        do {
            if(_mapView.isContinuousScrollEnabled())
                break;
            final GeoPoint tl_ll = tl.toGeoPoint();
            final GeoPoint tr_ll = tr.toGeoPoint();
            final GeoPoint br_ll = br.toGeoPoint();
            final GeoPoint bl_ll = bl.toGeoPoint();
            if (Math.abs(tl_ll.getLongitude() - tr_ll.getLongitude()) <= 180)
                break;
            if(tl_ll.getLongitude() > 180 || tl_ll.getLongitude() < -180
                || tr_ll.getLongitude() > 180 || tr_ll.getLongitude() < -180
                || bl_ll.getLongitude() > 180 || bl_ll.getLongitude() < -180
                || br_ll.getLongitude() > 180 || br_ll.getLongitude() < -180) {

                return false;
            }
        } while(false);

        updateCorners(tl, tr, bl, br);
        return true;
    }

    public boolean place(GeoPoint c) {
        int rows = _placeRows;
        if (rows <= 0)
            rows = getNumRows();
        int cols = _placeCols;
        if (cols <= 0)
            cols = getNumColumns();
        return rows > 0 && cols > 0 && place(c, cols, rows);
    }

    /**
     * Align corners to MGRS grid
     */
    private boolean alignCorners(GeoPointMetaData corner1,
            GeoPointMetaData corner2) {
        if (corner1 == null || corner2 == null) {
            // Grid cleared
            updateCorners(null, null, null, null);
            return false;
        }
        // Corner 1 is the top-left corner
        // Corner 2 is the bottom-right corner
        GeoBounds bounds = new GeoBounds(corner1.get(), corner2.get());
        double s = getSpacing();

        // Need to compute relative to center or everything is off
        corner1 = GeoPointMetaData
                .wrap(new GeoPoint(bounds.getNorth(), bounds.getWest()));
        corner2 = GeoPointMetaData
                .wrap(new GeoPoint(bounds.getSouth(), bounds.getEast()));

        UTMPoint tl = UTMPoint.fromGeoPoint(corner1.get()), br = UTMPoint
                .fromGeoPoint(corner2.get());

        if (tl.getZoneDescriptor() == null || br.getZoneDescriptor() == null)
            return false;

        if(tl.getLngZone() != br.getLngZone() &&
                _gridJunctionStrategy == GridJunctionStrategy.Extend) {

            UTMPoint c = UTMPoint.fromGeoPoint(bounds.getCenter(null));
            final double halfWidth;
            final double halfHeight;
            if(c.getLngZone() == tl.getLngZone()) {
                halfWidth = c.getEasting()-tl.getEasting();
                halfHeight = tl.getNorthing()-c.getNorthing();
            } else if(c.getLngZone() == br.getLngZone()) {
                halfWidth = br.getEasting()-c.getEasting();
                halfHeight = c.getNorthing()-br.getNorthing();
            } else {
                // degenerate case, not expected
                halfWidth = corner1.get().distanceTo(
                        new GeoPoint(bounds.getNorth(), bounds.getEast())) / 2d;
                halfHeight = corner1.get().distanceTo(
                        new GeoPoint(bounds.getSouth(), bounds.getWest())) / 2d;
            }

            tl = new UTMPoint(c.getZoneDescriptor(),
                    c.getEasting()-halfWidth,
                    c.getNorthing()+halfHeight);
            br = new UTMPoint(c.getZoneDescriptor(),
                    c.getEasting()+halfWidth,
                    c.getNorthing()-halfHeight);
        }

        tl = new UTMPoint(tl.getZoneDescriptor(),
                round(tl.getEasting(), s, FLOOR),
                round(tl.getNorthing(), s, CEIL));
        br = new UTMPoint(br.getZoneDescriptor(),
                round(br.getEasting(), s, CEIL),
                round(br.getNorthing(), s, FLOOR));
        if (tl.toString().equals(br.toString()))
            br = new UTMPoint(tl.getZoneDescriptor(),
                    tl.getEasting() + s, tl.getNorthing() + s);
        UTMPoint tr = new UTMPoint(br.getZoneDescriptor(),
                br.getEasting(), tl.getNorthing());
        UTMPoint bl = new UTMPoint(tl.getZoneDescriptor(),
                tl.getEasting(), br.getNorthing());
        _placeCols = _placeRows = 0;
        updateCorners(tl, tr, bl, br);
        return true;
    }

    /**
     * Update corners and center marker
     * To be called after corners are MGRS-aligned
     * @param topLeft Top left corner point
     * @param bottomRight Bottom right corner point
     */
    private synchronized void updateCorners(UTMPoint topLeft,
            UTMPoint topRight,
            UTMPoint bottomLeft, UTMPoint bottomRight) {
        _topLeftUTM = topLeft;
        _topRightUTM = topRight;
        _bottomRightUTM = bottomRight;
        _bottomLeftUTM = bottomLeft;

        _topLeft = (_topLeftUTM) != null ? _topLeftUTM.toGeoPoint() : null;
        _topRight = (_topRightUTM != null) ?  _topRightUTM.toGeoPoint() : null;
        _bottomLeft = (_bottomLeftUTM != null) ? _bottomLeftUTM.toGeoPoint() : null;
        _bottomRight = (_bottomRightUTM != null) ? _bottomRightUTM.toGeoPoint() : null;
        if (!isValid()) {
            Marker center = _centerMarker;
            _centerMarker = null;
            if (center != null)
                center.removeFromGroup();
            onChanged(true);
            return;
        } else {
            if (_centerMarker == null) {
                _centerMarker = new Marker(getName() + "_marker");
                _centerMarker.setMetaString("callsign", "MGRS Grid");
                _centerMarker.setMetaBoolean("addToObjList", false);
                _centerMarker.setMovable(true);
                _centerMarker.setMetaBoolean("removable", true);
                _centerMarker.setMetaString("iconUri", "icons/grid_center.png");
                _centerMarker.setMetaString("menu", "menus/grid_center.xml");
                _centerMarker
                        .addOnPointChangedListener(
                                new OnPointChangedListener() {
                                    @Override
                                    public void onPointChanged(
                                            PointMapItem item) {
                                        CustomGrid grid = GridLinesMapComponent
                                                .getCustomGrid();
                                        if (grid != null
                                                && item.getGroup() != null) {
                                            GeoPoint gp = item.getPoint();
                                            GeoPoint gridCenter = grid
                                                    .getCenter();
                                            if (gridCenter != null
                                                    && gp.distanceTo(
                                                            gridCenter) > 0.1) {
                                                if (!grid.place(gp)) {
                                                    Toast.makeText(MapView
                                                            .getMapView()
                                                            .getContext(),
                                                            R.string.grid_fail,
                                                            Toast.LENGTH_LONG)
                                                            .show();
                                                    item.setPoint(gridCenter);
                                                }
                                            }
                                        }
                                    }
                                });
                _centerMarker
                        .addOnGroupChangedListener(
                                new OnGroupChangedListener() {
                                    @Override
                                    public void onItemAdded(MapItem item,
                                            MapGroup group) {
                                    }

                                    @Override
                                    public void onItemRemoved(MapItem item,
                                            MapGroup group) {
                                        CustomGrid grid = GridLinesMapComponent
                                                .getCustomGrid();
                                        if (grid != null)
                                            grid.clear();
                                    }
                                });
                MapView.getMapView().getRootGroup().addItem(_centerMarker);
            }
            _centerMarker.setPoint(getCenter());
        }

        onChanged(true);
        getPointBuffer();
    }

    /**
     * Calculate the coordinates for each grid line segment
     * Order:
     * [1 - 4] = bounding corners
     * [5 - Nx] = vertical lines: line1p1, line1p2, ..., lineNp1, lineNp2
     * [Nx + 1, Ny] = horizontal lines: same idea as above
     * @return Double buffer containing the points
     */
    synchronized DoubleBuffer getPointBuffer() {
        if (!isValid() || !isVisible())
            return null;
        if (_points == null) {
            switch(_gridJunctionStrategy) {
                case Extend:
                    buildPointsExtend();
                    break;
                case MultiGrid:
                    buildPointsMultiGrid();
                    break;
                default :
                    throw new IllegalStateException();
            }
        }
        return _points;
    }

    void buildPointsMultiGrid() {
        UTMPoint tl = UTMPoint.fromGeoPoint(_topLeft);
        UTMPoint tr = UTMPoint.fromGeoPoint(_topRight);
        UTMPoint bl = UTMPoint.fromGeoPoint(_bottomLeft);
        UTMPoint br = UTMPoint.fromGeoPoint(_bottomRight);

        // XXX - hemisphere check to select top/bottom line?
        IGeoPoint iTopLeft = (IGeoPoint) MarshalManager.marshal(_topLeft,
                GeoPoint.class,
                gov.tak.api.engine.map.coords.GeoPoint.class);
        double xDist;
        if(tl.getLngZone() == tr.getLngZone()) {
            // the graphic is contained within a single zone
            IGeoPoint iTopRight = (IGeoPoint) MarshalManager.marshal(_topRight,
                    GeoPoint.class,
                    gov.tak.api.engine.map.coords.GeoPoint.class);
            xDist = gov.tak.api.engine.map.coords.GeoCalculations
                    .distance(iTopLeft, iTopRight);
        } else { // calculate extent for multi-grid graphic generation
            IGeoPoint iTopRight = (IGeoPoint) MarshalManager.marshal(_topRight,
                    GeoPoint.class,
                    gov.tak.api.engine.map.coords.GeoPoint.class);
            final double xDist_0 = gov.tak.api.engine.map.coords.GeoCalculations
                    .distance(iTopLeft, iTopRight);

            xDist = 0;
            UTMPoint l = UTMPoint.fromGeoPoint(_topLeft);
            final int numZonesEW = UTMPoint.fromGeoPoint(_topRight).getLngZone()
                    - l.getLngZone()
                    + 1;
            ;               for(int i = 0; i < numZonesEW; i++) {
                // zone E bound
                final double zoneMaxLng = Math.min(
                        -180d + (l.getLngZone()*6),
                        _topRight.getLongitude());

                final GeoPoint g = l.toGeoPoint();
                final double leftLat = g.getLatitude();

                // XXX - bad math: LOB distance, zone convergence angle
                final double d = g.distanceTo(
                        new GeoPoint(
                                leftLat,
                                zoneMaxLng-0.0000001d // ~1cm inside zone
                        ));
                xDist += (i < (numZonesEW-1)) ? round(d, _mSpacing, FLOOR) : d;

                UTMPoint r = new UTMPoint(
                        l.getZoneDescriptor(),
                        l.getEasting() + d,
                        l.getNorthing());
                // shift left to the next EW zone
                l = UTMPoint.fromGeoPoint(
                        new GeoPoint(r.toGeoPoint().getLatitude(), zoneMaxLng));
            }
        }
        IGeoPoint iBottomLeft = (IGeoPoint) MarshalManager.marshal(
                _bottomLeft, GeoPoint.class,
                gov.tak.api.engine.map.coords.GeoPoint.class);
        double yDist = gov.tak.api.engine.map.coords.GeoCalculations
                .distance(iTopLeft, iBottomLeft);

        xDist = round(xDist, _mSpacing, ROUND);
        if (Double.compare(xDist, 0) == 0)
            xDist = _mSpacing;
        yDist = round(yDist, _mSpacing, ROUND);
        if (Double.compare(yDist, 0) == 0)
            yDist = _mSpacing;

        _xLines = Math.max(0, Math.abs((int) (xDist / _mSpacing)) - 1);
        _yLines = Math.max(0, Math.abs((int) (yDist / _mSpacing)) - 1);
        _pointCount = 5 + _xLines * 2 + _yLines * 2;
        ByteBuffer buf = com.atakmap.lang.Unsafe.allocateDirect(
                8 * 3 * _pointCount);
        buf.order(ByteOrder.nativeOrder());
        _points = buf.asDoubleBuffer();
        _pointPtr = Unsafe.getBufferPointer(_points);

        // Top-left corner
        addPoint(tl.toGeoPoint());
        addPoint(tr.toGeoPoint());
        addPoint(br.toGeoPoint());
        addPoint(bl.toGeoPoint());
        addPoint(tl.toGeoPoint());

        // MGRS label for each line + 4 for corners
        _labels = new String[_xLines + _yLines + 4];
        int l = 0;
        // Organize corners: vertical set then horizontal set
        _labels[l++] = getLabel(tl, true);
        _labels[_xLines + 1] = getLabel(tr, true);
        _labels[_xLines + 2] = getLabel(tl, false);
        _labels[_labels.length - 1] = getLabel(bl, false);

        // Vertical lines
        GeoPoint lastTop = _topLeft;
        GeoPoint lastBot = _bottomLeft;
        for (int x = 1; x <= _xLines; x++) {
            // Top line point
            GeoPoint top = GeoCalculations.pointAtDistance(lastTop, 90,
                    _mSpacing);
            UTMPoint tp = UTMPoint.fromGeoPoint(top);
            tp = new UTMPoint(tp.getZoneDescriptor(),
                    round(tp.getEasting(), _mSpacing, ROUND),
                    tl.getNorthing());
            top = tp.toGeoPoint();

            // Bottom line point
            GeoPoint bot = GeoCalculations.pointAtDistance(lastBot, 90,
                    _mSpacing);
            UTMPoint bp = UTMPoint.fromGeoPoint(bot);
            bp = new UTMPoint(bp.getZoneDescriptor(),
                    round(bp.getEasting(), _mSpacing, ROUND),
                    br.getNorthing());
            bot = bp.toGeoPoint();

            if (Double.compare(tp.getEasting(), bp.getEasting()) != 0 &&
                    FileSystemUtils.isEquals(tp.getZoneDescriptor(),
                            bp.getZoneDescriptor())) {
                // Eastings have diverged - check which is closer to the
                // last point and assume that's the more accurate value
                double topDist = top.distanceTo(lastTop);
                double botDist = bot.distanceTo(lastBot);
                if (Math.abs(topDist - _mSpacing) < Math
                        .abs(botDist - _mSpacing)) {
                    bp = new UTMPoint(bp.getZoneDescriptor(),
                            tp.getEasting(), bp.getNorthing());
                    bot = bp.toGeoPoint();
                } else {
                    tp = new UTMPoint(tp.getZoneDescriptor(),
                            bp.getEasting(), tp.getNorthing());
                    top = tp.toGeoPoint();
                }
            }

            _labels[l++] = getLabel(tp, true);
            addPoint(top);
            addPoint(bot);
            lastTop = top;
            lastBot = bot;
        }
        // Horizontal lines
        l += 2;
        lastTop = _topLeft;
        lastBot = _topRight;
        double nGCF = gcf(10000000, _mSpacing);
        for (int y = 1; y <= _yLines; y++) {
            // Top line point
            lastTop = GeoCalculations.pointAtDistance(lastTop, 180,
                    _mSpacing);
            UTMPoint lp = UTMPoint.fromGeoPoint(lastTop);
            lp = new UTMPoint(lp.getZoneDescriptor(),
                    tl.getEasting(),
                    round(lp.getNorthing(), nGCF, ROUND));
            lastTop = lp.toGeoPoint();
            addPoint(lastTop);
            _labels[l++] = getLabel(lp, false);
            // Right line point
            lastBot = GeoCalculations.pointAtDistance(lastBot, 180,
                    _mSpacing);
            UTMPoint rp = UTMPoint.fromGeoPoint(lastBot);
            rp = new UTMPoint(rp.getZoneDescriptor(),
                    br.getEasting(),
                    round(rp.getNorthing(), nGCF, ROUND));
            lastBot = rp.toGeoPoint();
            addPoint(lastBot);
        }

        _points.flip();
    }

    void buildPointsExtend() {
        UTMPoint tl = _topLeftUTM;
        UTMPoint tr = _topRightUTM;
        UTMPoint bl = _bottomLeftUTM;
        UTMPoint br = _bottomRightUTM;

        // XXX - hemisphere check to select top/bottom line?
        double xDist = tr.getEasting()-tl.getEasting();
        double yDist = tr.getNorthing()-br.getNorthing();

        xDist = round(xDist, _mSpacing, ROUND);
        if (Double.compare(xDist, 0) == 0)
            xDist = _mSpacing;
        yDist = round(yDist, _mSpacing, ROUND);
        if (Double.compare(yDist, 0) == 0)
            yDist = _mSpacing;

        _xLines = Math.max(0, Math.abs((int) (xDist / _mSpacing)) - 1);
        _yLines = Math.max(0, Math.abs((int) (yDist / _mSpacing)) - 1);
        _pointCount = 5 + _xLines * 2 + _yLines * 2;
        ByteBuffer buf = com.atakmap.lang.Unsafe.allocateDirect(
                8 * 3 * _pointCount);
        buf.order(ByteOrder.nativeOrder());
        _points = buf.asDoubleBuffer();
        _pointPtr = Unsafe.getBufferPointer(_points);

        // Top-left corner
        addPoint(tl.toGeoPoint());
        addPoint(tr.toGeoPoint());
        addPoint(br.toGeoPoint());
        addPoint(bl.toGeoPoint());
        addPoint(tl.toGeoPoint());

        // MGRS label for each line + 4 for corners
        _labels = new String[_xLines + _yLines + 4];
        int l = 0;
        // Organize corners: vertical set then horizontal set
        _labels[l++] = getLabel(tl, true);
        _labels[_xLines + 1] = getLabel(tr, true);
        _labels[_xLines + 2] = getLabel(tl, false);
        _labels[_labels.length - 1] = getLabel(bl, false);

        // Vertical lines
        UTMPoint lastTop = _topLeftUTM;
        UTMPoint lastBot = _bottomLeftUTM;
        for (int x = 1; x <= _xLines; x++) {
            // Top line point
            UTMPoint top = new UTMPoint(lastTop.getZoneDescriptor(),
                    lastTop.getEasting() + _mSpacing,
                    lastTop.getNorthing());

            // Bottom line point
            UTMPoint bot = new UTMPoint(lastBot.getZoneDescriptor(),
                    lastBot.getEasting() + _mSpacing,
                    lastBot.getNorthing());

            _labels[l++] = getLabel(top, true);
            addPoint(top.toGeoPoint());
            addPoint(bot.toGeoPoint());
            lastTop = top;
            lastBot = bot;
        }
        // Horizontal lines
        l += 2;
        lastTop = _topLeftUTM;
        lastBot = _topRightUTM;
        double nGCF = gcf(10000000, _mSpacing);
        for (int y = 1; y <= _yLines; y++) {
            // Left line point
            lastTop = new UTMPoint(lastTop.getZoneDescriptor(),
                    lastTop.getEasting(),
                    lastTop.getNorthing() - _mSpacing);
            addPoint(lastTop.toGeoPoint());
            _labels[l++] = getLabel(lastTop, false);
            // Right line point
            lastBot = new UTMPoint(lastBot.getZoneDescriptor(),
                    lastBot.getEasting(),
                    lastBot.getNorthing() - _mSpacing);
            addPoint(lastBot.toGeoPoint());
        }

        _points.flip();
    }

    // Should only be called within above function
    private void addPoint(GeoPoint p) {
        _points.put(p.getLongitude());
        _points.put(p.getLatitude());
        _points.put(0); // To be populated by renderer
    }

    /**
     * Given a label index, return its corresponding position index
     * @param i Label index
     * @return Position index
     */
    synchronized int getLabelPositionIndex(int i) {
        if (i == 0 || i == _xLines + 2)
            // Top-left corner
            return 0;
        else if (i == _xLines + 1)
            // Top-right corner
            return 1;
        else if (i == _labels.length - 1)
            // Bottom-left corner
            return 3;
        else if (i > 0 && i <= _xLines)
            // Top row
            return 3 + (i * 2);
        else
            // Left column
            return 3 + (_xLines * 2) + (i - (_xLines + 2))
                    * 2;
    }

    /**
     * Test whether this grid is drawing it's labels
     * This is different than showLabels since it also depends on
     * the map zoom level
     * @param lat Draw latitude
     * @param mapRes Map resolution
     * @return True if drawing labels
     */
    public synchronized boolean isDrawingLabels(double lat, double mapRes) {
        if (!_showLabels || !isValid())
            return false;

        int precision = getLabelPrecision();
        double mercatorscale = Math
                .cos(lat / ConversionFactors.DEGREES_TO_RADIANS);
        if (mercatorscale < 0.0001)
            mercatorscale = 0.0001;
        double metersPerPixel = mapRes * mercatorscale;
        return ((metersPerPixel * precision) / _mSpacing) <= 0.05;
    }

    /**
     * Get the appropriate label value precision based on spacing
     * @return Value precision (i.e. 100m spacing = first 3 digits)
     */
    public synchronized int getLabelPrecision() {
        return _precision == 0 ? MathUtils.clamp(5 - (int) Math.floor(
                Math.log10(_mSpacing)), 1, 5) : _precision;
    }

    /**
     * Set the label precision manually
     * @param precision Precision from 0 to 5 (0 means use automatic)
     */
    public synchronized void setLabelPrecision(int precision) {
        if (_precision != precision) {
            _precision = MathUtils.clamp(precision, 0, 5);
            onChanged(false);
        }
    }

    private synchronized GeoPoint getPoint(int pointIdx) {
        return new GeoPoint(
                Unsafe.getDouble(_pointPtr + pointIdx * 24 + 8),
                Unsafe.getDouble(_pointPtr + pointIdx * 24));
    }

    /**
     * Get the list of grid labels (horizontal first then vertical)
     * @param trunc Truncate according to the default precision
     * @return List of labels
     */
    public synchronized String[] getLabels(boolean trunc) {
        if (_labels == null)
            return new String[0];
        String[] ret = new String[_labels.length];
        int precision = getLabelPrecision();
        double exp = Math.pow(10, 5 - precision);
        for (int i = 0; i < _labels.length; i++) {
            if (_labels[i] == null) {
                _labels[i] = "";
                ret[i] = "";
                continue;
            }
            if (trunc) {
                try {
                    int meters = Integer.parseInt(_labels[i]);
                    meters = (int) round(meters, exp, ROUND);
                    if (meters >= 100000)
                        meters -= 100000;
                    ret[i] = LABEL_FMT.format(meters).substring(0, precision);
                } catch (Exception e) {
                    ret[i] = _labels[i];
                }
            } else
                ret[i] = _labels[i];
        }
        return ret;
    }

    @Override
    public synchronized Bundle preDrawCanvas(CapturePP cap) {
        if (!isValid() || !isVisible())
            return null;
        Bundle data = new Bundle();

        // Grid lines
        PointF[] points = new PointF[5];

        // Bounding box
        int ind = 0, i = 0;
        for (; i < 5; i++)
            points[i] = cap.forward(getPoint(i));
        data.putSerializable("gridLine" + (ind++), points);

        // Vertical/horizontal lines
        int p = 0;
        for (i = 5; i < _pointCount; i++) {
            if (p == 0)
                points = new PointF[2];
            points[p++] = cap.forward(getPoint(i));
            if (p == 2) {
                data.putSerializable("gridLine" + (ind++), points);
                p = 0;
            }
        }
        data.putInt("gridLineCount", ind);

        if (isDrawingLabels(cap.getBounds().getSouth(),
                cap.getMapResolution())) {
            // Label points
            PointF[] lp = new PointF[_labels.length];
            for (i = 0; i < _labels.length; i++) {
                lp[i] = cap.forward(getPoint(getLabelPositionIndex(i)));
                boolean top = i < getVerticalLineCount() + 2;
                if (!top)
                    lp[i].y -= 2;
            }
            data.putSerializable("labelPoints", lp);
        }
        return data;
    }

    @Override
    public synchronized void drawCanvas(CapturePP cap, Bundle data) {
        drawFittedGridImpl(cap);
    }

    /**
     * Draw fitted grid to canvas
     * @param cap Capture post-processor
     */
    public synchronized void drawFittedGrid(ImageryCapturePP cap) {
        drawFittedGridImpl(cap);
    }
    private boolean drawFittedGridImpl(CapturePP cap) {
        if (!isValid() || !isVisible())
            return false;
        Canvas can = cap.getCanvas();
        Paint paint = cap.getPaint();
        Path path = cap.getPath();
        float lineWeight = cap.getLineWeight();

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(cap.getThemeColor(getColor()));
        paint.setStrokeWidth(getStrokeWeight() * lineWeight);

        if(_gridJunctionStrategy == GridJunctionStrategy.Extend ||
                _topLeftUTM.getLngZone() == _topRightUTM.getLngZone()) {

            // Draw grid lines
            double spacing = getSpacing();
            final double horizontalRange =
                    (cap instanceof MapItemCapturePP) ?
                            ((MapItemCapturePP) cap).getHorizontalRange() :
                            getNumColumns() * getSpacing();
            final double verticalRange =
                    (cap instanceof MapItemCapturePP) ?
                            ((MapItemCapturePP) cap).getVerticalRange() :
                            getNumRows() * getSpacing();
            double rangeX = horizontalRange / spacing;
            double rangeY = verticalRange / spacing;
            int colCount = (int) Math.ceil(rangeX);
            int rowCount = (int) Math.ceil(rangeY);
            float celSize = (float) (cap.getWidth() / rangeX);
            for (int i = 0; i <= colCount; i++) {
                float x = Math.min(i * celSize, cap.getWidth());
                path.moveTo(x, 0);
                path.lineTo(x, cap.getHeight());
            }
            for (int i = 0; i <= rowCount; i++) {
                float y = Math.min(i * celSize, cap.getHeight());
                path.moveTo(0, y);
                path.lineTo(cap.getWidth(), y);
            }
        } else { // multi-grid
            GeoPoint rowEastBound = _topRight;
            GeoPoint gridLowerRight = _bottomRight;

            final int numZonesEW = _topRightUTM.getLngZone()-_topLeftUTM.getLngZone()+1;

            MutableUTMPoint rowWesting = new MutableUTMPoint(_topLeftUTM);

            GeoPoint[] junctionLines = new GeoPoint[numZonesEW*2];

            final double spacing = _mSpacing;
            for(int j = -1; j <= _yLines; j++) {
                UTMPoint left = new UTMPoint(rowWesting);
                for(int i = 0; i < numZonesEW; i++) {
                    // zone E bound
                    final double zoneMaxLng = Math.min(
                            -180d + ((left.getLngZone()+i)*6),
                            rowEastBound.getLongitude());

                    GeoPoint lla = left.toGeoPoint();

                    // record the junction line top+bottom
                    if(i > 0 && (j == -1 || j == _yLines))
                        junctionLines[(i*2) + ((j == -1) ? 0 : 1)] = lla;

                    // XXX - bad math: LOB distance, zone convergence angle
                    final double d = lla.distanceTo(
                            new GeoPoint(
                                    lla.getLatitude(),
                                    zoneMaxLng -0.0000001d // ~1cm inside zone
                            ));
                    UTMPoint right = new UTMPoint(
                            left.getZoneDescriptor(),
                            left.getEasting() + d,
                            left.getNorthing());

                    // insert the line
                    PointF xy;

                    // NOTE: `view.scratch.geo` is seeded with `left`
                    xy = cap.forward(left.toGeoPoint());
                    path.moveTo(xy.x, xy.y);

                    xy = cap.forward(right.toGeoPoint());
                    path.lineTo(xy.x, xy.y);

                    // shift left to the next EW zone
                    left = UTMPoint.fromGeoPoint(
                            new GeoPoint(right.toGeoPoint().getLatitude(), zoneMaxLng));
                }

                // shift south
                rowWesting.offset(0d, -spacing);

                rowEastBound = GeoCalculations.pointAtDistance(rowEastBound, rowEastBound.bearingTo(gridLowerRight), spacing);
            }

            for(int j = 0; j < _xLines; j++) {
                final int pidx = (5*3) + ((j*2)*3);
                PointF top = cap.forward(new GeoPoint(_points.get(pidx+1), _points.get(pidx)));
                path.moveTo(top.x, top.y);
                PointF bottom = cap.forward(new GeoPoint(_points.get(pidx+3+1), _points.get(pidx+3)));
                path.lineTo(bottom.x, bottom.y);
            }

            // left edge
            {
                PointF top = cap.forward(new GeoPoint(_points.get(1), _points.get(0)));
                path.moveTo(top.x, top.y);
                PointF bottom = cap.forward(new GeoPoint(_points.get(10), _points.get(9)));
                path.lineTo(bottom.x, bottom.y);
            }
            // right edge
            {
                PointF top = cap.forward(new GeoPoint(_points.get(4), _points.get(3)));
                path.moveTo(top.x, top.y);
                PointF bottom = cap.forward(new GeoPoint(_points.get(7), _points.get(6)));
                path.lineTo(bottom.x, bottom.y);
            }
            // flush before changing style
            can.drawPath(path, paint);
            path.reset();

            // draw any zone junctions
            for(int i = 0; i < junctionLines.length/2; i++) {
                if(junctionLines[i*2] == null || junctionLines[i*2+1] == null)
                    continue;
                PointF top = cap.forward(junctionLines[i*2]);
                path.moveTo(top.x, top.y);
                PointF bottom = cap.forward(junctionLines[i*2+1]);
                path.lineTo(bottom.x, bottom.y);
            }

            // zone junctions are indicated by accentuated lines, choose highlight color based on
            // luminance contrast
            final int color = cap.getThemeColor(getColor());
            float bglum = 0.2126f * Color.red(color)
                    + 0.7152f * Color.green(color)
                    + 0.0722f * Color.blue(color);
            final int zoneJunctionHighlightColor = (bglum > 127.5f) ? Color.BLACK : Color.WHITE;

            paint.setStrokeWidth(Math.max((getStrokeWeight()+2) * lineWeight, 3));

            can.drawPath(path, paint);

            paint.setColor(zoneJunctionHighlightColor);
            paint.setStrokeWidth(Math.max(getStrokeWeight() * lineWeight, 1));
        }
        can.drawPath(path, paint);
        path.reset();
        return true;
    }

    public static double round(double val, double nearest, int roundType) {
        double sign = Math.signum(val);
        if (Double.compare(sign, 0.0) == 0)
            return 0;
        val = Math.abs(val) / nearest;
        switch (roundType) {
            case FLOOR:
                val = Math.floor(val);
                break;
            case ROUND:
                val = Math.round(val);
                break;
            case CEIL:
                val = Math.ceil(val);
                break;
        }
        return sign * val * nearest;
    }

    public static double gcf(double val1, double val2) {
        return (Double.compare(val1, 0) == 0 || Double.compare(val2, 0) == 0)
                ? val1 + val2
                : gcf(val2, val1 % val2);
    }

    private static String getLabel(MGRSPoint mgrs, boolean east) {
        return east ? mgrs.getEastingDescriptor()
                : mgrs.getNorthingDescriptor();
    }

    private static String getLabel(UTMPoint utm, boolean east) {
        return getLabel(new MGRSPoint(utm), east);
    }

    /**
     * Get the easting or northing of a point
     * @param lat Latitude of point
     * @param lon Longitude of point
     * @param east True to return easting, false for northing
     * @return The easting/northing descriptor (5-digit number)
     */
    private static String getLabel(double lat, double lon, boolean east) {
        return getLabel(MGRSPoint.fromLatLng(Ellipsoid.WGS_84,
                lat, lon, null), east);
    }

    public synchronized void onChanged(boolean recalcGrid) {
        if (recalcGrid)
            _points = null;
        for (OnChangedListener ocl : _listeners)
            ocl.onChanged(this);
    }

    public synchronized void addOnChangedListener(OnChangedListener ocl) {
        if (!_listeners.contains(ocl))
            _listeners.add(ocl);
    }

    public synchronized void removeOnChangedListener(OnChangedListener ocl) {
        _listeners.remove(ocl);
    }

    public interface OnChangedListener {
        void onChanged(CustomGrid grid);
    }
}
