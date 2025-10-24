
package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.util.Collection;

public class MockTileClientJNI extends MockTileMatrixJNI implements TileClient {
    boolean authFailed;

    public MockTileClientJNI(String name, int srid, ZoomLevel[] levels,
                             double originX, double originY, Envelope bounds) {
        super(name, srid, levels, originX, originY, bounds);

        authFailed = false;
    }

    @Override
    public void clearAuthFailed() {
        authFailed = false;
    }

    @Override
    public void checkConnectivity() {
        // no-op
    }

    String callback_msg = "message";
    boolean callback_fatal = true;

    @Override
    public void cache(CacheRequest request, CacheRequestListener listener) {
        listener.onRequestStarted();
        listener.onRequestComplete();
        listener.onRequestError(null, callback_msg, callback_fatal);
        listener.onRequestCanceled();
        listener.onRequestProgress(1, 2, 3, 4, 5, 6);
    }

    @Override
    public int estimateTileCount(CacheRequest request) {
        return 0;
    }

    @Override
    public <T> T getControl(Class<T> controlClazz) {
        // no-op
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls) {
        // no-op
    }
}
