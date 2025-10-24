
package com.atakmap.android.missionpackage.event;

import android.content.Context;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.missionpackage.file.MissionPackageContent;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.ui.MissionPackageListFileItem;
import com.atakmap.android.missionpackage.ui.MissionPackageListGroup;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gov.tak.api.importfiles.ImportResolver;

/**
 * Processes changes to Mission Packages via registered handlers
 */
public class MissionPackageEventProcessor2 implements
        IMissionPackageEventHandler2 {

    private static final String TAG = "MissionPackageEventProcessor2";
    /**
     * List of handlers for processing Mission Packages
     */
    private final List<IMissionPackageEventHandler2> _addEventHandlers;

    public MissionPackageEventProcessor2(final Context context,
                                        final MapGroup mapGroup) {
        _addEventHandlers = new ArrayList<>();
        _addEventHandlers.add(new MissionPackageFileAttachmentHandler2(context,
                mapGroup));
        _addEventHandlers.add(new MissionPackageShapefileHandler2(context));
        _addEventHandlers.add(new MissionPackageEventHandler2(context)); // catch all
    }

    @Override
    public boolean add(MissionPackageListGroup group, File file) {

        for (IMissionPackageEventHandler2 handler : _addEventHandlers) {
            if (handler.add(group, file)) {
                Log.d(TAG, handler.getClass().getName() + " added: "
                        + file.getAbsolutePath());
                return true;
            }
        }

        Log.w(TAG, "No handlers added file: " + file.getAbsolutePath());
        return false;
    }

    @Override
    public boolean remove(MissionPackageListGroup group,
                          MissionPackageListFileItem item) {
        for (IMissionPackageEventHandler2 handler : _addEventHandlers) {
            if (handler.remove(group, item)) {
                Log.d(TAG, handler.getClass().getName() + " removed: "
                        + item.toString());
                return true;
            }
        }

        Log.w(TAG, "No handlers removed file: " + item.toString());
        return false;
    }

    @Override
    public boolean extract(MissionPackageManifest manifest,
                           MissionPackageContent content, ZipFile zipFile,
                           File atakDataDir, byte[] buffer, List<ImportResolver> sorters)
            throws IOException {
        for (IMissionPackageEventHandler2 handler : _addEventHandlers) {
            if (handler.extract(manifest, content, zipFile, atakDataDir,
                    buffer, sorters)) {
                Log.d(TAG, handler.getClass().getName() + " extracted: "
                        + content.toString());
                return true;
            }
        }

        Log.w(TAG, "No handlers extract content: " + content.toString());
        return false;
    }
}
