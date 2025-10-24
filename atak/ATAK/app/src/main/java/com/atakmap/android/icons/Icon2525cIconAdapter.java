
package com.atakmap.android.icons;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.atakmap.android.config.FiltersConfig;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.AssetMapDataRef;
import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.IconUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.cot.detail.Role;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;

/**
 * Moved logic from IconsMapAdapter into this class
 * Adapt icon based on CoT/2525C symbols
 */
public class Icon2525cIconAdapter implements IconAdapter {

    private static final String TAG = "Icon2525cIconAdapter";

    public static final int MINIMAL_REPRESENTATION = 0xDDDD;

    private Context _context;
    /** `MapDataRef.getUri()` -> icon */
    private final Map<String, Icon> _iconCache = new ConcurrentHashMap<>();
    /** `Marker.getType()` -> icon data ref */
    private final Map<String, MapDataRef> _typeIconRefMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> teamColors = new ConcurrentHashMap<>();

    // Ability to add additional implementations of icon adapters to the system 
    // that still perform the work on 2525c markers
    private final static ConcurrentLinkedQueue<IconAdapter> adapters = new ConcurrentLinkedQueue<>();

    private final AtakPreferences prefs;

    /** 
     * Register in a more capable 2525 icon adapter to be used as a higher priority than the 
     * current 2525 adapter.
     * @param adapter the custom adapter which would further augment  the current 2525c
     * capability.
     */
    public static void addAdapter(final IconAdapter adapter) {
        if (!adapters.contains(adapter))
            adapters.add(adapter);
    }

    /** 
     * Removes an adapter previously registered by the call to addAdapter
     * @param adapter the custom adapter which would further augment  the current 2525c
     * capability.
     */
    public static void removeAdapter(final IconAdapter adapter) {
        adapters.remove(adapter);
    }

    private FiltersConfig _filters; // do not use directly for this class
    /** records filter lookup misses (`ConcurrentHashMap` cannot contain `null` values) */
    private final Set<String> _noFilterTypes = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    public Icon2525cIconAdapter(final Context context) {
        try {
            _context = context;

            AssetManager assetMgr = context.getAssets();
            _filters = FiltersConfig.parseFromStream(assetMgr
                    .open("filters/icon_filters.xml"));
            if (_filters != null)
                _filters.setComparator("type",
                        new FiltersConfig.StringStartsWithComparator());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize", e);
        }
        prefs = AtakPreferences.getInstance(context);
    }

    @Override
    public void dispose() {
        for (IconAdapter adapter : adapters) {
            try {
                adapter.dispose();
            } catch (Exception e) {
                Log.e(TAG, "error disposing of an adapter", e);
            }
        }

        _context = null;
        _filters = null;
        _iconCache.clear();
        _noFilterTypes.clear();
        _typeIconRefMap.clear();
    }

    /**
     * Given a marker, will lookup the assetPath for the icon.   Use as a wrapper 
     * for the robust yet slow _filter implementation.
     */
    public String lookupFromFilter(final Marker m) {
        String type = m.getType();
        if (type == null)
            return null;
        String assetPath = null;

        if (_noFilterTypes.contains(type))
            return null;

        FiltersConfig.Filter f = _filters
                .lookupFilter(Collections.singletonMap("type", type));
        if (f != null) {
            assetPath = f.getValue();
        }

        // if type was not filtered, record for future short-circuit
        if (assetPath == null)
            _noFilterTypes.add(type);
        return assetPath;

    }

    private String jumperAbove(final String name) {
        String retval = name.toLowerCase(LocaleUtil.getCurrent());
        if (retval.equals("dark blue"))
            retval = "darkblue";
        else if (retval.equals("dark green"))
            retval = "darkgreen";
        return "icons/above_" + retval + ".png";
    }

    private String jumperBelow(final String name) {
        String retval = name.toLowerCase(LocaleUtil.getCurrent());
        if (retval.equals("dark blue"))
            retval = "darkblue";
        else if (retval.equals("dark green"))
            retval = "darkgreen";
        return "icons/below_" + retval + ".png";
    }

    public static int teamToColor(final String name) {
        Integer retval = teamColors.get(name);
        if (retval != null)
            return retval;

        String val = name.toLowerCase(LocaleUtil.getCurrent());
        switch (val) {
            case "orange":
                val = "#FFFF7700";
                break;
            case "maroon":
                val = "#FF7F0000";
                break;
            case "purple":
                val = "#FF7F007F";
                break;
            case "dark blue":
                val = "#FF00007F";
                break;
            case "teal":
                val = "#FF007F7F";
                break;
            case "dark green":
                val = "#FF007F00";
                break;
            case "brown":
                val = "#FFA0714F";
                break;
            case "cyan":
                val = "#FF00FFFF";
                break;
            case "blue":
                val = "#FF0000FF";
                break;
            case "green":
                val = "#FF00FF00";
                break;
            case "red":
                val = "#FFFF0000";
                break;
            case "magenta":
                val = "#FFFF00FF";
                break;
            case "yellow":
                val = "#FFFFFF00";
                break;
            case "rad sensor":
                val = "yellow";
                break;
            case "pink":
                val = "#FFFF1493";
                break;
        }

        try {
            retval = Color.parseColor(val);
        } catch (Exception e) {
            retval = Color.WHITE;
        }

        teamColors.put(name, retval);

        return retval;
    }

