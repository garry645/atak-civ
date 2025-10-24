package com.atakmap.map.formats.c3dt;

import android.graphics.Color;

import com.atakmap.lang.Unsafe;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.opengl.GLES20FixedPipeline;

import java.nio.FloatBuffer;

import gov.tak.platform.commons.opengl.GLES20;
import gov.tak.platform.commons.opengl.GLES30;

final class GLVolume
{
    public static void drawLLA(MapRendererState view, Envelope aabb, int color)
    {
        FloatBuffer verts = null;
        try
        {
            verts = Unsafe.allocateDirect(3 * (8 + 8 + 8), FloatBuffer.class);

            // bottom
            put(view, aabb.maxY, aabb.minX, aabb.minZ, verts); // upper
            put(view, aabb.maxY, aabb.maxX, aabb.minZ, verts);
            put(view, aabb.maxY, aabb.maxX, aabb.minZ, verts); // right
            put(view, aabb.minY, aabb.maxX, aabb.minZ, verts);
            put(view, aabb.minY, aabb.maxX, aabb.minZ, verts); // bottom
            put(view, aabb.minY, aabb.minX, aabb.minZ, verts);
            put(view, aabb.minY, aabb.minX, aabb.minZ, verts); // left
            put(view, aabb.maxY, aabb.minX, aabb.minZ, verts);
            // top
            put(view, aabb.maxY, aabb.minX, aabb.maxZ, verts); // upper
            put(view, aabb.maxY, aabb.maxX, aabb.maxZ, verts);
            put(view, aabb.maxY, aabb.maxX, aabb.maxZ, verts); // right
            put(view, aabb.minY, aabb.maxX, aabb.maxZ, verts);
            put(view, aabb.minY, aabb.maxX, aabb.maxZ, verts); // bottom
            put(view, aabb.minY, aabb.minX, aabb.maxZ, verts);
            put(view, aabb.minY, aabb.minX, aabb.maxZ, verts); // left
            put(view, aabb.maxY, aabb.minX, aabb.maxZ, verts);

            // UL
            put(view, aabb.maxY, aabb.minX, aabb.minZ, verts);
            put(view, aabb.maxY, aabb.minX, aabb.maxZ, verts);
            // UR
            put(view, aabb.maxY, aabb.maxX, aabb.minZ, verts);
            put(view, aabb.maxY, aabb.maxX, aabb.maxZ, verts);
            // LR
            put(view, aabb.minY, aabb.maxX, aabb.minZ, verts);
            put(view, aabb.minY, aabb.maxX, aabb.maxZ, verts);
            // LL
            put(view, aabb.minY, aabb.minX, aabb.minZ, verts);
            put(view, aabb.minY, aabb.minX, aabb.maxZ, verts);

            verts.flip();

            GLES20FixedPipeline.glEnableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
            GLES20FixedPipeline.glLineWidth(1f);
            GLES20FixedPipeline.glColor4f(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f);
            GLES20FixedPipeline.glVertexPointer(3, GLES20FixedPipeline.GL_FLOAT, 0, verts);
            GLES20FixedPipeline.glDrawArrays(GLES20FixedPipeline.GL_LINES, 0, verts.limit() / 3);

            GLES20FixedPipeline.glDisableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
        } finally
        {
            if (verts != null)
                Unsafe.free(verts);
        }
    }

