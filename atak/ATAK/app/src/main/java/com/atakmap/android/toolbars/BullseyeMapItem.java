
package com.atakmap.android.toolbars;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;

import com.atakmap.android.cot.importer.MarkerImporter;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.widgets.AngleOverlayShape;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.spatial.file.export.KMZFolder;
import com.ekito.simpleKML.model.Folder;

public class BullseyeMapItem extends Marker
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "BullseyeMapItem";

    public static final String BULLSEYE_COT_TYPE = "u-r-b-bullseye";

    private static final double RANGE_CIRCLE_STROKE_WEIGHT = 3d;

    private static final int RANGE_CIRCLE_STYLE = Polyline.STYLE_OUTLINE_STROKE_MASK
            | Polyline.STYLE_CLOSED_MASK |
            Polyline.STYLE_STROKE_MASK | Polyline.STYLE_FILLED_MASK;

    private final AngleOverlayShape aos;
    private final MapView mapView;
    private RangeAndBearingMapItem rabmi;
    private final RangeCircle rabCircle;
    private MapEventDispatcher.MapEventDispatchListener centerAdded = null;
    private final AtakPreferences prefs;
    private boolean ignorePreferences = false;

    /**
     * Construct a bullseye map item with a specified center point and a provided uid
     * @param mapView the map view
     * @param point the point to start out with.   If a center point map item is added, then the
     *              center will be driven by the point map item
     * @param uid the uid that is provided to the bullseye
     */
    public BullseyeMapItem(MapView mapView, GeoPointMetaData point,
            String uid) {
        super(point, uid);
        this.mapView = mapView;
        setType(BULLSEYE_COT_TYPE);
        // bullseyes start out green
        setColor(Color.GREEN);

        aos = new AngleOverlayShape(getUID() + ".AOS");
        aos.setCenter(point);
        aos.setSimpleSpokeView(true);
        aos.setProjectionProportion(true);
        aos.setMetaBoolean("addToObjList", false);
        aos.setCenterMarker(this);
        aos.setMetaString("iconUri", ATAKUtilities.getResourceUri(
                R.drawable.bullseye));
        setClickable(true);
        setMovable(true);

        rabCircle = new RangeCircle(mapView, getUID() + ".RC");
        rabCircle.setMetaBoolean("nevercot", true);
        rabCircle.setMetaBoolean("removable", false);
        rabCircle.setMetaBoolean("editable", false);
        rabCircle.setMetaBoolean("addToObjList", false);
        rabCircle.setStyle(RANGE_CIRCLE_STYLE);
        rabCircle.setStrokeWeight(RANGE_CIRCLE_STROKE_WEIGHT);
        rabCircle.setClickable(false);
        rabCircle.setCenterMarker(this);
        // default range ring size
        setRadiusRings(100);
        setNumRings(1);
        // by default the range rings are not visible
        setMetaBoolean("rangeRingVisible", false);

        aos.setMetaString("rangeRingUID", rabCircle.getUID());

        addOnGroupChangedListener(centerGroupChangeListener);

        prefs = AtakPreferences.getInstance(mapView.getContext());
        prefs.registerListener(this);
        onSharedPreferenceChanged(prefs.getSharedPrefs(), "rab_north_ref_pref");
        onSharedPreferenceChanged(prefs.getSharedPrefs(), "rab_brg_units_pref");
    }

    /**
     * Sets the radius of each of the range rings associated with the bullseye
     * @param v the value in meters
     */
    public void setRadiusRings(double v) {
        rabCircle.setRadius(v);
        refreshRangeRings(
                getVisible() && getMetaBoolean("rangeRingVisible", false));
    }

    /**
     * Sets the number of range rings that will be drawn around the bullseye.
     * @param v the number of range rings
     */
    public void setNumRings(int v) {
        rabCircle.setNumRings(v);
        refreshRangeRings(
                getVisible() && getMetaBoolean("rangeRingVisible", false));
    }

    @Override
    public void setPoint(GeoPointMetaData point) {
        super.setPoint(point);
        if (rabCircle != null)
            rabCircle.setCenterPoint(point);

        if (aos != null)
            aos.setCenter(point);
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        aos.setClickable(clickable);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        aos.setTitle(title);
        rabCircle.setTitle(title);
    }

    @Override
    protected void onVisibleChanged() {
        super.onVisibleChanged();
        aos.setVisible(getVisible());
        rabCircle.setVisible(getVisible() && getMetaBoolean(
                "rangeRingVisible", false));
    }

    void removeOverlay(BullseyeMapItem centerMarker,
            boolean removeCenter) {
        centerMarker.removeOnGroupChangedListener(centerGroupChangeListener);

        MapItem mi = MapGroup.deepFindItemWithMetaString(
                MapView._mapView.getRootGroup(), "uid",
                centerMarker.getMetaString("bullseyeUID", ""));

        MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
        MapItem rings = null;
        if (rabGroup != null && mi != null)
            rings = rabGroup.deepFindUID(mi.getMetaString("rangeRingUID", ""));

        //remove rings, if it had range rings
        if (rings instanceof DrawingCircle)
            rings.removeFromGroup();

        if (mi instanceof AngleOverlayShape)
            mi.removeFromGroup();

        if (centerMarker.getType().contentEquals(BULLSEYE_COT_TYPE)) {
            if (removeCenter)
                centerMarker.removeFromGroup();
        } else {
            centerMarker.removeMetaData("bullseyeUID");
            centerMarker.removeMetaData("bullseyeOverlay");
        }

        if (rabmi != null)
            rabmi.removeFromGroup();

        prefs.unregisterListener(this);
    }

    private void makeRabWidget() {
        final String rabUUID = getUID() + ".RB";
        final String centerMarkerUID = getMetaString("centerMarkerUID", null);
        final String edgeMarkerUID = getMetaString("edgeMarkerUID", null);
        final MapItem centerMarker = mapView.getMapItem(centerMarkerUID);
        final MapItem edgeMarker = mapView.getMapItem(edgeMarkerUID);

        if (centerMarker instanceof PointMapItem
                && edgeMarker instanceof PointMapItem) {
            rabmi = RangeAndBearingMapItem
                    .createOrUpdateRABLine(rabUUID, (PointMapItem) centerMarker,
                            (PointMapItem) edgeMarker, false);
            rabmi.setMetaBoolean("addToObjList", false);
            OnGroupChangedListener ogcl = new OnGroupChangedListener() {
                @Override
                public void onItemAdded(MapItem item, MapGroup group) {
                }

                @Override
                public void onItemRemoved(MapItem item, MapGroup group) {
                    if (rabmi != null)
                        rabmi.removeFromGroup();
                }
            };
            centerMarker.addOnGroupChangedListener(ogcl);
            edgeMarker.addOnGroupChangedListener(ogcl);

            rabmi.setMetaBoolean("removable", false);
            if (rabmi.getGroup() == null)
                RangeAndBearingMapComponent.getGroup().addItem(rabmi);

        } else {
            if (rabmi != null) {
                rabmi.removeFromGroup();
            }
        }
    }

    @Override
    public void setMetaString(String key, String value) {
        super.setMetaString(key, value);
        if (key.equals("centerMarkerUID") || key.equals("edgeMarkerUID"))
            pair();
    }

    private void pair() {

        final String centerMarkerUID = getMetaString("centerMarkerUID", null);
        final String edgeMarkerUID = getMetaString("edgeMarkerUID", null);

        if (centerMarkerUID == null)
            return;

        final MapItem centerMarker = mapView.getMapItem(centerMarkerUID);
        if (centerMarker == null && centerAdded == null) {
            mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.ITEM_ADDED,
                    centerAdded = new MapEventDispatcher.MapEventDispatchListener() {
                        @Override
                        public void onMapEvent(MapEvent event) {
                            if (event.getItem().getUID()
                                    .equals(centerMarkerUID)) {

                                if (getGroup() != null
                                        && event.getItem().getGroup() != null) {
                                    pair();
                                    makeRabWidget();
                                }
                            }

                        }
                    });
        }

        if (centerMarker instanceof BullseyeMapItem)
            return;

        if (centerMarker instanceof PointMapItem) {
            if (centerAdded != null)
                mapView.getMapEventDispatcher()
                        .removeMapEventListener(centerAdded);

            final PointMapItem pmi = (PointMapItem) centerMarker;
            setMovable(centerMarker.getMovable());

            pmi.addOnGroupChangedListener(new MapItem.OnGroupChangedListener() {
                @Override
                public void onItemAdded(MapItem item, MapGroup group) {

                }

                @Override
                public void onItemRemoved(MapItem item, MapGroup group) {
                    BullseyeMapItem.this.removeFromGroup();
                }
            });

            pmi.addOnPointChangedListener(new OnPointChangedListener() {
                @Override
                public void onPointChanged(PointMapItem item) {
                    setPoint(pmi.getPoint());
                    persist(mapView.getMapEventDispatcher(),
                            null, BullseyeMapItem.this.getClass());
                }
            });
            addOnPointChangedListener(new OnPointChangedListener() {
                @Override
                public void onPointChanged(PointMapItem item) {
                    pmi.setPoint(getPoint());
                    pmi.persist(mapView.getMapEventDispatcher(),
                            null, BullseyeMapItem.this.getClass());
                }
            });

            centerMarker.addOnVisibleChangedListener(
                    centerMarkerVisibilityListener);

            if (edgeMarkerUID != null) {
                MapItem mi = mapView.getMapItem(edgeMarkerUID);
                if (mi != null) {
                    makeRabWidget();
                } else {
                    mapView.getMapEventDispatcher().addMapEventListener(
                            MapEvent.ITEM_ADDED,
                            new MapEventDispatcher.MapEventDispatchListener() {
                                @Override
                                public void onMapEvent(MapEvent event) {
                                    if (event.getItem().getUID()
                                            .equals(edgeMarkerUID)) {
                                        makeRabWidget();
                                    }
                                }
                            });
                }
            }

        }

    }

    /**
     * For the bullseye, set the bearing units as a boolean
     * @param type the bearing units
     */
    public void setBearingUnits(Angle type) {
        aos.setBearingUnits(type == Angle.DEGREE);
    }

    /**
     * The direction of the arrow for the bullseye, either from center to edge or edge to center
     * @param edgeToCenter true if it is edge to center
     */
    public void setEdgeToCenterDirection(boolean edgeToCenter) {
        aos.setEdgeToCenterDirection(edgeToCenter);
    }

    /**
     * Returns the current direction of the arrow
     * @return true if it is edge to center
     */
    public boolean isShowingEdgeToCenter() {
        return aos.isShowingEdgeToCenter();
    }

    /**
     * The radius provided in a specific distance unit
     * @param radius the radius in the provided units
     * @param distUnits the distance units to use.
     */
    public void setRadius(double radius, Span distUnits) {
        aos.setRadius(SpanUtilities.convert(radius, distUnits, Span.METER));
    }

    /**
     * Set he north reference for the bullseye
     * @param northRef the reference { TRUE, MAGNETIC, GRID }
     */
    public void setNorthReference(NorthReference northRef) {
        aos.setNorthReference(northRef);
    }

    /**
     * Set the color of the range rings.
     * @param color the color
     */
    public void setColorRings(int color) {
        rabCircle.setColor(color);
        refreshRangeRings(
                getVisible() && getMetaBoolean("rangeRingVisible", false));
    }

    /**
     * For the range rings, get the current radius rings spacing in meters.
     * @return the spacing distance in meters
     */
    public double getRadiusRings() {
        return rabCircle.getRadius();
    }

    /**
     * For the range rings, get the total number of rings to draw.
     * @return the number of rings
     */
    public int getNumRings() {
        return rabCircle.getNumRings();
    }

    /**
     * Get the radius of the bullseye (not to be confused with the separation radius for each of the rings.
     * @return the radius of the bullseye
     */
    public double getRadius() {
        return aos.getRadius();
    }

    /**
     * The units currently used by the bullseye radius use in conjunction with getRadius
     * @return the units currently used.
     */
    public Span getRadiusUnits() {
        return aos.getRadiusUnits();
    }

    /**
     * Return the bearing units current being used for the bullseye.
     * @return either MIL or DEGREE
     */
    public Angle getBearingUnits() {
        return aos.getBearingUnits();
    }

    /**
     * Return the north reference used by the
     * @return a valid north reference
     */
    public NorthReference getNorthReference() {
        return aos.getNorthReference();
    }

    public static class BullseyeMarkerImporter extends MarkerImporter {
        public BullseyeMarkerImporter(MapView mapView, MapGroup group) {
            super(mapView, group, BULLSEYE_COT_TYPE, false);
        }

        protected Marker createMarker(CotEvent event, Bundle extras) {
            //Log.d(TAG, "creating a new marker for: "+event.getUID());
            Marker m = new BullseyeMapItem(_mapView,
                    GeoPointMetaData.wrap(event.getGeoPoint()),
                    event.getUID());
            m.setType(event.getType());
            // push CoT markers up the Z-order stack using 1 higher than the default order.
            m.setZOrder(m.getZOrder() - 1d);

            m.setStyle(m.getStyle() | Marker.STYLE_MARQUEE_TITLE_MASK);
            m.setMetaString("how", event.getHow());
            m.setMetaString("entry", "CoT");

            // XXX: HACK HACK HACK HACK HACK HACK HACK HACK HACK
            if (isLocalImport(extras)) {
                m.setMetaBoolean("transient", false);
                m.setMetaBoolean("archive", true);

                // XXX - items need to have an 'entry' of "user" in order to
                // have elevation auto-populated from DTED. User items
                // persisted between invocations of ATAK lose their
                // metadata and are no registered for elevation
                // auto-population
                // marker.setMetaString("entry", "user");
            }

            // check to see if this is ourself
            final String deviceUID = _mapView.getSelfMarker().getUID();
            if (deviceUID.equals(event.getUID())) {
                m.setMetaBoolean("self", true);
                m.setIcon(new Icon.Builder().setImageUri(0,
                        ATAKUtilities.getResourceUri(R.drawable.friendlydir))
                        .setAnchor(32, 32).build());
            }

            // Give it full mutability.
            m.setMovable(true);
            m.setMetaBoolean("removable", true);
            m.setMetaBoolean("editable", true);

            // flag a marker as interesting at this time.  this will be used by the offscreen
            // search mechanism to weed out offscreen markers that are close but may no longer be
            // interesting.   New markers are interesting....
            if (!isStateSaverImport(extras))
                m.setMetaLong("offscreen_interest",
                        SystemClock.elapsedRealtime());

            return m;
        }
    }

    @Override
    protected Folder toKml() {
        // do not perform super - get everything from the various composite pieces starting with
        // the angle overlay shape
        try {
            Folder folder = (Folder) aos.toObjectOf(Folder.class,
                    null);

            Folder rFolder = (Folder) rabCircle.toObjectOf(Folder.class,
                    null);
            if (rFolder != null) {
                folder.getFeatureList().addAll(rFolder.getFeatureList());
                folder.getStyleSelector()
                        .addAll(rFolder.getStyleSelector());
            }

            final MapItem mapItem = mapView
                    .getMapItem(getMetaString("centerMarkerUID", null));
            if (mapItem instanceof Marker) {
                Folder mFolder = (Folder) ((Marker) mapItem)
                        .toObjectOf(Folder.class, null);
                if (mFolder != null) {
                    folder.getFeatureList().addAll(mFolder.getFeatureList());
                    folder.getStyleSelector()
                            .addAll(mFolder.getStyleSelector());
                }
            }

            return folder;
        } catch (Exception e) {
            Log.e(TAG, "Failed to export bullseye to KML", e);
        }

        return null;
    }

    @Override
    protected KMZFolder toKmz() {
        // do not perform super - get everything from the various composite pieces starting with
        // the angle overlay shape
        try {
            final KMZFolder folder = (KMZFolder) aos
                    .toObjectOf(KMZFolder.class, null);

            final KMZFolder rFolder = (KMZFolder) rabCircle
                    .toObjectOf(KMZFolder.class, null);
            if (rFolder != null)
                folder.getFeatureList().add(rFolder);

            final MapItem mapItem = mapView
                    .getMapItem(getMetaString("centerMarkerUID", null));
            if (mapItem instanceof Marker) {
                KMZFolder mFolder = (KMZFolder) ((Marker) mapItem)
                        .toObjectOf(KMZFolder.class, null);
                if (mFolder != null) {
                    folder.getFeatureList().add(mFolder);
                }
            }

            return folder;

        } catch (Exception e) {
            Log.e(TAG, "Failed to export bullseye to KMZ", e);
        }

        return null;
    }

    private void refreshRangeRings(final boolean visible) {
        if (aos.isShowingEdgeToCenter())
            rabCircle.setColor(Color.RED); //RED
        else
            rabCircle.setColor(Color.GREEN); //GREEN
        rabCircle.setStrokeWeight(RANGE_CIRCLE_STROKE_WEIGHT);
        rabCircle.setStyle(RANGE_CIRCLE_STYLE);
        rabCircle.setClickable(false);
        rabCircle.setMetaBoolean("editable", false);
        rabCircle.setMetaBoolean("addToObjList", false);
        rabCircle.refresh(mapView.getMapEventDispatcher(), null,
                rabCircle.getClass());

        rabCircle.setVisible(visible);
        aos.setMetaString("rangeRingUID", rabCircle.getUID());
    }

    private final OnGroupChangedListener centerGroupChangeListener = new OnGroupChangedListener() {
        @Override
        public void onItemAdded(MapItem item, MapGroup group) {
            BullseyeMapItem bullseyeMapItem = (BullseyeMapItem) item;
            bullseyeMapItem.getGroup().addItem(bullseyeMapItem.aos);
            bullseyeMapItem.getGroup().addItem(rabCircle);

        }

        @Override
        public void onItemRemoved(MapItem item, MapGroup group) {
            ((BullseyeMapItem) item).aos.removeFromGroup();
            MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
            MapItem rings = null;
            if (rabGroup != null) {
                rings = rabGroup.deepFindUID(((BullseyeMapItem) item).aos
                        .getMetaString("rangeRingUID", ""));
            }
            //remove rings, if it had range rings
            if (rings instanceof DrawingCircle)
                rings.removeFromGroup();

            if (rabmi != null)
                rabmi.removeFromGroup();

        }
    };

    /**
     * For a bullseye, the changes to the global north reference and angular units are
     * global which follows the behavior of the range and bearing arrow.   A plugin might want
     * to localize the north reference and angular units and not have it track changes global
     * changes
     * @param ignore true will ignore the global changes
     */
    public void setIgnorePreferenceChanges(boolean ignore) {
        this.ignorePreferences = ignore;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp,
            String key) {

        if (ignorePreferences)
            return;

        if (key == null)
            return;

        switch (key) {
            case "rab_north_ref_pref":
                setNorthReference(NorthReference
                        .findFromValue(Integer.parseInt(sp.getString(key,
                                String.valueOf(
                                        NorthReference.MAGNETIC.getValue())))));
                break;
            case "rab_brg_units_pref":
                setBearingUnits(
                        Angle.findFromValue(Integer.parseInt(sp.getString(
                                key,
                                String.valueOf(Angle.DEGREE.getValue())))));
                break;
        }
    }

    private final OnVisibleChangedListener centerMarkerVisibilityListener = new OnVisibleChangedListener() {
        @Override
        public void onVisibleChanged(MapItem item) {
            final String uid = getMetaString("centerMarkerUID", null);
            if (uid != null && uid.equals(item.getUID())) {
                setVisible(item.getVisible());
            } else {
                item.removeOnVisibleChangedListener(
                        centerMarkerVisibilityListener);
            }
        }
    };
}
