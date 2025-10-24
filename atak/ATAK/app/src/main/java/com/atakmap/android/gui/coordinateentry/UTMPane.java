
package com.atakmap.android.gui.coordinateentry;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

public class UTMPane extends AbstractPane implements View.OnClickListener {

    private final static String TAG = "UTMPane";

    private static final int FORMATTED = 0;
    private static final int PARTIALLY_FORMATTED = 1;
    private static final int RAW = 2;

    private final EditText _utmZone, _utmEast, _utmNorth,
            _utmRawZone, _utmRawEastingNorthing, _utmRaw;

    private final AtakPreferences _prefs;

    private boolean watch = true; // only used to make sure setting the
    // utmRaw does not cycle back around

    UTMPane(Context ctx) {
        super(ctx, "utm_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_utm,
                        null));

        _prefs = AtakPreferences.getInstance(context);

        _utmZone = view.findViewById(R.id.coordDialogUTMZoneText);
        _utmEast = view.findViewById(R.id.coordDialogUTMEastingText);
        _utmNorth = view.findViewById(R.id.coordDialogUTMNorthingText);
        _utmRaw = view.findViewById(R.id.rawUTM);
        _utmRawZone = view.findViewById(R.id.coordDialogUTMZoneRawText);
        _utmRawEastingNorthing = view
                .findViewById(R.id.coordDialogUTMEastingNorthingRawText);

        _utmZone.setSelectAllOnFocus(true);
        _utmEast.setSelectAllOnFocus(true);
        _utmNorth.setSelectAllOnFocus(true);

        addOnChangeListener(_utmZone);
        addOnChangeListener(_utmEast);
        addOnChangeListener(_utmNorth);

        _utmRawZone.addTextChangedListener(rawUtmWatcher);
        _utmRawEastingNorthing.addTextChangedListener(rawUtmWatcher);

        _utmRawZone.setSelectAllOnFocus(true);
        _utmRawEastingNorthing.setSelectAllOnFocus(true);

        view.findViewById(R.id.swapUTM).setOnClickListener(this);
        _utmRaw.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    // see the sync method
                    if (!watch)
                        return;

