
package com.atakmap.android.missionpackage.event;

import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.missionpackage.file.MissionPackageContent;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.ui.MissionPackageListFileItem;
import com.atakmap.android.missionpackage.ui.MissionPackageListGroup;
import com.atakmap.util.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Handler for Mission Package being processed
 *
 * @deprecated use {@link IMissionPackageEventHandler2}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public interface IMissionPackageEventHandler {

    /**
     * If file is supported, add to group/manifest. Does not actually update the zip
     * 
     * @param group the group to add to
     * @param file the file
     * @return true if event is consumed
     */
    boolean add(MissionPackageListGroup group, File file);

    /**
     * If file is supported, remove from group/manifest. Does not actually update the zip
     * 
     * @param group the group to remove from
     * @param file the file
     * @return true if event is consumed
     */
    boolean remove(MissionPackageListGroup group,
            MissionPackageListFileItem file);

    /**
     * If content is supported, extract it from the zip
     * 
     * @param manifest the manifest to use during the extraction
     * @param content the content
     * @param zipFile the file
     * @param atakDataDir the directory to unzip to
     * @param buffer the buffer to use
     * @param sorters the list of appropriate sorters to use during matching after the mission
     *                package is extracted
     * @return true if event is consumed
     */
    boolean extract(MissionPackageManifest manifest,
            MissionPackageContent content, ZipFile zipFile,
            File atakDataDir, byte[] buffer, List<ImportResolver> sorters)
            throws IOException;
}
