package com.atakmap.android.importfiles.callbacks;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import gov.tak.api.importfiles.ImportResolver;

final class CotCallback implements ImportResolver.BeginImportListener {
    private static final String TAG = "CotCallback";

    private final Context _context;

    public CotCallback() {
        _context = MapView.getMapView().getContext();
    }

    @Override
    public boolean onBeginImport(File file, EnumSet<ImportResolver.SortFlags> sortFlags, Object opaque) {
        if (!(opaque instanceof String))
            return false;

        Intent intent = new Intent();
        intent.setAction(ImportExportMapComponent.IMPORT_COT);
        intent.putExtra("xml", opaque.toString());
        AtakBroadcast.getInstance().sendBroadcast(intent);

        // remove the .cot file from source location so it won't be reimported next time ATAK starts
        File atakdata = new File(_context.getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG, "Deleted import CoT: " + file.getAbsolutePath());

        return true;
    }
}
