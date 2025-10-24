
package com.atakmap.android.gui.coordinateentry;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.android.user.geocode.GeocodingUtil;
import com.atakmap.app.R;
import com.atakmap.app.preferences.GeocoderPreferenceFragment;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.List;

public class AddrPane extends AbstractPane implements View.OnClickListener {

    private final static String TAG = "AddrPane";

    private final EditText addressET; //Address Edit Text Field
    private final TextView _licenseTv;
    private final CheckBox _dropAddressChk;

    private String address = "";
    private String address_src = "";

    private final GeocodeManager.GeocoderChangedListener gcl;

    AddrPane(Context ctx) {
        super(ctx, "addr_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_addr,
                        null));
        addressET = view.findViewById(R.id.addressET);

        _licenseTv = view.findViewById(R.id.license);
        _dropAddressChk = view.findViewById(R.id.coordDialogAddress);

        view.findViewById(R.id.button_convert_address).setOnClickListener(this);

        view.findViewById(R.id.button_change_addresslookup)
                .setOnClickListener(this);
        GeocodeManager.getInstance(context).registerGeocoderChangeListener(
                gcl = new GeocodeManager.GeocoderChangedListener() {
                    @Override
                    public void onGeocoderChanged(
                            GeocodeManager.Geocoder geocoder) {
                        if (_licenseTv != null)
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                    _licenseTv
                                            .setText(getAddressLookupSource());
                                }
                            });

                    }
                });
        _licenseTv.setText(getAddressLookupSource());

    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return CoordinateFormat.ADDRESS.getDisplayName();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {
        _originalPoint = currentPoint;
        address = "";
        addressET.setText(address);

        setEditable(addressET, isGeocodingAvailable() && editable);
        setEditable(_dropAddressChk, isGeocodingAvailable() && editable);

        if (isGeocodingAvailable() && editable &&
                (_originalPoint != null || !FileSystemUtils.isEmpty(address))) {
            handleSearchButton();
        }
    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {

            if (_originalPoint != null) {
                _originalPoint.setMetaValue("address_text", address);
                _originalPoint.setMetaValue("address_geocoder", address_src);
                _originalPoint.setMetaValue("address_lookuptime",
                        new CoordinatedTime().toString());
                if (_dropAddressChk.isChecked())
                    _originalPoint.setMetaValue("address_usage_hint", "title");
            } else if (!FileSystemUtils.isEmpty(address))
                throw new IllegalArgumentException("empty coordinate");

            return _originalPoint;
        } catch (IllegalArgumentException e) {
            throw new CoordinateException(
                    context.getString(R.string.goto_input_tip4), e);
        }
    }

    @Override
    public String format(GeoPointMetaData gp) {
        if (gp != null) {
            return (String) gp.getMetaData("address_text");
        } else {
            return null;
        }
    }

    @Override
    public void autofill(GeoPointMetaData point) {
        address = null;
        addressET.setText("");
        _originalPoint = point;
        handleSearchButton();
        fireOnChange();
    }

    /*
     * Geocoding of the departure or destination address
     */
    public void handleSearchButton() {
        final String locationAddress = addressET.getText().toString();
        if (locationAddress.isEmpty()) {
            if (_originalPoint != null) {
                final ProgressDialog pd = ProgressDialog.show(context,
                        context.getString(R.string.goto_dialog1),
                        context.getString(R.string.goto_dialog2), true,
                        false);

                GeocodingUtil.lookup(
                        GeocodeManager.getInstance(context)
                                .getSelectedGeocoder(),
                        _originalPoint.get(), GeocodingUtil.NO_LIMIT,
                        new GeocodingUtil.ResultListener() {
                            @Override
                            public void onResult(GeocodeManager.Geocoder coder,
                                    String originalAddress,
                                    GeoPoint originalPoint,
                                    List<Pair<String, GeoPoint>> addresses,
                                    GeocodeManager.GeocoderException error) {
                                view.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!addresses.isEmpty()) {
                                            address = addresses.get(0).first;
                                            address_src = getAddressLookupSource();
                                            addressET.setText(address);
                                        } else {
                                            address = "";
                                            address_src = getAddressLookupSource();
                                            addressET.setText(address);
                                            Toast.makeText(context,
                                                    R.string.address_not_found,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        pd.dismiss();
                                    }
                                });
                            }
                        });
            } else {
                Toast.makeText(context,
                        context.getString(R.string.goto_input_tip9),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            final ProgressDialog pd = ProgressDialog.show(context,
                    context.getString(R.string.goto_dialog1),
                    locationAddress, true,
                    false);

            MapView view = MapView.getMapView();
            GeoBounds gb = view.getBounds();
            GeocodingUtil.lookup(
                    GeocodeManager.getInstance(context).getSelectedGeocoder(),
                    gb, locationAddress, GeocodingUtil.NO_LIMIT,
                    new GeocodingUtil.ResultListener() {
                        @Override
                        public void onResult(GeocodeManager.Geocoder coder,
                                String originalAddress, GeoPoint originalPoint,
                                List<Pair<String, GeoPoint>> addresses,
                                GeocodeManager.GeocoderException error) {
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!addresses.isEmpty()) {
                                        address = addresses.get(0).first;
                                        address_src = getAddressLookupSource();

                                        addressET.setText(address);
                                        if (addresses.get(0).second != null)
                                            _originalPoint = GeoPointMetaData
                                                    .wrap(addresses
                                                            .get(0).second);
                                    } else {
                                        Toast.makeText(context,
                                                R.string.address_not_found,
                                                Toast.LENGTH_LONG).show();
                                    }
                                    pd.dismiss();
                                }
                            });

                        }
                    });

        }
    }

    private boolean isGeocodingAvailable() {
        List<GeocodeManager.Geocoder> geocoderList = GeocodeManager
                .getInstance(context).getAllGeocoders();
        for (GeocodeManager.Geocoder geocoder : geocoderList) {
            if (geocoder instanceof GeocodeManager.Geocoder2)
                if (((GeocodeManager.Geocoder2) geocoder).isAvailable())
                    return true;
        }
        return false;
    }

    /**
     * Returns the address lookup source, or null if no address lookup source
     * is selected.
     */
    public String getAddressLookupSource() {
        GeocodeManager.Geocoder curr = GeocodeManager.getInstance(context)
                .getSelectedGeocoder();
        return curr.getTitle();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.button_change_addresslookup) {
            GeocoderPreferenceFragment.showPicker(context);
            // Find address
        } else if (id == R.id.button_convert_address) {
            if (isGeocodingAvailable()) {
                handleSearchButton();
            } else {
                Toast.makeText(
                        context,
                        context.getString(R.string.goto_input_tip8),
                        Toast.LENGTH_SHORT).show();

            }
            view.post(new Runnable() {
                @Override
                public void run() {
                    hideKeyboard();
                }
            });
        }
    }

    @Override
    public void dispose() {
        if (gcl != null)
            GeocodeManager.getInstance(context)
                    .unregisterGeocoderChangeListener(gcl);
    }
}
