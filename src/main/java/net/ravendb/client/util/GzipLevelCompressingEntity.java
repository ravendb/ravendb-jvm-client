package net.ravendb.client.util;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip compressing entity with a configurable deflater level. Uses sync flush,
 * so flushing the stream sends the data written so far (needed for streamed requests like bulk insert).
 */
public class GzipLevelCompressingEntity extends HttpEntityWrapper {

    private static final String GZIP_CODEC = "gzip";

    private final int level;

    public GzipLevelCompressingEntity(final HttpEntity entity, final int level) {
        super(entity);
        this.level = level;
    }

    @Override
    public String getContentEncoding() {
        return GZIP_CODEC;
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public boolean isChunked() {
        // force content chunking
        return true;
    }

    @Override
    public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        final GZIPOutputStream gzip = new GZIPOutputStream(outStream, 8192, true) {
            {
                def.setLevel(level);
            }
        };
        super.writeTo(gzip);

        // Only close output stream if the wrapped entity has been
        // successfully written out
        gzip.close();
    }
}
