package com.atakmap.map.layer.raster.tilereader.opengl;

import android.util.Pair;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.interop.Interop;
import com.atakmap.map.RenderContext;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.EnvelopeFilter;
import com.atakmap.map.layer.feature.control.SpatialFilterControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetProjection2;
import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.map.layer.raster.controls.ImageryRelativeScaleControl;
import com.atakmap.map.layer.raster.controls.TileCacheControl;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.opengl.GLDiagnostics;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;
import com.atakmap.math.PointI;
import com.atakmap.math.RectD;
import com.atakmap.math.Rectangle;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLTextureCache;
import com.atakmap.opengl.Shader;
import com.atakmap.util.ConfigOptions;
import com.atakmap.util.ReferenceCount;
import com.atakmap.util.Releasable;

import java.util.*;

import gov.tak.api.util.Disposable;

class NodeCore implements Disposable, TileCacheControl.OnTileUpdateListener
{
    private final static double POLE_LATITUDE_LIMIT_EPISLON = 0.00001d;
    private final static Interop<Matrix> Matrix_interop = Interop.findInterop(Matrix.class);

    private final static Map<RenderContext, ReferenceCount<NodeContextResources>> resourcesReferences = new IdentityHashMap<>();

    final NodeInitializer init;
    Shader shader;
    final RenderState renderState = new RenderState();
    NodeInitializer.Result initResult;

    public final TileReader tileReader;
    /**
     * Projects between the image coordinate space and WGS84.
     */
    public final DatasetProjection2 imprecise;
    /**
     * Projects between the image coordinate space and WGS84.
     */
    public final DatasetProjection2 precise;

    public final int srid;
    public com.atakmap.map.layer.raster.tilereader.opengl.VertexResolver vertexResolver;
    public GLTextureCache textureCache;

    public final String type;

    public final String uri;

    public final boolean textureBorrowEnabled;

    public final boolean textureCopyEnabled;

    /**
     * local GSD for dataset
     */
    public final double gsd;

    /* corner coordinates of dataset NOT tile */

    /**
     * upper-left corner for dataset
     */
    public final GeoPoint upperLeft;
    /**
     * upper-right corner for dataset
     */
    public final GeoPoint upperRight;
    /**
     * lower-right corner for dataset
     */
    public final GeoPoint lowerRight;
    /**
     * lower-left corner for dataset
     */
    public final GeoPoint lowerLeft;

    /**
     * longitudinal unwrapping for datasets which cross the IDL
     */
    public final double unwrap;

    /**
     * minification filter to be applied to texture
     */
    public int minFilter;
    /**
     * magnification filter to be applied to texture
     */
    public int magFilter;

    // modulation color components
    public int color;
    public float colorR;
    public float colorG;
    public float colorB;
    public float colorA;

    public boolean debugDrawEnabled;
    public final NodeOptions options;
    public final LinkedList<Releasable> releasables;
    public boolean loadingTextureEnabled;

    public boolean disposed;

    public RectD[] drawROI;
    public int drawPumpHemi;
    public int drawPumpLevel;

    public boolean progressiveLoading;

    public final long fadeTimerLimit;

    public boolean versionCheckEnabled = true;

    int tilesThisFrame;

    Matrix xproj;
    Matrix mvp;
    long mvpPtr;

    final NodeContextResources resources;
    private final ReferenceCount<NodeContextResources> resourcesRef;

    TileReader.AsynchronousIO asyncio;

    boolean suspended;
    int stateMask;

    RenderContext context;

    GLDiagnostics diagnostics = new GLDiagnostics();
    boolean diagnosticsEnabled = false;

    SurfaceRendererControl surfaceControl;
    ImageryRelativeScaleControl scaleControl;

    TileCacheControl cacheControl;
    TileClientControl clientControl;
    SurfaceBoundsFilter spatialFilter;

    private Set<PointI> updatedTilesWrite = new HashSet<>();
    Set<PointI> updatedTiles = new HashSet<>();

    ArrayList<Pair<GLQuadTileNode4.RequestRegion, Collection<GLMapRenderable2>>> requestRegions = new ArrayList<>();

