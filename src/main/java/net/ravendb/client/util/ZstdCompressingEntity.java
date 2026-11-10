package net.ravendb.client.util;

import com.github.luben.zstd.ZstdOutputStream;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZstdCompressingEntity extends HttpEntityWrapper {

    private static final String ZSTD_CODEC = "zstd";

    private final Integer level;

    public ZstdCompressingEntity(final HttpEntity entity) {
        this(entity, null);
    }

    /**
     * @param entity wrapped entity
     * @param level zstd compression level (null - zstd-jni default)
     */
    public ZstdCompressingEntity(final HttpEntity entity, final Integer level) {
        super(entity);
        this.level = level;
    }

    @Override
    public String getContentEncoding() {
        return ZSTD_CODEC;
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
        final ZstdOutputStream zstd = level != null ? new ZstdOutputStream(outStream, level) : new ZstdOutputStream(outStream);
        super.writeTo(zstd);

        // Only close output stream if the wrapped entity has been
        // successfully written out
        zstd.close();
    }
}
