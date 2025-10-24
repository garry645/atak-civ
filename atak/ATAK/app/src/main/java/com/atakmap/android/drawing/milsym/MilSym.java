
package com.atakmap.android.drawing.milsym;

import android.content.Context;

import androidx.annotation.NonNull;

import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.cotdetails.extras.ExtraDetailsManager;
import com.atakmap.android.cotdetails.extras.ExtraDetailsProvider;
import com.atakmap.android.drawing.DrawingToolsMapComponent;
import com.atakmap.android.icons.Icon2525cIconAdapter;
import com.atakmap.android.icons.IconAdapter;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.menu.MapMenuHandler;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.android.user.EnterLocationDropDownReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.DeveloperOptions;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.util.Collections2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.util.Disposable;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525cSymbologyProvider;

public final class MilSym implements Disposable, SymbologyProvider.SymbologyProvidersChangedListener {
    private final Context context;
    private final MapView mapView;

    private MapMenuHandler menuHandler;

    private final MilSymManager manager;
    private final MilSymLayer layer;

    private final MilSymReceiver receiver;

    private final MilSymIconAdapter iconAdapter = new MilSymIconAdapter();

    private final ExtraDetailsProvider extraDetailsProvider;
    private final MilSymDetailHandler2 detailHandler;
    private final static Map<ISymbolTable, Map<String, List<ISymbolTable.Folder>>> symbolPathMap = new HashMap<>();

    private final static Map<String, String> translationTable = new HashMap<>();
    private final static Map<ISymbolTable, Map<String, String>> transNameToCode = new HashMap<>();

    private MilSymPallet milSymPallet;

    @Override
    public void onSymbologyProvidersChanged() {
        Collection<ISymbologyProvider>  providers = SymbologyProvider.getProviders();

        for(ISymbologyProvider provider : providers) {
            buildSymbolPathMapping(provider.getSymbolTable(), null);
        }
    }

    static class MilSymIconAdapter implements IconAdapter {

        @Override
        public boolean adapt(Marker marker) {
            final String milsym = marker.getMetaString("milsym", null);
            // check has symbol code
            if(milsym == null)
                return false;
            // quick check for renders as single point icon without actually rendering
            return (SymbologyProvider.getDefaultSourceShape(milsym) == ShapeType.Point);
        }

        @Override
        public void dispose() {
        }
    }

    public MilSym(Context context, MapView mapView) {

        this.context = context;
        this.mapView = mapView;

        onSymbologyProvidersChanged();
        SymbologyProvider.addSymbologyProvidersChangedListener(this);

        detailHandler = new MilSymDetailHandler2();
        manager = new MilSymManager(mapView, this.context);
        layer = new MilSymLayer(manager.getDataStore());

        receiver = new MilSymReceiver(mapView, this.context,
                symbolPathMap);

        CotDetailManager.getInstance().registerHandler(detailHandler);

        Icon2525cIconAdapter.addAdapter(iconAdapter);

        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED,
                manager);
        mapView.getMapEventDispatcher()
                .addMapEventListener(MapEvent.ITEM_REMOVED, manager);
        mapView.getMapTouchController().addDeconflictionListener(manager);

        if (menuHandler == null)
            menuHandler = new TacticalGraphicMenuFactory(this.context);

        MapMenuReceiver.getInstance().registerMapMenuHandler(menuHandler);

