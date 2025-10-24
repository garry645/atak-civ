package com.atakmap.map.layer.feature.vectortiles;

import android.graphics.Bitmap;
import android.util.Pair;

import com.atakmap.interop.Interop;
import com.atakmap.lang.Objects;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.RenderContext;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.control.ColorControl;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.EnvelopeFilter;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.control.SpatialFilterControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.controls.TileCacheControl;
import com.atakmap.map.layer.raster.controls.TileClientControl;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.opengl.GLMapLayer3;
import com.atakmap.map.layer.raster.opengl.GLMapLayerSpi3;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileEncodeException;
import com.atakmap.map.layer.raster.tilematrix.TileGrid;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.layer.raster.tilereader.opengl.GLTiledMapLayer2;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.math.Rectangle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public final class GLVectorTiles implements GLMapLayer3, GLMapRenderable2
{
    private final static Map<String, Collection<Schema>> styleSchemas = new HashMap<>();
    static {
        styleSchemas.put("omt", Arrays.asList(Schema.OMT));
        styleSchemas.put("rbt", Arrays.asList(Schema.RBT_CULTURAL, Schema.RBT_PHYSICAL));
    }
    private final static Interop<gov.tak.api.engine.map.RenderContext> RenderContext_interop = Interop.findInterop(gov.tak.api.engine.map.RenderContext.class);
    private final static Map<MapRenderer, DeduplicateContext> deduplicateContexts = new WeakHashMap<>();

    public final static GLMapLayerSpi3 SPI = new GLMapLayerSpi3() {
        @Override
        public int getPriority() {
            return GLTiledMapLayer2.SPI.getPriority()+1;
        }

        @Override
        public GLMapLayer3 create(Pair<MapRenderer, DatasetDescriptor> object) {
            final String uri = object.second.getUri();
            final String offlineCachePath = object.second.getExtraData("offlineCache");
            final Boolean overlay = object.second.getLocalData("overlay", Boolean.class);
            final boolean isOverlay = (overlay != null && overlay);
            final RenderContext ctx = com.atakmap.map.LegacyAdapters.getRenderContext(object.first);
            GLMapRenderable2 gltiles = null;
            TileMatrix tiles = null;
            long[] ptr = new long[1];
            Schema schema = null;
            do {
                TileClient client = TileClientFactory.create(uri, offlineCachePath, null);
                if(isCompatible(client)) {
                    schema = getSchema(client);
                    String style = null;
                    if(schema != null) {
                        for (Map.Entry<String, Collection<Schema>> entry : styleSchemas.entrySet()) {
                            for (Schema s : entry.getValue()) {
                                if (s.matches(schema, true)) {
                                    style = entry.getKey();
                                    break;
                                }
                            }
                            if (style != null)
                                break;
                        }
                    }
                    // XXX -
                    if(Objects.equals(style, "rbt"))
                        client = new RBTTileClient(client);
                    gltiles = createClientImpl(RenderContext_interop.getPointer(ctx), client, isOverlay, !Objects.equals(style, "omt"), ptr);
                    final TileCacheControl ctrl = client.getControl(TileCacheControl.class);
                    if(ctrl != null && ptr[0] != 0L) {
                        ctrl.setOnTileUpdateListener(new TileUpdateForwarder(gltiles, ptr[0]));
                    }
                    tiles = client;
                    break;
                } else if (client != null) {
                     client.dispose();
                }
                TileContainer container = TileContainerFactory.open(uri, true, null);
                if(isCompatible(container)) {
                    schema = getSchema(container);
                    String style = null;
                    if(schema != null) {
                        for(Map.Entry<String, Collection<Schema>> entry : styleSchemas.entrySet()) {
                            for(Schema s : entry.getValue()) {
                                if (s.matches(schema, true)) {
                                    style = entry.getKey();
                                    break;
                                }
                            }
                            if(style != null)
                                break;
                        }
                    }
                    // XXX -
                    if(Objects.equals(style, "rbt") || isRBT(schema))
                        container = new RBTTileContainer(container);
                    gltiles = createContainerImpl(RenderContext_interop.getPointer(ctx), container, isOverlay, !Objects.equals(style, "omt"), ptr);
                    tiles = container;
                    break;
                } else if (container != null) {
                     container.dispose();
                }
            } while(false);
            if(gltiles == null)
                return null;

            DeduplicateContext dedupe;
            synchronized(deduplicateContexts) {
                dedupe = deduplicateContexts.get(object.first);
                if(dedupe == null)
                    deduplicateContexts.put(object.first, dedupe=new DeduplicateContext());
            }
            // restrict hit-testability for single layer datasets only at this time for purposes of
            // backwards compatibility
            return new GLVectorTiles(object.second, uri, gltiles, tiles, ptr[0], (schema != null && schema.schema.size() == 1), isOverlay, dedupe);
        }
    };

    final DatasetDescriptor desc;
    final String uri;
    final GLMapRenderable2 impl;
    final TileMatrix tiles;
    final long pointer;
    boolean lastOffline;
    final TileClientControl clientControl;
    final ColorControl colorControl = new ColorControl() {
        @Override
        public void setColor(int color) {
            GLVectorTiles.setColor(pointer, color);
        }

        @Override
        public int getColor() {
            return GLVectorTiles.getColor(pointer);
        }
    };

    final HitTestControl hitTest = new HitTestControl() {
        @Override
        public void hitTest(MapRenderer3 renderer, HitTestQueryParameters params, Collection<HitTestResult> results) {
            if(params.resultFilter != null && !params.resultFilter.acceptClass(Feature.class))
                return;
            LinkedList<Feature> fids = new LinkedList<>();
            MapSceneModel sm = renderer.getMapSceneModel(true, renderer.getDisplayOrigin());
            GLVectorTiles.hitTest(pointer, fids, params.point.x, sm.height-params.point.y, params.geo.getLatitude(), params.geo.getLongitude(), sm.gsd, params.size, params.limit);
            for(Feature f : fids)
                results.add(new HitTestResult(f));
        }
    };

    final boolean hitTestable;

    SurfaceRendererControl[] surfaceControl;

    final SurfaceBoundsFilter surfaceBoundsFilter = new SurfaceBoundsFilter();

    boolean renderedContent;

    String tilesUrl;
    boolean isOverlay;
    final DeduplicateContext dedupe;

    GLVectorTiles(DatasetDescriptor desc, String uri, GLMapRenderable2 impl, TileMatrix tiles, long pointer, boolean hitTestable, boolean isOverlay, DeduplicateContext dedupe) {
        this.desc = desc;
        this.uri = uri;
        this.impl = impl;
        this.tiles = tiles;
        this.pointer = pointer;
        this.hitTestable = hitTestable;
        this.isOverlay = isOverlay;
        this.dedupe = dedupe;

        if(this.tiles instanceof TileClient) {
            final TileClient client = (TileClient)this.tiles;
            this.clientControl = client.getControl(TileClientControl.class);
            this.lastOffline = (this.clientControl != null) ? this.clientControl.isOfflineOnlyMode() : false;

            final SpatialFilterControl spatialFilterControl = client.getControl(SpatialFilterControl.class);
            if(spatialFilterControl != null)
                spatialFilterControl.setFilter(surfaceBoundsFilter);

            final StreamingTiles streamingTiles = client.getControl(StreamingTiles.class);
            if(streamingTiles != null)
                this.tilesUrl = streamingTiles.url;
        } else {
            this.clientControl = null;
        }
    }

    @Override
    public String getLayerUri() {
        return uri;
    }

    @Override
    public DatasetDescriptor getInfo() {
        return desc;
    }

    @Override
    public <T extends MapControl> T getControl(Class<T> clazz) {
        if(this.hitTestable && clazz.equals(HitTestControl.class))
            return (T) hitTest;
        else if(clazz.equals(ColorControl.class))
            return (T) colorControl;
        else if(this.tiles instanceof TileClient)
            return ((TileClient) this.tiles).getControl(clazz);
        else if(this.tiles instanceof Controls)
            return ((Controls)this.tiles).getControl(clazz);
        return null;
    }

    @Override
    public void draw(GLMapView view) {
        this.draw(view, GLMapView.RENDER_PASS_SURFACE);
    }

    @Override
    public void draw(GLMapView view, int renderPass) {
        if (view.currentScene.renderPump != dedupe.sceneRenderPump) {
            dedupe.sceneRenderPump = view.currentScene.renderPump;
            dedupe.renderPumpBasemapUrls.clear();
        }

        // de-duplicate streaming tiles that may be available to the user as both basemap and
        // overlay
        if (tilesUrl != null) {
            if (!isOverlay) {
                // record the basemap as rendered for this pump
                dedupe.renderPumpBasemapUrls.add(tilesUrl);
            } else if (dedupe.renderPumpBasemapUrls.contains(tilesUrl)) {
                // if the overlay has any rendered content, clear it from the map
                if (renderedContent)
                    release();
                // skip rendering basemap redundant content
                return;
            }
        }

        if(this.clientControl != null) {
            final boolean offline = this.clientControl.isOfflineOnlyMode();
            // signal a refresh when going back online
            if(!offline && (offline != this.lastOffline))
                sourceContentUpdated(this.pointer);
            this.lastOffline = offline;
        }
        if(this.surfaceControl == null)
            this.surfaceControl = new SurfaceRendererControl[] {view.getControl(SurfaceRendererControl.class)};
        if(MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
            surfaceBoundsFilter.update();
        impl.draw(view, renderPass);
        renderedContent = true;
    }

    @Override
    public void release() {
        impl.release();
        renderedContent = false;
    }

    @Override
    public int getRenderPass() {
        return impl.getRenderPass();
    }

    static boolean isCompatible(TileMatrix tiles)
    {
        if(!(tiles instanceof Controls))
            return false;
        final Controls ctrls = (Controls)tiles;
        final TilesMetadataControl config = ctrls.getControl(TilesMetadataControl.class);
        if(config == null)
            return false;
        final Map<String, Object> metadata = config.getMetadata();
        return (metadata != null) ? Objects.equals(metadata.get("content"), "vector") : false;
    }

    static Schema getSchema(TileMatrix tiles) {
        if(!(tiles instanceof Controls))
            return null;
        final Controls ctrls = (Controls)tiles;
        final TilesMetadataControl config = ctrls.getControl(TilesMetadataControl.class);
        if(config == null)
            return null;
        final Map<String, Object> metadata = config.getMetadata();
        if(metadata == null)
            return null;
        if(Objects.equals(metadata.get("styleSchema"), "omt"))
            return Schema.OMT;
        if(metadata.containsKey("btp_schema_version"))
            return Schema.RBT;
        final String json = (String)metadata.get("json");
        if(json == null)
            return null;
        try {
            JSONObject blob = new JSONObject(json);
            JSONArray vector_layers = blob.optJSONArray("vector_layers");
            if(vector_layers == null)
                return null;
            Map<String, Set<String>> schema = new HashMap<>();
            for(int i = 0; i < vector_layers.length(); i++) {
                JSONObject layer = vector_layers.optJSONObject(i);
                if(layer == null)
                    continue;
                final String id = layer.optString("id", null);
                if(id == null)
                    continue;
                final JSONObject fields = layer.optJSONObject("fields");
                if(fields == null)
                    continue;
                Set<String> layerFields = new HashSet<>();
                Iterator<String> it = fields.keys();
                while(it.hasNext())
                    layerFields.add(it.next());
                schema.put(id, layerFields);
            }

            return new Schema(schema);
        } catch(Throwable t) {
            return null;
        }
    }

    final static class TileUpdateForwarder implements TileCacheControl.OnTileUpdateListener
    {
        final long pointer;
        final GLMapRenderable2 managed;

        TileUpdateForwarder(GLMapRenderable2 managed, long pointer) {
            this.pointer = pointer;
            this.managed = managed;
        }

        @Override
        public void onTileUpdated(int level, int x, int y) {
            sourceContentUpdated(this.pointer, level, x, y);
        }
    };

    static abstract class RBTTileMatrix implements TileMatrix, Controls
    {
        final TileMatrix impl;
        final PointD origin;
        final int srid;
        final TileMatrix.ZoomLevel[] zoomLevels;

        RBTTileMatrix(TileMatrix impl) {
            this.impl = impl;

            final ZoomLevel[] implZooms = impl.getZoomLevel();

            // assume World Mercator as RBT default
            TileGrid tileGrid = TileGrid.WorldMercator;
            do {
                if(!(impl instanceof Controls))
                    break;
                final TilesMetadataControl ctrl = ((Controls) impl).getControl(TilesMetadataControl.class);
                if(ctrl == null)
                    break;
                final Map<String, Object> metadata = ctrl.getMetadata();
                if(metadata == null)
                    break;
                final Object crs = metadata.get("crs");
                if(!(crs instanceof String))
                    break;
                switch((String)crs) {
                    case "EPSG:3395" :
                        tileGrid = TileGrid.WorldMercator;
                        break;
                    case "EPSG:3857" :
                        tileGrid = TileGrid.WebMercator;
                        break;
                    case "EPSG:4326" :
                        tileGrid = TileGrid.WGS84;
                        break;
                    default :
                        break;
                }
            } while(false);

            ZoomLevel[] allLevels = TileMatrix.Util.createQuadtree(tileGrid.zoomLevels[0],  implZooms[implZooms.length-1].level+1);
            this.zoomLevels = new ZoomLevel[implZooms.length];
            for(int i = 0; i < implZooms.length; i++)
                this.zoomLevels[i] = allLevels[implZooms[i].level];
            this.origin = tileGrid.origin;
            this.srid = tileGrid.srid;
        }

        @Override
        public String getName() {
            return impl.getName();
        }

        @Override
        public int getSRID() {
            return srid;
        }

        @Override
        public ZoomLevel[] getZoomLevel() {
            return zoomLevels;
        }

        @Override
        public double getOriginX() {
            return origin.x;
        }

        @Override
        public double getOriginY() {
            return origin.y;
        }

        @Override
        public Bitmap getTile(int zoom, int x, int y, Throwable[] error) {
            return impl.getTile(zoom, x, y, error);
        }

        @Override
        public byte[] getTileData(int zoom, int x, int y, Throwable[] error) {
            return impl.getTileData(zoom, x, y, error);
        }

        @Override
        public Envelope getBounds() {
            return impl.getBounds();
        }

        @Override
        public void dispose() {
            impl.dispose();
        }

        @Override
        public <T> T getControl(Class<T> controlClazz) {
            return (impl instanceof Controls) ? ((Controls) impl).getControl(controlClazz) : null;
        }

        @Override
        public void getControls(Collection<Object> controls) {
            if (impl instanceof Controls)
                ((Controls) impl).getControls(controls);
        }
    }

    final static class RBTTileContainer extends RBTTileMatrix implements TileContainer
    {
        final TileContainer impl;

        RBTTileContainer(TileContainer impl) {
            super(impl);
            this.impl = impl;
        }

        @Override
        public boolean isReadOnly() {
            return impl.isReadOnly();
        }

        @Override
        public void setTile(int level, int x, int y, byte[] data, long expiration) {
            impl.setTile(level, x, y, data, expiration);
        }

        @Override
        public void setTile(int level, int x, int y, Bitmap data, long expiration) throws TileEncodeException {
            impl.setTile(level, x, y, data, expiration);
        }

        @Override
        public boolean hasTileExpirationMetadata() {
            return impl.hasTileExpirationMetadata();
        }

        @Override
        public long getTileExpiration(int level, int x, int y) {
            return impl.getTileExpiration(level, x, y);
        }
    }

    static boolean isRBT(Schema schema) {
        return (schema == Schema.RBT) ||
                (schema == Schema.RBT_CULTURAL) ||
                (schema == Schema.RBT_PHYSICAL);
    }

    final static class RBTTileClient extends RBTTileMatrix implements TileClient
    {
        final TileClient impl;

        RBTTileClient(TileClient impl) {
            super(impl);
            this.impl = impl;
        }

        @Override
        public void clearAuthFailed() {
            impl.clearAuthFailed();
        }

        @Override
        public void checkConnectivity() {
            impl.checkConnectivity();
        }

        @Override
        public void cache(CacheRequest request, CacheRequestListener listener) {
            impl.cache(request, listener);
        }

        @Override
        public int estimateTileCount(CacheRequest request) {
            return impl.estimateTileCount(request);
        }
    }

    final class SurfaceBoundsFilter implements EnvelopeFilter {

        Collection<Envelope> surfaceBounds = Collections.emptySet();

        void update() {
            if(surfaceControl[0] == null)
                return;
            surfaceBounds = new ArrayList<>(surfaceControl[0].getSurfaceBounds());
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

    static class DeduplicateContext {
        int sceneRenderPump = -1;
        Set<String> renderPumpBasemapUrls = new HashSet<>();
    }

    static native GLMapRenderable2 createClientImpl(long ctxptr, TileClient tiles, boolean overlay, boolean autostyle, long[] ptr);
    static native GLMapRenderable2 createContainerImpl(long ctxptr, TileContainer tiles, boolean overlay, boolean autostyle, long[] ptr);
    static native void sourceContentUpdated(long ptr);
    static native void sourceContentUpdated(long ptr, int zoom, int x, int y);
    static native void hitTest(long pointer, Collection<Feature> fids, float screenX, float screenY, double lat, double lng, double gsd, float radius, int limit);
    static native int getColor(long pointer);
    static native void setColor(long pointer, int color);
}
