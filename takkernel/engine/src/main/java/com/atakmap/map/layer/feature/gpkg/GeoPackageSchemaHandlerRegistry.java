package com.atakmap.map.layer.feature.gpkg;

import com.atakmap.map.gpkg.GeoPackage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import gov.tak.api.annotation.DeprecatedApi;

public final
class GeoPackageSchemaHandlerRegistry
{
    //==================================
    //
    //  PUBLIC INTERFACE
    //
    //==================================

    /** @deprecated use {@link #getHandler2(GeoPackage)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static synchronized GeoPackageSchemaHandler
    getHandler(GeoPackage geoPackage)
    {
        final GeoPackageSchemaHandler2 handler = getHandler2(geoPackage);
        return (handler != null) ? new SchemaHandlerAdapter.Backward(handler) : null;
    }

    public static GeoPackageSchemaHandler2 getHandler2(GeoPackage geoPackage)
    {
        for(GeoPackageSchemaHandler2.Spi spi : _registry) {
            GeoPackageSchemaHandler2 handler = spi.create(geoPackage);
            if(handler != null)
                return handler;
        }
        return new DefaultGeoPackageSchemaHandler3(geoPackage);
    }

    /** @deprecated use {@link #register(GeoPackageSchemaHandler2.Spi)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public synchronized static void
    register(GeoPackageSchemaHandler.Factory factory)
    {
        GeoPackageSchemaHandler2.Spi spi = _legacySpis.get(factory);
        if(spi != null)
            return;
        spi = new SchemaHandlerAdapter.Spi(factory);
        _legacySpis.put(factory, spi);
        register(spi);
    }

    /** @deprecated use {@link #unregister(GeoPackageSchemaHandler2.Spi)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static synchronized void
    unregister(GeoPackageSchemaHandler.Factory factory)
    {
        GeoPackageSchemaHandler2.Spi spi = _legacySpis.get(factory);
        if(spi == null)
            return;
        unregister(spi);
    }

    public static void register(GeoPackageSchemaHandler2.Spi spi)
    {
        _registry.add(spi);
    }

    public static void unregister(GeoPackageSchemaHandler2.Spi spi)
    {
        _registry.remove(spi);
    }

    //==================================
    //
    //  PRIVATE IMPLEMENTATION
    //
    //==================================

    private GeoPackageSchemaHandlerRegistry()
    {
    }

    //==================================
    //  PRIVATE REPRESENTATION
    //==================================

    private static Set<GeoPackageSchemaHandler2.Spi> _registry = new ConcurrentSkipListSet<>(new Comparator<GeoPackageSchemaHandler2.Spi>() {
        @Override
        public int compare(GeoPackageSchemaHandler2.Spi a, GeoPackageSchemaHandler2.Spi b) {
            if(a.getPriority() != b.getPriority())
                return a.getPriority()-b.getPriority();
            else if(a == b)
                return 0;
            else
                return a.hashCode()-b.hashCode();
        }
    });
    private static Map<GeoPackageSchemaHandler.Factory, GeoPackageSchemaHandler2.Spi> _legacySpis = new HashMap<>();
}
