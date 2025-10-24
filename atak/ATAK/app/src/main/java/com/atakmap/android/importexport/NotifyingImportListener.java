package com.atakmap.android.importexport;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import gov.tak.api.importfiles.ImportResolver;

/**
 * Posts a notification when an {@link gov.tak.api.importfiles.ImportResolver}
 * {@code onFileSorted's} with an {@link ImportResolver.SortFlags#SHOW_NOTIFICATIONS} sort flag.
 */
final class NotifyingImportListener implements ImportResolver.ImportListener {
    private static final String TAG = "ImportListenerImpl";

    @Override
    public void onFileImported(File src, File dst, EnumSet<ImportResolver.SortFlags> sortFlags) {
        MapView mv = MapView.getMapView();
        if (sortFlags.contains(ImportResolver.SortFlags.SHOW_NOTIFICATIONS) && mv != null) {
            Intent i = new Intent(ImportExportMapComponent.ZOOM_TO_FILE_ACTION);
            i.putExtra("filepath", dst.getAbsolutePath());
            Context ctx = mv.getContext();
            NotificationUtil.getInstance().postNotification(
                    NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                    NotificationUtil.BLUE,
                    ctx.getString(R.string.importmgr_finished_import,
                            src.getName()),
                    null, null, i);
            Log.d(TAG,
                    "begin onFileSorted, notification " + getClass()
                            + " for: " + dst.getAbsolutePath());
        }
    }
}
