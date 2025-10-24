
package com.atakmap.android.cotselector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.comms.ReportingRate;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.ModifierApi;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.platform.symbology.SymbologyProvider;


@ModifierApi(since = "5.6", modifiers = {"implements OnTypeChangedListener2"}, target = "5.9")
public class CoTSelector extends DropDownReceiver
        implements CustomListView.OnTypeChangedListener,
        CustomListView.OnTypeChangedListener2 {

    public static final String TAG = "CoTSelector";

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public interface TypeChangedListener {
        void onTypeChanged(String type);
    }

    public interface TypeChangedListener2 {
        void onTypeChanged(String type, String milsym);
    }

    private PointMapItem m;
    private TypeChangedListener2 tl;
    private final CustomListView clistview;
    private final View csuitmain;

    public CoTSelector(final MapView mapView) {
        super(mapView);

        // inflate the layout
        LayoutInflater inflater = LayoutInflater.from(mapView.getContext());
        csuitmain = inflater.inflate(R.layout.csuitmain, null);
        LinearLayout mainLL = csuitmain
                .findViewById(R.id.MainLL);

        clistview = new CustomListView(getMapView().getContext());
        clistview.init(
                csuitmain,
                (CustomListView.OnTypeChangedListener2) this);
        clistview.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mainLL.addView(clistview);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
    }

    /**
     * Given a PointMapItem, show the CoT selector that is used to select a new
     * CoT type.
     * @param m the map item
     * @param tl the type change listener
     */
    public void show(final PointMapItem m,
                     final TypeChangedListener2 tl) {
        this.m = m;
        this.tl = tl;

        final String milsym = m.getMetaString("milsym",null);
        ISymbologyProvider provider = null;
        if(milsym != null) {
            provider = SymbologyProvider.getProviderFromSymbol(milsym);
        }
        if(provider == null) {
            provider = ATAKUtilities.getDefaultSymbologyProvider();
        }

        clistview.setProvider(provider);
        clistview.setType(m.getType(), milsym);

        showDropDown(csuitmain, FULL_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                HALF_HEIGHT);
    }
    /**
     * Given a PointMapItem, show the CoT selector that is used to select a new
     * CoT type.
     * @param m the map item
     * @param tl the type change listener
     * @deprecated
     */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public void show(final PointMapItem m,
            final TypeChangedListener tl) {

        show(m, (tl != null) ? new TypeChangedListener2() {
            @Override
            public void onTypeChanged(String type, String milsym) {
                tl.onTypeChanged(type);
            }
        } : null);
    }

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    synchronized static public void changeType(final PointMapItem m,
                                               final String type,
                                               final MapView mapView,
                                               final TypeChangedListener tl) {

        changeType(m, type, null, mapView, tl != null ?
                new TypeChangedListener2() {
                    @Override
                    public void onTypeChanged(String type, String milsym) {
                        tl.onTypeChanged(type);
                    }
                } : null
        );
    }

    synchronized static public void changeType(final PointMapItem m,
        final String type,
        final String milsym,
        final MapView mapView,
        final TypeChangedListener2 tl) {

        final AtakPreferences _prefs = AtakPreferences.getInstance(mapView
                .getContext());

        m.setType(type);
        m.setMetaString("milsym", milsym);

        //if marker already has color, lets update it based on CoT Type
        //if (m.hasMetaValue("color")) {
        //int color = Marker.getAffiliationColor(type);
        // if (color != m.getMetaInteger("color", 0)) {
        //color has changed
        // m.setMetaInteger("color", color);
        //}
        // }

        //
        //   FOR LEGACY - COPY OF THE BROKEN CoTInfoBroadcastReceiver::saveTargetData
        //
        final MapGroup source = m.getGroup();
        MapGroup parent = null;

        if (source != null) {
            parent = source.getParentGroup();
        }

        // if the group is not null, as in this item already exists in a group then

        if (parent != null) {
            for (MapGroup dest : parent.getChildGroups()) {
                String destGroupName = dest.getFriendlyName();
                if ((!FileSystemUtils.isEmpty(destGroupName))
                        &&
                        (type.charAt(2) == Character.toLowerCase(destGroupName
                                .charAt(0)))) {
                    if (dest != source) {
                        //Log.d(TAG, "marker: " + m.getUID() + " change group from: " + source + " to: " + dest);
                        //source.removeItem(m);
                        dest.addItem(m);

                        break;
                    }
                }
            }
        }

        // also persist the last selected CoT type
        _prefs.set("lastCoTTypeSet", type);
        _prefs.remove("lastIconsetPath");
        m.refresh(mapView.getMapEventDispatcher(), null, CoTSelector.class);
        m.persist(mapView.getMapEventDispatcher(), null, CoTSelector.class);
        if (tl != null) {
            tl.onTypeChanged(type, milsym);
        }

    }

    @Override
    public void notifyChanged(final String type) {
        notifyChanged(type, null);
    }

    @Override
    public void notifyChanged(final String type, String milsym) {
        changeType(m, type, milsym, getMapView(), tl);
        closeDropDown();
    }


    @Override
    public void disposeImpl() {
    }

    public static void displaySelfPicker() {
        final MapView mapView = MapView.getMapView();
        if (mapView == null)
            return;

        final AtakPreferences pref = AtakPreferences.getInstance(null);

        // inflate the layout
        final LayoutInflater inflater = LayoutInflater
                .from(mapView.getContext());
        View csuitmain = inflater.inflate(R.layout.csuitmain, null);
        LinearLayout mainLL = csuitmain
                .findViewById(R.id.MainLL);

        CustomListView clistview = new CustomListView(mapView.getContext());
        clistview.allowAffiliationChange(false);

        final AlertDialog.Builder builder = new AlertDialog.Builder(
                ATAKApplication.getCurrentActivity());

        LinearLayout ll = new LinearLayout(builder.getContext());
        ll.setMinimumHeight((int) (mapView.getHeight() * .95f));
        csuitmain.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        ll.addView(csuitmain);
        builder.setView(ll);
        final AlertDialog ad = builder.create();

        clistview.init(
                csuitmain,
                new CustomListView.OnTypeChangedListener2() {
                    @Override
                    public void notifyChanged(String type, String milsym) {
                        pref.set("locationUnitType", type);
                        mapView.getMapData().setMetaString("deviceType", type);
                        AtakBroadcast.getInstance().sendBroadcast(
                                new Intent(ReportingRate.REPORT_LOCATION)
                                        .putExtra("reason",
                                                "device type change "));
                        ad.dismiss();
                    }
                });
        clistview.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mainLL.addView(clistview);
        clistview.setType(pref.get("locationUnitType", "a-f-G-U-C"), null);

        ad.show();
        AlertDialogHelper.adjust(ad, .95, .95);
    }

}
