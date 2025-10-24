
package com.atakmap.android.eud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.atakmap.android.data.ClearContentRegistry;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigation.NavButtonManager;
import com.atakmap.android.navigation.models.NavButtonModel;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.update.ProductProviderManager;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;

import java.io.File;

import gov.tak.api.engine.net.auth.HttpClientAuthenticationRegistry;
import gov.tak.api.engine.net.auth.IOAuthTokenStore;
import gov.tak.api.engine.net.auth.OAuthAuthenticationSpi;
import gov.tak.api.engine.net.auth.OAuthTokenManager;

public final class EudApiMapComponent extends DropDownMapComponent implements
        ClearContentRegistry.ClearContentListener {

    private Context _context;

    private EudApiDropDownReceiver ddr;
    private EudApiClient client;

    private AtakPreferences _prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener _prefsListener;

    private NavButtonModel _model;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        super.onCreate(context, intent, view);

        if (!isDeviceSupported())
            return;

        _context = context;
        _prefs = AtakPreferences.getInstance(view.getContext());
        _prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                    String key) {

                if (key == null)
                    return;

                switch (key) {
                    case "eud_api_sync_mapsources":
                    case "eud_api_sync_plugins":
                        int syncResourcesMask = getResourcesMask(prefs);
                        client.setResourcesSyncMask(syncResourcesMask);

                        // spawn a background thread to begin sync'ing the resources
                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                final int mask = client.getResourcesSyncMask();
                                client.evictResources(~mask);
                                try {
                                    client.syncResources(mask, false);
                                } catch (Throwable ignored) {
                                }
                            }
                        }, "eud-sync-resources");
                        t.setPriority(Thread.NORM_PRIORITY);
                        t.start();
                        break;
                    case "eud_api_disable_option":
                        configureLinkEudVisibility();
                        break;
                    case "eud_api_initial_link":
                        if (_prefs.get(key, false)) {
                            // the user has performed an initial link of the EUD, hide button
                            if (_model != null)
                                NavButtonManager.getInstance()
                                        .removeButtonModel(_model);
                        } else {
                            if (_model != null)
                                NavButtonManager.getInstance()
                                        .addButtonModel(_model);
                        }
                    default:
                        break;
                }

                switch (key) {
                    case "eud_api_sync_mapsources":
                    case "eud_api_sync_plugins":
                    case "eud_api_disable_option":
                    case "eud_api_initial_link":
                        if (client != null && client.isLinked()
                                && _model != null) {
                            NavButtonManager.getInstance()
                                    .removeButtonModel(_model);
                        }
                }
            }
        };

        // create an encrypted token store
        final IOAuthTokenStore tokenStore = new EncryptedPreferencesOAuthDataStore(
                AtakPreferences.getInstance(
                        MapView.getMapView().getContext()).getSharedPrefs(),
                "eud-api");

        final OAuthTokenManager tokenManager = new OAuthTokenManager(
                tokenStore);

        HttpClientAuthenticationRegistry
                .registerSpi(new OAuthAuthenticationSpi(tokenManager));

        client = new EudApiClient(
                view.getContext(),
                _context,
                new File(view.getContext().getFilesDir(),
                        EudApiDropDownReceiver.SYNCED_RESOURCES_PATH),
                tokenManager);

        ddr = new EudApiDropDownReceiver(
                view,
                client,
                tokenManager,
                _prefs.getSharedPrefs());

        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(EudApiDropDownReceiver.ACTION_LINK_EUD);
        ddFilter.addAction(EudApiDropDownReceiver.ACTION_UNLINK_EUD);
        ddFilter.addAction(EudApiDropDownReceiver.ACTION_SYNC_RESOURCES);
        ddFilter.addAction("com.atakmap.app.COMPONENTS_CREATED");
        registerDropDownReceiver(ddr, ddFilter);

        _prefs.registerListener(_prefsListener);
        client.setResourcesSyncMask(getResourcesMask(_prefs.getSharedPrefs()));

        // for custom preferences
        EudApiPreferenceFragment.staticClient = client;

        // Toolbar button model
        _model = new NavButtonModel.Builder()
                .setReference("linkeud.xml")
                .setName(_context.getString(R.string.actionbar_link_eud))
                .setImage(_context.getDrawable(R.drawable.ic_link_eud))
                .setAction(EudApiDropDownReceiver.ACTION_LINK_EUD)
                .build();

        configureLinkEudVisibility();

        ClearContentRegistry.getInstance().registerListener(this);

    }

    private boolean isDeviceSupported() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private static int getResourcesMask(SharedPreferences prefs) {
        int syncResourcesMask = -1;
        if (!prefs.getBoolean("eud_api_sync_mapsources", true))
            syncResourcesMask &= ~EudApiClient.RESOURCES_MAP_SOURCES;
        if (!prefs.getBoolean("eud_api_sync_plugins", true))
            syncResourcesMask &= ~EudApiClient.RESOURCES_PLUGINS;
        return syncResourcesMask;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);

        if (!isDeviceSupported())
            return;

        ClearContentRegistry.getInstance().unregisterListener(this);
        ProductProviderManager.removeProvider(client.getProductProvider());
        _prefs.unregisterListener(_prefsListener);
    }

    @Override
    public void onClearContent(boolean clearmaps) {
        client.unlink();
    }

    private void configureLinkEudVisibility() {
        if (!_prefs.get("eud_api_disable_option", false)) {
            if (client == null || !client.isLinked()) {
                NavButtonManager.getInstance().addButtonModel(_model);
            } else {
                NavButtonManager.getInstance().removeButtonModel(_model);
            }
            ToolsPreferenceFragment
                    .register(
                            new ToolsPreferenceFragment.ToolPreference(
                                    _context.getString(
                                            R.string.eud_preferences),
                                    _context.getString(
                                            R.string.eud_preferences_summary),
                                    "eud_link_preference",
                                    _context.getResources().getDrawable(
                                            R.drawable.ic_link_eud, null),
                                    new EudApiPreferenceFragment()));
            // spawn a background thread to begin sync'ing the resources
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        client.syncResources(EudApiClient.RESOURCES_MAP_SOURCES,
                                false);
                    } catch (Throwable ignored) {
                    }
                }
            });
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();

        } else {
            NavButtonManager.getInstance().removeButtonModel(_model);
            ToolsPreferenceFragment.unregister("eud_link_preference");
            if (client != null) {
                if (client.isLinked())
                    client.unlink();
                else
                    client.evictResources(-1);
            }
        }
    }
}
