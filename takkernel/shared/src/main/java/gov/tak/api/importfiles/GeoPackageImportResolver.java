package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.gpkg.GeoPackage;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import gov.tak.api.commons.graphics.Drawable;

public final class GeoPackageImportResolver
        extends ImportResolver {
    public GeoPackageImportResolver(String displayName, File destinationDir, Drawable icon) {
        super("gpkg", destinationDir, displayName, icon);
        contentType = "GeoPackage";
        mimeType = "application/octet-stream";
    }

    @Override
    public boolean match(File file) {
        return super.match(file) && GeoPackage.isGeoPackage(file);
    }

    @Override
    protected void onFileSorted(File src,
                                File dst,
                                EnumSet<SortFlags> flags) {
        notifyFileSortedListeners(src, dst, flags);
    }
}