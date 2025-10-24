
package com.atak.plugins.impl;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.update.AppMgmtUtils;
import com.atakmap.app.ATAKActivity;

/**
 * {@link MapComponent} specialization for instances provided by plugins. The plugin specific
 * {@link Context} will be forwarded to the underlying instance during the various callbacks.
 */
final class PluginInjectedMapComponent implements MapComponent {
    final Context pluginContext;
    final MapComponent impl;

    private final static String TAG = "PluginInjectedMapComponent";

    PluginInjectedMapComponent(Context context, MapComponent component) {
        this.pluginContext = context;
        this.impl = component;
    }

    @Override
    public void onCreate(Context ignored, Intent intent, MapView view) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    impl.onCreate(pluginContext, intent, view);
                } catch (Throwable t) {
                    errorOnCreate = true;
                    handleError(view.getContext(), "Error loading plugin %s",
                            t);
                }
            }
        };

        // ensure method is called on the main thread
        if (Looper.myLooper() == Looper.getMainLooper())
            r.run();
        else
            ((ATAKActivity) view.getContext()).runOnUiThread(r);
    }

    @Override
    public void onDestroy(Context ignored, MapView view) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    impl.onDestroy(pluginContext, view);
                } catch (Throwable t) {
                    if (!errorOnCreate)
                        handleError(view.getContext(),
                                "Error unloading plugin %s", t);
                }
            }
        };
        // ensure method is called on the main thread
        if (Looper.myLooper() == Looper.getMainLooper())
            r.run();
        else
            ((ATAKActivity) view.getContext()).runOnUiThread(r);
    }

    @Override
    public void onStart(Context ignored, MapView view) {
        impl.onStart(pluginContext, view);
    }

    @Override
    public void onStop(Context ignored, MapView view) {
        impl.onStop(pluginContext, view);
    }

    @Override
    public void onPause(Context ignored, MapView view) {
        impl.onPause(pluginContext, view);
    }

    @Override
    public void onResume(Context ignored, MapView view) {
        impl.onResume(pluginContext, view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        impl.onConfigurationChanged(newConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(Context ignored, Menu menu) {
        return impl.onCreateOptionsMenu(pluginContext, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Context ignored, Menu menu) {
        return impl.onPrepareOptionsMenu(pluginContext, menu);
    }

    @Override
    public boolean onOptionsItemSelected(Context ignored, MenuItem item) {
        return impl.onOptionsItemSelected(pluginContext, item);
    }

    @Override
    public void onOptionsMenuClosed(Context ignored, Menu menu) {
        impl.onOptionsMenuClosed(pluginContext, menu);
    }

    private void handleError(Context ctx, String state, Throwable t) {
        final String pkgName = pluginContext.getPackageName();

        final String appName = AppMgmtUtils.getAppNameOrPackage(ctx, pkgName);
        Toast.makeText(ctx,
                String.format(state, appName),
                Toast.LENGTH_SHORT).show();
        Log.e(TAG, "error with plugin: " + appName + " " + pkgName, t);

        Thread unloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AtakPluginRegistry.get().unloadPlugin(pkgName);
            }
        }, "unload-plugin-" + pkgName);
        unloadThread.start();

    }

    private boolean errorOnCreate = false;
}
