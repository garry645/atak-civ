
package com.atak.plugins.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.menu.ActionMenuData;
import com.atakmap.android.update.AppMgmtActivity;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.atakmap.android.widgets.ProgressWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.map.opengl.GLRenderGlobals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Map Component that manages the loading of all plugin MapComponent.  
 * This makes use of the a plugin model which isolates plugins from
 * each other.
 */
public class PluginMapComponent extends AbstractMapComponent implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "PluginMapComponent";

    private Context context;
    private AtakPreferences prefs;
    private AtakPluginRegistry pluginRegistry;
    private MapView mapView;

    @Override
    public void onCreate(final Context context, final Intent intent,
            final MapView mapViewFinal) {

        this.mapView = mapViewFinal;
        this.context = context;
        prefs = AtakPreferences.getInstance(context);

        registerReceiver(context, uninstallReceiver,
                new AtakBroadcast.DocumentedIntentFilter(
                        "com.atakmap.app.APP_REMOVED"));

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                onCreateO(context, mapViewFinal);
            }
        });
        t.setName("PluginScannerThread");
        t.start();
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences prefs, String key) {

        if (key == null)
            return;

        //Log.d(TAG, "prefs changed: " + key);

        if (key.equals("atakPluginScanningOnStartup")) {
            if (prefs.getBoolean("atakPluginScanningOnStartup",
                    true)) {
                Log.d(TAG,
                        "atakPluginScanningOnStartup enabled");

                //mark the app as dirty, so that it can correctly launch safe mode when needed
                prefs.edit()
                        .putBoolean("pluginSafeMode", true)
                        .apply();
            } else {
                Log.d(TAG,
                        "atakPluginScanningOnStartup disabled");

                //unset dirty flag because it doesn't matter if we're not loading plugins on start
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("pluginSafeMode", false);

                final Set<AtakPluginRegistry.PluginDescriptor> plugins = pluginRegistry
                        .getPluginDescriptors();
                Log.d(TAG,
                        "clearing shouldLoad for plugin count: "
                                + plugins.size());
                for (AtakPluginRegistry.PluginDescriptor plugin : plugins) {
                    edit.remove(AtakPluginRegistry.SHOULD_LOAD
                            + plugin.getPackageName());
                }

                edit.apply();
            }
        }
    }

    private void onCreateO(final Context context,
            final MapView mapViewFinal) {
        MapActivity mapActivity = (MapActivity) context;

        if (!(mapActivity.getApplication() instanceof ATAKApplication)) {
            Log.e(TAG, "Activity is not pluggable.");
            throw new IllegalStateException();
        }

        pluginRegistry = AtakPluginRegistry
                .initialize(mapViewFinal);
        Log.d(TAG,
                "Starting up with plugin-api version: "
                        + ATAKConstants.getPluginApi(false));

        prefs.registerListener(this);

        // grab preference that says that ATAK exited uncleanly last time
        boolean atakExitedUncleanlyLastTime = prefs.get(
                "pluginSafeMode", false);

        Log.d(TAG, "Detected that app's last shutdown was "
                + (atakExitedUncleanlyLastTime ? "NOT clean" : "clean"));

        // if plugin scanning is disabled...
        if (!prefs.get("atakPluginScanningOnStartup", true)) {
            //unload plugins
            Log.d(TAG,
                    "Startup scanning disabled, removing all from ActionBar");
            Intent toolsintent = new Intent(ActionBarReceiver.ADD_NEW_TOOLS);
            toolsintent.putExtra("menus", new ActionMenuData[0]);
            AtakBroadcast.getInstance().sendBroadcast(toolsintent);

            boolean trusted = AtakPluginRegistry
                    .get()
                    .scanAndLoadPlugins( new AtakPluginRegistry.PluginLoadingProgressCallback2() {
                        @Override
                        public boolean loadOnlyMandatory() {
                            return true;
                        }

                        @Override
                        public void onProgressUpdated(int percent) { }

                        @Override
                        public void onComplete(int numLoaded) {}
                    });
            finished(true, trusted);
            return;
        }

        // now set it to 'true', we'll mark it 'false' when we actually exit cleanly...
        prefs.set("pluginSafeMode", true);

        // if plugin ATAK exited uncleanly
        if (atakExitedUncleanlyLastTime) {
            Log.d(TAG,
                    "opening dialog to allow user to select if plugins should be re-loaded");
            // display an option to the user to skip loading of the plugins
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.load_plugins_question)
                            .setMessage(String.format(LocaleUtil.getCurrent(), 
                                            context.getString(R.string.unclean_exit_possible_plugin), 
                                    context.getString(R.string.app_name)))
                            .setPositiveButton(R.string.load_plugins_confirm,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int id) {
                                            dialog.dismiss();

                                            final Thread t = new Thread(new Runnable() {
                                                public void run() {
                                                    boolean trusted = AtakPluginRegistry
                                                            .get()
                                                            .scanAndLoadPlugins( new AtakPluginRegistry.PluginLoadingProgressCallback2() {
                                                                @Override
                                                                public boolean loadOnlyMandatory() {
                                                                    return false;
                                                                }

                                                                @Override
                                                                public void onProgressUpdated(int percent) { }

                                                                @Override
                                                                public void onComplete(int numLoaded) {}
                                                            });
                                                    finished(false, trusted);
                                                }
                                            });
                                            t.start();
                                        }
                                    })
                            .setNeutralButton(R.string.skip_plugin_loading_once,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int id) {
                                            Log.d(TAG,
                                                    "Skipping plugin loading");
                                            dialog.dismiss();
                                            boolean trusted = AtakPluginRegistry
                                                    .get()
                                                    .scanAndLoadPlugins( new AtakPluginRegistry.PluginLoadingProgressCallback2() {
                                                        @Override
                                                        public boolean loadOnlyMandatory() {
                                                            return true;
                                                        }

                                                        @Override
                                                        public void onProgressUpdated(int percent) { }

                                                        @Override
                                                        public void onComplete(int numLoaded) {}
                                                    });
                                            finished(true, trusted);
                                        }
                                    })
                            .setNegativeButton(
                                    R.string.disable_automatic_plugin_loading, // implemented
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int id) {
                                            dialog.dismiss();
                                            Log.d(TAG,
                                                    "Disabled plugin loading from warning dialog");
                                            prefs.set(
                                                    "atakPluginScanningOnStartup",
                                                    false);
                                            // since no plugins have been loaded...
                                            boolean trusted = AtakPluginRegistry
                                                    .get()
                                                    .scanAndLoadPlugins(new AtakPluginRegistry.PluginLoadingProgressCallback2() {
                                                        @Override
                                                        public boolean loadOnlyMandatory() {
                                                            return true;
                                                        }

                                                        @Override
                                                        public void onProgressUpdated(int percent) { }

                                                        @Override
                                                        public void onComplete(int numLoaded) {}
                                                    });
                                            finished(true, trusted);
                                        }
                                    })
                            .create();
                    dialog.show();
                }
            });
        } else {
            Log.d(TAG,
                    "automatically loading plugins because app exited cleanly last shutdown");
            boolean trusted = AtakPluginRegistry.get().scanAndLoadPlugins(null);
            finished(false, trusted);
        }
    }

    private void finished(boolean loadOnlyMandatory, boolean trusted) {
        Log.d(TAG, "finished loading plugins, without error");

        // running this on the ui thread so that state saver does not start prematurely
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(
                        "com.atakmap.app.COMPONENTS_CREATED");
                i.putExtra("trusted", trusted);
                i.putExtra("loadOnlyMandatory", loadOnlyMandatory);
                AtakBroadcast.getInstance().sendBroadcast(i);
            }
        });

        // wait a few seconds because plugins might crash a few seconds after being loaded.
        // this attempts to reduce the looping that currently occurs with trying to load older
        // versions of datasync for with newer versions of atak.   This is just an example case.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
                prefs.set("pluginSafeMode", false);
            }
        }, "plugin-sanity-check");
        t.start();

        if (!trusted)
            constructAndDisplayWarning();
    }

    private void constructAndDisplayWarning() {
        // show fading widget on the bottom if it is untrusted
        final RootLayoutWidget root = (RootLayoutWidget) mapView
                .getComponentExtra("rootLayoutWidget");
        final LinearLayoutWidget notification = new LinearLayoutWidget(
                LinearLayoutWidget.WRAP_CONTENT,
                LinearLayoutWidget.WRAP_CONTENT,
                LinearLayoutWidget.HORIZONTAL);
        notification.setPoint(0, mapView.getHeight() - 150);

        final MarkerIconWidget untrust = new MarkerIconWidget();
        untrust.setIcon(createIcon(R.drawable.untrusted));
        untrust.setSize(42, 42);
        notification.addWidget(untrust);

        final ProgressWidget timeBar = new ProgressWidget();
        timeBar.setMax(5000);
        timeBar.setHeight(42);
        timeBar.setMargins(0f, GLRenderGlobals.getRelativeScaling() * 42f, 0f,
                GLRenderGlobals.getRelativeScaling() * -6f);
        timeBar.setWidth(300);
        notification.addWidget(timeBar);

        untrust.addOnClickListener(new MapWidget.OnClickListener() {
            @Override
            public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                        context);
                alertBuilder
                        .setTitle(R.string.plugin_warning)
                        .setMessage(
                                R.string.plugin_warning_message)
                        .setPositiveButton(R.string.see_more,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        Activity a = ((Activity) context);
                                        Intent mgmtPlugins = new Intent(a,
                                                AppMgmtActivity.class);
                                        a.startActivityForResult(mgmtPlugins,
                                                ToolsPreferenceFragment.APP_MGMT_REQUEST_CODE);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null);
                alertBuilder.create().show();
            }
        });

        root.addWidget(notification);

        Thread t = new Thread() {
            public void run() {
                for (int i = 0; i < 13; ++i) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                    if (i < 12)
                        timeBar.setProgress(5000 - (i * 500));
                }
                root.removeWidget(notification);
            }
        };
        t.start();
    }

    private Icon createIcon(final int icon) {
        Icon.Builder b = new Icon.Builder();
        b.setAnchor(0, 0);
        b.setColor(Icon.STATE_DEFAULT, Color.WHITE);
        b.setSize(42, 42);

        final String uri = "android.resource://"
                + mapView.getContext().getPackageName()
                + "/" + icon;

        b.setImageUri(Icon.STATE_DEFAULT, uri);
        return b.build();
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG,
                "destroying PluginMapComponent, so setting flag that app exited cleanly");
        // ATAK exited cleanly, so set the preference as such
        AtakPreferences prefs = AtakPreferences.getInstance(context);
        prefs.set("pluginSafeMode", false);
        AtakPluginRegistry.get().dispose();
        unregisterReceiver(context, uninstallReceiver);
    }

    private static final Map<String, Drawable> pluginIconMap = new HashMap<>();

    /**
     * Store icon, and return predictable icon ID
     * Stores copy of icon scaled to 32x32
     *
     * @param tool the tool descriptor that contains the icon.
     * @return icon ID (hash of icon content)
     */
    public static String addPluginIcon(IToolbarItem tool) {
        String uid = tool.getClass().getName();
        Drawable icon = tool.getIcon();
        if (!(icon instanceof BitmapDrawable)) {
            if (!FileSystemUtils.isEmpty(tool.getDescription()))
                uid += ":" + tool.getDescription();
            if (!FileSystemUtils.isEmpty(tool.getShortDescription()))
                uid += ":" + tool.getShortDescription();
        }
        if (FileSystemUtils.isEmpty(uid))
            uid = UUID.randomUUID().toString();

        return addPluginIcon(uid, tool.getIcon());
    }

    public static String addPluginIcon(String toolUid, Drawable icon) {
        Log.d(TAG, "addPluginIcon: " + toolUid);
        pluginIconMap.put(toolUid, icon);
        return toolUid;
    }

    /**
     * Given an identifier, get the icon for the plugin.
     * @param id string identifier
     * @return the icon for the plugin in drawable form.
     */
    public static Drawable getPluginIconWithId(String id) {
        return pluginIconMap.get(id);
    }

    private final BroadcastReceiver uninstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String pkgName = intent.getStringExtra("package");

            // if the package is uninstalled, make sure to invalidate the
            // hash cache for that particular package
            if (action != null && pkgName != null)
                PluginValidator.invalidateHash(pkgName);
        }
    };

}
