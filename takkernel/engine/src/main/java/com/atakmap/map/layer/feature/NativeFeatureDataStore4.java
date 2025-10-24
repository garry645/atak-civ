package com.atakmap.map.layer.feature;

import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.MathUtils;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class NativeFeatureDataStore4 extends NativeFeatureDataStore2 implements FeatureDataStore4
{
    NativeFeatureDataStore4(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    @Override
    public void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, Feature.AltitudeMode altitudeMode, double extrude, int attrUpdateType) throws DataStoreException
    {
        Feature.Traits traits = new Feature.Traits();
        traits.altitudeMode = altitudeMode;
        traits.extrude = extrude;
        updateFeature(fid, updatePropertyMask, name, geometry, style, attributes, attrUpdateType, traits);
    }

    @Override
    public void updateFeature(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, int attrUpdateType, Feature.Traits traits) throws DataStoreException
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            int cupdatePropertyMask = 0;
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_ATTRIBUTES))
                cupdatePropertyMask |= getFIELD_ATTRIBUTES();
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_NAME))
                cupdatePropertyMask |= getFIELD_NAME();
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_GEOMETRY))
                cupdatePropertyMask |= getFIELD_GEOMETRY();
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_STYLE))
                cupdatePropertyMask |= getFIELD_STYLE();

            int cattrUpdateType = 0;
            switch (attrUpdateType)
            {
                case UPDATE_ATTRIBUTES_ADD_OR_REPLACE:
                    cattrUpdateType = getUPDATE_ATTRIBUTES_ADD_OR_REPLACE();
                    break;
                case UPDATE_ATTRIBUTES_SET:
                    cattrUpdateType = getUPDATE_ATTRIBUTES_SET();
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            updateFeature(this.pointer.raw,
                    fid,
                    cupdatePropertyMask,
                    name,
                    Interop.getRawPointer(geometry),
                    Interop.getRawPointer(style),
                    (attributes != null) ? attributes.pointer.raw : 0L,
                    cattrUpdateType,
                    traits);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /*************************************************************************/

    static long getPointer(FeatureDataStore4 managed)
    {
        if (managed instanceof NativeFeatureDataStore4)
            return ((NativeFeatureDataStore4) managed).pointer.raw;
        else
            return 0L;
    }

    static FeatureDataStore2 create(Pointer pointer, Object ownerRef)
    {
        return new NativeFeatureDataStore4(pointer, ownerRef);
    }

    static native Pointer wrap(FeatureDataStore4 object);
    static boolean hasPointer(FeatureDataStore4 object) {
        return (object instanceof NativeFeatureDataStore4);
    }

    static native void destruct(Pointer pointer);

    /*************************************************************************/

    static native void updateFeature(long pointer, long fid, int cupdatePropertyMask, String name, long geom, long style, long attrs, int cattrUpdateType, Feature.Traits traits);
}
