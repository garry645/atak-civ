
package com.atakmap.android.missionpackage.api;

import android.widget.Toast;
import com.atakmap.android.maps.MapView;

import com.atakmap.android.missionpackage.file.task.MissionPackageBaseTask;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

import gov.tak.api.annotation.DeprecatedApi;

@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ToastSaveCallback implements SaveAndSendCallback {

    @Override
    public void onMissionPackageTaskComplete(final MissionPackageBaseTask task,
            final boolean success) {
        final MapView mapView = task.getMapView();
        if (mapView != null) {
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    if (success)
                        Toast.makeText(task.getContext(),
                                "Exported: "
                                        + FileSystemUtils.prettyPrint(
                                                new File(task.getManifest()
                                                        .getLastSavedPath())),
                                Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(task.getContext(),
                                "Failed to export: "
                                        + task.getManifest().getName(),
                                Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
