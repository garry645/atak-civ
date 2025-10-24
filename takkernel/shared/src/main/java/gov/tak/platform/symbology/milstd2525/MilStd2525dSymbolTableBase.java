package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.log.Log;
import com.atakmap.map.AtakMapView;
import com.atakmap.util.Collections2;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import armyc2.c5isr.renderer.utilities.DrawRules;
import armyc2.c5isr.renderer.utilities.MSInfo;
import armyc2.c5isr.renderer.utilities.MSLookup;
import armyc2.c5isr.renderer.utilities.MilStdAttributes;
import armyc2.c5isr.renderer.utilities.SymbolUtilities;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.ColorBlendMode;
import gov.tak.api.commons.graphics.ColorFilter;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;

abstract class MilStd2525dSymbolTableBase<Node> implements ISymbolTable
{
    final static int VERSION_Dch1 = 11;
    final static int VERSION_E = 13;

    private static final String TAG = "MilStd2525dSymbolTable";
    final int version;
    static final Map<String, String> symbolSummary = new HashMap<>();

    FolderAdapter root = new FolderAdapter(null, null);
    Map<String, Symbol> allSymbols = new HashMap<>();
    final IMilStd2525dInterop interop;

    MilStd2525dSymbolTableBase(int version) {
        this.version = version;
        interop = new MilStd2525dInterop();
    }

    abstract void initRoot();

