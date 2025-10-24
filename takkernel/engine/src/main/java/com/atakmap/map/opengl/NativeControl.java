package com.atakmap.map.opengl;

import com.atakmap.interop.Interop;
import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.NativeControlFactory;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.LayerImplExtension;
import com.atakmap.util.ReadWriteLock;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.util.Disposable;

@DontObfuscate
final class NativeControl implements MapControl, Disposable {

    private static final String TAG = "NativeControl";

    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(NativeControl.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object owner;

    NativeControl(Pointer pointer, Object owner) {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);

        this.pointer = pointer;
        this.owner = owner;
    }

    public String getNativeType() {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getName(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    public Pointer getControlPointer() {
        this.rwlock.acquireRead();
        try
        {
            if (this.pointer.raw == 0L)
                throw new IllegalStateException();
            return getControlPointer(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    public Object getOwner() {
        return owner;
    }

    @Override
    public void dispose() {
        if (this.cleaner != null)
            this.cleaner.clean();
    }

    /**
     * Callback when visiting native controls
     *
     * @param <T>
     */
    @DontObfuscate
    public interface Visitor<T> {
        void onVisitNativeControl(T nativeControl);
    }

    public static <T extends MapControl> void visitNativeControls(MapRenderer3 renderer, Layer2 layer, Class<T> controlClass, Visitor<T> visitor) {
        visitNativeControls(renderer, layer, controlClass.getSimpleName(), controlClass, visitor);
    }

    public static <T extends MapControl> void visitNativeControls(MapRenderer3 renderer, Layer2 layer, String nativeTypeName, Class<T> controlClass, Visitor<T> visitor) {

        Visitor<NativeControl> factorizedVisitor = new Visitor<NativeControl>() {
            @Override
            public void onVisitNativeControl(NativeControl nativeControl) {

                final String verifyNativeTypeName = nativeTypeName == null ? nativeControl.getNativeType() : nativeTypeName;

                // just verify the name is correct (though it should be)
                if (nativeControl.getNativeType().equals(verifyNativeTypeName)) {
                    T control = controlClass == null ?
                            NativeControlFactory.create(verifyNativeTypeName, nativeControl.getControlPointer(), null) :
                            NativeControlFactory.create(controlClass, nativeControl.getControlPointer(), null);
                    if (control != null)
                        visitor.onVisitNativeControl(control);
                }
            }
        };
        visitNativeControls(renderer, layer, nativeTypeName, factorizedVisitor);
    }

    public static void visitNativeControls(final MapRenderer3 renderer, final Layer2 layer, final Visitor<Iterator<MapControl>> visitor) {
        visitNativeControls(renderer, Collections.singleton(layer), new Visitor<Iterator<Map.Entry<Layer2, Collection<MapControl>>>>() {
            @Override
            public void onVisitNativeControl(Iterator<Map.Entry<Layer2, Collection<MapControl>>> controls) {
                // NOTE: we only expect one, but fulfill the contract strictly
                while(controls.hasNext()) {
                    Map.Entry<Layer2, Collection<MapControl>> entry = controls.next();
                    if(entry.getKey() == layer) {
                        visitor.onVisitNativeControl(entry.getValue().iterator());
                        break;
                    }
                }
            }
        });
    }

    public static void visitNativeControls(final MapRenderer3 renderer, final Collection<Layer2> layers, final Visitor<Iterator<Map.Entry<Layer2, Collection<MapControl>>>> visitor) {
        final Iterator<Layer2> iter = layers.iterator();
        while(iter.hasNext()) {
            final Layer2 layer = iter.next();
            visitNativeControls(renderer, layer, (String) null,
                new Visitor<NativeControl>() {
                    boolean visited = false;

                    @Override
                    public void onVisitNativeControl(NativeControl nativeControl) {
                        // only handle the first control of the first layer
                        if (visited) return;
                        visited = true;

                        // we have at least one control. while remaining within the scope of the
                        // initial visitation, recurse a second visit to gather the controls and
                        // invoke the client visitor
                        Map<Layer2, Collection<MapControl>> controls = new LinkedHashMap<>();
                        Layer2 l = layer;
                        while(true) {
                            NativeControlGatherer gatherer = new NativeControlGatherer();
                            visitNativeControls(renderer, layer, (String) null, gatherer);
                            if(!gatherer.controls.isEmpty())
                                controls.put(l, gatherer.controls);
                            // iterate the remaining layers
                            if(!iter.hasNext())
                                break;
                            else
                                l = iter.next();
                        }

                        visitor.onVisitNativeControl(controls.entrySet().iterator());
                    }
                });
        }
    }

    public static void visitNativeControls(MapRenderer3 renderer, Layer2 layer, String nativeTypeName, Visitor<NativeControl> visitor) {

        if (renderer == null || visitor == null)
            throw new IllegalArgumentException();

        // no renderer controls for now
        if (layer == null)
            return;

        Interop<GLMapView> rendererInterop = Interop.findInterop(GLMapView.class);
        Interop<Layer> layerInterop = Interop.findInterop(Layer.class);

        // something isn't native
        if (rendererInterop == null || layerInterop == null || !(renderer instanceof GLMapView))
            return;

        boolean legacy = false;
        long renderPtr = rendererInterop.getPointer((GLMapView)renderer);
        long layerPtr = 0;

        // Could be a wrapper. Try the impl extension first
        LayerImplExtension implExt = layer.getExtension(LayerImplExtension.class);
        if (implExt != null) {
            layerPtr = layerInterop.getPointer(implExt.getLayer());
            legacy = true; // this is a Layer v1
        }

        // use regular interop method
        if (layerPtr == 0)
            layerPtr = layerInterop.getPointer(layer);

        if (renderPtr == 0L || layerPtr == 0L)
            return;

        if (legacy)
            visitNativeControlsLegacy(renderPtr, layerPtr, nativeTypeName, visitor);
        else
            visitNativeControls(renderPtr, layerPtr, nativeTypeName, visitor);
    }

    final static class NativeControlGatherer implements Visitor<NativeControl> {

        final Collection<MapControl> controls = new LinkedList<>();

        @Override
        public void onVisitNativeControl(NativeControl nativeControl) {
            // interop the control
            final MapControl managedControl = NativeControlFactory.create(
                    nativeControl.getNativeType(), nativeControl.getControlPointer(), null);
            // if successfully interop'ed, add to the controls list
            if(managedControl != null)
                controls.add(managedControl);
        }

        void scatter(Visitor<Iterator<MapControl>> visitor) {
            visitor.onVisitNativeControl(controls.iterator());
        }
    }

    static native void visitNativeControls(long renderer, long layer, String controlType, Visitor visitor);
    static native void visitNativeControlsLegacy(long renderer, long layer, String controlType, Visitor visitor);

    /*************************************************************************/
    // Interop implementation
    static NativeControl create(Pointer pointer, Object owner)
    {
        return new NativeControl(pointer, owner);
    }

    static long getPointer(NativeControl control)
    {
        if (control != null)
            return control.pointer.raw;
        else
            return 0L;
    }

    static boolean hasPointer(NativeControl control)
    {
        return control != null && control.pointer != null
                && control.pointer.raw != 0L;
    }

    static native void destruct(Pointer pointer);

    // this is mapped to getType() in C++
    static native String getName(long ptr);
    static native Pointer getControlPointer(long ptr);
}
