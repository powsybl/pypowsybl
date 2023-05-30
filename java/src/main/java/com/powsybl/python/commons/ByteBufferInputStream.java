package com.powsybl.python.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

    ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        int realLen = Math.min(len, buffer.remaining());
        buffer.get(bytes, off, realLen);
        return realLen;
    }

    @Override
    public int available() {
        return buffer.remaining();
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
