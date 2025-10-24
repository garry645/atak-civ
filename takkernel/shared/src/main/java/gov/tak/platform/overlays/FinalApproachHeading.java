package gov.tak.platform.overlays;


import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;

/**
 * A MapWidget class that will display an arrow representing the Final Attack Heading towards a
 * targeted IP and with an offset from some designator point. The widget will also display a cone
 * that represents the offset width from the heading on each side. The final approach heading text
 * value will be displayed above the heading arrow, as well as the text heading values of the cone's
 * sides.<br>
 * <br>
 */
public final class FinalApproachHeading {

    public final static String TAG = FinalApproachHeading.class.getSimpleName();
    private final ConcurrentLinkedQueue<OnPropertyChangedListener> _onPropertyChanged = new ConcurrentLinkedQueue<>();

    private IGeoPoint _designator;
    private IGeoPoint _target;
    private boolean _reverse = true;
    private double _angle;

    private int _arrowLength = 200;

    private int _arrowColor;

    private boolean _touchCircleVisible = true;

    private int _touchCircleColor;

    private int _wedgeColor;

    private double _wedgeWidth;

    private double _wedgeRange;

    private boolean _dangerCondition = false;

    public boolean _visible = true;

    private double _fahOffset;

    private double distance;

    private static final int MAX_DISTANCE = 1841 * 100;
    private final static int DEFAULT_DISTANCE = 5;

    /**
     * A listener for the {@link FinalApproachHeading} changing the angle of the final approach heading.
     */
    public interface OnPropertyChangedListener {
        /**
         * Callback when the arrow angle changes
         * @param arrow the arrow associated with the angle change
         */
        void onAngleChanged(FinalApproachHeading arrow);
        void onVisibleChanged(FinalApproachHeading arrow);

        /**
         * Callback when the touchability of the arrow changes
         * @param arrow the arrow associated with the touchability change
         */
        void onTouchableChanged(FinalApproachHeading arrow);

        /**
         * Callback when the target point changes
         * @param arrow the arrow associated with the target point change
         */
        void onTargetChanged(FinalApproachHeading arrow);

        /**
         * Callback when the designator point changes
         * @param arrow the arrow associated with the designator point change
         */
        void onDesignatorChanged(FinalApproachHeading arrow);

        /**
         * Callback when the cone width angle changes
         * @param arrow the arrow associated with the arrow width change
         */
        void onWedgeWidthChanged(FinalApproachHeading arrow);

        /**
         * Callback when the arrow leg changes
         * @param arrow the arrow associated with the arrow leg change
         */
        void onLegChanged(FinalApproachHeading arrow);

        /**
         * Callback when the arrow length changes
         * @param arrow the arrow associated with the arrow length change
         */
        void onArrowLengthChanged(FinalApproachHeading arrow);
    }

    public FinalApproachHeading() {
    }

    /**
     * Add a property change listener for the arrow
     * @param l the listener
     */
    public void addOnPropertyChangedListener(
            OnPropertyChangedListener l) {
        if (!_onPropertyChanged.contains(l))
            _onPropertyChanged.add(l);
    }

    /**
     * Remove a property change listener
     * @param l the listener
     */
    public void removeOnPropertyChangedListener(
            OnPropertyChangedListener l) {
        _onPropertyChanged.remove(l);
    }
    public void setDesignator(IGeoPoint designator)
    {
        this._designator = designator;
        this.onDesignatorPointChanged();
    }

    /**
     * Return the designator associated with this point
     * @return the point map item or null if no designator is set
     */
    public IGeoPoint getDesignator()
    {
        return _designator;
    }

    public void setTarget(IGeoPoint target)
    {
        this._target = target;
        this.onTargetPointChanged();
    }

    /**
     * Return the target associated with this point
     * @return the point map item or null if no target is set
     */
    public IGeoPoint getTarget() {
        return _target;
    }

    public void setReverse(boolean reverse)
    {
        this._reverse = reverse;
        this.onTargetPointChanged();
    }

    public boolean getReverse()
    {
        return _reverse;
    }

    public void setAngle(double angle)
    {
        this._angle = angle;
    }

    public double getAngle()
    {
        return _angle;
    }

    /**
     * Sets the length of the arrow, in pixels.
     * 
     * @param length The length of the arrow, in pixels
     */
    public void setArrowLength(int length)
    {
        if(length < 0) throw new IllegalArgumentException();
        if(length != _arrowLength) {
            this._arrowLength = length;
            onFahLengthChanged();
        }
    }

    public int getArrowLength()
    {
        return _arrowLength;
    }

    public void setArrowColor(int color)
    {
        this._arrowColor = color;
    }

    public int getArrowColor()
    {
        return _arrowColor;
    }

