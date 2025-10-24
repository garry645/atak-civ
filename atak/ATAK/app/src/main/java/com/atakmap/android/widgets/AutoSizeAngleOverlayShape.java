
package com.atakmap.android.widgets;

import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.DefaultMetaDataHolder;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.conversion.GeomagneticField;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoSizeAngleOverlayShape extends Shape
        implements AnchoredMapItem {

    protected NorthReference _azimuth = NorthReference.TRUE;
    private Angle _angle = Angle.DEGREE;

    private PointMapItem centerMarker;
    protected GeoPointMetaData center;
    private GeoPoint ellipseTestX;
    private GeoPoint ellipseTestY;
    protected double offset = 0;

    private boolean showEdgeToCenterDirection = false;
    private boolean showProjectionProportition = false;

    public interface OnPropertyChangedListener {
        /**
         * Fired when a property has changed with the AutoSizeAngleOverlayShape.
         */
        void onPropertyChanged();
    }

    private final List<OnPropertyChangedListener> onPropertyChangedListeners = new CopyOnWriteArrayList<>();

    public AutoSizeAngleOverlayShape(final String uid) {
        this(MapItem.createSerialId(), new DefaultMetaDataHolder(), uid);
    }

    public AutoSizeAngleOverlayShape(final long serialId,
            final MetaDataHolder2 metadata,
            final String uid) {
        super(serialId, metadata, uid);
    }

    /**
     * The center marker for the angle overlay shape.
     * @param centerMarker the center marker
     */
    public void setCenterMarker(PointMapItem centerMarker) {
        this.centerMarker = centerMarker;
    }

    @Override
    public PointMapItem getAnchorItem() {
        return this.centerMarker;
    }

    public void addOnPropertyChangedListener(
            OnPropertyChangedListener listener) {
        if (!onPropertyChangedListeners.contains(listener)) {
            onPropertyChangedListeners.add(listener);
        }
    }

    public void removeOnPropertyChangedListener(
            OnPropertyChangedListener listener) {
        onPropertyChangedListeners.remove(listener);
    }

    protected void firePropertyChangedEvent() {
        for (OnPropertyChangedListener listener : onPropertyChangedListeners) {
            listener.onPropertyChanged();
        }
    }

    /**
     * Set the north reference for the angle overlay.
     * @param northReference the north reference
     */
    public void setNorthReference(NorthReference northReference) {
        switch (northReference) {
            case TRUE:
                setTrueAzimuth();
                break;
            case MAGNETIC:
                setMagneticAzimuth();
                break;
            case GRID:
                setGridAzimuth();
                break;
        }
    }

    /**
     * Get the north reference for the angle overlay.
     * @return the north reference
     */
    public NorthReference getNorthReference() {
        return _azimuth;
    }

    private void setTrueAzimuth() {
        _azimuth = NorthReference.TRUE;
        offset = 0;
        if (showProjectionProportition)
            computeEllipseTestVerts();
        super.onPointsChanged();
        firePropertyChangedEvent();
    }

    private void setMagneticAzimuth() {
        _azimuth = NorthReference.MAGNETIC;
        if (center != null) {
            //get declination at center
            Date d = CoordinatedTime.currentDate();
            GeomagneticField gmf;
            gmf = new GeomagneticField((float) center.get().getLatitude(),
                    (float) center.get().getLongitude(), 0,
                    d.getTime());

            offset = gmf.getDeclination();
            if (showProjectionProportition)
                computeEllipseTestVerts();
            super.onPointsChanged();
            firePropertyChangedEvent();
        }
    }

    private void setGridAzimuth() {
        _azimuth = NorthReference.GRID;
        if (center != null) {
            //get Grid Convergence using center and point on the bullseye
            GeoPoint test0 = GeoCalculations.pointAtDistance(center.get(), 0,
                    100);
            GeoPoint test180 = GeoCalculations.pointAtDistance(center.get(),
                    180, 100);
            offset = ATAKUtilities.computeGridConvergence(test0, test180);
            if (showProjectionProportition)
                computeEllipseTestVerts();
            super.onPointsChanged();
            firePropertyChangedEvent();
        }
    }

    public void setProjectionProportion(final boolean projectionProportition) {
        showProjectionProportition = projectionProportition;
        if (showProjectionProportition)
            computeEllipseTestVerts();
    }

    protected void computeEllipseTestVerts() {
        if (center != null) {
            ellipseTestX = GeoCalculations.pointAtDistance(center.get(), 90,
                    100);
            ellipseTestY = GeoCalculations.pointAtDistance(center.get(), 0,
                    100);
        }
    }

    public boolean getProjectionProportition() {
        return showProjectionProportition;
    }

    public void save() {
        if (centerMarker != null)
            centerMarker.persist(MapView.getMapView().getMapEventDispatcher(),
                    null, this.getClass());
    }

    public void setEdgeToCenterDirection(boolean edgeToCenter) {
        if (showEdgeToCenterDirection != edgeToCenter) {
            showEdgeToCenterDirection = edgeToCenter;
            firePropertyChangedEvent();
        }
    }

    /**
     * If the arrow is pointing from edge to corner.
     * @return true if the arrow is pointing from edge to corner.
     */
    public boolean isShowingEdgeToCenter() {
        return showEdgeToCenterDirection;
    }

    /**
     * set the bearing units in degrees or mils
     * @param showDegrees true for degrees
     */
    public void setBearingUnits(final boolean showDegrees) {
        _angle = showDegrees ? Angle.DEGREE : Angle.MIL;
    }

    public Angle getBearingUnits() {
        return _angle;
    }

    /**
     * Sets the center point for the angle shape.
     * @param gp the center point
     */
    public void setCenter(GeoPointMetaData gp) {
        synchronized (this) {
            center = gp;

            if (_azimuth == NorthReference.MAGNETIC) {
                if (center != null) {
                    //get declination at center
                    Date d = CoordinatedTime.currentDate();
                    GeomagneticField gmf;
                    gmf = new GeomagneticField(
                            (float) center.get().getLatitude(),
                            (float) center.get().getLongitude(), 0,
                            d.getTime());

                    offset = gmf.getDeclination();
                }
            }

            if (showProjectionProportition)
                computeEllipseTestVerts();
        }
        super.onPointsChanged();
    }

    @Override
    public GeoPointMetaData getCenter() {
        synchronized (this) {
            return center;
        }
    }

    public double getOffsetAngle() {
        return offset;
    }

    @Override
    public GeoPoint[] getPoints() {
        if (center == null)
            return new GeoPoint[0];
        else
            return new GeoPoint[] {
                    center.get()
            };
    }

    @Override
    public GeoPointMetaData[] getMetaDataPoints() {
        if (center == null)
            return new GeoPointMetaData[0];
        else
            return new GeoPointMetaData[] {
                    center
            };
    }

    @Override
    public GeoBounds getBounds(MutableGeoBounds bounds) {
        if (bounds != null) {
            bounds.set(this.getPoints());
            return bounds;
        } else {
            return GeoBounds.createFromPoints(
                    this.getPoints());
        }
    }

}
