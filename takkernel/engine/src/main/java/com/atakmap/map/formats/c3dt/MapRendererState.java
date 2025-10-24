package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapSceneModel;
import gov.tak.api.engine.map.RenderContext;

import com.atakmap.math.Frustum;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;

import java.util.Comparator;

final class MapRendererState
{
    public static final class ScratchPad
    {
        private ScratchPad()
        {
        }

        public final GeoPoint geo = GeoPoint.createMutable();
        public final PointD pointD = new PointD(0d, 0d, 0d);
        public final Matrix matrix = Matrix.getIdentity();
        public final float[] matrixF = new float[16];
        public final double[] matrixD = new double[16];
    }

    public RenderContext ctx;
    public MapSceneModel scene;
    public int top;
    public int left;
    public int bottom;
    public int right;
    public double northBound;
    public double westBound;
    public double southBound;
    public double eastBound;
    public int drawSrid;
    public boolean isDepthTest;
    public int uMVP;
    public double maxScreenSpaceError = 16d;
    public ViewUpdateResults viewUpdateResults;

    public final ScratchPad scratch = new ScratchPad();
    public final Matrix projection = Matrix.getIdentity();
    public final GeoPoint camera = GeoPoint.createMutable();

    public MapRendererState(RenderContext ctx)
    {
        this.ctx = ctx;
    }

    public MapRendererState(MapRendererState other)
    {
        this(other.ctx);

        this.scene = new MapSceneModel(other.scene);
        this.top = other.top;
        this.left = other.left;
        this.bottom = other.bottom;
        this.right = other.right;
        this.northBound = other.northBound;
        this.westBound = other.westBound;
        this.southBound = other.southBound;
        this.eastBound = other.eastBound;
        this.drawSrid = other.drawSrid;
        this.isDepthTest = other.isDepthTest;
        this.uMVP = other.uMVP;
        this.projection.set(other.projection);
        this.maxScreenSpaceError = other.maxScreenSpaceError;
        this.viewUpdateResults = other.viewUpdateResults;
        this.scene.mapProjection.inverse(this.scene.camera.location, this.camera);
    }
}
