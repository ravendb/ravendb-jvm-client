package net.ravendb.client.util;

import com.github.luben.zstd.ZstdOutputStream;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZstdCompressingEntity extends HttpEntityWrapper {

    private static final String ZSTD_CODEC = "zstd";

    public ZstdCompressingEntity(final HttpEntity entity) {
        super(entity);
    }

    @Override
    public Header getContentEncoding() {
        return new BasicHeader(HTTP.CONTENT_ENCODING, ZSTD_CODEC);
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
        final ZstdOutputStream zstd = new ZstdOutputStream(outStream);

        wrappedEntity.writeTo(zstd);
        // Only close output stream if the wrapped entity has been
        // successfully written out
        zstd.close();
    }
}
