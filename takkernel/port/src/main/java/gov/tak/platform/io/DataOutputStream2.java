package gov.tak.platform.io;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataOutputStream2 extends FilterOutputStream implements DataOutput {
    final static ByteOrder defaultOrder = ByteOrder.BIG_ENDIAN;

    final ByteBuffer buf;

    public DataOutputStream2(OutputStream out) {
        this(out, defaultOrder, 8192);
    }

    public DataOutputStream2(OutputStream out, ByteOrder order) {
        this(out, order, 8192);
    }

    public DataOutputStream2(OutputStream out, int bufferSize) {
        this(out, defaultOrder, bufferSize);
    }

    public DataOutputStream2(OutputStream out, ByteOrder order, int bufferSize) {
        super(out);
        buf = ByteBuffer.wrap(new byte[bufferSize]);
        buf.order(order);
    }

    public void order(ByteOrder order) {
        buf.order(order);
    }

    public ByteOrder order() {
        return buf.order();
    }

    void ensureCapacity(int n) throws IOException {
        if(buf.remaining() < n) flush();
    }


    @Override
    public void writeBoolean(boolean b) throws IOException {
        ensureCapacity(1);
        buf.put(b ? (byte)1 : (byte)0);
    }

    @Override
    public void writeByte(int i) throws IOException {
        ensureCapacity(1);
        buf.put((byte)i);
    }

    @Override
    public void writeShort(int i) throws IOException {
        ensureCapacity(2);
        buf.putShort((short)i);
    }

    @Override
    public void writeChar(int i) throws IOException {
        ensureCapacity(2);
        buf.putChar((char)i);
    }

    @Override
    public void writeInt(int i) throws IOException {
        ensureCapacity(4);
        buf.putInt(i);
    }

    @Override
    public void writeLong(long l) throws IOException {
        ensureCapacity(8);
        buf.putLong(l);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        ensureCapacity(4);
        buf.putFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        ensureCapacity(8);
        buf.putDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        for(int i = 0; i < s.length(); i++)
            writeByte(s.charAt(i)&0xFF);
    }

    @Override
    public void writeChars(String s) throws IOException {
        for(int i = 0; i < s.length(); i++)
            writeChar(s.charAt(i));
    }

    @Override
    public void writeUTF(String s) throws IOException {
        int len = 0;
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c != 0 && c <= 0x7F)
                len += 1;
            else if(c <= 0x7FF)
                len += 2;
            else if(c <= 0xFFFF)
                len += 3;
            else
                throw new UTFDataFormatException("char out of range");
        }
        if(len > 0xFFFF)
            throw new UTFDataFormatException("len > 0xFFFF");
        byte[] bb = new byte[3];
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c != 0 && c <= 0x7F) {
                writeByte(c);
            } else if(c <= 0x7FF) {
                bb[0] = (byte)(0xc0 | (0x1f & (c >> 6)));
                bb[1] = (byte)(0x80 | (0x3f & c));
                write(bb, 0, 2);
            } else if(c <= 0xFFFF) {
                bb[0] = (byte)(0xe0 | (0x0f & (c >> 12)));
                bb[1] = (byte)(0x80 | (0x3f & (c >>  6)));
                bb[2] = (byte)(0x80 | (0x3f & c));
                write(bb, 0, 3);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(len <= buf.capacity()) {
            ensureCapacity(len);
            buf.put(b, off, len);
        } else {
            this.flush();
            out.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        if(buf.position() > 0) {
            out.write(buf.array(), 0, buf.position());
            buf.clear();
        }
    }
}
