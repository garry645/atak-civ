package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.filesystem.HashingUtils;
import com.atakmap.lang.Objects;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelHitTestControl;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.map.layer.model.SceneObjectControl;
import com.atakmap.map.layer.model.opengl.GLSceneSpi;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import com.atakmap.math.PointD;
import com.atakmap.opengl.DepthSampler;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.util.Collections2;
import com.atakmap.util.ConfigOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.math.IMatrix;
import gov.tak.platform.commons.opengl.GLES30;

class GLTileset implements GLMapRenderable2, Controls
{
    private static final String TAG = "com.atakmap.map.formats.c3dt.GLTileset";

    private final static Matrix Y_UP_TO_Z_UP = new Matrix(1d, 0d, 0d, 0d,
            0d, 0d, -1d, 0d,
            0d, 1d, 0d, 0d,
            0d, 0d, 0d, 1d);

    private static final double tileSize = 16;

    public final static GLSceneSpi SPI = new GLSceneSpi()
    {
        @Override
        public GLMapRenderable2 create(MapRenderer renderer, ModelInfo info, String cacheDir)
        {
            if (info == null)
                return null;
            if (!Objects.equals(info.type, Cesium3DTilesModelInfoSpi.INSTANCE.getName()))
                return null;

            try
            {
                String globalCacheDir = ConfigOptions.getOption("3dtiles.cache-dir", null);
                ContentContainer cache = null;
                boolean streaming = (info.uri.startsWith("http:") || info.uri.startsWith("https:"));
                if(info.metadata != null && info.metadata.containsAttribute("streaming"))
                    streaming |= info.metadata.getIntAttribute("streaming") != 0;
                if (streaming && (cacheDir != null || globalCacheDir != null))
                {
                    final String relativeUri = info.uri.substring(0, info.uri.lastIndexOf('/'));
                    if (globalCacheDir != null) {
                        final String cacheName = info.name != null ? info.name : HashingUtils.sha256sum(info.uri);
                        cache = ContentSources.createCache(cacheName, new File(globalCacheDir), relativeUri);
                    } else if (cacheDir != null) {
                        cache = ContentSources.createCache(new File(cacheDir), relativeUri);
                    }
                }
                AssetAccessor assetAccessor = new AssetAccessor(cache);
                return new GLTileset(renderer, info, assetAccessor);
            } catch (Throwable t)
            {
                return null;
            }
        }

        @Override
        public int getPriority()
        {
            return 0;
        }
    };

    private final MapRenderer mapRenderer;
    private final ModelInfo info;
    private final AssetAccessor assetAccessor;
    private final HashMap<Long, GLTile> gltiles = new HashMap<>();
    private final Set<Object> controls = Collections2.newIdentityHashSet();
    private final MapRendererState state;
    private Tileset tileset;
    private DepthSampler depthSampler;
    private double maxScreenSpaceError;

    GLTileset(MapRenderer mapRenderer, ModelInfo info, AssetAccessor assetAccessor)
    {
        this.mapRenderer = mapRenderer;
        this.info = info;
        this.assetAccessor = assetAccessor;
        controls.add(new SceneControlImpl());
        controls.add(new HitTestControlImpl());

        this.state = new MapRendererState(((MapRenderer2)mapRenderer).getRenderContext());
    }

    @Override
    public int getRenderPass()
    {
        return GLMapView.RENDER_PASS_SCENES;
    }

    @Override
    public void draw(GLMapView view, int renderPass)
    {
        if(!MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SCENES))
            return;

