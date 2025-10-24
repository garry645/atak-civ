package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;

public class FeatureDataStoreInteropTests extends FeatureDataStore2Tests
{
    @Override
    protected FeatureDataStore2 createDataStore() {
        com.atakmap.interop.Interop<FeatureDataStore2> interop = com.atakmap.interop.Interop.findInterop(FeatureDataStore2.class);
        return interop.create(interop.wrap(new FeatureSetDatabase2(null)));
    }
}
