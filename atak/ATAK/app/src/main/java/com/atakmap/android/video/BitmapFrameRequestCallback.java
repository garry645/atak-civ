
package com.atakmap.android.video;

import android.graphics.Bitmap;

public interface BitmapFrameRequestCallback {
    /**
     * Called when the next available video frame is ready and has been converted to a
     * Bitmap. The bitmap is only valid for the duration of this invocation; it
     * MAY be modified or recycled by the system sometime after invocation completes.
     * If the implementor wishes to retain the
     * Bitmap for use beyond the duration of the callback, it should copy it.
     *
     * @param bitmapFrame bitmap representation of the most recently available video frame
     */
    void frameReady(Bitmap bitmapFrame);

    /**
     * Invoked if video playback has been terminated or the system otherwise knows it will not
     * be able to fulfill the bitmap request.
     */
    void frameNotAvailable();
}
