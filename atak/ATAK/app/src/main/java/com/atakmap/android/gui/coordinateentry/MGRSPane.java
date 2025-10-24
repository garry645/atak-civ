
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

public class MGRSPane extends AbstractPane implements View.OnClickListener {

    private static final String TAG = "MGRSPane";

    private static final int FORMATTED = 0;
    private static final int PARTIALLY_FORMATTED = 1;
    private static final int RAW = 2;

    private final EditText _mgrsZone, _mgrsSquare, _mgrsEast, _mgrsNorth,
            _mgrsRawZoneSquare, _mgrsRawEastingNorthing, _mgrsRaw;
    private final AtakPreferences _prefs;

    private boolean watch = true; // only used to make surce setting the mgrsRaw does not cycle back around

    MGRSPane(Context ctx) {
        super(ctx, "mgrs_pane_id",
                LayoutInflater.from(ctx).inflate(R.layout.coordinate_pane_mgrs,
                        null));
        _prefs = AtakPreferences.getInstance(context);

        _mgrsZone = view.findViewById(R.id.coordDialogMGRSGridText);
        _mgrsSquare = view.findViewById(R.id.coordDialogMGRSSquareText);
        _mgrsEast = view.findViewById(R.id.coordDialogMGRSEastingText);
        _mgrsNorth = view.findViewById(R.id.coordDialogMGRSNorthingText);

        _mgrsZone.setSelectAllOnFocus(true);
        _mgrsSquare.setSelectAllOnFocus(true);
        _mgrsEast.setSelectAllOnFocus(true);
        _mgrsNorth.setSelectAllOnFocus(true);

        _mgrsZone.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 3) {
                    _mgrsSquare.requestFocus();
                    if (_mgrsSquare.getText().length() > 0)
                        _mgrsSquare.setSelection(0, _mgrsSquare.getText()
                                .length());
                }
            }
        });
        _mgrsSquare.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 2) {
                    _mgrsEast.requestFocus();
                    if (_mgrsEast.getText().length() > 0)
                        _mgrsEast.setSelection(0, _mgrsEast.getText().length());
                }
            }
        });

        _mgrsEast.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 5) {
                    _mgrsNorth.requestFocus();
                    if (_mgrsNorth.getText().length() > 0)
                        _mgrsNorth.setSelection(0, _mgrsNorth.getText()
                                .length());
                }
            }
        });

        addOnChangeListener(_mgrsZone);
        addOnChangeListener(_mgrsSquare);
        addOnChangeListener(_mgrsEast);
        addOnChangeListener(_mgrsNorth);

        view.findViewById(R.id.swap).setOnClickListener(this);

        _mgrsRawZoneSquare = view
                .findViewById(R.id.coordDialogMGRSZoneSquareText);
        _mgrsRawEastingNorthing = view
                .findViewById(R.id.coordDialogMGRSEastingNorthingText);

        _mgrsRawZoneSquare.addTextChangedListener(rawMgrsWatcher);
        _mgrsRawEastingNorthing.addTextChangedListener(rawMgrsWatcher);

        _mgrsRawZoneSquare.setSelectAllOnFocus(true);
        _mgrsRawEastingNorthing.setSelectAllOnFocus(true);

        _mgrsRaw = view.findViewById(R.id.rawMGRS);

        _mgrsRaw.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    // see the sync method
                    if (!watch)
                        return;

                    GeoPoint gp = CoordinateFormatUtilities.convert(
                            s.toString(),
                            CoordinateFormat.MGRS);
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

        _mgrsRaw.setSelectAllOnFocus(true);

    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return CoordinateFormat.MGRS.getDisplayName();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onActivate(GeoPointMetaData currentPoint, boolean editable) {

        _originalPoint = currentPoint;

        setMode(_prefs.get("coordview.formattedMGRS", FORMATTED));

        final String[] mgrs;
        if (currentPoint != null)
            mgrs = CoordinateFormatUtilities.formatToStrings(currentPoint.get(),
                    CoordinateFormat.MGRS);
        else
            mgrs = new String[] {
                    "", "", "", ""
            };

        _originalRepresentation = mgrs;

        _mgrsZone.setText(mgrs[0]);
        _mgrsSquare.setText(mgrs[1]);
        _mgrsEast.setText(mgrs[2]);
        _mgrsNorth.setText(mgrs[3]);

        setUpperCase(_mgrsZone);
        setUpperCase(_mgrsSquare);
        setUpperCase(_mgrsRawZoneSquare);
        setUpperCase(_mgrsRaw);

        setEditable(_mgrsRaw, editable);
        setEditable(_mgrsNorth, editable);
        setEditable(_mgrsEast, editable);
        setEditable(_mgrsSquare, editable);
        setEditable(_mgrsRawZoneSquare, editable);
        setEditable(_mgrsRawEastingNorthing, editable);
        setEditable(_mgrsZone, editable);

        syncMode();

    }

    @Override
    public GeoPointMetaData getGeoPointMetaData() throws CoordinateException {
        try {
            String[] coord = new String[] {
                    _mgrsZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()),
                    _mgrsSquare.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()),
                    _mgrsEast.getText().toString(),
                    _mgrsNorth.getText().toString()
            };

            if (isEmptyCoordinates(coord))
                return null;

            if (!compareCoordinates(coord, _originalRepresentation))
                return _originalPoint;

            return GeoPointMetaData
                    .wrap(CoordinateFormatUtilities.convert(coord,
                            CoordinateFormat.MGRS));
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
                    CoordinateFormat.MGRS);

            return String.format(
                    "%s%s%s%s",
                    retval[0].toUpperCase(LocaleUtil.getCurrent()),
                    retval[1].toUpperCase(LocaleUtil.getCurrent()),
                    retval[2],
                    retval[3]);
        } catch (IllegalArgumentException e) {
            return null;
        }

    }

    @Override
    public void autofill(GeoPointMetaData point) {
        if (point != null) {
            String[] c = CoordinateFormatUtilities.formatToStrings(
                    point.get(),
                    CoordinateFormat.MGRS);
            _mgrsZone.setText(c[0]);
            _mgrsSquare.setText(c[1]);
            _mgrsEast.setText("");
            _mgrsNorth.setText("");
            syncMode();
            showKeyboard();

            if (_mgrsEast.getVisibility() == View.VISIBLE) {
                if (FileSystemUtils.isEmpty(_mgrsZone.getText().toString()))
                    _mgrsZone.requestFocus();
                else
                    _mgrsEast.requestFocus();

            } else if (_mgrsRawEastingNorthing
                    .getVisibility() == View.VISIBLE) {
                if (FileSystemUtils
                        .isEmpty(_mgrsRawZoneSquare.getText().toString()))
                    _mgrsRawZoneSquare.requestFocus();
                else
                    _mgrsRawEastingNorthing.requestFocus();
            } else {
                _mgrsRaw.requestFocus();
            }

            fireOnChange();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.swap) {
            setMode((_prefs.get("coordview.formattedMGRS", FORMATTED) + 1) % 3);
        }
    }

    private void sync(GeoPointMetaData gp) {
        final String[] mgrs;
        if (gp != null)
            mgrs = CoordinateFormatUtilities.formatToStrings(gp.get(),
                    CoordinateFormat.MGRS);
        else
            mgrs = new String[] {
                    "", "", "", ""
            };

        _mgrsZone.setText(mgrs[0]);
        _mgrsSquare.setText(mgrs[1]);
        _mgrsEast.setText(mgrs[2]);
        _mgrsNorth.setText(mgrs[3]);
    }

    private void syncMode() {
        try {
            watch = false;
            _mgrsRawZoneSquare.setText(_mgrsZone.getText().toString()
                    .toUpperCase(LocaleUtil.getCurrent())
                    +
                    _mgrsSquare.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()));

            _mgrsRawEastingNorthing.setText(_mgrsEast.getText().toString() +
                    _mgrsNorth.getText().toString());

            _mgrsRaw.setText(
                    _mgrsZone.getText().toString()
                            .toUpperCase(LocaleUtil.getCurrent()) +
                            _mgrsSquare.getText().toString()
                                    .toUpperCase(LocaleUtil.getCurrent())
                            +
                            _mgrsEast.getText().toString() +
                            _mgrsNorth.getText().toString());

            if (_mgrsRaw.getVisibility() == View.VISIBLE)
                _mgrsRaw.setSelection(_mgrsRaw.getText().length());

            watch = true;
        } catch (Exception e) {
            Log.e(TAG, " caught exception ", e);
        }
    }

    private void setMode(final int val) {
        _prefs.set("coordview.formattedMGRS", val);
        switch (val) {
            case PARTIALLY_FORMATTED:
                syncMode();
                _mgrsZone.setVisibility(View.GONE);
                _mgrsSquare.setVisibility(View.GONE);
                _mgrsEast.setVisibility(View.GONE);
                _mgrsNorth.setVisibility(View.GONE);
                _mgrsRaw.setVisibility(View.GONE);
                _mgrsRawEastingNorthing.setVisibility(View.VISIBLE);
                _mgrsRawZoneSquare.setVisibility(View.VISIBLE);
                break;
            case RAW:
                syncMode();
                _mgrsZone.setVisibility(View.GONE);
                _mgrsSquare.setVisibility(View.GONE);
                _mgrsEast.setVisibility(View.GONE);
                _mgrsNorth.setVisibility(View.GONE);
                _mgrsRaw.setVisibility(View.VISIBLE);
                _mgrsRawEastingNorthing.setVisibility(View.GONE);
                _mgrsRawZoneSquare.setVisibility(View.GONE);
                break;
            case FORMATTED:
            default:
                _mgrsZone.setVisibility(View.VISIBLE);
                _mgrsSquare.setVisibility(View.VISIBLE);
                _mgrsEast.setVisibility(View.VISIBLE);
                _mgrsNorth.setVisibility(View.VISIBLE);
                _mgrsRaw.setVisibility(View.GONE);
                _mgrsRawEastingNorthing.setVisibility(View.GONE);
                _mgrsRawZoneSquare.setVisibility(View.GONE);
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

    AfterTextChangedWatcher rawMgrsWatcher = new AfterTextChangedWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            try {
                // see the sync method
                if (!watch)
                    return;

                final String gz = _mgrsRawZoneSquare.getText().toString();
                final String en = _mgrsRawEastingNorthing.getText().toString();
                if (gz.length() != 5)
                    return;

                _mgrsRawEastingNorthing.requestFocus();

                GeoPoint gp = CoordinateFormatUtilities.convert(
                        gz + en,
                        CoordinateFormat.MGRS);

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
