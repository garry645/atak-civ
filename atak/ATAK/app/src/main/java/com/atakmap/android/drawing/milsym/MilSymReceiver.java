
package com.atakmap.android.drawing.milsym;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MultiPolyline;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;

class MilSymReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {
    final static String ACTION_SELECT_TYPE = "com.atakmap.android.milsym.SELECT_TYPE";
    final static String ACTION_REVERSE_PATH = "com.atakmap.android.milsym.REVERSE_PATH";
    final static String EXTRA_UID = "uid";

    private final Context context;
    private String selectorUid;
    private MilSymFragment fragment;
    private final Map<ISymbolTable, Map<String, List<ISymbolTable.Folder>>> symbolPathMap;

    private final DrawingToolbarExtender toolbarExtender;

    private Runnable pending;

    DrawingToolbarExtender getToolbarExtender() {
        return toolbarExtender;
    }

    MilSymReceiver(final MapView mapView, final Context context,
            final Map<ISymbolTable, Map<String, List<ISymbolTable.Folder>>> symbolPathMap) {
        super(mapView);
        this.context = context;
        this.symbolPathMap = symbolPathMap;
        toolbarExtender = new DrawingToolbarExtender(mapView, context,
                this);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(ACTION_SELECT_TYPE)) {
            showPane(intent.getStringExtra(EXTRA_UID),
                    EnumSet.copyOf(ISymbolTable.MASK_AREAS_AND_LINES),
                    new MilSymFragment.OnClearSymbolListener() {
                        @Override
                        public void onClear() {
                            MapItem item = getMapView().getMapItem(selectorUid);
                            if (item != null) {
                                TacticalGraphic.clearMilsymMetadata(item);
                                // persist the symbology change
                                item.persist(
                                        getMapView().getMapEventDispatcher(),
                                        null, MilSymManager.class);
                            }

                            closeDropDown();
                        }
                    }, new MilSymFragment.OnSymbolSelectedListener() {
                        @Override
                        public void onSymbolSelected(
                                ISymbolTable.Symbol symbol) {
                            MapItem item = getMapView().getMapItem(selectorUid);

                            if (item != null) {
                                item.setMetaString("milsym", symbol.getCode());

                                // propagate symbol code to children for `MultiPolyline`
                                if (item instanceof MultiPolyline)
                                    for (MapItem child : ((MultiPolyline) item)
                                            .getLines())
                                        child.setMetaString("milsym",
                                                symbol.getCode());

                                // persist the symbology change
                                item.persist(
                                        MapView.getMapView()
                                                .getMapEventDispatcher(),
                                        null, MilSymManager.class);
                            }
                        }
                    });
        } else if (action.equals(ACTION_REVERSE_PATH)) {
            String uid = intent.getStringExtra(EXTRA_UID);
            Polyline polyline = (Polyline) getMapView().getMapItem(uid);
            GeoPoint[] geoPoints = polyline.getPoints();
            Collections.reverse(Arrays.asList(geoPoints));
            polyline.setPoints(geoPoints);

            String milsym = polyline.getMetaString("milsym", null);
            // cycle the milsym metadata to force control point regeneration (if necessary)
            if (milsym != null) {
                polyline.removeMetaData("milsym");
                polyline.setMetaString("milsym", milsym);
            }
        }

        toolbarExtender.onReceive(context, intent);
    }

    public void showPane(final String uid, EnumSet<ShapeType> filter,
            MilSymFragment.OnClearSymbolListener clearCallback,
            MilSymFragment.OnSymbolSelectedListener callback) {
        selectorUid = uid;
        final ISymbologyProvider provider = ATAKUtilities.getDefaultSymbologyProvider();

        // instantiate the plugin view if necessary

        synchronized (MilSymReceiver.this) {
            if (fragment == null)
                fragment = new MilSymFragment(context, provider.getSymbolTable(),
                        true);
            else
                fragment.setSymbolTable(provider.getSymbolTable());
        }
        fragment.setOnSymbolSelectedListener(
                new MilSymFragment.OnSymbolSelectedListener() {
                    @Override
                    public void onSymbolSelected(ISymbolTable.Symbol symbol) {
                        callback.onSymbolSelected(symbol);
                        closeDropDown();
                    }
                });
        fragment.setOnClearSymbolListener(clearCallback);

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!isVisible()) {

                    MapItem item = getMapView().getMapItem(uid);
                    if (item != null) {
                        fragment.reset(item);
                        final String milsym = item.getMetaString("milsym",
                                null);
                        if (milsym != null) {
                            final Map<String, List<ISymbolTable.Folder>> map =
                                    symbolPathMap.get(ATAKUtilities.getDefaultSymbologyProvider().getSymbolTable());
                            if(map != null)
                            {
                                final List<ISymbolTable.Folder> path = map.get(milsym);
                                if (path != null) {
                                    fragment.setPath(path);
                                }
                            }
                        }

                    } else {
                        fragment.reset(filter);
                    }

                    showDropDown(fragment, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                            HALF_HEIGHT, MilSymReceiver.this);
                }
            }
        };

        if (!isClosed()) {
            pending = r;
            closeDropDown();
            return;
        }

        r.run();
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownClose() {
        if (pending != null)
            pending.run();
        pending = null;
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    protected boolean onBackButtonPressed() {
        if (fragment.stopSearching())
            return true;

        return super.onBackButtonPressed();
    }

}
