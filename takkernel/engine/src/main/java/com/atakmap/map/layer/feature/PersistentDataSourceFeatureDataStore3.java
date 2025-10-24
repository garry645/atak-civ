package com.atakmap.map.layer.feature;

import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.control.DataSourceDataStoreControl;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentDataSourceFeatureDataStore3 implements FeatureDataStore2, DataSourceDataStoreControl
{
    NativeFeatureDataStore2 impl;
    Pointer pointer;

    CallbackForwarder callbackForwarder;
    Set<OnDataStoreContentChangedListener> listeners;

    public PersistentDataSourceFeatureDataStore3(File database)
    {
        this(database, false);
    }

    public PersistentDataSourceFeatureDataStore3(File database, boolean readOnly)
    {
        this.pointer = PersistentDataSourceFeatureDataStore2.create(database != null ? database.getAbsolutePath() : null, readOnly);

        this.listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.callbackForwarder = new CallbackForwarder(this, this.listeners);

        this.impl = new NativeFeatureDataStore2(PersistentDataSourceFeatureDataStore2.asBase(this.pointer.raw), null);
        this.impl.addOnDataStoreContentChangedListener(this.callbackForwarder);
    }

    /**
     * Get all feature sets for a given source file
     *
     * @param f Source file
     * @return Feature set cursor
     */
    public FeatureSetCursor queryFeatureSets(File f)
    {
        impl.rwlock.acquireRead();
        try
        {
            final Pointer retval = PersistentDataSourceFeatureDataStore2.queryFeatureSets(pointer.raw, f.getAbsolutePath());
            if (retval == null)
                return FeatureSetCursor.EMPTY;
            return new NativeFeatureSetCursor(retval, this);
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    // DataSourceFeatureDataStore

    @Override
    public boolean contains(File file)
    {
        impl.rwlock.acquireRead();
        try
        {
            return PersistentDataSourceFeatureDataStore2.contains(this.pointer.raw, file.getAbsolutePath());
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public File getFile(FeatureSet info)
    {
        impl.rwlock.acquireRead();
        try
        {
            final String retval = PersistentDataSourceFeatureDataStore2.getFile(this.pointer.raw, info.getId());
            if (retval == null)
                return null;
            return new File(retval);
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public boolean add(File file) throws IOException
    {
        impl.rwlock.acquireRead();
        try
        {
            return PersistentDataSourceFeatureDataStore2.add(this.pointer.raw, file.getAbsolutePath());
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public boolean add(File file, String hint) throws IOException
    {
        impl.rwlock.acquireRead();
        try
        {
            return PersistentDataSourceFeatureDataStore2.add(this.pointer.raw, file.getAbsolutePath(), hint);
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public void remove(File file)
    {
        impl.rwlock.acquireRead();
        try
        {
            PersistentDataSourceFeatureDataStore2.remove(this.pointer.raw, file.getAbsolutePath());
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public boolean update(File file) throws IOException
    {
        impl.rwlock.acquireRead();
        try
        {
            return PersistentDataSourceFeatureDataStore2.update(this.pointer.raw, file.getAbsolutePath());
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public boolean update(FeatureSet featureSet) throws IOException
    {
        File f = getFile(featureSet);
        if (f == null)
            return false;
        return update(f);
    }

    @Override
    public FileCursor queryFiles()
    {
        impl.rwlock.acquireRead();
        try
        {
            final Pointer retval = PersistentDataSourceFeatureDataStore2.queryFiles(this.pointer.raw);
            return (retval != null) ? new NativeFileCursor(retval, this) : null;
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        impl.rwlock.acquireRead();
        try
        {
            PersistentDataSourceFeatureDataStore2.setReadOnly(this.pointer.raw, readOnly);
        } finally
        {
            impl.rwlock.releaseRead();
        }
    }

    // FeatureDataStore2

    @Override
    public FeatureCursor queryFeatures(FeatureQueryParameters params) throws DataStoreException {
        return impl.queryFeatures(params);
    }

    @Override
    public int queryFeaturesCount(FeatureQueryParameters params) throws DataStoreException {
        return impl.queryFeaturesCount(params);
    }

    @Override
    public FeatureSetCursor queryFeatureSets(FeatureSetQueryParameters params) throws DataStoreException {
        return impl.queryFeatureSets(params);
    }

    @Override
    public int queryFeatureSetsCount(FeatureSetQueryParameters params) throws DataStoreException {
        return impl.queryFeatureSetsCount(params);
    }

    @Override
    public long insertFeature(long fsid, long fid, FeatureDefinition2 def, long version) throws DataStoreException {
        return impl.insertFeature(fsid, fid, def, version);
    }

    @Override
    public long insertFeature(Feature feature) throws DataStoreException {
        return impl.insertFeature(feature);
    }

    @Override
    public void insertFeatures(FeatureCursor features) throws DataStoreException {
        impl.insertFeatures(features);
    }

    @Override
    public long insertFeatureSet(FeatureSet featureSet) throws DataStoreException {
        return impl.insertFeatureSet(featureSet);
    }

    @Override
    public void insertFeatureSets(FeatureSetCursor featureSet) throws DataStoreException {
        impl.insertFeatureSets(featureSet);
    }

    @Override
    public void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, int attrUpdateType) throws DataStoreException {
        impl.updateFeature(fid, updatePropertyMask, name, geometry, style, attributes, attrUpdateType);
    }

    @Override
    public void updateFeatureSet(long fsid, String name, double minResolution, double maxResolution) throws DataStoreException {
        impl.updateFeatureSet(fsid, name, minResolution, maxResolution);
    }

    @Override
    public void updateFeatureSet(long fsid, String name) throws DataStoreException {
        impl.updateFeatureSet(fsid, name);
    }

    @Override
    public void updateFeatureSet(long fsid, double minResolution, double maxResolution) throws DataStoreException {
        impl.updateFeatureSet(fsid, minResolution, maxResolution);
    }

    @Override
    public void deleteFeature(long fid) throws DataStoreException {
        impl.deleteFeature(fid);
    }

    @Override
    public void deleteFeatures(FeatureQueryParameters params) throws DataStoreException {
        impl.deleteFeatures(params);
    }

    @Override
    public void deleteFeatureSet(long fsid) throws DataStoreException {
        impl.deleteFeatureSet(fsid);
    }

    @Override
    public void deleteFeatureSets(FeatureSetQueryParameters params) throws DataStoreException {
        impl.deleteFeatureSets(params);
    }

    @Override
    public void setFeatureVisible(long fid, boolean visible) throws DataStoreException {
        impl.setFeatureVisible(fid, visible);
    }

    @Override
    public void setFeaturesVisible(FeatureQueryParameters params, boolean visible) throws DataStoreException {
        impl.setFeaturesVisible(params, visible);
    }

    @Override
    public void setFeatureSetVisible(long fsid, boolean visible) throws DataStoreException {
        impl.setFeatureSetVisible(fsid, visible);
    }

    @Override
    public void setFeatureSetsVisible(FeatureSetQueryParameters params, boolean visible) throws DataStoreException {
        impl.setFeatureSetsVisible(params, visible);
    }

    @Override
    public boolean hasTimeReference() {
        return impl.hasTimeReference();
    }

    @Override
    public long getMinimumTimestamp() {
        return impl.getMinimumTimestamp();
    }

    @Override
    public long getMaximumTimestamp() {
        return impl.getMaximumTimestamp();
    }

    @Override
    public String getUri() {
        return impl.getUri();
    }

    @Override
    public boolean supportsExplicitIDs() {
        return impl.supportsExplicitIDs();
    }

    @Override
    public int getModificationFlags() {
        return impl.getModificationFlags();
    }

    @Override
    public int getVisibilityFlags() {
        return impl.getVisibilityFlags();
    }

    @Override
    public boolean hasCache() {
        return impl.hasCache();
    }

    @Override
    public void clearCache() {
        impl.clearCache();
    }

    @Override
    public long getCacheSize() {
        return impl.getCacheSize();
    }

    @Override
    public void acquireModifyLock(boolean bulkModification) throws InterruptedException {
        impl.acquireModifyLock(bulkModification);
    }

    @Override
    public void releaseModifyLock() {
        impl.releaseModifyLock();
    }

    @Override
    public void addOnDataStoreContentChangedListener(OnDataStoreContentChangedListener l) {
        listeners.add(l);
    }

    @Override
    public void removeOnDataStoreContentChangedListener(OnDataStoreContentChangedListener l) {
        listeners.remove(l);
    }

    @Override
    public void dispose() {
        impl.removeOnDataStoreContentChangedListener(callbackForwarder);
        impl.dispose();
    }

    final static class CallbackForwarder implements FeatureDataStore2.OnDataStoreContentChangedListener
    {

        final WeakReference<FeatureDataStore2> dbRef;
        final Set<OnDataStoreContentChangedListener> callbacks;

        CallbackForwarder(FeatureDataStore2 db, Set<OnDataStoreContentChangedListener> callbacks) {
            this.dbRef = new WeakReference<>(db);
            this.callbacks = callbacks;
        }

        @Override
        public void onDataStoreContentChanged(FeatureDataStore2 dataStore) {
            final FeatureDataStore2 db = this.dbRef.get();
            if(db != null)
                for(OnDataStoreContentChangedListener cb : callbacks)
                    cb.onDataStoreContentChanged(db);
        }

        @Override
        public void onFeatureInserted(FeatureDataStore2 dataStore, long fid, FeatureDefinition2 def, long version) {
            final FeatureDataStore2 db = this.dbRef.get();
            if(db != null)
                for(OnDataStoreContentChangedListener cb : callbacks)
                    cb.onFeatureInserted(db, fid, def, version);
        }

        @Override
        public void onFeatureUpdated(FeatureDataStore2 dataStore, long fid, int modificationMask, String name, Geometry geom, Style style, AttributeSet attribs, int attribsUpdateType) {
            final FeatureDataStore2 db = this.dbRef.get();
            if(db != null)
                for(OnDataStoreContentChangedListener cb : callbacks)
                    cb.onFeatureUpdated(db, fid, modificationMask, name, geom, style, attribs, attribsUpdateType);
        }

        @Override
        public void onFeatureDeleted(FeatureDataStore2 dataStore, long fid) {
            final FeatureDataStore2 db = this.dbRef.get();
            if(db != null)
                for(OnDataStoreContentChangedListener cb : callbacks)
                    cb.onFeatureDeleted(db, fid);
        }

        @Override
        public void onFeatureVisibilityChanged(FeatureDataStore2 dataStore, long fid, boolean visible) {
            final FeatureDataStore2 db = this.dbRef.get();
            if(db != null)
                for(OnDataStoreContentChangedListener cb : callbacks)
                    cb.onFeatureVisibilityChanged(db, fid, visible);
        }
    }
}
