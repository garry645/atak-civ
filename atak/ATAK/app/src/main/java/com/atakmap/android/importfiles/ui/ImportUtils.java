
package com.atakmap.android.importfiles.ui;

import androidx.annotation.NonNull;

import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ImportUtils {

    private static final String TAG = "ImportUtils";

    /**
     * Remove all subfiles and directories of a directory from selectedItems
     * @param directory the directory to process
     * @param selectedFilesMap a map version of selectedItems
     */
    public static void removeSubFilesFromSetRecursive(@NonNull
    final File directory,
            @NonNull
            final Map<String, File> selectedFilesMap,
            @NonNull
            final Set<File> selectedItems) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File dirFile : files) {
                    try {
                        File subFile = selectedFilesMap
                                .get(dirFile.getCanonicalPath());
                        if (subFile != null) {
                            selectedItems.remove(subFile);
                        }
                        removeSubFilesFromSetRecursive(dirFile,
                                selectedFilesMap,
                                selectedItems);
                    } catch (IOException e) {
                        Log.d(TAG,
                                "Unable to get a canonical path in removeSubfilesFromSelectedRecursive",
                                e);
                    }
                }
            }
        }
    }
}
