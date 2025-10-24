package com.atakmap.map.layer.elevation;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import com.atakmap.map.layer.control.GradientControl;
import com.atakmap.util.ReadWriteLock;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.util.Disposable;

@DontObfuscate
final class NativeGradientControl implements GradientControl, Disposable {

    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(NativeGradientControl.class);

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object owner;


    NativeGradientControl(Pointer pointer, Object owner)
    {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);

        this.pointer = pointer;
        this.owner = owner;
    }

    @Override
    public void dispose() {
        if (this.cleaner != null)
            this.cleaner.clean();
    }


    @Override
    public int[] getGradientColors() {
        rwlock.acquireRead();
        try {
            return getGradientColors(this.pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public String[] getLineItemStrings() {
        rwlock.acquireRead();
        try {
            return getLineItemString(this.pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public int getMode() {
        rwlock.acquireRead();
        try {
            return getMode(this.pointer.raw);
        } finally {
            rwlock.releaseRead();
        }
    }

    @Override
    public void setMode(int mode) {
        rwlock.acquireRead();
        try {
            setMode(this.pointer.raw, mode);
        } finally {
            rwlock.releaseRead();
        }
    }

    /*************************************************************************/
    // Interop implementation
    static NativeGradientControl create(Pointer pointer, Object owner)
    {
        return new NativeGradientControl(pointer, owner);
    }

    static long getPointer(NativeGradientControl control)
    {
        if (control != null)
            return control.pointer.raw;
        else
            return 0L;
    }

    static boolean hasPointer(NativeGradientControl object)
    {
        return object != null && object.pointer != null && object.pointer.raw != 0L;
    }

    static native void destruct(Pointer pointer);

    static native int[] getGradientColors(long ptr);
    static native String[] getLineItemString(long ptr);
    static native int getMode(long ptr);
    static native void setMode(long ptr, int mode);
}
