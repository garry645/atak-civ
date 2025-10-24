package com.atakmap.map.formats.quantizedmesh;

import com.atakmap.interop.Pointer;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
final class QMENative {
    private QMENative() {}

    static native Pointer createFromByteArray(byte[] buf, int off, int len, int level, int srid, String uri, String type);
    static native Pointer createFromPointer(long buf, int off, int len, int level, int srid, String uri, String type);
}
