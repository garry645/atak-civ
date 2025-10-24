
package com.atakmap.android.drawing.details.msd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.drawing.mapItems.MsdShape;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.ColorButton;
import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.toolbars.UnitsArrayAdapter;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

public class ShapeMsdDropDownReceiver extends DropDownReceiver implements
        OnStateListener, ShapeMsdAdapter.EntryChangedListener {

    public static final String TAG = "ShapeMsdDropDownReceiver";

    public static final String MSD_ACTION = "com.atakmap.android.maps.SHAPE_MSD";
    private final View msdShapeView;
    private final UnitPreferences _prefs;
    private Span _units = Span.METER;
    private final ShapeMsdAdapter shapeMsdAdapter;

    static final ThreadLocal<DecimalFormat> _one = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return LocaleUtil.getDecimalFormat("#.#");
        }
    };

    static final ThreadLocal<DecimalFormat> _two = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return LocaleUtil.getDecimalFormat("#.##");
        }
    };

    private final CheckBox msd_enabled;
    private final EditText rangeTxt;
    private final Spinner unitsSp;
    private final ColorButton colorBtn;
    private final UnitsArrayAdapter adapter;
    private Shape current;

    /**************************** CONSTRUCTOR *****************************/

    public ShapeMsdDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        _prefs = new UnitPreferences(mapView.getContext());
        msdShapeView = PluginLayoutInflater.inflate(context,
                R.layout.shape_msd_dropdown, null);

        msd_enabled = msdShapeView.findViewById(R.id.msd_enabled);
        rangeTxt = msdShapeView.findViewById(R.id.msd_range);
        adapter = new UnitsArrayAdapter(getMapView().getContext(),
                R.layout.spinner_text_view, Span.values());
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        unitsSp = msdShapeView.findViewById(R.id.msd_units);
        unitsSp.setAdapter(adapter);
        colorBtn = msdShapeView.findViewById(R.id.msd_color);

        shapeMsdAdapter = new ShapeMsdAdapter(mapView,
                new ShapeMsdAdapter.ShapeMsdEntrySelectedListener() {
                    @Override
                    public void onMsdEntrySelected(double range, int color) {
                        colorBtn.setColor(color);
                        final double r = SpanUtilities.convert(range,
                                Span.METER, _units);
                        rangeTxt.setText(range < 100 ? _two.get().format(r)
                                : _one.get().format(r));
                    }
                });

        ListView listView = msdShapeView.findViewById(R.id.listview);
        listView.setAdapter(shapeMsdAdapter);

        ArrayList<ShapeMsdEntry> arrayList = shapeMsdAdapter.getCurrentList();
        for (ShapeMsdEntry entry : arrayList)
            added(entry);

        shapeMsdAdapter.addEntryChangedListener(this);

        ImageButton add = msdShapeView.findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShapeMsdEntry ShapeMsdEntry = new ShapeMsdEntry(
                        UUID.randomUUID().toString(), "",
                        0, Color.RED);
                AlertDialog.Builder builder = shapeMsdAdapter
                        .createEdit(ShapeMsdEntry);
                builder.show();
            }
        });

        rangeTxt.addTextChangedListener(textWatcher);

    }

    public void disposeImpl() {
        if (shapeMsdAdapter != null)
            shapeMsdAdapter.dispose();
    }

    @Override
    public void added(ShapeMsdEntry entry) {
    }

    @Override
    public void removed(ShapeMsdEntry entry) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        // Find the map item being targeted
        String uid = intent.getStringExtra("shapeUID");
        if (FileSystemUtils.isEmpty(uid))
            uid = intent.getStringExtra("assocSetUID");
        if (FileSystemUtils.isEmpty(uid))
            uid = intent.getStringExtra("uid");
        if (FileSystemUtils.isEmpty(uid)) {
            Log.w(TAG, "Failed to find UID for action: " + action);
            return;
        }

        MapItem item = getMapView().getMapItem(uid);

        if (item == null) {
            Log.w(TAG, "Failed to find item with UID " + uid
                    + " for action: " + action);
            return;
        }

        if (action.equals(MSD_ACTION)) {
            item = ATAKUtilities.findAssocShape(item);
            if (item instanceof Shape) {
                current = null;
                init((Shape) item);
                showDropDown(msdShapeView, THIRD_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                        HALF_HEIGHT, false);
            }
        }
    }

    private void init(Shape shape) {

        final double range;
        final int color;

        final MsdShape existing = findExistingMSD(getMapView(), shape);
        if (existing != null) {
            range = existing.getRange();
            color = existing.getStrokeColor();
        } else {
            range = 0;
            color = Color.RED;
        }

        msd_enabled.setChecked(existing != null);

        _units = _prefs.getRangeUnits(range);
        final double r = SpanUtilities.convert(range, Span.METER, _units);
        rangeTxt.setText(
                range < 100 ? _two.get().format(r) : _one.get().format(r));

        unitsSp.setSelection(adapter.getPosition(_units));
        unitsSp.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (current == null)
                    return;

                _units = (Span) parent.getSelectedItem();
                _prefs.setRangeSystem(_units.getType());
                if (msd_enabled.isChecked()) {
                    double r = getRange(rangeTxt, unitsSp);
                    int c = colorBtn.getColor();
                    update(shape, r, c);
                }
            }
        });

        colorBtn.setColor(color);

        colorBtn.setOnColorSelectedListener(
                new ColorPalette.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color, String label) {
                        if (current == null)
                            return;

                        if (msd_enabled.isChecked()) {
                            double r = getRange(rangeTxt, unitsSp);
                            int c = colorBtn.getColor();
                            update(shape, r, c);
                        }
                    }
                });

        msd_enabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current == null)
                    return;

                if (msd_enabled.isChecked()) {
                    double r = getRange(rangeTxt, unitsSp);
                    int c = colorBtn.getColor();
                    update(shape, r, c);
                } else {
                    remove(shape);
                }
            }
        });

        current = shape;
    }

    final AfterTextChangedWatcher textWatcher = new AfterTextChangedWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            try {
                if (current == null)
                    return;

                if (msd_enabled.isChecked()) {
                    double r = getRange(rangeTxt, unitsSp);
                    int c = colorBtn.getColor();
                    update(current, r, c);
                }
            } catch (Exception ignored) {
            }
        }
    };

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    /**
     * Find the corresponding MSD shape for a given parent shape
     * @param shape Parent shape
     * @return MSD shape or null if not found
     */
    static MsdShape findExistingMSD(@NonNull
    final MapView mapView,
            @NonNull
            final Shape shape) {
        MapItem existing = mapView.getMapItem(shape.getUID() + ".msd");
        return existing instanceof MsdShape ? (MsdShape) existing : null;
    }

    private void remove(Shape shape) {
        final MsdShape existing = findExistingMSD(getMapView(), shape);
        if (existing != null) {
            existing.removeFromGroup();
            shape.persist(getMapView().getMapEventDispatcher(),
                    null, getClass());
        }
    }

    private void update(Shape shape, double newRange, int newColor) {
        try {
            final MsdShape existing = findExistingMSD(getMapView(), shape);

            // Check that range is greater than zero
            if (newRange < 0) {
                Toast.makeText(getMapView().getContext(),
                        R.string.nineline_text10,
                        Toast.LENGTH_LONG).show();
                return;
            }

            MsdShape msd = existing != null ? existing
                    : new MsdShape(getMapView(), shape);
            msd.setRange(newRange);
            msd.setStrokeColor(newColor);
            msd.addToShapeGroup();
            shape.persist(getMapView().getMapEventDispatcher(),
                    null, getClass());
        } catch (Exception e) {
            Log.e(TAG,
                    "error entering information", e);
        }
    }

    /**
     * Given a range and units produce the range in meters
     * @param rangeTxt the range edit text
     * @param unitSp the spinner
     * @return the range in meters
     */
    static double getRange(final EditText rangeTxt, final Spinner unitSp) {
        final Span span = (Span) unitSp.getSelectedItem();
        String rangeString = rangeTxt.getText().toString();
        try {
            double range = Double.parseDouble(rangeString);
            return SpanUtilities.convert(range, span, Span.METER);
        } catch (Exception e) {
            return 0.0;
        }
    }

}
