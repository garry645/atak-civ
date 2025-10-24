package com.atakmap.android.importfiles.callbacks;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionpackage.file.MissionPackageExtractorFactory;
import com.atakmap.coremap.ZipUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.importfiles.ImportMissionPackageResolver;
import gov.tak.api.importfiles.ImportResolver;

final class MissionPackageCallback implements ImportResolver.BeginImportListener, ImportMissionPackageResolver.MissionPackageMatchListener {
    private final String TAG = "MissionPackageCallback";

    @Override
    public boolean onBeginImport(File file, EnumSet<ImportResolver.SortFlags> sortFlags, Object opaque) {
        if (opaque instanceof ImportMissionPackageResolver) {
            ImportMissionPackageResolver resolver = (ImportMissionPackageResolver) opaque;
            File dest = resolver.getDestinationPath(file);
            if (IOProviderFactory.exists(dest) && IOProviderFactory.isFile(dest)) {
                // Delete existing to be overwritten
                File f = FileSystemUtils.moveToTemp(MapView.getMapView().getContext(), dest);
                FileSystemUtils.deleteFile(f);
            }
            sortFlags.add(ImportResolver.SortFlags.IMPORT_COPY);
        }
        return false;
    }

    @Override
    public boolean onMatch(ImportMissionPackageResolver resolver, File file) {
        if (resolver.getStrict()) {
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
                List<ImportResolver> sorters = ImportFilesTask.GetKernelSorters(
                        MapView.getMapView().getContext(), resolver.getStrict());

                for (ImportResolver sorter : sorters) {
                    // do not recheck this against any of the mission package sorters otherwise
                    // it will infinitely recurse.
                    if (sorter instanceof ImportMissionPackageResolver)
                        continue;

                    if (sorter.match(file)) {
                        Log.d(TAG, "file is already another type of file ["
                                + sorter.getDisplayableName()
                                + "], cannot be just a plain old zip file: "
                                + file.getName());
                        ImportExportMapComponent.getInstance().cleanupResolvers(sorters);
                        return false;
                    }
                }
                Log.d(TAG, "(Non-strict) processing zip file as a datapackage: "
                        + resolver.getDestinationPath(file).getName());
                ImportExportMapComponent.getInstance().cleanupResolvers(sorters);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean onMatch(File file) {
        return false;
    }
}
