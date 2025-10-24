
package com.atakmap.android.video;

import android.graphics.Bitmap;

import com.atakmap.coremap.log.Log;
import com.partech.mobilevid.SurfaceVideoConsumer;
import com.partech.pgscmedia.MediaException;
import com.partech.pgscmedia.VideoMediaFormat;
import com.partech.pgscmedia.frameaccess.NativeIntArray;
import com.partech.pgscmedia.frameaccess.VideoFrameConverter;
import com.partech.pgscmedia.frameaccess.VideoFrameData;

import java.util.ArrayList;
import java.util.List;

class BitmapVideoConsumer extends SurfaceVideoConsumer {
    public static final String TAG = "BitmapVideoConsumer";

    private VideoMediaFormat fmt;
    private VideoFrameConverter converter;
    private Bitmap bitmap;

    private VideoMediaFormat.PixelFormat frameFormat = null;

    private final List<BitmapFrameRequestCallback> callbacks = new ArrayList<>();

    @Override
    public synchronized void mediaVideoFrame(VideoFrameData frame) {
        super.mediaVideoFrame(frame);

        if (callbacks.isEmpty())
            return;

        if (!convertFrame(frame))
            return;

        for (BitmapFrameRequestCallback callback : callbacks) {
            try {
                callback.frameReady(bitmap);
            } catch (Throwable ignored) {
            }
        }

        callbacks.clear();
    }

    private boolean convertFrame(VideoFrameData frame) {
        if (frame == null)
            return false;

        if (bitmap == null || frame.getWidth() != bitmap.getWidth()
                || frame.getHeight() != bitmap.getHeight()) {
            setupConverter(frame.getWidth(), frame.getHeight(),
                    frame.getPixelFormat());
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
            bitmap = Bitmap.createBitmap(frame.getWidth(), frame.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bitmap.setHasAlpha(false);
        }

        if (converter == null || frame.getPixelFormat() != frameFormat)
            setupConverter(frame.getWidth(), frame.getHeight(),
                    frame.getPixelFormat());

        try {
            converter.convert(frame);

            NativeIntArray output = (NativeIntArray) converter.getOutputArray();
            int offset = output.offset + converter.getOutputOffsets()[0];
            int stride = converter.getOutputStrides()[0];

            bitmap.setPixels(output.intArray, offset, stride, 0, 0,
                    frame.getWidth(), frame.getHeight());
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Error converting video frame", e);
        }
        return false;
    }

    synchronized void setFormat(VideoMediaFormat fmt) {
        this.fmt = fmt;
        // force to recreate
        this.converter = null;
    }

    private void setupConverter(int w, int h,
            VideoMediaFormat.PixelFormat srcPixelFmt) {
        try {
            fmt = new VideoMediaFormat(fmt.trackNum, srcPixelFmt,
                    fmt.aspectRatio, w, h);
            converter = new VideoFrameConverter(fmt,
                    VideoMediaFormat.PixelFormat.PIXELS_RGB_PACKED);
            converter.setScaleForAspect(false);

            frameFormat = srcPixelFmt;
        } catch (MediaException e) {
            Log.e(TAG, "Error initializing video frame converter", e);
        }
    }

    synchronized void requestBitmapFrame(BitmapFrameRequestCallback callback) {
        callbacks.add(callback);
    }

    synchronized void videoStopped() {
        for (BitmapFrameRequestCallback callback : callbacks) {
            try {
                callback.frameNotAvailable();
            } catch (Throwable ignored) {
            }
        }
        callbacks.clear();
    }

}
