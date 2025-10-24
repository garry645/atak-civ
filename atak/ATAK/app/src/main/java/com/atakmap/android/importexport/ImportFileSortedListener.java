package com.atakmap.android.importexport;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

import gov.tak.api.importfiles.ImportResolver;

final class ImportFileSortedListener implements ImportResolver.FileSortedListener {
    @Override
    public void onFileSorted(ImportResolver.FileSortedNotification notification) {
        String path = notification.getDstUri();
        if (FileSystemUtils.isEmpty(path))
            return;
        URIContentHandler h = URIContentManager.getInstance()
                .getHandler(new File(FileSystemUtils
                        .sanitizeWithSpacesAndSlashes(path)));
        if (h != null && h.isActionSupported(GoTo.class))
            ((GoTo) h).goTo(false);
    }
}