    public static void drawProj(MapRendererState view, Envelope aabb, int color)
    {
        FloatBuffer verts = null;
        try
        {
            verts = Unsafe.allocateDirect(3 * (8 + 8 + 8), FloatBuffer.class);

            // bottom
            putProj(view, aabb.minX, aabb.maxY, aabb.minZ, verts); // upper
            putProj(view, aabb.maxX, aabb.maxY, aabb.minZ, verts);
            putProj(view, aabb.maxX, aabb.maxY, aabb.minZ, verts); // right
            putProj(view, aabb.maxX, aabb.minY, aabb.minZ, verts);
            putProj(view, aabb.maxX, aabb.minY, aabb.minZ, verts); // bottom
            putProj(view, aabb.minX, aabb.minY, aabb.minZ, verts);
            putProj(view, aabb.minX, aabb.minY, aabb.minZ, verts); // left
            putProj(view, aabb.minX, aabb.maxY, aabb.minZ, verts);
            // top
            putProj(view, aabb.minX, aabb.maxY, aabb.maxZ, verts); // upper
            putProj(view, aabb.maxX, aabb.maxY, aabb.maxZ, verts);
            putProj(view, aabb.maxX, aabb.maxY, aabb.maxZ, verts); // right
            putProj(view, aabb.maxX, aabb.minY, aabb.maxZ, verts);
            putProj(view, aabb.maxX, aabb.minY, aabb.maxZ, verts); // bottom
            putProj(view, aabb.minX, aabb.minY, aabb.maxZ, verts);
            putProj(view, aabb.minX, aabb.minY, aabb.maxZ, verts); // left
            putProj(view, aabb.minX, aabb.maxY, aabb.maxZ, verts);

            // UL
            putProj(view, aabb.minX, aabb.maxY, aabb.minZ, verts);
            putProj(view, aabb.minX, aabb.maxY, aabb.maxZ, verts);
            // UR
            putProj(view, aabb.maxX, aabb.maxY, aabb.minZ, verts);
            putProj(view, aabb.maxX, aabb.maxY, aabb.maxZ, verts);
            // LR
            putProj(view, aabb.maxX, aabb.minY, aabb.minZ, verts);
            putProj(view, aabb.maxX, aabb.minY, aabb.maxZ, verts);
            // LL
            putProj(view, aabb.minX, aabb.minY, aabb.minZ, verts);
            putProj(view, aabb.minX, aabb.minY, aabb.maxZ, verts);

            verts.flip();

            GLES20FixedPipeline.glEnableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
            GLES20FixedPipeline.glLineWidth(1f);
            GLES20FixedPipeline.glColor4f(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f);
            GLES20FixedPipeline.glVertexPointer(3, GLES20FixedPipeline.GL_FLOAT, 0, verts);
            GLES20FixedPipeline.glDrawArrays(GLES20FixedPipeline.GL_LINES, 0, verts.limit() / 3);

            GLES20FixedPipeline.glDisableClientState(GLES20FixedPipeline.GL_VERTEX_ARRAY);
        } finally
        {
            if (verts != null)
                Unsafe.free(verts);
        }
    }
    
