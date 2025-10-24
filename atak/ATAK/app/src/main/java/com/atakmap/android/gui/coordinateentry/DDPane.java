
package com.atakmap.android.gui.coordinateentry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import gov.tak.platform.lang.Parsers;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class DDPane extends AbstractPane {

    private final String TAG = "DDPane";

    private final EditText _ddLat, _ddLon;

    DDPane(Context ctx) {
        super(ctx, "dd_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_dd,
                        null));
        _ddLat = view.findViewById(R.id.coordDialogDDLatText);
        _ddLon = view.findViewById(R.id.coordDialogDDLonText);

        _ddLat.setSelectAllOnFocus(true);
        _ddLon.setSelectAllOnFocus(true);

        addOnChangeListener(_ddLat);
        addOnChangeListener(_ddLon);

        customCopy(new EditText[] {
                _ddLat, _ddLon
        });
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return CoordinateFormat.DD.getDisplayName();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {
        _originalPoint = currentPoint;

        final String[] dd;
        if (currentPoint != null) {
            dd = CoordinateFormatUtilities.formatToStrings(currentPoint.get(),
                    CoordinateFormat.DD);
        } else {
            dd = new String[] {
                    "", ""
            };
        }

        _originalRepresentation = dd;

        _ddLat.setText(dd[0]);
        _ddLon.setText(dd[1]);

        setEditable(_ddLon, editable);
        setEditable(_ddLat, editable);

    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {
            String[] coord = new String[] {
                    _ddLat.getText().toString(), _ddLon.getText().toString()
            };

            if (isEmptyCoordinates(coord)) {
                return null;
            }

            if (!compareCoordinates(coord, _originalRepresentation))
                return _originalPoint;
            return GeoPointMetaData.wrap(CoordinateFormatUtilities
                    .convert(coord, CoordinateFormat.DD));
        } catch (IllegalArgumentException e) {
            throw new CoordinateException(
                    context.getString(R.string.goto_input_tip3), e);
        }
    }

    @Override
    public String format(GeoPointMetaData gp) {
        if (gp == null)
            return null;

        try {
            final String[] retval = CoordinateFormatUtilities.formatToStrings(
                    gp.get(),
                    CoordinateFormat.DD);

            return String.format("%s%s, %s%S",
                    retval[0], "\u00B0",
                    retval[1], "\u00B0");
        } catch (IllegalArgumentException e) {
            return null;
        }

    }

    @Override
    public void autofill(GeoPointMetaData point) {
        if (point != null) {
            String[] c = CoordinateFormatUtilities.formatToStrings(
                    point.get(),
                    CoordinateFormat.DMS);
            _ddLon.setText(c[3]);
            _ddLon.setSelection(_ddLon.getText().length());

            _ddLat.setText(c[0]);
            _ddLat.setSelection(_ddLat.getText().length());
            showKeyboard();
            fireOnChange();
        }
    }

    protected boolean processPaste(@NonNull String data) {
        if (data.contains(" ")) {
            String[] s = data.split(" ");
            double lat = Parsers.parseDouble(s[0], Double.NaN);
            double lon = Parsers.parseDouble(s[1], Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                _ddLat.setText((Double.toString(lat)));
                _ddLon.setText((Double.toString(lon)));
                return true;
            }
        }
        return false;
    }
}
