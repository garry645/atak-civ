package com.atakmap.android.importfiles.callbacks;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import gov.tak.api.importfiles.ImportResolver;

final class VideoCallback implements ImportResolver.BeginImportListener {
    private final String TAG = "VideoCallback";

    @Override
    public boolean onBeginImport(File file, EnumSet<ImportResolver.SortFlags> sortFlags, Object opaque) {
        File atakdata = new File(MapView.getMapView().getContext().getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG, "deleted imported video: " + file.getAbsolutePath());
        return true;
    }
}
