package net.ravendb.client.documents.operations.attachments;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

public class LimitedInputStream extends InputStream {
    private final InputStream _in;
    private final long _max;
    private long _pos = 0;
    private long _mark = EOF;
    private boolean _closed = false;

    public LimitedInputStream(InputStream in, long size) {
        _in = in;
        _max = size;
    }

    @Override
    public int read() throws IOException {
        if (_closed) {
            throw new IOException("Attempted read on closed stream.");
        }

        if (_max >= 0 && _pos >= _max) {
            return EOF;
        }
        final int result = _in.read();
        _pos++;
        return result;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (_closed) {
            throw new IOException("Attempted read on closed stream.");
        }

        if (_max >= 0 && _pos >= _max) {
            return EOF;
        }
        long maxRead = _max >= 0 ? Math.min(len, _max - _pos) : len;
        int bytesRead = _in.read(b, off, (int)maxRead);

        if (bytesRead == EOF) {
            return EOF;
        }

        _pos += bytesRead;
        return bytesRead;
    }

    @Override
    public long skip(final long n) throws IOException {
        long toSkip = _max >= 0 ? Math.min(n, _max - _pos) : n;
        long skippedBytes = _in.skip(toSkip);
        _pos += skippedBytes;
        return skippedBytes;
    }

    @Override
    public int available() throws IOException {
        if (_max >= 0 && _pos >= _max) {
            return 0;
        }
        return _in.available();
    }

    @Override
    public String toString() {
        return _in.toString();
    }

    @Override
    public void close() throws IOException {
        if (_closed) {
            return;
        }

        long toSkip = _max - _pos;
        while (toSkip > 0) {
            toSkip -= this.skip(toSkip);
        }

        _closed = true;
    }

    public void close(boolean closeInnerStreamOnly) throws IOException {
        if (closeInnerStreamOnly) {
            _in.close();
        } else {
            close();
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        _in.reset();
        _pos = _mark;
    }

    @Override
    public synchronized void mark(final int readlimit) {
        _in.mark(readlimit);
        _mark = _pos;
    }

    @Override
    public boolean markSupported() {
        return _in.markSupported();
    }
}