                    GeoPoint gp = CoordinateFormatUtilities.convert(
                            s.toString(),
                            CoordinateFormat.UTM);
                    if (gp != null) {
                        sync(GeoPointMetaData.wrap(gp));
                    } else {
                        sync(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error occurred", e);
                    sync(null);
                }
            }
        });
        _utmRaw.setSelectAllOnFocus(true);
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return context.getString(R.string.utm);
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {
        _originalPoint = currentPoint;

        setMode(_prefs.get("coordview.formattedUTM", FORMATTED));

        final String[] utm;
        if (currentPoint != null)
            utm = CoordinateFormatUtilities.formatToStrings(currentPoint.get(),
                    CoordinateFormat.UTM);
        else
            utm = new String[] {
                    "", "", ""
            };

        _originalRepresentation = utm;

        _utmZone.setText(utm[0]);
        _utmEast.setText(utm[1]);
        _utmNorth.setText(utm[2]);

        setUpperCase(_utmZone);
        setUpperCase(_utmRawZone);
        setUpperCase(_utmRaw);

        _utmZone.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 3) {
                    _utmEast.requestFocus();
                    if (_utmEast.getText().length() > 0)
                        _utmEast.setSelection(0, _utmEast.getText()
                                .length());
                }
            }
        });

        _utmEast.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 6) {
                    _utmNorth.requestFocus();
                    if (_utmNorth.getText().length() > 0)
                        _utmNorth.setSelection(0, _utmNorth.getText()
                                .length());
                }
            }
        });

        setEditable(_utmZone, editable);
        setEditable(_utmEast, editable);
        setEditable(_utmNorth, editable);
        setEditable(_utmRawZone, editable);
        setEditable(_utmRawEastingNorthing, editable);
        setEditable(_utmRaw, editable);

        syncMode();

    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {
            String[] coord = new String[] {
                    _utmZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()),
                    _utmEast.getText().toString(),
                    _utmNorth.getText().toString()
            };

            if (isEmptyCoordinates(coord))
                return null;

            if (!compareCoordinates(coord, _originalRepresentation))
                return _originalPoint;
            return GeoPointMetaData
                    .wrap(CoordinateFormatUtilities.convert(coord,
                            CoordinateFormat.UTM));
        } catch (IllegalArgumentException e) {
            throw new CoordinateException(
                    context.getString(R.string.goto_input_tip2), e);
        }
    }

    @Override
    public String format(GeoPointMetaData gp) {
        if (gp == null)
            return null;
        try {
            final String[] retval = CoordinateFormatUtilities.formatToStrings(
                    gp.get(),
                    CoordinateFormat.UTM);

            return String.format("%s%s%s",
                    retval[0].toUpperCase(LocaleUtil.getCurrent()),
                    retval[1],
                    retval[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void autofill(GeoPointMetaData point) {
        if (point != null) {
            String[] c = CoordinateFormatUtilities.formatToStrings(
                    point.get(),
                    CoordinateFormat.UTM);
            _utmZone.setText(c[0]);
            _utmEast.setText("");
            _utmNorth.setText("");
            _utmEast.requestFocus();

            syncMode();
            showKeyboard();

            if (_utmEast.getVisibility() == View.VISIBLE)
                if (FileSystemUtils.isEmpty(_utmZone.getText().toString()))
                    _utmZone.requestFocus();
                else
                    _utmEast.requestFocus();
            else if (_utmRawEastingNorthing.getVisibility() == View.VISIBLE)
                if (FileSystemUtils.isEmpty(_utmRawZone.getText().toString()))
                    _utmRawZone.requestFocus();
                else
                    _utmRawEastingNorthing.requestFocus();
            else
                _utmRaw.requestFocus();

            fireOnChange();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.swapUTM) {
            setMode((_prefs.get("coordview.formattedUTM", FORMATTED) + 1) % 3);
        }
    }

    private void sync(GeoPointMetaData gp) {
        final String[] utm;
        if (gp != null)
            utm = CoordinateFormatUtilities.formatToStrings(gp.get(),
                    CoordinateFormat.UTM);
        else
            utm = new String[] {
                    "", "", "", ""
            };

        _utmZone.setText(utm[0]);
        _utmEast.setText(utm[1]);
        _utmNorth.setText(utm[2]);
    }

    private void syncMode() {

        try {
            watch = false;

            _utmRawZone.setText(_utmZone.getText().toString()
                    .toUpperCase(LocaleUtil.getCurrent()));

            _utmRawEastingNorthing.setText(_utmEast.getText().toString() +
                    _utmNorth.getText().toString());

            _utmRaw.setText(
                    _utmZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent())
                            +
                            _utmEast.getText().toString() +
                            _utmNorth.getText().toString());
            _utmRaw.setSelection(_utmRaw.getText().length());
            watch = true;
        } catch (Exception e) {
            Log.e(TAG, " caught exception ", e);
        }

    }

    private void setMode(final int val) {
        _prefs.set("coordview.formattedUTM", val);
        switch (val) {
            case PARTIALLY_FORMATTED:
                syncMode();
                _utmZone.setVisibility(View.GONE);
                _utmEast.setVisibility(View.GONE);
                _utmNorth.setVisibility(View.GONE);
                _utmRaw.setVisibility(View.GONE);
                _utmRawEastingNorthing.setVisibility(View.VISIBLE);
                _utmRawZone.setVisibility(View.VISIBLE);
                break;
            case RAW:
                syncMode();
                _utmZone.setVisibility(View.GONE);
                _utmEast.setVisibility(View.GONE);
                _utmNorth.setVisibility(View.GONE);
                _utmRaw.setVisibility(View.VISIBLE);
                _utmRawEastingNorthing.setVisibility(View.GONE);
                _utmRawZone.setVisibility(View.GONE);
                break;
            case FORMATTED:
            default:
                _utmZone.setVisibility(View.VISIBLE);
                _utmEast.setVisibility(View.VISIBLE);
                _utmNorth.setVisibility(View.VISIBLE);
                _utmRaw.setVisibility(View.GONE);
                _utmRawEastingNorthing.setVisibility(View.GONE);
                _utmRawZone.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Takes an existing edit text and makes sure that only capital letters may be used
     * @param editText the edit text
     */
    private void setUpperCase(final EditText editText) {
        final InputFilter[] editFilters = editText.getFilters();
        final InputFilter[] newFilters = new InputFilter[editFilters.length
                + 2];
        System.arraycopy(editFilters, 0, newFilters, 2, editFilters.length);
        newFilters[0] = new InputFilter.AllCaps();
        newFilters[1] = invalidFilter;
        editText.setFilters(newFilters);
    }

    private final InputFilter invalidFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            if (source == null)
                return null;
            final String s = source.toString();
            String filtered = s.replaceAll("[^0-9a-zA-Z ]+", "");
            return filtered;
        }
    };

    AfterTextChangedWatcher rawUtmWatcher = new AfterTextChangedWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            try {
                // see the sync method
                if (!watch)
                    return;

                final String gz = _utmRawZone.getText().toString();
                final String en = _utmRawEastingNorthing.getText().toString();
                if (gz.length() != 3)
                    return;

                _utmRawEastingNorthing.requestFocus();

                GeoPoint gp = CoordinateFormatUtilities.convert(
                        gz + en,
                        CoordinateFormat.UTM);

                if (gp != null) {
                    sync(GeoPointMetaData.wrap(gp));
                } else {
                    sync(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "error occurred", e);
                sync(null);
            }
        }

    };
}
