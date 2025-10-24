
package com.atakmap.android.video;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.util.ResUtils;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.android.video.manager.VideoXMLHandler;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.util.EnumSet;
import java.util.List;

/**
 * Sorts video aliases
 */
public class ImportVideoAliasResolver extends gov.tak.api.importfiles.ImportResolver {

    private static final String TAG = "ImportVideoAliasSort";

    private static final String CONTENT_TYPE = "Video Alias";
    private static final String MIME_TYPE = "application/xml";

    private final Context _context;
    private final VideoXMLHandler _xmlHandler = new VideoXMLHandler();

    public ImportVideoAliasResolver(Context context) {
        super(".xml", null, context.getString(R.string.video_alias),
                ResUtils.getDrawable(context, R.drawable.ic_video_alias));
        _context = context;
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;
        try (BufferedReader br = new BufferedReader(
                IOProviderFactory.getFileReader(file))) {
            // read first few hundred bytes and search for known strings
            char[] buffer = new char[1024];
            int numRead = br.read(buffer);
            br.close();
            if (numRead < 1) {
                Log.d(TAG, "Failed to read txt stream");
                return false;
            }
            final String content = String.valueOf(buffer, 0, numRead);
            return content.contains("<videoConnections>")
                    || content.contains("<feed>");
        } catch (Exception e) {
            Log.d(TAG, "Failed to match txt", e);
        }
        return false;
    }

    @Override
    public boolean beginImport(File file, EnumSet<SortFlags> flags) {
        // Deserialize new aliases
        List<ConnectionEntry> imported = null;
        try {
            byte[] encoded = FileSystemUtils.read(file);
            String xml = new String(encoded, FileSystemUtils.UTF8_CHARSET);
            imported = _xmlHandler.parse(xml);
        } catch (Exception e) {
            Log.d(TAG, "Failed to read video links XML: " + file, e);
        }
        if (FileSystemUtils.isEmpty(imported))
            return false;

        // Add and persist entries
        VideoManager.getInstance().addEntries(imported);

        onFileSorted(file, file, flags);

        File atakdata = new File(_context.getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG, "deleted imported video alias: "
                    + file.getAbsolutePath());

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        return file;
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(CONTENT_TYPE, MIME_TYPE);
    }
}
