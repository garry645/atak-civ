
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;
import android.widget.Toast;

import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.missionpackage.file.MissionPackageExtractorFactory;
import com.atakmap.app.R;
import com.atakmap.coremap.ZipUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportMissionPackageResolver;

/**
 * Sorts ATAK Mission Packages
 *
 * @deprecated use {@link ImportMissionPackageResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportMissionPackageSort extends ImportResolver {

    public static class ImportMissionV1PackageSort
            extends ImportMissionPackageSort {

        /**
         * Construct an importer that works on MissionPackage/DataPackage files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         *
         * @param context     the extension to be used
         * @param validateExt if the extension needs to be validated
         * @param copyFile    if the file needs to be copied
         * @param bStrict     strict if the mission package is required to have a manifest
         */
        public ImportMissionV1PackageSort(Context context, boolean validateExt,
                boolean copyFile, boolean bStrict) {

            super(context, ".zip", validateExt, copyFile, bStrict);

        }
    }

    public static class ImportMissionV2PackageSort
            extends ImportMissionPackageSort {

        /**
         * Construct an importer that works on MissionPackage/DataPackage files.
         *
         * @param context     the extension to be used
         * @param validateExt if the extension needs to be validated
         * @param copyFile    if the file needs to be copied
         * @param bStrict     strict if the mission package is required to have a manifest
         */
        public ImportMissionV2PackageSort(Context context, boolean validateExt,
                boolean copyFile, boolean bStrict) {

            super(context, ".dpk", validateExt, copyFile, bStrict);

        }
    }

    private static final String TAG = "ImportMissionPackageSort";
    private static final String CONTENT_TYPE = "Data Package";

    private final Context _context;
    private final boolean _bStrict;

    /**
     * Construct an importer that works on MissionPackage/DataPackage files.    Users of this class
     * should either use .zip or .dpk for the mission package extension.
     * @param context the extension to be used
     * @param ext the extension of the file either .zip or .dpk
     * @param validateExt if the extension needs to be validated
     * @param copyFile if the file needs to be copied
     * @param bStrict strict if the mission package is required to have a manifest
     */
    protected ImportMissionPackageSort(Context context, String ext,
            boolean validateExt,
            boolean copyFile, boolean bStrict) {
        super(ext,
                FileSystemUtils.TOOL_DATA_DIRECTORY + File.separatorChar
                        + context.getString(R.string.mission_package_folder),
                context.getString(R.string.mission_package_name),
                context.getDrawable(R.drawable.ic_menu_missionpackage));
        _context = context;
        _bStrict = bStrict;
    }

    @Override
    public void filterFoundResolvers(List<ImportResolver> importResolvers, File file) {
        // mission packages that are well-formed are mission packages
        if (_bStrict) {
            Log.d(TAG, "proper mission package detected: " + file.getName() + " processing as such");
            importResolvers.clear();
            importResolvers.add(this);
        }
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;

        if (_bStrict) {
            try {
                // it is a .zip, now lets see if it is a Mission Package manifest
                boolean bMatch = MissionPackageExtractorFactory
                        .HasManifest(file);
                Log.d(TAG, "(Strict) manifest "
                        + (bMatch ? "found" : "not found"));
                return bMatch;
            } catch (Exception ioe) {
                return false;
            }
        } else {
            // just ensure it is a valid zip
            if (!FileSystemUtils.isFile(file)) {
                Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                return false;
            }

            if (ZipUtils.isZip(file)) {
                List<ImportResolver> sorters = ImportFilesTask.GetSorters(
                        _context, _bValidateExt, _bCopyFile, true, _bStrict);

                for (ImportResolver resolver : sorters) {
                    // do not recheck this against any of the mission package sorters otherwise
                    // it will infinitely recurse.
                    if (resolver instanceof ImportMissionPackageSort)
                        continue;

                    if (resolver.match(file)) {
                        Log.d(TAG, "file is already another type of file ["
                                + resolver.getDisplayableName()
                                + "], cannot be just a plain old zip file: "
                                + file.getName());
                        return false;
                    }
                }
                Log.d(TAG, "(Non-strict) processing zip file as a datapackage: "
                        + getDestinationPath(file).getName());
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean beginImport(File file, Set<SortFlags> flags) {
        File dest = getDestinationPath(file);
        if (IOProviderFactory.exists(dest) && IOProviderFactory.isFile(dest)) {
            // Delete existing to be overwritten
            File f = FileSystemUtils.moveToTemp(_context, dest);
            FileSystemUtils.deleteFile(f);
        }
        // mission packages only support copy or move
        flags = new HashSet<>(flags);
        flags.add(SortFlags.IMPORT_COPY);
        return super.beginImport(file, flags);
    }

    /**
     * @deprecated see {@link com.atakmap.android.missionpackage.MissionPackageUtils#importMissionPackage(Context)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
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
                                ImportMissionPackageSort importer;
                                if (file.toString()
                                        .toLowerCase(LocaleUtil.getCurrent())
                                        .endsWith("zip"))
                                    importer = new ImportMissionPackageSort(
                                            context, ".zip",
                                            true, true,
                                            true);
                                else
                                    importer = new ImportMissionPackageSort(
                                            context, ".dpk",
                                            true, true,
                                            true);

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
                                        .beginImport(
                                                file);
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
                            }

                            @Override
                            public void onDialogClosed() {
                            }
                        }, context);
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(CONTENT_TYPE, "application/zip");
    }
}
