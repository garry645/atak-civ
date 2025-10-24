package com.atakmap.map.projection;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.math.PointD;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate final class NativeProjection implements Projection
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(Projection.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    Pointer pointer;
    Object owner;

    int spatialReferenceId;
    boolean is3D;

    private NativeProjection(Pointer pointer)
    {
        this(pointer, null);
    }

    NativeProjection(Pointer pointer, Object owner)
    {
        NativePeerManager.register(this, pointer, this.rwlock, null, CLEANER);

        this.pointer = pointer;
        this.owner = owner;
        this.spatialReferenceId = getSrid(pointer.raw);
        this.is3D = is3D(pointer.raw);
    }

    @Override
    public PointD forward(GeoPoint g, PointD p)
    {
        if (p == null)
            p = new PointD(0, 0);
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final boolean hae = (g.getAltitudeReference() == GeoPoint.AltitudeReference.HAE);
            return forward(this.pointer.raw, g.getLatitude(), g.getLongitude(), hae ? g.getAltitude() : Double.NaN, p) ?
                    p : null;
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public GeoPoint inverse(PointD p, GeoPoint g)
    {
        if (g == null || !g.isMutable())
            g = GeoPoint.createMutable();
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            final boolean hae = (g.getAltitudeReference() == GeoPoint.AltitudeReference.HAE);
            return inverse(this.pointer.raw, p.x, p.y, p.z, g) ? g : null;
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public int getSpatialReferenceID()
    {
        return this.spatialReferenceId;
    }

    @Override
    public boolean is3D()
    {
        return is3D;
    }

    @Override
    public double getMinLatitude()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getMinLatitude(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public double getMinLongitude()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getMinLongitude(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public double getMaxLatitude()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getMaxLatitude(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public double getMaxLongitude()
    {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getMaxLongitude(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    // Interop<Projection>
    static long getPointer(Projection obj)
    {
        if (obj instanceof NativeProjection)
        {
            NativeProjection impl = (NativeProjection) obj;
            impl.rwlock.acquireRead();
            try
            {
                return impl.pointer.raw;
            } finally
            {
                impl.rwlock.releaseRead();
            }
        } else
        {
            return 0L;
        }
    }

    static Projection create(Pointer pointer, Object owner)
    {
        if (isWrapper(pointer.raw))
            return unwrap(pointer.raw);
        else
            return new NativeProjection(pointer, owner);
    }

    static native void destruct(Pointer pointer);

    static native Pointer wrap(Projection managed);

    static native boolean isWrapper(long pointer);

    static native Projection unwrap(long pointer);

    static native int getSrid(long pointer);

    static native boolean is3D(long pointer);

    static native boolean forward(long pointer, double lat, double lng, double hae, PointD result);

    static native boolean inverse(long pointer, double x, double y, double z, GeoPoint result);

    static native double getMinLatitude(long pointer);

    static native double getMinLongitude(long pointer);

    static native double getMaxLatitude(long pointer);

    static native double getMaxLongitude(long pointer);

}