    @Override
    public synchronized Folder getRoot() {
        if(root.childrenm.isEmpty()) {
            initRoot();
            fillAllSymbols(root);
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
        getRoot();
        try {
            int version = Integer.parseInt(code.substring(0, 2));

            if(!(version == this.version || (this.version == 11 && version == 10))) {
                return null;
            }
        }
        catch(Exception e) {
            return null;
        }
        code = SymbolUtilities.getBasicSymbolID(code);
        return allSymbols.get(code);
    }

    static <Node> String getCode(Node p, int version, Interop<Node> nodeInterop) {
        // transform basic code to full code, per MIL-STD-2525D A.5.2
        StringBuilder code = new StringBuilder();
        // version (correct value?)
        if(version == 11) code.append("10");
        else code.append(version);
        code.append("00"); // standard identity
        code.append(nodeInterop.getSymbolSetCode(p)); // symbol set
        code.append("00"); // status
        code.append("00"); // HQ/Task Force/Dummy Amplifier
        code.append(nodeInterop.getCode(p)); // Entity+Entity Type+Entity Subtypw
        code.append("00"); // Sector 1 Modifier
        code.append("00"); // Sector 2 Modifier
        if(version == 13)
            code.append("0000000000");
        return code.toString();
    }

    static ShapeType getShapeType(MSInfo symbol) {
        final String geomType = symbol.getGeometry();
        if(geomType == null)
            return null;
        switch(geomType.toLowerCase()) {
            case "point" :
                return ShapeType.Point;
            case "line" :
                return ShapeType.LineString;
            case "area":
                return ShapeType.Polygon;
            default :
                return null;
        }
    }

    static <Node> EnumSet<ShapeType> push(FolderAdapter f, Node p, Stack<String> pathStack, int version, IMilStd2525dInterop interop, Interop<Node> nodeInterop) {
        String code = getCode(p, version, nodeInterop);
        MSInfo symbol = MSLookup.getInstance().getMSLInfo(nodeInterop.getSymbolSetCode(p)+nodeInterop.getCode(p), version);
        if(nodeInterop.getChildren(p).isEmpty()) {
            if(symbol == null)
                return EnumSet.noneOf(ShapeType.class);
            return push(f, code, symbol, pathStack, interop);
        } else {
            FolderAdapter c = (FolderAdapter) f.childrenm.get(nodeInterop.getName(p));
            if(c == null) {
                if(symbol != null && getShapeType(symbol) == ShapeType.Point && getPreviewDrawable(interop, getCode(p, version, nodeInterop), -1) != null) {
                    //Drawable d = getPreviewDrawable(interop, getCode(p, nodeInterop), -1);
                    if(symbol.getDrawRule() != DrawRules.DONOTDRAW) {
                        push(f, code, symbol, pathStack, interop);
                    }
                    c = new SymbolFolderAdapter(getCode(p, version, nodeInterop), symbol, pathStack.toArray(new String[0]), f, interop);
                } else {
                    c = new FolderAdapter(nodeInterop.getName(p), f);
                }
                f.childrenm.put(nodeInterop.getName(p), c);
            }
            for(Node n : nodeInterop.getChildren(p)) {
                pathStack.push(nodeInterop.getName(n));
                c.contentMask.addAll(push(c, n, pathStack, version, interop, nodeInterop));
                pathStack.pop();
            }
            f.contentMask.addAll(c.contentMask);
            if(c.contentMask.isEmpty())
                f.childrenm.remove(nodeInterop.getName(p));
            return f.contentMask;
        }
    }

    static EnumSet<ShapeType> push(FolderAdapter f, String code, MSInfo symbol, Stack<String> pathStack, IMilStd2525dInterop interop) {
        final ShapeType geomType = getShapeType(symbol);
        if(geomType == null)
            return EnumSet.noneOf(ShapeType.class);
        EnumSet<ShapeType> contentMask;
        switch(geomType) {
            case Point:
                contentMask = EnumSet.of(ShapeType.Point);
                break;
            case LineString:
                contentMask = EnumSet.copyOf(MASK_LINE);
                break;
            case Polygon:
                contentMask = EnumSet.copyOf(MASK_AREA);
                break;
            default :
                return EnumSet.noneOf(ShapeType.class);
        }
        f.symbols.add(new SymbolAdapter(code, symbol, contentMask, (String[])pathStack.toArray(new String[0]), f, interop));
        return contentMask;

    }

    void fillAllSymbols(FolderAdapter folder)
    {
        for(Folder child : folder.children)
        {
            fillAllSymbols((FolderAdapter) child);
            if(child instanceof Symbol)
                allSymbols.put(SymbolUtilities.getBasicSymbolID(((Symbol)child).getCode()), (Symbol)child);
        }

        for(Symbol symbol : folder.symbols)
        {
            allSymbols.put(SymbolUtilities.getBasicSymbolID(symbol.getCode()), symbol);
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
                    String code = element.getElementsByTagName("CODE").item(0).getTextContent();
                    if (!code.equals("00000000000000000000")) {
                        String summary = element.getElementsByTagName("SUMMARY").item(0).getTextContent();
                        // A summary value of NONE means the element was found in the specification but did not
                        // contain a description.
                        // A summary value of CF means "can't find" meaning the corresponding element could not
                        // be found in the specification at all
                        if (summary.equals("NONE") || summary.equals("CF")) {
                            continue;
                        }
                        final String symid = SymbolUtilities.getBasicSymbolID(code);
                        if (!symbolSummary.containsKey(symid)) {
                            symbolSummary.put(SymbolUtilities.getBasicSymbolID(symid), summary);
                        } else {
                            Log.d(TAG, "Symbol map already contains an entry for " + code);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    final static class SymbolAdapter implements Symbol, Entry2 {
        public final MSInfo info;
        public final String code;

        public final EnumSet<ShapeType> contentMask;

        final String[] path;
        final Folder parent;
        final IMilStd2525dInterop interop;

        SymbolAdapter(String code, MSInfo info, EnumSet<ShapeType> contentMask, String[] path, Folder parent, IMilStd2525dInterop interop) {
            this.info = info;
            this.code = code;
            this.contentMask = contentMask;
            this.path = path.clone();
            this.parent = parent;
            this.interop = interop;
        }

        @Override
        public String getName() {
            return info.getName();
        }

        @Override
        public EnumSet<ShapeType> getContentMask() {
            return contentMask;
        }

        @Override
        public Folder getParent() {
            return parent;
        }

        @Override
        public String getFullName() {
            if(path.length > 2)
                return path[path.length-2] + ", " + info.getName();
            return info.getName();
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getSummary() {
            return symbolSummary.get(SymbolUtilities.getBasicSymbolID(code));
        }

        @Override
        public Drawable getPreviewDrawable(int color) {
            return MilStd2525dSymbolTableBase.getPreviewDrawable(interop, code, color);
        }
    }

    static class FolderAdapter implements Folder, Entry2 {

        Map<String, ISymbolTable.Folder> childrenm = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });
        public Collection<Folder> children = childrenm.values();
        public Set<Symbol> symbols = new TreeSet<>(new Comparator<ISymbolTable.Symbol>() {
            @Override
            public int compare(ISymbolTable.Symbol a, ISymbolTable.Symbol b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        final String name;
        final Folder parent;
        EnumSet contentMask = EnumSet.noneOf(ShapeType.class);

        FolderAdapter(String name, Folder parent) {
            this.name = name;
            this.parent = parent;
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
        public Folder getParent() {
            return parent;
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

    static class SymbolFolderAdapter extends FolderAdapter implements Symbol {
        public final MSInfo info;
        public final String code;

        final String[] path;
        final IMilStd2525dInterop interop;

        SymbolFolderAdapter(String code, MSInfo info, String[] path, Folder parent, IMilStd2525dInterop interop) {
            super(info.getName(), parent);

            this.info = info;
            this.code = code;
            this.path = path.clone();
            this.interop = interop;
        }

        @Override
        public String getName() {
            return info.getName();
        }

        @Override
        public String getFullName() {
            if(path.length > 2)
                return path[path.length-2] + ", " + info.getName();
            return info.getName();
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getSummary() {
            return symbolSummary.get(SymbolUtilities.getBasicSymbolID(code));
        }

        @Override
        public Drawable getPreviewDrawable(int color) {
            return MilStd2525dSymbolTableBase.getPreviewDrawable(interop, code, color);
        }
    }

    static Drawable getPreviewDrawable(IMilStd2525dInterop interop, String code, int color) {
        Map<String, String> symRenderAttribs = new HashMap<>();

        int fontSize = AtakMapView.getDefaultTextFormat().getFontSize();

        symRenderAttribs.put(MilStdAttributes.PixelSize, "128");
        symRenderAttribs.put(MilStdAttributes.DrawAsIcon, "true");
        final String basicId = SymbolUtilities.getBasicSymbolID(code);
        switch(basicId.substring(0, 4)) {
            case "0112": // air / civilian
            case "0512": // space / civilian
            case "1111": // land civilian unit / civilian
            case "1516": // land equipment / civilian
            case "3014": // sea surface / civilian
            case "3512": // sea subsurface / civilian
                symRenderAttribs.put(MilStdAttributes.FillColor, MilStd2525dSymbologyProviderBase.PURPLE_ICON_FILL);
                break;
            default :
                break;
        }
        Bitmap bmp = interop.renderSinglePointIcon(code, Collections.emptyMap(), symRenderAttribs, null,"serif", 0, fontSize);
        if(bmp == null)
            return null;

        Drawable d = new BitmapDrawable(bmp);
        if(color != 0)
            d.setColorFilter(new ColorFilter(color, ColorBlendMode.SrcIn));

        return d;
    }

    interface Interop<Node> {
        String getName(Node node);
        String getSymbolSetCode(Node node);
        String getCode(Node node);
        Collection<Node> getChildren(Node node);
    }
}
