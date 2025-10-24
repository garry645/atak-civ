
package com.atakmap.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.HashSet;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class GifImageView extends ImageView {

    public GifImageView(Context context) {
        super(context);
    }

    public GifImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GifImageView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public enum GifImageViewState {
        NoGif,
        Playing,
        Paused
    }

    private final HashSet<Consumer<GifImageViewState>> stateChangeListeners = new HashSet<>();
    private GifImageViewState state = GifImageViewState.NoGif;
    private GifDecoder decoder = null;

    public boolean setGif(byte[] data) {
        return setGif(data, true);
    }

    public boolean setGif(byte[] data, boolean autoplay) {

        requestState(GifImageViewState.NoGif);

        GifDecoder gd = new GifDecoder();
        int decoderStatus = gd.read(data);
        boolean success = decoderStatus == GifDecoder.STATUS_OK;

        if (success) {
            decoder = gd;
            state = GifImageViewState.Paused;

            if (autoplay) {
                success = requestState(GifImageViewState.Playing);
            }
        }

        return success;
    }

    public GifImageViewState getState() {
        return state;
    }

    public boolean requestState(GifImageViewState newState) {
        switch (newState) {
            case NoGif:
                requestState(GifImageViewState.Paused);
                decoder = null;
                state = GifImageViewState.NoGif;
                for (Consumer<GifImageViewState> l : stateChangeListeners)
                    l.accept(state);
                return true;

            case Playing:
                if (state != GifImageViewState.Paused)
                    return false;
                state = GifImageViewState.Playing;
                startPlayback();
                for (Consumer<GifImageViewState> l : stateChangeListeners)
                    l.accept(state);
                return true;

            case Paused:
                if (state != GifImageViewState.Playing)
                    return false;
                state = GifImageViewState.Paused;
                stopPlayback();
                for (Consumer<GifImageViewState> l : stateChangeListeners)
                    l.accept(state);
                return true;

            default:
                throw new RuntimeException("Unknown state: " + newState);
        }
    }

    private final Runnable updateFrameTask = new Runnable() {
        @Override
        public void run() {
            if (isAttachedToWindow()) {
                if (state == GifImageViewState.Playing) {
                    decoder.advance();
                    setImageBitmap(decoder.getNextFrame());
                    postDelayed(updateFrameTask, decoder.getNextDelay());
                }
            }
        }
    };

    private void startPlayback() {
        updateFrameTask.run();
    }

    private void stopPlayback() {
        removeCallbacks(updateFrameTask);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startPlayback();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPlayback();
    }

    public void addStateChangeListener(Consumer<GifImageViewState> l) {
        stateChangeListeners.add(l);
    }

    public void removeStateChangeListener(Consumer<GifImageViewState> l) {
        stateChangeListeners.remove(l);
    }
}
