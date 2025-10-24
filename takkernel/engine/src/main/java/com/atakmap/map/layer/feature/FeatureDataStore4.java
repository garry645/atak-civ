package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;

import java.lang.annotation.Native;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface FeatureDataStore4 extends FeatureDataStore3
{
    @Native
    public final static int PROPERTY_FEATURE_TRAITS = 0x40;

    void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, int attrUpdateType, Feature.Traits traits) throws DataStoreException;
}
