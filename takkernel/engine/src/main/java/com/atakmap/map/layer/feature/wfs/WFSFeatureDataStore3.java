package com.atakmap.map.layer.feature.wfs;

import java.io.File;

import com.atakmap.map.layer.feature.AbstractFeatureDataStore;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDefinition2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.feature.geometry.Geometry;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use {@link WFSFeatureDataStore4} */
@Deprecated
@DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public class WFSFeatureDataStore3 extends AbstractFeatureDataStore implements Runnable
{

    final WFSFeatureDataStore4 _impl;

    public WFSFeatureDataStore3(String uri, File workingDir)
    {
        super(0,
                VISIBILITY_SETTINGS_FEATURESET);

        _impl = new WFSFeatureDataStore4(uri, workingDir);
        _impl.addOnDataStoreContentChangedListener(new FeatureDataStore2.OnDataStoreContentChangedListener() {
            @Override
            public void onDataStoreContentChanged(FeatureDataStore2 dataStore) {
                dispatchDataStoreContentChangedNoSync();
            }

            @Override
            public void onFeatureInserted(FeatureDataStore2 dataStore, long fid, FeatureDefinition2 def, long version) {
                dispatchDataStoreContentChangedNoSync();
            }

            @Override
            public void onFeatureUpdated(FeatureDataStore2 dataStore, long fid, int modificationMask, String name, Geometry geom, Style style, AttributeSet attribs, int attribsUpdateType) {
                dispatchDataStoreContentChangedNoSync();
            }

            @Override
            public void onFeatureDeleted(FeatureDataStore2 dataStore, long fid) {
                dispatchDataStoreContentChangedNoSync();
            }

            @Override
            public void onFeatureVisibilityChanged(FeatureDataStore2 dataStore, long fid, boolean visible) {
                dispatchDataStoreContentChangedNoSync();
            }
        });
    }


    /**************************************************************************/
    // Runnable
    @Override
    public void run()
    {
        _impl.run();
    }

    /**************************************************************************/

    @Override
    public Feature getFeature(long fid)
    {
        try {
            return Utils.getFeature(_impl, fid);
        } catch(DataStoreException e) {
            return null;
        }
    }

    @Override
    public FeatureCursor queryFeatures(FeatureQueryParameters params)
    {
        try {
            return _impl.queryFeatures(Adapters.adapt(params, null));
        } catch(DataStoreException e) {
            return FeatureCursor.EMPTY;
        }
    }

    @Override
    public int queryFeaturesCount(FeatureQueryParameters params)
    {
        try {
            return _impl.queryFeaturesCount(Adapters.adapt(params, null));
        } catch(DataStoreException e) {
            return 0;
        }
    }

    @Override
    public synchronized FeatureSet getFeatureSet(long fsid)
    {
        try {
            return Utils.getFeatureSet(_impl, fsid);
        } catch(DataStoreException e) {
            return null;
        }
    }

    @Override
    public synchronized FeatureSetCursor queryFeatureSets(FeatureSetQueryParameters params)
    {
        try {
            final com.atakmap.map.layer.feature.FeatureSetCursor result = _impl.queryFeatureSets(Adapters.adapt(params, null));
            return new FeatureSetCursor() {
                @Override
                public FeatureSet get() {
                    return result.get();
                }

                @Override
                public boolean moveToNext() {
                    return result.moveToNext();
                }

                @Override
                public void close() {
                    result.close();
                }

                @Override
                public boolean isClosed() {
                    return result.isClosed();
                }
            };
        } catch(DataStoreException e) {
            return FeatureSetCursor.EMPTY;
        }
    }

    @Override
    public synchronized int queryFeatureSetsCount(FeatureSetQueryParameters params)
    {
        try {
            return _impl.queryFeatureSetsCount(Adapters.adapt(params, null));
        } catch(DataStoreException e) {
            return 0;
        }
    }

    @Override
    public boolean isInBulkModification()
    {
        return false;
    }

    @Override
    public synchronized boolean isFeatureSetVisible(long fsid)
    {
        try {
            return Utils.isFeatureSetVisible(_impl, fsid);
        } catch(DataStoreException e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable()
    {
        return true;
    }

    @Override
    public synchronized void refresh()
    {
        _impl.refresh();
    }

    @Override
    public String getUri()
    {
        return _impl.getUri();
    }

    @Override
    public synchronized void dispose()
    {
        _impl.dispose();
    }

    @Override
    protected void setFeatureVisibleImpl(long fid, boolean visible)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setFeaturesVisibleImpl(FeatureQueryParameters params, boolean visible)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected synchronized void setFeatureSetVisibleImpl(long setId, boolean visible)
    {
        try {
            _impl.setFeatureSetVisible(setId, visible);
        } catch(DataStoreException ignored) {}
    }

    @Override
    protected synchronized void setFeatureSetsVisibleImpl(FeatureSetQueryParameters params, boolean visible)
    {
        try {
            _impl.setFeatureSetsVisible(Adapters.adapt(params, null), visible);
        } catch(DataStoreException ignored) {}
    }

    @Override
    protected void beginBulkModificationImpl()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void endBulkModificationImpl(boolean successful)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteAllFeatureSetsImpl()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected FeatureSet insertFeatureSetImpl(String provider, String type, String name, double minResolution, double maxResolution, boolean returnRef)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureSetImpl(long fsid, String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureSetImpl(long fsid, double minResolution, double maxResolution)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureSetImpl(long fsid, String name, double minResolution, double maxResolution)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteFeatureSetImpl(long fsid)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Feature insertFeatureImpl(long fsid, String name, Geometry geom, Style style, AttributeSet attributes, boolean returnRef)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureImpl(long fid, String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureImpl(long fid, Geometry geom)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureImpl(long fid, Style style)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureImpl(long fid, AttributeSet attributes)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFeatureImpl(long fid, String name, Geometry geom, Style style, AttributeSet attributes)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteFeatureImpl(long fsid)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteAllFeaturesImpl(long fsid)
    {
        throw new UnsupportedOperationException();
    }
}
