package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.EnvelopeFilter;
import com.atakmap.map.layer.feature.GeometryFilter;
import com.atakmap.map.layer.feature.control.SpatialFilterControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.TileCacheControl;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.spatial.GeometryTransformer;
import com.atakmap.util.ReferenceCount;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TileProxy implements TileClient
{
    final ReferenceCount<TileClient> client;
    final ReferenceCount<TileContainer> cache;
    final ExecutorService clientExecutor;
    final TileCacheControl.OnTileUpdateListener[] listener;
    final Projection proj;
    long expiry;
    boolean offlineOnly = false;
    long refreshInterval = 0L;

    int priority = 0;

    /**
     * The download queue. If no prioritization is specified, tasks are dispatched immediately by
     * the executor and the queue is not filled.
     *
     * <P>All operations on this field should be synchronized on {@link #queued}
     */
    final ArrayList<TileFetchTask> downloadQueue = new ArrayList<>();
    /**
     * The indices of all queued tiles, per
     * {@link OSMUtils#getOSMDroidSQLiteIndex(long, long, long)}
     */
    final Map<Long, TileFetchTask> queued = new HashMap<>();

    final PrioritizerImpl prioritizer = new PrioritizerImpl();

    final TileClientControl clientControl = new TileClientControl() {
        @Override
        public void setOfflineOnlyMode(boolean offlineOnly) {
            if(offlineOnly != TileProxy.this.offlineOnly) {
                TileProxy.this.offlineOnly = offlineOnly;
                if(offlineOnly) {
                    // going offline, clear download queue
                    synchronized (queued)
                    {
                        for (TileFetchTask task : downloadQueue)
                        {
                            task.cache.dereference();
                            task.client.dereference();
                        }
                        downloadQueue.clear();
                        queued.clear();
                    }
                }
            }
        }

        @Override
        public boolean isOfflineOnlyMode() {
            return offlineOnly;
        }

        @Override
        public void refreshCache() {
            prioritizer.expireTiles(System.currentTimeMillis());
        }

        @Override
        public void setCacheAutoRefreshInterval(long milliseconds) {
            refreshInterval = milliseconds;
        }

        @Override
        public long getCacheAutoRefreshInterval() {
            return refreshInterval;
        }
    };

    EnvelopeFilter mbbFilter;
    GeometryFilter geomFilter;

    final SpatialFilterControl spatialFilterControl = new SpatialFilterControl() {

        @Override
        public void setFilter(EnvelopeFilter filter) {
            mbbFilter = filter;
            geomFilter = null;
        }

        @Override
        public void setFilter(GeometryFilter filter) {
            mbbFilter = null;
            geomFilter = filter;
        }
    };

    public TileProxy(TileClient client, TileContainer cache)
    {
        this(client,
                cache,
                System.currentTimeMillis() - (24L * 60L * 60L * 1000L), // 24 hours old
                Executors.newFixedThreadPool(3, new NamedThreadFactory("TileProxy[" + client.getName() + "]")));
    }

    TileProxy(TileClient client, TileContainer cache, long expiry, ExecutorService clientExecutor)
    {
        if (client == null)
            throw new IllegalArgumentException();
        if (cache == null)
            throw new IllegalArgumentException();
        if (clientExecutor == null)
            throw new IllegalArgumentException();

        this.client = new SharedTileMatrix<>(client);
        this.cache = new SharedTileMatrix<>(cache);
        this.expiry = expiry;
        this.clientExecutor = clientExecutor;
        this.listener = new TileCacheControl.OnTileUpdateListener[1];

        this.proj = ProjectionFactory.getProjection(client.getSRID());
    }

    /**
     * Requests that the client abort the download of the specified tile
     *
     * @param zoom  The zoom level
     * @param x     The tile column
     * @param y     The tile row
     */
    public void abortTile(int zoom, int x, int y)
    {
        synchronized (queued)
        {
            // XXX - this may need to be revisited if the queue is reimplemented as a circular
            //       buffer to free up a slot on abort
            final TileFetchTask task = queued.remove(OSMUtils.getOSMDroidSQLiteIndex(zoom,x, y));
            if(task != null) {
                task.aborted = true;
            }
        }
    }

    private void downloadTile(int zoom, int x, int y)
    {
        final Envelope bounds = TileMatrix.Util.getTileBounds(client.value, zoom, x, y);
        final EnvelopeFilter f = mbbFilter;
        // skip download if offline-only mode or if the tile doesn't pass the spatial filter
        if(offlineOnly || (f != null && !f.accept(GeometryTransformer.transform(bounds, proj, EquirectangularMapProjection.INSTANCE)))) {
            return;
        }
        synchronized (queued)
        {
            final long key = OSMUtils.getOSMDroidSQLiteIndex(zoom, x, y);
            if(queued.containsKey(key))
                return;
            final TileFetchTask task = new TileFetchTask(++priority, queued, client, cache, x, y, zoom, bounds, expiry, listener);
            queued.put(key, task);
            if(prioritizer.isPrioritized)
            {
                downloadQueue.add(task);
                clientExecutor.execute(new Downloader());
            } else {
                clientExecutor.execute(task);
            }
        }
    }

    @Override
    public void clearAuthFailed()
    {
        client.value.clearAuthFailed();
    }

    @Override
    public void checkConnectivity()
    {
        client.value.checkConnectivity();
    }

    @Override
    public void cache(CacheRequest request, CacheRequestListener listener)
    {
        client.value.cache(request, listener);
    }

    @Override
    public int estimateTileCount(CacheRequest request)
    {
        return client.value.estimateTileCount(request);
    }

    @Override
    public <T> T getControl(Class<T> controlClazz)
    {
        if (TileCacheControl.class.isAssignableFrom(controlClazz))
            return (T) prioritizer;
        if (TileClientControl.class.isAssignableFrom(controlClazz))
            return (T) clientControl;
        if(TileContainer.class.equals(controlClazz))
            return (T)  cache.value;
        if(SpatialFilterControl.class.equals(controlClazz))
            return (T)  spatialFilterControl;
        return client.value.getControl(controlClazz);
    }

    @Override
    public void getControls(Collection<Object> controls)
    {
        controls.add(prioritizer);
        controls.add(cache.value);
        controls.add(clientControl);
        client.value.getControls(controls);
    }

    @Override
    public String getName()
    {
        return client.value.getName();
    }

    @Override
    public int getSRID()
    {
        return client.value.getSRID();
    }

    @Override
    public ZoomLevel[] getZoomLevel()
    {
        return client.value.getZoomLevel();
    }

    @Override
    public double getOriginX()
    {
        return client.value.getOriginX();
    }

    @Override
    public double getOriginY()
    {
        return client.value.getOriginY();
    }

    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
    {
        final Bitmap retval = cache.value.getTile(zoom, x, y, error);
        downloadTile(zoom, x, y);
        return retval;
    }

    @Override
    public byte[] getTileData(int zoom, int x, int y, Throwable[] error)
    {
        final byte[] retval = cache.value.getTileData(zoom, x, y, error);
        downloadTile(zoom, x, y);
        return retval;
    }

    @Override
    public Envelope getBounds()
    {
        return client.value.getBounds();
    }

    @Override
    public void dispose()
    {
        synchronized (listener)
        {
            listener[0] = null;
        }

        clientExecutor.shutdownNow();

        synchronized (queued)
        {
            for (TileFetchTask task : downloadQueue)
            {
                task.cache.dereference();
                task.client.dereference();
            }
            downloadQueue.clear();
            queued.clear();
        }

        client.dereference();
        cache.dereference();
    }

    final static class SharedTileMatrix<T extends TileMatrix> extends ReferenceCount<T>
    {
        public SharedTileMatrix(T value)
        {
            super(value, true);
        }

        @Override
        protected void onDereferenced()
        {
            super.onDereferenced();
            value.dispose();
        }
    }

    final class Downloader implements Runnable
    {
        @Override
        public void run()
        {
            TileFetchTask task;
            int queueSize;
            synchronized (queued)
            {
                if (downloadQueue.isEmpty())
                    return;
                task = downloadQueue.remove(downloadQueue.size() - 1);
                queueSize = downloadQueue.size();
            }

            task.run();
        }
    }

    final class TileFetchTask implements Runnable
    {
        final ReferenceCount<TileClient> client;
        final ReferenceCount<TileContainer> cache;
        final int x;
        final int y;
        final int z;
        final long expiry;
        final TileCacheControl.OnTileUpdateListener[] callback;
        final Envelope bounds;
        final double centroidX;
        final double centroidY;
        final double centroidZ;
        final double radius;
        boolean aborted;

        int priority;

        final Map<Long, TileFetchTask> queued;

        TileFetchTask(int priority, Map<Long, TileFetchTask> queued, ReferenceCount<TileClient> client, ReferenceCount<TileContainer> cache, int x, int y, int z, Envelope bounds, long expiry, TileCacheControl.OnTileUpdateListener[] l)
        {
            this.priority = priority;
            this.queued = queued;
            this.client = client;
            this.cache = cache;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiry = expiry;
            this.callback = l;
            this.aborted = false;

            this.client.reference();
            this.cache.reference();

            this.bounds = bounds;
            centroidX = (bounds.minX + bounds.maxX) / 2d;
            centroidY = (bounds.minY + bounds.maxY) / 2d;
            centroidZ = (bounds.minZ + bounds.maxZ) / 2d;
            radius = MathUtils.distance(centroidX, centroidY, centroidZ, bounds.maxX, bounds.maxY, bounds.maxZ);
        }

        @Override
        public void run()
        {
            try
            {
                final EnvelopeFilter filter = TileProxy.this.mbbFilter;
                aborted |= (filter != null) && !filter.accept(GeometryTransformer.transform(bounds, proj, EquirectangularMapProjection.INSTANCE));
                if(this.aborted)
                    return;

                final long expiration = cache.value.hasTileExpirationMetadata() ?
                        cache.value.getTileExpiration(z, x, y) :
                        (cache.value.getTileData(z, x, y, null) != null ? expiry : -1L);

                // if the tile is considered expired, download
                if (expiration < expiry)
                {
                    byte[] data = client.value.getTileData(z, x, y, null);
                    if (data != null)
                    {
                        cache.value.setTile(z, x, y, data, System.currentTimeMillis());
                        // signal update
                        synchronized (callback)
                        {
                            if (callback[0] != null)
                                callback[0].onTileUpdated(z, x, y);
                        }
                    }
                }
            } catch (Throwable t)
            {
            } finally
            {
                client.dereference();
                cache.dereference();

                synchronized (queued) {
                    queued.remove(OSMUtils.getOSMDroidSQLiteIndex(z, x, y));
                }
            }
        }
    }

    final class PrioritizerImpl implements TileCacheControl, Comparator<TileFetchTask>
    {

        boolean isPrioritized;
        GeoPoint p;
        PointD xyz0 = new PointD(0d, 0d, 0d);
        PointD xyz1 = new PointD(0d, 0d, 0d);
        PointD xyz2 = new PointD(0d, 0d, 0d);

        @Override
        public int compare(TileFetchTask a, TileFetchTask b)
        {
            final double da = MathUtils.distance(a.centroidX, a.centroidY, a.centroidZ, xyz0.x, xyz0.y, xyz0.z);
            final double db = MathUtils.distance(b.centroidX, b.centroidY, b.centroidZ, xyz0.x, xyz0.y, xyz0.z);
            if (da <= a.radius && db <= b.radius)
                return b.z - a.z;
            else if (da <= a.radius)
                return 1;
            else if (db < b.radius)
                return -1;
                // both radii are greater
            else if ((da - a.radius) < (db - b.radius))
                return 1;
            else if ((da - a.radius) > (db - b.radius))
                return -1;
            else if (a.z < b.z)
                return 1;
            else if (a.z > b.z)
                return -1;
            else
                return a.priority - b.priority;
        }

        @Override
        public void prioritize(GeoPoint p)
        {
            this.p = (p != null) ? new GeoPoint(p) : null;
            if (this.p != null)
            {
                proj.forward(p, xyz0);
                proj.forward(p, xyz1);
                proj.forward(p, xyz2);
            }

            synchronized (queued)
            {
                this.isPrioritized = (p != null);
                if(p != null)
                    Collections.sort(downloadQueue, this);
            }
        }

        @Override
        public void abort(int level, int x, int y)
        {
            abortTile(level, x, y);
        }

        @Override
        public boolean isQueued(int level, int x, int y)
        {
            synchronized (queued)
            {
                return queued.containsKey(OSMUtils.getOSMDroidSQLiteIndex(level, x, y));
            }
        }

        @Override
        public void setOnTileUpdateListener(OnTileUpdateListener l)
        {
            synchronized (listener)
            {
                listener[0] = l;
            }
        }

        @Override
        public void expireTiles(long expiry)
        {
            TileProxy.this.expiry = expiry;
        }
    }
}
