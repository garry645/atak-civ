package gov.tak.api.importfiles;

import java.io.File;
import java.util.EnumSet;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Imports GPX Route Files
 *
 */
public class ImportGPXRouteResolver extends ImportGPXResolver {

    private static final String TAG = "ImportGPXRouteSort";

    public ImportGPXRouteResolver(String displayName, File destinationDir, Drawable icon) {
        super(displayName, destinationDir, icon);
    }

    @Override
    protected void onFileSorted(File src, File dst, EnumSet<SortFlags> flags) {
        notifyFileSortedListeners(src, dst, flags);
    }
}
