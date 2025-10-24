
package com.atakmap.android.cotselector;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.comms.ReportingRate;

import gov.tak.api.cot.detail.Role;

public final class RoleSelector {

    public static final String TAG = "CoTSelector";

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

        CustomRoleListView clistview = new CustomRoleListView(
               ATAKApplication.getCurrentActivity());
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
                new CustomRoleListView.OnTypeChangedListener() {
                    @Override
                    public void notifyChanged(Role role) {
                        if (CotMapComponent.getInstance().getRolesManager()
                                .isDefault(role)) {
                            pref.set("atakRoleType", role.getName());
                            pref.remove("customRole");
                            pref.remove("customRoleAbbreviation");
                            pref.remove("customRoleUseTypeIcon");
                        } else {
                            pref.set("atakRoleType", role.getFallback());
                            pref.set("customRole", role.getName());
                            pref.set("customRoleAbbreviation",
                                    role.getAbbreviation());
                            pref.set("customRoleUseTypeIcon",
                                    role.useTypeIcon());
                        }
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

        ad.show();
        AlertDialogHelper.adjust(ad, .95, .95);
    }

}
