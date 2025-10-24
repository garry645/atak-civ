package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.interop.Pointer;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import com.atakmap.math.PointD;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.coords.GeoCalculations;

@DontObfuscate
final class Model
{
    private static final String TAG = "com.atakmap.map.formats.c3dt.Model";

    private final static Matrix Y_UP_TO_Z_UP = new Matrix(1d, 0d, 0d, 0d,
            0d, 0d, -1d, 0d,
            0d, 1d, 0d, 0d,
            0d, 0d, 0d, 1d);

    private final Pointer pointer;
    private double[] rtc;

    Model(Pointer ptr)
    {
        this.pointer = ptr;
    }

    public double[] getRtc()
    {
        if (rtc == null) {
            double[] rtcValue = getRtc(pointer.raw);
            rtc = rtcValue != null ? rtcValue : new double[3];
        }
        return rtc;
    }

    public long bind()
    {
        return bindModel(pointer.raw);
    }

    public void release(long vao)
    {
        releaseModel(vao);
    }

    public void destroy()
    {
        destroy(pointer.raw);
    }

    public boolean hittest(MapRenderer mapRenderer, float screenX, float screenY, GeoPoint geoPoint)
    {
        if (!(mapRenderer instanceof MapRenderer2))
            return false;

        final MapSceneModel mapSceneModel = ((MapRenderer2) mapRenderer).getMapSceneModel(false, DisplayOrigin.UpperLeft);

        Matrix projection = Matrix.getIdentity();
        projection.set(mapSceneModel.camera.projection);
        projection.concatenate(mapSceneModel.camera.modelView);
        Matrix projectionInverse;
        try {
            projectionInverse = projection.createInverse();
        } catch (NoninvertibleTransformException e) {
            Log.e(TAG, "Could not create inverse transform.", e);
            return false;
        }
        double x = (screenX / mapSceneModel.width) * 2.0f - 1f;
        double y = ((mapSceneModel.height - screenY) / mapSceneModel.height) * 2.0f - 1f;
        double z = projection.transform(mapSceneModel.camera.target, null).z;
        PointD target = projectionInverse.transform(new PointD(x, y, z), null);
        PointD location = mapSceneModel.camera.location;
        PointD hitPoint = new PointD();
        boolean success = hittest(pointer.raw,
                location.x, location.y, location.z,
                target.x - location.x, target.y - location.y, target.z - location.z,
                hitPoint);
        if (success) mapSceneModel.mapProjection.inverse(hitPoint, geoPoint);
        return success;
    }

    private static native void destroy(long ptr);

    private static native long bindModel(long ptr);

    private static native void releaseModel(long vao);

    private static native double[] getRtc(long ptr);

    private static native boolean hittest(long ptr, double originx, double originy, double originz, double directionx, double directiony, double directionz, PointD hitPoint);
}