package com.atakmap.map.layer.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class SceneInfoFactory
{
    private SceneInfoFactory() {}
    public static void registerAll()
    {
        registerAllNative();
    }

    public static void unregisterAll()
    {
        unregisterAllNative();
    }

    public static boolean isSupported(String path, String hint)
    {
        return isSupportedNative(path, hint);
    }

    public static Set<ModelInfo> create(String path, String hint)
    {
        ModelInfo[] result = createNative(path, hint);
        if(result == null || result.length == 0)
            return null;
        return new HashSet<>(Arrays.asList(result));
    }

    static native void registerAllNative();
    static native void unregisterAllNative();
    static native boolean isSupportedNative(String path, String hint);
    static native ModelInfo[] createNative(String path, String hint);
}
