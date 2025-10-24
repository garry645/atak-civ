
package com.atak.plugins.impl;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;

import gov.tak.api.plugin.IPlugin;

final class UniversalPluginMapComponent extends AbstractMapComponent {
    final IPlugin plugin;

    UniversalPluginMapComponent(IPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        this.plugin.onStop();
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        this.plugin.onStart();
    }
}
