package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.interop.Pointer;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;

import java.util.Collection;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class NativeTileClient extends NativeTileMatrix implements TileClient
{
    NativeTileClient(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    @Override
    public <T> T getControl(Class<T> controlClazz)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return (T)getControl(this.pointer.raw, controlClazz);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public void getControls(Collection<Object> controls)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            getControls(this.pointer.raw, controls);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void clearAuthFailed()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            clearAuthFailed(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void checkConnectivity()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            checkConnectivity(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void cache(CacheRequest request, CacheRequestListener listener)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            cache(this.pointer.raw, request, listener);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public int estimateTileCount(CacheRequest request)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return estimateTileCount(this.pointer.raw, request);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    static TileClient create(Pointer pointer, Object owner)
    {
        return new NativeTileClient(pointer, owner);
    }

    static native Pointer wrap(TileClient object);

    private static native Object getControl(long raw, Class<?> controlClazz);
    private static native void getControls(long raw, Collection<Object> controls);
    private static native void clearAuthFailed(long raw);
    private static native void checkConnectivity(long raw);
    private static native int estimateTileCount(long raw, CacheRequest request);
    private static native void cache(long raw, CacheRequest request, CacheRequestListener listener);

}
