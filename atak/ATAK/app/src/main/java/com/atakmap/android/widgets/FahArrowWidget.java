
package com.atakmap.android.widgets;

import android.content.SharedPreferences;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.maps.graphics.widgets.GLWidgetsMapComponent;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ATAKUtilities;
import gov.tak.platform.lang.Parsers;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * A MapWidget class that will display an arrow representing the Final Attack Heading towards a
 * targeted IP and with an offset from some designator point. The widget will also display a cone
 * that represents the offset width from the heading on each side. The final attack heading text
 * value will be displayed above the heading arrow, as well as the text heading values of the cone's
 * sides.<br>
 * <br>
 */
public class FahArrowWidget extends ShapeWidget implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String TAG = "FahArrowWidget";
    private final ConcurrentLinkedQueue<OnFahAngleChangedListener> _onFahAngleChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnFahWidthChangedListener> _onFahWidthChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnFahLegChangedListener> _onFahLegChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnTouchableChangedListener> _onTouchableChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnTargetPointChangedListener> _onTargetPointChanged = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnDesignatorPointChangedListener> _onDesignatorPointChanged = new ConcurrentLinkedQueue<>();
    private double _fahAngle;
    private double _fahOffset;
    private double _fahWidth;

    private PointMapItem _target;
    private PointMapItem _designator;
    private GeoPoint _lastDesignatorLoc;
    private boolean _fakeDesignator;
    private final RectF _hitBox = new RectF(-64f, -64f, 64f, 64f);
    private final MapView _mapView;
    private boolean reverse = true;
    private boolean dangerCondition = false;

    private double distance;

    private static final int MAX_DISTANCE = 1841 * 100;
    private final static int DEFAULT_DISTANCE = 5;

    /**
     * A listener for the FahArrowWidget changing the angle of the final attack heading.
     */
    public interface OnFahAngleChangedListener {
        /**
         * Callback when the arrow angle changes
         * @param arrow the arrow associated with the angle change
         */
        void onFahAngleChanged(FahArrowWidget arrow);
    }

    /**
     * A listener for the FahArrowWidget changing it's touchable state.
     */
    public interface OnTouchableChangedListener {
        /**
         * Callback when the touchability of the arrow changes
         * @param arrow the arrow associated with the touchability change
         */
        void onTouchableChanged(FahArrowWidget arrow);
    }

    /**
     * A listener for the FahArrowWidget changing the targeted location.
     */
    public interface OnTargetPointChangedListener {
        /**
         * Callback when the target point changes
         * @param arrow the arrow associated with the target point change
         */
        void onTargetChanged(FahArrowWidget arrow);
    }

    /**
     * A listener for the FahArrowWidget changing the designator's point.
     */
    public interface OnDesignatorPointChangedListener {
        /**
         * Callback when the designator point changes
         * @param arrow the arrow associated with the designator point change
         */
        void onDesignatorChanged(FahArrowWidget arrow);
    }

    /**
     * A listener for the FahArrowWidget changing the width angle of the cone.
     */
    public interface OnFahWidthChangedListener {
        /**
         * Callback when the arrow width changes
         * @param arrow the arrow associated with the arrow width change
         */
        void onFahWidthChanged(FahArrowWidget arrow);
    }

    public interface OnFahLegChangedListener {
        /**
         * Callback when the arrow leg changes
         * @param arrow the arrow associated with the arrow leg change
         */
        void onFahLegChanged(FahArrowWidget arrow);
    }

    /**
     * Add an target point change listener for the arrow
     * @param l the listener
     */
    public void addOnTargetPointChangedListener(
            OnTargetPointChangedListener l) {
        if (!_onTargetPointChanged.contains(l))
            _onTargetPointChanged.add(l);
    }

    /**
     * Add an arrow angle change listener
     * @param l the listener
     */
    public void addOnFahAngleChangedListener(OnFahAngleChangedListener l) {
        if (!_onFahAngleChanged.contains(l))
            _onFahAngleChanged.add(l);
    }

    /**
     * Add an arrow width change listener
     * @param l the listener
     */
    public void addOnFahWidthChangedListener(OnFahWidthChangedListener l) {
        if (!_onFahWidthChanged.contains(l))
            _onFahWidthChanged.add(l);
    }

    /**
     * Add an arrow leg change listener
     * @param l the listener
     */
    public void addOnFahLegChangedListener(OnFahLegChangedListener l) {
        if (!_onFahLegChanged.contains(l))
            _onFahLegChanged.add(l);
    }

    /**
     * Add an arrow touchability change listener
     * @param l the listener
     */
    public void addOnTouchableChangedListener(OnTouchableChangedListener l) {
        if (!_onTouchableChanged.contains(l))
            _onTouchableChanged.add(l);
    }

    /**
     * Add designator point change listener
     * @param l the listener
     */
    public void addOnDesignatorPointChangedListener(
            OnDesignatorPointChangedListener l) {
        if (!_onDesignatorPointChanged.contains(l))
            _onDesignatorPointChanged.add(l);
    }

    /**
     * Remove an arrow target point change listener
     * @param l the listener
     */
    public void removeOnTargetPointChangedListener(
            OnTargetPointChangedListener l) {
        _onTargetPointChanged.remove(l);
    }

    /**
     * Remove an arrow angle change listener
     * @param l the listener
     */
    public void removeOnFahAngleChangedListener(OnFahAngleChangedListener l) {
        _onFahAngleChanged.remove(l);
    }

    /**
     * Remove an arrow width point change listener
     * @param l the listener
     */
    public void removeOnFahWidthChangedListener(OnFahWidthChangedListener l) {
        _onFahWidthChanged.remove(l);
    }

    /**
     * Remove an arrow leg change listener
     * @param l the listener
     */
    public void removeOnFahLegChangedListener(OnFahLegChangedListener l) {
        _onFahLegChanged.remove(l);
    }

    /**
     * Remove an arrow touchability change listener
     * @param l the listener
     */
    public void removeOnTouchableChangedListener(OnTouchableChangedListener l) {
        _onTouchableChanged.remove(l);
    }

    /**
     * Remove an arrow designator change listener
     * @param l the listener
     */
    public void removeOnDesignatorPointChangedListener(
            OnDesignatorPointChangedListener l) {
        _onDesignatorPointChanged.remove(l);
    }

    FahArrowWidget(final MapView mapView) {
        _mapView = mapView;
        setZOrder(0d);
        AtakPreferences _preferences = AtakPreferences.getInstance(_mapView
                .getContext());
        _preferences.registerListener(this);
        distance = Parsers.parseInt(
                _preferences.get("fahDistance", null), DEFAULT_DISTANCE)
                * 1852d;

    }

    /**
     * Only used with the red X fah.
     */
    public void enableStandaloneManipulation() {
        addOnPressListener(_arrowListener);
        addOnUnpressListener(_arrowListener);
        addOnMoveListener(_arrowListener);
    }

    /**
     * Sets the target item for this widget to follow.
     * 
     * @param target The PointMapItem that represents the target.
     */
    public void setTargetItem(PointMapItem target) {
        if (_target != null) {
            _target.removeOnVisibleChangedListener(_targetVisListner);
            _target.removeOnPointChangedListener(_targetListener);
            _mapView.getMapEventDispatcher().removeMapItemEventListener(
                    _target, _removeListener);
        }
        _target = target;

        _target.addOnVisibleChangedListener(_targetVisListner);
        _target.addOnPointChangedListener(_targetListener);
        _mapView.getMapEventDispatcher().addMapItemEventListener(_target,
                _removeListener);

        if (_designator == null || _fakeDesignator) {
            // slightly south of the target
            _lastDesignatorLoc = new GeoPoint(
                    _target.getPoint().getLatitude() - .01d,
                    _target.getPoint().getLongitude());
        }
        this.onTargetPointChanged();
        _updateAngle();

    }

    /**
     * Sets the designator item for this widget to follow and adjust the heading off of this
     * location and the target location.
     * 
     * @param designator The PointMapItem that represents the designator.
     */
    public void setDesignatorItem(PointMapItem designator) {
        if (_designator != null) {
            _designator.removeOnPointChangedListener(_designatorListener);
            _mapView.getMapEventDispatcher().removeMapItemEventListener(
                    _designator, _removeListener);
        }
        _designator = designator;
        _fakeDesignator = _designator == null;

        if (_designator != null) {
            _lastDesignatorLoc = _designator.getPoint();
            _designator.addOnPointChangedListener(_designatorListener);
            _mapView.getMapEventDispatcher().addMapItemEventListener(
                    _designator, _removeListener);
        }
        this.onDesignatorPointChanged();
        _updateAngle();
    }

    /**
     * Removes the listeners from the target.
     */
    public void dispose() {
        // Remove from parent first to stop rendering.
        if (getParent() != null)
            getParent().removeWidget(this);
        // Then remove listener to the target point
        if (_target != null) {
            _target.removeOnPointChangedListener(_targetListener);
            _mapView.getMapEventDispatcher().removeMapItemEventListener(
                    _target, _removeListener);
            _target = null;
        }
        if (_designator != null) {
            _designator.removeOnPointChangedListener(_designatorListener);
            _mapView.getMapEventDispatcher().removeMapItemEventListener(
                    _designator,
                    _removeListener);
            _designator = null;
        }
    }

    /**
     * Set the offset of the fah arrow in degrees from the designator
     * @param offset the offset in degrees magnetic
     */
    public void setFahOffset(final double offset) {
        if (_fahOffset != offset) {
            _fahOffset = offset;
            _updateAngle();
        }
    }

    /**
     * Sets the width of the fah arrow in degrees
     * @param fahWidth the width of the arrow
     */
    public void setFahWidth(final double fahWidth) {
        if (_fahWidth != fahWidth) {
            _fahWidth = fahWidth;
            this.onFahWidthChanged();
        }
    }

    /**
     * Get the true offset from the fah in degrees
     * @return the offset in degrees
     */
    public double getTrueOffset() {
        if (_lastDesignatorLoc == null || _target == null) {
            return 0;
        }
        // Get the distance and azimuth to from the designator to the target
        int bearing = (int) Math.round(GeoCalculations
                .bearingTo(_lastDesignatorLoc, _target.getPoint()));
        double diff = ATAKUtilities.convertFromTrueToMagnetic(
                _target.getPoint(), bearing)
                - _fahAngle;
        if (diff < 0) {
            diff = 360 + diff;
        }
        diff = diff % 360;
        if (diff == 0 || diff == 1) {
            diff = 360;
        }
        return Math.round(diff);
    }

    /**
     * Trigger a complete rebuild the FAH.
     */
    public void rebuild() {
        this.onTargetPointChanged();
    }

    /**
     * Toggles the ability to draw the reverse of the FAH.
     * on by default.
     */
    public void setDrawReverse(final boolean reverse) {
        this.reverse = reverse;
        this.onTargetPointChanged();
    }

    /**
     * Set if the final attack heading should indicate a danger condition exists
     * @param dangerCondition true if there is a danger condition, otherwise false
     */
    public void setDangerCondition(final boolean dangerCondition) {
        this.dangerCondition = dangerCondition;
        // trigger a rebuild
        onDesignatorPointChanged();
    }

    /**
     * Get if the final attack heading should indicate a danger condition
     * @return true if there is a danger condition
     */
    public boolean getDangerCondition() {
        return dangerCondition;
    }

    public void setFahValues(final double offset, final double width) {

        if (_lastDesignatorLoc == null || _target == null) {
            return;
        }

        int bearing = (int) Math
                .round(GeoCalculations.bearingTo(_lastDesignatorLoc,
                        _target.getPoint()));

        // Now update the angle based on the azimuth and offset
        //Round the fahAngle to nearest 5 like everything else
        _fahAngle = (int) Math.round((bearing + _fahOffset) / 5f) * 5;
        _fahWidth = width;

        //Log.d(TAG, "offset: " + offset + " width: " + width + " computed FAH angle: " + _fahAngle);

        this.onFahLegChanged();

    }

    /**
     * If the fah arrow widget is editable via touching the blue ball
     * @param touchable true if touchable
     */
    public void setTouchable(boolean touchable) {
        super.setTouchable(touchable);
        this.onTouchableChanged();
    }

    @Override
    public boolean testHit(float x, float y) {
        return _hitBox.contains(x, y);
    }

    @Override
    public MapWidget seekHit(float x, float y) {
        if (isTouchable() && this.testHit(x, y))
            return this;
        else
            return null;
    }

    /**
     * Return the final attack heading angle in degrees magnetic
     * @return the arrow angle in degrees magnetic
     */
    public double getFahAngle() {
        return _fahAngle;
    }

    /**
     * Return the final attack heading width
     * @return the width in degrees
     */
    public double getFahWidth() {
        return _fahWidth;
    }

    /**
     * Is the final attack heading arrow touchable
     * @return true if the final attack heading is touchable.
     */
    public boolean getTouchable() {
        return isTouchable();
    }

    /**
     * Obtain the target point or null if the target is not set
     * @return the target point or null if the target is not set
     */
    public GeoPoint getTargetPoint() {
        if (_target != null)
            return _target.getPoint();
        else
            return null;
    }

    /**
     * Return the target associated with this point
     * @return the point map item or null if no target is set
     */
    public PointMapItem getTarget() {
        return _target;
    }

    /**
     * Obtain the appropriate distance for the final attack heading arrow
     * @return the distance in meters
     * @deprecated
     * This will be replaced with a setDistance and getDistance and will remove all distance
     * calculation logic from the Fah Arrow Widget.
     */
    @DeprecatedApi(since = "4.8.1", removeAt = "5.1", forRemoval = true)
    public double getAppropriateDistance() {
        if (Double.compare(distance, 0d) == 0) {

            double computed = -1;

            // line 3 is stored in NM
            if (_target != null)
                computed = _target.getMetaDouble("nineline_line_3", computed);

            computed = SpanUtilities.convert(computed, Span.NAUTICALMILE,
                    Span.METER);

            if (!(computed > 0) && (_designator != null)) {
                // overhead or 0, use the distance to the target
                computed = GeoCalculations.distanceTo(
                        _designator.getPoint(),
                        _target.getPoint());
            } else if (!(computed > 0)) {
                // if there is no designator (unhinged) and overhead, just assume it is the default
                computed = DEFAULT_DISTANCE * 1852d;
            }

            computed = Math.abs(computed);

            if (computed > MAX_DISTANCE)
                computed = MAX_DISTANCE;
            return computed;
        } else {
            return distance;
        }

    }

    /**
     * Return true if the arrow is to draw both the forwaed and reverse cone.
     * @return true if it is to draw the reverse cone
     */
    public boolean drawReverse() {
        return reverse;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        if (key == null)
            return;

        if (key.equals("fahDistance")) {
            distance = Parsers.parseInt(
                    sharedPreferences.getString(key, null), DEFAULT_DISTANCE)
                    * 1852d;
            rebuild();
        }
    }

    /**
     * Returns the position of the designator if the designator is set
     * @return the geopoint or null if no designator is set.
     */
    public GeoPoint getDesignatorPoint() {
        if (_designator != null)
            return _designator.getPoint();
        else
            return null;
    }

    /**
     * Return the designator associated with this point
     * @return the point map item or null if no designator is set
     */
    public PointMapItem getDesignator() {
        return _designator;
    }

    private void onFahAngleChanged() {
        for (OnFahAngleChangedListener l : _onFahAngleChanged) {
            l.onFahAngleChanged(this);
        }
    }

    private void onFahWidthChanged() {
        for (OnFahWidthChangedListener l : _onFahWidthChanged) {
            l.onFahWidthChanged(this);
        }
    }

    private void onFahLegChanged() {
        for (OnFahLegChangedListener l : _onFahLegChanged) {
            l.onFahLegChanged(this);
        }
    }

    private void onTouchableChanged() {
        for (OnTouchableChangedListener l : _onTouchableChanged) {
            l.onTouchableChanged(this);
        }
    }

    private void onTargetPointChanged() {
        _updateAngle();
        for (OnTargetPointChangedListener l : _onTargetPointChanged) {
            l.onTargetChanged(this);
        }
    }

    private void onDesignatorPointChanged() {
        _updateAngle();
        for (OnDesignatorPointChangedListener l : _onDesignatorPointChanged) {
            l.onDesignatorChanged(this);
        }
    }

    private void _updateAngle() {
        if (_lastDesignatorLoc == null || _target == null) {
            return;
        }

        if (_fakeDesignator) {
            // track the target point in the south
            _lastDesignatorLoc = new GeoPoint(
                    _target.getPoint().getLatitude() - .01d,
                    _target.getPoint().getLongitude());
        }

        // Get the distance and azimuth to from the designator to the target
        // use TRUE for the angle computation.
        int bearing = (int) Math.round(ATAKUtilities.convertFromTrueToMagnetic(
                _target.getPoint(),
                GeoCalculations.bearingTo(_lastDesignatorLoc,
                        _target.getPoint())));

        // Now update the angle based on the azimuth and offset
        _fahAngle = (int) Math.round((bearing + _fahOffset) / 5f) * 5;

        // Call the update to the GL class
        this.onFahAngleChanged();
    }

    private final OnPointChangedListener _targetListener = new OnPointChangedListener() {
        @Override
        public void onPointChanged(PointMapItem item) {
            // It's fine if the target point changes often, since it would have to be moved by a
            // user
            onTargetPointChanged();
        }
    };

    private final OnPointChangedListener _designatorListener = new OnPointChangedListener() {
        @Override
        public void onPointChanged(PointMapItem item) {
            // Since the designator can be the user, we should prevent the FAH Widget from moving
            // a lot when the user's GPS jitters

            if (_lastDesignatorLoc == null
                    ||
                    Math.abs(_lastDesignatorLoc
                            .distanceTo(item.getPoint())) > 10) {

                _lastDesignatorLoc = item.getPoint();
                onDesignatorPointChanged();
            }
        }
    };

    private final MapEventDispatcher.OnMapEventListener _removeListener = new MapEventDispatcher.OnMapEventListener() {
        @Override
        public void onMapItemMapEvent(MapItem item, MapEvent event) {
            if (_target == item
                    && event.getType().equals(MapEvent.ITEM_REMOVED)) {
                setVisible(false);
            }
        }
    };

    private final MapWidgetTouchListener _arrowListener = new MapWidgetTouchListener();

    // ******************************** Listeners ************************************//
    private class MapWidgetTouchListener implements MapWidget.OnMoveListener,
            MapWidget.OnPressListener, MapWidget.OnUnpressListener {

        boolean isMovingFah = false;
        float lastMoveX = Float.NaN;
        float lastMoveY = Float.NaN;

        private void _updateFahAngle(float x, float y) {

            if (_target == null)
                return;

            GeoPoint fahPt = _mapView
                    .inverse(x, y, MapView.InverseMode.RayCast).get();
            double bear = GeoCalculations.bearingTo(fahPt,
                    _target.getPoint());
            if (bear < 0)
                bear = bear + 360;
            double orig = GeoCalculations.bearingTo(
                    _lastDesignatorLoc,
                    _target.getPoint());

            if (orig < bear)
                orig += 360;

            double offset = orig - bear;

            int offsetInt = (int) Math.round(offset);
            offsetInt = (offsetInt / 5) * 5;

            if (offsetInt <= 180)
                offsetInt = -offsetInt;
            else if (offsetInt > 540)
                offsetInt = 720 - offsetInt;
            else
                offsetInt = 360 - offsetInt;

            setFahOffset(offsetInt);
        }

        @Override
        public void onMapWidgetUnpress(MapWidget widget, MotionEvent event) {
            if (isMovingFah) {
                _updateFahAngle(event.getX(), event.getY());
                isMovingFah = false;
            }

            // wait for the unpress to completely occur, then pop the listeners
            Thread t = new Thread("unpress") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                    _mapView.getMapEventDispatcher().popListeners();
                }
            };
            t.start();
        }

        @Override
        public void onMapWidgetPress(MapWidget widget, MotionEvent event) {
            lastMoveX = event.getX();
            lastMoveY = event.getY();
            isMovingFah = true;

            // do not pass through any map clicks to any other listeners during the moving
            _mapView.getMapEventDispatcher().pushListeners();

            _mapView.getMapEventDispatcher().clearListeners(
                    MapEvent.MAP_CLICK);

            _mapView.getMapEventDispatcher().clearListeners(
                    MapEvent.ITEM_CLICK);
        }

        @Override
        public boolean onMapWidgetMove(MapWidget widget, MotionEvent event) {
            if (isMovingFah) {
                float x = event.getX();
                float y = event.getY();
                float d = 0f;

                if (!Float.isNaN(lastMoveX) && !Float.isNaN(lastMoveY)) {
                    // Don't update on every move event, too much computation
                    d = (float) Math.sqrt((lastMoveX - x) * (lastMoveX - x)
                            + (lastMoveY - y)
                                    * (lastMoveY - y));
                } else {
                    lastMoveX = x;
                    lastMoveY = y;
                }

                if (d > 25) {
                    lastMoveX = x;
                    lastMoveY = y;
                    _updateFahAngle(x, y);
                }
                return true;
            }
            return false;
        }
    }

    public static class Item extends Shape {

        private final MapView mapView;
        private final FahArrowWidget arrow;
        private final GLWidgetsMapComponent.WidgetTouchHandler touchListener;

        public Item(MapView view) {
            super(UUID.randomUUID().toString());

            this.mapView = view;
            this.arrow = new FahArrowWidget(view);
            this.setZOrder(Double.NEGATIVE_INFINITY);
            LayoutWidget w = new LayoutWidget();
            w.addWidget(this.arrow);
            this.touchListener = new GLWidgetsMapComponent.WidgetTouchHandler(
                    view, w);
            setClickable(true);
            setMetaBoolean("remoteDelete", false);
        }

        @Override
        public void setClickable(boolean t) {

            // potentially ben previously added by addition into a group
            mapView.removeOnTouchListener(this.touchListener);

            if (t)
                mapView.addOnTouchListenerAt(1, this.touchListener);
            else
                mapView.removeOnTouchListener(this.touchListener);

            arrow.setTouchable(t);
        }

        /**
         * Return the Final Attack Heading associated with this map item.
         * @return the final attack heading arrow
         */
        public FahArrowWidget getFAH() {
            return this.arrow;
        }

        @Override
        public GeoPoint[] getPoints() {
            return new GeoPoint[0];
        }

        @Override
        public GeoPointMetaData[] getMetaDataPoints() {
            return new GeoPointMetaData[0];
        }

        @Override
        public GeoBounds getBounds(MutableGeoBounds bounds) {
            if (bounds == null)
                bounds = new MutableGeoBounds(-90, -180, 90, 180);
            else
                bounds.set(-90, -180, 90, 180);
            return bounds;
        }

        @Override
        protected void onGroupChanged(boolean added, MapGroup mapGroup) {
            super.onGroupChanged(added, mapGroup);

            // make sure that the touch listener does not get added too many 
            // times.
            mapView.removeOnTouchListener(this.touchListener);

            if (added) {
                // only add the touch listener if it is touchable
                if (arrow.getTouchable()) {
                    mapView.addOnTouchListenerAt(1, this.touchListener);
                }
            } else
                mapView.removeOnTouchListener(this.touchListener);
        }
    }

    private final MapItem.OnVisibleChangedListener _targetVisListner = new MapItem.OnVisibleChangedListener() {
        @Override
        public void onVisibleChanged(MapItem item) {
            MapItem tgt = _target;
            if (tgt == null)
                return;

            if (item == tgt) {
                setVisible(item.getVisible());
            } else {
                item.removeOnVisibleChangedListener(this);
            }
        }
    };
}
