package gov.tak.api.engine.map.features;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.marshal.IMarshal;
import gov.tak.api.util.Disposable;

import com.atakmap.map.layer.feature.geometry.GeometryFactory;

import java.nio.ByteBuffer;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.platform.marshal.MarshalManager;

@DontObfuscate
public abstract class Geometry implements Disposable
{
    protected com.atakmap.map.layer.feature.geometry.Geometry impl;

    Geometry(@NonNull com.atakmap.map.layer.feature.geometry.Geometry implementation) {

        if (implementation == null)
            throw new IllegalArgumentException("implementation is null");

        impl = implementation;
    }

    /**
     * Sets the dimension of the geometry. A value of '2' represents 2D geometries (x,y coordinate
     * pairs); a value of '3' represents 3D geometries (x,y,z coordinate triplets).
     *
     * @param dimension The new dimension for the geometry; values
     *                  of '2' and '3' are supported.
     *
     * @throws IllegalArgumentException when dimension is out of range
     */
    public final void setDimension(int dimension)
    {
        if (dimension != 2 && dimension != 3)
            throw new IllegalArgumentException("dimension out of range");
        impl.setDimension(dimension);
    }

    /**
     * Returns the diemnsion of the geometry. A value of '2' represents 2D geometries (x,y
     * coordinate pairs); a value of '3' represents 3D geometries (x,y,z coordinate triplets).
     *
     * @return  The dimension of the geometry
     */
    public final int getDimension()
    {
        return impl.getDimension();
    }

    /**
     * Returns the bounding box enclosing the geometry. If the geometry is empty an envelope of all
     * NAN values shall be returned.
     *
     * <P>For 2D geometries, the 'minZ' and 'maxZ' values shall be
     * set to '0'.
     *
     * @return  An Envelope instance.
     */
    public final Envelope getEnvelope()
    {
        com.atakmap.map.layer.feature.geometry.Envelope envImpl = impl.getEnvelope();
        return new Envelope(envImpl.minX, envImpl.minY, envImpl.minZ, envImpl.maxX,
                envImpl.maxY, envImpl.maxZ);
    }

    @Override
    public final void dispose()
    {
        impl.dispose();
    }

    @Override
    public final boolean equals(Object o)
    {
        com.atakmap.map.layer.feature.geometry.Geometry otherImpl = null;
        if (o instanceof com.atakmap.map.layer.feature.geometry.Geometry)
            otherImpl = (com.atakmap.map.layer.feature.geometry.Geometry)o;
        else if (o instanceof Geometry)
            otherImpl = ((Geometry)o).impl;

        if (otherImpl == null)
            return false;

        return impl.equals(otherImpl);
    }

    @Override
    public final int hashCode()
    {
        return impl.hashCode();
    }

    /**
     * Creates a copy of the specified geometry.
     */
    @Override
    public final Geometry clone()
    {
        return create(impl.clone());
    }

    // Would like to avoid making public, but see comments in marshal section below
    public static Geometry create(com.atakmap.map.layer.feature.geometry.Geometry implementation)
    {
        if (implementation == null)
            return null;

        if (implementation instanceof com.atakmap.map.layer.feature.geometry.Point) {
            return new Point((com.atakmap.map.layer.feature.geometry.Point)implementation);
        } else if (implementation instanceof com.atakmap.map.layer.feature.geometry.GeometryCollection) {
            return new GeometryCollection((com.atakmap.map.layer.feature.geometry.GeometryCollection)implementation);
        } else if (implementation instanceof com.atakmap.map.layer.feature.geometry.LineString) {
            return new LineString((com.atakmap.map.layer.feature.geometry.LineString)implementation);
        } else if (implementation instanceof com.atakmap.map.layer.feature.geometry.Polygon) {
            return new Polygon((com.atakmap.map.layer.feature.geometry.Polygon)implementation);
        } else {
            throw new IllegalStateException();
        }
    }

    // Marshal API -> IMPL
    // IMPL -> API is handled in com.atakmap.map.layer.feature.geometry.Geometry because an instance
    // of the class is required for static initializer to run.

    private static class ToImplMarshal implements IMarshal {

        @Override
        public <T, V> T marshal(V in) {
            Geometry geom = (Geometry) in;
            if (geom == null)
                return null;
            return (T)geom.impl;
        }
    }

    static {
        MarshalManager.registerMarshal(new ToImplMarshal(), Geometry.class, com.atakmap.map.layer.feature.geometry.Geometry.class);
    }
}
