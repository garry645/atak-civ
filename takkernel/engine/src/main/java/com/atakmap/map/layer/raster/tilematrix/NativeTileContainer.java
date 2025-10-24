package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.interop.Pointer;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class NativeTileContainer extends NativeTileMatrix implements TileContainer
{
    NativeTileContainer(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    @Override
    public boolean isReadOnly()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return isReadOnly(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void setTile(int level, int x, int y, byte[] data, long expiration)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            setTileData(this.pointer.raw, level, x, y, data, expiration);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public void setTile(int level, int x, int y, Bitmap data, long expiration) throws TileEncodeException
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            setTile(this.pointer.raw, level, x, y, data, expiration);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public boolean hasTileExpirationMetadata()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return hasTileExpirationMetadata(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public long getTileExpiration(int level, int x, int y)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getTileExpiration(this.pointer.raw, level, x, y);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    static TileContainer create(Pointer pointer, Object owner)
    {
        return new NativeTileContainer(pointer, owner);
    }

    static native Pointer wrap(TileContainer object);

    private static native boolean isReadOnly(long raw);
    private static native void setTile(long raw, int level, int x, int y, Bitmap data, long expiration);
    private static native void setTileData(long raw, int level, int x, int y, byte[] data, long expiration);
    private static native boolean hasTileExpirationMetadata(long raw);
    private static native long getTileExpiration(long raw, int level, int x, int y);
}
