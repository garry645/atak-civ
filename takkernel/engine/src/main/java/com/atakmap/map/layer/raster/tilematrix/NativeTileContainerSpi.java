package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.util.Disposable;

@DontObfuscate
final class NativeTileContainerSpi implements TileContainerSpi, Disposable
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(TileContainerSpi.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object object;

    NativeTileContainerSpi(Pointer pointer, Object owner)
    {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);

        this.pointer = pointer;
        this.object = owner;
    }

    @Override
    public void dispose()
    {
        if (this.cleaner != null)
            this.cleaner.clean();
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
            this.rwlock.acquireRead();
        }
    }

    @Override
    public String getDefaultExtension()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getDefaultExtension(this.pointer.raw);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    @Override
    public TileContainer create(String name, String path, TileMatrix spec)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return create(this.pointer.raw, name, path, spec);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    @Override
    public TileContainer open(String path, TileMatrix spec, boolean readOnly)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return open(this.pointer.raw, path, spec, readOnly);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    @Override
    public boolean isCompatible(TileMatrix spec)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return isCompatible(this.pointer.raw, spec);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    /*************************************************************************/
    // Interop
    static native long getPointer(TileContainerSpi object);

    static native Pointer wrap(TileContainerSpi object);

    static native boolean hasPointer(TileContainerSpi object);

    static native TileContainerSpi create(Pointer pointer, Object ownerReference);

    static native boolean hasObject(long pointer);

    static native TileContainerSpi getObject(long pointer);

    //static Pointer clone(long otherRawPointer);
    static native void destruct(Pointer pointer);

    private static native String getName(long pointer);
    private static native String getDefaultExtension(long pointer);
    private static native TileContainer create(long pointer, String name, String path, TileMatrix spec);
    private static native TileContainer open(long pointer, String path, TileMatrix spec, boolean readOnly);
    private static native boolean isCompatible(long pointer, TileMatrix spec);
}
