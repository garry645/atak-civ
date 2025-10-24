
package com.atakmap.android.test.helpers;

import android.content.pm.ApplicationInfo;

import android.util.Log;

import com.atak.plugins.impl.AtakPluginRegistry;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ClassLoaderReplacer {
    private static final String TAG = "ClassLoaderReplacer";
    private static final Map<Class<?>, ClassLoader> oldLoader = new HashMap<>();

    public static void fixClassLoaderForClass(Class<?> clazz, String pluginPkg)
            throws NoSuchFieldException, IllegalAccessException {
        AtakPluginRegistry.PluginDescriptor plugin = AtakPluginRegistry.get()
                .getPlugin(pluginPkg);
        ClassLoader classLoader = clazz.getClassLoader();

        if (classLoader != null) {
            oldLoader.put(clazz, classLoader);
        } else {
            throw new NullPointerException(
                    "ClassLoader for plugin: " + plugin + " was null.");
        }
        setClassLoader(clazz, getPluginLoader(plugin));
    }

    public static void restoreLoader(Class<?> clazz)
            throws NoSuchFieldException, IllegalAccessException {
        ClassLoader oldClassLoader = oldLoader.remove(clazz);

        if (oldClassLoader != null) {
            setClassLoader(clazz, oldClassLoader);
        } else {
            Log.w(TAG, "Unable to restore classloader for class: " + clazz);
        }
    }

    private static ClassLoader getPluginLoader(
            AtakPluginRegistry.PluginDescriptor plugin)
            throws NoSuchFieldException, IllegalAccessException {
        ApplicationInfo info = plugin.extensions.get(0).parent.appInfo;
        return pluginClassLoaders().get(info.sourceDir);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClassLoader> pluginClassLoaders()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = AtakPluginRegistry.class
                .getDeclaredField("loadedSourceDirs");
        field.setAccessible(true);
        return (Map<String, ClassLoader>) field.get(AtakPluginRegistry.get());
    }

    // I am using reflection on the reflection libraries to replace the class loader.
    // May God have mercy on my soul.
    private static void setClassLoader(Class<?> clazz, ClassLoader loader)
            throws NoSuchFieldException, IllegalAccessException {
        Field classLoaderField = Class.class.getDeclaredField("classLoader");
        classLoaderField.setAccessible(true);
        classLoaderField.set(clazz, loader);
    }
}
