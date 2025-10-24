
package com.atakmap.android.drawing.milsym;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.editableShapes.Rectangle;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.Circle;
import com.atakmap.app.R;
import com.atakmap.math.MathUtils;

import java.util.EnumSet;
import java.util.List;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.MilSymDetailHandler;
import gov.tak.api.symbology.ShapeType;
import gov.tak.platform.symbology.SymbologyProvider;

public final class MilSymFragment extends Fragment
        implements MilSymSelectorAdapter.OnSymbolSelectedListener {

    /**
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver: java.lang.IllegalStateException: Fragment com.atakmap.android.drawing.milsym.MilSymFragment must be a public static class to be  properly recreated from instance state.
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver:      at androidx.fragment.app.FragmentTransaction.doAddOp(FragmentTransaction.java:306)
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver:      at androidx.fragment.app.BackStackRecord.doAddOp(BackStackRecord.java:195)
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver:      at androidx.fragment.app.FragmentTransaction.replace(FragmentTransaction.java:400)
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver:      at androidx.fragment.app.FragmentTransaction.replace(FragmentTransaction.java:350)
     * 02-07 08:17:30.392  8792  8792 D DropDownReceiver:      at com.atakmap.android.dropdown.DropDownReceiver.fragmentReplaceTransaction(DropDownReceiver.java:655)
     */
    private final Context context;
    private final MilSymSelectorAdapter selector;

    private TextView pathTextView;

    private OnSymbolSelectedListener symbolSelectedListener = null;

    OnClearSymbolListener clearSymbolListener = null;

    Button clearButton;
    ImageButton searchButton;
    EditText searchText;

    @Override
    public void onSymbolSelected(ISymbolTable.Symbol symbol) {
        stopSearching();
        if (symbolSelectedListener != null)
            symbolSelectedListener.onSymbolSelected(symbol);
    }

    @Override
    public void onPathChanged(String path) {
        if (pathTextView != null)
            pathTextView.setText(path);

    }

    /**
     * After a reset allow the path to be set based on what the symbol
     * had previously.   There might be cases where a reset is desired not to
     * take the item to the appropriate subpath so this is broken out.
     * @param path the array of folders starting with the most generalized down to the
     *             most specific.
     */
    public void setPath(List<ISymbolTable.Folder> path) {
        selector.setPath(path);
    }

    public boolean stopSearching() {
        if (searchText.getVisibility() == View.VISIBLE) {
            searchButton.callOnClick();
            return true;
        }
        return false;
    }

    public interface OnSymbolSelectedListener {
        void onSymbolSelected(ISymbolTable.Symbol symbol);
    }

    public interface OnClearSymbolListener {
        void onClear();
    }

    void setOnSymbolSelectedListener(OnSymbolSelectedListener listener) {
        symbolSelectedListener = listener;
    }

    void setOnClearSymbolListener(OnClearSymbolListener listener) {
        clearSymbolListener = listener;
        if (clearButton != null) {
            clearButton
                    .setVisibility(listener == null ? View.GONE : View.VISIBLE);
            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clearSymbolListener != null)
                        clearSymbolListener.onClear();
                }
            });
        }
    }

    MilSymFragment(Context context, ISymbolTable symbolTable, boolean isMulti) {
        this.context = context;
        selector = new MilSymSelectorAdapter(context, null, symbolTable,
                isMulti);
        selector.setListener(this);
    }

    public void setSymbolTable(ISymbolTable symbolTable) {
        selector.setSymbolTable(symbolTable);
    }

    void reset(MapItem item) {
        if (selector != null) {
            if (item == null)
                selector.setContentFilter(EnumSet.allOf(ShapeType.class));
            else if (item instanceof Rectangle)
                selector.setContentFilter(EnumSet.of(ShapeType.Rectangle,
                        ShapeType.Polygon, ShapeType.LineString));
            else if ((item instanceof Circle)
                    || (item instanceof DrawingCircle))
                selector.setContentFilter(EnumSet.of(ShapeType.Circle,
                        ShapeType.Polygon, ShapeType.LineString));
            else if ((item instanceof Ellipse)
                    || (item instanceof DrawingEllipse))
                selector.setContentFilter(EnumSet.of(ShapeType.Ellipse,
                        ShapeType.Polygon, ShapeType.LineString));
            else if ((item instanceof Polyline) && MathUtils.hasBits(
                    ((Polyline) item).getStyle(), Polyline.STYLE_CLOSED_MASK))
                selector.setContentFilter(
                        EnumSet.of(ShapeType.Polygon, ShapeType.LineString));
            else if ((item instanceof Polyline))
                selector.setContentFilter(ISymbolTable.MASK_LINE);

            ISymbolTable symbolTable = null;
            do {
                if(item == null)
                    break;
                final String symbolCode = item.getMetaString(MilSymDetailHandler.MILSYM_ATTR, null);
                if(symbolCode == null)
                    break;
                symbolTable = SymbologyProvider.getSymbolTable(symbolCode);
            } while(false);
            selector.setSymbolTable(
                    (symbolTable != null) ?
                            symbolTable :
                            ATAKUtilities.getDefaultSymbologyProvider().getSymbolTable());
            selector.reset();
        }
    }

    void reset(EnumSet<ShapeType> filter) {
        if (selector != null) {
            selector.setSymbolTable(ATAKUtilities.getDefaultSymbologyProvider().getSymbolTable());
            selector.setContentFilter(filter);
            selector.reset();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // instantiate the plugin view if necessary

        View mainLayout = PluginLayoutInflater.inflate(context,
                R.layout.milsym_layout, null);
        ListView typeSelectListView = mainLayout
                .findViewById(R.id.typeSelectListView);
        pathTextView = mainLayout.findViewById(R.id.typePathTextView);
        final ImageButton filterButton = mainLayout
                .findViewById(R.id.filterButton);
        searchButton = mainLayout
                .findViewById(R.id.searchButton);
        searchText = mainLayout.findViewById(R.id.searchText);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchText.getVisibility() != View.VISIBLE) {
                    pathTextView.setVisibility(View.GONE);
                    searchText.setVisibility(View.VISIBLE);
                    searchText.setText("");
                    searchText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) context
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchText,
                            InputMethodManager.SHOW_IMPLICIT);
                } else {
                    searchText.setText("");
                    searchText.setVisibility(View.GONE);
                    pathTextView.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) context
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);

                }
            }
        });

        filterButton.setSelected(selector.isFiltering());
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selector.setIsFiltering(!selector.isFiltering());
                filterButton.setSelected(selector.isFiltering());
            }
        });
        searchText.addTextChangedListener(selector);
        final Button backButton = mainLayout.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchText.getVisibility() != View.VISIBLE)
                    selector.pop();
            }
        });
        clearButton = mainLayout.findViewById(R.id.clearButton);

        setOnClearSymbolListener(clearSymbolListener);
        setOnSymbolSelectedListener(symbolSelectedListener);

        typeSelectListView.setAdapter(selector);
        return mainLayout;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

}
