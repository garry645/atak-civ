package gov.tak.api.importfiles;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;

import com.atakmap.coremap.log.Log;


import gov.tak.api.commons.graphics.Drawable;

/**
 * Sorts Mission Packages
 */
public class ImportMissionPackageResolver extends ImportResolver {
    public static class ImportMissionV1PackageResolver
            extends ImportMissionPackageResolver {

        /**
         * Construct an importer that works on MissionPackage/DataPackage files.    Users of this class
         * should either use .zip or .dpk for the mission package extension.
         *
         * @param strict     strict if the mission package is required to have a manifest
         */
        public ImportMissionV1PackageResolver(File destinationDir, String displayName, Drawable icon, boolean strict) {

            super(".zip", destinationDir, displayName, icon, strict);
        }
    }

    public static class ImportMissionV2PackageResolver
            extends ImportMissionPackageResolver {

        /**
         * Construct an importer that works on MissionPackage/DataPackage files.
         *
         * @param strict     strict if the mission package is required to have a manifest
         */
        public ImportMissionV2PackageResolver(File destinationDir, String displayName, Drawable icon, boolean strict) {

            super( ".dpk", destinationDir, displayName, icon, strict);
        }
    }

    private static final String TAG = "ImportMissionPackageSort";

    private final boolean _bStrict;

    public interface MissionPackageMatchListener extends MatchListener {
        boolean onMatch(ImportMissionPackageResolver resolver, File file);
    }

    public boolean getStrict() {
        return _bStrict;
    }

    /**
     * Construct an importer that works on MissionPackage/DataPackage files.    Users of this class
     * should either use .zip or .dpk for the mission package extension.
     * @param ext the extension of the file either .zip or .dpk
     * @param strict strict if the mission package is required to have a manifest
     */
    public ImportMissionPackageResolver(String ext, File destinationDir, String displayName, Drawable icon, boolean strict) {
        super(ext, destinationDir, displayName, icon);
        _bStrict = strict;
        contentType = "Data Package";
        mimeType = "application/zip";
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

        boolean matched = false;
        for (MatchListener l : getMatchListeners()) {
            if (l instanceof MissionPackageMatchListener) {
                matched |= ((MissionPackageMatchListener)l).onMatch(this, file);
            }
        }
        return matched;
    }

    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        notifyBeginImportListeners(file, flags, this);
        return super.beginImport(file, flags);
    }
}
