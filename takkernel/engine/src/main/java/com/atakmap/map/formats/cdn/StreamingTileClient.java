package com.atakmap.map.formats.cdn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.ContainerInitializationControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.gpkg.GeoPackageTileContainer;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.osm.OSMDroidTileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientSpi;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileProxy;
import com.atakmap.map.layer.raster.tilematrix.TileScraper;
import com.atakmap.map.projection.Projection;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClientBuilder;
import gov.tak.api.engine.net.IResponse;
import gov.tak.api.engine.net.auth.HttpClientAuthenticationRegistry;
import gov.tak.api.engine.net.auth.IHttpClientAuthenticationHandler;

final class StreamingTileClient implements TileClient, TilesMetadataControl
{
    public final static TileClientSpi SPI = new TileClientSpi() {
        @Override
        public String getName() {
            return "tiles";
        }

        @Override
        public TileClient create(String path, String offlineCachePath, Options opts) {
            try {
                File f = new File(path);
                if(!f.exists())
                    return null;
                // limit to 16kb to avoid DoS
                final int limit = 16*1024;
                final StreamingTiles config = StreamingTiles.parse(f, limit);
                if(config == null)
                    return null;
                TileClient client = new StreamingTileClient(f, config);
                if((opts != null && opts.proxy) && offlineCachePath != null) {
                    do {
                        String[] preferredProviders = new String[]
                        {
                            OSMDroidTileContainer.SPI.getName(),
                            GeoPackageTileContainer.SPI.getName(),
                            null,
                        };
                        TileContainer cache = null;
                        for(String preferredProvider : preferredProviders) {
                            cache = TileContainerFactory.openOrCreateCompatibleContainer(offlineCachePath, client, preferredProvider);
                            if (cache != null)
                                break;
                        }
                        if(cache == null)
                            break;
                        client = new TileProxy(client, cache);
                    } while(false);
                }
                return client;
            } catch(Throwable t) {
                return null;
            }
        }

        @Override
        public int getPriority() {
            return 1;
        }
    };

    private final static String TAG = "StreamingTileClient";

    private final static BitmapFactory.Options CACHE_OPTS = new BitmapFactory.Options();

    static
    {
        CACHE_OPTS.inJustDecodeBounds = true;
    }

    private final File configFile;
    private final StreamingTiles config;
    private final int srid;
    IHttpClientAuthenticationHandler auth;
    private Envelope bounds;
    private PointD origin;

    public StreamingTileClient(StreamingTiles src)
    {
        this(null, src);
    }

