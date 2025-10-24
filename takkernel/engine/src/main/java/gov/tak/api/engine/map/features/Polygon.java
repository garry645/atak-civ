package gov.tak.api.engine.map.features;

import java.lang.reflect.Array;
import java.util.Collection;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.annotation.NonNull;

/**
 * An arbitrary shape made of a exterior ring which is a closed line string. Optional interior
 * rings which are closed line strings can represent "holes" inside in the shape.
 *
 * <P>Note: The polygon is malformed if any of its rings are not closed.</P>
 */
@DontObfuscate
public final class Polygon extends Geometry
{
    private final InteriorRings interiorRings;
    private LineString exteriorRing;

    Polygon(com.atakmap.map.layer.feature.geometry.Polygon implementation)
    {
        super(implementation);
        interiorRings = new InteriorRings();
    }

    /**
     * Creates a new polygon with a default dimension of '2'.
     */
    public Polygon()
    {
        this(2);
    }

    /**
     * Creates a new polygon with a specified dimension of '2' or '3'.
     *
     * @param dimension the dimension of '2' or '3'
     * @throws IllegalArgumentException if dimension is illegal
     */
    public Polygon(int dimension)
    {
        this(new com.atakmap.map.layer.feature.geometry.Polygon(dimension));
    }

    /**
     * Creates a new polygon with its exterior ring initialized as a copy of 'exteriorRing'. The
     * dimension of the polygon will be adopted from 'exteriorRing'.
     *
     * @param exteriorRing the exterior ring line string
     * @throws IllegalArgumentException if ring is malformed
     */
    public Polygon(LineString exteriorRing)
    {
        this(exteriorRing.getDimension());
        this.setExteriorRing(exteriorRing);
    }

    /**
     * Creates a new polygon with its exterior ring initialized as a copy of 'exteriorRing' and
     * interior rings initialized as a copy of interiorRings. The dimension of the polygon will be
     * adopted from 'exteriorRing'.
     *
     * @param exteriorRing the exterior ring line string
     * @param interiorRings the interior rings of the polygon
     *
     * @throws IllegalArgumentException if any ring is malformed
     */
    public Polygon(LineString exteriorRing, Collection<LineString> interiorRings)
    {
        this(exteriorRing);
        this.getInteriorRings().addAll(interiorRings);
    }

    com.atakmap.map.layer.feature.geometry.Polygon getImpl() {
        return (com.atakmap.map.layer.feature.geometry.Polygon)impl;
    }

    /**
     * Set the exterior ring of the polygon
     *
     * @param ring the exterior ring
     * @throws IllegalArgumentException if ring is malformed
     */
    public void setExteriorRing(@NonNull LineString ring)
    {
        if (ring == null)
            throw new NullPointerException();
        checkMalformedRing(ring);

        if (exteriorRing == null) {
            getImpl().addRing((com.atakmap.map.layer.feature.geometry.LineString) ring.impl);
            exteriorRing = ring;
        }
    }

    /**
     * Returns the exterior ring.
     */
    public LineString getExteriorRing()
    {
        com.atakmap.map.layer.feature.geometry.LineString extRing = getImpl().getExteriorRing();
        if (extRing == null)
            return null;
        return new LineString(extRing);
    }

    /**
     * Add an interior ring
     *
     * @param ring the ring
     * @throws IllegalArgumentException if ring is malformed
     */
    public void addInteriorRing(LineString ring)
    {
        checkMalformedRing(ring);
        getInteriorRings().add(ring);
    }

    /**
     * Resets the polygon to an empty polygon. All points in the exterior ring are removed and all
     * interior rings are removed.
     */
    public void clear()
    {
        getImpl().clear();
    }

    /**
     * Returns the interior rings collections
     */
    public Collection<LineString> getInteriorRings()
    {
        return interiorRings;
    }

    private class InteriorRings implements Collection<LineString>
    {
        @Override
        public boolean add(LineString object)
        {
            checkMalformedRing(object);
            return getImpl().getInteriorRings().add(object.getImpl());
        }

        @Override
        public boolean addAll(Collection<? extends LineString> collection)
        {
            boolean result = false;
            for (LineString ring : collection)
                result |= this.add(ring);
            return result;
        }

        @Override
        public void clear()
        {
            getImpl().getInteriorRings().clear();
        }

        @Override
        public boolean contains(Object object)
        {
            // Geometry.equals will marshal
            return getImpl().getInteriorRings().contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            // Geometry.equals will marshal
            return getImpl().getInteriorRings().contains(collection);
        }

        @Override
        public boolean isEmpty()
        {
            return (this.size() == 0);
        }

        @Override
        public java.util.Iterator<LineString> iterator()
        {
            return new Iterator();
        }

        @Override
        public boolean remove(Object object)
        {
            // Geometry.equals will marshal
            return getImpl().getInteriorRings().remove(object);
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            // Geometry.equals will marshal
            return getImpl().getInteriorRings().removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection)
        {
            // Geometry.equals will marshal
            return getImpl().getInteriorRings().retainAll(collection);
        }

        @Override
        public int size()
        {
            return getImpl().getInteriorRings().size();
        }

        @Override
        public Object[] toArray()
        {
            return this.toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] array)
        {
            com.atakmap.map.layer.feature.geometry.Geometry[] geomImpls = getImpl().getInteriorRings().toArray(new com.atakmap.map.layer.feature.geometry.Geometry[0]);

            final int size = this.size();
            if (array.length < size)
                array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);

            int i = 0;
            for (i = 0; i < size; i++)
                array[i] = (T) create(geomImpls[i]);
            for (; i < array.length; i++)
                array[i] = null;
            return array;
        }

        class Iterator implements java.util.Iterator<LineString>
        {
            java.util.Iterator<com.atakmap.map.layer.feature.geometry.LineString> iterImpl;

            Iterator()
            {
                iterImpl = getImpl().getInteriorRings().iterator();
            }

            @Override
            public boolean hasNext()
            {
                return iterImpl.hasNext();
            }

            @Override
            public LineString next()
            {
                com.atakmap.map.layer.feature.geometry.LineString lineStringImpl = iterImpl.next();
                return new LineString(lineStringImpl);
            }

            @Override
            public void remove()
            {
               iterImpl.remove();
            }
        }
    }

    private static void checkMalformedRing(LineString ring) {
        if (!ring.isClosed())
            throw new IllegalArgumentException("Polygon rings must be closed");
    }
}