    NodeCore(RenderContext ctx,
             String type,
             NodeInitializer init,
             NodeInitializer.Result result,
             int srid,
             double gsdHint,
             NodeOptions opts)
    {

        if (result.reader == null || result.imprecise == null)
            throw new NullPointerException();

        this.context = ctx;
        this.type = type;
        this.init = init;
        this.initResult = result;
        this.tileReader = this.initResult.reader;
        this.imprecise = this.initResult.imprecise;
        this.precise = this.initResult.precise;
        this.srid = srid;
        this.uri = this.tileReader.getUri();

        this.debugDrawEnabled = (ConfigOptions.getOption("imagery.debug-draw-enabled", 0) != 0);
        this.fadeTimerLimit = ConfigOptions.getOption("glquadtilenode2.fade-timer-limit", 0L);

        this.options = new NodeOptions();
        if(opts != null) {
            this.options.childTextureCopyResolvesParent = opts.childTextureCopyResolvesParent;
            this.options.adaptiveTileLod = opts.adaptiveTileLod;
            this.options.levelTransitionAdjustment = opts.levelTransitionAdjustment;
            this.options.progressiveLoad = opts.progressiveLoad;
            this.options.textureBorrowEnabled = opts.textureBorrowEnabled;
            this.options.textureCache = opts.textureCache;
            this.options.textureCopyEnabled = opts.textureCopyEnabled;
        }
        this.options.progressiveLoad &= this.tileReader.isMultiResolution();
        if(this.tileReader.isMultiResolution())
            this.options.levelTransitionAdjustment = Math.max(1d, this.options.levelTransitionAdjustment);

        this.color = 0xFFFFFFFF;
        this.colorR = 1f;
        this.colorG = 1f;
        this.colorB = 1f;
        this.colorA = 1f;

        this.releasables = new LinkedList<Releasable>();

        this.loadingTextureEnabled = false;

        this.textureBorrowEnabled = (ConfigOptions.getOption("imagery.texture-borrow", 1) != 0);
        this.textureCopyEnabled = (ConfigOptions.getOption("imagery.texture-copy", 1) != 0);
        this.textureCache = this.options.textureCache;

        this.upperLeft = GeoPoint.createMutable();
        this.imprecise.imageToGround(new PointD(0, 0), this.upperLeft);
        this.upperRight = GeoPoint.createMutable();
        this.imprecise.imageToGround(new PointD(this.tileReader.getWidth() - 1, 0), this.upperRight);
        this.lowerRight = GeoPoint.createMutable();
        this.imprecise.imageToGround(new PointD(this.tileReader.getWidth() - 1,
                this.tileReader.getHeight() - 1), this.lowerRight);
        this.lowerLeft = GeoPoint.createMutable();
        this.imprecise.imageToGround(new PointD(0, this.tileReader.getHeight() - 1), this.lowerLeft);

        // if dataset bounds cross poles (e.g. Google "Flat Projection"
        // tile server), clip the bounds inset a very small amount from the
        // poles
        final double minLat = MathUtils.min(this.upperLeft.getLatitude(), this.upperRight.getLatitude(), this.lowerRight.getLatitude());
        final double maxLat = MathUtils.max(this.upperLeft.getLatitude(), this.upperRight.getLatitude(), this.lowerRight.getLatitude());

        if (minLat < -90d || maxLat > 90d)
        {
            final double minLatLimit = -90d + POLE_LATITUDE_LIMIT_EPISLON;
            final double maxLatLimit = 90d - POLE_LATITUDE_LIMIT_EPISLON;
            this.upperLeft.set(MathUtils.clamp(this.upperLeft.getLatitude(), minLatLimit, maxLatLimit), this.upperLeft.getLongitude());
            this.upperRight.set(MathUtils.clamp(this.upperRight.getLatitude(), minLatLimit, maxLatLimit), this.upperRight.getLongitude());
            this.lowerRight.set(MathUtils.clamp(this.lowerRight.getLatitude(), minLatLimit, maxLatLimit), this.lowerRight.getLongitude());
            this.lowerLeft.set(MathUtils.clamp(this.lowerLeft.getLatitude(), minLatLimit, maxLatLimit), this.lowerLeft.getLongitude());
        }

        float minLng = (float) MathUtils.min(this.upperLeft.getLongitude(),
                this.upperRight.getLongitude(),
                this.lowerRight.getLongitude(),
                this.lowerLeft.getLongitude());
        float maxLng = (float) MathUtils.max(this.upperLeft.getLongitude(),
                this.upperRight.getLongitude(),
                this.lowerRight.getLongitude(),
                this.lowerLeft.getLongitude());

        if (minLng < -180 && maxLng > -180)
            this.unwrap = 360;
        else if (maxLng > 180 && minLng < 180)
            this.unwrap = -360;
        else
            this.unwrap = 0;

        if (!Double.isNaN(gsdHint))
        {
            this.gsd = gsdHint;
        } else
        {
            this.gsd = DatasetDescriptor.computeGSD(this.tileReader.getWidth(),
                    this.tileReader.getHeight(),
                    this.upperLeft,
                    this.upperRight,
                    this.lowerRight,
                    this.lowerLeft);
        }

        this.minFilter = GLES20FixedPipeline.GL_LINEAR;
        this.magFilter = GLES20FixedPipeline.GL_LINEAR;

        if (this.imprecise != null)
            this.releasables.add(this.imprecise);
        if (this.precise != null)
            this.releasables.add(this.precise);

        this.drawROI = new RectD[] {new RectD(), new RectD()};
        this.drawPumpHemi = -1;

        this.progressiveLoading = false;

        this.disposed = false;

        this.xproj = Matrix.getIdentity();
        this.mvp = Matrix.getIdentity();
        this.mvpPtr = Matrix_interop.getPointer(this.mvp);

        if (this.tileReader != null)
        {
            this.asyncio = this.tileReader.getControl(TileReader.AsynchronousIO.class);
            this.cacheControl = this.tileReader.getControl(TileCacheControl.class);
            if (this.cacheControl != null)
                this.cacheControl.setOnTileUpdateListener(this);
            this.clientControl = this.tileReader.getControl(TileClientControl.class);
            this.spatialFilter = new SurfaceBoundsFilter();
            final SpatialFilterControl spatialFilterControl = this.tileReader.getControl(SpatialFilterControl.class);
            if(spatialFilterControl != null)
                spatialFilterControl.setFilter(this.spatialFilter);
        }

        synchronized (resourcesReferences)
        {
            ReferenceCount<NodeContextResources> ref = resourcesReferences.get(ctx);
            if (ref == null)
            {
                ref = new ReferenceCount<NodeContextResources>(new NodeContextResources(), false)
                {
                    @Override
                    protected void onDereferenced()
                    {
                        Unsafe.free(value.coordStreamBuffer);
                        super.onDereferenced();
                    }
                };
                resourcesReferences.put(ctx, ref);
            }
            this.resourcesRef = ref;
            this.resources = this.resourcesRef.reference();
        }
    }

