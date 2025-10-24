
package com.atakmap.android.importfiles.task;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importfiles.sort.ImportAPKResolver;
import com.atakmap.android.importfiles.sort.ImportAPKSort;
import com.atakmap.android.importfiles.sort.ImportAlternateContactSort;
import com.atakmap.android.importfiles.sort.ImportCertResolver;
import com.atakmap.android.importfiles.sort.ImportCertSort;
import com.atakmap.android.importfiles.sort.ImportCotSort;
import com.atakmap.android.importfiles.sort.ImportDRWSort;
import com.atakmap.android.importfiles.sort.ImportDTEDSort;
import com.atakmap.android.importfiles.sort.ImportDTEDZSort;
import com.atakmap.android.importfiles.sort.ImportGMLSort;
import com.atakmap.android.importfiles.sort.ImportGMLZSort;
import com.atakmap.android.importfiles.sort.ImportGPXRouteSort;
import com.atakmap.android.importfiles.sort.ImportGPXSort;
import com.atakmap.android.importfiles.sort.ImportGRGResolver;
import com.atakmap.android.importfiles.sort.ImportGRGSort;
import com.atakmap.android.importfiles.sort.ImportGeoJsonSort;
import com.atakmap.android.importfiles.sort.ImportGeoJsonZSort;
import com.atakmap.android.importfiles.sort.ImportINFZResolver;
import com.atakmap.android.importfiles.sort.ImportINFZSort;
import com.atakmap.android.importfiles.sort.ImportJPEGResolver;
import com.atakmap.android.importfiles.sort.ImportJPEGSort;
import com.atakmap.android.importfiles.sort.ImportJSONPrefResolver;
import com.atakmap.android.importfiles.sort.ImportJSONPrefSort;
import com.atakmap.android.importfiles.sort.ImportKMLSort;
import com.atakmap.android.importfiles.sort.ImportKMZPackageResolver;
import com.atakmap.android.importfiles.sort.ImportKMZPackageSort;
import com.atakmap.android.importfiles.sort.ImportKMZResolver;
import com.atakmap.android.importfiles.sort.ImportKMZSort;
import com.atakmap.android.importfiles.sort.ImportLPTSort;
import com.atakmap.android.importfiles.sort.ImportLayersSort;
import com.atakmap.android.importfiles.sort.ImportMVTSort;
import com.atakmap.android.importfiles.sort.ImportMissionPackageSort;
import com.atakmap.android.importfiles.sort.ImportPrefResolver;
import com.atakmap.android.importfiles.sort.ImportPrefSort;
import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.importfiles.sort.ImportSHPSort;
import com.atakmap.android.importfiles.sort.ImportSHPZSort;
import com.atakmap.android.importfiles.sort.ImportSQLiteResolver;
import com.atakmap.android.importfiles.sort.ImportSQLiteSort;
import com.atakmap.android.importfiles.sort.ImportSupportInfoResolver;
import com.atakmap.android.importfiles.sort.ImportSupportInfoSort;
import com.atakmap.android.importfiles.sort.ImportTXTSort;
import com.atakmap.android.importfiles.sort.ImportTilesetResolver;
import com.atakmap.android.importfiles.sort.ImportTilesetSort;
import com.atakmap.android.importfiles.sort.ImportUserIconSetSort;
import com.atakmap.android.importfiles.sort.ImportVideoResolver;
import com.atakmap.android.importfiles.sort.ImportVideoSort;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.raster.ImageryFileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportAlternateContactResolver;
import gov.tak.api.importfiles.ImportCotResolver;
import gov.tak.api.importfiles.ImportDRWResolver;
import gov.tak.api.importfiles.ImportDTEDResolver;
import gov.tak.api.importfiles.ImportDTEDZResolver;
import gov.tak.api.importfiles.ImportGMLResolver;
import gov.tak.api.importfiles.ImportGMLZResolver;
import gov.tak.api.importfiles.ImportGPXResolver;
import gov.tak.api.importfiles.ImportGPXRouteResolver;
import gov.tak.api.importfiles.ImportGeoJsonResolver;
import gov.tak.api.importfiles.ImportGeoJsonZResolver;
import gov.tak.api.importfiles.ImportKMLResolver;
import gov.tak.api.importfiles.ImportLPTResolver;
import gov.tak.api.importfiles.ImportLayersResolver;
import gov.tak.api.importfiles.ImportMVTResolver;
import gov.tak.api.importfiles.ImportMissionPackageResolver;
import gov.tak.api.importfiles.ImportSHPResolver;
import gov.tak.api.importfiles.ImportSHPZResolver;
import gov.tak.api.importfiles.ImportTXTResolver;
import gov.tak.api.importfiles.ImportUserIconSetResolver;
import gov.tak.platform.marshal.MarshalManager;

