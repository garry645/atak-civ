package com.atakmap.map.formats.c3dt;

import com.atakmap.interop.Pointer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;

import java.io.File;

@DontObfuscate
final class Tileset
{
    @DontObfuscate
    public static class OpenOptions
    {
        double maxScreenSpaceError = 16.0;
    }

    private final Pointer pointer;

    Tileset(Pointer pointer)
    {
        this.pointer = pointer;
    }

    public static Tileset parse(String uri, AssetAccessor assetAccessor, OpenOptions opts)
    {
        if (new File(uri).exists()) {
            // Adjust direct file path to use URI
            uri = "file://" + CesiumUtility.nativePathToUriPath(uri);
        }
        return create(uri, assetAccessor, opts);
    }

    public static Tileset parse(File file, AssetAccessor assetAccessor, OpenOptions opts)
    {
        String uri = "file://" + CesiumUtility.nativePathToUriPath(file.getAbsolutePath());
        return parse(uri, assetAccessor, opts);
    }

    public ViewUpdateResults updateView(MapRenderer3 renderer)
    {
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(true, DisplayOrigin.UpperLeft);
        double vFov = Math.toRadians(mapSceneModel.camera.fov);
        double aspectRatio = ((double) mapSceneModel.width / mapSceneModel.height);
        double hFov = 2 * Math.atan(Math.tan(vFov / 2.0) * aspectRatio);

        return updateView(mapSceneModel.camera.location.x, mapSceneModel.camera.location.y, mapSceneModel.camera.location.z,
                mapSceneModel.camera.target.x - mapSceneModel.camera.location.x, mapSceneModel.camera.target.y - mapSceneModel.camera.location.y, mapSceneModel.camera.target.z - mapSceneModel.camera.location.z,
                mapSceneModel.camera.up.x, mapSceneModel.camera.up.y, mapSceneModel.camera.up.z,
                mapSceneModel.width, mapSceneModel.height,
                hFov, vFov);
    }

    public ViewUpdateResults updateView(double positionx, double positiony, double positionz,
                                        double directionx, double directiony, double directionz,
                                        double upx, double upy, double upz,
                                        double viewportSizex, double viewportSizey,
                                        double hfov, double vfov)
    {
        return updateView(pointer.raw, positionx, positiony, positionz, directionx, directiony, directionz,
                upx, upy, upz, viewportSizex, viewportSizey, hfov, vfov);
    }

    public static void renderTiles(long[] vaos, double[][] mvps, boolean useShader, int u_mvp)
    {
        renderimpl(vaos, mvps, useShader, u_mvp);
    }

    /**
     * Must be called from main thread.
     */
    public void loadRootTileSync()
    {
        loadRootTileSync(pointer.raw);
    }

    public Tile getRootTile()
    {
        return getRootTile(pointer.raw);
    }

    public void dispose()
    {
        destroy(pointer.raw);
    }

    private static native Tileset create(String uri, AssetAccessor assetAccessor, OpenOptions opts);

    private static native ViewUpdateResults updateView(long ptr, double positionx, double positiony, double positionz,
                                          double directionx, double directiony, double directionz,
                                          double upx, double upy, double upz,
                                          double viewportSizex, double viewportSizey,
                                          double hfov, double vfov);

    private static native void loadRootTileSync(long ptr);

    private static native Tile getRootTile(long ptr);

    private static native void destroy(long ptr);

    private static native void renderimpl(long[] vaos, double[][] mvps, boolean useShader, int u_mvp);
}
