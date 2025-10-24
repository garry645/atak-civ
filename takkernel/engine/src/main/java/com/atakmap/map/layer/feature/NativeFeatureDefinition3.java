package com.atakmap.map.layer.feature;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class NativeFeatureDefinition3 implements FeatureDefinition3
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(FeatureDefinition3.class);

    Pointer pointer;
    final ReadWriteLock rwlock = new ReadWriteLock();
    final Object owner;

    NativeFeatureDefinition3(Pointer pointer, Object owner) {
        this.pointer = pointer;
        this.owner = owner;
        NativePeerManager.register(this, this.pointer, this.rwlock, null, CLEANER);
    }

    @Override
    public Object getRawGeometry()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final Object rawGeom = getRawGeometry(this.pointer.raw);
            switch (getGeomCoding())
            {
                case FeatureDefinition.GEOM_WKT:
                    return (String) rawGeom;
                case FeatureDefinition.GEOM_WKB:
                case FeatureDefinition.GEOM_SPATIALITE_BLOB:
                    return (byte[]) rawGeom;
                case FeatureDefinition.GEOM_ATAK_GEOMETRY:
                    return Interop.createGeometry((Pointer) rawGeom, this);
                default:
                    return null;
            }
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public int getGeomCoding()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final int cgeomCoding = getGeomCoding(this.pointer.raw);
            if (cgeomCoding == NativeFeatureDataSource.GeometryEncoding_GeomBlob)
                return FeatureDefinition.GEOM_SPATIALITE_BLOB;
            else if (cgeomCoding == NativeFeatureDataSource.GeometryEncoding_GeomGeom)
                return FeatureDefinition.GEOM_ATAK_GEOMETRY;
            else if (cgeomCoding == NativeFeatureDataSource.GeometryEncoding_GeomWkt)
                return FeatureDefinition.GEOM_WKT;
            else if (cgeomCoding == NativeFeatureDataSource.GeometryEncoding_GeomWkb)
                return FeatureDefinition.GEOM_WKB;
            else
                throw new IllegalStateException();
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public String getName()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getName(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public int getStyleCoding()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getStyleCoding(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public Object getRawStyle()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final Object rawStyle = getRawStyle(this.pointer.raw);
            if (rawStyle == null)
                return null;
            switch (getStyleCoding())
            {
                case FeatureDefinition.STYLE_OGR:
                    return (String) rawStyle;
                case FeatureDefinition.STYLE_ATAK_STYLE:
                    return Interop.createStyle((Pointer) rawStyle, this);
                default:
                    return null;
            }
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public AttributeSet getAttributes()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final Pointer cattrs = getAttributes(this.pointer.raw);
            if (cattrs == null)
                return null;
            return new AttributeSet(cattrs, this);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public Feature get()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();

            return new Feature(FeatureDataStore2.FEATURESET_ID_NONE,
                    FeatureDataStore2.FEATURE_ID_NONE,
                    getName(this.pointer.raw),
                    Feature.getGeometry(this),
                    Feature.getStyle(this),
                    this.getAttributes(),
                    getAltitudeMode(),
                    getExtrude(),
                    getTimestamp(this.pointer.raw),
                    FeatureDataStore2.FEATURE_VERSION_NONE);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public long getTimestamp()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getTimestamp(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public Feature.AltitudeMode getAltitudeMode()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            int val = getAltitudeMode(this.pointer.raw);
            return Feature.AltitudeMode.from(val);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public double getExtrude()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getExtrude(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    // interop
    static long getPointer(FeatureDefinition3 object) {
        return (object instanceof NativeFeatureDefinition3) ? ((NativeFeatureDefinition3)object).pointer.raw : 0L;
    }
    static native Pointer wrap(FeatureDefinition3 object);
    static boolean hasPointer(FeatureDefinition3 object) {
        return (object instanceof NativeFeatureDefinition3);
    }
    static FeatureDefinition3 create(Pointer pointer, Object ownerReference) {
        return new NativeFeatureDefinition3(pointer, ownerReference);
    }
    static native boolean hasObject(long pointer);
    static native FeatureDefinition3 getObject(long pointer);
    static native void destruct(Pointer pointer);

    // implementation
    static native Object getRawGeometry(long ptr);
    static native int getGeomCoding(long ptr);
    static native String getName(long ptr);
    static native int getStyleCoding(long ptr);
    static native Object getRawStyle(long ptr);
    static native Pointer getAttributes(long ptr);
    static native long getTimestamp(long ptr);
    static native int getAltitudeMode(long ptr);
    static native double getExtrude(long ptr);
}
