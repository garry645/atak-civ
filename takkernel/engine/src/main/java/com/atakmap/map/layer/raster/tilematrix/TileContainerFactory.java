package com.atakmap.map.layer.raster.tilematrix;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.interop.Interop;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.spatial.GeometryTransformer;
import com.atakmap.util.Visitor;

public final class TileContainerFactory
{
    final static Interop<TileContainerSpi> TileContainerSpi_interop = Interop.findInterop(TileContainerSpi.class);

    private final static Map<TileContainerSpi, Pointer> spis = new IdentityHashMap<>();

    private TileContainerFactory()
    {
    }

    /**
     * Opens or creates a {@link TileContainer} at the specified location that
     * will be able to
     *
     * @param path The path of the target tile container
     * @param spec Describes the layout of the target tile container, may not
     *             be <code>null</code>
     * @param hint The desired <I>provider</I>, or <code>null</code> to select
     *             any compatible container provider
     * @return A new {@link TileContainer} capable of storing tile data
     * described by the specified matrix, or <code>null</code> if no
     * such container could be created.
     */
    public static TileContainer openOrCreateCompatibleContainer(String path, TileMatrix spec, String hint)
    {
        if (path == null)
            return null;

        return openOrCreateCompatibleContainerNative(path, spec, hint);
    }

    /**
     * Opens an already existing tile container at the specified location.
     *
     * @param path     The path
     * @param readOnly <code>true</code> to open as read-only,
     *                 <code>false</code> to allow read-write.
     * @param hint     The desired <I>provider</I>, or <code>null</code> to
     *                 select any container provider that can open the file at
     *                 specified location.
     * @return A new {@link TileContainer} instance providing access to the
     * tile content at the specified location or <code>null</code> if
     * no such container could be opened.
     */
    public synchronized static TileContainer open(String path, boolean readOnly, String hint)
    {
        if (path == null)
            return null;

        return openNative(path, readOnly, hint);
    }

    /**
     * Registers the specified spi.
     *
     * @param spi
     */
    public synchronized static void registerSpi(TileContainerSpi spi)
    {
        if (spis.containsKey(spi))
            return;
        final Pointer pointer = TileContainerSpi_interop.wrap(spi);
        spis.put(spi, pointer);
        registerNative(pointer);
    }

    /**
     * Unregisters the specified spi.
     *
     * @param spi
     */
    public synchronized static void unregisterSpi(TileContainerSpi spi)
    {
        final Pointer pointer = spis.remove(spi);
        if (pointer == null)
            return;
        unregisterNative(pointer.raw);
        TileContainerSpi_interop.destruct(pointer);
    }

    /**
     * Visits all registered spis.
     *
     * @param visitor The callback that will be invoked when visiting the
     *                registered spis.
     */
    public synchronized static void visitSpis(Visitor<Collection<TileContainerSpi>> visitor)
    {
        visitSpisNative(new Visitor<TileContainerSpi[]>() {
            @Override
            public void visit(TileContainerSpi[] object) {
                visitor.visit(Arrays.asList(object));
            }
        });
    }

    /**
     * Visits all registered spis compatible with the specified tile matrix.
     *
     * @param visitor The callback that will be invoked when visiting the
     *                compatible registered spis.
     */
    public synchronized static void visitCompatibleSpis(Visitor<Collection<TileContainerSpi>> visitor, TileMatrix spec)
    {
        visitCompatibleSpisNative(new Visitor<TileContainerSpi[]>() {
            @Override
            public void visit(TileContainerSpi[] object) {
                visitor.visit(Arrays.asList(object));
            }
        }, spec);
    }

    public static TileMatrix createSpec(String name, TileGrid tileGrid, int minZoom, int maxZoom, Map<String, Object> metadata) {
        return new CreateSpec(
                name,
                tileGrid.srid,
                tileGrid.origin,
                Arrays.copyOfRange(tileGrid.zoomLevels, minZoom, maxZoom),
                GeometryTransformer.transform(tileGrid.bounds_wgs84, 4326, tileGrid.srid),
                metadata);
    }

    static native void registerNative(Pointer pointer);
    static native void unregisterNative(long pointer);
    static native TileContainer openOrCreateCompatibleContainerNative(String path, TileMatrix spec, String hint);
    static native TileContainer openNative(String path, boolean readOnly, String hint);
    static native void visitSpisNative(Visitor<TileContainerSpi[]> visitor);
    static native void visitCompatibleSpisNative(Visitor<TileContainerSpi[]> visitor, TileMatrix spec);
}
