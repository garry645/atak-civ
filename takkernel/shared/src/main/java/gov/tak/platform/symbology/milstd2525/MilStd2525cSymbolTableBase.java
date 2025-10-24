package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.log.Log;
import com.atakmap.map.AtakMapView;
import com.atakmap.util.Collections2;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.ColorBlendMode;
import gov.tak.api.commons.graphics.ColorFilter;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;

abstract class MilStd2525cSymbolTableBase<SymbolDef> implements ISymbolTable
{
    private static final String TAG = "MilStd2525dSymbolTable";
    static final Map<String, String> symbolSummary = new HashMap<>();

    static final EnumSet<ShapeType> AUTOSHAPE_EXPLICIT_CONTROL_POINTS = EnumSet.of(ShapeType.Rectangle, ShapeType.Polygon, ShapeType.LineString);

    static final Set<String> AUTOSHAPE_EXPLICIT_CONTROL_POINTS_SYMBOL_CODES = new HashSet<>(
        Arrays.asList(
            // three-point
            "G*G*OAF---****X",
            "G*M*BDI---****X",
            "G*T*B-----****X",
            "G*T*C-----****X",
            "G*T*H-----****X",
            "G*T*J-----****X",
            "G*T*L-----****X",
            "G*T*M-----****X",
            "G*T*P-----****X",
            "G*T*R-----****X",
            "G*T*T-----****X",
            "G*T*W-----****X",
            "G*T*WP----****X",
            "G*T*X-----****X",
            "G*T*Z-----****X",
            "G*M*ORS---****X",
            "G*M*ORA---****X",
            "G*M*BCD---****X",
            "G*M*BCE---****X",
            "G*M*OFG---****X",
            "G*G*OAS---****X",
            "G*G*OLI---****X",
            "G*G*PD----****X",
            "G*M*BDD---****X",
            "G*M*BDE---****X",
            "G*T*Y-----****X",
            "G*M*OEB---****X",
            "G*M*OED---****X",
            "G*M*OET---****X",
            "G*G*GAS---****X",
            "G*M*ORP---****X",
            "G*M*ORC---****X",
            "G*M*ORS---****X",
            "G*M*ORA---****X",
            "G*T*US----****X",
            "G*M*OT----****X",
            "G*G*SLA---****X",
            "G*G*DLP---****X",
            "G*M*BCA---****X",
            // four-point
            "G*M*BCB---****X"
        )
    );

    FolderAdapter root = new FolderAdapter(null);
    Map<String, Symbol> allSymbols = new HashMap<>();

    abstract Map<String, SymbolDef> getAllSymbolDefs();

    @Override
    public synchronized Folder getRoot() {
        if(root.childrenm.isEmpty()) {
            IMilStd2525cInterop<SymbolDef, ?, ?> interop = (IMilStd2525cInterop<SymbolDef, ?, ?>) new MilStd2525cInterop();
            Map<String, SymbolDef> symbols = getAllSymbolDefs();
            for(SymbolDef symbol : symbols.values()) {
                push(root, symbol, interop);
            }
            fillAllSymbols(root, interop);
        }
        return root;
    }

    @Override
    public List<Symbol> find(String searchString, EnumSet<ShapeType> contentMask) {
        List<Symbol> results = new ArrayList<>();
        if(searchString != null) {
            getRoot();
            String searchStringLower = searchString.toLowerCase();
            String spaceSearchString = " " + searchStringLower;
            for (Symbol symbol : allSymbols.values()) {
                if (contentMask != null && !Collections2.containsAny(symbol.getContentMask(), contentMask))
                    continue;
                String fullName = symbol.getFullName().toLowerCase();
                if (fullName.startsWith(searchStringLower) || fullName.contains(spaceSearchString)) {
                    results.add(symbol);
                }
            }
        }

        return results;
    }

