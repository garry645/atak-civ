package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.formats.c3dt.Tile.TileRefine;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;
import com.atakmap.math.Rectangle;
import com.atakmap.opengl.Shader;

import java.nio.FloatBuffer;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.platform.commons.opengl.GLES30;

final class GLTile
{
    final static double USE_ECEF_AABB_THRESHOLD = 100000d;

    Model model;
    Long vao;
    double radius;
    double paddedRadius;
    GeoPoint centroid;
    PointD center_ecef;
    final Envelope aabb_lla;
    final Envelope aabb_ecef;
    Volume.Box obb_ecef;
    Volume boundingVolume;
    Matrix transform;
    Envelope boundingBox;
    double geometricError;
    TileRefine tileRefine;

    FloatBuffer boundingVolumeMask;
    int boundingVolumeMaskSrid;
    PointD boundingVolumeRTC = new PointD();
    int boundingVolumeVbo;

    GLTile(Tile tile, Model model)
    {
        this.model = model;
        this.vao = model.bind();
        this.aabb_lla = Tile.approximateBounds(tile);
        this.transform = tile.getTransform();
        this.boundingBox = tile.getBoundingBox();
        this.boundingVolume = tile.getBoundingVolume();
        this.geometricError = tile.getGeometricError();
        this.tileRefine = tile.getRefine();
        if (boundingVolume instanceof Volume.Region)
        {
            Volume.Region region = (Volume.Region) boundingVolume;

            final double east = Math.toDegrees(region.east);
            final double west = Math.toDegrees(region.west);
            final double north = Math.toDegrees(region.north);
            final double south = Math.toDegrees(region.south);

            centroid = new GeoPoint((north + south) / 2d, (east + west) / 2d, (region.maximumHeight + region.minimumHeight) / 2d);

            final double metersDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(centroid.getLatitude());
            final double metersDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(centroid.getLatitude());

            radius = MathUtils.distance(west * metersDegLng, north * metersDegLat, region.maximumHeight, east * metersDegLng, south * metersDegLat, region.minimumHeight) / 2d;

            final double pad = Math.max(metersDegLat, metersDegLng) / Math.min(metersDegLat, metersDegLng);
            paddedRadius = radius * pad;

            center_ecef = ECEFProjection.INSTANCE.forward(centroid, null);
            aabb_ecef = new Envelope(
                    center_ecef.x-radius,
                    center_ecef.y-radius,
                    center_ecef.z-radius,
                    center_ecef.x+radius,
                    center_ecef.y+radius,
                    center_ecef.z+radius);
        } else if (boundingVolume instanceof Volume.Sphere)
        {
            Volume.Sphere sphere = (Volume.Sphere) boundingVolume;

            radius = sphere.radius;

            center_ecef = new PointD(sphere.centerX, sphere.centerY, sphere.centerZ);
            centroid = ECEFProjection.INSTANCE.inverse(center_ecef, null);

            final double metersDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(centroid.getLatitude());
            final double metersDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(centroid.getLatitude());

            final double pad = Math.max(metersDegLat, metersDegLng) / Math.min(metersDegLat, metersDegLng);
            paddedRadius = radius * pad;

            aabb_ecef = new Envelope(
                    center_ecef.x-radius,
                    center_ecef.y-radius,
                    center_ecef.z-radius,
                    center_ecef.x+radius,
                    center_ecef.y+radius,
                    center_ecef.z+radius);
        } else if (boundingVolume instanceof Volume.Box)
        {
            Volume.Box box = (Volume.Box) boundingVolume;

            center_ecef = new PointD(box.centerX, box.centerY, box.centerZ);
            centroid = ECEFProjection.INSTANCE.inverse(center_ecef, null);

            obb_ecef = new Volume.Box();
            obb_ecef.centerX = center_ecef.x;
            obb_ecef.centerY = center_ecef.y;
            obb_ecef.centerZ = center_ecef.z;
            obb_ecef.xDirHalfLen = box.xDirHalfLen;
            obb_ecef.yDirHalfLen = box.yDirHalfLen;
            obb_ecef.zDirHalfLen = box.zDirHalfLen;

            // construct the corners of the OBB

            final double aMinX = center_ecef.x-box.xDirHalfLen[0]-box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double aMinY = center_ecef.y-box.xDirHalfLen[1]-box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double aMinZ = center_ecef.z-box.xDirHalfLen[2]-box.yDirHalfLen[2]-box.zDirHalfLen[2];
            final double aMaxX = center_ecef.x+box.xDirHalfLen[0]+box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double aMaxY = center_ecef.y+box.xDirHalfLen[1]+box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double aMaxZ = center_ecef.z+box.xDirHalfLen[2]+box.yDirHalfLen[2]+box.zDirHalfLen[2];

            final double bMinX = center_ecef.x-box.xDirHalfLen[0]+box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double bMinY = center_ecef.y-box.xDirHalfLen[1]+box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double bMinZ = center_ecef.z-box.xDirHalfLen[2]+box.yDirHalfLen[2]-box.zDirHalfLen[2];
            final double bMaxX = center_ecef.x+box.xDirHalfLen[0]-box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double bMaxY = center_ecef.y+box.xDirHalfLen[1]-box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double bMaxZ = center_ecef.z+box.xDirHalfLen[2]-box.yDirHalfLen[2]+box.zDirHalfLen[2];

            final double cMinX = center_ecef.x-box.xDirHalfLen[0]+box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double cMinY = center_ecef.y-box.xDirHalfLen[1]+box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double cMinZ = center_ecef.z-box.xDirHalfLen[2]+box.yDirHalfLen[2]+box.zDirHalfLen[2];
            final double cMaxX = center_ecef.x+box.xDirHalfLen[0]-box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double cMaxY = center_ecef.y+box.xDirHalfLen[1]-box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double cMaxZ = center_ecef.z+box.xDirHalfLen[2]-box.yDirHalfLen[2]-box.zDirHalfLen[2];

            final double dMinX = center_ecef.x-box.xDirHalfLen[0]-box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double dMinY = center_ecef.y-box.xDirHalfLen[1]-box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double dMinZ = center_ecef.z-box.xDirHalfLen[2]-box.yDirHalfLen[2]+box.zDirHalfLen[2];
            final double dMaxX = center_ecef.x+box.xDirHalfLen[0]+box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double dMaxY = center_ecef.y+box.xDirHalfLen[1]+box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double dMaxZ = center_ecef.z+box.xDirHalfLen[2]+box.yDirHalfLen[2]-box.zDirHalfLen[2];

            // compute AABB of OBB
            aabb_ecef = new Envelope(
                    Math.min(MathUtils.min(aMinX, bMinX, cMinX, dMinX), MathUtils.min(aMaxX, bMaxX, cMaxX, dMaxX)),
                    Math.min(MathUtils.min(aMinY, bMinY, cMinY, dMinY), MathUtils.min(aMaxY, bMaxY, cMaxY, dMaxY)),
                    Math.min(MathUtils.min(aMinZ, bMinZ, cMinZ, dMinZ), MathUtils.min(aMaxZ, bMaxZ, cMaxZ, dMaxZ)),
                    Math.max(MathUtils.max(aMinX, bMinX, cMinX, dMinX), MathUtils.max(aMaxX, bMaxX, cMaxX, dMaxX)),
                    Math.max(MathUtils.max(aMinY, bMinY, cMinY, dMinY), MathUtils.max(aMaxY, bMaxY, cMaxY, dMaxY)),
                    Math.max(MathUtils.max(aMinZ, bMinZ, cMinZ, dMinZ), MathUtils.max(aMaxZ, bMaxZ, cMaxZ, dMaxZ))
            );

            // derive radius from OBB corners
            radius = MathUtils.max(
                    MathUtils.distance(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ) / 2d,
                    MathUtils.distance(bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ) / 2d,
                    MathUtils.distance(cMinX, cMinY, cMinZ, cMaxX, cMaxY, cMaxZ) / 2d,
                    MathUtils.distance(dMinX, dMinY, dMinZ, dMaxX, dMaxY, dMaxZ) / 2d
            );

            final double metersDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(centroid.getLatitude());
            final double metersDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(centroid.getLatitude());

            final double pad = Math.max(metersDegLat, metersDegLng) / Math.min(metersDegLat, metersDegLng);
            paddedRadius = radius * pad;
        } else
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Draw depth insetting for tile.
     * @param view the map renderer state
     * @return true if rendered, false otherwise
     */
    public boolean insetDepth(MapRendererState view)
    {
        // estimate screen space error
        final double metersPerPixelAtD = computeMetersPerPixel(view);
        final double sse = geometricError / metersPerPixelAtD;

        // ensure the backfaces will not be culled since they are used to punch a hole into the
        // depth buffer
        final boolean backfaceVisible =
                (sse <= view.maxScreenSpaceError) ||
                        (MathUtils.distance(
                                view.scene.camera.location.x,
                                view.scene.camera.location.y,
                                view.scene.camera.location.z,
                                center_ecef.x,
                                center_ecef.y,
                                center_ecef.z)+radius < view.scene.camera.farMeters);

        // if masking has not been applied by an ancestor and there is maskable content to be drawn,
        // employ stenciling to inset the mesh (or child meshes) into the current scene, respecting
        // foreground
        if(backfaceVisible && tileRefine == TileRefine.Replace)
        {
            // high resolution mesh is inset into existing scene by using a constructive solid
            // geometry type of method. Subsequent render of the mesh should completely overwrite
            // any lower resolution data that is not in the foreground.

            GLES30.glEnable(GLES30.GL_STENCIL_TEST);
            GLES30.glColorMask(false, false, false, false);
            GLES30.glDepthMask(false);
            GLES30.glStencilMask(0xFF);

            final boolean inVolume = Rectangle.contains(
                    aabb_lla.minX, aabb_lla.minY, aabb_lla.maxX, aabb_lla.maxY,
                    view.camera.getLongitude(), view.camera.getLatitude());

            // 1. Draw bounding volume, respecting depth if outside of bounding volume, ignoring
            // depth if inside bounding volume
            GLES30.glStencilFunc(GLES30.GL_ALWAYS, 0x1, 0x1);
            GLES30.glStencilOp(
                    GLES30.GL_ZERO,
                    inVolume ?
                            GLES30.GL_REPLACE : GLES30.GL_KEEP,
                    GLES30.GL_REPLACE);
            drawBoundingVolume(view);

            // 2. Draw mesh silhouette over bounding volume, respecting stencil and ignoring depth
            // zero out if stencil fails, increment if stencil passes
            GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_INCR, GLES30.GL_INCR);
            GLES30.glStencilFunc(GLES30.GL_EQUAL, 0x1, 0x1);
            GLTileset.renderTiles(new GLTile[]{this}, view);

            // 3. Punch hole into depth buffer using back faces of bounding volume in mesh
            // silhouette region
            GLES30.glStencilMask(0x0);
            GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_KEEP);
            GLES30.glStencilFunc(GLES30.GL_LESS, 0x1, 0xFF);
            GLES30.glDepthMask(true);
            GLES30.glDepthFunc(GLES30.GL_GREATER);
            drawBoundingVolume(view);

            // NOTE: Observation is that step #4 slightly edges out use of `glClear`

            // 4. Clear the bounding volume region in the stencil buffer
            GLES30.glDepthMask(false);
            GLES30.glStencilMask(0xFF);
            GLES30.glStencilOp(GLES30.GL_ZERO, GLES30.GL_ZERO, GLES30.GL_ZERO);
            GLES30.glStencilFunc(GLES30.GL_ALWAYS, 0x0, 0x0);
            drawBoundingVolume(view);

            // 5. Restore normal rendering
            GLES30.glDisable(GLES30.GL_STENCIL_TEST);
            GLES30.glDepthMask(true);
            GLES30.glDepthFunc(GLES30.GL_LEQUAL);
            GLES30.glColorMask(true, true, true, true);
            GLES30.glStencilMask(0x0);
            return true;
        }
        return false;
    }

    private void drawBoundingVolume(MapRendererState view)
    {
        drawBoundingVolume(view, 1f, 1f, 1f, 1f);
    }

    private void drawBoundingVolume(MapRendererState view, float r, float g, float b, float a)
    {
        if(this.boundingVolumeMask == null || view.drawSrid != boundingVolumeMaskSrid)
        {
            if(this.boundingVolumeMask == null)
                this.boundingVolumeMask = Unsafe.allocateDirect(14 * 3, FloatBuffer.class);

            if(view.drawSrid != 4978 || radius < USE_ECEF_AABB_THRESHOLD)
                GLVolume.constructVolumeLLA(view, this.aabb_lla, this.boundingVolumeRTC, this.boundingVolumeMask);
            else if(this.obb_ecef != null)
                GLVolume.constructVolumeOBB(view, this.obb_ecef, this.boundingVolumeRTC, this.boundingVolumeMask);
            else
                GLVolume.constructVolumeProj(view, this.aabb_ecef, this.boundingVolumeRTC, this.boundingVolumeMask);
            this.boundingVolumeMaskSrid = view.drawSrid;

            int[] vbos = new int[1];
            GLES30.glGenBuffers(1, vbos, 0);
            boundingVolumeVbo = vbos[0];
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, boundingVolumeVbo);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, boundingVolumeMask.limit() * 4, this.boundingVolumeMask, GLES30.GL_STATIC_DRAW);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        }

        view.scratch.matrix.set(view.scene.camera.projection);
        view.scratch.matrix.concatenate(view.scene.camera.modelView);
        view.scratch.matrix.translate(boundingVolumeRTC.x, boundingVolumeRTC.y, boundingVolumeRTC.z);
        view.scratch.matrix.get(view.scratch.matrixD, Matrix.MatrixOrder.COLUMN_MAJOR);
        for(int i = 0; i < 16; i++) view.scratch.matrixF[i] = (float)view.scratch.matrixD[i];

        Shader shader = com.atakmap.opengl.Shader.get(view.ctx);

        GLES30.glUseProgram(shader.handle);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLRenderGlobals.get(view.ctx).getWhitePixel().getTexId());

        GLES30.glUniform4f(shader.uColor, r, g, b, a);
        GLES30.glUniformMatrix4fv(shader.uMVP, 1, false, view.scratch.matrixF, 0);
        GLES30.glUniform1i(shader.uTexture, 0);

        GLES30.glDisableVertexAttribArray(shader.aColors);
        GLES30.glDisableVertexAttribArray(shader.aTexCoords);
        GLES30.glDisableVertexAttribArray(shader.aNormals);

        GLES30.glVertexAttrib4f(shader.aColors, 1.f, 1.f, 1.f, 1.f);
        GLES30.glVertexAttrib2f(shader.aTexCoords, 0.5f, 0.5f);
        GLES30.glVertexAttrib3f(shader.aNormals, 0.f, 0.f, 1.f);

        GLES30.glEnableVertexAttribArray(shader.aVertexCoords);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, boundingVolumeVbo);
        GLES30.glVertexAttribPointer(shader.aVertexCoords, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, this.boundingVolumeMask.limit() / 3);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    private double computeMetersPerPixel(MapRendererState view)
    {
        // XXX - distance camera to object
        if(view.drawSrid == 4978) {
            view.scratch.pointD.x = center_ecef.x;
            view.scratch.pointD.y = center_ecef.y;
            view.scratch.pointD.z = center_ecef.z;
        } else
        {
            view.scene.mapProjection.forward(centroid, view.scratch.pointD);
        }
        final double centroidWorldX = view.scratch.pointD.x;
        final double centroidWorldY = view.scratch.pointD.y;
        final double centroidWorldZ = view.scratch.pointD.z;

        double cameraWorldX = view.scene.camera.location.x;
        double cameraWorldY = view.scene.camera.location.y;
        double cameraWorldZ = view.scene.camera.location.z;

        // distance of the camera to the centroid of the tile
        final double dcam = MathUtils.distance(
                cameraWorldX * view.scene.displayModel.projectionXToNominalMeters,
                cameraWorldY * view.scene.displayModel.projectionYToNominalMeters,
                cameraWorldZ * view.scene.displayModel.projectionZToNominalMeters,
                centroidWorldX * view.scene.displayModel.projectionXToNominalMeters,
                centroidWorldY * view.scene.displayModel.projectionYToNominalMeters,
                centroidWorldZ * view.scene.displayModel.projectionZToNominalMeters
        );

        double metersPerPixelAtD = (dcam * Math.tan(view.scene.camera.fov / 2d) / ((view.top - view.bottom) / 2d));
        // if bounding sphere does not contain camera, compute meters-per-pixel at centroid,
        // else use nominal meters-per-pixel
        if (dcam <= radius)
        {
            // XXX -
            return 0.01d; // 1cm
        }
        return metersPerPixelAtD;
    }

    public void release()
    {
        if (model != null)
        {
            if (vao != null) model.release(vao);
            model.destroy();
        }
        model = null;
        vao = null;

        Unsafe.free(this.boundingVolumeMask);
        this.boundingVolumeMask = null;
        this.boundingVolumeMaskSrid = -1;
        if (boundingVolumeVbo > 0) {
            int[] vbos = new int[] {boundingVolumeVbo};
            GLES30.glDeleteBuffers(1, vbos, 0);
        }
        this.boundingVolumeVbo = 0;
    }
}
