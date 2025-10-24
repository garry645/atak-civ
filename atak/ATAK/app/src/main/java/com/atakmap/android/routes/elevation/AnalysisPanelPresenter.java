
package com.atakmap.android.routes.elevation;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.atakmap.android.drawing.details.GenericDetailsView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.android.util.SimpleSeekBarChangeListener;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.routes.elevation.model.UnitConverter;
import com.atakmap.android.viewshed.ViewshedDropDownReceiver;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationData;

import java.text.DecimalFormat;

import gov.tak.platform.util.LimitingThread;

public class AnalysisPanelPresenter implements View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFERENCE_SHOW_VIEWSHED = "ElevationProfileShowViewshed";
    public static final String PREFERENCE_PROFILE_VIEWSHED_ALT = "ElevationProfileViewshedAltMeters";
    public static final String PREFERENCE_PROFILE_VIEWSHED_RADIUS = "ElevationProfileViewshedRadius";
    public static final String PREFERENCE_PROFILE_VIEWSHED_CIRCLE = "ElevationProfileViewshedCircle";
    public static final String PREFERENCE_PROFILE_VIEWSHED_OPACITY = "ElevationProfileViewshedOpacity";

    public static final String PREFERENCE_PROFILE_VIEWSHED_OPACITY_TOGETHER = "ElevationProfileViewshedOpacityTogether";

    public static final String PREFERENCE_PROFILE_VIEWSHED_OPACITY_UNSEEN = "ElevationProfileViewshedOpacityUnseen";
    public static final String PREFERENCE_PROFILE_VIEWSHED_OPACITY_SEEN = "ElevationProfileViewshedOpacitySeen";
    public static final String PREFERENCE_PROFILE_VIEWSHED_MODEL = "ElevationProfileViewshedModel";

    private AnalysisPanelView _analysisPanelView;
    private View _viewshedDetails;
    private CheckBox _showViewshedCB;
    protected CheckBox _showLineOfSightCB;
    private TextView altitudeUnitsTV;
    private EditText altitudeET;
    protected AtakPreferences prefs = null;
    private double _totalDistance = 0;
    private double _maxSlope = 0;
    private boolean _isOpen = true;
    private Drawable _arrowOpen;
    private Drawable _arrowClosed;
    private AtakPreferences _prefs;


    private int intensitySeen = 50;
    private int intensityUnseen = 50;
    private boolean adjustTogether = true;
    private int elevationModel = ElevationData.MODEL_TERRAIN;

    protected final LimitingThread intensityLT;

    // private int _totalDistanceUnit = Span.ENGLISH;
    private UnitConverter.FORMAT _maxSlopeUnit = UnitConverter.FORMAT.GRADE;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            @Nullable String key) {
        if (key == null)
            return;
        if (key.equals("alt_unit_pref")) {
            _analysisPanelView.post(new Runnable() {
                @Override
                public void run() {
                    altitudeUnitsTV.setText(getAltitudeUnitsTV() == Span.METER
                            ? R.string.meter_units
                            : R.string.ft);
                    double heightAboveMarker = prefs.get(
                            PREFERENCE_PROFILE_VIEWSHED_ALT, 2d);
                    if (getAltitudeUnitsTV() == Span.FOOT)
                        heightAboveMarker = heightAboveMarker
                                * ConversionFactors.METERS_TO_FEET;
                    DecimalFormat _two = LocaleUtil.getDecimalFormat("0.00");
                    altitudeET.setText(_two.format(heightAboveMarker));
                }
            });
        }

    }

    public AnalysisPanelPresenter() {

        intensityLT = new LimitingThread("viewshedopacity",
                new Runnable() {
                    @Override
                    public void run() {
                        //set the viewshed transparency
                        prefs.set(PREFERENCE_PROFILE_VIEWSHED_OPACITY_SEEN,
                                intensitySeen);
                        prefs.set(PREFERENCE_PROFILE_VIEWSHED_OPACITY_UNSEEN,
                                intensityUnseen);
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException ignored) {
                        }
                    }
                });
    }

    public void bind(AnalysisPanelView v, final MapView mapView) {
        _analysisPanelView = v;
        _prefs = AtakPreferences.getInstance(_analysisPanelView.getContext());
        _analysisPanelView.getTotalDistText().setOnClickListener(this);
        _analysisPanelView.getMaxSlopeText().setOnClickListener(this);
        _analysisPanelView.getToggleView().setOnClickListener(this);

        prefs = AtakPreferences.getInstance(mapView
                .getContext());
        prefs.registerListener(this);

        View viewshedView = _analysisPanelView.getViewshedView();
        if (viewshedView instanceof ViewGroup)
            GenericDetailsView.addEditTextPrompts((ViewGroup) viewshedView);
        _showViewshedCB = viewshedView.findViewById(
                R.id.showViewshed_cb);

        _showLineOfSightCB = viewshedView.findViewById(R.id.showLineOfSight_cb);
        _showLineOfSightCB.setChecked(
                prefs.get("elevation_chart_lineofsight_visible", false));
        _showLineOfSightCB.setOnClickListener(this);

        altitudeET = viewshedView
                .findViewById(R.id.altitude_et);

        double heightAboveMarker = prefs.get(
                PREFERENCE_PROFILE_VIEWSHED_ALT, 2d);
        if (getAltitudeUnitsTV() == Span.FOOT)
            heightAboveMarker = heightAboveMarker
                    * ConversionFactors.METERS_TO_FEET;

        altitudeUnitsTV = viewshedView
                .findViewById(R.id.altitude_units);
        altitudeUnitsTV.setText(
                getAltitudeUnitsTV() == Span.METER ? R.string.meter_units
                        : R.string.ft);

        DecimalFormat _two = LocaleUtil.getDecimalFormat("0.00");
        altitudeET.setText(_two.format(heightAboveMarker));
        altitudeET.addTextChangedListener(getAltitudeTextWatcher(altitudeET));

        final EditText radiusET = viewshedView
                .findViewById(R.id.radius_et);
        radiusET.setText(String.valueOf(prefs.get(
                PREFERENCE_PROFILE_VIEWSHED_RADIUS, 7000)));
        radiusET.addTextChangedListener(
                getRadiusTextWatcher(mapView, radiusET));

        CheckBox circleCB = viewshedView
                .findViewById(R.id.circularViewshed_cb);

        //boolean circlePref = prefs.getBoolean(
        //        PREFERENCE_PROFILE_VIEWSHED_CIRCLE, false);

        boolean circlePref = true; // hard code this always

        circleCB.setChecked(circlePref);
        circleCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                prefs.set(PREFERENCE_PROFILE_VIEWSHED_CIRCLE,
                        isChecked);
            }
        });

        intensitySeen = prefs.get(PREFERENCE_PROFILE_VIEWSHED_OPACITY_SEEN, 50);
        intensityUnseen = prefs.get(PREFERENCE_PROFILE_VIEWSHED_OPACITY_UNSEEN,
                50);
        adjustTogether = prefs.get(PREFERENCE_PROFILE_VIEWSHED_OPACITY_TOGETHER,
                true);
        elevationModel = prefs.get(PREFERENCE_PROFILE_VIEWSHED_MODEL,
                ElevationData.MODEL_TERRAIN);

        final Switch modelSwitch = viewshedView
                .findViewById(R.id.source_elevation);
        modelSwitch.setChecked(elevationModel == ElevationData.MODEL_SURFACE);
        modelSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                elevationModel = isChecked ? ElevationData.MODEL_SURFACE
                        : ElevationData.MODEL_TERRAIN;
                prefs.set(PREFERENCE_PROFILE_VIEWSHED_MODEL, elevationModel);
            }
        });
        final SeekBar transSeekSeen = viewshedView
                .findViewById(R.id.intensity_seek_seen);
        final SeekBar transSeekUnseen = viewshedView
                .findViewById(R.id.intensity_seek_unseen);
        final TextView primaryLabel = viewshedView
                .findViewById(R.id.intensity_primary_label);
        final TextView secondaryLabel = viewshedView
                .findViewById(R.id.intensity_secondary_label);

        CheckBox checkBox = viewshedView
                .findViewById(R.id.intensity_lock_sliders);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                primaryLabel
                        .setVisibility(isChecked ? View.GONE : View.VISIBLE);
                secondaryLabel
                        .setVisibility(isChecked ? View.GONE : View.VISIBLE);
                transSeekUnseen
                        .setVisibility(isChecked ? View.GONE : View.VISIBLE);
                adjustTogether = isChecked;
                transSeekUnseen.setProgress(transSeekSeen.getProgress());
                prefs.set(PREFERENCE_PROFILE_VIEWSHED_OPACITY_TOGETHER,
                        isChecked);
            }
        });
        checkBox.setChecked(adjustTogether);

        transSeekUnseen.setProgress(intensityUnseen);
        transSeekSeen.setProgress(intensitySeen);

        transSeekSeen
                .setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                            boolean fromUser) {
                        //there is a bug when the alpha is set to 100, so to avoid it set 99 as the max
                        if (progress == 100)
                            progress = 99;
                        intensitySeen = progress;
                        if (adjustTogether)
                            intensityUnseen = progress;
                        intensityLT.exec();
                    }
                });

        transSeekUnseen
                .setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                            boolean fromUser) {
                        //there is a bug when the alpha is set to 100, so to avoid it set 99 as the max
                        if (progress == 100)
                            progress = 99;
                        intensityUnseen = progress;
                        intensityLT.exec();
                    }
                });

        _viewshedDetails = viewshedView.findViewById(
                R.id.viewshedDetailsLayout);
        _showViewshedCB.setOnClickListener(this);
        if (prefs.get(PREFERENCE_SHOW_VIEWSHED, false)) {
            _showViewshedCB.setChecked(true);
            _viewshedDetails.setVisibility(View.VISIBLE);
        }
        Resources resources = _analysisPanelView.getResources();
        _arrowOpen = resources.getDrawable(R.drawable.arrowright);
        _arrowClosed = resources.getDrawable(R.drawable.arrowleft);

    }

    protected AfterTextChangedWatcher getRadiusTextWatcher(MapView mapView,
            EditText radiusET) {
        return new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (radiusET.getText().length() == 0)
                    return;
                int radius;
                try {
                    radius = Integer.parseInt(radiusET.getText().toString());
                } catch (Exception e) {
                    Toast.makeText(
                            mapView.getContext(),
                            mapView.getContext().getString(
                                    R.string.radius_num_warn),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (radius < 1
                        || radius > ViewshedDropDownReceiver.MAX_RADIUS) {
                    Toast.makeText(
                            mapView.getContext(),
                            mapView.getContext().getString(
                                    R.string.radius_1_to_40000),
                            Toast.LENGTH_SHORT).show();
                    s.clear();
                    if (radius > ViewshedDropDownReceiver.MAX_RADIUS) {
                        s.append(Integer
                                .toString(ViewshedDropDownReceiver.MAX_RADIUS));
                        prefs.set(
                                AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_RADIUS,
                                ViewshedDropDownReceiver.MAX_RADIUS);
                    }
                    return;
                } else {
                    prefs.set(
                            AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_RADIUS,
                            radius);
                }
            }
        };
    }

    protected AfterTextChangedWatcher getAltitudeTextWatcher(
            TextView altitudeET) {
        return new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (altitudeET.getText().length() > 0) {
                    double heightAboveGround;
                    try {
                        heightAboveGround = Double.parseDouble(altitudeET
                                .getText()
                                .toString());
                    } catch (Exception e) {
                        return;
                    }

                    if (getAltitudeUnitsTV() != Span.METER) {
                        heightAboveGround = heightAboveGround
                                * ConversionFactors.FEET_TO_METERS;
                    }

                    prefs.set(
                            AnalysisPanelPresenter.PREFERENCE_PROFILE_VIEWSHED_ALT,
                            heightAboveGround);

                }
            }
        };
    }

    private void _closeView() {
        synchronized (this) {
            _isOpen = false;
            _analysisPanelView.getToggleImage().setImageDrawable(_arrowClosed);
            _analysisPanelView.setVisibility(View.GONE);
        }
    }

    private synchronized void _openView() {
        synchronized (this) {
            _isOpen = true;
            _analysisPanelView.getToggleImage().setImageDrawable(_arrowOpen);
            _analysisPanelView.setVisibility(View.VISIBLE);
        }
    }

    public void updateTotalDist(final double feet) {
        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                int rangeFmt = Integer
                        .parseInt(_prefs.get("rab_rng_units_pref",
                                String.valueOf(Span.METRIC)));
                Span rangeUnits;
                switch (rangeFmt) {

                    case Span.ENGLISH:
                        rangeUnits = Span.MILE;
                        break;
                    case Span.NM:
                        rangeUnits = Span.NAUTICALMILE;
                        break;
                    case Span.METRIC:
                    default:
                        rangeUnits = Span.METER;
                        break;
                }
                _totalDistance = SpanUtilities.convert(feet, Span.FOOT,
                        rangeUnits);
                _analysisPanelView.getTotalDistText()
                        .setText(
                                SpanUtilities.formatType(rangeFmt,
                                        _totalDistance,
                                        rangeUnits));

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (_analysisPanelView == null)
            return;
        // Total distance units toggle
        if (v == _analysisPanelView.getTotalDistText()) {
            // if(_totalDistanceUnit == Span.ENGLISH) {
            // _totalDistanceUnit = Span.METRIC;
            // } else {
            // _totalDistanceUnit = Span.ENGLISH;
            // }
            // updateTotalDist(_totalDistance);
            // ((RouteElevationPresenter)
            // Presenter.getInstance(RouteElevationPresenter.class)).onRouteElevationChartClick(v);
        }

        // Max slope units toggle
        else if (v == _analysisPanelView.getMaxSlopeText()) {
            if (_maxSlopeUnit.equals(UnitConverter.FORMAT.GRADE)) {
                _maxSlopeUnit = UnitConverter.FORMAT.DEGREE;
            } else {
                _maxSlopeUnit = UnitConverter.FORMAT.GRADE;
            }
            updateMaxSlope(_maxSlope);
        }

        // Toggle right-side view
        else if (v == _analysisPanelView.getToggleView()) {
            if (_isOpen) {
                _closeView();
            } else {
                _openView();
            }
        }

        // Toggle viewshed
        else if (v == _showViewshedCB) {
            prefs.set(PREFERENCE_SHOW_VIEWSHED, _showViewshedCB.isChecked());

            if (_showViewshedCB.isChecked())
                _viewshedDetails.setVisibility(View.VISIBLE);
            else
                _viewshedDetails.setVisibility(View.GONE);
        } else if (v == _showLineOfSightCB) {
            prefs.set("elevation_chart_lineofsight_visible",
                    _showLineOfSightCB.isChecked());

        }
    }

    public void updateMaxAlt(final GeoPointMetaData alt) {
        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                _analysisPanelView.getMaxAltText().setText(
                        AltitudeUtilities.format(alt.get(),
                                prefs.getSharedPrefs()));
            }
        });
    }

    public void updateMinAlt(final GeoPointMetaData alt) {
        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                _analysisPanelView.getMinAltText().setText(
                        AltitudeUtilities.format(alt.get(),
                                prefs.getSharedPrefs()));
            }
        });
    }

    public void updateTotalGain(final double feet) {

        final int altFmt = Integer.parseInt(prefs.get("alt_unit_pref",
                String.valueOf(Span.ENGLISH)));

        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                double value;
                Span valueSpan;
                if (altFmt == Span.METRIC) {
                    valueSpan = Span.METER;
                    value = SpanUtilities.convert(feet, Span.FOOT, Span.METER);
                } else {
                    value = feet;
                    valueSpan = Span.FOOT;
                }
                _analysisPanelView.getTotalGainText().setText(
                        SpanUtilities.format(value, valueSpan, 0));
            }
        });
    }

    public void updateTotalLoss(final double feet) {
        final int altFmt = Integer.parseInt(prefs.get("alt_unit_pref",
                String.valueOf(Span.ENGLISH)));

        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                double value;
                Span valueSpan;
                if (altFmt == Span.METRIC) {
                    valueSpan = Span.METER;
                    value = SpanUtilities.convert(feet, Span.FOOT, Span.METER);
                } else {
                    value = feet;
                    valueSpan = Span.FOOT;
                }
                _analysisPanelView.getTotalLossText().setText(
                        SpanUtilities.format(value, valueSpan, 0));
            }
        });
    }

    public void updateMaxSlope(final double slope) {
        _maxSlope = slope;
        _analysisPanelView.post(new Runnable() {
            @Override
            public void run() {
                _analysisPanelView.getMaxSlopeText().setText(
                        UnitConverter.formatToString(slope,
                                UnitConverter.FORMAT.SLOPE,
                                _maxSlopeUnit));
            }
        });
    }

    private Span getAltitudeUnitsTV() {
        Span altUnits;
        switch (Integer
                .parseInt(prefs.get("alt_unit_pref", "0"))) {
            case 1:
                altUnits = Span.METER;
                break;
            case 0:
            default: // default to feet
                altUnits = Span.FOOT;
                break;
        }
        return altUnits;
    }
}
