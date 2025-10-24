package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;

import java.util.IdentityHashMap;
import java.util.Map;

public final class TileClientFactory
{
    final static Interop<TileClientSpi> TileClientSpi_interop = Interop.findInterop(TileClientSpi.class);

    private final static Map<TileClientSpi, Pointer> spis = new IdentityHashMap<>();


    private TileClientFactory()
    {
    }

    public static synchronized TileClient create(String path, String offlineCache, TileClientSpi.Options opts)
    {
        return createNative(path, offlineCache, opts);
    }


    public static synchronized void registerSpi(TileClientSpi spi)
    {
        if (spis.containsKey(spi))
            return;
        final Pointer pointer = TileClientSpi_interop.wrap(spi);
        spis.put(spi, pointer);
        registerNative(pointer);
    }


    public static synchronized void unregisterSpi(TileClientSpi spi)
    {
        final Pointer pointer = spis.remove(spi);
        if (pointer == null)
            return;
        unregisterNative(pointer.raw);
        TileClientSpi_interop.destruct(pointer);
    }

    static native TileClient createNative(String path, String offlineCache, TileClientSpi.Options opts);
    static native void registerNative(Pointer pointer);
    static native void unregisterNative(long raw);
}
