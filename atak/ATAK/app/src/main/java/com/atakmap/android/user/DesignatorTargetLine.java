
package com.atakmap.android.user;



import android.content.Intent;
import android.content.SharedPreferences;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbars.DynamicRangeAndBearingEndpoint;
import com.atakmap.android.toolbars.RangeAndBearingMapItem;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;

import java.util.ArrayList;
import java.util.UUID;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.overlays.LaserBasket;

public class DesignatorTargetLine implements
        PointMapItem.OnPointChangedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "DesignatorTargetLine";

    private final String _rabUUID = UUID.randomUUID().toString();

    private final DynamicRangeAndBearingEndpoint drabe;

    private final Polyline[] wedges = new Polyline[] {
            new Polyline(UUID.randomUUID().toString()),
            new Polyline(UUID.randomUUID().toString()),
            new Polyline(UUID.randomUUID().toString()),
            new Polyline(UUID.randomUUID().toString()),
            new Polyline(UUID.randomUUID().toString())
    };

    private final MapGroup _mapGroup;
    private final MapView _mapView;
    private final AtakPreferences _preferences;
    private static boolean _showDegrees = true;

    private PointMapItem target;
    private PointMapItem designator;

    boolean targetLine = false;
    boolean safetyZone = false;

    // Clear the current point
    private IGeoPoint prevTargetPt = null;
    private IGeoPoint prevDesignatorPt = null;

    private final static int transparency = 50;
    private final static int DEFAULT_DISTANCE = 5;

    private double distance;

    private LaserBasket laserBasket;

    public DesignatorTargetLine(MapView mapView, MapGroup mapGroup) {
        _mapGroup = mapGroup;
        _mapView = mapView;
        _preferences = AtakPreferences.getInstance(_mapView
                .getContext());
        _preferences.registerListener(this);
        _showDegrees = _preferences.get("laserBasketDegrees", true);

        distance = getInteger(_preferences.getSharedPrefs(),
                "laserBasketDistance", DEFAULT_DISTANCE) * 1852d;
        drabe = new DynamicRangeAndBearingEndpoint(_mapView,
                GeoPointMetaData.wrap(GeoPoint.ZERO_POINT),
                _rabUUID + "pt1");
        drabe.setPostDragAction(new Runnable() {
            @Override
            public void run() {
                PointMapItem pmi = target;
                if (pmi != null) {
                    Intent localDetails = new Intent();
                    localDetails.setAction(
                            "com.atakmap.android.action.SHOW_POINT_DETAILS");
                    localDetails.putExtra("uid", pmi.getUID());
                    AtakBroadcast.getInstance().sendBroadcast(localDetails);

                }
            }
        });
    }

    private int getInteger(SharedPreferences _preferences,
            String key, int defaultVal) {
        try {
            return Integer.parseInt(_preferences
                    .getString(key, "" + defaultVal));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @Override
    public void onPointChanged(final PointMapItem item) {
        if (item != target && item != designator) {
            // should only be receiving updates for the currently active friendly
            item.removeOnPointChangedListener(this);
        }

        IGeoPoint tPoint = MarshalManager.marshal(target.getPoint(),
                GeoPoint.class, IGeoPoint.class);
        IGeoPoint dPoint = MarshalManager.marshal(designator.getPoint(),
                GeoPoint.class, IGeoPoint.class);
        if (prevDesignatorPt == null || prevTargetPt == null ||
                Math.abs(GeoCalculations.distance(prevTargetPt, tPoint)) > 0.1
                ||
                Math.abs(GeoCalculations.distance(prevDesignatorPt,
                        dPoint)) > 0.1) {

            laserBasket = new LaserBasket.Builder()
                    .setTarget(tPoint)
                    .setDesignator(dPoint)
                    .setAddTargetLine(true)
                    .setAddSafetyZones(true)
                    .setLaserBasketRange(distance)
                    .build();

            draw();
            // Update the current points
            prevDesignatorPt = dPoint;
            prevTargetPt = tPoint;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        if (key == null)
            return;

        if (key.equals("laserBasketDegrees")) {
            _showDegrees = sharedPreferences.getBoolean(key, true);
            prevDPoint = null; // force a redraw by dropping the cached previous point
            draw();
        } else if (key.equals("laserBasketDistance")) {
            distance = getInteger(sharedPreferences, key, DEFAULT_DISTANCE)
                    * 1852d;
            draw();
        }
    }

    synchronized public void set(final PointMapItem t) {
        if (t == null)
            return;
        IGeoPoint tPoint = MarshalManager.marshal(t.getPoint(), GeoPoint.class,
                IGeoPoint.class);
        IGeoPoint dPoint = GeoCalculations.pointAtDistance(tPoint, -180,
                distance);
        drabe.setPoint(MarshalManager.marshal(dPoint, IGeoPoint.class,
                GeoPoint.class));
        _mapGroup.addItem(drabe);
        set(t, drabe);
    }

    synchronized public void set(final PointMapItem t,
            final PointMapItem d) {

        //If we get passed in null return
        if (t == null || d == null)
            return;
        //If this is the first time and we don't have a stored designator or target
        if (laserBasket == null) {
            target = t;
            designator = d;
            target.addOnPointChangedListener(this);
            designator.addOnPointChangedListener(this);

            laserBasket = new LaserBasket.Builder()
                    .setTarget(MarshalManager.marshal(target.getPoint(),
                            GeoPoint.class, IGeoPoint.class))
                    .setDesignator(MarshalManager.marshal(designator.getPoint(),
                            GeoPoint.class, IGeoPoint.class))
                    .setAddTargetLine(true)
                    .setAddSafetyZones(true)
                    .setLaserBasketRange(distance)
                    .build();
            //Else see if either of the points change, only redraw if they did
        } else if (target == null || designator == null
                || !d.getUID().equals(designator.getUID())
                || !t.getUID().equals(target.getUID())) {
            reset();
            target = t;
            designator = d;
            target.addOnPointChangedListener(this);
            designator.addOnPointChangedListener(this);

            laserBasket = new LaserBasket.Builder()
                    .setTarget(MarshalManager.marshal(target.getPoint(),
                            GeoPoint.class, IGeoPoint.class))
                    .setDesignator(MarshalManager.marshal(designator.getPoint(),
                            GeoPoint.class, IGeoPoint.class))
                    .setAddTargetLine(true)
                    .setAddSafetyZones(true)
                    .setLaserBasketRange(distance)
                    .build();
        }
    }

    synchronized public void reset() {
        if (target != null) {
            target.removeOnPointChangedListener(this);
            target = null;
        }
        if (designator != null) {
            designator.removeOnPointChangedListener(this);
            designator = null;
        }
        clearSafetyZone();
        clearTargetLine();
        safetyZone = false;
        targetLine = false;

        prevDesignatorPt = null;
        prevTargetPt = null;
        prevDPoint = null;
    }

    public void showSafetyZone(final boolean b) {
        //Only go through changing the visibility if the visibility actually changed.
        if (safetyZone != b) {
            safetyZone = b;
            if (!b) {
                clearSafetyZone();
                prevBearing = Integer.MIN_VALUE;
            } else {
                drawSafetyZone();
            }
        }
    }

    public void showTargetLine(final boolean b) {
        if (targetLine != b) {
            targetLine = b;

            MapItem rb = _mapGroup.deepFindItem("uid", _rabUUID);
            if (rb != null) {
                rb.setVisible(b);
                if (b) {
                    _mapGroup.addItem(drabe);
                } else {
                    _mapGroup.removeItem(drabe);
                }
            } else {
                if (b)
                    drawTargetLine();
            }
        }
    }

    public void showAll(boolean b) {
        showSafetyZone(b);
        showTargetLine(b);
    }

    private int prevBearing = Integer.MIN_VALUE;
    private IGeoPoint prevDPoint = null;

    private void drawSafetyZone() {
        if (laserBasket == null || target == null || designator == null
                || _mapGroup == null)
            return;

        IGeoPoint tPoint = laserBasket.getTarget();
        IGeoPoint dPoint = laserBasket.getDesignator();

        // compute the angle to the designator
        double rawBearing = GeoCalculations.bearing(dPoint, tPoint);

        // use the bearing of the given designator
        int bearing = (int) Math.round(GeoCalculations
                .convertFromTrueToMagnetic(dPoint, rawBearing));

        // compute the point at the given distance
        if (distance > 0) {
            dPoint = GeoCalculations.pointAtDistance(tPoint, rawBearing - 180,
                    distance);
        }

        if (prevBearing == bearing && prevDPoint != null
                && GeoCalculations.distance(prevDPoint, dPoint) < 0.1)
            return;

        prevBearing = bearing;
        prevDPoint = dPoint;

        ArrayList<Feature> lbDetails = (ArrayList<Feature>) laserBasket
                .getBasket();
        int length = lbDetails.size();
        for (int n = 1; n < length; n++) {
            com.atakmap.map.layer.feature.AttributeSet attributeSet = lbDetails
                    .get(n).getAttributes();
            String[] labels = attributeSet.getStringArrayAttribute("label");
            AttributeSet labelMap = new AttributeSet();
            AttributeSet seg0 = new AttributeSet();
            AttributeSet seg2 = new AttributeSet();

            if (_showDegrees && labels.length > 0 && !labels[0].isEmpty()) {
                seg0.setAttribute("segment", 0);
                seg0.setAttribute("text", labels[0]);
                labelMap.setAttribute("seg0", seg0);
            }
            if (_showDegrees && labels.length > 1 && !labels[1].isEmpty()) {
                seg2.setAttribute("segment", LaserBasket.NUM_SEGMENTS + 1);
                seg2.setAttribute("text", labels[1]);
                labelMap.setAttribute("seg2", seg2);
            }

            Polygon lbPolygon = (Polygon) lbDetails.get(n).getGeometry();
            LineString polygonLineString = lbPolygon.getExteriorRing();
            GeoPoint[] geoPoints = new GeoPoint[polygonLineString
                    .getNumPoints()];
            for (int j = 0; j < polygonLineString.getNumPoints(); j++) {
                geoPoints[j] = new GeoPoint(polygonLineString.getY(j),
                        polygonLineString.getX(j));
            }
            CompositeStyle wedgeStyle = (CompositeStyle) lbDetails.get(n)
                    .getStyle();
            int renderColor = ((BasicStrokeStyle) wedgeStyle.getStyle(1))
                    .getColor();
            int wedgeNum = n - 1;
            wedges[wedgeNum].setClickable(false);
            wedges[wedgeNum].setPoints(geoPoints);
            wedges[wedgeNum].setLabels(labelMap);
            wedges[wedgeNum].setStyle(wedges[wedgeNum].getStyle()
                    | Shape.STYLE_FILLED_MASK | Polyline.STYLE_CLOSED_MASK);
            wedges[wedgeNum].setFillColor(renderColor);
            wedges[wedgeNum].setStrokeWeight(1d);
            int zOrder = 100 + (int) Math.floor(n / 2.0d);
            wedges[wedgeNum].setZOrder(zOrder);
            if (wedges[wedgeNum].getGroup() == null) {
                _mapGroup.addItem(wedges[wedgeNum]);
            }
        }

    }

    private void drawTargetLine() {
        if (target == null || designator == null || _mapGroup == null)
            return;

        if (RangeAndBearingMapItem.getRABLine(_rabUUID) == null) {
            designator.setMetaString("rabUUID", _rabUUID);
            target.setMetaString("rabUUID", _rabUUID);
            RangeAndBearingMapItem rb = RangeAndBearingMapItem
                    .createOrUpdateRABLine(_rabUUID, designator, target,
                            false);
            rb.setTitle(_mapView.getContext().getString(
                    R.string.designator_target_line));
            rb.setType("rb");
            rb.setMetaBoolean("removable", false);
            rb.setBearingUnits(Angle.DEGREE);
            rb.setNorthReference(NorthReference.MAGNETIC);
            rb.setMetaBoolean("disable_polar", true);
            rb.allowSlantRangeToggle(false);
            _mapGroup.addItem(rb);
        }
    }

    private void draw() {

        if (target == null || designator == null || _mapGroup == null)
            return;

        //Should only be drawing if safetyZone = true
        if (safetyZone) {
            drawSafetyZone();
        }

        if (targetLine)
            drawTargetLine();
    }

    public void clearSafetyZone() {
        if (_mapGroup != null && wedges[0] != null) {
            for (Polyline wedge : wedges) {
                _mapGroup.removeItem(wedge);
            }
        }
    }

    public void clearTargetLine() {
        RangeAndBearingMapItem rb = RangeAndBearingMapItem.getRABLine(_rabUUID);
        if (rb != null) {
            rb.removeFromGroup();
            rb.dispose();
        }
        if (drabe != null)
            drabe.removeFromGroup();
    }

    //Called when the dropdown is closed, clears visibility of everything.
    public void end() {
        clearSafetyZone();
        clearTargetLine();
        safetyZone = false;
        targetLine = false;

        prevDesignatorPt = null;
        prevTargetPt = null;
        prevBearing = Integer.MIN_VALUE;
    }
}