        if (tileset == null) {
            maxScreenSpaceError = 16.0;
            if (state.ctx.getRenderSurface() != null)
                maxScreenSpaceError *= state.ctx.getRenderSurface().getDpi() / 240d;

            long refreshInterval;
            if (info.metadata != null && info.metadata.containsAttribute("refreshInterval"))
                refreshInterval = info.metadata.getLongAttribute("refreshInterval");

            Tileset.OpenOptions opts = new Tileset.OpenOptions();
            opts.maxScreenSpaceError = maxScreenSpaceError;
            tileset = Tileset.parse(info.uri, assetAccessor, opts);
            tileset.loadRootTileSync();
            if (info.metadata != null && info.metadata.containsAttribute("aabb")) {
                double[] aabb = info.metadata.getDoubleArrayAttribute("aabb");
                Envelope envelope = new Envelope(aabb[0], aabb[1], aabb[2], aabb[3], aabb[4], aabb[5]);
                getControl(SceneControlImpl.class).dispatchSceneBoundsChanged(envelope, maxScreenSpaceError / tileSize);
            }
        }

        // update state
        state.scene = view.currentPass.scene;
        state.top = view.currentPass.top;
        state.left = view.currentPass.left;
        state.bottom = view.currentPass.bottom;
        state.right = view.currentPass.right;
        state.northBound = view.currentPass.northBound;
        state.westBound = view.currentPass.westBound;
        state.southBound = view.currentPass.southBound;
        state.eastBound = view.currentPass.eastBound;
        state.drawSrid = view.currentPass.drawSrid;
        state.isDepthTest = false;
        state.uMVP = -1;
        state.scratch.matrix.set(state.scene.camera.projection);
        state.scratch.matrix.concatenate(state.scene.camera.modelView);
        state.maxScreenSpaceError = maxScreenSpaceError;
        state.scene.mapProjection.inverse(state.scene.camera.location, state.camera);