    static void constructVolumeLLA(MapRendererState view, Envelope aabb, PointD rtc_xyz, FloatBuffer buf) {
        if(rtc_xyz == null) {
            rtc_xyz = new PointD();
        } else {
            view.scratch.geo.set((aabb.maxY+aabb.minY)/2d, (aabb.maxX+aabb.minX)/2d, (aabb.maxZ+aabb.minZ)/2d);
            view.scene.mapProjection.forward(view.scratch.geo, rtc_xyz);
        }

        // front = maxZ
        // back = minZ
        // UL = minX, maxY
        // LR = maxX, minY
        view.scratch.geo.set(aabb.maxY, aabb.minX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front TL
        view.scratch.geo.set(aabb.maxY, aabb.maxX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front TR
        view.scratch.geo.set(aabb.minY, aabb.minX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front BL
        view.scratch.geo.set(aabb.minY, aabb.maxX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front BR
        view.scratch.geo.set(aabb.minY, aabb.maxX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back BR
        view.scratch.geo.set(aabb.maxY, aabb.maxX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front TR
        view.scratch.geo.set(aabb.maxY, aabb.maxX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back TR
        view.scratch.geo.set(aabb.maxY, aabb.minX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front TL
        view.scratch.geo.set(aabb.maxY, aabb.minX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back TL
        view.scratch.geo.set(aabb.minY, aabb.minX, aabb.maxZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // front BL
        view.scratch.geo.set(aabb.minY, aabb.minX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back BL
        view.scratch.geo.set(aabb.minY, aabb.maxX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back BR
        view.scratch.geo.set(aabb.maxY, aabb.minX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back TL
        view.scratch.geo.set(aabb.maxY, aabb.maxX, aabb.minZ);
        view.scene.mapProjection.forward(view.scratch.geo, view.scratch.pointD);
        buf.put((float) (view.scratch.pointD.x-rtc_xyz.x));
        buf.put((float) (view.scratch.pointD.y-rtc_xyz.y));
        buf.put((float) (view.scratch.pointD.z-rtc_xyz.z)); // back TR
        buf.flip();
    }

    static void constructVolumeOBB(MapRendererState view, Volume.Box obb, PointD rtc_xyz, FloatBuffer buf) {
        if(rtc_xyz == null) {
            rtc_xyz = new PointD();
        } else {
            rtc_xyz.x = obb.centerX;
            rtc_xyz.y = obb.centerY;
            rtc_xyz.z = obb.centerZ;
        }

        final double fax = 1d;
        final double bax = -1d;
        final double lay = -1d;
        final double ray = 1d;
        final double taz = 1d;
        final double baz = -1d;

        final double fltX = (obb.centerX-rtc_xyz.x)+fax*obb.xDirHalfLen[0]+lay*obb.yDirHalfLen[0]+taz*obb.zDirHalfLen[0]; // front-left-top
        final double fltY = (obb.centerY-rtc_xyz.y)+fax*obb.xDirHalfLen[1]+lay*obb.yDirHalfLen[1]+taz*obb.zDirHalfLen[1];
        final double fltZ = (obb.centerZ-rtc_xyz.z)+fax*obb.xDirHalfLen[2]+lay*obb.yDirHalfLen[2]+taz*obb.zDirHalfLen[2];
        final double flbX = (obb.centerX-rtc_xyz.x)+fax*obb.xDirHalfLen[0]+lay*obb.yDirHalfLen[0]+baz*obb.zDirHalfLen[0]; // front-left-bottom
        final double flbY = (obb.centerY-rtc_xyz.y)+fax*obb.xDirHalfLen[1]+lay*obb.yDirHalfLen[1]+baz*obb.zDirHalfLen[1];
        final double flbZ = (obb.centerZ-rtc_xyz.z)+fax*obb.xDirHalfLen[2]+lay*obb.yDirHalfLen[2]+baz*obb.zDirHalfLen[2];
        final double frtX = (obb.centerX-rtc_xyz.x)+fax*obb.xDirHalfLen[0]+ray*obb.yDirHalfLen[0]+taz*obb.zDirHalfLen[0]; // front-right-top
        final double frtY = (obb.centerY-rtc_xyz.y)+fax*obb.xDirHalfLen[1]+ray*obb.yDirHalfLen[1]+taz*obb.zDirHalfLen[1];
        final double frtZ = (obb.centerZ-rtc_xyz.z)+fax*obb.xDirHalfLen[2]+ray*obb.yDirHalfLen[2]+taz*obb.zDirHalfLen[2];
        final double frbX = (obb.centerX-rtc_xyz.x)+fax*obb.xDirHalfLen[0]+ray*obb.yDirHalfLen[0]+baz*obb.zDirHalfLen[0]; // front-right-bottom
        final double frbY = (obb.centerY-rtc_xyz.y)+fax*obb.xDirHalfLen[1]+ray*obb.yDirHalfLen[1]+baz*obb.zDirHalfLen[1];
        final double frbZ = (obb.centerZ-rtc_xyz.z)+fax*obb.xDirHalfLen[2]+ray*obb.yDirHalfLen[2]+baz*obb.zDirHalfLen[2];
        final double brtX = (obb.centerX-rtc_xyz.x)+bax*obb.xDirHalfLen[0]+ray*obb.yDirHalfLen[0]+taz*obb.zDirHalfLen[0]; // back-right-top
        final double brtY = (obb.centerY-rtc_xyz.y)+bax*obb.xDirHalfLen[1]+ray*obb.yDirHalfLen[1]+taz*obb.zDirHalfLen[1];
        final double brtZ = (obb.centerZ-rtc_xyz.z)+bax*obb.xDirHalfLen[2]+ray*obb.yDirHalfLen[2]+taz*obb.zDirHalfLen[2];
        final double brbX = (obb.centerX-rtc_xyz.x)+bax*obb.xDirHalfLen[0]+ray*obb.yDirHalfLen[0]+baz*obb.zDirHalfLen[0]; // back-right-bottom
        final double brbY = (obb.centerY-rtc_xyz.y)+bax*obb.xDirHalfLen[1]+ray*obb.yDirHalfLen[1]+baz*obb.zDirHalfLen[1];
        final double brbZ = (obb.centerZ-rtc_xyz.z)+bax*obb.xDirHalfLen[2]+ray*obb.yDirHalfLen[2]+baz*obb.zDirHalfLen[2];
        final double bltX = (obb.centerX-rtc_xyz.x)+bax*obb.xDirHalfLen[0]+lay*obb.yDirHalfLen[0]+taz*obb.zDirHalfLen[0]; // back-left-top
        final double bltY = (obb.centerY-rtc_xyz.y)+bax*obb.xDirHalfLen[1]+lay*obb.yDirHalfLen[1]+taz*obb.zDirHalfLen[1];
        final double bltZ = (obb.centerZ-rtc_xyz.z)+bax*obb.xDirHalfLen[2]+lay*obb.yDirHalfLen[2]+taz*obb.zDirHalfLen[2];
        final double blbX = (obb.centerX-rtc_xyz.x)+bax*obb.xDirHalfLen[0]+lay*obb.yDirHalfLen[0]+baz*obb.zDirHalfLen[0]; // back-left-bottom
        final double blbY = (obb.centerY-rtc_xyz.y)+bax*obb.xDirHalfLen[1]+lay*obb.yDirHalfLen[1]+baz*obb.zDirHalfLen[1];
        final double blbZ = (obb.centerZ-rtc_xyz.z)+bax*obb.xDirHalfLen[2]+lay*obb.yDirHalfLen[2]+baz*obb.zDirHalfLen[2];

        buf.put((float) brtX); // back-right-top -- 0
        buf.put((float) brtY);
        buf.put((float) brtZ);
        buf.put((float) frtX); // front-right-top -- 1
        buf.put((float) frtY);
        buf.put((float) frtZ);
        buf.put((float) bltX); // back-left-top -- 2
        buf.put((float) bltY);
        buf.put((float) bltZ);
        buf.put((float) fltX); // front-left-top -- 3
        buf.put((float) fltY);
        buf.put((float) fltZ);
        buf.put((float) flbX); // front-left-back -- 4
        buf.put((float) flbY);
        buf.put((float) flbZ);
        buf.put((float) frtX); // front-right-top -- 5
        buf.put((float) frtY);
        buf.put((float) frtZ);
        buf.put((float) frbX); // front-right-bottom -- 6
        buf.put((float) frbY);
        buf.put((float) frbZ);
        buf.put((float) brtX); // back-right-top -- 7
        buf.put((float) brtY);
        buf.put((float) brtZ);
        buf.put((float) brbX); // back-right-bottom -- 8
        buf.put((float) brbY);
        buf.put((float) brbZ);
        buf.put((float) bltX); // back-left-top -- 9
        buf.put((float) bltY);
        buf.put((float) bltZ);
        buf.put((float) blbX); // back-left-bottom -- 10
        buf.put((float) blbY);
        buf.put((float) blbZ);
        buf.put((float) flbX); // front-left-bottom -- 11
        buf.put((float) flbY);
        buf.put((float) flbZ);
        buf.put((float) brbX); // back-right-bottom -- 12
        buf.put((float) brbY);
        buf.put((float) brbZ);
        buf.put((float) frbX); // front-right-bottom -- 13
        buf.put((float) frbY);
        buf.put((float) frbZ);
        buf.flip();
    }

    static void constructVolumeProj(MapRendererState view, Envelope aabb, PointD rtc_xyz, FloatBuffer buf) {
        if(rtc_xyz == null) {
            rtc_xyz = new PointD();
        } else {
            rtc_xyz.x = (aabb.maxX+aabb.minX)/2d;
            rtc_xyz.y = (aabb.maxY+aabb.minY)/2d;
            rtc_xyz.z = (aabb.maxZ+aabb.minZ)/2d;
        }

        final double fx = aabb.maxX-rtc_xyz.x;
        final double bx = aabb.minX-rtc_xyz.x;
        final double ly = aabb.minY-rtc_xyz.y;
        final double ry = aabb.maxY-rtc_xyz.y;
        final double tz = aabb.maxZ-rtc_xyz.z;
        final double bz = aabb.minZ-rtc_xyz.z;

        final double fltX = fx; // front-left-top
        final double fltY = ly;
        final double fltZ = tz;
        final double flbX = fx; // front-left-bottom
        final double flbY = ly;
        final double flbZ = bz;
        final double frtX = fx; // front-right-top
        final double frtY = ry;
        final double frtZ = tz;
        final double frbX = fx; // front-right-bottom
        final double frbY = ry;
        final double frbZ = bz;
        final double brtX = bx; // back-right-top
        final double brtY = ry;
        final double brtZ = tz;
        final double brbX = bx; // back-right-bottom
        final double brbY = ry;
        final double brbZ = bz;
        final double bltX = bx; // back-left-top
        final double bltY = ly;
        final double bltZ = tz;
        final double blbX = bx; // back-left-bottom
        final double blbY = ly;
        final double blbZ = bz;

        buf.put((float) brtX); // back-right-top -- 0
        buf.put((float) brtY);
        buf.put((float) brtZ);
        buf.put((float) frtX); // front-right-top -- 1
        buf.put((float) frtY);
        buf.put((float) frtZ);
        buf.put((float) bltX); // back-left-top -- 2
        buf.put((float) bltY);
        buf.put((float) bltZ);
        buf.put((float) fltX); // front-left-top -- 3
        buf.put((float) fltY);
        buf.put((float) fltZ);
        buf.put((float) flbX); // front-left-back -- 4
        buf.put((float) flbY);
        buf.put((float) flbZ);
        buf.put((float) frtX); // front-right-top -- 5
        buf.put((float) frtY);
        buf.put((float) frtZ);
        buf.put((float) frbX); // front-right-bottom -- 6
        buf.put((float) frbY);
        buf.put((float) frbZ);
        buf.put((float) brtX); // back-right-top -- 7
        buf.put((float) brtY);
        buf.put((float) brtZ);
        buf.put((float) brbX); // back-right-bottom -- 8
        buf.put((float) brbY);
        buf.put((float) brbZ);
        buf.put((float) bltX); // back-left-top -- 9
        buf.put((float) bltY);
        buf.put((float) bltZ);
        buf.put((float) blbX); // back-left-bottom -- 10
        buf.put((float) blbY);
        buf.put((float) blbZ);
        buf.put((float) flbX); // front-left-bottom -- 11
        buf.put((float) flbY);
        buf.put((float) flbZ);
        buf.put((float) brbX); // back-right-bottom -- 12
        buf.put((float) brbY);
        buf.put((float) brbZ);
        buf.put((float) frbX); // front-right-bottom -- 13
        buf.put((float) frbY);
        buf.put((float) frbZ);
        buf.flip();
    }

    private static void put(MapRendererState view, double lat, double lng, double alt, FloatBuffer verts)
    {
        put(view, lat, lng, alt, 0d, 0d, 0d, verts);
    }

    private static void put(MapRendererState view, double lat, double lng, double alt, double tx, double ty, double tz, FloatBuffer verts)
    {
        view.scratch.geo.set(lat, lng, alt);
        view.scene.forward(view.scratch.geo, view.scratch.pointD);
        verts.put((float)(view.scratch.pointD.x-tx));
        verts.put((float)(view.scratch.pointD.y-ty));
        verts.put((float)(view.scratch.pointD.z-tz));
    }

    private static void putProj(MapRendererState view, double x, double y, double z, FloatBuffer verts)
    {
        putProj(view, x, y, z, 0d, 0d, 0d, verts);
    }

    private static void putProj(MapRendererState view, double x, double y, double z, double tx, double ty, double tz, FloatBuffer verts)
    {
        view.scratch.pointD.x = x;
        view.scratch.pointD.y = y;
        view.scratch.pointD.z = z;
        view.scene.forward.transform(view.scratch.pointD, view.scratch.pointD);
        verts.put((float)(view.scratch.pointD.x-tx));
        verts.put((float)(view.scratch.pointD.y-ty));
        verts.put((float)(view.scratch.pointD.z-tz));
    }
}
