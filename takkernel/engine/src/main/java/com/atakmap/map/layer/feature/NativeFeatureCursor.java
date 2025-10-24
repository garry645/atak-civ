package com.atakmap.map.layer.feature;

import com.atakmap.coremap.log.Log;
import com.atakmap.interop.Pointer;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class NativeFeatureCursor implements FeatureCursor, FeatureDefinition4
{
    private final static String TAG = "NativeFeatureCursor";

    final ReadWriteLock rwlock = new ReadWriteLock();
    Pointer pointer;
    Object owner;

    NativeFeatureCursor(Pointer pointer, Object owner)
    {
        this.pointer = pointer;
        this.owner = owner;
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
            final int cstyleCoding = getStyleCoding(this.pointer.raw);
            if (cstyleCoding == NativeFeatureDataSource.StyleEncoding_StyleOgr)
                return FeatureDefinition.STYLE_OGR;
            else if (cstyleCoding == NativeFeatureDataSource.StyleEncoding_StyleStyle)
                return FeatureDefinition.STYLE_ATAK_STYLE;
            else
                throw new IllegalStateException();
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

            return new Feature(getFsid(this.pointer.raw),
                    getId(this.pointer.raw),
                    getName(this.pointer.raw),
                    Feature.getGeometry(this),
                    Feature.getStyle(this),
                    this.getAttributes(),
                    getAltitudeMode(),
                    getExtrude(),
                    getTimestamp(this.pointer.raw),
                    getVersion(this.pointer.raw));
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public long getId()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getId(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public long getVersion()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getVersion(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public long getFsid()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getFsid(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public boolean moveToNext()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return moveToNext(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public void close()
    {
        this.rwlock.acquireWrite();
        try
        {
            if (this.pointer.raw != 0L)
                destruct(this.pointer);
        } finally
        {
            this.rwlock.releaseWrite();
        }
    }

    @Override
    public boolean isClosed()
    {
        this.rwlock.acquireRead();
        try
        {
            return (this.pointer.raw == 0L);
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

    @Override
    protected void finalize()
    {
        if (this.pointer.raw != 0L)
            Log.w(TAG, "Native FeatureCursor leaked");
    }

    static long getPointer(FeatureCursor object) {
        return (object instanceof NativeFeatureCursor) ? ((NativeFeatureCursor)object).pointer.raw : 0L;
    }
    static native Pointer wrap(FeatureCursor object);
    static boolean hasPointer(FeatureCursor object) {
        return (object instanceof NativeFeatureCursor);
    }
    static FeatureCursor create(Pointer pointer, Object ownerReference) {
        return new NativeFeatureCursor(pointer, ownerReference);
    }
    static native boolean hasObject(long pointer);
    static native FeatureCursor getObject(long pointer);
    static native void destruct(Pointer pointer);

    static long castToPointer(Class<?> as, FeatureCursor object) {
        if (as == null)
            return 0L;
        else if(!(object instanceof NativeFeatureCursor))
            return 0L;
        else if (as.equals(FeatureDefinition3.class))
            return asFeatureDefinition(((NativeFeatureCursor)object).pointer.raw);
        else
            return 0L;
    }
    static native long asFeatureDefinition(long ptr);

    static native Object getRawGeometry(long ptr);

    static native int getGeomCoding(long ptr);

    static native String getName(long ptr);

    static native int getStyleCoding(long ptr);

    static native Object getRawStyle(long ptr);

    static native Pointer getAttributes(long ptr);

    static native long getId(long ptr);

    static native long getVersion(long ptr);

    static native long getFsid(long ptr);

    static native boolean moveToNext(long ptr);

    static native long getTimestamp(long ptr);

    static native int getAltitudeMode(long ptr);

    static native double getExtrude(long ptr);

    static native Feature.Traits getTraits(long ptr);
}
