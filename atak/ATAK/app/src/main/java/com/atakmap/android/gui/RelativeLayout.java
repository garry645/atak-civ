package com.atakmap.android.gui;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.atakmap.app.BuildConfig;
import com.atakmap.coremap.log.Log;

public final class RelativeLayout extends android.widget.RelativeLayout {

    private final static String TAG = "RelativeLayout";

    public RelativeLayout(Context context) {
        super(context);
    }

    public RelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param child  Direct child of this ViewParent containing target
     * @param target The view that needs to redraw
     */
    @Override
    public void onDescendantInvalidated(@NonNull View child, @NonNull View target) {
        if(Thread.currentThread() != Looper.getMainLooper().getThread()) {
            final String msg = "Only the original thread that created a view hierarchy can touch its views."
                    + " Expected: UI Thread"
                    + " Calling: " + Thread.currentThread().getName();
            Log.e(TAG, msg, new Exception());
            if (BuildConfig.DEBUG)
                throw new RuntimeException(msg);
            return;
        }
        super.onDescendantInvalidated(child, target);
    }
}