    public void setTouchCircleVisible(boolean visible)
    {
        this._touchCircleVisible = visible;
        this.onTouchableChanged();
    }

    public boolean getTouchCircleVisible()
    {
        return _touchCircleVisible;
    }

    public void setTouchCircleColor(int color)
    {
        this._touchCircleColor = color;
    }

    public int getTouchCircleColor()
    {
        return _touchCircleColor;
    }

    public void setWedgeColor(int color)
    {
        this._wedgeColor = color;
    }

    public int getWedgeColor()
    {
        return _wedgeColor;
    }

    public void setWedgeWidth(double width)
    {
        if(_wedgeWidth != width) {
            this._wedgeWidth = width;
            onFahWidthChanged();
        }
    }

    public double getWedgeWidth()
    {
        return _wedgeWidth;
    }

    public void setWedgeRange(double range)
    {
        this._wedgeRange = range;
    }

    public double getWedgeRange()
    {
        return _wedgeRange;
    }

    public void setDangerCondition(boolean dangerCondition)
    {
        if (_dangerCondition != dangerCondition) {
            this._dangerCondition = dangerCondition;
            onDesignatorPointChanged();
        }
    }

    public boolean getDangerCondition()
    {
        return _dangerCondition;
    }

    public void setVisible(boolean visible)
    {
        this._visible = visible;
        this.onVisibleChanged();
    }

    public boolean getVisible()
    {
        return _visible;
    }

    public double getDistance()
    {
        if (_target == null) {
            return Double.NaN;
        } else {
            return GeoCalculations.distance(_target, getValidDesignator());
        }
    }

    private void onFahAngleChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onAngleChanged(this);
        }
    }

    private void onFahLengthChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onArrowLengthChanged(this);
        }
    }
    private void onFahWidthChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onWedgeWidthChanged(this);
        }
    }

    private void onFahLegChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onLegChanged(this);
        }
    }

    private void onVisibleChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onVisibleChanged(this);
        }
    }
    private void onTouchableChanged() {
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onTouchableChanged(this);
        }
    }

    private void onTargetPointChanged() {
        _updateAngle();
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onTargetChanged(this);
        }
    }

    private void onDesignatorPointChanged() {
        _updateAngle();
        for (OnPropertyChangedListener l : _onPropertyChanged) {
            l.onDesignatorChanged(this);
        }
    }

    private void _updateAngle() {
        if (_target == null) {
            return;
        }

        // Get the distance and azimuth to from the designator to the target
        // use TRUE for the angle computation.
        int bearing = (int) Math.round(GeoCalculations.convertFromTrueToMagnetic(
                _target,
                GeoCalculations.bearing(getValidDesignator(), _target)));

        // Now update the angle based on the azimuth and offset
        final int angle = (int) Math.round((bearing + _fahOffset) / 5f) * 5;

        // Call the update to the GL class
        if(angle != _angle) {
            _angle = angle;
            this.onFahAngleChanged();
        }
    }

    public void rebuild() { this.onTargetPointChanged(); }

    public void setFahOffset(final double offset) {
        if (_fahOffset != offset) {
            _fahOffset = offset;
            _updateAngle();
        }
    }

    @DeprecatedApi(since = "4.8.1", removeAt = "5.1", forRemoval = true)
    public double getFahOffset() {
        return _fahOffset;
    }

    public void setFahValues(final double offset, final double width) {

        if (_target == null) {
            return;
        }

        int bearing = (int) Math
                .round(GeoCalculations.bearing(getValidDesignator(), _target));

        // Now update the angle based on the azimuth and offset
        //Round the fahAngle to nearest 5 like everything else
        _angle = (int) Math.round((bearing + _fahOffset) / 5f) * 5;
        _wedgeWidth = width;

        //Log.d(TAG, "offset: " + offset + " width: " + width + " computed FAH angle: " + _fahAngle);

        this.onFahLegChanged();

    }

    private IGeoPoint getValidDesignator()
    {
        if (_designator != null) {
            return _designator;
        } else {
            if (_target != null) {
                return new GeoPoint(_target.getLatitude() - .01d, _target.getLongitude());
            } else {
                return new GeoPoint(0, 0);
            }
        }
    }

    public double getTrueOffset() {
        if (_target == null) {
            return 0;
        }
        // Get the distance and azimuth to from the designator to the target
        int bearing = (int) Math.round(GeoCalculations.bearing(getValidDesignator(), _target));
        double diff = GeoCalculations.convertFromTrueToMagnetic(_target, bearing) - _angle;
        if (diff < 0) {
            diff = 360 + diff;
        }
        diff = diff % 360;
        if (diff == 0 || diff == 1) {
            diff = 360;
        }
        return Math.round(diff);
    }
}
