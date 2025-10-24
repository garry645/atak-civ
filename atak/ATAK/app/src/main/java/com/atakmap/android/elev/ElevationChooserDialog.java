
package com.atakmap.android.elev;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.atakmap.android.preference.AtakPreferences;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.feature.geometry.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class ElevationChooserDialog {

    private final static Comparator<ElevationSource> SOURCE_COMPARATOR = new Comparator<ElevationSource>() {
        @Override
        public int compare(ElevationSource a, ElevationSource b) {
            final int nameComp = a.getName().compareToIgnoreCase(b.getName());
            if (nameComp != 0)
                return nameComp;
            return a.hashCode() - b.hashCode();
        }
    };

    private final static Comparator<ElevationValue> VALUE_COMPARATOR = new Comparator<ElevationValue>() {
        @Override
        public int compare(ElevationValue a, ElevationValue b) {
            if (a.gsd < b.gsd)
                return -1;
            else if (a.gsd > b.gsd)
                return 1;
            else
                return a.source.compareToIgnoreCase(b.source);
        }
    };

    /**
     * Callback to return the elevation upon selection.
     */
    public interface OnElevationSelectedListener {
        /**
         * The resulting geopoint based on the elevation selection.
         * @param point the geopoint metadata
         */
        void onElevationSelected(GeoPointMetaData point);
    }

    private final static class ElevationValue {
        final double height;
        final double gsd;
        final String source;

        public ElevationValue(double height, double gsd, String source) {
            this.height = height;
            this.gsd = gsd;
            if (source == null)
                this.source = "unknown";
            else
                this.source = source;
        }
    }

    /**
     * Displays a dialog that allows the user to choose an elevation from
     * available elevation sources. The supplied callback is invoked with the
     * selected value. The callback <b>will not</b> be invoked if the dialog is
     * canceled.
     *
     * <P>The callback will be invoked without the dialog showing for the
     * following cases:
     * <UL>
     *     <LI>No elevation data is available at the selected location
     *     <LI>Only one source of valid elevation data is available at the
     *         selected location
     * </UL>
     *
     * @param appContext    The application context
     * @param latitude      The latitude of the coordinate
     * @param longitude     The longitude of the coordinate
     * @param filter        The elevation source filter (may be {@code null})
     * @param onSelect      The callback when an elevation is selected
     */
    public static void show(Context appContext, double latitude,
            double longitude, ElevationSource.QueryParameters filter,
            final OnElevationSelectedListener onSelect,
            final DialogInterface.OnClickListener onCancelDismiss) {

        Collection<ElevationSource> sources = new TreeSet<>(SOURCE_COMPARATOR);
        ElevationSourceManager.getSources(sources);
        if (sources.isEmpty()) {
            onSelect.onElevationSelected(null);
            return;
        }

        ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
        if (filter != null) {
            params.minResolution = filter.minResolution;
            params.maxResolution = filter.maxResolution;
            params.targetResolution = filter.targetResolution;
            params.minCE = filter.minCE;
            params.minLE = filter.minLE;
            params.authoritative = filter.authoritative;
            params.flags = filter.flags;
            params.types = filter.types;
            params.order = filter.order;
        }
        params.spatialFilter = new Point(longitude, latitude);

        final List<ElevationValue> values = new ArrayList<>(sources.size());
        for (ElevationSource source : sources) {
            try (ElevationSource.Cursor result = source.query(params)) {
                while (result.moveToNext()) {
                    ElevationChunk chunk = result.get();
                    if (chunk == null)
                        continue;
                    final double el = chunk.sample(latitude, longitude);
                    if (Double.isNaN(el))
                        continue;
                    ElevationValue value = new ElevationValue(el,
                            result.getResolution(),
                            result.getType());
                    values.add(value);
                    break;
                }
            }
        }
        if (values.isEmpty()) {
            onSelect.onElevationSelected(null);
            return;
        } else if (values.size() == 1) {
            ElevationValue value = values.get(0);
            onSelect.onElevationSelected(
                    GeoPointMetaData.wrap(
                            new GeoPoint(latitude, longitude, value.height),
                            null,
                            value.source));
            return;
        }

        Collections.sort(values, VALUE_COMPARATOR);

        final AtakPreferences prefs = AtakPreferences.getInstance(appContext);
        final String altRef = prefs.get("alt_display_pref", "MSL");
        final Span altUnits;
        switch (Parsers.parseInt(prefs.get("alt_unit_pref", "0"), 0)) {
            case 1:
                altUnits = Span.METER;
                break;
            case 0:
            default: // default to feet
                altUnits = Span.FOOT;
                break;
        }

        GeoPoint point = GeoPoint.createMutable();
        point.set(latitude, longitude);

        final String[] selectionOptions = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            final ElevationValue value = values.get(i);
            point.set(value.height);

            //just use fixed MSL or HAE based on prefs
            String altString = altRef.equals("HAE")
                    ? EGM96.formatHAE(point, altUnits)
                    : EGM96.formatMSL(point, altUnits);

            selectionOptions[i] = appContext.getString(
                    R.string.s_s_resolution_s,
                    altString,
                    value.source,
                    SpanUtilities.format(value.gsd, Span.METER));
        }

        AlertDialog.Builder b = new AlertDialog.Builder(appContext);
        b.setTitle(R.string.select_elevation_source);
        b.setSingleChoiceItems(selectionOptions, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int selectedIndex) {
                        if (selectedIndex < 0) {
                            if (onCancelDismiss != null)
                                onCancelDismiss.onClick(dialog, -1);
                            return;
                        }
                        final ElevationValue value = values.get(selectedIndex);
                        onSelect.onElevationSelected(
                                GeoPointMetaData.wrap(
                                        new GeoPoint(latitude, longitude,
                                                value.height),
                                        null,
                                        value.source));
                        dialog.dismiss();
                    }
                });
        b.setNegativeButton(R.string.cancel, onCancelDismiss);
        b.show();
    }
}