    @Override
    public void onTileUpdated(int level, int x, int y)
    {
        synchronized (updatedTilesWrite)
        {
            updatedTilesWrite.add(new PointI(x, y, level));
        }
        if (this.surfaceControl != null)
        {
            this.surfaceControl.markDirty(getNodeBounds(level, x, y), false);
        }
    }

    void refreshUpdateList()
    {
        synchronized (updatedTilesWrite)
        {
            updatedTiles.clear();
            updatedTiles.addAll(updatedTilesWrite);
            updatedTilesWrite.clear();
        }
    }

    @Override
    public synchronized void dispose()
    {
        if (this.disposed)
            return;
        this.init.dispose(this.initResult);
        synchronized (resourcesReferences)
        {
            this.resourcesRef.dereference();
            if (!this.resourcesRef.isReferenced())
                resourcesReferences.values().remove(this.resourcesRef);
        }
        if (this.cacheControl != null)
            this.cacheControl.setOnTileUpdateListener(null);

        this.disposed = true;
    }

    static NodeCore create(RenderContext ctx, ImageInfo info, TileReaderFactory.Options readerOpts, NodeOptions opts, boolean throwOnReaderFailedInit, NodeInitializer init)
    {
        NodeInitializer.Result result = init.init(info, readerOpts);
        if (result.error != null && throwOnReaderFailedInit)
            throw new RuntimeException(result.error);

        return new NodeCore(ctx,
                info.type,
                init,
                result,
                info.srid,
                info.maxGsd,
                opts);
    }

    Envelope getNodeBounds(int level, long tileColumn, long tileRow)
    {
        PointD scratchP = new PointD();
        GeoPoint scratchG = GeoPoint.createMutable();

        final long tileSrcX = tileReader.getTileSourceX(level, tileColumn);
        final long tileSrcY = tileReader.getTileSourceY(level, tileRow);
        final long tileSrcWidth = tileReader.getTileSourceWidth(level, tileColumn);
        final long tileSrcHeight = tileReader.getTileSourceHeight(level, tileRow);

        double minLat = 90;
        double maxLat = -90;
        double minLng = 180;
        double maxLng = -180;

        scratchP.x = tileSrcX;
        scratchP.y = tileSrcY;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX + tileSrcWidth;
        scratchP.y = tileSrcY;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX + tileSrcWidth;
        scratchP.y = tileSrcY + tileSrcHeight;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        scratchP.x = tileSrcX;
        scratchP.y = tileSrcY + tileSrcHeight;
        imprecise.imageToGround(scratchP, scratchG);
        if (scratchG.getLatitude() < minLat)
            minLat = scratchG.getLatitude();
        if (scratchG.getLatitude() > maxLat)
            maxLat = scratchG.getLatitude();
        if (scratchG.getLongitude() < minLng)
            minLng = scratchG.getLongitude();
        if (scratchG.getLongitude() > maxLng)
            maxLng = scratchG.getLongitude();

        return new Envelope(minLng, minLat, 0d, maxLng, maxLat, 0d);
    }

    final static class SurfaceBoundsFilter implements EnvelopeFilter {

        Collection<Envelope> surfaceBounds = Collections.emptySet();
        int surfaceBoundsVersion = -1;

        void update(GLMapView view, SurfaceRendererControl surfaceControl) {
            if(view.currentScene.drawVersion != surfaceBoundsVersion)
                surfaceBounds = new ArrayList<>(surfaceControl.getSurfaceBounds());
        }

        @Override
        public boolean accept(Envelope bounds) {
            if(surfaceBounds.isEmpty())
                return true;
            final Collection<Envelope> test = surfaceBounds;
            for(Envelope e : test) {
                if(Rectangle.intersects(e.minX, e.minY, e.maxX, e.maxY, bounds.minX, bounds.minY, bounds.maxX, bounds.maxY, false)) {
                    return true;
                }
            }
            return false;
        }
    }
}