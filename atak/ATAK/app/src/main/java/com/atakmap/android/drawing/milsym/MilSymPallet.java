
package com.atakmap.android.drawing.milsym;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.EnterLocationTool;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.user.icon.IconPallet;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;

final class MilSymPallet
        implements IconPallet, MilSymFragment.OnSymbolSelectedListener {

    private static final String TAG = "MilSymPallet";

    private final MapView _mapView;
    private final MilSymFragment _fragment;
    private final String _name;
    String selectedCode;

    public MilSymPallet(MapView mapView, Context context,
            ISymbologyProvider symbology) {
        _mapView = mapView;
        _name = symbology.getName();
        _fragment = new MilSymFragment(context, symbology.getSymbolTable(),
                false);
        _fragment.setOnSymbolSelectedListener(this);
    }

    @Override
    public String getTitle() {
        return _name;
    }

    @Override
    public String getUid() {
        return "com.atakmap.android.milsym.plugin.MilSymPallet";
    }

    @Override
    public Fragment getFragment() {
        return _fragment;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }

    @Override
    public MapItem getPointPlacedIntent(GeoPointMetaData point,
            String uid) {
        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                point);
        mc.showCotDetails(false);
        Marker marker = mc.placePoint();
        marker.setMetaString("milsym", selectedCode);
        String s = CotUtils.cotTypeFromMil2525C(selectedCode);
        if (FileSystemUtils.isEmpty(s))
            s = "a-u-G";

        marker.setType(s);
        marker.persist(_mapView.getMapEventDispatcher(), null,
                MilSymPallet.class);

        return marker;
    }

    @Override
    public void select(int resId) {
    }

    @Override
    public void clearSelection(boolean bPauseListener) {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void onSymbolSelected(ISymbolTable.Symbol symbol) {

        this.selectedCode = symbol.getCode();
        Tool tool = ToolManagerBroadcastReceiver.getInstance()
                .getActiveTool();
        if (tool != null
                && EnterLocationTool.TOOL_NAME
                        .equals(tool.getIdentifier())) {
            return;
        }

        Intent myIntent = new Intent();
        myIntent.setAction("com.atakmap.android.maps.toolbar.BEGIN_TOOL");
        myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
        myIntent.putExtra("current_type", "point");
        myIntent.putExtra("checked_position", 0); //not using for this pallet, just set not -1
        AtakBroadcast.getInstance().sendBroadcast(myIntent);

    }

}