    public Symbol getSymbol(String code)
    {
        if(code == null)
            return null;
        IMilStd2525cInterop<SymbolDef, ?, ?> interop = (IMilStd2525cInterop<SymbolDef, ?, ?>) new MilStd2525cInterop();
        code = interop.getBasicSymbolId(code);
        getRoot();
        return allSymbols.get(code);
    }

    static <SymbolDef> void push(FolderAdapter root, SymbolDef symbol, IMilStd2525cInterop<SymbolDef, ?, ?> interop) {
        // XXX - skip empty path
        if(interop.getFullPath(symbol).equals(""))
            return;
        // XXX - skip empty path
        if(interop.getFullPath(symbol).equals("Basic Shapes"))
            return;

        final String path = interop.getFullPath(symbol).replace("WARFIGHTING SYMBOLS", "Warfighting Symbology");
        final String[] paths = path.split("/");

        FolderAdapter p = root;
        for(String subpath : paths) {
            FolderAdapter f = (FolderAdapter) p.childrenm.get(subpath);
            if(f == null)
                p.childrenm.put(subpath, f=new FolderAdapter(subpath));
            p = f;
        }

        EnumSet<ShapeType> contentMask;
        switch(interop.getDrawCategory(symbol)) {
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_AUTOSHAPE:
                if(AUTOSHAPE_EXPLICIT_CONTROL_POINTS_SYMBOL_CODES.contains(interop.getBasicSymbolId(symbol))) {
                    contentMask = AUTOSHAPE_EXPLICIT_CONTROL_POINTS;
                    break;
                }
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_LINE:
                contentMask = MASK_AREAS_AND_LINES;
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_SUPERAUTOSHAPE:
                if(AUTOSHAPE_EXPLICIT_CONTROL_POINTS_SYMBOL_CODES.contains(interop.getBasicSymbolId(symbol))) {
                    contentMask = AUTOSHAPE_EXPLICIT_CONTROL_POINTS;
                    break;
                }
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_POLYGON:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_SECTOR_PARAMETERED_AUTOSHAPE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_3D_AIRSPACE:
                contentMask = MASK_AREA;
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ARROW:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_ROUTE:
                contentMask = MASK_LINE;
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTLINE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWOPOINTARROW:
                contentMask = EnumSet.of(ShapeType.Rectangle, ShapeType.LineString);
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_RECTANGULAR_PARAMETERED_AUTOSHAPE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_TWO_POINT_RECT_PARAMETERED_AUTOSHAPE:
                contentMask = EnumSet.of(ShapeType.Rectangle);
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_POINT:
                contentMask = EnumSet.of(ShapeType.Point);
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_CIRCULAR_PARAMETERED_AUTOSHAPE:
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_CIRCULAR_RANGEFAN_AUTOSHAPE:
                contentMask = EnumSet.of(ShapeType.Circle);
                break;
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_DONOTDRAW:
                return; // ignore, not rendered
            case MilStd2525cDrawRuleConstants.DRAW_CATEGORY_UNKNOWN:
            default:
                contentMask = EnumSet.noneOf(ShapeType.class);
                break;
        }
        p.symbols.add(new SymbolAdapter(interop.getBasicSymbolId(symbol), symbol, contentMask, paths, interop));
    }

    void fillAllSymbols(FolderAdapter folder, IMilStd2525cInterop<SymbolDef, ?, ?> interop)
    {
        for(Symbol symbol : folder.symbols)
        {
            allSymbols.put(interop.getBasicSymbolId(symbol.getCode()), symbol);
            folder.contentMask.addAll(symbol.getContentMask());
        }
        for(Folder child : folder.children)
        {
            fillAllSymbols((FolderAdapter) child, interop);
            folder.contentMask.addAll(((FolderAdapter) child).contentMask);
        }
    }

