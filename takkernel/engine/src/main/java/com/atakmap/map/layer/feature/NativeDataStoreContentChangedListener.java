package com.atakmap.map.layer.feature;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class NativeDataStoreContentChangedListener implements FeatureDataStore2.OnDataStoreContentChangedListener
{
    static {
        com.atakmap.interop.Interop.registerInterop(FeatureDataStore2.OnDataStoreContentChangedListener.class,
                new com.atakmap.interop.Interop<FeatureDataStore2.OnDataStoreContentChangedListener>() {
                    @Override
                    public long getPointer(FeatureDataStore2.OnDataStoreContentChangedListener object) {
                        return (object instanceof NativeDataStoreContentChangedListener) ? ((NativeDataStoreContentChangedListener)object).pointer.raw : 0L;
                    }

                    @Override
                    public FeatureDataStore2.OnDataStoreContentChangedListener create(Pointer pointer, Object owner) {
                        return new NativeDataStoreContentChangedListener(pointer, owner, 0L);
                    }

                    @Override
                    public Pointer clone(long pointer) {
                        return null;
                    }

                    @Override
                    public Pointer wrap(FeatureDataStore2.OnDataStoreContentChangedListener object) {
                        return null;
                    }

                    @Override
                    public void destruct(Pointer pointer) {
                        NativeDataStoreContentChangedListener.destruct(pointer);
                    }

                    @Override
                    public boolean hasObject(long pointer) {
                        return false;
                    }

                    @Override
                    public FeatureDataStore2.OnDataStoreContentChangedListener getObject(long pointer) {
                        return null;
                    }

                    @Override
                    public boolean hasPointer(FeatureDataStore2.OnDataStoreContentChangedListener object) {
                        return (object instanceof NativeDataStoreContentChangedListener);
                    }

                    @Override
                    public boolean supportsWrap() {
                        return false;
                    }

                    @Override
                    public boolean supportsClone() {
                        return false;
                    }

                    @Override
                    public boolean supportsCreate() {
                        return true;
                    }
                });
    }
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(FeatureDataStore2.OnDataStoreContentChangedListener.class);

    private Pointer pointer;
    private final ReadWriteLock rwlock = new ReadWriteLock();
    private Object owner;
    private long datastorePtr;

    NativeDataStoreContentChangedListener(Pointer pointer, Object owner, long datastorePtr)
    {
        this.pointer = pointer;
        this.owner = owner;
        this.datastorePtr = datastorePtr;

        NativePeerManager.register(this, pointer, this.rwlock, null, CLEANER);
    }

    @Override
    public void onDataStoreContentChanged(FeatureDataStore2 dataStore) {
        rwlock.acquireRead();
        try {
            if(pointer.raw == 0L)
                return;
            onDataStoreContentChanged(pointer.raw, dataStore, datastorePtr);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void onFeatureInserted(FeatureDataStore2 dataStore, long fid, FeatureDefinition2 def, long version) {
        rwlock.acquireRead();
        try {
            if(pointer.raw == 0L)
                return;
            onFeatureInserted(pointer.raw, dataStore, datastorePtr, fid, def, version);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void onFeatureUpdated(FeatureDataStore2 dataStore, long fid, int modificationMask, String name, Geometry geom, Style style, AttributeSet attribs, int attribsUpdateType) {
        rwlock.acquireRead();
        try {
            if(pointer.raw == 0L)
                return;
            onFeatureUpdated(pointer.raw, dataStore, datastorePtr, fid, modificationMask, name, geom, style, attribs, attribsUpdateType);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void onFeatureDeleted(FeatureDataStore2 dataStore, long fid) {
        rwlock.acquireRead();
        try {
            if(pointer.raw == 0L)
                return;
            onFeatureDeleted(pointer.raw, dataStore, datastorePtr, fid);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void onFeatureVisibilityChanged(FeatureDataStore2 dataStore, long fid, boolean visible) {
        rwlock.acquireRead();
        try {
            if(pointer.raw == 0L)
                return;
            onFeatureVisibilityChanged(pointer.raw, dataStore, datastorePtr, fid, visible);
        } finally {
            rwlock.releaseRead();
        }
    }

    // interop
    static native void destruct(Pointer pointer);

    // native implementation
    static native void onDataStoreContentChanged(long ptr, FeatureDataStore2 dataStore, long datastorPtr);
    static native void onFeatureInserted(long ptr, FeatureDataStore2 dataStore, long datastorPtr, long fid, FeatureDefinition2 def, long version);
    static native void onFeatureUpdated(long ptr, FeatureDataStore2 dataStore, long datastorPtr, long fid, int modificationMask, String name, Geometry geom, Style style, AttributeSet attribs, int attribsUpdateType);
    static native void onFeatureDeleted(long ptr, FeatureDataStore2 dataStore, long datastorPtr, long fid);
    static native void onFeatureVisibilityChanged(long ptr, FeatureDataStore2 dataStore, long datastorPtr, long fid, boolean visible);
}
