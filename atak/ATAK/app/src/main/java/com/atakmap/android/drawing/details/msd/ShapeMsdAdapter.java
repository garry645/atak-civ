
package com.atakmap.android.drawing.details.msd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.gui.ColorButton;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.toolbars.UnitsArrayAdapter;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.util.Disposable;

public class ShapeMsdAdapter extends BaseAdapter implements
        SharedPreferences.OnSharedPreferenceChangeListener, Disposable {
    private final MapView _mapView;
    private final LayoutInflater mInflater;
    private final Context context;
    private final ArrayList<ShapeMsdEntry> shapeEntryArrayList = new ArrayList<>();
    private final AtakPreferences prefs;
    private final ConcurrentLinkedQueue<EntryChangedListener> entryChangedListeners = new ConcurrentLinkedQueue<>();
    private static final String SHAPE_MSD_ENTRY = "shape_msd_entry.";
    private final UnitPreferences _prefs;
    private final ShapeMsdEntrySelectedListener listener;

    interface ShapeMsdEntrySelectedListener {
        void onMsdEntrySelected(double range, int color);
    }

    public ShapeMsdAdapter(final MapView mapView,
            final ShapeMsdEntrySelectedListener listener) {
        _mapView = mapView;
        context = mapView.getContext();
        mInflater = LayoutInflater.from(context);
        prefs = AtakPreferences.getInstance(mapView.getContext());
        _prefs = new UnitPreferences(mapView.getContext());
        load();
        prefs.registerListener(this);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return shapeEntryArrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return shapeEntryArrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * Add a KML Entry to the adapter
     * @param entry the kml entry
     */
    public void add(ShapeMsdEntry entry) {
        addPreference(entry);
        shapeEntryArrayList.add(entry);
        Collections.sort(shapeEntryArrayList, ShapeMsdEntryComparator);
        notifyDataSetChanged();
        fireEntryAdded(entry);
    }

    /**
     * Remove an entry from the adapter
     * @param entry the kml entry
     */
    public void remove(ShapeMsdEntry entry) {
        removePreference(entry);
        shapeEntryArrayList.remove(entry);
        notifyDataSetChanged();
        fireEntryRemoved(entry);
    }

    @Override
    public View getView(final int position, View convertView,
            ViewGroup parent) {
        final ViewHolder holder;

        final ShapeMsdEntry entry = shapeEntryArrayList.get(position);

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.shape_msd_row, null);

            holder.title = convertView
                    .findViewById(R.id.titleTextView);
            holder.delete = convertView
                    .findViewById(R.id.remove);
            holder.edit = convertView
                    .findViewById(R.id.edit);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onMsdEntrySelected(entry.range, entry.color);
            }
        });

        holder.title.setText(entry.title);

        AlertDialog.Builder deleteWarning = createDelete(entry);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteWarning.show();
            }
        });

        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder editDialog = createEdit(entry);
                editDialog.show();
            }
        });

        return convertView;
    }

    public AlertDialog.Builder createEdit(final ShapeMsdEntry entry) {

        View v = mInflater.inflate(R.layout.add_shapemsd_entry, null, false);

        final EditText etName = v.findViewById(R.id.name);
        etName.setText(entry.title);

        Span _units = _prefs.getRangeUnits(entry.range);
        final double r = SpanUtilities.convert(entry.range, Span.METER, _units);
        final EditText rangeTxt = v.findViewById(R.id.msd_range);
        rangeTxt.setText(r < 100 ? ShapeMsdDropDownReceiver._two.get().format(r)
                : ShapeMsdDropDownReceiver._one.get().format(r));

        final ColorButton colorBtn = v.findViewById(R.id.msd_color);
        colorBtn.setColor(entry.color);

        final UnitsArrayAdapter adapter = new UnitsArrayAdapter(
                _mapView.getContext(),
                R.layout.spinner_text_view, Span.values());
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        final Spinner unitsSp = v.findViewById(R.id.msd_units);
        unitsSp.setAdapter(adapter);
        unitsSp.setSelection(adapter.getPosition(_units));

        AlertDialog.Builder editDialog = new AlertDialog.Builder(
                _mapView.getContext())
                        .setCancelable(false).setView(v)
                        .setTitle(context.getString(R.string.edit))
                        .setPositiveButton(context.getString(R.string.update),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface,
                                            int i) {

                                        String title = etName.getText()
                                                .toString();

                                        double r = ShapeMsdDropDownReceiver
                                                .getRange(rangeTxt, unitsSp);
                                        int c = colorBtn.getColor();

                                        ShapeMsdEntry et = new ShapeMsdEntry(
                                                entry.id, title, r, c);
                                        remove(entry);
                                        add(et);
                                    }
                                })
                        .setNegativeButton(context.getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface,
                                            int i) {
                                        dialogInterface.dismiss();
                                    }
                                });

        return editDialog;
    }

    private AlertDialog.Builder createDelete(ShapeMsdEntry entry) {
        //Delete Dialog
        final AlertDialog.Builder deleteWarning = new AlertDialog.Builder(
                _mapView.getContext());
        deleteWarning
                .setMessage(
                        "You are about ready to delete Minimum Safe Distance Favorite: "
                                + entry.title);
        deleteWarning.setPositiveButton(context.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        remove(entry);
                    }
                });
        deleteWarning.setNegativeButton("Cancel", null);
        return deleteWarning;
    }

    static class ViewHolder {
        TextView title;
        ImageButton delete;
        ImageButton edit;

    }

    private final Comparator<ShapeMsdEntry> ShapeMsdEntryComparator = new Comparator<ShapeMsdEntry>() {
        @Override
        public int compare(ShapeMsdEntry t0, ShapeMsdEntry t1) {
            return t0.title.compareToIgnoreCase(t1.title);
        }
    };

    private ShapeMsdEntry prefToEntry(final String name, final String val) {
        if (val == null)
            return null;

        try {

            final String uid = name.replace(SHAPE_MSD_ENTRY, "");
            final String[] v = val.split(",");
            final String title = v[0];
            final double range = Double.parseDouble(v[1]);
            final int color = Integer.parseInt(v[2]);
            return new ShapeMsdEntry(uid, title, range, color);
        } catch (Exception ignored) {
            return null;
        }

    }

    private void load() {
        Map<String, ?> preferences = prefs.getAll();
        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            final String name = entry.getKey();
            final Object val = entry.getValue();
            if (name.startsWith(SHAPE_MSD_ENTRY) && val instanceof String) {
                ShapeMsdEntry sme = prefToEntry(name, (String) val);
                if (sme != null)
                    shapeEntryArrayList.add(sme);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key == null)
            return;

        if (false && key.startsWith(SHAPE_MSD_ENTRY)) {
            ShapeMsdEntry sme = prefToEntry(key,
                    sharedPreferences.getString(key, null));

            ShapeMsdEntry removal = null;
            for (ShapeMsdEntry s : shapeEntryArrayList)
                if (s.id.equals(sme.id))
                    removal = s;
            if (removal != null)
                shapeEntryArrayList.remove(removal);

            shapeEntryArrayList.add(sme);
            Collections.sort(shapeEntryArrayList, ShapeMsdEntryComparator);
            notifyDataSetChanged();

        }

    }

    private void addPreference(ShapeMsdEntry entry) {
        prefs.set(SHAPE_MSD_ENTRY + entry.id,
                entry.title.replace(',', ' ') + "," + entry.range + ","
                        + entry.color);
    }

    private void removePreference(ShapeMsdEntry entry) {
        prefs.remove(SHAPE_MSD_ENTRY + entry.id);
    }

    interface EntryChangedListener {
        void added(ShapeMsdEntry entry);

        void removed(ShapeMsdEntry entry);
    }

    public void addEntryChangedListener(
            EntryChangedListener entryChangedListener) {
        entryChangedListeners.add(entryChangedListener);
    }

    public void removeEntryChangedListener(
            EntryChangedListener entryChangedListener) {
        entryChangedListeners.remove(entryChangedListener);
    }

    private void fireEntryAdded(ShapeMsdEntry entry) {
        for (EntryChangedListener entryChangedListener : entryChangedListeners)
            entryChangedListener.added(entry);
    }

    private void fireEntryRemoved(ShapeMsdEntry entry) {
        for (EntryChangedListener entryChangedListener : entryChangedListeners)
            entryChangedListener.removed(entry);
    }

    public ArrayList<ShapeMsdEntry> getCurrentList() {
        return new ArrayList<>(shapeEntryArrayList);
    }

    @Override
    public void dispose() {
        prefs.unregisterListener(this);

    }
}
