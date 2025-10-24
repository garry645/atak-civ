
package com.atakmap.android.cotdetails;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.drawing.details.GenericDetailsView;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.android.util.DatePickerFragment;
import com.atakmap.android.util.TimePickerFragment;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.lang.Objects;
import com.atakmap.map.elevation.ElevationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.lang.Parsers;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;

public class ModifierInfoView extends ExtendedInfoView {

    private static final String TAG = "ModifierInfoView";

    private static final String SHOW_MODIFIERS_PREF = "com.atakmap.android.cotdetails.SHOW_MODIFIERS";

    private final Context context;

    private static final List<String> MODIFIER_EXCLUDE = Collections.singletonList(""); //Arrays.asList("W", "X", "D", "R", "S", "AB", "AC", "AG", "AH", "AI", "AQ", "B", "H", "J", "K", "L", "M", "N", "P", "T", "V", "Y", "AA", "AD", "AE", "AF", "AL", "AO", "AP", "AR", "AS", "T1", "R2", "H1", "H2" );
    private static final Map<String, String[]> MODIFIER_ENUM = new HashMap<String, String[]>() {
        {
            put("F", new String[] {
                    "", "+", "-", "+/-"
            });
            put("R", new String[] {
                    "", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW",
                    "MX", "MY"
            });
            put("K", new String[] {
                    "", "FO", "SO", "MO", "NO", "UNK"
            });
            put("AG", new String[] {
                    "", "NS", "NL"
            });
            put("AL", new String[] {
                    "", "Fully Capable", "Damaged/Rendered Ineffective",
                    "Destroyed", "Full to Capacity"
            });
        }
    };

    // 5.3.6.7 Date-time group. Date-time group (DTG) is defined as the date and time expressed in
    // an alphanumeric combination. The alphanumeric combination used is day-time- time
    // zone-month-year. The alphanumeric combination can be displayed in a number of ways.
    // In its longest form, sixteen characters, it is composed of eight digits (first pair of
    // digits denotes the date, second pair denotes the hours, third pair denotes the minutes and
    // fourth pair denotes the seconds) followed by the time zone suffix, followed by a three-letter
    // month abbreviation and four digits for the year: DDHHMMSSZMONYYYY. It can also be expressed
    // in shorter forms by removing characters, such as DDHHMMZMONYY. On order (O/O) is a valid
    // substitute for DTG.
    private final static CoordinatedTime.SimpleDateFormatThread dateFormatter = new CoordinatedTime.SimpleDateFormatThread(
            "ddHHmm'Z'MMMyyy", LocaleUtil.US);

    private PointMapItem _marker;

    protected MapView mapView;

    public ModifierInfoView(MapView mapView) {
        super(mapView.getContext());
        this.mapView = mapView;
        this.context = mapView.getContext();
    }


    public ModifierInfoView(Context context) {
        super(context);
        this.mapView = MapView.getMapView();
        this.context = mapView.getContext();
    }

