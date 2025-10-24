package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;

public class FeatureSetDatabase2Tests extends FeatureDataStore4Tests
{
    @Override
    protected FeatureDataStore4 createDataStore() {
        return new FeatureSetDatabase2(null);
    }
}
