
package com.atakmap.android.eud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.atakmap.android.contentservices.cdn.TiledGeospatialContentMarshal;
import com.atakmap.android.grg.GRGMapComponent;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.layers.LayersMapComponent;
import com.atakmap.android.update.ProductProviderManager;
import com.atakmap.android.wfs.WFSImporter;
import com.atakmap.coremap.filesystem.SecureDelete;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.filesystem.HashingUtils;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.math.MathUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.engine.net.auth.OAuthAccessToken;
import gov.tak.api.engine.net.auth.OAuthTokenManager;
import gov.tak.platform.client.eud.EndPoints;
import gov.tak.platform.client.eud.MapSources;

public final class EudApiClient {
    public static final int RESOURCES_PLUGINS = 0x1;
    public static final int RESOURCES_MAP_SOURCES = 0x2;

    private final OAuthTokenManager _tokenManager;
    private final File _resourcesRootDir;
    private int _resourcesSyncMask = -1;

    EudApiProductProvider _productProvider;
    private final Context _pluginContext;

    public EudApiClient(Context mainContext, Context pluginContext,
            File resourcesDir, OAuthTokenManager tokenManager) {
        _tokenManager = tokenManager;
        _resourcesRootDir = resourcesDir;
        _productProvider = new EudApiProductProvider((Activity) mainContext,
                new File(_resourcesRootDir, "plugins"), tokenManager);
        _pluginContext = pluginContext;
    }

    EudApiProductProvider getProductProvider() {
        return _productProvider;
    }

    public void unlink() {
        evictResources(-1);
        _tokenManager.removeToken(EndPoints.AUTH_SERVER, EndPoints.client_id);
    }

    public void setResourcesSyncMask(int mask) {
        _resourcesSyncMask = mask;
    }

    public int getResourcesSyncMask() {
        return _resourcesSyncMask;
    }

    public boolean isLinked() {
        final OAuthAccessToken t = _tokenManager.getToken(EndPoints.AUTH_SERVER,
                EndPoints.client_id);
        return (t != null && t.isValid());
    }

    public boolean syncResources(int resourcesMask, boolean force)
            throws IOException {
        OAuthAccessToken t = _tokenManager.getToken(EndPoints.AUTH_SERVER,
                EndPoints.client_id);
        final boolean authorized = (t != null && t.isValid());

        resourcesMask &= _resourcesSyncMask;

        if (MathUtils.hasBits(resourcesMask, RESOURCES_PLUGINS)) {
            if (authorized)
                ProductProviderManager.addProvider(_productProvider);
            else
                evictResources(RESOURCES_PLUGINS);
        }

        if (MathUtils.hasBits(resourcesMask, RESOURCES_MAP_SOURCES)) {
            File syncedMapSourcesDir = new File(_resourcesRootDir,
                    "mapsources");
            syncedMapSourcesDir.mkdirs();

            Set<String> mapSourceFiles = new HashSet<>();
            while (true) {
                MapSources ms = MapSources.get(EndPoints.MAP_SOURCES, t);
                if (ms == null)
                    throw new RuntimeException(
                            "Failed to access map sources API");

                for (MapSources.Element e : ms.elements) {
                    File mapSourceFile = new File(syncedMapSourcesDir,
                            e.file_src.substring(
                                    e.file_src.lastIndexOf('/') + 1));
                    mapSourceFiles.add(mapSourceFile.getName());
                    byte[] xml = MapSources.getFile(t, e);
                    if (xml == null)
                        continue;
                    if (mapSourceFile.exists() && !force) {
                        final String hashLocal = HashingUtils
                                .sha256sum(mapSourceFile);
                        final String hashRemote = HashingUtils.sha256sum(xml);
                        if (hashLocal.equals(hashRemote)) {
                            // don't overwrite the local copy but still allow it to pass through to
                            // be imported as this will keep the local file and the parsed/imported
                            // representations up to date.  Saavy importers will ignore the request
                            // as the file has not been changed
                            xml = null;
                        }
                    }

                    if (xml != null) {
                        try (FileOutputStream fos = IOProviderFactory
                                .getOutputStream(mapSourceFile)) {
                            fos.write(xml);
                        }
                    }

                    importMapSource(mapSourceFile,
                            ImportExportMapComponent.ACTION_IMPORT_DATA);
                }

                break;
            }

            // evict any map sources that have been removed from the server listing
            File[] installedMapSourceFiles = syncedMapSourcesDir.listFiles();
            if (installedMapSourceFiles != null) {
                for (File mapSourceFile : installedMapSourceFiles) {
                    if (!mapSourceFiles.contains(mapSourceFile.getName()))
                        importMapSource(mapSourceFile,
                                ImportExportMapComponent.ACTION_DELETE_DATA);
                }
            }
        }

        return true;
    }

    private static void importMapSource(File mapSourceFile, String action) {
        final StreamingTiles tiles = StreamingTiles.parse(mapSourceFile,
                16 * 1024);
        final boolean overlay = (tiles != null && tiles.overlay);
        final String type = (tiles == null || tiles.content == null) ? "imagery"
                : tiles.content;
        Intent i = new Intent(action);
        i.putExtra(ImportReceiver.EXTRA_URI,
                Uri.fromFile(mapSourceFile).toString());
        i.putExtra(ImportReceiver.EXTRA_ADVANCED_OPTIONS, true);
        // configure content type and MIME
        switch (type) {
            case "terrain":
                i.putExtra(ImportReceiver.EXTRA_CONTENT,
                        TiledGeospatialContentMarshal.INSTANCE_TERRAIN
                                .getContentType());
                i.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                        TiledGeospatialContentMarshal.DEFAULT_MIME_TYPE_TERRAIN);
                break;
            case "imagery":
                // imagery is default path
            default:
                i.putExtra(ImportReceiver.EXTRA_CONTENT,
                        overlay ? GRGMapComponent.IMPORTER_CONTENT_TYPE
                                : LayersMapComponent.IMPORTER_CONTENT_TYPE);
                i.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                        overlay ? GRGMapComponent.IMPORTER_DEFAULT_MIME_TYPE
                                : WFSImporter.MIME_XML);
                break;
        }

        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    void evictResources(int mask) {
        if (MathUtils.hasBits(mask, RESOURCES_PLUGINS)) {
            ProductProviderManager.removeProvider(_productProvider);
            SecureDelete.deleteDirectory(
                    new File(_resourcesRootDir, "plugins"),
                    false, true);
        }

        if (MathUtils.hasBits(mask, RESOURCES_MAP_SOURCES)) {
            File syncedMapSourcesDir = new File(_resourcesRootDir,
                    "mapsources");
            File[] mapSourceFiles = syncedMapSourcesDir.listFiles();
            if (mapSourceFiles != null) {
                for (File mapSourceFile : mapSourceFiles)
                    importMapSource(mapSourceFile,
                            ImportExportMapComponent.ACTION_DELETE_DATA);
            }
            SecureDelete.deleteDirectory(
                    syncedMapSourcesDir,
                    false, true);
        }
    }
}
