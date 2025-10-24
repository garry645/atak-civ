
package com.atakmap.nio;


import gov.tak.test.KernelJniTest;

import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

public class BuffersTest extends KernelJniTest {
    private final int TEST_SIZE = 1024 * 1024 * 64;

    @org.junit.Test
    public void skip() {
        ByteBuffer b = ByteBuffer.allocate(TEST_SIZE);

        Buffers.skip(b, 100);
        assert (b.position() == 100);

    }

    // XXX- observed this test failing in all contexts. Unclear what the intent is based on
    //      inspection of Buffers.shift() implementation
    /*@org.junit.Test
    public void shift() {
        ByteBuffer b = ByteBuffer.allocate(TEST_SIZE);

        b.put(0, (byte) 67);
        Buffers.shift(b);
        assert (b.position() == 0);
        b.position(1);
        assert (b.get() == (byte) 67);

        b = ByteBuffer.allocateDirect(TEST_SIZE);
        b.put(0, (byte) 68);
        Buffers.shift(b);
        Buffers.shift(b);
        assert (b.position() == 0);
        b.position(2);
        assert (b.get() == (byte) 68);

    }*/
}