        // pull the projection matrix from the graphics state
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION, view.scratch.matrixF, 0);
        for (int i = 0; i < 16; i++)
            state.projection.set(i % 4, i / 4, view.scratch.matrixF[i]);

        ViewUpdateResults viewUpdateResults = tileset.updateView(view);
        state.viewUpdateResults = viewUpdateResults;

        for (Tile tile : viewUpdateResults.tilesFadingOut)
        {
            releaseTile(tile);
        }

        for (Tile tile : viewUpdateResults.tilesToRenderThisFrame)
        {
            GLTile glTile = getGlTile(tile);
            if (glTile == null) continue;
            glTile.insetDepth(state);
        }

        renderTiles(viewUpdateResults.tilesToRenderThisFrame, state);
    }

    private void renderTiles(Tile[] tiles, MapRendererState state)
    {
        ArrayList<GLTile> gltiles = new ArrayList<>();
        for (Tile tile : tiles)
        {
            GLTile glTile = getGlTile(tile);
            if (glTile == null) continue;
            gltiles.add(glTile);
        }
        renderTiles(gltiles.toArray(new GLTile[0]), state);
    }

    static void renderTiles(GLTile[] tiles, MapRendererState state)
    {
        if (tiles.length == 0) return;

        Matrix cameraMvp = Matrix.getIdentity();
        if (state.scene.camera.perspective)
        {
            cameraMvp.set(state.scene.camera.projection);
            cameraMvp.concatenate(state.scene.camera.modelView);
        } else
        {
            // pull the projection matrix from the graphics state
            cameraMvp.set(state.scene.camera.projection);
            // concatenate the scene transform
            cameraMvp.concatenate(state.scene.forward);
        }

        long[] vaos = new long[tiles.length];
        double[][] mvps = new double[tiles.length][];
        for (int a = 0; a < tiles.length; a++)
        {
            GLTile glTile = tiles[a];
            vaos[a] = glTile.vao;

            state.scratch.matrix.set(cameraMvp);
            if (state.drawSrid == 4326) {
                // convert from ECEF to LLA
                Envelope aabb = glTile.boundingBox;

                Matrix lla2ecef = lla2ecef((aabb.minY + aabb.maxY) / 2d, (aabb.minX + aabb.maxX) / 2d);
                try {
                    Matrix ecef2lla = lla2ecef.createInverse();
                    state.scratch.matrix.concatenate(ecef2lla);
                } catch (NoninvertibleTransformException e) {
                    Log.w(TAG, "Could not invert lla2ecef matrix transformation");
                }
            }
            // apply tile transform
            Matrix transform = glTile.transform;
            state.scratch.matrix.concatenate(transform);

            // apply RTC
            double[] rtc = glTile.model.getRtc();
            state.scratch.matrix.translate(rtc[0], rtc[1], rtc[2]);

            // apply y-up to z-up
            state.scratch.matrix.concatenate(Y_UP_TO_Z_UP);

            state.scratch.matrix.get(state.scratch.matrixD, Matrix.MatrixOrder.COLUMN_MAJOR);
            mvps[a] = Arrays.copyOf(state.scratch.matrixD, state.scratch.matrixD.length);
        }

        Tileset.renderTiles(vaos, mvps, !state.isDepthTest, state.uMVP);
    }

    private GLTile getGlTile(Tile tile)
    {
        if (!tile.isRenderable()) return null;
        Long tilePtr = tile.pointer.raw;
        GLTile gltile = gltiles.get(tilePtr);
        if (gltile == null)
        {
            TileRenderContent renderContent = tile.getRenderContent();
            if (renderContent == null) return null;
            gltile = new GLTile(tile, renderContent.getModel());
            gltiles.put(tilePtr, gltile);
        }
        return gltile;
    }

    private void releaseTile(Tile tile)
    {
        Long tileId = tile.pointer.raw;
        GLTile gltile = gltiles.remove(tileId);
        if (gltile != null)
        {
            gltile.release();
        }
    }

    @Override
    public void release()
    {
        for (GLTile gltile : gltiles.values())
        {
            gltile.release();
        }
        gltiles.clear();

        if (depthSampler != null)
        {
            depthSampler.dispose();
            depthSampler = null;
        }
        if (tileset != null)
        {
            tileset.dispose();
            tileset = null;
        }
    }

    @Override
    public <T> T getControl(Class<T> controlClazz)
    {
        if (controlClazz == null)
            return null;
        for (Object ctrl : controls)
            if (controlClazz.isAssignableFrom(ctrl.getClass()))
                return controlClazz.cast(ctrl);
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls)
    {
        controls.addAll(this.controls);
    }

    private static class SceneControlImpl implements SceneObjectControl
    {
        final Set<OnBoundsChangedListener> listeners = Collections2.newIdentityHashSet();

        @Override
        public boolean isModifyAllowed()
        {
            return false;
        }

        @Override
        public void setLocation(GeoPoint location)
        {
        }

        @Override
        public void setLocalFrame(Matrix localFrame)
        {
        }

        @Override
        public void setSRID(int srid)
        {
        }

        @Override
        public void setAltitudeMode(ModelInfo.AltitudeMode mode)
        {
        }

        @Override
        public GeoPoint getLocation()
        {
            return null;
        }

        @Override
        public int getSRID()
        {
            return 4326;
        }

        @Override
        public Matrix getLocalFrame()
        {
            return null;
        }

        @Override
        public ModelInfo.AltitudeMode getAltitudeMode()
        {
            return ModelInfo.AltitudeMode.Absolute;
        }

        @Override
        public void addOnSceneBoundsChangedListener(OnBoundsChangedListener l)
        {
            synchronized (listeners)
            {
                listeners.add(l);
            }
        }

        @Override
        public void removeOnSceneBoundsChangedListener(OnBoundsChangedListener l)
        {
            synchronized (listeners)
            {
                listeners.remove(l);
            }
        }

        void dispatchSceneBoundsChanged(Envelope aabb, double minRes)
        {
            synchronized (listeners)
            {
                for (OnBoundsChangedListener l : listeners)
                    l.onBoundsChanged(aabb, minRes, 0d);
            }
        }
    }

    private class HitTestControlImpl implements ModelHitTestControl
    {
        @Override
        public boolean hitTest(final float screenX, final float screenY, final GeoPoint geoPoint)
        {
            final boolean[] retval = new boolean[] {false, false};
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    boolean isAttached = ((MapRenderer2)mapRenderer).getRenderContext().isAttached();
                    if (!isAttached) ((MapRenderer2)mapRenderer).getRenderContext().attach();
                    do
                    {
                        if (tileset == null)
                            break;
                        if (depthSampler == null)
                            depthSampler = DepthSampler.create();
                        if (depthSampler == null)
                            break;

                        MapRendererState queryState = new MapRendererState(state);
                        queryState.isDepthTest = true;
                        queryState.uMVP = depthSampler.program.uMVP;

                        // pull the projection matrix from the graphics state

                        // NOTE: We are pushing the far plane very far out as
                        // the distribution of depth values is concentrated
                        // close to the near plane. During rendering, we want
                        // to minimize the distance between the near and far
                        // planes to avoid z-fighting, hwoever, during the
                        // depth hit-test, we'll push them further apart to get
                        // better precision for depth value retrieval.

                        // A better implementation of depth value encoding and
                        // the use of an actual perspective projection could
                        // mitigate the need to modify here
                        GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_PROJECTION);
                        GLES20FixedPipeline.glPushMatrix();
                        GLES20FixedPipeline.glOrthof(queryState.left, queryState.right, queryState.bottom, queryState.top, (float) queryState.scene.camera.near, -2f);
                        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION, queryState.scratch.matrixF, 0);
                        for (int i = 0; i < 16; i++)
                            queryState.projection.set(i % 4, i / 4, queryState.scratch.matrixF[i]);
                        GLES20FixedPipeline.glPopMatrix();
                        GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_MODELVIEW);

                        GLES30.glDepthMask(true);
                        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
                        GLES30.glDepthFunc(GLES30.GL_LEQUAL);

                        depthSampler.begin(screenX, queryState.top - screenY);
                        renderTiles(queryState.viewUpdateResults.tilesToRenderThisFrame, queryState);

                        float depth = depthSampler.getDepth();
                        depthSampler.end();

                        if (depth == 0f || depth >= 1.0f)
                            break;

                        if (queryState.scene.camera.perspective)
                        {
                            // transform screen location at depth to NDC
                            queryState.scratch.pointD.x = ((screenX - queryState.left) / (queryState.right - queryState.left)) * 2.0f - 1f;
                            queryState.scratch.pointD.y = ((queryState.top - screenY) / (queryState.top - queryState.bottom)) * 2.0f - 1f;
                            queryState.scratch.pointD.z = depth * 2.0f - 1.0f;

                            // compute inverse transform
                            queryState.scratch.matrix.set(queryState.scene.camera.projection);
                            queryState.scratch.matrix.concatenate(queryState.scene.camera.modelView);

                            try
                            {
                                queryState.scratch.matrix.set(queryState.scratch.matrix.createInverse());
                            } catch (NoninvertibleTransformException e)
                            {
                                break;
                            }

                            // NDC -> projection
                            queryState.scratch.matrix.transform(queryState.scratch.pointD, queryState.scratch.pointD);
                        } else
                        {
                            // pull the ortho matrix
                            queryState.scratch.matrix.set(queryState.projection);

                            queryState.scratch.pointD.x = screenX;
                            queryState.scratch.pointD.y = queryState.top - screenY;
                            queryState.scratch.pointD.z = 0;

                            queryState.scratch.matrix.transform(queryState.scratch.pointD, queryState.scratch.pointD);
                            queryState.scratch.pointD.z = depth * 2.0f - 1.0f;

                            try
                            {
                                queryState.scratch.matrix.set(queryState.scratch.matrix.createInverse());
                            } catch (NoninvertibleTransformException e)
                            {
                                break;
                            }

                            // NDC -> ortho
                            queryState.scratch.matrix.transform(queryState.scratch.pointD, queryState.scratch.pointD);
                            // ortho -> projection
                            queryState.scene.inverse.transform(queryState.scratch.pointD, queryState.scratch.pointD);
                        }
                        // projection -> LLA
                        queryState.scene.mapProjection.inverse(queryState.scratch.pointD, geoPoint);
                        retval[1] = true;
                    } while (false);

                    if (!isAttached) ((MapRenderer2)mapRenderer).getRenderContext().detach();

                    synchronized (retval)
                    {
                        retval[0] = true;
                        retval.notify();
                    }
                }
            };
            if (mapRenderer.isRenderThread())
                r.run();
            else
                mapRenderer.queueEvent(r);
            synchronized (retval)
            {
                while (!retval[0])
                    try
                    {
                        retval.wait();
                    } catch (InterruptedException ignored) {}
            }
            return retval[1];
        }
    }

    private class CesiumHitTestControlImpl implements ModelHitTestControl
    {
        @Override
        public boolean hitTest(final float screenX, final float screenY, final GeoPoint geoPoint)
        {
            final boolean[] retval = new boolean[] {false, false};
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    boolean isAttached = ((MapRenderer2)mapRenderer).getRenderContext().isAttached();
                    if (!isAttached) ((MapRenderer2)mapRenderer).getRenderContext().attach();
                    do
                    {
                        // XXX - need to sort tiles by distance from camera
                        for (Tile tile : state.viewUpdateResults.tilesToRenderThisFrame) {
                            Long tilePtr = tile.pointer.raw;
                            GLTile gltile = gltiles.get(tilePtr);
                            if (gltile != null && gltile.model != null && gltile.model.hittest(mapRenderer, screenX, screenY, geoPoint)) {
                               retval[1] = true;
                               break;
                           }
                        }
                    } while (false);

                    if (!isAttached) ((MapRenderer2)mapRenderer).getRenderContext().detach();

                    synchronized (retval)
                    {
                        retval[0] = true;
                        retval.notify();
                    }
                }
            };
            if (mapRenderer.isRenderThread())
                r.run();
            else
                mapRenderer.queueEvent(r);
            synchronized (retval)
            {
                while (!retval[0])
                    try
                    {
                        retval.wait();
                    } catch (InterruptedException ignored) {}
            }
            return retval[1];
        }
    }

    private static Matrix lla2ecef(double lat, double lng)
    {
        final Matrix mx = Matrix.getIdentity();

        GeoPoint llaOrigin = new GeoPoint(lat, lng, 0d);
        PointD ecefOrgin = ECEFProjection.INSTANCE.forward(llaOrigin, null);

        // if draw projection is ECEF and source comes in as LLA, we can
        // transform from LLA to ECEF by creating a local ENU CS and
        // chaining the following conversions (all via matrix)
        // 1. LCS -> LLA
        // 2. LLA -> ENU
        // 3. ENU -> ECEF
        // 4. ECEF -> NDC (via MapSceneModel 'forward' matrix)

        // construct ENU -> ECEF
        final double phi = Math.toRadians(llaOrigin.getLatitude());
        final double lambda = Math.toRadians(llaOrigin.getLongitude());

        mx.translate(ecefOrgin.x, ecefOrgin.y, ecefOrgin.z);

        Matrix enu2ecef = new Matrix(
                -Math.sin(lambda), -Math.sin(phi) * Math.cos(lambda), Math.cos(phi) * Math.cos(lambda), 0d,
                Math.cos(lambda), -Math.sin(phi) * Math.sin(lambda), Math.cos(phi) * Math.sin(lambda), 0d,
                0, Math.cos(phi), Math.sin(phi), 0d,
                0d, 0d, 0d, 1d
        );

        mx.concatenate(enu2ecef);

        // construct LLA -> ENU
        final double metersPerDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(llaOrigin.getLatitude());
        final double metersPerDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(llaOrigin.getLatitude());

        mx.scale(metersPerDegLng, metersPerDegLat, 1d);
        mx.translate(-llaOrigin.getLongitude(), -llaOrigin.getLatitude(), -llaOrigin.getAltitude());

        return mx;
    }
}
