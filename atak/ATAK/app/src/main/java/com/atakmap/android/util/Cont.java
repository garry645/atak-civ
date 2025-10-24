
package com.atakmap.android.util;

/**
 * Generic interface for a "continuation" -- that is, a computation which can register callbacks.
 * <p>
 * Can be thought of as an event stream managed via a callback API, and thus used similarly to Flow
 *  from kotlin coroutines, or Observable from rxJava2. Implemented here to allow for similar
 *  functionality, while keeping ATAK core's dependencies minimal.
 */
public interface Cont<A> {
    void registerCallback(Callback<A> callback);

    interface Callback<A> {
        void onInvoke(A value);
    }
}
