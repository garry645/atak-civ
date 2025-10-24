
package com.atakmap.android.munitions;

import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.munitions.util.MunitionsHelper;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.Circle;
import com.atakmap.android.warning.DangerCloseCalculator;
import com.atakmap.app.R;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import gov.tak.api.annotation.DeprecatedApi;

public class RangeRing extends PointMapItem implements AnchoredMapItem,
        DangerCloseCalculator.ClosestItemListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        MapItem.OnGroupChangedListener, PointMapItem.OnPointChangedListener,
        MapItem.OnVisibleChangedListener {

    public static final String MENU = "menus/danger_close_menu.xml";

    private final MapView _mapView;
    private MapGroup _mapGroup;
    private final PointMapItem _target;
    private Circle _standing;
    private Circle _standingFilled;
    private Circle _prone;
    private Circle _proneFilled;
    private final String _weapon;
    private final int _innerRange;
    private final int _outerRange;
    private final String _fromLine;
    private String _category;

    // TODO:switch to using GL version but for now this works

    public RangeRing(MapView mapView, MapGroup mapGroup1,
            PointMapItem target1, String weapon, int innerRange,
            int outerRange, String from) {
        super(GeoPoint.ZERO_POINT,
                target1.getUID() + "." + weapon + "." + from);

        _mapView = mapView;
        _weapon = weapon;
        _mapGroup = mapGroup1;
        _target = target1;
        _innerRange = innerRange;
        _outerRange = outerRange;
        _fromLine = from;
        setMetaString("target", _target.getMetaString("uid", null));
        setTitle(weapon);
        setMetaString("iconUri", ATAKUtilities.getResourceUri(
                R.drawable.ic_circle));
        setMetaInteger("color", Color.RED);
        setMetaString("deleteAction", DangerCloseReceiver.REMOVE_WEAPON);
        setMetaBoolean("removable", false);
        setMetaString("menu", "menus/range_ring_menu.xml");
        AtakPreferences pref = AtakPreferences
                .getInstance(mapView.getContext());
        pref.registerListener(this);

        if (outerRange > 0) {
            createStanding(mapView, outerRange);
        }

        if (innerRange > 0
                && pref.get("prone_standing_coloring", false)) {
            createProne();
        }

        _target.addOnPointChangedListener(this);
        _target.addOnVisibleChangedListener(this);
        _target.addOnGroupChangedListener(this);
        _mapGroup.addItem(this);

        if (hasNoLine()) {
            DangerCloseCalculator.getInstance().registerListener(this, _target);
        }
        if (_target.getMetaInteger("dangerclose", 0) < _outerRange)
            _target.setMetaInteger("dangerclose", _outerRange);
    }

    private void createStanding(MapView mapView, int outerRange) {
        _standing = new Circle(_target.getGeoPointMetaData(), outerRange);
        _standingFilled = new Circle(_target.getGeoPointMetaData(), outerRange);

        // set up the main standing circle
        _standing.setMetaString("menu", MENU);
        if (_mapGroup == null) {
            _mapGroup = mapView.getRootGroup();
        }

        _standing.setStrokeColor(Color.RED);

        _standing.setStrokeWeight(2d);
        _standing.setZOrder(2d);
        String newName;
        int spot = _weapon.indexOf("[");
        if (spot != -1) {
            newName = _weapon.substring(0, spot);
        } else {
            newName = _weapon;
        }
        newName = newName +
                " S: " +
                _outerRange +
                "m";
        _standing.setTitle(newName);
        _standing.setMetaString("target",
                _target.getMetaString("uid", null));
        _standing.setMetaBoolean("addToObjList", false);

        // render the fill to match previous behavior where the fill is not clickable
        _standingFilled.setFillColor(Color.argb(39, 255, 0, 0));
        _standingFilled.setStrokeColor(Color.RED);
        _standingFilled.setClickable(false);
        _standingFilled.setMetaBoolean("addToObjList", false);

        // add it to the map
        _mapGroup.addItem(_standing);
        _mapGroup.addItem(_standingFilled);


        if (hasNoLine()) {
            _standing.setVisible(false);
            _standingFilled.setVisible(false);
        } else {
            _standing.setVisible(_mapGroup.getVisible());
            _standingFilled.setVisible(_mapGroup.getVisible());
        }

    }

    @Override
    public void onPointChanged(PointMapItem item) {
        GeoPointMetaData ncp = item.getGeoPointMetaData();
        if (_prone != null) {
            _prone.setCenterPoint(ncp);
            _proneFilled.setCenterPoint(ncp);
        }
        if (_standing != null) {
            _standing.setCenterPoint(ncp);
            _standingFilled.setCenterPoint(ncp);
        }
    }

    // Visibility of target changed

    @Override
    public void onVisibleChanged(MapItem item) {
        if (_prone != null) {
            _prone.setVisible(item.getVisible());
            _proneFilled.setVisible(item.getVisible());
        }
        if (_standing != null) {
            _standing.setVisible(item.getVisible());
            _standingFilled.setVisible(item.getVisible());
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (key == null)
            return;
        if (key.equals("prone_standing_coloring")) {
            boolean showProne = sharedPreferences
                    .getBoolean("prone_standing_coloring", false);

            if (showProne) {
                if (getGroup() != null)
                    createProne();
            } else {
                removeProne();
            }
        }
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup newParent) {
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup oldParent) {
        removeItem();
    }

    private void createProne() {
        _prone = new Circle(_target.getGeoPointMetaData(), _innerRange);
        _proneFilled = new Circle(_target.getGeoPointMetaData(), _innerRange);

        // set up the main prone circle
        _prone.setMetaString("menu", MENU);

        _prone.setStrokeColor(Color.YELLOW);


        _prone.setStrokeWeight(2d);
        _prone.setZOrder(2d);
        int spot = _weapon.indexOf("[");
        String newName;
        if (spot != -1) {
            newName = _weapon.substring(0, spot);
        } else {
            newName = _weapon;
        }
        newName = newName +
                " P: " +
                _innerRange +
                "m";

        _prone.setTitle(newName);
        _prone.setMetaString("target", _target.getMetaString("uid", null));
        _prone.setMetaBoolean("addToObjList", false);

        // render the fill to match previous behavior where the fill is not clickable
        _proneFilled.setFillColor(Color.argb(39, 255, 255, 0));
        _proneFilled.setStrokeColor(Color.YELLOW);
        _proneFilled.setClickable(false);
        _proneFilled.setMetaBoolean("addToObjList", false);

        _mapGroup.addItem(_prone);
        _mapGroup.addItem(_proneFilled);

        if (hasNoLine()) {
            _prone.setVisible(false);
            _proneFilled.setVisible(false);
        } else {
            _prone.setVisible(_mapGroup.getVisible());
            _proneFilled.setVisible(_mapGroup.getVisible());
        }
    }

    private void removeProne() {
        if (_prone != null) {
            _prone.setVisible(false);
            _proneFilled.setVisible(false);
        }
        _mapGroup.removeItem(_prone);
        _mapGroup.removeItem(_proneFilled);
    }

    public void setCategory(String category) {
        _category = category;
    }

    public String getCategory() {
        return _category;
    }

    @Override
    public double getRange() {
        return _outerRange;
    }

    public String getFromLine() {
        return _fromLine;
    }

    public boolean hasNoLine() {
        return MunitionsHelper.hasNoLine(_fromLine);
    }

    @Override
    public void onClosestItem(DangerCloseCalculator.DangerCloseAlert ic) {
        if (ic.getDistance() < _outerRange) {
            //Log.d(TAG, "onClosestItem in range: " + ic.toString() + ", of: " + this.toString());
            if (_target.getMetaInteger("dangerclose", 0) < _outerRange)
                _target.setMetaInteger("dangerclose", _outerRange);
        }
    }

    private void removeItem() {
        if (_prone != null) {
            _prone.setVisible(false);
            _proneFilled.setVisible(false);
        }
        _mapGroup.removeItem(_prone);
        _mapGroup.removeItem(_proneFilled);

        if (_standing != null) {
            _standing.setVisible(false);
            _standingFilled.setVisible(false);
        }
        _mapGroup.removeItem(_standing);
        _mapGroup.removeItem(_standingFilled);

        _mapGroup.removeItem(this);

        DangerCloseCalculator.getInstance().unregisterListener(this);
        _target.setMetaInteger("dangerclose", 0);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (_prone != null) {
            _prone.setVisible(visible);
            _proneFilled.setVisible(visible);
        }
        if (_standing != null) {
            _standing.setVisible(visible);
            _standingFilled.setVisible(visible);
        }
    }

    @NonNull
    @Override
    public String toString() {
        if (_weapon != null)
            return "Range Rings for " + _weapon;
        return "Range Rings";
    }

    @Override
    protected void onVisibleChanged() {
        boolean visible = getVisible();
        if (_target != null) {
            _target.persist(_mapView.getMapEventDispatcher(),
                    null, this.getClass());
        }
        super.onVisibleChanged();
    }

    @Override
    public boolean getVisible() {
        return super.getVisible();
    }

    /**
     * Please use setVisible(boolean)
     * @deprecated
     * @param visible sets the visibility of the rings
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", removeAt = "5.8", forRemoval = true)
    public void setStandingProneVisible(boolean visible) {
        setVisible(visible);
    }

    public String getWeaponName() {
        return _weapon;
    }

    public void remove() {
        _target.removeOnPointChangedListener(this);
        _target.removeOnVisibleChangedListener(this);
        _target.removeOnGroupChangedListener(this);

        removeItem();
    }

    public Circle get_standing() {
        return _standing;
    }

    public Circle get_prone() {
        return _prone;
    }

    public int getInnerRange() {
        return _innerRange;
    }

    public int getOuterRange() {
        return _outerRange;
    }

    @Override
    public PointMapItem getAnchorItem() {
        return _target;
    }

    @Override
    public GeoPoint getPoint() {
        return _target.getPoint();
    }
}
