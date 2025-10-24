package gov.tak.api.importfiles;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.commons.graphics.Drawable;

/**
 * Sorts Video Files.
 */
public class ImportVideoResolver extends ImportResolver {

    private static final String TAG = "ImportVideoSort";

    public final static Set<String> VIDEO_EXTENSIONS = new HashSet<>(
            Arrays.asList("mpeg", "mpg", "ts", "avi", "mp4",
                    "264", "265", "wmv", "mov", "webm",
                    "mov", "mkv", "flv"));

    public ImportVideoResolver(String ext, File destinationDir, String displayName, Drawable icon) {
        super(ext, destinationDir, displayName, icon);
    }

    public ImportVideoResolver(String displayName, File destinationDir, Drawable icon) {
        this(null, destinationDir, displayName, icon);
        contentType = "Video";
        mimeType = "application/octet-stream";
    }

    @Override
    public boolean match(File file) {
        // Check against default video extensions if this resolver does not
        // specify a single extension
        if (ext == null) {
            String ext = FileSystemUtils.getExtension(file, false, false)
                    .toLowerCase(LocaleUtil.getCurrent());
            return VIDEO_EXTENSIONS.contains(ext);
        }

        // Default matching
        return super.match(file);
    }

    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        if (flags.contains(SortFlags.IMPORT_INPLACE)) {
            flags.remove(SortFlags.IMPORT_INPLACE);
            flags.add(SortFlags.IMPORT_COPY);
        }

        super.beginImport(file, flags);

        notifyBeginImportListeners(file, flags, null);

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        if (file == null)
            return null;
        return new File(destinationDir, file.getName());
    }
}
