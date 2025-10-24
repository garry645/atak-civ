
package com.atakmap.android.jumpbridge;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.atakmap.android.cotdetails.CoTInfoView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.CameraController;

import gov.tak.api.annotation.DeprecatedApi;

@Deprecated
@DeprecatedApi(since = "5.1", forRemoval = true, removeAt = "5.4")
public class JumpBridgeDropDownReceiver extends DropDownReceiver implements
        OnStateListener {
    private final View infoView;
    private final AtakPreferences _prefs;

    protected JumpBridgeDropDownReceiver(MapView mapView) {
        super(mapView);
        LayoutInflater inflater = LayoutInflater
                .from(getMapView().getContext());
        _prefs = AtakPreferences.getInstance(mapView
                .getContext());
        infoView = inflater.inflate(R.layout.jumpbridgeinfo, null);

    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null)
            return;

        if (intent.getAction().equals(JumpBridgeMapComponent.JM_WARNING)) {
            MapItem temp = MapView.getMapView().getRootGroup()
                    .deepFindItem("uid", intent.getStringExtra("targetUID"));
            if (!(temp instanceof Marker))
                return;
            final Marker targetPMI = (Marker) temp;
            if (intent.hasExtra("warning")) {
                infoView.findViewById(R.id.warningText).setVisibility(
                        View.VISIBLE);
                ((TextView) infoView.findViewById(R.id.warningText))
                        .setText(intent.getStringExtra("warning"));
            } else {
                infoView.findViewById(R.id.warningText)
                        .setVisibility(View.GONE);
                ((TextView) infoView.findViewById(R.id.warningText))
                        .setText(intent.getStringExtra(""));
            }

            setSelected(targetPMI, "asset:/icons/outline.png");
            ((ImageButton) infoView.findViewById(R.id.cotInfoNameTitle))
                    .setImageDrawable(CoTInfoView.getPointIcon(targetPMI,
                            getMapView()));
            ((TextView) infoView.findViewById(R.id.cotInfoNameTV))
                    .setText(targetPMI.getTitle());
            CoordinateFormat _cFormat = CoordinateFormat.find(_prefs.get(
                    "coord_display_pref",
                    context.getString(R.string.coord_display_pref_default)));
            final String p = CoordinateFormatUtilities.formatToString(
                    targetPMI.getPoint(), _cFormat);
            final String a = AltitudeUtilities.format(targetPMI.getPoint(),
                    _prefs.getSharedPrefs());
            final Button _coordButton = infoView
                    .findViewById(R.id.cotInfoCoordButton);
            _coordButton.setText(p + "\n" + a);
            infoView.findViewById(R.id.cotInfoPanButton)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            GeoPoint gp = targetPMI.getPoint();
                            CameraController.Programmatic.panTo(
                                    getMapView().getRenderer3(), gp, false);

                        }
                    });

            if (!isVisible()) {
                setRetain(true);
                showDropDown(infoView, THREE_EIGHTHS_WIDTH, FULL_HEIGHT,
                        FULL_WIDTH,
                        HALF_HEIGHT, this);
            }
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

}
