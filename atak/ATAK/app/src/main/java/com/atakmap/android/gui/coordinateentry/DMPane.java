
package com.atakmap.android.gui.coordinateentry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class DMPane extends AbstractPane {

    private final static String TAG = "DMPane";

    private final EditText _dmLatD, _dmLatM, _dmLonD, _dmLonM;

    DMPane(Context ctx) {
        super(ctx, "dm_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_dm,
                        null));
        _dmLatD = view.findViewById(R.id.coordDialogDMLatDegText);
        _dmLatM = view.findViewById(R.id.coordDialogDMLatMinText);
        _dmLonD = view.findViewById(R.id.coordDialogDMLonDegText);
        _dmLonM = view.findViewById(R.id.coordDialogDMLonMinText);

        _dmLatD.setSelectAllOnFocus(true);
        _dmLatM.setSelectAllOnFocus(true);
        _dmLonD.setSelectAllOnFocus(true);
        _dmLonM.setSelectAllOnFocus(true);

        addOnChangeListener(_dmLatD);
        addOnChangeListener(_dmLatM);
        addOnChangeListener(_dmLonD);
        addOnChangeListener(_dmLonM);
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return CoordinateFormat.DM.getDisplayName();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {
        _originalPoint = currentPoint;

        final String[] dm;
        if (currentPoint != null) {
            dm = CoordinateFormatUtilities.formatToStrings(currentPoint.get(),
                    CoordinateFormat.DM);
        } else {
            dm = new String[] {
                    "", "", "", ""
            };
        }

        _originalRepresentation = dm;

        _dmLatD.setText(dm[0]);
        _dmLatM.setText(dm[1]);
        _dmLonD.setText(dm[2]);
        _dmLonM.setText(dm[3]);

        setEditable(_dmLatD, editable);
        setEditable(_dmLatM, editable);
        setEditable(_dmLonD, editable);
        setEditable(_dmLonM, editable);

    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {
            String[] coord = new String[] {
                    _dmLatD.getText().toString(), _dmLatM.getText().toString(),
                    _dmLonD.getText().toString(), _dmLonM.getText().toString()
            };

            if (isEmptyCoordinates(coord))
                return null;

            if (!compareCoordinates(coord, _originalRepresentation))
                return _originalPoint;

            return GeoPointMetaData.wrap(CoordinateFormatUtilities
                    .convert(coord, CoordinateFormat.DM));
        } catch (IllegalArgumentException e) {
            throw new CoordinateException(
                    context.getString(R.string.goto_input_tip5), e);
        }
    }

    @Override
    public String format(GeoPointMetaData gp) {
        if (gp == null)
            return null;

        try {
            final String[] retval = CoordinateFormatUtilities.formatToStrings(
                    gp.get(),
                    CoordinateFormat.DM);

            return String.format("%s%s %s%s, %s%s %s%s",
                    retval[0], "\u00B0",
                    retval[1], "'",
                    retval[2], "\u00B0",
                    retval[3], "'");
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
            _dmLonD.setText(c[3]);
            _dmLatD.setText(c[0]);
            _dmLatM.setText("");
            _dmLonM.setText("");
            _dmLatM.requestFocus();
            showKeyboard();
            fireOnChange();
        }
    }
}
