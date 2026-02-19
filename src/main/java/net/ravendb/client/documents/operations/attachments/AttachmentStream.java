package net.ravendb.client.documents.operations.attachments;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AttachmentStream extends InputStream {

    private ClassicHttpResponse response;
    private InputStream inner;

    public AttachmentStream(ClassicHttpResponse response, InputStream inner) {
        this.response = response;
        this.inner = inner;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return inner.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureOpen();
        return inner.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        return inner.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        return inner.skip(n);
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return inner.available();
    }

    @Override
    public void close() throws IOException {
        try {
            if (inner != null) {
                inner.close();
            }
        } finally {
            IOUtils.closeQuietly(response, null);
            inner = null;
            response = null;
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (inner != null) {
            inner.mark(readlimit);
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        ensureOpen();
        inner.reset();
    }

    @Override
    public boolean markSupported() {
        return inner != null && inner.markSupported();
    }

    private void ensureOpen() throws IOException {
        if (inner == null) {
            throw new IOException("Stream is closed");
        }
    }
}
