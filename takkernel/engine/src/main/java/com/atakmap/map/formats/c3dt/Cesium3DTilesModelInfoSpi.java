package com.atakmap.map.formats.c3dt;

import android.net.Uri;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.map.layer.model.ModelInfoSpi;
import com.atakmap.map.layer.model.opengl.GLSceneFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class Cesium3DTilesModelInfoSpi implements ModelInfoSpi
{
    private static final String TAG = "Cesium3DTilesModelInfoSpi";
    public final static ModelInfoSpi INSTANCE = new Cesium3DTilesModelInfoSpi();

    static
    {
        GLSceneFactory.registerSpi(GLTileset.SPI);
    }

    @Override
    public String getName()
    {
        return "Cesium 3D Tiles";
    }

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public boolean isSupported(String uriString)
    {
        // URI must have tileset.json
        try
        {
            URI uri = new URI(uriString);
            String path = uri.getPath();
            String[] parts = path.split("/");
            if (parts.length > 0 && parts[parts.length - 1].toLowerCase(LocaleUtil.getCurrent()).contains("tileset.json"))
                return true;
            else if (uri.getScheme() != null && uri.getScheme().equals("http") || uri.getScheme().equals("https"))
                // we will allow through all HTTPS URLs, but then subsequently
                // filter in `create`
                return true;
        } catch (Exception e)
        {
            // ignore
        }

        // fallback on File test
        File f = FileSystemUtils.getFile(uriString);
        final File tilesetJson = getTilesetJson(f, true);
        return (tilesetJson != null);
    }

    @Override
    public Set<ModelInfo> create(String s)
    {
        File f = new File(s);
        if (FileSystemUtils.checkExtension(f, "zip") ||
                FileSystemUtils.checkExtension(f, "3tz"))
        {
            try
            {
                f = new ZipVirtualFile(f);
            } catch (Throwable ignored) {}
        }
        // remote URL load
        if (!IOProviderFactory.exists(f))
        {
            // if the URL does not specifically reference the `tileset.json` file, try to insert
            // XXX - obtained transformed URIs
            List<String> uris = new ArrayList<>(2);
            // XXX - try load each
            if (!s.toLowerCase(LocaleUtil.getCurrent()).contains("tileset.json") && !s.toLowerCase(LocaleUtil.getCurrent()).contains("root.json"))
            {
                final String[] rootAliases = { "tileset.json", "root.json" };
                for(String rootAlias : rootAliases) {
                    try {
                        URI uri = new URI(s);
                        String baseUri = s;
                        if (baseUri.indexOf('?') >= 0)
                            baseUri = s.substring(0, baseUri.indexOf('?'));
                        if (!baseUri.endsWith("/"))
                            baseUri += "/";
                        if (uri.getRawQuery() != null)
                            uris.add(baseUri + rootAlias + "?" + uri.getRawQuery());
                        else
                            uris.add(baseUri + rootAlias);
                    } catch (Throwable t) {
                        // insert of `tileset.json` file failed, halt further processing
                        return null;
                    }
                }
                if(uris.isEmpty())
                    return null;
            } else {
                uris.add(s);
            }
            for(String u : uris) {
                final Set<ModelInfo> retval = createFromUri(u);
                if (retval != null) return retval;
            }
            return null;
        }

        if (IOProviderFactory.isDirectory(f))
        {
            final File tilesetJson = getTilesetJson(f, true);
            if (tilesetJson != null)
                f = tilesetJson;
        }
        if (!IOProviderFactory.exists(f) || !f.getName().equals("tileset.json"))
            return null;

        return createFromFile(f);
    }

    private Set<ModelInfo> createFromFile(File file)
    {
        String uri = "file://" + CesiumUtility.nativePathToUriPath(file.getAbsolutePath());
        return createFromUri(uri);
    }
    
    private Set<ModelInfo> createFromUri(String uri)
    {
        Tileset tileset = Tileset.parse(uri, new AssetAccessor(null), null);
        if (tileset == null) return null;
        ModelInfo info = new ModelInfo();
        info.altitudeMode = ModelInfo.AltitudeMode.Absolute;
        info.localFrame = null;
        info.minDisplayResolution = Double.MAX_VALUE;
        info.maxDisplayResolution = 0d;
        String name = uri;
        try {
            name = Uri.parse(uri).getPath();
        } catch (Throwable ignored) {}
        info.name = name;
        info.uri = uri;
        info.srid = 4326;
        info.type = getName();

        // Make sure we've loaded the root tile
        tileset.loadRootTileSync();
        Tile rootTile = tileset.getRootTile();
        if (rootTile != null) {
            final Envelope bounds = rootTile.getBoundingBox();
            if (bounds != null) {
                info.location = new GeoPoint((bounds.minY + bounds.maxY) / 2d, (bounds.minX + bounds.maxX) / 2d);
                info.metadata = new AttributeSet();
                info.metadata.setAttribute("aabb",
                        new double[]{
                                bounds.minX,
                                bounds.minY,
                                bounds.minZ,
                                bounds.maxX,
                                bounds.maxY,
                                bounds.maxZ
                        });
            }
        }
        tileset.dispose();
        return Collections.singleton(info);
    }

    private static File getTilesetJson(File f, boolean recurseOnZip)
    {
        if (FileSystemUtils.checkExtension(f, "zip") ||
                FileSystemUtils.checkExtension(f, "3tz"))
        {
            try
            {
                f = new ZipVirtualFile(f);
            } catch (Throwable ignored) {}
        }
        // attempt to recurse if flag is set and source is a zip file
        final boolean recurse = (recurseOnZip && f instanceof ZipVirtualFile);
        // locate the `tileset.json` file
        File tilesetJson;
        if (!IOProviderFactory.isDirectory(f))
            return null; // XXX - workaround for ATAK-12324
        else if (f instanceof ZipVirtualFile)
            tilesetJson = new ZipVirtualFile(f, "tileset.json");
        else
            tilesetJson = new File(f, "tileset.json");
        if (!IOProviderFactory.exists(tilesetJson) && recurse)
        {
            // recurse 1 level if `tileset.json` was not found at root
            File[] children = IOProviderFactory.listFiles(f);
            if (children != null)
            {
                for (File c : children)
                {
                    tilesetJson = getTilesetJson(c, false);
                    if (tilesetJson != null)
                        break;
                }
            }
        }

        if (tilesetJson == null)
            return null;

        return (IOProviderFactory.exists(tilesetJson) &&
                tilesetJson.getName().equals("tileset.json")) ? tilesetJson : null;
    }
}
