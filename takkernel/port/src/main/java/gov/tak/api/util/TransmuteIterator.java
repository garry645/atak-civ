package gov.tak.api.util;

import java.util.Iterator;

import gov.tak.api.util.function.Function;

class TransmuteIterator<T, V> implements Iterator<V> {
    final Iterator<T> impl;
    final Function<T, V> transmute;

    public TransmuteIterator(Iterator<T> impl, Function<T, V> transmute) {
        this.impl = impl;
        this.transmute = transmute;
    }

    @Override
    public boolean hasNext() {
        return impl.hasNext();
    }

    @Override
    public V next() {
        return transmute.apply(impl.next());
    }

    @Override
    public void remove() {
        impl.remove();
    }
}
