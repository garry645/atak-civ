package com.atakmap.map.layer.raster.mosaic.opengl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLResolvableMapRenderable;
import com.atakmap.spatial.SpatialCalculator;

class MosaicPendingData
{

    /**
     * holds the results of the query
     */
    public final Set<MosaicDatabase2.Frame> frames;
    /**
     * calculates occlusion
     */
    public final SpatialCalculator spatialCalc;
    /**
     * list of currently rendered frame URIs
     */
    public final Set<String> loaded;

    private final Set<MosaicDatabase2.Frame> preloading = new HashSet<>();
    int preloadsRequested;
    int preloadsWaiting;

    public MosaicDatabase2 database;

    final ExecutorService preloadInitPool;

    public MosaicPendingData()
    {
        this.frames = new HashSet<>();
        this.spatialCalc = new SpatialCalculator.Builder().inMemory().build();
        this.loaded = new HashSet<String>();
        this.database = null;

        this.preloadInitPool = new ThreadPoolExecutor(4, 4, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        ((ThreadPoolExecutor)this.preloadInitPool).allowCoreThreadTimeOut(true);
    }

    /**
     * Preloads the specified frame into {@code mosaic.visibleFrames} in a background thread
     *
     * @param mosaic
     * @param frame
     */
    void preload(final GLMosaicMapLayer mosaic, final MosaicDatabase2.Frame frame) {
        // instantiate (but do not initialize) renderable for frame that is not currently loaded
        if (loaded.contains(mosaic.resolvePath(frame.path)))
            return;
        preloadsRequested++;
        synchronized (preloading) {
            preloading.add(frame);
        }
        preloadInitPool.submit(new Runnable() {
            public void run() {
                try {
                    GLResolvableMapRenderable renderable = mosaic.createRootNode(frame);
                    if (renderable != null)
                        mosaic.visibleFrames.put(frame, renderable);
                } finally {
                    synchronized (preloading) {
                        preloading.remove(frame);
                        preloading.notify();
                    }
                }
            }
        });
    }

    void preload(final GLMosaicMapLayer2 mosaic, final MosaicDatabase2.Frame frame) {
        // instantiate (but do not initialize) renderable for frame that is not currently loaded
        if (loaded.contains(mosaic.resolvePath(frame.path)))
            return;
        preloadsRequested++;
        synchronized (preloading) {
            preloading.add(frame);
        }
        preloadInitPool.submit(new Runnable() {
            public void run() {
                try {
                    GLMapRenderable2 renderable = mosaic.createRootNode(frame);
                    if (renderable != null)
                        mosaic.visibleFrames.put(frame, renderable);
                } finally {
                    synchronized (preloading) {
                        preloading.remove(frame);
                        preloading.notify();
                    }
                }
            }
        });
    }

    int waitForPreload() {
        synchronized (preloading) {
            preloadsWaiting = preloading.size();
        }
        while(true) {
            synchronized (preloading) {
                if(preloading.isEmpty())
                    break;
                try {
                    preloading.wait();
                } catch(InterruptedException ignored) {}
            }
        }
        int i = preloadsRequested;
        preloadsRequested = 0;
        return i;
    }
}
