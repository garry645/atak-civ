package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class NativeTileMatrix implements TileMatrix
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(TileMatrix.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object owner;

    NativeTileMatrix(Pointer pointer, Object owner)
    {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);

        this.pointer = pointer;
        this.owner = owner;
    }

    @Override
    public String getName()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getName(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public int getSRID()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getSRID(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public ZoomLevel[] getZoomLevel()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getZoomLevel(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public double getOriginX()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getOriginX(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public double getOriginY()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getOriginY(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getTile(this.pointer.raw, zoom, x, y, error);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }


    @Override
    public byte[] getTileData(int zoom, int x, int y, Throwable[] error)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getTileData(this.pointer.raw, zoom, x, y, error);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public Envelope getBounds()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getBounds(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void dispose()
    {
        if (this.cleaner != null)
            this.cleaner.clean();
    }

    /*************************************************************************/
    // Interop implementation
    static TileMatrix create(Pointer pointer, Object owner)
    {
        return new NativeTileMatrix(pointer, owner);
    }

    static long getPointer(TileMatrix managed)
    {
        if (managed instanceof NativeTileMatrix)
            return ((NativeTileMatrix) managed).pointer.raw;
        else
            return 0L;
    }

    static boolean hasPointer(TileMatrix object)
    {
        if (object instanceof NativeTileMatrix)
            return true;
        return false;
    }

    //static Pointer clone(long otherRawPointer);
    static native void destruct(Pointer pointer);

    private static native String getName(long raw);
    private static native int getSRID(long raw);
    private static native ZoomLevel[] getZoomLevel(long raw);
    private static native double getOriginX(long raw);
    private static native double getOriginY(long raw);
    private static native Bitmap getTile(long raw, int zoom, int x, int y, Throwable[] error);
    private static native byte[] getTileData(long raw, int zoom, int x, int y, Throwable[] error);
    private static native Envelope getBounds(long raw);

}
