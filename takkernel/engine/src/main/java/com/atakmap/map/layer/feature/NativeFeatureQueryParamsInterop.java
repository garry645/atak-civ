package com.atakmap.map.layer.feature;

import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;

class NativeFeatureQueryParamsInterop extends com.atakmap.interop.Interop<FeatureQueryParameters>
{
    @Override
    public long getPointer(FeatureQueryParameters obj)
    {
        return 0;
    }

    @Override
    public FeatureQueryParameters create(Pointer pointer, Object owner)
    {
        return null;
    }

    @Override
    public Pointer clone(long pointer)
    {
        return null;
    }

    @Override
    public Pointer wrap(FeatureQueryParameters object)
    {
        Pointer pointer = NativeFeatureDataStore2.FeatureQueryParameters_create();
        NativeFeatureDataStore2.FeatureQueryParameters_adapt(object, pointer.raw);
        return pointer;
    }

    @Override
    public void destruct(Pointer pointer)
    {
        NativeFeatureDataStore2.FeatureQueryParameters_destruct(pointer);
    }

    @Override
    public boolean hasObject(long pointer)
    {
        return false;
    }

    @Override
    public FeatureQueryParameters getObject(long pointer)
    {
        return null;
    }

    @Override
    public boolean hasPointer(FeatureQueryParameters object)
    {
        return false;
    }

    @Override
    public boolean supportsWrap()
    {
        return true;
    }

    @Override
    public boolean supportsClone()
    {
        return false;
    }

    @Override
    public boolean supportsCreate()
    {
        return false;
    }
}
