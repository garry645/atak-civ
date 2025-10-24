package gov.tak.api.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import gov.tak.api.util.function.Function;

class TransmuteMap<K, T, V> implements Map<K, V> {
    final Map<K, T> impl;
    final Function<T, V> forward;
    final Function<V, T> inverse;

    TransmuteMap(Map<K, T> impl, Function<T, V> forward, Function<V, T> inverse) {
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
    public boolean containsKey(Object o) {
        return impl.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        if(inverse != null) {
            return impl.containsValue(inverse.apply((V)o));
        } else {
            for(V v : values()) {
                if(Objects.equals(o, v))
                    return true;
            }
            return false;
        }
    }

    @Override
    public V get(Object o) {
        return forward.apply(impl.get(o));
    }

    @Override
    public V put(K k, V v) {
        if(v == null)
            return forward.apply(impl.put(k, null));
        else if(inverse != null)
            return forward.apply(impl.put(k, inverse.apply(v)));
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object o) {
        return forward.apply(impl.remove(o));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for(Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public Set<K> keySet() {
        return impl.keySet();
    }

    @Override
    public Collection<V> values() {
        return Transmute.collection(impl.values(), forward);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Transmute.set(
                impl.entrySet(),
                new Function<Entry<K, T>, Entry<K, V>>() {
                    @Override
                    public Entry<K, V> apply(Entry<K, T> arg) {
                        return new TransmuteEntry(arg);
                    }
                });
    }

    class TransmuteEntry implements Map.Entry<K, V> {

        final Entry<K, T> impl;

        TransmuteEntry(Entry<K, T> impl) {
            this.impl = impl;
        }

        @Override
        public K getKey() {
            return impl.getKey();
        }

        @Override
        public V getValue() {
            return forward.apply(impl.getValue());
        }

        @Override
        public V setValue(V v) {
            if(inverse == null)
                throw new UnsupportedOperationException();
            return forward.apply(impl.setValue(inverse.apply(v)));
        }
    }
}
