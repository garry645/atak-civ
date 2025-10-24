
package com.atakmap.android.cotdetails.sensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.cot.detail.SensorDetailHandler;
import com.atakmap.android.drawing.details.GenericDetailsView;
import com.atakmap.android.drawing.details.GenericPointDetailsView;
import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.gui.ColorPalette.OnColorSelectedListener;
import com.atakmap.android.gui.UrlUiHandler;
import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.hashtags.view.RemarksLayout;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.SensorFOV;
import com.atakmap.android.maps.SensorFOV.OnMetricsChangedListener;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.android.util.SimpleSeekBarChangeListener;
import com.atakmap.android.video.AddEditAlias;
import com.atakmap.android.video.AddEditAlias.AliasModifiedListener;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.VideoListDialog;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.CameraController;
import com.atakmap.math.MathUtils;

import java.util.List;

public class SensorDetailsView extends GenericPointDetailsView implements
        OnMetricsChangedListener, Shape.OnFillColorChangedListener,
        Shape.OnStrokeColorChangedListener, Shape.OnStrokeWeightChangedListener,
        AliasModifiedListener, View.OnClickListener {

    public static final String TAG = "SensorDetailsView";
    public static final int MAX_SENSOR_RANGE = 60000;

    private static final String TRUE_NORTH = Angle.DEGREE_SYMBOL + "TN";
    private static final String MAGNETIC_AZIMUTH = Angle.DEGREE_SYMBOL + "MZ";
    private static final String[] DIRECTION_AZIMUTH_OPTIONS = new String[] {
            TRUE_NORTH, MAGNETIC_AZIMUTH
    };

    private String sensorUID;

    private int currentAzimuth = 0;
    private ImageButton _sendButton;
    private MapView _mapView;

    private MapItem sensorItem;
    private Marker sensorMarker;
    private SensorFOV sensorFOV;

    private boolean isInitalized = false;
    private EditText nameET;
    private Button sensorVideoUrlBtn;
    private EditText rangeET, rangeLinesET;
    private Spinner directionAzimuthSpin;
    private EditText directionET;
    private EditText fovET;
    private SeekBar rangeSeek, rangeLineSeek;
    private SeekBar directionSeek;
    private SeekBar fovSeek;
    protected UrlUiHandler urlUiHandler;
    private RemarksLayout remarksLayout;
    private CheckBox fovVisibleCB;
    private CheckBox anglesVisibleCB;
    private ImageButton _strokeColorBtn;
    private SeekBar _strokeWeightSeek;
    private ImageButton _rangeLineStrokeColorBtn;
    private SeekBar _rangeLineStrokeWeightSeek;
    private ImageButton _fillColorBtn;
    private SeekBar _fillAlphaSeek;
    private Button _coordButton;
    private View _rangeLinesLayout;
    private TextView _rangeLineMax;

    private boolean ignoreRangeUpdate = false;
    private boolean ignoreRangeLinesUpdate = false;

    /**
     * *************************** CONSTRUCTOR ***************************
     */

    public SensorDetailsView(Context context) {
        super(context);
        if (!isInEditMode())
            _mapView = MapView.getMapView();
    }

    public SensorDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            _mapView = MapView.getMapView();
    }

    @Override
    protected void _init() {
        GenericDetailsView.addEditTextPrompts(this);
        sensorMarker = (Marker) sensorItem;

        _addressLayout = this.findViewById(R.id.dgAddressLayout);
        _addressText = this.findViewById(R.id.dgInfoAddress);
        _addressInfoText = this.findViewById(R.id.dgInfoAddressInfo);

        nameET = this.findViewById(R.id.sensorNameET);
        nameET.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //Apply new callsign
                sensorItem.setMetaString("callsign", s.toString());
                sensorMarker.setTitle(s.toString());
                sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                        this.getClass());
            }
        });

        sensorVideoUrlBtn = this.findViewById(R.id.sensorVideoUrlBtn);
        sensorVideoUrlBtn.setOnClickListener(this);

        findViewById(R.id.videoAliasButton).setOnClickListener(this);

        fovVisibleCB = this.findViewById(R.id.fovVisibilityCB);
        fovVisibleCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {

                sensorItem.toggleMetaData(SensorDetailHandler.HIDE_FOV,
                        !isChecked);
                if (!isChecked) {
                    anglesVisibleCB.setChecked(false);
                    sensorItem.toggleMetaData(
                            SensorDetailHandler.DISPLAY_LABELS, false);
                    sensorFOV.setMetrics(sensorFOV.getAzimuth(),
                            sensorFOV.getFOV(),
                            sensorFOV.getExtent(), false,
                            sensorFOV.getRangeLines());
                }
                sensorFOV.setVisible(isChecked);
                sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                        this.getClass());
                _rangeLinesLayout
                        .setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        anglesVisibleCB = this.findViewById(R.id.labelVisibilityCB);
        anglesVisibleCB.setEnabled(sensorItem.getVisible());
        anglesVisibleCB
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        Log.d(TAG, "anglesVisibleCB: " + isChecked);
                        sensorItem.toggleMetaData(
                                SensorDetailHandler.DISPLAY_LABELS, isChecked);
                        sensorFOV.setMetrics(sensorFOV.getAzimuth(),
                                sensorFOV.getFOV(),
                                sensorFOV.getExtent(), isChecked,
                                sensorFOV.getRangeLines());
                        sensorItem.persist(_mapView.getMapEventDispatcher(),
                                null,
                                this.getClass());
                        rangeLineSeek.setEnabled(isChecked);
                    }
                });

        _rangeLineMax = this.findViewById(R.id.rangeLineMax);
        _rangeLinesLayout = this.findViewById(R.id.rangeLinesLayout);

        //Range extent
        rangeET = this.findViewById(R.id.sensorRangeET);
        rangeET.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int currentSel = rangeET.getSelectionEnd();
                if (s.length() <= 0)
                    return;

                int val;
                try {
                    val = (int) Math.round(Double.parseDouble(s.toString()));
                } catch (NumberFormatException nfe) {
                    try {
                        val = Integer.parseInt(s.toString());
                    } catch (NumberFormatException nfe2) {
                        val = sensorItem.getMetaInteger(
                                SensorDetailHandler.RANGE_ATTRIBUTE, 0);
                        String s2 = String.valueOf(val);
                        rangeET.setText(s2);
                        rangeET.setSelection(s2.length());
                        return;
                    }
                }
                if (val > MAX_SENSOR_RANGE) {
                    String s2 = String.valueOf(MAX_SENSOR_RANGE);
                    rangeET.setText(s2);
                    rangeET.setSelection(s2.length());
                    return;
                }

                //Apply to new range to FOV
                if (sensorFOV != null && (int) sensorFOV.getExtent() != val) {
                    sensorFOV.setMetrics(sensorFOV.getAzimuth(),
                            sensorFOV.getFOV(), val, sensorFOV.isShowLabels(),
                            sensorFOV.getRangeLines());
                    sensorItem.setMetaInteger(
                            SensorDetailHandler.RANGE_ATTRIBUTE, val);
                    sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                            this.getClass());
                }

                ignoreRangeUpdate = true;
                rangeSeek.setProgress(toSeekBarScale(val));
                ignoreRangeUpdate = false;

                try {
                    rangeET.setSelection(Math.min(rangeET.getText().length(),
                            currentSel));
                    rangeET.setSelection(s.length());
                } catch (IndexOutOfBoundsException ioobe) {
                    Log.d(TAG, "error occurred setting the selection");
                }
            }
        });

        rangeSeek = this.findViewById(R.id.rangeSeek);
        ((TextView) findViewById(R.id.rangeMax))
                .setText(Integer.toString(MAX_SENSOR_RANGE));
        rangeSeek.setMax(100);
        rangeSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!ignoreRangeUpdate) {
                    rangeET.setText(Integer
                            .toString(fromSeekBarScale(seekBar.getProgress())));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                rangeET.requestFocus();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                //if the seek bar was changed by the user, update the range
                if (!ignoreRangeUpdate)
                    //exponentially grow the range so that there is finer adjustment
                    //on the low end and more coarse adjustment on the high end
                    rangeET.setText(Integer
                            .toString(fromSeekBarScale(seekBar.getProgress())));

            }
        });

        //Range Lines
        rangeLinesET = this.findViewById(R.id.rangeLinesET);
        rangeLinesET.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0)
                    return;

                int val = Integer.parseInt(s.toString());

                // lower bounds for human entry
                if (val < 25) {
                    rangeLinesET.setText("25");
                    return;
                }

                //Apply to new range to FOV
                if (sensorFOV != null
                        && (int) sensorFOV.getRangeLines() != val) {
                    sensorFOV.setMetrics(sensorFOV.getAzimuth(),
                            sensorFOV.getFOV(), sensorFOV.getExtent(),
                            sensorFOV.isShowLabels(), val);
                    sensorItem.setMetaInteger(
                            SensorDetailHandler.RANGE_LINES_ATTRIBUTE, val);
                    sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                            this.getClass());
                }

                int estimatedProg = Math.round(val);
                int currentProg = rangeLineSeek.getProgress();
                if (estimatedProg != currentProg) {
                    ignoreRangeLinesUpdate = true;
                    rangeLineSeek.setProgress(Math.round(val));
                    ignoreRangeLinesUpdate = false;
                }
            }
        });
        rangeLineSeek = this.findViewById(R.id.rangeLineSeek);
        rangeLineSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!ignoreRangeLinesUpdate) {
                    int currentProg = rangeLineSeek.getProgress();
                    if (currentProg < 25)
                        currentProg = 25;
                    rangeLinesET.setText(String.valueOf(currentProg));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                rangeLinesET.requestFocus();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                //if the seek bar was changed by the user, update the range 
                if (!ignoreRangeLinesUpdate && fromUser)
                    //exponentially grow the range so that there is finer adjustment
                    //on the low end and more coarse adjustment on the high end
                    rangeLinesET.setText(String.valueOf(progress));
            }
        });

        directionAzimuthSpin = this
                .findViewById(R.id.directionAzimuthSpinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(
                _mapView.getContext(),
                R.layout.spinner_text_view_dark, DIRECTION_AZIMUTH_OPTIONS);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        directionAzimuthSpin.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {

                        final String ref = DIRECTION_AZIMUTH_OPTIONS[arg2];
                        directionET.setContentDescription(getContext()
                                .getString(R.string.direction_units, ref));
                        if (currentAzimuth == arg2)
                            return;
                        currentAzimuth = arg2;
                        if (ref.equals(TRUE_NORTH)) {
                            directionET.setText(String
                                    .valueOf((int) sensorFOV.getAzimuth()));
                        } else if (ref.equals(MAGNETIC_AZIMUTH)) {
                            float trueAzimuth = sensorFOV.getAzimuth();
                            double magAzimuth = ATAKUtilities
                                    .convertFromTrueToMagnetic(
                                            sensorMarker.getPoint(),
                                            trueAzimuth);
                            directionET.setText(String.valueOf((int) Math
                                    .round(magAzimuth)));
                        }

                        //save azimuth preference to the marker
                        sensorItem.setMetaInteger(
                                SensorDetailHandler.MAG_REF_ATTRIBUTE,
                                currentAzimuth);
                        sensorItem.persist(_mapView.getMapEventDispatcher(),
                                null, this.getClass());
                    }
                });

        directionAzimuthSpin.setAdapter(adapter);
        directionET = this.findViewById(R.id.directionET);
        directionSeek = this.findViewById(R.id.directionSeek);
        directionSeek.setMax(359);
        directionET.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0)
                    return;
                int val = Integer.parseInt(s.toString());
                int rounded = AngleUtilities.roundDeg(val);
                if (val != rounded) {
                    final String displayString = String.valueOf(rounded);
                    directionET.setText(displayString);
                    directionET.setSelection(displayString.length());
                    return;
                }
                val = rounded;
                int trueVal = val % 360;
                if (DIRECTION_AZIMUTH_OPTIONS[currentAzimuth]
                        .equals(MAGNETIC_AZIMUTH)) {
                    //convert back to true
                    trueVal = (int) Math.round(ATAKUtilities
                            .convertFromMagneticToTrue(sensorMarker.getPoint(),
                                    val));
                }

                //Apply to new direction to FOV
                if (sensorFOV != null
                        && (int) sensorFOV.getAzimuth() != trueVal) {
                    sensorFOV.setMetrics(trueVal, sensorFOV.getFOV(),
                            sensorFOV.getExtent(), sensorFOV.isShowLabels(),
                            sensorFOV.getRangeLines());
                    sensorItem.setMetaInteger(
                            SensorDetailHandler.AZIMUTH_ATTRIBUTE, trueVal);
                    sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                            this.getClass());
                }

                //if seekbar progress does not match angle, update seekbar
                if (directionSeek.getProgress() != val - 1)
                    directionSeek.setProgress(val - 1);
                directionET.setSelection(s.length());
            }
        });

        directionSeek
                .setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                            boolean fromUser) {
                        directionET.setText(
                                String.valueOf(
                                        directionSeek.getProgress() + 1));
                    }
                });

        fovET = this.findViewById(R.id.fovET);
        fovSeek = this.findViewById(R.id.fovSeek);
        fovSeek.setMax(359);
        fovET.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0)
                    return;
                int val = Integer.parseInt(s.toString());
                if (val >= 360) {
                    fovET.setText(R.string.threefiftynine);
                    fovET.setSelection(fovET.length());
                    return;
                }

                //Apply to new direction to FOV
                if (sensorFOV != null && (int) sensorFOV.getFOV() != val) {
                    sensorFOV.setMetrics(sensorFOV.getAzimuth(), val,
                            sensorFOV.getExtent(), sensorFOV.isShowLabels(),
                            sensorFOV.getRangeLines());
                    sensorItem.setMetaInteger(
                            SensorDetailHandler.FOV_ATTRIBUTE, val);
                    sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                            this.getClass());
                }

                //if seekbar progress does not match angle, update seekbar
                if (fovSeek.getProgress() != val)
                    fovSeek.setProgress(val);

                try {
                    fovET.setSelection(s.length());
                } catch (Exception e) {
                    // ATAK-19909 Playstore Crash: java.lang.IndexOutOfBoundsException SensorDetailView
                    // Exception java.lang.IndexOutOfBoundsException: setSpan (2 ... 2) ends beyond length 1
                }
            }
        });
        fovSeek.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                fovET.setText(String.valueOf(fovSeek.getProgress()));
            }
        });

        urlUiHandler = new UrlUiHandler(this);

        remarksLayout = this.findViewById(R.id.remarksLayout);
        remarksLayout.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "sensor change: " + s);
                //Apply new callsign
                sensorItem.setMetaString("remarks", s.toString());
                sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                        this.getClass());
            }
        });

        _coordButton = this
                .findViewById(R.id.sensorCenterButton);
        _point = sensorMarker;

        _coordButton.setText(_unitPrefs.formatPoint(
                _point.getGeoPointMetaData(), true));
        _coordButton.setOnClickListener(this);

        formatAddress();

        _sendButton = this.findViewById(R.id.sendButton);
        _sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent contactList = new Intent();
                contactList.setAction(ContactPresenceDropdown.SEND_LIST);
                contactList.putExtra("targetUID", sensorUID);

                AtakBroadcast.getInstance().sendBroadcast(contactList);
            }
        });

        _fillAlphaSeek = this.findViewById(R.id.fillAlphaSeek);
        _fillAlphaSeek
                .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        sensorItem.persist(_mapView.getMapEventDispatcher(),
                                null,
                                this.getClass());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        clearFocus();
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                            boolean fromUser) {
                        double alpha = progress / 255d;
                        sensorItem.setMetaDouble(SensorDetailHandler.FOV_ALPHA,
                                alpha);
                        sensorFOV.setAlpha((float) alpha);
                    }
                });

        _strokeWeightSeek = findViewById(R.id.strokeWeightSeek);
        _strokeWeightSeek
                .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        sensorItem.persist(_mapView.getMapEventDispatcher(),
                                null,
                                this.getClass());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        clearFocus();
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int prog,
                            boolean fromUser) {
                        double weight = prog / 20d;
                        sensorItem.setMetaDouble(
                                SensorDetailHandler.STROKE_WEIGHT,
                                weight);
                        sensorFOV.setStrokeWeight(weight);
                    }
                });

        _strokeColorBtn = findViewById(R.id.strokeColorBtn);
        _strokeColorBtn.setOnClickListener(this);

        _rangeLineStrokeWeightSeek = findViewById(
                R.id.rangeLineStrokeWeightSeek);
        _rangeLineStrokeWeightSeek
                .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        sensorItem.persist(_mapView.getMapEventDispatcher(),
                                null,
                                this.getClass());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        clearFocus();
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int prog,
                            boolean fromUser) {
                        double weight = prog / 20d;
                        sensorItem.setMetaDouble(
                                SensorDetailHandler.RANGE_LINES_STROKE_WEIGHT,
                                weight);
                        sensorFOV.setRangeLineStrokeWeight(weight);
                    }
                });

        _rangeLineStrokeColorBtn = findViewById(R.id.rangeLineStrokeColorBtn);
        _rangeLineStrokeColorBtn.setOnClickListener(this);

        _fillColorBtn = findViewById(R.id.fillColorBtn);
        _fillColorBtn.setOnClickListener(this);

        _updateColorButtonDrawable();

        findViewById(R.id.placeEndPointButton).setOnClickListener(this);

        isInitalized = true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Edit associated video alias
        if (v == sensorVideoUrlBtn) {
            AddEditAlias aea = new AddEditAlias(getContext());

            String videoUID = sensorItem.getMetaString("videoUID", "");
            ConnectionEntry entry = VideoManager.getInstance()
                    .getEntry(videoUID);

            //show the view to enter a new alias or modify an existing one
            aea.addEditConnection(entry, this);
        }

        // Show video alias selector
        else if (id == R.id.videoAliasButton) {
            VideoListDialog d = new VideoListDialog(_mapView);
            d.show(false, new VideoListDialog.Callback() {
                @Override
                public void onVideosSelected(List<ConnectionEntry> s) {
                    if (s.isEmpty())
                        return;
                    ConnectionEntry ce = s.get(0);
                    sensorVideoUrlBtn.setText(ce.getAlias());
                    sensorItem.setMetaString("videoUID", ce.getUID());
                    sensorItem.setMetaString("videoUrl",
                            ConnectionEntry.getURL(ce, false));
                    sensorItem.persist(_mapView.getMapEventDispatcher(),
                            null, this.getClass());
                }
            });
        }

        // Set center coordinate
        else if (v == _coordButton)
            _onCoordSelected();

        // Change stroke or fill color
        else if (v == _fillColorBtn || v == _strokeColorBtn
                || v == _rangeLineStrokeColorBtn) {
            int color = Color.BLACK;
            if (v == _fillColorBtn) {
                color = sensorFOV.getFillColor();
            } else if (v == _strokeColorBtn) {
                color = sensorFOV.getStrokeColor();
            } else if (v == _rangeLineStrokeColorBtn) {
                color = sensorFOV.getRangeLineStrokeColor();
            }

            final AlertDialog.Builder b = new AlertDialog.Builder(getContext());
            b.setTitle(R.string.point_dropper_text21);
            ColorPalette palette = new ColorPalette(getContext());
            palette.setColor(color);
            b.setView(palette);
            final AlertDialog alert = b.create();
            OnColorSelectedListener l = new OnColorSelectedListener() {
                @Override
                public void onColorSelected(int color, String label) {
                    alert.dismiss();
                    if (v == _fillColorBtn) {
                        int a = sensorFOV.getFillColor() >> 24;
                        sensorFOV.setFillColor((a << 24) | (color & 0xFFFFFF));
                    } else if (v == _strokeColorBtn) {
                        sensorFOV.setStrokeColor(color);
                    } else if (v == _rangeLineStrokeColorBtn) {
                        sensorFOV.setRangeLineStrokeColor(color);
                    }
                    _updateColorButtonDrawable();
                }
            };
            palette.setOnColorSelectedListener(l);
            alert.show();
        }

        // Change sensor end point
        else if (id == R.id.placeEndPointButton)
            SensorDetailHandler.selectFOVEndPoint(sensorMarker, false, true);
    }

    @Override
    public void _updateColorButtonDrawable() {
        if (sensorFOV == null)
            return;
        float[] colorArray = sensorFOV.getColor();
        sensorItem.setMetaDouble(SensorDetailHandler.FOV_RED, colorArray[0]);
        sensorItem.setMetaDouble(SensorDetailHandler.FOV_GREEN, colorArray[1]);
        sensorItem.setMetaDouble(SensorDetailHandler.FOV_BLUE, colorArray[2]);
        sensorItem.setMetaInteger(SensorDetailHandler.STROKE_COLOR,
                sensorFOV.getStrokeColor());
        sensorItem.setMetaInteger(SensorDetailHandler.RANGE_LINES_STROKE_COLOR,
                sensorFOV.getRangeLineStrokeColor());
        sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                this.getClass());
        _fillColorBtn.setColorFilter(0xFF000000 |
                (sensorFOV.getFillColor() & 0xFFFFFF));
        _strokeColorBtn.setColorFilter(0xFF000000 |
                (sensorFOV.getStrokeColor() & 0xFFFFFF));
        _rangeLineStrokeColorBtn.setColorFilter(0xFF000000 |
                (sensorFOV.getRangeLineStrokeColor() & 0xFFFFFF));
    }

    public void setSensorMarker(String uid) {
        //find the marker, and set the UI to the values
        MapItem mi = _mapView.getMapItem(uid);
        if (!SensorDetailHandler.hasFoV(mi))
            return;

        removeListeners();

        sensorItem = mi;
        sensorMarker = (Marker) sensorItem;
        sensorUID = uid;
        MapItem sfov = _mapView.getMapItem(uid
                + SensorDetailHandler.UID_POSTFIX);
        if (sfov instanceof SensorFOV)
            sensorFOV = (SensorFOV) sfov;
        if (sfov == null) {
            //if no sensor FOV was found create a default sensor FOV and set it to not be visible
            //TODO in this case we could pull the data from the marker if its available, and use
            //it to create SensorFOV, rather than using hardcoded defaults here
            SensorDetailHandler.addFovToMap(sensorMarker, 270, 45, 100,
                    new float[] {
                            1, 1, 1, 0.3f
                    }, false, false, 100);

            sensorItem.setMetaBoolean(SensorDetailHandler.HIDE_FOV,
                    true);
            sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                    this.getClass());

            sfov = _mapView.getMapItem(uid
                    + SensorDetailHandler.UID_POSTFIX);
            if (sfov instanceof SensorFOV)
                sensorFOV = (SensorFOV) sfov;
            if (sensorFOV == null) {
                Log.e(TAG, "sensor field of view was not found: " +
                        (uid + SensorDetailHandler.UID_POSTFIX));
                return;
            }
        }

        if (!isInitalized)
            _init();

        if (sensorItem
                .hasMetaValue(SensorDetailHandler.MAG_REF_ATTRIBUTE)) {
            int i = sensorItem.getMetaInteger(
                    SensorDetailHandler.MAG_REF_ATTRIBUTE, 0);
            if (i != 0)
                directionAzimuthSpin.setSelection(1);
        }

        updateFOVDetails();

        sensorFOV.addOnMetricsChangedListener(this);
        sensorFOV.addOnStrokeWeightChangedListener(this);
        sensorFOV.addOnStrokeColorChangedListener(this);
        sensorFOV.addOnFillColorChangedListener(this);
        sensorMarker.addOnPointChangedListener(this);
    }

    @Override
    public void onClose() {
        removeListeners();
    }

    private void removeListeners() {
        if (sensorFOV != null) {
            sensorFOV.removeOnMetricsChangedListener(this);
            sensorFOV.removeOnStrokeWeightChangedListener(this);
            sensorFOV.removeOnStrokeColorChangedListener(this);
            sensorFOV.removeOnFillColorChangedListener(this);
        }
        if (sensorMarker != null)
            sensorMarker.removeOnPointChangedListener(this);
    }

    private void updateFOVDetails() {
        //run on UI thread
        ((Activity) _mapView.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                nameET.setText(sensorItem.getMetaString("callsign", ""));
                String videoUID = sensorItem.getMetaString("videoUID", "");

                ConnectionEntry entry = VideoManager.getInstance()
                        .getEntry(videoUID);
                if (entry != null && entry.isRemote())
                    sensorVideoUrlBtn.setText(entry.getAlias());
                else
                    sensorVideoUrlBtn.setText(sensorItem.getMetaString(
                            "videoUrl", "None"));

                urlUiHandler.update(sensorItem);

                remarksLayout.setText(sensorItem.getMetaString("remarks", ""));
                rangeET.setText(String.valueOf((int) sensorFOV.getExtent()));
                rangeLinesET.setText(
                        String.valueOf((int) sensorFOV.getRangeLines()));

                rangeLineSeek.setMax((int) sensorFOV.getExtent());
                _rangeLineMax
                        .setText(String.valueOf((int) sensorFOV.getExtent()));

                if (directionAzimuthSpin.getSelectedItemPosition() == 1) {
                    float trueAzimuth = sensorFOV.getAzimuth();
                    double magAzimuth = ATAKUtilities
                            .convertFromTrueToMagnetic(sensorMarker.getPoint(),
                                    trueAzimuth);
                    directionET.setText(String.valueOf((int) Math
                            .round(magAzimuth)));
                } else
                    directionET.setText(String.valueOf((int) sensorFOV
                            .getAzimuth()));
                fovET.setText(String.valueOf((int) sensorFOV.getFOV()));

                double a = MathUtils.clamp(sensorItem.getMetaDouble(
                        SensorDetailHandler.FOV_ALPHA, 0.3d), 0, 1);
                _fillAlphaSeek.setProgress((int) (a * 255));

                double sw = sensorItem.getMetaDouble(
                        SensorDetailHandler.STROKE_WEIGHT, 0);
                _strokeWeightSeek.setProgress((int) (sw * 20));

                double rlsw = sensorItem.getMetaDouble(
                        SensorDetailHandler.RANGE_LINES_STROKE_WEIGHT, 0);
                _rangeLineStrokeWeightSeek.setProgress((int) (rlsw * 20));

                if (sensorItem.hasMetaValue(SensorDetailHandler.HIDE_FOV))
                    fovVisibleCB.setChecked(false);

                anglesVisibleCB.setChecked(sensorItem.getMetaBoolean(
                        SensorDetailHandler.DISPLAY_LABELS, false));
                _extrasLayout.setItem(sensorItem);
            }
        });
    }

    // show dialog box to enter the altitude
    protected void _onCoordSelected() {
        CoordinateEntryCapability.getInstance(getContext()).showDialog(
                getContext().getString(R.string.point_dropper_text19),
                _point.getGeoPointMetaData(),
                _point.getMovable(), _mapView.getPoint(), _cFormat, false,
                new CoordinateEntryCapability.ResultCallback() {
                    @Override
                    public void onResultCallback(String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {
                        CoordinateFormat cf = CoordinateFormat.find(pane);
                        if (cf.getDisplayName().equals(pane)
                                && cf != CoordinateFormat.ADDRESS)
                            _cFormat = cf;

                        setAddress(point, _point, _nameEdit);

                        sensorMarker.setPoint(point);

                        // this seems to be in contrast of current capability
                        //_coordButton
                        //        .setText(coordView.getFormattedString());

                        CameraController.Programmatic.panTo(
                                _mapView.getRenderer3(), point.get(), true);

                        sensorItem.persist(
                                _mapView.getMapEventDispatcher(), null,
                                this.getClass());

                    }
                });

    }

    @Override
    public void onPointChanged(final PointMapItem item) {
        item.removeOnPointChangedListener(this);
        _point = sensorMarker;
        _mapView.post(new Runnable() {
            @Override
            public void run() {
                _coordButton.setText(_unitPrefs.formatPoint(
                        _point.getGeoPointMetaData(), true));
                formatAddress();
            }
        });
    }

    @Override
    public void onMetricsChanged(SensorFOV fov) {
        //update the Sensor FOV details UI
        updateFOVDetails();
    }

    /**
     * If the user modified the associated video alias, update the name 
     * and save the URL
     */
    @Override
    public void aliasModified(ConnectionEntry selected) {
        if (selected == null) {
            sensorVideoUrlBtn.setText(R.string.none);
            sensorItem.setMetaString("videoUID", "");
            sensorItem.persist(
                    _mapView.getMapEventDispatcher(), null,
                    this.getClass());

            return;
        }
        sensorVideoUrlBtn.setText(selected.getAlias());
        ConnectionEntry entry = VideoManager.getInstance()
                .getEntry(selected.getUID());
        if (entry != null && entry.isRemote()) {
            sensorItem.setMetaString("videoUrl",
                    ConnectionEntry.getURL(entry, false));
            sensorItem.setMetaString("videoUID", entry.getUID());
            sensorItem.persist(_mapView.getMapEventDispatcher(), null,
                    getClass());
        }
    }

    @Override
    public void onStrokeColorChanged(Shape s) {
        updateFOVDetails();
    }

    @Override
    public void onFillColorChanged(Shape s) {
        updateFOVDetails();
    }

    @Override
    public void onStrokeWeightChanged(Shape s) {
        updateFOVDetails();
    }

    /**
     * Method to logorithmically change a sensor fov value [0,MAX_SENSOR_RANGE] into a
     * seek bar value [0,100]
     * @param val [0, MAX_SENSOR_RANGE]
     * @return [0,100]
     */
    private static int toSeekBarScale(int val) {
        if (val < 1)
            return 0;
        return (int) Math
                .round(25 * Math.log10(val / (MAX_SENSOR_RANGE / 10000f)));
    }

    /**
     * Method to logorithmically change a seek bar value [0,100] into a
     * sensor fov value [0,MAX_SENSOR_RANGE]
     * @param val [0,100]
     * @return [0, MAX_SENSOR_RANGE]
     */
    private static int fromSeekBarScale(int val) {
        if (val < 1)
            return 0;
        return (int) Math
                .round(Math.pow(10, val / 25f) * (MAX_SENSOR_RANGE / 10000f));
    }

}
