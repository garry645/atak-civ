
package com.atakmap.android.elev.tiles;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.atakmap.android.contentservices.cdn.TiledGeospatialContentMarshal;
import com.atakmap.android.importexport.AbstractImporter;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

public final class TiledElevationImporter extends AbstractImporter {
    final static Set<String> supportedMimeTypes = TiledGeospatialContentMarshal.MIME_TYPES_TERRAIN;

    private final TiledElevationSources _db;

    /** @deprecated use {@link #TiledElevationImporter(TiledElevationSources)} */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public TiledElevationImporter(Context context, TiledElevationSources db) {
        this(db);
    }

    public TiledElevationImporter(TiledElevationSources db) {
        super(TiledGeospatialContentMarshal.INSTANCE_TERRAIN.getContentType());

        _db = db;
    }

    @Override
    public Set<String> getSupportedMIMETypes() {
        return supportedMimeTypes;
    }

    @Override
    public CommsMapComponent.ImportResult importData(InputStream source,
            String mime, Bundle b) throws IOException {
        return CommsMapComponent.ImportResult.FAILURE;
    }

    @Override
    public CommsMapComponent.ImportResult importData(Uri uri, String mime,
            Bundle b) throws IOException {
        final File file = FileSystemUtils.getFile(uri);
        if (file == null)
            return CommsMapComponent.ImportResult.FAILURE;
        final ElevationSource elevationSource = _db.add(file);
        if (elevationSource == null)
            return CommsMapComponent.ImportResult.FAILURE;

        return CommsMapComponent.ImportResult.SUCCESS;
    }

    @Override
    public boolean deleteData(Uri uri, String mime) throws IOException {
        final File file = FileSystemUtils.getFile(uri);
        if (file == null)
            return false;

        final ElevationSource elevationSource = _db.getSource(file);
        if (elevationSource == null)
            return false;

        ElevationSourceManager.detach(elevationSource);
        _db.remove(file);

        return true;
    }
}
