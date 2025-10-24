package com.atakmap.map.formats.c3dt;

import com.atakmap.interop.Pointer;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class TileRenderContent
{
    private final Pointer pointer;

    TileRenderContent(Pointer ptr)
    {
        this.pointer = ptr;
    }

    public Model getModel()
    {
        return getModel(pointer.raw);
    }

    private static native Model getModel(long ptr);
}
