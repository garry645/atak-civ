
package com.atakmap.android.geofence.data;

import androidx.annotation.NonNull;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.Association;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.user.FilterMapOverlay;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.Circle;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.util.Disposable;

/**
 * Wraps a shape to create a Geo Fence
 */
public class GeoFence implements PointMapItem.OnPointChangedListener,
        MapItem.OnHeightChangedListener, Disposable {
    private static final String TAG = "GeoFence";

    public static final int DEFAULT_ENTRY_RADIUS_KM = 160;
    public static final int MAX_ENTRY_RADIUS_KM = 5000;

    // Minimum and maximum valid elevations +-1Mm
    public static final int MIN_ELEVATION = -1000000;
    public static final int MAX_ELEVATION = 1000000;

    /**
     * Enumerate trigger types
     */
    public enum Trigger {
        Entry,
        Exit,
        Both
    }

    /**
     * Enumerate supported categories for monitored map items
     * Only CoT atoms (a-*) are currently supported
     */
    public enum MonitoredTypes {
        TAKUsers(new MapGroup.MapItemsCallback.Or(
                new FilterMapOverlay.TypeFilter("self"),
                new MapGroup.MapItemsCallback.And(
                        new FilterMapOverlay.TypeFilter("a-f"),
                        new FilterMapOverlay.MetaStringExistsFilter("team")))),
        Friendly(new FilterMapOverlay.TypeFilter(new String[] {
                "a-f", "self"
        })),
        Hostile(new FilterMapOverlay.TypeFilter("a-h")),
        Custom(new FilterMapOverlay.TypeFilter(new String[] {
                "self", "a-f", "a-h", "a-n", "a-u"
        })),
        All(new FilterMapOverlay.TypeFilter(new String[] {
                "self", "a-f", "a-h", "a-n", "a-u"
        }));

        private final MapGroup.MapItemsCallback filter;
        private final ConcurrentHashMap<PointMapItem, Boolean> items;

        MonitoredTypes(MapGroup.MapItemsCallback filter) {
            this.filter = filter;
            this.items = new ConcurrentHashMap<>();
        }

        public MapGroup.MapItemsCallback getFilter() {
            return filter;
        }

        /**
         * Get list of items for this Monitored type
         * @return an immutable copy list (may not be modified, but will not throw
         *      ConcurrentModificationException for iteration
         */
        public List<PointMapItem> getItems() {
            return Collections.unmodifiableList(new ArrayList<>(
                    items.keySet()));
        }

        public boolean add(PointMapItem item) {
            //TODO no .equals so need to clean this up...
            return items.put(item, Boolean.TRUE) == null;

        }

        public boolean remove(PointMapItem item) {
            //TODO no .equals so need to clean this up...
            return items.remove(item) != null;

        }

        public int size() {
            return items.size();
        }
    }

    private final MapItem _mapItem;

    /**
     * True if actively tracking
     */
    private boolean _tracking;

    private Trigger _trigger;

    private MonitoredTypes _monitoredTypes;

    /**
     * Number of KM to monitor (radius from shape center point)
     * for TriggerTypes.Entry. Used at initiation of monitoring to determine which map items to track
     */
    private int _rangeKM;

    // Min and max elevations in meters HAE
    // NaN means no min/max
    private double _minElevation = Double.NaN, _maxElevation = Double.NaN;

    /**
     * Get the item height + base elevation
     * @param item Map item
     * @return Height + elevation in meters HAE
     */
    private double getHeightElevation(final MapItem item) {
        if (item == null || !item.hasMetaValue("height"))
            return Double.NaN;
        double height = item.getHeight();
        if (Double.isNaN(height) || Double.compare(height, 0) == 0)
            return Double.NaN;

        return getBaseElevation(item) + height;
    }

    private double getBaseElevation(final MapItem item) {
        if (item == null || !item.hasMetaValue("height"))
            return Double.NaN;
        double height = item.getHeight();
        if (Double.isNaN(height) || Double.compare(height, 0) == 0)
            return Double.NaN;

        GeoPointMetaData center;
        if (item instanceof PointMapItem)
            center = ((PointMapItem) item).getGeoPointMetaData();
        else if (item instanceof AnchoredMapItem && ((AnchoredMapItem) item)
                .getAnchorItem() != null)
            center = ((AnchoredMapItem) item).getAnchorItem()
                    .getGeoPointMetaData();
        else if (item instanceof Shape)
            center = ((Shape) item).getCenter();
        else
            return Double.NaN;

        if (center.get().isAltitudeValid())
            return EGM96.getHAE(center.get());

        return Double.NaN;
    }

    /**
     * ctor
     * creates fence based on height of the provided shape
     *
     * @param mapItem the map item to fence on
     * @param tracking if it is currently being considered
     * @param trigger the trigger for the geofence
     * @param monitoredTypes the monitor type
     * @param rangeKM (kilometers)
     */
    public GeoFence(final MapItem mapItem, final boolean tracking,
            final Trigger trigger,
            final MonitoredTypes monitoredTypes, final int rangeKM) {
        this._mapItem = mapItem;
        double maxHeight = getHeightElevation(mapItem);
        double minHeight = getBaseElevation(mapItem);
        setValues(tracking, trigger, monitoredTypes, rangeKM, minHeight,
                maxHeight);

        if (mapItem instanceof PointMapItem) {
            ((PointMapItem) mapItem).addOnPointChangedListener(this);
            mapItem.addOnHeightChangedListener(this);
        } else if (mapItem instanceof AnchoredMapItem) {
            ((AnchoredMapItem) mapItem).getAnchorItem()
                    .addOnPointChangedListener(this);
            mapItem.addOnHeightChangedListener(this);
        }
    }

    public GeoFence(final MapItem mapItem, final int rangeKM) {
        this(mapItem, true, Trigger.Entry, MonitoredTypes.All, rangeKM);
    }

    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public GeoFence(MapItem mapItem, boolean tracking, Trigger trigger,
            MonitoredTypes monitoredTypes, int rangeKM,
            double minElevation,
            double maxElevation) {
        _mapItem = mapItem;
        setValues(tracking, trigger, monitoredTypes, rangeKM,
                minElevation, maxElevation);
    }

    private void setValues(boolean tracking, Trigger trigger,
            MonitoredTypes monitoredTypes, int rangeKM, double minElevation,
            double maxElevation) {
        setTracking(tracking);
        setTrigger(trigger);
        setMonitoredTypes(monitoredTypes);
        setRangeKM(rangeKM);
        setElevationRange(minElevation, maxElevation);
    }

    public MonitoredTypes getMonitoredTypes() {
        return _monitoredTypes;
    }

    public void setMonitoredTypes(MonitoredTypes _monitoredTypes) {
        this._monitoredTypes = _monitoredTypes;
        if (_mapItem != null) {
            _mapItem.setMetaString(GeoFenceConstants.MARKER_MONITOR,
                    _monitoredTypes.toString());
        }
    }

    public String getMapItemUid() {
        return _mapItem.getUID();
    }

    public boolean isTracking() {
        return _tracking;
    }

    public void setTracking(boolean tracking) {
        this._tracking = tracking;
        if (_mapItem != null) {
            _mapItem.setMetaBoolean(GeoFenceConstants.MARKER_TRACKING,
                    _tracking);

            // radials check for existence of a variable, not its value
            String menuState = GeoFenceConstants.MARKER_TRACKING + "_menustate";
            MapItem shape = ATAKUtilities.findAssocShape(_mapItem);
            MapItem marker = shape instanceof AnchoredMapItem
                    ? ((AnchoredMapItem) shape).getAnchorItem()
                    : _mapItem;
            shape.toggleMetaData(menuState, _tracking);
            if (marker != null)
                marker.toggleMetaData(menuState, _tracking);

            if (shape instanceof DrawingCircle) {
                List<Circle> rings = ((DrawingCircle) shape).getRings();
                for (Circle circle : rings)
                    circle.toggleMetaData(menuState, _tracking);
            } else if (shape instanceof DrawingEllipse) {
                List<Ellipse> rings = ((DrawingEllipse) shape).getEllipses();
                for (Ellipse ellipse : rings)
                    ellipse.toggleMetaData(menuState, _tracking);
            } else if (shape instanceof DrawingRectangle) {
                Association[] associations = ((DrawingRectangle) shape)
                        .getAssociations();
                for (Association association : associations)
                    association.toggleMetaData(menuState, _tracking);
            }
        }
    }

    public boolean isElevationMonitored() {
        return !Double.isNaN(_minElevation) || !Double.isNaN(_maxElevation);
    }

    public Trigger getTrigger() {
        return _trigger;
    }

    public void setTrigger(Trigger trigger) {
        this._trigger = trigger;

        if (_mapItem != null)
            _mapItem.setMetaString(GeoFenceConstants.MARKER_TRIGGER,
                    _trigger.toString());
    }

    /**
     * Number of KM to monitor (radius from shape center point)
     * for TriggerTypes.Entry. Used at initiation of monitoring to determine which map items to track
     *
     * @return the number of kilometers
     */
    public int getRangeKM() {
        return _rangeKM;
    }

    /**
     * Set the maximum range for consideration of any of the tests (enter or exit)
     * @param rangeKM the range in kilometers
     */
    public void setRangeKM(int rangeKM) {
        if (rangeKM < 1) {
            Log.w(TAG, "Using default Entry radius for: " + rangeKM);
            rangeKM = GeoFence.DEFAULT_ENTRY_RADIUS_KM;
        } else if (rangeKM > GeoFence.MAX_ENTRY_RADIUS_KM) {
            Log.w(TAG, "Using max Entry radius for: " + rangeKM);
            rangeKM = GeoFence.MAX_ENTRY_RADIUS_KM;
        }
        this._rangeKM = rangeKM;
        if (_mapItem != null)
            _mapItem.setMetaDouble(GeoFenceConstants.MARKER_BOUNDING_SPHERE,
                    _rangeKM * 1000);
    }

    public double getMinElevation() {
        return _minElevation;
    }

    public double getMaxElevation() {
        return _maxElevation;
    }

    /**
     * The elevation range will be the base and height of the shape moving forward and
     * not modfiable by the user.
     * @param minAlt
     * @param maxAlt
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void setElevationRange(double minAlt, double maxAlt) {

        _minElevation = getBaseElevation(_mapItem);
        _maxElevation = getHeightElevation(_mapItem);

        if (_mapItem != null) {
            _mapItem.setMetaDouble(GeoFenceConstants.MARKER_ELEVATION_MIN,
                    _minElevation);
            _mapItem.setMetaDouble(GeoFenceConstants.MARKER_ELEVATION_MAX,
                    _maxElevation);
            _mapItem.setMetaBoolean(
                    GeoFenceConstants.MARKER_ELEVATION_MONITORED,
                    isElevationMonitored());
        }
    }

    public boolean isValid() {
        if (_mapItem == null)
            return false;

        if (_trigger == null)
            return false;

        return _monitoredTypes != null;
    }

    /**
     * Unsets all of the map item variables in order to correctly remove the geofence.
     */
    public static void clearGeofenceState(final MapView mapView,
            final MapItem mapItem) {

        if (mapItem == null)
            return;

        mapItem.removeMetaData(GeoFenceConstants.MARKER_TRIGGER);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_TRACKING);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_MONITOR);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_BOUNDING_SPHERE);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_ELEVATION_MONITORED);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_ELEVATION_MIN);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_ELEVATION_MAX);
        mapItem.removeMetaData(GeoFenceConstants.MARKER_MONITOR_UIDS);

        final String menuState = GeoFenceConstants.MARKER_TRACKING
                + "_menustate";
        MapItem shape = ATAKUtilities.findAssocShape(mapItem);
        MapItem marker = shape instanceof AnchoredMapItem
                ? ((AnchoredMapItem) shape).getAnchorItem()
                : mapItem;
        shape.removeMetaData(menuState);

        if (marker != null)
            marker.removeMetaData(menuState);

        if (mapItem.getGroup() != null)
            mapItem.persist(mapView.getMapEventDispatcher(), null,
                    GeoFence.class);

    }

    /** 
     * Create A GeoFence from the mapItem
     */
    public static GeoFence fromMapItem(final MapItem mapItem) {
        if (mapItem == null)
            return null;

        String triggerString = mapItem.getMetaString(
                GeoFenceConstants.MARKER_TRIGGER, Trigger.Entry.toString());
        boolean tracking = mapItem.getMetaBoolean(
                GeoFenceConstants.MARKER_TRACKING, false);
        String monitoredString = mapItem.getMetaString(
                GeoFenceConstants.MARKER_MONITOR,
                MonitoredTypes.All.toString());
        double rangeKM = mapItem.getMetaDouble(
                GeoFenceConstants.MARKER_BOUNDING_SPHERE,
                GeoFenceConstants.DEFAULT_ENTRY_RADIUS_METERS) / 1000;

        double minElevation = mapItem.getMetaDouble(
                GeoFenceConstants.MARKER_ELEVATION_MIN,
                GeoPoint.UNKNOWN);
        double maxElevation = mapItem.getMetaDouble(
                GeoFenceConstants.MARKER_ELEVATION_MAX,
                GeoPoint.UNKNOWN);

        Trigger trigger;
        try {
            trigger = Trigger.valueOf(triggerString);
        } catch (Exception e) {
            Log.w(TAG, "Invalid trigger: " + triggerString, e);
            trigger = Trigger.Entry;
        }

        MonitoredTypes monitoredTypes;
        try {
            monitoredTypes = MonitoredTypes.valueOf(monitoredString);
        } catch (Exception e) {
            Log.w(TAG, "Invalid MonitoredTypes: " + monitoredString, e);
            monitoredTypes = MonitoredTypes.All;
        }

        if (rangeKM < 1) {
            Log.w(TAG, "Using default Entry radius for: " + rangeKM);
            rangeKM = GeoFence.DEFAULT_ENTRY_RADIUS_KM;
        }
        if (rangeKM > GeoFence.MAX_ENTRY_RADIUS_KM) {
            Log.w(TAG, "Using max Entry radius for: " + rangeKM);
            rangeKM = GeoFence.MAX_ENTRY_RADIUS_KM;
        }

        GeoPointMetaData center;
        if (mapItem instanceof PointMapItem)
            center = ((PointMapItem) mapItem).getGeoPointMetaData();
        else
            center = ShapeUtils.getShapeCenter(mapItem);

        if (center == null)
            return null;

        GeoFence gf = new GeoFence(mapItem, tracking, trigger, monitoredTypes,
                (int) Math.round(rangeKM));
        if (!gf.isValid()) {
            Log.w(TAG, "Invalid GeoFence");
            return null;
        }

        return gf;

    }

    @Deprecated
    public static GeoFence fromCot(CotEvent from) {
        CotDetail link = from.getDetail().getFirstChildByName(0, "link");
        if (link == null) {
            Log.w(TAG,
                    "Unable to create GeoFence from CoT without link detail");
            return null;
        }

        CotDetail geofence = from.getDetail().getFirstChildByName(0,
                "__geofence");
        if (geofence == null) {
            Log.w(TAG,
                    "Unable to create GeoFence from CoT without geofence detail");
            return null;
        }

        String uid = link.getAttribute("uid");
        //String type = link.getAttribute("type");

        String triggerString = geofence.getAttribute("trigger");
        String monitoredString = geofence.getAttribute("monitored");
        String boundingSphere = geofence.getAttribute("boundingSphere");
        //the following are expressed in M HAE

        Trigger trigger;
        try {
            trigger = Trigger.valueOf(triggerString);
        } catch (Exception e) {
            Log.w(TAG, "Invalid trigger: " + triggerString, e);
            trigger = Trigger.Entry;
        }

        MonitoredTypes monitoredTypes;
        try {
            monitoredTypes = MonitoredTypes.valueOf(monitoredString);
        } catch (Exception e) {
            Log.w(TAG, "Invalid MonitoredTypes: " + monitoredString, e);
            monitoredTypes = MonitoredTypes.All;
        }

        int rangeKM;
        try {
            //Convert from Meters to KM
            rangeKM = Integer.parseInt(boundingSphere) / 1000;
            if (rangeKM < 1) {
                Log.w(TAG, "Using default Entry radius for: " + rangeKM);
                rangeKM = GeoFence.DEFAULT_ENTRY_RADIUS_KM;
            }
            if (rangeKM > GeoFence.MAX_ENTRY_RADIUS_KM) {
                Log.w(TAG, "Using max Entry radius for: " + rangeKM);
                rangeKM = GeoFence.MAX_ENTRY_RADIUS_KM;
            }
        } catch (Exception e) {
            Log.w(TAG, "Invalid boundingSphere: " + boundingSphere, e);
            rangeKM = GeoFence.DEFAULT_ENTRY_RADIUS_KM;
        }

        MapItem item = MapView.getMapView().getMapItem(uid);
        GeoFence gf = new GeoFence(item, false, trigger, monitoredTypes,
                rangeKM);
        if (!gf.isValid()) {
            Log.w(TAG, "Invalid CoT GeoFence");
            return null;
        }

        return gf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GeoFence geoFence = (GeoFence) o;
        return _tracking == geoFence._tracking &&
                _rangeKM == geoFence._rangeKM &&
                Double.compare(geoFence._minElevation, _minElevation) == 0 &&
                Double.compare(geoFence._maxElevation, _maxElevation) == 0 &&
                Objects.equals(_mapItem, geoFence._mapItem) &&
                _trigger == geoFence._trigger &&
                _monitoredTypes == geoFence._monitoredTypes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_mapItem, _tracking, _trigger, _monitoredTypes,
                _rangeKM, _minElevation, _maxElevation);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(LocaleUtil.getCurrent(),
                "%s, %b, %s, %s, %d, %f, %f",
                _mapItem.getUID(),
                _tracking,
                _trigger.toString(), _monitoredTypes.toString(), _rangeKM,
                _minElevation, _maxElevation);
    }

    @Override
    public void onHeightChanged(MapItem item) {
        _minElevation = getBaseElevation(item);
        _maxElevation = getHeightElevation(item);
    }

    @Override
    public void onPointChanged(PointMapItem item) {
        _minElevation = getBaseElevation(item);
        _maxElevation = getHeightElevation(item);
    }

    @Override
    public void dispose() {

    }
}
