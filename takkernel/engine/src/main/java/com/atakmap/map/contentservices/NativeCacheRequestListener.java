package com.atakmap.map.contentservices;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.util.Disposable;

@DontObfuscate
public final class NativeCacheRequestListener implements CacheRequestListener, Disposable {

    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(CacheRequestListener.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object owner;

    NativeCacheRequestListener(Pointer pointer, Object owner)
    {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);

        this.pointer = pointer;
        this.owner = owner;
    }

    @Override
    public void dispose() {
        if (this.cleaner != null)
            this.cleaner.clean();
    }

    @Override
    public void onRequestStarted() {
        this.rwlock.acquireRead();
        try
        {
            onRequestStartedNative(pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void onRequestComplete() {
        this.rwlock.acquireRead();
        try
        {
            onRequestCompleteNative(pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void onRequestProgress(int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTotalProgress) {
        this.rwlock.acquireRead();
        try
        {
            onRequestProgressNative(pointer.raw, taskNum, numTasks, taskProgress, maxTaskProgress, totalProgress, maxTaskProgress);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public boolean onRequestError(Throwable t, String message, boolean fatal) {
        this.rwlock.acquireRead();
        try
        {
            return onRequestErrorNative(pointer.raw, t, message, fatal);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void onRequestCanceled() {
        this.rwlock.acquireRead();
        try
        {
            onRequestCanceledNative(pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /*************************************************************************/
    // Interop implementation
    static CacheRequestListener create(Pointer pointer, Object owner)
    {
        return new NativeCacheRequestListener(pointer, owner);
    }

    static long getPointer(CacheRequestListener managed)
    {
        if (managed instanceof NativeCacheRequestListener)
            return ((NativeCacheRequestListener) managed).pointer.raw;
        else
            return 0L;
    }

    static boolean hasPointer(CacheRequestListener object)
    {
        if (object instanceof NativeCacheRequestListener)
            return true;
        return false;
    }

    //static Pointer clone(long otherRawPointer);
    static native void destruct(Pointer pointer);

    static native void onRequestCompleteNative(long ptr);
    static native void onRequestStartedNative(long ptr);
    static native void onRequestProgressNative(long ptr, int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTaskProgress1);
    static native void onRequestCanceledNative(long ptr);
    static native boolean onRequestErrorNative(long ptr, Throwable t, String message, boolean fatal);

}
