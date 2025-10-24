package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.util.Disposable;

@DontObfuscate
final class NativeTileClientSpi implements TileClientSpi, Disposable
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(TileContainerSpi.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object object;

    NativeTileClientSpi(Pointer pointer, Object owner)
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
    public TileClient create(String path, String offlineCachePath, Options opts)
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return create(this.pointer.raw, path, offlineCachePath, opts);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    @Override
    public int getPriority()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getPriority(this.pointer.raw);
        } finally
        {
            this.rwlock.acquireRead();
        }
    }

    /*************************************************************************/
    // Interop
    static native long getPointer(TileClientSpi object);

    static native Pointer wrap(TileClientSpi object);

    static native boolean hasPointer(TileClientSpi object);

    static native TileClientSpi create(Pointer pointer, Object ownerReference);

    static native boolean hasObject(long pointer);

    static native TileClientSpi getObject(long pointer);

    //static Pointer clone(long otherRawPointer);
    static native void destruct(Pointer pointer);

    private static native int getPriority(long ptr);
    private static native String getName(long ptr);
    private static native TileClient create(long ptr, String path, String offlineCachePath, Options opts);
}
