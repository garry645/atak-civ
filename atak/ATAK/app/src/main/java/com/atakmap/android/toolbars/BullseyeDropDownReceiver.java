
package com.atakmap.android.toolbars;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.cotdetails.CoTInfoView;
import com.atakmap.android.cotdetails.extras.ExtraDetailsLayout;
import com.atakmap.android.drawing.details.GenericDetailsView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.UrlUiHandler;
import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.hashtags.HashtagContent;
import com.atakmap.android.hashtags.HashtagManager;
import com.atakmap.android.hashtags.view.RemarksLayout;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.android.widgets.AutoSizeAngleOverlayShape;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.CameraController;

import java.text.DecimalFormat;

public class BullseyeDropDownReceiver extends DropDownReceiver implements
        OnStateListener, PointMapItem.OnPointChangedListener,
        View.OnClickListener,
        MapItem.OnGroupChangedListener, HashtagManager.OnUpdateListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "BullseyeDropDownReceiver";

    protected static final DecimalFormat DEC_FMT_2 = LocaleUtil
            .getDecimalFormat("#.##");

    protected static final Span[] unitsArray = new Span[] {
            Span.METER, Span.KILOMETER, Span.NAUTICALMILE, Span.FOOT, Span.MILE
    };

    public static final String DROPDOWN_TOOL_IDENTIFIER = "com.atakmap.android.toolbars.BullseyeDropDown";

    protected BullseyeMapItem bullseyeMapItem = null;

    protected final MapView _mapView;
    protected final Context _context;
    protected final UnitPreferences prefs;
    protected Marker centerMarker;

    protected CoordinateFormat _cFormat;
    protected ViewGroup bullseyeLayout;
    private TextView directionLabel;
    private TextView bearingUnitLabel, bearingRefLabel;
    protected Spinner bUnits;
    protected Spinner rUnits;
    private CheckBox showRingsCB;
    private TextView numRingsTV;
    private Button bRadiusButton, rRadiusButton;
    protected EditText title;
    private View ringsLayout;
    private View radiusLayout;
    protected UnitsArrayAdapter unitsAdapter;
    private TextView centerPointLabel;
    protected Intent reopenIntent;
    protected UrlUiHandler urlUiHandler;
    private RemarksLayout remarksLayout;
    protected ExtraDetailsLayout extrasLayout;

    protected final AutoSizeAngleOverlayShape.OnPropertyChangedListener propertyChangedListener = new AutoSizeAngleOverlayShape.OnPropertyChangedListener() {
        @Override
        public void onPropertyChanged() {
            refresh();
        }
    };

    public BullseyeDropDownReceiver(MapView mapView) {
        super(mapView);
        _mapView = mapView;
        _context = mapView.getContext();
        prefs = new UnitPreferences(_context);
        prefs.registerListener(this);
    }

    @Override
    public void onReceive(Context ignoreContext, Intent intent) {

        if (isVisible()) {
            reopenIntent = intent;
            closeDropDown();
            return;
        }

        bullseyeMapItem = null;
        centerMarker = null;

        boolean edit = intent.hasExtra("edit");

        String markerUID = intent.getStringExtra("marker_uid");
        MapItem mi = _mapView.getRootGroup().deepFindUID(markerUID);

        if (mi instanceof BullseyeMapItem)
            bullseyeMapItem = (BullseyeMapItem) mi;
        else if (mi.hasMetaValue("bullseyeUID")) {
            MapItem b = _mapView
                    .getMapItem(mi.getMetaString("bullseyeUID", null));
            if (b instanceof BullseyeMapItem)
                bullseyeMapItem = (BullseyeMapItem) b;

        } else if (edit)
            return;

        if (!edit && bullseyeMapItem == null) {
            centerMarker = (Marker) mi;
            double distance = prefs.get("bullseyeDistance", 2000);
            int numRings = prefs.get("bullseyeNumRings", 4);
            double radiusRings = prefs.get("bullseyeRadiusRings", 400d);
            boolean direction = prefs.get("bullseyeDirection", false);
            bullseyeMapItem = BullseyeTool.createBullseye(getMapView(),
                    centerMarker, distance);
            if (bullseyeMapItem != null) {
                bullseyeMapItem.setNumRings(numRings);
                bullseyeMapItem.setRadiusRings(radiusRings);
                bullseyeMapItem.setEdgeToCenterDirection(direction);
            }
        }

        openBullseye();
    }

    protected void openBullseye() {
        if (!isClosed())
            closeDropDown();

        createLayout();

        showDropDown(bullseyeLayout, THREE_EIGHTHS_WIDTH, FULL_HEIGHT,
                FULL_WIDTH, HALF_HEIGHT, this);
        setRetain(true);
    }

    /**
     * Toggle if the Bullseye's range rings are shown or not
     * 
     * @param intent - the intent with the relevant info
     */
    public void toggleRings(Intent intent) {
        String markerUID = intent.getStringExtra("marker_uid");
        MapItem mi = _mapView.getRootGroup().deepFindUID(markerUID);
        if (!(mi instanceof Marker))
            return;

        Marker marker = (Marker) mi;

        String bUID = marker.getMetaString("bullseyeUID", "");
        mi = _mapView.getRootGroup().deepFindUID(bUID);
        if (!(mi instanceof BullseyeMapItem))
            return;

        BullseyeMapItem bullseyeMapItem = (BullseyeMapItem) mi;

        //if the dropdown is showing this bullseye's information, just
        //toggle the checkbox and the handler will do the rest
        if (isVisible() && this.bullseyeMapItem == bullseyeMapItem) {
            showRingsCB.setChecked(!showRingsCB.isChecked());
            return;
        }

        marker.setMetaBoolean("rangeRingVisible",
                !marker.getMetaBoolean("rangeRingVisible", false));

        // more politely calls refresh
        bullseyeMapItem.setNumRings(bullseyeMapItem.getNumRings());

        marker.persist(_mapView.getMapEventDispatcher(),
                null, getClass());
    }

    /**
     * Toggle if the Bullseye labels are from edge to center or center to edge
     *
     * @param intent - the intent with the relevant info - specifically the marker_uid
     */
    public void toggleDirection(Intent intent) {
        // can be called externally so it will need to make use of the intent marker uid
        String marker_uid = intent.getStringExtra("marker_uid");
        MapItem mi = _mapView.getMapItem(marker_uid);
        if (mi instanceof BullseyeMapItem) {
            BullseyeMapItem bullseye = (BullseyeMapItem) mi;
            if (bullseye.isShowingEdgeToCenter()) {
                bullseye.setEdgeToCenterDirection(false);
                bullseye.setColorRings(Color.GREEN);
            } else {
                bullseye.setEdgeToCenterDirection(true);
                bullseye.setColorRings(Color.RED);
            }
            bullseye.persist(_mapView.getMapEventDispatcher(),
                    null, BullseyeTool.class);
            refresh();
        } else {
            Log.e(TAG, "error, map item not a bullseye");
        }
    }

    /**
     * Toggle if the bearing of the Bullseye is oriented about 
     * true or magnetic north
     * 
     * @param intent - the intent with the relevant info
     */
    public void toggleBearing(Intent intent) {
        final String marker_uid = intent.getStringExtra("marker_uid");
        final MapItem mi = _mapView.getMapItem(marker_uid);
        if (mi instanceof BullseyeMapItem) {
            BullseyeMapItem bullseye = (BullseyeMapItem) mi;
            if (intent.hasExtra("degrees")) {
                bullseye.setBearingUnits(Angle.DEGREE);
                if (!bullseye.getType().startsWith("a-h")) {
                    bullseye.removeMetaData("mils_mag");
                    bullseye.setMetaBoolean("deg_mag", true);
                }
            } else {
                bullseye.setBearingUnits(Angle.MIL);
                if (!bullseye.getType().startsWith("a-h")) {
                    bullseye.removeMetaData("deg_mag");
                    bullseye.setMetaBoolean("mils_mag", true);
                }
            }
            bullseye.persist(_mapView.getMapEventDispatcher(),
                    null, getClass());
            refresh();
        } else {
            Log.e(TAG, "error, map item not a bullseye");
        }
    }

    /**
     * Set up the view of the dropdown given the current AngleOverlayShape 
     *
     */
    protected void createLayout() {

        if (bullseyeMapItem == null)
            return;

        bullseyeLayout = (ViewGroup) LayoutInflater.from(_context).inflate(
                R.layout.bullseye_details, getMapView(), false);

        if (centerMarker != null)
            centerMarker.removeOnPointChangedListener(this);

        centerMarker = bullseyeMapItem;

        centerMarker.addOnPointChangedListener(this);

        GenericDetailsView.addEditTextPrompts(bullseyeLayout);
        title = bullseyeLayout
                .findViewById(R.id.nameEditText);
        title.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        title.setText(bullseyeMapItem.getTitle());
        title.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            synchronized public void afterTextChanged(Editable s) {
                String newString = s.toString();
                if (!newString.equals(bullseyeMapItem.getTitle())) {
                    bullseyeMapItem.setTitle(newString);
                    if (centerMarker != null && centerMarker.getType()
                            .startsWith("u-r-b-bullseye"))
                        centerMarker.setTitle(newString);
                }
            }
        });

        unitsAdapter = new UnitsArrayAdapter(
                getMapView().getContext(),
                R.layout.spinner_text_view, unitsArray);
        unitsAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        bRadiusButton = bullseyeLayout
                .findViewById(R.id.bullseyeRadiusButton);
        bUnits = bullseyeLayout.findViewById(
                R.id.bullseyeRadiusUnitsSpinner);
        bUnits.setAdapter(unitsAdapter);
        bUnits.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                final Span selectedUnits = unitsAdapter.getItem(position);
                if (selectedUnits == null)
                    return;

                prefs.setRangeSystem(selectedUnits.getType());

                Span rUnits = bullseyeMapItem.getRadiusUnits();
                if (rUnits != selectedUnits) {
                    double radius = SpanUtilities.convert(
                            bullseyeMapItem.getRadius(),
                            Span.METER, selectedUnits);
                    bullseyeMapItem.setRadius(radius, selectedUnits);
                    prefs.setRangeSystem(selectedUnits.getType());
                    refresh();
                }
            }
        });
        bRadiusButton.setOnClickListener(this);

        numRingsTV = bullseyeLayout.findViewById(R.id.ringsText);

        rRadiusButton = bullseyeLayout.findViewById(
                R.id.ringRadiusButton);
        rUnits = bullseyeLayout.findViewById(
                R.id.ringRadiusUnitsSpinner);
        rUnits.setAdapter(unitsAdapter);
        rUnits.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Span selectedUnits = unitsAdapter.getItem(position);
                if (selectedUnits != null) {
                    prefs.setRangeSystem(selectedUnits.getType());
                    refresh();
                }
            }
        });

        ringsLayout = bullseyeLayout.findViewById(R.id.ringsView);
        radiusLayout = bullseyeLayout.findViewById(R.id.radiusView);
        showRingsCB = bullseyeLayout
                .findViewById(R.id.showRingsCB);
        showRingsCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                bullseyeMapItem.setMetaBoolean("hasRangeRings", isChecked);
                if (isChecked) {

                    bullseyeMapItem.setMetaBoolean("rangeRingVisible", true);
                    bullseyeMapItem.setNumRings(bullseyeMapItem.getNumRings());
                    ringsLayout.setVisibility(View.VISIBLE);
                    radiusLayout.setVisibility(View.VISIBLE);
                } else {
                    bullseyeMapItem.setMetaBoolean("rangeRingVisible", false);
                    bullseyeMapItem.setNumRings(bullseyeMapItem.getNumRings());
                    ringsLayout.setVisibility(View.GONE);
                    radiusLayout.setVisibility(View.GONE);
                }
                refresh();
            }
        });

        rRadiusButton.setOnClickListener(this);

        bullseyeLayout.findViewById(R.id.ringsMinusButton)
                .setOnClickListener(this);
        bullseyeLayout.findViewById(R.id.ringsPlusButton)
                .setOnClickListener(this);

        if (centerMarker.getType()
                .contentEquals(BullseyeTool.BULLSEYE_COT_TYPE)) {
            View centerTextView = bullseyeLayout
                    .findViewById(R.id.centerPointButton);
            centerTextView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayCoordinateDialog(centerMarker);
                }
            });
        }
        centerPointLabel = bullseyeLayout
                .findViewById(R.id.centerPointLabel);

        directionLabel = bullseyeLayout
                .findViewById(R.id.centerDirectionLabel);
        View centerDirection = bullseyeLayout
                .findViewById(R.id.centerDirectionView);
        centerDirection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (directionLabel.getText().toString().equals(_context
                        .getString(R.string.bullseye_from_center))) {
                    bullseyeMapItem.setEdgeToCenterDirection(true);
                    bullseyeMapItem.setColorRings(Color.RED);
                } else {
                    bullseyeMapItem.setEdgeToCenterDirection(false);
                    bullseyeMapItem.setColorRings(Color.GREEN);
                }
                refresh();
            }
        });

        bearingUnitLabel = bullseyeLayout
                .findViewById(R.id.bearingUnitLabel);
        View bearingUnit = bullseyeLayout.findViewById(R.id.bearingUnitView);
        bearingUnit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prefs.getBearingUnits() == Angle.MIL) {
                    prefs.setBearingUnits(Angle.DEGREE);
                } else {
                    prefs.setBearingUnits(Angle.MIL);
                }
                refresh();
            }
        });

        bearingRefLabel = bullseyeLayout
                .findViewById(R.id.bearingRefLabel);
        View bearingRef = bullseyeLayout.findViewById(R.id.bearingRefView);
        bearingRef.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final NorthReference current = prefs.getNorthReference();
                switch (current) {
                    case GRID:
                        prefs.setNorthReference(NorthReference.TRUE);
                        break;
                    case TRUE:
                        prefs.setNorthReference(NorthReference.MAGNETIC);
                        break;
                    default:
                        prefs.setNorthReference(NorthReference.GRID);
                        break;
                }
                bullseyeMapItem.refresh(getMapView().getMapEventDispatcher(),
                        null,
                        BullseyeDropDownReceiver.class);
                refresh();
            }
        });
        bearingRef.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(_context);
                builder.setTitle(_context.getString(R.string.select_space) +
                        _context.getString(R.string.preferences_text352));
                Resources res = _context.getResources();
                String[] northList = res
                        .getStringArray(R.array.north_refs_label);
                builder.setItems(northList,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                switch (which) {
                                    case 0:
                                        bullseyeMapItem.setNorthReference(
                                                NorthReference.TRUE);
                                        break;
                                    case 1:
                                        bullseyeMapItem.setNorthReference(
                                                NorthReference.MAGNETIC);
                                        break;
                                    case 2:
                                        bullseyeMapItem.setNorthReference(
                                                NorthReference.GRID);
                                        break;
                                    default:
                                        Log.d(TAG,
                                                "Unexpected North Reference Selection");
                                }
                                refresh();
                            }
                        });
                builder.setNegativeButton(R.string.cancel, null);
                builder.create().show();
                return true;
            }
        });

        urlUiHandler = new UrlUiHandler(bullseyeLayout);
        urlUiHandler.update(centerMarker);

        remarksLayout = bullseyeLayout.findViewById(R.id.remarksLayout);
        remarksLayout.setText(centerMarker.getRemarks());
        remarksLayout.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                centerMarker.setRemarks(s.toString());
            }
        });

        extrasLayout = bullseyeLayout.findViewById(R.id.extrasLayout);

        bullseyeLayout.findViewById(R.id.sendLayout).setOnClickListener(this);

        HashtagManager.getInstance().registerUpdateListener(this);

        // called during creation, does not need to be on the ui thread
        refreshImpl();

        // logic to display in the most sensible form possible when first presenting to the
        // end user.   Setting the selection will automatically trigger a refresh of the values
        Span bSpan = prefs.getRangeUnits(bullseyeMapItem.getRadius());
        bUnits.setSelection(unitsAdapter.getPosition(bSpan));

        Span rSpan = prefs.getRangeUnits(bullseyeMapItem.getRadiusRings());
        rUnits.setSelection(unitsAdapter.getPosition(rSpan));

    }

    protected void refresh() {
        if (!isVisible() || bullseyeMapItem == null || bullseyeLayout == null)
            return;

        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            _mapView.post(new Runnable() {
                @Override
                public void run() {
                    refreshImpl();
                }
            });
        } else {
            refreshImpl();
        }
    }

    private void refreshImpl() {
        title.setText(bullseyeMapItem.getTitle());

        CoTInfoView.refreshAuthorLayout(_mapView, bullseyeMapItem,
                bullseyeLayout.findViewById(R.id.dgAuthorLayout));

        final String latLon = CoordinateFormatUtilities.formatToString(
                bullseyeMapItem.getGeoPointMetaData().get(),
                prefs.getCoordinateFormat());
        centerPointLabel.setText(latLon);

        // Direction
        directionLabel.setText(bullseyeMapItem.isShowingEdgeToCenter()
                ? R.string.bullseye_to_center
                : R.string.bullseye_from_center);

        // Bearing units
        bearingUnitLabel
                .setText(bullseyeMapItem.getBearingUnits() == Angle.MIL
                        ? R.string.mils_full
                        : R.string.degrees_full);

        // North reference
        NorthReference ref = bullseyeMapItem.getNorthReference();
        if (ref == NorthReference.TRUE)
            bearingRefLabel.setText(R.string.tn_no_units);
        else if (ref == NorthReference.MAGNETIC)
            bearingRefLabel.setText(R.string.mz_no_units);
        else
            bearingRefLabel.setText(R.string.gn_no_units);

        // Rings toggle

        boolean b = bullseyeMapItem.getMetaBoolean("rangeRingVisible",
                false);
        showRingsCB.setChecked(b);

        ringsLayout.setVisibility(b ? View.VISIBLE : View.GONE);
        radiusLayout.setVisibility(b ? View.VISIBLE : View.GONE);

        double bRadius = SpanUtilities.convert(
                bullseyeMapItem.getRadius(),
                Span.METER, (Span) bUnits.getSelectedItem());
        bRadiusButton.setText(DEC_FMT_2.format(bRadius));

        double rRadius = SpanUtilities.convert(
                bullseyeMapItem.getRadiusRings(),
                Span.METER, (Span) rUnits.getSelectedItem());
        rRadiusButton.setText(DEC_FMT_2.format(rRadius));

        numRingsTV
                .setText(String.valueOf(bullseyeMapItem.getNumRings()));

        urlUiHandler.update(centerMarker);
        extrasLayout.setItem(bullseyeMapItem);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        final Context ctx = getMapView().getContext();

        // Set bullseye or range ring radius
        if (id == R.id.bullseyeRadiusButton || id == R.id.ringRadiusButton) {
            final Button btn = (Button) v;
            Spinner s = id == R.id.bullseyeRadiusButton ? bUnits : rUnits;
            final Span span = (Span) s.getSelectedItem();
            final EditText et = new EditText(ctx);
            et.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et.setText(btn.getText());
            et.selectAll();

            AlertDialog.Builder b = new AlertDialog.Builder(ctx);
            b.setMessage(ctx.getString(R.string.rb_circle_dialog)
                    + span.getPlural() + ":");
            b.setView(et);
            b.setPositiveButton(R.string.ok, null);
            b.setNegativeButton(R.string.cancel, null);
            final AlertDialog d = b.create();
            if (d.getWindow() != null)
                d.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            d.show();
            d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            double radius = 0;
                            try {
                                radius = Double.parseDouble(et.getText()
                                        .toString());
                            } catch (Exception ignore) {
                            }
                            if (radius > 0.0) {
                                if (id == R.id.bullseyeRadiusButton)
                                    bullseyeMapItem.setRadius(radius, span);
                                else
                                    bullseyeMapItem.setRadiusRings(
                                            SpanUtilities.convert(
                                                    radius, span, Span.METER));
                                btn.setText(et.getText().toString());
                                d.dismiss();
                            } else {
                                Toast.makeText(ctx, R.string.rb_circle_tip2,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
            et.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId,
                        KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE)
                        d.getButton(DialogInterface.BUTTON_POSITIVE)
                                .performClick();
                    return false;
                }
            });
            et.requestFocus();
        }

        // Add or subtract range ring
        else if (id == R.id.ringsMinusButton || id == R.id.ringsPlusButton) {

            boolean add = id == R.id.ringsPlusButton;
            if (!add && bullseyeMapItem.getNumRings() <= 1) {
                Toast.makeText(ctx, R.string.details_text57,
                        Toast.LENGTH_LONG).show();
                return;
            } else if (add
                    && bullseyeMapItem.getNumRings() >= RangeCircle.MAX_RINGS) {
                Toast.makeText(ctx, R.string.details_text59,
                        Toast.LENGTH_LONG).show();
                return;
            }
            int rings = bullseyeMapItem.getNumRings() + (add ? 1 : -1);
            bullseyeMapItem.setNumRings(rings);
            numRingsTV.setText((rings < 10 ? "0" : "") + rings);
        }

        // Send bullseye
        else if (id == R.id.sendLayout) {
            if (centerMarker == null)
                return;
            CotEvent event = CotEventFactory.createCotEvent(centerMarker);
            Intent i = new Intent(ContactPresenceDropdown.SEND_LIST);
            i.putExtra("com.atakmap.contact.CotEvent", event);
            AtakBroadcast.getInstance().sendBroadcast(i);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownClose() {
        HashtagManager.getInstance().unregisterUpdateListener(this);

        if (reopenIntent != null) {
            onReceive(getMapView().getContext(), reopenIntent);
            reopenIntent = null;
        }
        if (centerMarker != null) {
            centerMarker.persist(_mapView.getMapEventDispatcher(),
                    null, this.getClass());
        }
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownVisible(boolean v) {
        if (v) {
            setSelected(centerMarker);
            if (centerMarker != null)
                CameraController.Programmatic.panTo(getMapView().getRenderer3(),
                        centerMarker.getPoint(), true);
            refresh();
        }
    }

    @Override
    protected void disposeImpl() {
        prefs.unregisterListener(this);
    }

    @Override
    public void onPointChanged(PointMapItem s) {
        if (s == centerMarker)
            refresh();
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup group) {
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup group) {
        if (this.bullseyeMapItem == item)
            closeDropDown();
    }

    @Override
    public void onHashtagsUpdate(HashtagContent content) {
        if (content == centerMarker) {
            getMapView().post(new Runnable() {
                @Override
                public void run() {
                    remarksLayout.setText(centerMarker.getRemarks());
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedprefs,
            String key) {
        if (key == null)
            return;
        if (key.equals(UnitPreferences.COORD_FMT)
                || key.equals(UnitPreferences.RANGE_SYSTEM)) {
            refresh();
        }
    }

    /**
     * Show the dialog to manually enter the center coordinate
     *
     * @param bullseye - the point to populate the views with
     */
    private static void displayCoordinateDialog(final Marker bullseye) {
        final MapView _mapView = MapView.getMapView();
        if (_mapView == null)
            return;
        final Context _context = _mapView.getContext();

        CoordinateEntryCapability.getInstance(_context).showDialog(
                _context.getString(R.string.rb_coord_title),
                bullseye.getGeoPointMetaData(),
                bullseye.getMovable(),
                _mapView.getPoint(),
                CoordinateFormat
                        .find(bullseye.getMetaString("coordFormat", "")),
                false,
                new CoordinateEntryCapability.ResultCallback() {
                    @Override
                    public void onResultCallback(String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {
                        // On click get the geopoint and elevation double in ft

                        CoordinateFormat cf = CoordinateFormat.find(pane);

                        if (point == null || !point.get().isValid())
                            return;

                        // always look up the elevation since the point has been moved
                        // ATAK-7066 If Bullseye center location is changed in Details, elevation is not updated

                        final GeoPoint gp = point.get();
                        point = ATAKUtilities
                                .getElevationMetadata(gp.getLatitude(),
                                        gp.getLongitude());

                        bullseye.setMetaString("coordFormat",
                                cf.getDisplayName());
                        Intent showDetails = new Intent();
                        showDetails.setAction(
                                "com.atakmap.android.maps.SHOW_DETAILS");
                        showDetails.putExtra("uid", bullseye.getUID());
                        AtakBroadcast.getInstance().sendBroadcast(showDetails);
                        bullseye.setPoint(point);
                        CameraController.Programmatic.panTo(
                                _mapView.getRenderer3(), point.get(), true);

                    }
                });
    }

}
