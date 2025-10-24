package com.atakmap.map.formats.c3dt;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.annotation.NonNull;

@DontObfuscate
final class CesiumUtility
{
    private CesiumUtility()
    {
    }

    public static String nativePathToUriPath(@NonNull String path)
    {
        return nativePathToUriPathImpl(path);
    }

    private static native String nativePathToUriPathImpl(String path);
}
