package com.atakmap.android.metrics.activity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;

public class ActivityHelper {

    /**
     * Responsible for setting the insets for Android 15 and higher so that important
     * touch regions do not get blocked by the navigation bar.
     * @param activity the activity to set the insets on.
     */
    public static void removeEdgeToEdge(Activity activity) {
        final View v = activity.findViewById(android.R.id.content);
        v.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets windowInsets) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    final Insets displayCutout = windowInsets.getInsets(WindowInsets.Type.displayCutout());
                    final Insets systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars());

                    final int marginLeft = Math.max(displayCutout.left, systemBars.left);
                    final int marginTop = Math.max(displayCutout.top, systemBars.top);
                    final int marginRight = Math.max(displayCutout.right, systemBars.right);
                    final int marginBottom = Math.max(displayCutout.bottom, systemBars.bottom);
                    view.setBackgroundColor(Color.BLACK);
                    view.setPadding(marginLeft, marginTop, marginRight, marginBottom);
                }
                return windowInsets;
            }
        });

    }
}