    public ModifierInfoView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        this.mapView = MapView.getMapView();
        this.context = mapView.getContext();
    }

    public ModifierInfoView(Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mapView = MapView.getMapView();
        this.context = mapView.getContext();
    }

    @Override
    public void setMarker(PointMapItem m) {
        if (m != _marker) {
            this._marker = m;
            getModifierView(m);
        }
    }

    abstract static class ModifierInputHandler {

        private final MapEventDispatcher _mapEventDispatcher;
        private final MapItem _item;
        private final Modifier _modifier;
        private final ArrayList<View> _inputViews;

        ModifierInputHandler(MapItem item, Modifier modifier, MapEventDispatcher eventDispatcher) {
            _item = item;
            _modifier = modifier;
            _mapEventDispatcher = eventDispatcher;
            _inputViews = new ArrayList<>();
        }

        void addInputView(View v) {
            _inputViews.add(v);
        }

        protected final void update() {
            final int limit = _inputViews.size();
            StringBuilder value = null;
            for(int i = 0; i < limit; i++) {
                final String input = getValue(_inputViews.get(i));
                if(input == null)
                    continue;
                if(value != null)
                    value.append(',');
                else
                    value = new StringBuilder();
                value.append(input);

            }
            if(value != null) {
                _item.setMetaString(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                + _modifier.getId(),
                        value.toString());
            } else {
                _item.removeMetaData(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                + _modifier.getId()
                );
            }
            incrementModifierVersion(_mapEventDispatcher, _item);
        }

        protected abstract String getValue(View inputView);
    }

    final static class BooleanModifierHandler extends ModifierInputHandler implements OnClickListener {
        BooleanModifierHandler(MapItem item, Modifier modifier, MapEventDispatcher eventDispatcher) {
            super(item, modifier, eventDispatcher);
        }

        @Override
        protected String getValue(View view) {
            final CheckBox checkBox = (CheckBox)view;
            return checkBox.isChecked() ? "true" : null;
        }

        @Override
        public void onClick(View view) {
            update();
        }
    }

    static class ModifierTextWatcher extends ModifierInputHandler implements TextWatcher {
        boolean _changed;

        ModifierTextWatcher(MapItem item, Modifier modifier, MapEventDispatcher eventDispatcher) {
            super(item, modifier, eventDispatcher);
            _changed = false;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            _changed = false;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            _changed = true;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(_changed) {
                update();
                _changed = false;
            }
        }

        @Override
        protected String getValue(View inputView) {
            final Editable text = ((EditText)inputView).getText();
            return (text.length() > 0) ? text.toString() : null;
        }
    }

    static void incrementModifierVersion(MapEventDispatcher eventDispatcher, MapItem item) {
        item.setMetaInteger("modifierVersion",
                item.getMetaInteger("modifierVersion",
                        0) + 1);
        item.persist(eventDispatcher, null,
                ModifierInfoView.class);

    }

    final static class EnumModifierHandler extends ModifierInputHandler implements
            AdapterView.OnItemSelectedListener,
            OnTouchListener
    {
        private boolean _userSelected = false;

        EnumModifierHandler(MapItem item, Modifier modifier, MapEventDispatcher eventDispatcher) {
            super(item, modifier, eventDispatcher);
        }

        @Override
        protected String getValue(View inputView) {
            final AdapterView adapterView = (AdapterView)inputView;
            return adapterView.getItemAtPosition(adapterView.getSelectedItemPosition()).toString();
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            _userSelected = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i,
                long l) {
            if (!_userSelected)
                return;
            _userSelected = false;
            update();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // NO-OP
        }
    }

    private static TextView getTitleView(Context context, String title) {
        TextView titleView = new TextView(context);
        titleView.setTextColor(
                context.getResources().getColor(R.color.heading_yellow));
        titleView.setTextSize(
                context.getResources().getDimension(R.dimen.draper_font)
                        / context.getResources().getDisplayMetrics().density);
        titleView.setText(title);
        return titleView;
    }

    public ExtendedInfoView getModifierView(PointMapItem _marker) {
        if (_marker == null)
            return this;

        this._marker = _marker;
        String markerType = _marker.getType();

        removeAllViews();
        setOrientation(LinearLayout.VERTICAL);

        View modifierDetailView = LayoutInflater.from(context)
                .inflate(R.layout.unit_modifiers, null);
        LinearLayout modifierToggle = modifierDetailView
                .findViewById(R.id.modifierToggle);
        CheckBox showModifiersCB = modifierDetailView
                .findViewById(R.id.showModifiersCB);
        LinearLayout modifierList = modifierDetailView
                .findViewById(R.id.modifierList);

        String m2525 = _marker.getMetaString("milsym", null);
        if (m2525 == null)
             m2525 = CotUtils.mil2525cFromCotType(markerType)
                .toUpperCase();

        if (m2525.isEmpty()) {
            setVisibility(View.GONE);
            return this;
        } else {
            setVisibility(View.VISIBLE);
        }

        Collection<Modifier> modifiers = SymbologyProvider.getModifiers(m2525);

        _marker.removeOnMetadataChangedListener("milsym", changeListener);
        _marker.addOnMetadataChangedListener("milsym",changeListener);

        if (!modifiers.isEmpty()) {
            modifierToggle.setVisibility(VISIBLE);

            AtakPreferences prefs = AtakPreferences.getInstance(context);

            boolean showModifiers = prefs.get(SHOW_MODIFIERS_PREF, false);
            showModifiersCB.setChecked(showModifiers);
            modifierList.setVisibility(showModifiers ? VISIBLE : GONE);

            showModifiersCB.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                boolean isChecked) {
                            if (isChecked) {
                                modifierList.setVisibility(VISIBLE);
                            } else {
                                modifierList.setVisibility(GONE);
                            }
                            prefs.set(SHOW_MODIFIERS_PREF, isChecked);
                        }
                    });
        }

        ModifierInfoView.Builder builder = new ModifierInfoView.Builder(mapView, modifierList,
                _marker);

        for (Modifier modifier : modifiers) {
            if (!MODIFIER_EXCLUDE.contains(modifier.getId())) {
                builder.addModifierView(modifier);
            }
        }
        builder.build();
        addView(modifierDetailView);
        return this;
    }


    private final MapItem.OnMetadataChangedListener changeListener =
            new MapItem.OnMetadataChangedListener() {
        @Override
        public void onMetadataChanged(MapItem item, String field) {

            if(!field.equals("milsym"))
                return;

            final String type = item.getType();

            String m2525 = CotUtils.mil2525cFromCotType(type).toUpperCase();
            if (m2525.isEmpty()) {
                setVisibility(View.GONE);
                return;
            } else {
                setVisibility(View.VISIBLE);
            }

            ISymbologyProvider symbology = ATAKUtilities
                    .getDefaultSymbologyProvider();
            Collection<Modifier> modifiers = symbology
                    .getModifiers(m2525);
            AttributeSet metadata = MarshalManager.marshal(item,
                    MetaDataHolder2.class, AttributeSet.class);
            for (String key : metadata.getAttributeNames()) {
                if (key.startsWith(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX)) {
                    String modifierId = key.replace(
                            MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX,
                            "");
                    boolean modifierMatch = false;
                    for (Modifier modifier : modifiers) {
                        if (modifier.getId().equals(modifierId)) {
                            modifierMatch = true;
                            break;
                        }
                    }
                    if (!modifierMatch) {
                        item.removeMetaData(
                                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                        + modifierId);
                    }
                }
            }

            item.persist(mapView.getMapEventDispatcher(), null,
                    this.getClass());
        }
    };

    public static class Builder {
        final MapView _mapView;
        final LinearLayout _linearLayout;
        final MapItem _item;

        /**
         * Given a map item, create the appropriate ModifierInfoView
         * @param mapView the mapView
         * @param linearLayout the linear layout to populate
         * @param item the item to back the view
         */
        public Builder(MapView mapView, LinearLayout linearLayout, MapItem item) {
            _mapView = mapView;
            _linearLayout = linearLayout;
            _item = item;
        }

        public Builder addModifierView(Modifier modifier) {
            final Class<?> parsedValueType = (modifier.getParsedValueType() != null) ?
                    modifier.getParsedValueType() :
                    String.class;

            if (parsedValueType.equals(Boolean.class)) {
                for(View v : createBooleanModifierView(_mapView, _item, modifier))
                    _linearLayout.addView(v);
            } else if (parsedValueType.equals(Enum.class)) {
                for(View v : createEnumModifierView(_mapView, _item, modifier))
                    _linearLayout.addView(v);
            } else if (parsedValueType.equals(Date.class)) {
                for(View v : createDateTimeModifierView(_mapView, _item, modifier))
                    _linearLayout.addView(v);
            } else {
                // default handler is text view; accounts for number input
                for(View v : createTextModifierView(_mapView, _item, modifier))
                    _linearLayout.addView(v);
            }

            return this;
        }

        public LinearLayout build() {
            GenericDetailsView.addEditTextPrompts(_linearLayout);
            return _linearLayout;
        }
    }

    private static <T> T[] ensureValues(T[] array, int capacity, T defaultValue) {
        if(array.length < capacity) {
            array = (T[])Arrays.copyOf(array, capacity, array.getClass());
        }
        for(int i = 0; i < array.length; i++)
            if(array[i] == null)
                array[i] = defaultValue;
        return array;
    }

    private static View[] createBooleanModifierView(final MapView mapView, final MapItem item, final Modifier modifier) {
        View[] views = new View[modifier.getNumFields()];
        final Context context = mapView.getContext();
        final BooleanModifierHandler handler = new BooleanModifierHandler(
                item, modifier, mapView.getMapEventDispatcher());
        String[] initValues = ensureValues(item.getMetaString(
                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                        + modifier.getId(),
                "false").split(","), modifier.getNumFields(), "false");
        for(int i = 0; i < modifier.getNumFields(); i++) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setTextColor(context.getResources()
                    .getColor(R.color.heading_yellow));
            checkBox.setTextSize(context.getResources()
                    .getDimension(R.dimen.draper_font)
                    / context.getResources()
                    .getDisplayMetrics().density);
            checkBox.setText(modifier.getName(i));
            final boolean attributeValue = initValues[i].equals("true");
            checkBox.setChecked(attributeValue);
            checkBox.setOnClickListener(handler);
            handler.addInputView(checkBox);
            views[i] = checkBox;
        }
        return views;
    }

    private static View[] createEnumModifierView(final MapView mapView, final MapItem item, final Modifier modifier) {
        String[] enumOptions = MODIFIER_ENUM.get(modifier.getId());
        if(enumOptions == null) {
            // marked as enum, but values not known. allow free-text entry
            return createTextModifierView(mapView, item, modifier);
        }

        final Context context = mapView.getContext();

        View[] views = new View[modifier.getNumFields()*2];
        final EnumModifierHandler inputHandler = new EnumModifierHandler(item, modifier,
                mapView.getMapEventDispatcher());
        final String[] attributeValues = ensureValues(item.getMetaString(
                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                        + modifier.getId(),
                "").split(","), modifier.getNumFields(), "");
        for(int i = 0; i < modifier.getNumFields(); i++) {
            views[i*2] = getTitleView(context, modifier.getName(i));
            Spinner spinner = new Spinner(mapView.getContext());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    mapView.getContext(),
                    android.R.layout.simple_spinner_item, enumOptions);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setBackground(
                    context.getDrawable(R.drawable.btn_gray));
            if (!attributeValues[i].isEmpty()) {
                for (int n = 0; n < enumOptions.length; n++) {
                    if (attributeValues[i].equals(enumOptions[n])) {
                        spinner.setSelection(n);
                        break;
                    }
                }
            }

            spinner.setOnTouchListener(inputHandler);
            spinner.setOnItemSelectedListener(inputHandler);
            inputHandler.addInputView(spinner);
            views[(i*2)+1] = spinner;
        }
        return views;
    }

    private static View[] createTextModifierView(final MapView mapView, final MapItem item, final Modifier modifier) {
        if(modifier.getName().toLowerCase(Locale.US).contains("altitude")) {
            return createAltitudeModifierView(mapView, item, modifier);
        }

        final Context context = mapView.getContext();
        final ModifierTextWatcher handler = new ModifierTextWatcher(item, modifier,
                mapView.getMapEventDispatcher());
        final int numFields = modifier.getNumFields();
        View[] views = new View[modifier.getNumFields() * 2];
        final String[] attributeValues = ensureValues(item.getMetaString(
                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                        + modifier.getId(),
                "").split(","), modifier.getNumFields(), "");
        for(int i = 0; i < numFields; i++) {
            views[(i*2)] = getTitleView(context, modifier.getName(i));
            EditText editText = new EditText(mapView.getContext(), null,
                    0, R.style.darkButton);
            editText.setContentDescription(modifier.getName());

            int padding = getPaddingInPxFromDp(mapView,6);
            editText.setPadding(padding, padding, padding, padding);
            if (modifier.getParsedValueType() != null && Number.class.isAssignableFrom(modifier.getParsedValueType())) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
            }
            editText.setEnabled(true);
            editText.setFocusable(true);
            editText.setVisibility(VISIBLE);
            editText.setText(attributeValues[i]);
            editText.addTextChangedListener(handler);
            handler.addInputView(editText);
            views[(i*2)+1] = editText;
        }
        return views;
    }

    private static View[] createAltitudeModifierView(final MapView mapView, final MapItem item, final Modifier modifier) {
        View[] views = new View[modifier.getNumFields()*2];
        final Context context = mapView.getContext();
        final UnitPreferences unitPrefs = new UnitPreferences(context);

        boolean agl =  unitPrefs.get("alt_display_agl", false);
        final String altitudeUnits = unitPrefs.getAltitudeUnits().getPlural();
        final String altitudeReference = (agl ? "AGL" : unitPrefs.getAltitudeReference());
        final String altRefUnitsLabel = unitPrefs.getAltitudeUnits().getAbbrev() + " "
                + altitudeReference;

        final IGeoPoint[] controlPoints = (item instanceof Shape) ?
                MarshalManager.marshal(((Shape)item).getPoints(), GeoPoint[].class,
                        IGeoPoint[].class) :
                (item instanceof PointMapItem) ?
                    new IGeoPoint[] {
                            MarshalManager.marshal(((PointMapItem)item).getPoint(), GeoPoint.class,
                                    IGeoPoint.class)
                    } :
                null;

        final String[] attributeValues = ensureValues(item.getMetaString(
                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                        + modifier.getId(),
                "").split(","), modifier.getNumFields(), "");

        final ArrayList<EditText> valueEditors = new ArrayList<>(modifier.getNumFields());
        final AfterTextChangedWatcher handler = new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String[] values = new String[valueEditors.size()];
                String defaultValue = null;
                for(int i = valueEditors.size()-1; i >= 0; i--) {
                    String alt = valueEditors.get(i).getText().toString();
                    final boolean empty = alt.isEmpty();
                    if(!empty) {
                        defaultValue = "0";
                    } else {
                        alt = defaultValue;
                    }
                    if(alt != null)
                        values[i] = String.valueOf(displayAltToMSLMeters(unitPrefs,
                                Parsers.parseDouble(alt, 0.0), agl, controlPoints));
                }

                StringBuilder value = new StringBuilder();
                if(values[0] != null) {
                    value.append(values[0]);
                    for(int i = 1; i < values.length; i++) {
                        if(values[i] == null)
                            break;
                        value.append(',');
                        value.append(values[i]);
                    }
                }

                // update
                item.setMetaString("milsym.altitudeReference", altitudeReference);
                item.setMetaString("milsym.altitudeUnits", altitudeUnits);
                item.setMetaString(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + modifier.getId(),
                        value.toString());
                // dispatch the update
                incrementModifierVersion(mapView.getMapEventDispatcher(), item);
            }
        };

        for(int i = 0; i < modifier.getNumFields(); i++) {
            TextView text = new TextView(context);
            text.setTextColor(context.getResources()
                    .getColor(R.color.heading_yellow));
            text.setTextSize(context.getResources()
                    .getDimension(R.dimen.draper_font)
                    / context.getResources().getDisplayMetrics().density);
            text.setText(modifier.getName(i) + " " + altRefUnitsLabel);
            views[i*2] = text;

            EditText editText = new EditText(context, null, 0, R.style.darkButton);
            editText.setContentDescription(modifier.getName() + " " + altRefUnitsLabel);

            int padding = getPaddingInPxFromDp(mapView,6);
            editText.setPadding(padding, padding, padding, padding);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_FLAG_SIGNED);

            // convert attribute value to presentation reference+units
            double d1 = Parsers.parseDouble(attributeValues[i], Double.NaN);
            d1 = displayAltFromMSLMeters(unitPrefs, d1, agl, controlPoints);

            if (!Double.isNaN(d1))
                editText.setText(String.format(Locale.US, "%d",
                        (int) Math.round(d1)));
            else
                editText.setText("");

            editText.addTextChangedListener(handler);
            valueEditors.add(editText);
            views[(i*2)+1] = editText;
        }

        return views;
    }

    private static View[] createDateTimeModifierView(final MapView mapView, final MapItem item, final Modifier modifier) {
        // XXX - given the complexity associated with managing multiple datetime pickers, punting
        //       to generic text entry for the time being
        if(modifier.getNumFields() != 1) {
            return createTextModifierView(mapView, item, modifier);
        }

        View[] views = new View[2];

        final Context context = mapView.getContext();

        views[0] = getTitleView(context, modifier.getName());
        View calendarClearView = LayoutInflater.from(context)
                .inflate(R.layout.calendar_clear, null);
        Button calendarButton = calendarClearView
                .findViewById(R.id.calendarButton);
        Button clearButton = calendarClearView
                .findViewById(R.id.clearButton);
        String current = item.getMetaString(
                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                        + modifier.getId(),
                "");
        calendarButton.setText(current.toUpperCase(Locale.US));
        views[1] = calendarClearView;
        calendarButton
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeZone(
                                TimeZone.getTimeZone("UTC"));
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        String initialDate = item.getMetaString(
                                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                        + modifier.getId(),
                                "");

                        Date date;
                        if (!FileSystemUtils.isEmpty(initialDate)) {
                            try {
                                date = dateFormatter
                                        .parse(initialDate);
                                cal.setTime(date);
                            } catch (Exception ignored) {
                            }
                        }
                        long searchStart = cal.getTime().getTime();

                        DatePickerFragment startFragment = new DatePickerFragment();
                        startFragment.init(searchStart,
                                new DatePickerFragment.DatePickerListener() {
                                    @Override
                                    public void onDatePicked(
                                            int year,
                                            int month, int day) {
                                        Calendar time = Calendar
                                                .getInstance(
                                                        TimeZone.getTimeZone(
                                                                "UTC"));
                                        time.set(year, month, day);

                                        TimePickerFragment timePickerFragment = new TimePickerFragment();
                                        timePickerFragment.init(
                                                time.getTime()
                                                        .getTime(),
                                                new TimePickerFragment.TimePickerListener() {
                                                    @Override
                                                    public void onTimePicked(
                                                            int hourOfDay,
                                                            int minute) {
                                                        time.set(
                                                                Calendar.HOUR_OF_DAY,
                                                                hourOfDay);
                                                        time.set(
                                                                Calendar.MINUTE,
                                                                minute);
                                                        String timeString = dateFormatter
                                                                .format(time
                                                                        .getTime());
                                                        calendarButton
                                                                .setText(
                                                                        timeString
                                                                                .toUpperCase(
                                                                                        Locale.US));
                                                        item.setMetaString(
                                                                MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                                                        + modifier.getId(),
                                                                timeString
                                                                        .toUpperCase(
                                                                                Locale.US));
                                                        incrementModifierVersion(
                                                                mapView.getMapEventDispatcher(),
                                                                item);

                                                    }
                                                }, true);
                                        timePickerFragment.show(
                                                ((Activity) mapView
                                                        .getContext())
                                                        .getFragmentManager(),
                                                "startTimePicker");
                                    }
                                },
                                0, 0);
                        startFragment.show(
                                ((Activity) mapView.getContext())
                                        .getFragmentManager(),
                                "startDatePicker");
                    }
                });
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                item.removeMetaData(
                        MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                                + modifier.getId());
                incrementModifierVersion(mapView.getMapEventDispatcher(), item);
                calendarButton.setText("");
            }
        });

        return views;
    }

    private static IGeoPoint getControlPoint(IGeoPoint[]controlPoints) {
        if(controlPoints == null || controlPoints.length < 1)
            return null;

        if(controlPoints.length == 1)
            return controlPoints[0];

        int pointCount = controlPoints.length;
        if(controlPoints[0].getLatitude() == controlPoints[pointCount-1].getLatitude()
                && controlPoints[0].getLongitude() == controlPoints[pointCount-1].getLongitude()) {

            pointCount--;
        }

        return GeoCalculations.computeAverage(controlPoints, 0, pointCount, true);
    }

    static double displayAltToMSLMeters(UnitPreferences unitPrefs, double displayAlt, boolean agl, IGeoPoint[]controlPoints) {
        double mslMeters = SpanUtilities.convert(displayAlt, unitPrefs.getAltitudeUnits(), Span.METER);
        final boolean hae = Objects.equals("HAE", unitPrefs.getAltitudeReference());
        if (hae || agl) {
            // need reference point if HAE or AGL
            final IGeoPoint referencePoint = getControlPoint(controlPoints);
            if (referencePoint == null)
                return Double.NaN;
            if(agl) {
                double elevation = ElevationManager.getElevation(
                        referencePoint.getLatitude(), referencePoint.getLongitude(), null);
                // getElevation returns hae - need to turn that into msl
                elevation = GeoCalculations.haeToMsl(referencePoint.getLatitude(), referencePoint.getLongitude(), elevation);

                mslMeters += elevation;
            }
            // HAE -> MSL
            if(hae) {
                mslMeters = GeoCalculations.haeToMsl(
                        referencePoint.getLatitude(), referencePoint.getLongitude(), mslMeters);
            }
        }

        return mslMeters;
    }

    private static double displayAltFromMSLMeters(UnitPreferences unitPrefs, double mslMeters, boolean agl, IGeoPoint[] controlPoints) {
        double displayAlt = mslMeters;
        final boolean hae = Objects.equals("HAE", unitPrefs.getAltitudeReference());
        if (hae || agl) {
            // need reference point if HAE or AGL
            final IGeoPoint referencePoint = getControlPoint(controlPoints);
            if (referencePoint == null)
                return Double.NaN;
            if(agl) {
                double elevation = ElevationManager.getElevation(
                        referencePoint.getLatitude(), referencePoint.getLongitude(), null);
                // convert local height to MSL for AGL calculation
                elevation = GeoCalculations.haeToMsl(
                        referencePoint.getLatitude(), referencePoint.getLongitude(), elevation);
                displayAlt -= elevation;
            } else { // HAE
                displayAlt = GeoCalculations.mslToHae(
                        referencePoint.getLatitude(), referencePoint.getLongitude(), displayAlt);
            }
        }

        return SpanUtilities.convert(displayAlt, Span.METER, unitPrefs.getAltitudeUnits());
    }

    private  static int getPaddingInPxFromDp(MapView mapView, int dp) {
        final float scale = mapView.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
