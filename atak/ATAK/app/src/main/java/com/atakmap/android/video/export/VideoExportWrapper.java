
package com.atakmap.android.video.export;

import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

public class VideoExportWrapper {
    /**
     * List of exports
     */
    private final List<ConnectionEntry> _exports;

    public VideoExportWrapper() {
        _exports = new ArrayList<>();
    }

    public VideoExportWrapper(ConnectionEntry e) {
        _exports = new ArrayList<>();
        _exports.add(e);
    }

    public List<ConnectionEntry> getExports() {
        return _exports;
    }

    /**
     * Add all data from the specified folder to 'this' folder
     * @param folder the folder to add
     */
    public void add(VideoExportWrapper folder) {
        if (folder == null)
            return;

        if (!FileSystemUtils.isEmpty(folder.getExports()))
            _exports.addAll(folder.getExports());
    }

    public void add(ConnectionEntry entry) {
        if (entry == null)
            return;

        _exports.add(entry);
    }

    /**
     * Returns a boolean based on the size of the export list.
     * @return true if the number of exports is 0 or false if the number of exports is greater than 0.
     */
    public boolean isEmpty() {
        return FileSystemUtils.isEmpty(_exports);
    }

}
