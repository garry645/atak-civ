package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.util.Disposer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public abstract class FeatureDataStore2Tests extends KernelJniTest
{
    protected abstract FeatureDataStore2 createDataStore();
    
    protected void assumeModifcationFlags(FeatureDataStore2 datastore, int flags) {
        Assume.assumeTrue((datastore.getModificationFlags()&flags)==flags);
    }

    @Test
    public void insertFeatureSet_explicit_id_roundtrip() throws DataStoreException
    {
        insertFeatureSet_roundtrip(123456789L);
    }

    @Test
    public void insertFeatureSet_implicit_id_roundtrip() throws DataStoreException
    {
        insertFeatureSet_roundtrip(FeatureDataStore2.FEATURESET_ID_NONE);
    }

    public void insertFeatureSet_roundtrip(long requestedFsid) throws DataStoreException
    {
        final FeatureDataStore2 datastore = createDataStore();
        try(Disposer disposer = new Disposer(datastore))
        {
            Assume.assumeTrue((requestedFsid == FeatureDataStore2.FEATURESET_ID_NONE) || datastore.supportsExplicitIDs());

            assumeModifcationFlags(datastore,
                    FeatureDataStore2.MODIFY_FEATURESET_INSERT);
            FeatureSet toInsert = new FeatureSet(requestedFsid, "a", "b", "c", 67890.0d, 12345.0d, FeatureDataStore2.FEATURESET_VERSION_NONE);
            long insertedFsid = datastore.insertFeatureSet(toInsert);
            Assert.assertNotEquals(insertedFsid, FeatureDataStore2.FEATURESET_ID_NONE);
            if(requestedFsid != FeatureDataStore2.FEATURESET_ID_NONE)
                Assert.assertEquals(requestedFsid, insertedFsid);

            FeatureSet inserted = Utils.getFeatureSet(datastore, insertedFsid);
            Assert.assertNotNull(inserted);

            Assert.assertEquals(insertedFsid, inserted.getId());
            Assert.assertEquals(toInsert.getProvider(), inserted.getProvider());
            Assert.assertEquals(toInsert.getName(), inserted.getName());
            Assert.assertEquals(toInsert.getType(), inserted.getType());
            Assert.assertTrue(inserted.getMinResolution() >= inserted.getMinResolution());
            Assert.assertTrue(inserted.getMaxResolution() <= inserted.getMaxResolution());
            Assert.assertNotEquals(inserted.getVersion(), FeatureDataStore2.FEATURESET_VERSION_NONE);
        }
    }
    
    @Test
    public void insertFeature_Feature_explicit_id_roundtrip() throws DataStoreException
    {
        insertFeature_Feature_roundtrip(111222333L);
    }

    @Test
    public void insertFeature_Feature_implicit_id_roundtrip() throws DataStoreException
    {
        insertFeature_Feature_roundtrip(FeatureDataStore2.FEATURE_ID_NONE);
    }

    protected void insertFeature_Feature_roundtrip(long requestedFid) throws DataStoreException
    {
        final FeatureDataStore2 datastore = createDataStore();
        try(Disposer disposer = new Disposer(datastore))
        {
            Assume.assumeTrue((requestedFid == FeatureDataStore2.FEATURESET_ID_NONE) || datastore.supportsExplicitIDs());

            assumeModifcationFlags(datastore,
                    FeatureDataStore2.MODIFY_FEATURESET_INSERT|
                          FeatureDataStore2.MODIFY_FEATURESET_FEATURE_INSERT);

            FeatureSet fs = new FeatureSet("a", "b", "c", 67890.0d, 12345.0d);
            long fsid = datastore.insertFeatureSet(fs);
            Assert.assertNotEquals(fsid, FeatureDataStore2.FEATURESET_ID_NONE);

            // insert feature
            Feature toInsert = new Feature(
                    fsid,
                    requestedFid,
                    "afeature",
                    new Point(10, 9, 8),
                    new IconPointStyle(0xFF005599, "iconpath"),
                    new AttributeSet(),
                    Feature.AltitudeMode.ClampToGround,
                    0d,
                    FeatureDataStore2.TIMESTAMP_NONE,
                    FeatureDataStore2.FEATURE_VERSION_NONE
            );
            long insertedFid = datastore.insertFeature(toInsert);

            // verify feature
            Assert.assertNotEquals(insertedFid, FeatureDataStore2.FEATURE_ID_NONE);
            if(requestedFid != FeatureDataStore2.FEATURE_ID_NONE)
                Assert.assertEquals(insertedFid, requestedFid);

            Feature inserted = Utils.getFeature(datastore, insertedFid);
            Assert.assertNotNull(inserted);

            Assert.assertEquals(fsid, inserted.getFeatureSetId());
            Assert.assertEquals(insertedFid, inserted.getId());
            Assert.assertEquals(toInsert.getName(), inserted.getName());
            Assert.assertEquals(toInsert.getGeometry(), inserted.getGeometry());
            if(datastore instanceof FeatureDataStore3) {
                Assert.assertEquals(toInsert.getAltitudeMode(), inserted.getAltitudeMode());
                Assert.assertEquals(toInsert.getExtrude(), inserted.getExtrude(), 0d);
            }
            Assert.assertEquals(toInsert.getStyle(), inserted.getStyle());
            Assert.assertEquals(toInsert.getAttributes(), inserted.getAttributes());
            if(datastore.hasTimeReference())
                Assert.assertEquals(toInsert.getTimestamp(), inserted.getTimestamp());
        }
    }

    @Test
    public void insertFeature_FeatureDefinition_roundtrip() throws DataStoreException
    {
        final FeatureDataStore2 datastore = createDataStore();
        try(Disposer disposer = new Disposer(datastore))
        {
            assumeModifcationFlags(datastore,
                    FeatureDataStore2.MODIFY_FEATURESET_INSERT|
                            FeatureDataStore2.MODIFY_FEATURESET_FEATURE_INSERT);

            FeatureSet fs = new FeatureSet("a", "b", "c", 67890.0d, 12345.0d);
            long fsid = datastore.insertFeatureSet(fs);
            Assert.assertNotEquals(fsid, FeatureDataStore2.FEATURESET_ID_NONE);

            // XXX - insert feature
            // XXX - verify feature
        }
    }
}
