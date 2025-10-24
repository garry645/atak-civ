
package com.atakmap.android.missionpackage;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.atakmap.android.config.FiltersConfig;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.filesharing.android.service.AndroidFileInfo;
import com.atakmap.android.filesharing.android.service.FileInfoPersistanceHelper;
import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.icons.UserIconDatabase;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.maps.AssetMapDataRef;
import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.SqliteMapDataRef;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.file.MissionPackageManifestAdapter;
import com.atakmap.android.missionpackage.ui.MissionPackageListGroup;
import com.atakmap.android.toolbars.RangeCircle;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.android.user.icon.SpotMapPallet;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.vehicle.VehicleShape;
import com.atakmap.android.vehicle.model.VehicleModel;
import com.atakmap.android.vehicle.model.VehicleModelCache;
import com.atakmap.android.vehicle.model.VehicleModelInfo;
import com.atakmap.android.vehicle.model.cot.VehicleModelImporter;
import com.atakmap.app.R;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.importfiles.ImportMissionPackageResolver;
import gov.tak.api.importfiles.ImportResolver;
import gov.tak.platform.marshal.MarshalManager;

/**
 * Utility methods mainly used by Mission Packages
 */
public class MissionPackageUtils {

    private static final String TAG = "MissionPackageUtils";
    private static final int MAX_MISSIONPACKAGE_NAMELENGTH = 30;
    private static final Map<String, String> _iconURIs = new HashMap<>();
    private static FiltersConfig _iconFilters;

    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT_SHORT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy", LocaleUtil.getCurrent());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT_FULL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy HH:mm:ss", LocaleUtil.getCurrent());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    public static String getDefaultName(Context context,
            String deviceCallsign) {
        if (FileSystemUtils.isEmpty(deviceCallsign))
            return context == null ? "MP"
                    : context
                            .getString(R.string.mission_package_name);

        return context.getString(R.string.mission_package_prefix) + "-"
                + deviceCallsign;
    }

    /**
     * Get a unique filename based on whether a given filepath is taken
     * @param path File path
     * @param name package name, not including the .zip extension
     * @return filename including extension, but not path e.g. packagename.zip
     */
    public static String getUniqueFilename(String path, String name) {
        name = FileSystemUtils.sanitizeFilename(name);
        if (!FileSystemUtils.isFile(new File(path, name + ".zip")))
            return name + ".zip";

        // iterate and find a suitable untaken filename...
        // assuming user will not wrap/receive 200 packages with same name...
        for (int index = 1; index <= 200; index++) {
            String temp = String.format(LocaleUtil.getCurrent(), "%s-%d.zip",
                    name, index);
            if (!FileSystemUtils.isFile(new File(path, temp)))
                return temp;
        }

        // oh well, overwrite an existing filename
        return name;
    }

    public static String getUniqueName(Context context, String name) {

        if (FileSystemUtils.isEmpty(name))
            name = getDefaultName(context, null);

        // set max name length
        if (name.length() > MAX_MISSIONPACKAGE_NAMELENGTH)
            name = name.substring(0, MAX_MISSIONPACKAGE_NAMELENGTH);

        name = FileSystemUtils.sanitizeFilename(name);

        AndroidFileInfo file = FileInfoPersistanceHelper.instance()
                .getFileInfoFromUserLabel(name,
                        FileInfoPersistanceHelper.TABLETYPE.SAVED);
        if (file == null) {
            // name is available
            return name;
        }

        // iterate and find a suitable untaken name...
        // assuming user will not create 200 packages with same name...
        for (int index = 1; index <= 200; index++) {
            String temp = String.format(LocaleUtil.getCurrent(), "%s-%d", name,
                    index);
            file = FileInfoPersistanceHelper.instance()
                    .getFileInfoFromUserLabel(temp,
                            FileInfoPersistanceHelper.TABLETYPE.SAVED);
            if (file == null)
                return temp;
        }

        return getDefaultName(context, null);
    }

    /**
     * Retrieve icon URI from CoT event
     * @param event CoT event to scan
     * @return Icon URI string
     */
    public static String getIconURI(CotEvent event) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return null;

        // Load icon filters
        if (_iconFilters == null) {
            AssetManager assetMgr = mv.getContext().getAssets();
            try {
                _iconFilters = FiltersConfig.parseFromStream(assetMgr
                        .open("filters/icon_filters.xml"));
            } catch (Exception ignore) {
            }
        }

        // Set default type icons
        if (_iconURIs.isEmpty()) {
            _iconURIs.put("b-t-f", ATAKUtilities.getResourceUri(
                    R.drawable.nav_chatnext));
            _iconURIs.put("b-i-v", ATAKUtilities.getResourceUri(
                    R.drawable.ic_video_alias));
            _iconURIs.put("u-d-r", "asset://icons/rectangle.png");
            _iconURIs.put("u-d-f", ATAKUtilities.getResourceUri(
                    R.drawable.shape));
            _iconURIs.put("u-d-f-m", ATAKUtilities.getResourceUri(
                    R.drawable.multipolyline));
            _iconURIs.put(DrawingCircle.COT_TYPE, ATAKUtilities.getResourceUri(
                    R.drawable.ic_circle));
            _iconURIs.put(DrawingEllipse.COT_TYPE, ATAKUtilities.getResourceUri(
                    R.drawable.ic_circle));
            _iconURIs.put(RangeCircle.COT_TYPE, ATAKUtilities.getResourceUri(
                    R.drawable.ic_circle));
            _iconURIs.put(VehicleShape.COT_TYPE, ATAKUtilities.getResourceUri(
                    R.drawable.pointtype_aircraft));
            _iconURIs.put(VehicleModel.COT_TYPE, ATAKUtilities.getResourceUri(
                    R.drawable.pointtype_aircraft));
            _iconURIs.put("u-rb-a", ATAKUtilities.getResourceUri(
                    R.drawable.pairing_line_white));
            _iconURIs.put("u-r-b-bullseye", ATAKUtilities.getResourceUri(
                    R.drawable.bullseye));
            _iconURIs.put("b-m-r", ATAKUtilities.getResourceUri(
                    R.drawable.ic_route));
            _iconURIs.put("b-a-o-can", "asset://icons/alarm-trouble.png");
            _iconURIs.put("a-f-G", "asset://icons/friendly.png");
            _iconURIs.put("a-h-G", "asset://icons/target.png");
            _iconURIs.put("a-n-G", "asset://icons/neutral.png");
            _iconURIs.put("a-u-G", "asset://icons/unknown.png");
            _iconURIs.put("b-r-f-h-c", "asset://icons/damaged.png");
            _iconURIs.put("b-m-p-c", "asset://icons/reference_point.png");
        }

        String type = event.getType();
        String key = type;

        String iconPath = null;
        CotDetail userIcon = event.findDetail("usericon");
        if (userIcon != null)
            iconPath = userIcon.getAttribute("iconsetpath");
        if (!FileSystemUtils.isEmpty(iconPath))
            key += "\\" + iconPath;
        if (_iconURIs.containsKey(key))
            return _iconURIs.get(key);

        // Find icon using path
        String iconUri = null;
        if (!FileSystemUtils.isEmpty(iconPath)) {
            // Find user-provided icon
            if (iconPath.startsWith(Icon2525cPallet.COT_MAPPING_2525)) {
                // Find 2525c icon

                String type2525 = CotUtils.mil2525cFromCotType(type);
                if (!FileSystemUtils.isEmpty(type2525))
                    iconUri = AssetMapDataRef.toUri(
                            Icon2525cPallet.ASSET_PATH + type2525 + ".png");
            } else if (iconPath.startsWith(SpotMapPallet.COT_MAPPING_SPOTMAP)) {
                // Find spot map icon (generic point or label)
                iconUri = AssetMapDataRef.toUri("icons/reference_point.png");
                if (iconPath.endsWith("LABEL"))
                    iconUri = ATAKUtilities.getResourceUri(
                            R.drawable.enter_location_label_icon);
            } else if (UserIcon.IsValidIconsetPath(iconPath, false,
                    mv.getContext())) {
                // Database icon
                String optimizedQuery = UserIcon
                        .GetIconBitmapQueryFromIconsetPath(iconPath,
                                mv.getContext());
                if (!FileSystemUtils.isEmpty(optimizedQuery)) {
                    MapDataRef iconRef = new SqliteMapDataRef(
                            UserIconDatabase.instance(mv.getContext())
                                    .getDatabaseName(),
                            optimizedQuery);
                    iconUri = iconRef.toUri();
                }
            }
        }

        // Find icon using type
        if (FileSystemUtils.isEmpty(iconUri)) {
            // Lookup default type icon
            if (_iconFilters != null && FileSystemUtils.isEmpty(iconUri)) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", type);
                FiltersConfig.Filter f = _iconFilters.lookupFilter(
                        metadata);
                String v = f != null ? f.getValue() : null;
                if (!FileSystemUtils.isEmpty(v))
                    iconUri = AssetMapDataRef.toUri(v);
            }
        }

        //Log.d(TAG, key + " -> " + iconUri);
        _iconURIs.put(key, iconUri);

        return iconUri;
    }

    public static Drawable getIconDrawable(CotEvent event) {
        MapView mv = MapView.getMapView();
        if (mv == null || event == null)
            return null;

        String type = event.getType();

        // Vehicle model icons are only available as drawables, which are much
        // more flexible than bitmap URIs
        if (type.equals(VehicleModel.COT_TYPE)
                || type.equals("overhead_marker")) {
            CotDetail model = event.findDetail("model");
            if (model != null) {
                String name = model.getAttribute("name");
                String category = model.getAttribute("category");
                VehicleModelInfo info;
                if (FileSystemUtils.isEmpty(category))
                    info = VehicleModelImporter.findModel(name);
                else
                    info = VehicleModelCache.getInstance().get(category, name);
                if (info != null)
                    return info.getIcon();
            }
        }

        // Legacy icon URI fallback
        String uri = getIconURI(event);
        if (uri != null) {
            Bitmap bmp = ATAKUtilities.getUriBitmap(uri);
            if (bmp != null)
                return new BitmapDrawable(mv.getResources(), bmp);
        }
        return null;
    }

    /**
     * Get the map item color for this CoT event
     * @param event CoT event to scan
     * @return Color (default: white)
     */
    public static int getColor(CotEvent event) {
        if (event != null) {
            Integer colorInt = parseColor(event, "color", "argb");
            if (colorInt == null)
                colorInt = parseColor(event, "color", "value");
            if (colorInt == null)
                colorInt = parseColor(event, "strokeColor", "value");
            if (colorInt == null)
                colorInt = parseColor(event, "link_attr", "color");
            if (colorInt != null)
                return colorInt;
        }
        return Color.WHITE;
    }

    private static Integer parseColor(CotEvent event, String key,
            String value) {
        CotDetail color = event.findDetail(key);
        if (color != null) {
            try {
                return Integer.parseInt(color.getAttribute(value));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private static final Map<AndroidFileInfo, MissionPackageListGroup> missionPackageListGroupCache = new ConcurrentHashMap<>();

    public static List<MissionPackageListGroup> getUiPackages(MapGroup group) {
        List<MissionPackageListGroup> manifests = new ArrayList<>();

        final FileInfoPersistanceHelper fiph = FileInfoPersistanceHelper
                .instance();
        if (fiph == null)
            return manifests;

        List<AndroidFileInfo> files = fiph
                .allFiles(FileInfoPersistanceHelper.TABLETYPE.SAVED);

        if (files == null || files.isEmpty())
            return manifests;

        for (AndroidFileInfo fi : files) {
            // get local manifest from DB
            File f = fi.file();

            if (!IOProviderFactory.exists(f)) {
                missionPackageListGroupCache.remove(fi);
                continue;
            }

            synchronized (missionPackageListGroupCache) {
                MissionPackageListGroup cached = missionPackageListGroupCache
                        .get(fi);

                // if it was cached but not equal (ie the hash had changed, remove the old cache or
                // the file no longer exists.
                for (AndroidFileInfo cachedfi : missionPackageListGroupCache
                        .keySet()) {
                    if ((!cachedfi.equals(fi) && cachedfi.file().equals(f))
                            || !cachedfi.file().exists()) {
                        missionPackageListGroupCache.remove(cachedfi);
                    }
                }
                if (cached == null) {
                    final MissionPackageManifest c = MissionPackageManifest
                            .fromXml(
                                    fi.fileMetadata(), f.getAbsolutePath(),
                                    true);
                    if (c == null || !c.isValid()) {
                        Log.w(TAG, "Failed to load Manifest: " + fi);
                        continue;
                    }

                    cached = MissionPackageManifestAdapter
                            .adapt(c, fi.userName(), group);
                    if (cached == null || !cached.isValid()) {
                        Log.w(TAG, "Failed to load Manifest wrapper: " + fi);
                        continue;
                    }
                    missionPackageListGroupCache.put(fi, cached);
                }
                manifests.add(cached);
            }
        }
        return manifests;
    }

    public static int getPackageCount() {
        FileInfoPersistanceHelper fiph = FileInfoPersistanceHelper.instance();
        if (fiph == null)
            return 0;

        List<AndroidFileInfo> files = fiph.allFiles(
                FileInfoPersistanceHelper.TABLETYPE.SAVED);
        if (FileSystemUtils.isEmpty(files))
            return 0;

        return files.size();
    }

    public static String abbreviateFilename(String filename, int limit) {
        if (FileSystemUtils.isEmpty(filename))
            return "";

        if (filename.length() <= limit)
            return filename;

        if (limit < 0)
            return filename;

        int fileNameLength = filename.length();
        return filename
                .substring(0, ((limit / 2) - 3))
                .concat("...")
                .concat(filename.substring(fileNameLength - (limit / 2),
                        fileNameLength));
    }

    /**
     * Format modified date for UI
     * @param file File object
     * @return Date string
     */
    public static String getModifiedDate(File file) {
        return getModifiedDate(IOProviderFactory.lastModified(file));
    }

    public static String getModifiedDate(Long time, boolean full) {
        if (full)
            return TIME_FORMAT_FULL.get().format(time);
        else
            return TIME_FORMAT_SHORT.get().format(time);
    }

    public static String getModifiedDate(Long time) {
        return getModifiedDate(time, true);
    }

    public static void importMissionPackage(final Context context) {
        ImportFileBrowserDialog
                .show(context.getString(R.string.select_space)
                                + context.getString(
                                R.string.mission_package_name),
                        new String[] {
                                "zip", "dpk"
                        },
                        new ImportFileBrowserDialog.DialogDismissed() {
                            @Override
                            public void onFileSelected(
                                    File file) {
                                ImportMissionPackageResolver importer;
                                if (file.toString()
                                        .toLowerCase(LocaleUtil.getCurrent())
                                        .endsWith("zip"))
                                    importer = createResolver(context, ".zip");
                                else
                                    importer = createResolver(context, ".dpk");
                                if (!importer
                                        .match(file)) {
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.mission_package_name)
                                                            +
                                                            context.getString(
                                                                    R.string.preferences_text435),
                                                    Toast.LENGTH_LONG)
                                            .show();
                                    return;
                                }

                                boolean success = importer
                                        .beginImport(file, EnumSet.noneOf(ImportResolver.SortFlags.class));
                                if (success) {
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.mission_package_name)
                                                            +
                                                            context.getString(
                                                                    R.string.preferences_text436),
                                                    Toast.LENGTH_LONG)
                                            .show();

                                } else {
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.mission_package_name)
                                                            +
                                                            R.string.preferences_text435,
                                                    Toast.LENGTH_LONG)
                                            .show();

                                }

                                ImportExportMapComponent.getInstance().cleanupResolvers(List.of(importer));
                            }

                            @Override
                            public void onDialogClosed() {
                            }
                        }, context);
    }

    public static ImportMissionPackageResolver createResolver(final Context context, String ext) {
        Drawable drawable = MapView.getMapView().getContext().getDrawable(R.drawable.ic_menu_missionpackage);
        gov.tak.api.commons.graphics.Drawable engineDrawable = MarshalManager.marshal(drawable, Drawable.class, gov.tak.api.commons.graphics.Drawable.class);
        String folderName = FileSystemUtils.TOOL_DATA_DIRECTORY + File.separatorChar + context.getString(R.string.mission_package_folder);
        ImportMissionPackageResolver resolver = new ImportMissionPackageResolver(ext, FileSystemUtils.getItem(folderName), context.getString(R.string.mission_package_name), engineDrawable, true);
        ImportExportMapComponent.getInstance().initImportResolver(resolver);
        return resolver;
    }
}
