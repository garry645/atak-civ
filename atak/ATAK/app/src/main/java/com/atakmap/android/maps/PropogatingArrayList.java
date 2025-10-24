
package com.atakmap.android.maps;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import gov.tak.api.util.AttributeSet;

/**
 * {@link DefaultMetaDataHolder} returns this custom {@code ArrayList} in it's {@code getMetaStringArrayList()}
 * to ensure that changes made to the returned {@code ArrayList} are propogated back into it's backing
 * {@link AttributeSet}. This is necessary because {@link AttributeSet}s don't natively support
 * {@code ArrayLists}, only {@code String[]}s.
 *
 * @param <E>
 */
final class PropogatingArrayList<E extends String> extends ArrayList<E> {
    private final String _key;
    private final AttributeSet _attrs;

    PropogatingArrayList(String key, AttributeSet attrs,
            Collection<? extends E> originalCollection) {
        _key = key;
        _attrs = attrs;
        addAll(originalCollection);
    }

    @Override
    public E set(int idx, E element) {
        super.set(idx, element);
        propogateChanges();
        return element;
    }

    @Override
    public boolean add(E element) {
        super.add(element);
        propogateChanges();
        return true;
    }

    @Override
    public void add(int idx, E element) {
        super.add(idx, element);
        propogateChanges();
    }

    @Override
    public E remove(int idx) {
        E removed = super.remove(idx);
        propogateChanges();
        return removed;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = super.remove(o);
        propogateChanges();
        return removed;
    }

    @Override
    public void clear() {
        super.clear();
        propogateChanges();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean rv = super.addAll(c);
        propogateChanges();
        return rv;
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        propogateChanges();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean rv = super.removeAll(c);
        propogateChanges();
        return rv;
    }

    @Override
    public boolean addAll(int idx, Collection<? extends E> c) {
        boolean rv = super.addAll(idx, c);
        propogateChanges();
        return rv;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean rv = super.retainAll(c);
        propogateChanges();
        return rv;
    }

    @NonNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> rv = super.subList(fromIndex, toIndex);
        propogateChanges();
        return rv;
    }

    private void propogateChanges() {
        String[] newArr = this.toArray(new String[0]);
        _attrs.setAttribute(_key, newArr);
    }
}
