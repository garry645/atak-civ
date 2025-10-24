package gov.tak.api.engine.map;

import com.atakmap.interop.Interop;

import gov.tak.api.annotation.DontObfuscate;

// implementation detail class
@DontObfuscate
class CurrentRenderContext
{

    private static ThreadLocal<RenderContext<?>> threadLocalRenderContext = new ThreadLocal<>();
    private static com.atakmap.interop.Interop<gov.tak.api.engine.map.RenderContext> RenderContext_interop = Interop.findInterop(gov.tak.api.engine.map.RenderContext.class);

    public static void setCurrent(RenderContext<?> renderContext)
    {
        threadLocalRenderContext.set(renderContext);
        long nativePointer = RenderContext_interop.getPointer(renderContext);
        setCurrentNative(nativePointer);
    }

    public static RenderContext<?> getCurrent()
    {
        return threadLocalRenderContext.get();
    }

    private static native void setCurrentNative(long renderContext);
}
