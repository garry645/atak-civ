package com.atakmap.lang;

final class UnsafePlatform
{
    private UnsafePlatform() {}

    static Unsafe.DirectByteBufferApi[] directByteBufferApi()
    {
        return new Unsafe.DirectByteBufferApi[0];
    }
}