/**
 * Background task to parse a directory and move files to other directories, which are watched by
 * other components which handle import into ATAK
 */
public class ImportFilesTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "ImportFilesTask";

    private static final Set<String> extensions = new HashSet<>();
    // array form of the above set
    private static String[] extensionList;

    static {
        // setup list of supported file extensions
        extensions.add("dpk");
        extensions.add("zip");
        extensions.add("kml");
        extensions.add("kmz");
        extensions.add("lpt");
        extensions.add("drw");
        extensions.add("xml");
        extensions.add("txt");
        extensions.add("pref");
        extensions.add("cot");
        extensions.add("sqlite");
        extensions.add("shp");
        extensions.add("gml");
        extensions.add("gpx");
        extensions.add("geojson");
        extensions.add("mvt");
        extensions.add("jpg");
        extensions.add("jpeg");
        extensions.add("png");
        extensions.add("csv");
        extensions.add("inf");
        extensions.add("infz");
        extensions.add("apk");
        extensions.addAll(ImportVideoSort.VIDEO_EXTENSIONS);

        // now pull in all supported native imagery extensions
        String[] exts;
        for (ImageryFileType.AbstractFileType fileType : ImageryFileType
                .getFileTypes()) {
            exts = fileType.getSuffixes();
            if (exts == null || exts.length < 1)
                continue;

            for (String ext : exts) {
                if (FileSystemUtils.isEmpty(ext))
                    continue;

                // found a new supported extension
                extensions.add(ext);
            }
        }

        extensionList = extensions.toArray(new String[0]);
        Log.d(TAG,
                "internal registered file extension: "
                        + Arrays.toString(extensionList));
    }

    /**
     * Register an extension should be called only by ImportExportMapComponent::addImporterClass
     */
    public static synchronized void registerExtension(String extension) {
        if (extension != null && !extension.isEmpty()) {
            if (extension.startsWith("."))
                extension = extension.substring(1);
            Log.d(TAG, "registering external extension: " + extension);
            extensions.add(extension);
            extensionList = extensions.toArray(new String[0]);
        }
    }

    /**
     * Unregister an extension should be called only by ImportExportMapComponent::addImporterClass
     */
    public static synchronized void unregisterExtension(
            String extension) {
        if (extension != null && !extension.isEmpty()) {
            if (extension.startsWith("."))
                extension = extension.substring(1);
            Log.d(TAG, "unregistering external extension: " + extension);
            extensions.remove(extension);
            extensionList = extensions.toArray(new String[0]);
        }
    }

    public static synchronized String[] getSupportedExtensions() {
        return extensionList;
    }

    /**
     * Destination path of already sorted files, do not overwrite during a single import as ATAK may
     * still be processing it...
     */
    private final Set<String> _sortedFiles;

    private final Context _context;

    public ImportFilesTask(Context context) {
        this._context = context;
        this._sortedFiles = new HashSet<>();
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Thread.currentThread().setName("ImportFilesTask");

        String[] atakRoots = FileSystemUtils.findMountPoints();
        if (atakRoots == null || atakRoots.length < 1) {
            Log.w(TAG, "Found no ATAK mount points");
            return 0;
        }

        // get sorters, require proper file extensions, move files into atak data dir
        List<gov.tak.api.importfiles.ImportResolver> sorters = GetKernelSorters(_context, false);
        if (sorters.isEmpty()) {
            Log.w(TAG, "Found no ATAK import sorters");
            return 0;
        }

        int numberSorted = 0;
        for (String dir : atakRoots) {
            if (dir == null || dir.isEmpty())
                continue;

            File importDir = new File(_context.getCacheDir(),
                    FileSystemUtils.ATAKDATA);
            numberSorted += sort(importDir, sorters);
        }

        Log.d(TAG, "Importing from atakroots numberSorted: " + numberSorted);
        return numberSorted;
    }

    /**
     * Get list of sorters for supported file types, with the specified configuration settings
     * 
     * @param context
     * @param validateExt
     * @param copyFile
     * @param importInPlace - see ImportInPlaceResolver.ctor
     * @param bKMZStrict -see ImportKMZSort._bStrict
     * @return
     *
     * @deprecated will return a {@link List} of {@link gov.tak.api.importfiles.ImportResolver}s
     */
    @Deprecated
    @DeprecatedApi(since = "5.5")
    public static List<ImportResolver> GetSorters(Context context,
            boolean validateExt,
            boolean copyFile, boolean importInPlace, boolean bKMZStrict) {
        List<ImportResolver> sorters = new ArrayList<>();

        sorters.add(new ImportSupportInfoSort(copyFile));

        // check mission package manifest prior to KMZ, since a Mission Package
        // may contain a KML file and in that case import could be improperly
        // classified as a KMZ file (if validateExt is false e.g. Import
        // Manager RemoteResource)
        sorters.add(new ImportMissionPackageSort.ImportMissionV1PackageSort(
                context, validateExt,
                copyFile, true));
        sorters.add(new ImportMissionPackageSort.ImportMissionV2PackageSort(
                context, validateExt,
                copyFile, true));

        sorters.add(new ImportUserIconSetSort(context, validateExt));

        sorters.add(new ImportKMZPackageSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportGRGSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportKMLSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportKMZSort(context, validateExt, copyFile,
                importInPlace, bKMZStrict));
        sorters.add(new ImportTXTSort(context, ".xml", validateExt, copyFile));
        sorters.add(new ImportTXTSort(context, ".txt", validateExt, copyFile));
        sorters.add(new ImportAlternateContactSort(context, validateExt,
                copyFile));
        sorters.add(new ImportSQLiteSort(context, validateExt, copyFile));
        sorters.add(new ImportPrefSort(context, validateExt, copyFile));
        sorters.add(new ImportJSONPrefSort(context, validateExt, copyFile));
        sorters.add(new ImportCertSort(context, validateExt, copyFile));
        sorters.add(new ImportDRWSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportLPTSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportGPXSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportGPXRouteSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportJPEGSort(context, ".jpg", validateExt, copyFile));
        sorters.add(
                new ImportJPEGSort(context, ".jpeg", validateExt, copyFile));
        sorters.add(new ImportCotSort(context, validateExt, copyFile));
        sorters.add(new ImportTilesetSort(context, validateExt, copyFile));

        sorters.add(new ImportSHPSort(context, validateExt, copyFile,
                importInPlace));

        sorters.add(new ImportSHPZSort(context, validateExt, copyFile,
                importInPlace));

        sorters.add(new ImportGMLSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportGMLZSort(context, validateExt, copyFile,
                importInPlace));

        sorters.add(new ImportGeoJsonSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(new ImportGeoJsonZSort(context, validateExt, copyFile,
                importInPlace));

        sorters.add(new ImportMVTSort(context, validateExt, copyFile,
                importInPlace));
        sorters.add(
                new ImportMVTSort(context, ".mbtiles", validateExt, copyFile,
                        importInPlace));

        sorters.add(new ImportDTEDZSort.ImportDTEDZv1Sort(context, validateExt,
                copyFile,
                importInPlace));
        sorters.add(new ImportDTEDZSort.ImportDTEDZv2Sort(context, validateExt,
                copyFile,
                importInPlace));
        sorters.add(new ImportDTEDSort(context));

        sorters.add(new ImportINFZSort(context, validateExt));
        sorters.add(new ImportAPKSort(context, validateExt));

        // TODO: Since the video sorters currently do not have a way of
        //  validating a file w/out the extension, always check the extension
        //  for now - otherwise any file that makes it this far will be
        //  considered a video if validateExt is false
        // See ATAK-10892 - Files were previously falling through and being
        // accepted by the MPEG sorter if all other matchers failed
        sorters.add(new ImportVideoSort(context, true, copyFile));

        // now add dynamically registered importers
        // TODO we could further refactor all resolvers to be dynamically registered, and add
        // priority for importers, to determine order in which they are evaluated, if necessary
        if (ImportExportMapComponent.getInstance() != null) {
            Collection<ImportResolver> importerResolvers = ImportExportMapComponent
                    .getInstance().getImporterResolvers();
            if (importerResolvers != null && !importerResolvers.isEmpty()) {
                for (ImportResolver resolver : importerResolvers) {
                    try {
                        if (resolver != null) {
                            Log.d(TAG, "Adding Import Resolver of type: "
                                    + resolver.getClass().getName());
                            //update current options on this resolver
                            resolver.setOptions(validateExt, copyFile);
                            sorters.add(resolver);
                        } else {
                            Log.w(TAG, "Failed to add Importer");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to add importer", e);
                    }
                }
            }
        }

        // The Layer importer tends to match a lot of things and can get in the way of things
        // like the GRGSort. Add it near the end so things like the MissionPackage extractor
        // can pick an appropriate importer without prompting the user.
        sorters.add(new ImportLayersSort(context));

        // finally if its a zip (but has no manifest) lets create a Mission Package out of it and
        // import it contents.   In this case force a copy of the contents.
        sorters.add(new ImportMissionPackageSort.ImportMissionV1PackageSort(
                context, validateExt,
                true, false));

        return sorters;
    }

    /**
     * Get list of sorters for supported file types, with the specified configuration settings
     * </br></br>
     * Clean ({@link ImportExportMapComponent#cleanupResolvers(List)}
     * these up when done with them.
     *
     * @param context
     * @return
     *
     * @deprecated signature will change to {@code List<gov.tak.api.importfiles.ImporterResolver> GetSorters((Context context,
     *                                                   boolean validateExt,
     *                                                   boolean copyFile, boolean importInPlace, boolean bKMZStrict)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static List<gov.tak.api.importfiles.ImportResolver> GetKernelSorters(Context context, boolean bKMZStrict) {
        List<gov.tak.api.importfiles.ImportResolver> sorters = new ArrayList<>();
        sorters.add(new ImportKMLResolver(context.getString(R.string.kml_file),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_kml)));
        sorters.add(new ImportAlternateContactResolver(context.getString(R.string.contact_info),
                FileSystemUtils.getRoot(), getDrawable(context, R.drawable.ic_menu_import_file)));
        sorters.add(new ImportCotResolver(context.getString(R.string.cot_event),
                FileSystemUtils.getRoot(), getDrawable(context, R.drawable.unknown)));
        sorters.add(new ImportDRWResolver(context.getString(R.string.drw_file),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_falconview_drw)));
        sorters.add(new ImportDTEDResolver(context.getString(R.string.dted_cell),
                FileSystemUtils.getItem(FileSystemUtils.DTED_DIRECTORY), getDrawable(context, R.drawable.ic_overlay_dted)));
        sorters.add(new ImportDTEDZResolver.ImportDTEDZv1Resolver(context.getString(R.string.zipped_dted),
                FileSystemUtils.getItem(FileSystemUtils.DTED_DIRECTORY), getDrawable(context, R.drawable.ic_overlay_dted)));
        sorters.add(new ImportDTEDZResolver.ImportDTEDZv2Resolver(context.getString(R.string.zipped_dted),
                FileSystemUtils.getItem(FileSystemUtils.DTED_DIRECTORY), getDrawable(context, R.drawable.ic_overlay_dted)));
        sorters.add(new ImportGeoJsonResolver("GeoJSON",
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        sorters.add(new ImportGeoJsonZResolver("Zipped GeoJSON",
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        sorters.add(new ImportGMLResolver("GML",
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        sorters.add(new ImportGMLZResolver("Zipped GML",
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        sorters.add(new ImportGPXResolver(context.getString(R.string.gpx_file),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_gpx)));
        sorters.add(new ImportGPXRouteResolver(context.getString(R.string.gpx_file),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_gpx)));
        sorters.add(new ImportLPTResolver(context.getString(R.string.gpx_file),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_falconview_lpt)));
        sorters.add(new ImportMVTResolver(context.getString(R.string.mvt_file), FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY),
                getDrawable(context, R.drawable.ic_mvt)));
        sorters.add(new ImportMVTResolver(".mbtiles", FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), context.getString(R.string.mvt_file),
                getDrawable(context, R.drawable.ic_mvt)));
        sorters.add(new ImportPrefResolver(context));
        sorters.add(new ImportSHPResolver(context.getString(R.string.shapefile),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        sorters.add(new ImportSHPZResolver(context.getString(R.string.zipped_shapefile),
                FileSystemUtils.getItem(FileSystemUtils.OVERLAYS_DIRECTORY), getDrawable(context, R.drawable.ic_shapefile)));
        File mpDestinationDir = FileSystemUtils.getItem(FileSystemUtils.TOOL_DATA_DIRECTORY + File.separatorChar
                + context.getString(R.string.mission_package_folder));
        sorters.add(new ImportMissionPackageResolver.ImportMissionV1PackageResolver(mpDestinationDir, context.getString(R.string.mission_package_name),
                getDrawable(context, R.drawable.ic_shapefile), true));
        sorters.add(new ImportMissionPackageResolver.ImportMissionV2PackageResolver(mpDestinationDir, context.getString(R.string.mission_package_name),
                getDrawable(context, R.drawable.ic_shapefile), true));
        sorters.add(new ImportUserIconSetResolver(context.getString(R.string.user_icon_set), FileSystemUtils.getRoot(),
                getDrawable(context, R.drawable.cot_icon_sugp)));
        sorters.add(new ImportTXTResolver(".txt", FileSystemUtils.getRoot(), ImportTXTResolver.CONTENT_TYPE,
                getDrawable(context, R.drawable.ic_details)));
        sorters.add(new ImportTXTResolver(".xml", FileSystemUtils.getRoot(), ImportTXTResolver.CONTENT_TYPE,
                getDrawable(context, R.drawable.ic_details)));

        // now add dynamically registered importers
        // TODO we could further refactor all resolvers to be dynamically registered, and add
        // priority for importers, to determine order in which they are evaluated, if necessary
        if (ImportExportMapComponent.getInstance() != null) {
            Collection<gov.tak.api.importfiles.ImportResolver> importerResolvers = ImportExportMapComponent
                    .getInstance().getKernelImporterResolvers();
            if (importerResolvers != null && !importerResolvers.isEmpty()) {
                for (gov.tak.api.importfiles.ImportResolver resolver : importerResolvers) {
                    try {
                        if (resolver != null) {
                            Log.d(TAG, "Adding Import Resolver of type: "
                                    + resolver.getClass().getName());
                            sorters.add(resolver);
                        } else {
                            Log.w(TAG, "Failed to add Importer");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to add importer", e);
                    }
                }
            }
        }

        // The Layer importer tends to match a lot of things and can get in the way of things
        // like the GRGSort. Add it near the end so things like the MissionPackage extractor
        // can pick an appropriate importer without prompting the user.
        sorters.add(new ImportLayersResolver(context.getString(R.string.imagery),
                FileSystemUtils.getItem("imagery"), getDrawable(context, R.drawable.ic_menu_maps)));

        // Yet-to-be ported, but now subclassing kernel ImportResolver
        sorters.add(new ImportSupportInfoResolver());
        sorters.add(new ImportVideoResolver(context));
        sorters.add(new ImportAPKResolver(context));
        sorters.add(new ImportTilesetResolver(context));
        sorters.add(new ImportKMZPackageResolver(context));
        sorters.add(new ImportGRGResolver(context));
        sorters.add(new ImportKMZResolver(context, bKMZStrict));
        sorters.add(new ImportSQLiteResolver(context));
        sorters.add(new ImportJSONPrefResolver(context));
        sorters.add(new ImportCertResolver(context));
        sorters.add(new ImportJPEGResolver(context, ".jpg"));
        sorters.add(new ImportJPEGResolver(context, ".jpeg"));
        sorters.add(new ImportINFZResolver(context));

        // finally if its a zip (but has no manifest) lets create a Mission Package out of it and
        // import it contents.   In this case force a copy of the contents.
        sorters.add(new ImportMissionPackageResolver.ImportMissionV1PackageResolver(mpDestinationDir, context.getString(R.string.mission_package_name),
                getDrawable(context, R.drawable.ic_shapefile), false));

        for (gov.tak.api.importfiles.ImportResolver resolver : sorters) {
            ImportExportMapComponent.getInstance().initImportResolver(resolver);
        }
        return sorters;
    }

    private static gov.tak.api.commons.graphics.Drawable getDrawable(Context context, int resId) {
        return MarshalManager.marshal(context.getDrawable(resId), Drawable.class, gov.tak.api.commons.graphics.Drawable.class);
    }

    private int sort(File dir, List<gov.tak.api.importfiles.ImportResolver> sorters) {
        if (dir == null) {
            Log.d(TAG, "Import directory null.");
            return 0;
        } else if (!IOProviderFactory.exists(dir)) {
            Log.d(TAG, "Import dir not found: " + dir.getAbsolutePath());
            return 0;
        } else if (!IOProviderFactory.isDirectory(dir)) {
            Log.d(TAG, "Import path not a directory: " + dir.getAbsolutePath());
            return 0;
        }

        Log.d(TAG, "Importing from directory: " + dir.getAbsolutePath());
        int numberSorted = 0;
        File[] files = IOProviderFactory.listFiles(dir);
        if (files != null) {
            for (File file : files) {
                if (file == null || !IOProviderFactory.exists(file))
                    continue;

                // if subdir, recurse
                if (IOProviderFactory.isDirectory(file)) {
                    numberSorted += sort(file, sorters);
                    continue;
                }

                // otherwise attempt to sort the file
                boolean sorted = false;
                for (gov.tak.api.importfiles.ImportResolver sorter : sorters) {
                    // see if this sorter can handle the current file
                    if (sorter.match(file)) {
                        // do not overwrite is we've already imported a file to the anticipated
                        // location
                        File destPath = sorter.getDestinationPath(file);
                        if (destPath == null) {
                            Log.w(TAG,
                                    sorter
                                            + ", Unable to determine destination path for: "
                                            + file.getAbsolutePath());
                            continue;
                        }

                        if (_sortedFiles.contains(destPath.getAbsolutePath())) {
                            Log.w(TAG,
                                    sorter
                                            + ", Matched, but destination path already exists: "
                                            + destPath.getAbsolutePath());
                            break;
                        }

                        // now attempt to sort (i.e. move the file to proper location)
                        sorted = sorter.beginImport(file, EnumSet.noneOf(gov.tak.api.importfiles.ImportResolver.SortFlags.class));
                        if (sorted) {
                            numberSorted++;
                            _sortedFiles.add(destPath.getAbsolutePath());
                            Log.d(TAG,
                                    sorter + ", Sorted: "
                                            + file.getAbsolutePath()
                                            + " to "
                                            + destPath.getAbsolutePath());
                            break;
                        } else
                            Log.w(TAG,
                                    sorter
                                            + ", Matched, but did not sort: "
                                            + file.getAbsolutePath());
                    } // end if sorter match was found
                }

                if (!sorted) {
                    Log.i(TAG,
                            "Did not sort unsupported file: "
                                    + file.getAbsolutePath());
                }
            } // end file loop
        }

        // if no files left in this directory, remove it
        files = IOProviderFactory.listFiles(dir);

        if (IOProviderFactory.exists(dir) && IOProviderFactory.isDirectory(dir)
                && (files == null || files.length < 1)) {
            Log.i(TAG, "Cleaning up empty directory: " + dir.getAbsolutePath());
            FileSystemUtils.delete(dir);
        }

        for (gov.tak.api.importfiles.ImportResolver sorter : sorters) {
            if (sorter.getFileSorted()) {
                sorter.finalizeImport();
            }
        }

        return numberSorted;
    }
}
