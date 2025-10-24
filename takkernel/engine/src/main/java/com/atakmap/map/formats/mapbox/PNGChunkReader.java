package com.atakmap.map.formats.mapbox;

import com.atakmap.io.SubInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class PNGChunkReader {
    interface Callback {
        void chunk(int length, int type, InputStream data);
        void crc32(int expected);
    }

    final static long PNG_MAGIC = 0x89504e470d0a1a0aL;
    InputStream stream;
    ByteBuffer buf;
    boolean invalid;

    PNGChunkReader(InputStream stream) {
        this.stream = stream;
        this.buf = ByteBuffer.wrap(new byte[8]).order(ByteOrder.BIG_ENDIAN);

        // verify PNG magic
        try {
            final long magic = (this.stream.read(this.buf.array(), 0, 8) == 8) ?
                    this.buf.getLong(0) : 0L;
            this.invalid = (magic != PNG_MAGIC);
        } catch(IOException e) {
            this.invalid = true;
        }
    }

    boolean nextChunk(Callback callback) throws IOException {
        if(this.invalid)
            return false;

        // read length & chunk type
        if(this.stream.read(this.buf.array(), 0, 8) == -1)
            return false;
        final int length = this.buf.getInt(0);
        final int type = this.buf.getInt(4);

        // create view of data
        final InputStream dataView = new SubInputStream(this.stream, length);
        // invoke callback
        callback.chunk(length, type, dataView);
        // fast-forward
        dataView.skip(length);
        // CRC
        if(this.stream.read(this.buf.array(), 0, 4) < 4)
            throw new EOFException("Unexpected EOF reading CRC32");
        callback.crc32(this.buf.getInt(0));
        return true;
    }
}
