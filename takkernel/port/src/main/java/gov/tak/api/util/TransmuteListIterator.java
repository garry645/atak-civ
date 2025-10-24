package gov.tak.api.util;

import java.util.Iterator;
import java.util.ListIterator;

import gov.tak.api.util.function.Function;

public class TransmuteListIterator<T, V> extends TransmuteIterator<T, V> implements ListIterator<V> {
    final ListIterator<T> impl;
    final Function<T, V> forward;
    final Function<V, T> inverse;

    public TransmuteListIterator(ListIterator<T> impl, Function<T, V> forward, Function<V, T> inverse) {
        super(impl, forward);
        this.impl = impl;
        this.forward = forward;
        this.inverse = inverse;
    }

    @Override
    public boolean hasPrevious() {
        return impl.hasPrevious();
    }

    @Override
    public V previous() {
        return forward.apply(impl.previous());
    }

    @Override
    public int nextIndex() {
        return impl.nextIndex();
    }

    @Override
    public int previousIndex() {
        return impl.previousIndex();
    }

    @Override
    public void set(V v) {
        if(inverse == null)
            throw new UnsupportedOperationException();
        impl.set(inverse.apply(v));
    }

    @Override
    public void add(V v) {
        if(inverse == null)
            throw new UnsupportedOperationException();
        impl.add(inverse.apply(v));
    }
}