    @Override
    public boolean adapt(final Marker marker) {
        return adapt2525cMarkerIcon(marker);
    }

    /**
     * Return true if marker icon was adapted
     * 
     * @param marker the marker to adapt
     * @return true if the marker was adapted
     */
    boolean adapt2525cMarkerIcon(final Marker marker) {
        MapDataRef iconRef = null;
        int color = 0;
        final String type = marker.getType();

        // Check any user added adapters
        for (IconAdapter adapter : adapters) {
            try {
                if (adapter.adapt(marker))
                    return true;
            } catch (Exception e) {
                Log.e(TAG, "error attempting to adapt marker", e);
            }
        }

        if (marker.hasMetaValue("iconUri")) {
            String uri = marker.getMetaString("iconUri", null);
            // FIXME: need to revisit to make more flexible - right now 
            // MapDataRef.parseUri() is being too simple for what I am doing
            // parse of asset://icons/foo gives me AssetMapDataRef point to just 
            // foo and not icons/foo
            if (uri != null) {
                iconRef = new AssetMapDataRef(uri);
            }

        }

        if (type != null && !marker.getMetaBoolean("readiness", true)) {
            // Check for affiliation and display correct icon. 99.9% of the time
            // it should be friendly.
            String iconPath = "icons/damaged.png"; // Friendly by default
            if (type.startsWith("a-h")) // Hostile
                iconPath = "icons/damaged_hostile.png";
            else if (type.startsWith("a-n")) // Neutral
                iconPath = "icons/damaged_neutral.png";
            else if (type.startsWith("a-u")) // Unknown
                iconPath = "icons/damaged_unknown.png";
            iconRef = new AssetMapDataRef(iconPath);
        }

        if (marker.hasMetaValue("team") &&
                (!marker.getMetaBoolean("displayAsSymbologyGraphic", false)
                        && !prefs.get("displayAsSymbologyGraphic", false))) {
            color = teamToColor(marker.getMetaString("team", "white"));

            //build out the ATAK icon
            final String role = marker.getMetaString("atakRoleType",
                    "Team Member");
            String iconPath = "icons/roles/";
            if (role.equalsIgnoreCase("HQ")) {
                iconPath += "hq";
            } else if (role.equalsIgnoreCase("Team Lead")) {
                iconPath += "teamlead";
            } else if (role.equalsIgnoreCase("Sniper")) {
                iconPath += "sniper";
            } else if (role.equalsIgnoreCase("K9")) {
                iconPath += "k9";
            } else if (role.equalsIgnoreCase("RTO")) {
                iconPath += "rto";
            } else if (role.equalsIgnoreCase("Medic")) {
                iconPath += "medic";
            } else if (role.equalsIgnoreCase("Forward Observer")) {
                iconPath += "forwardobserver";
            } else {
                iconPath += "team";
            }

            final Role r = CotMapComponent.getInstance().getRolesManager()
                    .getRole(
                            marker.getMetaString("customRole", null),
                            marker.getMetaString("customRoleAbbreviation",
                                    null));
            if (r != null) {
                iconPath = "icons/roles/team";
            }

            final String how = marker.getMetaString("how", "");
            if (how.startsWith("h-"))
                iconPath += "_human";
            else if (how.startsWith("m-g-l"))
                iconPath += "_nogps";

            iconPath += ".png";
            if (r != null) {
                Bitmap bitmap = CotMapComponent.getInstance().getRolesManager()
                        .getIcon(r.getAbbreviation(), iconPath, _context);
                String encoded = IconUtilities.encodeBitmap(bitmap);
                iconRef = MapDataRef.parseUri(encoded);
            }

            if (!how.equals("h-e") && marker.hasMetaValue("jumper")) {
                String position = marker.getMetaString("jumper", "");
                String team = marker.getMetaString("team", "white");
                if (position.equals("below"))
                    iconPath = jumperBelow(team);
                else if (position.equals("above"))
                    iconPath = jumperAbove(team);
                color = 0; // bit of a hack, but don't want the blended color for the jumper
                           // icons, the icon's color
                           // will get reset to it's original color when the jumper reaches
                           // the ground and the
                           // default team icon is used
            }
            if (r == null)
                iconRef = new AssetMapDataRef(iconPath);
        } else if (marker.hasMetaValue("color")
                && marker.getMetaInteger("color", 0) != 0)
            color = marker.getMetaInteger("color", 0);

        if (iconRef == null)
            iconRef = _typeIconRefMap.get(type);

        if (type != null && iconRef == null) {
            String assetPath = null;
            do {
                // check filters
                if (_filters != null) {
                    assetPath = lookupFromFilter(marker);
                    if (assetPath != null)
                        break;
                }
                // look up 2525c
                assetPath = _findAssetPath(type);
            } while (false);
            if (assetPath != null) {
                iconRef = new AssetMapDataRef(assetPath);
                _typeIconRefMap.put(type, iconRef);
            }
        }

        if (iconRef == null && !MapItem.EMPTY_TYPE.equals(type) &&
                !ATAKUtilities.isSelf(MapView.getMapView(), marker) &&
                marker.getIcon() == null) {
            //Log.d(TAG, "No icon found for type: " + type);
            color = 0;

            if (type != null) {
                if (type.startsWith("a-f") || type.startsWith("a-a")) { //friendly, assumed
                    iconRef = new AssetMapDataRef("icons/unknown-type-f.png");
                } else if (type.startsWith("a-n")) { //neutral
                    iconRef = new AssetMapDataRef("icons/unknown-type-n.png");
                } else if (type.startsWith("a-h") || type.startsWith("a-j")
                        || type.startsWith("a-k") || type.startsWith("a-s")) {//hostile, joker, faker, suspect
                    iconRef = new AssetMapDataRef("icons/unknown-type-h.png");
                } else if (type.startsWith("a-u") || type.startsWith("a-p")) {// unknown, pending
                    iconRef = new AssetMapDataRef("icons/unknown-type-u.png");
                } else {
                    iconRef = new AssetMapDataRef("icons/unknown-type.png");
                }
            }
        }

        if (iconRef != null) {
            final String iconRefUri = iconRef.toUri();
            Icon i = _iconCache.get(iconRefUri);
            if (i == null) {
                Icon.Builder builder = new Icon.Builder();

                if (iconRefUri
                        .startsWith("asset://" + Icon2525cPallet.ASSET_PATH)) {
                    String milsym = iconRefUri.replace(
                            "asset://" + Icon2525cPallet.ASSET_PATH, "");
                    milsym = milsym.replace(".png", "")
                            .toUpperCase(LocaleUtil.US);
                    Log.d(TAG, "request for milsym: " + milsym + " " + " from: "
                            + iconRefUri + " for: " + type);
                    if (!milsym.isEmpty()) {
                        try {
                            gov.tak.api.commons.graphics.Bitmap bmp = SymbologyProvider
                                    .renderSinglePointIcon(milsym,
                                            new AttributeSet(), null);
                            Bitmap abmp = MarshalManager.marshal(bmp,
                                    gov.tak.api.commons.graphics.Bitmap.class,
                                    Bitmap.class);

                            builder.setImageUri(0,
                                    IconUtilities.encodeBitmap(abmp));
                        } catch (Exception e) {
                            Log.e(TAG,
                                    "error occurred obtaining " + milsym
                                            + " from the provider for " + type,
                                    e);
                            builder.setImageUri(0, iconRefUri);
                        }
                    } else {
                        builder.setImageUri(0, iconRefUri);
                    }
                } else {
                    builder.setImageUri(0, iconRefUri);
                }
                builder.setAnchor(Icon.ANCHOR_CENTER, Icon.ANCHOR_CENTER);
                builder.setSize(32, 32);

                int affinityColor = 0xFFFFFFFF;
                if (type != null && type.length() > 3) {
                    switch (type.substring(2, 3)) {
                        case "f":
                            affinityColor = 0xFF80E0FF;
                            break;
                        case "n":
                            affinityColor = 0xFFAAFFAA;
                            break;
                        case "h":
                            affinityColor = 0xFFFF8080;
                            break;
                        case "u":
                            affinityColor = 0xFFFFFF80;
                    }
                }
                final String dotUri = "android.resource://"
                        + _context.getPackageName() + "/"
                        + R.drawable.white_dot;
                builder.setImageUri(MINIMAL_REPRESENTATION, dotUri);
                builder.setColor(MINIMAL_REPRESENTATION, affinityColor);

                i = builder.build();
                _iconCache.put(iconRefUri, i);
            }
            if (marker.getIcon() != i
                    || marker.getIcon().getColor(0) != color) {
                // if icon is colorized, build new from uncolored icon
                if (color != 0) {
                    Icon.Builder builder = new Icon.Builder(i);
                    builder.setColor(0, color);
                    i = builder.build();
                }
                marker.setIcon(i);
            }

        }

        return iconRef != null;
    }

    private boolean _checkAsset(String pathName) {
        boolean found = false;
        try {
            AssetFileDescriptor fd = _context.getAssets().openFd(pathName);
            fd.close();
            found = true;
        } catch (IOException e) {
            // nothing
        }
        return found;
    }

    private String _findAssetPath(final String cotType) {

        String type2525 = CotUtils.mil2525cFromCotType(cotType);
        if (FileSystemUtils.isEmpty(type2525))
            return null;

        return Icon2525cPallet.ASSET_PATH + type2525 + ".png";
    }

}
