package gov.tak.api.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

import gov.tak.api.util.function.Function;

class TransmuteCollection<T, V> implements Collection<V> {

    final Collection<T> impl;
    final Function<T, V> forward;
    final Function<V, T> inverse;

    TransmuteCollection(Collection<T> impl, Function<T, V> transmute) {
        this(impl, transmute, null);
    }

    TransmuteCollection(Collection<T> impl, Function<T, V> forward, Function<V, T> inverse) {
        this.impl = impl;
        this.forward = forward;
        this.inverse = inverse;
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if(this.inverse != null) {
            return impl.contains(inverse.apply((V)o));
        } else {
            for(T t : impl)
                if(Objects.equals(o, forward.apply(t)))
                    return true;
        }

        return false;
    }

    @Override
    public Iterator<V> iterator() {
        return new TransmuteIterator<T, V>(impl.iterator(), forward);
    }

    @Override
    public Object[] toArray() {
        Object[] array = impl.toArray();
        for(int i = 0; i < array.length; i++)
            array[i] = forward.apply((T)array[i]);
        return array;
    }

    @Override
    public <Ta> Ta[] toArray(Ta[] ts) {
        return null;
    }

    @Override
    public boolean add(V v) {
        if(inverse == null)
            throw new UnsupportedOperationException();
        return impl.add(inverse.apply(v));
    }

    @Override
    public boolean remove(Object o) {
        if(inverse != null) {
            return impl.remove(inverse.apply((V)o));
        } else {
            Iterator<V> iter = iterator();
            while(iter.hasNext()) {
                if(Objects.equals(o, iter.next())) {
                    iter.remove();
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        boolean v = true;
        for(Object o : collection)
            v &= contains(o);
        return v;
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
        boolean v = false;
        for(V o : collection)
            v |= add(o);
        return v;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean v = false;
        for(Object o : collection)
            v |= remove(o);
        return v;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        final int size = size();
        Iterator<V> iter = iterator();
        while(iter.hasNext()) {
            if(!collection.contains(iter.next()))
                iter.remove();
        }
        return size != size();
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    final static class S<T, V> extends TransmuteCollection<T, V> implements Set<V> {
        S(Set<T> impl, Function<T, V> forward, Function<V, T> inverse) {
            super(impl, forward, inverse);
        }
    }

    final static class L<T, V> extends TransmuteCollection<T, V> implements List<V> {
        final List<T> impl;

        L(List<T> impl, Function<T, V> forward, Function<V, T> inverse) {
            super(impl, forward, inverse);
            this.impl = impl;
        }

        @Override
        public boolean addAll(int i, Collection<? extends V> collection) {
            return false;
        }

        @Override
        public V get(int i) {
            return null;
        }

        @Override
        public V set(int i, V v) {
            return null;
        }

        @Override
        public void add(int i, V v) {

        }

        @Override
        public V remove(int i) {
            return forward.apply(impl.remove(i));
        }

        @Override
        public int indexOf(Object o) {
            if(inverse != null) {
                return impl.indexOf(inverse.apply((V)o));
            } else {
                int i = 0;
                Iterator<V> iter = iterator();
                while(iter.hasNext()) {
                    if(Objects.equals(o, iter.next()))
                        return i;
                    i++;
                }
                return -1;
            }
        }

        @Override
        public int lastIndexOf(Object o) {
            if(inverse != null) {
                return impl.lastIndexOf(inverse.apply((V)o));
            } else {
                int index = -1;
                int i = 0;
                Iterator<V> iter = iterator();
                while(iter.hasNext()) {
                    if(Objects.equals(o, iter.next())) {
                        index = i;
                    }
                    i++;
                }
                return index;
            }
        }

        @Override
        public ListIterator<V> listIterator() {
            return new TransmuteListIterator<T, V>(impl.listIterator(), forward, inverse);
        }

        @Override
        public ListIterator<V> listIterator(int i) {
            return new TransmuteListIterator<T, V>(impl.listIterator(i), forward, inverse);
        }

        @Override
        public List<V> subList(int a, int b) {
            return new L<T, V>(impl.subList(a, b), forward, inverse);
        }
    }

    public static <T, V> Collection<V> collection(Collection<T> source, Function<T, V> transmute) {
        return collection(source, null);
    }
    public static <T, V> Collection<V> collection(Collection<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new TransmuteCollection<>(source, forward, inverse);
    }
    public static <T, V> Set<V> set(Set<T> source, Function<T, V> transmute) {
        return set(source, transmute, null);
    }
    public static <T, V> Set<V> set(Set<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new S<>(source, forward, inverse);
    }
    public static <T, V> List<V> list(List<T> source, Function<T, V> transmute) {
        return list(source, transmute, null);
    }
    public static <T, V> List<V> list(List<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new L<>(source, forward, inverse);
    }
}
