
package com.atakmap.android.drawing.milsym;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Shape;
import com.atakmap.app.R;
import com.atakmap.util.Collections2;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;
import gov.tak.platform.marshal.MarshalManager;

class MilSymSelectorAdapter extends BaseAdapter implements TextWatcher {

    private String searchText;
    private List<ISymbolTable.Symbol> searchResults = null;

    private final boolean isMulti;

    private boolean isFiltering = false;

    private Shape subject;

    public void setSubject(Shape shape) {
        subject = shape;
    }

    public void setIsFiltering(boolean b) {
        notifyDataSetInvalidated();
        isFiltering = b;
    }

    public boolean isFiltering() {
        return isFiltering;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {
        searchText = charSequence.toString();
        searchResults = MilSym.find(searchText, contentFilter);

        notifyDataSetInvalidated();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {

    }

    interface OnSymbolSelectedListener {
        void onSymbolSelected(ISymbolTable.Symbol symbol);

        void onPathChanged(String path);
    }

    private final List<ISymbolTable.Folder> stack = new ArrayList<>();
    private ISymbolTable.Folder peek;
    private final Context context;
    private OnSymbolSelectedListener listener;

    private final EnumSet<ShapeType> contentFilter;

    private ISymbolTable symbolTable;

    MilSymSelectorAdapter(Context context, Shape subject,
            ISymbolTable symbolTable, boolean isMulti) {
        this.context = context;
        this.symbolTable = symbolTable;
        this.subject = subject;
        this.isMulti = isMulti;
        peek = this.symbolTable.getRoot();
        stack.add(peek);

        contentFilter = isMulti
                ? EnumSet.copyOf(ISymbolTable.MASK_AREAS_AND_LINES)
                : EnumSet.of(ShapeType.Point);
    }

    void setSymbolTable(@NonNull ISymbolTable symbolTable) {
        Objects.requireNonNull(symbolTable);
        this.symbolTable = symbolTable;

        reset();
    }

    /**
     * After a reset allow the path to be set based on what the symbol
     * had previously.   There might be cases where a reset is desired not to
     * take the item to the appropriate subpath so this is broken out.
     * @param path the array of folders starting with the most generalized down to the
     *             most specific.
     */
    public void setPath(List<ISymbolTable.Folder> path) {
        if (searchResults != null)
            searchResults.clear();
        searchText = "";

        peek = this.symbolTable.getRoot();
        stack.clear();
        stack.add(peek);
        for (ISymbolTable.Folder folder : path) {
            peek = folder;
            stack.add(peek);
        }
        notifyDataSetInvalidated();
        dispatchPathChanged();
    }

    @Override
    public int getCount() {

        if (searchText != null && searchText.length() >= 3) {
            int count = 0;
            if (!isFiltering())
                return searchResults.size();

            for (ISymbolTable.Symbol s : searchResults) {
                if (applyFilter(s))
                    ++count;
            }
            return count;

        } else {
            int count = 0;
            for (ISymbolTable.Folder f : peek.getChildren())
                if (Collections2.containsAny(contentFilter, f.getContentMask()))
                    count++;
            for (ISymbolTable.Symbol s : peek.getSymbols())
                if (Collections2.containsAny(contentFilter, s.getContentMask())
                        && applyFilter(s))
                    count++;
            return count;
        }
    }

    boolean isSearching() {
        return searchText != null && searchText.length() >= 3;
    }

    @Override
    public Object getItem(int i) {
        if (isSearching()) {
            int count = 0;
            if (!isFiltering())
                return searchResults.get(i);

            for (ISymbolTable.Symbol s : searchResults) {
                if (applyFilter(s)) {
                    if (count == i)
                        return s;
                    ++count;
                }
            }
        }

        for (ISymbolTable.Folder f : peek.getChildren()) {
            if (Collections2.containsAny(contentFilter, f.getContentMask())) {
                if (i == 0)
                    return f;
                i--;
            }
        }
        for (ISymbolTable.Symbol s : peek.getSymbols()) {
            if (Collections2.containsAny(contentFilter, s.getContentMask())
                    && applyFilter(s)) {
                if (i == 0)
                    return s;
                i--;
            }
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        // XXX -
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int buttonStyle = R.style.darkButton;

        Button button = new Button(
                new ContextThemeWrapper(context, buttonStyle), null,
                buttonStyle);

        Object o = getItem(i);
        if (o instanceof ISymbolTable.Folder) {
            final ISymbolTable.Folder folder = ((ISymbolTable.Folder) o);
            button.setPadding(6, 24, 6, 24);

            button.setText(MilSym.getTranslatedName(folder));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    push(folder);
                }
            });
        } else if (o instanceof ISymbolTable.Symbol) {

            final ISymbolTable.Symbol symbol = (ISymbolTable.Symbol) o;
            button.setText(isSearching() ? MilSym.getTranslatedFullName(symbol)
                    : MilSym.getTranslatedName(symbol));
            button.setPadding(6, 3, 6, 3);

            Drawable d = MarshalManager.marshal(
                    symbol.getPreviewDrawable(isMulti ? -1 : 0),
                    gov.tak.api.commons.graphics.Drawable.class,
                    Drawable.class);
            button.setCompoundDrawables(d, null, null, null);

            String symbolName = MilSym.getTranslatedName(symbol);
            //String symbolCode = symbol.getCode();
            String symbolSummary = symbol.getSummary();
            if (symbolSummary != null) {
                Drawable info = context.getResources()
                        .getDrawable(R.drawable.info);
                info.setBounds(0, 0, 128, 128);
                button.setCompoundDrawables(d, null, info, null);
                button.setTag(symbolName);

                button.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            if (motionEvent.getX() >= button.getRight()
                                    - button.getTotalPaddingRight()) {
                                AlertDialog summaryDialog = new AlertDialog.Builder(
                                        MapView.getMapView().getContext())
                                                .setIcon(R.drawable.info)
                                                .setTitle(MilSym
                                                        .getTranslatedFullName(
                                                                symbol))
                                                .setMessage(symbolSummary)
                                                .setPositiveButton(R.string.ok,
                                                        null)
                                                .create();
                                try {
                                    summaryDialog.show();
                                } catch (Exception ignored) {
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });
            } else {
                button.setCompoundDrawables(d, null, null, null);
            }
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onSymbolSelected(symbol);
                }
            });
        }
        return button;
    }

    boolean applyFilter(ISymbolTable.Symbol symbol) {
        if (subject == null || !isFiltering())
            return true;

        return true;
    }

    void setContentFilter(EnumSet<ShapeType> mask) {
        contentFilter.clear();
        contentFilter.addAll(mask);
        if (isMulti)
            contentFilter.retainAll(ISymbolTable.MASK_AREAS_AND_LINES);
        else
            contentFilter.retainAll(EnumSet.of(ShapeType.Point));
    }

    void pop() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
            peek = stack.get(stack.size() - 1);
            notifyDataSetInvalidated();
        }
        dispatchPathChanged();
    }

    void push(ISymbolTable.Folder folder) {
        stack.add(folder);
        peek = folder;
        notifyDataSetInvalidated();
        dispatchPathChanged();
    }

    void reset() {
        if (searchResults != null)
            searchResults.clear();
        searchText = "";
        stack.clear();
        peek = symbolTable.getRoot();
        stack.add(peek);
        notifyDataSetInvalidated();
        dispatchPathChanged();
    }

    void setListener(OnSymbolSelectedListener listener) {
        this.listener = listener;
    }

    void dispatchPathChanged() {
        if (listener != null) {
            StringBuilder path = new StringBuilder();
            for (int i = 1; i < stack.size(); i++) {
                final String subpath = MilSym.getTranslatedName(stack.get(i));
                if (subpath == null)
                    continue;
                path.append('/');
                path.append(subpath);
            }
            listener.onPathChanged(path.toString());
        }
    }
}
