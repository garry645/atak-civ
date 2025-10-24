package com.atakmap.map;

import com.atakmap.interop.Pointer;
import com.atakmap.util.ReadWriteLock;

import java.util.HashMap;
import java.util.Map;

public final class NativeControlFactory {

    private static final Map<Class<?>, Spi<?>> spiByClass = new HashMap<>();
    private static final Map<String, Spi<?>> spiByName = new HashMap<>();

    private static final ReadWriteLock rwLock = new ReadWriteLock();

    public interface Spi<T extends MapControl> {
        T createNativeControl(Pointer pointer, Object owner);
    }

    public static <T extends MapControl> void registerSpi(Class<?> clazz, Spi<T> spi) {
        registerSpi(clazz, clazz.getSimpleName(), spi);
    }

    public static <T extends MapControl> void registerSpi(Class<?> clazz, String nativeName, Spi<T> spi) {
        rwLock.acquireWrite();
        try {
            spiByClass.put(clazz, spi);
            spiByName.put(nativeName, spi);
        } finally {
            rwLock.releaseWrite();
        }
    }

    public static <T extends MapControl> T create(Class<T> controlClass, Pointer pointer, Object owner) {
        rwLock.acquireRead();
        try {
            Spi<?> spi = spiByClass.get(controlClass);
            if (spi != null)
                return (T) spi.createNativeControl(pointer, owner);
            return null;
        } finally {
            rwLock.releaseRead();
        }
    }

    public static <T extends MapControl> T create(String nativeName, Pointer pointer, Object owner) {
        rwLock.acquireRead();
        try {
            Spi<?> spi = spiByName.get(nativeName);
            if (spi != null)
                return (T) spi.createNativeControl(pointer, owner);
            return null;
        } finally {
            rwLock.releaseRead();
        }
    }
}
