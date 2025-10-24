package com.atakmap.map.formats.c3dt;

import com.atakmap.interop.Pointer;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class TileExternalContent
{
    private final Pointer pointer;

    TileExternalContent(Pointer ptr)
    {
        this.pointer = ptr;
    }
}
