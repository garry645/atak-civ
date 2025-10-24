
package com.atakmap.android.drawing.milsym;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.atakmap.android.drawing.DrawingToolsMapComponent;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.tools.ShapeCreationTool;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;

final class MilSymManager
        implements MapEventDispatcher.MapEventDispatchListener,
        MapTouchController.DeconflictionListener,
        MapGroup.OnItemListChangedListener,
        MapRenderer2.OnCameraChangedListener2,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private final FeatureDataStore2 datastore = new FeatureSetDatabase2(null);
    private final Map<MapItem, TacticalGraphic> tacticalGraphics = new ConcurrentHashMap<>();
    private final Map<Marker, TacticalMarker> tacticalMarkers = new ConcurrentHashMap<>();

    private final MapView mapView;
    private Timer movedTimer = null;

    AtakPreferences _prefs;

    private final PropertyChangeListener milSymChangedListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            AttributeSet attrs = (AttributeSet) propertyChangeEvent.getSource();
            String uid = attrs.getStringAttribute("uid", null);
            if (uid != null) {
                MapItem mapItem = MapView.getMapView().getMapItem(uid);
                onMilSymChanged(mapItem);
            }
        }
    };

    public MilSymManager(MapView mapView, Context context) {
        this.mapView = mapView;

        SymbologyProvider.addSymbologyProvidersChangedListener(new SymbologyProvider.SymbologyProvidersChangedListener() {
            @Override
            public void onSymbologyProvidersChanged() {
                refreshAll();
            }
        });

        _prefs = new AtakPreferences(context);

        _prefs.registerListener(this);
    }

    void refreshAll() {

        // short circuit if no tactical graphics exist during the refresh
        if (tacticalGraphics.isEmpty())
            return;

        final GeoBounds screenBounds = mapView.getBounds();

        for (TacticalGraphic tg : tacticalGraphics.values()) {
            if (tg.isInBounds(screenBounds))
                tg.refresh();
        }
    }

    boolean shouldAdd(MapItem item) {
        if (item instanceof Marker)
            return true;
        MapItem originalItem = item;
        //DrawingEllipse has meta value "ignoreRender" by default
        if (item instanceof DrawingEllipse) {
            item = ((DrawingEllipse) item).getOutermostEllipse();
        }
        return !item.hasMetaValue("ignoreRender") && isSupported(originalItem);
    }

    boolean isSupported(MapItem item) {
        return item instanceof EditablePolyline
                || MilSymUtils.isDrawingShape(item);
    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        final MapItem item = mapEvent.getItem();
        AttributeSet attrs = MarshalManager.marshal(item, MetaDataHolder2.class,
                AttributeSet.class);
        switch (mapEvent.getType()) {
            case MapEvent.ITEM_ADDED:
                if (shouldAdd(item)) {
                    attrs.addPropertyChangeListener("milsym",
                            milSymChangedListener);
                    onMilSymChanged(item);
                }
                break;
            case MapEvent.ITEM_REMOVED:
                attrs.removePropertyChangeListener("milsym",
                        milSymChangedListener);

                if (tacticalGraphics.remove(item) != null) {
                    try {
                        datastore.deleteFeatureSet(item.getSerialId());
                    } catch (DataStoreException ignored) {
                    }
                }
                break;
            default:
                break;
        }
    }

    private void onMilSymChanged(MapItem mapItem) {
        if (mapItem == null)
            return;

        final String milsym = mapItem.getMetaString("milsym", null);
        // XXX - the below is not thread-safe!
        if (mapItem instanceof Shape) {
            final boolean tracked = tacticalGraphics.containsKey(mapItem);
            if (milsym != null && !tracked) {
                tacticalGraphics.put(mapItem, new TacticalGraphic(datastore, (Shape) mapItem));
            } else if (milsym == null && tracked) {
                TacticalGraphic graphic = tacticalGraphics.get(mapItem);
                try {
                    datastore.deleteFeatureSet(mapItem.getSerialId());
                } catch (DataStoreException ignored) {
                }
                if (graphic != null && !graphic
                        .isHidingDrawingShape(graphic.getSubject())) {
                    graphic.showDrawingShape();
                }
                tacticalGraphics.remove(mapItem);
            }
        } else if (mapItem instanceof Marker) {
            final boolean tracked = tacticalMarkers.containsKey(mapItem);
            if (milsym != null && !tracked) {
                tacticalMarkers.put((Marker) mapItem,
                        new TacticalMarker((Marker) mapItem));
            } else if (milsym == null && tracked) {
                tacticalMarkers.remove(mapItem);
            }
        }
    }

    public FeatureDataStore2 getDataStore() {
        return datastore;
    }

    @Override
    public void onConflict(SortedSet<MapItem> sortedSet) {
        Iterator<MapItem> iterator = sortedSet.iterator();

        while (iterator.hasNext()) {
            MapItem mapItem = iterator.next();
            if (mapItem instanceof DrawingCircle
                    && MilSymUtils.isEditing(mapItem)
                    && mapItem.hasMetaValue("milsym"))
                iterator.remove();
        }
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup mapGroup) {
        Tool activeTool = ToolManagerBroadcastReceiver.getInstance()
                .getActiveTool();

        if (mapGroup == DrawingToolsMapComponent.getGroup() &&
                item.hasMetaValue("creating") &&
                activeTool != null &&
                activeTool.getIdentifier()
                        .equals(ShapeCreationTool.TOOL_IDENTIFIER)) {
            if (shouldAdd(item)) {
                AttributeSet attrs = MarshalManager.marshal(item,
                        MetaDataHolder2.class, AttributeSet.class);
                attrs.addPropertyChangeListener("milsym",
                        milSymChangedListener);
                onMilSymChanged(item);
            }
        }
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup mapGroup) {
        Tool activeTool = ToolManagerBroadcastReceiver.getInstance()
                .getActiveTool();
        if (mapGroup == DrawingToolsMapComponent.getGroup() &&
                item.hasMetaValue("creating") &&
                activeTool != null &&
                activeTool.getIdentifier()
                        .equals(ShapeCreationTool.TOOL_IDENTIFIER)) {
            AttributeSet attrs = MarshalManager.marshal(item,
                    MetaDataHolder2.class, AttributeSet.class);
            attrs.removePropertyChangeListener("milsym", milSymChangedListener);

            if (tacticalGraphics.remove(item) != null) {
                try {
                    datastore.deleteFeatureSet(item.getSerialId());
                } catch (DataStoreException ignored) {
                }
            }
        }

    }

    @Override
    public void onCameraChanged(MapRenderer2 mapRenderer2) {
        if (movedTimer != null)
            movedTimer.cancel();

        movedTimer = new Timer("MilSymManagerTimer");

        movedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshAll();
                } catch (Exception ignored) {
                    // since there is no indication at this level that the manager is being
                    // disposed it will attempt to execute potentially after shutdown has
                    // started.  In this case an exception will be thrown and caught and
                    // things will quietly continue
                }
            }
        }, 400);
    }

    @Override
    public void onCameraChangeRequested(MapRenderer2 mapRenderer2) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if(key == null)
            return;
        if (key.equals("alt_display_agl") ||
                key.equals(UnitPreferences.ALTITUDE_REFERENCE)
                || key.equals(UnitPreferences.ALTITUDE_UNITS)) {
            refreshAll();
        }
    }
}