        mapView.addLayer(MapView.RenderStack.POINT_OVERLAYS, layer);

        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        MilSymReceiver.ACTION_SELECT_TYPE));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        MilSymReceiver.ACTION_REVERSE_PATH));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolbarBroadcastReceiver.SET_TOOLBAR));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolbarBroadcastReceiver.UNSET_TOOLBAR));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolbarBroadcastReceiver.OPEN_TOOLBAR));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolManagerBroadcastReceiver.BEGIN_TOOL));
        AtakBroadcast.getInstance().registerReceiver(receiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolManagerBroadcastReceiver.END_TOOL));
        AtakBroadcast.getInstance().registerReceiver(
                receiver.getToolbarExtender(),
                new AtakBroadcast.DocumentedIntentFilter(
                        DrawingToolbarExtender.SHAPE_CREATED));
        AtakBroadcast.getInstance().registerReceiver(
                receiver.getToolbarExtender(),
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolbarBroadcastReceiver.SET_TOOLBAR));
        AtakBroadcast.getInstance().registerReceiver(
                receiver.getToolbarExtender(),
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolbarBroadcastReceiver.UNSET_TOOLBAR));
        AtakBroadcast.getInstance().registerReceiver(
                receiver.getToolbarExtender(),
                new AtakBroadcast.DocumentedIntentFilter(
                        ToolManagerBroadcastReceiver.END_TOOL));

       if (DeveloperOptions.getIntOption("milsym.single-point", 0) != 0)
        {
            ISymbologyProvider symbology = new MilStd2525cSymbologyProvider();
            milSymPallet = new MilSymPallet(mapView, this.context, symbology);
            EnterLocationDropDownReceiver.getInstance(mapView)
                    .addPallet(milSymPallet);
        }
        DrawingToolsMapComponent.getGroup()
                .addOnItemListChangedListener(manager);
        DrawingToolsMapComponent.getGroup()
                .addOnItemListChangedListener(receiver.getToolbarExtender());
        mapView.getRenderer3().addOnCameraChangedListener(manager);

        extraDetailsProvider = new MilSymExtraDetails(mapView, this.context);
        ExtraDetailsManager.getInstance().addProvider(extraDetailsProvider);
    }

    /**
     * Obtain the symbol name from the Symbol object
     * @param symbol the symbol.
     * @return the translated name if it exists otherwise english
     */
    public static String getTranslatedName(
            final @NonNull ISymbolTable.Entry symbol) {
        String name = null;
        if (symbol instanceof ISymbolTable.Symbol)
            name = translationTable
                    .get(((ISymbolTable.Symbol) symbol).getCode());
        else if (symbol instanceof ISymbolTable.Folder)
            name = translationTable.get(symbol.getName());

        if (FileSystemUtils.isEmpty(name))
            name = symbol.getName();

        return name;
    }

    /**
     * Gets a translated full name from the current list of names
     * @param symbol the symbol
     * @return the full name
     */
    public static String getTranslatedFullName(
            final @NonNull ISymbolTable.Symbol symbol) {
        String fName = symbol.getFullName();
        if (symbol.getName().equals(fName))
            return getTranslatedName(symbol);
        else {
            List<ISymbolTable.Folder> folders = null;

            for(Map<String, List<ISymbolTable.Folder>> map : symbolPathMap.values())
            {
                folders = map.get(symbol.getCode());
                if(folders != null)
                    break;
            }
            if (folders != null && folders.size() > 2) {
                ISymbolTable.Folder f = folders.get(folders.size() - 2);
                fName = getTranslatedName(f) + ", " + getTranslatedName(symbol);
                //fName = f.getName() + ", " + symbol.getName();
            }

            return fName;
        }
    }

    /**
     * Implementation of a search using the default provider
     *
     * @param searchText the text used for matching
     * @param contentFilter the filter based on the types of shapes
     * @return a list of symbols that match the search text
     */
    public static List<ISymbolTable.Symbol> find(
            final @NonNull String searchText,
            EnumSet<ShapeType> contentFilter) {
        return find(ATAKUtilities.getDefaultSymbologyProvider(), searchText,
                contentFilter);
    }

    /**
     * Implementation of a search using a provided symbology provider
     *
     * @param provider the symbology provider
     * @param searchText the text used for matching
     * @param contentFilter the filter based on the types of shapes
     * @return a list of symbols that match the search text
     */
    public static List<ISymbolTable.Symbol> find(ISymbologyProvider provider,
            final @NonNull String searchText,
            EnumSet<ShapeType> contentFilter) {
        List<ISymbolTable.Symbol> retval = new ArrayList<>();
        Map<String, String> providerMap = transNameToCode.get(provider.getSymbolTable());

        if(providerMap != null) {
            for (String key : providerMap.keySet())
                if (key.toLowerCase(LocaleUtil.getCurrent()).contains(
                        searchText.toLowerCase(LocaleUtil.getCurrent()))) {
                    ISymbolTable.Symbol symbol = provider.getSymbolTable()
                            .getSymbol(providerMap.get(key));
                    if (symbol != null
                            && Collections2.containsAny(symbol.getContentMask(),
                            contentFilter))
                        retval.add(symbol);
                }
        }

        // if no symbols were found based on translation, do last ditch attempt against API
        if (retval.isEmpty()) {
            retval.addAll(
                    provider.getSymbolTable().find(searchText, contentFilter));
        }

        return retval;
    }

    /**
     * Gets the nested structure for the symbols
     * @param symbol the symbol
     * @return the folder path it lives under or null
     */
    public static List<ISymbolTable.Folder> getFolderPath(
            final @NonNull ISymbolTable.Symbol symbol) {
        final Map<String, List<ISymbolTable.Folder>> map =
                symbolPathMap.get(ATAKUtilities.getDefaultSymbologyProvider().getSymbolTable());

        if(map != null)
            return map.get(symbol.getCode());
        return null;
    }

    private String generateString(final String usKey, final String usName,
            boolean folder) {
        // rules for resource name
        // lower case
        // _ replaces asterisk space hyphen and comma and parenthesis

        String resourceName = usKey.replaceAll("[*()\\- ,]", "_")
                .toLowerCase(LocaleUtil.US);

        if(resourceName.length() > 4 && resourceName.charAt(0) == 's' && resourceName.charAt(3) != 'p') {
            resourceName = resourceName.substring(0,3) + "p" +  resourceName.substring(4);
        }

        if (folder)
            resourceName = "ms2525c_folder_" + resourceName;
        Class<?> c = com.atakmap.app.R.string.class;
        int resID = 0;

        try {
            Field f = c.getField(resourceName);
            resID = f.getInt(null);
        } catch (Exception ignored) {
        }

        if (resID == 0) {
            return usName;
        }
        return context.getString(resID);
    }

    private void buildSymbolPathMapping(ISymbolTable symbolTable,
            List<ISymbolTable.Folder> path) {
        if (path == null)
            path = new ArrayList<>();

        if (symbolPathMap.get(symbolTable) != null)
            return;

        symbolPathMap.put(symbolTable, new HashMap<>());
        transNameToCode.put(symbolTable, new HashMap<>());
        buildSymbolPathMapping(symbolTable, symbolTable.getRoot(), path);
    }

    private void buildSymbolPathMapping(ISymbolTable symbolTable, ISymbolTable.Folder folder,
                                        List<ISymbolTable.Folder> path) {

        Map<String, List<ISymbolTable.Folder>> symbolTableMap = symbolPathMap.get(symbolTable);
        Map<String, String> symbolTransNameToCode = transNameToCode.get(symbolTable);

        //explicitly add root level translations
        {
            String root = "Friendly";
            String translation = generateString(root, root, false);
            translationTable.put(root, translation);

            root = "Neutral";
            translation = generateString(root, root, false);
            translationTable.put(root, translation);

            root = "Unknown";
            translation = generateString(root, root, false);
            translationTable.put(root, translation);

            root = "Hostile";
            translation = generateString(root, root, false);
            translationTable.put(root, translation);
        }
        for (ISymbolTable.Symbol symbol : folder.getSymbols()) {
            String s = symbol.getCode();
            if (symbolTableMap != null)
                symbolTableMap.put(s, path);

            String translation = generateString(s, symbol.getName(), false);
            translationTable.put(s, translation);
            if (symbolTransNameToCode != null)
                symbolTransNameToCode.put(translation, s);
        }
        for (ISymbolTable.Folder cFolder : folder.getChildren()) {
            ArrayList<ISymbolTable.Folder> cPath = new ArrayList<>(path);
            cPath.add(cFolder);
            translationTable.put(cFolder.getName(),
                    generateString(cFolder.getName(), cFolder.getName(), true));
            buildSymbolPathMapping(symbolTable, cFolder, cPath);
        }
    }

    private String toString(List<ISymbolTable.Folder> path,
            ISymbolTable.Symbol symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(symbol.getCode());
        sb.append(" // ");
        for (ISymbolTable.Folder p : path) {
            sb.append(p.getName());
            sb.append(" // ");
        }
        sb.append(" // ");
        sb.append(symbol.getName());
        sb.append(" // ");
        sb.append(symbol.getFullName());

        return sb.toString();
    }

    @Override
    public void dispose() {

        if (menuHandler != null)
            MapMenuReceiver.getInstance().unregisterMapMenuHandler(menuHandler);

        mapView.removeLayer(MapView.RenderStack.POINT_OVERLAYS, layer);
        AtakBroadcast.getInstance().unregisterReceiver(receiver);
        AtakBroadcast.getInstance()
                .unregisterReceiver(receiver.getToolbarExtender());

        mapView.getMapEventDispatcher()
                .removeMapEventListener(MapEvent.ITEM_ADDED, manager);
        mapView.getMapEventDispatcher()
                .removeMapEventListener(MapEvent.ITEM_REMOVED, manager);
        mapView.getMapTouchController().removeDeconflictionListener(manager);
        mapView.getRenderer3().removeOnCameraChangedListener(manager);

        DrawingToolsMapComponent.getGroup()
                .removeOnItemListChangedListener(manager);
        DrawingToolsMapComponent.getGroup()
                .removeOnItemListChangedListener(receiver.getToolbarExtender());

        if (milSymPallet != null)
            EnterLocationDropDownReceiver.getInstance(mapView)
                    .removePallet(milSymPallet);

        CotDetailManager.getInstance().unregisterHandler(detailHandler);
        ExtraDetailsManager.getInstance().removeProvider(extraDetailsProvider);

        Icon2525cIconAdapter.removeAdapter(iconAdapter);
    }

}
