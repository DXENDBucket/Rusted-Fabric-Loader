package io.github.endx.rustedfabric.android.patcher;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class CountingOutputStream extends FilterOutputStream {
    private long count;

    CountingOutputStream(OutputStream output) {
        super(output);
    }

    long count() {
        return count;
    }

    @Override
    public void write(int value) throws IOException {
        out.write(value);
        count++;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        out.write(buffer, offset, length);
        count += length;
    }
}