    static void parseDescriptions(IResourceManager resources) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try(InputStream res = resources.openRawResource("symbolconstant2525d", ResourceType.Raw)) {
            if(res == null)
                return;
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(res);

            NodeList list = doc.getElementsByTagName("SYMBOL");
            for (int i=0; i < list.getLength(); i++)
            {
                org.w3c.dom.Node node = list.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String code = element.getElementsByTagName("SYMBOLID").item(0).getTextContent();
                    String summary = element.getElementsByTagName("SUMMARY").item(0).getTextContent();
                    // A summary value of NONE means the element was found in the specification but did not
                    // contain a description.
                    // A summary value of CF means "can't find" meaning the corresponding element could not
                    // be found in the specification at all
                    if (summary.equals("NONE") || summary.equals("CF")) {
                        continue;
                    }
                    if (!symbolSummary.containsKey(code)) {
                        symbolSummary.put(code, summary);
                    } else {
                        Log.d(TAG, "Symbol map already contains an entry for " + code);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    final static class SymbolAdapter<SymbolDef> implements Symbol {
        public final SymbolDef info;
        public final String code;
        final IMilStd2525cInterop<SymbolDef, ?, ?> interop;

        public final EnumSet<ShapeType> contentMask;

        final String[] path;

        SymbolAdapter(String code, SymbolDef info, EnumSet<ShapeType> contentMask, String[] path, IMilStd2525cInterop<SymbolDef, ?, ?> interop) {
            this.info = info;
            this.interop = interop;
            this.code = code;
            this.contentMask = contentMask;
            this.path = path.clone();
        }

        @Override
        public String getName() {
            return interop.getDescription(info);
        }

        @Override
        public EnumSet<ShapeType> getContentMask() {
            return contentMask;
        }

        @Override
        public String getFullName() {
            if(path.length > 2)
                return path[path.length-2] + ", " + interop.getDescription(info);
            return interop.getDescription(info);
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getSummary() {
            return symbolSummary.get(code);
        }

        @Override
        public Drawable getPreviewDrawable(int color) {
            Map<String, String> symRenderAttribs = new HashMap<>();

            symRenderAttribs.put(MilStd2525cAttributes.PixelSize, "128");
            symRenderAttribs.put(MilStd2525cAttributes.DrawAsIcon,
                    contentMask.contains(ShapeType.Point) ?
                            "false" : "true");
            symRenderAttribs.put("SYMSTD", "2525C");

            int fontSize = AtakMapView.getDefaultTextFormat().getFontSize();
            // handle civilian unit types
            final String basicId = interop.getBasicSymbolId(code);
            if (basicId.startsWith("S*A*C") ||
                basicId.startsWith("S*S*X") ||
                basicId.startsWith("S*G*UULC") ||
                basicId.startsWith("S*G*EVC")) {

                symRenderAttribs.put(MilStd2525cAttributes.FillColor, MilStd2525cSymbologyProviderBase.PURPLE_ICON_FILL);
            }

            String iconCode = code;
            if(contentMask.contains(ShapeType.Point) && interop.getAffiliationLetterCode(code) == '*')
                iconCode = interop.setAffiliation(code, "U"); // render unknown as default it not specified

            Bitmap bmp = interop.renderSinglePointIcon(iconCode, Collections.emptyMap(), symRenderAttribs, null, "serif", 0, fontSize);
            if(bmp == null)
                return null;

            Drawable d = new BitmapDrawable(bmp);
            if(color != 0)
                d.setColorFilter(new ColorFilter(color, ColorBlendMode.SrcIn));

            return d;
        }
    }

    final static class FolderAdapter implements Folder {

        private final Map<String, Folder> childrenm = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });
        public Collection<Folder> children = childrenm.values();
        public Set<Symbol> symbols = new TreeSet<>(new Comparator<Symbol>() {
            @Override
            public int compare(Symbol a, Symbol b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        final String name;
        EnumSet<ShapeType> contentMask = EnumSet.noneOf(ShapeType.class);

        FolderAdapter(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public EnumSet<ShapeType> getContentMask() {
            return contentMask;
        }

        @Override
        public Collection<Folder> getChildren() {
            return children;
        }

        @Override
        public Collection<Symbol> getSymbols() {
            return symbols;
        }
    }
}

