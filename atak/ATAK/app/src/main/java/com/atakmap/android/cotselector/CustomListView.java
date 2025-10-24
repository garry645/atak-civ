
package com.atakmap.android.cotselector;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.atakmap.android.drawing.milsym.MilSym;
import com.atakmap.android.icons.CotDescriptions;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.util.Collections2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.ModifierApi;
import gov.tak.api.cot.CotUtils;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525;


/**
 * TODO: rewrite to better use just the Provider information
 */
public class CustomListView extends ListView
        implements View.OnClickListener, View.OnKeyListener, TextWatcher {


    static final String TAG = "CustomListView";


    // these are the raw folder names, not translated
    private final String[] weightedFolders = new String[] {
            "Subsurface Track",
            "Special Operations Forces (SOF) Unit",
            "Space Track",
            "Sea Surface Track",
            "Ground Track",
            "Air Track",
    };

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public interface OnTypeChangedListener {
        void notifyChanged(String type);
    }

    // package-private
    interface OnTypeChangedListener2  {
        void notifyChanged(String type, String milsym);
    }


    private final String SEARCH_RESULTS = "#REQ:SearchResults";
    private OnTypeChangedListener2 cs;

    private ISymbologyProvider provider;

    private Affiliation selectedAffil = null;
    private final List<MilStd2525CAdapter> adapterList = new ArrayList<>();

    private final Map<String, MilStd2525CAdapter> symbolParentMap = new HashMap<>();

    private ImageButton backB = null;

    private ImageButton searchB = null;

    private EditText searchText = null;
    private String searchString;
    private List<ISymbolTable.Symbol> searchResults = null;

    private boolean affiliationChange = true;


    public CustomListView(Context context) {
        super(context);
    }

    private static class VirtualFolder implements ISymbolTable.Folder {

        String name;
        final ArrayList<ISymbolTable.Folder> children = new ArrayList<>();
        final ArrayList<ISymbolTable.Symbol> symbols = new ArrayList<>();

        public VirtualFolder(String name) {
            this.name = name;
        }
        @Override
        public Collection<ISymbolTable.Folder> getChildren() {
            return children;
        }

        @Override
        public Collection<ISymbolTable.Symbol> getSymbols() {
            return symbols;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public EnumSet<ShapeType> getContentMask() {
            return EnumSet.allOf(ShapeType.class);
        }
    }

    private boolean hasWarfightingPreserveFolder(ISymbolTable.Folder folder) {
        String[] warfightingStrings = { "air", "land equipment", "" +
                "", "land unit", "mine warfare", "sea subsurface", "space"};

        for(final String str : warfightingStrings) {
            if(folder.getName().toLowerCase(Locale.US).contains(str)) {
                return true;
            }
        }
        return false;
    }


    private boolean hasWarfightingCollapseFolder(ISymbolTable.Folder folder) {

        String[] warfightingStrings = { "warfighting" };

        for(String string : warfightingStrings) {
            if(folder.getName().toLowerCase().contains(string)) {
                return true;
            }
        }
        return false;
    }

    void setProvider(ISymbologyProvider provider) {

        if(this.provider == provider)
            return;

        selectedAffil = null;
        adapterList.clear();
        symbolParentMap.clear();
        this.provider = provider;

        String req = "#REQ:ROOT";
        ArrayList<String> vals = new ArrayList<>();
        vals.add("Friendly");
        vals.add("Neutral");
        vals.add("Unknown");
        vals.add("Hostile");

        VirtualFolder rootfolder = new VirtualFolder("Root");

        MilStd2525CAdapter newAdapter = new MilStd2525CAdapter(getContext(),
                R.layout.a2525listitem, this,
                vals, req, null, rootfolder);

        for(final String child : vals) {
            Affiliation affil;
            switch (child) {
                case "Friendly":
                    affil = Affiliation.Friend;
                    break;
                case "Neutral":
                    affil = Affiliation.Neutral;
                    break;
                case "Hostile":
                    affil = Affiliation.Hostile;
                    break;
                case "Unknown":
                default:
                    affil = Affiliation.Unknown;
                    break;
            }
            ISymbolTable.Folder virtual = new VirtualFolder(child);

            rootfolder.getChildren().add(virtual);

            final ISymbolTable.Folder symbolTable = provider.getSymbolTable().getRoot();
            for (ISymbolTable.Folder folder : symbolTable.getChildren()) {

                if (hasWarfightingCollapseFolder(folder)) {
                    for(ISymbolTable.Folder warfighting : folder.getChildren()) {
                        virtual.getChildren().add(warfighting);
                    }
                    for(ISymbolTable.Symbol warfighting : folder.getSymbols()) {
                        virtual.getSymbols().add(warfighting);
                    }
                } else if(hasWarfightingPreserveFolder(folder)) {
                    virtual.getChildren().add(folder);
                }
            }
            addFolder(rootfolder, virtual, newAdapter, affil);
            rootfolder.children.add(virtual);
        }

        final ISymbolTable.Folder symbolTable = provider.getSymbolTable().getRoot();
        for (ISymbolTable.Folder folder : symbolTable.getChildren()) {
            if (!hasWarfightingCollapseFolder(folder) && !hasWarfightingPreserveFolder(folder)) {
                addFolder(provider.getSymbolTable().getRoot(), folder, newAdapter, null);
                vals.add(folder.getName());
            }
        }
        adapterList.add(newAdapter);
    }

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public void init(final View v, final OnTypeChangedListener cs) {
        init(v, new OnTypeChangedListener2() {
            @Override
            public void notifyChanged(String type, String milsym) {
                cs.notifyChanged(type);
            }
        });
    }

    void init(final View v, final CustomListView.OnTypeChangedListener2 cs) {
        this.cs = cs;

        setProvider(ATAKUtilities.getDefaultSymbologyProvider());

        setScrollbarFadingEnabled(false);

        // this.possibleAffils = possibleAffils
        // get selected affil and possible affils from bundle
        // set up the buttons

        backB = v.findViewById(R.id.BackB);
        backB.setOnClickListener(this);

        searchB = v.findViewById(R.id.hierarchy_search_btn);
        searchB.setOnClickListener(this);

        searchText = v.findViewById(R.id.hierarchy_search_text);
        searchText.addTextChangedListener(this);

        InputMethodManager imm = (InputMethodManager) ATAKApplication.getCurrentActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);


        // only need to listen for the key listener when the search text is active
        searchText.setOnKeyListener(this);
    }

    @Override
    public void onVisibilityChanged(@NonNull View changedView,
                                    int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView.equals(this)) {
            cancelSearch();
        }

    }

    /** @deprecated use {@link #setType(String, String)} */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public void setType(final String initCotType) {
        setType(initCotType, null);
    }

    void setType(final String initCotType, String milsym) {
        if (initCotType != null) {

            switch (initCotType.charAt(2)) {
                case 'p':
                    selectedAffil = Affiliation.Pending;
                    break;
                case 'u':
                    selectedAffil = Affiliation.Unknown;
                    break;
                case 'a':
                    selectedAffil = Affiliation.AssumedFriend;
                    break;
                case 'f':
                    selectedAffil = Affiliation.Friend;
                    break;
                case 'n':
                    selectedAffil = Affiliation.Neutral;
                    break;
                case 's':
                    selectedAffil = Affiliation.Suspect;
                    break;
                case 'h':
                    selectedAffil = Affiliation.Hostile;
                    break;
                case 'j':
                    selectedAffil = Affiliation.Joker;
                    break;
                case 'k':
                    selectedAffil = Affiliation.Faker;
                    break;
                case 'o':
                default:
                    selectedAffil = Affiliation.Unknown;
            }

            this.handleDataChanged();
            for (MilStd2525CAdapter adptr : adapterList) { //Let everyone know about the change
                adptr.notifyDataSetChanged();
            }

            MilStd2525CAdapter temp = symbolParentMap.get(CotDescriptions.normalize(milsym));

            if (temp == null)  {
                String s = "#REQ:" + get2525FromCoT(initCotType) + ".png";// "#REQ:ROOT"; // should get
                // some kind of string from
                // the bundle here, maybe
                temp = findAdapter(s);
                if (temp != null && temp.getParent() != null)
                    temp = temp.getParent();
            }

            if (temp == null) {
                String tempCot = initCotType;
                while (temp == null) {
                    if (tempCot.length() < 2)
                        temp = findAdapter("#REQ:ROOT");
                    else {
                        tempCot = tempCot.substring(0, tempCot.length() - 2);
                        String t = "#REQ:" + get2525FromCoT(tempCot) + ".png";
                        temp = findAdapter(t);
                    }
                }
            }

            setTheAdapter(temp);
            if (milsym == null)
                temp.setType(get2525FromCoT(initCotType));
            else
                temp.setType(milsym);

            temp.notifyDataSetChanged();
        }
    }

    private MilStd2525CAdapter findAdapter(String req) {
        MilStd2525CAdapter adp = null;
        for (MilStd2525CAdapter adptr : adapterList) {
            if (adptr.requires.equals(req)) {
                adp = adptr;
                break;
            }
        }
        return adp;
    }


    private ISymbolTable.Symbol symbolForFolder(ISymbolTable.Folder parent, ISymbolTable.Folder folder) {
        for(ISymbolTable.Symbol sym : parent.getSymbols()) {
            if(folder.getName().equalsIgnoreCase(sym.getName()))
                return sym;
        }

        return null;
    }

    private String createSymbolString(ISymbolTable.Symbol sym) {
        StringBuilder sb = new StringBuilder(sym.getCode().toLowerCase());
        sb.append(".png");

        if(sb.charAt(1) == '*')
            sb.setCharAt(1, '_');
        if(sb.charAt(3) == '*')
            sb.setCharAt(3, 'p');

        for(int i=0;i < sb.length();++i)
            if(sb.charAt(i) == '*')
                sb.setCharAt(i, '-');

        return sb.toString();
    }

    private String anchorForFolder(ISymbolTable.Folder parent, ISymbolTable.Folder folder) {
        ISymbolTable.Symbol sym = symbolForFolder(parent, folder);
        String anchor = null;
        if(sym != null) {
            anchor = createSymbolString(sym);
            if(anchor != null)
                return anchor;
        }

        return "##" + folder.getName();
    }

    private void addFolder(ISymbolTable.Folder parent, ISymbolTable.Folder folder, MilStd2525CAdapter parentAdapter, Affiliation currAffil) {
        if(!folder.getContentMask().contains(ShapeType.Point))
            return;

        String req;
        ArrayList<String> vals = new ArrayList<>();

        req = "#REQ:" + anchorForFolder(parent, folder);

        for (ISymbolTable.Folder f : folder.getChildren()) {
            if (!f.getContentMask().contains(ShapeType.Point))
                continue;
            vals.add(f.getName());
        }

        for (ISymbolTable.Symbol sym : folder.getSymbols()) {
            if (Collections2.containsIgnoreCase(vals, sym.getName()))
                continue;

            if(!sym.getContentMask().contains(ShapeType.Point))
                continue;
            vals.add(sym.getName());
        }

        for (String wf: weightedFolders) {
            if (vals.remove(wf))
                vals.add(0, wf);
        }

        MilStd2525CAdapter adapter = new MilStd2525CAdapter(getContext(),
                R.layout.a2525listitem, this,
                vals, req, parentAdapter, folder);
        adapterList.add(adapter);
        parentAdapter.addChild(adapter);
        adapter.setPath(folder.getName());

        for (ISymbolTable.Symbol sym : folder.getSymbols()) {
            String code = CotDescriptions.normalize(sym.getCode());
            if (code != null)
                symbolParentMap.put(code, adapter);
        }
        for (ISymbolTable.Folder f : folder.getChildren()) {
            addFolder(folder, f, adapter, currAffil);
        }
    }
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public void add2525ToList(String req, MilStd2525CAdapter pre, ArrayList<String> vals) {
        add2525ToList(req, pre, vals, null);
    }

    private void add2525ToList(String req, MilStd2525CAdapter pre, ArrayList<String> vals, ISymbolTable.Folder folder) {

        MilStd2525CAdapter newAdapter = new MilStd2525CAdapter(getContext(),
                R.layout.a2525listitem, this,
                vals, req, pre, folder);

        adapterList.add(newAdapter);
    }

    /**
     * Loads an icon based on the sidc
     * @param fn the filename of the sidc
     * @return  the bitmap drawable
     */
    @ModifierApi(since = "5.6", target = "5.9", modifiers = "")
    public BitmapDrawable requestLoadIcon(String fn) {
        return CotDescriptions.getIcon(getContext(), fn, selectedAffil);
    }

    public void setTheAdapter(ListAdapter listadapter) {
        super.setAdapter(listadapter);

        if (listadapter instanceof MilStd2525CAdapter) {
            MilStd2525CAdapter adapter = (MilStd2525CAdapter) listadapter;
            if (adapter.requires.equals("#REQ:ROOT")) {
                setSelectedAffil(null);
            }
            if (!searchB.isSelected()) {
                searchText.clearFocus();
                searchText.setText(getPath(
                        (adapter)));
            }
        }
    }

    public void allowAffiliationChange(boolean affiliationChange) {
        this.affiliationChange = affiliationChange;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(backB)) {
            searchB.setSelected(false);
            MilStd2525CAdapter adp = (MilStd2525CAdapter) getAdapter();

            if (adp.getParent() == null)
                return;

            if (!affiliationChange && adp.getParent().requires.equals("#REQ:ROOT"))
                return;

            goBack(adp.getParent());
        }
        if (view.equals(searchB)) {
            searchB.setSelected(!searchB.isSelected());
            if (searchB.isSelected()) {
                searchText.setEnabled(true);
                searchText.setText("");
                searchText.requestFocus();
            } else {
                cancelSearch();
                dismissKeyboard(searchText);
            }
        }
    }

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public boolean goDeeper(String string) {
        cancelSearch();
        ListAdapter adapter = findAdapter(string);
        if(adapter == null) return false;
        setTheAdapter(adapter);
        return true;
    }

    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public boolean canGoDeeper(String string) {
        MilStd2525CAdapter adapter = findAdapter(string);
        return adapter != null;
    }

    private String getPath(MilStd2525CAdapter adapter) {
        if(adapter.getParent() == null) {
            return "/";
        }
        if(adapter.getPath().equals("Friendly") ||
                adapter.getPath().equals("Hostile") ||
                adapter.getPath().equals("Unknown") ||
                adapter.getPath().equals("Neutral")) {
            return "/";
        }
        return getPath(adapter.getParent()) + adapter.getPath() + "/";
    }


    private void cancelSearch() {
        dismissKeyboard(searchText);
        searchB.setSelected(false);
        searchText.clearFocus();
        searchText.setEnabled(false);
        MilStd2525CAdapter adp = (MilStd2525CAdapter) getAdapter();
        if (adp.requires.equals(SEARCH_RESULTS)) {
            goBack(adp.getParent());
            return;
        }
        searchText.setText(getPath(
                ((MilStd2525CAdapter) getAdapter())));
    }

    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) ATAKApplication.getCurrentActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    void printBranch(ISymbolTable.Folder root, int depth) {
        if (depth <= 0)
            return;
        StringBuilder symbolSB = new StringBuilder();
        for (ISymbolTable.Symbol symbol : root.getSymbols()) {
            symbolSB.append(symbol.getName() + " (" + symbol.getCode() + ")");
            symbolSB.append(", ");
        }

        StringBuilder folderSB = new StringBuilder();
        for (ISymbolTable.Folder folder : root.getChildren()) {
            folderSB.append(folder.getName());
            folderSB.append(", ");
            printBranch(folder, depth - 1);
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {

        if (charSequence == null)
            return;

        if (!searchB.isSelected())
            return;

        if (!searchText.hasFocus()) {
            return;
        }

        searchString = charSequence.toString();

        if (searchString == null || searchString.length() < 3) {
            searchResults = null;
        } else {
            searchResults = MilSym.find(provider, searchString,
                    EnumSet.of(ShapeType.Point));
        }

        ArrayList<String> matchingSymbols = new ArrayList<>();
        VirtualFolder folder = new VirtualFolder("");
        if (searchResults != null) {

            for (ISymbolTable.Symbol symbol : searchResults) {
                if(matchingSymbols.contains(symbol.getName()))
                    continue;
                matchingSymbols
                        .add(symbol.getName());
                folder.getSymbols().add(symbol);

            }
        }
        adapterList.remove(findAdapter(SEARCH_RESULTS));
        MilStd2525CAdapter current = (MilStd2525CAdapter) getAdapter();
        // if we are already looking at search results use the original previous
        // otherwise if we are transitioning from heirarchical display to search
        // results capture the previous for future reference.
        MilStd2525CAdapter previous = (current.requires.equals(SEARCH_RESULTS))
                ? current.getParent()
                : current;
        add2525ToList(SEARCH_RESULTS, previous, matchingSymbols, folder);
        MilStd2525CAdapter adp = findAdapter(SEARCH_RESULTS);

        setTheAdapter(adp);
        adp.notifyDataSetChanged();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {

    }

    private String get2525FromCoT(String cot) {
        if (cot != null && cot.indexOf("a") == 0 && cot.length() > 3) {
            StringBuilder s2525C = new StringBuilder("s_");

            for (int x = 4; x < cot.length(); x += 2) {
                char[] t = {
                        cot.charAt(x)
                };
                String s = new String(t);
                s2525C.append(s.toLowerCase(LocaleUtil.getCurrent()));
                if (x == 4) {
                    s2525C.append("p");
                }
            }
            for (int x = s2525C.length(); x < 15; x++) {
                if (x == 10 && s2525C.charAt(2) == 'g'
                        && s2525C.charAt(4) == 'i') {
                    s2525C.append("h");
                } else {
                    s2525C.append("-");
                }
            }
            return s2525C.toString();
        }

        return "";
    }

    private boolean is2525D(final String code) {
        if (code != null && code.length() >= 20)
            return true;

        return false;
    }

    private String getCoTFrom2525(String string) {
        String code = string;

        Affiliation affiliation = SymbologyProvider.getAffiliation(code);

        if(is2525D(string))
            code = MilStd2525.get2525CFrom2525D(string, false);

        if(code == null) {
            if(affiliation == null)
                return "a-u-G";

            char c = 'u';
            switch (affiliation) {
                case Pending:
                    c = 'p';
                    break;
                case Unknown:
                    c = 'u';
                    break;
                case AssumedFriend:
                    c = 'a';
                    break;
                case Friend:
                    c = 'f';
                    break;
                case Neutral:
                    c = 'n';
                    break;
                case Hostile:
                    c = 'h';
                    break;
                case Joker:
                    c = 'j';
                    break;
                case Faker:
                    c = 'k';
                    break;
                default:
            }
            return "a-" + c + "-G";
        }

        return CotUtils.cotTypeFromMil2525C(code);
    }

    public void sendCoTFrom2525(String s2525) {
        if (cs != null) {
            ///xxx
            int index = s2525.indexOf(".png");
            if(index != -1){
                s2525 = s2525.substring(0, index);
            }
            String aff = s2525;
            if(selectedAffil != null)
                aff = SymbologyProvider.setAffiliation(s2525, selectedAffil);
            if(aff != null)
                s2525 = aff;

            cs.notifyChanged(getCoTFrom2525(s2525), s2525);
        }
    }

    boolean goDeeper(MilStd2525CAdapter adapter) {
        cancelSearch();
        setTheAdapter(adapter);
        return true;
    }

    boolean goBack(final MilStd2525CAdapter prev) {
        if(prev != null) {
            setTheAdapter(prev);

            if(prev.requires.equals("#REQ:ROOT")) {
                setSelectedAffil(null);
            }
            return true;
        }
        return false;
    }

    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public void setSelectedAffil(int affil) {
        Affiliation[] affiliations = Affiliation.values();
        if (affil < 0 || affil >= affiliations.length) {
            return;
        }
        setSelectedAffil(affiliations[affil]);
    }
    void setSelectedAffil(Affiliation adffil) {
        selectedAffil = adffil;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (searchB.isSelected()) {
                cancelSearch();
                return true;
            }
        }
        return false;
    }

}
