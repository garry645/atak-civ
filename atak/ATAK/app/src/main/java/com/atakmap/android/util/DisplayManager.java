
package com.atakmap.android.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Looper;
import android.view.WindowManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.log.Log;

import java.util.HashSet;
import java.util.Set;

public class DisplayManager {

    private final static Set<String> tempScreenLockHolders = new HashSet<>();

    private final static String TAG = "DisplayManager";

    /**
     * Upon resuming the activity, make sure to set the appropriate flag
     * if there are registered locks on the display keep alive.
     * @param mapView the mapview to grab the activity from.
     */
    public static void checkAndSet(final MapView mapView) {

        if (mapView == null)
            return;

        Log.d(TAG, "refresh screen lock status");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final Activity activity = ((Activity) mapView.getContext());

                if (!tempScreenLockHolders.isEmpty()) {
                    activity.getWindow()
                            .addFlags(
                                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    final KeyguardManager km = (KeyguardManager) activity
                            .getSystemService(Context.KEYGUARD_SERVICE);
                    if (km != null) {
                        KeyguardManager.KeyguardLock kl = km
                                .newKeyguardLock("atakKeyGuard");
                        kl.reenableKeyguard();
                    }

                } else {
                    final AtakPreferences pref = AtakPreferences
                            .getInstance(activity);

                    if (!pref.get("atakScreenLock", false))
                        activity.getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    if (!pref.get("atakDisableKeyguard", false)) {
                        final KeyguardManager km = (KeyguardManager) activity
                                .getSystemService(Context.KEYGUARD_SERVICE);
                        if (km != null) {
                            KeyguardManager.KeyguardLock kl = km
                                    .newKeyguardLock("atakKeyGuard");
                            kl.disableKeyguard();
                        }
                    }
                }
            }
        };

        final Activity activity = ((Activity) mapView.getContext());
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            activity.runOnUiThread(r);
        }
    }

    /**
     * Acquires a temporary screen lock.   To be used when a tool would like to turn off
     * the screen lock and keyguard for a period of time.
     * @param mapView the mapView to be used.
     * @param name the name associated with the request
     */
    static public void acquireTemporaryScreenLock(MapView mapView,
            String name) {

        final Activity activity = ((Activity) mapView.getContext());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "acquire the temporary screen lock for: " + name);
                tempScreenLockHolders.add(name);
                checkAndSet(mapView);
            }
        });

    }

    /**
     * Release a held screen lock.
     * @param mapView the mapView to be used.
     * @param name the name associated with the request
     */
    static public void releaseTemporaryScreenLock(MapView mapView,
            String name) {

        final Activity activity = ((Activity) mapView.getContext());

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempScreenLockHolders.remove(name);
                Log.d(TAG, "release the temporary screen lock for: " + name);
                checkAndSet(mapView);
            }
        });
    }

}
