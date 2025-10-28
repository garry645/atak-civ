
package com.atakmap.android.gui.coordinateentry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.elev.ElevationChooserDialog;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.gui.DragLinearLayout;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationManager;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.util.Disposable;
import gov.tak.platform.lang.Parsers;

public class CoordinateEntryCapability
        implements View.OnClickListener, Disposable,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final Map<String, CoordinateEntryPane> coordinateEntryPanes = new HashMap<>();

    private static final String COORDINATE_ENTRY_PANE_PRIORITY_PREFERENCE_PREFIX = "coordinateentrypane_entry.";

    private static final String DEFAULT_COORDINATE_ENTRY_TABS_PREF = "coordinate_entry_tabs";
    private static final String COORDINATE_ENTRY_TABS_LAST_SELECTED_PREF = "coordinate_entry_last_selected";

    private static final String FORCE_PORTRAIT_LAYOUT = "coord_entry_capability_force_portrait";

    private final static String TAG = "CoordinateEntryCapability";

    private static final DecimalFormat NO_DEC_FORMAT = LocaleUtil
            .getDecimalFormat("##0");
    private static final DecimalFormat ONE_DEC_FORMAT = LocaleUtil
            .getDecimalFormat(
                    "##0.0");
    private static final DecimalFormat TWO_DEC_FORMAT = LocaleUtil
            .getDecimalFormat(
                    "##0.00");

    private static final ElevationManager.QueryParameters DTED_FILTER = new ElevationManager.QueryParameters();
    static {
        DTED_FILTER.types = new HashSet<>(Arrays.asList(
                "DTED",
                "DTED0",
                "DTED1",
                "DTED2",
                "DTED3"));
    }

    private final View panel;
    private final Context context;
    private AlertDialog dialog;

    private final AtakPreferences _prefs;
    private final UnitPreferences _unitPrefs;

    private final DragLinearLayout tabs;
    private final FrameLayout coordEntryHolderPortrait;
    private final FrameLayout coordEntryHolderLandscape;

    private static CoordinateEntryCapability _instance;

    private CoordinateEntryPane currentPane;
    private final FrameLayout currentPaneFrame;

    private boolean forcePortraitLayout = true;

    // common tools for the entry panel
    private final Button _dtedButton;
    private final Button _clearButton;
    private final Button _autofill;
    private final ImageButton _copyButton;
    private final EditText _elevText;
    private final TextView _elevSource;
    private final RadioGroup _affiliationGroup;

    private double altitudeMSL = 0.0d; // altitude in feet
    private String altitudeSrc = GeoPointMetaData.UNKNOWN;

    // the localized values used when the dialog is shown.
    private GeoPointMetaData _mapCenter;
    private GeoPointMetaData _originalPoint;
    private GeoPointMetaData _currentPoint;
    private boolean _editable;
    private boolean _displayElevationControls;

    private boolean isPortrait;

    private final TextView _elevUnits;

    public interface ResultCallback {
        /**
         * Returns the new GeoPoint if resolved and also contains a map of Strings and Values that
         * might be specific to the information set as part of a specific pane.   The desire is to
         * set this as part of hte name values in the MapItem
         * @param pane the well known name for the pane that the user was on when the dialog was dismissed.
         * @param point the point that was entered into the pane
         * @param suggestedAffiliation the suggested affiliation that the user has selected if
         *                             the affiliation option is checked.   The suggested affiliation
         *                             is to be used with the goto tool.
         */
        void onResultCallback(String pane, GeoPointMetaData point,
                String suggestedAffiliation);
    }

    public interface ResultCallback2 {
        /**
         * Returns the new GeoPoint if resolved and also contains a map of Strings and Values that
         * might be specific to the information set as part of a specific pane.   The desire is to
         * set this as part of hte name values in the MapItem
         * @param paneId the pane identifier for the pane that the user was on when the dialog was dismissed.
         * @param pane the well known name for the pane that the user was on when the dialog was dismissed.
         * @param point the point that was entered into the pane
         * @param suggestedAffiliation the suggested affiliation that the user has selected if
         *                             the affiliation option is checked.   The suggested affiliation
         *                             is to be used with the goto tool.
         */
        void onResultCallback(String paneId, String pane,
                GeoPointMetaData point,
                String suggestedAffiliation);
    }

    /**
     * Register the coordinate entry pane with the coordinate entry panel.
     * @param pane the coordinate entry pane
     */
    public synchronized void registerPane(final CoordinateEntryPane pane) {
        unregisterPane(pane);
        coordinateEntryPanes.put(pane.getUID(), pane);
        View v = LayoutInflater.from(context)
                .inflate(R.layout.coordinate_entry_item, null);
        ((TextView) (v.findViewById(R.id.coordinate_entry_item_name)))
                .setText(pane.getName());
        v.setTag(pane.getUID());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                GeoPointMetaData gp = _currentPoint;

                if (currentPane != null) {
                    try {
                        gp = currentPane.getGeoPointMetaData();
                    } catch (CoordinateEntryPane.CoordinateException coordinateException) {
                        handleCoordinateException(coordinateException);
                    }
                }
                currentPane = pane;
                for (int i = 0; i < tabs.getChildCount(); ++i) {
                    View tab = tabs.getChildAt(i);
                    TextView tv = tab
                            .findViewById(R.id.coordinate_entry_item_name);
                    tv.setTextColor(
                            context.getResources().getColor(R.color.white));
                    tv.setTypeface(null, Typeface.NORMAL);
                    tab.findViewById(R.id.landscape_selected)
                            .setVisibility(View.INVISIBLE);
                    tab.findViewById(R.id.portrait_selected)
                            .setVisibility(View.INVISIBLE);
                }
                TextView tv = v.findViewById(R.id.coordinate_entry_item_name);
                tv.setTextColor(context.getResources().getColor(R.color.green));
                tv.setTypeface(null, Typeface.BOLD);

                if (forcePortraitLayout || isPortrait) {
                    v.findViewById(R.id.portrait_selected)
                            .setVisibility(View.VISIBLE);
                } else {
                    v.findViewById(R.id.landscape_selected)
                            .setVisibility(View.VISIBLE);
                }

                currentPaneFrame.removeAllViews();
                currentPaneFrame.addView(pane.getView());
                pane.setOnChangedListener(onChangedListener);
                pane.onActivate(gp, _editable);
            }
        });
        v.setLayoutParams(new LinearLayout.LayoutParams(250,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tabs.addDragView(v, v.findViewById(R.id.coordinate_entry_item_handle),
                computeLocation(pane));
    }

    /**
     * Unregiste the coordinate entry pane with the coordinate entry panel.
     * @param pane the pane
     */
    public synchronized void unregisterPane(CoordinateEntryPane pane) {
        coordinateEntryPanes.remove(pane.getUID());
        pane.setOnChangedListener(null);
        for (int i = 0; i < tabs.getChildCount(); ++i) {
            View v = tabs.getChildAt(i);
            if (pane.getUID().equals(v.getTag())) {
                panel.post(new Runnable() {
                    @Override
                    public void run() {
                        tabs.removeDragView(v);
                    }
                });
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String s) {
        if (s == null || !s.equals(DEFAULT_COORDINATE_ENTRY_TABS_PREF))
            return;

        Set<String> arr = sharedPreferences.getStringSet(s, null);

        List<String> systemSupplied = Arrays.asList(context.getResources()
                .getStringArray(R.array.coordinate_entry_values));

        if (arr == null)
            return;

        for (int i = 0; i < tabs.getChildCount(); ++i) {
            final View v = tabs.getChildAt(i);
            final String tabid = (String) v.getTag();
            if (!systemSupplied.contains(tabid)) {
                // not one of the system supplied tabs, allow it to remain visible
                v.setVisibility(View.VISIBLE);
            } else if (!arr.contains(tabid)) {
                // system supplied tab that is not in the supplied array
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    private CoordinateEntryCapability(Context ctx) {
        context = ctx;
        panel = LayoutInflater.from(ctx).inflate(R.layout.coordinate_panel,
                null);
        _prefs = AtakPreferences.getInstance(ctx);
        _unitPrefs = new UnitPreferences(ctx);

        tabs = panel.findViewById(R.id.coordEntryList);

        tabs.setOnViewSwapListener(
                new DragLinearLayout.OnViewSwapListener() {
                    @Override
                    public void onSwap(View firstView, int firstPosition,
                            View secondView, int secondPosition) {

                        String firstUid = (String) firstView.getTag();
                        String secondUid = (String) secondView.getTag();
                        if (firstUid != null && secondUid != null) {
                            _prefs.set(
                                    COORDINATE_ENTRY_PANE_PRIORITY_PREFERENCE_PREFIX
                                            + firstUid,
                                    secondPosition);
                            _prefs.set(
                                    COORDINATE_ENTRY_PANE_PRIORITY_PREFERENCE_PREFIX
                                            + secondUid,
                                    firstPosition);
                        }
                    }
                });

        coordEntryHolderPortrait = panel
                .findViewById(R.id.coordEntryHolderPortrait);
        coordEntryHolderLandscape = panel
                .findViewById(R.id.coordEntryHolderLandscape);

        currentPaneFrame = panel.findViewById(R.id.currentCoordPane);

        _dtedButton = panel.findViewById(R.id.coordDialogDtedButton);
        _dtedButton.setOnClickListener(this);

        _clearButton = panel.findViewById(R.id.clearButton);
        _clearButton.setOnClickListener(this);

        _copyButton = panel.findViewById(R.id.copyButton);
        _copyButton.setOnClickListener(this);

        _autofill = panel.findViewById(R.id.coordDialogMGRSGridButton);
        _autofill.setOnClickListener(this);

        _elevSource = panel.findViewById(R.id.coordDialogElevationSource);

        _elevText = panel.findViewById(R.id.coordDialogElevationText);
        AbstractPane.setSelectAllOnFocus(_elevText);

        _elevUnits = panel.findViewById(R.id.coordDialogElevationUnits);

        _elevText.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                try {
                    String txt = _elevText.getText().toString().trim();

                    double userEnteredMsl = Double.parseDouble(txt);

                    if (!useAltitudeFeet()) {
                        userEnteredMsl = SpanUtilities.convert(userEnteredMsl,
                                Span.METER, Span.FOOT);
                    }

                    if (Double.compare(Math.round(userEnteredMsl),
                            Math.round(altitudeMSL)) != 0) {
                        altitudeMSL = userEnteredMsl;
                        altitudeSrc = GeoPointMetaData.USER;

                        _elevSource.setText(altitudeSrc);
                    }
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }
        });

        _affiliationGroup = panel.findViewById(R.id.affiliationGroup);
        _affiliationGroup
                .check(_prefs.get("affiliation_group_coordview", 0));

        _affiliationGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group,
                            int checkedId) {
                        _affiliationGroup.clearCheck();
                        _affiliationGroup.check(checkedId);
                        _prefs.set("affiliation_group_coordview",
                                checkedId);

                    }
                });

        registerPane(new MGRSPane(ctx));
        registerPane(new DDPane(ctx));
        registerPane(new DMPane(ctx));
        registerPane(new DMSPane(ctx));
        registerPane(new UTMPane(ctx));
        registerPane(new AddrPane(ctx));

        onSharedPreferenceChanged(_prefs.getSharedPrefs(),
                DEFAULT_COORDINATE_ENTRY_TABS_PREF);
        _prefs.registerListener(this);

    }

    /**
     * Returns the singleton instance of the CoordinateEntryCapability
     * @param ctx the context to use
     * @return the coordinate entry panel
     */
    public synchronized static CoordinateEntryCapability getInstance(
            Context ctx) {

        if (_instance == null) {
            _instance = new CoordinateEntryCapability(ctx);
        }

        return _instance;
    }

    /**
     * Show a dialog with the appropriate view and must be run on the ui thread
     * @param title the title of the dialog box
     * @param originalPoint the point that is currently to be used in each of the panes
     * @param editable if the screens are editable or for display purposes only.
     * @param mapCenter the center of the map when the dialog was shown
     * @param coordinateFormat the coordinate format used for selecting the pane to show, null will
     *                         show the last user selected pane.
     * @param showAffiliation if it is desired to show the affiliation selector at the bottom of the screen.
     * @param callback the callback that is used when the dialog is dismissed
     */
    public synchronized void showDialog(String title,
            GeoPointMetaData originalPoint,
            boolean editable,
            GeoPointMetaData mapCenter,
            CoordinateFormat coordinateFormat,
            boolean showAffiliation,
            ResultCallback callback) {
        showDialog(title, originalPoint, editable, mapCenter,
                coordinateFormat != null
                        ? findId(coordinateFormat.getDisplayName())
                        : null,
                showAffiliation, new ResultCallback2() {
                    @Override
                    public void onResultCallback(String paneId, String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {

                        if (MetricsApi.shouldRecordMetric()) {
                            Bundle bundle = new Bundle();
                            bundle.putString("paneId", paneId);
                            bundle.putString("point", point.toString());
                            bundle.putString("suggestedAffiliation",
                                    suggestedAffiliation);
                            bundle.putString(MetricsUtils.FIELD_INFO, "closed");
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "CoordinateEntryCapability", bundle);
                        }

                        if (callback != null)
                            callback.onResultCallback(pane, point,
                                    suggestedAffiliation);
                    }
                });
    }

    /**
     * Show a dialog with the appropriate view and must be run on the ui thread
     * @param title the title of the dialog box
     * @param originalPoint the point that is currently to be used in each of the panes
     * @param editable if the screens are editable or for display purposes only.
     * @param mapCenter the center of the map when the dialog was shown
     * @param paneid the pane identifier that should be presented, null uses the last user
     *             selected pane identifer
     * @param showAffiliation if it is desired to show the affiliation selector at the bottom of the screen.
     * @param callback the callback that is used when the dialog is dismissed
     */
    public synchronized void showDialog(String title,
            GeoPointMetaData originalPoint,
            boolean editable,
            GeoPointMetaData mapCenter,
            String paneid,
            boolean showAffiliation,
            ResultCallback2 callback) {
        this.showDialog(title, originalPoint, editable, true, mapCenter, paneid,
                showAffiliation, callback);
    }

    /**
     * Show a dialog with the appropriate view and must be run on the ui thread
     * @param title the title of the dialog box
     * @param originalPoint the point that is currently to be used in each of the panes
     * @param editable if the screens are editable or for display purposes only.
     * @param displayElevationControls if the elevation controls should be shown
     * @param mapCenter the center of the map when the dialog was shown
     * @param paneid the pane identifier that should be presented, null uses the last user
     *             selected pane identifer
     * @param showAffiliation if it is desired to show the affiliation selector at the bottom of the screen.
     * @param callback the callback that is used when the dialog is dismissed
     */
    public synchronized void showDialog(String title,
            GeoPointMetaData originalPoint,
            boolean editable,
            boolean displayElevationControls,
            GeoPointMetaData mapCenter,
            String paneid,
            boolean showAffiliation,
            ResultCallback2 callback) {

        if (dialog != null) {
            dialog.cancel();
        }

        try {
            final ViewParent parent = panel.getParent();
            if (parent instanceof ViewGroup)
                ((ViewGroup) parent).removeView(panel);
        } catch (Exception ignore) {
        }

        isPortrait = MapView.getMapView().isPortrait();

        currentPaneFrame.removeAllViews();

        if (getVisibleTabsCount() == 0) {
            Toast.makeText(context,
                    "There are no coordinate Entry Tabs Enabled or supplied by plugins",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // force the layout of the tabs in portrait mode only (true by default)
        forcePortraitLayout = _prefs.get(FORCE_PORTRAIT_LAYOUT, true);

        _originalPoint = originalPoint;

        // make sure the current pane is null so it does not carry over a previous pane
        currentPane = null;

        if (_originalPoint != null
                && !Double.isNaN(_originalPoint.get().getAltitude())) {
            double altFtMsl = SpanUtilities.convert(
                    EGM96.getMSL(originalPoint.get()), Span.METER, Span.FOOT);
            altitudeSrc = _originalPoint.getAltitudeSource();
            altitudeMSL = altFtMsl;

            // correct the altitude feet / meters display
            _elevUnits.setText(
                    useAltitudeFeet() ? context.getString(R.string.ft_msl5)
                            : "m msl");
            if (!useAltitudeFeet())
                _elevText.setText(_formatElevationMeters(
                        EGM96.getMSL(originalPoint.get())));
            else
                _elevText.setText(_formatElevation(altFtMsl));

        } else {
            altitudeSrc = "";
            altitudeMSL = Double.NaN;
            _elevUnits.setText(
                    useAltitudeFeet() ? context.getString(R.string.ft_msl5)
                            : "m msl");
            _elevText.setText("");
        }

        _elevSource.setText(altitudeSrc);

        _elevText.setEnabled(editable);
        _elevText.setTextColor(editable ? 0xFFFFFFFF : 0xFFCCCCCC);

        _dtedButton.setEnabled(editable);

        // the three components related to elevation
        _elevSource.setVisibility(
                displayElevationControls ? View.VISIBLE : View.INVISIBLE);
        _elevText.setVisibility(
                displayElevationControls ? View.VISIBLE : View.INVISIBLE);
        _dtedButton.setVisibility(
                displayElevationControls ? View.VISIBLE : View.INVISIBLE);

        _clearButton.setEnabled(editable);
        _autofill.setEnabled(editable);

        _mapCenter = mapCenter;
        _displayElevationControls = displayElevationControls;
        _editable = editable;
        _currentPoint = _originalPoint;

        _affiliationGroup.setOrientation(
                isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        _affiliationGroup
                .setVisibility(showAffiliation ? View.VISIBLE : View.INVISIBLE);

        AlertDialog.Builder b = new AlertDialog.Builder(context);

        if (!FileSystemUtils.isEmpty(title)) {
            final TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setGravity(Gravity.LEFT);
            titleView.setTextSize(28);
            titleView.setTextColor(
                    context.getResources().getColor(R.color.light_blue));
            b.setCustomTitle(titleView);
        }

        b.setView(panel);

        b.setPositiveButton(R.string.ok, null);
        b.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (MetricsApi.shouldRecordMetric()) {
                            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                    MetricsUtils.EVENT_WIDGET_STATE,
                                    "CoordinateEntryCapability",
                                    MetricsUtils.EVENT_STATUS_CANCELLED);
                        }
                    }
                });

        dialog = b.create();

        ((FrameLayout) tabs.getParent()).removeAllViews();

        if (forcePortraitLayout || isPortrait) {
            coordEntryHolderPortrait.addView(tabs);
            coordEntryHolderPortrait.setVisibility(View.VISIBLE);
            coordEntryHolderLandscape.setVisibility(View.GONE);
            panel.findViewById(R.id.portrait_divider)
                    .setVisibility(View.VISIBLE);
            panel.findViewById(R.id.landscape_divider).setVisibility(View.GONE);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            relayoutItems(true);
        } else {
            coordEntryHolderLandscape.addView(tabs);
            coordEntryHolderLandscape.setVisibility(View.VISIBLE);
            coordEntryHolderPortrait.setVisibility(View.GONE);
            panel.findViewById(R.id.portrait_divider).setVisibility(View.GONE);
            panel.findViewById(R.id.landscape_divider)
                    .setVisibility(View.VISIBLE);
            tabs.setOrientation(LinearLayout.VERTICAL);
            relayoutItems(false);
        }

        // make sure that the pane can be used during the next call to showDialog
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                ViewGroup parent = ((ViewGroup) panel.getParent());
                if (parent != null)
                    parent.removeView(panel);
            }
        });

        dialog.setCancelable(false);

        dialog.show();

        if (isPortrait)
            AlertDialogHelper.adjust(dialog, .95, .65);
        else
            AlertDialogHelper.adjust(dialog, .75, .95);

        setSelected(paneid);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (currentPane != null) {
                            GeoPointMetaData gpm = null;

                            try {
                                gpm = currentPane.getGeoPointMetaData();
                            } catch (CoordinateEntryPane.CoordinateException ignored) {
                                // no need to tell the user that this has had an issue a more generic
                                // message will be provided.
                            }

                            if (gpm == null) {
                                Log.e(TAG,
                                        "the coordinate could not be produced",
                                        new Exception());
                                Toast.makeText(context,
                                        R.string.please_correct_coordinate_entry,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final GeoPoint gp = gpm.get();
                            final double altitudeHAE = EGM96.getHAE(
                                    gp.getLatitude(),
                                    gp.getLongitude(),
                                    SpanUtilities.convert(altitudeMSL,
                                            Span.FOOT, Span.METER));
                            final GeoPoint ngp = new GeoPoint(gp.getLatitude(),
                                    gp.getLongitude(), altitudeHAE);
                            final GeoPointMetaData retVal = new GeoPointMetaData(
                                    ngp);

                            Map<String, Object> metadata = gpm.getMetaData();
                            for (String key : metadata.keySet())
                                retVal.setMetaValue(key, metadata.get(key));

                            // get the derivation value of the altitude
                            retVal.setAltitudeSource(altitudeSrc);

                            // since this came from the coordinate panel, mark the geopoint as user entered.
                            retVal.setGeoPointSource(GeoPointMetaData.USER);

                            if (callback != null) {

                                _unitPrefs.set(
                                        COORDINATE_ENTRY_TABS_LAST_SELECTED_PREF,
                                        currentPane.getUID());

                                if (MetricsApi.shouldRecordMetric()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("paneId",
                                            currentPane.getUID());
                                    bundle.putString("point",
                                            retVal.toString());
                                    bundle.putString("suggestedAffiliation",
                                            showAffiliation ? getDropPointType()
                                                    : null);
                                    bundle.putString(MetricsUtils.FIELD_INFO,
                                            "closed");
                                    MetricsUtils.record(
                                            MetricsUtils.CATEGORY_TOOL,
                                            MetricsUtils.EVENT_WIDGET_STATE,
                                            "CoordinateEntryCapability",
                                            bundle);
                                }

                                callback.onResultCallback(currentPane.getUID(),
                                        currentPane.getName(),
                                        retVal,
                                        showAffiliation ? getDropPointType()
                                                : null);
                            }

                            dialog.dismiss();
                        } else {
                            Log.e(TAG,
                                    "internal error occurred where the current pane is null");
                            dialog.dismiss();
                        }
                    }
                });

        if (MetricsApi.shouldRecordMetric()) {
            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                    MetricsUtils.EVENT_WIDGET_STATE,
                    "CoordinateEntryCapability", "shown");
        }

    }

    /**
     * Given a well known name, return the first panel id that is encountered that matches
     * the well known name.
     * @param wnn the well known name for a coordinate type which is case insensitive.
     * @return the unique identifier for the well known name or null if no well known name is found.
     */
    public String findId(String wnn) {
        if (wnn == null)
            return null;

        for (CoordinateEntryPane coordinateEntryPane : coordinateEntryPanes
                .values()) {
            if (coordinateEntryPane.getName().equalsIgnoreCase(wnn)) {
                return coordinateEntryPane.getUID();
            }
        }
        return null;
    }

    /**
     * Returns a well known name of a coordinate pane given the pane id.
     * @param paneid the pane id to get the well known name
     * @return returns a well known name or null if the panel id is not valid.
     */
    public String findWellKnownName(String paneid) {
        final CoordinateEntryPane pane = coordinateEntryPanes.get(paneid);
        if (pane != null)
            return pane.getName();
        return null;
    }

    /**
     * Given a coordinate entry panel id, convert the geopoint into a human readable
     * format for display to the user.
     * @param id the id
     * @param gp the geopoint
     * @return the human readable name.
     */
    public String format(final String id, final GeoPointMetaData gp) {
        CoordinateEntryPane p = coordinateEntryPanes.get(id);
        if (p != null)
            return p.format(gp);
        return null;
    }

    private void relayoutItems(boolean portait) {
        for (int i = 0; i < tabs.getChildCount(); ++i) {
            final View v = tabs.getChildAt(i);
            final View handle = v
                    .findViewById(R.id.coordinate_entry_item_handle);

            final FrameLayout.LayoutParams lp;
            ((FrameLayout) handle.getParent()).removeAllViews();
            if (portait) {
                v.findViewById(R.id.portrait_line).setVisibility(View.VISIBLE);
                v.findViewById(R.id.landscape_line).setVisibility(View.GONE);

                lp = new FrameLayout.LayoutParams(50, 35);
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                handle.setPadding(0, 10, 0, 10);
                handle.setLayoutParams(lp);
                ((FrameLayout) v.findViewById(R.id.portrait_handle_container))
                        .addView(handle);

            } else {
                v.findViewById(R.id.landscape_line).setVisibility(View.VISIBLE);
                v.findViewById(R.id.portrait_line).setVisibility(View.GONE);

                lp = new FrameLayout.LayoutParams(35, 50);
                lp.gravity = Gravity.CENTER_VERTICAL;
                handle.setLayoutParams(lp);
                handle.setPadding(10, 0, 10, 0);
                ((FrameLayout) v.findViewById(R.id.landscape_handle_container))
                        .addView(handle);
            }

        }
    }

    /**
     * Determine how many enabled tabs there are.
     * @return the number of visible tabs.
     */
    private int getVisibleTabsCount() {
        int count = 0;
        for (int i = 0; i < tabs.getChildCount(); ++i) {
            View v = tabs.getChildAt(i);
            if (v.getVisibility() == View.VISIBLE)
                count++;
        }
        return count;

    }

    /**
     * Attempt to select the current tab to the provided pane id.   However if the pane id is not
     * visible then it will revert to the last chosen pane.   Only as a last resort, it will show
     * the first encountered visible pane.
     * @param paneid the pane id to show
     */
    private void setSelected(String paneid) {

        final String lastPaneId = _unitPrefs.get(
                COORDINATE_ENTRY_TABS_LAST_SELECTED_PREF,
                null);

        // must be run after the panel is displayed in the alert dialog
        panel.post(new Runnable() {
            @Override
            public void run() {

                View firstVisiblePane = null;
                View lastPane = null;
                View identifiedPane = null;

                for (int i = 0; i < tabs.getChildCount()
                        && identifiedPane == null; ++i) {
                    final View v = tabs.getChildAt(i);
                    final CoordinateEntryPane pane = coordinateEntryPanes
                            .get((String) v.getTag());
                    if (pane != null && v.getVisibility() == View.VISIBLE) {
                        if (paneid != null && pane.getUID().equals(paneid)) {
                            identifiedPane = v;
                        } else if (firstVisiblePane == null) {
                            firstVisiblePane = v;
                        } else if (lastPaneId != null
                                && pane.getUID().equals(lastPaneId)) {
                            lastPane = v;
                        }
                    }
                }

                View preferredPane = identifiedPane;
                if (preferredPane == null)
                    preferredPane = lastPane;
                if (preferredPane == null)
                    preferredPane = firstVisiblePane;

                if (preferredPane != null) {
                    preferredPane.callOnClick();

                    if (isPortrait)
                        scrollToView(coordEntryHolderPortrait, preferredPane);
                    else
                        scrollToView(coordEntryHolderLandscape, preferredPane);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        // Pull from DTED
        if (v == _dtedButton)
            _pullElevation();

        // Clear coordinate
        else if (v == _clearButton)
            clear();

        // copy button
        else if (v == _copyButton)
            copy();
        else if (v == _autofill) {
            if (currentPane != null)
                currentPane.autofill(_mapCenter);
        }

    }

    private void copy() {
        try {
            if (currentPane != null) {
                String text = currentPane
                        .format(currentPane.getGeoPointMetaData());
                if (!FileSystemUtils.isEmpty(text)) {
                    String elev = _elevText.getText().toString().trim();
                    if (!elev.isEmpty()) {
                        text += " " + elev +
                                ((_elevUnits == null) ? "ft MSL"
                                        : _elevUnits.getText());
                    }

                    Log.d(TAG, "Copy location to clipboard: " + text);
                    ATAKUtilities.copyClipboard("location", text, true);
                } else {
                    Log.w(TAG, "Failed to copy location to clipboard");
                }
            }
        } catch (CoordinateEntryPane.CoordinateException ce) {
            handleCoordinateException(ce);
        }
    }

    private void clear() {
        _currentPoint = null;
        if (currentPane != null) {
            currentPane.onActivate(null, _editable);
        }
        if (_displayElevationControls) {
            altitudeSrc = "";
            altitudeMSL = Double.NaN;
            _elevText.setText("");
            _elevSource.setText("");
        }
    }

    private void _pullElevation() {
        try {
            final GeoPointMetaData point = currentPane.getGeoPointMetaData();
            if (point != null) {
                final String pullElevationMode = _prefs
                        .get("pull_elevation_mode", "best");
                if (pullElevationMode.equals("prompt")) {
                    // pull the elevation and make sure it is in MSL
                    ElevationChooserDialog.show(context,
                            point.get().getLatitude(),
                            point.get().getLongitude(),
                            null,
                            new ElevationChooserDialog.OnElevationSelectedListener() {

                                @Override
                                public void onElevationSelected(
                                        GeoPointMetaData selected) {
                                    _updateAltitude(selected);
                                }
                            },
                            null);
                } else {
                    ElevationManager.QueryParameters filter = pullElevationMode
                            .equals("dted") ? DTED_FILTER : null;
                    GeoPointMetaData altHAE = new GeoPointMetaData();
                    ElevationManager.getElevation(point.get(), filter, altHAE);
                    _updateAltitude(altHAE);
                }
            }
        } catch (CoordinateEntryPane.CoordinateException ce) {
            handleCoordinateException(ce);
        }
    }

    private void _updateAltitude(GeoPointMetaData altHAE) {
        if (altHAE != null && altHAE.get().isAltitudeValid()) {
            double alt = EGM96.getMSL(altHAE.get());
            double altFtMsl = SpanUtilities.convert(alt,
                    Span.METER, Span.FOOT);

            // correct the altitude feet / meters display
            _elevUnits.setText(useAltitudeFeet()
                    ? context.getString(R.string.ft_msl5)
                    : "m msl");
            if (!useAltitudeFeet())
                _elevText.setText(_formatElevationMeters(
                        EGM96.getMSL(altHAE.get())));
            else
                _elevText.setText(_formatElevation(altFtMsl));

            altitudeMSL = altFtMsl;
            altitudeSrc = altHAE.getAltitudeSource();
            _elevSource.setText(altitudeSrc);
        } else {
            Toast.makeText(
                    context,
                    context.getResources().getString(
                            R.string.goto_input_tip1),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static String _formatElevation(double e) {
        if (Math.abs(e) < 100) {
            return TWO_DEC_FORMAT.format(e);
        } else if (Math.abs(e) < 1000) {
            return ONE_DEC_FORMAT.format(e);
        } else {
            return NO_DEC_FORMAT.format(e);
        }
    }

    private String getDropPointType() {
        final int id = _affiliationGroup.getCheckedRadioButtonId();
        if (id == R.id.coordDialogDropUnknown)
            return "a-u-G";
        else if (id == R.id.coordDialogDropNeutral)
            return "a-n-G";
        else if (id == R.id.coordDialogDropHostile)
            return "a-h-G";
        else if (id == R.id.coordDialogDropFriendly)
            return "a-f-G";
        else if (id == R.id.coordDialogDropGeneric)
            return "b-m-p-s-m";
        else if (id == R.id.coordDialogMoveRedX)
            return "redx";
        else if (id == R.id.coordDialogMovePOI)
            return "poi";
        else if (id == R.id.coordDialogDropNothing)
            return null;
        else
            return null;
    }

    private int computeLocation(CoordinateEntryPane pane) {

        int idx = tabs.getChildCount() - 1;

        final int priority = _prefs
                .get(COORDINATE_ENTRY_PANE_PRIORITY_PREFERENCE_PREFIX
                        + pane.getUID(), idx + 1);

        for (int i = 0; i < tabs.getChildCount(); ++i) {
            final View v = tabs.getChildAt(i);
            final String tag = (String) v.getTag();
            int curr_priority = _prefs
                    .get(COORDINATE_ENTRY_PANE_PRIORITY_PREFERENCE_PREFIX
                            + tag, 0);
            if (priority <= curr_priority) {
                idx = i;
                break;
            } else {
                idx = i + 1;
            }
        }
        return idx;
    }

    @Override
    public void dispose() {
        for (CoordinateEntryPane pane : coordinateEntryPanes.values()) {
            unregisterPane(pane);
            pane.dispose();
            _prefs.unregisterListener(this);

        }
    }

    private void handleCoordinateException(
            CoordinateEntryPane.CoordinateException ce) {
        Log.e(TAG, ce.getMessage(), ce.getCause());
        Toast.makeText(context,
                ce.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    final CoordinateEntryPane.OnChangedListener onChangedListener = new CoordinateEntryPane.OnChangedListener() {
        @Override
        public void onChange(CoordinateEntryPane coordinateEntryPane) {
            //if (coordinateEntryPane == currentPane) {
            //    Log.d(TAG, "change occurred: " + currentPane + " " +
            //            currentPane.format(currentPane.getGeoPointMetaData()));
            //}

            try {
                // the point has been moved 
                GeoPointMetaData gpm = coordinateEntryPane
                        .getGeoPointMetaData();
                if (gpm != null && _currentPoint != null &&
                        gpm.get().distanceTo(_currentPoint.get()) > 1) {
                    altitudeSrc = GeoPointMetaData.USER;
                    _elevSource.setText(altitudeSrc);
                }
            } catch (CoordinateEntryPane.CoordinateException ignored) {
            }
        }
    };

    /**
     * Used to scroll to the given view.
     *
     * @param scrollViewParent Parent ScrollView
     * @param view View to which we need to scroll.
     */
    private void scrollToView(final FrameLayout scrollViewParent,
            final View view) {
        // Get deepChild Offset
        Point childOffset = new Point();
        getDeepChildOffset(scrollViewParent, view.getParent(), view,
                childOffset);
        // Scroll to child.
        if (scrollViewParent instanceof ScrollView)
            ((ScrollView) scrollViewParent).smoothScrollTo(0, childOffset.y);
        else if (scrollViewParent instanceof HorizontalScrollView)
            ((HorizontalScrollView) scrollViewParent)
                    .smoothScrollTo(childOffset.x, 0);
    }

    /**
     * Used to get deep child offset.
     * <p/>
     * 1. We need to scroll to child in scrollview, but the child may not the direct child to scrollview.
     * 2. So to get correct child position to scroll, we need to iterate through all of its parent views till the main parent.
     *
     * @param mainParent        Main Top parent.
     * @param parent            Parent.
     * @param child             Child.
     * @param accumulatedOffset Accumulated Offset.
     */
    private void getDeepChildOffset(final ViewGroup mainParent,
            final ViewParent parent, final View child,
            final Point accumulatedOffset) {
        accumulatedOffset.x += child.getLeft();
        accumulatedOffset.y += child.getTop();
        if (parent instanceof ViewGroup) {
            ViewGroup parentGroup = (ViewGroup) parent;
            if (parentGroup.equals(mainParent)) {
                return;
            }
            getDeepChildOffset(mainParent, parentGroup.getParent(), parentGroup,
                    accumulatedOffset);
        }
    }

    private boolean useAltitudeFeet() {
        return Parsers.parseInt(_prefs.get("alt_unit_pref", "0"), 0) != 1 ||
                _elevUnits == null;
    }

    private static String _formatElevationMeters(double e) {
        if (Math.abs(e) < 100 * ConversionFactors.METERS_TO_FEET) {
            return TWO_DEC_FORMAT.format(e);
        } else if (Math.abs(e) < 1000 * ConversionFactors.METERS_TO_FEET) {
            return ONE_DEC_FORMAT.format(e);
        } else {
            return NO_DEC_FORMAT.format(e);
        }
    }

}
