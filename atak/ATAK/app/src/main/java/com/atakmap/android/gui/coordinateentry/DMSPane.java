
package com.atakmap.android.gui.coordinateentry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class DMSPane extends AbstractPane {

    public static final String TAG = "DMSPane";

    private final EditText _dmsLatD, _dmsLatM, _dmsLatS, _dmsLonD, _dmsLonM,
            _dmsLonS;

    DMSPane(Context ctx) {
        super(ctx, "dms_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_dms,
                        null));
        _dmsLatD = view.findViewById(R.id.coordDialogDMSLatDegText);
        _dmsLatM = view.findViewById(R.id.coordDialogDMSLatMinText);
        _dmsLatS = view.findViewById(R.id.coordDialogDMSLatSecText);
        _dmsLonD = view.findViewById(R.id.coordDialogDMSLonDegText);
        _dmsLonM = view.findViewById(R.id.coordDialogDMSLonMinText);
        _dmsLonS = view.findViewById(R.id.coordDialogDMSLonSecText);

        _dmsLatD.setSelectAllOnFocus(true);
        _dmsLatM.setSelectAllOnFocus(true);
        _dmsLatS.setSelectAllOnFocus(true);

        _dmsLonD.setSelectAllOnFocus(true);
        _dmsLonM.setSelectAllOnFocus(true);
        _dmsLonS.setSelectAllOnFocus(true);

        addOnChangeListener(_dmsLatD);
        addOnChangeListener(_dmsLatM);
        addOnChangeListener(_dmsLatS);
        addOnChangeListener(_dmsLonD);
        addOnChangeListener(_dmsLonM);
        addOnChangeListener(_dmsLonS);
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return CoordinateFormat.DMS.getDisplayName();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {
        _originalPoint = currentPoint;

        final String[] dms;
        if (currentPoint != null) {
            dms = CoordinateFormatUtilities.formatToStrings(currentPoint.get(),
                    CoordinateFormat.DMS);
        } else {
            dms = new String[] {
                    "", "", "", "", "", ""
            };
        }

        _originalRepresentation = dms;

        _dmsLatD.setText(dms[0]);
        _dmsLatM.setText(dms[1]);
        _dmsLatS.setText(dms[2]);
        _dmsLonD.setText(dms[3]);
        _dmsLonM.setText(dms[4]);
        _dmsLonS.setText(dms[5]);

        setEditable(_dmsLatD, editable);
        setEditable(_dmsLatM, editable);
        setEditable(_dmsLatS, editable);
        setEditable(_dmsLonD, editable);
        setEditable(_dmsLonM, editable);
        setEditable(_dmsLonS, editable);

    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {
            String[] coord = new String[] {
                    _dmsLatD.getText().toString(),
                    _dmsLatM.getText().toString(),
                    _dmsLatS.getText().toString(),
                    _dmsLonD.getText().toString(),
                    _dmsLonM.getText().toString(),
                    _dmsLonS.getText().toString()
            };

            if (isEmptyCoordinates(coord))
                return null;

            if (!compareCoordinates(coord, _originalRepresentation))
                return _originalPoint;
            return GeoPointMetaData
                    .wrap(CoordinateFormatUtilities.convert(coord,
                            CoordinateFormat.DMS));
        } catch (IllegalArgumentException e) {
            throw new CoordinateException(
                    context.getString(R.string.goto_input_tip6), e);
        }
    }

    @Override
    public String format(GeoPointMetaData gp) {
        if (gp == null)
            return null;

        try {
            final String[] retval = CoordinateFormatUtilities.formatToStrings(
                    gp.get(),
                    CoordinateFormat.DMS);
            return String.format("%s%s %s%s %s%s, %s%s %s%s %s%s",
                    retval[0], "\u00B0",
                    retval[1], "'",
                    retval[2], "''",
                    retval[3], "\u00B0",
                    retval[4], "'",
                    retval[5], "''");
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
            _dmsLonD.setText(c[3]);
            _dmsLatD.setText(c[0]);
            _dmsLatM.setText("");
            _dmsLonM.setText("");
            _dmsLatS.setText("");
            _dmsLonS.setText("");
            _dmsLatM.requestFocus();
            showKeyboard();
            fireOnChange();
        }
    }
}
