package com.atakmap.map.layer.feature;

import com.atakmap.interop.Pointer;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class NativeFeatureDefinition4 extends NativeFeatureDefinition3 implements FeatureDefinition4
{
    NativeFeatureDefinition4(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    @Override
    public Feature.Traits getTraits()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getTraits(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    static native Feature.Traits getTraits(long ptr);

    // Interop
    static long getPointer(FeatureDefinition4 object) {
        return (object instanceof NativeFeatureDefinition4) ? ((NativeFeatureDefinition4)object).pointer.raw : 0L;
    }
    static Pointer wrap(FeatureDefinition4 object){return NativeFeatureDefinition3.wrap(object);};
    static boolean hasPointer(FeatureDefinition4 object) {
        return (object instanceof NativeFeatureDefinition4);
    }
    static FeatureDefinition4 create(Pointer pointer, Object ownerReference) {
        return new NativeFeatureDefinition4(pointer, ownerReference);
    }
    static boolean hasObject(long pointer){ return NativeFeatureDefinition3.hasObject(pointer);};
    static FeatureDefinition4 getObject(long pointer)
    {
        FeatureDefinition3 featureDefinition = NativeFeatureDefinition3.getObject(pointer);
        if (featureDefinition instanceof FeatureDefinition4)
        {
            return ((FeatureDefinition4) featureDefinition);
        }
        return null;
    };
    static void destruct(Pointer pointer){NativeFeatureDefinition3.destruct(pointer);};
}
