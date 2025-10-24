package gov.tak.api.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.util.function.Function;

public final class Transmute {
    private Transmute() {}

    public static <T, V> Collection<V> collection(Collection<T> source, Function<T, V> transmute) {
        return collection(source, transmute, null);
    }
    public static <T, V> Collection<V> collection(Collection<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new TransmuteCollection<>(source, forward, inverse);
    }
    public static <T, V> Set<V> set(Set<T> source, Function<T, V> transmute) {
        return set(source, transmute, null);
    }
    public static <T, V> Set<V> set(Set<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new TransmuteCollection.S<>(source, forward, inverse);
    }
    public static <T, V> List<V> list(List<T> source, Function<T, V> transmute) {
        return list(source, transmute, null);
    }
    public static <T, V> List<V> list(List<T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new TransmuteCollection.L<>(source, forward, inverse);
    }
    public static <K, T, V> Map<K, V> map(Map<K, T> source, Function<T, V> transmute) {
        return map(source, transmute, null);
    }
    public static <K, T, V> Map<K, V> map(Map<K, T> source, Function<T, V> forward, Function<V, T> inverse) {
        return new TransmuteMap<>(source, forward, inverse);
    }
}
