
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportCotResolver;

/**
 * Dispatches CoT Events, rather than sorts to an ATAK data directory
 *
 * @deprecated use {@link ImportCotResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportCotSort extends ImportResolver {

    private static final String TAG = "ImportCotSort";

    private static final int PROBE_SIZE = 384;

    private final static String COTMATCH = "<event";
    private final static String COTMATCH2 = "<point";

    private final char[] _charBuffer; // reuse for performance
    private final Context _context;

    public ImportCotSort(final Context context, final boolean validateExt,
            final boolean copyFile) {
        super(".cot", "", validateExt, copyFile);
        _context = context;
        _charBuffer = new char[PROBE_SIZE];
    }

    @Override
    public boolean match(final File file) {
        if (!super.match(file))
            return false;

        // it is a .cot, now lets see if it contains reasonable CoT
        try (FileInputStream fis = IOProviderFactory.getInputStream(file)) {
            return isCoT(fis, _charBuffer);
        } catch (IOException e) {
            Log.e(TAG, "Error checking if CoT: " + file.getAbsolutePath(), e);
        }

        return false;
    }

    private static boolean isCoT(final InputStream stream,
            final char[] buffer) {
        try {
            // read first few hundred bytes and search for known CoT strings
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));
            int numRead = -1;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .cot stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            return isCoT(content);
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .cot", e);
            return false;
        }
    }

    public static boolean isCoT(String content) {
        if (FileSystemUtils.isEmpty(content)) {
            Log.w(TAG, "Unable to match empty content");
            return false;
        }

        boolean match = content.contains(COTMATCH)
                && content.contains(COTMATCH2);
        if (!match) {
            Log.d(TAG, "Failed to match content from .cot: ");
        }

        return match;
    }

    @Override
    public boolean beginImport(File file) {
        return beginImport(file, Collections.emptySet());
    }

    /**
     * Send intent so CoT will be dispatched internally within ATAK
     * 
     * @param file the file to import
     * @return true if the import was successful
     */
    @Override
    public boolean beginImport(final File file, final Set<SortFlags> flags) {
        String event;
        try (FileInputStream fis = IOProviderFactory.getInputStream(file)) {
            event = FileSystemUtils.copyStreamToString(fis, true,
                    FileSystemUtils.UTF8_CHARSET, _charBuffer);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load CoT Event: " + file.getAbsolutePath(),
                    e);
            return false;
        }

        if (FileSystemUtils.isEmpty(event)) {
            Log.e(TAG, "Failed to load CoT Event: " + file.getAbsolutePath());
            return false;
        }

        Intent intent = new Intent();
        intent.setAction(ImportExportMapComponent.IMPORT_COT);
        intent.putExtra("xml", event);
        AtakBroadcast.getInstance().sendBroadcast(intent);

        // remove the .cot file from source location so it won't be reimported next time ATAK starts
        File atakdata = new File(_context.getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG, "Deleted import CoT: " + file.getAbsolutePath());

        return true;
    }

    @Override
    public File getDestinationPath(File file) {
        return file;
    }

    @Override
    public String getDisplayableName() {
        return _context.getString(R.string.cot_event);
    }

    @Override
    public Drawable getIcon() {
        return _context.getDrawable(R.drawable.unknown);
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("CoT Event", "application/cot+xml");
    }

}
