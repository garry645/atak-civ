
package com.atak.plugins.impl;

import android.content.Context;
import android.content.pm.PackageManager;

import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.util.Disposable;

class ATAKServiceController implements IServiceController, Disposable {

    /** Service registry. Only a single service may be registered against a given type */
    final Map<Class<?>, Object> registry = new HashMap<>();
    final String packageName;
    final MapActivity mapActivity;

    /**
     * Component registry. A given object may be registered multiple times as different types.
     * <p>
     * Note that in the future, registrable component types could become variant; today all
     * registrable types in ATAK will either be or will be wrapped as `MapComponent`
     */
    final Map<Class<?>, Map<Object, MapComponent>> registeredComponents = new HashMap<>();

    final PluginContext pluginContext;

    ATAKServiceController(String packageName, MapActivity mapActivity,
            ClassLoader pluginClassLoader)
            throws PackageManager.NameNotFoundException {
        this.packageName = packageName;
        this.mapActivity = mapActivity;

        this.pluginContext = new PluginContext(this.mapActivity
                .createPackageContext(
                        packageName,
                        Context.CONTEXT_IGNORE_SECURITY
                                | Context.CONTEXT_INCLUDE_CODE),
                pluginClassLoader);

        registerService(MapView.class, mapActivity.getMapView());
        registerService(PluginContextProvider.class,
                new PluginContextProvider() {
                    @Override
                    public Context getPluginContext() {
                        return pluginContext;
                    }
                });
        registerService(ClassLoader.class, pluginClassLoader);
        registerService(IHostUIService.class,
                new ATAKUIService(mapActivity.getMapView()));

    }

    synchronized <T> void registerService(Class<T> type, T object) {
        registry.put(type, object);
    }

    @Override
    public synchronized <T> T getService(Class<T> serviceType) {
        final T svc = (T) registry.get(serviceType);
        if (svc != null)
            return svc;
        // no direct hit
        for (Map.Entry<Class<?>, Object> entry : registry.entrySet()) {
            // see if the client is requesting a superclass/superinterface
            if (entry.getKey().isAssignableFrom(serviceType))
                return (T) entry.getValue();
        }
        return null;
    }

    @Override
    public synchronized <T> boolean registerComponent(Class<T> type, T obj) {
        final MapComponent toRegister;
        if (MapComponent.class.isAssignableFrom(type))
            toRegister = new PluginInjectedMapComponent(pluginContext,
                    (MapComponent) obj);
        else if (IToolbarItem.class.isAssignableFrom(type))
            toRegister = new ToolMapComponent((IToolbarItem) obj,
                    packageName);
        else
            return false;

        mapActivity.registerMapComponent(toRegister);
        Map<Object, MapComponent> registrations = registeredComponents
                .get(type);
        if (registrations == null)
            registeredComponents.put(type,
                    registrations = new IdentityHashMap<>());
        registrations.put(obj, toRegister);

        return true;
    }

    @Override
    public synchronized <T> boolean unregisterComponent(Class<T> type, T obj) {
        if (registeredComponents.containsKey(type)) {
            final Map<Object, MapComponent> registrations = registeredComponents
                    .get(type);
            if (registrations != null) {
                final MapComponent toUnregister = registrations.remove(obj);
                if (toUnregister == null)
                    return false;
                mapActivity.unregisterMapComponent(toUnregister);
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        List<MapComponent> toDestroy = new LinkedList<>();
        synchronized (this) {
            for (Map<Object, MapComponent> components : registeredComponents
                    .values())
                toDestroy.addAll(components.values());
            registeredComponents.clear();
        }

        for (MapComponent component : toDestroy)
            mapActivity.unregisterMapComponent(component);
    }
}
