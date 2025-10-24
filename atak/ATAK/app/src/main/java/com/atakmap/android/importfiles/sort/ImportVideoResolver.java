
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.util.ResUtils;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Sorts Video Files.
 */
public class ImportVideoResolver extends gov.tak.api.importfiles.ImportResolver {

    private static final String TAG = "ImportVideoSort";

    private static final String CONTENT_TYPE = "Video";

    private final Context _context;

    public final static Set<String> VIDEO_EXTENSIONS = new HashSet<>(
            Arrays.asList("mpeg", "mpg", "ts", "avi", "mp4",
                    "264", "265", "wmv", "mov", "webm",
                    "mov", "mkv", "flv"));

    public ImportVideoResolver(Context context, String ext) {
        super(ext, FileSystemUtils.getRoot(), context.getString(R.string.video),
                ResUtils.getDrawable(context, R.drawable.cot_icon_sugp));
        _context = context;
    }

    public ImportVideoResolver(Context context) {
        this(context, null);
    }

    @Override
    public boolean match(File file) {

        // Check against default video extensions if this sorter does not
        // specify a single extension
        if (ext == null) {
            String ext = FileSystemUtils.getExtension(file, false, false)
                    .toLowerCase(LocaleUtil.getCurrent());
            return VIDEO_EXTENSIONS.contains(ext);
        }
        // Default matching
        else return super.match(file);

        // TODO: Check if the file is actually a video - otherwise this sorter
        //  is useless when validateExt = false
    }

    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {

        if (flags.contains(SortFlags.IMPORT_INPLACE)) {
            flags.remove(SortFlags.IMPORT_INPLACE);
            flags.add(SortFlags.IMPORT_COPY);
        }

        super.beginImport(file, flags);

        File atakdata = new File(_context.getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG,
                    "deleted imported video: " + file.getAbsolutePath());

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        if (file == null)
            return null;
        return new File(FileSystemUtils.getItem("tools/videos"),
                file.getName());
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(CONTENT_TYPE, ResourceFile.UNKNOWN_MIME_TYPE);
    }
}