    private StreamingTileClient(File configFile, StreamingTiles src)
    {
        this.configFile = configFile;
        this.config = src;
        if(this.config.srs == null)
            throw new IllegalArgumentException();

        if(!this.config.srs.matches("EPSG:\\d+"))
            throw new IllegalArgumentException();

        this.srid = Integer.parseInt(this.config.srs.substring(5));
        final Projection proj = MobileImageryRasterLayer2.getProjection(srid);
        this.origin = (this.config.origin == null) ?
                proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMinLongitude()), null) :
                new PointD(this.config.origin.x, this.config.origin.y);

        if(this.config.bounds != null) {
            this.bounds = new Envelope(
                    this.config.bounds.minX, this.config.bounds.minY, 0d,
                    this.config.bounds.maxX, this.config.bounds.maxY, 0d);
        } else {
            PointD ul = proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMinLongitude()), null);
            PointD ur = proj.forward(new GeoPoint(proj.getMaxLatitude(), proj.getMaxLongitude()), null);
            PointD lr = proj.forward(new GeoPoint(proj.getMinLatitude(), proj.getMaxLongitude()), null);
            PointD ll = proj.forward(new GeoPoint(proj.getMinLatitude(), proj.getMinLongitude()), null);

            this.bounds = new Envelope(MathUtils.min(ul.x, ur.x, lr.x, ll.x),
                    MathUtils.min(ul.y, ur.y, lr.y, ll.y),
                    0d,
                    MathUtils.max(ul.x, ur.x, lr.x, ll.x),
                    MathUtils.max(ul.y, ur.y, lr.y, ll.y),
                    0);
        }
    }

    @Override
    public String getName()
    {
        return this.config.name;
    }

    @Override
    public int getSRID()
    {
        return this.srid;
    }

    @Override
    public ZoomLevel[] getZoomLevel()
    {
        return this.config.tileMatrix;
    }

    @Override
    public double getOriginX()
    {
        return this.origin.x;
    }

    @Override
    public double getOriginY()
    {
        return this.origin.y;
    }

    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
    {
        byte[] data = this.getTileData(zoom, x, y, error);
        if (data == null)
            return null;
        Bitmap retval = BitmapFactory.decodeByteArray(data, 0, data.length);

        // XXX - resize to 256x256. This obviously isn't the most efficient
        // solution, however, it's no worse than doing the flip on the
        // legacy tilesets
        if (retval != null
                && (retval.getWidth() != 256 || retval.getHeight() != 256))
        {
            Bitmap scaled = Bitmap.createScaledBitmap(retval, 256, 256, false);
            retval.recycle();
            retval = scaled;
        }

        return retval;
    }

    @Override
    public byte[] getTileData(int zoom, int x, int y, Throwable[] error)
    {
        if(config.invertYAxis)
            y = (1<<zoom)-y-1;

        byte[] retval = null;
        try
        {
            String url = config.url;
            url = url.replace("{$x}", String.valueOf(x));
            url = url.replace("{$y}", String.valueOf(y));
            url = url.replace("{$z}", String.valueOf(zoom));

            IHttpClientBuilder client = HttpClientBuilder.newBuilder(url);
            client.addHeader("User-Agent", "TAK");

            // NOTE: auth is deferred due to a circular dependency in ATAK construction between the
            // module responsible for imagery (including import) and the module responsible for the
            // EUD API, which installs the authentication handler

            // look up auth handler given authentication
            if(config.authorization != null && auth == null) {
                Map<String, String> extras = new HashMap<>();
                if(config.authorization.clientId != null)
                    extras.put("clientId", config.authorization.clientId);
                auth = HttpClientAuthenticationRegistry.createAuthenticationHandler(config.authorization.type, config.authorization.server, extras);
            }
            if(auth != null)
                client = auth.configureClient(client);
            try(IResponse response = client.get().execute()) {
                if(response.getCode()/100 == 2)
                    retval = FileSystemUtils.read(response.getBody());
            }
        } catch (IOException e)
        {
            IOException ex = e;
            String reason = "IO Error";
            if (e instanceof java.net.SocketTimeoutException)
            {
                ex = null;
                reason = "Timeout";
            }
            Log.e(TAG, reason + " during tile download, "
                    + this.getName() + " (" + zoom + ", "
                    + x + ", " + y + ")", ex);
            if (error != null)
                error[0] = e;
        } catch (Throwable t)
        {
            Log.e(TAG,
                    "Unspecified Error during tile download, "
                            + this.getName() + " (" + zoom + ", "
                            + x + ", " + y + ")", t);
            if (error != null)
                error[0] = t;
        }

        return retval;
    }

    @Override
    public Envelope getBounds()
    {
        return this.bounds;
    }

    @Override
    public void dispose()
    {
        // XXX -
    }

    @Override
    public void checkConnectivity()
    {
    }

    @Override
    public void clearAuthFailed()
    {
    }

    @Override
    public <T> T getControl(Class<T> controlClazz)
    {
        if (controlClazz.isAssignableFrom(this.getClass()))
            return controlClazz.cast(this);
        if(File.class.equals(controlClazz))
            return (T) this.configFile;
        if(StreamingTiles.class.equals(controlClazz))
            return (T) this.config;
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls)
    {
        controls.add(this);
    }

    @Override
    public void cache(CacheRequest request, CacheRequestListener listener)
    {
        if(!this.config.downloadable) {
            if(listener != null)
                listener.onRequestError(
                        new UnsupportedOperationException("Source is not downloadable"),
                        "Source is not downloadable",
                        true);
            return;
        }
        String preferredProvider = OSMDroidTileContainer.SPI.getName();
        if (request != null && request.preferredContainerProvider != null)
            preferredProvider = request.preferredContainerProvider;

        TileContainer sink;

        if (request == null)
        {
            Log.e(TAG, "Unable to create tile container for cache request == null");
            if (listener != null)
                listener.onRequestError(null, "Unable to create tile container for cache request == null", true);
            return;
        }

        sink = TileContainerFactory.openOrCreateCompatibleContainer(request.cacheFile.getAbsolutePath(), this, preferredProvider);
        if (sink == null)
        {
            Log.e(TAG, "Unable to create tile container for cache request");
            if (listener != null)
                listener.onRequestError(null, "Unable to create tile container for cache request", true);
            return;
        }
        if(sink instanceof Controls) {
            ContainerInitializationControl ctrl = ((Controls)sink).getControl(ContainerInitializationControl.class);
            if(ctrl != null) {
                Map<String, Object> metadata = new HashMap<>();
                if (this.config.content != null)
                    metadata.put("content", this.config.content);
                if (this.config.metadata != null)
                    metadata.putAll(this.config.metadata);
                if(!metadata.isEmpty())
                    ctrl.setMetadata(metadata);
            }
        }
        try
        {
            TileScraper scraper = new TileScraper(this, sink, request, listener);
            scraper.run();
        } finally
        {
            sink.dispose();
        }
    }

    @Override
    public int estimateTileCount(CacheRequest request)
    {
        return TileScraper.estimateTileCount(this, request);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> m = new HashMap<>();
        if(this.config.metadata != null)
            m.putAll(this.config.metadata);
        if(this.config.content != null)
            m.put("content", this.config.content);
        return m;
    }
}
