package gov.tak.api.engine.map.features;

import java.lang.reflect.Array;
import java.util.Collection;

import gov.tak.api.annotation.NonNull;

public final class GeometryCollection extends Geometry
{
    private Children geometries;

    GeometryCollection(com.atakmap.map.layer.feature.geometry.GeometryCollection implementation)
    {
        super(implementation);
        geometries = new Children();
    }

    public GeometryCollection(int dimension)
    {
        this(new com.atakmap.map.layer.feature.geometry.GeometryCollection(dimension));
    }

    com.atakmap.map.layer.feature.geometry.GeometryCollection getImpl() {
        return (com.atakmap.map.layer.feature.geometry.GeometryCollection)impl;
    }

    public Geometry addGeometry(@NonNull Geometry geometry)
    {
        if (geometry == null)
            throw new IllegalArgumentException("geometry is null");

        return create(getImpl().addGeometry(geometry.impl));
    }

    public void removeGeometry(Geometry geometry)
    {
        if (geometry != null)
            getImpl().removeGeometry(geometry.impl);
    }

    public Collection<Geometry> getGeometries()
    {
        return this.geometries;
    }

    private class Children implements Collection<Geometry>
    {
        @Override
        public boolean add(Geometry object)
        {
            return getImpl().getGeometries().add(object.impl);
        }

        @Override
        public boolean addAll(Collection<? extends Geometry> collection)
        {
            // based on knowing underlying impl, this is probably best approach
            boolean result = false;
            for (Geometry geom : collection) {
                result |= getImpl().getGeometries().add(geom.impl);
            }
            return result;
        }

        @Override
        public void clear()
        {
            getImpl().getGeometries().clear();
        }

        @Override
        public boolean contains(Object object)
        {
            // Geometry.equals compares true for both types
            return getImpl().getGeometries().contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            // Geometry.equals compares true for both types
            return getImpl().getGeometries().containsAll(collection);
        }

        @Override
        public boolean isEmpty()
        {
            return getImpl().getGeometries().isEmpty();
        }

        @Override
        public java.util.Iterator<Geometry> iterator()
        {
            return new Iterator();
        }

        @Override
        public boolean remove(Object object)
        {
            // Geometry.equals compares true for both types
            return getImpl().getGeometries().remove(object);
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            // Geometry.equals compares true for both types
            return getImpl().getGeometries().removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection)
        {
            // Geometry.equals compares true for both types
            return getImpl().getGeometries().retainAll(collection);
        }

        @Override
        public int size()
        {
            return getImpl().getGeometries().size();
        }

        @Override
        public Object[] toArray()
        {
            return this.toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] array)
        {
            com.atakmap.map.layer.feature.geometry.Geometry[] geomImpls = getImpl().getGeometries().toArray(new com.atakmap.map.layer.feature.geometry.Geometry[0]);
            final int size = geomImpls.length;
            if (array.length < size)
                array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);

            int i = 0;
            for (i = 0; i < size; i++)
                array[i] = (T) create(geomImpls[i]);
            for (; i < array.length; i++)
                array[i] = null;

            // Client code shouldn't expect reference equality.
            return array;
        }

        class Iterator implements java.util.Iterator<Geometry>
        {

            java.util.Iterator<com.atakmap.map.layer.feature.geometry.Geometry> iterImpl;

            @Override
            public boolean hasNext()
            {
                return iterImpl.hasNext();
            }

            @Override
            public Geometry next()
            {
                com.atakmap.map.layer.feature.geometry.Geometry geomImpl = iterImpl.next();

                // Client code shouldn't expect reference equality.
                return create(geomImpl);
            }

            @Override
            public void remove()
            {
                iterImpl.remove();
            }
        }
    }
}
