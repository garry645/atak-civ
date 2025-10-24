
package com.atak.plugins.impl;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapComponent;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;

public class AbstractPlugin implements IPlugin {

    private final IServiceController serviceController;
    private final IToolbarItem[] pluginTools;
    private final MapComponent pluginMapComponent;

    /**
     * Produces a map component without an associated tool icon in the toolbar.
     * @param serviceController the service controller
     * @param mapComponent the map component.
     */
    public AbstractPlugin(@NonNull IServiceController serviceController,
            @NonNull MapComponent mapComponent) {
        this(serviceController, new IToolbarItem[0], mapComponent);
    }

    /**
     * Produces a map component with an associated tool icon in the toolbar.
     * @param serviceController the service controller
     * @param tool the tool used to launch a drop down
     * @param mapComponent the map component.
     */
    public AbstractPlugin(@NonNull IServiceController serviceController,
            @NonNull IToolbarItem tool,
            @NonNull MapComponent mapComponent) {
        this(serviceController, new IToolbarItem[] {
                tool
        }, mapComponent);
    }

    /**
     * Produces a map component with an associated tool icon in the toolbar.
     * @param serviceController the service controller
     * @param tools the collection of tools used to launch a drop down
     * @param mapComponent the map component.
     */
    public AbstractPlugin(@NonNull IServiceController serviceController,
            @NonNull IToolbarItem[] tools,
            @NonNull MapComponent mapComponent) {
        this.serviceController = serviceController;
        pluginMapComponent = mapComponent;
        pluginTools = tools;
    }

    @Override
    public void onStart() {

        for (IToolbarItem pluginTool : pluginTools) {
            serviceController.registerComponent(IToolbarItem.class,
                    pluginTool);
        }
        serviceController.registerComponent(MapComponent.class,
                pluginMapComponent);
    }

    @Override
    public void onStop() {

        for (IToolbarItem pluginTool : pluginTools) {
            serviceController.unregisterComponent(IToolbarItem.class,
                    pluginTool);
        }

        serviceController.unregisterComponent(MapComponent.class,
                pluginMapComponent);
    }
}
