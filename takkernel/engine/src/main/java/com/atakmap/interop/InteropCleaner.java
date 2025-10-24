package com.atakmap.interop;

import com.atakmap.coremap.log.Log;

public final class InteropCleaner extends NativePeerManager.Cleaner
{
    final Interop interop;
    final Class<?> interopClass;

    public InteropCleaner(Class<?> clazz)
    {
        this(Interop.findInterop(clazz), clazz);
    }

    public InteropCleaner(Interop interop)
    {
        this(interop, null);
    }

    private InteropCleaner(Interop interop, Class<?> interopClass)
    {
        if (interop == null)
            throw new IllegalArgumentException();
        this.interop = interop;
        this.interopClass = interopClass;
    }

    @Override
    protected void run(Pointer pointer, Object opaque)
    {
        if (opaque != null)
            Log.w("InteropCleaner", "Cleaning " + interopClass + ", non-null opaque provided");
        this.interop.destruct(pointer);
    }
}
